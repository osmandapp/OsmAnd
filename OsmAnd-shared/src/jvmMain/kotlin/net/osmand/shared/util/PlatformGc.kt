package net.osmand.shared.util

actual fun runGarbageCollector() {
	System.gc()
}
