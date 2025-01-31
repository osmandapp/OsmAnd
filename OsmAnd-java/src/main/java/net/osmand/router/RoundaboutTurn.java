package net.osmand.router;

import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class RoundaboutTurn {
	private final List<RouteSegmentResult> routeSegmentResults;
	private final RouteSegmentResult current;
	private final RouteSegmentResult prev;
	private final int iteration;
	private final boolean roundabout;
	private final boolean miniRoundabout;
	private final boolean prevRoundabout;
	private final boolean leftSide;

	public RoundaboutTurn(List<RouteSegmentResult> routeSegmentResults, int i, boolean leftSide) {
		this.routeSegmentResults = routeSegmentResults;
		this.leftSide = leftSide;
		iteration = i;
		current = routeSegmentResults.size() > i ? routeSegmentResults.get(i) : null;
		prev = i > 0 && routeSegmentResults.size() > i ? routeSegmentResults.get(i - 1) : null;
		roundabout = current != null && current.getObject().roundabout();
		prevRoundabout = prev != null && prev.getObject().roundabout();
		miniRoundabout = isMiniRoundabout(prev, current);
	}

	public boolean isRoundaboutExist() {
		return roundabout || miniRoundabout || prevRoundabout;
	}

	public TurnType getRoundaboutType() {
		if (prev == null || current == null) {
			return null;
		}
		if (prevRoundabout) {
			// already analyzed!
			return null;
		}
		if (roundabout) {
			return processRoundaboutTurn();
		}
		if (miniRoundabout) {
			return processMiniRoundaboutTurn();
		}
		return null;
	}

	private boolean isMiniRoundabout(RouteSegmentResult prev, RouteSegmentResult current) {
		if (prev == null || current == null) {
			return false;
		}
		int[] prevTypes = prev.getObject().getPointTypes(prev.getEndPointIndex());
		int[] currentTypes = current.getObject().getPointTypes(current.getStartPointIndex());
		if (prevTypes != null && currentTypes != null) {
			Integer miniType = prev.getObject().region.decodingRules.get("highway#mini_roundabout");
			if (miniType == null) {
				return false;
			}
			boolean p = false;
			boolean c = false;
			for (int t : prevTypes) {
				if (t == miniType) {
					p = true;
					break;
				}
			}
			for (int t : currentTypes) {
				if (t == miniType) {
					c = true;
					break;
				}
			}
			return p && c;
		}
		return false;
	}

	private TurnType processRoundaboutTurn() {
		int exit = 1;
		RouteSegmentResult last = current;
		RouteSegmentResult firstRoundabout = current;
		RouteSegmentResult lastRoundabout = current;
		List<Float> turnAngles = new ArrayList<>();
		for (int j = iteration; j < routeSegmentResults.size(); j++) {
			RouteSegmentResult rnext = routeSegmentResults.get(j);
			last = rnext;
			if (rnext.getObject().roundabout()) {
				lastRoundabout = rnext;
				boolean plus = rnext.getStartPointIndex() < rnext.getEndPointIndex();
				int k = rnext.getStartPointIndex();
				if (j == iteration) {
					// first exit could be immediately after roundabout enter
//					k = plus ? k + 1 : k - 1;
				}
				while (k != rnext.getEndPointIndex()) {
					int attachedRoads = rnext.getAttachedRoutes(k).size();
					if (attachedRoads > 0) {
						exit++;
//						rnext.getAttachedRoutes(k).
						float turnAngle = calculateRoundaboutTurnAngle(rnext, firstRoundabout, rnext, k);
						turnAngles.add(turnAngle);
					}
					k = plus ? k + 1 : k - 1;
				}
			} else {
				break;
			}
		}
		// combine all roundabouts
		TurnType t = TurnType.getExitTurn(exit, 0, leftSide);
		float turnAngle;
		turnAngle = calculateRoundaboutTurnAngle(last, firstRoundabout, lastRoundabout, -1);
		t.setTurnAngle(turnAngle);
		t.setOtherTurnAngles(turnAngles);
		return t;
	}

	private float calculateRoundaboutTurnAngle(RouteSegmentResult last, RouteSegmentResult firstRoundabout,
			RouteSegmentResult lastRoundabout, int ind) {
		float turnAngle;
		// usually covers more than expected
		float turnAngleBasedOnOutRoads = (float) MapUtils.degreesDiff(
				ind < 0 ? last.getBearingBegin() : last.getBearingBegin(ind, RouteSegmentResult.DIST_BEARING_DETECT), prev.getBearingEnd());
		// Angle based on circle method tries
		// 1. to calculate antinormal to roundabout circle on roundabout entrance and
		// 2. normal to roundabout circle on roundabout exit
		// 3. calculate angle difference
		// This method doesn't work if you go from S to N touching only 1 point of roundabout,
		// but it is very important to identify very sharp or very large angle to understand did you pass whole roundabout or small entrance
		float turnAngleBasedOnCircle = (float) -MapUtils.degreesDiff(firstRoundabout.getBearingBegin(), 
				ind < 0 ? last.getBearingEnd() : lastRoundabout.getBearingEnd(ind, RouteSegmentResult.DIST_BEARING_DETECT) + 180);
		if (Math.abs(turnAngleBasedOnOutRoads) > 120) {
			// correctly identify if angle is +- 180, so we approach from left or right side
			turnAngle = turnAngleBasedOnCircle;
		} else {
			turnAngle = turnAngleBasedOnOutRoads;
		}
		return turnAngle;
	}

	private TurnType processMiniRoundaboutTurn() {
		List<RouteSegmentResult> attachedRoutes = current.getAttachedRoutes(current.getStartPointIndex());
		boolean clockwise = current.getObject().isClockwise(leftSide);
		if(!Algorithms.isEmpty(attachedRoutes)) {
			RoadSplitStructure rs = calculateSimpleRoadSplitStructure(attachedRoutes);
			int rightAttaches = rs.roadsOnRight;
			int leftAttaches = rs.roadsOnLeft;
			int exit = 1;
			if (clockwise) {
				exit += leftAttaches;
			} else {
				exit += rightAttaches;
			}
			TurnType t = TurnType.getExitTurn(exit, 0, leftSide);
			float turnAngleBasedOnOutRoads = (float) MapUtils.degreesDiff(current.getBearingBegin(), prev.getBearingEnd());
			float turnAngleBasedOnCircle = (float) -MapUtils.degreesDiff(current.getBearingBegin(), prev.getBearingEnd() + 180);
			if (Math.abs(turnAngleBasedOnOutRoads) > 120) {
				t.setTurnAngle(turnAngleBasedOnCircle) ;
			} else {
				t.setTurnAngle(turnAngleBasedOnOutRoads) ;
			}
			return t;
		}
		return null;
	}

	private RoadSplitStructure calculateSimpleRoadSplitStructure(List<RouteSegmentResult> attachedRoutes) {
		double prevAngle = MapUtils.normalizeDegrees360(prev.getBearingBegin() - 180);
		double currentAngle = MapUtils.normalizeDegrees360(current.getBearingBegin());
		RoadSplitStructure rs = new RoadSplitStructure();
		for (RouteSegmentResult attached : attachedRoutes) {
			double attachedAngle = MapUtils.normalizeDegrees360(attached.getBearingBegin());
			boolean rightSide;
			if (prevAngle > currentAngle) {
				rightSide = attachedAngle > currentAngle && attachedAngle < prevAngle;
			} else {
				boolean leftSide = attachedAngle > prevAngle && attachedAngle < currentAngle;
				rightSide = !leftSide;
			}

			if (rightSide) {
				rs.roadsOnRight++;
			} else {
				rs.roadsOnLeft++;
			}
		}
		return rs;
	}
}
