package net.osmand.plus.myplaces.tracks.dialogs.viewholders

import android.view.View
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder
import net.osmand.plus.myplaces.tracks.EmptySmartFolderListener

class EmptySmartFolderViewHolder(view: View, private var listener: EmptySmartFolderListener) :
	EmptyTracksViewHolder(view, null) {
	override fun bindView() {
		super.bindView()
		title.setText(R.string.empty_smart_folder_title)
		description.setText(R.string.empty_smart_folder_description)
		button.setTitleId(R.string.edit_filter)
		button.setOnClickListener {
			listener.editFilters()
		}
	}
}