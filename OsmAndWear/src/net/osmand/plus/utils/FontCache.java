package net.osmand.plus.utils;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;

import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontCache {

	public static final int FONT_WEIGHT_NORMAL = 400;
	public static final int FONT_WEIGHT_MEDIUM = 500;

	private static final Map<Pair<Typeface, Integer>, Typeface> FONT_MAP = new ConcurrentHashMap<>();

	@NonNull
	public static Typeface getMediumFont() {
		return getFont(null, FONT_WEIGHT_MEDIUM);
	}

	@NonNull
	public static Typeface getNormalFont() {
		return getFont(null, FONT_WEIGHT_NORMAL);
	}

	@NonNull
	public static Typeface getMediumFont(@Nullable Typeface original) {
		return getFont(original, FONT_WEIGHT_MEDIUM);
	}

	@NonNull
	public static Typeface getNormalFont(@Nullable Typeface original) {
		return getFont(original, FONT_WEIGHT_NORMAL);
	}

	@NonNull
	public static Typeface getFont(@Nullable Typeface original, int weight) {
		Pair<Typeface, Integer> pair = Pair.create(original, weight);

		Typeface typeface = FONT_MAP.get(pair);
		if (typeface == null) {
			if (VERSION.SDK_INT >= VERSION_CODES.P) {
				boolean italic = original != null && original.isItalic();
				typeface = Typeface.create(original, weight, italic);
			} else {
				typeface = Typeface.create(original, weight > FONT_WEIGHT_NORMAL ? BOLD : NORMAL);
			}
			FONT_MAP.put(pair, typeface);
		}
		return typeface;
	}
}
