package net.osmand.plus.helpers;

import static net.osmand.plus.routing.AlarmInfoType.PEDESTRIAN;
import static net.osmand.plus.routing.AlarmInfoType.RAILWAY;
import static net.osmand.plus.routing.AlarmInfoType.SPEED_CAMERA;
import static net.osmand.plus.routing.AlarmInfoType.TUNNEL;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_LONG_ALARM_ANNOUNCE;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_LONG_PNT_APPROACH;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_SHORT_ALARM_ANNOUNCE;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_SHORT_PNT_APPROACH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.Amenity.AmenityRoutePoint;
import net.osmand.data.LocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.list.array.TIntArrayList;

public class WaypointHelper {

	private static final int NOT_ANNOUNCED = 0;
	private static final int ANNOUNCED_ONCE = 1;
	private static final int ANNOUNCED_DONE = 2;

	private int searchDeviationRadius = 500;
	private int poiSearchDeviationRadius = 100;

	// don't annoy users by lots of announcements
	private static final int APPROACH_POI_LIMIT = 1;
	private static final int ANNOUNCE_POI_LIMIT = 3;

	OsmandApplication app;
	// every time we modify this collection, we change the reference (copy on write list)
	public static final int TARGETS = 0;
	public static final int WAYPOINTS = 1;
	public static final int POI = 2;
	public static final int FAVORITES = 3;
	public static final int ALARMS = 4;
	public static final int MAX = 5;
	public static final int[] SEARCH_RADIUS_VALUES = {50, 100, 200, 500, 1000, 2000, 5000};
	private static final double DISTANCE_IGNORE_DOUBLE_SPEEDCAMS = 150;
	private static final double DISTANCE_IGNORE_DOUBLE_RAILWAYS = 50;

	private List<List<LocationPointWrapper>> locationPoints = new ArrayList<>();
	private final ConcurrentHashMap<LocationPoint, Integer> locationPointsStates = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<AlarmInfoType, AlarmInfo> lastAnnouncedAlarms = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<AlarmInfoType, Long> lastAnnouncedAlarmsTime = new ConcurrentHashMap<>();
	private static final int SAME_ALARM_INTERVAL = 30;//in seconds
	private TIntArrayList pointsProgress = new TIntArrayList();
	private RouteCalculationResult route;

	private ApplicationMode appMode;


	public WaypointHelper(OsmandApplication application) {
		app = application;
		appMode = app.getSettings().getApplicationMode();
	}


	public List<LocationPointWrapper> getWaypoints(int type) {
		if (type == TARGETS) {
			return getTargets(new ArrayList<>());
		}
		if (type >= locationPoints.size()) {
			return Collections.emptyList();
		}
		return locationPoints.get(type);
	}

	public void locationChanged(Location location) {
		app.getAppCustomization();
		announceVisibleLocations();
	}

	public int getRouteDistance(@NonNull LocationPointWrapper point) {
		return route.getDistanceToPoint(point.routeIndex);
	}

	public boolean isPointPassed(@NonNull LocationPointWrapper point) {
		return route.isPointPassed(point.routeIndex);
	}

	public boolean isAmenityNoPassed(@Nullable Amenity amenity) {
		if (amenity != null) {
			List<LocationPointWrapper> points = locationPoints.get(POI);
			for (LocationPointWrapper point : points) {
				if (point.point instanceof AmenityLocationPoint) {
					if (amenity.equals(((AmenityLocationPoint) point.point).getAmenity())) {
						return !isPointPassed(point);
					}
				}
			}
		}
		return false;
	}

	public void removeVisibleLocationPoint(LocationPointWrapper lp) {
		if (lp.type < locationPoints.size()) {
			locationPoints.get(lp.type).remove(lp);
		}
	}

	public void removeVisibleLocationPoint(List<LocationPointWrapper> points) {
		List<TargetPoint> ps = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		boolean[] checkedIntermediates = null;
		for (LocationPointWrapper lp : points) {
			if (lp.type == TARGETS) {
				if (checkedIntermediates == null) {
					checkedIntermediates = new boolean[ps.size()];
					Arrays.fill(checkedIntermediates, true);
				}
				if (((TargetPoint) lp.point).intermediate) {
					checkedIntermediates[((TargetPoint) lp.point).index] = false;
				} else {
					checkedIntermediates[ps.size() - 1] = false;
				}
			} else if (lp.type < locationPoints.size()) {
				locationPoints.get(lp.type).remove(lp);
			}
		}
		if (checkedIntermediates != null) {
			commitPointsRemoval(checkedIntermediates);
		}
	}

