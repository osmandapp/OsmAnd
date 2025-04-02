package net.osmand.plus.search.dialogs;

import static net.osmand.plus.search.ShowQuickSearchMode.CURRENT;
import static net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType.QUICK_SEARCH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.util.Algorithms;

import java.util.Set;

public class QuickSearchToolbarController extends TopToolbarController {

	private final MapFragmentsHelper fragmentsHelper;

	public QuickSearchToolbarController(@NonNull MapActivity activity) {
		super(QUICK_SEARCH);

		fragmentsHelper = activity.getFragmentsHelper();

		setOnBackButtonClickListener(v -> showQuickSearch());
		setOnTitleClickListener(v -> showQuickSearch());
		setOnCloseButtonClickListener(v -> {
			fragmentsHelper.closeExplore();
			fragmentsHelper.closeQuickSearch();
		});

		setOnCloseToolbarListener(fragmentsHelper::closeExplore);
		setOnActionButtonClickListener(v -> {
			ExplorePlacesFragment fragment = getExplorePlacesFragment();
			if (fragment != null) {
				fragment.toggleState();
			} else {
				OsmandApplication app = activity.getMyApplication();
				Set<PoiUIFilter> filters = app.getPoiFilters().getOverwrittenPoiFilters();
				if (!Algorithms.isEmpty(filters)) {
					PoiUIFilter filter = filters.iterator().next();
					FragmentManager manager = activity.getSupportFragmentManager();
					ExplorePlacesFragment.Companion.showInstance(manager, filter);
				}
			}
		});
		setActionButtonIcons(R.drawable.ic_flat_list_dark, R.drawable.ic_flat_list_dark);
		setActionButtonVisible(true);
	}

	private void showQuickSearch() {
		fragmentsHelper.closeExplore();
		fragmentsHelper.showQuickSearch(CURRENT, false);
	}

	@Nullable
	private ExplorePlacesFragment getExplorePlacesFragment() {
		return fragmentsHelper.getFragment(ExplorePlacesFragment.Companion.getTAG());
	}
}
