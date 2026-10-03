package net.osmand.plus.routing;

import static net.osmand.plus.routing.SuppressedRecalculationPrompt.DEVIATION_END_FOLLOW_TIME;
import static net.osmand.plus.routing.SuppressedRecalculationPrompt.DEVIATION_END_INTERVAL;
import static net.osmand.plus.routing.SuppressedRecalculationPrompt.MAX_SUPPRESSED_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SuppressedRecalculationPromptTest {

	private static final long START = 1_000_000;
	// BRouter recalculates every ~6 s while the rider is off the route
	private static final long RECALCULATION_INTERVAL = 6000;

	@Test
	public void singleBackwardRecalculationIsSilent() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START));
	}

	@Test
	public void announcedOnceWhenBackwardRoutesKeepComing() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 10));
	}

	@Test
	public void announcedAfterMaxSuppressedTime() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START));
		assertFalse(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME));
		assertTrue(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME + 1));
	}

	@Test
	public void forwardRouteStartsNewDeviation() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 5));

		prompt.reset();
		long next = START + 5 * RECALCULATION_INTERVAL;
		assertFalse(prompt.shouldAnnounce(next));
		assertEquals(1, countAnnouncements(prompt, next, 5));
	}

	@Test
	public void followedRouteStartsNewDeviation() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 5));

		long next = START + 5 * RECALCULATION_INTERVAL;
		long followed = START + 5 * RECALCULATION_INTERVAL - DEVIATION_END_FOLLOW_TIME - 1000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME);
		assertFalse(prompt.shouldAnnounce(next));
		assertEquals(1, countAnnouncements(prompt, next, 5));
	}

	@Test
	public void followedRouteStartKeepsDeviation() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 5));

		long next = START + 5 * RECALCULATION_INTERVAL;
		long followed = START + 5 * RECALCULATION_INTERVAL - DEVIATION_END_FOLLOW_TIME - 1000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME - 1);
		assertEquals(0, countAnnouncements(prompt, next, 5));
	}

	@Test
	public void notFollowedRouteStartsFollowingOver() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 5));

		long next = START + 5 * RECALCULATION_INTERVAL;
		long followed = START + 5 * RECALCULATION_INTERVAL - DEVIATION_END_FOLLOW_TIME - 2000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME / 2);
		prompt.onRouteNotFollowed();
		// the time before the rider left the route does not count
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME / 2 + 1000);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME + 1000);
		assertEquals(0, countAnnouncements(prompt, next, 5));
	}

	@Test
	public void singleBackwardRouteBeforeFollowedRouteStaysSilent() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START));

		prompt.onRouteFollowed(START + 1000);
		prompt.onRouteFollowed(START + 1000 + DEVIATION_END_FOLLOW_TIME);
		// a new deviation after the rider was back on the route starts its own 15 s
		assertFalse(prompt.shouldAnnounce(START + 1000 + DEVIATION_END_FOLLOW_TIME + 2 * MAX_SUPPRESSED_TIME));
	}

	@Test
	public void resetDoesNotAnnounceSingleBackwardRecalculation() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START));

		prompt.reset();
		assertFalse(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME * 2));
	}

	@Test
	public void longPauseStartsNewDeviation() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertEquals(1, countAnnouncements(prompt, START, 5));

		long last = START + 4 * RECALCULATION_INTERVAL;
		long next = last + DEVIATION_END_INTERVAL + 1;
		assertFalse(prompt.shouldAnnounce(next));
		assertEquals(1, countAnnouncements(prompt, next, 5));
	}

	private static int countAnnouncements(@NonNull SuppressedRecalculationPrompt prompt, long start, int recalculations) {
		int count = 0;
		for (int i = 0; i < recalculations; i++) {
			if (prompt.shouldAnnounce(start + i * RECALCULATION_INTERVAL)) {
				count++;
			}
		}
		return count;
	}
}
