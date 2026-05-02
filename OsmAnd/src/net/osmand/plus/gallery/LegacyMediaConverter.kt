package net.osmand.plus.gallery

import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard
import net.osmand.plus.mapcontextmenu.builders.cards.UrlImageCard
import net.osmand.plus.plugins.mapillary.MapillaryContributeCard
import net.osmand.plus.plugins.mapillary.MapillaryImageCard
import net.osmand.plus.wikipedia.WikiImageCard
import net.osmand.shared.media.domain.MediaDetails
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaResource
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.domain.RemoteMetadata

/**
 * TODO: remove
 * Temporary mapper to bridge legacy ImageCard objects to the new MediaItem domain model.
 */
object LegacyMediaConverter {

	fun convertImageCards(legacyItems: List<ImageCard>): List<GalleryItem> {
		return legacyItems.map { item -> convertImageCard(item) }
	}

	private fun convertImageCard(item: ImageCard): GalleryItem {
		return when (item) {
			is MapillaryContributeCard -> GalleryItem.MapillaryContribute

			is WikiImageCard -> GalleryItem.Media(
				convertWikiImage(item),
				item.isImageDownloadFailed
			)

			is MapillaryImageCard -> GalleryItem.Media(
				convertMapillary(item),
				item.isImageDownloadFailed
			)

			is UrlImageCard -> GalleryItem.Media(
				convertUrlImage(item),
				item.isImageDownloadFailed,
				showProgress = true
			)

			else -> GalleryItem.Media(
				convertGenericImage(item),
				item.isImageDownloadFailed
			)
		}
	}

	private fun convertWikiImage(card: WikiImageCard): MediaItem.Remote {
		val wikiImage = card.wikiImage
		val metadata = wikiImage.metadata

		return MediaItem.Remote(
			sourceUrl = wikiImage.imageStubUrl,
			title = wikiImage.imageName,
			type = MediaType.PHOTO,
			origin = MediaOrigin.WIKIPEDIA,
			resource = MediaResource(
				thumbnailUri = card.thumbnailUrl,
				previewUri = card.imageUrl,
				fullUri = card.imageHiresUrl
			),
			details = MediaDetails(
				description = metadata.getDescription(null) ?: "",
				author = metadata.author ?: "",
				date = metadata.date ?: "",
				license = metadata.license ?: "",
				viewUrl = wikiImage.getUrlWithCommonAttributions()
			)
		)
	}

	private fun convertMapillary(card: MapillaryImageCard): MediaItem.Remote {
		return createRemoteImageItem(
			card = card,
			sourceUrl = card.imageUrl ?: "",
			thumbnailUri = card.thumbnailUrl ?: card.imageUrl ?: "",
			previewUri = card.galleryFullSizeUrl ?: card.imageUrl ?: "",
			fullUri = card.imageHiresUrl ?: card.galleryFullSizeUrl ?: card.imageUrl ?: "",
			origin = MediaOrigin.MAPILLARY
		)
	}

	private fun convertUrlImage(card: UrlImageCard): MediaItem.Remote {
		val sourceUrl = card.imageUrl ?: ""
		val viewUrl = card.suitableUrl ?: card.imageUrl ?: ""

		return createRemoteImageItem(
			card = card,
			sourceUrl = sourceUrl,
			thumbnailUri = card.thumbnailUrl ?: sourceUrl,
			previewUri = card.galleryFullSizeUrl ?: sourceUrl,
			fullUri = card.imageHiresUrl ?: card.galleryFullSizeUrl ?: sourceUrl,
			viewUrl = viewUrl,
			origin = MediaOrigin.UNKNOWN // TODO: check if this is OSM-related image by parsing json
		)
	}

	private fun convertGenericImage(card: ImageCard): MediaItem.Remote {
		return createRemoteImageItem(
			card = card,
			sourceUrl = card.imageUrl ?: "",
			thumbnailUri = card.thumbnailUrl ?: card.imageUrl ?: "",
			previewUri = card.galleryFullSizeUrl ?: card.imageUrl ?: "",
			fullUri = card.imageHiresUrl ?: card.galleryFullSizeUrl ?: card.imageUrl ?: "",
			origin = MediaOrigin.UNKNOWN
		)
	}

	private fun createRemoteImageItem(
		card: ImageCard,
		sourceUrl: String,
		thumbnailUri: String,
		previewUri: String,
		fullUri: String,
		viewUrl: String = card.url ?: "",
		origin: MediaOrigin
	): MediaItem.Remote {
		return MediaItem.Remote(
			sourceUrl = sourceUrl,
			title = card.title ?: "",
			type = MediaType.PHOTO,
			origin = origin,
			resource = MediaResource(
				thumbnailUri = thumbnailUri,
				previewUri = previewUri,
				fullUri = fullUri
			),
			details = createMediaDetails(viewUrl),
			metadata = createRemoteMetadata(card)
		)
	}

	private fun createMediaDetails(viewUrl: String): MediaDetails {
		return MediaDetails(
			viewUrl = viewUrl
		)
	}

	private fun createRemoteMetadata(card: ImageCard): RemoteMetadata {
		return RemoteMetadata(
			key = card.key ?: "",
			latitude = card.location?.latitude,
			longitude = card.location?.longitude,
			cameraAngle = card.ca,
			timestamp = card.timestamp?.time,
			userName = card.userName ?: "",
			externalLink = card.isExternalLink
		)
	}
}