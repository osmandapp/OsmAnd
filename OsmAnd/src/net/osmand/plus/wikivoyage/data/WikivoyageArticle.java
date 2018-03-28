package net.osmand.plus.wikivoyage.data;

import net.osmand.plus.GPXUtilities.GPXFile;

public class WikivoyageArticle {

	String id;
	String title;
	String content;
	String isPartOf;
	double lat;
	double lon;
	String imageTitle;
	GPXFile gpxFile;
	long cityId;
	long originalId;
	String lang;

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public String getIsPartOf() {
		return isPartOf;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public String getImageTitle() {
		return imageTitle;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public long getCityId() {
		return cityId;
	}

	public long getOriginalId() {
		return originalId;
	}

	public String getLang() {
		return lang;
	}
}
