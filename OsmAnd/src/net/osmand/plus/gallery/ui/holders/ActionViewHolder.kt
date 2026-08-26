package net.osmand.plus.gallery.ui.holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class ActionViewHolder(
	itemView: View,
	private val onActionClicked: (View, GalleryAction) -> Unit
) : RecyclerView.ViewHolder(itemView) {

	fun bindView(
		nightMode: Boolean,
		mapActivity: MapActivity,
		item: GalleryItem.Action
	) {
		itemView.findViewById<View>(R.id.card_background).visibility = View.GONE
		AndroidUtils.setBackgroundColor(
			mapActivity,
			itemView,
			ColorUtilities.getActivityBgColorId(nightMode)
		)
		AndroidUtils.setTextPrimaryColor(
			mapActivity,
			itemView.findViewById(R.id.title),
			nightMode
		)
		itemView.findViewById<View>(R.id.button).setOnClickListener {
			onActionClicked(it, item.action)
		}
	}
}