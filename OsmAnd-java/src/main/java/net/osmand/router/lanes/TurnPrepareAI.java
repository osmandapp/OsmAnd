package net.osmand.router.lanes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.binary.RouteDataObject;
import net.osmand.router.lanes.TurnTypeAI.Lane;
import net.osmand.router.lanes.TurnTypeAI.LaneGroup;
import net.osmand.router.lanes.TurnTypeAI.Maneuver;
import net.osmand.router.lanes.TurnTypeAI.TransportMode;
import net.osmand.router.lanes.TurnTypeAI.TurnIndication;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.util.MapUtils;

/**
 * Turn and lane preparation written on {@link TurnTypeAI}.
 *
 * <p>The pass answers three questions per junction, in this order, and never lets a later answer
 * rewrite an earlier one:
 *
 * <ol>
 * <li><b>Is there a maneuver at all?</b> Geometry decides. A turn is a turn when the road bends
 *     more than {@link #TURN_MIN_DEG}; below that there is a maneuver only if the road actually
 *     forks, and a crossroads you drive straight through is not an instruction.</li>
 * <li><b>Which lanes lead there?</b> The lane model of the road BEFORE the junction is read from
 *     OSM, lanes the current profile may not use are excluded from the start, and among the rest
 *     the run of neighbouring lanes on the side of the maneuver is marked active. A run never
 *     crosses a line that {@code change:lanes} forbids crossing.</li>
 * <li><b>Does the lane picture change the maneuver?</b> Only in one direction: a straight-on
 *     instruction may become a slight turn or a keep when every active lane says so. A maneuver
 *     that geometry called a turn is never softened by lanes.</li>
 * </ol>
 *
 * <p>Everything it produces goes into {@link RouteSegmentResult#setTurnTypeAI}, which keeps the
 * legacy {@link TurnType} in step through {@link TurnTypeAI#getOldTurnType()}.
 */
public class TurnPrepareAI {

	/** below this a bend is not a turn, it is the road going where the road goes */
	private static final float TURN_MIN_DEG = 45;
	/** past this a turn is sharp */
	private static final float SHARP_DEG = 120;
	/** and past this it is a U-turn, on the side of the road people drive on */
	private static final float U_TURN_DEG = 150;
	/** how far a pair of bends must turn in total to be one U-turn */
	private static final float U_TURN_ROTATION_DEG = 120;
	/** an arrow and a road closer than this to each other are about the same thing */
	private static final float ARROW_MATCHES_ROAD_DEG = 60;
	/** arrows are not painted for a choice further away than this */
	private static final float ARROWS_REACH_M = 150;
	/** beyond this the road is bending, not running straight */
	private static final float BENDS_DEG = 25;
	/** below this the road goes straight, whatever the arrow beside it says */
	private static final float TURN_HINT_DEG = 10;
	/** two bends the same way within this are one maneuver */
	private static final float SAME_MANEUVER_M = 50;
	/** how far ahead a turn still decides which lane to be in now */
	private static final float LOOK_AHEAD_M = 100;
	/** an unmarked road shorter than this keeps the lanes of the road before it */
	private static final float CARRY_LANES_M = 60;
	/** a straight-on instruction this close to the next real turn belongs to the same junction */
	private static final float ONE_JUNCTION_M = 100;

	private static final Set<TurnIndication> STRAIGHT_INDICATIONS = EnumSet.of(
			TurnIndication.THROUGH, TurnIndication.NONE,
			TurnIndication.MERGE_TO_LEFT, TurnIndication.MERGE_TO_RIGHT);

	// ------------------------------------------------------------------ entry point

	public void prepareTurnResults(RoutingContext ctx, List<RouteSegmentResult> result) {
		if (result == null || result.isEmpty()) {
			return;
		}
		boolean leftHand = ctx != null && ctx.leftSideNavigation;
		TransportMode mode = modeOf(ctx);
		for (int i = 0; i < result.size(); i++) {
			result.get(i).setTurnTypeAI(defineTurn(result, i, leftHand, mode));
		}
		dropContinuationOfTheSameTurn(result);
		justifyUTurns(result, leftHand);
		oneLaneInstructionPerInterchange(result);
		lookAheadToTheNextTurn(result);
		keepBecomesCarryOn(result);
		removeEmptyCarryOn(result);
		muteStraightsOfTheSameJunction(result);
	}

	// ------------------------------------------------------------------ one junction

	private TurnTypeAI defineTurn(List<RouteSegmentResult> route, int i, boolean leftHand, TransportMode mode) {
		RouteSegmentResult current = route.get(i);
		if (i == 0) {
			// the driver is already on this road: nothing happened here, so nothing is said
			return null;
		}
		RouteSegmentResult prev = route.get(i - 1);
		if (prev.getObject().roundabout()) {
			return null; // leaving a roundabout was announced when entering it
		}
		if (current.getObject().roundabout()) {
			return roundabout(route, i, leftHand, mode);
		}

		JunctionAI junction = JunctionAI.at(prev, current);
		double leftDeg = junction.routeAngle();
		List<Lane> lanes = lanesOf(prev, leftHand);
		boolean ownMarkings = !lanes.isEmpty();
		double carried = prev.getDistance();
		for (int j = i - 2; lanes.isEmpty() && j >= 0 && carried < CARRY_LANES_M; j--) {
			// a few metres of unmarked road between two junctions is the same carriageway: the
			// lanes painted before it still say which lane leads where
			lanes = lanesOf(route.get(j), leftHand);
			carried += route.get(j).getDistance();
		}
		double decisive = decisiveAngle(route, i, junction, lanes, leftHand);
		boolean decidedAhead = decisive != junction.routeAngle();
		TurnIndication routeArrow = lanes.isEmpty() ? null : routeArrow(junction, lanes, decisive);

		Maneuver maneuver = maneuverOf(leftDeg, taggedTurn(current), leftHand);
		boolean choice = junction.isChoice() || distinctArrows(lanes).size() > 1;
		if (maneuver == null && routeArrow != null && choice) {
			// geometry is silent but the road is marked and there is a choice: the arrow the route
			// takes is the instruction, which is how a driver reads a junction like this
			maneuver = maneuverOfIndication(routeArrow, leftHand);
			if (maneuver == Maneuver.CONTINUE) {
				Maneuver keep = keepOf(junction);
				maneuver = keep != null ? keep : distinctArrows(lanes).size() > 1 ? Maneuver.CONTINUE : null;
			}
		}
		if (maneuver == null) {
			maneuver = keepOf(junction);
			if (maneuver == null) {
				return null; // straight on through a junction that does not fork: not an instruction
			}
		}
		if (maneuver == Maneuver.CONTINUE && !ownMarkings) {
			// borrowed markings are there to serve the junction they were painted for; repeating
			// them at the next one, with nothing to announce, is how one junction becomes three
			return null;
		}
		if (lanes.isEmpty()) {
			lanes = countedLanes(prev);
		}
		boolean mapped = hasArrows(lanes);
		lanes = mapped ? markActive(lanes, wanted(maneuver, routeArrow), mode)
				: markBySide(lanes, maneuver, junction);
		// only arrows the map painted may correct the maneuver; ones we painted ourselves came from
		// the maneuver in the first place and would just be read back
		maneuver = mapped ? refineByLanes(maneuver, lanes) : maneuver;
		// a carry-on with lanes to show is the warning a driver needs in time to change lane, so it
		// speaks; a carry-on with nothing to show has nothing to say
		boolean skipToSpeak = maneuver == Maneuver.CONTINUE && lanes.isEmpty();
		return new TurnTypeAI(maneuver, leftHand, mode, 0, (float) -leftDeg, skipToSpeak, lanes)
				.decidedAhead(decidedAhead);
	}

