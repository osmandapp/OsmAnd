package net.osmand.data;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DataSourceType {

	OFFLINE(R.string.shared_string_offline),
	ONLINE(R.string.shared_string_online);

	@StringRes
	public final int nameId;

	DataSourceType(@StringRes int nameId) {
		this.nameId = nameId;
	}
}
