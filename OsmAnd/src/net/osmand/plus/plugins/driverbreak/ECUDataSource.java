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
 * Live ECU fuel data source (OBD-II or J1939).
 */
public interface ECUDataSource {

	/**
	 * @return instantaneous fuel rate in L/h, or NaN when unavailable
	 */
	float getFuelRateLitresPerHour();

	/**
	 * @return tank fill level 0–100 %, or NaN when unavailable
	 */
	float getFuelLevelPercent();

	/** @return true when the backend is connected and polling */
	boolean isConnected();

	void addListener(@NonNull ECUDataListener listener);

	void removeListener(@NonNull ECUDataListener listener);
}
