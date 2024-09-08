package net.osmand.plus.myplaces.tracks.filters

import android.app.Activity
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.myplaces.tracks.TracksSearchFilter
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterDateViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterNameViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterNameViewHolder.TextChangedListener
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterOtherViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterRangeViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.ListFilterViewHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.gpx.filters.DateTrackFilter
import net.osmand.shared.gpx.filters.FilterType
import net.osmand.shared.gpx.filters.ListTrackFilter
import net.osmand.shared.gpx.filters.OtherTrackFilter
import net.osmand.shared.gpx.filters.RangeTrackFilter
import net.osmand.shared.gpx.filters.TextTrackFilter

class FiltersAdapter(
	private val app: OsmandApplication,
	private val activity: Activity,
	private val fragmentManager: FragmentManager,
	private val filter: TracksSearchFilter,
	private val nightMode: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

	private var items = filter.currentFilters

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = UiUtilities.getInflater(parent.context, nightMode)

		return when (FilterType.entries[viewType]) {
			FilterType.TEXT -> {
				val view = inflater.inflate(R.layout.filter_name_item, parent, false)
				FilterNameViewHolder(view, nightMode)
			}

			FilterType.RANGE -> {
				val view = inflater.inflate(R.layout.filter_range_item, parent, false)
				FilterRangeViewHolder(view, nightMode)
			}

			FilterType.DATE_RANGE -> {
				val view = inflater.inflate(R.layout.filter_date_item, parent, false)
				FilterDateViewHolder(view, nightMode)
			}

			FilterType.SINGLE_FIELD_LIST -> {
				val view = inflater.inflate(R.layout.filter_list_item, parent, false)
				ListFilterViewHolder(
					app,
					view,
					nightMode)
			}

			FilterType.OTHER -> {
				val view = inflater.inflate(R.layout.filter_list_item, parent, false)
				FilterOtherViewHolder(view, nightMode)
			}
		}
	}

	override fun getItemViewType(position: Int): Int {
		val filter = items[position]
		return filter.trackFilterType.filterType.ordinal
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		if (holder is FilterNameViewHolder) {
			holder.bindView((item as TextTrackFilter).value,
				object : TextChangedListener {
					override fun onTextChanged(newText: String) {
						item.value = newText
					}
				})
		} else if (holder is FilterRangeViewHolder) {
			holder.bindView(item as RangeTrackFilter<*>)
		} else if (holder is FilterDateViewHolder) {
			holder.bindView(item as DateTrackFilter, activity)
		} else if (holder is ListFilterViewHolder) {
			holder.bindView(item as ListTrackFilter, fragmentManager)
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

	fun onTracksFilteringComplete() {
		for (i in 0 until items.size) {
			if (items[i].trackFilterType.updateOnOtherFiltersChangeNeeded) {
				notifyItemChanged(i)
			}
		}
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