package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.io.File;

public class WikivoyageSearchHistoryItem {

	File articleFile;
	String articleTitle;
	String lang;
	String isPartOf;
	long lastAccessed;

	public static String getKey(String lang, String title, @Nullable File file) {
		return lang + ":" + title + (file != null ? ":" + file.getName() : "");
	}

	public String getKey() {
		return getKey(lang, articleTitle, articleFile);
	}

	public File getArticleFile() {
		return articleFile;
	}

	@Nullable
	public String getTravelBook(@NonNull OsmandApplication app) {
		return articleFile != null ? TravelArticle.getTravelBook(app, articleFile) : null;
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
