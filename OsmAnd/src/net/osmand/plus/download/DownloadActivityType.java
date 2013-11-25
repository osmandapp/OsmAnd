package net.osmand.plus.download;

public enum DownloadActivityType {
	NORMAL_FILE, ROADS_FILE, /*SRTM_FILE, */HILLSHADE_FILE, SRTM_COUNTRY_FILE;

	public static boolean isCountedInDownloads(IndexItem es) {
		DownloadActivityType tp = es.getType();
		if(tp == HILLSHADE_FILE || tp == SRTM_COUNTRY_FILE || es.isVoiceItem()){
			return false;
		}
		return true;
	}
	
	public static boolean isCountedInDownloads(DownloadActivityType tp) {
		if(tp == HILLSHADE_FILE || tp == SRTM_COUNTRY_FILE){
			return false;
		}
		return true;
	}
	
	
}