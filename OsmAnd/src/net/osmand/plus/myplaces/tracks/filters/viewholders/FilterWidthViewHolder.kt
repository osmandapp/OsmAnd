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
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.TrackFilterPropertiesAdapter
import net.osmand.plus.myplaces.tracks.filters.WidthTrackFilter
import net.osmand.plus.routing.cards.RouteLineWidthCard
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms

class FilterWidthViewHolder(var app: OsmandApplication, itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: WidthTrackFilter? = null

	private val filterPropertiesClosed = object : DialogClosedListener {
		override fun onDialogClosed() {
			updateValues()
		}
	}
	private val adapter =
		WidthAdapter(app, nightMode, null, filterPropertiesClosed)


	init {
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

	fun bindView(filter: WidthTrackFilter, fragmentManager: FragmentManager) {
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
			val items = ArrayList(it.allWidth)
			items.remove("")
			items.add(0, "")
			adapter.items = items
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

	class WidthAdapter(
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
		lateinit var filter: WidthTrackFilter
		private val newSelectedItemsListener =
			object : FilterAllVariantsListFragment.NewSelectedItemsListener {
				override fun setSelectedItemsDiff(
					allSelectedItems: List<String>,
					selectedItems: List<String>) {
					filterCollection("")
					filter.setSelectedWidths(allSelectedItems)
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
			} else if (position == itemCount - 1 &&
				items.size > MIN_VISIBLE_COUNT + additionalItems.size) {
				SHOW_ALL_ITEM_TYPE
			} else {
				ITEM_TYPE
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			when (holder) {
				is ShowAllViewHolder -> {
					holder.title.setText(R.string.shared_string_show_all)
					holder.itemView.setOnClickListener {
						val widthFilter = WidthTrackFilter(null)
						widthFilter.initWithValue(filter)
						widthFilter.setFullWidthCollection(filter.allWidthCollection)
						val widthAdapter =
							WidthAdapter(app, nightMode, null, null)
						widthAdapter.filter = widthFilter
						widthAdapter.isExpanded = true
						widthAdapter.items = ArrayList(filter.allWidth)
						FilterAllVariantsListFragment.showInstance(
							app,
							fragmentManager,
							widthFilter,
							filterPropertiesClosed,
							widthAdapter,
							newSelectedItemsListener)
					}
				}

				is FilterVariantViewHolder -> {
					val widthName = getItem(position)
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
						holder.icon.setImageDrawable(appearanceDrawable)
					}
					AndroidUiHelper.updateVisibility(holder.icon, true)
					AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
					holder.itemView.setOnClickListener {
						filter.setWidthSelected(widthName, !filter.isWidthSelected(widthName))
						this.notifyItemChanged(position)
					}
					holder.checkBox.isChecked = filter.isWidthSelected(widthName)
					holder.count.text = filter.getTracksCountForWidth(widthName).toString()

				}
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
				filteredItems.addAll(filter.allWidth)
			} else {
				var matcher = SearchPhrase.NameStringMatcher(
					query.trim { it <= ' ' },
					CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
				filter.let {
					for (city in it.allWidth) {
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