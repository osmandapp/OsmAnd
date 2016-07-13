package net.osmand.plus.search;

import android.graphics.drawable.Drawable;

import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

public class SearchListItem {

	protected OsmandApplication app;
	private SearchResult searchResult;
	private double distance;

	public SearchListItem(OsmandApplication app, SearchResult searchResult) {
		this.app = app;
		this.searchResult = searchResult;
	}

	public SearchResult getSearchResult() {
		return searchResult;
	}

	public String getName() {
		return searchResult.localeName;
	}

	public String getTypeName() {
		switch (searchResult.objectType) {
			case CITY:
			case POSTCODE:
				City city = (City) searchResult.object;
				return Algorithms.capitalizeFirstLetterAndLowercase(city.getType().toString());
			case VILLAGE:
				city = (City) searchResult.object;
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					return Algorithms.capitalizeFirstLetterAndLowercase(city.getType().toString())
							+ " near "
							+ searchResult.localeRelatedObjectName
							+ (searchResult.distRelatedObjectName > 0 ? " (" + OsmAndFormatter.getFormattedDistance((float)searchResult.distRelatedObjectName, app) + ")" : "");
				} else {
					return Algorithms.capitalizeFirstLetterAndLowercase(city.getType().toString());
				}
			case STREET:
				Street street = (Street) searchResult.object;
				City streetCity = street.getCity();
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					return searchResult.localeRelatedObjectName
							+ (searchResult.distRelatedObjectName > 0 ? "(" + OsmAndFormatter.getFormattedDistance((float)searchResult.distRelatedObjectName, app) + ")" : "");
				} else {
					return streetCity.getName() + " - " + Algorithms.capitalizeFirstLetterAndLowercase(streetCity.getType().name());
				}
			case HOUSE:
				return "House";
			case STREET_INTERSECTION:
				return "Street intersection";
			case POI_TYPE:
				AbstractPoiType abstractPoiType = (AbstractPoiType) searchResult.object;
				String res;
				if (abstractPoiType instanceof PoiCategory) {
					res = "POI category";
				} else if (abstractPoiType instanceof PoiFilter) {
					PoiFilter poiFilter = (PoiFilter) abstractPoiType;
					res = poiFilter.getPoiCategory() != null ? poiFilter.getPoiCategory().getTranslation() : "POI filter";

				} else if (abstractPoiType instanceof PoiType) {
					PoiType poiType = (PoiType) abstractPoiType;
					res = poiType.getParentType() != null ? poiType.getParentType().getTranslation() : null;
					if (res == null) {
						res = poiType.getCategory() != null ? poiType.getCategory().getTranslation() : null;
					}
					if (res == null) {
						res = "POI type";
					}
				} else {
					res = "POI type";
				}
				return res;
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				return amenity.getType().toString();
			case LOCATION:
				break;
			case FAVORITE:
				break;
			case REGION:
				break;
			case RECENT_OBJ:
				break;
			case WPT:
				break;
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return searchResult.objectType.name();
	}

	public Drawable getIcon() {
		switch (searchResult.objectType) {
			case CITY:
				break;
			case VILLAGE:
				break;
			case POSTCODE:
				break;
			case STREET:
				break;
			case HOUSE:
				break;
			case STREET_INTERSECTION:
				break;
			case POI_TYPE:
				break;
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				Drawable drawable = null;
				PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
				if (st != null) {
					//drawable = app.getIconsCache().getMapIcon(st.getOsmTag() + "_" + st.getOsmValue());
				}
				return drawable;
			case LOCATION:
				break;
			case FAVORITE:
				break;
			case REGION:
				break;
			case RECENT_OBJ:
				break;
			case WPT:
				break;
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return null;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
}
