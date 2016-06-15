package net.osmand.core.samples.android.sample1;

import java.util.Locale;

public class MapUtils {

	public static final String LANGUAGE;

	static {
		LANGUAGE = getLanguage();
	}

	public static float unifyRotationTo360(float rotate) {
		while (rotate < -180) {
			rotate += 360;
		}
		while (rotate > +180) {
			rotate -= 360;
		}
		return rotate;
	}

	private static String getLanguage() {
		String langCode = Locale.getDefault().getLanguage();
		if (langCode.isEmpty()) {
			langCode = "en";
		}
		return langCode;
	}
}
