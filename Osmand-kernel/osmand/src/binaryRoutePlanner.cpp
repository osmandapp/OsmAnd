#include "common.h"
#include <queue>
#include <algorithm>
#include "binaryRead.h"
#include "binaryRoutePlanner.h"
#include <functional>

static bool PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
static const int REVERSE_WAY_RESTRICTION_ONLY = 1024;
static const int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 900;

static const int ROUTE_POINTS = 11;
static const float TURN_DEGREE_MIN = 45;
static const short RESTRICTION_NO_RIGHT_TURN = 1;
static const short RESTRICTION_NO_LEFT_TURN = 2;
static const short RESTRICTION_NO_U_TURN = 3;
static const short RESTRICTION_NO_STRAIGHT_ON = 4;
static const short RESTRICTION_ONLY_RIGHT_TURN = 5;
static const short RESTRICTION_ONLY_LEFT_TURN = 6;
static const short RESTRICTION_ONLY_STRAIGHT_ON = 7;

inline int roadPriorityComparator(float o1DistanceFromStart, float o1DistanceToEnd,
		float o2DistanceFromStart, float o2DistanceToEnd, float heuristicCoefficient) {
	// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
	float f1 = o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd;
	float f2 = o2DistanceFromStart + heuristicCoefficient * o2DistanceToEnd;
	if(f1 == f2) {
		return 0;
	}
	return f1 < f2 ? -1 : 1;
}


static double convert31YToMeters(int y1, int y2) {
	// translate into meters
	return (y1 - y2) * 0.01863f;
}

static double convert31XToMeters(int x1, int x2) {
	// translate into meters
	return (x1 - x2) * 0.011f;
}

	// translate into meters
static double squareRootDist(int x1, int y1, int x2, int y2) {
	double dy = convert31YToMeters(y1, y2);
	double dx = convert31XToMeters(x1, x2);
	return sqrt(dx * dx + dy * dy);
//		return measuredDist(x1, y1, x2, y2);
}


static double squareDist(int x1, int y1, int x2, int y2) {
	// translate into meters
	double dy = convert31YToMeters(y1, y2);
	double dx = convert31XToMeters(x1, x2);
	return dx * dx + dy * dy;
}

static double h(RoutingContext* ctx, float distanceToFinalPoint, SHARED_PTR<RouteSegment> next) {
	return distanceToFinalPoint / ctx->getMaxDefaultSpeed();

}
static double h(RoutingContext* ctx, int targetEndX, int targetEndY,
		int startX, int startY) {
	double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
	return distance / ctx->getMaxDefaultSpeed();
}


struct SegmentsComparator : public std::binary_function<SHARED_PTR<RouteSegment>, SHARED_PTR<RouteSegment>, bool>
{
	RoutingContext* ctx;
	SegmentsComparator(RoutingContext* c) : ctx(c){

	}
	bool operator()(const SHARED_PTR<RouteSegment> lhs, const SHARED_PTR<RouteSegment> rhs) const
    {
    	return roadPriorityComparator(lhs.get()->distanceFromStart, lhs.get()->distanceToEnd, rhs.get()->distanceFromStart,
    			rhs.get()->distanceToEnd, ctx->getHeuristicCoefficient()) > 0;
    }
};
struct NonHeuristicSegmentsComparator : public std::binary_function<SHARED_PTR<RouteSegment>, SHARED_PTR<RouteSegment>, bool>
{
    bool operator()(const SHARED_PTR<RouteSegment> lhs, const SHARED_PTR<RouteSegment> rhs) const
    {
    	return roadPriorityComparator(lhs.get()->distanceFromStart, lhs.get()->distanceToEnd, rhs.get()->distanceFromStart, rhs.get()->distanceToEnd, 0.5) > 0;
    }
};

typedef UNORDERED(map)<int64_t, SHARED_PTR<RouteSegment> > VISITED_MAP;
typedef priority_queue<SHARED_PTR<RouteSegment>, vector<SHARED_PTR<RouteSegment> >, SegmentsComparator > SEGMENTS_QUEUE;
bool processRouteSegment(RoutingContext* ctx, bool reverseWaySearch,
		SEGMENTS_QUEUE& graphSegments, VISITED_MAP& visitedSegments, int targetEndX, int targetEndY,
		SHARED_PTR<RouteSegment> segment, VISITED_MAP& oppositeSegments);
