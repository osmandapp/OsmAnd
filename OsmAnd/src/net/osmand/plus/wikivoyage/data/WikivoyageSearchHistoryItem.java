package net.osmand.plus.wikivoyage.data;

public class WikivoyageSearchHistoryItem {

	long cityId;
	String articleTitle;
	String lang;
	String isPartOf;
	long lastAccessed;

	public long getCityId() {
		return cityId;
	}

	public String getArticleTitle() {
		return articleTitle;
	}

	public String getLang() {
		return lang;
	}

	public String getIsPartOf() {
		return isPartOf;
	}

	public long getLastAccessed() {
		return lastAccessed;
	}
}
