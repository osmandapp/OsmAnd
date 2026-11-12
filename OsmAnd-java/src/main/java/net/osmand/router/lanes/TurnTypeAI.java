package net.osmand.router.lanes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.router.TurnType;

/** A lane model written from the OSM data model rather than from the current in-memory encoding. */
public final class TurnTypeAI {

	// ------------------------------------------------------------------ maneuver

	/** The maneuver itself. */
	public enum Maneuver {
		CONTINUE(1),
		TURN_LEFT(2),
		SLIGHT_LEFT(3),
		SHARP_LEFT(4),
		TURN_RIGHT(5),
		SLIGHT_RIGHT(6),
		SHARP_RIGHT(7),
		KEEP_LEFT(8),
		KEEP_RIGHT(9),
		U_TURN(10),
		OFF_ROUTE(12),
		ROUNDABOUT(13);

		private static final int U_TURN_LEFT_HAND = 11;
		private static final int ROUNDABOUT_LEFT_HAND = 14;

		private final int code;

		Maneuver(int code) {
			this.code = code;
		}

		int code(boolean leftHandTraffic) {
			if (leftHandTraffic && this == U_TURN) {
				return U_TURN_LEFT_HAND;
			}
			if (leftHandTraffic && this == ROUNDABOUT) {
				return ROUNDABOUT_LEFT_HAND;
			}
			return code;
		}
	}

	// ------------------------------------------------------------------ lane vocabulary

	/** The values of {@code turn:lanes}, spelled as OSM spells them. */
	public enum TurnIndication {
		NONE("none"),
		THROUGH("through"),
		LEFT("left"),
		RIGHT("right"),
		SLIGHT_LEFT("slight_left"),
		SLIGHT_RIGHT("slight_right"),
		SHARP_LEFT("sharp_left"),
		SHARP_RIGHT("sharp_right"),
		REVERSE("reverse"),
		MERGE_TO_LEFT("merge_to_left"),
		MERGE_TO_RIGHT("merge_to_right");

		/** what a mapper writes instead of "through" often enough to be worth reading */
		private static final String THROUGH_SPELT_OTHERWISE = "straight";

		private final String osmValue;

		TurnIndication(String osmValue) {
			this.osmValue = osmValue;
		}

		public String osmValue() {
			return osmValue;
		}

		public static TurnIndication parse(String v) {
			for (TurnIndication t : values()) {
				if (t.osmValue.equals(v)) {
					return t;
				}
			}
			if (THROUGH_SPELT_OTHERWISE.equals(v)) {
				// not a guess: "straight" is the same word as "through", written by a mapper who wrote what the arrow
				// looks like.
				return THROUGH;
			}
			return null; // an unknown word is not an indication, and guessing one would be worse
		}
	}

	/** Which group of the carriageway a lane belongs to. */
	public enum LaneGroup {
		FORWARD("forward"),
		BACKWARD("backward"),
		/** the central lanes, shared by both directions - a left-turn pocket, a suicide lane */
		BOTH_WAYS("both_ways");

		private final String suffix;

		LaneGroup(String suffix) {
			this.suffix = suffix;
		}

		public String suffix() {
			return suffix;
		}
	}

	/** The transport modes of the OSM access hierarchy, each pointing at the key that it refines. */
	public enum TransportMode {
		ACCESS("access", null),
		FOOT("foot", "access"),
		VEHICLE("vehicle", "access"),
		BICYCLE("bicycle", "vehicle"),
		MOTOR_VEHICLE("motor_vehicle", "vehicle"),
		MOTORCAR("motorcar", "motor_vehicle"),
		MOTORCYCLE("motorcycle", "motor_vehicle"),
		MOPED("moped", "motor_vehicle"),
		HGV("hgv", "motor_vehicle"),
		EMERGENCY("emergency", "motor_vehicle"),
		PSV("psv", "motor_vehicle"),
		BUS("bus", "psv"),
		TAXI("taxi", "psv");

