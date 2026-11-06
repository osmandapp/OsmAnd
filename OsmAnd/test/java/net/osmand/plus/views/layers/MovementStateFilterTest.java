package net.osmand.plus.views.layers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MovementStateFilterTest {

	private static final long SEC = 1000;

	@Test
	public void noisySpeedAtRestStaysStationary() {
		MovementStateFilter filter = new MovementStateFilter();
		float[] speeds = {0.05f, 0.15f, 0.3f, 0.08f, 0.6f, 0.12f, 0.25f, 0.0f, 0.4f, 0.1f, 0.18f, 0.9f, 0.05f};
		int flips = 0;
		boolean prev = false;
		for (int i = 0; i < speeds.length; i++) {
			boolean moving = filter.update(i * SEC, speeds[i]);
			if (moving != prev) {
				flips++;
			}
			prev = moving;
		}
		assertEquals(0, flips);
	}

	@Test
	public void walkingStartsAndStopsMovement() {
		MovementStateFilter filter = new MovementStateFilter();
		long t = 0;
		assertFalse(filter.update(t, 0f));
		int secondsToStart = 0;
		while (!filter.update(t += SEC, 1.4f)) {
			secondsToStart++;
		}
		assertTrue("walking detected in " + secondsToStart + " s", secondsToStart <= 2);
		for (int i = 0; i < 10; i++) {
			assertTrue(filter.update(t += SEC, 1.4f));
		}
		int secondsToStop = 0;
		while (filter.update(t += SEC, 0.05f)) {
			secondsToStop++;
		}
		assertTrue("stop detected in " + secondsToStop + " s", secondsToStop <= 5);
	}

	@Test
	public void slowWalkAroundThresholdKeepsMoving() {
		MovementStateFilter filter = new MovementStateFilter();
		long t = 0;
		while (!filter.update(t += SEC, 1.0f)) ;
		float[] speeds = {0.4f, 0.3f, 0.6f, 0.25f, 0.5f, 0.35f};
		for (float speed : speeds) {
			assertTrue(filter.update(t += SEC, speed));
		}
	}

	@Test
	public void sameFixDoesNotChangeState() {
		MovementStateFilter filter = new MovementStateFilter();
		filter.update(0, 0f);
		for (int i = 0; i < 100; i++) {
			assertFalse(filter.update(SEC, 1.0f));
		}
	}

	@Test
	public void longGapResetsToCurrentSpeed() {
		MovementStateFilter filter = new MovementStateFilter();
		filter.update(0, 0f);
		assertTrue(filter.update(60 * SEC, 2f));
		assertFalse(filter.update(120 * SEC, 0f));
	}
}
