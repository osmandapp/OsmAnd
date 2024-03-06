package net.osmand.plus.myplaces.tracks.dialogs

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.EmptySmartFolderListener
import net.osmand.plus.myplaces.tracks.TracksSearchFilter
import net.osmand.plus.myplaces.tracks.dialogs.TracksFilterFragment.Companion.showInstance
import net.osmand.plus.myplaces.tracks.filters.SmartFolderUpdateListener
import net.osmand.plus.track.data.SmartFolder
import net.osmand.plus.track.data.TracksGroup
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem

class SmartFolderFragment : TrackFolderFragment(), SmartFolderUpdateListener,
	EmptySmartFolderListener,
	DialogClosedListener {

	companion object {
		private val TAG = SmartFolderFragment::class.java.simpleName

		fun showInstance(manager: FragmentManager, folder: SmartFolder, target: Fragment) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TrackFolderFragment.TAG)) {
				val fragment = SmartFolderFragment()
				fragment.setSmartFolder(folder)
				fragment.setTargetFragment(target, 0)
				fragment.retainInstance = true
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TrackFolderFragment.TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun getFragmentTag(): String {
		return TAG
	}

	override fun onBackPressed() {
		dismiss()
	}

	override fun showFolderOptionMenu(): Boolean {
		val items: MutableList<PopUpMenuItem> = ArrayList()
		items.add(PopUpMenuItem.Builder(app)
			.setTitleId(R.string.shared_string_select)
			.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
			.setOnClickListener {
				smartFolder?.let {
					showTracksSelection(
						folder = it,
						fragment = this,
						trackItems = null,
						tracksGroups = null,
						screenPositionData = null)
				}
			}.create())

		items.add(PopUpMenuItem.Builder(app)
			.setTitleId(R.string.shared_string_refresh)
			.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_update))
			.setOnClickListener {
				reloadTracks()
			}
			.showTopDivider(true)
			.create())

		items.add(PopUpMenuItem.Builder(app)
			.setTitleId(R.string.edit_filter)
			.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_filter_dark))
			.setOnClickListener {
				editFilters()
			}
			.showTopDivider(true)
			.create())

		val view = requireActivity().findViewById<View>(R.id.action_folder_menu)
		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = view
		displayData.menuItems = items
		displayData.nightMode = isNightMode
		PopUpMenu.show(displayData)

		return true
	}

	private fun showTracksSelection(
		folder: TracksGroup, fragment: BaseTrackFolderFragment,
		trackItems: Set<TrackItem?>?, tracksGroups: Set<TracksGroup?>?,
		screenPositionData: ScreenPositionData?
	) {
		val manager = requireActivity().supportFragmentManager
		TracksSelectionFragment.showInstance(
			manager, folder, fragment, trackItems, tracksGroups, screenPositionData
		)
	}

	override fun onResume() {
		super.onResume()
		app.smartFolderHelper.addUpdateListener(this)
		updateContent()
	}


	override fun onPause() {
		super.onPause()
		app.smartFolderHelper.removeUpdateListener(this)
	}

	override fun getCurrentTrackGroup(): TracksGroup {
		return smartFolder!!
	}

	override fun onSmartFolderUpdated(smartFolder: SmartFolder) {
		if (this.smartFolder == smartFolder) {
			updateContent()
		}
	}

	override fun onSmartFoldersUpdated() {
		super.onSmartFoldersUpdated()
		val actualFolder = smartFolderHelper.getSmartFolder(smartFolder.folderName)
		if (actualFolder != null && actualFolder != smartFolder) {
			smartFolder = actualFolder
		}
		updateContent()
	}

	override fun setupAdapter(view: View) {
		super.setupAdapter(view)
		adapter.setEmptySmartFolderListener(this)
	}

	override fun editFilters() {
		if (smartFolder != null) {
			val manager = fragmentManager
			val trackItems = ArrayList<TrackItem>()
			trackItems.addAll(smartFolderHelper.getAllAvailableTrackItems())
			val filter = TracksSearchFilter(app, trackItems)
			filter.initSelectedFilters(smartFolder.filters)
			if (manager != null) {
				targetFragment?.let { showInstance(app, manager, it, filter, this, smartFolder, null) }
			}
		}
	}

	override fun onDialogClosed() {
		updateContent()
	}

}