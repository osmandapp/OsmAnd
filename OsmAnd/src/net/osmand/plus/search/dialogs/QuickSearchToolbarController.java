package net.osmand.plus.search.dialogs;

import static net.osmand.plus.search.ShowQuickSearchMode.CURRENT;
import static net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType.QUICK_SEARCH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;

public class QuickSearchToolbarController extends TopToolbarController {

	private final MapFragmentsHelper fragmentsHelper;
	private PoiUIFilter selectedFilter;

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
			}
		});
		setActionButtonIcons(R.drawable.ic_flat_list_dark, R.drawable.ic_flat_list_dark);
		setActionButtonVisible(getExplorePlacesFragment() != null);
	}

	private void showQuickSearch() {
		fragmentsHelper.closeExplore();
		fragmentsHelper.showQuickSearch(CURRENT, false);
	}

	@Override
	public void updateToolbar(@NonNull TopToolbarView toolbarView) {
		super.updateToolbar(toolbarView);

		boolean visible = getExplorePlacesFragment() != null;
		setActionButtonVisible(visible);
		AndroidUiHelper.updateVisibility(toolbarView.getActionButton(), visible);
	}

	@Nullable
	private ExplorePlacesFragment getExplorePlacesFragment() {
		return fragmentsHelper.getFragment(ExplorePlacesFragment.Companion.getTAG());
	}

	@Nullable
	public PoiUIFilter getSelectedFilter() {
		return selectedFilter;
	}

	public void setSelectedFilter(@Nullable PoiUIFilter filter) {
		selectedFilter = filter;
	}
}
