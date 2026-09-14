package net.osmand.router.lanes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.binary.ObfConstants;
import net.osmand.binary.RouteDataObject;
import net.osmand.router.lanes.TurnTypeAI.Lane;
import net.osmand.router.lanes.TurnTypeAI.LaneGroup;
import net.osmand.router.lanes.TurnTypeAI.Maneuver;
import net.osmand.router.lanes.TurnTypeAI.TransportMode;
import net.osmand.router.lanes.TurnTypeAI.TurnIndication;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.router.GeneralRouter;
import net.osmand.util.MapUtils;

/**
 * Turn and lane preparation written on TurnTypeAI. The pass answers three questions per junction, in this
 * order, and never lets a later answer rewrite an earlier one: Is there a maneuver at all? Geometry
 * decides.
 */
public class TurnPrepareAI {

	private GeneralRouter router;

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
	/** what a lane change needs: the instruction taken in, the mirror, the blind spot, the move */
	private static final float LOOK_AHEAD_S = 8;
	/** the approach to a junction, which is a length of road and not a time */
	private static final float LOOK_AHEAD_M = 100;
	/** the speed a segment the router did not time is assumed to run at, which is the old 100 m */
	private static final float ASSUMED_SPEED = 100 / 6f;
	/** How close two instructions are before they are one place rather than two. */
	private static final float SAME_PLACE_M = 100;
	/** an unmarked road shorter than this keeps the lanes of the road before it */
	private static final float CARRY_LANES_M = 60;
	/** how far apart two instructions can be and still be one interchange, with no room between */
	private static final float INTERCHANGE_M = 400;
	/** a straight-on instruction this close to the next real turn belongs to the same junction */
	private static final float ONE_JUNCTION_M = 100;

	private static final Set<TurnIndication> STRAIGHT_INDICATIONS = EnumSet.of(
			TurnIndication.THROUGH, TurnIndication.NONE,
			TurnIndication.MERGE_TO_LEFT, TurnIndication.MERGE_TO_RIGHT);

	// ------------------------------------------------------------------ entry point

