package net.osmand.plus.mapcontextmenu.builders;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.MapUtils;

import java.util.List;

public class SyncedItemMenuBuilder extends MenuBuilder {

	protected FavouritePoint favouritePoint;
	protected Object originObject;
	protected GPXUtilities.WptPt wptPt;

	public SyncedItemMenuBuilder(MapActivity mapActivity) {
		super(mapActivity);
		setShowNearestWiki(true);
	}

	protected void acquireOriginObject() {
		String originObjectName = favouritePoint.getOriginObjectName();
		if (originObjectName.length() > 0) {
			if (originObjectName.startsWith(Amenity.class.getSimpleName())) {
				originObject = findAmenity(originObjectName, favouritePoint.getLatitude(), favouritePoint.getLongitude());
			} else if (originObjectName.startsWith(TransportStop.class.getSimpleName())) {
				originObject = findTransportStop(originObjectName, favouritePoint.getLatitude(), favouritePoint.getLongitude());
			}
		}
	}

	public Object getOriginObject() {
		return originObject;
	}

	protected Amenity findAmenity(String nameStringEn, double lat, double lon) {
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

	protected TransportStop findTransportStop(String nameStringEn, double lat, double lon) {

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
