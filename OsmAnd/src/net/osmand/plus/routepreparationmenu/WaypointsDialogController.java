package net.osmand.plus.routepreparationmenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.Location;
import net.osmand.OnCompleteCallback;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogProgressChanged;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.MapRouteCalculationProgressListener;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WaypointsDialogController extends BaseDialogController implements IDialogProgressChanged {

	private static final String PROCESS_ID = "manage_route_waypoints";

	private final WaypointHelper waypointHelper;
	private final TargetPointsHelper targetPointsHelper;
	private boolean useRouteInfoMenu;
	private final List<Object> initialTargetPoints;

	public WaypointsDialogController(@NonNull OsmandApplication app) {
		super(app);
		this.waypointHelper = app.getWaypointHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
		this.initialTargetPoints = getActivePoints();
	}

	public void bindDialog(@NonNull IDialog dialog) {
		dialogManager.register(PROCESS_ID, dialog);
	}

	public void setUseRouteInfoMenu(boolean useRouteInfoMenu) {
		this.useRouteInfoMenu = useRouteInfoMenu;
	}

	public boolean isUseRouteInfoMenu() {
		return useRouteInfoMenu;
	}

	@Override
	public void onDialogProgressChanged(@NonNull String progressTag, int progress) {
		if (Objects.equals(progressTag, MapRouteCalculationProgressListener.TAG)) {
			if (getDialog() instanceof WaypointsFragment fragment) {
				fragment.updateRouteCalculationProgress(progress);
			}
		}
	}

	public void onClearClicked(@NonNull OnCompleteCallback callback) {
		targetPointsHelper.clearAllPoints(true);
		callback.onComplete();
	}

	public void onCancelChanges(@NonNull OnCompleteCallback callback) {
		onApplyChanges(initialTargetPoints, callback);
	}

	public void onApplyChanges(@Nullable List<Object> items) {
		onApplyChanges(items, () -> {});
	}

	public void onApplyChanges(@Nullable List<Object> items, @NonNull OnCompleteCallback callback) {
		if (hasChangesToApply(items)) {
			app.runInUIThread(() -> onApplyChangesImpl(items, callback), 50);
		} else {
			callback.onComplete();
		}
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

	private boolean hasChangesToApply(@Nullable List<Object> items) {
		List<Object> activePoints = getActivePoints();
		return !Objects.equals(items, activePoints);
	}

	@NonNull
	public List<Object> getActivePoints() {
		return getActivePoints(getTargetPoints());
	}

	@NonNull
	public List<Object> getActivePoints(@NonNull List<Object> points) {
		List<Object> activePoints = new ArrayList<>();
		for (Object p : points) {
			if (p instanceof LocationPointWrapper w) {
				if (w.type == WaypointHelper.TARGETS) {
					activePoints.add(p);
				}
			}
		}
		return activePoints;
	}

	@NonNull
	public List<Object> getTargetPoints() {
		List<Object> points = new ArrayList<>();
		for (int i = 0; i < WaypointHelper.MAX; i++) {
			List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
			if ((i == WaypointHelper.WAYPOINTS || i == WaypointHelper.TARGETS) && waypointHelper.isTypeVisible(i)) {
				if (i == WaypointHelper.TARGETS) {
					TargetPoint start = targetPointsHelper.getPointToStart();
					if (start == null) {
						LatLon latLon;
						Location loc = app.getLocationProvider().getLastKnownLocation();
						if (loc != null) {
							latLon = new LatLon(loc.getLatitude(), loc.getLongitude());
						} else {
							OsmandMapTileView mapView = app.getOsmandMap().getMapView();
							latLon = new LatLon(mapView.getLatitude(), mapView.getLongitude());
						}
						start = TargetPoint.createStartPoint(latLon,
								new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
										getString(R.string.shared_string_my_location)));
					} else {
						LatLon latLon = new LatLon(start.getLatitude(), start.getLongitude());
						String name = !start.getOnlyName().isEmpty() ? start.getOnlyName()
								: (getString(R.string.route_descr_map_location)
								+ " " + getString(R.string.route_descr_lat_lon, latLon.getLatitude(), latLon.getLongitude()));
						PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_LOCATION, name);
						start = TargetPoint.createStartPoint(latLon, pd);
					}
					points.add(new LocationPointWrapper(WaypointHelper.TARGETS, start, 0f, 0));

				}
				if (tp != null && !tp.isEmpty()) {
					points.addAll(tp);
				}
			}
		}
		return points;
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