		private final String key;
		private final String refinesKey;

		TransportMode(String key, String refinesKey) {
			this.key = key;
			this.refinesKey = refinesKey;
		}

		public String key() {
			return key;
		}

		/** the more general key this one refines, or null for the root */
		public TransportMode refines() {
			return refinesKey == null ? null : byKey(refinesKey);
		}

		public static TransportMode byKey(String key) {
			for (TransportMode m : values()) {
				if (m.key.equals(key)) {
					return m;
				}
			}
			return null;
		}
	}

	/** The values an access tag can carry, plus what each one means for a router. */
	public enum AccessValue {
		YES(true),
		DESIGNATED(true),
		PERMISSIVE(true),
		DESTINATION(true),
		PRIVATE(false),
		NO(false),
		USE_SIDEPATH(false);

		private final boolean open;

		AccessValue(boolean open) {
			this.open = open;
		}

		/** may a vehicle of this mode drive here at all - the nuances of why not are kept above */
		public boolean isOpen() {
			return open;
		}

		public static AccessValue parse(String v) {
			if (v == null) {
				return null;
			}
			for (AccessValue a : values()) {
				if (a.name().equalsIgnoreCase(v)) {
					return a;
				}
			}
			return null;
		}
	}

	/**
	 * {@code change:lanes}. The tag is written per lane but describes the line PAINTED BESIDE it, so two
	 * neighbours always say the same thing twice and sometimes disagree; see reconcileChanges(List) for what
	 * is done about that.
	 */
	public enum LaneChange {
		UNKNOWN,
		ALLOWED,
		FORBIDDEN
	}

	// ------------------------------------------------------------------ the lane

	/** One physical lane, as the map describes it, plus the one thing the router adds. */
	public static final class Lane {

		/** every indication of {@code turn:lanes}, in the order they were written, never truncated */
		private final List<TurnIndication> turns;
		private final LaneGroup group;
		/** only the keys the map actually carried, so "absent" stays distinguishable from "no" */
		private final Map<TransportMode, AccessValue> access;
		private final LaneChange changeLeft;
		private final LaneChange changeRight;
		/** {@code destination:lanes}, one lane may name several places */
		private final List<String> destinations;
		/** {@code width:lanes} in metres, Double#NaN when the map is silent */
		private final double width;
		/** NOT from the map: this lane leads where the route goes */
		private final boolean active;
		/** NOT from the map: which of this lane's arrows the route takes, null when it has none */
		private final TurnIndication taken;

		private Lane(List<TurnIndication> turns, LaneGroup group, Map<TransportMode, AccessValue> access,
		             LaneChange changeLeft, LaneChange changeRight, List<String> destinations,
		             double width, boolean active, TurnIndication taken) {
			this.turns = Collections.unmodifiableList(new ArrayList<>(turns));
			this.group = group;
			EnumMap<TransportMode, AccessValue> copy = new EnumMap<>(TransportMode.class);
			copy.putAll(access);
			this.access = Collections.unmodifiableMap(copy);
			this.changeLeft = changeLeft;
			this.changeRight = changeRight;
			this.destinations = Collections.unmodifiableList(new ArrayList<>(destinations));
			this.width = width;
			this.active = active;
			this.taken = active ? taken : null;
		}

		public List<TurnIndication> turns() {
			return turns;
		}

		public LaneGroup group() {
			return group;
		}

		public LaneChange changeLeft() {
			return changeLeft;
		}

		public LaneChange changeRight() {
			return changeRight;
		}

		public List<String> destinations() {
			return destinations;
		}

		public double width() {
			return width;
		}

		public boolean isActive() {
			return active;
		}

		/** The arrow of this lane that the route takes. */
		public TurnIndication taken() {
			return taken;
		}

		public Lane withActive(boolean value) {
			return new Lane(turns, group, access, changeLeft, changeRight, destinations, width, value,
					value ? taken : null);
		}

