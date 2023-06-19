package net.osmand.plus.plugins.srtm;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TerrainMode {
	HILLSHADE(R.string.shared_string_hillshade),
	SLOPE(R.string.shared_string_slope);

	final int nameId;

	TerrainMode(@StringRes int nameId) {
		this.nameId = nameId;
	}
}
