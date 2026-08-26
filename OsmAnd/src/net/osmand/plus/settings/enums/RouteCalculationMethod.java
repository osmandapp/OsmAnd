package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;

public enum RouteCalculationMethod {

	AUTO(R.string.route_calculation_method_auto, R.string.route_calculation_method_auto_desc),
	STANDARD_ONLY(R.string.route_calculation_method_standard_only, R.string.route_calculation_method_standard_only_desc),
	FAST_ONLY(R.string.route_calculation_method_fast_only, R.string.route_calculation_method_fast_only_desc);

	@StringRes
	private final int titleId;
	@StringRes
	private final int descriptionId;

	RouteCalculationMethod(@StringRes int titleId, @StringRes int descriptionId) {
		this.titleId = titleId;
		this.descriptionId = descriptionId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(getTitleId());
	}

	@NonNull
	public String getDescription(@NonNull Context ctx) {
		return ctx.getString(getDescriptionId());
	}

	public boolean isFastRoutingPossible(@Nullable ApplicationMode mode) {
		return (this == AUTO || this == FAST_ONLY) && canProfileUseFastRouting(mode);
	}

	public boolean isFastRoutingOnly(@Nullable ApplicationMode mode) {
		return this == FAST_ONLY && canProfileUseFastRouting(mode);
	}

	public boolean canProfileUseFastRouting(@Nullable ApplicationMode mode) {
		return mode != null && mode.getRouteService() == RouteService.OSMAND &&
				(ApplicationMode.CAR.isDerivedRoutingFrom(mode) || ApplicationMode.BICYCLE.isDerivedRoutingFrom(mode));
	}
}
