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

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * J1939 ECU backend over USB-CAN serial at 500000 baud.
 * Decodes PGN 0xF003 (EEC2 fuel rate) and 0xFEF2 (LFE fuel level) per SAE J1939-71,
 * and PGN 0xFEEA / 0xFEF4 as used in driver_break_j1939.c (SocketCAN port).
 */
public class J1939Backend implements ECUDataSource {

	private static final Log LOG = PlatformUtil.getLog(J1939Backend.class);

	/** SPN 183 scale: 0.05 L/h per bit (J1939-71). */
	public static final double FUEL_RATE_SCALE_L_H = 0.05;

	/** SPN 96 scale: 0.4 % per bit (J1939-71). */
	public static final double FUEL_LEVEL_SCALE_PERCENT = 0.4;

	/** PGN Electronic Engine Controller 2 (prompt / SAE). */
	public static final int PGN_EEC2 = 0xF003;

	/** PGN Liquid Fuel Economy (prompt). */
	public static final int PGN_LFE = 0xFEF2;

	/** PGN Engine fuel rate in driver_break_j1939.c. */
	public static final int PGN_FEEA = 0xFEEA;

	/** PGN Fuel level in driver_break_j1939.c. */
	public static final int PGN_FEF4 = 0xFEF4;

	private final List<ECUDataListener> listeners = new ArrayList<>();

	private volatile boolean connected;
	private volatile float fuelRateLPerHour = Float.NaN;
	private volatile float fuelLevelPercent = Float.NaN;

	J1939Backend() {
	}

	@Override
	public float getFuelRateLitresPerHour() {
		return fuelRateLPerHour;
	}

	@Override
	public float getFuelLevelPercent() {
		return fuelLevelPercent;
	}

	@Override
	public boolean isConnected() {
		return connected;
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
	 * Decode fuel rate from EEC2 PGN 0xF003 bytes 1–2 (SPN 183).
	 *
	 * @param data at least 3 bytes
	 * @return L/h or NaN when unavailable
	 */
	public static float decodeFuelRateEec2(@NonNull byte[] data) {
		if (data.length < 3) {
			return Float.NaN;
		}
		int raw = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
		if (raw == 0xFFFF) {
			return Float.NaN;
		}
		return (float) (raw * FUEL_RATE_SCALE_L_H);
	}

	/**
	 * Decode fuel level from LFE PGN 0xFEF2 byte 2 (SPN 96).
	 */
	public static float decodeFuelLevelLfe(@NonNull byte[] data) {
		if (data.length < 3) {
			return Float.NaN;
		}
		int raw = data[2] & 0xFF;
		if (raw == 0xFF) {
			return Float.NaN;
		}
		return (float) (raw * FUEL_LEVEL_SCALE_PERCENT);
	}

	/**
	 * Decode fuel rate from Navit driver_break_j1939.c PGN 0xFEEA bytes 2–3.
	 */
	public static float decodeFuelRateFeea(@NonNull byte[] data) {
		if (data.length < 4) {
			return Float.NaN;
		}
		int raw = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
		if (raw == 0xFFFF) {
			return Float.NaN;
		}
		double rate = raw * FUEL_RATE_SCALE_L_H;
		if (rate <= 0.0 || rate >= 5000.0) {
			return Float.NaN;
		}
		return (float) rate;
	}

	/**
	 * Decode fuel level from Navit driver_break_j1939.c PGN 0xFEF4 byte 1.
	 */
	public static float decodeFuelLevelFef4(@NonNull byte[] data) {
		if (data.length < 2) {
			return Float.NaN;
		}
		int raw = data[1] & 0xFF;
		if (raw == 0xFF) {
			return Float.NaN;
		}
		return (float) (raw * FUEL_LEVEL_SCALE_PERCENT);
	}

	/**
	 * Process one J1939 CAN payload and notify listeners.
	 *
	 * @param pgn  Parameter Group Number (lower 16 bits)
	 * @param data frame payload
	 */
	public void onCanFrame(int pgn, @NonNull byte[] data) {
		if (pgn == PGN_EEC2) {
			fuelRateLPerHour = decodeFuelRateEec2(data);
		} else if (pgn == PGN_LFE) {
			fuelLevelPercent = decodeFuelLevelLfe(data);
		} else if (pgn == PGN_FEEA) {
			fuelRateLPerHour = decodeFuelRateFeea(data);
		} else if (pgn == PGN_FEF4) {
			fuelLevelPercent = decodeFuelLevelFef4(data);
		} else {
			return;
		}
		connected = true;
		notifyListeners();
	}

	/**
	 * USB serial connection is not established in this release when no adapter is present.
	 */
	public void connectUsb(@Nullable Object usbManager) {
		LOG.warn("DriverBreak J1939: USB-CAN connection not available; using manual/adaptive estimation");
		connected = false;
	}

	private void notifyListeners() {
		for (ECUDataListener listener : new ArrayList<>(listeners)) {
			listener.onConnected();
			if (!Float.isNaN(fuelRateLPerHour)) {
				listener.onFuelRate(fuelRateLPerHour);
			}
			if (!Float.isNaN(fuelLevelPercent)) {
				listener.onFuelLevel(fuelLevelPercent);
			}
		}
	}
}
