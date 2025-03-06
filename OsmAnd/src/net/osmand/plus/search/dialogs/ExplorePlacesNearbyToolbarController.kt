package net.osmand.plus.search.dialogs

import androidx.fragment.app.FragmentManager
import net.osmand.plus.exploreplaces.ExplorePlacesFragment
import net.osmand.plus.views.mapwidgets.TopToolbarController

class ExplorePlacesNearbyToolbarController(val fragmentManager: FragmentManager) : TopToolbarController(TopToolbarControllerType.EXPLORE_PLACES_NEARBY) {
	init {
		setCloseBtnVisible(true)
		setRefreshBtnVisible(true)
		setOnRefreshButtonClickListener {
			ExplorePlacesFragment.showInstance(fragmentManager)
		}
	}
}