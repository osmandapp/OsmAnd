package net.osmand.plus.search.dialogs

import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.views.mapwidgets.TopToolbarController

class ExplorePlacesNearbyToolbarController(
	val app: OsmandApplication,
	val fragmentManager: FragmentManager) :
	TopToolbarController(TopToolbarControllerType.EXPLORE_PLACES_NEARBY) {
	init {
		setCloseBtnVisible(true)
		title = app.getString(R.string.popular_places)
		setRefreshBtnVisible(true)
		setRefreshBtnIconIds(
			R.drawable.ic_flat_list_dark,
			R.drawable.ic_flat_list_dark)
	}
}