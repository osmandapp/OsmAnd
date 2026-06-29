/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks continuous driving time and distance per travel mode.
 * Port of driver_break_check_driving_time and hiking/cycling distance logic in driver_break.c.
 */
public class BreakTimer {

	private static final Log LOG = PlatformUtil.getLog(BreakTimer.class);

	private static final double EARTH_RADIUS_M = 6371000.0;

	/** Minimum movement between location fixes to accumulate distance (m). */
	private static final double MIN_DISTANCE_DELTA_M = 1.0;

	public enum BreakEvent {
		/** Configurable soft warning (car soft limit, motorcycle soft limit). */
		SOFT_LIMIT,
		/** Mandatory stop required (truck, motorcycle). */
		MANDATORY,
		/** Break timer elapsed; stop recommended or required by distance (car interval, hiking/cycling). */
		BREAK_DUE
	}

	public interface BreakEventListener {
		void onBreakEvent(@NonNull BreakEvent event, @NonNull TravelMode mode);
	}

	public interface ThresholdProvider {
		int getCarSoftLimitHours();

		int getCarBreakIntervalHours();

		int getTruckMandatoryBreakHours();

		int getMotorcycleSoftLimitMinutes();

		int getMotorcycleMandatoryBreakMinutes();

		double getHikingMainDistKm();

		double getCyclingMainDistKm();
	}

	private final List<BreakEventListener> listeners = new ArrayList<>();
	private final ThresholdProvider thresholds;

	private TravelMode mode = TravelMode.CAR;
	private long drivingStartElapsedMs;
	private long lastBreakElapsedMs;
	private long accumulatedDistanceM;
	private boolean breakInProgress;
	private Location lastLocation;

	private boolean softLimitFired;
	private boolean mandatoryFired;
	private boolean breakDueFired;

	/**
	 * @param thresholds supplies mode-specific limits from {@link DriverBreakSettings}
	 */
	public BreakTimer(@NonNull ThresholdProvider thresholds) {
		this.thresholds = thresholds;
		long now = SystemClock.elapsedRealtime();
		drivingStartElapsedMs = now;
		lastBreakElapsedMs = now;
	}

	/**
	 * Called on each location fix while navigation is active.
	 *
	 * @param location current GPS fix
	 */
	public void onLocationUpdate(@NonNull Location location) {
		if (breakInProgress) {
			lastLocation = location;
			return;
		}
		if (lastLocation != null && location.hasAccuracy()) {
			float delta = lastLocation.distanceTo(location);
			if (delta >= MIN_DISTANCE_DELTA_M) {
				accumulatedDistanceM += delta;
				checkDistanceEvents();
			}
		}
		lastLocation = location;
		checkTimeEvents();
	}

	/** Begin a rest break; pauses driving-time accumulation. */
	public void startBreak() {
		breakInProgress = true;
	}

	/** End a rest break; resets continuous driving time and distance counters. */
	public void endBreak() {
		breakInProgress = false;
		long now = SystemClock.elapsedRealtime();
		lastBreakElapsedMs = now;
		accumulatedDistanceM = 0;
		softLimitFired = false;
		mandatoryFired = false;
		breakDueFired = false;
	}

	/**
	 * @return current timer snapshot; never null
	 */
	@NonNull
	public BreakStatus getStatus() {
		long continuousMs = breakInProgress ? 0 : SystemClock.elapsedRealtime() - lastBreakElapsedMs;
		long nextDue = computeNextBreakDueMs(continuousMs);
		return new BreakStatus(continuousMs, accumulatedDistanceM, breakInProgress, mode, nextDue);
	}

	public void setMode(@NonNull TravelMode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			endBreak();
			LOG.info("DriverBreak: travel mode set to " + mode.getConfigKey());
		}
	}

	public void addListener(@NonNull BreakEventListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(@NonNull BreakEventListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Restore persisted session state after app restart.
	 */
	public void restoreState(long lastBreakElapsedMs, long accumulatedDistanceM, boolean breakInProgress,
			@NonNull TravelMode mode) {
		this.lastBreakElapsedMs = lastBreakElapsedMs;
		this.accumulatedDistanceM = accumulatedDistanceM;
		this.breakInProgress = breakInProgress;
		this.mode = mode;
		checkDistanceEvents();
		checkTimeEvents();
	}

	public long getLastBreakElapsedMs() {
		return lastBreakElapsedMs;
	}

	public long getAccumulatedDistanceM() {
		return accumulatedDistanceM;
	}

	public boolean isBreakInProgress() {
		return breakInProgress;
	}

	@NonNull
	public TravelMode getMode() {
		return mode;
	}

	private void checkTimeEvents() {
		if (breakInProgress) {
			return;
		}
		long continuousMs = SystemClock.elapsedRealtime() - lastBreakElapsedMs;
		long continuousMin = continuousMs / 60000L;

		switch (mode) {
			case CAR:
				if (!softLimitFired && continuousMin >= thresholds.getCarSoftLimitHours() * 60L) {
					fire(BreakEvent.SOFT_LIMIT);
					softLimitFired = true;
				}
				if (!breakDueFired && continuousMin >= thresholds.getCarBreakIntervalHours() * 60L) {
					fire(BreakEvent.BREAK_DUE);
					breakDueFired = true;
				}
				break;
			case TRUCK:
				if (!mandatoryFired && continuousMin >= thresholds.getTruckMandatoryBreakHours() * 60L) {
					fire(BreakEvent.MANDATORY);
					mandatoryFired = true;
				}
				break;
			case MOTORCYCLE:
				if (!softLimitFired && continuousMin >= thresholds.getMotorcycleSoftLimitMinutes()) {
					fire(BreakEvent.SOFT_LIMIT);
					softLimitFired = true;
				}
				if (!mandatoryFired && continuousMin >= thresholds.getMotorcycleMandatoryBreakMinutes()) {
					fire(BreakEvent.MANDATORY);
					mandatoryFired = true;
				}
				break;
			case HIKING:
			case CYCLING:
				break;
			default:
				break;
		}
	}

	private void checkDistanceEvents() {
		if (mode != TravelMode.HIKING && mode != TravelMode.CYCLING) {
			return;
		}
		double thresholdM = mode == TravelMode.HIKING
				? thresholds.getHikingMainDistKm() * 1000.0
				: thresholds.getCyclingMainDistKm() * 1000.0;
		if (!breakDueFired && accumulatedDistanceM >= thresholdM) {
			fire(BreakEvent.BREAK_DUE);
			breakDueFired = true;
		}
	}

	private long computeNextBreakDueMs(long continuousMs) {
		switch (mode) {
			case CAR:
				return Math.max(0L, thresholds.getCarBreakIntervalHours() * 3600000L - continuousMs);
			case TRUCK:
				return Math.max(0L, thresholds.getTruckMandatoryBreakHours() * 3600000L - continuousMs);
			case MOTORCYCLE:
				return Math.max(0L, thresholds.getMotorcycleMandatoryBreakMinutes() * 60000L - continuousMs);
			case HIKING:
				return Math.max(0L, (long) (thresholds.getHikingMainDistKm() * 1000.0 - accumulatedDistanceM));
			case CYCLING:
				return Math.max(0L, (long) (thresholds.getCyclingMainDistKm() * 1000.0 - accumulatedDistanceM));
			default:
				return 0L;
		}
	}

	private void fire(@NonNull BreakEvent event) {
		LOG.info("DriverBreak: break event " + event + " for mode " + mode.getConfigKey());
		for (BreakEventListener listener : new ArrayList<>(listeners)) {
			listener.onBreakEvent(event, mode);
		}
	}
}
