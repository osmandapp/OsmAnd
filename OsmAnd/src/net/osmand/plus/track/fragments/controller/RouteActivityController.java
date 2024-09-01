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
import net.osmand.plus.settings.fragments.customizable.CustomizableSingleSelectionDialogFragment;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.shared.gpx.primitives.RouteActivityGroup;

import java.util.Objects;

public class RouteActivityController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	private static final String PROCESS_ID = "select_route_activity";

	private static final String NONE_ACTIVITY_KEY = "none";

	private final RouteActivitySelectionHelper routeActivityHelper;

	public RouteActivityController(@NonNull OsmandApplication app,
	                               @NonNull RouteActivitySelectionHelper routeActivityHelper) {
		super(app);
		this.routeActivityHelper = routeActivityHelper;
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


		// None activity
		displayData.addDisplayItem(new DisplayItem()
				.setLayoutId(defLayoutId)
				.setTitle(getString(R.string.shared_string_none))
				.setTag(NONE_ACTIVITY_KEY)
		);
		// Divider
		displayData.addDisplayItem(new DisplayItem().setLayoutId(R.layout.list_item_divider_basic));

		// Categorized activities
		for (RouteActivityGroup group : routeActivityHelper.getActivityGroups()) {
			// Header
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
			// Divider
			displayData.addDisplayItem(new DisplayItem().setLayoutId(R.layout.list_item_divider_basic));
		}
		displayData.putExtra(BACKGROUND_COLOR, activeColor);

		int selectedIndex = 0;
		RouteActivity selectedActivity = routeActivityHelper.getSelectedRouteActivity();
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
			routeActivityHelper.onSelectRouteActivity(activity);
		} else if (Objects.equals(NONE_ACTIVITY_KEY, selected.getTag())) {
			routeActivityHelper.onSelectRouteActivity(null);
		}
		dialogManager.askDismissDialog(processId);
	}

	@Nullable
	public RouteActivitySelectionHelper getRouteActivityHelper() {
		return routeActivityHelper;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull RouteActivitySelectionHelper routeActivityHelper) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new RouteActivityController(app, routeActivityHelper));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableSingleSelectionDialogFragment.showInstance(fragmentManager, PROCESS_ID);
	}

	@Nullable
	public static RouteActivityController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (RouteActivityController) dialogManager.findController(PROCESS_ID);
	}
}
