package net.osmand.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DataSourceType {

	OFFLINE(R.string.shared_string_offline_only, R.drawable.ic_action_offline),
	ONLINE(R.string.shared_string_online_only, R.drawable.ic_world_globe_dark);

	@StringRes
	public final int nameId;
	@DrawableRes
	public final int iconId;

	DataSourceType(@StringRes int nameId, @DrawableRes int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}
}
