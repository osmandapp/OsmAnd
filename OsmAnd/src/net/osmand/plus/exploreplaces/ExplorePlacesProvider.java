package net.osmand.plus.exploreplaces;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.ExploreTopPlacePoint;
import net.osmand.data.QuadRect;
import net.osmand.plus.activities.MapActivity;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExplorePlacesProvider {

	public final int MAX_LEVEL_ZOOM_CACHE = 13;
	@NotNull List<ExploreTopPlacePoint> getDataCollection(QuadRect mapRect);

	@NotNull List<ExploreTopPlacePoint> getDataCollection(QuadRect mapRect, int limit);

	void showPointInContextMenu(@NotNull MapActivity it, @NotNull ExploreTopPlacePoint item);

	void addListener(ExplorePlacesListener listener);

	void removeListener(ExplorePlacesListener listener);

	Amenity getAmenity(LatLon latLon, long id);

	boolean isLoading();

	// data version is increased once new data is downloaded
	int getDataVersion();

	interface ExplorePlacesListener {
		// once new data is downloaded data version is increased
		void onNewExplorePlacesDownloaded();

		default void onPartialExplorePlacesDownloaded() {}
	}
}
