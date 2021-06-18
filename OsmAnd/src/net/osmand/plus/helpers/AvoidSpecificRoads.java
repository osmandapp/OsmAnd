package net.osmand.plus.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RouteSegmentSearchResult;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvoidSpecificRoads {

	private static final float MAX_AVOID_ROUTE_SEARCH_RADIUS_DP = 32f;

	private final OsmandApplication app;
	private final Map<LatLon, AvoidRoadInfo> impassableRoads = new LinkedHashMap<>();

	public AvoidSpecificRoads(final OsmandApplication app) {
		this.app = app;
		loadImpassableRoads();
	}

	public long getLastModifiedTime() {
		return app.getSettings().getImpassableRoadsLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		app.getSettings().setImpassableRoadsLastModifiedTime(lastModifiedTime);
	}

	public void loadImpassableRoads() {
		for (AvoidRoadInfo avoidRoadInfo : app.getSettings().getImpassableRoadPoints()) {
			impassableRoads.put(new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude), avoidRoadInfo);
		}
	}

	public Map<LatLon, AvoidRoadInfo> getImpassableRoads() {
		return impassableRoads;
	}

	public void initRouteObjects(boolean force) {
		for (Map.Entry<LatLon, AvoidRoadInfo> entry : impassableRoads.entrySet()) {
			AvoidRoadInfo roadInfo = entry.getValue();
			if (roadInfo.id != 0) {
				for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
					if (force) {
						builder.removeImpassableRoad(roadInfo.id);
					} else {
						builder.addImpassableRoad(roadInfo.id);
					}
				}
			}
			if (force || roadInfo.id == 0) {
				addImpassableRoad(null, entry.getKey(), false, true, roadInfo.appModeKey);
			}
		}
	}

	private ArrayAdapter<AvoidRoadInfo> createAdapter(MapActivity mapActivity, boolean nightMode) {
		final ArrayList<AvoidRoadInfo> points = new ArrayList<>(impassableRoads.values());
		final LatLon mapLocation = mapActivity.getMapLocation();
		final LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);

		return new ArrayAdapter<AvoidRoadInfo>(themedContext, R.layout.waypoint_reached, R.id.title, points) {
			@NonNull
			@Override
			public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
				View v = convertView;
				if (v == null || v.findViewById(R.id.info_close) == null) {
					v = inflater.inflate(R.layout.waypoint_reached, parent, false);
				}
				final AvoidRoadInfo item = getItem(position);
				v.findViewById(R.id.all_points).setVisibility(View.GONE);
				((ImageView) v.findViewById(R.id.waypoint_icon))
						.setImageDrawable(getIcon(R.drawable.ic_action_road_works_dark));

				LatLon latLon = item != null ? new LatLon(item.latitude, item.longitude) : null;
				String name = item != null ? item.name : app.getString(R.string.shared_string_road);
				((TextView) v.findViewById(R.id.waypoint_dist)).setText(getDist(mapLocation, latLon));
				((TextView) v.findViewById(R.id.waypoint_text)).setText(name);
				ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
				remove.setVisibility(View.VISIBLE);
				remove.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark));
				remove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						remove(item);
						removeImpassableRoad(item);
						notifyDataSetChanged();
						recalculateRoute(item != null ? item.appModeKey : null);
					}
				});
				return v;
			}
		};
	}

	private Drawable getIcon(@DrawableRes int iconId) {
		return app.getUIUtilities().getThemedIcon(iconId);
	}

	private String getDist(@NonNull LatLon loc, @Nullable LatLon point) {
		double dist = point == null ? 0 : MapUtils.getDistance(loc, point);
		return OsmAndFormatter.getFormattedDistance((float) dist, app);
	}

	public String getText(@Nullable LatLon point) {
		if (point != null) {
			AvoidRoadInfo obj = impassableRoads.get(point);
			if (obj != null && !TextUtils.isEmpty(obj.name)) {
				return obj.name;
			}
		}
		return app.getString(R.string.shared_string_road);
	}

	public String getRoadName(@Nullable RouteDataObject obj) {
		if (obj != null) {
			String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
			boolean transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
			String name = RoutingHelperUtils.formatStreetName(
					obj.getName(locale, transliterate),
					obj.getRef(locale, transliterate, true),
					obj.getDestinationName(locale, transliterate, true),
					app.getString(R.string.towards)
			);
			if (!TextUtils.isEmpty(name)) {
				return name;
			}
		}
		return app.getString(R.string.shared_string_road);
	}

	private void recalculateRoute(@Nullable String appModeKey) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(appModeKey, null);
		app.getRoutingHelper().onSettingsChanged(mode);
	}

	public void removeImpassableRoad(LatLon latLon) {
		app.getSettings().removeImpassableRoad(latLon);
		AvoidRoadInfo obj = impassableRoads.remove(latLon);
		if (obj != null) {
			for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
				builder.removeImpassableRoad(obj.id);
			}
		}
	}

	public void removeImpassableRoad(AvoidRoadInfo obj) {
		removeImpassableRoad(getLocation(obj));
	}

	public void showDialog(@NonNull final MapActivity mapActivity, final @Nullable ApplicationMode mode) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		Context themedContext = UiUtilities.getThemedContext(mapActivity, nightMode);

		AlertDialog.Builder bld = new AlertDialog.Builder(themedContext);
		bld.setTitle(R.string.impassable_road);
		if (impassableRoads.isEmpty()) {
			bld.setMessage(R.string.avoid_roads_msg);
		} else {
			final ArrayAdapter<AvoidRoadInfo> listAdapter = createAdapter(mapActivity, nightMode);
			bld.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AvoidRoadInfo point = listAdapter.getItem(which);
					if (point != null) {
						showOnMap(mapActivity, point.latitude, point.longitude, point.name);
					}
					dialog.dismiss();
				}

			});
		}
		bld.setPositiveButton(R.string.shared_string_select_on_map, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				selectFromMap(mapActivity, mode);
			}
		});
		bld.setNegativeButton(R.string.shared_string_close, null);
		bld.show();
	}

	public void selectFromMap(@NonNull final MapActivity mapActivity) {
		ContextMenuLayer cm = mapActivity.getMapLayers().getContextMenuLayer();
		cm.setSelectOnMap(new CallbackWithObject<LatLon>() {
			@Override
			public boolean processResult(LatLon result) {
				addImpassableRoad(mapActivity, result, true, false, null);
				return true;
			}
		});
	}

	public void selectFromMap(@NonNull final MapActivity mapActivity, @Nullable final ApplicationMode mode) {
		ContextMenuLayer cm = mapActivity.getMapLayers().getContextMenuLayer();
		cm.setSelectOnMap(new CallbackWithObject<LatLon>() {
			@Override
			public boolean processResult(LatLon result) {
				addImpassableRoad(mapActivity, result, true, false, mode != null ? mode.getStringKey() : null);
				return true;
			}
		});
	}

	public void addImpassableRoad(@Nullable final MapActivity mapActivity,
	                              @NonNull final LatLon loc,
	                              final boolean showDialog,
	                              final boolean skipWritingSettings,
	                              @Nullable final String appModeKey) {
		final Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());

		ApplicationMode defaultAppMode = app.getRoutingHelper().getAppMode();
		final ApplicationMode appMode = appModeKey != null ? ApplicationMode.valueOfStringKey(appModeKey, defaultAppMode) : defaultAppMode;

		List<RouteSegmentResult> roads = app.getMeasurementEditingContext() != null
				? app.getMeasurementEditingContext().getRoadSegmentData(appMode)
				: app.getRoutingHelper().getRoute().getOriginalRoute();
		if (mapActivity != null && roads != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			float maxDistPx = MAX_AVOID_ROUTE_SEARCH_RADIUS_DP * tb.getDensity();
			RouteSegmentSearchResult searchResult =
					RouteSegmentSearchResult.searchRouteSegment(loc.getLatitude(), loc.getLongitude(), maxDistPx / tb.getPixDensity(), roads);
			if (searchResult != null) {
				QuadPoint point = searchResult.getPoint();
				LatLon newLoc = new LatLon(MapUtils.get31LatitudeY((int) point.y), MapUtils.get31LongitudeX((int) point.x));
				ll.setLatitude(newLoc.getLatitude());
				ll.setLongitude(newLoc.getLongitude());

				RouteDataObject object = roads.get(searchResult.getRoadIndex()).getObject();
				AvoidRoadInfo avoidRoadInfo = getAvoidRoadInfoForDataObject(object, newLoc.getLatitude(), newLoc.getLongitude(), appMode.getStringKey());
				addImpassableRoadInternal(avoidRoadInfo, showDialog, mapActivity, newLoc);
				if (!skipWritingSettings) {
					app.getSettings().addImpassableRoad(avoidRoadInfo);
				}
				return;
			}
		}
		app.getLocationProvider().getRouteSegment(ll, appMode, false, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					if (mapActivity != null) {
						Toast.makeText(mapActivity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					}
				} else {
					AvoidRoadInfo avoidRoadInfo = getAvoidRoadInfoForDataObject(object, ll.getLatitude(), ll.getLongitude(), appMode.getStringKey());
					addImpassableRoadInternal(avoidRoadInfo, showDialog, mapActivity, loc);
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
		if (!skipWritingSettings) {
			AvoidRoadInfo avoidRoadInfo = getAvoidRoadInfoForDataObject(null, loc.getLatitude(), loc.getLongitude(), appMode.getStringKey());
			app.getSettings().addImpassableRoad(avoidRoadInfo);
		}
	}

	public void replaceImpassableRoad(final MapActivity activity,
	                                  final AvoidRoadInfo currentObject,
									  final LatLon newLoc,
									  final boolean showDialog,
									  final AvoidSpecificRoadsCallback callback) {
		final Location ll = new Location("");
		ll.setLatitude(newLoc.getLatitude());
		ll.setLongitude(newLoc.getLongitude());

		ApplicationMode defaultAppMode = app.getRoutingHelper().getAppMode();
		final ApplicationMode appMode = ApplicationMode.valueOfStringKey(currentObject.appModeKey, defaultAppMode);

		app.getLocationProvider().getRouteSegment(ll, appMode, false, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					final LatLon oldLoc = getLocation(currentObject);
					app.getSettings().moveImpassableRoad(oldLoc, newLoc);
					impassableRoads.remove(oldLoc);
					for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
						builder.removeImpassableRoad(currentObject.id);
					}
					AvoidRoadInfo avoidRoadInfo = getAvoidRoadInfoForDataObject(object, newLoc.getLatitude(), newLoc.getLongitude(), appMode.getStringKey());

					addImpassableRoadInternal(avoidRoadInfo, showDialog, activity, newLoc);
					if (callback != null) {
						callback.onAddImpassableRoad(true, avoidRoadInfo);
					}
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return callback != null && callback.isCancelled();
			}
		});
	}

	private void addImpassableRoadInternal(@NonNull AvoidRoadInfo avoidRoadInfo,
										   boolean showDialog,
										   @Nullable MapActivity activity,
										   @NonNull LatLon loc) {
		boolean roadAdded = false;
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			roadAdded |= builder.addImpassableRoad(avoidRoadInfo.id);
		}
		if (roadAdded) {
			app.getSettings().updateImpassableRoadInfo(avoidRoadInfo);
			impassableRoads.put(loc, avoidRoadInfo);
		} else {
			LatLon location = getLocation(avoidRoadInfo);
			if (location != null) {
				app.getSettings().removeImpassableRoad(location);
			}
		}
		recalculateRoute(avoidRoadInfo.appModeKey);
		if (activity != null) {
			if (showDialog) {
				showDialog(activity, ApplicationMode.valueOfStringKey(avoidRoadInfo.appModeKey, null));
			}
			MapContextMenu menu = activity.getContextMenu();
			if (menu.isActive()) {
				menu.close();
			}
			activity.refreshMap();
		}
	}

	private void showOnMap(MapActivity ctx, double lat, double lon, String name) {
		int zoom = ctx.getMapView().getZoom() < 15 ? 15 : ctx.getMapView().getZoom();
		PointDescription pd = new PointDescription("", name);
		ctx.getMyApplication().getSettings().setMapLocationToShow(lat, lon, zoom, pd, false, null);
		MapActivity.launchMapActivityMoveToTop(ctx);
	}

	public LatLon getLocation(AvoidRoadInfo avoidRoadInfo) {
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			if (builder.getImpassableRoadLocations().contains(avoidRoadInfo.id)) {
				return new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude);
			}
		}
		return null;
	}

	public interface AvoidSpecificRoadsCallback {

		void onAddImpassableRoad(boolean success, AvoidRoadInfo avoidRoadInfo);

		boolean isCancelled();
	}

	private AvoidRoadInfo getAvoidRoadInfoForDataObject(@Nullable RouteDataObject object, double lat, double lon, String appModeKey) {
		AvoidRoadInfo avoidRoadInfo = impassableRoads.get(new LatLon(lat, lon));
		if (avoidRoadInfo == null) {
			avoidRoadInfo = new AvoidRoadInfo();
		}
		if (object != null) {
			avoidRoadInfo.id = object.id;
//			avoidRoadInfo.direction = object.directionRoute(0, true);
			avoidRoadInfo.direction = Double.NaN;
		} else {
			avoidRoadInfo.id = 0;
			avoidRoadInfo.direction = Double.NaN;
		}
		avoidRoadInfo.latitude = lat;
		avoidRoadInfo.longitude = lon;
		avoidRoadInfo.appModeKey = appModeKey;
		avoidRoadInfo.name = getRoadName(object);
		return avoidRoadInfo;
	}

	public static class AvoidRoadInfo {
		public long id;
		public double direction = Double.NaN;
		public double latitude;
		public double longitude;
		public String name;
		public String appModeKey;

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;

			AvoidRoadInfo other = (AvoidRoadInfo) obj;
			return Math.abs(latitude - other.latitude) < 0.00001
					&& Math.abs(longitude - other.longitude) < 0.00001
					&& Algorithms.objectEquals(name, other.name)
					&& Algorithms.objectEquals(appModeKey, other.appModeKey)
					&& id == other.id;
		}
	}
}