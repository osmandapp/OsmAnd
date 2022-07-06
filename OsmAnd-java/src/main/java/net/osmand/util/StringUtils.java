package net.osmand.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class StringUtils {

	public static final String UTF_8 = "UTF-8";

	public static String encode(String text) throws UnsupportedEncodingException {
		return URLEncoder.encode(text, UTF_8);
	}

	public static String decode(String text) throws UnsupportedEncodingException {
		return URLDecoder.decode(text, UTF_8);
	}

}
