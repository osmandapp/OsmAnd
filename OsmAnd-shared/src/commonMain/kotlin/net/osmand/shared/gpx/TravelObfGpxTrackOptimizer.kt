package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.util.KMapUtils
import kotlin.math.abs

/**
 * Joins the pieces a route was cut into by the files it was written to. A route crossing a region
 * boundary is written once per file, and the two pieces overlap by a few points near the edge, so
 * a naive concatenation leaves a loop there and counts the overlap twice.
 *
 * A copy of `TravelObfGpxTrackOptimizer` in OsmAnd-java, which stays there for android; this copy
 * is for iOS. Both work on the shared gpx types already, so the two can be compared directly.
 *
 * Human-based version of OverlappedSegmentsMergerDS / OverlappedSegmentsMergerGPT (commit 17701568cb)
 */
object TravelObfGpxTrackOptimizer {

	private const val MAX_JUMPS_OVER_UNIQUE_POINTS = 5
	private const val EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE = 10.0
	private const val PRECISION_DUPES = KMapUtils.DEFAULT_LATLON_PRECISION
	private const val PRECISION_EQUAL = KMapUtils.DEFAULT_LATLON_PRECISION // ~1 meter
	private const val CLOSE_DISTANCE = 50.0 // meters
	// cheap bounds for CLOSE_DISTANCE: a degree of latitude is never shorter than 110574 m, and a
	// degree of longitude never shorter than 9600 m within the map (|lat| <= 85.05), so neither can
	// reject a pair that is really within CLOSE_DISTANCE
	private const val CLOSE_MAX_LAT_DEGREES = CLOSE_DISTANCE / 110574.0
	private const val CLOSE_MAX_LON_DEGREES = CLOSE_DISTANCE / 9600.0

	/** The track with its overlapping ends trimmed and the pieces joined end to end. */
	fun mergeOverlappedSegmentsAtEdges(track: Track): Track {
		val duplicates = HashSet<String>()
		findDisplacedEdgePointsToDeduplicate(track, duplicates)

		val cleanedSegments = ArrayList<TrkSegment>()
		deduplicatePointsFromEdges(track, duplicates, cleanedSegments)

		val joinedSegments = ArrayList<TrkSegment>()
		joinCleanedSegments(cleanedSegments, joinedSegments)

		val joinedTrack = Track()
		joinedTrack.segments = joinedSegments
		return joinedTrack
	}

	/**
	 * The ends of one piece rarely land on a point of the other; they land on a line between two of
	 * them. Such an end counts as a duplicate of that stretch.
	 */
	private fun findDisplacedEdgePointsToDeduplicate(track: Track, duplicates: MutableSet<String>) {
		val edgePoints = HashSet<WptPt>()
		for (seg in track.segments) {
			val points = seg.points
			if (points.isNotEmpty()) {
				edgePoints.add(points[0])
				edgePoints.add(points[points.size - 1])
			}
		}
		if (edgePoints.isNotEmpty()) {
			for (seg in track.segments) {
				val points = seg.points
				for (i in 1 until points.size) {
					searchEdgePointsDuplicates(duplicates, edgePoints, points[i], points[i - 1])
				}
			}
		}
	}

	private fun searchEdgePointsDuplicates(
		duplicates: MutableSet<String>, edgePoints: Set<WptPt>, p1: WptPt, p2: WptPt
	) {
		for (edge in edgePoints) {
			val coeff = KMapUtils.getProjectionCoeff(
				edge.lat, edge.lon, p1.lat, p1.lon, p2.lat, p2.lon
			)
			if (coeff > 0.0 && coeff < 1.0) {
				val dist = KMapUtils.getOrthogonalDistance(
					edge.lat, edge.lon, p1.lat, p1.lon, p2.lat, p2.lon
				)
				if (dist > 0 && dist < EDGE_POINTS_MAX_ORTHOGONAL_DISTANCE) {
					duplicates.add(llKey(edge))
				}
			}
		}
	}

	private fun deduplicatePointsFromEdges(
		track: Track, duplicates: MutableSet<String>, cleanedSegments: MutableList<TrkSegment>
	) {
		for (seg in track.segments) {
			val clean = TrkSegment()
			val points = seg.points
			if (points.isNotEmpty()) {
				var fromIndex = 0
				var toIndex = points.size - 1 // inclusive indexes

				markDanglingEdgePointsToDeduplicate(points, duplicates)

				fromIndex += countDuplicates(true, fromIndex, toIndex, points, duplicates)
				toIndex -= countDuplicates(false, fromIndex, toIndex, points, duplicates)

				if (fromIndex < toIndex) {
					clean.points.addAll(points.subList(fromIndex, toIndex + 1))
				}

				// edges [0, -1] must not be considered as future duplicates
				for (i in 1 until points.size - 1) {
					duplicates.add(llKey(points[i]))
				}
			}
			if (clean.points.isNotEmpty()) {
				cleanedSegments.add(clean)
			}
		}
	}

	/** How many points to trim from one end, jumping over a few unique ones between duplicates. */
	private fun countDuplicates(
		forward: Boolean, fromIndex: Int, toIndex: Int, points: List<WptPt>, duplicates: Set<String>
	): Int {
		var dupes = 0
		var uniques = 0
		val a = if (forward) fromIndex else toIndex
		val b = if (forward) toIndex else fromIndex
		var i = a
		while (i != b) {
			if (duplicates.contains(llKey(points[i]))) {
				dupes += uniques + 1 // jumped over
				uniques = 0
			} else {
				if (++uniques > MAX_JUMPS_OVER_UNIQUE_POINTS) {
					break
				}
			}
			i += if (forward) 1 else -1
		}

		return if (dupes > 1) dupes else 0 // keep solitary duplicate at the edge
	}