	/**
	 * The maneuver as geometry sees it, or null for straight on. A {@code turn} tag on the road
	 * being entered outranks the angle, because it is a statement and the angle is a measurement.
	 */
	private Maneuver maneuverOf(double leftDeg, TurnIndication tagged, boolean leftHand) {
		if (tagged != null) {
			Maneuver m = maneuverOfIndication(tagged, leftHand);
			return m != null && m != Maneuver.CONTINUE ? m : null;
		}
		double abs = Math.abs(leftDeg);
		if (abs < TURN_MIN_DEG) {
			return null;
		}
		boolean left = leftDeg > 0;
		if (abs >= U_TURN_DEG && left != leftHand) {
			// a U-turn is made towards the centre line, which is on the driver's left where people
			// drive on the right; the other way round it is just a very sharp turn
			return Maneuver.U_TURN;
		}
		if (abs >= SHARP_DEG) {
			return left ? Maneuver.SHARP_LEFT : Maneuver.SHARP_RIGHT;
		}
		return left ? Maneuver.TURN_LEFT : Maneuver.TURN_RIGHT;
	}

	private Maneuver maneuverOfIndication(TurnIndication t, boolean leftHand) {
		switch (t) {
			case LEFT:
				return Maneuver.TURN_LEFT;
			case SLIGHT_LEFT:
				return Maneuver.SLIGHT_LEFT;
			case SHARP_LEFT:
				return Maneuver.SHARP_LEFT;
			case RIGHT:
				return Maneuver.TURN_RIGHT;
			case SLIGHT_RIGHT:
				return Maneuver.SLIGHT_RIGHT;
			case SHARP_RIGHT:
				return Maneuver.SHARP_RIGHT;
			case REVERSE:
				return Maneuver.U_TURN;
			default:
				return Maneuver.CONTINUE;
		}
	}

	/**
	 * A keep instruction is about a FORK: another road leaving beside the route at a small angle,
	 * which a driver could take by mistake. A road leaving at a right angle is a junction, and
	 * driving straight through one needs no instruction.
	 */
	private Maneuver keepOf(JunctionAI junction) {
		if (junction.forkLeft() > 0 && junction.forkRight() == 0) {
			return Maneuver.KEEP_RIGHT;
		}
		if (junction.forkRight() > 0 && junction.forkLeft() == 0) {
			return Maneuver.KEEP_LEFT;
		}
		return null;
	}

	/** lanes may soften a straight instruction, never a turn: they say where a lane goes, not how hard */
	private Maneuver refineByLanes(Maneuver maneuver, List<Lane> lanes) {
		if (maneuver != Maneuver.CONTINUE && maneuver != Maneuver.KEEP_LEFT && maneuver != Maneuver.KEEP_RIGHT) {
			return maneuver;
		}
		TurnIndication common = null;
		for (Lane lane : lanes) {
			if (!lane.isActive()) {
				continue;
			}
			TurnIndication t = lane.taken() != null ? lane.taken()
					: lane.turns().isEmpty() ? null : lane.turns().get(0);
			if (t == null) {
				return maneuver;
			}
			if (common != null && common != t) {
				return maneuver;
			}
			common = t;
		}
		if (common == TurnIndication.SLIGHT_LEFT) {
			return Maneuver.SLIGHT_LEFT;
		}
		if (common == TurnIndication.SLIGHT_RIGHT) {
			return Maneuver.SLIGHT_RIGHT;
		}
		return maneuver;
	}

	/**
	 * Which arrow of the marked road the route takes.
	 *
	 * <p>A junction is read by counting, not by measuring: when the roads that leave and the kinds
	 * of arrow painted on the lanes are the same in number, the road that is second from the left
	 * is the one the second arrow from the left points at. Only when the two do not line up does
	 * the angle decide, by the arrow whose usual direction is closest to where the route goes.
	 */
	/**
	 * How far the route turns by the time the marked choice is actually made.
	 *
	 * <p>Markings that offer more kinds of arrow than the junction has roads are not about that
	 * junction alone: three left arrows and a through arrow can all lead onto the same next road,
	 * and which of them a driver needs is settled one junction further on. In that case the angle
	 * that decides is measured from the marked road to wherever the route has got to by then,
	 * which is what a driver reading those arrows is really being asked.
	 *
	 * <p>The reach stops at the first road that carries markings of its own, because that road
	 * answers for its own junction, and at {@link #ARROWS_REACH_M}, because nobody paints arrows
	 * for a turn that far away.
	 */
	private double decisiveAngle(List<RouteSegmentResult> route, int i, JunctionAI junction,
			List<Lane> lanes, boolean leftHand) {
		List<TurnIndication> arrows = distinctArrows(lanes);
		// two signs that the arrows are not about this junction: they offer more kinds than there
		// are roads here, or they offer no way of carrying straight on while the road does exactly
		// that - left or right, said on a road that runs straight, is said about what comes next
		boolean moreKindsThanRoads = arrows.size() > junction.optionCount();
		boolean straightWithoutAThroughArrow = Math.abs(junction.routeAngle()) < BENDS_DEG
				&& !arrows.contains(TurnIndication.THROUGH);
		if (!moreKindsThanRoads && !straightWithoutAThroughArrow) {
			return junction.routeAngle();
		}
		double angle = junction.routeAngle();
		double distance = route.get(i).getDistance();
		for (int j = i + 1; j < route.size() && distance <= ARROWS_REACH_M; j++) {
			if (!lanesOf(route.get(j - 1), leftHand).isEmpty()) {
				break;
			}
			angle = MapUtils.degreesDiff(bearingOut(route.get(i - 1)), bearingIn(route.get(j)));
			distance += route.get(j).getDistance();
		}
		return angle;
	}

