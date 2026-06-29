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

import net.osmand.data.QuadRect;

/**
 * County or country administrative region used for grouped SRTM downloads.
 */
public final class ElevationAdministrativeRegion {

	private final String regionId;
	private final String displayName;
	private final QuadRect boundingBox;
	@Nullable
	private final String countryDisplayName;

	public ElevationAdministrativeRegion(@NonNull String regionId, @NonNull String displayName,
			@NonNull QuadRect boundingBox, @Nullable String countryDisplayName) {
		this.regionId = regionId;
		this.displayName = displayName;
		this.boundingBox = boundingBox;
		this.countryDisplayName = countryDisplayName;
	}

	@NonNull
	public String getRegionId() {
		return regionId;
	}

	@NonNull
	public String getDisplayName() {
		return displayName;
	}

	@NonNull
	public QuadRect getBoundingBox() {
		return boundingBox;
	}

	@Nullable
	public String getCountryDisplayName() {
		return countryDisplayName;
	}

	public boolean isCountryLevel() {
		return countryDisplayName == null;
	}
}
