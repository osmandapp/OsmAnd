package net.osmand.plus.gallery.ui.viewer

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.plugins.audionotes.library.data.MediaAttachment
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry
import net.osmand.plus.settings.coordinates.CoordinateFormatFormatter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.wikipedia.WikiAlgorithms
import net.osmand.shared.media.MediaUriResolver
import net.osmand.shared.util.Localization
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.media.library.MediaFormatting
import net.osmand.util.Algorithms

sealed class DetailsItem {
	abstract val card: Int
	abstract val key: String

	data class Header(override val card: Int, val title: String) : DetailsItem() {
		override val key get() = "header:$card"
	}

	data class Text(override val card: Int, val text: String) : DetailsItem() {
		override val key get() = "text:$card"
	}

	data class Row(
		override val card: Int,
		val label: String,
		val value: String,
		val iconId: Int = 0,
		val tintIcon: Boolean = true,
		val action: RowAction? = null,
		val middleEllipsis: Boolean = false
	) : DetailsItem() {
		override val key get() = "row:$card:$label"
	}

	data class Attachment(override val card: Int, val attachment: MediaAttachment) : DetailsItem() {
		override val key get() = "attachment:${attachment.kind}:${attachment.name}:${attachment.latLon}:${attachment.link.href}"
	}

	data class EmptyAttachments(override val card: Int) : DetailsItem() {
		override val key get() = "empty:$card"
	}
}

sealed class RowAction {
	data class ShowOnMap(val lat: Double, val lon: Double, val title: String) : RowAction()
	data class OpenUrl(val url: String) : RowAction()
}

class MediaDetailsContentBuilder(private val app: OsmandApplication) {

	fun build(item: MediaItem, metadata: GalleryMediaMetadata?, entry: MediaLibraryEntry?): List<DetailsItem> {
		val items = mutableListOf<DetailsItem>()
		items += DetailsItem.Header(FILE_CARD, app.getString(R.string.shared_string_file))
		val details = item.details
		details?.getDescription(app.language)?.takeIf { it.isNotBlank() }?.let { items += DetailsItem.Text(FILE_CARD, it) }
		items += row(R.string.shared_string_name, item.title, middleEllipsis = true)
		metadata?.format?.let { items += row(R.string.shared_string_format, it) }
		(metadata?.creationTimeMs ?: metadata?.lastModifiedTimeMs)?.let {
			items += row(R.string.shared_string_created, OsmAndFormatter.getFormattedDateTime(app, it))
		}
		metadata?.sizeBytes?.takeIf { it > 0 }?.let { items += row(R.string.shared_string_size, AndroidUtils.formatSize(app, it)) }
		if (item.type == MediaType.PHOTO || item.type == MediaType.VIDEO) {
			MediaFormatting.resolution(metadata?.width, metadata?.height)?.let { items += row(R.string.shared_string_resolution, it) }
		}
		if (item.type == MediaType.VIDEO || item.type == MediaType.AUDIO) {
			metadata?.durationMs?.let { items += row(R.string.duration, MediaFormatting.duration(it)) }
		}
		val lat = entry?.lat ?: metadata?.latLon?.latitude
		val lon = entry?.lon ?: metadata?.latLon?.longitude
		if (lat != null && lon != null) {
			items += row(R.string.shared_string_location, CoordinateFormatFormatter.formatPrimary(app, lat, lon),
				iconId = R.drawable.ic_action_location_marker_outlined, action = RowAction.ShowOnMap(lat, lon, item.title))
		}
		details?.author?.takeIf { it.isNotBlank() }?.let { items += row(R.string.shared_string_author, it) }
		details?.date?.let { date ->
			val formatted = WikiAlgorithms.formatWikiDate(date).takeUnless { Algorithms.isEmpty(it) } ?: date
			if (formatted.isNotBlank()) items += row(R.string.shared_string_added, formatted)
		}
		val origin = item.origin
		val source = origin.titleKey?.let { Localization.getString(it) }
		val sourceIcon = AndroidUtils.getDrawableId(app, origin.iconName)
		if (!source.isNullOrEmpty() || sourceIcon != 0) {
			items += row(R.string.shared_string_source, source.orEmpty(), iconId = sourceIcon, tintIcon = false)
		}
		details?.license?.takeIf { it.isNotBlank() }?.let { items += row(R.string.shared_string_license, it) }
		if (item is MediaItem.Remote) {
			MediaUriResolver.getDetailsLink(item)?.takeIf { it.isNotBlank() }?.let {
				items += row(R.string.shared_string_link, it, action = RowAction.OpenUrl(it))
			}
		} else {
			items += DetailsItem.Header(ATTACHED_CARD, app.getString(R.string.shared_string_attached_to))
			val attachments = entry?.attachments.orEmpty()
			if (attachments.isEmpty()) {
				items += DetailsItem.EmptyAttachments(ATTACHED_CARD)
			} else {
				attachments.forEach { items += DetailsItem.Attachment(ATTACHED_CARD, it) }
			}
		}
		return items
	}

	private fun row(labelId: Int, value: String, iconId: Int = 0, tintIcon: Boolean = true, action: RowAction? = null,
		middleEllipsis: Boolean = false) = DetailsItem.Row(FILE_CARD, app.getString(labelId), value, iconId, tintIcon, action, middleEllipsis)

	companion object {
		const val FILE_CARD = 0
		const val ATTACHED_CARD = 1
	}
}
