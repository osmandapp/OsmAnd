package net.osmand.plus.myplaces.tracks.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndDialogFragment
import net.osmand.plus.myplaces.tracks.DialogClosedListener
import net.osmand.plus.myplaces.tracks.TrackFiltersHelper
import net.osmand.plus.myplaces.tracks.filters.ListFilterAdapter
import net.osmand.plus.myplaces.tracks.filters.ListTrackFilter
import net.osmand.plus.myplaces.tracks.filters.SmartFolderUpdateListener
import net.osmand.plus.myplaces.tracks.filters.TrackFilterPropertiesAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButton
import studio.carbonylgroup.textfieldboxes.ExtendedEditText

class FilterAllVariantsListFragment : BaseOsmAndDialogFragment(), SmartFolderUpdateListener {
	companion object {
		val TAG: String = FilterAllVariantsListFragment::class.java.simpleName

		fun showInstance(
			app: OsmandApplication,
			manager: FragmentManager,
			filter: ListTrackFilter,
			dialogClosedListener: DialogClosedListener?,
			selectedItemsListener: NewSelectedItemsListener) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val initialFilter =
					TrackFiltersHelper.createFilter(app, filter.trackFilterType, null)
				if (initialFilter !is ListTrackFilter) {
					throw IllegalArgumentException("Filter should be subclass from ListTrackFilter")
				}
				initialFilter.initWithValue(filter)
				val nightMode = app.daynightHelper.isNightMode(true)
				val currentFilter =
					TrackFiltersHelper.createFilter(
						app,
						filter.trackFilterType,
						null) as ListTrackFilter
				currentFilter.initWithValue(filter)
				currentFilter.setFullItemsCollection(filter.allItemsCollection)
				val adapter = ListFilterAdapter(app, nightMode, null, null)
				adapter.filter = currentFilter
				adapter.showAllItems = true
				adapter.items = ArrayList(filter.allItems)
				val fragment = FilterAllVariantsListFragment()
				fragment.initialFilter = initialFilter
				fragment.retainInstance = true
				fragment.dialogClosedListener = dialogClosedListener
				fragment.currentChangesFilter = currentFilter
				fragment.adapter = adapter
				fragment.selectedItemsListener = selectedItemsListener
				fragment.show(manager, TAG)
			}
		}
	}

	lateinit var initialFilter: ListTrackFilter
	lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

	var progressBar: ProgressBar? = null
	private var showButton: DialogButton? = null
	private var dialogClosedListener: DialogClosedListener? = null
	private lateinit var appBar: AppBarLayout
	private lateinit var currentChangesFilter: ListTrackFilter
	private lateinit var selectedItemsListener: NewSelectedItemsListener
	private val textWatcher: TextWatcher = object : TextWatcher {
		override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
		override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
		override fun afterTextChanged(s: Editable) {
			onFilterQueryChanged(s.toString())
		}
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
		val view: View =
			themedInflater.inflate(R.layout.fragment_collection_filter_property, container, false)
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
		setupQuery(view)
	}

	private fun onFilterQueryChanged(query: String) {
		(adapter as TrackFilterPropertiesAdapter).filterCollection(query)
	}

	private fun setupBottomMenu(view: View) {
		showButton = view.findViewById(R.id.show_button)
		showButton?.setOnClickListener {
			val newSelectedItems = ArrayList<String>()
			val oldSelectedItems = initialFilter.selectedItems
			val currentSelectedItems =
				currentChangesFilter.selectedItems
			for (selectedItem in currentSelectedItems) {
				if (!oldSelectedItems.contains(selectedItem)) {
					newSelectedItems.add(selectedItem)
				}
			}
			selectedItemsListener.setSelectedItemsDiff(currentSelectedItems, newSelectedItems)
			dismiss()
		}
		progressBar = view.findViewById(R.id.progress_bar)
	}

	fun setupToolbar(view: View) {
		appBar = view.findViewById(R.id.app_bar_layout)
		view.findViewById<Toolbar>(R.id.toolbar).apply {
			navigationContentDescription = app.getString(R.string.shared_string_close)
			val navigationIconColorId =
				if (nightMode) R.color.active_buttons_and_links_text_dark else R.color.icon_color_default_light
			navigationIcon = getIcon(R.drawable.ic_action_close, navigationIconColorId)
			setNavigationOnClickListener {
				closeWithoutApply()
			}
			setTitle(currentChangesFilter.trackFilterType.nameResId)
		}
	}

	private fun closeWithoutApply() {
		if (filterChanged()) {
			val builder = AlertDialog.Builder(requireContext())
			builder.setTitle(R.string.discard_filter_changes)
			builder.setMessage(R.string.discard_changes_prompt)
			builder.setNegativeButton(R.string.shared_string_cancel, null)
			builder.setPositiveButton(R.string.discard_changes) { _, _ ->
				closeWithoutApplyConfirmed()
			}
			builder.show()
		} else {
			dismiss()
		}
	}

	private fun closeWithoutApplyConfirmed() {
		dismiss()
	}

	private fun setupList(view: View) {
		val recyclerView = view.findViewById<RecyclerView>(R.id.filters_list)
		recyclerView.layoutManager = LinearLayoutManager(app)
		recyclerView.itemAnimator = null
		recyclerView.adapter = adapter
	}

	private fun setupQuery(view: View) {
		val queryInput: ExtendedEditText = view.findViewById(R.id.query_et)
		queryInput.addTextChangedListener(textWatcher)
	}

	private fun filterChanged(): Boolean {
		return initialFilter != currentChangesFilter
	}

	override fun onResume() {
		super.onResume()
		context?.let {
			updateNightMode()
			updateStatusBarColor(requireDialog().window)
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		dialogClosedListener?.onDialogClosed()
	}

	interface NewSelectedItemsListener {
		fun setSelectedItemsDiff(allSelectedItems: List<String>, selectedItems: List<String>)
	}
}