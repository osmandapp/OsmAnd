package net.osmand.plus.search.dialogs

import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.exploreplaces.ExplorePlacesFragment
import net.osmand.plus.views.mapwidgets.TopToolbarController

class ExplorePlacesNearbyToolbarController(val fragmentManager: FragmentManager) : TopToolbarController(TopToolbarControllerType.EXPLORE_PLACES_NEARBY) {
	init {
		setCloseBtnVisible(false)
		setSaveViewVisible(true)
		setSaveViewTextId(R.string.show_list)
		setOnSaveViewClickListener {
			ExplorePlacesFragment.showInstance(fragmentManager)
		}
		setOnBackButtonClickListener {
		}
	}
}