		/** the route goes this way on this lane */
		public Lane withTaken(TurnIndication arrow) {
			return new Lane(turns, group, access, changeLeft, changeRight, destinations, width, true, arrow);
		}

		public Lane withChange(LaneChange left, LaneChange right) {
			return new Lane(turns, group, access, left, right, destinations, width, active, taken);
		}

		/** the same lane with other arrows, everything else the map said about it kept */
		public Lane withTurns(List<TurnIndication> value) {
			return new Lane(value, group, access, changeLeft, changeRight, destinations, width, active, taken);
		}

		/** the value the map gave for exactly this key, with no inheritance applied */
		public AccessValue declaredAccess(TransportMode mode) {
			return access.get(mode);
		}

		/** Walks the access hierarchy upwards and answers with the first thing the map said. */
		public AccessValue resolvedAccess(TransportMode mode) {
			for (TransportMode m = mode; m != null; m = m.refines()) {
				AccessValue v = access.get(m);
				if (v != null) {
					return v;
				}
			}
			return null;
		}

		/** open to this mode, with an absent tag read as "nobody said no" */
		public boolean isOpenTo(TransportMode mode) {
			AccessValue v = resolvedAccess(mode);
			return v == null || v.isOpen();
		}

		/**
		 * The mode this lane exists FOR, if any: the most specific key tagged {@code designated}. This is what
		 * tells a bus lane from a lane a bus happens to be allowed on, and it is the only thing a renderer needs
		 * to know to draw a lane as somebody else's.
		 */
		public TransportMode designatedFor() {
			TransportMode best = null;
			for (Map.Entry<TransportMode, AccessValue> e : access.entrySet()) {
				if (e.getValue() == AccessValue.DESIGNATED && (best == null || refines(e.getKey(), best))) {
					best = e.getKey();
				}
			}
			return best;
		}

		/** A lane kept for somebody else. */
		public boolean isForeignTo(TransportMode mode) {
			TransportMode owner = designatedFor();
			if (owner == null || isSameOrRefines(mode, owner)) {
				return false;
			}
			AccessValue mine = resolvedAccess(mode);
			return mine == null || !mine.isOpen();
		}

		/** may a vehicle of this mode be in this lane at all */
		public boolean isUsableBy(TransportMode mode) {
			return isOpenTo(mode) && !isForeignTo(mode);
		}

		/** may a driver leave this lane sideways - {@code true} unless the map forbids it */
		public boolean mayChangeLeft() {
			return changeLeft != LaneChange.FORBIDDEN;
		}

		public boolean mayChangeRight() {
			return changeRight != LaneChange.FORBIDDEN;
		}

		private static boolean refines(TransportMode candidate, TransportMode other) {
			for (TransportMode m = candidate; m != null; m = m.refines()) {
				if (m == other) {
					return true;
				}
			}
			return false;
		}

		private static boolean isSameOrRefines(TransportMode mode, TransportMode owner) {
			return refines(mode, owner) || refines(owner, mode);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(active ? "+" : "-");
			for (int i = 0; i < turns.size(); i++) {
				sb.append(i > 0 ? ";" : "").append(turns.get(i) == taken ? "*" : "")
						.append(turns.get(i).osmValue());
			}
			TransportMode owner = designatedFor();
			if (owner != null) {
				sb.append('[').append(owner.key()).append(']');
			}
			if (group == LaneGroup.BOTH_WAYS) {
				sb.append("[centre]");
			}
			if (!mayChangeLeft() || !mayChangeRight()) {
				sb.append(mayChangeLeft() ? "|>" : "<|");
			}
			return sb.toString();
		}

		public static final class Builder {
			private final List<TurnIndication> turns = new ArrayList<>();
			private LaneGroup group = LaneGroup.FORWARD;
			private final Map<TransportMode, AccessValue> access = new EnumMap<>(TransportMode.class);
			private LaneChange changeLeft = LaneChange.UNKNOWN;
			private LaneChange changeRight = LaneChange.UNKNOWN;
			private final List<String> destinations = new ArrayList<>();
			private double width = Double.NaN;
			private boolean active;