	private fun markDanglingEdgePointsToDeduplicate(points: List<WptPt>, duplicates: MutableSet<String>) {
		if (points.size > 1) {
			if (duplicates.contains(llKey(points[1]))) {
				duplicates.add(llKey(points[0]))
			}
			if (duplicates.contains(llKey(points[points.size - 2]))) {
				duplicates.add(llKey(points[points.size - 1]))
			}
		}
	}

	/**
	 * Takes a piece and keeps attaching whichever of the rest touches either of its ends, in either
	 * direction, until nothing more fits; then starts again with what is left.
	 */
	private fun joinCleanedSegments(
		segmentsToJoin: List<TrkSegment>, joinedSegments: MutableList<TrkSegment>
	) {
		val done = BooleanArray(segmentsToJoin.size)
		while (true) {
			val result = ArrayList<WptPt>()
			for (i in segmentsToJoin.indices) {
				if (!done[i]) {
					done[i] = true
					if (segmentsToJoin[i].points.isNotEmpty()) {
						addSegmentToResult(result, false, segmentsToJoin[i], false) // "head" segment
						while (true) {
							var stop = true
							for (j in segmentsToJoin.indices) {
								if (!done[j] && considerSegmentToJoin(result, segmentsToJoin[j])) {
									done[j] = true
									stop = false
								}
							}
							if (stop) {
								break // nothing joined
							}
						}
						break // segment is done
					}
				}
			}
			if (result.isEmpty()) {
				break // all done
			}
			val joined = TrkSegment()
			joined.points.addAll(result)
			joinedSegments.add(joined)
		}
	}

	private fun addSegmentToResult(
		result: MutableList<WptPt>, insert: Boolean, segment: TrkSegment, reverse: Boolean
	) {
		val points = ArrayList<WptPt>()
		for (wpt in segment.points) {
			points.add(WptPt(wpt.lat, wpt.lon))
		}
		if (reverse) {
			points.reverse()
		}
		if (insert) {
			val skipTrailingPoint = result.isNotEmpty() && points.isNotEmpty() &&
					equalWptPts(points[points.size - 1], result[0])
			result.addAll(0, points.subList(0, points.size - (if (skipTrailingPoint) 1 else 0))) // insert
		} else {
			val skipLeadingPoint = result.isNotEmpty() && points.isNotEmpty() &&
					equalWptPts(points[0], result[result.size - 1])
			result.addAll(
				result.size, points.subList(if (skipLeadingPoint) 1 else 0, points.size)
			) // append
		}
	}

	private fun considerSegmentToJoin(result: MutableList<WptPt>, candidate: TrkSegment): Boolean {
		if (result.isEmpty()) {
			return false
		}

		if (candidate.points.isEmpty()) {
			return true
		}

		val firstPoint = result[0]
		val lastPoint = result[result.size - 1]
		val firstCandidate = candidate.points[0]
		val lastCandidate = candidate.points[candidate.points.size - 1]

		val avoidClosedLoop = (result.size > 1 && equalWptPts(firstPoint, lastPoint)) ||
				(candidate.points.size > 1 && equalWptPts(firstCandidate, lastCandidate))

		if (avoidClosedLoop) {
			return false
		} else if (closeWptPts(lastPoint, firstCandidate)) {
			addSegmentToResult(result, false, candidate, false) // result + Candidate
		} else if (closeWptPts(lastPoint, lastCandidate)) {
			addSegmentToResult(result, false, candidate, true) // result + etadidnaC
		} else if (closeWptPts(firstPoint, firstCandidate)) {
			addSegmentToResult(result, true, candidate, true) // etadidnaC + result
		} else if (closeWptPts(firstPoint, lastCandidate)) {
			addSegmentToResult(result, true, candidate, false) // Candidate + result
		} else {
			return false
		}

		return true
	}

	private fun equalWptPts(p1: WptPt, p2: WptPt): Boolean =
		KMapUtils.areLatLonEqual(p1.lat, p1.lon, p2.lat, p2.lon, PRECISION_EQUAL)

	private fun closeWptPts(p1: WptPt, p2: WptPt): Boolean {
		// by distance, not per axis in degrees: a degree of longitude is shorter than a degree of
		// latitude, so the same tolerance used to accept ~55 m north-south and only ~36 m east-west
		// at these latitudes, and a gap between two pieces was joined or not depending on its bearing.
		// The pairs are O(segments^2) and almost all of them are far apart, so reject those by the
		// bounds above and keep the distance for the few that can pass it
		if (abs(p1.lat - p2.lat) > CLOSE_MAX_LAT_DEGREES ||
			abs(p1.lon - p2.lon) > CLOSE_MAX_LON_DEGREES) {
			return false
		}
		return KMapUtils.getDistance(p1.lat, p1.lon, p2.lat, p2.lon) <= CLOSE_DISTANCE
	}

	/** A point rounded to about a metre, which is how two readings of one place are matched. */
	private fun llKey(edge: WptPt): String =
		"${(edge.lat / PRECISION_DUPES).toInt()},${(edge.lon / PRECISION_DUPES).toInt()}"
}
