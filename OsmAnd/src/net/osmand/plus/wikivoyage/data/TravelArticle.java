package net.osmand.plus.wikivoyage.data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.text.TextUtils;
import net.osmand.GPXUtilities.GPXFile;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class TravelArticle {

	private static final String IMAGE_ROOT_URL = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String THUMB_PREFIX = "320px-";
	private static final String REGULAR_PREFIX = "1280px-";//1280, 1024, 800

	String title;
	String content;
	String isPartOf;
	double lat;
	double lon;
	String imageTitle;
	GPXFile gpxFile;
	long tripId;
	long originalId;
	String lang;
	String contentsJson;
	String aggregatedPartOf;


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

	public long getTripId() {
		return tripId;
	}

	public long getOriginalId() {
		return originalId;
	}

	public String getLang() {
		return lang;
	}

	public String getContentsJson() {
		return contentsJson;
	}

	public String getAggregatedPartOf() {
		return aggregatedPartOf;
	}

	@Nullable
	public String getGeoDescription() {
		if (TextUtils.isEmpty(aggregatedPartOf)) {
			return null;
		}

		String[] parts = aggregatedPartOf.split(",");
		if (parts.length > 0) {
			StringBuilder res = new StringBuilder();
			res.append(parts[parts.length - 1]);
			if (parts.length > 1) {
				res.append(" \u2022 ").append(parts[0]);
			}
			return res.toString();
		}

		return null;
	}

	@NonNull
	public static String getImageUrl(@NonNull String imageTitle, boolean thumbnail) {
		imageTitle = imageTitle.replace(" ", "_");
		try {
			imageTitle = URLDecoder.decode(imageTitle, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		String[] hash = getHash(imageTitle);
		try {
			imageTitle = URLEncoder.encode(imageTitle, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		String prefix = thumbnail ? THUMB_PREFIX : REGULAR_PREFIX;
		return IMAGE_ROOT_URL + "thumb/" + hash[0] + "/" + hash[1] + "/" + imageTitle + "/" + prefix + imageTitle;
	}

	@Size(2)
	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s)));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}
}
