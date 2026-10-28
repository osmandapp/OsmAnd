package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.widgets.TextViewEx

class ShowAllViewHolder(view: View) : RecyclerView.ViewHolder(view) {
	var title: TextViewEx
	init {
		title = view.findViewById(R.id.title)
	}
}