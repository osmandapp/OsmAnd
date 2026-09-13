package net.osmand.shared.routing

import net.osmand.shared.routing.RoadSplitStructure.AttachedRoadInfo
import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Everything about turn lanes: reading `turn:lanes` off a road, working out which of them the driver
 * is allowed to be in, and turning that into the arrows shown for a manoeuvre.
 *
 * A lane is one int. Bit 0 says whether it is active for this manoeuvre - whether staying in it gets
 * you where the route goes - and the bits above it hold up to three turns the lane's sign shows,
 * primary first. [TurnType] owns that packing; this owns what to do with it.
 *
 * A copy of the lane logic in `RouteResultPreparation` in OsmAnd-java, which stays there for android
 * and tools. The copy is for iOS, so that it stops using the C++ router's own version in
 * `routeResultPreparation.cpp` and describes a manoeuvre the way android does.
 * `TurnLanesCompatTest` checks the copy against the java original.
 */
object TurnLanes {

	private const val TURN_DEGREE_MIN = 45.0
	private const val TURN_SLIGHT_DEGREE = 5.0
	private const val MAX_SPEAK_PRIORITY = 5

	// ---- reading the tags off a road ----

	/**
	 * The `turn:lanes` of [ro] as seen by someone driving towards [dirToNorthEastPi], or null when
	 * the road declares none. On a two way road the direction picks between the forward and the
	 * backward tag.
	 */
	@JvmStatic
	fun parseTurnLanes(ro: RouteDataObject, dirToNorthEastPi: Double): IntArray? {
		val turnLanes: String? = if (ro.getOneway() == 0) {
			// we should get direction to detect forward or backward
			val cmp = ro.directionRoute(0, true)
			if (abs(KMapUtils.alignAngleDifference(dirToNorthEastPi - cmp)) < PI / 2) {
				ro.getValue("turn:lanes:forward")
			} else {
				ro.getValue("turn:lanes:backward")
			}
		} else {
			ro.getValue("turn:lanes")
		}
		if (turnLanes == null) {
			return null
		}
		return calculateRawTurnLanes(turnLanes, 0)
	}

	/**
	 * An empty lane array sized by the road's `lanes` tags, for a road that says how many lanes it
	 * has but not what they are for. Null when the count cannot be read.
	 */
	@JvmStatic
	fun parseLanes(ro: RouteDataObject, dirToNorthEastPi: Double): IntArray? {
		var lns = 0
		try {
			if (ro.getOneway() == 0) {
				// we should get direction to detect forward or backward
				val cmp = ro.directionRoute(0, true)
				if (abs(KMapUtils.alignAngleDifference(dirToNorthEastPi - cmp)) < PI / 2) {
					if (ro.getValue("lanes:forward") != null) {
						lns = ro.getValue("lanes:forward")!!.toInt()
					}
				} else {
					if (ro.getValue("lanes:backward") != null) {
						lns = ro.getValue("lanes:backward")!!.toInt()
					}
				}
				if (lns == 0 && ro.getValue("lanes") != null) {
					lns = ro.getValue("lanes")!!.toInt() / 2
				}
			} else {
				lns = ro.getValue("lanes")!!.toInt()
			}
			if (lns > 0) {
				return IntArray(lns)
			}
		} catch (e: NumberFormatException) {
			// java caught the same and fell through to null
		} catch (e: NullPointerException) {
			// java's Integer.parseInt(null) threw this; the oneway branch has no null check
		}
		return null
	}

	/** The `turn:lanes` string of [segment], from whichever tag its direction of travel calls for. */
	@JvmStatic
	fun getTurnLanesString(segment: RouteSegmentResult): String? {
		return if (segment.getObject().getOneway() == 0) {
			if (segment.isForwardDirection()) {
				segment.getObject().getValue("turn:lanes:forward")
			} else {
				segment.getObject().getValue("turn:lanes:backward")
			}
		} else {
			segment.getObject().getValue("turn:lanes")
		}
	}

