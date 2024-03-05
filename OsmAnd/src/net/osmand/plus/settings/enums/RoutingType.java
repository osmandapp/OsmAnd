package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum RoutingType {

	A_STAR_2_PHASE(R.string.routing_type_a_star_2_phase),
	A_STAR_CLASSIC(R.string.routing_type_a_star_classic),
	HH_JAVA(R.string.routing_type_hh_java),
	HH_CPP(R.string.routing_type_hh_cpp);

	@StringRes
	private final int titleId;

	RoutingType(@StringRes int titleId) {
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

	public boolean isHHRouting() {
		return this == HH_JAVA || this == HH_CPP;
	}
}
