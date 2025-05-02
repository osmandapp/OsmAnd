package net.osmand.shared.wiki

import okio.ByteString.Companion.encodeUtf8

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
    private const val OSMAND_IMAGES_BASE_URL = "https://data.osmand.net/wikimedia/images-1280"
    private const val IMAGE_FULL_SIZE = 1280
    private const val IMAGE_THUMB_SIZE = 480
    private const val IMAGE_ICON_SIZE = 160

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
        val hash = getWikiHash(imageFileName);
        val h1 = hash.first;
        val h2 = hash.second;
        return WikiImage(
            wikiMediaTag = imageFileName,
            imageName = imageFileName,
            imageHiResUrl = "$OSMAND_IMAGES_BASE_URL/$h1/$h2/$imageFileName?width=$IMAGE_FULL_SIZE",
            imageStubUrl = "$OSMAND_IMAGES_BASE_URL/$h1/$h2/$imageFileName?width=$IMAGE_THUMB_SIZE",
            imageIconUrl = "$OSMAND_IMAGES_BASE_URL/$h1/$h2/$imageFileName?width=$IMAGE_ICON_SIZE"
        )
    }

    private fun getWikiHash(imageFileName: String): Pair<String, String> {
        val md5 = imageFileName.encodeUtf8().md5().hex();
        return Pair(md5.substring(0, 1), md5.substring(0, 2))
    }
}
