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
}