			public Builder turns(List<TurnIndication> value) {
				turns.clear();
				turns.addAll(value);
				return this;
			}

			public Builder group(LaneGroup value) {
				group = value;
				return this;
			}

			public Builder access(TransportMode mode, AccessValue value) {
				if (value != null) {
					access.put(mode, value);
				}
				return this;
			}

			public Builder change(LaneChange left, LaneChange right) {
				changeLeft = left;
				changeRight = right;
				return this;
			}

			public Builder destinations(List<String> value) {
				destinations.clear();
				destinations.addAll(value);
				return this;
			}

			public Builder width(double value) {
				width = value;
				return this;
			}

			public Builder active(boolean value) {
				active = value;
				return this;
			}

			private TurnIndication taken;

			public Builder taken(TurnIndication value) {
				taken = value;
				active |= value != null;
				return this;
			}

			public Lane build() {
				return new Lane(turns, group, access, changeLeft, changeRight, destinations, width, active,
						taken);
			}
		}
	}

	// ------------------------------------------------------------------ state

	private final Maneuver maneuver;
	private final boolean leftHandTraffic;
	/** the traveller this turn was prepared for - a route always belongs to somebody */
	private final TransportMode traveller;
	private final int exitOut;
	private final float turnAngle;
	private final boolean skipToSpeak;
	/** left to right as the driver of this maneuver sees them, central lanes included */
	private final List<Lane> lanes;
	/** the lanes were chosen knowing what happens beyond this junction, so nothing should redo it */
	private boolean lanesDecidedAhead;

	public TurnTypeAI(Maneuver maneuver, boolean leftHandTraffic, TransportMode traveller, int exitOut,
	                  float turnAngle, boolean skipToSpeak, List<Lane> lanes) {
		this.maneuver = maneuver;
		this.leftHandTraffic = leftHandTraffic;
		this.traveller = traveller == null ? TransportMode.MOTORCAR : traveller;
		this.exitOut = exitOut;
		this.turnAngle = turnAngle;
		this.skipToSpeak = skipToSpeak;
		this.lanes = Collections.unmodifiableList(new ArrayList<>(lanes));
	}

	public Maneuver maneuver() {
		return maneuver;
	}

	public List<Lane> lanes() {
		return lanes;
	}

	// ------------------------------------------------------------------ the model's own accessors

	public int exitOut() {
		return exitOut;
	}

	public float turnAngle() {
		return turnAngle;
	}

	public boolean skipToSpeak() {
		return skipToSpeak;
	}

	public boolean leftHandTraffic() {
		return leftHandTraffic;
	}

	public TransportMode traveller() {
		return traveller;
	}

	public boolean lanesDecidedAhead() {
		return lanesDecidedAhead;
	}

	public TurnTypeAI decidedAhead(boolean value) {
		TurnTypeAI copy = new TurnTypeAI(maneuver, leftHandTraffic, traveller, exitOut, turnAngle,
				skipToSpeak, lanes);
		copy.lanesDecidedAhead = value;
		return copy;
	}

	/** The lane table as the driver of THIS route sees it, and the only thing an interface needs. */
	public LanePanelAI panel() {
		return LanePanelAI.of(this);
	}

	public TurnTypeAI withLanes(List<Lane> newLanes) {
		return new TurnTypeAI(maneuver, leftHandTraffic, traveller, exitOut, turnAngle, skipToSpeak, newLanes)
				.decidedAhead(lanesDecidedAhead);
	}

	public TurnTypeAI withManeuver(Maneuver newManeuver) {
		return new TurnTypeAI(newManeuver, leftHandTraffic, traveller, exitOut, turnAngle, skipToSpeak, lanes)
				.decidedAhead(lanesDecidedAhead);
	}

