package net.osmand.plus.myplaces.tracks.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import net.osmand.CallbackWithObject
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndDialogFragment
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.tracks.TackFiltersContainer
import net.osmand.plus.myplaces.tracks.TracksSearchFilter
import net.osmand.plus.myplaces.tracks.filters.FilterChangedListener
import net.osmand.plus.myplaces.tracks.filters.FiltersAdapter
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper
import net.osmand.plus.track.data.SmartFolder
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton

class TracksFilterFragment : BaseOsmAndDialogFragment(),
	FilterChangedListener {
	companion object {
		val TAG: String = TracksFilterFragment::class.java.simpleName

		fun showInstance(
			manager: FragmentManager,
			filter: TracksSearchFilter,
			trackFiltersContainer: TackFiltersContainer,
			smartFolder: SmartFolder?) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = TracksFilterFragment()
				fragment.retainInstance = true
				fragment.trackFiltersContainer = trackFiltersContainer
				fragment.filter = filter
				fragment.smartFolder = smartFolder
				fragment.show(manager, TAG)
			}
		}
	}

	lateinit var filter: TracksSearchFilter
	var adapter: FiltersAdapter? = null
	var resetAllButton: DialogButton? = null
	var progressBar: ProgressBar? = null
	var showButton: DialogButton? = null
	private lateinit var smartFolderHelper: SmartFolderHelper
	private var smartFolder: SmartFolder? = null
	private lateinit var trackFiltersContainer: TackFiltersContainer
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
			val statusBarColor =
				if (nightMode) R.color.status_bar_secondary_dark else R.color.status_bar_secondary_light
			ContextCompat.getColor(requireContext(), statusBarColor)
			window.statusBarColor = ContextCompat.getColor(requireContext(), statusBarColor)
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
			dismiss()
			trackFiltersContainer.onFilterDialogClosed()
		}
		progressBar = view.findViewById(R.id.progress_bar)
	}

	fun setupToolbar(view: View) {
		appBar = view.findViewById(R.id.app_bar_layout)
		view.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationIcon = app.uiUtilities.getThemedIcon(R.drawable.ic_arrow_back)
			setNavigationOnClickListener {
				closeWithoutApply()
			}
		}
		val saveFilterBtn = view.findViewById<ImageView>(R.id.save_filters_btn)
		saveFilterBtn.setOnClickListener {
			if (smartFolder != null) {
				app.smartFolderHelper.saveSmartFolder(smartFolder!!, filter.currentFilters)
				Toast.makeText(app, R.string.smart_folder_saved, Toast.LENGTH_SHORT).show()
				dismiss()
				trackFiltersContainer.onFilterDialogClosed()
			} else {
				app.dialogManager.showSaveSmartFolderDialog(
					requireActivity(),
					nightMode,
					filter.currentFilters)
			}
		}
		val closeButton = view.findViewById<View>(R.id.close_button)
		if (closeButton != null) {
			closeButton.setOnClickListener {
				closeWithoutApply()
			}
			if (closeButton is ImageView) {
				UiUtilities.rotateImageByLayoutDirection(closeButton)
			}
		}
	}

	private fun closeWithoutApply() {
		if (filter.appliedFiltersCount > 0) {
			val fragmentManager = fragmentManager
			fragmentManager?.let {
				LeaveFiltersConfirmBottomSheet.showInstance(it, this)
			}
		} else {
			dismiss()
		}
	}

	fun closeWithoutApplyConfirmed() {
		filter.resetFilteredItems()
		if (smartFolder == null) {
			filter.resetCurrentFilters()
		}
		adapter?.let {
			it.updateItems()
			it.notifyItemRangeChanged(0, it.itemCount)
		}
		dismiss()
		trackFiltersContainer.onFilterDialogClosed()
	}

	private fun setupList(view: View) {
		adapter = FiltersAdapter(requireActivity(), filter, nightMode)
		val recyclerView = view.findViewById<RecyclerView>(R.id.filters_list)
		recyclerView.layoutManager = LinearLayoutManager(app)
		recyclerView.itemAnimator = null
		recyclerView.adapter = adapter
	}

	override fun onFilterChanged() {
		updateUI()
		applyFilter()
	}

	private fun applyFilter() {
		updateProgressVisibility(true)
		filter.filter()
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
		for (filter in filter.currentFilters) {
			filter.initFilter()
		}
		adapter?.notifyDataSetChanged()
		updateNightMode()
		updateStatusBarColor(requireDialog().window)
		filter.setCallback(CallbackWithObject<List<TrackItem>> { trackItems ->
			updateProgressVisibility(false)
			filter.filteredTrackItems = trackItems
			updateUI()
			return@CallbackWithObject true
		})
		filter.addFiltersChangedListener(this)
	}

	override fun onPause() {
		super.onPause()
		filter.removeFiltersChangedListener(this)
	}

	private fun updateProgressVisibility(visible: Boolean) {
		AndroidUiHelper.setVisibility(
			if (visible) View.VISIBLE else View.GONE, progressBar)
	}
}