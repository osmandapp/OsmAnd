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

import java.util.ArrayList;
import java.util.List;

/**
 * MegaSquirt ECU backend — not yet implemented.
 *
 * TODO: Implement serial protocol parser for MS1/MS2/MS3/MS3-Pro/MicroSquirt.
 * See navit/src/plugins/osd_driver_break/driver_break_megasquirt.c for the
 * reference implementation to port.
 */
public class MegaSquirtBackend implements ECUDataSource {

	private final List<ECUDataListener> listeners = new ArrayList<>();

	MegaSquirtBackend() {
	}

	@Override
	public float getFuelRateLitresPerHour() {
		throw new UnsupportedOperationException("MegaSquirt backend not yet implemented");
	}

	@Override
	public float getFuelLevelPercent() {
		throw new UnsupportedOperationException("MegaSquirt backend not yet implemented");
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public void addListener(@NonNull ECUDataListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(@NonNull ECUDataListener listener) {
		listeners.remove(listener);
	}
}
