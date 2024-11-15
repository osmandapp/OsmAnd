package net.osmand.shared.extensions

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
	return this.usePinned { pinned ->
		NSData.create(
			bytes = pinned.addressOf(0),
			length = this.size.toULong()
		)
	}
}
