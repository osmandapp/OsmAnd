package net.osmand.shared.util

import java.net.URLDecoder
import java.net.URLEncoder

actual object UrlEncoder {
	actual fun encode(s: String): String {
		return URLEncoder.encode(s, "UTF-8")
	}

	actual fun decode(s: String): String {
		return try {
			URLDecoder.decode(s, "UTF-8")
		} catch (e: Exception) {
			s
		}
	}
}