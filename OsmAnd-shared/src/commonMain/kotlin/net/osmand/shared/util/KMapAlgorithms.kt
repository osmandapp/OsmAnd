package net.osmand.shared.util

import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt

object KMapAlgorithms {

	fun augmentTrkSegmentWithAltitudes(sgm: TrkSegment, decodedSteps: List<Int>, startEle: Double) {
		val stepDist = decodedSteps[0]
		var stepHNextInd = 1
		var prevHDistX = 0.0
		sgm.points[0].ele = startEle
		var i = 1

		while (i < sgm.points.size) {
			val prev = sgm.points[i - 1]
			val cur = sgm.points[i]
			val origHDistX = prevHDistX
			val len = KMapUtils.getDistance(prev.lat, prev.lon, cur.lat, cur.lon) / stepDist
			val curHDistX = len + prevHDistX
			var hInc = 0.0

			while (curHDistX > stepHNextInd && stepHNextInd < decodedSteps.size) {
				if (prevHDistX < stepHNextInd) {
					hInc += (stepHNextInd - prevHDistX) * decodedSteps[stepHNextInd]
					if (stepHNextInd - prevHDistX > 0.5) {
						// introduce extra point
						val fraction = (stepHNextInd - prevHDistX) / (curHDistX - origHDistX)
						val newPt = WptPt(
							lat = prev.lat + fraction * (cur.lat - prev.lat),
							lon = prev.lon + fraction * (cur.lon - prev.lon),
						)
						newPt.ele = prev.ele + hInc
						sgm.points.add(i, newPt)
						i++
					}
					prevHDistX = stepHNextInd.toDouble()
				}
				stepHNextInd++
			}

			if (stepHNextInd < decodedSteps.size) {
				hInc += (curHDistX - prevHDistX) * decodedSteps[stepHNextInd]
			}

			cur.ele = prev.ele + hInc
			prevHDistX = curHDistX
			i++
		}
	}

	fun decodeIntHeightArrayGraph(str: String, repeatBits: Int): List<Int> {
		val maxRepeats = (1 shl repeatBits) - 1
		val res = mutableListOf<Int>()
		val ch = str.toCharArray()
		res.add(ch[0].code)
		for (i in 1 until ch.size) {
			val c = ch[i]
			var rept = c.code and maxRepeats
			while (rept > 0) {
				res.add(0)
				rept--
			}
			val num = c.code shr repeatBits
			if (num % 2 == 0) {
				res.add(num shr 1)
			} else {
				res.add(-(num shr 1))
			}
		}
		return res
	}


	/**
	 * Whether the segment from (x1, y1) to (x2, y2) crosses the one from (x3, y3) to (x4, y4).
	 * Franklin Antonio's "Faster Line Segment Intersection" from Graphics Gems III, with Keith
	 * Woodward's check for collinear overlapping segments; a copy of `MapAlgorithms.linesIntersect`.
	 */
	fun linesIntersect(
		x1: Double, y1: Double, x2: Double, y2: Double,
		x3: Double, y3: Double, x4: Double, y4: Double
	): Boolean {
		// Return false if either of the lines have zero length
		if (x1 == x2 && y1 == y2 || x3 == x4 && y3 == y4) {
			return false
		}
		val ax = x2 - x1
		val ay = y2 - y1
		val bx = x3 - x4
		val by = y3 - y4
		val cx = x1 - x3
		val cy = y1 - y3

		val alphaNumerator = by * cx - bx * cy
		val commonDenominator = ay * bx - ax * by
		if (commonDenominator > 0) {
			if (alphaNumerator < 0 || alphaNumerator > commonDenominator) {
				return false
			}
		} else if (commonDenominator < 0) {
			if (alphaNumerator > 0 || alphaNumerator < commonDenominator) {
				return false
			}
		}
		val betaNumerator = ax * cy - ay * cx
		if (commonDenominator > 0) {
			if (betaNumerator < 0 || betaNumerator > commonDenominator) {
				return false
			}
		} else if (commonDenominator < 0) {
			if (betaNumerator > 0 || betaNumerator < commonDenominator) {
				return false
			}
		}
		if (commonDenominator == 0.0) {
			// The lines are parallel. Check if they're collinear.
			val y3LessY1 = y3 - y1
			// see http://mathworld.wolfram.com/Collinear.html
			val collinearityTestForP3 = x1 * (y2 - y3) + x2 * (y3LessY1) + x3 * (y1 - y2)
			// If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4
			if (collinearityTestForP3 == 0.0) {
				// The lines are collinear. Now check if they overlap.
				if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 ||
					x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 ||
					x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2
				) {
					if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 ||
						y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 ||
						y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2
					) {
						return true
					}
				}
			}
			return false
		}
		return true
	}

	/**
	 * Where the horizontal line at [middleY] crosses the segment from the previous node to the node,
	 * or [Int.MIN_VALUE] when it does not; a copy of `MapAlgorithms.ray_intersect_x`.
	 */
	fun rayIntersectX(prevX: Int, prevY: Int, x: Int, y: Int, middleY: Int): Int {
		var px = prevX
		var py = prevY
		var nx = x
		var ny = y
		var my = middleY
		// prev node above line
		// x,y node below line
		if (py > ny) {
			val tx = nx
			val ty = ny
			nx = px
			ny = py
			px = tx
			py = ty
		}
		if (ny == my || py == my) {
			my -= 1
		}
		if (py > my || ny < my) {
			return Int.MIN_VALUE
		} else {
			if (ny == py) {
				// the node on the boundary !!!
				return nx
			}
			// that tested on all cases (left/right)
			val rx = nx + (my.toDouble() - ny) * (nx.toDouble() - px) / (ny.toDouble() - py)
			return rx.toInt()
		}
	}
}
