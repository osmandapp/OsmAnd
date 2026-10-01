package net.osmand.plus.routing;

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
