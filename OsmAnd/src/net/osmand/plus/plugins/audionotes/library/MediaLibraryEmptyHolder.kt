package net.osmand.plus.plugins.audionotes.library

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.utils.ColorUtilities

class MediaLibraryEmptyHolder(itemView: View, private val app: OsmandApplication) : RecyclerView.ViewHolder(itemView) {
	fun bind(item: GalleryItem.NoMedia, nightMode: Boolean) {
		itemView.findViewById<ImageView>(R.id.icon).setImageDrawable(
			app.uiUtilities.getPaintedIcon(item.iconResId, ColorUtilities.getDefaultIconColor(app, nightMode)))
		itemView.findViewById<TextView>(R.id.title).setText(item.titleResId)
		itemView.findViewById<TextView>(R.id.description).setText(item.descriptionResId)
		itemView.findViewById<View>(R.id.action_button).visibility = View.GONE
	}
}
