package com.skash.kmod

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.windows.FALSE
import platform.windows.FindWindowA
import platform.windows.GetWindowThreadProcessId
import platform.windows.HANDLE
import platform.windows.OpenProcess
import platform.windows.PROCESS_VM_OPERATION
import platform.windows.PROCESS_VM_READ
import platform.windows.PROCESS_VM_WRITE
import platform.windows.ReadProcessMemory
import platform.windows.SIZE_TVar
import platform.windows.WriteProcessMemory
import win32.tlhelp32.CreateToolhelp32Snapshot
import win32.tlhelp32.MODULEENTRY32W
import win32.tlhelp32.Module32FirstW
import win32.tlhelp32.Module32NextW
import win32.tlhelp32.TH32CS_SNAPMODULE
import win32.tlhelp32.TH32CS_SNAPMODULE32
import kotlin.Int

class WindowsProcess @OptIn(ExperimentalForeignApi::class) constructor(
    val name: String,
    val processId: UInt,
    val handle: HANDLE
) {

    @OptIn(ExperimentalForeignApi::class)
    operator fun get(module: String): WindowsModule? = memScoped {
        val flags = (TH32CS_SNAPMODULE or TH32CS_SNAPMODULE32).toUInt()

        val snapshot = CreateToolhelp32Snapshot(
            dwFlags = flags,
            th32ProcessID = processId
        )

        val moduleEntry = alloc<MODULEENTRY32W>()
        moduleEntry.dwSize = sizeOf<MODULEENTRY32W>().toUInt()


        if (Module32FirstW(snapshot, moduleEntry.ptr) != 0) {

            while (Module32NextW(snapshot, moduleEntry.ptr) != 0) {
                val currentModuleName = moduleEntry.szModule.toKString()

                if (currentModuleName.equals(module, ignoreCase = true)) {
                    val baseAddress = moduleEntry.modBaseAddr.rawValue.toLong()
                    val size = moduleEntry.modBaseSize

                    return@memScoped WindowsModule(
                        name = name,
                        process = this@WindowsProcess,
                        baseAddress = Address(baseAddress),
                        size = size
                    )
                }
            }
        }

        return@memScoped null
    }

    fun deref(baseAddress: Address, vararg offsets: Int): Address = baseAddress.value.deref(*offsets)

    @OptIn(ExperimentalForeignApi::class)
    fun Long.deref(vararg offsets: Int): Address {
        var currentAddress = this

        for (i in 0 until offsets.size - 1) {
            val pointer32 = read<IntVar, Int>(Address(currentAddress + offsets[i])) { it.value }
            currentAddress = pointer32.toUInt().toLong()

            if (currentAddress == 0L) {
                return Address(0)
            }
        }

        return Address(currentAddress + offsets.last())
    }


    @OptIn(ExperimentalForeignApi::class)
    inline fun <reified T : CVariable, R> read(address: Address, extractor: (T) -> R): R = memScoped {
        val typeSize = sizeOf<T>()
        val rawBuffer = allocArray<ByteVar>(typeSize)
        val bytesRead = alloc<SIZE_TVar>()

        val result = ReadProcessMemory(
            hProcess = handle,
            lpBaseAddress = address.value.toCPointer(),
            lpBuffer = rawBuffer,
            nSize = sizeOf<T>().toULong(),
            lpNumberOfBytesRead = bytesRead.ptr,
        )

        return@memScoped extractor(rawBuffer.reinterpret<T>().pointed)
    }

    @OptIn(ExperimentalForeignApi::class)
    fun writeInt(value: Int) = memScoped {

        val nativeInt = alloc<IntVar>()
        val bytesWritten = alloc<SIZE_TVar>()

        nativeInt.value = value

        val address = 0x03092DC8L
        val lpvoidAddress: COpaquePointer? = address.toCPointer()

        WriteProcessMemory(
            hProcess = handle,
            lpBaseAddress = lpvoidAddress,
            lpBuffer = nativeInt.ptr,
            nSize = sizeOf<IntVar>().toULong(),
            lpNumberOfBytesWritten = bytesWritten.ptr
        )
    }

    companion object {

        @OptIn(ExperimentalForeignApi::class)
        fun byWindowName(name: String): WindowsProcess? = memScoped {
            val window = FindWindowA(null, name) ?: return null
            val processIdPtr = alloc<UIntVarOf<UInt>>()
            val processId = GetWindowThreadProcessId(window, processIdPtr.ptr)
            val perms = PROCESS_VM_OPERATION or PROCESS_VM_WRITE or PROCESS_VM_READ
            val handle = OpenProcess(perms.toUInt(), FALSE, processIdPtr.value)
                ?: return@memScoped null

            return@memScoped WindowsProcess(
                name = "",
                handle = handle,
                processId = processIdPtr.value,
            )
        }

        fun byProcessName(name: String): WindowsProcess? {
            return null
        }
    }
}