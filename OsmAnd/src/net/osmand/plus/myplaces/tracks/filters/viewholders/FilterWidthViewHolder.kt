package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.WidthTrackFilter
import net.osmand.plus.routing.cards.RouteLineWidthCard
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.util.Algorithms

class FilterWidthViewHolder(itemView: View, nightMode: Boolean) :
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
	private var filter: WidthTrackFilter? = null

	init {
		app = itemView.context.applicationContext as OsmandApplication
		this.nightMode = nightMode
		title = itemView.findViewById(R.id.title)
		selectedValue = itemView.findViewById(R.id.selected_value)
		divider = itemView.findViewById(R.id.divider)
		explicitIndicator = itemView.findViewById(R.id.explicit_indicator)
		titleContainer = itemView.findViewById(R.id.title_container)
		titleContainer.setOnClickListener { _: View? ->
			expanded = !expanded
			updateExpandState()
		}
		recycler = itemView.findViewById(R.id.variants)
	}

	fun bindView(filter: WidthTrackFilter) {
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
			val adapter = WidthAdapter()
			adapter.items.clear()
			adapter.items.addAll(it.allWidth)
			adapter.items.remove("")
			adapter.items.add(0, "")
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			updateSelectedValue(it)
		}
	}

	private fun updateSelectedValue(it: WidthTrackFilter): Boolean {
		selectedValue.text = "${it.selectedWidths.size}"
		return AndroidUiHelper.updateVisibility(selectedValue, it.selectedWidths.size > 0)
	}

	inner class WidthAdapter : RecyclerView.Adapter<FilterVariantViewHolder>() {
		var items = ArrayList<String>()
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterVariantViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)
			val view =
				inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
			return FilterVariantViewHolder(view, nightMode)
		}

		override fun getItemCount(): Int {
			return items.size
		}

		override fun onBindViewHolder(holder: FilterVariantViewHolder, position: Int) {
			val widthName = items[position]
			if (Algorithms.isEmpty(widthName)) {
				holder.title.text = app.getString(R.string.not_specified)
				holder.icon.setImageDrawable(app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled))
			} else {
				var iconColor = R.color.track_filter_width_standard
				holder.title.text = when (widthName) {
					RouteLineWidthCard.WidthMode.THICK.widthKey -> app.getString(R.string.rendering_value_bold_name)
					RouteLineWidthCard.WidthMode.THIN.widthKey -> app.getString(R.string.rendering_value_thin_name)
					RouteLineWidthCard.WidthMode.MEDIUM.widthKey -> app.getString(R.string.rendering_value_medium_name)
					else -> {
						iconColor = R.color.track_filter_width_custom
						"${app.getString(R.string.shared_string_custom)}: $widthName"
					}
				}
				val appearanceDrawable =
					TrackAppearanceFragment.getTrackIcon(
						app,
						widthName,
						false,
						app.getColor(iconColor))
				val marginTrackIconH =
					app.resources.getDimensionPixelSize(R.dimen.standard_icon_size)
				UiUtilities.setMargins(holder.icon, marginTrackIconH, 0, marginTrackIconH, 0)
				holder.icon.setImageDrawable(appearanceDrawable)
			}
			AndroidUiHelper.updateVisibility(holder.icon, true)
			AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
			filter?.let { widthFilter ->
				holder.itemView.setOnClickListener {
					widthFilter.setWidthSelected(widthName, !widthFilter.isWidthSelected(widthName))
					this.notifyItemChanged(position)
					updateSelectedValue(widthFilter)
				}
				holder.checkBox.isChecked = widthFilter.isWidthSelected(widthName)
				holder.count.text = widthFilter.allWidthCollection[widthName].toString()
			}
		}
	}
}