	/**
	 * Packs a `turn:lanes` string into one int per lane. [calcTurnType] is the turn being made, and
	 * decides which of a lane's several signs becomes its primary one: a lane that offers the turn
	 * the route takes shows that turn first.
	 */
	@JvmStatic
	fun calculateRawTurnLanes(turnLanes: String, calcTurnType: Int): IntArray {
		val splitLaneOptions = splitKeepingEmpty(turnLanes, "|")
		val lanes = IntArray(splitLaneOptions.size)
		for (i in splitLaneOptions.indices) {
			val laneOptions = splitDroppingTrailingEmpty(splitLaneOptions[i], ";")
			for (j in laneOptions.indices) {
				val turn = TurnType.convertType(laneOptions[j])
				val primary = TurnType.getPrimaryTurn(lanes[i])
				if (primary == 0) {
					TurnType.setPrimaryTurnAndReset(lanes, i, turn)
				} else {
					if (turn == calcTurnType ||
						(TurnType.isRightTurn(calcTurnType) && TurnType.isRightTurn(turn)) ||
						(TurnType.isLeftTurn(calcTurnType) && TurnType.isLeftTurn(turn))
					) {
						TurnType.setPrimaryTurnShiftOthers(lanes, i, turn)
					} else if (TurnType.getSecondaryTurn(lanes[i]) == 0) {
						TurnType.setSecondaryTurn(lanes, i, turn)
					} else if (TurnType.getTertiaryTurn(lanes[i]) == 0) {
						TurnType.setTertiaryTurn(lanes, i, turn)
					} else {
						// ignore
					}
				}
			}
		}
		return lanes
	}

	/** Every distinct turn the string mentions, in the order it first mentions them. */
	@JvmStatic
	fun getUniqTurnTypes(turnLanes: String): IntArray {
		val turnTypes = LinkedHashSet<Int>()
		val splitLaneOptions = splitKeepingEmpty(turnLanes, "|")
		for (i in splitLaneOptions.indices) {
			val laneOptions = splitDroppingTrailingEmpty(splitLaneOptions[i], ";")
			for (j in laneOptions.indices) {
				turnTypes.add(TurnType.convertType(laneOptions[j]))
			}
		}
		val r = IntArray(turnTypes.size)
		var i = 0
		for (t in turnTypes) {
			r[i++] = t
		}
		return r
	}

	/**
	 * How many lanes [attached] has, never less than one: from its own count, or from the number of
	 * lanes its `turn:lanes` describes, or half the count of a two way road.
	 */
	@JvmStatic
	fun countLanesMinOne(attached: RouteSegmentResult): Int {
		val oneway = attached.getObject().getOneway() != 0
		val lns = attached.getObject().getLanes()
		if (lns == 0) {
			val tls = getTurnLanesString(attached)
			if (tls != null) {
				return max(1, countOccurrences(tls, '|'))
			}
		}
		if (oneway) {
			return max(1, lns)
		}
		try {
			if (attached.isForwardDirection() && attached.getObject().getValue("lanes:forward") != null) {
				return attached.getObject().getValue("lanes:forward")!!.toInt()
			} else if (!attached.isForwardDirection() && attached.getObject().getValue("lanes:backward") != null) {
				return attached.getObject().getValue("lanes:backward")!!.toInt()
			}
		} catch (e: NumberFormatException) {
			// java printed the stack trace and carried on
			e.printStackTrace()
		}
		return max(1, (lns + 1) / 2)
	}

	private fun countOccurrences(haystack: String, needle: Char): Int {
		var count = 0
		for (i in haystack.indices) {
			if (haystack[i] == needle) {
				count++
			}
		}
		return count
	}

	// ---- which lanes are the ones to be in ----

	/** Turns bit 0 on for every lane whose primary sign is [mainTurnType]. True when any was. */
	@JvmStatic
	fun setAllowedLanes(mainTurnType: Int, lanesArray: IntArray): Boolean {
		var turnSet = false
		for (i in lanesArray.indices) {
			if (TurnType.getPrimaryTurn(lanesArray[i]) == mainTurnType) {
				lanesArray[i] = lanesArray[i] or 1
				turnSet = true
			}
		}
		return turnSet
	}

