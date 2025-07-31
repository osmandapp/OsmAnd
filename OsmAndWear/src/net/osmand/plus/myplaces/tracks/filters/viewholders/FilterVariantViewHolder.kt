package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx

class FilterVariantViewHolder(var view: View, val nightMode: Boolean) :
	RecyclerView.ViewHolder(view) {
	var title: TextViewEx
	var count: TextViewEx
	var checkBox: AppCompatCheckBox
	var icon: AppCompatImageView
	var divider: View

	init {
		title = view.findViewById(R.id.title)
		count = view.findViewById(R.id.count)
		checkBox = view.findViewById(R.id.compound_button)
		icon = view.findViewById(R.id.icon)
		divider = view.findViewById(R.id.divider)
		var app = view.context.applicationContext as OsmandApplication
		UiUtilities.setupCompoundButton(
			nightMode,
			ColorUtilities.getActiveColor(app, nightMode),
			checkBox)
	}
}