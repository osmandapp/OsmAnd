package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

public enum MapLayerType {

	MAP_SOURCE(R.string.map_source, R.drawable.ic_world_globe_dark),
	MAP_UNDERLAY(R.string.map_underlay, R.drawable.ic_layer_bottom),
	MAP_OVERLAY(R.string.map_overlay, R.drawable.ic_layer_top);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	MapLayerType(@StringRes int nameId, @DrawableRes int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getMapSourceNameForLayer(@NonNull OsmandApplication app) {
		return getMapLayerSettings(app).get();
	}

	@NonNull
	public CommonPreference<String> getMapLayerSettings(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (this == MAP_SOURCE) {
			return settings.MAP_TILE_SOURCES;
		} else if (this == MAP_OVERLAY) {
			return settings.MAP_OVERLAY;
		} else {
			return settings.MAP_UNDERLAY;
		}
	}
}