	public TurnTypeAI withSkipToSpeak(boolean value) {
		return new TurnTypeAI(maneuver, leftHandTraffic, traveller, exitOut, turnAngle, value, lanes)
				.decidedAhead(lanesDecidedAhead);
	}

	// ------------------------------------------------------------------ the one way out

	/**
	 * The legacy object, for everything that has not moved to this model: widgets, the voice router, Android
	 * Auto, the external API, GPX. It is an EXPORT and it is lossy by construction.
	 */
	public TurnType getOldTurnType() {
		int[] packed = new int[lanes.size()];
		for (int i = 0; i < lanes.size(); i++) {
			Lane lane = lanes.get(i);
			int v = lane.isActive() ? 1 : 0;
			List<TurnIndication> turns = lane.turns();
			for (int t = 0; t < turns.size() && t < 3; t++) {
				int legacy = legacyTurnCode(turns.get(t));
				v |= legacy << (t == 0 ? 1 : t == 1 ? 5 : 10);
			}
			packed[i] = v;
		}
		// the old flags mean "a turn to that side is possible here, though it is not the maneuver", which in this
		// model is simply a lane that indicates it
		boolean possiblyLeft = false;
		boolean possiblyRight = false;
		for (Lane lane : lanes) {
			for (TurnIndication t : lane.turns()) {
				possiblyLeft |= t == TurnIndication.LEFT || t == TurnIndication.SHARP_LEFT
						|| (t == TurnIndication.REVERSE && !leftHandTraffic);
				possiblyRight |= t == TurnIndication.RIGHT || t == TurnIndication.SHARP_RIGHT
						|| (t == TurnIndication.REVERSE && leftHandTraffic);
			}
		}
		return new TurnType(maneuver.code(leftHandTraffic), exitOut, turnAngle, skipToSpeak,
				packed.length == 0 ? null : packed, possiblyLeft, possiblyRight);
	}

	private static int legacyTurnCode(TurnIndication t) {
		switch (t) {
			case THROUGH:
			case NONE:
			case MERGE_TO_LEFT:
			case MERGE_TO_RIGHT:
				return Maneuver.CONTINUE.code;
			case LEFT:
				return Maneuver.TURN_LEFT.code;
			case SLIGHT_LEFT:
				return Maneuver.SLIGHT_LEFT.code;
			case SHARP_LEFT:
				return Maneuver.SHARP_LEFT.code;
			case RIGHT:
				return Maneuver.TURN_RIGHT.code;
			case SLIGHT_RIGHT:
				return Maneuver.SLIGHT_RIGHT.code;
			case SHARP_RIGHT:
				return Maneuver.SHARP_RIGHT.code;
			case REVERSE:
				return Maneuver.U_TURN.code;
			default:
				return Maneuver.CONTINUE.code;
		}
	}

	// ------------------------------------------------------------------ reading OSM

	/** the tags this model reads, each as {@code :lanes[: ]} */
	private static final List<TransportMode> ACCESS_KEYS = Arrays.asList(
			TransportMode.ACCESS, TransportMode.VEHICLE, TransportMode.MOTOR_VEHICLE,
			TransportMode.MOTORCAR, TransportMode.MOTORCYCLE, TransportMode.MOPED,
			TransportMode.HGV, TransportMode.EMERGENCY, TransportMode.PSV,
			TransportMode.BUS, TransportMode.TAXI, TransportMode.BICYCLE, TransportMode.FOOT);

	/** Builds the lane list a driver sees, from the tags of the way they are driving on. */
	public static List<String> relevantTags() {
		List<String> tags = new ArrayList<>();
		tags.add("oneway");
		tags.add("lanes");
		for (LaneGroup g : LaneGroup.values()) {
			tags.add("lanes:" + g.suffix());
		}
		for (String key : vectorKeys()) {
			tags.add(key + ":lanes");
			for (LaneGroup g : LaneGroup.values()) {
				tags.add(key + ":lanes:" + g.suffix());
			}
		}
		return tags;
	}

