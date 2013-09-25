package net.osmand.plus.download;

public enum DownloadActivityType {
	NORMAL_FILE, ROADS_FILE, SRTM_FILE, HILLSHADE_FILE, SRTM_COUNTRY_FILE;

	public static boolean isCountedInDownloads(DownloadActivityType tp) {
		return tp != SRTM_FILE && tp != HILLSHADE_FILE && tp != SRTM_COUNTRY_FILE;
	}
}