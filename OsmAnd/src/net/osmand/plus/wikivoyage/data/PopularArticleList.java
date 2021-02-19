package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;

import java.util.ArrayList;
import java.util.List;

public class PopularArticleList {

	public static final int POPULAR_ARTICLES_COUNT_PER_PAGE = 30;

	private final List<TravelArticle> articles;
	private int pageCount;

	public PopularArticleList() {
		this.articles = new ArrayList<>();
		pageCount = 0;
	}

	public PopularArticleList(@NonNull PopularArticleList articles) {
		this.articles = articles.articles;
		this.pageCount = articles.pageCount;
	}

	public void clear() {
		articles.clear();
		pageCount = 0;
	}

	@NonNull
	public List<TravelArticle> getArticles() {
		return articles;
	}

	public void add(@NonNull TravelArticle article) {
		articles.add(article);
	}

	public boolean contains(@NonNull TravelArticle article) {
		return articles.contains(article);
	}

	public boolean isFullPage() {
		return articles.size() >= pageCount * POPULAR_ARTICLES_COUNT_PER_PAGE;
	}

	public void nextPage() {
		pageCount++;
	}

	public boolean containsAmenity(@NonNull Amenity amenity) {
		for (TravelArticle article : articles) {
			if (article.getRouteId().equals(amenity.getAdditionalInfo(Amenity.ROUTE_ID))) {
				return true;
			}
		}
		return false;
	}
}