package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.util.MapUtils;

import java.util.List;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private final FavouritePoint fav;
	private Object originObject;

	public FavouritePointMenuBuilder(MapActivity mapActivity, final FavouritePoint fav) {
		super(mapActivity);
		this.fav = fav;
		setShowNearestWiki(true);
		acquireOriginObject();
	}

	public void acquireOriginObject()
	{
		String originObjectName = fav.getOriginObjectName();
		if (originObjectName.length() > 0) {
			if (originObjectName.startsWith(Amenity.class.getSimpleName())) {
				originObject = findAmenity(originObjectName, fav.getLatitude(), fav.getLongitude());
			} else if (originObjectName.startsWith(TransportStop.class.getSimpleName())) {
				originObject = findTransportStop(originObjectName, fav.getLatitude(), fav.getLongitude());
			}
		}
	}

	public Object getOriginObject() {
		return originObject;
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	public void buildInternal(View view) {
		if (originObject != null && originObject instanceof Amenity) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, (Amenity) originObject);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
	}

	private Amenity findAmenity(String nameStringEn, double lat, double lon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new BinaryMapIndexReader.SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		for (Amenity amenity : amenities) {
			String stringEn = amenity.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return amenity;
			}
		}
		return null;
	}

	private TransportStop findTransportStop(String nameStringEn, double lat, double lon) {

		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<TransportStop> res = app.getResourceManager().searchTransportSync(rect.top, rect.left,
				rect.bottom, rect.right, new ResultMatcher<TransportStop>() {

					@Override
					public boolean publish(TransportStop object) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});

		for (TransportStop stop : res) {
			String stringEn = stop.toStringEn();
			if (stringEn.equals(nameStringEn)) {
				return stop;
			}
		}
		return null;
	}
}
