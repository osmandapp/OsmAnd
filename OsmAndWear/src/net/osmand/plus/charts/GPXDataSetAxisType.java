package net.osmand.plus.charts;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GPXDataSetAxisType {

	DISTANCE(R.string.distance, R.drawable.ic_action_distance),
	TIME(R.string.shared_string_time, R.drawable.ic_action_time_span),
	TIME_OF_DAY(R.string.time_of_day, R.drawable.ic_action_time_of_day);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	GPXDataSetAxisType(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@NonNull
	public String getName(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}
