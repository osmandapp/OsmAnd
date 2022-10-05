package net.osmand.plus.plugins.weather.units;

import androidx.annotation.NonNull;

public enum CloudConstants {

	PERCENT("%");

	private String unit;

	CloudConstants(@NonNull String unit) {
		this.unit = unit;
	}

	@NonNull
	public String getUnit() {
		return unit;
	}

}
