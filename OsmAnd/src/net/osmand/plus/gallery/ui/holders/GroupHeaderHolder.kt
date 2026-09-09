package net.osmand.plus.gallery.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.media.domain.MediaType

class GroupHeaderHolder(itemView: View, private val app: OsmandApplication, private val onClick: (MediaType) -> Unit) : RecyclerView.ViewHolder(itemView) {
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
		itemView.findViewById<ImageView>(R.id.arrow).apply {
			setImageDrawable(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_arrow_down, ColorUtilities.getDefaultIconColor(app, nightMode)))
			rotation = if (item.collapsed) -180f else 0f
		}
		itemView.setOnClickListener { onClick(item.type) }
	}
}
