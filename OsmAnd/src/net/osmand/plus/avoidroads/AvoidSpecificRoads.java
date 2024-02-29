package net.osmand.plus.avoidroads;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RouteSegmentSearchResult;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvoidSpecificRoads {

	private static final float MAX_AVOID_ROUTE_SEARCH_RADIUS_DP = 32f;

	private final OsmandApplication app;
	private final Map<LatLon, AvoidRoadInfo> impassableRoads = new LinkedHashMap<>();

	public AvoidSpecificRoads(@NonNull OsmandApplication app) {
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

	protected void recalculateRoute(@Nullable String appModeKey) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(appModeKey, null);
		app.getRoutingHelper().onSettingsChanged(mode);
	}

	public void removeImpassableRoad(@NonNull LatLon latLon) {
		app.getSettings().removeImpassableRoad(latLon);
		AvoidRoadInfo obj = impassableRoads.remove(latLon);
		if (obj != null) {
			for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
				builder.removeImpassableRoad(obj.id);
			}
		}
	}

	public void removeImpassableRoad(@NonNull AvoidRoadInfo info) {
		removeImpassableRoad(new LatLon(info.latitude, info.longitude));
	}

	public void selectFromMap(@NonNull MapActivity mapActivity, @Nullable ApplicationMode mode) {
		ContextMenuLayer menuLayer = mapActivity.getMapLayers().getContextMenuLayer();
		menuLayer.setSelectOnMap(result -> {
			addImpassableRoad(mapActivity, result, true, false, mode != null ? mode.getStringKey() : null);
			return true;
		});
	}

	public void addImpassableRoad(@Nullable MapActivity mapActivity, @NonNull LatLon loc,
	                              boolean showDialog, boolean skipWritingSettings, @Nullable String appModeKey) {
		Location ll = new Location("");
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());

		ApplicationMode defaultAppMode = app.getRoutingHelper().getAppMode();
		ApplicationMode appMode = appModeKey != null ? ApplicationMode.valueOfStringKey(appModeKey, defaultAppMode) : defaultAppMode;

		List<RouteSegmentResult> roads = app.getMeasurementEditingContext() != null
				? app.getMeasurementEditingContext().getRoadSegmentData(appMode)
				: app.getRoutingHelper().getRoute().getOriginalRoute();
		if (mapActivity != null && roads != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			float maxDistPx = MAX_AVOID_ROUTE_SEARCH_RADIUS_DP * tb.getDensity();
			RouteSegmentSearchResult searchResult =
					RouteSegmentSearchResult.searchRouteSegment(loc.getLatitude(), loc.getLongitude(), maxDistPx / tb.getPixDensity(), roads);
			if (searchResult != null) {
				QuadPointDouble point = searchResult.getPoint();
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

	public void replaceImpassableRoad(MapActivity activity,
	                                  AvoidRoadInfo currentObject,
	                                  LatLon newLoc,
	                                  boolean showDialog,
	                                  AvoidSpecificRoadsCallback callback) {
		Location ll = new Location("");
		ll.setLatitude(newLoc.getLatitude());
		ll.setLongitude(newLoc.getLongitude());

		ApplicationMode defaultAppMode = app.getRoutingHelper().getAppMode();
		ApplicationMode appMode = ApplicationMode.valueOfStringKey(currentObject.appModeKey, defaultAppMode);

		app.getLocationProvider().getRouteSegment(ll, appMode, false, new ResultMatcher<RouteDataObject>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					Toast.makeText(activity, R.string.error_avoid_specific_road, Toast.LENGTH_LONG).show();
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					LatLon oldLoc = getLocation(currentObject);
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
			if (!builder.getImpassableRoadLocations().contains(avoidRoadInfo.id)) {
				builder.addImpassableRoad(avoidRoadInfo.id);
				roadAdded = true;
			}
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
				AvoidRoadsDialog.showDialog(activity, ApplicationMode.valueOfStringKey(roadInfo.getAppModeKey(), null));
			}
			MapContextMenu menu = activity.getContextMenu();
			if (menu.isActive()) {
				menu.close();
			}
			activity.refreshMap();
		}
	}

	@Nullable
	public LatLon getLocation(@NonNull AvoidRoadInfo avoidRoadInfo) {
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

	@NonNull
	private AvoidRoadInfo getAvoidRoadInfoForDataObject(@Nullable RouteDataObject object, double lat, double lon, String appModeKey) {
		AvoidRoadInfo avoidRoadInfo = impassableRoads.get(new LatLon(lat, lon));
		if (avoidRoadInfo == null) {
			avoidRoadInfo = new AvoidRoadInfo();
		}
		if (object != null) {
			avoidRoadInfo.id = object.id;
//			avoidRoadInfo.direction = object.directionRoute(0, true);
		} else {
			avoidRoadInfo.id = 0;
		}
		avoidRoadInfo.direction = Double.NaN;
		avoidRoadInfo.latitude = lat;
		avoidRoadInfo.longitude = lon;
		avoidRoadInfo.appModeKey = appModeKey;
		avoidRoadInfo.name = getRoadName(object);

		return avoidRoadInfo;
	}
}