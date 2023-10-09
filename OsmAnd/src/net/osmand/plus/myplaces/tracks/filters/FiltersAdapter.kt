package net.osmand.plus.myplaces.tracks.filters

import android.app.Activity
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.myplaces.tracks.TracksSearchFilter
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterCityViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterDateViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterDurationViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterNameViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterNameViewHolder.TextChangedListener
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterOtherViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterRangeViewHolder
import net.osmand.plus.utils.UiUtilities

class FiltersAdapter(
	private val activity: Activity,
	private val filter: TracksSearchFilter,
	private val nightMode: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

	private var items = filter.currentFilters

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)

		return when (FilterType.values()[viewType]) {
			FilterType.NAME -> {
				val view = inflater.inflate(R.layout.filter_name_item, parent, false)
				FilterNameViewHolder(view, nightMode)
			}

			FilterType.LENGTH,
			FilterType.TIME_IN_MOTION,
			FilterType.AVERAGE_SPEED,
			FilterType.MAX_SPEED,
			FilterType.UPHILL,
			FilterType.DOWNHILL,
			FilterType.MAX_ALTITUDE,
			FilterType.AVERAGE_ALTITUDE -> {
				val view = inflater.inflate(R.layout.filter_range_item, parent, false)
				FilterRangeViewHolder(view, nightMode)
			}

			FilterType.DURATION -> {
				val view = inflater.inflate(R.layout.filter_range_item, parent, false)
				FilterDurationViewHolder(view, nightMode)
			}

			FilterType.DATE_CREATION -> {
				val view = inflater.inflate(R.layout.filter_date_item, parent, false)
				FilterDateViewHolder(view, nightMode)
			}

			FilterType.CITY -> {
				val view = inflater.inflate(R.layout.filter_city_item, parent, false)
				FilterCityViewHolder(view, nightMode)
			}

			FilterType.OTHER -> {
				val view = inflater.inflate(R.layout.filter_other_item, parent, false)
				FilterOtherViewHolder(view, nightMode)
			}
		}
	}

	override fun getItemViewType(position: Int): Int {
		val filter = items[position]
		return filter.filterType.ordinal
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		if (holder is FilterNameViewHolder) {
			holder.bindView((item as TrackNameFilter).value,
				object : TextChangedListener {
					override fun onTextChanged(newText: String) {
						item.value = newText
					}
				})
		} else if (holder is FilterDurationViewHolder) {
			holder.bindView(item as DurationTrackFilter)
		} else if (holder is FilterRangeViewHolder) {
			if (item.filterType == FilterType.TIME_IN_MOTION) {
				holder.bindView(item as TimeInMotionTrackFilter)
			} else if (item.filterType == FilterType.LENGTH) {
				holder.bindView(item as LengthTrackFilter)
			} else if (item.filterType == FilterType.AVERAGE_SPEED) {
				holder.bindView(item as AverageSpeedTrackFilter)
			} else if (item.filterType == FilterType.MAX_SPEED) {
				holder.bindView(item as MaxSpeedTrackFilter)
			} else if (item.filterType == FilterType.AVERAGE_ALTITUDE) {
				holder.bindView(item as AverageAltitudeTrackFilter)
			} else if (item.filterType == FilterType.MAX_ALTITUDE) {
				holder.bindView(item as MaxAltitudeTrackFilter)
			} else if (item.filterType == FilterType.UPHILL) {
				holder.bindView(item as UphillTrackFilter)
			} else if (item.filterType == FilterType.DOWNHILL) {
				holder.bindView(item as DownhillTrackFilter)
			}
		} else if (holder is FilterDateViewHolder) {
			holder.bindView(item as DateCreationTrackFilter, activity)
		} else if (holder is FilterCityViewHolder) {
			holder.bindView(item as CityTrackFilter)
		} else if (holder is FilterOtherViewHolder) {
			holder.bindView(item as OtherTrackFilter)
		}
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun getFilter(): Filter {
		return filter
	}

	private fun updateItem(item: Any) {
		val index = items.indexOf(item)
		if (index != -1) {
			notifyItemChanged(index)
		}
	}

	fun onItemsSelected(items: Set<Any>) {
		for (item in items) {
			updateItem(item)
		}
	}

	fun updateItems() {
		items = filter.currentFilters
	}
}