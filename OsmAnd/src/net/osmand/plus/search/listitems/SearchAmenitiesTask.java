package net.osmand.plus.search.listitems;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.MapUtils;

import java.util.List;

public class SearchAmenitiesTask extends AsyncTask<Void, Void, List<Amenity>> {

	private final LatLon latLon;
	private final PoiUIFilter filter;
	private final int radiusMeters;
	private final CallbackWithObject<List<Amenity>> callback;

	public SearchAmenitiesTask(@NonNull PoiUIFilter filter, @NonNull LatLon latLon,
			int radiusMeters,  @Nullable CallbackWithObject<List<Amenity>> callback) {
		this.filter = filter;
		this.latLon = latLon;
		this.radiusMeters = radiusMeters;
		this.callback = callback;
	}

	@Override
	protected List<Amenity> doInBackground(Void... params) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radiusMeters);
		return getAmenities(rect);
	}

	@NonNull
	private List<Amenity> getAmenities(@NonNull QuadRect rect) {
		return filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, null);
	}

	@Override
	protected void onPostExecute(List<Amenity> amenities) {
		if (callback != null) {
			callback.processResult(amenities);
		}
	}
}
