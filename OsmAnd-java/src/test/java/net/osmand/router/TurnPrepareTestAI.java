package net.osmand.router;

import static net.osmand.util.RouterUtilTest.getRoadId;
import static net.osmand.util.RouterUtilTest.getRoadStartPoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.GsonBuilder;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.lanes.LanePanelAI;
import net.osmand.router.lanes.TurnPrepareAI;
import net.osmand.router.lanes.TurnTypeAI;
import net.osmand.router.lanes.TurnTypeAI.TurnIndication;

/**
 * The same cases as {@link RouteResultPreparationTest}, prepared by {@link TurnPrepareAI}.
 *
 * <p>The recorded strings were written for another implementation, and many of them encode a human
 * judgement about what an instruction should say rather than a fact about the map, so a case gets
 * one of three verdicts:
 *
 * <ul>
 * <li><b>OK</b> - the maneuver and the lanes say the same thing;</li>
 * <li><b>SIMILAR</b> - a difference a person has to judge: neighbouring maneuvers on the
 *     left-to-right ladder ({@link TurnType#orderFromLeftToRight}, so straight against keep right),
 *     the same lanes with a different set marked, the same arrows in a different order, or only
 *     the muting;</li>
 * <li><b>FAIL</b> - no instruction where one was expected, an instruction where none was, a
 *     maneuver two steps away or more, or a different lane structure.</li>
 * </ul>
 *
 * <p>A case with any FAIL fails the test and prints what differs. SIMILAR is counted and printed,
 * never asserted.
 *
 * <h3>Lane format</h3>
 *
 * {@code [MUTE] KR:TU|TL|TL,+C|+C|+C,TR} - the maneuver, then one entry per lane, left to right as
 * the driver sees them, arrows in the order the map wrote them. The {@code +} marks the arrow the
 * route takes, not the lane, so a lane whose arrows are {@code TL,C} where the route goes straight
 * reads {@code TL,+C}. The old format wrote the taken arrow first and marked the lane instead
 * ({@code +C,TL}); both parse to the same thing here, which is what lets the two be compared.
 */
@RunWith(Parameterized.class)
public class TurnPrepareTestAI {

	/**
	 * Measured on 2026-09-11 over the whole file, after the verdict was tightened so that marking
	 * lanes which lead somewhere else counts as a failure rather than a judgement call. That
	 * re-scoring alone moved 23 expectations from SIMILAR to FAIL; the number is not comparable
	 * with anything measured before it.
	 */
	private static final int MEASURED_FAIL = 37;

	private static final String MAP = "src/test/resources/Turn_lanes_test.obf";
	private static final String CASES = "/test_turn_lanes.json";
	private static final String MUTE = "[MUTE] ";

	enum Verdict {
		/** the record names the road but says nothing about it: reached, never judged */
		SKIP, OK, SIMILAR, FAIL
	}

	private static final Map<Verdict, Integer> TOTALS = new LinkedHashMap<>();
	private static final Map<String, Integer> REASONS = new LinkedHashMap<>();
	private static final List<String> REPORT = new ArrayList<>();
	private static int CASE_COUNT;

	private final TestEntry te;
	private final String name;

