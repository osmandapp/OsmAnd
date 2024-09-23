package net.osmand.plus.track.fragments.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.plus.track.fragments.SelectRouteActivityFragment;
import net.osmand.plus.track.helpers.RouteActivitySearchFilter;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.RouteActivityGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectRouteActivityController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	private static final String PROCESS_ID = "select_route_activity";

	private static final String NONE_ACTIVITY_KEY = "none";

	private final RouteActivitySelectionHelper routeActivitySelectionHelper;
	private final RouteActivityHelper routeActivityHelper;
	private final RouteActivitySearchFilter searchFilter;
	private List<DisplayItem> lastSearchResults = new ArrayList<>();
	private boolean inSearchMode;

	public SelectRouteActivityController(@NonNull OsmandApplication app,
	                                     @NonNull RouteActivitySelectionHelper routeActivitySelectionHelper) {
		super(app);
		this.routeActivityHelper = app.getRouteActivityHelper();
		this.routeActivitySelectionHelper = routeActivitySelectionHelper;
		this.searchFilter = createSearchFilter();
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		UiUtilities iconsCache = app.getUIUtilities();
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defLayoutId = R.layout.bottom_sheet_item_with_radio_btn;

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.shared_string_activity));
		displayData.putExtra(BACKGROUND_COLOR, activeColor);

		if (!isInSearchMode()) {
			displayData.addDisplayItem(new DisplayItem()
					.setLayoutId(defLayoutId)
					.setTitle(getString(R.string.shared_string_none))
					.setTag(NONE_ACTIVITY_KEY)
			);
			displayData.addDisplayItem(new DisplayItem().setLayoutId(R.layout.list_item_divider_basic));

			// Categorized activities
			for (RouteActivityGroup group : routeActivityHelper.getActivityGroups()) {
				displayData.addDisplayItem(new DisplayItem()
						.setLayoutId(R.layout.list_item_header_48dp)
						.setTitle(group.getLabel())
				);
				// Activities of the category
				for (RouteActivity routeActivity : group.getActivities()) {
					displayData.addDisplayItem(new DisplayItem()
							.setLayoutId(defLayoutId)
							.setTitle(routeActivity.getLabel())
							.setNormalIcon(iconsCache.getThemedIcon(AndroidUtils.getIconId(app, routeActivity.getIconName())))
							.setControlsColor(activeColor)
							.setTag(routeActivity)
					);
				}
				displayData.addDisplayItem(new DisplayItem().setLayoutId(R.layout.list_item_divider_basic));
			}
		} else {
			int size = lastSearchResults.size();
			if (size > 0) {
				lastSearchResults.get(size - 1).hideBottomDivider();
			}
			displayData.addAllDisplayItems(lastSearchResults);
		}

		int selectedIndex = -1;
		RouteActivity selectedActivity = routeActivitySelectionHelper.getSelectedActivity();
		if (selectedActivity != null) {
			for (int i = 0; i < displayData.getItemsSize(); i++) {
				DisplayItem item = displayData.getItemAt(i);
				if (item.getTag() instanceof RouteActivity activity) {
					if (Objects.equals(selectedActivity.getId(), activity.getId())) {
						selectedIndex = i;
					}
				}
			}
		}
		displayData.putExtra(SELECTED_INDEX, selectedIndex);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		if (selected.getTag() instanceof RouteActivity activity) {
			routeActivitySelectionHelper.onSelectRouteActivity(activity);
		} else if (Objects.equals(NONE_ACTIVITY_KEY, selected.getTag())) {
			routeActivitySelectionHelper.onSelectRouteActivity(null);
		}
		dialogManager.askDismissDialog(processId);
	}

	@NonNull
	private RouteActivitySearchFilter createSearchFilter() {
		RouteActivitySearchFilter filter = new RouteActivitySearchFilter(filteredActivities -> {
			boolean nightMode = isNightMode();
			UiUtilities iconsCache = app.getUIUtilities();
			int activeColor = ColorUtilities.getActiveColor(app, nightMode);

			List<DisplayItem> searchResults = new ArrayList<>();
			for (RouteActivity activity : filteredActivities) {
				int iconId = AndroidUtils.getIconId(app, activity.getIconName());
				searchResults.add(new DisplayItem()
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_radio_btn)
						.setTitle(activity.getLabel())
						.setDescription(activity.getGroup().getLabel())
						.setNormalIcon(iconsCache.getThemedIcon(iconId))
						.setControlsColor(activeColor)
						.setShowBottomDivider(true, 0)
						.setTag(activity)
				);
			}
			this.lastSearchResults = searchResults;
			SelectRouteActivityFragment screen = getRouteActivityScreen();
			if (screen != null) {
				screen.askUpdateContent();
			}
		});
		filter.setItems(routeActivityHelper.getActivities());
		return filter;
	}

	public void enterSearchMode() {
		inSearchMode = true;
		SelectRouteActivityFragment screen = getRouteActivityScreen();
		if (screen != null) {
			screen.onScreenModeChanged();
		}
	}

	public void exitSearchMode() {
		inSearchMode = false;
		resetSearchResults();
		SelectRouteActivityFragment screen = getRouteActivityScreen();
		if (screen != null) {
			screen.onScreenModeChanged();
		}
	}

	public void clearSearchQuery() {
		resetSearchResults();
	}

	public void searchActivities(String text) {
		searchFilter.filter(text);
	}

	private void resetSearchResults() {
		lastSearchResults = new ArrayList<>();
	}

	public boolean isInSearchMode() {
		return inSearchMode;
	}

	@Nullable
	private SelectRouteActivityFragment getRouteActivityScreen() {
		return (SelectRouteActivityFragment) getDialog();
	}

	@Nullable
	public RouteActivitySelectionHelper getRouteActivityHelper() {
		return routeActivitySelectionHelper;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull RouteActivitySelectionHelper routeActivitySelectionHelper) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new SelectRouteActivityController(app, routeActivitySelectionHelper));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		SelectRouteActivityFragment.showInstance(fragmentManager, PROCESS_ID);
	}

	@Nullable
	public static SelectRouteActivityController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (SelectRouteActivityController) dialogManager.findController(PROCESS_ID);
	}
}
