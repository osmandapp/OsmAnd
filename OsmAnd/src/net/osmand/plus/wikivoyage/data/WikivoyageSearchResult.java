package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageSearchResult {

	private static final int SHOW_LANGS = 3;

	TravelArticleIdentifier articleId;

	String imageTitle;
	String isPartOf;

	List<String> langs = new ArrayList<>();

	public WikivoyageSearchResult(@NonNull TravelArticle article, @Nullable List<String> langs) {
		articleId = article.generateIdentifier();
		imageTitle = article.imageTitle;
		isPartOf = article.isPartOf;
		if (langs != null) {
			this.langs = langs;
		}
	}

	public WikivoyageSearchResult(String routeId, String articleTitle, String isPartOf, String imageTitle, @Nullable List<String> langs) {
		TravelArticle article = new TravelArticle();
		article.routeId = routeId;
		article.title = articleTitle;
		this.articleId = article.generateIdentifier();
		this.imageTitle = imageTitle;
		this.isPartOf = isPartOf;
		if (langs != null) {
			this.langs = langs;
		}
	}

	public TravelArticleIdentifier getArticleId() {
		return articleId;
	}

	public String getArticleTitle() {
		return articleId.title;
	}

	public String getArticleRouteId() {
		return articleId.routeId;
	}

	public List<String> getLangs() {
		return langs;
	}

	public String getIsPartOf() {
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
