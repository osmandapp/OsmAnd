package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum HistorySource {

	SEARCH(R.string.shared_string_search_history),
	NAVIGATION(R.string.navigation_history);

	@StringRes
	public final int nameId;

	HistorySource(@StringRes int nameId) {
		this.nameId = nameId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}

	@NonNull
	public static HistorySource getHistorySourceByName(@Nullable String name) {
		for (HistorySource source : values()) {
			if (source.name().equalsIgnoreCase(name)) {
				return source;
			}
		}
		return SEARCH;
	}
}