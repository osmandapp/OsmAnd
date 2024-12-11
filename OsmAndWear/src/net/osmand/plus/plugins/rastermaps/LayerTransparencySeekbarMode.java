package net.osmand.plus.plugins.rastermaps;

import android.content.Context;

import net.osmand.plus.R;

public enum LayerTransparencySeekbarMode {
	OVERLAY(R.string.overlay_transparency),
	UNDERLAY(R.string.map_transparency),
	OFF(R.string.shared_string_off),
	UNDEFINED(R.string.shared_string_none);

	private final int key;

	LayerTransparencySeekbarMode(int key) {
		this.key = key;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}
}
