package net.osmand.router.lanes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

/**
 * What a junction offers, read before anything is decided about the maneuver.
 *
 * <p>A driver at a junction does not measure degrees, they count choices: these roads leave here,
 * mine is the second from the left, and the arrows painted on my lane say which one each lane
 * leads to. This class is the first half of that - the options and where the route sits among
 * them. {@link TurnPrepareAI} does the second half.
 *
 * <p>Angles are degrees away from the road being left, positive to the left, so the options sort
 * from left to right by descending angle.
 */
public final class JunctionAI {

	/** the road we came in on, seen among the roads that leave */
	private static final double BACKWARD_MIN_DEG = 165;
	/** a road leaving at less than this is a fork rather than a crossing */
	static final double FORK_MAX_DEG = 45;

	/** one road leaving the junction */
	public static final class Option {
		final double angle;
		final boolean route;
		final String highway;
		final RouteSegmentResult segment;

		Option(double angle, boolean route, String highway, RouteSegmentResult segment) {
			this.angle = angle;
			this.route = route;
			this.highway = highway;
			this.segment = segment;
		}

		/** the road itself, so a caller can ask it how many lanes it has */
		public RouteSegmentResult segment() {
			return segment;
		}

		public double angle() {
			return angle;
		}

		public boolean isRoute() {
			return route;
		}

		public String highway() {
			return highway;
		}
	}

	private final List<Option> options;
	private final double routeAngle;
	private final int routeRank;
	private final int forkLeft;
	private final int forkRight;

	private JunctionAI(List<Option> options, double routeAngle, int routeRank, int forkLeft, int forkRight) {
		this.options = Collections.unmodifiableList(options);
		this.routeAngle = routeAngle;
		this.routeRank = routeRank;
		this.forkLeft = forkLeft;
		this.forkRight = forkRight;
	}

	/**
	 * Reads the junction between two segments of the route. Roads that only lead back where we came
	 * from are dropped, and so is a road so much smaller than the one we are on that no driver would
	 * mistake it for a choice: a driveway beside a trunk road is not a fork.
	 */
	public static JunctionAI at(RouteSegmentResult prev, RouteSegmentResult current) {
		double out = prev.getBearingEnd(prev.getEndPointIndex(),
				Math.min(prev.getDistance(), RouteSegmentResult.DIST_BEARING_DETECT));
		double routeAngle = MapUtils.degreesDiff(out, current.getBearingBegin(current.getStartPointIndex(),
				Math.min(current.getDistance(), RouteSegmentResult.DIST_BEARING_DETECT)));
		String routeHighway = current.getObject().getHighway();
		String fromHighway = prev.getObject().getHighway();
		int mine = Math.max(rank(routeHighway), rank(fromHighway));

		List<Option> options = new ArrayList<>();
		options.add(new Option(routeAngle, true, routeHighway, current));
		List<RouteSegmentResult> attached = current.getAttachedRoutes(current.getStartPointIndex());
		if (attached != null) {
			for (RouteSegmentResult road : attached) {
				double angle = MapUtils.degreesDiff(out, road.getBearingBegin());
				if (Math.abs(angle) > BACKWARD_MIN_DEG || Math.abs(angle - routeAngle) < 1) {
					continue; // the road we arrived on, or the route itself listed again
				}
				String highway = road.getObject().getHighway();
				if (rank(highway) > mine + 1) {
					continue; // too small to be one of the choices
				}
				options.add(new Option(angle, false, highway, road));
			}
		}
		Collections.sort(options, new Comparator<Option>() {
			@Override
			public int compare(Option a, Option b) {
				return Double.compare(b.angle, a.angle); // left first
			}
		});
		int rank = 0;
		int forkLeft = 0;
		int forkRight = 0;
		for (int i = 0; i < options.size(); i++) {
			Option option = options.get(i);
			if (option.route) {
				rank = i;
			} else if (Math.abs(option.angle) < FORK_MAX_DEG) {
				if (option.angle > routeAngle) {
					forkLeft++;
				} else {
					forkRight++;
				}
			}
		}
		return new JunctionAI(options, routeAngle, rank, forkLeft, forkRight);
	}

	/** the route's own option, which knows the road it leads onto */
	public Option routeOption() {
		for (Option option : options) {
			if (option.isRoute()) {
				return option;
			}
		}
		return options.get(0);
	}

	/** every road that leaves, left to right, the route among them */
	public List<Option> options() {
		return options;
	}

	/** degrees the route turns, positive to the left */
	public double routeAngle() {
		return routeAngle;
	}

	/** how many options are to the left of the route, which is also the route's index */
	public int routeRank() {
		return routeRank;
	}

	public int optionCount() {
		return options.size();
	}

	/** the junction offers a choice: staying on the route is a decision, not the only way on */
	public boolean isChoice() {
		return options.size() > 1;
	}

	/**
	 * The route leaves the main road onto a slip road while another road carries it on. An exit is
	 * drawn as a gentle bend, sometimes of a few degrees, but it is a turn: the lane that leads to
	 * it is marked with an arrow, and the route takes that arrow however small the angle is.
	 */
	public boolean routeLeavesMainRoad() {
		boolean routeOnLink = false;
		boolean otherOnRoad = false;
		for (Option option : options) {
			if (option.isRoute()) {
				routeOnLink = isLink(option.highway);
			} else if (!isLink(option.highway)) {
				otherOnRoad = true;
			}
		}
		return routeOnLink && otherOnRoad;
	}

	private static boolean isLink(String highway) {
		return highway != null && highway.endsWith("_link");
	}

	/** roads leaving beside the route at a small angle, which is what a keep instruction is about */
	public int forkLeft() {
		return forkLeft;
	}

	public int forkRight() {
		return forkRight;
	}

	/**
	 * How big a road is, smaller number for bigger road. Used only to drop the choices nobody
	 * would count as choices.
	 */
	public static int rank(String highway) {
		if (highway == null) {
			return 5;
		}
		if (highway.startsWith("motorway") || highway.startsWith("trunk")) {
			return 0;
		}
		if (highway.startsWith("primary")) {
			return 1;
		}
		if (highway.startsWith("secondary")) {
			return 2;
		}
		if (highway.startsWith("tertiary")) {
			return 3;
		}
		if (highway.equals("unclassified") || highway.equals("residential") || highway.equals("living_street")
				|| highway.equals("road")) {
			return 4;
		}
		return 5;
	}
}
