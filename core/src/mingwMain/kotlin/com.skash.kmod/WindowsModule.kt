package com.skash.kmod

import kotlinx.cinterop.CVariable
import kotlinx.cinterop.ExperimentalForeignApi

class WindowsModule(
    val name: String,
    val process: WindowsProcess,
    val baseAddress: Address,
    val size: UInt
) {

    @OptIn(ExperimentalForeignApi::class)
    inline fun <reified T : CVariable, R> read(
        address: Address,
        extractor: (T) -> R
    ): R = process.read(address, extractor = extractor)

    fun deref(vararg offsets: Int): Address = process.deref(baseAddress, *offsets)
}