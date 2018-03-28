package net.osmand.plus.wikivoyage.data;

public class SearchResult {

	String searchTerm;
	long cityId;
	String articleTitle;
	String lang;

	public String getSearchTerm() {
		return searchTerm;
	}

	public long getCityId() {
		return cityId;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public String getLang() {
		return lang;
	}
}
