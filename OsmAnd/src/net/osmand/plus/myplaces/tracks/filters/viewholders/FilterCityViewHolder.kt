package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx

class FilterCityViewHolder(itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val app: OsmandApplication
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: CityTrackFilter? = null

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		divider = itemView.findViewById(R.id.divider)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener { v: View? ->
			expanded = !expanded
			updateExpandState()
		}
		recycler = itemView.findViewById(R.id.cities)
	}

	fun bindView(filter: CityTrackFilter) {
		this.filter = filter
		title.setText(filter.displayNameId)
		updateExpandState()
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(recycler, expanded)
	}

	private fun updateValues() {
		filter?.let {
			val adapter = CityAdapter()
			adapter.items.clear()
			adapter.items.addAll(it.fullCitiesList)
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			selectedValue.text = "${it.selectedCities.size}"
			AndroidUiHelper.updateVisibility(selectedValue, it.selectedCities.size > 0)
		}
	}

	inner class CityAdapter : RecyclerView.Adapter<CityViewHolder>() {
		var items = ArrayList<String>()
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)
			val view =
				inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
			return CityViewHolder(view)
		}

		override fun getItemCount(): Int {
			return items.size
		}

		override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
			val cityName = items[position]
			holder.title.text = cityName
			AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
			filter?.let { cityFilter ->
				holder.itemView.setOnClickListener {
					cityFilter.setCitySelected(cityName, !cityFilter.isCitySelected(cityName))
					this.notifyItemChanged(position)
					updateValues()
				}
				holder.checkBox.isChecked = cityFilter.isCitySelected(cityName)
			}
		}
	}

	inner class CityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var title: TextViewEx
		var checkBox: AppCompatCheckBox
		var divider: View

		init {
			title = view.findViewById(R.id.title)
			checkBox = view.findViewById(R.id.compound_button)
			divider = view.findViewById(R.id.divider)
			UiUtilities.setupCompoundButton(
				nightMode,
				net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
				checkBox)
		}
	}
}