package net.osmand.plus.views.layers;

import static net.osmand.data.Amenity.WIKIDATA;

import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.FavouritePoint;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.List;

public class PlaceDetailsObject extends BaseDetailsObject {

	public PlaceDetailsObject(String lang) {
		super(lang);
	}

	public PlaceDetailsObject(Object object, String lang) {
		super(object, lang);
	}

	public PlaceDetailsObject(List<Amenity> amenities, String lang) {
		super(amenities, lang);
	}

	@Nullable
	public WptPt getWptPt() {
		for (Object object : getObjects()) {
			if (object instanceof WptPt wptPt) {
				return wptPt;
			}
		}
		return null;
	}

	@Nullable
	public FavouritePoint getFavouritePoint() {
		for (Object object : getObjects()) {
			if (object instanceof FavouritePoint point) {
				return point;
			}
		}
		return null;
	}

	@Override
	protected String getWikidata(Object object) {
		String wikidata = super.getWikidata(object);
		if (Algorithms.isEmpty(wikidata)) {
			if (object instanceof WptPt wptPt) {
				return wptPt.getExtensionsToRead().get(WIKIDATA);
			} else if (object instanceof FavouritePoint point) {
				return point.getAmenityExtensions().get(WIKIDATA);
			}
		}
		return wikidata;
	}

	@Override
	public boolean overlapsWith(Object object) {
		boolean overlapped = super.overlapsWith(object);

		if (!overlapped) {
			if (object instanceof WptPt wptPt) {
				return overlapOriginName(wptPt.getAmenityOriginName());
			} else if (object instanceof FavouritePoint point) {
				return overlapOriginName(point.getAmenityOriginName());
			}
		}
		return overlapped;
	}

	private boolean overlapOriginName(@Nullable String originName) {
		if (!Algorithms.isEmpty(originName)) {
			for (Amenity amenity : getAmenities()) {
				if (Algorithms.stringsEqual(amenity.toStringEn(), originName)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected boolean isSupportedObjectType(Object object) {
		return super.isSupportedObjectType(object)
				|| object instanceof WptPt wptPt && !Algorithms.isEmpty(wptPt.getAmenityOriginName())
				|| object instanceof FavouritePoint point && !Algorithms.isEmpty(point.getAmenityOriginName());
	}
}