	/**
	 * Marks the lanes between the two indices active, and promotes [activeTurn] to the primary sign
	 * of each of them so they all show the same arrow.
	 */
	@JvmStatic
	fun setActiveLanesRange(rawLanes: IntArray, activeBeginIndex: Int, activeEndIndex: Int, activeTurn: Int) {
		var k = activeBeginIndex
		while (k < rawLanes.size && k <= activeEndIndex) {
			rawLanes[k] = rawLanes[k] or 1
			k++
		}
		k = activeBeginIndex
		while (k < rawLanes.size && k <= activeEndIndex) {
			if (TurnType.getPrimaryTurn(rawLanes[k]) != activeTurn) {
				if (TurnType.getSecondaryTurn(rawLanes[k]) == activeTurn) {
					TurnType.setSecondaryToPrimary(rawLanes, k)
				} else if (TurnType.getTertiaryTurn(rawLanes[k]) == activeTurn) {
					TurnType.setTertiaryToPrimary(rawLanes, k)
				}
			}
			k++
		}
	}

	/**
	 * Whether any of the lanes in the range can serve [mainTurnType] - the same turn, or one close
	 * enough to count. A sharp turn or a u turn is only allowed from the outermost lane, which is
	 * what the two flags decide.
	 */
	@JvmStatic
	fun hasAllowedLanes(mainTurnType: Int, lanesArray: IntArray, startActiveIndex: Int, endActiveIndex: Int): Boolean {
		if (lanesArray.isEmpty() || startActiveIndex > endActiveIndex) {
			return false
		}
		val activeLines = IntArray(endActiveIndex - startActiveIndex + 1)
		var i = startActiveIndex
		var j = 0
		while (i <= endActiveIndex) {
			activeLines[j] = lanesArray[i]
			i++
			j++
		}
		val possibleSharpLeftOrUTurn = startActiveIndex == 0
		val possibleSharpRightOrUTurn = endActiveIndex == lanesArray.size - 1
		for (k in activeLines.indices) {
			val turnType = TurnType.getPrimaryTurn(activeLines[k])
			if (turnType == mainTurnType) {
				return true
			}
			if (TurnType.isLeftTurnNoUTurn(mainTurnType) && TurnType.isLeftTurnNoUTurn(turnType)) {
				return true
			}
			if (TurnType.isRightTurnNoUTurn(mainTurnType) && TurnType.isRightTurnNoUTurn(turnType)) {
				return true
			}
			if (mainTurnType == TurnType.C && TurnType.isSlightTurn(turnType)) {
				return true
			}
			if (possibleSharpLeftOrUTurn && TurnType.isSharpLeftOrUTurn(mainTurnType) && TurnType.isSharpLeftOrUTurn(turnType)) {
				return true
			}
			if (possibleSharpRightOrUTurn && TurnType.isSharpRightOrUTurn(mainTurnType) && TurnType.isSharpRightOrUTurn(turnType)) {
				return true
			}
		}
		return false
	}

	/**
	 * The manoeuvre to announce, taken from the active lanes rather than from the geometry: if the
	 * lane the driver must be in says "sharp right", that is the turn, whatever the angle suggested.
	 * Muted when three or more lanes all go more or less straight, which is not worth speaking.
	 */
	@JvmStatic
	fun getActiveTurnType(lanes: IntArray?, leftSide: Boolean, oldTurnType: TurnType): TurnType {
		if (lanes == null || lanes.isEmpty()) {
			return oldTurnType
		}
		var tp = oldTurnType.value
		var cnt = 0
		val isOldTurnTypeSharp = TurnType.isSharpOrReverse(tp)
		for (k in lanes.indices) {
			val ln = lanes[k]
			if ((ln and 1) > 0) {
				val oneActiveLane = intArrayOf(lanes[k])
				if (hasAllowedLanes(oldTurnType.value, oneActiveLane, 0, 0)) {
					tp = TurnType.getPrimaryTurn(lanes[k])
					if (isOldTurnTypeSharp && TurnType.isSharpOrReverse(tp)) {
						break
					}
				}
				cnt++
			}
		}
		val t = TurnType.valueOf(tp, leftSide)
		// mute when most lanes have a straight/slight direction
		if (cnt >= 3 && TurnType.isSlightTurn(t.value)) {
			t.isSkipToSpeak = true
		}
		return t
	}

