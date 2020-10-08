package net.osmand.plus.helpers.enums;

import android.content.Context;

import net.osmand.plus.R;

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
		return this == DrivingRegion.AUSTRALIA ||
				this == DrivingRegion.US ||
				this == DrivingRegion.CANADA;
	}

	public String getDescription(Context ctx) {
		return ctx.getString(leftHandDriving ? R.string.left_side_navigation : R.string.right_side_navigation) +
				", " +
				defMetrics.toHumanString(ctx).toLowerCase();
	}

	public static DrivingRegion getDrivingRegionByLocale() {
		Locale df = Locale.getDefault();
		if (df == null) {
			return DrivingRegion.EUROPE_ASIA;
		}
		if (df.getCountry().equalsIgnoreCase(Locale.US.getCountry())) {
			return DrivingRegion.US;
		} else if (df.getCountry().equalsIgnoreCase(Locale.CANADA.getCountry())) {
			return DrivingRegion.CANADA;
		} else if (df.getCountry().equalsIgnoreCase(Locale.JAPAN.getCountry())) {
			return DrivingRegion.JAPAN;
		} else if (df.getCountry().equalsIgnoreCase("au")) {
			return DrivingRegion.AUSTRALIA;
		} else if (df.getCountry().equalsIgnoreCase(Locale.UK.getCountry())) {
			return DrivingRegion.UK_AND_OTHERS;
		}
		return DrivingRegion.EUROPE_ASIA;
	}
}