	public void prepareTurnResults(RoutingContext ctx, List<RouteSegmentResult> result) {
		this.router = ctx == null || ctx.config == null ? null : ctx.config.router;
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
		narrowToTheLaneThatLeadsOn(result);
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
		// the road we are leaving carries on past this junction as one of its own branches, and the markings are
		// painted on it: they say what its lanes do further along, at the junction it reaches, not what this one
		// offers the route leaving it here.
		boolean carriesOn = marksTheRoadThatCarriesOn(junction, prev);
		List<Lane> lanes = carriesOn ? Collections.<Lane>emptyList() : lanesOf(prev, leftHand);
		boolean ownMarkings = !lanes.isEmpty();
		double carried = prev.getDistance();
		for (int j = i - 2; !carriesOn && lanes.isEmpty() && j >= 0 && carried < CARRY_LANES_M; j--) {
			// a few metres of unmarked road between two junctions is the same carriageway: the lanes painted before
			// it still say which lane leads where
			List<Lane> painted = lanesOf(route.get(j), leftHand);
			if (!painted.isEmpty() && !fitsTheRoad(painted, prev)) {
				// unless the road being driven says it has a different number of lanes, and then the carriageway plainly
				// is not the same one: markings for four lanes on a road that says it has two describe somewhere the
				// driver has already left
				break;
			}
			lanes = painted;
			carried += route.get(j).getDistance();
		}
		double decisive = decisiveAngle(route, i, junction, lanes, leftHand);
		boolean decidedAhead = decisive != junction.routeAngle();
		TurnIndication routeArrow = lanes.isEmpty() ? null
				: routeArrow(junction, lanes, decisive, taggedTurn(current));

		Maneuver maneuver = maneuverOf(leftDeg, taggedTurn(current), leftHand);
		boolean choice = junction.isChoice() || distinctArrows(lanes).size() > 1;
		if (maneuver == null && routeArrow != null && choice) {
			// geometry is silent but the road is marked and there is a choice: the arrow the route takes is the
			// instruction, which is how a driver reads a junction like this
			maneuver = maneuverOfIndication(routeArrow, leftHand);
			if (maneuver == Maneuver.CONTINUE) {
				Maneuver keep = keepOf(junction);
				maneuver = keep != null ? keep : distinctArrows(lanes).size() > 1 ? Maneuver.CONTINUE : null;
			}
		}
		if (maneuver == null) {
			maneuver = keepOf(junction);
		}
		if (maneuver == null && junction.optionCount() > 2) {
			// the ways part to both sides at once, so the route keeps to neither: it takes the middle one, and what
			// that is called is the same question the lanes answer - the roads in their order, each one further out
			// than the one inside it.
			maneuver = maneuverOfIndication(spreadArrows(junction).get(junction.routeRank()), leftHand);
		}
		if (maneuver == null) {
			return null; // straight on through a junction that does not fork: not an instruction
		}
		if (maneuver == Maneuver.CONTINUE && !ownMarkings) {
			// borrowed markings are there to serve the junction they were painted for; repeating them at the next
			// one, with nothing to announce, is how one junction becomes three
			return null;
		}
		if (lanes.isEmpty()) {
			lanes = countedLanes(prev, junction);
		}
		boolean mapped = hasArrows(lanes);
		// an arrow read out of markings that do not describe this junction is a guess; the maneuver the geometry
		// measured is not.
		boolean describes = describesThisJunction(junction, distinctArrows(lanes));
		TurnIndication says = describes || !isRealTurn(maneuver) ? routeArrow : arrowOf(maneuver);
		lanes = mapped ? markActive(lanes, wanted(maneuver, says), mode)
				: markBySide(lanes, maneuver, junction);
		if (mapped && isRealTurn(maneuver) && !anyActive(lanes)) {
			lanes = fromTheEdge(lanes, maneuver, mode);
		}
		// only arrows the map painted may correct the maneuver; ones we painted ourselves came from the maneuver
		// in the first place and would just be read back
		maneuver = mapped ? refineByLanes(maneuver, lanes) : maneuver;
		// a carry-on with lanes to show is the warning a driver needs in time to change lane, so it speaks; a
		// carry-on with nothing to show has nothing to say
		boolean skipToSpeak = maneuver == Maneuver.CONTINUE && lanes.isEmpty();
		return new TurnTypeAI(maneuver, leftHand, mode, 0, (float) -leftDeg, skipToSpeak, lanes)
				.decidedAhead(decidedAhead);
	}

	/** The maneuver as geometry sees it, or null for straight on. */
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
			// a U-turn is made towards the centre line, which is on the driver's left where people drive on the
			// right; the other way round it is just a very sharp turn
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
	 * A keep instruction is about a FORK: another road leaving beside the route at a small angle, which a
	 * driver could take by mistake.
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

