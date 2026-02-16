package net.osmand.plus.plugins.astro

import net.osmand.util.Algorithms

data class AstroArticle(
    val wikidata: String,
    val lang: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val summaryJson: String?,
    private val mobileHtml: ByteArray?
) {
    fun getMobileHtmlString(): String? {
        return mobileHtml?.let {
            try {
                Algorithms.gzipToString(mobileHtml)
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AstroArticle

        if (wikidata != other.wikidata) return false
        if (lang != other.lang) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (thumbnailUrl != other.thumbnailUrl) return false
        if (summaryJson != other.summaryJson) return false
        if (mobileHtml != null) {
            if (other.mobileHtml == null) return false
            if (mobileHtml.size != other.mobileHtml.size) return false
        } else if (other.mobileHtml != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = wikidata.hashCode()
        result = 31 * result + lang.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + (summaryJson?.hashCode() ?: 0)
        result = 31 * result + (mobileHtml?.contentHashCode() ?: 0)
        return result
    }
}
