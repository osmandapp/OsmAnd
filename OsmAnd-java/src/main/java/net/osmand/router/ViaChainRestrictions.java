package net.osmand.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

// Restrictions whose via is a chain of ways (#12537). The search keeps its position inside such restrictions as a state
// of the route segment. A segment with a state is a separate node of the search (own visited key), so a path that came
// from "from" and a path that entered the same via ways from a side road don't merge.
// Step i of a chain means that the segment is on via i and:
// - forward search: the path came along from, via1 .. via i;
// - backward search: the rest of the path is the restricted maneuver (no_*: via i+1 .. viaN, to;
//   only_*: via i+1 .. and then a road that leaves the chain), so "from" may not precede via1.
public class ViaChainRestrictions {

	public static final ViaChainState FORBIDDEN = new ViaChainState(new long[0], -1);

	public static class ViaChainState {
		final long[] steps; // chain index << STEP_BITS | step, sorted
		final int key; // small unique id of the state, part of the visited key of the segment

		private ViaChainState(long[] steps, int key) {
			this.steps = steps;
			this.key = key;
		}
	}

	private static class ViaChain {
		final int index;
		final long from;
		final long[] vias;
		final long to;
		final boolean onlyRestriction;

		ViaChain(int index, long from, long[] vias, long to, int type) {
			this.index = index;
			this.from = from;
			this.vias = vias;
			this.to = to;
			this.onlyRestriction = type >= MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN;
		}
	}

	private static final int STEP_BITS = 6;
	// ponytail: 9 bits of the visited key, enough for the chains around one route; widen the key if it ever overflows
	private static final int MAX_STATES = 1 << 9;

	private final List<ViaChain> chains = new ArrayList<>();
	private final Map<String, ViaChain> chainsById = new HashMap<>(); // "from id:restriction index"
	private final TLongObjectHashMap<List<ViaChain>> chainsByRoad = new TLongObjectHashMap<>(); // from, vias and to
	private final Map<String, ViaChainState> states = new HashMap<>();

	public void registerRoad(RouteDataObject road) {
		for (int k = 0; k < road.getRestrictionLength(); k++) {
			long[] vias = road.getRestrictionViaWays(k);
			String id = road.getId() + ":" + k;
			if (vias != null && !chainsById.containsKey(id)) {
				ViaChain chain = new ViaChain(chains.size(), road.getId(), vias, road.getRestrictionId(k),
						road.getRestrictionType(k));
				chains.add(chain);
				chainsById.put(id, chain);
				addChainToRoad(chain.from, chain);
				for (long via : vias) {
					addChainToRoad(via, chain);
				}
				addChainToRoad(chain.to, chain);
			}
		}
	}

	public static long getVisitedKey(long pointId, RouteSegment segment) {
		return segment.viaChainState == null ? pointId : pointId + ((long) segment.viaChainState.key << 54);
	}

	// state of the segment on "next" reached from "current" at the node of nodeRoads, FORBIDDEN if the turn is restricted
	ViaChainState nextState(RouteSegment current, RouteDataObject next, RouteSegment nodeRoads, boolean reverseWaySearch) {
		if (chains.isEmpty()) {
			return null;
		}
		long currentId = current.getRoad().getId();
		long nextId = next.getId();
		TLongArrayList steps = new TLongArrayList();
		long[] currentSteps = current.viaChainState == null ? new long[0] : current.viaChainState.steps;
		for (long step : currentSteps) {
			ViaChain chain = chains.get((int) (step >> STEP_BITS));
			int i = (int) (step & ((1 << STEP_BITS) - 1));
			if (!reverseWaySearch) {
				boolean lastVia = i == chain.vias.length - 1;
				if (!lastVia && nextId == chain.vias[i + 1]) {
					steps.add(step + 1);
				} else if (chain.onlyRestriction != (lastVia && nextId == chain.to)) {
					return FORBIDDEN; // only_* leaves the chain or no_* completes it
				}
			} else if (i > 0) {
				if (nextId == chain.vias[i - 1]) {
					steps.add(step - 1);
				}
			} else if (nextId == chain.from) {
				return FORBIDDEN;
			}
		}
		List<ViaChain> nextChains = chainsByRoad.get(reverseWaySearch ? nextId : currentId);
		for (int c = 0; nextChains != null && c < nextChains.size(); c++) {
			ViaChain chain = nextChains.get(c);
			if (!reverseWaySearch && chain.from == currentId) {
				if (nextId == chain.vias[0]) {
					steps.add(stepOf(chain, 0));
				} else if (chain.onlyRestriction && isAtNode(nodeRoads, chain.vias[0])) {
					return FORBIDDEN;
				}
			} else if (reverseWaySearch && chain.from == nextId) {
				if (chain.onlyRestriction && currentId != chain.vias[0] && isAtNode(nodeRoads, chain.vias[0])) {
					return FORBIDDEN;
				}
			} else if (reverseWaySearch) {
				for (int j = 0; j < chain.vias.length; j++) {
					boolean lastVia = j == chain.vias.length - 1;
					boolean followsChain = lastVia ? currentId == chain.to : currentId == chain.vias[j + 1];
					boolean restrictedRest = chain.onlyRestriction ? !followsChain : lastVia && followsChain;
					if (nextId == chain.vias[j] && restrictedRest) {
						steps.add(stepOf(chain, j));
					}
				}
			}
		}
		return getState(steps);
	}

	static RouteSegment withState(RouteSegment segment, ViaChainState state) {
		if (segment == null || state == null) {
			return segment;
		}
		RouteSegment copy = new RouteSegment(segment.getRoad(), segment.getSegmentStart(), segment.getSegmentEnd());
		copy.viaChainState = state;
		return copy;
	}

	private ViaChainState getState(TLongArrayList steps) {
		if (steps.isEmpty()) {
			return null;
		}
		long[] sorted = steps.toArray();
		Arrays.sort(sorted);
		String id = Arrays.toString(sorted);
		ViaChainState state = states.get(id);
		if (state == null) {
			if (states.size() + 1 >= MAX_STATES) {
				throw new IllegalStateException("Too many via chain states");
			}
			state = new ViaChainState(sorted, states.size() + 1);
			states.put(id, state);
		}
		return state;
	}

	private static long stepOf(ViaChain chain, int via) {
		return ((long) chain.index << STEP_BITS) + via;
	}

	private static boolean isAtNode(RouteSegment nodeRoads, long roadId) {
		for (RouteSegment s = nodeRoads; s != null; s = s.getNext()) {
			if (s.getRoad().getId() == roadId) {
				return true;
			}
		}
		return false;
	}

	private void addChainToRoad(long roadId, ViaChain chain) {
		if (!chainsByRoad.containsKey(roadId)) {
			chainsByRoad.put(roadId, new ArrayList<>());
		}
		chainsByRoad.get(roadId).add(chain);
	}
}
