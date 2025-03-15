package net.osmand.shared.wiki

object WikiHelper {
    data class WikiTagData(
        val wikiImages: List<WikiImage>,
        val wikidataId: String?,
        val wikiCategory: String?,
        val wikiTitle: String?
    )

    private const val WIKIPEDIA_PREFIX = ".wikipedia.org/wiki/"
    private const val WIKIPEDIA_TAG_PREFIX = "wikipedia:"
    private const val WIKIMEDIA_FILE_PREFIX = "File:"
    private const val WIKIMEDIA_CATEGORY_PREFIX = "Category:"
    private const val WIKIMEDIA_FILE_URL = "https://commons.wikimedia.org/wiki/Special:FilePath/"
    private const val IMAGE_THUMB_SIZE = 480
    private const val IMAGE_ICON_SIZE = 64

    fun extractWikiTagData(tags: Map<String, String>?): WikiTagData {
        val wikiImages = mutableListOf<WikiImage>()
        val wikidataId = tags?.get("wikidata") ?: ""
        val wikiTitle = extractWikiTitle(tags)
        val wikiCategory = extractWikiCategory(tags)

        tags?.get("wikimedia_commons")?.let {
            if (it.startsWith(WIKIMEDIA_FILE_PREFIX)) {
                addFile(wikiImages, it.removePrefix(WIKIMEDIA_FILE_PREFIX))
            }
        }

        return WikiTagData(wikiImages, wikidataId, wikiCategory, wikiTitle)
    }

    private fun extractWikiTitle(tags: Map<String, String>?): String? {
        val directTitle = tags?.get("wikipedia")?.let { parseWikipediaUrl(it) }
        if (!directTitle.isNullOrEmpty()) return directTitle

        return tags?.entries?.firstOrNull { it.key.startsWith(WIKIPEDIA_TAG_PREFIX) }
            ?.let { it.key.removePrefix(WIKIPEDIA_TAG_PREFIX) + ":" + it.value }
    }

    private fun extractWikiCategory(tags: Map<String, String>?): String? {
        return tags?.get("wikimedia_commons")
            ?.takeIf { it.startsWith(WIKIMEDIA_CATEGORY_PREFIX) }
            ?.removePrefix(WIKIMEDIA_CATEGORY_PREFIX)
    }

    private fun parseWikipediaUrl(url: String): String {
        val urlIndex = url.indexOf(WIKIPEDIA_PREFIX)
        return if (urlIndex > 0) {
            val lang = url.substringBefore(WIKIPEDIA_PREFIX).substringAfterLast("/")
            val title = url.substring(urlIndex + WIKIPEDIA_PREFIX.length)
            "$lang:$title"
        } else url
    }

    fun addFile(wikiImages: MutableList<WikiImage>, imageFileName: String) {
        getImageData(imageFileName).let { wikiImages.add(it) }
    }

    fun getImageData(imageFileName: String): WikiImage {
        return WikiImage(
            wikiMediaTag = imageFileName,
            imageName = imageFileName,
            imageStubUrl = "$WIKIMEDIA_FILE_URL$imageFileName?width=$IMAGE_THUMB_SIZE",
            imageHiResUrl = "$WIKIMEDIA_FILE_URL$imageFileName",
            imageIconUrl = "$WIKIMEDIA_FILE_URL$imageFileName?width=$IMAGE_ICON_SIZE"
        )
    }

    fun getImageData(
        imageFileName: String,
        baseUrl: String,
        thumbSize: Int,
        iconSize: Int
    ): WikiImage {
        return WikiImage(
            wikiMediaTag = imageFileName,
            imageName = imageFileName,
            imageStubUrl = "$baseUrl$imageFileName?width=$thumbSize",
            imageHiResUrl = "$baseUrl$imageFileName",
            imageIconUrl = "$baseUrl$imageFileName?width=$iconSize"
        )
    }

}