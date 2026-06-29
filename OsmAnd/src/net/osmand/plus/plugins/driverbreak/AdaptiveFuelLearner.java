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

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

/**
 * Exponential moving average fuel consumption learner over fuel_samples rows.
 * EMA alpha chosen for stable convergence (0.1); Navit C source uses rolling windows instead.
 */
public class AdaptiveFuelLearner {

	private static final Log LOG = PlatformUtil.getLog(AdaptiveFuelLearner.class);

	/** EMA smoothing factor for learned L/km rate. */
	public static final double EMA_ALPHA = 0.1;

	private final DriverBreakSettings settings;

	public AdaptiveFuelLearner(@NonNull DriverBreakSettings settings) {
		this.settings = settings;
	}

	/**
	 * Record a fuel sample and update the learned rate. Must run on dbExecutor.
	 *
	 * @param distanceM   distance since previous sample (m)
	 * @param fuelLitres  fuel consumed over that distance (L)
	 * @param mode        travel mode key
	 */
	public void recordSample(double distanceM, double fuelLitres, @NonNull TravelMode mode) {
		if (!settings.isAdaptiveFuelEnabledSync()) {
			return;
		}
		if (distanceM <= 0.0 || fuelLitres < 0.0) {
			return;
		}
		double distanceKm = distanceM / 1000.0;
		double sampleLPerKm = fuelLitres / distanceKm;
		long now = System.currentTimeMillis() / 1000L;
		settings.getDatabase().addFuelSample(now, distanceM, fuelLitres, mode.getConfigKey());

		double current = settings.getLearnedRateLPerKmSync();
		double updated;
		if (current <= 0.0) {
			updated = sampleLPerKm;
		} else {
			updated = EMA_ALPHA * sampleLPerKm + (1.0 - EMA_ALPHA) * current;
		}
		settings.setLearnedRateLPerKmSync(updated);
		LOG.debug("DriverBreak: adaptive rate updated to " + updated + " L/km");
	}

	/**
	 * Recompute learned rate from all stored samples using EMA. Must run on dbExecutor.
	 *
	 * @return learned litres per km
	 */
	public double recomputeFromHistory() {
		if (!settings.isAdaptiveFuelEnabledSync()) {
			return settings.getLearnedRateLPerKmSync();
		}
		double rate = 0.0;
		for (DriverBreakDatabase.FuelSampleRow row : settings.getDatabase().getFuelSamples()) {
			if (row.distanceM <= 0.0) {
				continue;
			}
			double sample = row.fuelLitres / (row.distanceM / 1000.0);
			if (rate <= 0.0) {
				rate = sample;
			} else {
				rate = EMA_ALPHA * sample + (1.0 - EMA_ALPHA) * rate;
			}
		}
		if (rate > 0.0) {
			settings.setLearnedRateLPerKmSync(rate);
		}
		return rate;
	}
}
