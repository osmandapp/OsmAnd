package net.osmand.plus.settings.enums;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.shared.settings.enums.MetricsConstants;

import java.util.Locale;

/**
 * Class represents specific for driving region
 * Signs, leftHandDriving
 */
public enum DrivingRegion {

	EUROPE_ASIA(R.string.driving_region_europe_asia, MetricsConstants.KILOMETERS_AND_METERS, false),
	US(R.string.driving_region_us, MetricsConstants.MILES_AND_FEET, false),
	CANADA(R.string.driving_region_canada, MetricsConstants.KILOMETERS_AND_METERS, false),
	UK_AND_OTHERS(R.string.driving_region_uk, MetricsConstants.MILES_AND_METERS, true),
	JAPAN(R.string.driving_region_japan, MetricsConstants.KILOMETERS_AND_METERS, true),
	INDIA(R.string.driving_region_india, MetricsConstants.KILOMETERS_AND_METERS, true),
	AUSTRALIA(R.string.driving_region_australia, MetricsConstants.KILOMETERS_AND_METERS, true);

	public final boolean leftHandDriving;
	public final MetricsConstants defMetrics;
	public final int name;

	DrivingRegion(int name, MetricsConstants def, boolean leftHandDriving) {
		this.name = name;
		defMetrics = def;
		this.leftHandDriving = leftHandDriving;
	}

	public boolean isAmericanTypeSigns() {
		return this == AUSTRALIA ||
				this == US ||
				this == CANADA;
	}

	public String getDescription(Context ctx) {
		return ctx.getString(leftHandDriving ? R.string.left_side_navigation : R.string.right_side_navigation) +
				", " +
				defMetrics.toHumanString().toLowerCase();
	}

	public static DrivingRegion getDrivingRegionByLocale() {
		Locale df = Locale.getDefault();
		if (df == null) {
			return EUROPE_ASIA;
		}
		if (df.getCountry().equalsIgnoreCase(Locale.US.getCountry())) {
			return US;
		} else if (df.getCountry().equalsIgnoreCase(Locale.CANADA.getCountry())) {
			return CANADA;
		} else if (df.getCountry().equalsIgnoreCase(Locale.JAPAN.getCountry())) {
			return JAPAN;
		} else if (df.getCountry().equalsIgnoreCase("au")) {
			return AUSTRALIA;
		} else if (df.getCountry().equalsIgnoreCase(Locale.UK.getCountry())) {
			return UK_AND_OTHERS;
		} else if (df.getCountry().equalsIgnoreCase("in")) {
			return INDIA;
		}
		return EUROPE_ASIA;
	}
}