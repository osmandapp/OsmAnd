package net.osmand.shared.util

import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(NativeRuntimeApi::class)
actual fun runGarbageCollector() {
	GC.collect()
}
