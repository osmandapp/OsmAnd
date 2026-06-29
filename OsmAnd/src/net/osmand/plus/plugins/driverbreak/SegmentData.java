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
 * One route segment input for {@link EnergyModel#routeCost}.
 */
public class SegmentData {

	private final double distanceM;
	private final double deltaH;
	private final double elevationM;
	private final double speedLimitMs;

	public SegmentData(double distanceM, double deltaH, double elevationM, double speedLimitMs) {
		this.distanceM = distanceM;
		this.deltaH = deltaH;
		this.elevationM = elevationM;
		this.speedLimitMs = speedLimitMs;
	}

	public double getDistanceM() {
		return distanceM;
	}

	public double getDeltaH() {
		return deltaH;
	}

	public double getElevationM() {
		return elevationM;
	}

	public double getSpeedLimitMs() {
		return speedLimitMs;
	}

	@NonNull
	public static SegmentData of(double distanceM, double deltaH, double elevationM, double speedLimitMs) {
		return new SegmentData(distanceM, deltaH, elevationM, speedLimitMs);
	}
}
