package net.osmand.plus.search;

import android.graphics.drawable.Drawable;

import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.FavouritePoint;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.render.RenderingIcons;
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
							+ (searchResult.distRelatedObjectName > 0 ? " (" + OsmAndFormatter.getFormattedDistance((float)searchResult.distRelatedObjectName, app) + ")" : "");
				} else {
					return streetCity.getName() + " - " + Algorithms.capitalizeFirstLetterAndLowercase(streetCity.getType().name());
				}
			case HOUSE:
				return "";
			case STREET_INTERSECTION:
				return "";
			case POI_TYPE:
				AbstractPoiType abstractPoiType = (AbstractPoiType) searchResult.object;
				String res;
				if (abstractPoiType instanceof PoiCategory) {
					res = "";
				} else if (abstractPoiType instanceof PoiFilter) {
					PoiFilter poiFilter = (PoiFilter) abstractPoiType;
					res = poiFilter.getPoiCategory() != null ? poiFilter.getPoiCategory().getTranslation() : "";

				} else if (abstractPoiType instanceof PoiType) {
					PoiType poiType = (PoiType) abstractPoiType;
					res = poiType.getParentType() != null ? poiType.getParentType().getTranslation() : null;
					if (res == null) {
						res = poiType.getCategory() != null ? poiType.getCategory().getTranslation() : null;
					}
					if (res == null) {
						res = "";
					}
				} else {
					res = "";
				}
				return res;
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				PoiCategory pc = amenity.getType();
				PoiType pt = pc.getPoiTypeByKeyName(amenity.getSubType());
				String typeStr = amenity.getSubType();
				if (pt != null) {
					typeStr = pt.getTranslation();
				} else if (typeStr != null) {
					typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
				}
				return typeStr;
			case LOCATION:
				break;
			case FAVORITE:
				FavouritePoint fav = (FavouritePoint) searchResult.object;
				return fav.getCategory().length() == 0 ?
						app.getString(R.string.shared_string_favorites) : fav.getCategory();
			case REGION:
				break;
			case RECENT_OBJ:
				HistoryEntry entry = (HistoryEntry) searchResult.object;
				boolean hasTypeInDescription = !Algorithms.isEmpty(entry.getName().getTypeName());
				if (hasTypeInDescription) {
					return entry.getName().getTypeName();
				} else {
					return app.getString(R.string.shared_string_history);
				}
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
				return app.getIconsCache().getIcon(R.drawable.ic_action_building_number,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case VILLAGE:
				return app.getIconsCache().getIcon(R.drawable. ic_action_home_dark,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case POSTCODE:
			case STREET:
				return app.getIconsCache().getIcon(R.drawable.ic_action_street_name,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case HOUSE:
				return app.getIconsCache().getIcon(R.drawable.ic_action_building,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case STREET_INTERSECTION:
				return app.getIconsCache().getIcon(R.drawable.ic_action_intersection,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case POI_TYPE:
				AbstractPoiType abstractPoiType = (AbstractPoiType) searchResult.object;
				if (RenderingIcons.containsBigIcon(abstractPoiType.getIconKeyName())) {
					int iconId = RenderingIcons.getBigIconResourceId(abstractPoiType.getIconKeyName());
					return app.getIconsCache().getIcon(iconId,
							app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
				} else {
					return null;
				}
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				String id = null;
				PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
				if (st != null) {
					if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
						id = st.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
						id = st.getOsmTag() + "_" + st.getOsmValue();
					}
				}
				if (id != null) {
					int iconId = RenderingIcons.getBigIconResourceId(id);
					return app.getIconsCache().getIcon(iconId,
							app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
				} else {
					return null;
				}
			case LOCATION:
				return app.getIconsCache().getIcon(R.drawable.ic_action_coordinates_latitude,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case FAVORITE:
				FavouritePoint fav = (FavouritePoint) searchResult.object;
				return FavoriteImageDrawable.getOrCreate(app, fav.getColor(), false);
			case REGION:
				return app.getIconsCache().getIcon(R.drawable.ic_world_globe_dark,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case RECENT_OBJ:
				HistoryEntry entry = (HistoryEntry) searchResult.object;
				return app.getIconsCache().getIcon(SearchHistoryFragment.getItemIcon(entry.getName()),
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
			case WPT:
				return app.getIconsCache().getIcon(R.drawable.map_action_flag_dark,
						app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
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
