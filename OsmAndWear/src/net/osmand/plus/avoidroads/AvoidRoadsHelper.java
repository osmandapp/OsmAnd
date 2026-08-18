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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AvoidRoadsHelper {

	private static final float MAX_AVOID_ROUTE_SEARCH_RADIUS_DP = 32f;

	private final OsmandApplication app;
	private final DirectionPointsHelper pointsHelper;
	private final List<AvoidRoadInfo> impassableRoads = new LinkedList<>();

	public AvoidRoadsHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.pointsHelper = new DirectionPointsHelper(app);
		loadImpassableRoads();
	}

	@NonNull
	public DirectionPointsHelper getPointsHelper() {
		return pointsHelper;
	}

	public long getLastModifiedTime() {
		return app.getSettings().getImpassableRoadsLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		app.getSettings().setImpassableRoadsLastModifiedTime(lastModifiedTime);
	}

	public void loadImpassableRoads() {
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			builder.clearImpassableRoadLocations();
		}
		impassableRoads.clear();
		impassableRoads.addAll(app.getSettings().getImpassableRoadPoints());
	}

	@NonNull
	public List<AvoidRoadInfo> getImpassableRoads() {
		return impassableRoads;
	}

	@NonNull
	public Set<LatLon> getImpassableRoadsCoordinates() {
		Set<LatLon> list = new HashSet<>();
		for (AvoidRoadInfo info : impassableRoads) {
			list.add(info.getLatLon());
		}
		return list;
	}

	public void initRouteObjects(boolean force) {
		for (AvoidRoadInfo roadInfo : impassableRoads) {
			long id = roadInfo.getId();
			if (id != 0) {
				for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
					if (force) {
						builder.removeImpassableRoad(id);
					} else {
						builder.addImpassableRoad(id);
					}
				}
			}
			if (force || id == 0) {
				addImpassableRoad(null, roadInfo.getLatLon(), false, true, roadInfo.getAppModeKey());
			}
		}
	}

	protected void recalculateRoute(@Nullable String appModeKey) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(appModeKey, null);
		app.getRoutingHelper().onSettingsChanged(mode);
	}

	public void removeImpassableRoad(@NonNull LatLon latLon) {
		app.getSettings().removeImpassableRoad(latLon);

		AvoidRoadInfo roadInfo = getAvoidRoadInfo(latLon);
		if (roadInfo != null) {
			impassableRoads.remove(roadInfo);

			for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
				builder.removeImpassableRoad(roadInfo.getId());
			}
		}
	}

	public void removeImpassableRoad(@NonNull AvoidRoadInfo roadInfo) {
		app.getSettings().removeImpassableRoad(roadInfo.getLatLon());
		impassableRoads.remove(roadInfo);

		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			builder.removeImpassableRoad(roadInfo.getId());
		}
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
			RouteSegmentSearchResult searchResult = RouteSegmentSearchResult.searchRouteSegment(
					loc.getLatitude(), loc.getLongitude(), maxDistPx / tb.getPixDensity(), roads);
			if (searchResult != null) {
				QuadPointDouble point = searchResult.getPoint();
				LatLon newLoc = new LatLon(MapUtils.get31LatitudeY((int) point.y), MapUtils.get31LongitudeX((int) point.x));
				ll.setLatitude(newLoc.getLatitude());
				ll.setLongitude(newLoc.getLongitude());

				RouteDataObject object = roads.get(searchResult.getRoadIndex()).getObject();
				AvoidRoadInfo avoidRoadInfo = getOrCreateAvoidRoadInfo(object, newLoc.getLatitude(), newLoc.getLongitude(), appMode.getStringKey());
				addImpassableRoadInternal(avoidRoadInfo, showDialog, mapActivity);
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
					AvoidRoadInfo avoidRoadInfo = getOrCreateAvoidRoadInfo(object, ll.getLatitude(), ll.getLongitude(), appMode.getStringKey());
					addImpassableRoadInternal(avoidRoadInfo, showDialog, mapActivity);
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
		if (!skipWritingSettings) {
			AvoidRoadInfo avoidRoadInfo = getOrCreateAvoidRoadInfo(null, loc.getLatitude(), loc.getLongitude(), appMode.getStringKey());
			app.getSettings().addImpassableRoad(avoidRoadInfo);
		}
	}

	public void replaceImpassableRoad(MapActivity activity,
	                                  AvoidRoadInfo currentObject,
	                                  LatLon newLoc,
	                                  boolean showDialog,
	                                  AvoidRoadsCallback callback) {
		Location ll = new Location("");
		ll.setLatitude(newLoc.getLatitude());
		ll.setLongitude(newLoc.getLongitude());

		ApplicationMode defaultAppMode = app.getRoutingHelper().getAppMode();
		ApplicationMode appMode = ApplicationMode.valueOfStringKey(currentObject.getAppModeKey(), defaultAppMode);

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
					if (oldLoc != null) {
						app.getSettings().moveImpassableRoad(oldLoc, newLoc);
					}
					impassableRoads.remove(currentObject);
					for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
						builder.removeImpassableRoad(currentObject.getId());
					}

					AvoidRoadInfo avoidRoadInfo = getOrCreateAvoidRoadInfo(object, newLoc.getLatitude(), newLoc.getLongitude(), appMode.getStringKey());
					addImpassableRoadInternal(avoidRoadInfo, showDialog, activity);
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

	private void addImpassableRoadInternal(@NonNull AvoidRoadInfo roadInfo, boolean showDialog, @Nullable MapActivity activity) {
		boolean roadAdded = false;
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			if (!builder.getImpassableRoadLocations().contains(roadInfo.getId())) {
				builder.addImpassableRoad(roadInfo.getId());
				roadAdded = true;
			}
		}
		if (roadAdded) {
			app.getSettings().updateImpassableRoadInfo(roadInfo);
			impassableRoads.add(0, roadInfo);
		} else {
			LatLon location = getLocation(roadInfo);
			if (location != null) {
				app.getSettings().removeImpassableRoad(location);
			}
		}
		recalculateRoute(roadInfo.getAppModeKey());
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
			if (builder.getImpassableRoadLocations().contains(avoidRoadInfo.getId())) {
				return avoidRoadInfo.getLatLon();
			}
		}
		return null;
	}

	@NonNull
	private AvoidRoadInfo getOrCreateAvoidRoadInfo(@Nullable RouteDataObject object, double lat, double lon, @NonNull String appModeKey) {
		AvoidRoadInfo roadInfo = getAvoidRoadInfo(new LatLon(lat, lon));
		return roadInfo != null ? roadInfo : createAvoidRoadInfo(object, lat, lon, appModeKey);
	}

	@Nullable
	private AvoidRoadInfo getAvoidRoadInfo(@NonNull LatLon latLon) {
		for (AvoidRoadInfo info : impassableRoads) {
			if (latLon.equals(info.getLatLon())) {
				return info;
			}
		}
		return null;
	}

	@NonNull
	private AvoidRoadInfo createAvoidRoadInfo(@Nullable RouteDataObject object, double lat, double lon, @NonNull String appModeKey) {
		long id = object != null ? object.getId() : 0;
//		double direction = object != null ? object.directionRoute(0, true) : Double.NaN;
		return new AvoidRoadInfo(id, Double.NaN, lat, lon, getRoadName(object), appModeKey);
	}

	@NonNull
	private String getRoadName(@Nullable RouteDataObject obj) {
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
}