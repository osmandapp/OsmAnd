package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.domain.MediaType

class GroupHeaderHolder(itemView: View, private val app: OsmandApplication, private val onClick: (MediaType) -> Unit) : RecyclerView.ViewHolder(itemView) {
	private val arrow: ImageView = itemView.findViewById(R.id.arrow)
	private var boundType: MediaType? = null
	private var boundCollapsed = false

	fun bind(item: GalleryItem.GroupHeader, nightMode: Boolean) {
		itemView.findViewById<TextView>(R.id.title).apply {
			setText(when (item.type) {
				MediaType.PHOTO -> R.string.shared_string_photo
				MediaType.VIDEO -> R.string.shared_string_video
				MediaType.AUDIO -> R.string.shared_string_audio
				else -> R.string.shared_string_media
			})
			setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
		}
		arrow.setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_arrow_down, ColorUtilities.getDefaultIconColor(app, nightMode)))
		val rotation = if (item.collapsed) -180f else 0f
		arrow.animate().cancel()
		if (boundType == item.type && boundCollapsed != item.collapsed && GalleryMotion.animationsEnabled(app)) {
			arrow.animate().rotation(rotation).setDuration(GalleryMotion.MOVE_DURATION_MS).setInterpolator(GalleryMotion.CURVE).start()
		} else {
			arrow.rotation = rotation
		}
		boundType = item.type
		boundCollapsed = item.collapsed
		itemView.setOnClickListener { onClick(item.type) }
	}
}
