package net.osmand.shared.util

actual object PlatformUtil {
	actual fun currentTimeMillis(): Long {
		return System.currentTimeMillis()
	}
}