	private TurnIndication routeArrow(JunctionAI junction, List<Lane> lanes, double angle) {
		List<TurnIndication> arrows = distinctArrows(lanes);
		if (arrows.isEmpty()) {
			return null;
		}
		if (arrows.size() == 1) {
			return arrows.get(0);
		}
		// the exit relaxation is about the geometry of THIS junction; when the arrows look past it
		// the angle already carries the whole picture and needs no help
		boolean leaving = junction.routeLeavesMainRoad() && angle == junction.routeAngle();
		if (junction.optionCount() == arrows.size()) {
			TurnIndication byOrder = arrows.get(junction.routeRank());
			if (plausible(byOrder, angle) || (leaving && gentle(byOrder)
					&& onTheRouteSide(byOrder, junction, angle))) {
				return byOrder;
			}
		}
		if (junction.optionCount() > 1) {
			// the counts do not line up, but the order still does: the leftmost road is what the
			// leftmost arrow points at, the rightmost road what the rightmost arrow points at
			int index = (int) Math.round(junction.routeRank() * (arrows.size() - 1.0)
					/ (junction.optionCount() - 1.0));
			TurnIndication byOrder = arrows.get(Math.max(0, Math.min(arrows.size() - 1, index)));
			if (plausible(byOrder, angle) || (leaving && gentle(byOrder)
					&& onTheRouteSide(byOrder, junction, angle))) {
				return byOrder;
			}
		}
		// a road that bends noticeably is taking one of the turning arrows, not the straight one:
		// junctions are drawn gentler than the arrows painted on them
		// an exit bends gently, so it counts as bending only when there is a gentle arrow to take;
		// "left or through" on a road that runs almost straight is not an exit's pair of arrows
		boolean bends = Math.abs(angle) >= BENDS_DEG || (leaving && hasGentleTurn(arrows));
		TurnIndication best = null;
		double bestDistance = Double.MAX_VALUE;
		for (TurnIndication arrow : arrows) {
			if (bends && (arrow == TurnIndication.THROUGH || usualAngle(arrow) * angle < 0)) {
				continue;
			}
			double distance = Math.abs(usualAngle(arrow) - angle);
			if (distance < bestDistance) {
				best = arrow;
				bestDistance = distance;
			}
		}
		if (best != null) {
			return best;
		}
		for (TurnIndication arrow : arrows) {
			double distance = Math.abs(usualAngle(arrow) - angle);
			if (distance < bestDistance) {
				best = arrow;
				bestDistance = distance;
			}
		}
		return best;
	}

	/**
	 * Counting the roads and counting the arrows can agree by accident. On a big interchange the
	 * markings describe the whole thing - left, left, through, through, slight right - while the
	 * junction under the wheels offers only one of those, so "the second road from the left" is
	 * not "the second arrow from the left". An arrow that is not straight has to point the way the
	 * route actually goes.
	 */
	private boolean plausible(TurnIndication arrow, double routeAngle) {
		if (arrow == TurnIndication.THROUGH) {
			return true;
		}
		// the road has to bend at least halfway towards what the arrow promises: a twelve degree
		// bend is not the "left" of left-or-through, it is the "through"
		double promise = Math.abs(usualAngle(arrow));
		return Math.abs(routeAngle) >= Math.max(TURN_HINT_DEG, promise / 2)
				&& usualAngle(arrow) * routeAngle > 0;
	}

	private boolean hasGentleTurn(List<TurnIndication> arrows) {
		for (TurnIndication arrow : arrows) {
			if (arrow != TurnIndication.THROUGH && gentle(arrow)) {
				return true;
			}
		}
		return false;
	}

	/** an exit bears off gently, so the arrow it is drawn with is a gentle one */
	private boolean gentle(TurnIndication arrow) {
		return Math.abs(usualAngle(arrow)) <= Math.abs(usualAngle(TurnIndication.SLIGHT_LEFT));
	}

	/**
	 * The arrow points the way the route leaves relative to the roads it leaves beside: an exit
	 * that bends five degrees is still the left one of two when the road it leaves runs to the
	 * right of it.
	 */
	private boolean onTheRouteSide(TurnIndication arrow, JunctionAI junction, double angle) {
		double side = usualAngle(arrow);
		if (side == 0) {
			return true;
		}
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && (angle - option.angle()) * side > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A lane with nothing painted on it is not a lane without an answer: {@code turn:lanes=left|}
	 * says the left lane turns and the other one carries on, which is the whole point of writing
	 * the empty half.
	 */
	private List<TurnIndication> arrowsOf(Lane lane) {
		return lane.turns().isEmpty() ? Collections.singletonList(TurnIndication.THROUGH) : lane.turns();
	}

	/** every kind of arrow the road carries, ordered left to right */
	private List<TurnIndication> distinctArrows(List<Lane> lanes) {
		List<TurnIndication> arrows = new ArrayList<>();
		for (Lane lane : lanes) {
			for (TurnIndication arrow : arrowsOf(lane)) {
				TurnIndication normalized = normalize(arrow);
				if (!arrows.contains(normalized)) {
					arrows.add(normalized);
				}
			}
		}
		Collections.sort(arrows, new Comparator<TurnIndication>() {
			@Override
			public int compare(TurnIndication a, TurnIndication b) {
				return Double.compare(usualAngle(b), usualAngle(a));
			}
		});
		return arrows;
	}

	/** none, through and the two merges all mean "carry on" */
	private static TurnIndication normalize(TurnIndication arrow) {
		return STRAIGHT_INDICATIONS.contains(arrow) ? TurnIndication.THROUGH : arrow;
	}

	/** where an arrow usually points, in degrees to the left, so arrows and roads can be compared */
	private static double usualAngle(TurnIndication arrow) {
		switch (arrow) {
			case REVERSE:
				return 180;
			case SHARP_LEFT:
				return 135;
			case LEFT:
				return 90;
			case SLIGHT_LEFT:
				return 40;
			case SLIGHT_RIGHT:
				return -40;
			case RIGHT:
				return -90;
			case SHARP_RIGHT:
				return -135;
			default:
				return 0;
		}
	}

	/**
	 * A road that says how many lanes it has but not what they are for. The lanes are still real
	 * and a driver still has to be in the right one, so they are modelled with no arrows at all,
	 * which is the honest shape: we know they exist, we do not know what is painted on them.
	 */
	private List<Lane> countedLanes(RouteSegmentResult segment) {
		int count = laneCount(segment);
		if (count < 2) {
			return Collections.emptyList(); // one lane is not a choice worth drawing
		}
		List<Lane> lanes = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			lanes.add(new Lane.Builder().group(segment.isForwardDirection()
					? LaneGroup.FORWARD : LaneGroup.BACKWARD).build());
		}
		return lanes;
	}