bool processIntersections(RoutingContext* ctx, SEGMENTS_QUEUE& graphSegments,
		VISITED_MAP& visitedSegments, VISITED_MAP& oppositeSegments,
		double distFromStart, double distToFinalPoint,
		SHARED_PTR<RouteSegment> segment, int segmentEnd, SHARED_PTR<RouteSegment> inputNext,
		bool reverseWay);

/**
 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
 * return list of segments
 */
void searchRouteInternal(RoutingContext* ctx, SHARED_PTR<RouteSegment> start, SHARED_PTR<RouteSegment> end, bool leftSideNavigation) {
	// measure time
	ctx->visitedSegments = 0;
	ctx->timeToCalculate.start();
	// FIXME initial direction
//	if(ctx.config.initialDirection != null) {
//		ctx.firstRoadId = (start->road->id << ROUTE_POINTS) + start.getSegmentStart();
//		double plusDir = start->road->directionRoute(start.segmentStart, true);
//		double diff = plusDir - ctx.config.initialDirection;
//		if(Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
//			ctx.firstRoadDirection = 1;
//		} else if(Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
//			ctx.firstRoadDirection = -1;
//		}
//
//	}

	SegmentsComparator sgmCmp(ctx);
	SEGMENTS_QUEUE graphDirectSegments(sgmCmp);
	SEGMENTS_QUEUE graphReverseSegments(sgmCmp);

	// Set to not visit one segment twice (stores road.id << X + segmentStart)
	VISITED_MAP visitedDirectSegments ;
	VISITED_MAP visitedOppositeSegments;

	// FIXME run recalculation
	bool runRecalculation = false;

	// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
	int targetEndX = end->road->pointsX[end->segmentStart];
	int targetEndY = end->road->pointsY[end->segmentStart];
	int startX = start->road->pointsX[start->segmentStart];
	int startY = start->road->pointsY[start->segmentStart];
	float estimatedDistance = (float) h(ctx, targetEndX, targetEndY, startX, startY);
	end->distanceToEnd = start->distanceToEnd = estimatedDistance;

	graphDirectSegments.push(start);
	graphReverseSegments.push(end);

	// Extract & analyze segment with min(f(x)) from queue while final segment is not found
	bool inverse = false;
	bool init = false;

	NonHeuristicSegmentsComparator nonHeuristicSegmentsComparator;
	SEGMENTS_QUEUE * graphSegments;
	if(inverse) {
		graphSegments = &graphReverseSegments;
	} else {
		graphSegments = &graphDirectSegments;
	}
	while (graphSegments->size() > 0) {
		SHARED_PTR<RouteSegment> segment = graphSegments->top();
		graphSegments->pop();
		bool routeFound = false;
		if (!inverse) {
			routeFound = processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, targetEndX, targetEndY,
					segment, visitedOppositeSegments);
		} else {
			routeFound = processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, startX, startY, segment,
					visitedDirectSegments);
		}
		if (graphReverseSegments.size() == 0 || graphDirectSegments.size() == 0 || routeFound) {
			break;
		}
		if(runRecalculation) {
			// nothing to do
			inverse = false;
		} else if (!init) {
			inverse = !inverse;
			init = true;
		} else if (ctx->planRouteIn2Directions()) {
			inverse = !nonHeuristicSegmentsComparator(graphDirectSegments.top(), graphReverseSegments.top());
			if (graphDirectSegments.size() * 1.3 > graphReverseSegments.size()) {
				inverse = true;
			} else if (graphDirectSegments.size() < 1.3 * graphReverseSegments.size()) {
				inverse = false;
			}
		} else {
			// different strategy : use onedirectional graph
			inverse = ctx->getPlanRoadDirection() < 0;
		}
		if (inverse) {
			graphSegments = &graphReverseSegments;
		} else {
			graphSegments = &graphDirectSegments;
		}

		// check if interrupted
		if(ctx->isInterrupted()) {
			return;
		}
	}
	ctx->timeToCalculate.pause();
	osmand_log_print(LOG_WARN, "[Native] Result visited (visited roads %d, visited segments %d / %d , queue sizes %d / %d ) ",
			ctx-> visitedSegments, visitedDirectSegments.size(), visitedOppositeSegments.size(),
			graphDirectSegments.size(),graphReverseSegments.size());
}
bool processRouteSegment(RoutingContext* ctx, bool reverseWaySearch,
		SEGMENTS_QUEUE& graphSegments, VISITED_MAP& visitedSegments, int targetEndX, int targetEndY,
		SHARED_PTR<RouteSegment> segment, VISITED_MAP& oppositeSegments) {
	// Always start from segmentStart (!), not from segmentEnd
	// It makes difference only for the first start segment
	// Middle point will always be skipped from observation considering already visited
	SHARED_PTR<RouteDataObject> road = segment->road;
	int middle = segment->segmentStart;
	double obstaclePlusTime = 0;
	double obstacleMinusTime = 0;

	// 0. mark route segment as visited
	int64_t nt = (road->id << ROUTE_POINTS) + middle;
	if(visitedSegments.find(nt) != visitedSegments.end()) {
		return false;
	}
	ctx->visitedSegments++;
	// avoid empty segments to connect but mark the point as visited
	visitedSegments[nt] = SHARED_PTR<RouteSegment>();

	int oneway = ctx->isOneWay(road);
	bool minusAllowed;
	bool plusAllowed;
	if(ctx->firstRoadId == nt) {
		minusAllowed = ctx->firstRoadDirection <= 0;
		plusAllowed = ctx->firstRoadDirection >= 0;
	} else if (!reverseWaySearch) {
		minusAllowed = oneway <= 0;
		plusAllowed = oneway >= 0;
	} else {
		minusAllowed = oneway >= 0;
		plusAllowed = oneway <= 0;
	}

	// +/- diff from middle point
	int d = plusAllowed ? 1 : -1;
	if(segment->parentRoute.get() != NULL) {
		if(plusAllowed && middle < segment->road->pointsX.size() - 1) {
			obstaclePlusTime = ctx->calculateTurnTime(segment, segment->road->pointsX.size() - 1,
					segment->parentRoute, segment->parentSegmentEnd);
		}
		if(minusAllowed && middle > 0) {
			obstacleMinusTime = ctx->calculateTurnTime(segment, 0,
					segment->parentRoute, segment->parentSegmentEnd);
		}
	}
	// Go through all point of the way and find ways to continue
	// ! Actually there is small bug when there is restriction to move forward on way (it doesn't take into account)
	double posSegmentDist = 0;
	double negSegmentDist = 0;
	while (minusAllowed || plusAllowed) {
		// 1. calculate point not equal to middle
		// (algorithm should visit all point on way if it is not oneway)
		int segmentEnd = middle + d;
		bool positive = d > 0;
		if (!minusAllowed && d > 0) {
			d++;
		} else if (!plusAllowed && d < 0) {
			d--;
		} else {
			if (d <= 0) {
				d = -d + 1;
			} else {
				d = -d;
			}
		}
		if (segmentEnd < 0) {
			minusAllowed = false;
			continue;
		}
		if (segmentEnd >= road->pointsX.size()) {
			plusAllowed = false;
			continue;
		}
		// if we found end point break cycle
		int64_t nts = (road->id << ROUTE_POINTS) + segmentEnd;
		visitedSegments[nts]=segment;

		// 2. calculate point and try to load neighbor ways if they are not loaded
		int x = road->pointsX[segmentEnd];
		int y = road->pointsY[segmentEnd];
		if(positive) {
			posSegmentDist += squareRootDist(x, y,
					road->pointsX[segmentEnd - 1], road->pointsY[segmentEnd - 1]);
		} else {
			negSegmentDist += squareRootDist(x, y,
					road->pointsX[segmentEnd + 1], road->pointsY[segmentEnd + 1]);
		}

		// 2.1 calculate possible obstacle plus time
		if(positive) {
			double obstacle = ctx->defineRoutingObstacle(road, segmentEnd);
			if (obstacle < 0) {
				plusAllowed = false;
				continue;
			}
			obstaclePlusTime += obstacle;
		} else {
			double obstacle = ctx->defineRoutingObstacle(road, segmentEnd);
			if (obstacle < 0) {
				minusAllowed = false;
				continue;
			}
			obstacleMinusTime += obstacle;
		}
		// could be expensive calculation
		// FIXME memory check
//		int overhead = (ctx.visitedSegments - ctx.relaxedSegments ) *
//		STANDARD_ROAD_IN_QUEUE_OVERHEAD;
//		if(overhead > ctx.config.memoryLimitation * 0.95) {
//			throw new OutOfMemoryError("There is no enough memory " + ctx.config.memoryLimitation/(1<<20) + " Mb");
//		}
		SHARED_PTR<RouteSegment> next = ctx->loadRouteSegment(x, y);

		// 3. get intersected ways
		if (next.get() != NULL) {
			if((next.get() == segment.get() || next->road->id == road->id) && next->next.get() == NULL) {
				// simplification if there is no real intersection
				continue;
			}

			// Using A* routing algorithm
			// g(x) - calculate distance to that point and calculate time

			double priority = ctx->defineSpeedPriority(road);
			double speed = ctx->defineSpeed(road) * priority;
			if (speed == 0) {
				speed = ctx->getMinDefaultSpeed() * priority;
			}
			double distOnRoadToPass = positive? posSegmentDist : negSegmentDist;
			double distStartObstacles = segment->distanceFromStart + ( positive ? obstaclePlusTime : obstacleMinusTime) + distOnRoadToPass / speed;

			double distToFinalPoint = squareRootDist(x, y, targetEndX, targetEndY);
			bool routeFound = processIntersections(ctx, graphSegments, visitedSegments, oppositeSegments,
					distStartObstacles, distToFinalPoint, segment, segmentEnd, next, reverseWaySearch);
			if(routeFound) {
				return routeFound;
			}

		}
	}
	return false;
}

