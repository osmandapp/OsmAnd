package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
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
	private val app: OsmandApplication,
	itemView: View,
	mediaProvider: MediaProvider,
	onClick: (MediaItem) -> Unit,
	onLongClick: (MediaItem) -> Unit,
	onToggleSelection: (MediaItem) -> Unit,
	posterLoader: MediaPosterLoader,
	private val onMenuClick: (MediaItem, View) -> Unit
) : GalleryMediaListViewHolder(app, itemView, mediaProvider, onClick, onLongClick, onToggleSelection, posterLoader) {
	private var nightMode = false
	override fun bindView(mapActivity: FragmentActivity, galleryItem: GalleryItem.Media, nightMode: Boolean,
		selectionMode: Boolean, selected: Boolean, showDivider: Boolean) {
		this.nightMode = nightMode
		super.bindView(mapActivity, galleryItem, nightMode, selectionMode, selected, showDivider)
		bindAttachment(galleryItem)
		itemView.findViewById<ImageView>(R.id.overflow_button).apply {
			visibility = if (selectionMode) View.INVISIBLE else View.VISIBLE
			setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_overflow_menu_white, ColorUtilities.getDefaultIconColor(app, nightMode)))
			setOnClickListener { onMenuClick(galleryItem.mediaItem, it) }
		}
	}

	override fun updateSelection(selectionMode: Boolean, selected: Boolean, nightMode: Boolean) {
		super.updateSelection(selectionMode, selected, nightMode)
		itemView.findViewById<View>(R.id.overflow_button).visibility = if (selectionMode) View.INVISIBLE else View.VISIBLE
	}

	override fun getFadeableContentViews(): List<View> = super.getFadeableContentViews() +
		listOf(itemView.findViewById<View>(R.id.overflow_button), itemView.findViewById(R.id.attachment_container)).filter { it.isVisible }

	override fun updateMetadata(galleryItem: GalleryItem.Media) {
		super.updateMetadata(galleryItem)
		bindAttachment(galleryItem)
	}

	private fun bindAttachment(item: GalleryItem.Media) {
		val line = item.presentation?.attachment
		itemView.findViewById<View>(R.id.attachment_container).isVisible = line != null
		itemView.findViewById<View>(R.id.row_container).minimumHeight =
			AndroidUtils.dpToPxF(app, if (line == null) 68f else 100f).roundToInt()
		itemView.findViewById<ImageView>(R.id.attachment_icon).setImageDrawable(line?.iconDrawable)
		itemView.findViewById<TextView>(R.id.attachment_name).apply {
			text = line?.name
			setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		}
		itemView.findViewById<TextView>(R.id.attachment_count).apply {
			isVisible = line != null && line.extraCount > 0
			text = line?.let { app.getString(R.string.ltr_or_rtl_combine_via_pipe_plus, "", it.extraCount) }
			setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		}
	}
}
