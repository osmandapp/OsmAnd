package net.osmand.plus.avoidroads;

import static net.osmand.data.PointDescription.POINT_TYPE_BLOCKED_ROAD;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RouteSegmentSearchResult;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapLayers;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.MapUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AvoidRoadsHelper {

	private static final float MAX_AVOID_ROUTE_SEARCH_RADIUS_DP = 32f;
	// the avoided-road icons are drawn from this zoom
	private static final int SHOW_ON_MAP_MIN_ZOOM = 15;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final DirectionPointsHelper pointsHelper;
	private List<AvoidRoadInfo> impassableRoads = new LinkedList<>();

	public AvoidRoadsHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.pointsHelper = new DirectionPointsHelper(app);
		loadImpassableRoads();
	}

	@NonNull
	public DirectionPointsHelper getPointsHelper() {
		return pointsHelper;
	}

	@NonNull
	public List<AvoidRoadInfo> getImpassableRoads() {
		return impassableRoads;
	}

	public long getLastModifiedTime() {
		return settings.getImpassableRoadsLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		settings.setImpassableRoadsLastModifiedTime(lastModifiedTime);
	}

	public void loadImpassableRoads() {
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			builder.clearImpassableRoadLocations();
		}
		updateImpassableRoads(settings.getImpassableRoadPoints());
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
				if (force) {
					removeFromRoutingConfigs(id);
				} else {
					addToRoutingConfigs(id);
				}
			}
			if (id == 0) {
				addImpassableRoad(null, roadInfo.getLatLon(), false, true, roadInfo.getAppModeKey());
			} else if (force) {
				AvoidRoadsCallback callback = new AvoidRoadsCallback() {
					@Override
					public void onAddImpassableRoad(boolean success,
							@Nullable AvoidRoadInfo roadInfo) {
						if (!success) {
							addToRoutingConfigs(id);
						}
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				};
				replaceImpassableRoad(null, roadInfo, roadInfo.getLatLon(), false, callback);
			}
		}
	}

	protected void recalculateRoute(@Nullable String appModeKey) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(appModeKey, null);
		app.getRoutingHelper().onSettingsChanged(mode);
	}

	public void removeImpassableRoad(@NonNull LatLon latLon) {
		AvoidRoadInfo roadInfo = getAvoidRoadInfo(latLon);
		if (roadInfo != null) {
			removeImpassableRoad(roadInfo);
		} else {
			settings.removeImpassableRoad(latLon);
		}
	}

	public void removeImpassableRoad(@NonNull AvoidRoadInfo roadInfo) {
		removeImpassableRoadFromList(roadInfo);
		removeFromRoutingConfigs(roadInfo.getId());
		settings.removeImpassableRoad(roadInfo.getLatLon());
	}

	public void showOnMap(@NonNull MapActivity activity, @NonNull AvoidRoadInfo roadInfo) {
		int zoom = Math.max(activity.getMapView().getZoom(), SHOW_ON_MAP_MIN_ZOOM);
		PointDescription description = new PointDescription(POINT_TYPE_BLOCKED_ROAD, roadInfo.getName(app));
		settings.setMapLocationToShow(roadInfo.getLatitude(), roadInfo.getLongitude(), zoom, description, false, roadInfo);
		MapActivity.launchMapActivityMoveToTop(activity);
	}

	public void selectFromMap(@NonNull MapActivity activity, @Nullable ApplicationMode mode) {
		String key = mode != null ? mode.getStringKey() : null;
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		mapLayers.getContextMenuLayer().setSelectOnMap(result -> {
			addImpassableRoad(activity, result, true, false, key);
			return true;
		});
	}

	public void addImpassableRoad(@Nullable MapActivity activity, @NonNull LatLon latLon,
			boolean showDialog, boolean skipWritingSettings, @Nullable String appModeKey) {
		Location location = latLon.toLocation();
		ApplicationMode appMode = getAppMode(appModeKey);

		List<RouteSegmentResult> roads = app.getMeasurementEditingContext() != null
				? app.getMeasurementEditingContext().getRoadSegmentData(appMode)
				: app.getRoutingHelper().getRoute().getOriginalRoute();

		if (roads != null) {
			RotatedTileBox tb = app.getOsmandMap().getMapView().getRotatedTileBox();
			float maxDistPx = MAX_AVOID_ROUTE_SEARCH_RADIUS_DP * tb.getDensity();
			RouteSegmentSearchResult searchResult = RouteSegmentSearchResult.searchRouteSegment(
					latLon.getLatitude(), latLon.getLongitude(), maxDistPx / tb.getPixDensity(), roads);
			if (searchResult != null) {
				QuadPointDouble point = searchResult.getPoint();
				LatLon newLoc = new LatLon(MapUtils.get31LatitudeY((int) point.y), MapUtils.get31LongitudeX((int) point.x));
				location.setLatitude(newLoc.getLatitude());
				location.setLongitude(newLoc.getLongitude());

				RouteDataObject object = roads.get(searchResult.getRoadIndex()).getObject();
				AvoidRoadInfo roadInfo = getOrCreateAvoidRoadInfo(newLoc, appMode.getStringKey(), object);
				addImpassableRoadInternal(roadInfo, showDialog, activity);
				if (!skipWritingSettings) {
					settings.addImpassableRoad(roadInfo);
				}
				return;
			}
		}
		app.getLocationProvider().getRouteSegment(location, appMode, false, new ResultMatcher<>() {

			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					if (activity != null) {
						app.showToastMessage(R.string.error_avoid_specific_road);
					}
				} else {
					AvoidRoadInfo avoidRoadInfo = getOrCreateAvoidRoadInfo(location.toLatlon(), appMode.getStringKey(), object);
					addImpassableRoadInternal(avoidRoadInfo, showDialog, activity);
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

		});
		if (!skipWritingSettings) {
			AvoidRoadInfo roadInfo = getOrCreateAvoidRoadInfo(latLon, appMode.getStringKey(), null);
			settings.addImpassableRoad(roadInfo);
		}
	}

	public void replaceImpassableRoad(@Nullable MapActivity activity,
			@NonNull AvoidRoadInfo currentObject, @NonNull LatLon newLoc,
			boolean showDialog, @Nullable AvoidRoadsCallback callback) {
		Location location = newLoc.toLocation();
		ApplicationMode appMode = getAppMode(currentObject.getAppModeKey());

		app.getLocationProvider().getRouteSegment(location, appMode, false, new ResultMatcher<>() {
			@Override
			public boolean publish(RouteDataObject object) {
				if (object == null) {
					if (activity != null) {
						app.showToastMessage(R.string.error_avoid_specific_road);
					}
					if (callback != null) {
						callback.onAddImpassableRoad(false, null);
					}
				} else {
					LatLon oldLoc = getLocation(currentObject);
					if (oldLoc != null) {
						settings.moveImpassableRoad(oldLoc, newLoc);
					}
					removeImpassableRoadFromList(currentObject);
					removeFromRoutingConfigs(currentObject.getId());

					AvoidRoadInfo roadInfo = getOrCreateAvoidRoadInfo(newLoc, appMode.getStringKey(), object);
					addImpassableRoadInternal(roadInfo, showDialog, activity);
					if (callback != null) {
						callback.onAddImpassableRoad(true, roadInfo);
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

	private void addImpassableRoadInternal(@NonNull AvoidRoadInfo roadInfo, boolean showDialog,
			@Nullable MapActivity activity) {
		boolean roadAdded = addToRoutingConfigs(roadInfo.getId());
		if (roadAdded) {
			settings.updateImpassableRoadInfo(roadInfo);
			addImpassableRoadToList(roadInfo);
		} else {
			LatLon location = getLocation(roadInfo);
			if (location != null) {
				settings.removeImpassableRoad(location);
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

	@NonNull
	private ApplicationMode getAppMode(@Nullable String key) {
		ApplicationMode mode = ApplicationMode.valueOfStringKey(key, null);
		return mode != null ? mode : app.getRoutingHelper().getAppMode();
	}

	private boolean addToRoutingConfigs(long id) {
		boolean added = false;
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			if (!builder.getImpassableRoadLocations().contains(id)) {
				builder.addImpassableRoad(id);
				added = true;
			}
		}
		return added;
	}

	private void removeFromRoutingConfigs(long id) {
		for (RoutingConfiguration.Builder builder : app.getAllRoutingConfigs()) {
			builder.removeImpassableRoad(id);
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
	private AvoidRoadInfo getOrCreateAvoidRoadInfo(@NonNull LatLon latLon,
			@NonNull String appModeKey, @Nullable RouteDataObject object) {
		AvoidRoadInfo roadInfo = getAvoidRoadInfo(latLon);
		return roadInfo != null ? roadInfo : createAvoidRoadInfo(appModeKey, latLon.getLatitude(),
				latLon.getLongitude(), object);
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

	private void updateImpassableRoads(@NonNull List<AvoidRoadInfo> roadInfos) {
		impassableRoads = new LinkedList<>(roadInfos);
	}

	private void addImpassableRoadToList(@NonNull AvoidRoadInfo roadInfo) {
		List<AvoidRoadInfo> list = new LinkedList<>(impassableRoads);
		if (!list.contains(roadInfo)) {
			list.add(0, roadInfo);
		}
		impassableRoads = list;
	}

	private void removeImpassableRoadFromList(@NonNull AvoidRoadInfo roadInfo) {
		List<AvoidRoadInfo> list = new LinkedList<>(impassableRoads);
		list.remove(roadInfo);
		impassableRoads = list;
	}

	@NonNull
	private AvoidRoadInfo createAvoidRoadInfo(@NonNull String appModeKey, double lat, double lon,
			@Nullable RouteDataObject object) {
		long id = object != null ? object.getId() : 0;
//		double direction = object != null ? object.directionRoute(0, true) : Double.NaN;
		return new AvoidRoadInfo(id, Double.NaN, lat, lon, getRoadName(object), appModeKey);
	}

	@NonNull
	private String getRoadName(@Nullable RouteDataObject obj) {
		if (obj != null) {
			String locale = settings.MAP_PREFERRED_LOCALE.get();
			boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();
			String name = RoutingHelperUtils.formatStreetName(
					obj.getName(locale, transliterate),
					obj.getRef(locale, transliterate, true),
					obj.getDestinationName(locale, transliterate, true),
					app.getString(R.string.towards)
			);
			if (!TextUtils.isEmpty(name)) {
				return name;
			}
			return app.getString(getRoadTypeNameId(obj.getHighway()));
		}
		return app.getString(R.string.shared_string_road);
	}

	@StringRes
	private int getRoadTypeNameId(@Nullable String highway) {
		if (highway == null) {
			return R.string.shared_string_road;
		}
		return switch (highway) {
			case "motorway", "motorway_link", "trunk", "trunk_link" -> R.string.rendering_attr_highway_class_motorway_name;
			case "primary", "primary_link", "secondary", "secondary_link" -> R.string.rendering_attr_highway_class_state_road_name;
			case "residential", "living_street" -> R.string.rendering_attr_highway_class_street_name;
			case "service" -> R.string.rendering_attr_highway_class_service_name;
			case "track" -> R.string.rendering_attr_highway_class_track_name;
			case "footway", "pedestrian" -> R.string.rendering_attr_highway_class_footway_name;
			case "path" -> R.string.rendering_attr_highway_class_path_name;
			case "cycleway" -> R.string.rendering_attr_highway_class_cycleway_name;
			case "bridleway" -> R.string.rendering_attr_highway_class_bridleway_name;
			case "steps" -> R.string.rendering_attr_highway_class_steps_name;
			default -> R.string.shared_string_road;
		};
	}
}