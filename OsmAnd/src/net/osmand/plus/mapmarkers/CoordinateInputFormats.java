package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.support.annotation.IntDef;

import net.osmand.plus.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CoordinateInputFormats {

	public static final int DD_MM_MMM = 0;
	public static final int DD_MM_MMMM = 1;
	public static final int DD_DDDDD = 2;
	public static final int DD_DDDDDD = 3;
	public static final int DD_MM_SS = 4;

	public static int[] VALUES = new int[]{DD_MM_MMM, DD_MM_MMMM, DD_DDDDD, DD_DDDDDD, DD_MM_SS};

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({DD_MM_MMM, DD_MM_MMMM, DD_DDDDD, DD_DDDDDD, DD_MM_SS})
	@interface CoordinateInputFormatDef {
	}

	public static String formatToHumanString(Context ctx, @CoordinateInputFormatDef int format) {
		switch (format) {
			case DD_MM_MMM:
				return ctx.getString(R.string.dd_mm_mmm_format);
			case DD_MM_MMMM:
				return ctx.getString(R.string.dd_mm_mmmm_format);
			case DD_DDDDD:
				return ctx.getString(R.string.dd_ddddd_format);
			case DD_DDDDDD:
				return ctx.getString(R.string.dd_dddddd_format);
			case DD_MM_SS:
				return ctx.getString(R.string.dd_mm_ss_format);
		}
		return "Unknown format";
	}

	public static boolean containsThirdPart(@CoordinateInputFormatDef int format) {
		return format == DD_MM_MMM || format == DD_MM_MMMM || format == DD_MM_SS;
	}

	public static int getFirstPartSymbolsCount(@CoordinateInputFormatDef int format, boolean latitude) {
		return latitude ? 2 : 3;
	}

	public static int getSecondPartSymbolsCount(@CoordinateInputFormatDef int format) {
		switch (format) {
			case DD_MM_MMM:
			case DD_MM_MMMM:
			case DD_MM_SS:
				return 2;
			case DD_DDDDD:
				return 5;
			case DD_DDDDDD:
				return 6;
		}
		return 0;
	}

	public static int getThirdPartSymbolsCount(@CoordinateInputFormatDef int format) {
		switch (format) {
			case DD_MM_MMM:
				return 3;
			case DD_MM_MMMM:
				return 4;
			case DD_MM_SS:
				return 2;
			case DD_DDDDD:
			case DD_DDDDDD:
		}
		return 0;
	}

	public static String getFirstSeparator(@CoordinateInputFormatDef int format) {
		switch (format) {
			case DD_MM_MMM:
			case DD_MM_MMMM:
			case DD_MM_SS:
				return "°";
			case DD_DDDDD:
			case DD_DDDDDD:
				return ".";
		}
		return "";
	}

	public static String getSecondSeparator(@CoordinateInputFormatDef int format) {
		switch (format) {
			case DD_MM_MMM:
			case DD_MM_MMMM:
				return ".";
			case DD_DDDDD:
			case DD_DDDDDD:
				return "°";
			case DD_MM_SS:
				return "′";
		}
		return "";
	}
}
