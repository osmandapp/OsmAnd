package net.osmand.util;

public class TextDirectionUtil {

	public static final String LTR_MARK = "\u200e";
	public static final String RTL_MARK = "\u200f";

	public static String markAsLTR(String text) {
		return LTR_MARK + text;
	}

	public static String markAsRTL(String text) {
		return RTL_MARK + text;
	}

	public static String clearDirectionMarks(String text) {
		return text.replaceAll(LTR_MARK, "").replaceAll(RTL_MARK, "");
	}
}
