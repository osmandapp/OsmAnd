package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.SortTargetPointsTask;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaypointDialogHelper {

	private final List<WaypointDialogHelperCallback> helperCallbacks = new ArrayList<>();

	public interface WaypointDialogHelperCallback {
		void reloadAdapter();

		void deleteWaypoint(int position);
	}

	public void addHelperCallback(WaypointDialogHelperCallback callback) {
		helperCallbacks.add(callback);
	}

	public void removeHelperCallback(WaypointDialogHelperCallback callback) {
		helperCallbacks.remove(callback);
	}

	public static void updatePointInfoView(@NonNull Activity activity, @NonNull View localView,
	                                       @NonNull LocationPointWrapper ps,
	                                       boolean mapCenter, boolean nightMode,
	                                       boolean edit, boolean topBar) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		WaypointHelper wh = app.getWaypointHelper();
		LocationPoint point = ps.getPoint();
		TextView text = localView.findViewById(R.id.waypoint_text);
		if (!topBar) {
			AndroidUtils.setTextPrimaryColor(activity, text, nightMode);
		}
		TextView textShadow = localView.findViewById(R.id.waypoint_text_shadow);
		if (!edit) {
			localView.setOnClickListener(view -> showOnMap(app, activity, point, mapCenter));
		}
		TextView textDist = localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(activity, app, nightMode));
		int dist = -1;
		boolean startPoint = ps.type == WaypointHelper.TARGETS && ((TargetPoint) ps.point).start;
		if (!startPoint) {
			if (!wh.isRouteCalculated()) {
				if (activity instanceof MapActivity) {
					dist = (int) MapUtils.getDistance(((MapActivity) activity).getMapView().getLatitude(), ((MapActivity) activity)
							.getMapView().getLongitude(), point.getLatitude(), point.getLongitude());
				}
			} else {
				dist = wh.getRouteDistance(ps);
			}
		}

		if (dist > 0) {
			textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
		} else {
			textDist.setText("");
		}

		TextView textDeviation = localView.findViewById(R.id.waypoint_deviation);
		if (textDeviation != null) {
			if (dist > 0 && ps.deviationDistance > 0) {
				String devStr = "+" + OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app);
				textDeviation.setText(devStr);
				if (!topBar) {
					int colorId = ColorUtilities.getSecondaryTextColorId(nightMode);
					AndroidUtils.setTextSecondaryColor(activity, textDeviation, nightMode);
					if (ps.deviationDirectionRight) {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_right, colorId),
								null, null, null);
					} else {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_left, colorId),
								null, null, null);
					}
				}
				textDeviation.setVisibility(View.VISIBLE);
			} else {
				textDeviation.setText("");
				textDeviation.setVisibility(View.GONE);
			}
		}

		String descr;
		PointDescription pd = point.getPointDescription(app);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}

		if (textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);

		String pointDescription = "";
		TextView descText = localView.findViewById(R.id.waypoint_desc_text);
		if (descText != null) {
			AndroidUtils.setTextSecondaryColor(activity, descText, nightMode);
			switch (ps.type) {
				case WaypointHelper.TARGETS:
					TargetPoint targetPoint = (TargetPoint) ps.point;
					if (targetPoint.start) {
						pointDescription = activity.getResources().getString(R.string.starting_point);
					} else {
						pointDescription = targetPoint.getPointDescription(activity).getTypeName();
					}
					break;

				case WaypointHelper.FAVORITES:
					FavouritePoint favPoint = (FavouritePoint) ps.point;
					pointDescription = Algorithms.isEmpty(favPoint.getCategory()) ? activity.getResources().getString(R.string.shared_string_favorites) : favPoint.getCategory();
					break;
			}
		}

		if (Algorithms.objectEquals(descr, pointDescription)) {
			pointDescription = "";
		}
		if (dist > 0 && !Algorithms.isEmpty(pointDescription)) {
			pointDescription = "  •  " + pointDescription;
		}
		if (descText != null) {
			descText.setText(pointDescription);
		}
	}

	public static void updateRouteInfoMenu(@Nullable Activity ctx) {
		if (ctx instanceof MapActivity) {
			((MapActivity) ctx).getMapRouteInfoMenu().updateMenu();
		}
	}

	public static void switchStartAndFinish(@NonNull MapActivity mapActivity, boolean updateRoute) {
		OsmandApplication app = mapActivity.getApp();
		TargetPointsHelper targetsHelper = app.getTargetPointsHelper();
		TargetPoint finish = targetsHelper.getPointToNavigate();
		TargetPoint start = targetsHelper.getPointToStart();
		if (finish == null) {
			app.showShortToastMessage(R.string.mark_final_location_first);
		} else {
			switchStartAndFinish(app, start, finish, updateRoute);
			updateControls(mapActivity);
		}
	}

	private static void switchStartAndFinish(@NonNull OsmandApplication app, @Nullable TargetPoint start,
	                                         @NonNull TargetPoint finish, boolean updateRoute) {
		TargetPointsHelper targetsHelper = app.getTargetPointsHelper();
		targetsHelper.setStartPoint(new LatLon(finish.getLatitude(), finish.getLongitude()),
				false, finish.getPointDescription(app));
		if (start == null) {
			Location loc = app.getLocationProvider().getLastKnownLocation();
			if (loc != null) {
				targetsHelper.navigateToPoint(new LatLon(loc.getLatitude(),
						loc.getLongitude()), updateRoute, -1);
			}
		} else {
			targetsHelper.navigateToPoint(new LatLon(start.getLatitude(),
					start.getLongitude()), updateRoute, -1, start.getPointDescription(app));
		}
	}

	public static void reverseAllPoints(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		TargetPointsHelper targetsHelper = app.getTargetPointsHelper();

		TargetPoint finish = targetsHelper.getPointToNavigate();
		TargetPoint start = targetsHelper.getPointToStart();
		if (finish != null) {
			switchStartAndFinish(app, start, finish, false);
		}

		List<TargetPoint> points = targetsHelper.getIntermediatePoints();
		Collections.reverse(points);
		targetsHelper.reorderIntermediatePoints(points, true);

		updateControls(mapActivity);
	}

	public static void updateControls(@NonNull MapActivity mapActivity) {
		WaypointDialogHelper helper = mapActivity.getDashboard().getWaypointDialogHelper();
		if (helper != null) {
			for (WaypointDialogHelperCallback callback : helper.helperCallbacks) {
				callback.reloadAdapter();
			}
		}
		updateRouteInfoMenu(mapActivity);
	}

	public static void updateAfterDeleteWaypoint(@NonNull MapActivity mapActivity, int position) {
		WaypointDialogHelper helper = mapActivity.getDashboard().getWaypointDialogHelper();
		if (helper != null) {
			for (WaypointDialogHelperCallback callback : helper.helperCallbacks) {
				callback.deleteWaypoint(position);
			}
		}
		updateRouteInfoMenu(mapActivity);
	}

	public static void clearAllIntermediatePoints(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		app.getTargetPointsHelper().clearAllIntermediatePoints(true);
		updateControls(mapActivity);
	}

	public static void replaceStartWithFirstIntermediate(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();

		List<TargetPoint> intermediatePoints = targetPointsHelper.getIntermediatePointsWithTarget();
		TargetPoint firstIntermediate = intermediatePoints.remove(0);
		LatLon latLon = new LatLon(firstIntermediate.getLatitude(), firstIntermediate.getLongitude());
		targetPointsHelper.setStartPoint(latLon, false, firstIntermediate.getPointDescription(mapActivity));
		targetPointsHelper.reorderAllTargetPoints(intermediatePoints, true);

		updateControls(mapActivity);
	}

	public static void deletePoint(@NonNull MapActivity mapActivity,
	                               @Nullable ArrayAdapter<Object> adapter, @NonNull Object item,
	                               @NonNull List<LocationPointWrapper> deletedPoints) {
		OsmandApplication app = mapActivity.getApp();
		if (item instanceof LocationPointWrapper point && adapter != null) {
			if (point.type == WaypointHelper.TARGETS && adapter instanceof StableArrayAdapter stableAdapter) {
				updateAfterDeleteWaypoint(mapActivity, stableAdapter.getPosition(point));
			} else {
				ArrayList<LocationPointWrapper> arr = new ArrayList<>();
				arr.add(point);
				app.getWaypointHelper().removeVisibleLocationPoint(arr);

				deletedPoints.add(point);

				adapter.setNotifyOnChange(false);
				adapter.remove(point);
				if (adapter instanceof StableArrayAdapter stableAdapter) {
					stableAdapter.getObjects().remove(item);
					stableAdapter.refreshData();
				}
				adapter.notifyDataSetChanged();
			}
		}
	}

	public static void showOnMap(OsmandApplication app, Activity a, LocationPoint locationPoint, boolean center) {
		if (!(a instanceof MapActivity)) {
			return;
		}
		Object object = locationPoint;
		if (locationPoint instanceof AmenityLocationPoint) {
			object = ((AmenityLocationPoint) locationPoint).getAmenity();
		}
		app.getSettings().setMapLocationToShow(locationPoint.getLatitude(), locationPoint.getLongitude(),
				15, locationPoint.getPointDescription(a), false, object);
		MapActivity.launchMapActivityMoveToTop(a);
	}

	@SuppressLint("StaticFieldLeak")
	public static void sortAllTargets(@NonNull MapActivity mapActivity) {
		OsmAndTaskManager.executeTask(new SortTargetPointsTask(mapActivity));
	}
}