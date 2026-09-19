package net.osmand.shared.travel

import net.osmand.shared.util.KAlgorithms

/**
 * One page of the articles offered on the travel screen before anything is searched, filled a page
 * at a time as the search widens.
 *
 * A copy of `PopularArticles` in the android app, which stays there; this copy is for iOS. Note
 * that the copy constructor shares the list with the original rather than copying it, as java
 * does: that is how a page carries over what the previous one collected.
 */
class PopularArticles {

	private var articles: MutableList<TravelArticle>

	constructor() {
		articles = ArrayList()
	}

	constructor(other: PopularArticles) {
		articles = other.articles
	}

	fun clear() {
		articles = ArrayList()
	}

	fun getArticles(): List<TravelArticle> = ArrayList(articles)

	/** @return false once the page is full. */
	fun add(article: TravelArticle): Boolean {
		articles.add(article)
		return articles.size % ARTICLES_PER_PAGE != 0
	}

	fun contains(article: TravelArticle): Boolean = articles.contains(article)

	fun containsByRouteId(routeId: String): Boolean {
		for (article in articles) {
			if (article.getRouteId() == routeId) {
				return true
			}
		}
		return false
	}

	fun isEmpty(): Boolean = KAlgorithms.isEmpty(articles)

	companion object {
		const val ARTICLES_PER_PAGE: Int = 30
	}
}
