package net.osmand.plus.myplaces.tracks.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import net.osmand.CallbackWithObject
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndDialogFragment
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment
import net.osmand.plus.myplaces.tracks.TracksSearchFilter
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.FiltersAdapter
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper
import net.osmand.plus.myplaces.tracks.filters.SmartFolderUpdateListener
import net.osmand.plus.track.data.SmartFolder
import net.osmand.plus.track.data.TrackFolder
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColor
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.util.Algorithms

class TracksFilterFragment : BaseOsmAndDialogFragment(),
	FilterChangedListener, SmartFolderUpdateListener {
	companion object {
		val TAG: String = TracksFilterFragment::class.java.simpleName

		fun showInstance(
			app: OsmandApplication,
			manager: FragmentManager,
			target: Fragment?,
			filter: TracksSearchFilter,
			trackFiltersContainer: DialogClosedListener?,
			smartFolder: SmartFolder?,
			currentFolder: TrackFolder?) {
			manager.findFragmentByTag(TAG)?.let { foundFragment ->
				(foundFragment as TracksFilterFragment).dialog?.dismiss()
			}
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val initialFilter = TracksSearchFilter(app, arrayListOf())
				initialFilter.initSelectedFilters(filter.appliedFilters)
				val fragment = TracksFilterFragment()
				fragment.setTargetFragment(target, 0)
				fragment.initialFilterState = initialFilter
				fragment.retainInstance = true
				fragment.dialogClosedListener = trackFiltersContainer
				fragment.filter = filter
				fragment.currentFolder = currentFolder
				fragment.smartFolder = smartFolder
				fragment.show(manager, TAG)
			}
		}
	}

	lateinit var filter: TracksSearchFilter
	lateinit var initialFilterState: TracksSearchFilter
	var adapter: FiltersAdapter? = null
	var resetAllButton: DialogButton? = null
	var progressBar: ProgressBar? = null
	var showButton: DialogButton? = null
	private lateinit var smartFolderHelper: SmartFolderHelper
	private var smartFolder: SmartFolder? = null
	private var currentFolder: TrackFolder? = null
	private var dialogClosedListener: DialogClosedListener? = null
	private lateinit var appBar: AppBarLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		smartFolderHelper = app.smartFolderHelper
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val themeId =
			if (nightMode) R.style.OsmandDarkTheme else R.style.OsmandLightTheme_LightStatusBar
		val dialog = object : Dialog(requireContext(), themeId) {
			override fun onBackPressed() {
				closeWithoutApply()
			}
		}
		val window = dialog.window
		if (window != null) {
			if (!settings.DO_NOT_USE_ANIMATIONS.get()) {
				window.attributes.windowAnimations = R.style.Animations_Alpha
			}
			updateStatusBarColor(window)
		}
		return dialog
	}

	private fun updateStatusBarColor(window: Window?) {
		window?.let {
			window.statusBarColor = getStatusBarSecondaryColor(requireContext(), nightMode)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View {
		updateNightMode()
		val view: View = themedInflater.inflate(R.layout.fragment_track_filters, container, false)
		view.setBackgroundColor(
			ContextCompat.getColor(
				app,
				if (nightMode) R.color.activity_background_color_dark else R.color.list_background_color_light))
		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupToolbar(view)
		setupList(view)
		setupBottomMenu(view)
		updateUI()
		applyFilter()
	}

	private fun setupBottomMenu(view: View) {
		resetAllButton = view.findViewById(R.id.reset_all_button)
		resetAllButton?.setOnClickListener {
			filter.resetCurrentFilters()
			adapter?.let {
				it.updateItems()
				it.notifyItemRangeChanged(0, it.itemCount)
			}
		}
		showButton = view.findViewById(R.id.show_button)
		showButton?.setOnClickListener {
			val activity = activity
			if (activity != null) {
				val manager = activity.supportFragmentManager
				SearchMyPlacesTracksFragment.showInstance(
					manager,
					targetFragment,
					false,
					isUsedOnMap,
					smartFolder,
					filter,
					object : DialogClosedListener {
						override fun onDialogClosed() {
							updateFilters()
						}
					},
					currentFolder)
			}
		}
		progressBar = view.findViewById(R.id.progress_bar)
	}

	fun setupToolbar(view: View) {
		appBar = view.findViewById(R.id.app_bar_layout)
		view.findViewById<Toolbar>(R.id.toolbar).apply {
			inflateMenu(R.menu.show_filters_menu)
			val closeMenu = menu.findItem(R.id.action_filters)
			val descriptionId =
				if (smartFolder == null) R.string.save_as_smart_folder else R.string.save_filter
			MenuItemCompat.setContentDescription(
				closeMenu,
				app.getString(descriptionId))
			navigationContentDescription = app.getString(R.string.shared_string_close)
			val navigationIconColorId =
				if (nightMode) R.color.active_buttons_and_links_text_dark else R.color.icon_color_default_light
			navigationIcon =
				getIcon(R.drawable.ic_action_close, navigationIconColorId)
			setNavigationOnClickListener {
				closeWithoutApply()
			}
			setTitle(R.string.filter_screen_title)
			setOnMenuItemClickListener {
				when (it.itemId) {
					R.id.action_filters -> {
						if (smartFolder != null) {
							app.smartFolderHelper.saveSmartFolder(
								smartFolder!!,
								filter.currentFilters)
							Toast.makeText(app, R.string.smart_folder_saved, Toast.LENGTH_SHORT)
								.show()
							dismiss()
						} else {
							app.dialogManager.showSaveSmartFolderDialog(
								requireActivity(),
								nightMode,
								filter.currentFilters)
						}
						true
					}

					else -> {
						false
					}
				}
			}
		}
	}

	private fun closeWithoutApply() {
		if (filterChanged()) {
			val fragmentManager = fragmentManager
			fragmentManager?.let {
				val builder = AlertDialog.Builder(requireContext())
				builder.setTitle(R.string.discard_filter_changes)
				builder.setMessage(R.string.discard_filter_changes_prompt)
				builder.setNegativeButton(R.string.shared_string_cancel, null)
				builder.setPositiveButton(R.string.discard_changes) { dialog, which ->
					closeWithoutApplyConfirmed()
				}
				builder.show()
			}
		} else {
			dismiss()
		}
	}

	private fun closeWithoutApplyConfirmed() {
		filter.resetFilteredItems()
		if (smartFolder == null) {
			filter.initSelectedFilters(initialFilterState.appliedFilters)
		}
		adapter?.let {
			it.updateItems()
			it.notifyItemRangeChanged(0, it.itemCount)
		}
		dismiss()
	}

	private fun setupList(view: View) {
		fragmentManager?.let {
			adapter = FiltersAdapter(app, requireActivity(), it, filter, nightMode)
			val recyclerView = view.findViewById<RecyclerView>(R.id.filters_list)
			recyclerView.layoutManager = LinearLayoutManager(app)
			recyclerView.itemAnimator = null
			recyclerView.adapter = adapter
		}
	}

	override fun onFilterChanged() {
		updateUI()
		applyFilter()
	}

	private fun applyFilter() {
		updateProgressVisibility(true)
		filter.filter()
	}

	private fun filterChanged(): Boolean {
		var changed = false

		var initialFilters: List<BaseTrackFilter>? = if (smartFolder == null) {
			this.initialFilterState.appliedFilters
		} else {
			smartFolder?.filters
		}
		initialFilters?.let {
			if (Algorithms.isEmpty(it)) {
				changed = filter.appliedFiltersCount > 0
			} else {
				if (it.size != filter.appliedFiltersCount) {
					changed = true
				} else {
					for (folderFilter in it) {
						if (folderFilter != filter.getFilterByType(folderFilter.trackFilterType)) {
							changed = true
							break
						}
					}
				}
			}
		}
		return changed
	}

	private fun updateUI() {
		resetAllButton?.isEnabled = filter.appliedFiltersCount > 0
		var filteredItemsCount = 0
		if (filter.filteredTrackItems?.size != null) {
			filteredItemsCount = filter.filteredTrackItems!!.size
		}
		showButton?.setTitle(
			app.getString(R.string.shared_string_show) + " " +
					String.format(app.getString(R.string.number_in_breckets), filteredItemsCount))
	}

	override fun onResume() {
		super.onResume()
		smartFolderHelper.addUpdateListener(this)
		updateFilters()
	}

	private fun updateFilters() {
		for (filter in filter.currentFilters) {
			filter.initFilter()
		}
		adapter?.notifyDataSetChanged()
		context?.let {
			updateNightMode()
			updateStatusBarColor(requireDialog().window)
		}
		filter.setCallback(CallbackWithObject<List<TrackItem>> { trackItems ->
			updateProgressVisibility(false)
			filter.filteredTrackItems = trackItems
			adapter?.onTracksFilteringComplete()
			updateUI()
			return@CallbackWithObject true
		})
		filter.addFiltersChangedListener(this)
	}

	override fun onPause() {
		super.onPause()
		filter.removeFiltersChangedListener(this)
		smartFolderHelper.removeUpdateListener(this)
	}

	private fun updateProgressVisibility(visible: Boolean) {
		app.runInUIThread{
			AndroidUiHelper.setVisibility(
				if (visible) View.VISIBLE else View.GONE, progressBar)
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		dialogClosedListener?.onDialogClosed()
	}

	override fun onSmartFolderSaved(smartFolder: SmartFolder?) {
		super.onSmartFolderSaved(smartFolder)
		dismiss()
	}

	override fun onSmartFolderCreated(smartFolder: SmartFolder?) {
		dismiss()
	}
}