package net.osmand.plus.wikivoyage.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.text.Html;
import android.text.TextUtils;

import net.osmand.plus.GPXUtilities.GPXFile;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class WikivoyageArticle {

	private static final String IMAGE_ROOT_URL = "https://upload.wikimedia.org/wikipedia/commons/";
	private static final String THUMB_PREFIX = "320px-";
	private static final String REGULAR_PREFIX = "800px-";

	private static final int PARTIAL_CONTENT_PHRASES = 3;

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
	String contentsJson;
	String aggregatedPartOf;

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

	public String getContentsJson() {
		return contentsJson;
	}

	public String getAggregatedPartOf() {
		return aggregatedPartOf;
	}

	@Nullable
	public String getPartialContent() {
		if (content == null) {
			return null;
		}

		int firstParagraphStart = content.indexOf("<p>");
		int firstParagraphEnd = content.indexOf("</p>");
		if (firstParagraphStart == -1 || firstParagraphEnd == -1) {
			return null;
		}

		// 4 is the length of </p> tag
		String firstParagraphHtml = content.substring(firstParagraphStart, firstParagraphEnd + 4);
		String firstParagraphText = Html.fromHtml(firstParagraphHtml).toString().trim();
		String[] phrases = firstParagraphText.split("\\. ");

		StringBuilder res = new StringBuilder();
		int limit = Math.min(phrases.length, PARTIAL_CONTENT_PHRASES);
		for (int i = 0; i < limit; i++) {
			res.append(phrases[i]);
			if (i < limit - 1) {
				res.append(". ");
			}
		}

		return res.toString();
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
		String[] hash = getHash(imageTitle);
		String prefix = thumbnail ? THUMB_PREFIX : REGULAR_PREFIX;
		return IMAGE_ROOT_URL + "thumb/" + hash[0] + "/" + hash[1] + "/" + imageTitle + "/" + prefix + imageTitle;
	}

	@Size(2)
	@NonNull
	private static String[] getHash(@NonNull String s) {
		String md5 = new String(Hex.encodeHex(DigestUtils.md5(s.replace(" ", "_"))));
		return new String[]{md5.substring(0, 1), md5.substring(0, 2)};
	}
}
