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

}