bool proccessRestrictions(RoutingContext* ctx, SHARED_PTR<RouteDataObject> road, SHARED_PTR<RouteSegment> inputNext, bool reverseWay) {
	ctx->segmentsToVisitPrescripted.clear();
	ctx->segmentsToVisitNotForbidden.clear();
	bool exclusiveRestriction = false;
	SHARED_PTR<RouteSegment> next = inputNext;

	if (!reverseWay && road->restrictions.size() == 0) {
		return false;
	}
	if(!ctx->restrictionsAware()) {
		return false;
	}
	while (next.get() != NULL) {
		int type = -1;
		if (!reverseWay) {
			for (int i = 0; i < road->restrictions.size(); i++) {
				if ((road->restrictions[i] >> 3) == next->road->id) {
					type = road->restrictions[i] & 7;
					break;
				}
			}
		} else {
			for (int i = 0; i < next->road->restrictions.size(); i++) {
				int rt = next->road->restrictions[i] & 7;
				int64_t restrictedTo = next->road->restrictions[i] >> 3;
				if (restrictedTo == road->id) {
					type = rt;
					break;
				}

				// Check if there is restriction only to the other than current road
				if (rt == RESTRICTION_ONLY_RIGHT_TURN || rt == RESTRICTION_ONLY_LEFT_TURN
						|| rt == RESTRICTION_ONLY_STRAIGHT_ON) {
					// check if that restriction applies to considered junk
					SHARED_PTR<RouteSegment> foundNext = inputNext;
					while (foundNext.get() != NULL) {
						if (foundNext->road->id == restrictedTo) {
							break;
						}
						foundNext = foundNext->next;
					}
					if (foundNext.get() != NULL) {
						type = REVERSE_WAY_RESTRICTION_ONLY; // special constant
					}
				}
			}
		}
		if (type == REVERSE_WAY_RESTRICTION_ONLY) {
			// next = next.next; continue;
		} else if (type == -1 && exclusiveRestriction) {
			// next = next.next; continue;
		} else if (type == RESTRICTION_NO_LEFT_TURN || type == RESTRICTION_NO_RIGHT_TURN
				|| type == RESTRICTION_NO_STRAIGHT_ON || type == RESTRICTION_NO_U_TURN) {
			// next = next.next; continue;
		} else if (type == -1) {
			// case no restriction
			ctx->segmentsToVisitNotForbidden.push_back(next);
		} else {
			// case exclusive restriction (only_right, only_straight, ...)
			// 1. in case we are going backward we should not consider only_restriction
			// as exclusive because we have many "in" roads and one "out"
			// 2. in case we are going forward we have one "in" and many "out"
			if (!reverseWay) {
				exclusiveRestriction = true;
				ctx->segmentsToVisitNotForbidden.clear();
				ctx->segmentsToVisitPrescripted.push_back(next);
			} else {
				ctx->segmentsToVisitNotForbidden.push_back(next);
			}
		}
		next = next->next;
	}
	ctx->segmentsToVisitPrescripted.insert(ctx->segmentsToVisitPrescripted.end(), ctx->segmentsToVisitNotForbidden.begin(), ctx->segmentsToVisitNotForbidden.end());
	return true;
}

