package net.osmand.shared.wiki

class WikiImage(
    val wikiMediaTag: String,
    val imageName: String,
    val imageStubUrl: String,
    val imageHiResUrl: String,
    val imageIconUrl: String,
) {
    companion object {
        private const val WIKIMEDIA_COMMONS_URL = "https://commons.wikimedia.org/wiki/"
        private const val WIKIMEDIA_FILE = "File:"
    }

    val metadata = WikiMetadata.Metadata()

    private var mediaId: Long = -1

    fun setMediaId(mediaId: Long) {
        this.mediaId = mediaId
    }

    fun getMediaId(): Long {
        return mediaId
    }

    fun getUrlWithCommonAttributions(): String {
        return "$WIKIMEDIA_COMMONS_URL$WIKIMEDIA_FILE$wikiMediaTag"
    }
}