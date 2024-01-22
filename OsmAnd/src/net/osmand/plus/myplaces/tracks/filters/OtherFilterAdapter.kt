package net.osmand.plus.myplaces.tracks.filters

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx

class OtherFilterAdapter(
	val app: OsmandApplication,
	var nightMode: Boolean,
	val filterChangedListener: FilterChangedListener?) :
	RecyclerView.Adapter<OtherFilterAdapter.OtherViewHolder>() {

	var items = ArrayList<OtherTrackParam>()
	lateinit var fragmentManager: FragmentManager
	lateinit var filter: OtherTrackFilter

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtherViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)
		val view = inflater.inflate(R.layout.filter_other_item, parent, false)
		return OtherViewHolder(app, view, nightMode)
	}

	override fun onBindViewHolder(holder: OtherViewHolder, position: Int) {
		val param = items[position]
		holder.itemView.setOnClickListener {
			filter.setItemSelected(param, !filter.isParamSelected(param))
			notifyItemChanged(position)
			filterChangedListener?.onFilterChanged()
		}
		holder.switch.isChecked = filter.isParamSelected(param)
		holder.title.setText(param.displayName)
	}

	override fun getItemCount(): Int {
		return items.size
	}

	class OtherViewHolder(app: OsmandApplication, view: View, val nightMode: Boolean) :
		RecyclerView.ViewHolder(view) {
		var switch: AppCompatCheckBox
		var title: TextViewEx

		init {
			switch = view.findViewById(R.id.check)
			title = view.findViewById(R.id.param_title)
			UiUtilities.setupCompoundButton(
				nightMode,
				net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
				switch)
		}
	}
}