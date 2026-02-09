package net.osmand.plus.download;

public enum DownloadResourceGroupType {
	// headers
	WORLD_MAPS(net.osmand.plus.R.string.world_maps),
	REGION_MAPS(net.osmand.plus.R.string.region_maps),
	SRTM_HEADER(net.osmand.plus.R.string.download_srtm_maps),
	HILLSHADE_HEADER(net.osmand.plus.R.string.download_hillshade_maps),
	OTHER_MAPS_HEADER(net.osmand.plus.R.string.download_select_map_types),
	WIKIVOYAGE_HEADER(net.osmand.plus.R.string.shared_string_wikivoyage),

	NAUTICAL_WORLDWIDE_HEADER(net.osmand.plus.R.string.worldwide_maps),
	NAUTICAL_DEPTH_HEADER(net.osmand.plus.R.string.depth_contours),
	NAUTICAL_POINTS_HEADER(net.osmand.plus.R.string.nautical_depth_points),
	// headers with voice items
	VOICE_HEADER_TTS(net.osmand.plus.R.string.index_name_tts_voice),
	VOICE_HEADER_REC(net.osmand.plus.R.string.index_name_voice),
	// headers with font items
	FONTS_HEADER(net.osmand.plus.R.string.fonts_header),
	// headers with resources
	NAUTICAL_MAPS_GROUP(net.osmand.plus.R.string.nautical_maps),
	TRAVEL_GROUP(net.osmand.plus.R.string.download_maps_travel),
	OTHER_MAPS_GROUP(net.osmand.plus.R.string.download_select_map_types),
	OTHER_GROUP(net.osmand.plus.R.string.other_menu_group),
	SUBREGIONS(net.osmand.plus.R.string.regions),
	// screen items
	NAUTICAL_MAPS(net.osmand.plus.R.string.nautical_maps),
	WIKIVOYAGE_MAPS(net.osmand.plus.R.string.download_maps_travel),
	VOICE_TTS(net.osmand.plus.R.string.index_name_tts_voice),
	FONTS(net.osmand.plus.R.string.fonts_header),
	VOICE_REC(net.osmand.plus.R.string.index_name_voice),
	OTHER_MAPS(net.osmand.plus.R.string.download_select_map_types),
	EXTRA_MAPS(net.osmand.plus.R.string.extra_maps_menu_group),
	WORLD(-1),
	REGION(-1);

	final int resId;

	DownloadResourceGroupType(int resId) {
		this.resId = resId;
	}

	public boolean isScreen() {
		return this == WORLD || this == REGION || this == VOICE_TTS
				|| this == VOICE_REC || this == OTHER_MAPS || this == FONTS || this == NAUTICAL_MAPS || this == WIKIVOYAGE_MAPS;
	}

	public String getDefaultId() {
		return name().toLowerCase();
	}

	public int getResourceId() {
		return resId;
	}

	public boolean containsIndexItem() {
		return isHeader() && this != SUBREGIONS && this != OTHER_GROUP && this != OTHER_MAPS_GROUP
				&& this != NAUTICAL_MAPS_GROUP && this != TRAVEL_GROUP && this != EXTRA_MAPS;
	}

	public boolean isHeader() {
		return this == VOICE_HEADER_REC || this == VOICE_HEADER_TTS
				|| this == SUBREGIONS
				|| this == WORLD_MAPS || this == REGION_MAPS || this == OTHER_GROUP
				|| this == HILLSHADE_HEADER || this == SRTM_HEADER
				|| this == OTHER_MAPS_HEADER || this == OTHER_MAPS_GROUP
				|| this == FONTS_HEADER
				|| this == NAUTICAL_WORLDWIDE_HEADER
				|| this == NAUTICAL_DEPTH_HEADER
				|| this == NAUTICAL_POINTS_HEADER
				|| this == NAUTICAL_MAPS_GROUP
				|| this == WIKIVOYAGE_HEADER || this == TRAVEL_GROUP
				|| this == EXTRA_MAPS;
	}

	public static String getVoiceTTSId() {
		return "#" + OTHER_GROUP.name().toLowerCase() + "#" + VOICE_TTS.name().toLowerCase();
	}
}
