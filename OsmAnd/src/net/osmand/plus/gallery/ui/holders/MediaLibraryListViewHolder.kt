package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.data.MediaPosterLoader
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import kotlin.math.roundToInt

class MediaLibraryListViewHolder(
	app: OsmandApplication,
	itemView: View,
	mediaProvider: MediaProvider,
	onMediaItemClicked: (MediaItem) -> Unit,
	onMediaItemLongClicked: (MediaItem) -> Unit,
	onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader?,
	private val onMenuClicked: (MediaItem, View) -> Unit
) : GalleryMediaListViewHolder(app, itemView, mediaProvider, onMediaItemClicked, onMediaItemLongClicked, onToggleSelection, posterLoader) {

	private val rowContainer: View = itemView.findViewById(R.id.row_container)
	private val overflowButton: ImageView = itemView.findViewById(R.id.overflow_button)
	private val attachmentContainer: View = itemView.findViewById(R.id.attachment_container)
	private val attachmentIcon: ImageView = itemView.findViewById(R.id.attachment_icon)
	private val attachmentName: TextView = itemView.findViewById(R.id.attachment_name)
	private val attachmentCount: TextView = itemView.findViewById(R.id.attachment_count)

	override fun getFadeableContentViews(): List<MorphContent> =
		super.getFadeableContentViews() + listOf(overflowButton, attachmentContainer).filter { it.isVisible }.map { MorphContent(it, slides = true) }

	override fun onItemBound(galleryItem: GalleryItem.Media) {
		val mediaItem = galleryItem.mediaItem
		overflowButton.setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_overflow_menu_white, ColorUtilities.getDefaultIconColor(app, nightMode)))
		overflowButton.setOnClickListener { onMenuClicked(mediaItem, it) }
		val line = galleryItem.presentation?.attachment
		attachmentContainer.isVisible = line != null
		rowContainer.minimumHeight = AndroidUtils.dpToPxF(app, if (line == null) ROW_MIN_HEIGHT_DP else ATTACHED_ROW_MIN_HEIGHT_DP).roundToInt()
		attachmentIcon.setImageDrawable(line?.iconDrawable)
		attachmentName.text = line?.name
		attachmentName.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		attachmentCount.isVisible = line != null && line.extraCount > 0
		attachmentCount.text = line?.let { " | " + app.getString(R.string.media_attached_more_count, it.extraCount) }
		attachmentCount.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
	}

	override fun onSelectionModeBound(selectionMode: Boolean) {
		overflowButton.visibility = if (selectionMode) View.INVISIBLE else View.VISIBLE
	}

	companion object {
		private const val ROW_MIN_HEIGHT_DP = 68f
		private const val ATTACHED_ROW_MIN_HEIGHT_DP = 100f
	}
}
