package net.osmand.shared.travel

import net.osmand.shared.util.KAlgorithms

/**
 * One row of the travel search: enough to draw it and to fetch the article behind it.
 *
 * A copy of `WikivoyageSearchResult` in the android app, which stays there; this copy is for iOS.
 */
class WikivoyageSearchResult {

	val articleId: TravelArticleIdentifier

	val imageTitle: String?

	val isPartOf: String?

	var langs: List<String> = ArrayList()
		private set

	constructor(article: TravelArticle, langs: List<String>?) {
		articleId = article.generateIdentifier()
		imageTitle = article.imageTitle
		isPartOf = article.isPartOf
		if (langs != null) {
			this.langs = langs
		}
	}

	constructor(
		routeId: String?, articleTitle: String?, isPartOf: String?, imageTitle: String?,
		langs: List<String>?
	) {
		val article = TravelArticle()
		article.routeId = routeId
		article.title = articleTitle
		this.articleId = article.generateIdentifier()
		this.imageTitle = imageTitle
		this.isPartOf = isPartOf
		if (langs != null) {
			this.langs = langs
		}
	}

	fun getArticleTitle(): String? = articleId.title

	fun getArticleRouteId(): String? = articleId.routeId

	fun getIsPartOf(): String? = isPartOf

	/** The first few languages the article is written in, for the subtitle of the row. */
	fun getFirstLangsString(): String {
		val res = StringBuilder()
		val limit = minOf(SHOW_LANGS, langs.size)
		for (i in 0 until limit) {
			res.append(KAlgorithms.capitalizeFirstLetter(langs[i]))
			if (i != limit - 1) {
				res.append(", ")
			}
		}
		return res.toString()
	}

	companion object {
		private const val SHOW_LANGS = 3
	}
}
