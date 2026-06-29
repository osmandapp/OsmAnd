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

/**
 * Travel mode for Driver Break timing and POI discovery.
 * Maps to {@code driver_break_vehicle_type} in Navit driver_break.h.
 */
public enum TravelMode {
	CAR("car"),
	TRUCK("truck"),
	HIKING("hiking"),
	CYCLING("cycling"),
	MOTORCYCLE("motorcycle");

	private final String configKey;

	TravelMode(@NonNull String configKey) {
		this.configKey = configKey;
	}

	/**
	 * @return persisted config-table value for this mode
	 */
	@NonNull
	public String getConfigKey() {
		return configKey;
	}

	/**
	 * Parse a config-table travel mode string.
	 *
	 * @param key mode key from SQLite config; defaults to {@link #CAR} when unknown
	 * @return matching mode, never null
	 */
	@NonNull
	public static TravelMode fromConfigKey(@Nullable String key) {
		if (key == null) {
			return CAR;
		}
		for (TravelMode mode : values()) {
			if (mode.configKey.equalsIgnoreCase(key)) {
				return mode;
			}
		}
		return CAR;
	}
}
