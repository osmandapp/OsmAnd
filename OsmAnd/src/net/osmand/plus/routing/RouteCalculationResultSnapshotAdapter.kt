package net.osmand.plus.routing

import net.osmand.Location
import net.osmand.router.RouteSegmentResult
import net.osmand.router.RouteSegmentResultSnapshotAdapter
import net.osmand.shared.data.KLatLon
import net.osmand.shared.routing.details.RouteDetailsSnapshot
import net.osmand.shared.routing.details.RouteEvent
import net.osmand.shared.routing.details.RouteEventType
import net.osmand.shared.routing.details.RouteExitInfo
import net.osmand.shared.routing.details.RouteManeuver
import net.osmand.shared.routing.details.RoutePoint
import net.osmand.shared.routing.details.RouteSegment
import net.osmand.shared.routing.details.RouteServiceType
import net.osmand.shared.routing.details.RouteSummary
import kotlin.math.abs

/** Android-only eager copier from navigation result objects to the shared route-details contract. */
object RouteCalculationResultSnapshotAdapter {

	@JvmStatic
	fun create(source: RouteCalculationResult): RouteDetailsSnapshot {
		val locations = source.immutableAllLocations
		val directions = source.immutableAllDirections
		return RouteDetailsSnapshot(
			points = locations.mapIndexed { index, location ->
				copyPoint(location, source.getListDistanceForSnapshot(index))
			},
			segments = copySegments(source.immutableAllSegments, locations.size),
			maneuvers = directions.map(::copyManeuver),
			events = source.alarmInfo.map(::copyEvent),
			summary = copySummary(source, directions),
			statistics = emptyList(),
			currentRoutePointIndex = source.currentRoute,
			currentDirectionIndex = source.getCurrentDirectionInfoForSnapshot(),
			nextIntermediateIndex = source.nextIntermediate,
			intermediateRoutePointOffsets = copyIntermediateRoutePointOffsets(
				source.getIntermediateDirectionIndexesForSnapshot(),
				directions,
				locations.size,
			),
		)
	}

	private fun copyPoint(source: Location, distanceToFinishMeters: Int): RoutePoint = RoutePoint(
		location = KLatLon(source.latitude, source.longitude),
		distanceToFinishMeters = distanceToFinishMeters,
		altitudeMeters = source.altitude.takeIf { source.hasAltitude() },
		speedMetersPerSecond = source.speed.takeIf { source.hasSpeed() },
		timeMillis = source.time,
		provider = source.provider,
	)

	private fun copySegments(
		pointAlignedSegments: List<RouteSegmentResult>,
		routePointCount: Int,
	): List<RouteSegment> {
		if (pointAlignedSegments.isEmpty() || routePointCount == 0) {
			return emptyList()
		}
		val result = mutableListOf<RouteSegment>()
		var runStart = 0
		while (runStart < pointAlignedSegments.size && runStart < routePointCount) {
			val segment = pointAlignedSegments[runStart]
			var runEnd = runStart + 1
			while (runEnd < pointAlignedSegments.size && pointAlignedSegments[runEnd] === segment) {
				runEnd++
			}
			val (routePointStart, routePointEnd) = routePointRange(
				nativeStartPointIndex = segment.startPointIndex,
				nativeEndPointIndex = segment.endPointIndex,
				runStart = runStart,
				runEnd = runEnd,
				routePointCount = routePointCount,
			)
			result.add(
				RouteSegmentResultSnapshotAdapter.toSnapshot(segment, routePointStart, routePointEnd),
			)
			runStart = runEnd
		}
		return result
	}

	/**
	 * `RouteCalculationResult` normally stores a segment once per emitted point. A continuous
	 * non-final segment omits its native end point, while a synthetic final segment is appended at
	 * its end point. Convert those two Android list conventions to an inclusive shared range.
	 */
	internal fun routePointRange(
		nativeStartPointIndex: Int,
		nativeEndPointIndex: Int,
		runStart: Int,
		runEnd: Int,
		routePointCount: Int,
	): Pair<Int, Int> {
		val nativeEdgeCount = abs(nativeEndPointIndex - nativeStartPointIndex)
		val occurrenceCount = runEnd - runStart
		return when {
			occurrenceCount == nativeEdgeCount && runEnd < routePointCount -> runStart to runEnd
			occurrenceCount == nativeEdgeCount && runEnd >= routePointCount && runStart > 0 ->
				(runStart - 1) to (routePointCount - 1)
			else -> runStart to minOf(runEnd - 1, routePointCount - 1)
		}
	}

	internal fun copyManeuver(source: RouteDirectionInfo): RouteManeuver {
		val turn = source.turnType
		val exit = source.exitInfo
		return RouteManeuver(
			turnTypeValue = turn.value,
			routePointOffset = source.routePointOffset,
			routeEndPointOffset = source.routeEndPointOffset,
			distanceMeters = source.distance,
			expectedTimeSeconds = source.expectedTime,
			afterLeftTimeSeconds = source.afterLeftTime,
			averageSpeedMetersPerSecond = source.averageSpeed,
			turnAngleDegrees = turn.turnAngle,
			exitNumber = turn.exitOut,
			lanes = turn.lanes?.toList(),
			skipToSpeak = turn.isSkipToSpeak,
			possibleLeftTurn = turn.isPossibleLeftTurn,
			possibleRightTurn = turn.isPossibleRightTurn,
			otherTurnAngles = turn.otherTurnAngles?.toList(),
			streetName = source.streetName,
			ref = source.ref,
			destinationName = source.destinationName,
			destinationRef = source.destinationRef,
			exitInfo = exit?.let { RouteExitInfo(it.ref, it.exitStreetName) },
		)
	}

	internal fun copyEvent(source: AlarmInfo): RouteEvent = RouteEvent(
		type = RouteEventType.valueOf(source.type.name),
		location = KLatLon(source.latitude, source.longitude),
		locationIndex = source.locationIndex,
		lastLocationIndex = source.lastLocationIndex,
		intValue = source.intValue,
		floatValue = source.floatValue,
	)

	private fun copySummary(
		source: RouteCalculationResult,
		directions: List<RouteDirectionInfo>,
	): RouteSummary = RouteSummary(
		totalDistanceMeters = source.wholeDistance,
		totalTimeSeconds = directions.firstOrNull()?.afterLeftTime ?: 0,
		profileId = source.appMode?.stringKey,
		routeService = source.routeService?.let { RouteServiceType.valueOf(it.name) },
		routingTimeSeconds = source.routingTime,
		calculationTimeSeconds = source.calculateTime,
		visitedSegments = source.visitedSegments,
		loadedTiles = source.loadedTiles,
		initialCalculation = source.isInitialCalculation,
	)

	private fun copyIntermediateRoutePointOffsets(
		intermediateDirectionIndexes: IntArray,
		directions: List<RouteDirectionInfo>,
		routePointCount: Int,
	): List<Int> {
		if (routePointCount == 0) {
			return emptyList()
		}
		return intermediateDirectionIndexes.map { directionIndex ->
			directions.getOrNull(directionIndex)?.routePointOffset ?: 0
		}
	}
}