bool processIntersections(RoutingContext* ctx, SEGMENTS_QUEUE& graphSegments,
		VISITED_MAP& visitedSegments, VISITED_MAP& oppositeSegments,
		double distFromStart, double distToFinalPoint,
		SHARED_PTR<RouteSegment> segment, int segmentEnd, SHARED_PTR<RouteSegment> inputNext,
		bool reverseWay) {
	bool thereAreRestrictions = proccessRestrictions(ctx, segment->road, inputNext, reverseWay);
	vector<SHARED_PTR<RouteSegment> >::iterator nextIterator;
	if (thereAreRestrictions) {
		nextIterator = ctx->segmentsToVisitPrescripted.begin();
	}
	// Calculate possible ways to put into priority queue
	SHARED_PTR<RouteSegment> next = inputNext;
	bool hasNext = !thereAreRestrictions || nextIterator != ctx->segmentsToVisitPrescripted.end();
	while (hasNext) {
		if (thereAreRestrictions) {
			next = *nextIterator;
		}
		int64_t nts = (next->road->id << ROUTE_POINTS) + next->segmentStart;
		// 1. Check if opposite segment found so we can stop calculations
		if (oppositeSegments.find(nts) != oppositeSegments.end()) {
			// restrictions checked
			SHARED_PTR<RouteSegment> opposite = oppositeSegments[nts];
			// additional check if opposite way not the same as current one
			if (opposite.get() != NULL && (next->segmentStart != segmentEnd ||
					opposite->road->id != segment->road->id)) {
				SHARED_PTR<FinalRouteSegment> frs = SHARED_PTR<FinalRouteSegment>(new FinalRouteSegment);
				frs->direct = segment;
				frs->reverseWaySearch = reverseWay;
				SHARED_PTR<RouteSegment> op = SHARED_PTR<RouteSegment>(new RouteSegment(segment->road, segmentEnd));
				op->parentRoute = opposite;
				op->parentSegmentEnd = next->getSegmentStart();
				frs->opposite = op;
				frs->distanceFromStart = opposite->distanceFromStart + segment->distanceFromStart;
				ctx->finalRouteSegment = frs;
				return true;
			}
		}
		// road.id could be equal on roundabout, but we should accept them
		bool alreadyVisited = visitedSegments.find(nts) != visitedSegments.end();
		if (!alreadyVisited) {
			double distanceToEnd = h(ctx, distToFinalPoint, next);
			if (next->parentRoute.get() == NULL
					|| roadPriorityComparator(next->distanceFromStart, next->distanceToEnd, distFromStart, distanceToEnd,
							ctx->getHeuristicCoefficient()) > 0) {
				if (next->parentRoute.get() != NULL) {
					// already in queue remove it (we can not remove it)
					next = SHARED_PTR<RouteSegment>(new RouteSegment(next->road, next->segmentStart));
				}
				next->distanceFromStart = distFromStart;
				next->distanceToEnd = distanceToEnd;
				// put additional information to recover whole route after
				next->parentRoute = segment;
				next->parentSegmentEnd = segmentEnd;
				graphSegments.push(next);
			}
		} else {
			// the segment was already visited! We need to follow better route if it exists
			// that is very strange situation and almost exception (it can happen when we underestimate distnceToEnd)
			if (distFromStart < next->distanceFromStart && next->road->id != segment->road->id) {
				next->distanceFromStart = distFromStart;
				next->parentRoute = segment;
				next->parentSegmentEnd = segmentEnd;
			}
		}

		// iterate to next road
		if (thereAreRestrictions) {
			nextIterator++;
			hasNext = nextIterator != ctx->segmentsToVisitPrescripted.end();
		} else {
			next = next->next;
			hasNext = next.get() != NULL;
		}
	}
	return false;
}

