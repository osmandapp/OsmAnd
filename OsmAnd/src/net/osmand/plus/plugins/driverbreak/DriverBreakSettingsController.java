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
import androidx.annotation.Nullable;

import java.io.File;

/**
 * Persists Driver Break settings preference changes with validation from driver_break_osd.c.
 */
final class DriverBreakSettingsController {

	private static final double MIN_DRAG_CD = 0.01;
	private static final double MAX_DRAG_CD = 1.5;
	private static final double MIN_FRONTAL_AREA_SQM = 0.05;
	private static final double MAX_FRONTAL_AREA_SQM = 20.0;
	private static final double MIN_TOTAL_WEIGHT_KG = 1.0;
	private static final double MAX_TOTAL_WEIGHT_KG = 50000.0;

	private DriverBreakSettingsController() {
	}

	static boolean applyChange(@Nullable DriverBreakPlugin plugin, @NonNull String key,
			@NonNull Object newValue) {
		if (plugin == null) {
			return false;
		}
		DriverBreakSettings settings = plugin.getSettings();
		String text = String.valueOf(newValue);
		switch (key) {
			case "driver_break_travel_mode":
				TravelMode mode = TravelMode.fromConfigKey(text);
				settings.getDbExecutor().execute(() -> {
					settings.setTravelModeSync(mode);
					plugin.onTravelModeChanged(mode);
				});
				return true;
			case "driver_break_use_energy_routing":
				boolean energy = Boolean.TRUE.equals(newValue);
				settings.getDbExecutor().execute(() -> {
					settings.setUseEnergyRoutingSync(energy);
					plugin.onEnergyRoutingChanged(energy);
				});
				return true;
			case "driver_break_ecu_enabled":
				settings.getDbExecutor().execute(() -> settings.setEcuEnabledSync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_adaptive_fuel":
				settings.getDbExecutor().execute(() -> settings.setAdaptiveFuelEnabledSync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_water_pois_enabled":
				settings.getDbExecutor().execute(() -> settings.setWaterPoisEnabledSync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_dnt_priority":
				settings.getDbExecutor().execute(() -> settings.setDntPrioritySync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_hiking_alt_enabled":
				settings.getDbExecutor().execute(() -> settings.setHikingAltEnabledSync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_cycling_alt_enabled":
				settings.getDbExecutor().execute(() -> settings.setCyclingAltEnabledSync(Boolean.TRUE.equals(newValue)));
				return true;
			case "driver_break_total_weight":
				if (!validateRange(key, parseDouble(text), MIN_TOTAL_WEIGHT_KG, MAX_TOTAL_WEIGHT_KG)) {
					return false;
				}
				settings.getDbExecutor().execute(() -> settings.setConfigSync("total_weight", text));
				return true;
			case "driver_break_energy_drag_cd":
				if (!validateRange(key, parseDouble(text), MIN_DRAG_CD, MAX_DRAG_CD)) {
					return false;
				}
				settings.getDbExecutor().execute(() -> settings.setConfigSync("energy_drag_cd", text));
				return true;
			case "driver_break_energy_frontal_area_sqm":
				if (!validateRange(key, parseDouble(text), MIN_FRONTAL_AREA_SQM, MAX_FRONTAL_AREA_SQM)) {
					return false;
				}
				settings.getDbExecutor().execute(() -> settings.setConfigSync("energy_frontal_area_sqm", text));
				return true;
			default:
				String configKey = preferenceKeyToConfigKey(key);
				if (configKey == null) {
					return false;
				}
				settings.getDbExecutor().execute(() -> settings.setConfigSync(configKey, text));
				return true;
		}
	}

	@Nullable
	private static String preferenceKeyToConfigKey(@NonNull String preferenceKey) {
		if (!preferenceKey.startsWith("driver_break_")) {
			return null;
		}
		return preferenceKey.substring("driver_break_".length());
	}

	private static boolean validateRange(@NonNull String key, double value, double min, double max) {
		return !(Double.isNaN(value) || value < min || value > max);
	}

	private static double parseDouble(@NonNull String raw) {
		try {
			return Double.parseDouble(raw);
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}
}
