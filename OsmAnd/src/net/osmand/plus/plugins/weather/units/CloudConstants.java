package net.osmand.plus.plugins.weather.units;

import androidx.annotation.NonNull;

public enum CloudConstants {

	PERCENT("%");

	private final String unit;

	CloudConstants(@NonNull String unit) {
		this.unit = unit;
	}

	@NonNull
	public String getUnit() {
		return unit;
	}

}
