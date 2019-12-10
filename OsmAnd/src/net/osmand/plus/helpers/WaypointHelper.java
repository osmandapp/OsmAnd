package net.osmand.plus.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.Amenity.AmenityRoutePoint;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.IntermediatePointsDialog;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.list.array.TIntArrayList;

//	import android.widget.Toast;

/**
 */
public class WaypointHelper {
	private static final int NOT_ANNOUNCED = 0;
	private static final int ANNOUNCED_ONCE = 1;
	private static final int ANNOUNCED_DONE = 2;

	private int searchDeviationRadius = 500;
	private int poiSearchDeviationRadius = 100;
	private static final int LONG_ANNOUNCE_RADIUS = 700;
	private static final int SHORT_ANNOUNCE_RADIUS = 150;
	private static final int ALARMS_ANNOUNCE_RADIUS = 150;
	private static final int ALARMS_SHORT_ANNOUNCE_RADIUS = 100;

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

	private List<List<LocationPointWrapper>> locationPoints = new ArrayList<>();
	private ConcurrentHashMap<LocationPoint, Integer> locationPointsStates = new ConcurrentHashMap<>();
	private ConcurrentHashMap<AlarmInfo.AlarmInfoType, AlarmInfo> lastAnnouncedAlarms = new ConcurrentHashMap<>();
	private TIntArrayList pointsProgress = new TIntArrayList();
	private RouteCalculationResult route;

	private long announcedAlarmTime;
	private ApplicationMode appMode;


	public WaypointHelper(OsmandApplication application) {
		app = application;
		appMode = app.getSettings().getApplicationMode();
	}


