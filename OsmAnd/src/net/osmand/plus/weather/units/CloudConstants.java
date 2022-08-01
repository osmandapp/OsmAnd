package net.osmand.plus.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

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