	/** how many lanes the driver has in their direction */
	private int laneCount(RouteSegmentResult segment) {
		Map<String, String> tags = tagsOf(segment.getObject());
		Integer own = number(tags.get(segment.isForwardDirection() ? "lanes:forward" : "lanes:backward"));
		if (own != null) {
			return own;
		}
		Integer total = number(tags.get("lanes"));
		if (total == null) {
			return 0;
		}
		return segment.getObject().getOneway() != 0 ? total : Math.max(1, total / 2);
	}

	private static Integer number(String value) {
		try {
			return value == null ? null : Integer.valueOf(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private boolean hasArrows(List<Lane> lanes) {
		for (Lane lane : lanes) {
			if (!lane.turns().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Unmarked lanes, marked by where the route goes: a turn is made from the lanes on that side,
	 * as many of them as the road being entered has. The maneuver's own arrow is written onto them,
	 * because that is what a driver would see painted there if anyone had painted it.
	 */
	private List<Lane> markBySide(List<Lane> lanes, Maneuver maneuver, JunctionAI junction) {
		if (lanes.isEmpty()) {
			return lanes;
		}
		int count = lanes.size();
		TurnIndication arrow = arrowOf(maneuver);
		if ((maneuver == Maneuver.KEEP_LEFT || maneuver == Maneuver.KEEP_RIGHT)
				&& Math.abs(junction.routeAngle()) < BENDS_DEG && !junction.routeLeavesMainRoad()) {
			// a keep on a road that neither bends nor leaves paints nothing: the lanes carry on,
			// and an arrow to the side would tell a driver to move over for no reason. An exit is
			// the other case - it bears off however gently, and the lane to it is worth an arrow
			arrow = TurnIndication.THROUGH;
		}
		int width = Math.max(1, Math.min(count, laneCount(junction.routeOption().segment())));
		boolean routeOnTheLeft = true;
		JunctionAI.Option other = null;
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && (other == null || Math.abs(option.angle()) < Math.abs(other.angle()))) {
				other = option;
			}
		}
		if (other != null) {
			routeOnTheLeft = junction.routeAngle() > other.angle();
		} else if (isRightish(EnumSet.of(arrow))) {
			routeOnTheLeft = false;
		} else if (!isLeftish(EnumSet.of(arrow))) {
			width = count; // straight on with nothing else leaving: every lane carries on
		}
		int from = routeOnTheLeft ? 0 : count - width;
		int to = from + width - 1;

		// the other branch takes its own lanes from the same carriageway, and when the two do not
		// fit side by side the lane between them feeds both: that is where a driver has a choice
		int otherFrom = count;
		int otherTo = -1;
		TurnIndication otherArrow = TurnIndication.THROUGH;
		if (other != null) {
			int otherWidth = Math.max(1, Math.min(count, laneCount(other.segment())));
			otherArrow = arrowForAngle(other.angle());
			otherFrom = routeOnTheLeft ? count - otherWidth : 0;
			otherTo = otherFrom + otherWidth - 1;
		}

		List<Lane> out = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Lane lane = lanes.get(i);
			boolean active = i >= from && i <= to;
			List<TurnIndication> arrows = new ArrayList<>();
			if (active && arrow != TurnIndication.THROUGH) {
				arrows.add(arrow); // the route's own arrow first, it is the one being taken
			}
			if (i >= otherFrom && i <= otherTo && otherArrow != arrow) {
				arrows.add(otherArrow);
			}
			if (arrows.size() > 1 && !active) {
				Collections.reverse(arrows);
			}
			if (!arrows.isEmpty()) {
				lane = new Lane.Builder().group(lane.group()).turns(arrows).build();
			}
			out.add(active ? lane.withTaken(arrow == TurnIndication.THROUGH && lane.turns().isEmpty()
					? null : arrow) : lane.withActive(false));
		}
		return out;
	}

	/** the arrow a road leaving at this angle would be painted with */
	private TurnIndication arrowForAngle(double angle) {
		double abs = Math.abs(angle);
		if (abs < BENDS_DEG) {
			return TurnIndication.THROUGH;
		}
		if (abs < 60) {
			return angle > 0 ? TurnIndication.SLIGHT_LEFT : TurnIndication.SLIGHT_RIGHT;
		}
		if (abs < SHARP_DEG) {
			return angle > 0 ? TurnIndication.LEFT : TurnIndication.RIGHT;
		}
		return angle > 0 ? TurnIndication.SHARP_LEFT : TurnIndication.SHARP_RIGHT;
	}

	private TurnIndication arrowOf(Maneuver maneuver) {
		switch (maneuver) {
			case TURN_LEFT:
				return TurnIndication.LEFT;
			case SLIGHT_LEFT:
			case KEEP_LEFT:
				return TurnIndication.SLIGHT_LEFT;
			case SHARP_LEFT:
				return TurnIndication.SHARP_LEFT;
			case TURN_RIGHT:
				return TurnIndication.RIGHT;
			case SLIGHT_RIGHT:
			case KEEP_RIGHT:
				return TurnIndication.SLIGHT_RIGHT;
			case SHARP_RIGHT:
				return TurnIndication.SHARP_RIGHT;
			case U_TURN:
				return TurnIndication.REVERSE;
			default:
				// a keep is not a turn: on a road with nothing painted on it, staying in the lanes
				// that carry on is all it says
				return TurnIndication.THROUGH;
		}
	}

	/** the same lanes with nothing marked, ready to be matched against a new junction */
	private List<Lane> passive(List<Lane> lanes) {
		List<Lane> out = new ArrayList<>(lanes.size());
		for (Lane lane : lanes) {
			out.add(lane.withActive(false));
		}
		return out;
	}

	/** the arrows that lead where the route goes: the one it takes, or the maneuver's own family */
	private Set<TurnIndication> wanted(Maneuver maneuver, TurnIndication routeArrow) {
		if (routeArrow == null) {
			return indicationsFor(maneuver);
		}
		if (routeArrow == TurnIndication.THROUGH) {
			return STRAIGHT_INDICATIONS;
		}
		return EnumSet.of(routeArrow);
	}

	// ------------------------------------------------------------------ lanes

	/**
	 * The lane model of the road being left, in the driver's order.
	 *
	 * <p>Lanes are only built when the map actually marked them: a lane count alone says how many
	 * lanes there are, not what any of them is for, and inventing arrows from a count would put
	 * guesses on the screen.
	 */
	private List<Lane> lanesOf(RouteSegmentResult segment, boolean leftHand) {
		Map<String, String> tags = tagsOf(segment.getObject());
		// the map knows more about one-way than the tag does: a roundabout is one-way without
		// saying so, and an unsuffixed turn:lanes on a one-way road is the direction of travel
		int oneway = segment.getObject().getOneway();
		tags.put("oneway", oneway == 0 ? "no" : oneway > 0 ? "yes" : "-1");
		if (!hasTurnMarkings(tags)) {
			return Collections.emptyList();
		}
		return TurnTypeAI.parseLanes(tags, segment.isForwardDirection(), leftHand);
	}

	private boolean hasTurnMarkings(Map<String, String> tags) {
		for (LaneGroup g : LaneGroup.values()) {
			if (tags.containsKey("turn:lanes:" + g.suffix())) {
				return true;
			}
		}
		return tags.containsKey("turn:lanes");
	}

	/**
	 * Marks the lanes that lead where the route goes.
	 *
	 * <p>Three rules, in order:
	 * <ol>
	 * <li>a lane this traveller may not be in is not a candidate at all, whatever its arrow says:
	 *     a bus lane for a car, a cycle lane for anything but a bicycle, a lane the map closed;</li>
	 * <li>a candidate is a lane whose indications include one that matches the maneuver;</li>
	 * <li>when the candidates fall into separate runs with other lanes between them, the run made
	 *     FOR this traveller wins, and failing that the run on the side of the maneuver.</li>
	 * </ol>
	 * When nothing matches, every lane is left passive: showing no active lane is honest, showing
	 * the wrong one is not.
	 *
	 * <p>{@code change:lanes} deliberately plays no part here. A lane that leads where the route
	 * goes leads there whether or not a driver may move into it, and the restriction is about the
	 * lane somebody is in now, which this pass does not know. It is carried in the model for the
	 * route planner, which is where an unreachable exit has to be paid for.
	 */
	private List<Lane> markActive(List<Lane> lanes, Set<TurnIndication> wanted, TransportMode mode) {
		if (lanes.isEmpty()) {
			return lanes;
		}
		List<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < lanes.size(); i++) {
			Lane lane = lanes.get(i);
			if (!lane.isUsableBy(mode)) {
				continue;
			}
			for (TurnIndication t : arrowsOf(lane)) {
				if (wanted.contains(t)) {
					candidates.add(i);
					break;
				}
			}
		}
		if (candidates.isEmpty()) {
			return lanes;
		}
		List<Integer> run = pickRun(lanes, candidates, wanted, mode);
		List<Lane> out = new ArrayList<>(lanes.size());
		for (int i = 0; i < lanes.size(); i++) {
			Lane lane = lanes.get(i);
			if (!run.contains(i)) {
				out.add(lane.withActive(false));
				continue;
			}
			TurnIndication taken = null;
			for (TurnIndication arrow : lane.turns()) {
				if (wanted.contains(arrow)) {
					taken = arrow;
					break;
				}
			}
			out.add(taken != null ? lane.withTaken(taken) : lane.withActive(true));
		}
		return out;
	}

	/** the run made for this traveller, or failing that the one on the side the maneuver goes to */
	private List<Integer> pickRun(List<Lane> lanes, List<Integer> candidates, Set<TurnIndication> wanted,
			TransportMode mode) {
		List<List<Integer>> runs = new ArrayList<>();
		List<Integer> run = new ArrayList<>();
		for (int idx : candidates) {
			if (!run.isEmpty() && !adjacent(run.get(run.size() - 1), idx)) {
				runs.add(run);
				run = new ArrayList<>();
			}
			run.add(idx);
		}
		runs.add(run);
		// a cyclist belongs in the cycle lane and a bus in the bus lane, wherever it happens to be
		for (List<Integer> candidate : runs) {
			for (int idx : candidate) {
				if (lanes.get(idx).designatedFor() == mode) {
					return candidate;
				}
			}
		}
		boolean fromTheLeft = isLeftish(wanted);
		List<Integer> best = runs.get(0);
		for (List<Integer> candidate : runs) {
			if (fromTheLeft) {
				if (candidate.get(0) < best.get(0)) {
					best = candidate;
				}
			} else if (isRightish(wanted)) {
				if (candidate.get(candidate.size() - 1) > best.get(best.size() - 1)) {
					best = candidate;
				}
			} else if (candidate.size() > best.size()) {
				// straight on: the widest run of lanes that carry on is the one to be in
				best = candidate;
			}
		}
		return best;
	}

	private boolean adjacent(int left, int right) {
		return right == left + 1;
	}

	private Set<TurnIndication> indicationsFor(Maneuver maneuver) {
		switch (maneuver) {
			case TURN_LEFT:
				return EnumSet.of(TurnIndication.LEFT, TurnIndication.SHARP_LEFT);
			case SLIGHT_LEFT:
				return EnumSet.of(TurnIndication.SLIGHT_LEFT, TurnIndication.LEFT,
						TurnIndication.MERGE_TO_LEFT);
			case SHARP_LEFT:
				return EnumSet.of(TurnIndication.SHARP_LEFT, TurnIndication.LEFT);
			case TURN_RIGHT:
				return EnumSet.of(TurnIndication.RIGHT, TurnIndication.SHARP_RIGHT);
			case SLIGHT_RIGHT:
				return EnumSet.of(TurnIndication.SLIGHT_RIGHT, TurnIndication.RIGHT,
						TurnIndication.MERGE_TO_RIGHT);
			case SHARP_RIGHT:
				return EnumSet.of(TurnIndication.SHARP_RIGHT, TurnIndication.RIGHT);
			case U_TURN:
				return EnumSet.of(TurnIndication.REVERSE);
			case KEEP_LEFT:
				return EnumSet.of(TurnIndication.SLIGHT_LEFT, TurnIndication.MERGE_TO_LEFT,
						TurnIndication.THROUGH, TurnIndication.NONE);
			case KEEP_RIGHT:
				return EnumSet.of(TurnIndication.SLIGHT_RIGHT, TurnIndication.MERGE_TO_RIGHT,
						TurnIndication.THROUGH, TurnIndication.NONE);
			default:
				return STRAIGHT_INDICATIONS;
		}
	}

	/** the side the wanted arrows point to, so the run nearest that side can be preferred */
	private boolean isLeftish(Set<TurnIndication> wanted) {
		double sum = 0;
		for (TurnIndication arrow : wanted) {
			sum += usualAngle(arrow);
		}
		return sum > 0;
	}

	private boolean isRightish(Set<TurnIndication> wanted) {
		double sum = 0;
		for (TurnIndication arrow : wanted) {
			sum += usualAngle(arrow);
		}
		return sum < 0;
	}

	// ------------------------------------------------------------------ the junction around us

	// ------------------------------------------------------------------ roundabouts

	/**
	 * A roundabout is one maneuver, announced on entry and counted in exits. Every point of every
	 * roundabout segment that has a road attached is an exit, the one the route leaves by included.
	 */
	private TurnTypeAI roundabout(List<RouteSegmentResult> route, int i, boolean leftHand, TransportMode mode) {
		RouteSegmentResult prev = route.get(i - 1);
		int exit = 1;
		int last = i;
		for (int j = i; j < route.size(); j++) {
			RouteSegmentResult segment = route.get(j);
			if (!segment.getObject().roundabout()) {
				break;
			}
			last = j;
			boolean forward = segment.getStartPointIndex() < segment.getEndPointIndex();
			for (int k = segment.getStartPointIndex(); k != segment.getEndPointIndex(); k += forward ? 1 : -1) {
				if (k != segment.getStartPointIndex() && !segment.getAttachedRoutes(k).isEmpty()) {
					exit++;
				}
			}
		}
		RouteSegmentResult exitSegment = last + 1 < route.size() ? route.get(last + 1) : route.get(last);
		float angle = (float) MapUtils.degreesDiff(exitSegment.getBearingBegin(), bearingOut(prev));
		List<Lane> lanes = lanesOf(prev, leftHand);
		return new TurnTypeAI(Maneuver.ROUNDABOUT, leftHand, mode, exit, angle, false, lanes);
	}

	// ------------------------------------------------------------------ second pass

	/**
	 * One maneuver, however many segments it is made of. A U-turn is mapped as a slip road that
	 * leaves, bends and rejoins, so the road it rejoins bends the same way again a few metres
	 * later. That second bend is not a second instruction, it is the rest of the U-turn, and
	 * announcing a left turn in the middle of one is how a driver ends up in the wrong lane.
	 */
	private void dropContinuationOfTheSameTurn(List<RouteSegmentResult> route) {
		for (int i = 1; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null || !isRealTurn(turn.maneuver())) {
				continue;
			}
			double distance = 0;
			for (int j = i - 1; j >= 0 && distance <= SAME_MANEUVER_M; j--) {
				distance += route.get(j).getDistance();
				TurnTypeAI before = route.get(j).getTurnTypeAI();
				if (before == null) {
					continue;
				}
				if (before.maneuver() == Maneuver.U_TURN && sameSide(before.maneuver(), turn.maneuver())) {
					route.get(i).setTurnTypeAI(null);
				}
				break;
			}
		}
	}

	private boolean sameSide(Maneuver a, Maneuver b) {
		Set<TurnIndication> first = EnumSet.of(arrowOf(a));
		Set<TurnIndication> second = EnumSet.of(arrowOf(b));
		return (isLeftish(first) && isLeftish(second)) || (isRightish(first) && isRightish(second));
	}

	/**
	 * A U-turn across a median is mapped as two turns: left onto the link between the carriageways,
	 * then left again onto the other side. Two turns the same way within a few metres that add up
	 * to about half a circle are one U-turn, and it is announced at the first of them, where the
	 * driver still has to choose a lane.
	 */
	private void justifyUTurns(List<RouteSegmentResult> route, boolean leftHand) {
		for (int i = 1; i + 1 < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null || !isRealTurn(turn.maneuver()) || turn.maneuver() == Maneuver.U_TURN) {
				continue;
			}
			if (route.get(i).getDistance() >= SAME_MANEUVER_M) {
				continue; // a real road between the two turns, not the link across a median
			}
			int j = i + 1;
			TurnTypeAI next = route.get(j).getTurnTypeAI();
			if (next == null || !isRealTurn(next.maneuver()) || !sameSide(turn.maneuver(), next.maneuver())) {
				continue;
			}
			// how far the whole thing turns, measured from before the first bend to after the
			// second one: two turns of fifty degrees each are a U-turn, two of twenty are a bend
			double rotation = MapUtils.degreesDiff(bearingOut(route.get(i - 1)), bearingIn(route.get(j)));
			if (Math.abs(rotation) < U_TURN_ROTATION_DEG) {
				continue;
			}
			if (route.get(i - 1).getObject().getOneway() == 0 || route.get(j).getObject().getOneway() == 0) {
				continue; // on a two-way road a U-turn needs no link and no second turn
			}
			if (!sameName(route.get(i - 1), route.get(j))) {
				continue; // coming back onto the same road is what makes it a U-turn
			}
			route.get(i).setTurnTypeAI(turn.withManeuver(Maneuver.U_TURN)
					.withLanes(retarget(turn.lanes(), arrowOf(turn.maneuver()), TurnIndication.REVERSE)));
			route.get(j).setTurnTypeAI(next.withSkipToSpeak(true));
		}
	}

