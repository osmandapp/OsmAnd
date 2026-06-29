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
 * Remaining driving range from tank level and learned consumption.
 * Port of driver_break_update_remaining_range in driver_break_osd.c.
 */
public class RangeEstimator {

	private RangeEstimator() {
	}

	/**
	 * @param fuelLevelPercent   tank fill 0–100; NaN treated as unknown
	 * @param tankCapacityL      nominal tank capacity (L)
	 * @param learnedRateLPerKm  learned or configured consumption (L/km)
	 * @return remaining range in km, or -1 when inputs are unknown
	 */
	public static double remainingRangeKm(float fuelLevelPercent, double tankCapacityL, double learnedRateLPerKm) {
		if (Float.isNaN(fuelLevelPercent) || tankCapacityL <= 0.0 || learnedRateLPerKm <= 0.0) {
			return -1.0;
		}
		double fuelLitres = (fuelLevelPercent / 100.0) * tankCapacityL;
		return fuelLitres / learnedRateLPerKm;
	}
}