// FIXME
SHARED_PTR<RouteSegment> findRouteSegment(int px, int py, RoutingContext* ctx) {
	return ctx->loadSegmentAround(px, py);
}

 bool combineTwoSegmentResult(RouteSegmentResult& toAdd, RouteSegmentResult& previous,
			bool reverse) {
		bool ld = previous.endPointIndex > previous.startPointIndex;
		bool rd = toAdd.endPointIndex > toAdd.startPointIndex;
		if (rd == ld) {
			if (toAdd.startPointIndex == previous.endPointIndex && !reverse) {
				previous.endPointIndex = toAdd.endPointIndex;
				return true;
			} else if (toAdd.endPointIndex == previous.startPointIndex && reverse) {
				previous.startPointIndex = toAdd.startPointIndex;
				return true;
			}
		}
		return false;
	}

void addRouteSegmentToResult(vector<RouteSegmentResult>& result, RouteSegmentResult& res, bool reverse) {
	if (res.endPointIndex != res.startPointIndex) {
		if (result.size() > 0) {
			RouteSegmentResult last = result[result.size() - 1];
			if (last.object->id == res.object->id) {
				if (combineTwoSegmentResult(res, last, reverse)) {
					return;
				}
			}
		}
		result.push_back(res);
	}
}

vector<RouteSegmentResult> convertFinalSegmentToResults(RoutingContext* ctx) {
	vector<RouteSegmentResult> result;
	if (ctx->finalRouteSegment.get() != NULL) {
		osmand_log_print(LOG_INFO, "Routing calculated time distance %f", ctx->finalRouteSegment->distanceFromStart);
		SHARED_PTR<FinalRouteSegment> finalSegment = ctx->finalRouteSegment;
		// Get results from opposite direction roads
		SHARED_PTR<RouteSegment> segment = finalSegment->reverseWaySearch ? finalSegment->direct : finalSegment->opposite->parentRoute;
		int parentSegmentStart =
				finalSegment->reverseWaySearch ?
						finalSegment->opposite->getSegmentStart() : finalSegment->opposite->parentSegmentEnd;
		while (segment.get() != NULL) {
			RouteSegmentResult res(segment->road, parentSegmentStart, segment->getSegmentStart());
			parentSegmentStart = segment->parentSegmentEnd;
			segment = segment->parentRoute;
			addRouteSegmentToResult(result, res, false);
		}
		// reverse it just to attach good direction roads
		std::reverse(result.begin(), result.end());

		segment = finalSegment->reverseWaySearch ? finalSegment->opposite->parentRoute : finalSegment->direct;
		int parentSegmentEnd =
				finalSegment->reverseWaySearch ?
						finalSegment->opposite->parentSegmentEnd : finalSegment->opposite->getSegmentStart();

		while (segment.get() != NULL) {
			RouteSegmentResult res(segment->road, segment->getSegmentStart(), parentSegmentEnd);
			parentSegmentEnd = segment->parentSegmentEnd;
			segment = segment->parentRoute;
			// happens in smart recalculation
			addRouteSegmentToResult(result, res, true);
		}
		std::reverse(result.begin(), result.end());

	}
	return result;
}


vector<RouteSegmentResult> searchRouteInternal(RoutingContext* ctx, bool leftSideNavigation) {
	SHARED_PTR<RouteSegment> start = findRouteSegment(ctx->startX, ctx->startY, ctx);
	if(start.get() == NULL) {
		osmand_log_print(LOG_WARN, "Start point was not found [Native]");
		return vector<RouteSegmentResult>();
	} else {
		osmand_log_print(LOG_WARN, "Start point was found %lld [Native]", start->road->id);
	}
	SHARED_PTR<RouteSegment> end = findRouteSegment(ctx->endX, ctx->endY, ctx);
	if(end.get() == NULL) {
		osmand_log_print(LOG_WARN, "End point was not found [Native]");
		return vector<RouteSegmentResult>();
	} else {
		osmand_log_print(LOG_WARN, "End point was found %lld [Native]", end->road->id);
	}
	searchRouteInternal(ctx, start, end, leftSideNavigation);
	osmand_log_print(LOG_WARN, "[Native] Result timing (time to load %d, time to calc %d, loaded tiles %d) ", ctx->timeToLoad.getElapsedTime()
			, ctx->timeToCalculate.getElapsedTime(), ctx->loadedTiles);
	return convertFinalSegmentToResults(ctx);
}


