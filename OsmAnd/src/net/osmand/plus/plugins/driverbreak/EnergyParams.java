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
 * Physical parameters for the kinematic energy model.
 * Corresponds to fields in Navit {@code struct energy_model} and config aerodynamics.
 */
public class EnergyParams {

	private final double totalMassKg;
	private final double dragCd;
	private final double frontalAreaM2;
	private final double standbyPowerW;
	private final double recuperationEfficiency;
	private final double outsideTempCelsius;
	private final double maxSpeedMs;

	/**
	 * @param totalMassKg            total mass including cargo (kg); from config total_weight
	 * @param dragCd                 aerodynamic drag coefficient Cd (dimensionless)
	 * @param frontalAreaM2          frontal area A (m²)
	 * @param standbyPowerW          auxiliary/standby power draw (W); Navit default 0
	 * @param recuperationEfficiency fraction of downhill gravitational work recovered (0–1);
	 *                               Navit default 0.7 in energy_model_init
	 */
	public EnergyParams(double totalMassKg, double dragCd, double frontalAreaM2,
			double standbyPowerW, double recuperationEfficiency) {
		this(totalMassKg, dragCd, frontalAreaM2, standbyPowerW, recuperationEfficiency, 20.0, 80.0 / 3.6);
	}

	/**
	 * Full constructor including temperature and speed cap used by Navit energy_model_init.
	 */
	public EnergyParams(double totalMassKg, double dragCd, double frontalAreaM2,
			double standbyPowerW, double recuperationEfficiency,
			double outsideTempCelsius, double maxSpeedMs) {
		this.totalMassKg = totalMassKg;
		this.dragCd = dragCd;
		this.frontalAreaM2 = frontalAreaM2;
		this.standbyPowerW = standbyPowerW;
		this.recuperationEfficiency = recuperationEfficiency;
		this.outsideTempCelsius = outsideTempCelsius;
		this.maxSpeedMs = maxSpeedMs;
	}

	public double getTotalMassKg() {
		return totalMassKg;
	}

	public double getDragCd() {
		return dragCd;
	}

	public double getFrontalAreaM2() {
		return frontalAreaM2;
	}

	public double getStandbyPowerW() {
		return standbyPowerW;
	}

	public double getRecuperationEfficiency() {
		return recuperationEfficiency;
	}

	public double getOutsideTempCelsius() {
		return outsideTempCelsius;
	}

	public double getMaxSpeedMs() {
		return maxSpeedMs;
	}

	/**
	 * Build params from plugin configuration defaults in driver_break_config_default().
	 */
	@NonNull
	public static EnergyParams fromDriverBreakDefaults() {
		return new EnergyParams(80.0, 0.30, 2.2, 0.0, 0.7);
	}
}
