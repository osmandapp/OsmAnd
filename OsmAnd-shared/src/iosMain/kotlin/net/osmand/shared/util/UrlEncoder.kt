package net.osmand.shared.util

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.Foundation.stringByRemovingPercentEncoding

@OptIn(BetaInteropApi::class)
actual object UrlEncoder {
	actual fun encode(s: String): String {
		val nsString = NSString.create(string = s)
		return nsString.stringByAddingPercentEncodingWithAllowedCharacters(
			NSCharacterSet.URLQueryAllowedCharacterSet
		) ?: s
	}

	actual fun decode(s: String): String {
		val nsString = NSString.create(string = s)
		return nsString.stringByRemovingPercentEncoding ?: s
	}
}