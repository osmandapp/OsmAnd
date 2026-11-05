package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class PopularArticles {

	public static final int ARTICLES_PER_PAGE = 30;

	private List<TravelArticle> articles;

	public PopularArticles() {
		this.articles = new ArrayList<>();
	}

	public PopularArticles(@NonNull PopularArticles articles) {
		this.articles = articles.articles;
	}

	public void clear() {
		articles = new ArrayList<>();
	}

	@NonNull
	public List<TravelArticle> getArticles() {
		return new ArrayList<>(articles);
	}

	public boolean add(@NonNull TravelArticle article) {
		articles.add(article);
		return articles.size() % ARTICLES_PER_PAGE != 0;
	}

	public boolean contains(@NonNull TravelArticle article) {
		return articles.contains(article);
	}

	public boolean containsByRouteId(@NonNull String routeId) {
		for (TravelArticle article : articles) {
			if (article.getRouteId().equals(routeId)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return Algorithms.isEmpty(articles);
	}
}