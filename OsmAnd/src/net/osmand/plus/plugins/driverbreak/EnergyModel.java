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

import java.util.List;

/**
 * Kinematic energy cost model ported from Navit driver_break_energy.c.
 */
public class EnergyModel {

	/** Sea-level air density in kg/m³ (ISA); driver_break_energy_f_air_from_drag rho0. */
	public static final double AIR_DENSITY_SEA_LEVEL_KG_M3 = 1.225;

	/** Rolling resistance coefficient; driver_break_energy_model_from_config crr = 0.015. */
	public static final double CRR = 0.015;

	/** Standard gravity in m/s²; driver_break_energy.c uses 9.81. */
	public static final double G_MS2 = 9.81;

	/** Reference outside temperature (°C) in energy_calculate_segment. */
	private static final double REFERENCE_OUTSIDE_TEMP_C = 20.0;

	/** Linear temperature correction factor per °C; energy_calculate_segment tcorr. */
	private static final double TEMP_CORRECTION_PER_DEG_C = 0.0035;

	/** Elevation pressure correction factor; energy_calculate_segment ecorr. */
	private static final double ELEVATION_PRESSURE_FACTOR = 0.0001375;

	/** Reference elevation (m) for pressure correction; energy_calculate_segment. */
	private static final double ELEVATION_PRESSURE_REFERENCE_M = 100.0;

	/**
	 * Computes predicted energy cost in Joules for a single route segment using the Navit
	 * kinematic model: rolling resistance, speed-dependent aerodynamic drag (with temperature
	 * and elevation corrections to the drag coefficient), gravitational work against grade,
	 * and downhill recuperation. Standby/auxiliary power adds energy proportional to travel time.
	 *
	 * @param distanceM    segment length in metres; must be &gt; 0 for non-zero result
	 * @param deltaH       signed elevation change in metres (positive = uphill)
	 * @param elevationM   mean elevation of segment, used for air-density correction
	 * @param speedLimitMs posted speed limit in m/s, capped by params max speed
	 * @param params       vehicle physical parameters; must not be null
	 * @return predicted energy in Joules; 0 when distanceM &lt;= 0 or speed &lt;= 0
	 */
	public double segmentCost(double distanceM, double deltaH, double elevationM, double speedLimitMs,
			@NonNull EnergyParams params) {
		if (distanceM <= 0.0) {
			return 0.0;
		}

		double speedMs = effectiveSpeed(params, speedLimitMs);
		if (speedMs <= 0.0) {
			return rollingForce(params) * distanceM;
		}

		double fRoll = rollingForce(params);
		double fAirAdj = adjustedAirCoefficient(params, elevationM);
		double fAirForce = fAirAdj * speedMs * speedMs;
		double fh = deltaH * params.getTotalMassKg() * G_MS2 / distanceM;

		double fTotal = fRoll + fAirForce + fh;
		if (fh < 0.0) {
			double fRecup = -fh * params.getRecuperationEfficiency();
			fTotal = fRoll + fAirForce + fh + fRecup;
		}

		double timeS = distanceM / speedMs;
		double workJ = fTotal * distanceM;
		double standbyJ = params.getStandbyPowerW() * timeS;
		return workJ + standbyJ;
	}

	/**
	 * Sums {@link #segmentCost} over a route. Never throws; returns 0 for empty list.
	 *
	 * @param segments segment list; may be empty
	 * @param params   vehicle parameters
	 * @return total energy in Joules
	 */
	public double routeCost(@NonNull List<SegmentData> segments, @NonNull EnergyParams params) {
		if (segments.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		for (SegmentData segment : segments) {
			total += segmentCost(segment.getDistanceM(), segment.getDeltaH(), segment.getElevationM(),
					segment.getSpeedLimitMs(), params);
		}
		return total;
	}

	/**
	 * Air-resistance coefficient f_air (N·s²/m²) at sea level from Cd and frontal area.
	 * Port of driver_break_energy_f_air_from_drag.
	 */
	public static double airCoefficientFromDrag(double dragCd, double frontalAreaM2) {
		if (dragCd <= 0.0 || frontalAreaM2 <= 0.0) {
			return 0.0;
		}
		return 0.5 * AIR_DENSITY_SEA_LEVEL_KG_M3 * dragCd * frontalAreaM2;
	}

	/**
	 * Rolling resistance force F_roll = CRR * m * g.
	 * Port of driver_break_energy_model_from_config f_roll assignment.
	 */
	public static double rollingForce(@NonNull EnergyParams params) {
		return CRR * params.getTotalMassKg() * G_MS2;
	}

	private static double effectiveSpeed(@NonNull EnergyParams params, double speedLimitMs) {
		if (speedLimitMs > params.getMaxSpeedMs()) {
			return params.getMaxSpeedMs();
		}
		return speedLimitMs;
	}

	private static double adjustedAirCoefficient(@NonNull EnergyParams params, double elevationM) {
		double fAir = airCoefficientFromDrag(params.getDragCd(), params.getFrontalAreaM2());
		double tcorr = (REFERENCE_OUTSIDE_TEMP_C - params.getOutsideTempCelsius()) * TEMP_CORRECTION_PER_DEG_C;
		double ecorr = ELEVATION_PRESSURE_FACTOR * (elevationM - ELEVATION_PRESSURE_REFERENCE_M);
		return fAir * (1.0 + tcorr - ecorr);
	}
}