	private void commitPointsRemoval(boolean[] checkedIntermediates) {
		int cnt = 0;
		for (int i = checkedIntermediates.length - 1; i >= 0; i--) {
			if (!checkedIntermediates[i]) {
				cnt++;
			}
		}
		if (cnt > 0) {
			boolean changeDestinationFlag = !checkedIntermediates[checkedIntermediates.length - 1];
			if (cnt == checkedIntermediates.length) { // there is no alternative destination if all points are to be removed?
				app.getTargetPointsHelper().removeAllWayPoints(true, true);
			} else {
				for (int i = checkedIntermediates.length - 2; i >= 0; i--) { // skip the destination until a retained waypoint is found
					if (checkedIntermediates[i] && changeDestinationFlag) { // Find a valid replacement for the destination
						app.getTargetPointsHelper().makeWayPointDestination(cnt == 0, i);
						changeDestinationFlag = false;
					} else if (!checkedIntermediates[i]) {
						cnt--;
						app.getTargetPointsHelper().removeWayPoint(cnt == 0, i);
					}
				}
			}
		}
	}

	public LocationPointWrapper getMostImportantLocationPoint(List<LocationPointWrapper> list) {
		if (list != null) {
			list.clear();
		}
		LocationPointWrapper found = null;
		AnnounceTimeDistances atd = getVoiceRouter().getAnnounceTimeDistances();
		for (int type = 0; type < locationPoints.size(); type++) {
			if (type == ALARMS || type == TARGETS) {
				continue;
			}
			int kIterator = pointsProgress.get(type);
			List<LocationPointWrapper> lp = locationPoints.get(type);
			while (kIterator < lp.size()) {
				LocationPointWrapper lwp = lp.get(kIterator);
				if (lp.get(kIterator).routeIndex < route.getCurrentRoute()) {
					// skip
				} else {
					if (atd.isTurnStateActive(0,
							route.getDistanceToPoint(lwp.routeIndex), STATE_LONG_PNT_APPROACH)) {
						if (found == null || found.routeIndex < lwp.routeIndex) {
							found = lwp;
							if (list != null) {
								list.add(lwp);
							}
						}
					}
					break;
				}
				kIterator++;
			}
		}
		return found;
	}

	public AlarmInfo getMostImportantAlarm(SpeedConstants sc, boolean showCameras) {
		Location lastProjection = app.getRoutingHelper().getLastProjection();
		float mxspeed = route.getCurrentMaxSpeed(appMode.getRouteTypeProfile());
		float delta = app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f;
		AlarmInfo speedAlarm = createSpeedAlarm(sc, mxspeed, lastProjection, delta);
		if (speedAlarm != null) {
			getVoiceRouter().announceSpeedAlarm(speedAlarm.getIntValue(), lastProjection.getSpeed());
		}
		AlarmInfo mostImportant = speedAlarm;
		int mostPriority = speedAlarm != null ? speedAlarm.updateDistanceAndGetPriority(0, 0) : Integer.MAX_VALUE;
		float speed = lastProjection != null && lastProjection.hasSpeed() ? lastProjection.getSpeed() : 0;
		AnnounceTimeDistances atd = getVoiceRouter().getAnnounceTimeDistances();
		if (ALARMS < pointsProgress.size()) {
			int kIterator = pointsProgress.get(ALARMS);
			List<LocationPointWrapper> lp = locationPoints.get(ALARMS);
			while (kIterator < lp.size()) {
				LocationPointWrapper lwp = lp.get(kIterator);
				AlarmInfo inf = (AlarmInfo) lwp.point;
				int currentRoute = route.getCurrentRoute();
				// getLastLocationIndex()  == -1 is always < currentRoute
				if (inf.getLocationIndex() <= currentRoute && inf.getLastLocationIndex() < currentRoute) {
					// skip already passed alarms
				} else {
					int distanceByRoute = 0;
					Location lastKnownLocation = app.getRoutingHelper().getLastProjection();
					if (inf.getLocationIndex() < currentRoute) {
						// update remaining length
						inf.setFloatValue(route.getDistanceToPoint(lastKnownLocation, inf.getLastLocationIndex()));
					} else {
						distanceByRoute = route.getDistanceToPoint(lastKnownLocation, inf.getLocationIndex() - 1);
					}
					if (!atd.isTurnStateActive(0, distanceByRoute, STATE_LONG_PNT_APPROACH)) {
						// break once first future alarm is far away as others will be also far away
						break;
					}
					float time = speed > 0 ? distanceByRoute / speed : Integer.MAX_VALUE;
					int priority = inf.updateDistanceAndGetPriority(time, distanceByRoute);
					if (priority < mostPriority && (showCameras || inf.getType() != SPEED_CAMERA)) {
						mostImportant = inf;
						mostPriority = priority;
					}
				}
				kIterator++;
			}
		}
		return mostImportant;
	}

