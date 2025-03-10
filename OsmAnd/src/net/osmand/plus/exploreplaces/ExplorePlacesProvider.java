package net.osmand.plus.exploreplaces;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExplorePlacesProvider {

	public final int MAX_LEVEL_ZOOM_CACHE = 13;
	@NotNull List<Amenity> getDataCollection(QuadRect mapRect);

	@NotNull List<Amenity> getDataCollection(QuadRect mapRect, int limit);

	@NotNull List<Amenity> getDataCollection(QuadRect rect, int limit, int zoom);

	void addListener(ExplorePlacesListener listener);

	void removeListener(ExplorePlacesListener listener);

	boolean isLoading();

	// data version is increased once new data is downloaded
	int getDataVersion();

	boolean isLoadingRect(@NonNull QuadRect rect);

	interface ExplorePlacesListener {
		// once new data is downloaded data version is increased
		void onNewExplorePlacesDownloaded();

		default void onPartialExplorePlacesDownloaded() {}
	}
}
