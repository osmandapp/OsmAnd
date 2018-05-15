package net.osmand.plus.wikivoyage.data;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult {

	private static final int SHOW_LANGS = 3;

	long tripId;
	List<String> articleTitles = new ArrayList<>();
	List<String> langs = new ArrayList<>();
	List<String> isPartOf = new ArrayList<>();
	String imageTitle;

	public long getTripId() {
		return tripId;
	}

	public List<String> getArticleTitles() {
		return articleTitles;
	}

	public List<String> getLangs() {
		return langs;
	}

	public List<String> getIsPartOf() {
		return isPartOf;
	}

	public String getImageTitle() {
		return imageTitle;
	}

	public String getFirstLangsString() {
		StringBuilder res = new StringBuilder();
		int limit = Math.min(SHOW_LANGS, langs.size());
		for (int i = 0; i < limit; i++) {
			res.append(Algorithms.capitalizeFirstLetter(langs.get(i)));
			if (i != limit - 1) {
				res.append(", ");
			}
		}
		return res.toString();
	}
}
