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
	 * Iterates through the list provided to the adapter and replaces valid ImageCards
	 * with MediaItems, while keeping UI-specific cards untouched.
	 */
	fun convertList(legacyItems: List<Any>): List<Any> {
		return legacyItems.map { item ->
			when (item) {
				// IMPORTANT: MapillaryContributeCard is a UI action button, not a media item.
				// We must bypass it so the adapter can render it as MAPILLARY_CONTRIBUTE_TYPE.
				is MapillaryContributeCard -> item

				// Convert valid media cards
				is WikiImageCard -> MediaItem.Wiki(item.wikiImage)
				is MapillaryImageCard -> convertMapillary(item)
				is UrlImageCard -> convertUrlImage(item)

				// Fallback for base ImageCard instances
				is ImageCard -> convertGenericImage(item)

				// Pass through other UI states (ProgressCard, Integers, NoImagesCard, etc.)
				else -> item
			}
		}
	}

	private fun convertMapillary(card: MapillaryImageCard): MediaItem.Remote {
		return MediaItem.Remote(
			sourceUrl = card.imageUrl ?: "",
			title = card.title ?: "",
			type = MediaType.PHOTO, // Legacy items are always photos
			previewContent = MediaContent(
				standardUrl = card.imageUrl ?: "",
				thumbnailUrl = card.thumbnailUrl ?: card.imageUrl ?: "",
				hiResUrl = card.galleryFullSizeUrl ?: card.imageUrl ?: ""
			),
			origin = MediaOrigin.MAPILLARY
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
			sourceUrl = card.url ?: card.imageUrl ?: "",
			title = card.title ?: "",
			type = MediaType.PHOTO,
			previewContent = MediaContent(
				standardUrl = card.imageUrl ?: "",
				thumbnailUrl = card.thumbnailUrl ?: card.imageUrl ?: "",
				hiResUrl = card.galleryFullSizeUrl ?: card.imageHiresUrl ?: card.imageUrl ?: ""
			),
			origin = MediaOrigin.UNKNOWN
		)
	}
}