	public static List<Lane> parseLanes(Map<String, String> tags, boolean forward, boolean leftHandTraffic) {
		LaneGroup own = forward ? LaneGroup.FORWARD : LaneGroup.BACKWARD;
		List<Lane> ownLanes = parseGroup(tags, own, forward);
		List<Lane> centre = parseGroup(tags, LaneGroup.BOTH_WAYS, forward);
		if (!forward) {
			Collections.reverse(centre);
		}
		List<Lane> all = new ArrayList<>(ownLanes.size() + centre.size());
		if (leftHandTraffic) {
			all.addAll(ownLanes);
			all.addAll(centre);
		} else {
			all.addAll(centre);
			all.addAll(ownLanes);
		}
		return reconcileChanges(all);
	}

	/** One group. */
	private static List<Lane> parseGroup(Map<String, String> tags, LaneGroup group, boolean forward) {
		Map<String, String[]> vectors = new LinkedHashMap<>();
		int count = countOf(tags, group, forward);
		for (String key : vectorKeys()) {
			String[] v = vector(tags, key, group, forward);
			if (v != null) {
				count = Math.max(count, v.length);
				vectors.put(key, v);
			}
		}
		if (count <= 0) {
			return Collections.emptyList();
		}
		List<Lane.Builder> builders = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			builders.add(new Lane.Builder().group(group));
		}
		for (Map.Entry<String, String[]> e : vectors.entrySet()) {
			String[] v = e.getValue();
			if (v.length != count) {
				continue; // the vector does not describe this many lanes, so it describes nothing
			}
			for (int i = 0; i < count; i++) {
				apply(builders.get(i), e.getKey(), v[i]);
			}
		}
		List<Lane> lanes = new ArrayList<>(count);
		for (Lane.Builder b : builders) {
			lanes.add(b.build());
		}
		return lanes;
	}

	private static List<String> vectorKeys() {
		List<String> keys = new ArrayList<>();
		keys.add("turn");
		keys.add("change");
		keys.add("destination");
		keys.add("width");
		keys.add("cycleway");
		for (TransportMode m : ACCESS_KEYS) {
			keys.add(m.key());
		}
		return keys;
	}

	private static void apply(Lane.Builder b, String key, String raw) {
		String value = raw == null ? "" : raw.trim();
		switch (key) {
			case "turn":
				b.turns(parseTurns(value));
				return;
			case "change":
				applyChange(b, value);
				return;
			case "destination":
				b.destinations(splitValues(value));
				return;
			case "width":
				b.width(parseDouble(value));
				return;
			case "cycleway":
				// cycleway:lanes uses its own words for the same idea
				if ("lane".equals(value) || "share_busway".equals(value)) {
					b.access(TransportMode.BICYCLE, AccessValue.DESIGNATED);
				}
				return;
			default:
				for (TransportMode m : ACCESS_KEYS) {
					if (m.key().equals(key)) {
						b.access(m, AccessValue.parse(value));
						return;
					}
				}
		}
	}

	private static void applyChange(Lane.Builder b, String value) {
		switch (value) {
			case "no":
				b.change(LaneChange.FORBIDDEN, LaneChange.FORBIDDEN);
				return;
			case "not_left":
				b.change(LaneChange.FORBIDDEN, LaneChange.ALLOWED);
				return;
			case "not_right":
				b.change(LaneChange.ALLOWED, LaneChange.FORBIDDEN);
				return;
			case "yes":
				b.change(LaneChange.ALLOWED, LaneChange.ALLOWED);
				return;
			default:
				b.change(LaneChange.UNKNOWN, LaneChange.UNKNOWN);
		}
	}

	/** A restriction is a line between two lanes, so each one is written twice, once from each side. */
	private static List<Lane> reconcileChanges(List<Lane> lanes) {
		if (lanes.size() < 2) {
			return lanes;
		}
		LaneChange[] left = new LaneChange[lanes.size()];
		LaneChange[] right = new LaneChange[lanes.size()];
		for (int i = 0; i < lanes.size(); i++) {
			left[i] = lanes.get(i).changeLeft();
			right[i] = lanes.get(i).changeRight();
		}
		for (int i = 0; i + 1 < lanes.size(); i++) {
			LaneChange line = stricter(right[i], left[i + 1]);
			right[i] = line;
			left[i + 1] = line;
		}
		List<Lane> out = new ArrayList<>(lanes.size());
		for (int i = 0; i < lanes.size(); i++) {
			out.add(lanes.get(i).withChange(left[i], right[i]));
		}
		return out;
	}

	private static LaneChange stricter(LaneChange a, LaneChange b) {
		if (a == LaneChange.FORBIDDEN || b == LaneChange.FORBIDDEN) {
			return LaneChange.FORBIDDEN;
		}
		if (a == LaneChange.ALLOWED || b == LaneChange.ALLOWED) {
			return LaneChange.ALLOWED;
		}
		return LaneChange.UNKNOWN;
	}

	private static List<TurnIndication> parseTurns(String value) {
		List<TurnIndication> turns = new ArrayList<>();
		for (String part : splitValues(value)) {
			TurnIndication t = TurnIndication.parse(part);
			if (t != null && !turns.contains(t)) {
				turns.add(t);
			}
		}
		return turns;
	}

	/** {@code lanes: }, or {@code lanes} minus the other groups when the way is one-way. */
	private static int countOf(Map<String, String> tags, LaneGroup group, boolean forward) {
		Integer explicit = parseInt(tags.get("lanes:" + group.suffix()));
		if (explicit != null) {
			return explicit;
		}
		if (group == LaneGroup.BOTH_WAYS) {
			return 0;
		}
		Integer total = parseInt(tags.get("lanes"));
		if (total == null) {
			return 0;
		}
		Integer other = parseInt(tags.get("lanes:" + (forward ? LaneGroup.BACKWARD : LaneGroup.FORWARD).suffix()));
		Integer centre = parseInt(tags.get("lanes:" + LaneGroup.BOTH_WAYS.suffix()));
		if (other != null) {
			return Math.max(0, total - other - (centre == null ? 0 : centre));
		}
		return total;
	}

	/** The vector for one attribute of one group. */
	private static String[] vector(Map<String, String> tags, String key, LaneGroup group, boolean forward) {
		String suffixed = tags.get(key + ":lanes:" + group.suffix());
		if (suffixed != null) {
			return split(suffixed);
		}
		if (group != LaneGroup.BOTH_WAYS && isOneWay(tags)) {
			String plain = tags.get(key + ":lanes");
			if (plain != null) {
				return split(plain);
			}
		}
		return null;
	}

	private static boolean isOneWay(Map<String, String> tags) {
		String v = tags.get("oneway");
		return "yes".equals(v) || "1".equals(v) || "-1".equals(v);
	}

	private static String[] split(String vector) {
		return vector.split("\\|", -1);
	}

	private static List<String> splitValues(String cell) {
		List<String> values = new ArrayList<>();
		if (cell == null || cell.isEmpty()) {
			return values;
		}
		for (String part : cell.split(";", -1)) {
			String v = part.trim();
			if (!v.isEmpty()) {
				values.add(v);
			}
		}
		return values;
	}

	private static Integer parseInt(String v) {
		try {
			return v == null ? null : Integer.valueOf(v.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static double parseDouble(String v) {
		try {
			return v == null || v.isEmpty() ? Double.NaN : Double.parseDouble(v.trim());
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(maneuver.name());
		if (maneuver == Maneuver.ROUNDABOUT) {
			sb.append(" exit ").append(exitOut);
		}
		for (int i = 0; i < lanes.size(); i++) {
			sb.append(i == 0 ? " " : " | ").append(lanes.get(i));
		}
		return sb.toString();
	}
}
