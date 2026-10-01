package net.osmand.shared.travel

import net.osmand.shared.io.KFile

/**
 * The articles the user has saved, which [TravelObfHelper] falls back to when the obf file an
 * article came from is no longer there.
 *
 * `TravelLocalDataHelper` in the android app keeps these in its own sqlite database, together with
 * the reading history and the listeners the travel screens need; that stays platform side. This is
 * only the part the helper asks for.
 */
interface TravelBookmarks {

	/** Re-read whatever is cached, before a screen is filled. */
	fun refreshCachedData()

	fun getSavedArticle(file: KFile?, routeId: String?, lang: String?): TravelArticle?

	fun getSavedArticles(file: KFile?, routeId: String?): List<TravelArticle>

	fun addArticleToSaved(article: TravelArticle)

	fun removeArticleFromSaved(article: TravelArticle)

	companion object {

		/** For a platform with no bookmark storage: nothing is ever saved. */
		val NONE: TravelBookmarks = object : TravelBookmarks {
			override fun refreshCachedData() {}

			override fun getSavedArticle(file: KFile?, routeId: String?, lang: String?): TravelArticle? = null

			override fun getSavedArticles(file: KFile?, routeId: String?): List<TravelArticle> = emptyList()

			override fun addArticleToSaved(article: TravelArticle) {}

			override fun removeArticleFromSaved(article: TravelArticle) {}
		}
	}
}
