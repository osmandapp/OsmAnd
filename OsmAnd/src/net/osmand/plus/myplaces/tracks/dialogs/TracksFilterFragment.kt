package net.osmand.plus.myplaces.tracks.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndDialogFragment
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
			smartFolder: SmartFolder?) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = TracksFilterFragment()
				fragment.retainInstance = true
				fragment.filter = filter
				fragment.smartFolder = smartFolder
				fragment.show(manager, TAG)
			}
		}
	}

	lateinit var filter: TracksSearchFilter
	var adapter: FiltersAdapter? = null
	var resetAllButton: DialogButton? = null
	var showButton: DialogButton? = null
	private lateinit var smartFolderHelper: SmartFolderHelper
	private var smartFolder: SmartFolder? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		smartFolderHelper = app.smartFolderHelper
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
			filter.filter()
		}
	}

	fun setupToolbar(view: View) {
		val saveFilterBtn = view.findViewById<ImageView>(R.id.save_filters_btn)
		saveFilterBtn.setOnClickListener {
			if (smartFolder != null) {
				app.smartFolderHelper.saveSmartFolder(smartFolder!!, filter.currentFilters)
				Toast.makeText(app, R.string.smart_folder_saved, Toast.LENGTH_SHORT).show()
				dismiss()
			} else {
				smartFolderHelper.showSaveSmartFolderDialog(
					requireActivity(),
					nightMode,
					filter.currentFilters)
			}
		}
		val closeButton = view.findViewById<View>(R.id.close_button)
		if (closeButton != null) {
			closeButton.setOnClickListener {
				adapter?.let {
					it.updateItems()
					it.notifyItemRangeChanged(0, it.itemCount)
				}
				dismiss()
			}
			if (closeButton is ImageView) {
				UiUtilities.rotateImageByLayoutDirection(closeButton)
			}
		}
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
	}

	private fun updateUI() {
		resetAllButton?.isEnabled = filter.appliedFiltersCount > 0
	}

	override fun onResume() {
		super.onResume()
		filter.addFiltersChangedListener(this)
	}

	override fun onPause() {
		super.onPause()
		filter.removeFiltersChangedListener(this)
	}
}