package net.osmand.plus.mapmarkers;

import android.content.Context;

import net.osmand.plus.R;

public enum MapMarkersMode {
	TOOLBAR(R.string.shared_string_topbar),
	WIDGETS(R.string.shared_string_widgets),
	NONE(R.string.shared_string_none);

	private final int key;

	MapMarkersMode(int key) {
		this.key = key;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	public boolean isToolbar() {
		return this == TOOLBAR;
	}

	public boolean isWidgets() {
		return this == WIDGETS;
	}

	public boolean isNone() {
		return this == NONE;
	}

	public static MapMarkersMode[] possibleValues(Context context) {
		return new MapMarkersMode[]{TOOLBAR, WIDGETS, NONE};
	}
}