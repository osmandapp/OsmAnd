package net.osmand.plus.gallery

import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.builders.cards.UrlImageCard
import net.osmand.plus.plugins.mapillary.MapillaryContributeCard
import net.osmand.plus.plugins.mapillary.MapillaryImageCard
import net.osmand.plus.wikipedia.WikiImageCard
import net.osmand.shared.media.domain.MediaContent
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaType

/**
 * TODO: remove
 * Temporary mapper to bridge legacy ImageCard objects to the new MediaItem domain model.
 * Handles specific legacy overrides (like UrlImageCard's null thumbnails).
 */
object LegacyMediaConverter {

	/**
	 * Iterates through the legacy list and maps items to strictly typed GalleryItems.
	 */
	fun convertList(legacyItems: List<Any>): List<GalleryItem> {
		return legacyItems.mapNotNull { item -> convertItem(item, legacyItems) }
	}

	fun convertItem(item: Any, legacyItems: List<Any>): GalleryItem? {
		return when (item) {
			is MapillaryContributeCard -> GalleryItem.MapillaryContribute

			is WikiImageCard ->
				GalleryItem.Media(MediaItem.Wiki(item.wikiImage), item.isImageDownloadFailed)

			is MapillaryImageCard ->
				GalleryItem.Media(convertMapillary(item), item.isImageDownloadFailed)

			is UrlImageCard ->
				GalleryItem.Media(convertUrlImage(item), item.isImageDownloadFailed, showProgress = true)

			is ImageCard ->
				GalleryItem.Media(convertGenericImage(item), item.isImageDownloadFailed)

			is net.osmand.plus.mapcontextmenu.builders.cards.ProgressCard -> GalleryItem.Progress

			is net.osmand.plus.mapcontextmenu.builders.cards.NoImagesCard -> GalleryItem.NoImages

			// Handle legacy integer types (5 = NO_INTERNET, 6 = IMAGES_COUNT)
			is Int -> {
				when (item) {
					5 -> GalleryItem.NoInternet(isLoading = false)
					6 -> GalleryItem.ImagesCount(count = legacyItems.size - 1)
					else -> null
				}
			}

			else -> null
		}
	}

	private fun convertMapillary(card: MapillaryImageCard): MediaItem.Mapillary {
		return MediaItem.Mapillary(
			key = card.key ?: "",
			latitude = card.location?.latitude,
			longitude = card.location?.longitude,
			cameraAngle = card.ca,
			sourceUrl = card.imageUrl ?: "",
			title = card.title ?: "",
			previewContent = MediaContent(
				standardUrl = card.imageUrl ?: "",
				thumbnailUrl = card.thumbnailUrl ?: card.imageUrl ?: "",
				hiResUrl = card.galleryFullSizeUrl ?: card.imageUrl ?: ""
			),
			webpageUrl = card.url
		)
	}

	private fun convertUrlImage(card: UrlImageCard): MediaItem.Remote {
		return MediaItem.Remote(
			// Uses getSuitableUrl() per UrlImageCard logic
			sourceUrl = card.suitableUrl ?: card.imageUrl ?: "",
			title = card.title ?: "",
			type = MediaType.PHOTO,
			previewContent = MediaContent(
				standardUrl = card.imageUrl ?: "",
				// UrlImageCard explicitly returns null for getThumbnailUrl(),
				// so it safely falls back to standard imageUrl here
				thumbnailUrl = card.thumbnailUrl ?: card.imageUrl ?: "",
				// UrlImageCard overrides getGalleryFullSizeUrl() to return getImageUrl()
				hiResUrl = card.galleryFullSizeUrl ?: card.imageUrl ?: ""
			),
			origin = MediaOrigin.UNKNOWN // TODO: check is this OSM related image by parsing json
		)
	}

	private fun convertGenericImage(card: ImageCard): MediaItem.Remote {
		return MediaItem.Remote(
			sourceUrl = card.imageUrl ?: "",
			title = card.title ?: "",
			type = MediaType.PHOTO,
			previewContent = MediaContent(
				standardUrl = card.imageUrl ?: "",
				thumbnailUrl = card.thumbnailUrl ?: card.imageUrl ?: "",
				hiResUrl = card.galleryFullSizeUrl ?: card.imageHiresUrl ?: card.imageUrl ?: ""
			),
			origin = MediaOrigin.UNKNOWN,
			webpageUrl = card.url
		)
	}
}