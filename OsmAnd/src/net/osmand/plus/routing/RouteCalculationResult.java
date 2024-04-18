package net.osmand.plus.routing;

import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.QuadRect;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.ExitInfo;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RouteCalculationResult {
	private static final Log log = PlatformUtil.getLog(RouteCalculationResult.class);

	private static final double DISTANCE_CLOSEST_TO_INTERMEDIATE = 3000;
	private static final double DISTANCE_THRESHOLD_TO_INTERMEDIATE = 25;
	private static final double DISTANCE_THRESHOLD_TO_INTRODUCE_FIRST_AND_LAST_POINTS = 50;
	// could not be null and immodifiable!
	private final List<Location> locations;
	private final List<RouteDirectionInfo> directions;
	private final List<RouteSegmentResult> segments;
	private final List<AlarmInfo> alarmInfo;
	private final String errorMessage;
	private final int[] listDistance;
	private final int[] intermediatePoints;

	// Route information
	private final float routingTime;
	private final int visitedSegments;
	private final int loadedTiles;
	private final float calculateTime;

	protected int cacheCurrentTextDirectionInfo = -1;
	protected List<RouteDirectionInfo> cacheAgreggatedDirections;
	protected List<LocationPoint> locationPoints = new ArrayList<>();

	protected List<WorldRegion> missingMaps;

	// params
	protected final ApplicationMode appMode;
	protected final RouteService routeService;
	protected final double routeRecalcDistance;
	protected final double routeVisibleAngle;
	protected final boolean initialCalculation;

	// Note always currentRoute > get(currentDirectionInfo).routeOffset, 
	//         but currentRoute <= get(currentDirectionInfo+1).routeOffset 
	protected int currentDirectionInfo;
	protected int currentRoute;
	protected int nextIntermediate;
	protected int currentStraightAngleRoute = -1;
	protected Location currentStraightAnglePoint;


	public RouteCalculationResult(String errorMessage) {
		this.errorMessage = errorMessage;
		this.routingTime = 0;
		this.loadedTiles = 0;
		this.visitedSegments = 0;
		this.calculateTime = 0;
		this.intermediatePoints = new int[0];
		this.locations = new ArrayList<>();
		this.segments = new ArrayList<>();
		this.listDistance = new int[0];
		this.directions = new ArrayList<>();
		this.alarmInfo = new ArrayList<>();
		this.routeService = null;
		this.appMode = null;
		this.routeRecalcDistance = 0;
		this.routeVisibleAngle = 0;
		this.initialCalculation = false;
	}

	public RouteCalculationResult(List<Location> list, List<RouteDirectionInfo> directions,
	                              RouteCalculationParams params, List<LocationPoint> waypoints, boolean addMissingTurns) {
		this.routingTime = 0;
		this.loadedTiles = 0;
		this.calculateTime = 0;
		this.visitedSegments = 0;
		this.errorMessage = null;
		this.intermediatePoints = new int[params.intermediates == null ? 0 : params.intermediates.size()];
		List<Location> locations = list == null ? new ArrayList<>() : new ArrayList<>(list);
		List<RouteDirectionInfo> localDirections = directions == null ? new ArrayList<>() : new ArrayList<RouteDirectionInfo>(directions);
		if (!locations.isEmpty()) {
			checkForDuplicatePoints(locations, localDirections);
		}
		if (waypoints != null) {
			this.locationPoints.addAll(waypoints);
		}
		if (addMissingTurns) {
			removeUnnecessaryGoAhead(localDirections);
			addMissingTurnsToRoute(locations, localDirections, params.mode, params.ctx, params.leftSide,
					params.gpxRoute != null && params.gpxRoute.calculatedRouteTimeSpeed);
			// if there is no closest points to start - add it
			introduceFirstPointAndLastPoint(locations, localDirections, null, params.start, params.end, params.ctx);
		}
		this.appMode = params.mode;
		this.locations = Collections.unmodifiableList(locations);
		this.segments = new ArrayList<>();
		this.listDistance = new int[locations.size()];
		updateListDistanceTime(this.listDistance, this.locations);
		this.alarmInfo = new ArrayList<>();
		calculateIntermediateIndexes(params.ctx, this.locations, params.intermediates, localDirections, this.intermediatePoints);
		this.directions = Collections.unmodifiableList(localDirections);
		updateDirectionsTime(this.directions, this.listDistance);
		this.routeService = params.mode.getRouteService();
		if (params.ctx != null) {
			this.routeRecalcDistance = params.ctx.getSettings().ROUTE_RECALCULATION_DISTANCE.getModeValue(params.mode);
			this.routeVisibleAngle = routeService == RouteService.STRAIGHT ?
					params.ctx.getSettings().ROUTE_STRAIGHT_ANGLE.getModeValue(params.mode) : 0;
		} else {
			this.routeRecalcDistance = 0;
			this.routeVisibleAngle = 0;
		}
		this.initialCalculation = params.initialCalculation;
	}

	public RouteCalculationResult(List<RouteSegmentResult> list, Location start, LatLon end,
	                              List<LatLon> intermediates, OsmandApplication ctx, boolean leftSide,
	                              RoutingContext rctx, List<LocationPoint> waypoints, ApplicationMode mode,
								  boolean calculateFirstAndLastPoint, boolean initialCalculation) {
		if (rctx != null) {
			this.routingTime = rctx.routingTime;
			this.visitedSegments = rctx.getVisitedSegments();
			this.loadedTiles = rctx.getLoadedTiles();
			if (rctx.calculationProgress != null) {
				this.calculateTime = (float) (rctx.calculationProgress.timeToCalculate / 1.0e9);
			} else {
				this.calculateTime = 0;
			}
		} else {
			this.routingTime = 0;
			this.visitedSegments = 0;
			this.loadedTiles = 0;
			this.calculateTime = 0;
		}
		if (waypoints != null) {
			this.locationPoints.addAll(waypoints);
		}
		List<RouteDirectionInfo> computeDirections = new ArrayList<>();
		this.errorMessage = null;
		this.intermediatePoints = new int[intermediates == null ? 0 : intermediates.size()];
		List<Location> locations = new ArrayList<>();
		ArrayList<AlarmInfo> alarms = new ArrayList<>();
		List<RouteSegmentResult> segments = convertVectorResult(computeDirections, locations, list, alarms, ctx);
		if (calculateFirstAndLastPoint) {
			introduceFirstPointAndLastPoint(locations, computeDirections, segments, start, end, ctx);
		}
		this.locations = Collections.unmodifiableList(locations);
		this.segments = Collections.unmodifiableList(segments);
		this.listDistance = new int[locations.size()];
		calculateIntermediateIndexes(ctx, this.locations, intermediates, computeDirections, this.intermediatePoints);
		updateListDistanceTime(this.listDistance, this.locations);
		this.appMode = mode;
		this.routeService = mode.getRouteService();

		this.directions = Collections.unmodifiableList(computeDirections);
		updateDirectionsTime(this.directions, this.listDistance);
		this.alarmInfo = Collections.unmodifiableList(alarms);
		this.routeRecalcDistance = ctx.getSettings().ROUTE_RECALCULATION_DISTANCE.getModeValue(mode);
		this.routeVisibleAngle = routeService == RouteService.STRAIGHT ?
				ctx.getSettings().ROUTE_STRAIGHT_ANGLE.getModeValue(mode) : 0;
		this.initialCalculation = initialCalculation;
	}

	public ApplicationMode getAppMode() {
		return appMode;
	}

	public List<LocationPoint> getLocationPoints() {
		return locationPoints;
	}

	public List<AlarmInfo> getAlarmInfo() {
		return alarmInfo;
	}

	public List<WorldRegion> getMissingMaps() {
		return missingMaps;
	}

	public boolean hasMissingMaps() {
		return !Algorithms.isEmpty(missingMaps);
	}

	public boolean isInitialCalculation() {
		return initialCalculation;
	}

	private static void calculateIntermediateIndexes(Context ctx, List<Location> locations,
	                                                 List<LatLon> intermediates, List<RouteDirectionInfo> localDirections, int[] intermediatePoints) {
		if (intermediates != null && localDirections != null) {
			int[] interLocations = new int[intermediates.size()];
			for (int currentIntermediate = 0; currentIntermediate < intermediates.size(); currentIntermediate++) {
				double setDistance = DISTANCE_CLOSEST_TO_INTERMEDIATE;
				LatLon currentIntermediatePoint = intermediates.get(currentIntermediate);
				int prevLocation = currentIntermediate == 0 ? 0 : interLocations[currentIntermediate - 1];
				for (int currentLocation = prevLocation; currentLocation < locations.size();
				     currentLocation++) {
					double currentDistance = getDistanceToLocation(locations, currentIntermediatePoint, currentLocation);
					if (currentDistance < setDistance) {
						interLocations[currentIntermediate] = currentLocation;
						setDistance = currentDistance;
					} else if (currentDistance > DISTANCE_THRESHOLD_TO_INTERMEDIATE &&
							setDistance < DISTANCE_THRESHOLD_TO_INTERMEDIATE) {
						// finish search
						break;
					}

				}
				if (setDistance == DISTANCE_CLOSEST_TO_INTERMEDIATE) {
					return;
				}
			}


			int currentDirection = 0;
			int currentIntermediate = 0;
			while (currentIntermediate < intermediates.size() && currentDirection < localDirections.size()) {
				int locationIndex = localDirections.get(currentDirection).routePointOffset;
				if (locationIndex >= interLocations[currentIntermediate]) {
					// split directions
					if (locationIndex > interLocations[currentIntermediate]
							&& getDistanceToLocation(locations, intermediates.get(currentIntermediate), locationIndex) > 50) {
						RouteDirectionInfo toSplit = localDirections.get(currentDirection);
						RouteDirectionInfo info = new RouteDirectionInfo(localDirections.get(currentDirection).getAverageSpeed(), TurnType.straight());
						info.setRef(toSplit.getRef());
						info.setStreetName(toSplit.getStreetName());
						info.setRouteDataObject(toSplit.getRouteDataObject());
						info.setDestinationName(toSplit.getDestinationName());
						info.routePointOffset = interLocations[currentIntermediate];
						info.setDescriptionRoute(ctx.getString(R.string.route_head));//; //$NON-NLS-1$
						localDirections.add(currentDirection, info);
					}
					intermediatePoints[currentIntermediate] = currentDirection;
					currentIntermediate++;
				}
				currentDirection++;
			}
		}
	}

	private static double getDistanceToLocation(List<Location> locations, LatLon p, int currentLocation) {
		return MapUtils.getDistance(p,
				locations.get(currentLocation).getLatitude(), locations.get(currentLocation).getLongitude());
	}

	private static void attachAlarmInfo(List<AlarmInfo> alarms, RouteSegmentResult res, int intId, int locInd) {
		RouteDataObject rdo = res.getObject();
		int[] pointTypes = rdo.getPointTypes(intId);
		if (pointTypes != null) {
			RouteRegion reg = rdo.region;
			for (int r = 0; r < pointTypes.length; r++) {
				RouteTypeRule typeRule = reg.quickGetEncodingRule(pointTypes[r]);
				int x31 = rdo.getPoint31XTile(intId);
				int y31 = rdo.getPoint31YTile(intId);
				Location loc = new Location("");
				loc.setLatitude(MapUtils.get31LatitudeY(y31));
				loc.setLongitude(MapUtils.get31LongitudeX(x31));
				AlarmInfo info = AlarmInfo.createAlarmInfo(typeRule, locInd, loc);
				if (info != null) {
					// For STOP and TRAFFIC_CALMING first check if it has directional info
					boolean forward = res.isForwardDirection();
					boolean directionApplicable = rdo.isDirectionApplicable(forward, intId,
							info.getType() == AlarmInfoType.STOP ? res.getStartPointIndex() : -1, res.getEndPointIndex());
					if (!directionApplicable) {
						continue;
					}
					alarms.add(info);
				}
			}
		}
	}

	public double getRouteRecalcDistance() {
		return routeRecalcDistance;
	}

	public RouteService getRouteService() {
		return routeService;
	}

	public double getRouteVisibleAngle() {
		return routeVisibleAngle;
	}

	@Nullable
	public List<RouteSegmentResult> getOriginalRoute() {
		return getOriginalRoute(0);
	}

	@Nullable
	public List<RouteSegmentResult> getOriginalRoute(int startIndex) {
		return getOriginalRoute(startIndex, segments.size(), true);
	}

	@Nullable
	public List<RouteSegmentResult> getOriginalRoute(int startIndex, boolean includeFirstSegment) {
		return getOriginalRoute(startIndex, segments.size(), includeFirstSegment);
	}

	@Nullable
	public List<RouteSegmentResult> getOriginalRoute(int startIndex, int endIndex, boolean includeFirstSegment) {
		if (segments.size() == 0) {
			return null;
		}
		List<RouteSegmentResult> list = new ArrayList<>();
		if (includeFirstSegment) {
			list.add(segments.get(startIndex));
		}
		for (int i = ++startIndex; i < endIndex; i++) {
			if (segments.get(i - 1) != segments.get(i)) {
				list.add(segments.get(i));
			}
		}
		return list;
	}

	/**
	 * PREPARATION
	 */
	private static List<RouteSegmentResult> convertVectorResult(List<RouteDirectionInfo> directions,
	                                                            List<Location> locations, List<RouteSegmentResult> list,
	                                                            List<AlarmInfo> alarms, OsmandApplication ctx) {
		float prevDirectionTime = 0;
		float prevDirectionDistance = 0;
		double lastHeight = HEIGHT_UNDEFINED;
		List<RouteSegmentResult> segmentsToPopulate = new ArrayList<>();
		AlarmInfo tunnelAlarm = null;
		for (int routeInd = 0; routeInd < list.size(); routeInd++) {
			RouteSegmentResult s = list.get(routeInd);
			float[] vls = s.getObject().calculateHeightArray();
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			int prevLocationSize = locations.size();
			if (s.getObject().tunnel()) {
				if (tunnelAlarm == null) {
					LatLon latLon = s.getPoint(i);
					tunnelAlarm = new AlarmInfo(AlarmInfoType.TUNNEL, prevLocationSize);
					tunnelAlarm.setLatLon(latLon.getLatitude(), latLon.getLongitude());
					tunnelAlarm.setFloatValue(s.getDistance());
					alarms.add(tunnelAlarm);
				} else {
					tunnelAlarm.setFloatValue(tunnelAlarm.getFloatValue() + s.getDistance());
				}
			} else {
				if (tunnelAlarm != null) {
					tunnelAlarm.setLastLocationIndex(locations.size());
				}
				tunnelAlarm = null;
			}

			boolean lastSegment = routeInd + 1 == list.size();
			RouteSegmentResult nextSegment = lastSegment ? null : list.get(routeInd + 1);
			boolean gapAfter = nextSegment != null && !nextSegment.continuesBeyondRouteSegment(s);
			while (true) {
				if (i == s.getEndPointIndex() && !gapAfter && !lastSegment) {
					break;
				}
				Location n = new Location("");
				LatLon point = s.getPoint(i);
				n.setLatitude(point.getLatitude());
				n.setLongitude(point.getLongitude());
				n.setSpeed(s.getSegmentSpeed());
				if (vls != null && i * 2 + 1 < vls.length) {
					float h = vls[2 * i + 1];
					n.setAltitude(h);
					if (lastHeight == HEIGHT_UNDEFINED && locations.size() > 0) {
						for (Location l : locations) {
							if (!l.hasAltitude()) {
								l.setAltitude(h);
							}
						}
					}
					lastHeight = h;
				}
				locations.add(n);
				attachAlarmInfo(alarms, s, i, locations.size());
				segmentsToPopulate.add(s);
				if (i == s.getEndPointIndex()) {
					break;
				}
				if (plus) {
					i++;
				} else {
					i--;
				}
			}
			TurnType turn = s.getTurnType();

			if (turn != null) {
				RouteDirectionInfo info = new RouteDirectionInfo(s.getSegmentSpeed(), turn);
				if (routeInd < list.size()) {
					int lind = routeInd;
					if (turn.isRoundAbout()) {
						int roundAboutEnd = prevLocationSize;
						// take next name for roundabout (not roundabout name)
						while (lind < list.size() - 1 && list.get(lind).getObject().roundabout()) {
							roundAboutEnd += Math.abs(list.get(lind).getEndPointIndex()
									- list.get(lind).getStartPointIndex());
							lind++;
						}
						// Consider roundabout end.
						info.routeEndPointOffset = roundAboutEnd;
					}
					RouteSegmentResult current = (routeInd == lind) ? s : list.get(lind);
					String lang = ctx.getSettings().MAP_PREFERRED_LOCALE.get();
					boolean transliterate = ctx.getSettings().MAP_TRANSLITERATE_NAMES.get();
					info.setStreetName(current.getStreetName(lang, transliterate, list, lind));
					info.setDestinationName(current.getDestinationName(lang, transliterate, list, lind));

					RouteDataObject rdoWithShield = null;
					RouteDataObject rdoWithoutShield = null;
					if (s.getObject().isExitPoint()) {
						ExitInfo exitInfo = new ExitInfo();
						exitInfo.setRef(current.getObject().getExitRef());
						exitInfo.setExitStreetName(current.getObject().getExitName());
						info.setExitInfo(exitInfo);
						if (routeInd > 0 && (exitInfo.getRef() != null || exitInfo.getExitStreetName() != null)) {
							// set ref and road name (or shield icon) from previous segment because exit point is not consist of highway ref
							RouteSegmentResult previous;
							previous = list.get(routeInd - 1);
							rdoWithoutShield = previous.getObject();
							info.setRef(previous.getRef(lang, transliterate));
							if (info.getRef() != null) {
								rdoWithShield = previous.getObjectWithShield(list, lind);
							}
						}
					}

					if (info.getRef() == null) {
						String ref = current.getObject().getRef(ctx.getSettings().MAP_PREFERRED_LOCALE.get(),
								ctx.getSettings().MAP_TRANSLITERATE_NAMES.get(), current.isForwardDirection());
						String destRef = current.getObject().getDestinationRef(ctx.getSettings().MAP_PREFERRED_LOCALE.get(),
								ctx.getSettings().MAP_TRANSLITERATE_NAMES.get(), current.isForwardDirection());
						rdoWithoutShield = current.getObject();
						if (ref != null && !ref.equals(destRef)) {
							info.setRef(ref);
							rdoWithShield = current.getObjectWithShield(list, lind);
						}
					}

					if (info.getRef() != null) {
						if (rdoWithShield != null) {
							info.setRouteDataObject(rdoWithShield);
						} else {
							info.setRouteDataObject(rdoWithoutShield);
						}
					}
				}

				String description = toString(turn, ctx, false) + " " + RoutingHelperUtils.formatStreetName(info.getStreetName(),
						info.getRef(), info.getDestinationName(), ctx.getString(R.string.towards));
				description = description.trim();
				String[] pointNames = s.getObject().getPointNames(s.getStartPointIndex());
				if (pointNames != null) {
					for (int t = 0; t < pointNames.length; t++) {
						description = description.trim();
						description += " " + pointNames[t];
					}
				}
				info.setDescriptionRoute(description);
				info.routePointOffset = prevLocationSize;
				if (directions.size() > 0 && prevDirectionTime > 0 && prevDirectionDistance > 0) {
					RouteDirectionInfo prev = directions.get(directions.size() - 1);
					prev.setAverageSpeed(prevDirectionDistance / prevDirectionTime);
					prevDirectionDistance = 0;
					prevDirectionTime = 0;
				}
				directions.add(info);
			}
			prevDirectionDistance += s.getDistance();
			prevDirectionTime += s.getSegmentTime();
		}
		if (directions.size() > 0 && prevDirectionTime > 0 && prevDirectionDistance > 0) {
			RouteDirectionInfo prev = directions.get(directions.size() - 1);
			prev.setAverageSpeed(prevDirectionDistance / prevDirectionTime);
		}
		return segmentsToPopulate;
	}

	protected static void addMissingTurnsToRoute(@NonNull List<Location> locations,
	                                             @NonNull List<RouteDirectionInfo> originalDirections,
	                                             @NonNull ApplicationMode mode,
	                                             @NonNull Context ctx, boolean leftSide,
	                                             boolean useLocationTime) {
		if (locations.isEmpty()) {
			return;
		}
		// speed m/s
		float speed = mode.getDefaultSpeed();
		int minDistanceForTurn = mode.getMinDistanceForTurn();
		List<RouteDirectionInfo> computeDirections = new ArrayList<>();

		int[] listDistance = new int[locations.size()];
		listDistance[locations.size() - 1] = 0;
		for (int i = locations.size() - 1; i > 0; i--) {
			listDistance[i - 1] = Math.round(locations.get(i - 1).distanceTo(locations.get(i)));
			listDistance[i - 1] += listDistance[i];
		}

		int previousLocation = 0;
		int prevBearingLocation = 0;
		int prevStrictLocation = 0;
		float startSpeed = speed;
		Location prevLoc = locations.get(prevStrictLocation);
		if (useLocationTime && locations.size() > 1 && locations.get(1).getTime() > 0 && prevLoc.getTime() > 0
				&& locations.get(1).getTime() > prevLoc.getTime()) {
			startSpeed = locations.get(1).distanceTo(prevLoc) / ((locations.get(1).getTime() - prevLoc.getTime()) / 1000f);
		}
		RouteDirectionInfo previousInfo = new RouteDirectionInfo(startSpeed, TurnType.straight());
		previousInfo.routePointOffset = 0;
		previousInfo.setDescriptionRoute(ctx.getString(R.string.route_head));
		computeDirections.add(previousInfo);

		int distForTurn = 0;
		float previousBearing = 0;
		int startTurnPoint = 0;


		for (int i = 1; i < locations.size() - 1; i++) {

			Location next = locations.get(i + 1);
			Location current = locations.get(i);
			float bearing = current.bearingTo(next);
			// try to get close to current location if possible
			while (prevBearingLocation < i - 1) {
				if (locations.get(prevBearingLocation + 1).distanceTo(current) > 70) {
					prevBearingLocation++;
				} else {
					break;
				}
			}

			if (distForTurn == 0) {
				// measure only after turn
				previousBearing = locations.get(prevBearingLocation).bearingTo(current);
				startTurnPoint = i;
			}

			TurnType type = null;
			String description = null;
			float delta = previousBearing - bearing;
			while (delta < 0) {
				delta += 360;
			}
			while (delta > 360) {
				delta -= 360;
			}

			distForTurn += locations.get(i).distanceTo(locations.get(i + 1));
			if (i < locations.size() - 1 && distForTurn < minDistanceForTurn) {
				// For very smooth turn we try to accumulate whole distance
				// simply skip that turn needed for situation
				// 1) if you are going to have U-turn - not 2 left turns
				// 2) if there is a small gap between roads (turn right and after 4m next turn left) - so the direction head
				continue;
			}


			if (delta > 30 && delta < 330) {

				if (delta < 60) {
					type = TurnType.valueOf(TurnType.TSLL, leftSide);
					description = ctx.getString(R.string.route_tsll);
				} else if (delta < 120) {
					type = TurnType.valueOf(TurnType.TL, leftSide);
					description = ctx.getString(R.string.route_tl);
				} else if (delta < 150) {
					type = TurnType.valueOf(TurnType.TSHL, leftSide);
					description = ctx.getString(R.string.route_tshl);
				} else if (delta < 180) {
					if (leftSide) {
						type = TurnType.valueOf(TurnType.TSHL, leftSide);
						description = ctx.getString(R.string.route_tshl);
					} else {
						type = TurnType.valueOf(TurnType.TU, leftSide);
						description = ctx.getString(R.string.route_tu);
					}
				} else if (delta == 180) {
					type = TurnType.valueOf(TurnType.TU, leftSide);
					description = ctx.getString(R.string.route_tu);
				} else if (delta < 210) {
					if (leftSide) {
						type = TurnType.valueOf(TurnType.TU, leftSide);
						description = ctx.getString(R.string.route_tu);
					} else {
						description = ctx.getString(R.string.route_tshr);
						type = TurnType.valueOf(TurnType.TSHR, leftSide);
					}
				} else if (delta < 240) {
					description = ctx.getString(R.string.route_tshr);
					type = TurnType.valueOf(TurnType.TSHR, leftSide);
				} else if (delta < 300) {
					description = ctx.getString(R.string.route_tr);
					type = TurnType.valueOf(TurnType.TR, leftSide);
				} else {
					description = ctx.getString(R.string.route_tslr);
					type = TurnType.valueOf(TurnType.TSLR, leftSide);
				}

				// calculate for previousRoute 
				previousInfo.distance = listDistance[previousLocation] - listDistance[i];
				type.setTurnAngle(360 - delta);
				Location strictPrevious = locations.get(prevStrictLocation);
				if (useLocationTime && current.getTime() > 0 && strictPrevious.getTime() > 0
						&& current.getTime() > strictPrevious.getTime()) {
					float directionSpeed = previousInfo.distance / ((current.getTime() - strictPrevious.getTime()) / 1000f);
					previousInfo.setAverageSpeed(directionSpeed);
				}
				previousInfo = new RouteDirectionInfo(speed, type);
				previousInfo.setDescriptionRoute(description);
				previousInfo.routePointOffset = startTurnPoint;
				computeDirections.add(previousInfo);
				previousLocation = startTurnPoint;
				prevBearingLocation = i; // for bearing using current location
				prevStrictLocation = i;
			}
			// clear dist for turn
			distForTurn = 0;
		}

		previousInfo.distance = listDistance[previousLocation];
		Location strictPrevious = locations.get(prevStrictLocation);
		Location current = locations.get(locations.size() - 1);
		if (useLocationTime && current.getTime() > 0 && strictPrevious.getTime() > 0
				&& current.getTime() > strictPrevious.getTime()) {
			float directionSpeed = previousInfo.distance / ((current.getTime() - strictPrevious.getTime()) / 1000f);
			previousInfo.setAverageSpeed(directionSpeed);
		}
		if (originalDirections.isEmpty()) {
			originalDirections.addAll(computeDirections);
		} else {
			int currentDirection = 0;
			// one more
			for (int i = 0; i <= originalDirections.size() && currentDirection < computeDirections.size(); i++) {
				while (currentDirection < computeDirections.size()) {
					int distanceAfter = 0;
					if (i < originalDirections.size()) {
						RouteDirectionInfo resInfo = originalDirections.get(i);
						int r1 = computeDirections.get(currentDirection).routePointOffset;
						int r2 = resInfo.routePointOffset;
						distanceAfter = listDistance[resInfo.routePointOffset];
						float dist = locations.get(r1).distanceTo(locations.get(r2));
						// take into account that move roundabout is special turn that could be very lengthy
						if (dist < 100) {
							// the same turn duplicate
							currentDirection++;
							continue; // while cycle
						} else if (computeDirections.get(currentDirection).routePointOffset > resInfo.routePointOffset) {
							// check it at the next point
							break;
						}
					}

					// add turn because it was missed
					RouteDirectionInfo toAdd = computeDirections.get(currentDirection);

					if (i > 0) {
						// update previous
						RouteDirectionInfo previous = originalDirections.get(i - 1);
						toAdd.setAverageSpeed(previous.getAverageSpeed());
					}
					toAdd.distance = listDistance[toAdd.routePointOffset] - distanceAfter;
					if (i < originalDirections.size()) {
						originalDirections.add(i, toAdd);
					} else {
						originalDirections.add(toAdd);
					}
					i++;
					currentDirection++;
				}
			}

		}

		int sum = 0;
		for (int i = originalDirections.size() - 1; i >= 0; i--) {
			originalDirections.get(i).afterLeftTime = sum;
			sum += originalDirections.get(i).getExpectedTime();
		}
	}

	@Nullable
	public QuadRect getLocationsRect() {
		double left = 0, right = 0;
		double top = 0, bottom = 0;
		for (Location p : locations) {
			if (left == 0 && right == 0) {
				left = p.getLongitude();
				right = p.getLongitude();
				top = p.getLatitude();
				bottom = p.getLatitude();
			} else {
				left = Math.min(left, p.getLongitude());
				right = Math.max(right, p.getLongitude());
				top = Math.max(top, p.getLatitude());
				bottom = Math.min(bottom, p.getLatitude());
			}
		}
		return left == 0 && right == 0 ? null : new QuadRect(left, top, right, bottom);
	}

	public static String toString(TurnType type, Context ctx, boolean shortName) {
		if (type.isRoundAbout()) {
			if (shortName) {
				return ctx.getString(R.string.route_roundabout_short, type.getExitOut());
			} else {
				return ctx.getString(R.string.route_roundabout, type.getExitOut());
			}
		} else if (type.getValue() == TurnType.C) {
			return ctx.getString(R.string.route_head);
		} else if (type.getValue() == TurnType.TSLL) {
			return ctx.getString(R.string.route_tsll);
		} else if (type.getValue() == TurnType.TL) {
			return ctx.getString(R.string.route_tl);
		} else if (type.getValue() == TurnType.TSHL) {
			return ctx.getString(R.string.route_tshl);
		} else if (type.getValue() == TurnType.TSLR) {
			return ctx.getString(R.string.route_tslr);
		} else if (type.getValue() == TurnType.TR) {
			return ctx.getString(R.string.route_tr);
		} else if (type.getValue() == TurnType.TSHR) {
			return ctx.getString(R.string.route_tshr);
		} else if (type.getValue() == TurnType.TU) {
			return ctx.getString(R.string.route_tu);
		} else if (type.getValue() == TurnType.TRU) {
			return ctx.getString(R.string.route_tu);
		} else if (type.getValue() == TurnType.KL) {
			return ctx.getString(R.string.route_kl);
		} else if (type.getValue() == TurnType.KR) {
			return ctx.getString(R.string.route_kr);
		}
		return "";
	}

	public String getErrorMessage() {
		return errorMessage;
	}


	/**
	 * PREPARATION
	 * Remove unnecessary go straight from CloudMade.
	 * Remove also last direction because it will be added after.
	 */
	private void removeUnnecessaryGoAhead(List<RouteDirectionInfo> directions) {
		if (directions != null && directions.size() > 1) {
			for (int i = 1; i < directions.size(); ) {
				RouteDirectionInfo r = directions.get(i);
				if (r.getTurnType().getValue() == TurnType.C) {
					RouteDirectionInfo prev = directions.get(i - 1);
					prev.setAverageSpeed((prev.distance + r.distance)
							/ (prev.distance / prev.getAverageSpeed() + r.distance / r.getAverageSpeed()));
					prev.setDistance(prev.distance + r.distance);
					directions.remove(i);
				} else {
					i++;
				}
			}
		}
	}


	/**
	 * PREPARATION
	 * Check points for duplicates (it is very bad for routing) - cloudmade could return it
	 */
	public static void checkForDuplicatePoints(List<Location> locations, List<RouteDirectionInfo> directions) {
		// 
		for (int i = 0; i < locations.size() - 1; ) {
			if (locations.get(i).distanceTo(locations.get(i + 1)) == 0) {
				locations.remove(i);
				if (directions != null) {
					for (RouteDirectionInfo info : directions) {
						if (info.routePointOffset > i) {
							info.routePointOffset--;
						}
					}
				}
			} else {
				i++;
			}
		}
	}

	/**
	 * PREPARATION
	 * If beginning is too far from start point, then introduce GO Ahead
	 *
	 * @param end
	 */
	private static void introduceFirstPointAndLastPoint(List<Location> locations, List<RouteDirectionInfo> directions,
														List<RouteSegmentResult> segs, Location start, LatLon end,
														OsmandApplication ctx) {
		boolean firstPointIntroduced = introduceFirstPoint(locations, directions, segs, start);
		boolean lastPointIntroduced = introduceLastPoint(locations, directions, segs, end);
		if (firstPointIntroduced || lastPointIntroduced) {
			checkForDuplicatePoints(locations, directions);
		}
		RouteDirectionInfo lastDirInf = directions.size() > 0 ? directions.get(directions.size() - 1) : null;
		if ((lastDirInf == null || lastDirInf.routePointOffset < locations.size() - 1) && locations.size() - 1 > 0) {
			int type = TurnType.C;
			Location prevLast = locations.get(locations.size() - 2);
			float lastBearing = prevLast.bearingTo(locations.get(locations.size() - 1));
			float[] compute = new float[2];
			Location.distanceBetween(prevLast.getLatitude(), prevLast.getLongitude(),
					end.getLatitude(), end.getLongitude(), compute);
			float bearingToEnd = compute[1];
			double diff = MapUtils.degreesDiff(lastBearing, bearingToEnd);
			if (Math.abs(diff) > 10) {
				type = diff > 0 ? TurnType.KL : TurnType.KR;
			}
			// Wrong AvgSpeed for the last turn can cause significantly wrong total travel time if calculated route ends on a GPX route segment (then last turn is where GPX is joined again)
			RouteDirectionInfo info = new RouteDirectionInfo(lastDirInf != null ? lastDirInf.getAverageSpeed() : 1, TurnType.valueOf(type, false));
			if (segs != null) {
				RouteSegmentResult lastSegmentResult = segs.get(segs.size() - 1);
				RouteDataObject routeDataObject = lastSegmentResult.getObject();
				info.setRouteDataObject(routeDataObject);
				info.setRef(routeDataObject.getRef(ctx.getSettings().MAP_PREFERRED_LOCALE.get(),
						ctx.getSettings().MAP_TRANSLITERATE_NAMES.get(), lastSegmentResult.isForwardDirection()));
				info.setStreetName(routeDataObject.getName(ctx.getSettings().MAP_PREFERRED_LOCALE.get(),
						ctx.getSettings().MAP_TRANSLITERATE_NAMES.get()));
				info.setDestinationName(routeDataObject.getDestinationName(ctx.getSettings().MAP_PREFERRED_LOCALE.get(),
						ctx.getSettings().MAP_TRANSLITERATE_NAMES.get(), lastSegmentResult.isForwardDirection()));
			}
			info.distance = 0;
			info.afterLeftTime = 0;
			info.routePointOffset = locations.size() - 1;
			directions.add(info);
		}
	}

	private static boolean introduceFirstPoint(List<Location> locations, List<RouteDirectionInfo> directions,
											   List<RouteSegmentResult> segs, Location start) {
		Location firstLocation = Algorithms.isEmpty(locations) ? null : locations.get(0);
		if (firstLocation != null && firstLocation.distanceTo(start) > DISTANCE_THRESHOLD_TO_INTRODUCE_FIRST_AND_LAST_POINTS) {
			// Start location can have wrong altitude
			double firstValidAltitude = getFirstValidAltitude(locations);
			if (!Double.isNaN(firstValidAltitude)) {
				start.setAltitude(firstValidAltitude);
			}

			// add start point
			locations.add(0, start);
			// Add artificial route segment
			if (segs != null) {
				RouteSegmentResult straightSegment = generateStraightLineSegment(start, firstLocation);
				segs.add(0, straightSegment);
			}
			if (directions != null && !directions.isEmpty()) {
				for (RouteDirectionInfo i : directions) {
					i.routePointOffset++;
				}
				float averageSpeed = ApplicationMode.DEFAULT.getDefaultSpeed();
				RouteDirectionInfo info = new RouteDirectionInfo(averageSpeed, TurnType.straight());
				info.routePointOffset = 0;
				// info.setDescriptionRoute(ctx.getString( R.string.route_head));//; //$NON-NLS-1$
				directions.add(0, info);
			}
			return true;
		}
		return false;
	}

	private static double getFirstValidAltitude(List<Location> locations) {
		for (Location location : locations) {
			if (location.hasAltitude()) {
				return location.getAltitude();
			}
		}
		return Double.NaN;
	}

	private static boolean introduceLastPoint(List<Location> locations, List<RouteDirectionInfo> directions,
											  List<RouteSegmentResult> segs, LatLon end) {
		if (!locations.isEmpty()) {
			Location lastFoundLocation = locations.get(locations.size() - 1);

			Location endLocation = new Location(lastFoundLocation.getProvider());
			endLocation.setLatitude(end.getLatitude());
			endLocation.setLongitude(end.getLongitude());

			if (lastFoundLocation.distanceTo(endLocation) > DISTANCE_THRESHOLD_TO_INTRODUCE_FIRST_AND_LAST_POINTS) {
				int type = TurnType.C;
				if (directions != null && !directions.isEmpty()) {
					if (locations.size() > 2) {
						Location prevLast = locations.get(locations.size() - 2);
						float lastBearing = prevLast.bearingTo(lastFoundLocation);
						float bearingToEnd = lastFoundLocation.bearingTo(endLocation);
						double diff = MapUtils.degreesDiff(lastBearing, bearingToEnd);
						if (Math.abs(diff) > 10) {
							if (Math.abs(diff) < 60) {
								type = diff > 0 ? TurnType.TSLL : TurnType.TSLR;
							} else {
								type = diff > 0 ? TurnType.TL : TurnType.TR;
							}
						}

						float averageSpeed = ApplicationMode.DEFAULT.getDefaultSpeed();
						RouteDirectionInfo info = new RouteDirectionInfo(averageSpeed, TurnType.valueOf(type, false));
						info.routePointOffset = locations.size() - 1;
						directions.add(info);
					}
				}
				
				// add end point
				locations.add(endLocation);
				// Add artificial route segment
				if (segs != null) {
					RouteSegmentResult straightSegment = generateStraightLineSegment(lastFoundLocation, endLocation);
					straightSegment.setTurnType(TurnType.valueOf(type, false));
					segs.add(straightSegment);
				}
				return true;
			}
		}
		return false;
	}

	@NonNull
	private static RouteSegmentResult generateStraightLineSegment(@NonNull Location start, @NonNull Location end) {
		float averageSpeed = ApplicationMode.DEFAULT.getDefaultSpeed();
		LatLon startLatLon = new LatLon(start.getLatitude(), start.getLongitude());
		LatLon endLatLon = new LatLon(end.getLatitude(), end.getLongitude());
		List<LatLon> straightSegment = Arrays.asList(startLatLon, endLatLon);
		return RoutePlannerFrontEnd.generateStraightLineSegment(averageSpeed, straightSegment);
	}

	/**
	 * PREPARATION
	 * At the end always update listDistance local vars and time
	 */
	private static void updateListDistanceTime(int[] listDistance, List<Location> locations) {
		if (listDistance.length > 0) {
			listDistance[locations.size() - 1] = 0;
			for (int i = locations.size() - 1; i > 0; i--) {
				listDistance[i - 1] = Math.round(locations.get(i - 1).distanceTo(locations.get(i)));
				listDistance[i - 1] += listDistance[i];
			}
		}
	}

	/**
	 * PREPARATION
	 * At the end always update listDistance local vars and time
	 */
	private static void updateDirectionsTime(List<RouteDirectionInfo> directions, int[] listDistance) {
		int sumExpectedTime = 0;
		for (int i = directions.size() - 1; i >= 0; i--) {
			directions.get(i).distance = listDistance[directions.get(i).routePointOffset];
			if (i < directions.size() - 1) {
				directions.get(i).distance -= listDistance[directions.get(i + 1).routePointOffset];
			}
			sumExpectedTime += directions.get(i).getExpectedTime();
			directions.get(i).afterLeftTime = sumExpectedTime;
		}
	}

	//////////////////// MUST BE ALL SYNCHRONIZED ??? //////////////////////

	public List<Location> getImmutableAllLocations() {
		return locations;
	}

	public List<RouteDirectionInfo> getImmutableAllDirections() {
		return directions;
	}

	public List<RouteSegmentResult> getImmutableAllSegments() {
		return segments;
	}

	public List<Location> getRouteLocations() {
		if (currentRoute < locations.size()) {
			return locations.subList(currentRoute, locations.size());
		}
		return Collections.emptyList();
	}

	public int getRouteDistanceToFinish(int posFromCurrentIndex) {
		if (listDistance != null && currentRoute + posFromCurrentIndex < listDistance.length) {
			return listDistance[currentRoute + posFromCurrentIndex];
		}
		return 0;
	}

	@Nullable
	public RouteSegmentResult getCurrentSegmentResult() {
		int cs = currentRoute > 0 ? currentRoute - 1 : 0;
		if (cs < segments.size()) {
			return segments.get(cs);
		}
		return null;
	}

	@Nullable
	public RouteSegmentResult getNextStreetSegmentResult() {
		int cs = currentRoute > 0 ? currentRoute - 1 : 0;
		while (cs < segments.size()) {
			RouteSegmentResult segmentResult = segments.get(cs);
			if (!Algorithms.isEmpty(segmentResult.getObject().getName())) {
				return segmentResult;
			}
			cs++;
		}
		return null;
	}

	@Nullable
	public List<RouteSegmentResult> getUpcomingTunnel(float distToStart) {
		int cs = currentRoute > 0 ? currentRoute - 1 : 0;
		if (cs < segments.size()) {
			RouteSegmentResult prev = null;
			boolean tunnel = false;
			while (cs < segments.size() && distToStart > 0) {
				RouteSegmentResult segment = segments.get(cs);
				if (segment != prev) {
					if (segment.getObject().tunnel()) {
						tunnel = true;
						break;
					} else {
						distToStart -= segment.getDistance();
						prev = segment;
					}
				}
				cs++;
			}
			if (tunnel) {
				List<RouteSegmentResult> list = new ArrayList<>();
				while (cs < segments.size()) {
					RouteSegmentResult segment = segments.get(cs);
					if (segment != prev) {
						if (segment.getObject().tunnel()) {
							list.add(segment);
						} else {
							break;
						}
						prev = segment;
					}
					cs++;
				}
				return list;
			}
		}

		return null;
	}

	public float getCurrentMaxSpeed(int profile) {
		RouteSegmentResult res = getCurrentSegmentResult();
		if (res != null) {
			return res.getObject().getMaximumSpeed(res.isForwardDirection(), profile);
		}
		return 0;
	}

	public float getRoutingTime() {
		return routingTime;
	}

	public int getVisitedSegments() {
		return visitedSegments;
	}

	public float getCalculateTime() {
		return calculateTime;
	}

	public int getLoadedTiles() {
		return loadedTiles;
	}

	public int getWholeDistance() {
		if (listDistance.length > 0) {
			return listDistance[0];
		}
		return 0;
	}

	public boolean isCalculated() {
		return !locations.isEmpty();
	}

	public boolean isEmpty() {
		return locations.isEmpty() || currentRoute >= locations.size();
	}


	public void updateCurrentRoute(int currentRoute) {
		this.currentRoute = currentRoute;
		while (currentDirectionInfo < directions.size() - 1
				&& directions.get(currentDirectionInfo + 1).routePointOffset < currentRoute
				&& directions.get(currentDirectionInfo + 1).routeEndPointOffset < currentRoute) {
			currentDirectionInfo++;
		}
		while (nextIntermediate < intermediatePoints.length) {
			RouteDirectionInfo dir = directions.get(intermediatePoints[nextIntermediate]);
			if (dir.routePointOffset < currentRoute) {
				nextIntermediate++;
			} else {
				break;
			}
		}
	}

	public int getCurrentRoute() {
		return currentRoute;
	}

	public void passIntermediatePoint() {
		nextIntermediate++;
	}

	public int getNextIntermediate() {
		return nextIntermediate;
	}

	@Nullable
	public Location getLocationFromRouteDirection(RouteDirectionInfo info) {
		if (info != null && locations != null && info.routePointOffset < locations.size()) {
			return locations.get(info.routePointOffset);
		}
		return null;
	}


	/*public */NextDirectionInfo getNextRouteDirectionInfo(NextDirectionInfo info, Location fromLoc, boolean toSpeak) {
		int dirInfo = currentDirectionInfo;
		if (dirInfo < directions.size()) {
			// Locate next direction of interest
			int nextInd = dirInfo + 1;
			if (toSpeak) {
				while (nextInd < directions.size()) {
					RouteDirectionInfo i = directions.get(nextInd);
					if (i.getTurnType() != null && !i.getTurnType().isSkipToSpeak()) {
						break;
					}
					nextInd++;
				}
			}
			int dist = getDistanceToFinish(fromLoc);
			if (nextInd < directions.size()) {
				info.directionInfo = directions.get(nextInd);
				if (directions.get(nextInd).routePointOffset <= currentRoute
						&& currentRoute <= directions.get(nextInd).routeEndPointOffset)
					// We are not into a puntual direction.
					dist -= getListDistance(directions.get(nextInd).routeEndPointOffset);
				else
					dist -= getListDistance(directions.get(nextInd).routePointOffset);
			}
			if (intermediatePoints != null && nextIntermediate < intermediatePoints.length) {
				info.intermediatePoint = intermediatePoints[nextIntermediate] == nextInd;
			}
			info.directionInfoInd = nextInd;
			info.distanceTo = dist;
			return info;
		}
		info.directionInfoInd = -1;
		info.distanceTo = -1;
		info.directionInfo = null;
		return info;
	}

	/*public */
	@Nullable
	NextDirectionInfo getNextRouteDirectionInfoAfter(NextDirectionInfo prev, NextDirectionInfo next, boolean toSpeak) {
		int dirInfo = prev.directionInfoInd;
		if (dirInfo < directions.size() && prev.directionInfo != null) {
			int dist = getListDistance(prev.directionInfo.routePointOffset);
			int nextInd = dirInfo + 1;
			if (toSpeak) {
				while (nextInd < directions.size()) {
					RouteDirectionInfo i = directions.get(nextInd);
					if (i.getTurnType() != null && !i.getTurnType().isSkipToSpeak()) {
						break;
					}
					nextInd++;
				}
			}
			if (nextInd < directions.size()) {
				next.directionInfo = directions.get(nextInd);
				dist -= getListDistance(directions.get(nextInd).routePointOffset);
			}
			if (intermediatePoints != null && nextIntermediate < intermediatePoints.length) {
				next.intermediatePoint = intermediatePoints[nextIntermediate] == nextInd;
			}
			next.distanceTo = dist;
			next.directionInfoInd = nextInd;
			return next;
		}
		next.directionInfoInd = -1;
		next.distanceTo = -1;
		next.directionInfo = null;
		return null;
	}


	public List<RouteDirectionInfo> getRouteDirections() {
		if (currentDirectionInfo < directions.size() - 1) {
			if (cacheCurrentTextDirectionInfo != currentDirectionInfo) {
				cacheCurrentTextDirectionInfo = currentDirectionInfo;
				List<RouteDirectionInfo> list = currentDirectionInfo == 0 ? directions :
						directions.subList(currentDirectionInfo + 1, directions.size());
				cacheAgreggatedDirections = new ArrayList<>();
				RouteDirectionInfo p = null;
				for (RouteDirectionInfo i : list) {
//					if(p == null || !i.getTurnType().isSkipToSpeak() ||
//							(!Algorithms.objectEquals(p.getRef(), i.getRef()) &&
//									!Algorithms.objectEquals(p.getStreetName(), i.getStreetName()))) {
					if (p == null ||
							(i.getTurnType() != null && !i.getTurnType().isSkipToSpeak())) {
						p = new RouteDirectionInfo(i.getAverageSpeed(), i.getTurnType());
						p.routePointOffset = i.routePointOffset;
						p.routeEndPointOffset = i.routeEndPointOffset;
						p.setRouteDataObject(i.getRouteDataObject());
						p.setDestinationName(i.getDestinationName());
						p.setRef(i.getRef());
						p.setStreetName(i.getStreetName());
						p.setDescriptionRoute(i.getDescriptionRoutePart());
						cacheAgreggatedDirections.add(p);
					}
					float time = i.getExpectedTime() + p.getExpectedTime();
					p.distance += i.distance;
					p.setAverageSpeed(p.distance / time);
					p.afterLeftTime = i.afterLeftTime;
				}
			}
			return cacheAgreggatedDirections;
		}
		return Collections.emptyList();
	}

	@Nullable
	public Location getNextRouteLocation() {
		if (currentRoute < locations.size()) {
			return locations.get(currentRoute);
		}
		return null;
	}

	@Nullable
	public Location getNextRouteLocation(int after) {
		if (currentRoute + after >= 0 && currentRoute + after < locations.size()) {
			return locations.get(currentRoute + after);
		}
		return null;
	}

	@Nullable
	public Location getRouteLocationByDistance(int meters) {
		int increase = meters > 0 ? 1 : -1;
		for (int i = increase; currentRoute < locations.size() && currentRoute + i >= 0 && currentRoute + i < locations.size(); i = i + increase) {
			Location loc = locations.get(currentRoute + i);
			double dist = MapUtils.getDistance(locations.get(currentRoute), loc);
			if (dist >= Math.abs(meters)) {
				return loc;
			}
		}
		return null;
	}

	public boolean directionsAvailable() {
		return currentDirectionInfo < directions.size();
	}

	@Nullable
	public RouteDirectionInfo getCurrentDirection() {
		if (currentDirectionInfo < directions.size()) {
			return directions.get(currentDirectionInfo);
		}
		return null;
	}

	public int getDistanceToPoint(Location lastKnownLocation, int locationIndex ) {
		int dist = getDistanceToPoint(locationIndex);
		Location next = getNextRouteLocation();
		if (lastKnownLocation != null && next != null) {
			dist += MapUtils.getDistance(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
					next.getLatitude(), next.getLongitude());
		}
		return dist;
	}

	public int getDistanceToPoint(int locationIndex) {
		if (listDistance != null && currentRoute < listDistance.length && locationIndex < listDistance.length &&
				locationIndex > currentRoute) {
			return listDistance[currentRoute] - listDistance[locationIndex];
		}
		return 0;
	}

	public int getDistanceFromPoint(int locationIndex) {
		if (listDistance != null && locationIndex < listDistance.length) {
			return listDistance[locationIndex];
		}
		return 0;
	}

	public int getDistanceFromStart() {
		if (listDistance != null && currentRoute > 0 && currentRoute < listDistance.length) {
			return listDistance[0] - listDistance[currentRoute - 1];
		}
		return 0;
	}

	public boolean isPointPassed(int locationIndex) {
		return locationIndex <= currentRoute;
	}

	public int getDistanceToFinish(Location fromLoc) {
		Location ap = this.currentStraightAnglePoint;
		int rp = Math.max(currentStraightAngleRoute, currentRoute);
		if (listDistance != null && rp < listDistance.length) {
			int dist = listDistance[rp];
			Location l = locations.get(rp);
			if (ap != null) {
				if (fromLoc != null) {
					dist += fromLoc.distanceTo(ap);
				}
				dist += ap.distanceTo(l);
			} else if (fromLoc != null) {
				dist += fromLoc.distanceTo(l);
			}
			return dist;
		}
		return 0;
	}

	public int getDistanceToNextIntermediate(Location fromLoc) {
		int dist = getDistanceToFinish(fromLoc);
		if (listDistance != null && currentRoute < listDistance.length) {
			if (nextIntermediate >= intermediatePoints.length) {
				return 0;
			} else {
				int directionInd = intermediatePoints[nextIntermediate];
				return dist - getListDistance(directions.get(directionInd).routePointOffset);
			}
		}
		return 0;
	}

	public int getIndexOfIntermediate(int countFromLast) {
		int j = intermediatePoints.length - countFromLast - 1;
		if (j < intermediatePoints.length && j >= 0) {
			int i = intermediatePoints[j];
			return directions.get(i).routePointOffset;
		}
		return -1;
	}

	public int getIntermediatePointsToPass() {
		if (nextIntermediate >= intermediatePoints.length) {
			return 0;
		}
		return intermediatePoints.length - nextIntermediate;
	}

	public int getLeftTime(Location fromLoc) {
		int time = 0;
		if (currentDirectionInfo < directions.size()) {
			RouteDirectionInfo current = directions.get(currentDirectionInfo);
			time = current.afterLeftTime + getLeftTimeToNextDirection(fromLoc);
		}
		return time;
	}

	public int getLeftTimeToNextTurn(Location fromLoc) {
		int time = 0;
		if (currentDirectionInfo < directions.size()) {
			RouteDirectionInfo current = directions.get(currentDirectionInfo);
			// Locate next direction of interest
			int nextInd = currentDirectionInfo + 1;
			while (nextInd < directions.size()) {
				RouteDirectionInfo i = directions.get(nextInd);
				if (i.getTurnType() != null && !i.getTurnType().isSkipToSpeak()) {
					break;
				}
				nextInd++;
				time += i.getExpectedTime();
			}
			time += getLeftTimeToNextDirection(fromLoc);
		}
		return Math.max(time, 0);
	}

	public int getLeftTimeToNextDirection(Location fromLoc) {
		if (currentDirectionInfo < directions.size()) {
			RouteDirectionInfo current = directions.get(currentDirectionInfo);
			int distanceToNextTurn = getListDistance(currentRoute);
			distanceToNextTurn -= getListDistance(current.routePointOffset);
			Location l = locations.get(currentRoute);
			if (fromLoc != null) {
				distanceToNextTurn += fromLoc.distanceTo(l);
			}
			return (int) (distanceToNextTurn / current.getAverageSpeed());
		}
		return 0;
	}

	public int getLeftTimeToNextIntermediate(Location fromLoc) {
		if (nextIntermediate >= intermediatePoints.length) {
			return 0;
		}
		return getLeftTime(fromLoc) - directions.get(intermediatePoints[nextIntermediate]).afterLeftTime;
	}

	private int getListDistance(int index) {
		return listDistance.length > index ? listDistance[index] : 0;
	}

	public int getCurrentStraightAngleRoute() {
		return Math.max(currentStraightAngleRoute, currentRoute);
	}

	public Location getCurrentStraightAnglePoint() {
		return currentStraightAnglePoint;
	}

	public void updateNextVisiblePoint(int nextPoint, Location mp) {
		currentStraightAnglePoint = mp;
		currentStraightAngleRoute = nextPoint;
	}

	public static class NextDirectionInfo {
		public RouteDirectionInfo directionInfo;
		public int distanceTo;
		public boolean intermediatePoint;
		public String pointName;
		public int imminent;
		private int directionInfoInd;
	}

}
