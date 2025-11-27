package net.osmand.plus.mapcontextmenu;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.views.AmenityObjectsMerger;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.List;

public class SearchAmenitiesTask extends AsyncTask<Void, Void, List<Amenity>> {

	public static final int NEARBY_MAX_POI_COUNT = 10;
	private static final int NEARBY_POI_MIN_RADIUS = 250;
	private static final int NEARBY_POI_MAX_RADIUS = 1000;
	private static final int NEARBY_POI_SEARCH_FACTOR = 2;

	private final LatLon latLon;
	private final PoiUIFilter filter;
	private final Amenity amenity;
	private final AmenityObjectsMerger amenityObjectsCombiner;
	private SearchAmenitiesListener listener;

	protected SearchAmenitiesTask(@NonNull PoiUIFilter filter, @NonNull LatLon latLon,
	                              @NonNull String lang, @Nullable Amenity amenity) {
		this.filter = filter;
		this.latLon = latLon;
		this.amenity = amenity;
		this.amenityObjectsCombiner = new AmenityObjectsMerger(lang);
	}

	public void setListener(@Nullable SearchAmenitiesListener listener) {
		this.listener = listener;
	}

	@Override
	protected List<Amenity> doInBackground(Void... params) {
		int radius = NEARBY_POI_MIN_RADIUS;
		List<Amenity> amenities = Collections.emptyList();
		while (amenities.size() < NEARBY_MAX_POI_COUNT && radius <= NEARBY_POI_MAX_RADIUS) {
			if (isCancelled()) {
				break;
			}
			QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
			radius *= NEARBY_POI_SEARCH_FACTOR;

			amenities = collectAmenities(rect);
			if (amenities.size() >= NEARBY_MAX_POI_COUNT || radius > NEARBY_POI_MAX_RADIUS) {
				amenities = amenityObjectsCombiner.merge(amenities);
				amenities.remove(amenity);
			}
		}
		MapUtils.sortListOfMapObject(amenities, latLon.getLatitude(), latLon.getLongitude());
		return amenities.subList(0, Math.min(NEARBY_MAX_POI_COUNT, amenities.size()));
	}

	@NonNull
	private List<Amenity> collectAmenities(@NonNull QuadRect rect) {
		return filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, new ResultMatcher<>() {
			@Override
			public boolean publish(Amenity amenity) {
				return true;
			}

			@Override
			public boolean isCancelled() {
				return SearchAmenitiesTask.this.isCancelled();
			}
		});
	}

	@Override
	protected void onPostExecute(List<Amenity> amenities) {
		if (listener != null) {
			listener.onFinish(amenities);
		}
	}

	public interface SearchAmenitiesListener {
		void onFinish(List<Amenity> amenities);
	}
}
