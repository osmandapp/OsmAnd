package net.osmand.shared.routing

import kotlin.jvm.JvmStatic
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.LoggerFactory

/**
 * A manoeuvre: which way to go, plus the lane layout it applies to.
 *
 * A copy of `net.osmand.router.TurnType`, which stays in OsmAnd-java: android, tools and the C++ core keep using
 * the original, and this copy is for iOS. Keep the two identical; `TurnTypeCompatTest` checks that they behave
 * the same.
 *
 * Lanes are packed into an int per lane:
 * - bit 0: whether the lane is active for this manoeuvre;
 * - bits 1-4: primary turn, the one drawn on the map;
 * - bits 5-9: secondary turn;
 * - bits 10 and up: tertiary turn.
 */
class TurnType(
	val value: Int,
	var exitOut: Int,
	// clockwise head rotation, assuming the previous direction was north
	var turnAngle: Float,
	var isSkipToSpeak: Boolean,
	var lanes: IntArray?,
	var isPossibleLeftTurn: Boolean,
	var isPossibleRightTurn: Boolean
) {

	var otherTurnAngles: List<Float>? = null

	private constructor(value: Int) : this(value, 0, 0f, false, null, false, false)

	fun isLeftSide(): Boolean = value == RNLB || value == TRU

	fun isRoundAbout(): Boolean = value == RNDB || value == RNLB

	fun keepLeft(): Boolean = value == KL

	fun keepRight(): Boolean = value == KR

	fun goAhead(): Boolean = value == C

	fun getActiveCommonLaneTurn(): Int {
		val lanes = this.lanes
		if (lanes == null || lanes.isEmpty()) {
			return -1
		}
		for (lane in lanes) {
			if (lane % 2 == 1) {
				return getPrimaryTurn(lane)
			}
		}
		return -1
	}

	fun countTurnTypeDirections(type: Int, onlyActive: Boolean): Int {
		val lanes = this.lanes ?: return 0
		var count = 0
		for (lane in lanes) {
			val active = lane % 2 == 1
			if (onlyActive && !active) {
				continue
			}
			var primary = getPrimaryTurn(lane)
			if (primary == 0) {
				primary = C
			}
			if (primary == type) {
				count++
			}
			if (onlyActive) {
				continue
			}
			if (getSecondaryTurn(lane) == type) {
				count++
			}
			if (getTertiaryTurn(lane) == type) {
				count++
			}
		}
		return count
	}

	fun countDirections(): Int {
		val directions = HashSet<Int>()
		for (lane in lanes!!) {
			var primary = getPrimaryTurn(lane)
			if (primary == 0) {
				primary = C
			}
			directions.add(primary)
			val secondary = getSecondaryTurn(lane)
			if (secondary > 0) {
				directions.add(secondary)
			}
			val tertiary = getTertiaryTurn(lane)
			if (tertiary > 0) {
				directions.add(tertiary)
			}
		}
		return directions.size
	}

	fun toXmlString(): String = when (value) {
		C -> "C"
		TL -> "TL"
		TSLL -> "TSLL"
		TSHL -> "TSHL"
		TR -> "TR"
		TSLR -> "TSLR"
		TSHR -> "TSHR"
		KL -> "KL"
		KR -> "KR"
		TU -> "TU"
		TRU -> "TRU"
		OFFR -> "OFFR"
		RNDB -> "RNDB$exitOut"
		RNLB -> "RNLB$exitOut"
		else -> "C"
	}

	override fun toString(): String {
		var vl: String? = if (isRoundAbout()) {
			"Take $exitOut exit"
		} else when (value) {
			C -> "Go ahead"
			TSLL -> "Turn slightly left"
			TL -> "Turn left"
			TSHL -> "Turn sharply left"
			TSLR -> "Turn slightly right"
			TR -> "Turn right"
			TSHR -> "Turn sharply right"
			TU, TRU -> "Make uturn"
			KL -> "Keep left"
			KR -> "Keep right"
			OFFR -> "Off route"
			else -> null
		}
		if (vl != null) {
			val lanes = this.lanes
			if (lanes != null && lanes.isNotEmpty()) {
				vl += " (" + lanesToString(lanes) + ")"
			}
			return vl
		}
		return super.toString()
	}

	companion object {

		private val LOG = LoggerFactory.getLogger("TurnType")

		/** Continue, go straight. */
		const val C = 1

		/** Turn left. */
		const val TL = 2

		/** Turn slightly left. */
		const val TSLL = 3

		/** Turn sharply left. */
		const val TSHL = 4

		/** Turn right. */
		const val TR = 5

		/** Turn slightly right. */
		const val TSLR = 6

		/** Turn sharply right. */
		const val TSHR = 7

		/** Keep left. */
		const val KL = 8

		/** Keep right. */
		const val KR = 9

		/** U-turn. */
		const val TU = 10

		/** Right U-turn. */
		const val TRU = 11

		/** Off route. */
		const val OFFR = 12

		/** Roundabout. */
		const val RNDB = 13

		/** Roundabout, left hand traffic. */
		const val RNLB = 14

		private val TURNS_ORDER = intArrayOf(TU, TSHL, TL, TSLL, C, TSLR, TR, TSHR, TRU)

		@JvmStatic
		fun straight(): TurnType = valueOf(C, false)

		@JvmStatic
		fun valueOf(value: Int, leftSide: Boolean): TurnType {
			var vs = value
			if (vs == TU && leftSide) {
				vs = TRU
			} else if (vs == RNDB && leftSide) {
				vs = RNLB
			}
			return TurnType(vs)
		}

		@JvmStatic
		fun fromString(s: String?, leftSide: Boolean): TurnType {
			var t: TurnType? = when (s) {
				"C" -> valueOf(C, leftSide)
				"TL" -> valueOf(TL, leftSide)
				"TSLL" -> valueOf(TSLL, leftSide)
				"TSHL" -> valueOf(TSHL, leftSide)
				"TR" -> valueOf(TR, leftSide)
				"TSLR" -> valueOf(TSLR, leftSide)
				"TSHR" -> valueOf(TSHR, leftSide)
				"KL" -> valueOf(KL, leftSide)
				"KR" -> valueOf(KR, leftSide)
				"TU" -> valueOf(TU, leftSide)
				"TRU" -> valueOf(TRU, leftSide)
				"OFFR" -> valueOf(OFFR, leftSide)
				else -> null
			}
			if (t == null && s != null &&
				(s.startsWith("EXIT") || s.startsWith("RNDB") || s.startsWith("RNLB"))
			) {
				try {
					val type = if (s.contains("RNLB")) RNLB else RNDB
					t = getExitTurn(type, s.substring(4).toInt(), 0f, leftSide)
				} catch (e: NumberFormatException) {
					LOG.error("Cannot parse the exit number of '$s'", e)
				}
			}
			return t ?: straight()
		}

		@JvmStatic
		fun getExitTurn(out: Int, angle: Float, leftSide: Boolean): TurnType {
			val turn = valueOf(RNDB, leftSide)
			turn.exitOut = out
			turn.turnAngle = angle
			return turn
		}

		@JvmStatic
		private fun getExitTurn(type: Int, out: Int, angle: Float, leftSide: Boolean): TurnType {
			if (type != RNDB && type != RNLB) {
				return getExitTurn(out, angle, leftSide)
			}
			val turn = valueOf(type, leftSide)
			turn.exitOut = out
			turn.turnAngle = angle
			return turn
		}

		// ------------------------------------------------------------ lane packing

		/** Sets the primary turn and clears the secondary and tertiary ones. */
		@JvmStatic
		fun setPrimaryTurnAndReset(lanes: IntArray, lane: Int, turnType: Int) {
			lanes[lane] = turnType shl 1
		}

		@JvmStatic
		fun setPrimaryTurn(lanes: IntArray, lane: Int, turnType: Int) {
			lanes[lane] = lanes[lane] and (15 shl 1).inv()
			lanes[lane] = lanes[lane] or (turnType shl 1)
		}

		@JvmStatic
		fun getPrimaryTurn(laneValue: Int): Int = (laneValue shr 1) and ((1 shl 4) - 1)

		@JvmStatic
		fun setSecondaryTurn(lanes: IntArray, lane: Int, turnType: Int) {
			lanes[lane] = lanes[lane] and (15 shl 5).inv()
			lanes[lane] = lanes[lane] or (turnType shl 5)
		}

		@JvmStatic
		fun getSecondaryTurn(laneValue: Int): Int = (laneValue shr 5) and ((1 shl 5) - 1)

		@JvmStatic
		fun setTertiaryTurn(lanes: IntArray, lane: Int, turnType: Int) {
			lanes[lane] = lanes[lane] and (15 shl 10).inv()
			lanes[lane] = lanes[lane] or (turnType shl 10)
		}

		@JvmStatic
		fun getTertiaryTurn(laneValue: Int): Int = laneValue shr 10

		@JvmStatic
		fun setPrimaryTurnShiftOthers(lanes: IntArray, lane: Int, turnType: Int) {
			val primary = getPrimaryTurn(lanes[lane])
			val secondary = getSecondaryTurn(lanes[lane])
			// the tertiary turn is lost here
			setPrimaryTurnAndReset(lanes, lane, turnType)
			setSecondaryTurn(lanes, lane, primary)
			setTertiaryTurn(lanes, lane, secondary)
		}

		@JvmStatic
		fun setSecondaryToPrimary(lanes: IntArray, lane: Int) {
			val secondary = getSecondaryTurn(lanes[lane])
			val primary = getPrimaryTurn(lanes[lane])
			setPrimaryTurn(lanes, lane, secondary)
			setSecondaryTurn(lanes, lane, primary)
		}

		@JvmStatic
		fun setTertiaryToPrimary(lanes: IntArray, lane: Int) {
			val secondary = getSecondaryTurn(lanes[lane])
			val primary = getPrimaryTurn(lanes[lane])
			val tertiary = getTertiaryTurn(lanes[lane])
			setPrimaryTurn(lanes, lane, tertiary)
			setSecondaryTurn(lanes, lane, primary)
			setTertiaryTurn(lanes, lane, secondary)
		}

		@JvmStatic
		fun lanesToString(lanes: IntArray): String {
			val builder = StringBuilder()
			for (h in lanes.indices) {
				if (h > 0) {
					builder.append("|")
				}
				if (lanes[h] % 2 == 1) {
					builder.append("+")
				}
				var primary = getPrimaryTurn(lanes[h])
				if (primary == 0) {
					primary = 1
				}
				builder.append(valueOf(primary, false).toXmlString())
				val secondary = getSecondaryTurn(lanes[h])
				if (secondary != 0) {
					builder.append(",").append(valueOf(secondary, false).toXmlString())
				}
				val tertiary = getTertiaryTurn(lanes[h])
				if (tertiary != 0) {
					builder.append(",").append(valueOf(tertiary, false).toXmlString())
				}
			}
			return builder.toString()
		}

		@JvmStatic
		fun lanesFromString(lanesString: String?): IntArray? {
			if (KAlgorithms.isEmpty(lanesString)) {
				return null
			}
			val lanesArr = splitLikeJava(lanesString!!, "|")
			val lanes = IntArray(lanesArr.size)
			for (l in lanesArr.indices) {
				val turns = splitLikeJava(lanesArr[l], ",")
				var primaryTurn: TurnType? = null
				var secondaryTurn: TurnType? = null
				var tertiaryTurn: TurnType? = null
				var plus = false
				for (i in turns.indices) {
					var turn = turns[i]
					when (i) {
						0 -> {
							plus = turn.isNotEmpty() && turn[0] == '+'
							if (plus) {
								turn = turn.substring(1)
							}
							primaryTurn = fromString(turn, false)
						}
						1 -> secondaryTurn = fromString(turn, false)
						2 -> tertiaryTurn = fromString(turn, false)
					}
				}
				setPrimaryTurnAndReset(lanes, l, primaryTurn!!.value)
				if (secondaryTurn != null) {
					setSecondaryTurn(lanes, l, secondaryTurn.value)
				}
				if (tertiaryTurn != null) {
					setTertiaryTurn(lanes, l, tertiaryTurn.value)
				}
				if (plus) {
					lanes[l] = lanes[l] or 1
				}
			}
			return lanes
		}

		/**
		 * java's `String.split(regex)`: trailing empty parts are dropped, except that an input the
		 * delimiter never matches - the empty string included - is one part, itself. Kotlin's `split`
		 * keeps every part, so `"|C"` would lose its first, empty lane here and `""` would have none.
		 */
		private fun splitLikeJava(s: String, delimiter: String): List<String> {
			val parts = s.split(delimiter)
			return if (parts.size == 1) parts else parts.dropLastWhile { it.isEmpty() }
		}

		// ------------------------------------------------------------ classification

		@JvmStatic
		fun isLeftTurn(type: Int): Boolean =
			type == TL || type == TSHL || type == TSLL || type == TU || type == KL

		@JvmStatic
		fun isLeftTurnNoUTurn(type: Int): Boolean =
			type == TL || type == TSHL || type == TSLL || type == KL

		@JvmStatic
		fun isRightTurn(type: Int): Boolean =
			type == TR || type == TSHR || type == TSLR || type == TRU || type == KR

		@JvmStatic
		fun isRightTurnNoUTurn(type: Int): Boolean =
			type == TR || type == TSHR || type == TSLR || type == KR

		@JvmStatic
		fun isSlightTurn(type: Int): Boolean =
			type == TSLL || type == TSLR || type == C || type == KL || type == KR

		@JvmStatic
		fun isKeepDirectionTurn(type: Int): Boolean = type == C || type == KL || type == KR

		@JvmStatic
		fun isSharpOrReverse(type: Int): Boolean =
			type == TSHL || type == TSHR || type == TU || type == TRU

		@JvmStatic
		fun isSharpLeftOrUTurn(type: Int): Boolean = type == TSHL || type == TU

		@JvmStatic
		fun isSharpRightOrUTurn(type: Int): Boolean =
			// turn:lanes=reverse is transformed to TU only
			type == TSHR || type == TRU || type == TU

		@JvmStatic
		fun hasAnySlightTurnLane(type: Int): Boolean =
			isSlightTurn(getPrimaryTurn(type)) ||
					isSlightTurn(getSecondaryTurn(type)) ||
					isSlightTurn(getTertiaryTurn(type))

		@JvmStatic
		fun hasAnyTurnLane(type: Int, turn: Int): Boolean =
			getPrimaryTurn(type) == turn ||
					getSecondaryTurn(type) == turn ||
					getTertiaryTurn(type) == turn

		@JvmStatic
		fun collectTurnTypes(lane: Int, set: LinkedHashSet<Int>) {
			var turn = getPrimaryTurn(lane)
			if (turn != 0) {
				set.add(turn)
			}
			turn = getSecondaryTurn(lane)
			if (turn != 0) {
				set.add(turn)
			}
			turn = getTertiaryTurn(lane)
			if (turn != 0) {
				set.add(turn)
			}
		}

		@JvmStatic
		fun orderFromLeftToRight(type: Int): Int = when (type) {
			TU -> -5
			TSHL -> -4
			TL -> -3
			TSLL -> -2
			KL -> -1
			TRU -> 5
			TSHR -> 4
			TR -> 3
			TSLR -> 2
			KR -> 1
			else -> 0
		}

		@JvmStatic
		fun convertType(lane: String): Int = when (lane) {
			// merge is recognised as continue, even though it may be drawn differently
			"merge_to_left", "merge_to_right", "none", "through" -> C
			"slight_right" -> TSLR
			"slight_left" -> TSLL
			"right" -> TR
			"left" -> TL
			"sharp_right" -> TSHR
			"sharp_left" -> TSHL
			"reverse" -> TU
			// unknown string
			else -> C
		}

		@JvmStatic
		fun getPrev(turn: Int): Int {
			for (i in TURNS_ORDER.indices.reversed()) {
				if (TURNS_ORDER[i] == turn && i > 0) {
					return TURNS_ORDER[i - 1]
				}
			}
			return turn
		}

		@JvmStatic
		fun getNext(turn: Int): Int {
			for (i in TURNS_ORDER.indices) {
				if (TURNS_ORDER[i] == turn && i + 1 < TURNS_ORDER.size) {
					return TURNS_ORDER[i + 1]
				}
			}
			return turn
		}
	}
}
