package net.osmand.router;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.osmand.shared.routing.TurnLanes;
import net.osmand.shared.routing.TurnType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The lane logic moved to OsmAnd-shared, and this is the check that it still says what java said.
 *
 * Only the string handling is reproduced here, because that is where the two languages disagree
 * quietly: java's {@code split(regex)} drops trailing empty parts and Kotlin's does not, so
 * {@code "left;"} would gain a lane, and {@code "left||"} would lose two. Everything else in the
 * port is arithmetic on ints, which a behaviour test can pin; a split that is wrong by one empty
 * string produces a lane array of the wrong length and a turn arrow that is off by a lane.
 *
 * The java originals below are the code as it stood before the move, kept only for the comparison.
 */
public class TurnLanesCompatTest {

	/** java's calculateRawTurnLanes, verbatim from before the port. */
	private static int[] javaCalculateRawTurnLanes(String turnLanes, int calcTurnType) {
		String[] splitLaneOptions = turnLanes.split("\\|", -1);
		int[] lanes = new int[splitLaneOptions.length];
		for (int i = 0; i < splitLaneOptions.length; i++) {
			String[] laneOptions = splitLaneOptions[i].split(";");
			for (int j = 0; j < laneOptions.length; j++) {
				int turn = TurnType.convertType(laneOptions[j]);
				final int primary = TurnType.getPrimaryTurn(lanes[i]);
				if (primary == 0) {
					TurnType.setPrimaryTurnAndReset(lanes, i, turn);
				} else {
					if (turn == calcTurnType
							|| (TurnType.isRightTurn(calcTurnType) && TurnType.isRightTurn(turn))
							|| (TurnType.isLeftTurn(calcTurnType) && TurnType.isLeftTurn(turn))) {
						TurnType.setPrimaryTurnShiftOthers(lanes, i, turn);
					} else if (TurnType.getSecondaryTurn(lanes[i]) == 0) {
						TurnType.setSecondaryTurn(lanes, i, turn);
					} else if (TurnType.getTertiaryTurn(lanes[i]) == 0) {
						TurnType.setTertiaryTurn(lanes, i, turn);
					}
				}
			}
		}
		return lanes;
	}

	/** java's getUniqTurnTypes, verbatim from before the port. */
	private static int[] javaGetUniqTurnTypes(String turnLanes) {
		LinkedHashSet<Integer> turnTypes = new LinkedHashSet<>();
		String[] splitLaneOptions = turnLanes.split("\\|", -1);
		for (int i = 0; i < splitLaneOptions.length; i++) {
			String[] laneOptions = splitLaneOptions[i].split(";");
			for (int j = 0; j < laneOptions.length; j++) {
				turnTypes.add(TurnType.convertType(laneOptions[j]));
			}
		}
		int[] r = new int[turnTypes.size()];
		int i = 0;
		for (int t : turnTypes) {
			r[i++] = t;
		}
		return r;
	}

	private static final String[] FIXED = {
			"left|through|right",
			"left|through;right",
			"through|through;right|right",
			"slight_left|none|slight_right",
			"merge_to_left|through",
			"reverse;left|through",
			// the shapes that separate the two split rules
			"",
			"|",
			"||",
			"left|",
			"|left",
			"left||right",
			"left;",
			";left",
			"left;;",
			";;",
			"left;;right",
			"|;|",
			"none",
			"unknown_value",
			"through;through;through;through",
	};

	@Test
	public void testRawTurnLanesMatchTheJavaOriginalOnFixedStrings() {
		for (String s : FIXED) {
			for (int calc : new int[] {0, TurnType.C, TurnType.TL, TurnType.TR, TurnType.TSLL}) {
				assertArrayEquals("calculateRawTurnLanes(\"" + s + "\", " + calc + ")",
						javaCalculateRawTurnLanes(s, calc), TurnLanes.calculateRawTurnLanes(s, calc));
			}
		}
	}

	@Test
	public void testUniqTurnTypesMatchTheJavaOriginalOnFixedStrings() {
		for (String s : FIXED) {
			assertArrayEquals("getUniqTurnTypes(\"" + s + "\")",
					javaGetUniqTurnTypes(s), TurnLanes.getUniqTurnTypes(s));
		}
	}

	@Test
	public void testBothMatchTheJavaOriginalOnRandomStrings() {
		String[] words = {"left", "through", "right", "slight_left", "slight_right", "reverse",
				"merge_to_left", "merge_to_right", "none", "", "junk"};
		Random random = new Random(20260910);
		for (int n = 0; n < 20000; n++) {
			int lanes = random.nextInt(5);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < lanes; i++) {
				if (i > 0) {
					sb.append('|');
				}
				int opts = random.nextInt(3);
				for (int j = 0; j < opts; j++) {
					if (j > 0) {
						sb.append(';');
					}
					sb.append(words[random.nextInt(words.length)]);
				}
				if (random.nextInt(6) == 0) {
					sb.append(';');
				}
			}
			String s = sb.toString();
			int calc = new int[] {0, TurnType.C, TurnType.TL, TurnType.TR}[random.nextInt(4)];
			assertArrayEquals("calculateRawTurnLanes(\"" + s + "\", " + calc + ")",
					javaCalculateRawTurnLanes(s, calc), TurnLanes.calculateRawTurnLanes(s, calc));
			assertArrayEquals("getUniqTurnTypes(\"" + s + "\")",
					javaGetUniqTurnTypes(s), TurnLanes.getUniqTurnTypes(s));
		}
	}

	/** java's getPossibleTurns ordered by orderFromLeftToRight; the port must keep that order. */
	@Test
	public void testPossibleTurnsComeOutLeftToRight() {
		int[] lanes = TurnLanes.calculateRawTurnLanes("right|through|left", 0);
		Integer[] turns = TurnLanes.getPossibleTurns(lanes, false, false);
		List<Integer> order = new ArrayList<>();
		for (Integer t : turns) {
			order.add(TurnType.orderFromLeftToRight(t));
		}
		for (int i = 1; i < order.size(); i++) {
			assertEquals("turns must be sorted left to right", true, order.get(i - 1) <= order.get(i));
		}
		Set<Integer> unique = new LinkedHashSet<>();
		for (Integer t : turns) {
			unique.add(t);
		}
		assertEquals("no duplicates", unique.size(), turns.length);
	}
}
