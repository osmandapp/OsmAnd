package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ApproximationType {

	APPROX_CPP(R.string.gpx_approximation_cpp),
	APPROX_JAVA(R.string.gpx_approximation_java),
	APPROX_GEO_CPP(R.string.gpx_approximation_geo_cpp),
	APPROX_GEO_JAVA(R.string.gpx_approximation_geo_java);

	@StringRes
	private final int titleId;

	ApproximationType(@StringRes int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(getTitleId());
	}

	public boolean isNativeApproximation() {
		return this == APPROX_CPP || this == APPROX_GEO_CPP;
	}

	public boolean isGeoApproximation() {
		return this == APPROX_GEO_JAVA || this == APPROX_GEO_CPP;
	}
}