	public TurnPrepareTestAI(String name, TestEntry te) {
		this.name = name;
		this.te = te;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() throws IOException {
		Reader reader = new InputStreamReader(
				Objects.requireNonNull(TurnPrepareTestAI.class.getResourceAsStream(CASES)));
		TestEntry[] entries = new GsonBuilder().setPrettyPrinting().create().fromJson(reader, TestEntry[].class);
		reader.close();
		List<Object[]> cases = new ArrayList<>();
		for (TestEntry entry : entries) {
			if (!entry.isIgnore()) {
				cases.add(new Object[] {entry.getTestName(), entry});
			}
		}
		CASE_COUNT = cases.size();
		return cases;
	}

	@Test
	public void testLanes() throws Exception {
		List<RouteSegmentResult> route = route();
		Assert.assertNotNull(route);

		new TurnPrepareAI().prepareTurnResults(context(), route);

		Map<String, String> actual = collect(route);
		String produced = produced(route);
		REPORT.add(String.format("route    %-52s %s", name, produced));

		List<String> lines = new ArrayList<>();
		int failed = 0;
		for (Entry<String, String> expected : te.getExpectedResults().entrySet()) {
			String key = expected.getKey();
			String value = actual.get(key(getRoadId(key), getRoadStartPoint(key), actual));
			Verdict verdict = verdict(expected.getValue(), value);
			String reason = verdict == Verdict.OK ? "" : reason(expected.getValue(), value);
			count(verdict, verdict == Verdict.FAIL ? reason : "");
			REPORT.add(String.format("%-8s %-10s %-52s %-14s expected %-30s actual %s", verdict, reason,
					name, key, quote(expected.getValue()), quote(value)));
			lines.add(String.format("  %-8s %-8s %-14s expected %-30s actual %s", verdict, reason, key,
					quote(expected.getValue()), quote(value)));
			if (verdict == Verdict.FAIL) {
				failed++;
			}
		}
		if (failed > 0) {
			int total = lines.size();
			StringBuilder message = new StringBuilder();
			message.append(failed).append(" of ").append(total).append(" expectations differ in '")
					.append(name).append("'\n");
			if (total > failed) {
				message.append(total - failed).append(" of ").append(total)
						.append(" expectations OK | SIMILAR\n");
			}
			for (String line : lines) {
				message.append(line).append('\n');
			}
			message.append("  route: ").append(produced);
			Assert.fail(message.toString());
		}
	}

	// ------------------------------------------------------------------ verdicts

	static Verdict verdict(String expected, String actual) {
		if (expected == null) {
			// null and "" are not the same sentence: null says "this road is on the route" and
			// nothing more, while "" says "and it carries no instruction"
			return Verdict.SKIP;
		}
		if (expected.isEmpty()) {
			// whether a plain "carry on" is worth an instruction at all is a judgement, not a fact:
			// one side shows the lanes and says nothing, the other says nothing at all
			return actual == null ? Verdict.OK : onlyCarriesOn(actual) ? Verdict.SIMILAR : Verdict.FAIL;
		}
		if (actual == null) {
			return onlyCarriesOn(expected) ? Verdict.SIMILAR : Verdict.FAIL;
		}
		if (expected.equals(actual)) {
			return Verdict.OK;
		}
		Parsed e = Parsed.of(expected);
		Parsed a = Parsed.of(actual);
		Verdict worst = Verdict.OK;
		if (e.turn != null) {
			worst = worse(worst, compareTurns(e.turn, a.turn));
		}
		if (e.lanes != null) {
			worst = worse(worst, compareLanes(e.lanes, a.lanes));
		}
		if (worst == Verdict.OK && e.mute != a.mute) {
			worst = Verdict.SIMILAR; // only the voice differs
		}
		return worst;
	}

	/** an instruction that announces nothing: carry on, or keep to one side, on lanes that carry on */
	static boolean onlyCarriesOn(String value) {
		Parsed parsed = Parsed.of(value);
		if (parsed.turn != null && !parsed.turn.equals("C") && !parsed.turn.equals("KL")
				&& !parsed.turn.equals("KR")) {
			return false;
		}
		if (parsed.lanes == null) {
			return true;
		}
		for (LaneShape lane : LaneShape.parse(parsed.lanes)) {
			if (lane.marked != null && !lane.marked.equals("C")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Neighbours on the left-to-right ladder are one judgement apart: straight and keep right are
	 * the same road read by two people. Two steps or more is a different instruction.
	 */
	private static Verdict compareTurns(String expected, String actual) {
		if (expected.equals(actual)) {
			return Verdict.OK;
		}
		int e = TurnType.orderFromLeftToRight(TurnType.fromString(expected, false).getValue());
		int a = TurnType.orderFromLeftToRight(TurnType.fromString(actual, false).getValue());
		return Math.abs(e - a) <= 1 ? Verdict.SIMILAR : Verdict.FAIL;
	}

	/**
	 * The lane structure is a fact of the map and has to match. Which lanes are marked is the
	 * judgement: the old implementation marked the one lane it chose, this one marks every lane
	 * that leads where the route goes.
	 */
	private static Verdict compareLanes(String expected, String actual) {
		List<LaneShape> e = LaneShape.parse(expected);
		List<LaneShape> a = LaneShape.parse(actual);
		if (e.size() != a.size()) {
			return Verdict.FAIL;
		}
		boolean sameLanes = true;
		TreeSet<String> markedExpected = new TreeSet<>();
		TreeSet<String> markedActual = new TreeSet<>();
		for (int i = 0; i < e.size(); i++) {
			if (!e.get(i).arrows.equals(a.get(i).arrows)) {
				return Verdict.FAIL;
			}
			sameLanes &= Objects.equals(e.get(i).marked, a.get(i).marked);
			if (e.get(i).marked != null) {
				markedExpected.add(e.get(i).marked);
			}
			if (a.get(i).marked != null) {
				markedActual.add(a.get(i).marked);
			}
		}
		if (sameLanes) {
			return Verdict.OK;
		}
		// marking a different NUMBER of lanes that lead the same way is a judgement; marking lanes
		// that lead somewhere else is an error, and a driver following it ends up in the wrong lane
		return markedExpected.equals(markedActual) ? Verdict.SIMILAR : Verdict.FAIL;
	}

	static String reason(String expected, String actual) {
		if (expected == null) {
			return "";
		}
		if (actual == null) {
			return onlyCarriesOn(expected) ? "CARRYON" : "MISSING";
		}
		if (isEmpty(expected)) {
			return onlyCarriesOn(actual) ? "CARRYON" : "EXTRA";
		}
		Parsed e = Parsed.of(expected);
		Parsed a = Parsed.of(actual);
		Verdict turns = e.turn == null ? Verdict.OK : compareTurns(e.turn, a.turn);
		Verdict lanes = e.lanes == null ? Verdict.OK : compareLanes(e.lanes, a.lanes);
		if (turns == Verdict.FAIL) {
			return "TURN " + a.turn;
		}
		if (lanes == Verdict.FAIL) {
			List<LaneShape> expectedLanes = LaneShape.parse(e.lanes);
			List<LaneShape> actualLanes = LaneShape.parse(a.lanes);
			if (expectedLanes.size() != actualLanes.size()) {
				return "LANES";
			}
			for (int i = 0; i < expectedLanes.size(); i++) {
				if (!expectedLanes.get(i).arrows.equals(actualLanes.get(i).arrows)) {
					return "ARROWS"; // the lane carries different arrows, so nothing lines up
				}
			}
			return "MARKS"; // the same lanes, the wrong ones marked
		}
		if (turns == Verdict.SIMILAR) {
			return "TURN+-1";
		}
		if (lanes == Verdict.SIMILAR) {
			return "MARKS";
		}
		return "MUTE";
	}

	/** an expectation or a produced string, split into the three things it can carry */
	private static final class Parsed {
		final boolean mute;
		final String turn;
		final String lanes;

		private Parsed(boolean mute, String turn, String lanes) {
			this.mute = mute;
			this.turn = turn;
			this.lanes = lanes;
		}

		static Parsed of(String value) {
			boolean mute = value.startsWith(MUTE);
			String body = mute ? value.substring(MUTE.length()) : value;
			int colon = body.indexOf(':');
			if (colon >= 0) {
				return new Parsed(mute, body.substring(0, colon), body.substring(colon + 1));
			}
			boolean looksLikeLanes = body.indexOf('|') >= 0 || body.indexOf('+') >= 0 || body.indexOf(',') >= 0;
			return looksLikeLanes ? new Parsed(mute, null, body) : new Parsed(mute, body, null);
		}
	}

	/** one lane of either format: which arrows it carries, and which of them the route takes */
	private static final class LaneShape {
		final TreeSet<String> arrows = new TreeSet<>();
		String marked;

		static List<LaneShape> parse(String lanes) {
			List<LaneShape> parsed = new ArrayList<>();
			if (lanes == null || lanes.isEmpty()) {
				return parsed;
			}
			for (String lane : lanes.split("\\|", -1)) {
				LaneShape shape = new LaneShape();
				for (String arrow : lane.split(",", -1)) {
					boolean taken = arrow.startsWith("+");
					String code = taken ? arrow.substring(1) : arrow;
					shape.arrows.add(code);
					if (taken) {
						shape.marked = code;
					}
				}
				parsed.add(shape);
			}
			return parsed;
		}
	}

	private static Verdict worse(Verdict a, Verdict b) {
		return a.ordinal() >= b.ordinal() ? a : b;
	}

	private static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	private static String quote(String s) {
		return s == null ? "NULL" : "'" + s + "'";
	}

	private static void count(Verdict verdict, String reason) {
		TOTALS.put(verdict, TOTALS.containsKey(verdict) ? TOTALS.get(verdict) + 1 : 1);
		if (!reason.isEmpty()) {
			String key = reason.startsWith("TURN") ? "TURN" : reason;
			REASONS.put(key, REASONS.containsKey(key) ? REASONS.get(key) + 1 : 1);
		}
	}

	@AfterClass
	public static void report() {
		System.out.println();
		System.out.println("=== TurnPrepareAI against the recorded turn lanes ===");
		for (String line : REPORT) {
			System.out.println(line);
		}
		int skipped = total(Verdict.SKIP);
		int ok = total(Verdict.OK);
		int similar = total(Verdict.SIMILAR);
		int fail = total(Verdict.FAIL);
		System.out.printf("%nOK %d, SIMILAR %d, FAIL %d of %d judged expectations in %d cases"
						+ " (%d more reached but not judged)%n",
				ok, similar, fail, ok + similar + fail, CASE_COUNT, skipped);
		System.out.println("FAIL by cause: " + REASONS
				+ "  (MISSING no instruction produced, EXTRA one where none was expected,"
				+ " TURN a maneuver two steps away, MARKS the wrong lanes marked,"
				+ " ARROWS different arrows on a lane, LANES a different lane count,"
				+ " CARRYON one side shows a carry-on the other does not)");
		if (fail > MEASURED_FAIL) {
			System.out.printf("above the %d measured on 2026-09-11%n", MEASURED_FAIL);
		}
	}

	private static int total(Verdict verdict) {
		return TOTALS.containsKey(verdict) ? TOTALS.get(verdict) : 0;
	}

	// ------------------------------------------------------------------ what the route says

	private Map<String, String> collect(List<RouteSegmentResult> route) {
		Map<String, String> actual = new LinkedHashMap<>();
		for (RouteSegmentResult segment : route) {
			TurnTypeAI turn = segment.getTurnTypeAI();
			if (turn == null) {
				continue;
			}
			long id = ObfConstants.getOsmObjectId(segment.getObject());
			String formatted = format(turn);
			actual.put(id + ":" + segment.getStartPointIndex(), formatted);
			if (!actual.containsKey(String.valueOf(id))) {
				actual.put(String.valueOf(id), formatted);
			}
		}
		return actual;
	}

	/** every instruction the pass produced, in route order, so a missing one can be looked for */
	private String produced(List<RouteSegmentResult> route) {
		StringBuilder sb = new StringBuilder();
		for (RouteSegmentResult segment : route) {
			TurnTypeAI turn = segment.getTurnTypeAI();
			if (turn == null) {
				continue;
			}
			sb.append(sb.length() == 0 ? "" : ", ").append(ObfConstants.getOsmObjectId(segment.getObject()))
					.append(':').append(segment.getStartPointIndex()).append('=').append(format(turn));
		}
		return sb.toString();
	}

	private String key(long id, int startPoint, Map<String, String> actual) {
		String withPoint = id + ":" + startPoint;
		return startPoint >= 0 && actual.containsKey(withPoint) ? withPoint : String.valueOf(id);
	}

	private String format(TurnTypeAI turn) {
		StringBuilder sb = new StringBuilder();
		if (turn.skipToSpeak()) {
			sb.append(MUTE);
		}
		String maneuver = turn.getOldTurnType().toXmlString();
		sb.append(maneuver);
		String lanes = lanesOf(turn);
		if (!lanes.isEmpty()) {
			sb.append(':').append(lanes);
		}
		return sb.toString();
	}

	/**
	 * The lane row. {@code +} goes on the arrow the route takes, which the model records when it
	 * marks the lane rather than leaving it to be guessed from the maneuver. This is also the
	 * place a profile-aware code would be added: the panel knows whose lane each one is.
	 */
	private String lanesOf(TurnTypeAI turn) {
		LanePanelAI panel = turn.panel();
		if (panel.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (LanePanelAI.Lane lane : panel.lanes()) {
			if (lane.index() > 0) {
				sb.append('|');
			}
			List<TurnIndication> arrows = lane.arrows();
			if (arrows.isEmpty()) {
				sb.append(lane.isActive() ? "+C" : "C");
				continue;
			}
			for (int i = 0; i < arrows.size(); i++) {
				boolean taken = lane.isActive() && (arrows.get(i) == lane.taken()
						|| (lane.taken() == null && i == 0));
				sb.append(i > 0 ? "," : "").append(taken ? "+" : "").append(code(arrows.get(i)));
			}
		}
		return sb.toString();
	}

	private String code(TurnIndication arrow) {
		switch (arrow) {
			case LEFT:
				return "TL";
			case SLIGHT_LEFT:
				return "TSLL";
			case SHARP_LEFT:
				return "TSHL";
			case RIGHT:
				return "TR";
			case SLIGHT_RIGHT:
				return "TSLR";
			case SHARP_RIGHT:
				return "TSHR";
			case REVERSE:
				return "TU";
			default:
				return "C";
		}
	}

	// ------------------------------------------------------------------ the route itself

	private static RoutingContext ctx;

	private List<RouteSegmentResult> route() throws IOException, InterruptedException {
		File map = new File(MAP);
		RandomAccessFile raf = new RandomAccessFile(map, "r");
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		Map<String, String> params = te.getParams() == null ? new HashMap<String, String>() : te.getParams();
		params.put("car", "true");
		RoutingMemoryLimits limits = new RoutingMemoryLimits(
				RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", limits, params);
		BinaryMapIndexReader[] readers = {new BinaryMapIndexReader(raf, map)};
		ctx = fe.buildRoutingContext(config, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		ctx.leftSideNavigation = false;
		return fe.searchRoute(ctx, te.getStartPoint(), te.getEndPoint(), null).detailed;
	}

	private RoutingContext context() {
		return ctx;
	}
}
