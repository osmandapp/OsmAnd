package net.osmand.plus.track.fragments.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;
import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.OnCompleteCallback;
import net.osmand.gpx.GPXActivityUtils;
import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.fragments.customizable.CustomizableSingleSelectionDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;

public class RouteActivityController extends BaseDialogController
		implements IDisplayDataProvider, IDialogItemSelected {

	private static final String PROCESS_ID = "select_route_activity";

	private final Metadata metadata;
	private final RouteKey routeKey;
	private OnCompleteCallback onSelectionCompleted;

	public RouteActivityController(@NonNull OsmandApplication app,
	                               @NonNull Metadata metadata, @Nullable RouteKey routeKey,
	                               @NonNull OnCompleteCallback onSelectionCompleted) {
		super(app);
		this.metadata = metadata;
		this.routeKey = routeKey;
		setOnSelectionCompletedCallback(onSelectionCompleted);
	}

	public void setOnSelectionCompletedCallback(@NonNull OnCompleteCallback onSelectionCompleted) {
		this.onSelectionCompleted = onSelectionCompleted;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		UiUtilities iconsCache = app.getUIUtilities();
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(TITLE, getString(R.string.shared_string_activity));

		int index = 0;
		int selectedIndex = -1;
		OsmRouteType selectedType = GPXActivityUtils.fetchActivityType(metadata, routeKey);

		for (OsmRouteType routeType : OsmRouteType.allPossibleValues()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(AndroidUtils.getActivityTypeTitle(app, routeType))
					.setNormalIcon(iconsCache.getThemedIcon(AndroidUtils.getActivityTypeIcon(app, routeType)))
					.setControlsColor(activeColor)
					.setTag(routeType)
			);
			if (selectedType != null && selectedType == routeType) {
				selectedIndex = index;
			}
			index++;
		}
		displayData.putExtra(BACKGROUND_COLOR, activeColor);
		displayData.putExtra(SELECTED_INDEX, selectedIndex);
		return displayData;
	}

	@Override
	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected) {
		OsmRouteType selectedType = (OsmRouteType) selected.getTag();
		metadata.setActivity(selectedType.getName());
		dialogManager.askDismissDialog(processId);
		onSelectionCompleted.onComplete();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull Metadata metadata,
	                              @Nullable RouteKey routeKey, @NonNull OnCompleteCallback onSelectionCompleted) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new RouteActivityController(app, metadata, routeKey, onSelectionCompleted));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableSingleSelectionDialogFragment.showInstance(fragmentManager, PROCESS_ID);
	}

	@Nullable
	public static RouteActivityController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (RouteActivityController) dialogManager.findController(PROCESS_ID);
	}
}
