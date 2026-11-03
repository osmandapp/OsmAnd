package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.osmand.shared.routing.TurnType;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Random;

/**
 * {@link TurnType} is a copy of {@link net.osmand.router.TurnType}; this runs the two side by side.
 *
 * Everything a turn type does is arithmetic on small ints - the manoeuvre code, the packing of up to
 * three turns and an active bit into one int per lane, and the string forms of both - so the check
 * is exhaustive over the codes and randomised over the lane edits.
 */
public class TurnTypeCompatTest {

	private static final int CODES = 20; // every defined code is below 15; the rest is how unknown codes are treated

	private static final String[] NAMES = {"C", "TL", "TSLL", "TSHL", "TR", "TSLR", "TSHR", "KL", "KR", "TU",
			"TRU", "OFFR", "RNDB", "RNLB", "RNDB1", "RNDB3", "RNLB2", "RNDB12", "junk", "", "rndb2", "c"};

	private static final String[] LANE_WORDS = {"left", "through", "right", "slight_left", "slight_right",
			"sharp_left", "sharp_right", "reverse", "merge_to_left", "merge_to_right", "none", "", "junk", "LEFT"};

	@Test
	public void testCodePredicatesAgree() {
		for (int t = -1; t <= CODES; t++) {
			String m = "code " + t;
			assertEquals(m, net.osmand.router.TurnType.isLeftTurn(t), TurnType.isLeftTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isLeftTurnNoUTurn(t), TurnType.isLeftTurnNoUTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isRightTurn(t), TurnType.isRightTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isRightTurnNoUTurn(t), TurnType.isRightTurnNoUTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isSlightTurn(t), TurnType.isSlightTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isKeepDirectionTurn(t), TurnType.isKeepDirectionTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isSharpOrReverse(t), TurnType.isSharpOrReverse(t));
			assertEquals(m, net.osmand.router.TurnType.isSharpLeftOrUTurn(t), TurnType.isSharpLeftOrUTurn(t));
			assertEquals(m, net.osmand.router.TurnType.isSharpRightOrUTurn(t), TurnType.isSharpRightOrUTurn(t));
			assertEquals(m, net.osmand.router.TurnType.orderFromLeftToRight(t), TurnType.orderFromLeftToRight(t));
			assertEquals(m, net.osmand.router.TurnType.getPrev(t), TurnType.getPrev(t));
			assertEquals(m, net.osmand.router.TurnType.getNext(t), TurnType.getNext(t));
		}
		for (String w : LANE_WORDS) {
			assertEquals(w, net.osmand.router.TurnType.convertType(w), TurnType.convertType(w));
		}
	}

	@Test
	public void testInstancesAgreeOnEveryCodeAndName() {
		for (int t = 0; t <= CODES; t++) {
			for (boolean left : new boolean[] {false, true}) {
				assertSame("valueOf(" + t + ", " + left + ")",
						net.osmand.router.TurnType.valueOf(t, left), TurnType.valueOf(t, left));
			}
		}
		for (String s : NAMES) {
			for (boolean left : new boolean[] {false, true}) {
				assertSame("fromString(" + s + ", " + left + ")",
						net.osmand.router.TurnType.fromString(s, left), TurnType.fromString(s, left));
			}
		}
		for (int out = 0; out <= 6; out++) {
			for (float angle : new float[] {-179, -90, -30, 0, 30, 90, 179}) {
				for (boolean left : new boolean[] {false, true}) {
					assertSame("getExitTurn(" + out + ", " + angle + ", " + left + ")",
							net.osmand.router.TurnType.getExitTurn(out, angle, left), TurnType.getExitTurn(out, angle, left));
				}
			}
		}
		assertSame("straight", net.osmand.router.TurnType.straight(), TurnType.straight());
	}

