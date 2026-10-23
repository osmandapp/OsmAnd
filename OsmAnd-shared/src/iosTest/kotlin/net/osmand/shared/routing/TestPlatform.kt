package net.osmand.shared.routing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(ExperimentalForeignApi::class)
actual fun testEnvironment(name: String): String? = getenv(name)?.toKString()

actual fun testPlatformName(): String = "kotlin/native ios"

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun testHeapAfterGc(): Long {
	GC.collect()
	return GC.lastGCInfo?.memoryUsageAfter?.get("heap")?.totalObjectsSizeBytes ?: -1
}
