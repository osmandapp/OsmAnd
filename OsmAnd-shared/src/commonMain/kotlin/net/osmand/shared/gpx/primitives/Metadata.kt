package net.osmand.shared.gpx.primitives

import net.osmand.shared.util.KAlgorithms

class Metadata : GpxExtensions {
	var name: String? = null
	var desc: String? = null
	var link: String? = null
	var keywords: String? = null
	var time: Long = 0
	var author: Author? = null
	var copyright: Copyright? = null
	var bounds: Bounds? = null

	constructor()

	constructor(source: Metadata) {
		name = source.name
		desc = source.desc
		link = source.link
		keywords = source.keywords
		time = source.time
		val sourceAuthor = source.author
		if (sourceAuthor != null) {
			author = Author(sourceAuthor)
		}
		val sourceCopyright = source.copyright
		if (sourceCopyright != null) {
			copyright = Copyright(sourceCopyright)
		}
		val sourceBounds = source.bounds
		if (sourceBounds != null) {
			bounds = Bounds(sourceBounds)
		}
		copyExtensions(source)
	}

	fun getArticleTitle(): String? {
		return getExtensionsToRead()["article_title"]
	}

	fun getArticleLang(): String? {
		return getExtensionsToRead()["article_lang"]
	}

	fun getDescription(): String? {
		return desc
	}

	fun readDescription() {
		val readDescription = getExtensionsToWrite().remove("desc")
		if (!KAlgorithms.isEmpty(readDescription)) {
			desc = if (KAlgorithms.isEmpty(desc)) {
				readDescription
			} else {
				"$desc; $readDescription"
			}
		}
	}

	fun getRouteActivity(activities: List<RouteActivity>): RouteActivity? {
		return findRouteActivity(getKeywordAt(0), activities)
	}

	private fun findRouteActivity(id: String?, activities: List<RouteActivity>): RouteActivity? {
		return id?.let { activities.firstOrNull { it.id == id } }
	}

	fun setRouteActivity(activity: RouteActivity?, activities: List<RouteActivity>) {
		if (this.keywords != null) {
			val keywords = this.keywords!!.split(",")
			val previousActivity = findRouteActivity(keywords[0], activities)
			val startIndex = if (previousActivity != null) 1 else 0
			val keywordsBuilder = StringBuilder(activity?.id ?: "")
			for (i in startIndex until keywords.size) {
				if (keywordsBuilder.isNotEmpty()) {
					keywordsBuilder.append(",")
				}
				keywordsBuilder.append(keywords[i])
			}
			this.keywords = keywordsBuilder.toString()
		} else {
			this.keywords = activity?.id
		}
	}

	fun getFilteredKeywords(activities: List<RouteActivity>): String? {
		val keywords = getIndividualKeywords()
		if (keywords.isNotEmpty()) {
			val activity = findRouteActivity(keywords[0], activities)
			val startIndex = if (activity != null) 1 else 0
			val keywordsBuilder = StringBuilder()
			for (i in startIndex until keywords.size) {
				if (keywordsBuilder.isNotEmpty()) {
					keywordsBuilder.append(",")
				}
				keywordsBuilder.append(keywords[i])
			}
			return keywordsBuilder.toString()
		}
		return null
	}

	fun getKeywordAt(index: Int): String? {
		val keywords = getIndividualKeywords()
		return if (keywords.isNotEmpty() && keywords.size > index) keywords[index] else null
	}

	fun getIndividualKeywords(): List<String> {
		return if (keywords != null) keywords!!.split(",") else emptyList()
	}
}