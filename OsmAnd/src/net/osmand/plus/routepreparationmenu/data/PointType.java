package net.osmand.plus.routepreparationmenu.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum PointType {
	START(R.string.add_start_point),
	TARGET(R.string.add_destination_point),
	INTERMEDIATE(R.string.add_intermediate_point),
	HOME(R.string.add_home),
	WORK(R.string.add_work),
	PARKING(-1);

	@StringRes
	private final int titleId;

	PointType(@StringRes int titleId) {
		this.titleId = titleId;
	}

	@NonNull
	public String getTitle(@NonNull Context context) {
		return titleId == -1 ? "" : context.getString(titleId);
	}
}