	public void enableWaypointType(int type, boolean enable) {
		//An item will be displayed in the Waypoint list if either "Show..." or "Announce..." is selected for it in the Navigation settings
		//Keep both "Show..." and "Announce..." Nav settings in sync when user changes what to display in the Waypoint list, as follows:
		if (type == ALARMS) {
			app.getSettings().SHOW_TRAFFIC_WARNINGS.setModeValue(appMode, enable);
			app.getSettings().SPEAK_TRAFFIC_WARNINGS.setModeValue(appMode, enable);
			app.getSettings().SHOW_PEDESTRIAN.setModeValue(appMode, enable);
			app.getSettings().SPEAK_PEDESTRIAN.setModeValue(appMode, enable);
			app.getSettings().SHOW_TUNNELS.setModeValue(appMode, enable);
			app.getSettings().SPEAK_TUNNELS.setModeValue(appMode, enable);
			//But do not implicitly change speed_cam settings here because of legal restrictions in some countries, so Nav settings must prevail
		} else if (type == POI) {
			app.getSettings().SHOW_NEARBY_POI.setModeValue(appMode, enable);
			app.getSettings().ANNOUNCE_NEARBY_POI.setModeValue(appMode, enable);
		} else if (type == FAVORITES) {
			app.getSettings().SHOW_NEARBY_FAVORITES.setModeValue(appMode, enable);
			app.getSettings().ANNOUNCE_NEARBY_FAVORITES.setModeValue(appMode, enable);
		} else if (type == WAYPOINTS) {
			app.getSettings().SHOW_WPT.set(enable);
			app.getSettings().ANNOUNCE_WPT.set(enable);
		}
		recalculatePoints(route, type, locationPoints);
	}

	public void recalculatePoints(int type) {
		recalculatePoints(route, type, locationPoints);
	}


	public boolean isTypeConfigurable(int waypointType) {
		return waypointType != TARGETS;
	}

	public boolean isTypeVisible(int waypointType) {
		boolean vis = app.getAppCustomization().isWaypointGroupVisible(waypointType, route);
		if (!vis) {
			return false;
		}
		return vis;
	}

	public boolean isTypeEnabled(int type) {
		if (type == ALARMS) {
			return app.getSettings().SHOW_ROUTING_ALARMS.get() && app.getSettings().SHOW_TRAFFIC_WARNINGS.getModeValue(appMode);
		} else if (type == POI) {
			return app.getSettings().SHOW_NEARBY_POI.getModeValue(appMode);
		} else if (type == FAVORITES) {
			return app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(appMode);
		} else if (type == WAYPOINTS) {
			return app.getSettings().SHOW_WPT.get();
		}
		return true;
	}

	@Nullable
	public AlarmInfo getSpeedLimitAlarm(@NonNull SpeedConstants constants, boolean whenExceeded) {
		Location lastProjection = app.getRoutingHelper().getLastProjection();
		if (route != null) {
			float maxSpeed = route.getCurrentMaxSpeed(appMode.getRouteTypeProfile());
			float delta = whenExceeded ? app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f : maxSpeed * -1;
			return createSpeedAlarm(constants, maxSpeed, lastProjection, delta);
		}
		return null;
	}

