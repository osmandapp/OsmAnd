package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.CollatorStringMatcher
import net.osmand.CorwinLogger
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.dialogs.FilterAllVariantsListFragment
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.TrackFilterPropertiesAdapter
import net.osmand.plus.myplaces.tracks.filters.TrackFolderFilter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.search.core.SearchPhrase
import net.osmand.util.Algorithms
import net.osmand.view.ThreeStateCheckbox

class FilterFolderViewHolder(var app: OsmandApplication, itemView: View, nightMode: Boolean) :
	RecyclerView.ViewHolder(itemView) {
	private val nightMode: Boolean
	private var expanded = false
	private val title: TextViewEx
	private val selectedValue: TextViewEx
	private val recycler: RecyclerView
	private val titleContainer: View
	private val divider: View
	private val explicitIndicator: ImageView
	private lateinit var filter: TrackFolderFilter
	private lateinit var fragmentManager: FragmentManager
	private val filterChangedListener = object : FilterChangedListener {
		override fun onFilterChanged() {
			updateSelectedValue(filter)
		}
	}
	private val filterPropertiesClosed = object : DialogClosedListener {
		override fun onDialogClosed() {
			updateValues()
		}
	}
	private var adapter = FolderAdapter(app, nightMode, null, filterPropertiesClosed)

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
		recycler = itemView.findViewById(R.id.folders)
	}

	fun bindView(filter: TrackFolderFilter, fragmentManager: FragmentManager) {
		this.filter = filter
		adapter.filter = filter
		adapter.fragmentManager = fragmentManager
		title.setText(filter.displayNameId)
		updateExpandState()
		adapter.items = ArrayList(filter.allFolders)
		updateValues()
	}

	private fun updateExpandState() {
		val iconRes =
			if (expanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down
		explicitIndicator.setImageDrawable(app.uiUtilities.getIcon(iconRes, !nightMode))
		AndroidUiHelper.updateVisibility(recycler, expanded)
	}

	private fun updateValues() {
		var trackFolder = filter.currentFolder
		trackFolder?.let { currentTrackFolder ->
			val currentFolderName = currentTrackFolder.getDirName()
			adapter.items.remove(currentFolderName)
			adapter.items.add(0, currentFolderName)
		}
		recycler.adapter = adapter
		recycler.layoutManager = LinearLayoutManager(app)
		recycler.itemAnimator = null
		updateSelectedValue(filter)
	}

	private fun updateSelectedValue(it: TrackFolderFilter): Boolean {
		selectedValue.text =
			"${if (filter.isAllFoldersSelected) app.getString(R.string.all_folders) else it.selectedFolders.size}"
		return AndroidUiHelper.updateVisibility(
			selectedValue,
			filter.isAllFoldersSelected || it.selectedFolders.size > 0)
	}

	class FolderAdapter(
		val app: OsmandApplication,
		val nightMode: Boolean,
		val filterChangedListener: FilterChangedListener?,
		private val filterPropertiesClosed: DialogClosedListener?) :
		RecyclerView.Adapter<RecyclerView.ViewHolder>(),
		TrackFilterPropertiesAdapter {

		var fragmentManager: FragmentManager? = null
		var items = ArrayList<String>()
		var additionalItems = ArrayList<String>()
		var isExpanded = false
		lateinit var filter: TrackFolderFilter
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
		private var isAllFoldersBeingSet = false

		companion object {
			private val MIN_VISIBLE_COUNT = 5
			private val ALL_FOLDERS_ITEM_TYPE = 0
			private val FOLDER_ITEM_TYPE = 1
			private val SHOW_ALL_ITEM_TYPE = 2
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)
			return when (viewType) {
				ALL_FOLDERS_ITEM_TYPE -> {
					val view = inflater.inflate(
						R.layout.all_folders_filter_item,
						parent,
						false)
					AllFoldersViewHolder(app, view, nightMode)
				}

				SHOW_ALL_ITEM_TYPE -> {
					val view = inflater.inflate(
						R.layout.show_all_filter_items_item,
						parent,
						false)
					ShowAllViewHolder(view)
				}

				else -> {
					val view = inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
					FolderViewHolder(app, view, nightMode)
				}
			}
		}

		override fun getItemCount(): Int {
			return if (isExpanded) {
				items.size
			} else if (items.size > MIN_VISIBLE_COUNT) {
				MIN_VISIBLE_COUNT + additionalItems.size + 2
			} else {
				items.size + 1
			}
		}

		override fun getItemViewType(position: Int): Int {
			return if (isExpanded) {
				FOLDER_ITEM_TYPE
			} else if (position == 0) {
				ALL_FOLDERS_ITEM_TYPE
			} else if (position < itemCount - 1) {
				FOLDER_ITEM_TYPE
			} else {
				SHOW_ALL_ITEM_TYPE
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			holder.itemView.setOnClickListener(null)
			when (holder) {
				is FolderViewHolder -> {
					var context = holder.itemView.context.applicationContext
					val correctedPosition = getItemCorrectedPosition(position)
					val folderName = getFolderItem(position)
					holder.title.text = if (Algorithms.isEmpty(folderName)) {
						context.getString(R.string.root_folder)
					} else {
						folderName
					}
					AndroidUiHelper.updateVisibility(
						holder.divider,
						position != itemCount - 1)
					AndroidUiHelper.updateVisibility(
						holder.icon,
						true)
					var currentFolderPath = filter.currentFolder?.getDirName()
					AndroidUiHelper.updateVisibility(
						holder.currentMark,
						Algorithms.stringsEqual(currentFolderPath, folderName))
					holder.itemView.setOnClickListener {
						filter.setFolderSelected(
							folderName,
							!filter.isFolderSelected(folderName))
						updateAllFoldersCheck()
						holder.checkBox.isChecked = filter.isFolderSelected(folderName)
						this.notifyItemChanged(correctedPosition)
						this.notifyItemChanged(0)
						filterChangedListener?.onFilterChanged()
					}
					holder.checkBox.isChecked = filter.isFolderSelected(folderName)
					var itemsCount = filter.allFoldersCollection[folderName] ?: 0
					holder.count.text = itemsCount.toString()
					holder.itemView.isEnabled = itemsCount > 0
					holder.checkBox.isEnabled = itemsCount > 0
					holder.checkBox.setOnCheckedChangeListener(null)
					holder.icon.setImageDrawable(
						app.uiUtilities.getPaintedIcon(
							R.drawable.ic_action_folder,
							app.getColor(R.color.icon_color_default_light)))
				}

				is AllFoldersViewHolder -> {
					isAllFoldersBeingSet = true
					holder.switch.state = if (filter.isAllFoldersSelected ||
						filter.selectedFolders.size == items.size) {
						holder.switch.isChecked = true
						ThreeStateCheckbox.State.CHECKED
					} else if (filter.selectedFolders.size > 0) {
						holder.switch.isChecked = false
						ThreeStateCheckbox.State.MISC
					} else {
						holder.switch.isChecked = false
						ThreeStateCheckbox.State.UNCHECKED
					}
					isAllFoldersBeingSet = false
					if (holder.switch.state != ThreeStateCheckbox.State.UNCHECKED) {
						holder.icon.setImageDrawable(
							app.uiUtilities.getActiveIcon(
								R.drawable.ic_action_folder,
								nightMode))
					} else {
						holder.icon.setImageDrawable(
							app.uiUtilities.getPaintedIcon(
								R.drawable.ic_action_folder,
								app.getColor(R.color.icon_color_default_light)))
					}
					holder.switch.setOnCheckedChangeListener { _, isChecked ->
						onAllFolderSelected(holder.switch, isChecked)
					}
				}

				is ShowAllViewHolder -> {
					holder.title.setText(R.string.shared_string_show_all)
					holder.itemView.setOnClickListener {
						val folderFilter = TrackFolderFilter(null)
						folderFilter.initWithValue(filter)
						folderFilter.setFullFoldersCollection(filter.allFoldersCollection)
						val folderAdapter = FolderAdapter(app, nightMode, null, null)
						folderAdapter.filter = folderFilter
						folderAdapter.isExpanded = true
						folderAdapter.items = ArrayList(filter.allFolders)
						FilterAllVariantsListFragment.showInstance(
							app,
							fragmentManager!!,
							folderFilter,
							filterPropertiesClosed,
							folderAdapter,
							newSelectedItemsListener)
					}
				}
			}
		}

		private fun updateAllFoldersCheck() {
			filter.isAllFoldersSelected = filter.selectedFolders.size == items.size
		}

		private fun getFolderItem(position: Int): String {
			return if (isExpanded) {
				items[position]
			} else if (position <= MIN_VISIBLE_COUNT) {
				items[position - 1]
			} else {
				additionalItems[position - MIN_VISIBLE_COUNT - 1]
			}
		}

		private fun getItemCorrectedPosition(position: Int): Int {
			return if (isExpanded) {
				position
			} else {
				position - 1
			}
		}

		private fun onAllFolderSelected(checkbox: ThreeStateCheckbox, selected: Boolean) {
			if (isAllFoldersBeingSet) return
			filter.isAllFoldersSelected = selected
			if (selected) {
				checkbox.state = ThreeStateCheckbox.State.CHECKED
				filter.addSelectedItems(filter.allFolders)
			} else {
				filter.clearSelectedItems()
				checkbox.state = ThreeStateCheckbox.State.UNCHECKED
			}
			notifyDataSetChanged()
			filterChangedListener?.onFilterChanged()

		}

		override fun filterCollection(query: String) {
			CorwinLogger.log("onFilterQueryChanged")
			val filteredItems = ArrayList<String>()
			if (Algorithms.isEmpty(query)) {
				filteredItems.addAll(filter.allFolders)
			} else {
				var matcher = SearchPhrase.NameStringMatcher(
					query.trim { it <= ' ' },
					CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)
				filter.let {
					for (folder in it.allFolders) {
						if (matcher.matches(folder)) {
							filteredItems.add(folder)
						}
					}
				}
			}
			items = filteredItems
			notifyDataSetChanged()
		}

		override fun setNewSelectedItems(newSelectedItems: List<String>) {
			CorwinLogger.log("setNewSelectedItems")
			var additionalItems = ArrayList<String>()
			filter.addSelectedItems(newSelectedItems)
			for (selectedItem in newSelectedItems) {
				if (items.indexOf(selectedItem) >= MIN_VISIBLE_COUNT) {
					additionalItems.add(selectedItem)
				}
			}
			this.additionalItems = additionalItems
			notifyDataSetChanged()
		}
	}


	class AllFoldersViewHolder(app: OsmandApplication, view: View, val nightMode: Boolean) :
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

	class FolderViewHolder(app: OsmandApplication, view: View, val nightMode: Boolean) :
		RecyclerView.ViewHolder(view) {
		var title: TextViewEx
		var count: TextViewEx
		var checkBox: AppCompatCheckBox
		var divider: View
		val icon: ImageView
		val currentMark: TextViewEx


		init {
			icon = itemView.findViewById(R.id.icon)
			title = view.findViewById(R.id.title)
			count = view.findViewById(R.id.count)
			checkBox = view.findViewById(R.id.compound_button)
			divider = view.findViewById(R.id.divider)
			currentMark = view.findViewById(R.id.current_mark)
			UiUtilities.setupCompoundButton(
				nightMode,
				net.osmand.plus.utils.ColorUtilities.getActiveColor(app, nightMode),
				checkBox)
		}
	}
}