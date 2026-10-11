package com.kmod.sample

import com.skash.kmod.WindowsProcess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.value

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val process = WindowsProcess.byWindowName("AssaultCube") ?: return
    val module = process["ac_client.exe"] ?: return

    val healthPointer = module.deref(0x17E0A8, 0xEC)
    val health = module.read<IntVar, Int>(healthPointer) { it.value }
    println("Found health: $health")
}