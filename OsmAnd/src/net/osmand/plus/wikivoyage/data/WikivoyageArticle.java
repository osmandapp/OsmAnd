package net.osmand.plus.wikivoyage.data;

import android.support.annotation.NonNull;
import android.support.annotation.Size;

import net.osmand.plus.GPXUtilities.GPXFile;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class WikivoyageArticle {

	private static final String IMAGE_ROOT_URL = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String THUMB_PREFIX = "320px-";

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

	@NonNull
	public static String getThumbImageUrl(@NonNull String imageTitle) {
		String[] hash = getHash(imageTitle);
		return IMAGE_ROOT_URL + "thumb/" + hash[0] + "/" + hash[1] + "/" + imageTitle + "/" + THUMB_PREFIX + imageTitle;
	}

	@NonNull
	public static String getImageUrl(@NonNull String imageTitle) {
		String[] hash = getHash(imageTitle);
		return IMAGE_ROOT_URL + hash[0] + "/" + hash[1] + "/" + imageTitle;
	}

	@Size(2)
	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s.replace(" ", "_"))));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}
}
