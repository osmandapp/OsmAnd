package net.osmand.plus.wikivoyage.data;

public class WikivoyageSearchHistoryItem {

	String articleTitle;
	String lang;
	String isPartOf;
	long lastAccessed;
	
	
	public String getKey() {
		return TravelLocalDataHelper.getHistoryKey(lang, articleTitle);
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