	/** Which arrow of the marked road the route takes. */
	/** How far the route turns by the time the marked choice is actually made. */
	private double decisiveAngle(List<RouteSegmentResult> route, int i, JunctionAI junction,
			List<Lane> lanes, boolean leftHand) {
		List<TurnIndication> arrows = distinctArrows(lanes);
		// two signs that the arrows are not about this junction: they offer more kinds than there are roads here,
		// or they offer no way of carrying straight on while the road does exactly that - left or right, said on a
		// road that runs straight, is said about what comes next
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

	private TurnIndication routeArrow(JunctionAI junction, List<Lane> lanes, double angle,
			TurnIndication tagged) {
		List<TurnIndication> arrows = distinctArrows(lanes);
		if (arrows.isEmpty()) {
			return null;
		}
		if (tagged != null && !STRAIGHT_INDICATIONS.contains(tagged) && arrows.contains(tagged)) {
			// the road being entered states a direction, and the markings before it offer that very arrow: a
			// statement outranks a measurement here as much as it does in the maneuver.
			return tagged;
		}
		if (arrows.size() == 1) {
			return arrows.get(0);
		}
		// the exit relaxation is about the geometry of THIS junction; when the arrows look past it the angle
		// already carries the whole picture and needs no help
		boolean leaving = junction.routeLeavesMainRoad() && angle == junction.routeAngle();
		if (describesThisJunction(junction, arrows)) {
			// one arrow kind per road, and every road on the side its arrow points to: the mapper painted this
			// junction road by road, so the k-th road from the left takes the k-th kind from the left.
			return arrows.get(junction.routeRank());
		}
		if (junction.optionCount() > 1) {
			// the counts do not line up, but the order still does: the leftmost road is what the leftmost arrow
			// points at, the rightmost road what the rightmost arrow points at
			int index = (int) Math.round(junction.routeRank() * (arrows.size() - 1.0)
					/ (junction.optionCount() - 1.0));
			TurnIndication byOrder = arrows.get(Math.max(0, Math.min(arrows.size() - 1, index)));
			if (plausible(byOrder, angle) || (leaving && gentle(byOrder)
					&& onTheRouteSide(byOrder, junction, angle))) {
				return byOrder;
			}
		}
		// a road that bends noticeably is taking one of the turning arrows, not the straight one: junctions are
		// drawn gentler than the arrows painted on them an exit bends gently, so it counts as bending only when
		// there is a gentle arrow to take; "left or through" on a road that runs almost straight is not an exit's
		// pair of arrows
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

	/** Whether the markings are about the junction under the wheels. */
	private boolean describesThisJunction(JunctionAI junction, List<TurnIndication> arrows) {
		List<JunctionAI.Option> options = junction.options(); // already left to right
		if (options.size() != arrows.size()) {
			return false;
		}
		for (int k = 0; k < options.size(); k++) {
			double angle = options.get(k).angle();
			double side = usualAngle(arrows.get(k));
			boolean fits = side == 0 ? Math.abs(angle) < BENDS_DEG : side * angle > 0;
			if (!fits) {
				return false;
			}
		}
		return true;
	}

	/** Counting the roads and counting the arrows can agree by accident. */
	private boolean plausible(TurnIndication arrow, double routeAngle) {
		if (arrow == TurnIndication.THROUGH) {
			return true;
		}
		// the road has to bend at least halfway towards what the arrow promises: a twelve degree bend is not the
		// "left" of left-or-through, it is the "through"
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
	 * The arrow points the way the route leaves relative to the roads it leaves beside: an exit that bends
	 * five degrees is still the left one of two when the road it leaves runs to the right of it.
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
	 * A lane with nothing painted on it is not a lane without an answer: {@code turn:lanes=left|} says the
	 * left lane turns and the other one carries on, which is the whole point of writing the empty half.
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

	/** A road that says how many lanes it has but not what they are for. */
	private List<Lane> countedLanes(RouteSegmentResult segment, JunctionAI junction) {
		int count = laneCount(segment);
		if (count < 2 && !junction.isChoice()) {
			// one lane is not a choice worth drawing - unless another road leaves this junction, and then that one
			// lane is where both of them start, which is the whole choice there is to show.
			return Collections.emptyList();
		}
		count = Math.max(1, count);
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
	 * Unmarked lanes, marked by where the route goes: a turn is made from the lanes on that side, as many of
	 * them as the road being entered has.
	 */
	private List<Lane> markBySide(List<Lane> lanes, Maneuver maneuver, JunctionAI junction) {
		if (lanes.isEmpty()) {
			return lanes;
		}
		int count = lanes.size();
		if (count == 1 && junction.optionCount() > 2) {
			return new ArrayList<>(Collections.singletonList(theOneLane(lanes.get(0), junction)));
		}
		TurnIndication arrow = arrowOf(maneuver);
		// only an instruction that does not name a direction of its own can take one from the lanes
		boolean keeping = maneuver == Maneuver.KEEP_LEFT || maneuver == Maneuver.KEEP_RIGHT
				|| maneuver == Maneuver.CONTINUE;
		if ((maneuver == Maneuver.KEEP_LEFT || maneuver == Maneuver.KEEP_RIGHT)
				&& Math.abs(junction.routeAngle()) < BENDS_DEG && !junction.routeLeavesMainRoad()
				&& !partsBothWays(junction)) {
			// a keep on a road that neither bends nor leaves paints nothing: the lanes carry on, and an arrow to the
			// side would tell a driver to move over for no reason.
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
		if (arrow != TurnIndication.THROUGH && other != null && junction.routeLeavesMainRoad()
				&& width + Math.min(count, laneCount(other.segment())) - count > 1) {
			// the two branches have to fit side by side in the carriageway they leave, sharing at most the one lane
			// between them.
			width = 1;
		}
		int from = routeOnTheLeft ? 0 : count - width;
		int to = from + width - 1;

		// the other branch takes its own lanes from the same carriageway, and when the two do not fit side by side
		// the lane between them feeds both: that is where a driver has a choice
		int otherFrom = count;
		int otherTo = -1;
		TurnIndication otherArrow = TurnIndication.THROUGH;
		if (other != null) {
			// and it fits beside the route, sharing at most the one lane between them
			int otherWidth = Math.max(1, Math.min(Math.min(count, count - width + 1),
					laneCount(other.segment())));
			otherArrow = arrowForAngle(other.angle());
			if (keeping && width + otherWidth <= count && gentle(otherArrow)) {
				// The lanes divide between the two branches, so the picture is about which of them carries on and which
				// bears off.
				TurnIndication routeSide = routeOnTheLeft
						? TurnIndication.SLIGHT_LEFT : TurnIndication.SLIGHT_RIGHT;
				TurnIndication otherSide = routeOnTheLeft
						? TurnIndication.SLIGHT_RIGHT : TurnIndication.SLIGHT_LEFT;
				boolean routeCarriesOn = width > otherWidth;
				boolean otherCarriesOn = otherWidth > width;
				if (width == otherWidth && junction.routeAngle() * other.angle() > 0) {
					// the same side of straight: the straighter of the two is the road going on
					routeCarriesOn = Math.abs(junction.routeAngle()) < Math.abs(other.angle());
					otherCarriesOn = !routeCarriesOn;
				}
				arrow = routeCarriesOn ? TurnIndication.THROUGH : routeSide;
				otherArrow = otherCarriesOn ? TurnIndication.THROUGH : otherSide;
			} else if (keeping && otherArrow == TurnIndication.THROUGH
					&& !junction.routeLeavesMainRoad()) {
				// the branch leaves too gently for its angle to name an arrow, so its side names one: these lanes lead
				// off to one side of where the route goes, and a panel that draws them straight says they carry on,
				// which is the one thing they do not do.
				otherArrow = routeOnTheLeft ? TurnIndication.SLIGHT_RIGHT : TurnIndication.SLIGHT_LEFT;
			}
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
				if (active && arrow == TurnIndication.THROUGH) {
					// the route carries on through a lane the other branch also leaves from: the lane does both, and
					// drawing only the branch says this lane has to turn off
					arrows.add(arrow);
				}
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

	/** The single lane a whole junction leaves from. */
	private Lane theOneLane(Lane lane, JunctionAI junction) {
		List<TurnIndication> painted = spreadArrows(junction);
		return new Lane.Builder().group(lane.group()).turns(painted).build()
				.withTaken(painted.get(junction.routeRank()));
	}

	/** an arrow for each road of the junction, told apart by the order the roads are in */
	private List<TurnIndication> spreadArrows(JunctionAI junction) {
		List<JunctionAI.Option> options = junction.options(); // left to right
		TurnIndication[] painted = new TurnIndication[options.size()];
		int straightest = 0;
		for (int i = 1; i < options.size(); i++) {
			if (Math.abs(options.get(i).angle()) < Math.abs(options.get(straightest).angle())) {
				straightest = i;
			}
		}
		painted[straightest] = arrowForAngle(options.get(straightest).angle());
		for (int i = straightest - 1; i >= 0; i--) {
			painted[i] = furtherOut(arrowForAngle(options.get(i).angle()), painted[i + 1], true);
		}
		for (int i = straightest + 1; i < options.size(); i++) {
			painted[i] = furtherOut(arrowForAngle(options.get(i).angle()), painted[i - 1], false);
		}
		return Arrays.asList(painted);
	}

	/** the arrow, or the next one out from the neighbour's if it does not already lie beyond it */
	private TurnIndication furtherOut(TurnIndication arrow, TurnIndication inner, boolean left) {
		return Math.abs(usualAngle(arrow)) > Math.abs(usualAngle(inner)) ? arrow : oneStepOut(inner, left);
	}

	private TurnIndication oneStepOut(TurnIndication arrow, boolean left) {
		double out = Math.abs(usualAngle(arrow));
		if (out < Math.abs(usualAngle(TurnIndication.SLIGHT_LEFT))) {
			return left ? TurnIndication.SLIGHT_LEFT : TurnIndication.SLIGHT_RIGHT;
		}
		if (out < Math.abs(usualAngle(TurnIndication.LEFT))) {
			return left ? TurnIndication.LEFT : TurnIndication.RIGHT;
		}
		return left ? TurnIndication.SHARP_LEFT : TurnIndication.SHARP_RIGHT;
	}

	/** The lane a turn is taken from when the markings mark no lane for it. */
	private List<Lane> fromTheEdge(List<Lane> lanes, Maneuver maneuver, TransportMode mode) {
		double side = usualAngle(arrowOf(maneuver));
		if (side == 0 || lanes.isEmpty()) {
			return lanes;
		}
		int keep = -1;
		for (int i = 0; i < lanes.size(); i++) {
			if (!lanes.get(i).isUsableBy(mode)) {
				continue;
			}
			keep = i;
			if (side > 0) {
				break; // a left turn from the leftmost lane, a right one from the last
			}
		}
		if (keep < 0) {
			return lanes;
		}
		List<Lane> out = new ArrayList<>(lanes.size());
		for (int i = 0; i < lanes.size(); i++) {
			out.add(i == keep ? lanes.get(i).withActive(true) : lanes.get(i));
		}
		return out;
	}

	/** do the two branches leave to opposite sides of straight, so that neither carries on? */
	private boolean partsBothWays(JunctionAI junction) {
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && junction.routeAngle() * option.angle() < 0
					&& Math.abs(junction.routeAngle()) >= TURN_HINT_DEG
					&& Math.abs(option.angle()) >= TURN_HINT_DEG) {
				return true;
			}
		}
		return false;
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
				// a keep is not a turn: on a road with nothing painted on it, staying in the lanes that carry on is all
				// it says
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

	/** The lane model of the road being left, in the driver's order. */
	private List<Lane> lanesOf(RouteSegmentResult segment, boolean leftHand) {
		Map<String, String> tags = tagsOf(segment.getObject());
		// the map knows more about one-way than the tag does: a roundabout is one-way without saying so, and an
		// unsuffixed turn:lanes on a one-way road is the direction of travel
		int oneway = segment.getObject().getOneway();
		tags.put("oneway", oneway == 0 ? "no" : oneway > 0 ? "yes" : "-1");
		if (!hasTurnMarkings(tags)) {
			return Collections.emptyList();
		}
		return TurnTypeAI.parseLanes(tags, segment.isForwardDirection(), leftHand);
	}

	/** whether markings painted elsewhere can be about this road: a road of its own width */
	private boolean fitsTheRoad(List<Lane> painted, RouteSegmentResult segment) {
		int here = laneCount(segment);
		return here <= 0 || here == painted.size();
	}

	/** is the road being left one of the roads leaving this junction, carrying on past it? */
	private boolean marksTheRoadThatCarriesOn(JunctionAI junction, RouteSegmentResult prev) {
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && ObfConstants.getOsmObjectId(option.segment().getObject())
					== ObfConstants.getOsmObjectId(prev.getObject())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasTurnMarkings(Map<String, String> tags) {
		for (LaneGroup g : LaneGroup.values()) {
			if (tags.containsKey("turn:lanes:" + g.suffix())) {
				return true;
			}
		}
		return tags.containsKey("turn:lanes");
	}

	/** Marks the lanes that lead where the route goes. */
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

	/** A roundabout is one maneuver, announced on entry and counted in exits. */
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

	/** One maneuver, however many segments it is made of. */
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
	 * A U-turn across a median is mapped as two turns: left onto the link between the carriageways, then left
	 * again onto the other side.
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
			// how far the whole thing turns, measured from before the first bend to after the second one: two turns
			// of fifty degrees each are a U-turn, two of twenty are a bend
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

	/** A big interchange is many junctions in a row, and the markings on it describe all of them at once. */
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
		for (int j = i + 1; j < route.size() && distance <= SAME_PLACE_M; j++) {
			distance += route.get(j - 1).getDistance();
			TurnTypeAI next = route.get(j).getTurnTypeAI();
			if (next != null) {
				return isRealTurn(next.maneuver()) && distance <= SAME_PLACE_M;
			}
		}
		return false;
	}

	/** The approach to a turn belongs to the turn. */
	private void lookAheadToTheNextTurn(List<RouteSegmentResult> route) {
		// read from the end back, so that an instruction aimed at the turn can aim the one before it in turn.
		Maneuver aim = null;
		int aimedAt = -1;
		for (int i = route.size() - 1; i >= 0; i--) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn == null) {
				continue;
			}
			if (isRealTurn(turn.maneuver())) {
				aim = turn.maneuver(); // the turn the lanes behind it lead to
				aimedAt = i;
				continue;
			}
			if (turn.lanes().isEmpty() || turn.lanesDecidedAhead()) {
				continue; // these lanes were already chosen knowing what comes next
			}
			double distance = 0;
			for (int j = i; aimedAt > i && j < aimedAt; j++) {
				distance += route.get(j).getDistance();
			}
			boolean within = aim != null && distance <= reachFrom(route.get(i));
			if (within && i > 0 && arrowBelongsToThisJunction(
					JunctionAI.at(route.get(i - 1), route.get(i)), arrowOf(aim), turn.lanes())) {
				continue; // that arrow leads to a road of THIS junction, not to the turn ahead
			}
			if (within) {
				List<Lane> aimed = markActive(passive(turn.lanes()), wanted(aim, null), turn.traveller());
				if (!anyActive(aimed)) {
					// nothing here leads to that turn, or nothing that these arrows admit to: the turn ahead has no lane of
					// its own to point at.
					continue;
				}
				// the lanes moved, so the instruction has to move with them: a keep left whose only active lane is the
				// one bearing right is two instructions at once, and the lanes are the half that was just measured
				// against the road ahead
				Maneuver said = refineByLanes(turn.maneuver(), aimed);
				route.get(i).setTurnTypeAI(turn.withLanes(aimed).withManeuver(said).withSkipToSpeak(false));
				aimedAt = i; // and this instruction now aims the one before it
			} else if (turn.maneuver() == Maneuver.CONTINUE && justTurned(route, i)) {
				// we have only just turned and nothing is coming: the lanes of the road we are leaving have nothing left
				// to say
				route.get(i).setTurnTypeAI(null);
			}
		}
	}

	private boolean anyActive(List<Lane> lanes) {
		for (Lane lane : lanes) {
			if (lane.isActive()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does an arrow of this road point at a road of THIS junction? If it does, it is that road's arrow and
	 * belongs to whoever takes it, so a turn further on must not borrow it: marking the slight-right lanes
	 * because the route turns slightly right later would send a driver down the slip road that those arrows
	 * are actually for.
	 */
	private boolean arrowBelongsToThisJunction(JunctionAI junction, TurnIndication arrow,
			List<Lane> lanes) {
		double side = usualAngle(arrow);
		if (side == 0) {
			return true;
		}
		int roads = 0;
		for (JunctionAI.Option option : junction.options()) {
			if (!option.isRoute() && Math.abs(option.angle() - side) < ARROW_MATCHES_ROAD_DEG) {
				roads++;
			}
		}
		if (roads == 0) {
			return false;
		}
		// unless the markings offer that side more arrows than this junction has roads to take them.
		int arrows = 0;
		for (TurnIndication painted : distinctArrows(lanes)) {
			if (usualAngle(painted) != 0 && usualAngle(painted) * side > 0) {
				arrows++;
			}
		}
		return arrows <= roads;
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
	 * A keep whose every straight lane is already the lane to be in is not a keep: nobody is being asked to
	 * move over, the road simply carries on beside something that leaves it.
	 */
	/** Within one interchange, the lane to be in now is the lane that leads to the lane needed next. */
	private void narrowToTheLaneThatLeadsOn(List<RouteSegmentResult> route) {
		List<Integer> spoken = new ArrayList<>();
		for (int i = 0; i < route.size(); i++) {
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			if (turn != null && !turn.lanes().isEmpty()) {
				spoken.add(i);
			}
		}
		for (int k = spoken.size() - 2; k >= 0; k--) {
			int i = spoken.get(k);
			int j = spoken.get(k + 1);
			double distance = 0;
			for (int m = i; m < j; m++) {
				distance += route.get(m).getDistance();
			}
			if (distance > INTERCHANGE_M) {
				continue; // far enough apart to be two junctions, with room to change lane between
			}
			TurnTypeAI turn = route.get(i).getTurnTypeAI();
			int side = sideOfActiveLanes(route.get(j).getTurnTypeAI().lanes());
			if (side == 0 || side == sideOfActiveLanes(turn.lanes())) {
				// nothing to cross: the lanes to be in next are on the side these lanes already are, so being in any of
				// them still leads there.
				continue;
			}
			List<Lane> narrowed = narrow(turn.lanes(), side);
			if (narrowed != null) {
				route.get(i).setTurnTypeAI(turn.withLanes(narrowed));
			}
		}
	}

	/** which end of its road the active lanes sit at: -1 the left, 1 the right, 0 neither */
	private int sideOfActiveLanes(List<Lane> lanes) {
		int first = -1;
		int last = -1;
		for (int i = 0; i < lanes.size(); i++) {
			if (lanes.get(i).isActive()) {
				first = first < 0 ? i : first;
				last = i;
			}
		}
		if (first < 0 || (first == 0 && last == lanes.size() - 1)) {
			return 0; // nothing active, or the whole road: no side to be on
		}
		double middle = (lanes.size() - 1) / 2.0;
		double centre = (first + last) / 2.0;
		return centre == middle ? 0 : centre < middle ? -1 : 1;
	}

	/** the one active lane at the given end, the rest left passive, or null if there is nothing to do */
	private List<Lane> narrow(List<Lane> lanes, int side) {
		int keep = -1;
		int active = 0;
		for (int i = 0; i < lanes.size(); i++) {
			if (lanes.get(i).isActive()) {
				active++;
				keep = keep < 0 || side > 0 ? i : keep;
			}
		}
		if (active < 2) {
			return null;
		}
		List<Lane> out = new ArrayList<>(lanes.size());
		for (int i = 0; i < lanes.size(); i++) {
			if (i != keep) {
				out.add(lanes.get(i).withActive(false));
				continue;
			}
			// which of that lane's arrows is being taken is left alone: a lane at the join of two branches carries
			// both, and nothing here says the route has started onto one of them
			out.add(lanes.get(i));
		}
		return out;
	}

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

	/** A carry-on that shows nothing worth showing. */
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

	/** The road that leaves here is as big as ours, so telling a driver to keep to one side is worth saying. */
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

	/** Two instructions for one junction is one instruction too many. */
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

	/** How far ahead a turn still decides which lane to be in now, on this road. */
	private double reachFrom(RouteSegmentResult segment) {
		return Math.max(LOOK_AHEAD_M, speedOf(segment) * LOOK_AHEAD_S);
	}

	private double speedOf(RouteSegmentResult segment) {
		if (router != null) {
			// the speed the profile gives this road.
			float profile = router.defineVehicleSpeed(segment.getObject(), segment.isForwardDirection());
			if (profile > 0) {
				return profile;
			}
		}
		float speed = segment.getSegmentSpeed();
		return speed > 0 ? speed : ASSUMED_SPEED;
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
				// a boat, a train, a horse: no lane has an opinion about them, so ask the widest key there is and let an
				// explicit access=no still be heard
				return TransportMode.ACCESS;
		}
	}
}
