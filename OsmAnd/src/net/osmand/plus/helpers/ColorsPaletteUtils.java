package net.osmand.plus.helpers;

import static net.osmand.IndexConstants.TXT_EXT;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.io.File;

public class ColorsPaletteUtils {

	public static final String ROUTE_PREFIX = "route_";
	public static final String WEATHER_PREFIX = "weather_";
	public static final String USER_PALETTE_PREFIX = "user_palette_";
	public static final String DEFAULT_USER_PALETTE_FILE = USER_PALETTE_PREFIX + "default" + TXT_EXT;

	@NonNull
	public static String getPaletteName(@NonNull Context context, @NonNull File file) {
		String fileName = file.getName();
		if (DEFAULT_USER_PALETTE_FILE.equals(fileName)) {
			return context.getString(R.string.user_palette);
		}
		String prefix = null;
		if (fileName.startsWith(ROUTE_PREFIX)) {
			prefix = ROUTE_PREFIX;
		} else if (fileName.startsWith(WEATHER_PREFIX)) {
			prefix = WEATHER_PREFIX;
		} else if (fileName.startsWith(USER_PALETTE_PREFIX)) {
			prefix = USER_PALETTE_PREFIX;
		}
		if (prefix != null) {
			fileName = fileName.replace(prefix, "");
		}
		fileName = fileName.replace(TXT_EXT, "");
		StringBuilder result = new StringBuilder();
		for (String part : fileName.split("_")) {
			result.append(Algorithms.capitalizeFirstLetter(part)).append(" ");
		}
		return result.toString().trim();
	}

	@NonNull
	public static String getPaletteTypeName(@NonNull Context context, @NonNull File file) {
		String fileName = file.getName();
		if (fileName.startsWith(ROUTE_PREFIX)) {
			return context.getString(R.string.layer_route);
		} else if (fileName.startsWith(WEATHER_PREFIX)) {
			return context.getString(R.string.shared_string_weather);
		} else if (fileName.startsWith(USER_PALETTE_PREFIX)) {
			return context.getString(R.string.user_palette);
		}
		return context.getString(R.string.shared_string_terrain);
	}

}
