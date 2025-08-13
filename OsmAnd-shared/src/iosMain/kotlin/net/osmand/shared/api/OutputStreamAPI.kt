package net.osmand.shared.api

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.IOException
import platform.posix.uint8_tVar

interface OutputStreamAPI {

	@Throws(IOException::class)
	@OptIn(ExperimentalForeignApi::class)
	fun write(buffer: CPointer<uint8_tVar>?, maxLength: Int): Int

	@Throws(IOException::class)
	fun flush()
}