	public List<LocationPointWrapper> getWaypoints(int type) {
		if (type == TARGETS) {
			return getTargets(new ArrayList<WaypointHelper.LocationPointWrapper>());
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

	public int getRouteDistance(LocationPointWrapper point) {
		return route.getDistanceToPoint(point.routeIndex);
	}
	
	public boolean isPointPassed(LocationPointWrapper point) {
		return route.isPointPassed(point.routeIndex);
	}
	
	public boolean isAmenityNoPassed(Amenity a) {
		if (a != null) {
			List<LocationPointWrapper> points = locationPoints.get(POI);
			for (LocationPointWrapper point : points) {
				if (point.point instanceof AmenityLocationPoint) {
					if (a.equals(((AmenityLocationPoint) point.point).a)) {
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
			IntermediatePointsDialog.commitPointsRemoval(app, checkedIntermediates);
		}

	}

	public LocationPointWrapper getMostImportantLocationPoint(List<LocationPointWrapper> list) {
		//Location lastProjection = app.getRoutingHelper().getLastProjection();
		if (list != null) {
			list.clear();
		}
		LocationPointWrapper found = null;
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
					if (route.getDistanceToPoint(lwp.routeIndex) <= LONG_ANNOUNCE_RADIUS) {
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

	public AlarmInfo getMostImportantAlarm(OsmandSettings.SpeedConstants sc, boolean showCameras) {
		Location lastProjection = app.getRoutingHelper().getLastProjection();
		float mxspeed = route.getCurrentMaxSpeed();
		float delta = app.getSettings().SPEED_LIMIT_EXCEED.get() / 3.6f;
		AlarmInfo speedAlarm = createSpeedAlarm(sc, mxspeed, lastProjection, delta);
		if (speedAlarm != null) {
			getVoiceRouter().announceSpeedAlarm(speedAlarm.getIntValue(), lastProjection.getSpeed());
		}
		AlarmInfo mostImportant = speedAlarm;
		int value = speedAlarm != null ? speedAlarm.updateDistanceAndGetPriority(0, 0) : Integer.MAX_VALUE;
		float speed = lastProjection != null && lastProjection.hasSpeed() ? lastProjection.getSpeed() : 0;
		if (ALARMS < pointsProgress.size()) {
			int kIterator = pointsProgress.get(ALARMS);
			List<LocationPointWrapper> lp = locationPoints.get(ALARMS);
			while (kIterator < lp.size()) {
				AlarmInfo inf = (AlarmInfo) lp.get(kIterator).point;
				int currentRoute = route.getCurrentRoute();
				if (inf.getLocationIndex() < currentRoute && inf.getLastLocationIndex() != -1
						&& inf.getLastLocationIndex() < currentRoute) {
					// skip
				} else {
					if (inf.getType() == AlarmInfoType.TUNNEL && inf.getLastLocationIndex() != -1
							&& currentRoute > inf.getLocationIndex()
							&& currentRoute < inf.getLastLocationIndex()) {
						inf.setFloatValue(route.getDistanceToPoint(inf.getLastLocationIndex()));
					}
					int d = route.getDistanceToPoint(inf.getLocationIndex());
					if (d > LONG_ANNOUNCE_RADIUS) {
						break;
					}
					float time = speed > 0 ? d / speed : Integer.MAX_VALUE;
					int vl = inf.updateDistanceAndGetPriority(time, d);
					if (vl < value && (showCameras || inf.getType() != AlarmInfoType.SPEED_CAMERA)) {
						mostImportant = inf;
						value = vl;
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

	public AlarmInfo calculateMostImportantAlarm(RouteDataObject ro, Location loc, MetricsConstants mc,
												 OsmandSettings.SpeedConstants sc, boolean showCameras) {
		float mxspeed = ro.getMaximumSpeed(ro.bearingVsRouteDirection(loc));
		float delta = app.getSettings().SPEED_LIMIT_EXCEED.get() / 3.6f;
		AlarmInfo speedAlarm = createSpeedAlarm(sc, mxspeed, loc, delta);
		if (speedAlarm != null) {
			getVoiceRouter().announceSpeedAlarm(speedAlarm.getIntValue(), loc.getSpeed());
			return speedAlarm;
		}
		for (int i = 0; i < ro.getPointsLength(); i++) {
			int[] pointTypes = ro.getPointTypes(i);
			RouteRegion reg = ro.region;
			if (pointTypes != null) {
				for (int r = 0; r < pointTypes.length; r++) {
					RouteTypeRule typeRule = reg.quickGetEncodingRule(pointTypes[r]);
					AlarmInfo info = AlarmInfo.createAlarmInfo(typeRule, 0, loc);

					// For STOP first check if it has directional info
					// Looks like has no effect here
					//if (info != null && info.getType() != null && info.getType() == AlarmInfoType.STOP) {
					//	if (!ro.isStopApplicable(ro.bearingVsRouteDirection(loc), i)) {
					//		info = null;
					//	}
					//}

					if (info != null) {
						if (info.getType() != AlarmInfoType.SPEED_CAMERA || showCameras) {
							long ms = System.currentTimeMillis();
							if (ms - announcedAlarmTime > 50 * 1000) {
								announcedAlarmTime = ms;
								getVoiceRouter().announceAlarm(info, loc.getSpeed());
							}
							return info;
						}
					}
				}
			}
		}
		return null;
	}

	private static AlarmInfo createSpeedAlarm(OsmandSettings.SpeedConstants sc, float mxspeed, Location loc, float delta) {
		AlarmInfo speedAlarm = null;
		if (mxspeed != 0 && loc != null && loc.hasSpeed() && mxspeed != RouteDataObject.NONE_MAX_SPEED) {
			if (loc.getSpeed() > mxspeed + delta) {
				int speed;
				if (sc.imperial) {
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
				List<LocationPointWrapper> approachPoints = new ArrayList<LocationPointWrapper>();
				List<LocationPointWrapper> announcePoints = new ArrayList<LocationPointWrapper>();
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
					while (kIterator < lp.size()) {
						LocationPointWrapper lwp = lp.get(kIterator);
						if (type == ALARMS && lwp.routeIndex < currentRoute) {
							kIterator++;
							continue;
						}
						if (lwp.announce) {
							if (route.getDistanceToPoint(lwp.routeIndex) > LONG_ANNOUNCE_RADIUS * 2) {
								break;
							}
							LocationPoint point = lwp.point;
							double d1 = Math.max(0.0, MapUtils.getDistance(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
									point.getLatitude(), point.getLongitude()) - lwp.getDeviationDistance());
							Integer state = locationPointsStates.get(point);
							if (state != null && state == ANNOUNCED_ONCE
									&& voiceRouter.isDistanceLess(lastKnownLocation.getSpeed(), d1, SHORT_ANNOUNCE_RADIUS, 0f)) {
								locationPointsStates.put(point, ANNOUNCED_DONE);
								announcePoints.add(lwp);
							} else if (type != ALARMS && (state == null || state == NOT_ANNOUNCED)
									&& voiceRouter.isDistanceLess(lastKnownLocation.getSpeed(), d1, LONG_ANNOUNCE_RADIUS, 0f)) {
								locationPointsStates.put(point, ANNOUNCED_ONCE);
								approachPoints.add(lwp);
							} else if (type == ALARMS && (state == null || state == NOT_ANNOUNCED)) {
								AlarmInfo alarm = (AlarmInfo) point;
								AlarmInfoType t = alarm.getType();
								int announceRadius;
								boolean filter = false;
								switch (t) {
									case TRAFFIC_CALMING:
										announceRadius = ALARMS_SHORT_ANNOUNCE_RADIUS;
										filter = true;
										break;
									default:
										announceRadius = ALARMS_ANNOUNCE_RADIUS;
										break;
								}
								boolean proceed = voiceRouter.isDistanceLess(lastKnownLocation.getSpeed(), d1, announceRadius, 0f);
								if (proceed && filter) {
									AlarmInfo lastAlarm = lastAnnouncedAlarms.get(t);
									if (lastAlarm != null) {
										double dist = MapUtils.getDistance(lastAlarm.getLatitude(), lastAlarm.getLongitude(), alarm.getLatitude(), alarm.getLongitude());
										if (dist < ALARMS_SHORT_ANNOUNCE_RADIUS) {
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
							}
						} else if (type == FAVORITES) {
							voiceRouter.approachFavorite(lastKnownLocation, approachPoints);
						}
					}
				}
			}
		}
	}


	protected VoiceRouter getVoiceRouter() {
		return app.getRoutingHelper().getVoiceRouter();
	}

	public boolean isRouteCalculated() {
		return route != null && !route.isEmpty();
	}

	public List<LocationPointWrapper> getAllPoints() {
		List<LocationPointWrapper> points = new ArrayList<WaypointHelper.LocationPointWrapper>();
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


	protected List<LocationPointWrapper> getTargets(List<LocationPointWrapper> points) {
		List<TargetPoint> wts = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		for (int k = 0; k < wts.size(); k++) {
			final int index = wts.size() - k - 1;
			TargetPoint tp = wts.get(index);
			int routeIndex;
			if (route == null) {
				routeIndex = k == 0 ? Integer.MAX_VALUE : index;
			} else {
				routeIndex = k == 0 ? route.getImmutableAllLocations().size() - 1 : route.getIndexOfIntermediate(k - 1);
			}
			points.add(new LocationPointWrapper(route, TARGETS, tp, 0, routeIndex));
		}
		Collections.reverse(points);
		return points;
	}

	public void clearAllVisiblePoints() {
		this.locationPointsStates.clear();
		this.lastAnnouncedAlarms.clear();
		this.locationPoints = new ArrayList<List<LocationPointWrapper>>();
	}


	public void setNewRoute(RouteCalculationResult route) {
		List<List<LocationPointWrapper>> locationPoints = new ArrayList<List<LocationPointWrapper>>();
		recalculatePoints(route, -1, locationPoints);
		setLocationPoints(locationPoints, route);
	}


	protected void recalculatePoints(RouteCalculationResult route, int type, List<List<LocationPointWrapper>> locationPoints) {
		boolean all = type == -1;
		appMode = app.getSettings().getApplicationMode();
		if (route != null && !route.isEmpty()) {
			boolean showWaypoints = app.getSettings().SHOW_WPT.get(); // global
			boolean announceWaypoints = app.getSettings().ANNOUNCE_WPT.get(); // global
			
			if(route.getAppMode() != null) {
				appMode = route.getAppMode();
			}
			boolean showPOI = app.getSettings().SHOW_NEARBY_POI.getModeValue(appMode);
			boolean showFavorites = app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(appMode);
			boolean announceFavorites = app.getSettings().ANNOUNCE_NEARBY_FAVORITES.getModeValue(appMode);
			boolean announcePOI = app.getSettings().ANNOUNCE_NEARBY_POI.getModeValue(appMode);
			
			if ((type == FAVORITES || all)) {
				final List<LocationPointWrapper> array = clearAndGetArray(locationPoints, FAVORITES);
				if (showFavorites) {
					findLocationPoints(route, FAVORITES, array, app.getFavorites().getVisibleFavouritePoints(),
							announceFavorites);
					sortList(array);
				}
			}
			if ((type == ALARMS || all)) {
				final List<LocationPointWrapper> array = clearAndGetArray(locationPoints, ALARMS);
				if(route.getAppMode() != null) {
					calculateAlarms(route, array, appMode);
					sortList(array);
				}
			}
			if ((type == WAYPOINTS || all)) {
				final List<LocationPointWrapper> array = clearAndGetArray(locationPoints, WAYPOINTS);
				if (showWaypoints) {
					findLocationPoints(route, WAYPOINTS, array, app.getAppCustomization().getWaypoints(),
							announceWaypoints);
					findLocationPoints(route, WAYPOINTS, array, route.getLocationPoints(), announceWaypoints);
					sortList(array);
				}
			}
			if ((type == POI || all)) {
				final List<LocationPointWrapper> array = clearAndGetArray(locationPoints, POI);
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
			final double ld = MapUtils.getOrthogonalDistance(
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
			final List<Location> locs = route.getImmutableAllLocations();
			List<Amenity> amenities = new ArrayList<>();
			for (PoiUIFilter pf : app.getPoiFilters().getSelectedPoiFilters()) {
                amenities.addAll(pf.searchAmenitiesOnThePath(locs, poiSearchDeviationRadius));
			}
			for (Amenity a : amenities) {
				AmenityRoutePoint rp = a.getRoutePoint();
				int i = locs.indexOf(rp.pointA);
				if (i >= 0) {
					LocationPointWrapper lwp = new LocationPointWrapper(route, POI, new AmenityLocationPoint(a),
							(float) rp.deviateDistance, i);
					lwp.deviationDirectionRight = rp.deviationDirectionRight;
					lwp.setAnnounce(announcePOI);
					locationPoints.add(lwp);
				}
			}
		}
	}


	private void calculateAlarms(RouteCalculationResult route, List<LocationPointWrapper> array, ApplicationMode mode) {
		AlarmInfo prevSpeedCam = null;
		for (AlarmInfo i : route.getAlarmInfo()) {
			if (i.getType() == AlarmInfoType.SPEED_CAMERA) {
				if (app.getSettings().SHOW_ROUTING_ALARMS.get() && app.getSettings().SHOW_CAMERAS.getModeValue(mode)
						|| app.getSettings().SPEAK_SPEED_CAMERA.getModeValue(mode)) {
					LocationPointWrapper lw = new LocationPointWrapper(route, ALARMS, i, 0, i.getLocationIndex());
					if(prevSpeedCam != null &&  
							MapUtils.getDistance(prevSpeedCam.getLatitude(), prevSpeedCam.getLongitude(), 
									i.getLatitude(), i.getLongitude()) < DISTANCE_IGNORE_DOUBLE_SPEEDCAMS) {
						// ignore double speed cams
					} else {
						lw.setAnnounce(app.getSettings().SPEAK_SPEED_CAMERA.getModeValue(mode));
						array.add(lw);
						prevSpeedCam = i;
					}
				}
			} else {
				if (app.getSettings().SHOW_ROUTING_ALARMS.get() && app.getSettings().SHOW_TRAFFIC_WARNINGS.getModeValue(mode)
						|| app.getSettings().SPEAK_TRAFFIC_WARNINGS.getModeValue(mode)) {
					LocationPointWrapper lw = new LocationPointWrapper(route, ALARMS, i, 0, i.getLocationIndex());
					lw.setAnnounce(app.getSettings().SPEAK_TRAFFIC_WARNINGS.get());
					array.add(lw);
				}
			}
			

		}

	}


	private List<LocationPointWrapper> clearAndGetArray(List<List<LocationPointWrapper>> array,
														int ind) {
		while (array.size() <= ind) {
			array.add(new ArrayList<WaypointHelper.LocationPointWrapper>());
		}
		array.get(ind).clear();
		return array.get(ind);
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
				LocationPointWrapper lpw = new LocationPointWrapper(rt, type, p, dist, ind[0]);
				lpw.deviationDirectionRight = devDirRight[0];
				lpw.setAnnounce(announce);
				locationPoints.add(lpw);
			}
		}
	}


	/// 
	public Set<PoiUIFilter> getPoiFilters() {
		return app.getPoiFilters().getSelectedPoiFilters();
	}

	public static class LocationPointWrapper {

		public LocationPoint point;
		public float deviationDistance;
		public boolean deviationDirectionRight;
		public int type;

		int routeIndex;
		boolean announce = true;
		RouteCalculationResult route;

		public LocationPointWrapper() {
		}

		public LocationPointWrapper(RouteCalculationResult rt, int type, LocationPoint point, float deviationDistance, int routeIndex) {
			this.route = rt;
			this.type = type;
			this.point = point;
			this.deviationDistance = deviationDistance;
			this.routeIndex = routeIndex;
		}

		public void setAnnounce(boolean announce) {
			this.announce = announce;
		}

		public float getDeviationDistance() {
			return deviationDistance;
		}

		public boolean isDeviationDirectionRight() {
			return deviationDirectionRight;
		}

		public LocationPoint getPoint() {
			return point;
		}


		public Drawable getDrawable(Context uiCtx, OsmandApplication app, boolean nightMode) {
			if (type == POI) {
				Amenity amenity = ((AmenityLocationPoint) point).a;
				PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
				if (st != null) {
					if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
						return uiCtx.getResources().getDrawable(
								RenderingIcons.getBigIconResourceId(st.getIconKeyName()));
					} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
						return uiCtx.getResources().getDrawable(
								RenderingIcons.getBigIconResourceId(st.getOsmTag() + "_" + st.getOsmValue()));
					}
				}
				return null;

			} else if (type == TARGETS) {
				UiUtilities iconsCache = app.getUIUtilities();
				if (((TargetPoint) point).start) {
					if (app.getTargetPointsHelper().getPointToStart() == null) {
						return iconsCache.getIcon(R.drawable.ic_action_location_color, 0);
					} else {
						return iconsCache.getIcon(R.drawable.list_startpoint, 0);
					}
				} else if (((TargetPoint) point).intermediate) {
					return iconsCache.getIcon(R.drawable.list_intermediate, 0);
				} else {
					return iconsCache.getIcon(R.drawable.list_destination, 0);
				}

			} else if (type == FAVORITES || type == WAYPOINTS) {
				return FavoriteImageDrawable.getOrCreate(uiCtx, point.getColor(), false);

			} else if (type == ALARMS) {
				//assign alarm list icons manually for now
				String typeString = ((AlarmInfo) point).getType().toString();
				if (typeString.equals("SPEED_CAMERA")) {
					return uiCtx.getResources().getDrawable(R.drawable.mx_highway_speed_camera);
				} else if (typeString.equals("BORDER_CONTROL")) {
					return uiCtx.getResources().getDrawable(R.drawable.mx_barrier_border_control);
				} else if (typeString.equals("RAILWAY")) {
					if (app.getSettings().DRIVING_REGION.get().americanSigns) {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_railways_us);
					} else {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_railways);
					}
				} else if (typeString.equals("TRAFFIC_CALMING")) {
					if (app.getSettings().DRIVING_REGION.get().americanSigns) {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_traffic_calming_us);
					} else {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_traffic_calming);
					}
				} else if (typeString.equals("TOLL_BOOTH")) {
					return uiCtx.getResources().getDrawable(R.drawable.mx_toll_booth);
				} else if (typeString.equals("STOP")) {
					return uiCtx.getResources().getDrawable(R.drawable.list_stop);
				} else if (typeString.equals("PEDESTRIAN")) {
					if (app.getSettings().DRIVING_REGION.get().americanSigns) {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_pedestrian_us);
					} else {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_pedestrian);
					}
				} else if (typeString.equals("TUNNEL")) {
					if (app.getSettings().DRIVING_REGION.get().americanSigns) {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_tunnel_us);
					} else {
						return uiCtx.getResources().getDrawable(R.drawable.list_warnings_tunnel);
					}
				} else {
					return null;
				}

			} else {
				return null;
			}
		}

		@Override
		public int hashCode() {
			return ((point == null) ? 0 : point.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			LocationPointWrapper other = (LocationPointWrapper) obj;
			if (point == null) {
				if (other.point != null) {
					return false;
				}
			} else if (!point.equals(other.point)) {
				return false;
			}
			return true;
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

	public class AmenityLocationPoint implements LocationPoint {

		Amenity a;

		public AmenityLocationPoint(Amenity a) {
			this.a = a;
		}

		@Override
		public double getLatitude() {
			return a.getLocation().getLatitude();
		}

		@Override
		public double getLongitude() {
			return a.getLocation().getLongitude();
		}


		@Override
		public PointDescription getPointDescription(Context ctx) {
			return new PointDescription(PointDescription.POINT_TYPE_POI,
					OsmAndFormatter.getPoiStringWithoutType(a, app.getSettings().MAP_PREFERRED_LOCALE.get(),
							app.getSettings().MAP_TRANSLITERATE_NAMES.get()));
		}

		@Override
		public int getColor() {
			return 0;
		}

		@Override
		public boolean isVisible() {
			return true;
		}

	}

}


