package net.osmand.plus.plugins.driverbreak;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.Location;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link BreakTimer} thresholds from driver_break_config_default().
 */
@RunWith(AndroidJUnit4.class)
public class BreakTimerTest {

	@Test
	public void carSoftLimitFiresAtSevenHours() {
		TestThresholds thresholds = new TestThresholds();
		BreakTimer timer = new BreakTimer(thresholds);
		RecordingListener listener = new RecordingListener();
		timer.addListener(listener);
		timer.setMode(TravelMode.CAR);
		long now = SystemClock.elapsedRealtime();
		timer.restoreState(now - (7L * 60L * 60L * 1000L), 0, false, TravelMode.CAR);
		Location location = new Location("test");
		location.setAccuracy(10f);
		timer.onLocationUpdate(location);
		Assert.assertTrue(listener.softLimit);
	}

	@Test
	public void truckMandatoryAtFourHours() {
		TestThresholds thresholds = new TestThresholds();
		BreakTimer timer = new BreakTimer(thresholds);
		RecordingListener listener = new RecordingListener();
		timer.addListener(listener);
		timer.setMode(TravelMode.TRUCK);
		long now = SystemClock.elapsedRealtime();
		timer.restoreState(now - (4L * 60L * 60L * 1000L), 0, false, TravelMode.TRUCK);
		Location location = new Location("test");
		location.setAccuracy(10f);
		timer.onLocationUpdate(location);
		Assert.assertTrue(listener.mandatory);
	}

	@Test
	public void hikingDistanceBreakDue() {
		TestThresholds thresholds = new TestThresholds();
		BreakTimer timer = new BreakTimer(thresholds);
		RecordingListener listener = new RecordingListener();
		timer.addListener(listener);
		timer.setMode(TravelMode.HIKING);
		timer.restoreState(SystemClock.elapsedRealtime(), (long) (11.295 * 1000.0), false, TravelMode.HIKING);
		Assert.assertTrue(listener.breakDue);
	}

	@Test
	public void endBreakResetsAccumulators() {
		TestThresholds thresholds = new TestThresholds();
		BreakTimer timer = new BreakTimer(thresholds);
		timer.restoreState(SystemClock.elapsedRealtime() - 3600000L, 5000, false, TravelMode.CAR);
		timer.startBreak();
		timer.endBreak();
		BreakStatus status = timer.getStatus();
		Assert.assertEquals(0, status.getContinuousDrivingMs());
		Assert.assertEquals(0, status.getAccumulatedDistanceM());
	}

	@Test
	public void setModeResetsState() {
		TestThresholds thresholds = new TestThresholds();
		BreakTimer timer = new BreakTimer(thresholds);
		timer.setMode(TravelMode.CAR);
		timer.restoreState(SystemClock.elapsedRealtime() - 7200000L, 9000, false, TravelMode.CAR);
		timer.setMode(TravelMode.TRUCK);
		Assert.assertEquals(0, timer.getStatus().getContinuousDrivingMs());
	}

	private static final class TestThresholds implements BreakTimer.ThresholdProvider {
		@Override
		public int getCarSoftLimitHours() {
			return 7;
		}

		@Override
		public int getCarBreakIntervalHours() {
			return 4;
		}

		@Override
		public int getTruckMandatoryBreakHours() {
			return 4;
		}

		@Override
		public int getMotorcycleSoftLimitMinutes() {
			return 120;
		}

		@Override
		public int getMotorcycleMandatoryBreakMinutes() {
			return 210;
		}

		@Override
		public double getHikingMainDistKm() {
			return 11.295;
		}

		@Override
		public double getCyclingMainDistKm() {
			return 28.24;
		}
	}

	private static final class RecordingListener implements BreakTimer.BreakEventListener {
		boolean softLimit;
		boolean mandatory;
		boolean breakDue;

		@Override
		public void onBreakEvent(BreakTimer.BreakEvent event, TravelMode mode) {
			if (event == BreakTimer.BreakEvent.SOFT_LIMIT) {
				softLimit = true;
			} else if (event == BreakTimer.BreakEvent.MANDATORY) {
				mandatory = true;
			} else if (event == BreakTimer.BreakEvent.BREAK_DUE) {
				breakDue = true;
			}
		}
	}
}
