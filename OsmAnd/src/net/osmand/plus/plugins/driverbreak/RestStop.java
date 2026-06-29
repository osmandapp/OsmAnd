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

import net.osmand.data.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Candidate rest stop with nearby POI references.
 */
public class RestStop {

	private final LatLon coordinate;
	private final TravelMode mode;
	private final List<NearbyPoi> pois;
	private final double distanceFromRouteM;

	public RestStop(@NonNull LatLon coordinate, @NonNull TravelMode mode,
			@Nullable List<NearbyPoi> pois, double distanceFromRouteM) {
		this.coordinate = coordinate;
		this.mode = mode;
		this.pois = pois != null ? new ArrayList<>(pois) : new ArrayList<>();
		this.distanceFromRouteM = distanceFromRouteM;
	}

	@NonNull
	public LatLon getCoordinate() {
		return coordinate;
	}

	@NonNull
	public TravelMode getMode() {
		return mode;
	}

	@NonNull
	public List<NearbyPoi> getPois() {
		return Collections.unmodifiableList(pois);
	}

	public double getDistanceFromRouteM() {
		return distanceFromRouteM;
	}

	/**
	 * Lightweight POI reference attached to a rest stop.
	 */
	public static final class NearbyPoi {
		private final LatLon location;
		private final String name;
		private final String category;
		private final double distanceM;
		private final boolean networkPriority;

		public NearbyPoi(@NonNull LatLon location, @Nullable String name,
				@Nullable String category, double distanceM) {
			this(location, name, category, distanceM, false);
		}

		public NearbyPoi(@NonNull LatLon location, @Nullable String name,
				@Nullable String category, double distanceM, boolean networkPriority) {
			this.location = location;
			this.name = name;
			this.category = category;
			this.distanceM = distanceM;
			this.networkPriority = networkPriority;
		}

		@NonNull
		public LatLon getLocation() {
			return location;
		}

		@Nullable
		public String getName() {
			return name;
		}

		@Nullable
		public String getCategory() {
			return category;
		}

		public double getDistanceM() {
			return distanceM;
		}

		/** True when operator/network matches DNT/STF/DAV/SAC priority list. */
		public boolean isNetworkPriority() {
			return networkPriority;
		}
	}
}
