package net.osmand.plus.nearbyplaces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.PlatformUtil
import net.osmand.data.NearbyPlacePoint
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper.getDataCollection
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper.showPointInContextMenu
import net.osmand.plus.search.NearbyPlacesAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import org.apache.commons.logging.Log

class NearbyPlacesFragment : BaseOsmAndFragment(), NearbyPlacesAdapter.NearbyItemClickListener {
	private val log: Log = PlatformUtil.getLog(
		NearbyPlacesFragment::class.java)

	private lateinit var verticalNearbyAdapter: NearbyPlacesAdapter

	override fun getContentStatusBarNightMode(): Boolean {
		return nightMode
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?): View? {
		updateNightMode()
		return themedInflater.inflate(R.layout.fragment_nearby_places, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		AndroidUtils.addStatusBarPadding21v(requireActivity(), view)
		setupShowAll(view)
		setupToolBar(view)
		setupVerticalNearbyList(view)
	}

	fun onBackPress(): Boolean {
		if (isHidden) {
			if (mapActivity?.contextMenu?.isVisible == true) {
				mapActivity?.contextMenu?.hideMenus()
			} else {
				activity?.supportFragmentManager?.beginTransaction()
					?.show(this@NearbyPlacesFragment)
					?.commit()
			}
			return true
		} else {
			val quickSearchFragment = mapActivity?.fragmentsHelper?.quickSearchDialogFragment
			quickSearchFragment?.show()
			activity?.supportFragmentManager?.beginTransaction()
				?.remove(this@NearbyPlacesFragment)
				?.commit()
			return false
		}
	}

	private fun setupShowAll(view: View) {
		view.findViewById<ImageView>(R.id.location_icon)
			.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_marker_dark, nightMode))
		view.findViewById<View>(R.id.show_on_map).setOnClickListener {
			app.osmandMap.mapLayers.nearbyPlacesLayer.setCustomMapObjects(getDataCollection())
			hide()
		}
	}

	@ColorRes
	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.status_bar_main_dark else R.color.status_bar_main_light
	}

	private fun setupToolBar(view: View) {
		val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setTitleTextColor(app.getColor(ColorUtilities.getPrimaryTextColorId(nightMode)))
		toolbar.navigationIcon =
			getIcon(R.drawable.ic_arrow_back, ColorUtilities.getPrimaryIconColorId(nightMode))
		toolbar.setNavigationContentDescription(R.string.shared_string_close)
		toolbar.setNavigationOnClickListener { v: View? ->
			val mapActivity = mapActivity
			requireActivity().onBackPressed()
			if (mapActivity != null) {
				val searchDialog = mapActivity.fragmentsHelper.quickSearchDialogFragment
				searchDialog?.show()
			}
		}
	}

	private fun setupVerticalNearbyList(view: View) {
		val verticalNearbyList = view.findViewById<RecyclerView>(R.id.vertical_nearby_list)
		val nearbyData = NearbyPlacesHelper.getDataCollection()
		verticalNearbyAdapter = NearbyPlacesAdapter(app, nearbyData, true, this)
		verticalNearbyList.layoutManager = LinearLayoutManager(requireContext())
		verticalNearbyList.adapter = verticalNearbyAdapter
		verticalNearbyAdapter.notifyDataSetChanged()
	}

	val mapActivity: MapActivity?
		get() {
			val activity = activity
			return if (activity is MapActivity) {
				activity
			} else {
				null
			}
		}

	companion object {
		val TAG: String = NearbyPlacesFragment::class.java.simpleName
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, NearbyPlacesFragment(), TAG)
					.commitAllowingStateLoss()
			}
		}
	}

	override fun onNearbyItemClicked(item: NearbyPlacePoint) {
		mapActivity?.let {
			showPointInContextMenu(it, item)
			hide()
		}
	}

	fun hide() {
		val transaction = activity?.supportFragmentManager?.beginTransaction()
		transaction?.hide(this)
		transaction?.commit()
	}
}