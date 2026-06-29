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

import java.util.Locale;
import java.util.Objects;

/** One-degree SRTM / Copernicus tile index (floor of lon/lat). */
public final class SRTMTileIndex {

	public final int lonIndex;
	public final int latIndex;

	public SRTMTileIndex(int lonIndex, int latIndex) {
		this.lonIndex = lonIndex;
		this.latIndex = latIndex;
	}

	@NonNull
	public static SRTMTileIndex fromCoordinate(double lat, double lon) {
		return new SRTMTileIndex((int) Math.floor(lon), (int) Math.floor(lat));
	}

	@NonNull
	public String tileFileName() {
		double lat = latIndex + 0.5;
		double lon = lonIndex + 0.5;
		return SRTMElevationProvider.hgtFilename(lat, lon);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SRTMTileIndex)) {
			return false;
		}
		SRTMTileIndex that = (SRTMTileIndex) o;
		return lonIndex == that.lonIndex && latIndex == that.latIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lonIndex, latIndex);
	}

	@Override
	@NonNull
	public String toString() {
		return String.format(Locale.US, "%d,%d", lonIndex, latIndex);
	}
}
