package net.osmand.plus.wikivoyage.data;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult {

	List<String> searchTerm = new ArrayList<>();
	long cityId;
	List<String> articleTitle = new ArrayList<>();
	List<String> langs = new ArrayList<>();

	public List<String> getSearchTerm() {
		return searchTerm;
	}

	public long getCityId() {
		return cityId;
	}

	public List<String> getArticleTitle() {
		return articleTitle;
	}

	public List<String> getLang() {
		return langs;
	}
}
