package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.CollatorStringMatcher
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.dialogs.FilterAllVariantsListFragment
import net.osmand.plus.myplaces.tracks.filters.ColorTrackFilter
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.TrackFilterPropertiesAdapter
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms

class FilterColorViewHolder(var app: OsmandApplication, itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private var filter: ColorTrackFilter? = null

	private val filterPropertiesClosed = object : DialogClosedListener {
		override fun onDialogClosed() {
			updateValues()
		}
	}
	private val adapter = ColorAdapter(app, nightMode, null, filterPropertiesClosed)

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

	fun bindView(filter: ColorTrackFilter, fragmentManager: FragmentManager) {
		this.filter = filter
		adapter.items = ArrayList(filter.allColors)
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
			adapter.items = ArrayList(it.allColors)
			adapter.items.remove("")
			adapter.items.add(0, "")
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			updateSelectedValue(it)
		}
	}

	private fun updateSelectedValue(it: ColorTrackFilter): Boolean {
		selectedValue.text = "${it.selectedColors.size}"
		return AndroidUiHelper.updateVisibility(selectedValue, it.selectedColors.size > 0)
	}

	class ColorAdapter(
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
		lateinit var filter: ColorTrackFilter
		private val newSelectedItemsListener =
			object : FilterAllVariantsListFragment.NewSelectedItemsListener {
				override fun setSelectedItemsDiff(
					allSelectedItems: List<String>,
					selectedItems: List<String>) {
					filterCollection("")
					filter.setSelectedColor(allSelectedItems)
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
						val colorFilter = ColorTrackFilter(null)
						colorFilter.initWithValue(filter)
						colorFilter.setFullColorsCollection(filter.allColorsCollection)
						val colorAdapter =
							ColorAdapter(app, nightMode, null, null)
						colorAdapter.filter = colorFilter
						colorAdapter.isExpanded = true
						colorAdapter.items = ArrayList(filter.allColors)
						FilterAllVariantsListFragment.showInstance(
							app,
							fragmentManager,
							colorFilter,
							filterPropertiesClosed,
							colorAdapter,
							newSelectedItemsListener)
					}
				}

				is FilterVariantViewHolder -> {
					val colorName = getItem(position)
					if (Algorithms.isEmpty(colorName)) {
						holder.title.text = app.getString(R.string.not_specified)
						holder.icon.setImageDrawable(app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled))
					} else {
						val color = Color.parseColor(colorName)
						holder.title.text = colorName
						val transparencyIcon = getTransparencyIcon(app, color)
						val colorIcon =
							app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
						val layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
						holder.icon.setImageDrawable(layeredIcon)
					}
					AndroidUiHelper.updateVisibility(holder.icon, true)
					AndroidUiHelper.updateVisibility(holder.divider, position != itemCount - 1)
					holder.itemView.setOnClickListener {
						filter.setColorSelected(colorName, !filter.isColorSelected(colorName))
						this.notifyItemChanged(position)
					}
					holder.checkBox.isChecked = filter.isColorSelected(colorName)
					holder.count.text = filter.getTracksCountForColor(colorName).toString()

				}
			}
		}

		private fun getTransparencyIcon(app: OsmandApplication, @ColorInt color: Int): Drawable? {
			val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
			val transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
			return app.uiUtilities.getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor)
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
			} else if (position == itemCount - 1 && items.size != MIN_VISIBLE_COUNT + additionalItems.size) {
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
				filteredItems.addAll(filter.allColors)
			} else {
				var matcher = SearchPhrase.NameStringMatcher(
					query.trim { it <= ' ' },
					CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
				filter.let {
					for (city in it.allColors) {
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