	/** Every turn the lanes offer, left to right, with duplicates removed. */
	@JvmStatic
	fun getPossibleTurns(oLanes: IntArray, onlyPrimary: Boolean, uniqueFromActive: Boolean): Array<Int> {
		val possibleTurns = LinkedHashSet<Int>()
		val upossibleTurns = LinkedHashSet<Int>()
		for (i in oLanes.indices) {
			// Nothing is in the list to compare to, so add the first elements
			upossibleTurns.clear()
			upossibleTurns.add(TurnType.getPrimaryTurn(oLanes[i]))
			if (!onlyPrimary && TurnType.getSecondaryTurn(oLanes[i]) != 0) {
				upossibleTurns.add(TurnType.getSecondaryTurn(oLanes[i]))
			}
			if (!onlyPrimary && TurnType.getTertiaryTurn(oLanes[i]) != 0) {
				upossibleTurns.add(TurnType.getTertiaryTurn(oLanes[i]))
			}
			if (!uniqueFromActive) {
				possibleTurns.addAll(upossibleTurns)
			} else if ((oLanes[i] and 1) == 1) {
				if (possibleTurns.isNotEmpty()) {
					possibleTurns.retainAll(upossibleTurns)
					if (possibleTurns.isEmpty()) {
						break
					}
				} else {
					possibleTurns.addAll(upossibleTurns)
				}
			}
		}
		// Remove all turns from lanes not selected...because those aren't it
		if (uniqueFromActive) {
			for (i in oLanes.indices) {
				if ((oLanes[i] and 1) == 0) {
					possibleTurns.remove(TurnType.getPrimaryTurn(oLanes[i]))
					if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
						possibleTurns.remove(TurnType.getSecondaryTurn(oLanes[i]))
					}
					if (TurnType.getTertiaryTurn(oLanes[i]) != 0) {
						possibleTurns.remove(TurnType.getTertiaryTurn(oLanes[i]))
					}
				}
			}
		}
		val array = possibleTurns.toTypedArray()
		array.sortWith(compareBy { TurnType.orderFromLeftToRight(it) })
		return array
	}

	/**
	 * The one turn a set of active lanes agrees on, when there is one. With two on offer the choice
	 * falls to which end of the road the lanes sit at.
	 */
	@JvmStatic
	fun inferSlightTurnFromActiveLanes(oLanes: IntArray, mostLeft: Boolean, mostRight: Boolean): Int {
		val possibleTurns = getPossibleTurns(oLanes, false, false)
		if (possibleTurns.isEmpty()) {
			// No common turns, so can't determine anything.
			return 0
		}
		var infer = 0
		if (possibleTurns.size == 1) {
			infer = possibleTurns[0]
		} else if (possibleTurns.size == 2) {
			// this method could be adapted for 3+ turns
			infer = if (mostLeft && !mostRight) {
				possibleTurns[0]
			} else if (mostRight && !mostLeft) {
				possibleTurns[possibleTurns.size - 1]
			} else {
				possibleTurns[1]
			}
		}
		return infer
	}

	// ---- making the lanes agree with the turn that was chosen ----

	/**
	 * Promotes [type] to the primary sign of every lane that offers it, and switches off the lanes
	 * that do not. Does nothing unless the manoeuvre really is [type] and some lane offers it.
	 */
	@JvmStatic
	fun inferActiveTurnLanesFromTurn(tt: TurnType, type: Int) {
		var found = false
		if (tt.value == type && tt.lanes != null) {
			for (it in tt.lanes!!.indices) {
				val turn = tt.lanes!![it]
				if (TurnType.getPrimaryTurn(turn) == type ||
					TurnType.getSecondaryTurn(turn) == type ||
					TurnType.getTertiaryTurn(turn) == type
				) {
					found = true
					break
				}
			}
		}
		if (found) {
			for (it in tt.lanes!!.indices) {
				val turn = tt.lanes!![it]
				if (TurnType.getPrimaryTurn(turn) != type) {
					if (TurnType.getSecondaryTurn(turn) == type) {
						val st = TurnType.getSecondaryTurn(turn)
						TurnType.setSecondaryTurn(tt.lanes!!, it, TurnType.getPrimaryTurn(turn))
						TurnType.setPrimaryTurn(tt.lanes!!, it, st)
					} else if (TurnType.getTertiaryTurn(turn) == type) {
						val st = TurnType.getTertiaryTurn(turn)
						TurnType.setTertiaryTurn(tt.lanes!!, it, TurnType.getPrimaryTurn(turn))
						TurnType.setPrimaryTurn(tt.lanes!!, it, st)
					} else {
						tt.lanes!![it] = turn and 1.inv()
					}
				} else {
					tt.lanes!![it] = turn or 1
				}
			}
		}
	}

	/**
	 * Makes every active lane show the same arrow, so the driver is not told to be in lanes that
	 * then disagree. The turn to settle on is the one they all share, or the one the next manoeuvre
	 * needs when this one is a keep.
	 */
	@JvmStatic
	fun inferCommonActiveLane(currentTurn: TurnType, nextTurn: TurnType) {
		val lanes = currentTurn.lanes!!
		val turnSet = HashSet<Int>()
		for (i in lanes.indices) {
			if (lanes[i] % 2 == 1) {
				turnSet.add(TurnType.getPrimaryTurn(lanes[i]))
				if (TurnType.getSecondaryTurn(lanes[i]) != 0) {
					turnSet.add(TurnType.getSecondaryTurn(lanes[i]))
				}
				if (TurnType.getTertiaryTurn(lanes[i]) != 0) {
					turnSet.add(TurnType.getTertiaryTurn(lanes[i]))
				}
			}
		}
		var singleTurn = 0
		if (turnSet.size == 1) {
			singleTurn = turnSet.iterator().next()
		} else if ((currentTurn.goAhead() || currentTurn.keepLeft() || currentTurn.keepRight()) &&
			turnSet.contains(nextTurn.value)
		) {
			val nextTurnLane = nextTurn.getActiveCommonLaneTurn()
			if (currentTurn.isPossibleLeftTurn && TurnType.isLeftTurn(nextTurn.value)) {
				singleTurn = nextTurn.value
			} else if (currentTurn.isPossibleLeftTurn && TurnType.isLeftTurn(nextTurnLane)) {
				singleTurn = nextTurnLane
			} else if (currentTurn.isPossibleRightTurn && TurnType.isRightTurn(nextTurn.value)) {
				singleTurn = nextTurn.value
			} else if (currentTurn.isPossibleRightTurn && TurnType.isRightTurn(nextTurnLane)) {
				singleTurn = nextTurnLane
			} else if ((currentTurn.goAhead() || currentTurn.keepLeft() || currentTurn.keepRight()) &&
				TurnType.isKeepDirectionTurn(nextTurnLane)
			) {
				singleTurn = nextTurnLane
			}
		}
		if (singleTurn == 0) {
			singleTurn = currentTurn.value
			if (singleTurn == TurnType.KL || singleTurn == TurnType.KR) {
				return
			}
		}
		for (i in lanes.indices) {
			if (lanes[i] % 2 == 1 && TurnType.getPrimaryTurn(lanes[i]) != singleTurn) {
				if (TurnType.getSecondaryTurn(lanes[i]) == singleTurn) {
					TurnType.setSecondaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]))
					TurnType.setPrimaryTurn(lanes, i, singleTurn)
				} else if (TurnType.getTertiaryTurn(lanes[i]) == singleTurn) {
					TurnType.setTertiaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]))
					TurnType.setPrimaryTurn(lanes, i, singleTurn)
				} else {
					if (lanes.size == 1) {
						return
					}
					// disable lane
					lanes[i] = lanes[i] - 1
				}
			}
		}
	}

	/**
	 * Narrows this manoeuvre's active lanes to the ones that still work for the next manoeuvre, so a
	 * driver told to keep left twice in a row ends up in a lane that serves both.
	 */
	@JvmStatic
	fun mergeTurnLanes(leftSide: Boolean, currentSegment: RouteSegmentResult, nextSegment: RouteSegmentResult): Boolean {
		val active = MergeTurnLaneTurn(currentSegment)
		val target = MergeTurnLaneTurn(nextSegment)
		if (active.activeLen < 2) {
			return false
		}
		if (target.activeStartIndex == -1) {
			return false
		}
		var changed = false
		if (target.isActiveTurnMostLeft()) {
			// let only the most left lanes be enabled
			if (target.activeLen < active.activeLen) {
				active.activeEndIndex -= (active.activeLen - target.activeLen)
				changed = true
			}
		} else if (target.isActiveTurnMostRight()) {
			// next turn is right
			// let only the most right lanes be enabled
			if (target.activeLen < active.activeLen) {
				active.activeStartIndex += (active.activeLen - target.activeLen)
				changed = true
			}
		} else {
			// next turn is get through (take out the left and the right turn)
			if (target.activeLen < active.activeLen) {
				if (target.originalLanes!!.size == active.activeLen) {
					active.activeEndIndex = active.activeStartIndex + target.activeEndIndex
					active.activeStartIndex += target.activeStartIndex
					changed = true
				} else {
					var straightActiveLen = 0
					var straightActiveBegin = -1
					for (i in active.activeStartIndex..active.activeEndIndex) {
						if (TurnType.hasAnyTurnLane(active.originalLanes!![i], TurnType.C)) {
							straightActiveLen++
							if (straightActiveBegin == -1) {
								straightActiveBegin = i
							}
						}
					}
					if (straightActiveBegin != -1 && straightActiveLen <= target.activeLen) {
						active.activeStartIndex = straightActiveBegin
						active.activeEndIndex = straightActiveBegin + straightActiveLen - 1
						changed = true
					} else {
						// cause the next-turn goes forward exclude left most and right most lane
						if (active.activeStartIndex == 0) {
							active.activeStartIndex++
							active.activeLen--
						}
						if (active.activeEndIndex == active.originalLanes!!.size - 1) {
							active.activeEndIndex--
							active.activeLen--
						}
						// java divided by a float 2f here, not by an int
						val ratio = (active.activeLen - target.activeLen) / 2f
						if (ratio > 0) {
							active.activeEndIndex = ceil(active.activeEndIndex - ratio).toInt()
							active.activeStartIndex = floor(active.activeStartIndex + ratio).toInt()
						}
						changed = true
					}
				}
			}
		}
		if (!changed) {
			return false
		}

		// set the allowed lane bit
		val disabled = active.disabledLanes!!
		for (i in disabled.indices) {
			if (i >= active.activeStartIndex && i <= active.activeEndIndex &&
				active.originalLanes!![i] % 2 == 1
			) {
				disabled[i] = disabled[i] or 1
			}
		}
		currentSegment.getTurnType()!!.lanes = disabled
		return true
	}

	/**
	 * Replaces a "keep left" or "keep right" that the lanes contradict with the turn the lanes
	 * actually show, so the arrow and the words do not point different ways.
	 */
	@JvmStatic
	fun replaceConfusingKeepTurnsWithLaneTurn(currentSegment: RouteSegmentResult, leftSide: Boolean) {
		if (currentSegment.getTurnType() == null) {
			return
		}
		val currentTurn = currentSegment.getTurnType()!!.value
		val activeTurn = currentSegment.getTurnType()!!.getActiveCommonLaneTurn()
		var changeToActive = false
		if (TurnType.isKeepDirectionTurn(currentTurn) && !TurnType.isKeepDirectionTurn(activeTurn)) {
			if (TurnType.isLeftTurn(currentTurn) && !TurnType.isLeftTurn(activeTurn)) {
				changeToActive = true
			}
			if (TurnType.isRightTurn(currentTurn) && !TurnType.isRightTurn(activeTurn)) {
				changeToActive = true
			}
		}
		if (changeToActive) {
			val turn = TurnType.valueOf(activeTurn, leftSide)
			turn.lanes = currentSegment.getTurnType()!!.lanes
			currentSegment.setTurnType(turn)
		}
	}

	// ---- the roads on the other side of the fork ----

	/**
	 * Gives every road leaving on one side a turn of its own, fanning outwards from [mainLaneType]
	 * so that two roads leaving to the left do not both become "turn left".
	 */
	@JvmStatic
	fun synteticAssignTurnTypes(rs: RoadSplitStructure, mainLaneType: Int, roads: MutableList<AttachedRoadInfo>, left: Boolean) {
		val col = if (left) rs.leftLanesInfo else rs.rightLanesInfo
		val sign = if (left) 1 else -1
		col.sortWith { o1, o2 -> sign * o1.attachedAngle.compareTo(o2.attachedAngle) }
		var type = mainLaneType
		for (i in col.indices.reversed()) {
			val info = col[i]
			val turnByAngle = getTurnByAngle(info.attachedAngle)
			type = if (left && turnByAngle >= type) {
				TurnType.getPrev(type)
			} else if (!left && turnByAngle <= type) {
				TurnType.getNext(type)
			} else {
				turnByAngle
			}
			info.turnType = type
			roads.add(info)
		}
	}

	/** The turn an angle amounts to, from a slight bend through to a u turn. */
	@JvmStatic
	fun getTurnByAngle(angle: Double): Int {
		var turnType = TurnType.C
		if (angle < -150) {
			turnType = TurnType.TRU
		} else if (angle < -120) {
			turnType = TurnType.TSHR
		} else if (angle < -TURN_DEGREE_MIN) {
			turnType = TurnType.TR
		} else if (angle <= -TURN_SLIGHT_DEGREE) {
			turnType = TurnType.TSLR
		} else if (angle > -TURN_SLIGHT_DEGREE && angle < TURN_SLIGHT_DEGREE) {
			turnType = TurnType.C
		} else if (angle < TURN_DEGREE_MIN) {
			turnType = TurnType.TSLL
		} else if (angle < 120) {
			turnType = TurnType.TL
		} else if (angle < 150) {
			turnType = TurnType.TSHL
		} else {
			turnType = TurnType.TU
		}
		return turnType
	}

	/**
	 * The manoeuvre to announce at a fork, given what the lanes on this road say and what the roads
	 * on the other side take with them. A "keep" is dropped when only one turn is left once the
	 * other side's turns are accounted for, or when the side being kept only goes straight anyway.
	 */
	@JvmStatic
	fun getTurnByCurrentTurns(
		otherSideLanesInfo: List<AttachedRoadInfo>?,
		rawLanes: IntArray,
		keepTurnType: Int,
		leftSide: Boolean
	): TurnType {
		val otherSideTurns = LinkedHashSet<Int>()
		if (otherSideLanesInfo != null) {
			for (li in otherSideLanesInfo) {
				val parsed = li.parsedLanes
				if (parsed != null) {
					for (i in parsed) {
						TurnType.collectTurnTypes(i, otherSideTurns)
					}
				}
			}
		}
		val currentTurns = LinkedHashSet<Int>()
		for (ln in rawLanes) {
			TurnType.collectTurnTypes(ln, currentTurns)
		}
		var analyzedList = ArrayList(currentTurns)
		if (analyzedList.size > 1) {
			if (keepTurnType == TurnType.KL) {
				// no need analyze turns in left side (current direction)
				analyzedList.removeAt(0)
			} else if (keepTurnType == TurnType.KR) {
				// no need analyze turns in right side (current direction)
				analyzedList.removeAt(analyzedList.size - 1)
			}
			// Here we detect single case when turn lane continues on 1 road / single sign and all
			// other lane turns continue on the other side roads
			if (analyzedList.containsAll(otherSideTurns)) {
				currentTurns.removeAll(otherSideTurns)
				if (currentTurns.size == 1) {
					val detectedTurn = currentTurns.iterator().next()
					if (keepTurnType == TurnType.KR && TurnType.isLeftTurn(detectedTurn)) {
						return TurnType.valueOf(keepTurnType, leftSide)
					}
					if (keepTurnType == TurnType.KL && TurnType.isRightTurn(detectedTurn)) {
						return TurnType.valueOf(keepTurnType, leftSide)
					}
					return TurnType.valueOf(detectedTurn, leftSide)
				}
			} else {
				// Avoid "keep" instruction if active side contains only "through" moving
				analyzedList = ArrayList(currentTurns)
				if (keepTurnType == TurnType.KL && analyzedList[0] == TurnType.C) {
					return TurnType.valueOf(TurnType.C, leftSide)
				}
				if (keepTurnType == TurnType.KR && analyzedList[analyzedList.size - 1] == TurnType.C) {
					return TurnType.valueOf(TurnType.C, leftSide)
				}
			}
		}
		return TurnType.valueOf(keepTurnType, leftSide)
	}

	// ---- odds and ends ----

	/** How loudly a road deserves to be mentioned; the bigger the road, the smaller the number. */
	@JvmStatic
	fun highwaySpeakPriority(highway: String?): Int {
		if (highway == null || highway.endsWith("track") || highway.endsWith("services") ||
			highway.endsWith("service") || highway.endsWith("path")
		) {
			return MAX_SPEAK_PRIORITY
		}
		if (highway.endsWith("_link") || highway.endsWith("unclassified") || highway.endsWith("road") ||
			highway.endsWith("living_street") || highway.endsWith("residential")
		) {
			return 3
		}
		if (highway.endsWith("tertiary")) {
			return 2
		}
		if (highway.endsWith("secondary")) {
			return 1
		}
		return 0
	}

	/** Whether the string names [turnType] among its lanes. */
	@JvmStatic
	fun hasTurn(turnLanes: String?, turnType: Int): Boolean {
		if (turnLanes == null) {
			return false
		}
		for (lane in getUniqTurnTypes(turnLanes)) {
			if (lane == turnType) {
				return true
			}
		}
		return false
	}

	/** Whether any lane of the string is a sharp turn or a u turn. */
	@JvmStatic
	fun hasSharpOrReverseTurnLane(turnLanesPrevSegm: String?): Boolean {
		if (turnLanesPrevSegm == null) {
			return false
		}
		for (lane in getUniqTurnTypes(turnLanesPrevSegm)) {
			if (TurnType.isSharpOrReverse(lane)) {
				return true
			}
		}
		return false
	}

	/** Whether both roads offer exactly the same turns in the same order. */
	@JvmStatic
	fun hasSameTurnLanes(prevSegm: RouteSegmentResult, currentSegm: RouteSegmentResult): Boolean {
		val turnLanesPrevSegm = getTurnLanesString(prevSegm)
		val turnLanesCurrSegm = getTurnLanesString(currentSegm)
		if (turnLanesPrevSegm == null || turnLanesCurrSegm == null) {
			return false
		}
		val uniqPrev = getUniqTurnTypes(turnLanesPrevSegm)
		val uniqCurr = getUniqTurnTypes(turnLanesCurrSegm)
		if (uniqPrev.size != uniqCurr.size) {
			return false
		}
		for (i in uniqCurr.indices) {
			if (uniqPrev[i] != uniqCurr[i]) {
				return false
			}
		}
		return true
	}

	/** A Y split where the road narrows and the only road leaving it is wide enough to matter. */
	@JvmStatic
	fun isForkByLanes(curr: RouteSegmentResult, prev: RouteSegmentResult): Boolean {
		// check for Y-intersections with many lanes
		if (countLanesMinOne(curr) < countLanesMinOne(prev)) {
			val attachedRoutes = curr.getAttachedRoutes(curr.getStartPointIndex())
			if (attachedRoutes.size == 1) {
				return countLanesMinOne(attachedRoutes[0]) >= 2
			}
		}
		return false
	}

	// ---- splitting the way java's String.split does ----

	/**
	 * `s.split(delimiter)` the way java's `split(regex, -1)` does it: every part is kept, trailing
	 * empty ones included. Kotlin's own split already behaves this way; the name is here so the two
	 * shapes below cannot be mixed up.
	 */
	private fun splitKeepingEmpty(s: String, delimiter: String): List<String> = s.split(delimiter)

	/**
	 * `s.split(delimiter)` the way java's `split(regex)` does it with the default limit: trailing
	 * empty parts are dropped, except that a string with no delimiter in it comes back whole even
	 * when it is empty. Kotlin's split keeps the trailing empties, so "left;" would gain a lane
	 * that java never saw.
	 */
	private fun splitDroppingTrailingEmpty(s: String, delimiter: String): List<String> {
		val parts = s.split(delimiter)
		if (parts.size == 1) {
			return parts
		}
		var end = parts.size
		while (end > 0 && parts[end - 1].isEmpty()) {
			end--
		}
		return parts.subList(0, end)
	}

	/** One manoeuvre's lanes, split into the active run and the rest, for [mergeTurnLanes]. */
	private class MergeTurnLaneTurn(segment: RouteSegmentResult) {
		val turn: TurnType? = segment.getTurnType()
		val originalLanes: IntArray?
		val disabledLanes: IntArray?
		var activeStartIndex = -1
		var activeEndIndex = -1
		var activeLen = 0

		init {
			originalLanes = turn?.lanes
			val original = originalLanes
			if (original != null) {
				val disabled = IntArray(original.size)
				for (i in original.indices) {
					val ln = original[i]
					disabled[i] = ln and 1.inv()
					if ((ln and 1) > 0) {
						if (activeStartIndex == -1) {
							activeStartIndex = i
						}
						activeEndIndex = i
						activeLen++
					}
				}
				disabledLanes = disabled
			} else {
				disabledLanes = null
			}
		}

		fun isActiveTurnMostLeft(): Boolean = activeStartIndex == 0

		fun isActiveTurnMostRight(): Boolean = activeEndIndex == originalLanes!!.size - 1
	}
}
