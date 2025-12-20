package net.osmand.plus.settings.enums;

import static net.osmand.plus.utils.OsmAndFormatter.CENTIMETERS_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.INCHES_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.CollectionUtils;

public enum LengthUnits implements MeasurementUnits {

	METERS(R.string.shared_string_meters, R.string.m, 1),
	CENTIMETERS(R.string.shared_string_centimeters, R.string.centimeter, CENTIMETERS_IN_ONE_METER),
	INCHES(R.string.shared_string_inches, R.string.inch, INCHES_IN_ONE_METER),
	FEET(R.string.shared_string_feet, R.string.foot, FEET_IN_ONE_METER),
	YARDS(R.string.shared_string_yards, R.string.yard, YARDS_IN_ONE_METER);

	private final int symbolResId;
	private final int nameResId;
	private final float coefficient;

	LengthUnits(@StringRes int nameResId, @StringRes int symbolResId, float coefficient) {
		this.nameResId = nameResId;
		this.symbolResId = symbolResId;
		this.coefficient = coefficient;
	}

	@StringRes
	@Override
	public int getSymbolResId() {
		return symbolResId;
	}

	@StringRes
	@Override
	public int getNameResId() {
		return nameResId;
	}

	@Override
	public float getConversionCoefficient() {
		return coefficient;
	}

	@Override
	public boolean isMetric() {
		return CollectionUtils.equalsToAny(this, METERS, CENTIMETERS);
	}
}
