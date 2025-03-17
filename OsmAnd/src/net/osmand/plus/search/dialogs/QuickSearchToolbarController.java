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
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarView;

public class QuickSearchToolbarController extends TopToolbarController {

	private final MapFragmentsHelper fragmentsHelper;

	public QuickSearchToolbarController(@NonNull MapActivity activity) {
		super(QUICK_SEARCH);

		fragmentsHelper = activity.getFragmentsHelper();

		setOnBackButtonClickListener(v -> showQuickSearch());
		setOnTitleClickListener(v -> showQuickSearch());
		setOnCloseButtonClickListener(v -> fragmentsHelper.closeQuickSearch());

		setOnCloseToolbarListener(() -> {
			ExplorePlacesFragment fragment = getExplorePlacesFragment();
			if (fragment != null) {
				fragment.closeFragment();
			}
		});
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
}
