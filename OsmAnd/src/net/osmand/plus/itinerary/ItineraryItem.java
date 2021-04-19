package net.osmand.plus.itinerary;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.mapmarkers.MapMarker;

public class ItineraryItem {

	public final String id;
	public final Object object;
	public final LatLon latLon;
	public final ItineraryType type;
	public final ItineraryGroup group;

	public ItineraryItem(ItineraryGroup group, Object object, ItineraryType type) {
		this.type = type;
		this.group = group;
		this.object = object;
		this.id = acquireItemId(object);

		if (object instanceof MapMarker) {
			MapMarker marker = (MapMarker) object;
			latLon = marker.point;
		} else if (object instanceof WptPt) {
			WptPt point = (WptPt) object;
			latLon = new LatLon(point.lat, point.lon);
		} else if (object instanceof FavouritePoint) {
			FavouritePoint point = (FavouritePoint) object;
			latLon = new LatLon(point.getLatitude(), point.getLongitude());
		} else {
			latLon = null;
		}
	}

	private String acquireItemId(Object object) {
		if (object instanceof MapMarker) {
			return (((MapMarker) object)).id;
		} else if (object instanceof WptPt) {
			return (((WptPt) object)).name;
		} else if (object instanceof FavouritePoint) {
			return (((FavouritePoint) object)).getName();
		}
		return "";
	}
}
