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

import androidx.annotation.NonNull;

/**
 * Snapshot of break timer state for widgets and settings.
 */
public class BreakStatus {

	private final long continuousDrivingMs;
	private final long accumulatedDistanceM;
	private final boolean breakInProgress;
	private final TravelMode mode;
	private final long nextBreakDueMs;

	public BreakStatus(long continuousDrivingMs, long accumulatedDistanceM, boolean breakInProgress,
			@NonNull TravelMode mode, long nextBreakDueMs) {
		this.continuousDrivingMs = continuousDrivingMs;
		this.accumulatedDistanceM = accumulatedDistanceM;
		this.breakInProgress = breakInProgress;
		this.mode = mode;
		this.nextBreakDueMs = nextBreakDueMs;
	}

	public long getContinuousDrivingMs() {
		return continuousDrivingMs;
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

	/**
	 * @return milliseconds until next break threshold, or 0 when overdue
	 */
	public long getNextBreakDueMs() {
		return nextBreakDueMs;
	}
}
