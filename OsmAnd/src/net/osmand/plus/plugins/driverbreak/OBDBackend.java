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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.shared.obd.OBDDataComputer;
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * OBD-II backend delegating to OsmAnd {@link VehicleMetricsPlugin} / {@link OBDDataComputer}.
 * Fuel rate from PID 0x5E when available; MAF PID 0x10 fallback handled in shared OBD layer.
 */
public class OBDBackend implements ECUDataSource {

	private static final Log LOG = PlatformUtil.getLog(OBDBackend.class);

	private static final float MAF_AFR_PETROL = 14.7f;
	private static final float MAF_DENSITY_PETROL_KG_L = 0.745f;

	private final OsmandApplication app;
	private final List<ECUDataListener> listeners = new ArrayList<>();

	private OBDComputerWidget fuelRateWidget;
	private OBDComputerWidget fuelLevelWidget;

	public OBDBackend(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public float getFuelRateLitresPerHour() {
		ensureWidgets();
		if (fuelRateWidget == null) {
			return Float.NaN;
		}
		Object raw = fuelRateWidget.computeValue();
		if (raw == null || "N/A".equals(raw)) {
			return Float.NaN;
		}
		if (raw instanceof Number) {
			return ((Number) raw).floatValue();
		}
		return Float.NaN;
	}

	@Override
	public float getFuelLevelPercent() {
		ensureWidgets();
		if (fuelLevelWidget == null) {
			return Float.NaN;
		}
		Object raw = fuelLevelWidget.computeValue();
		if (raw == null || "N/A".equals(raw)) {
			return Float.NaN;
		}
		if (raw instanceof Number) {
			return ((Number) raw).floatValue();
		}
		return Float.NaN;
	}

	@Override
	public boolean isConnected() {
		VehicleMetricsPlugin plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin.class);
		return plugin != null && plugin.isConnected();
	}

	@Override
	public void addListener(@NonNull ECUDataListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(@NonNull ECUDataListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Estimate fuel rate (L/h) from MAF (g/s) when PID 0x5E is unavailable.
	 * Port of obd_maf_to_fuel_rate in driver_break_obd.c for petrol.
	 */
	public static float mafToFuelRateLitresPerHour(float mafGramsPerSecond) {
		if (mafGramsPerSecond <= 0.0f) {
			return 0.0f;
		}
		float fuelKgPerHour = (mafGramsPerSecond / MAF_AFR_PETROL) * 3600.0f / 1000.0f;
		return fuelKgPerHour / MAF_DENSITY_PETROL_KG_L;
	}

	private void ensureWidgets() {
		if (fuelRateWidget == null) {
			fuelRateWidget = OBDDataComputer.INSTANCE.registerWidget(
					OBDDataComputer.OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR, 0);
			fuelLevelWidget = OBDDataComputer.INSTANCE.registerWidget(
					OBDDataComputer.OBDTypeWidget.FUEL_LEFT_PERCENT, 0);
		}
	}

	/** Notify listeners after OBD poll; call from plugin when metrics update. */
	public void notifyListeners() {
		if (!isConnected()) {
			for (ECUDataListener listener : new ArrayList<>(listeners)) {
				listener.onDisconnected();
			}
			return;
		}
		float rate = getFuelRateLitresPerHour();
		float level = getFuelLevelPercent();
		for (ECUDataListener listener : new ArrayList<>(listeners)) {
			listener.onConnected();
			if (!Float.isNaN(rate)) {
				listener.onFuelRate(rate);
			}
			if (!Float.isNaN(level)) {
				listener.onFuelLevel(level);
			}
		}
	}
}
