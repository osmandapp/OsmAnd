package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.CollatorStringMatcher
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.dialogs.FilterAllVariantsListFragment
import net.osmand.plus.myplaces.tracks.filters.viewholders.FilterVariantViewHolder
import net.osmand.plus.myplaces.tracks.filters.viewholders.ShowAllViewHolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.search.core.SearchPhrase
import net.osmand.shared.gpx.filters.FilterChangedListener
import net.osmand.shared.gpx.filters.FolderSingleFieldTrackFilterParams
import net.osmand.shared.gpx.filters.ListTrackFilter
import net.osmand.shared.gpx.filters.SingleFieldTrackFilterParams
import net.osmand.util.Algorithms
import net.osmand.view.ThreeStateCheckbox

class ListFilterAdapter(
	val app: OsmandApplication,
	var nightMode: Boolean,
	val filterChangedListener: FilterChangedListener?,
	private val filterPropertiesClosed: DialogClosedListener?) :
	RecyclerView.Adapter<RecyclerView.ViewHolder>(),
	TrackFilterPropertiesAdapter {
	companion object {
		private const val MIN_VISIBLE_COUNT = 5
		private const val SELECT_ALL_ITEM_TYPE = 0
		private const val ITEM_TYPE = 1
		private const val SHOW_ALL_ITEM_TYPE = 2
	}

	var items = ArrayList<String>()
	var showAllItems = false
	private var additionalItems = ArrayList<String>()
	lateinit var fragmentManager: FragmentManager
	private lateinit var _filter: ListTrackFilter
	var filter: ListTrackFilter
		get() = _filter
		set(value) {
			_filter = value
			filterParamsAdapter = FilterParamsAdapter(app, value.collectionFilterParams)
		}
	private lateinit var filterParamsAdapter: FilterParamsAdapter
	private var isSelectAllItemsBeingSet = false

	private val newSelectedItemsListener =
		object : FilterAllVariantsListFragment.NewSelectedItemsListener {
			override fun setSelectedItemsDiff(
				allSelectedItems: List<String>,
				selectedItems: List<String>) {
				filterCollection("")
				filter.setSelectedItems(allSelectedItems)
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

			SELECT_ALL_ITEM_TYPE -> {
				val view = inflater.inflate(
					R.layout.all_folders_filter_item,
					parent,
					false)
				SelectAllViewHolder(app, view, nightMode)
			}

			else -> {
				val view = inflater.inflate(
					R.layout.show_all_filter_items_item,
					parent,
					false)
				val itemName = items[0]
				val selected = filter.isItemSelected(itemName)
				val topBottomPadding =
					app.resources.getDimensionPixelSize(R.dimen.content_padding_small)
				val leftRightPadding =
					if (items.size > 0 && filterParamsAdapter.getFilterItemIcon(itemName, selected, nightMode) != null) {
						app.resources.getDimensionPixelSize(R.dimen.content_padding_extra_large)
					} else {
						app.resources.getDimensionPixelSize(R.dimen.content_padding)
					}
				view.setPadding(
					leftRightPadding,
					topBottomPadding,
					leftRightPadding,
					topBottomPadding)
				ShowAllViewHolder(view)
			}
		}
	}


	private fun getFilterAllItemsIcon(
		filterParams: SingleFieldTrackFilterParams, isChecked: Boolean,
		nightMode: Boolean): Drawable? {
		if (filterParams is FolderSingleFieldTrackFilterParams) {
			return if (isChecked) {
				app.uiUtilities.getActiveIcon(
					R.drawable.ic_action_group_select_all,
					nightMode)
			} else {
				app.uiUtilities.getPaintedIcon(
					R.drawable.ic_action_group_select_all,
					app.getColor(R.color.icon_color_default_light))
			}
		} else {
			return null
		}

	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (holder) {
			is ShowAllViewHolder -> {
				holder.title.setText(R.string.shared_string_show_all)
				holder.itemView.setOnClickListener {
					FilterAllVariantsListFragment.showInstance(
						app,
						fragmentManager,
						filter,
						filterPropertiesClosed,
						newSelectedItemsListener,
						nightMode)
				}
			}

			is FilterVariantViewHolder -> {
				val itemName = getItem(position)
				val selected = filter.isItemSelected(itemName)
				val icon = filterParamsAdapter.getFilterItemIcon(itemName, selected, nightMode)
				holder.title.text = filterParamsAdapter.getItemText(itemName)
				holder.icon.setImageDrawable(icon)
				AndroidUiHelper.updateVisibility(holder.icon, icon != null)
				AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
				holder.itemView.setOnClickListener {
					filter.setItemSelected(itemName, !filter.isItemSelected(itemName))
					this.notifyItemChanged(position)
				}
				holder.count.text = filter.getTracksCountForItem(itemName).toString()
				holder.checkBox.isChecked = selected
			}

			is SelectAllViewHolder -> {
				isSelectAllItemsBeingSet = true
				holder.switch.state = if (filter.isSelectAllItemsSelected ||
					filter.selectedItems.size == items.size) {
					holder.switch.isChecked = true
					ThreeStateCheckbox.State.CHECKED
				} else if (filter.selectedItems.size > 0) {
					holder.switch.isChecked = false
					ThreeStateCheckbox.State.MISC
				} else {
					holder.switch.isChecked = false
					ThreeStateCheckbox.State.UNCHECKED
				}
				isSelectAllItemsBeingSet = false
				holder.icon.setImageDrawable(
					getFilterAllItemsIcon(
						filter.collectionFilterParams,
						holder.switch.state != ThreeStateCheckbox.State.UNCHECKED,
						nightMode))
				holder.switch.setOnCheckedChangeListener { _, isChecked ->
					onAllFolderSelected(holder.switch, isChecked)
				}
				holder.itemView.setOnClickListener {
					onAllFolderSelected(holder.switch, !filter.isSelectAllItemsSelected)
					this.notifyItemChanged(position)
				}
			}
		}
	}

	private fun onAllFolderSelected(checkbox: ThreeStateCheckbox, selected: Boolean) {
		if (isSelectAllItemsBeingSet) return
		filter.isSelectAllItemsSelected = selected
		if (selected) {
			checkbox.state = ThreeStateCheckbox.State.CHECKED
			filter.addSelectedItems(ArrayList(filter.allItemsCollection.keys))
		} else {
			filter.clearSelectedItems()
			checkbox.state = ThreeStateCheckbox.State.UNCHECKED
		}
		notifyDataSetChanged()
		filterChangedListener?.onFilterChanged()

	}

	override fun getItemCount(): Int {
		val correctionForSelectAllItem = if (hasSelectAllVariant()) 1 else 0
		return if (showAllItems) {
			items.size
		} else if (items.size > MIN_VISIBLE_COUNT) {
			MIN_VISIBLE_COUNT +
					additionalItems.size +
					(if (MIN_VISIBLE_COUNT + additionalItems.size == items.size) 0 else 1) +
					correctionForSelectAllItem
		} else {
			items.size + correctionForSelectAllItem
		}
	}

	override fun getItemViewType(position: Int): Int {
		return if (showAllItems) {
			ITEM_TYPE
		} else if (position == 0 && hasSelectAllVariant()) {
			SELECT_ALL_ITEM_TYPE
		} else if (position == itemCount - 1 && items.size > MIN_VISIBLE_COUNT + additionalItems.size) {
			SHOW_ALL_ITEM_TYPE
		} else {
			ITEM_TYPE
		}
	}

	private fun getItem(position: Int): String {
		val correctionForSelectAllItem = if (hasSelectAllVariant()) 1 else 0
		return if (showAllItems) {
			items[position]
		} else if (position - correctionForSelectAllItem < MIN_VISIBLE_COUNT) {
			items[position - correctionForSelectAllItem]
		} else {
			additionalItems[position - correctionForSelectAllItem - MIN_VISIBLE_COUNT]
		}
	}

	private fun hasSelectAllVariant() = filter.collectionFilterParams.hasSelectAllVariant()

	override fun setNewSelectedItems(newSelectedItems: List<String>) {
		val additionalItems = ArrayList<String>()
		val correctionForSelectAllItem = if (hasSelectAllVariant()) 1 else 0
		for (selectedItem in newSelectedItems) {
			if (items.indexOf(selectedItem) + correctionForSelectAllItem >= MIN_VISIBLE_COUNT) {
				additionalItems.add(selectedItem)
			}
		}
		this.additionalItems = additionalItems
		notifyDataSetChanged()
	}

	override fun filterCollection(query: String) {
		val filteredItems = ArrayList<String>()
		if (Algorithms.isEmpty(query)) {
			filteredItems.addAll(filter.allItems)
		} else {
			val matcher = SearchPhrase.NameStringMatcher(
				query.trim { it <= ' ' },
				CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
			filter.let {
				for (item in it.allItems) {
					if (matcher.matches(item)) {
						filteredItems.add(item)
					}
				}
			}
		}
		items = filteredItems
		notifyDataSetChanged()
	}

	class SelectAllViewHolder(app: OsmandApplication, view: View, val nightMode: Boolean) :
		RecyclerView.ViewHolder(view) {
		var switch: ThreeStateCheckbox
		var icon: ImageView

		init {
			switch = view.findViewById(R.id.toggle_item)
			icon = view.findViewById(R.id.icon)
			UiUtilities.setupCompoundButton(
				nightMode,
				net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
				switch)
		}
	}
}