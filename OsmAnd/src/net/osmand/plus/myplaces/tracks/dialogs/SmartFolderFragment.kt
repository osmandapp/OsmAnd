package net.osmand.plus.myplaces.tracks.dialogs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment
import net.osmand.plus.myplaces.tracks.filters.SmartFolderUpdateListener
import net.osmand.plus.track.data.SmartFolder
import net.osmand.plus.track.data.TracksGroup
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem

class SmartFolderFragment : TrackFolderFragment(), SmartFolderUpdateListener {

	companion object {
		private val TAG = SmartFolderFragment::class.java.simpleName

		fun showInstance(manager: FragmentManager, folder: SmartFolder, target: Fragment?) {
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
			.setOnClickListener { v: View? ->
				smartFolder?.let {
					showTracksSelection(
						it,
						this,
						null,
						null)
				}
			}.create())

		items.add(PopUpMenuItem.Builder(app)
			.setTitleId(R.string.shared_string_refresh)
			.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_update))
			.setOnClickListener { v: View? ->
				reloadTracks()
			}
			.showTopDivider(true)
			.create())

		items.add(PopUpMenuItem.Builder(app)
			.setTitleId(R.string.edit_fiilter)
			.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_filter_dark))
			.setOnClickListener { v: View? ->

				val manager = fragmentManager
				if (manager != null) {
					SearchMyPlacesTracksFragment.showInstance(
						manager,
						targetFragment,
						false,
						isUsedOnMap,
						smartFolder)

				}
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

	override fun restoreState(bundle: Bundle?) {
		super.restoreState(bundle)
	}

	private fun showTracksSelection(
		folder: TracksGroup, fragment: BaseTrackFolderFragment,
		trackItems: Set<TrackItem?>?, tracksGroups: Set<TracksGroup?>?) {
		val manager = requireActivity().supportFragmentManager
		TracksSelectionFragment.showInstance(
			manager,
			folder,
			fragment,
			trackItems,
			tracksGroups)
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
		if (actualFolder != smartFolder) {
			smartFolder = actualFolder
		}
		updateContent()
	}
}