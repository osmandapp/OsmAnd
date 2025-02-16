package net.osmand.plus.routepreparationmenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.OnCompleteCallback;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;

import java.util.ArrayList;
import java.util.List;

public class WaypointsDialogController extends BaseDialogController {

	private static final String PROCESS_ID = "manage_route_waypoints";

	private boolean useRouteInfoMenu;
	private List<Object> initialTargetPoints;

	public WaypointsDialogController(@NonNull OsmandApplication app) {
		super(app);
	}

	public void setUseRouteInfoMenu(boolean useRouteInfoMenu) {
		this.useRouteInfoMenu = useRouteInfoMenu;
	}

	public boolean isUseRouteInfoMenu() {
		return useRouteInfoMenu;
	}

	public void setInitialTargetPoints(@NonNull List<Object> initialTargetPoints) {
		if (this.initialTargetPoints == null) {
			this.initialTargetPoints = new ArrayList<>(initialTargetPoints);
		}
	}

	public void onClearClicked(@NonNull OnCompleteCallback callback) {
		app.getTargetPointsHelper().clearAllPoints(true);
		callback.onComplete();
	}

	public void onCancelChanges(@NonNull OnCompleteCallback callback) {
		onApplyChanges(initialTargetPoints, callback);
	}

	public void onApplyChanges(@Nullable List<Object> items, @NonNull OnCompleteCallback callback) {
		app.runInUIThread(() -> onApplyChangesImpl(items, callback), 50);
	}

	private void onApplyChangesImpl(@Nullable List<Object> items, @NonNull OnCompleteCallback callback) {
		List<TargetPoint> allTargets = new ArrayList<>();
		TargetPoint start = null;
		if (items != null) {
			for (Object obj : items) {
				if (obj instanceof LocationPointWrapper p) {
					if (p.getPoint() instanceof TargetPoint t) {
						if (t.start) {
							start = t;
						} else {
							t.intermediate = true;
						}
						allTargets.add(t);
					}
				}
			}
			if (!allTargets.isEmpty()) {
				allTargets.get(allTargets.size() - 1).intermediate = false;
			}
		}
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		if (start != null) {
			int startInd = allTargets.indexOf(start);
			TargetPoint first = allTargets.remove(0);
			if (startInd != 0) {
				start.start = false;
				start.intermediate = startInd != allTargets.size() - 1;
				if (targetPointsHelper.getPointToStart() == null) {
					start.getOriginalPointDescription().setName(PointDescription
							.getLocationNamePlain(app, start.getLatitude(), start.getLongitude()));
				}
				first.start = true;
				first.intermediate = false;
				targetPointsHelper.setStartPoint(new LatLon(first.getLatitude(), first.getLongitude()),
						false, first.getPointDescription(app));
			}
		}
		targetPointsHelper.reorderAllTargetPoints(allTargets, false);
		targetPointsHelper.updateRouteAndRefresh(true);
		callback.onComplete();
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	@Nullable
	public static WaypointsDialogController getInstance(@NonNull OsmandApplication app) {
		DialogManager manager = app.getDialogManager();
		return (WaypointsDialogController) manager.findController(PROCESS_ID);
	}

	@NonNull
	public static WaypointsDialogController createInstance(@NonNull OsmandApplication app,
	                                                       boolean useRouteInfoMenu) {
		WaypointsDialogController controller = new WaypointsDialogController(app);
		app.getDialogManager().register(PROCESS_ID, controller);
		controller.setUseRouteInfoMenu(useRouteInfoMenu);
		return controller;
	}
}
