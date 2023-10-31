package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.CollatorStringMatcher
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.dialogs.FilterAllVariantsListFragment
import net.osmand.plus.myplaces.tracks.filters.CityTrackFilter
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.TrackFilterPropertiesAdapter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms

class FilterCityViewHolder(var app: OsmandApplication, itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: CityTrackFilter? = null

	private val filterPropertiesClosed = object : DialogClosedListener {
		override fun onDialogClosed() {
			updateValues()
		}
	}
	private val adapter = CityAdapter(app, nightMode, null, filterPropertiesClosed)

	init {
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
		recycler = itemView.findViewById(R.id.variants)
	}

	fun bindView(filter: CityTrackFilter, fragmentManager: FragmentManager) {
		this.filter = filter
		adapter.filter = filter
		adapter.fragmentManager = fragmentManager
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
			adapter.items = ArrayList(it.allCities)
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			selectedValue.text = "${it.selectedCities.size}"
			AndroidUiHelper.updateVisibility(selectedValue, it.selectedCities.size > 0)
		}
	}

	class CityAdapter(
		val app: OsmandApplication,
		var nightMode: Boolean,
		val filterChangedListener: FilterChangedListener?,
		private val filterPropertiesClosed: DialogClosedListener?) :
		RecyclerView.Adapter<RecyclerView.ViewHolder>(),
		TrackFilterPropertiesAdapter {
		companion object {
			private val MIN_VISIBLE_COUNT = 5
			private val ITEM_TYPE = 0
			private val SHOW_ALL_ITEM_TYPE = 1
		}

		var items = ArrayList<String>()
		var isExpanded = false
		var additionalItems = ArrayList<String>()
		lateinit var fragmentManager: FragmentManager
		lateinit var filter: CityTrackFilter
		private val newSelectedItemsListener =
			object : FilterAllVariantsListFragment.NewSelectedItemsListener {
				override fun setSelectedItemsDiff(
					allSelectedItems: List<String>,
					selectedItems: List<String>) {
					filterCollection("")
					filter.setSelectedCities(allSelectedItems)
					setNewSelectedItems(selectedItems)
				}
			}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)
			return when (viewType) {
				ITEM_TYPE -> {
					val view =
						inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
					FilterVariantViewHolder(view, nightMode)
				}

				else -> {
					val view = inflater.inflate(
						R.layout.show_all_filter_items_item,
						parent,
						false)
					val topBottomPadding =
						app.resources.getDimensionPixelSize(R.dimen.content_padding_small)
					val leftRightPadding =
						app.resources.getDimensionPixelSize(R.dimen.content_padding)
					view.setPadding(
						leftRightPadding,
						topBottomPadding,
						leftRightPadding,
						topBottomPadding)
					ShowAllViewHolder(view)
				}
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			when (holder) {
				is ShowAllViewHolder -> {
					holder.title.setText(R.string.shared_string_show_all)
					holder.itemView.setOnClickListener {
						val cityFilter = CityTrackFilter(null)
						cityFilter.initWithValue(filter)
						cityFilter.setFullCitiesCollection(filter.allCitiesCollection)
						val cityAdapter =
							CityAdapter(app, nightMode, null, null)
						cityAdapter.filter = cityFilter
						cityAdapter.isExpanded = true
						cityAdapter.items = ArrayList(filter.allCities)
						FilterAllVariantsListFragment.showInstance(
							app,
							fragmentManager,
							cityFilter,
							filterPropertiesClosed,
							cityAdapter,
							newSelectedItemsListener)
					}
				}

				is FilterVariantViewHolder -> {
					val cityName = getItem(position)
					holder.title.text = cityName
					AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
					holder.itemView.setOnClickListener {
						filter.setCitySelected(cityName, !filter.isCitySelected(cityName))
						this.notifyItemChanged(position)
					}
					holder.checkBox.isChecked = filter.isCitySelected(cityName)
					holder.count.text = filter.allCitiesCollection[cityName].toString()
				}
			}
		}

		override fun getItemCount(): Int {
			return if (isExpanded) {
				items.size
			} else if (items.size > MIN_VISIBLE_COUNT) {
				MIN_VISIBLE_COUNT + additionalItems.size + if (MIN_VISIBLE_COUNT + additionalItems.size == items.size) 0 else 1
			} else {
				items.size
			}
		}

		override fun getItemViewType(position: Int): Int {
			return if (isExpanded) {
				ITEM_TYPE
			} else if (position == itemCount - 1 && items.size > MIN_VISIBLE_COUNT + additionalItems.size) {
				SHOW_ALL_ITEM_TYPE
			} else {
				ITEM_TYPE
			}
		}

		private fun getItem(position: Int): String {
			return if (isExpanded) {
				items[position]
			} else if (position < MIN_VISIBLE_COUNT) {
				items[position]
			} else {
				additionalItems[position - MIN_VISIBLE_COUNT]
			}
		}

		override fun setNewSelectedItems(newSelectedItems: List<String>) {
			var additionalItems = ArrayList<String>()
			for (selectedItem in newSelectedItems) {
				if (items.indexOf(selectedItem) >= MIN_VISIBLE_COUNT) {
					additionalItems.add(selectedItem)
				}
			}
			this.additionalItems = additionalItems
			notifyDataSetChanged()
		}

		override fun filterCollection(query: String) {
			val filteredItems = ArrayList<String>()
			if (Algorithms.isEmpty(query)) {
				filteredItems.addAll(filter.allCities)
			} else {
				var matcher = SearchPhrase.NameStringMatcher(
					query.trim { it <= ' ' },
					CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
				filter.let {
					for (city in it.allCities) {
						if (matcher.matches(city)) {
							filteredItems.add(city)
						}
					}
				}
			}
			items = filteredItems
			notifyDataSetChanged()
		}
	}
}