	@Test
	public void testLanePackingAgreesUnderRandomEdits() {
		Random random = new Random(20260913);
		for (int n = 0; n < 4000; n++) {
			int[] a = new int[1 + random.nextInt(6)];
			int[] b = new int[a.length];
			for (int k = 0; k < 12; k++) {
				int lane = random.nextInt(a.length);
				int turn = random.nextInt(16);
				switch (random.nextInt(7)) {
					case 0: net.osmand.router.TurnType.setPrimaryTurnAndReset(a, lane, turn); TurnType.setPrimaryTurnAndReset(b, lane, turn); break;
					case 1: net.osmand.router.TurnType.setPrimaryTurn(a, lane, turn); TurnType.setPrimaryTurn(b, lane, turn); break;
					case 2: net.osmand.router.TurnType.setSecondaryTurn(a, lane, turn); TurnType.setSecondaryTurn(b, lane, turn); break;
					case 3: net.osmand.router.TurnType.setTertiaryTurn(a, lane, turn); TurnType.setTertiaryTurn(b, lane, turn); break;
					case 4: net.osmand.router.TurnType.setPrimaryTurnShiftOthers(a, lane, turn); TurnType.setPrimaryTurnShiftOthers(b, lane, turn); break;
					case 5: net.osmand.router.TurnType.setSecondaryToPrimary(a, lane); TurnType.setSecondaryToPrimary(b, lane); break;
					default: net.osmand.router.TurnType.setTertiaryToPrimary(a, lane); TurnType.setTertiaryToPrimary(b, lane); break;
				}
				if (random.nextInt(3) == 0) {
					a[lane] |= 1;
					b[lane] |= 1;
				}
				assertArrayEquals(a, b);
				for (int i = 0; i < a.length; i++) {
					String m = "lane " + a[i];
					assertEquals(m, net.osmand.router.TurnType.getPrimaryTurn(a[i]), TurnType.getPrimaryTurn(b[i]));
					assertEquals(m, net.osmand.router.TurnType.getSecondaryTurn(a[i]), TurnType.getSecondaryTurn(b[i]));
					assertEquals(m, net.osmand.router.TurnType.getTertiaryTurn(a[i]), TurnType.getTertiaryTurn(b[i]));
					assertEquals(m, net.osmand.router.TurnType.hasAnySlightTurnLane(a[i]), TurnType.hasAnySlightTurnLane(b[i]));
					assertEquals(m, net.osmand.router.TurnType.hasAnyTurnLane(a[i], turn), TurnType.hasAnyTurnLane(b[i], turn));
					LinkedHashSet<Integer> js = new LinkedHashSet<>();
					LinkedHashSet<Integer> ks = new LinkedHashSet<>();
					net.osmand.router.TurnType.collectTurnTypes(a[i], js);
					TurnType.collectTurnTypes(b[i], ks);
					assertEquals(m, new java.util.ArrayList<>(js), new java.util.ArrayList<>(ks));
				}
			}
			String js = net.osmand.router.TurnType.lanesToString(a);
			assertEquals(js, TurnType.lanesToString(b));
			Same.outcome(js, () -> net.osmand.router.TurnType.lanesFromString(js), () -> TurnType.lanesFromString(js));

			// a turn carrying these lanes counts and describes them the same way
			int code = random.nextInt(15);
			int type = random.nextInt(15);
			net.osmand.router.TurnType jt = net.osmand.router.TurnType.valueOf(code, random.nextBoolean());
			TurnType kt = TurnType.valueOf(jt.getValue(), jt.isLeftSide());
			jt.setLanes(a);
			kt.setLanes(b);
			jt.setTurnAngle(random.nextInt(360) - 180);
			kt.setTurnAngle(jt.getTurnAngle());
			jt.setSkipToSpeak(random.nextBoolean());
			kt.setSkipToSpeak(jt.isSkipToSpeak());
			assertSame("with lanes " + js, jt, kt);
			assertEquals(js, jt.countTurnTypeDirections(type, true), kt.countTurnTypeDirections(type, true));
			assertEquals(js, jt.countTurnTypeDirections(type, false), kt.countTurnTypeDirections(type, false));
		}
	}

	@Test
	public void testLanesFromStringAgreesOnOddInput() {
		for (String s : new String[] {"", "|", "||", "C", "TL", "C|TR", "C|", "|C", "C||TR", "+C,TL|TR", "C,TL,TR,KL", ",C", "C,", "junk", "C|junk|TR", " C | TR ", "+", "+|+"}) {
			Same.outcome(s, () -> net.osmand.router.TurnType.lanesFromString(s), () -> TurnType.lanesFromString(s));
		}
	}

	private static void assertSame(String m, net.osmand.router.TurnType j, TurnType k) {
		if (j == null || k == null) {
			assertEquals(m, j == null, k == null);
			return;
		}
		assertEquals(m + " value", j.getValue(), k.getValue());
		assertEquals(m + " exitOut", j.getExitOut(), k.getExitOut());
		assertEquals(m + " turnAngle", j.getTurnAngle(), k.getTurnAngle(), 0f);
		assertEquals(m + " skipToSpeak", j.isSkipToSpeak(), k.isSkipToSpeak());
		assertEquals(m + " leftSide", j.isLeftSide(), k.isLeftSide());
		assertEquals(m + " roundabout", j.isRoundAbout(), k.isRoundAbout());
		assertEquals(m + " keepLeft", j.keepLeft(), k.keepLeft());
		assertEquals(m + " keepRight", j.keepRight(), k.keepRight());
		assertEquals(m + " goAhead", j.goAhead(), k.goAhead());
		assertEquals(m + " possibleLeft", j.isPossibleLeftTurn(), k.isPossibleLeftTurn());
		assertEquals(m + " possibleRight", j.isPossibleRightTurn(), k.isPossibleRightTurn());
		assertArrayEquals(m + " lanes", j.getLanes(), k.getLanes());
		Same.outcome(m + " countDirections", j::countDirections, k::countDirections);
		Same.outcome(m + " activeCommonLaneTurn", j::getActiveCommonLaneTurn, k::getActiveCommonLaneTurn);
		assertEquals(m + " xml", j.toXmlString(), k.toXmlString());
		if (!j.toString().contains("@")) { // java falls back to Object.toString for a code it has no words for
			assertEquals(m + " toString", j.toString(), k.toString());
		}
	}
}
