package net.osmand.plus.settings.enums;

import static net.osmand.shared.util.OsmAndFormatter.KILOGRAMS_IN_ONE_TON;
import static net.osmand.shared.util.OsmAndFormatter.POUNDS_IN_ONE_TON;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.CollectionUtils;

import java.text.DecimalFormat;

public enum WeightUnits implements MeasurementUnits {

	TONES(R.string.shared_string_tones, R.string.metric_ton, 1),
	KILOGRAMS(R.string.shared_string_kilograms, R.string.kg, KILOGRAMS_IN_ONE_TON),
	POUNDS(R.string.shared_string_pounds, R.string.metric_lbs, POUNDS_IN_ONE_TON);

	private final int symbolResId;
	private final int titleResId;
	private final float coefficient;

	WeightUnits(@StringRes int titleResId, @StringRes int symbolResId, float coefficient) {
		this.titleResId = titleResId;
		this.symbolResId = symbolResId;
		this.coefficient = coefficient;
	}

	@StringRes
	@Override
	public int getNameResId() {
		return titleResId;
	}

	@StringRes
	@Override
	public int getSymbolResId() {
		return symbolResId;
	}

	@Override
	public float getConversionCoefficient() {
		return coefficient;
	}

	@Override
	public boolean isMetric() {
		return CollectionUtils.equalsToAny(this, TONES, KILOGRAMS);
	}

	@NonNull
	@Override
	public String formatValue(float value) {
		if (this == POUNDS) {
			DecimalFormat formatter = new DecimalFormat("#,###.#");
			return formatter.format(value);
		}
		return MeasurementUnits.super.formatValue(value);
	}
}
