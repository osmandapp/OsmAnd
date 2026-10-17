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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SuppressedRecalculationPromptTest {

	private static final long START = 1_000_000;

	@Test
	public void forwardRecalculationIsAnnounced() {
		assertTrue(new SuppressedRecalculationPrompt().shouldAnnounce(START, false));
	}

	@Test
	public void backwardRecalculationsAnnouncedAfterMaxSuppressedTime() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START, true));
		assertFalse(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME, true));
		assertTrue(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME + 1, true));
	}

	@Test
	public void forwardRouteStartsNewSuppression() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START, true));
		assertTrue(prompt.shouldAnnounce(START + 1000, false));
		assertFalse(prompt.shouldAnnounce(START + MAX_SUPPRESSED_TIME + 2000, true));
	}

	@Test
	public void longPauseStartsNewSuppression() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START, true));
		assertFalse(prompt.shouldAnnounce(START + DEVIATION_END_INTERVAL + 1, true));
	}

	@Test
	public void promptIntervalGrowsUpToMax() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		List<Long> announced = new ArrayList<>();
		for (long second = 0; second <= 1000; second++) {
			if (prompt.shouldAnnounce(START + second * 1000, false)) {
				announced.add(second);
			}
		}
		assertEquals(Arrays.asList(0L, 5L, 18L, 50L, 129L, 325L, 625L, 925L), announced);
	}

	@Test
	public void followedRouteResetsPromptInterval() {
		SuppressedRecalculationPrompt prompt = announcedThreeTimes();
		long followed = START + 19000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME);
		assertTrue(prompt.shouldAnnounce(START + 35000, false));
		assertFalse(prompt.shouldAnnounce(START + 36000, false));
	}

	@Test
	public void shortFollowKeepsPromptInterval() {
		SuppressedRecalculationPrompt prompt = announcedThreeTimes();
		long followed = START + 19000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME - 1);
		assertFalse(prompt.shouldAnnounce(START + 35000, false));
	}

	@Test
	public void leavingRouteRestartsFollowTime() {
		SuppressedRecalculationPrompt prompt = announcedThreeTimes();
		long followed = START + 19000;
		prompt.onRouteFollowed(followed);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME / 2);
		prompt.onRouteNotFollowed();
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME / 2 + 1000);
		prompt.onRouteFollowed(followed + DEVIATION_END_FOLLOW_TIME);
		assertFalse(prompt.shouldAnnounce(START + 35000, false));
	}

	@Test
	public void newRouteResetsPromptInterval() {
		SuppressedRecalculationPrompt prompt = announcedThreeTimes();
		prompt.reset();
		assertTrue(prompt.shouldAnnounce(START + 19000, false));
	}

	@Test
	public void singleBackwardRouteBeforeFollowedRouteStaysSilent() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertFalse(prompt.shouldAnnounce(START, true));

		prompt.onRouteFollowed(START + 1000);
		prompt.onRouteFollowed(START + 1000 + DEVIATION_END_FOLLOW_TIME);
		assertFalse(prompt.shouldAnnounce(START + 1000 + DEVIATION_END_FOLLOW_TIME + 2 * MAX_SUPPRESSED_TIME, true));
	}

	@NonNull
	private static SuppressedRecalculationPrompt announcedThreeTimes() {
		SuppressedRecalculationPrompt prompt = new SuppressedRecalculationPrompt();
		assertTrue(prompt.shouldAnnounce(START, false));
		assertTrue(prompt.shouldAnnounce(START + 5000, false));
		assertTrue(prompt.shouldAnnounce(START + 18000, false));
		return prompt;
	}
}