	/** an invented arrow follows the maneuver it was invented for; a mapped one is left alone */
	private List<Lane> retarget(List<Lane> lanes, TurnIndication from, TurnIndication to) {
		List<Lane> out = new ArrayList<>(lanes.size());
		for (Lane lane : lanes) {
			if (lane.isActive() && lane.turns().size() == 1 && lane.turns().get(0) == from) {
				out.add(new Lane.Builder().group(lane.group()).turns(Collections.singletonList(to))
						.taken(to).build());
			} else {
				out.add(lane);
			}
		}
		return out;
	}

	private boolean sameName(RouteSegmentResult a, RouteSegmentResult b) {
		String first = a.getObject().getName();
		String second = b.getObject().getName();
		return first == null ? second == null : first.equals(second);
	}

	/**
	 * A big interchange is many junctions in a row, and the markings on it describe all of them at
	 * once. Announcing the lanes again at every bend tells a driver the same thing three times and
	 * hides which of them mattered. Of a run of instructions that announce no turn, two are worth
	 * keeping: the first, where the lanes are still choosable, and the one right before a real
	 * turn, which is the last warning. The rest go.
	 */
	private void oneLaneInstructionPerInterchange(List<RouteSegmentResult> route) {
		TurnTypeAI shown = null;
		for (int i = 0; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null) {
				continue;
			}
			if (isRealTurn(turn.maneuver())) {
				shown = null;
				continue;
			}
			boolean repeat = shown != null && sameLanes(shown.lanes(), turn.lanes());
			if (repeat && !precedesTurn(route, i)) {
				route.get(i).setTurnTypeAI(null);
			} else {
				shown = turn;
			}
		}
	}

	/** the same lanes with the same ones marked: a picture a driver has already been shown */
	private boolean sameLanes(List<Lane> a, List<Lane> b) {
		if (a.size() != b.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (a.get(i).isActive() != b.get(i).isActive() || !a.get(i).turns().equals(b.get(i).turns())) {
				return false;
			}
		}
		return true;
	}

	/** the next instruction is a real turn, and it is the one this lane picture leads to */
	private boolean precedesTurn(List<RouteSegmentResult> route, int i) {
		double distance = 0;
		for (int j = i + 1; j < route.size() && distance <= LOOK_AHEAD_M; j++) {
			distance += route.get(j - 1).getDistance();
			TurnTypeAI next = route.get(j).getTurnTypeAI();
			if (next != null) {
				return isRealTurn(next.maneuver()) && distance <= LOOK_AHEAD_M;
			}
		}
		return false;
	}

	/**
	 * The approach to a turn belongs to the turn. A driver told "carry on" a few metres before a
	 * left turn has to be in the left lane already, so the lanes of an instruction that only says
	 * "carry on" are marked for the turn that follows, not for the straight-on that never was.
	 * The instruction itself stays as it is: what is said does not change, only what is drawn.
	 */
	private void lookAheadToTheNextTurn(List<RouteSegmentResult> route) {
		for (int i = 0; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null || turn.lanes().isEmpty() || isRealTurn(turn.maneuver())
					|| turn.lanesDecidedAhead()) {
				continue; // these lanes were already chosen knowing what comes next
			}
			TurnTypeAI ahead = null;
			double distance = 0;
			for (int j = i + 1; j < route.size() && distance <= LOOK_AHEAD_M; j++) {
				distance += route.get(j - 1).getDistance();
				TurnTypeAI next = route.get(j).getTurnTypeAI();
				if (next == null) {
					continue;
				}
				if (isRealTurn(next.maneuver())) {
					ahead = distance <= LOOK_AHEAD_M ? next : null;
					break; // the turn this approach leads to
				}
			}
			if (ahead != null && i > 0
					&& arrowBelongsToThisJunction(JunctionAI.at(route.get(i - 1), route.get(i)),
							arrowOf(ahead.maneuver()))) {
				continue; // that arrow leads to a road of THIS junction, not to the turn ahead
			}
			if (ahead != null) {
				Set<TurnIndication> wanted = wanted(ahead.maneuver(), null);
				route.get(i).setTurnTypeAI(turn.withLanes(
						markActive(passive(turn.lanes()), wanted, turn.traveller())).withSkipToSpeak(false));
			} else if (turn.maneuver() == Maneuver.CONTINUE && justTurned(route, i)) {
				// we have only just turned and nothing is coming: the lanes of the road we are
				// leaving have nothing left to say
				route.get(i).setTurnTypeAI(null);
			}
		}
	}

	/**
	 * Does an arrow of this road point at a road of THIS junction? If it does, it is that road's
	 * arrow and belongs to whoever takes it, so a turn further on must not borrow it: marking the
	 * slight-right lanes because the route turns slightly right later would send a driver down the
	 * slip road that those arrows are actually for.
	 */
	private boolean arrowBelongsToThisJunction(JunctionAI junction, TurnIndication arrow) {
		double side = usualAngle(arrow);
		if (side == 0) {
			return true;
		}
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && Math.abs(option.angle() - side) < ARROW_MATCHES_ROAD_DEG) {
				return true;
			}
		}
		return false;
	}

	/** a real turn was announced within the last few metres */
	private boolean justTurned(List<RouteSegmentResult> route, int i) {
		double distance = 0;
		for (int j = i - 1; j >= 0 && distance <= CARRY_LANES_M; j--) {
			distance += route.get(j).getDistance();
			TurnTypeAI before = route.get(j).getTurnTypeAI();
			if (before != null) {
				return isRealTurn(before.maneuver());
			}
		}
		return false;
	}

	private boolean isRealTurn(Maneuver maneuver) {
		return maneuver != Maneuver.CONTINUE && maneuver != Maneuver.KEEP_LEFT
				&& maneuver != Maneuver.KEEP_RIGHT && maneuver != Maneuver.ROUNDABOUT;
	}

	/**
	 * A keep whose every straight lane is already the lane to be in is not a keep: nobody is being
	 * asked to move over, the road simply carries on beside something that leaves it. The old
	 * preparation calls this avoidKeepForThroughMoving and it is the same idea.
	 */
	private void keepBecomesCarryOn(List<RouteSegmentResult> route) {
		for (int i = 1; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null || turn.lanes().isEmpty()
					|| (turn.maneuver() != Maneuver.KEEP_LEFT && turn.maneuver() != Maneuver.KEEP_RIGHT)) {
				continue;
			}
			JunctionAI junction = JunctionAI.at(route.get(i - 1), route.get(i));
			if (junction.routeLeavesMainRoad()) {
				continue; // leaving onto a slip road is a keep whatever the lanes say
			}
			if (worthKeeping(junction)) {
				continue; // a fork between roads of one rank is a choice, and a keep is the answer
			}
			int active = straightAndActive(turn.lanes());
			if (active > 0 && active == straightLanes(turn.lanes())) {
				route.get(i).setTurnTypeAI(turn.withManeuver(Maneuver.CONTINUE));
			}
		}
	}

	/**
	 * A carry-on that shows nothing worth showing. When every lane that goes straight is already a
	 * lane to be in, two or more of them are, and at most one lane does anything else, the picture
	 * says "the road carries on", which is what the road was doing anyway. Three or more different
	 * arrows are worth a look, so those stay.
	 */
	private void removeEmptyCarryOn(List<RouteSegmentResult> route) {
		for (int i = 1; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null || turn.maneuver() != Maneuver.CONTINUE || turn.lanes().isEmpty()) {
				continue;
			}
			if (JunctionAI.at(route.get(i - 1), route.get(i)).routeLeavesMainRoad()) {
				continue;
			}
			if (distinctArrows(turn.lanes()).size() >= 3) {
				continue;
			}
			int active = straightAndActive(turn.lanes());
			if (active >= 2 && active == straightLanes(turn.lanes()) && turn.lanes().size() - active <= 1
					&& !precedesTurn(route, i)) {
				route.get(i).setTurnTypeAI(null);
			}
		}
	}

	/**
	 * The road that leaves here is as big as ours, so telling a driver to keep to one side is worth
	 * saying. Beside a smaller road it is not: nobody mistakes a service road for the way on.
	 */
	private boolean worthKeeping(JunctionAI junction) {
		int mine = JunctionAI.rank(junction.routeOption().highway());
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && JunctionAI.rank(option.highway()) <= mine) {
				return true;
			}
		}
		return false;
	}

	/** lanes to be in whose own arrow is "carry on" */
	private int straightAndActive(List<Lane> lanes) {
		int count = 0;
		for (Lane lane : lanes) {
			if (lane.isActive() && (lane.taken() == null || normalize(lane.taken()) == TurnIndication.THROUGH)) {
				count++;
			}
		}
		return count;
	}

	/** lanes that carry a "carry on" arrow at all */
	private int straightLanes(List<Lane> lanes) {
		int count = 0;
		for (Lane lane : lanes) {
			for (TurnIndication arrow : arrowsOf(lane)) {
				if (normalize(arrow) == TurnIndication.THROUGH) {
					count++;
					break;
				}
			}
		}
		return count;
	}

	/**
	 * Two instructions for one junction is one instruction too many. A straight-on that sits within
	 * {@link #ONE_JUNCTION_M} of the next real maneuver describes the same place, so it is kept in
	 * the model and silenced rather than removed. One that carries lanes is never silenced: it is
	 * the warning that lets a driver change lane before the junction rather than on it.
	 */
	private void muteStraightsOfTheSameJunction(List<RouteSegmentResult> route) {
		double toNextTurn = Double.MAX_VALUE;
		for (int i = route.size() - 1; i >= 0; i--) {
			RouteSegmentResult segment = route.get(i);
			TurnTypeAI turn = segment.getTurnTypeAI();
			if (turn != null && turn.maneuver() != Maneuver.CONTINUE) {
				toNextTurn = 0;
			} else if (turn != null && turn.lanes().isEmpty() && toNextTurn <= ONE_JUNCTION_M
					&& !turn.skipToSpeak()) {
				segment.setTurnTypeAI(turn.withSkipToSpeak(true));
			}
			toNextTurn += segment.getDistance();
		}
	}

	// ------------------------------------------------------------------ plumbing

	private double bearingOut(RouteSegmentResult segment) {
		return segment.getBearingEnd(segment.getEndPointIndex(),
				Math.min(segment.getDistance(), RouteSegmentResult.DIST_BEARING_DETECT));
	}

	private double bearingIn(RouteSegmentResult segment) {
		return segment.getBearingBegin(segment.getStartPointIndex(),
				Math.min(segment.getDistance(), RouteSegmentResult.DIST_BEARING_DETECT));
	}

	private TurnIndication taggedTurn(RouteSegmentResult segment) {
		String value = segment.getObject().getValue("turn");
		if (value == null) {
			return null;
		}
		int sep = value.indexOf(';');
		return TurnIndication.parse(sep > 0 ? value.substring(0, sep) : value);
	}

	/** exactly the keys the lane model reads, asked of the road once */
	private Map<String, String> tagsOf(RouteDataObject road) {
		Map<String, String> tags = new LinkedHashMap<>();
		for (String key : TurnTypeAI.relevantTags()) {
			String value = road.getValue(key);
			if (value != null) {
				tags.put(key, value);
			}
		}
		return tags;
	}

	/** the routing profile, in the terms the OSM access hierarchy uses */
	private TransportMode modeOf(RoutingContext ctx) {
		GeneralRouterProfile profile = ctx == null || ctx.config == null || ctx.config.router == null
				? null : ctx.config.router.getProfile();
		if (profile == null) {
			return TransportMode.MOTORCAR;
		}
		switch (profile) {
			case BICYCLE:
				return TransportMode.BICYCLE;
			case PEDESTRIAN:
				return TransportMode.FOOT;
			case MOPED:
				return TransportMode.MOPED;
			case PUBLIC_TRANSPORT:
				return TransportMode.BUS;
			case CAR:
				return TransportMode.MOTORCAR;
			default:
				// a boat, a train, a horse: no lane has an opinion about them, so ask the widest
				// key there is and let an explicit access=no still be heard
				return TransportMode.ACCESS;
		}
	}
}
