package net.osmand.shared.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformUtil {
	actual fun currentTimeMillis(): Long {
		return (NSDate().timeIntervalSince1970 * 1000).toLong()
	}
}