	@Nullable
	public AlarmInfo calculateSpeedLimitAlarm(@NonNull RouteDataObject object, @NonNull Location location, @NonNull SpeedConstants constants, boolean whenExceeded) {
		float maxSpeed = object.getMaximumSpeed(object.bearingVsRouteDirection(location), appMode.getRouteTypeProfile());
		float delta = whenExceeded ? app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f : maxSpeed * -1;
		return createSpeedAlarm(constants, maxSpeed, location, delta);
	}

	@Nullable
	public AlarmInfo calculateMostImportantAlarm(RouteDataObject ro, Location loc, MetricsConstants mc,
	                                             SpeedConstants sc, boolean showCameras) {
		float maxSpeed = ro.getMaximumSpeed(ro.bearingVsRouteDirection(loc), appMode.getRouteTypeProfile());
		float delta = app.getSettings().SPEED_LIMIT_EXCEED_KMH.get() / 3.6f;
		AlarmInfo speedAlarm = createSpeedAlarm(sc, maxSpeed, loc, delta);
		if (speedAlarm != null) {
			getVoiceRouter().announceSpeedAlarm(speedAlarm.getIntValue(), loc.getSpeed());
			return speedAlarm;
		}
		for (int i = 0; i < ro.getPointsLength(); i++) {
			int[] pointTypes = ro.getPointTypes(i);
			RouteRegion reg = ro.region;
			if (pointTypes != null) {
				for (int pointType : pointTypes) {
					RouteTypeRule typeRule = reg.quickGetEncodingRule(pointType);
					AlarmInfo info = AlarmInfo.createAlarmInfo(typeRule, 0, loc);
					if (info != null) {
						if (info.getType() != SPEED_CAMERA || showCameras) {
							return info;
						}
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private static AlarmInfo createSpeedAlarm(@NonNull SpeedConstants constants, float mxspeed, Location loc, float delta) {
		AlarmInfo speedAlarm = null;
		if (mxspeed != 0 && loc != null && loc.hasSpeed() && mxspeed != RouteDataObject.NONE_MAX_SPEED) {
			if (loc.getSpeed() > mxspeed + delta) {
				int speed;
				if (constants.getImperial()) {
					speed = Math.round(mxspeed * 3.6f / 1.6f);
				} else {
					speed = Math.round(mxspeed * 3.6f);
				}
				speedAlarm = AlarmInfo.createSpeedLimit(speed, loc);
			}
		}
		return speedAlarm;
	}

	public void announceVisibleLocations() {
		Location lastKnownLocation = app.getRoutingHelper().getLastProjection();
		if (lastKnownLocation != null && app.getRoutingHelper().isFollowingMode()) {
			for (int type = 0; type < locationPoints.size(); type++) {
				int currentRoute = route.getCurrentRoute();
				List<LocationPointWrapper> approachPoints = new ArrayList<>();
				List<LocationPointWrapper> announcePoints = new ArrayList<>();
				List<LocationPointWrapper> lp = locationPoints.get(type);
				if (lp != null) {
					int kIterator = pointsProgress.get(type);
					while (kIterator < lp.size() && lp.get(kIterator).routeIndex < currentRoute) {
						if (type == ALARMS) {
							AlarmInfo alarm = (AlarmInfo) lp.get(kIterator).getPoint();
							if (alarm.getLastLocationIndex() >= currentRoute) {
								break;
							}
						}
						kIterator++;
					}
					pointsProgress.set(type, kIterator);

					VoiceRouter voiceRouter = getVoiceRouter();
					AnnounceTimeDistances atd = voiceRouter.getAnnounceTimeDistances();
					RouteDirectionInfo nextRoute = voiceRouter.getNextRouteDirection();
					float atdSpeed = atd.getSpeed(lastKnownLocation);
					while (kIterator < lp.size()) {
						LocationPointWrapper lwp = lp.get(kIterator);
						if (lwp.announce) {
							if (!atd.isTurnStateActive(atdSpeed,
									route.getDistanceToPoint(lwp.routeIndex) / 2f, STATE_LONG_PNT_APPROACH)) {
								break;
							}
							LocationPoint point = lwp.point;
							double d1 = Math.max(0.0, route.getDistanceToPoint(lastKnownLocation, lwp.routeIndex - 1)
									- lwp.getDeviationDistance());
							Integer state = locationPointsStates.get(point);
							if (state != null && state == ANNOUNCED_ONCE
									&& atd.isTurnStateActive(atdSpeed, d1, STATE_SHORT_PNT_APPROACH)) {
								locationPointsStates.put(point, ANNOUNCED_DONE);
								announcePoints.add(lwp);
							} else if (type != ALARMS && (state == null || state == NOT_ANNOUNCED)
									&& atd.isTurnStateActive(atdSpeed, d1, STATE_LONG_PNT_APPROACH)) {
								locationPointsStates.put(point, ANNOUNCED_ONCE);
								approachPoints.add(lwp);
							} else if (type == ALARMS && (state == null || state == NOT_ANNOUNCED)) {
								AlarmInfo alarm = (AlarmInfo) point;
								AlarmInfoType t = alarm.getType();
								if (beforeTunnelEntrance(currentRoute, alarm)) {
									kIterator++;
									continue;
								}
								int announceRadius;
								boolean filterCloseAlarms = false;
								switch (t) {
									case TRAFFIC_CALMING:
									case HAZARD:
										announceRadius = STATE_SHORT_ALARM_ANNOUNCE;
										filterCloseAlarms = true;
										break;
									case PEDESTRIAN:
										announceRadius = ((nextRoute != null)
												&& (nextRoute.getTurnType().isRoundAbout())
												&& (kIterator != 0))
												? STATE_SHORT_ALARM_ANNOUNCE
												: STATE_LONG_ALARM_ANNOUNCE;
										break;
									default:
										announceRadius = STATE_LONG_ALARM_ANNOUNCE;
										break;
								}
								boolean proceed = atd.isTurnStateActive(atdSpeed, d1, announceRadius);
								if (proceed && filterCloseAlarms) {
									AlarmInfo lastAlarm = lastAnnouncedAlarms.get(t);
									if (lastAlarm != null) {
										double dist = MapUtils.getDistance(lastAlarm.getLatitude(), lastAlarm.getLongitude(), alarm.getLatitude(), alarm.getLongitude());
										if (atd.isTurnStateActive(atdSpeed, dist, STATE_SHORT_ALARM_ANNOUNCE)) {
											locationPointsStates.put(point, ANNOUNCED_DONE);
											proceed = false;
										}
									}
									Long timeLastAlarm = lastAnnouncedAlarmsTime.get(t);
									if (timeLastAlarm != null && proceed) {
										long ms = System.currentTimeMillis();
										if (ms - timeLastAlarm < SAME_ALARM_INTERVAL * 1000) {
											locationPointsStates.put(point, ANNOUNCED_DONE);
											proceed = false;
										}
									}
								}
								if (proceed) {
									locationPointsStates.put(point, ANNOUNCED_ONCE);
									approachPoints.add(lwp);
								}
							}
						}
						kIterator++;
					}
					if (!announcePoints.isEmpty()) {
						if (announcePoints.size() > ANNOUNCE_POI_LIMIT) {
							announcePoints = announcePoints.subList(0, ANNOUNCE_POI_LIMIT);
						}
						if (type == WAYPOINTS) {
							voiceRouter.announceWaypoint(announcePoints);
						} else if (type == POI) {
							voiceRouter.announcePoi(announcePoints);
						} else if (type == ALARMS) {
							// nothing to announce
						} else if (type == FAVORITES) {
							voiceRouter.announceFavorite(announcePoints);
						}
					}
					if (!approachPoints.isEmpty()) {
						if (approachPoints.size() > APPROACH_POI_LIMIT) {
							approachPoints = approachPoints.subList(0, APPROACH_POI_LIMIT);
						}
						if (type == WAYPOINTS) {
							voiceRouter.approachWaypoint(lastKnownLocation, approachPoints);
						} else if (type == POI) {
							voiceRouter.approachPoi(lastKnownLocation, approachPoints);
						} else if (type == ALARMS) {
							for (LocationPointWrapper pw : approachPoints) {
								AlarmInfo alarm = (AlarmInfo) pw.point;
								voiceRouter.announceAlarm(new AlarmInfo(alarm.getType(), -1), lastKnownLocation.getSpeed());
								lastAnnouncedAlarms.put(alarm.getType(), alarm);
								lastAnnouncedAlarmsTime.put(alarm.getType(), System.currentTimeMillis());
							}
						} else if (type == FAVORITES) {
							voiceRouter.approachFavorite(lastKnownLocation, approachPoints);
						}
					}
				}
			}
		}
	}

	private static boolean beforeTunnelEntrance(int currentRoute, AlarmInfo alarm) {
		return alarm.getLocationIndex() > currentRoute && alarm.getLastLocationIndex() > currentRoute;
	}

	protected VoiceRouter getVoiceRouter() {
		return app.getRoutingHelper().getVoiceRouter();
	}

	public boolean isRouteCalculated() {
		return route != null && !route.isEmpty();
	}

	@NonNull
	public List<LocationPointWrapper> getAllPoints() {
		List<LocationPointWrapper> points = new ArrayList<>();
		List<List<LocationPointWrapper>> local = locationPoints;
		TIntArrayList ps = pointsProgress;
		for (int i = 0; i < local.size(); i++) {
			List<LocationPointWrapper> loc = local.get(i);
			if (ps.get(i) < loc.size()) {
				points.addAll(loc.subList(ps.get(i), loc.size()));
			}
		}
		getTargets(points);
		sortList(points);
		return points;
	}

	@NonNull
	protected List<LocationPointWrapper> getTargets(@NonNull List<LocationPointWrapper> points) {
		List<TargetPoint> wts = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		for (int k = 0; k < wts.size(); k++) {
			int index = wts.size() - k - 1;
			TargetPoint tp = wts.get(index);
			int routeIndex;
			if (route == null) {
				routeIndex = k == 0 ? Integer.MAX_VALUE : index;
			} else {
				routeIndex = k == 0 ? route.getImmutableAllLocations().size() - 1 : route.getIndexOfIntermediate(k - 1);
			}
			points.add(new LocationPointWrapper(TARGETS, tp, 0, routeIndex));
		}
		Collections.reverse(points);
		return points;
	}

	public void clearAllVisiblePoints() {
		this.locationPointsStates.clear();
		this.lastAnnouncedAlarms.clear();
		this.lastAnnouncedAlarmsTime.clear();
		this.locationPoints = new ArrayList<>();
	}

	public void setNewRoute(RouteCalculationResult route) {
		List<List<LocationPointWrapper>> locationPoints = new ArrayList<>();
		recalculatePoints(route, -1, locationPoints);
		setLocationPoints(locationPoints, route);
	}

	protected void recalculatePoints(RouteCalculationResult route, int type, List<List<LocationPointWrapper>> locationPoints) {
		boolean all = type == -1;
		appMode = app.getSettings().getApplicationMode();
		if (route != null && !route.isEmpty()) {
			boolean showWaypoints = app.getSettings().SHOW_WPT.get(); // global
			boolean announceWaypoints = app.getSettings().ANNOUNCE_WPT.get(); // global

			if (route.getAppMode() != null) {
				appMode = route.getAppMode();
			}
			boolean showPOI = app.getSettings().SHOW_NEARBY_POI.getModeValue(appMode);
			boolean showFavorites = app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(appMode);
			boolean announceFavorites = app.getSettings().ANNOUNCE_NEARBY_FAVORITES.getModeValue(appMode);
			boolean announcePOI = app.getSettings().ANNOUNCE_NEARBY_POI.getModeValue(appMode);

			if ((type == FAVORITES || all)) {
				List<LocationPointWrapper> array = clearAndGetArray(locationPoints, FAVORITES);
				if (showFavorites) {
					findLocationPoints(route, FAVORITES, array, app.getFavoritesHelper().getVisibleFavouritePoints(),
							announceFavorites);
					sortList(array);
				}
			}
			if ((type == ALARMS || all)) {
				List<LocationPointWrapper> array = clearAndGetArray(locationPoints, ALARMS);
				if (route.getAppMode() != null) {
					calculateAlarms(route, array, appMode);
					sortList(array);
				}
			}
			if ((type == WAYPOINTS || all)) {
				List<LocationPointWrapper> array = clearAndGetArray(locationPoints, WAYPOINTS);
				if (showWaypoints) {
					findLocationPoints(route, WAYPOINTS, array, app.getAppCustomization().getWaypoints(),
							announceWaypoints);
					findLocationPoints(route, WAYPOINTS, array, route.getLocationPoints(), announceWaypoints);
					sortList(array);
				}
			}
			if ((type == POI || all)) {
				List<LocationPointWrapper> array = clearAndGetArray(locationPoints, POI);
				if (showPOI) {
					calculatePoi(route, array, announcePOI);
					sortList(array);
				}
			}
		}
	}

	private float dist(LocationPoint l, List<Location> locations, int[] ind, boolean[] devDirRight) {
		float dist = Float.POSITIVE_INFINITY;
		// Special iterations because points stored by pairs!
		for (int i = 1; i < locations.size(); i++) {
			double ld = MapUtils.getOrthogonalDistance(
					l.getLatitude(), l.getLongitude(),
					locations.get(i - 1).getLatitude(), locations.get(i - 1).getLongitude(),
					locations.get(i).getLatitude(), locations.get(i).getLongitude());
			if (ld < dist) {
				if (ind != null) {
					ind[0] = i;
				}
				dist = (float) ld;
			}
		}

		if (ind != null && dist < Float.POSITIVE_INFINITY) {
			int i = ind[0];
			devDirRight[0] = MapUtils.rightSide(l.getLatitude(), l.getLongitude(),
					locations.get(i - 1).getLatitude(), locations.get(i - 1).getLongitude(),
					locations.get(i).getLatitude(), locations.get(i).getLongitude());
		}

		return dist;
	}

	protected synchronized void setLocationPoints(List<List<LocationPointWrapper>> locationPoints, RouteCalculationResult route) {
		this.locationPoints = locationPoints;
		this.locationPointsStates.clear();
		this.lastAnnouncedAlarms.clear();
		this.lastAnnouncedAlarmsTime.clear();
		TIntArrayList list = new TIntArrayList(locationPoints.size());
		list.fill(0, locationPoints.size(), 0);
		this.pointsProgress = list;
		this.route = route;
	}


	protected void sortList(List<LocationPointWrapper> list) {
		Collections.sort(list, new Comparator<LocationPointWrapper>() {
			@Override
			public int compare(LocationPointWrapper olhs, LocationPointWrapper orhs) {
				int lhs = olhs.routeIndex;
				int rhs = orhs.routeIndex;
				if (lhs == rhs) {
					return Float.compare(olhs.deviationDistance, orhs.deviationDistance);
				}
				return lhs < rhs ? -1 : 1;
			}
		});
	}


	protected void calculatePoi(RouteCalculationResult route, List<LocationPointWrapper> locationPoints, boolean announcePOI) {
		if (app.getPoiFilters().isShowingAnyPoi()) {
			List<Location> locs = route.getImmutableAllLocations();
			List<Amenity> amenities = new ArrayList<>();
			for (PoiUIFilter pf : app.getPoiFilters().getSelectedPoiFilters()) {
				amenities.addAll(pf.searchAmenitiesOnThePath(locs, poiSearchDeviationRadius));
			}
			for (Amenity amenity : amenities) {
				AmenityRoutePoint routePoint = amenity.getRoutePoint();
				if (routePoint != null) {
					int i = locs.indexOf(routePoint.pointA);
					if (i >= 0) {
						AmenityLocationPoint point = new AmenityLocationPoint(amenity);
						LocationPointWrapper wrapper = new LocationPointWrapper(POI, point, (float) routePoint.deviateDistance, i);
						wrapper.deviationDirectionRight = routePoint.deviationDirectionRight;
						wrapper.setAnnounce(announcePOI);
						locationPoints.add(wrapper);
					}
				}
			}
		}
	}

	private void calculateAlarms(RouteCalculationResult route, List<LocationPointWrapper> array, ApplicationMode mode) {
		OsmandSettings settings = app.getSettings();
		if (!settings.SHOW_ROUTING_ALARMS.getModeValue(mode)) {
			return;
		}
		AlarmInfo prevSpeedCam = null;
		AlarmInfo prevRailway = null;
		for (AlarmInfo alarmInfo : route.getAlarmInfo()) {
			AlarmInfoType type = alarmInfo.getType();
			if (type == SPEED_CAMERA) {
				if (settings.SHOW_CAMERAS.getModeValue(mode) || settings.SPEAK_SPEED_CAMERA.getModeValue(mode)) {
					// ignore double speed cams
					if (prevSpeedCam == null || MapUtils.getDistance(prevSpeedCam.getLatitude(), prevSpeedCam.getLongitude(),
							alarmInfo.getLatitude(), alarmInfo.getLongitude()) >= DISTANCE_IGNORE_DOUBLE_SPEEDCAMS) {
						addPointWrapper(alarmInfo, array, settings.SPEAK_SPEED_CAMERA.getModeValue(mode));
						prevSpeedCam = alarmInfo;
					}
				}
			} else if (type == TUNNEL) {
				if (settings.SHOW_TUNNELS.getModeValue(mode) || settings.SPEAK_TUNNELS.getModeValue(mode)) {
					addPointWrapper(alarmInfo, array, settings.SPEAK_TUNNELS.getModeValue(mode));
				}
			} else if (type == PEDESTRIAN) {
				if (settings.SHOW_PEDESTRIAN.getModeValue(mode) || settings.SPEAK_PEDESTRIAN.getModeValue(mode)) {
					addPointWrapper(alarmInfo, array, settings.SPEAK_PEDESTRIAN.getModeValue(mode));
				}
			} else if (type == RAILWAY) {
				if (prevRailway == null || MapUtils.getDistance(prevRailway.getLatitude(), prevRailway.getLongitude(),
						alarmInfo.getLatitude(), alarmInfo.getLongitude()) >= DISTANCE_IGNORE_DOUBLE_RAILWAYS) {
					addPointWrapper(alarmInfo, array, settings.SPEAK_TRAFFIC_WARNINGS.getModeValue(mode));
					prevRailway = alarmInfo;
				}
			} else if (settings.SHOW_TRAFFIC_WARNINGS.getModeValue(mode) || settings.SPEAK_TRAFFIC_WARNINGS.getModeValue(mode)) {
				addPointWrapper(alarmInfo, array, settings.SPEAK_TRAFFIC_WARNINGS.getModeValue(mode));
			}
		}
	}

	private void addPointWrapper(AlarmInfo alarmInfo, List<LocationPointWrapper> array, boolean announce) {
		LocationPointWrapper pointWrapper = new LocationPointWrapper(ALARMS, alarmInfo, 0, alarmInfo.getLocationIndex());
		pointWrapper.setAnnounce(announce);
		array.add(pointWrapper);
	}

	private List<LocationPointWrapper> clearAndGetArray(List<List<LocationPointWrapper>> array, int index) {
		while (array.size() <= index) {
			array.add(new ArrayList<>());
		}
		array.get(index).clear();
		return array.get(index);
	}

	private void findLocationPoints(RouteCalculationResult rt, int type, List<LocationPointWrapper> locationPoints,
	                                List<? extends LocationPoint> points, boolean announce) {
		List<Location> immutableAllLocations = rt.getImmutableAllLocations();
		int[] ind = new int[1];
		boolean[] devDirRight = new boolean[1];
		for (LocationPoint p : points) {
			float dist = dist(p, immutableAllLocations, ind, devDirRight);
			int rad = getSearchDeviationRadius(type);
			if (dist <= rad) {
				LocationPointWrapper lpw = new LocationPointWrapper(type, p, dist, ind[0]);
				lpw.deviationDirectionRight = devDirRight[0];
				lpw.setAnnounce(announce);
				locationPoints.add(lpw);
			}
		}
	}

	public int getSearchDeviationRadius(int type) {
		return type == POI ? poiSearchDeviationRadius : searchDeviationRadius;
	}

	public void setSearchDeviationRadius(int type, int radius) {
		if (type == POI) {
			this.poiSearchDeviationRadius = radius;
		} else {
			this.searchDeviationRadius = radius;
		}
	}
}