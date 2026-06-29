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
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Map;

/**
 * Resolves OsmAnd map regions (county or country) for a geographic point.
 */
public final class ElevationRegionResolver {

	private static final Log LOG = PlatformUtil.getLog(ElevationRegionResolver.class);

	private ElevationRegionResolver() {
	}

	@Nullable
	public static ElevationAdministrativeRegion resolveAt(@NonNull OsmandApplication app, @NonNull LatLon location) {
		try {
			Map.Entry<WorldRegion, BinaryMapDataObject> entry =
					app.getRegions().getSmallestBinaryMapDataObjectAt(location);
			if (entry == null || entry.getKey() == null) {
				return null;
			}
			WorldRegion region = entry.getKey();
			if (region.isContinent()) {
				return null;
			}
			QuadRect bbox = region.getBoundingBox();
			if (bbox == null) {
				bbox = tileBoundingBox(location);
			}
			WorldRegion country = region.getCountryRegion();
			String countryName = null;
			if (country != null && country != region) {
				countryName = country.getLocaleName();
			}
			return new ElevationAdministrativeRegion(region.getRegionId(), region.getLocaleName(), bbox, countryName);
		} catch (IOException e) {
			LOG.warn("DriverBreak: could not resolve region at " + location + ": " + e.getMessage());
			return null;
		}
	}

	@NonNull
	private static QuadRect tileBoundingBox(@NonNull LatLon location) {
		int lonIdx = (int) Math.floor(location.getLongitude());
		int latIdx = (int) Math.floor(location.getLatitude());
		return new QuadRect(lonIdx, latIdx + 1, lonIdx + 1, latIdx);
	}
}
