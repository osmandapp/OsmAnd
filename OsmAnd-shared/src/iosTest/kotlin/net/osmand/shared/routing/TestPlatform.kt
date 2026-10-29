package net.osmand.shared.routing

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.toKString
import platform.CoreFoundation.CFRunLoopRunInMode
import platform.CoreFoundation.kCFRunLoopDefaultMode
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun <T> testAppCall(block: () -> T): T =
	try {
		autoreleasepool { block() }
	} finally {
		// the releases the collector leaves to the main thread, which the run loop of an app runs
		CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.0, false)
	}
