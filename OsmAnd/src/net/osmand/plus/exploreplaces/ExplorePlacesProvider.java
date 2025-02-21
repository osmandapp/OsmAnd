package net.osmand.plus.exploreplaces;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.activities.MapActivity;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExplorePlacesProvider {

	@NotNull List<NearbyPlacePoint> getDataCollection(QuadRect mapRect);

	@NotNull List<NearbyPlacePoint> getDataCollection(QuadRect mapRect, int limit);

	void showPointInContextMenu(@NotNull MapActivity it, @NotNull NearbyPlacePoint item);

	void addListener(ExplorePlacesListener listener);

	void removeListener(ExplorePlacesListener listener);

	Amenity getAmenity(LatLon latLon, long id);

	boolean isLoading();


	interface ExplorePlacesListener {
		void onNewExplorePlacesDownloaded();
	}
}
