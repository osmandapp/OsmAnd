package net.osmand.core.samples.android.sample1;

import java.text.MessageFormat;

public class Utils {

	public static String getFormattedDistance(double meters) {
		double mainUnitInMeters = 1000;
		String mainUnitStr = "km";
		if (meters >= 100 * mainUnitInMeters) {
			return (int) (meters / mainUnitInMeters + 0.5) + " " + mainUnitStr;
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.#} " + mainUnitStr, ((float) meters) / mainUnitInMeters).replace('\n', ' ');
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.##} " + mainUnitStr, ((float) meters) / mainUnitInMeters).replace('\n', ' ');
		} else {
			return ((int) (meters + 0.5)) + " m";
		}
	}

}
