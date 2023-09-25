package net.osmand.plus.download;

import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.EXTRA_ZIP_EXT;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;

public enum LocalIndexType {

	MAP_DATA(R.string.local_indexes_cat_map, R.drawable.ic_map, 10),
	TILES_DATA(R.string.local_indexes_cat_tile, R.drawable.ic_map, 60),
	SRTM_DATA(R.string.local_indexes_cat_srtm, R.drawable.ic_plugin_srtm, 40),
	TERRAIN_DATA(R.string.local_indexes_category_terrain, R.drawable.ic_action_terrain, 42),
	DEPTH_DATA(R.string.nautical_depth, R.drawable.ic_action_nautical_depth, 45),
	WIKI_DATA(R.string.local_indexes_cat_wiki, R.drawable.ic_plugin_wikipedia, 50),
	TRAVEL_DATA(R.string.download_maps_travel, R.drawable.ic_plugin_wikipedia, 60),
	WEATHER_DATA(R.string.weather_forecast, R.drawable.ic_action_umbrella, 65),
	TTS_VOICE_DATA(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up, 20),
	VOICE_DATA(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up, 30),
	FONT_DATA(R.string.fonts_header, R.drawable.ic_action_map_language, 35),
	DEACTIVATED(R.string.local_indexes_cat_backup, R.drawable.ic_type_archive, 1000);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;
	private final int orderIndex;

	LocalIndexType(@StringRes int titleId, @DrawableRes int iconId, int orderIndex) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.orderIndex = orderIndex;
	}

	@NonNull
	public String getHumanString(Context ctx) {
		return ctx.getString(titleId);
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public int getOrderIndex(@NonNull LocalIndexInfo info) {
		String fileName = info.getFileName();
		int index = info.getOriginalType().orderIndex;
		if (info.getType() == DEACTIVATED) {
			index += DEACTIVATED.orderIndex;
		}
		if (fileName.endsWith(BINARY_ROAD_MAP_INDEX_EXT)) {
			index++;
		}
		return index;
	}

	@NonNull
	public String getBasename(@NonNull OsmandApplication app, @NonNull LocalIndexInfo indexInfo) {
		String fileName = indexInfo.getFileName();
		if (fileName.endsWith(EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - EXTRA_ZIP_EXT.length());
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			OsmandSettings settings = app.getSettings();
			return settings.getTileSourceTitle(fileName);
		}
		if (indexInfo.getType() == TRAVEL_DATA && fileName.endsWith(BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
			return fileName.substring(0, fileName.length() - BINARY_WIKIVOYAGE_MAP_INDEX_EXT.length());
		}
		if (this == VOICE_DATA) {
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l);
		}
		if (this == FONT_DATA) {
			int l = fileName.indexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l).replace('_', ' ').replace('-', ' ');
		}
		int ls = fileName.lastIndexOf('_');
		if (ls >= 0) {
			return fileName.substring(0, ls);
		} else if (fileName.indexOf('.') > 0) {
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}
}