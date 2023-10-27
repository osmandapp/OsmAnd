package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.filters.TrackFolderFilter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.TextViewEx
import net.osmand.util.Algorithms

class FilterFolderViewHolder(itemView: View, nightMode: Boolean) :
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
	private var filter: TrackFolderFilter? = null

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
		recycler = itemView.findViewById(R.id.folders)
	}

	fun bindView(filter: TrackFolderFilter) {
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
			val adapter = FolderAdapter()
			adapter.items.clear()
			adapter.items.addAll(it.allFolders)
			var trackFolder = filter?.currentFolder
			trackFolder?.let { currentTrackFolder ->
				val currentFolderName = currentTrackFolder.getDirName()
				adapter.items.remove(currentFolderName)
				adapter.items.add(0, currentFolderName)
			}
			recycler.adapter = adapter
			recycler.layoutManager = LinearLayoutManager(app)
			recycler.itemAnimator = null
			updateSelectedValue(it)
		}
	}

	private fun updateSelectedValue(it: TrackFolderFilter): Boolean {
		selectedValue.text =
			"${if (filter?.isAllFoldersSelected == true) app.getString(R.string.all_folders) else it.selectedFolders.size}"
		return AndroidUiHelper.updateVisibility(
			selectedValue,
			filter?.isAllFoldersSelected == true || it.selectedFolders.size > 0)
	}

	inner class FolderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		var items = ArrayList<String>()
		var fullFoldersListExpanded: Boolean
		val MIN_VISIBLE_COUNT = 5
		val ALL_FOLDERS_ITEM_TYPE = 0
		val FOLDER_ITEM_TYPE = 1
		val SHOW_ALL_ITEM_TYPE = 2

		init {
			fullFoldersListExpanded = false
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = UiUtilities.getInflater(parent.context, nightMode)

			return when (viewType) {
				ALL_FOLDERS_ITEM_TYPE -> {
					val view = inflater.inflate(
						R.layout.all_folders_filter_item,
						parent,
						false)
					AllFoldersViewHolder(view)
				}

				SHOW_ALL_ITEM_TYPE -> {
					val view = inflater.inflate(
						R.layout.show_all_folders_filter_item_item,
						parent,
						false)
					ShowAllViewHolder(view)
				}

				else -> {
					val view = inflater.inflate(R.layout.track_filter_checkbox_item, parent, false)
					FolderViewHolder(view)
				}
			}
		}

		override fun getItemCount(): Int {
			return if (filter?.isAllFoldersSelected == true) {
				1
			} else if (items.size > MIN_VISIBLE_COUNT && !fullFoldersListExpanded) {
				return MIN_VISIBLE_COUNT + 2
			} else {
				return items.size + 2
			}
		}

		override fun getItemViewType(position: Int): Int {
			return if (position == 0) {
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
					val correctedPosition = position - 1
					val folderName = items[correctedPosition]
					holder.title.text = if (Algorithms.isEmpty(folderName)) {
						context.getString(R.string.root_folder)
					} else {
						folderName
					}
					AndroidUiHelper.updateVisibility(
						holder.divider,
						correctedPosition != itemCount - 1)
					filter?.let { folderFilter ->
						var currentFolderPath = folderFilter.currentFolder?.getDirName()
						AndroidUiHelper.updateVisibility(
							holder.currentMark,
							Algorithms.stringsEqual(currentFolderPath, folderName))
						holder.itemView.setOnClickListener {
							folderFilter.setSelectedFolders(
								folderName,
								!folderFilter.isFolderSelected(folderName))
							holder.checkBox.isChecked = folderFilter.isFolderSelected(folderName)
							this.notifyItemChanged(correctedPosition)
							filter?.let { updateSelectedValue(it) }
						}
						holder.checkBox.isChecked = folderFilter.isFolderSelected(folderName)
						holder.count.text = folderFilter.allFoldersCollection[folderName].toString()
					}
					holder.icon.setImageDrawable(
						app.uiUtilities.getPaintedIcon(
							R.drawable.ic_action_folder,
							app.getColor(R.color.icon_color_default_light)))
				}

				is AllFoldersViewHolder -> {
					filter?.let {
						holder.switch.isSelected = it.isAllFoldersSelected
						if (holder.switch.isSelected) {
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
					}
					holder.switch.setOnCheckedChangeListener { _, isChecked ->
						onAllFolderSelected(isChecked)
					}
				}

				is ShowAllViewHolder -> {
					holder.title.setText(if (fullFoldersListExpanded) R.string.shared_string_show_less else R.string.shared_string_show_all)
					holder.itemView.setOnClickListener {
						fullFoldersListExpanded = !fullFoldersListExpanded
						notifyDataSetChanged()
					}
				}
			}
		}

		private fun onAllFolderSelected(selected: Boolean) {
			filter?.isAllFoldersSelected = selected
			notifyDataSetChanged()
			filter?.let { updateSelectedValue(it) }

		}
	}


	inner class AllFoldersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var switch: SwitchCompat
		var icon: ImageView

		init {
			switch = view.findViewById(R.id.toggle_item)
			icon = view.findViewById(R.id.icon)
		}
	}

	inner class ShowAllViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		var title: TextViewEx

		init {
			title = view.findViewById(R.id.title)
		}
	}

	inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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