package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.text.DecimalFormat;

public interface MeasurementUnits {

	@StringRes
	int getNameResId();

	@StringRes
	int getSymbolResId();

	default float toBase(float value) {
		return value / getConversionCoefficient();
	}

	default float fromBase(float value) {
		return value * getConversionCoefficient();
	}

	float getConversionCoefficient();

	boolean isMetric();

	default boolean isImperial() {
		return !isMetric();
	}

	@NonNull
	default String formatValue(float value) {
		DecimalFormat formatter = new DecimalFormat("#.#");
		return formatter.format(value);
	}
}