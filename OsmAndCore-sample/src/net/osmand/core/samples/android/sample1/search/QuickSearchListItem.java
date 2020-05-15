package net.osmand.core.samples.android.sample1.search;

import android.graphics.drawable.Drawable;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleFormatter;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

public class QuickSearchListItem {

	protected SampleApplication app;
	private SearchResult searchResult;

	public QuickSearchListItem(SampleApplication app, SearchResult searchResult) {
		this.app = app;
		this.searchResult = searchResult;
	}

	public SearchResult getSearchResult() {
		return searchResult;
	}

	public static String getCityTypeStr(SampleApplication ctx, CityType type) {
		switch (type) {
			case CITY:
				return ctx.getString("city_type_city");
			case TOWN:
				return ctx.getString("city_type_town");
			case VILLAGE:
				return ctx.getString("city_type_village");
			case HAMLET:
				return ctx.getString("city_type_hamlet");
			case SUBURB:
				return ctx.getString("city_type_suburb");
			case DISTRICT:
				return ctx.getString("city_type_district");
			case NEIGHBOURHOOD:
				return ctx.getString("city_type_neighbourhood");
			default:
				return ctx.getString("city_type_city");
		}
	}

	public String getName() {
		return getName(app, searchResult);
	}

	public static String getName(SampleApplication app, SearchResult searchResult) {
		switch (searchResult.objectType) {
			case STREET:
				if (searchResult.localeName.endsWith(")")) {
					int i = searchResult.localeName.indexOf('(');
					if (i > 0) {
						return searchResult.localeName.substring(0, i).trim();
					}
				}
				break;
			case STREET_INTERSECTION:
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					return searchResult.localeName + " - " + searchResult.localeRelatedObjectName;
				}
				break;
			case LOCATION:
				return PointDescription.getLocationNamePlain(
						app, searchResult.location.getLatitude(), searchResult.location.getLongitude());
		}
		return searchResult.localeName;
	}

	public String getTypeName() {
		return getTypeName(app, searchResult);
	}

	public static String getTypeName(SampleApplication app, SearchResult searchResult) {
		switch (searchResult.objectType) {
			case CITY:
				City city = (City) searchResult.object;
				return getCityTypeStr(app, city.getType());
			case POSTCODE:
				return app.getString("postcode");
			case VILLAGE:
				city = (City) searchResult.object;
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					if (searchResult.distRelatedObjectName > 0) {
						return getCityTypeStr(app, city.getType())
								+ " • "
								+ SampleFormatter.getFormattedDistance((float) searchResult.distRelatedObjectName, app)
								+ " " + app.getString("shared_string_from") + " "
								+ searchResult.localeRelatedObjectName;
					} else {
						return getCityTypeStr(app, city.getType())
								+ ", "
								+ searchResult.localeRelatedObjectName;
					}
				} else {
					return getCityTypeStr(app, city.getType());
				}
			case STREET:
				StringBuilder streetBuilder = new StringBuilder();
				if (searchResult.localeName.endsWith(")")) {
					int i = searchResult.localeName.indexOf('(');
					if (i > 0) {
						streetBuilder.append(searchResult.localeName.substring(i + 1, searchResult.localeName.length() - 1));
					}
				}
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					if (streetBuilder.length() > 0) {
						streetBuilder.append(", ");
					}
					streetBuilder.append(searchResult.localeRelatedObjectName);
				}
				return streetBuilder.toString();
			case HOUSE:
				if (searchResult.relatedObject != null) {
					Street relatedStreet = (Street) searchResult.relatedObject;
					if (relatedStreet.getCity() != null) {
						return searchResult.localeRelatedObjectName + ", "
								+ relatedStreet.getCity().getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
					} else {
						return searchResult.localeRelatedObjectName;
					}
				}
				return "";
			case STREET_INTERSECTION:
				Street street = (Street) searchResult.object;
				if (street.getCity() != null) {
					return street.getCity().getName(searchResult.requiredSearchPhrase.getSettings().getLang(), true);
				}
				return "";
			case POI_TYPE:
				String res = "";
				if (searchResult.object instanceof AbstractPoiType) {
					AbstractPoiType abstractPoiType = (AbstractPoiType) searchResult.object;
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
				} else if (searchResult.object instanceof CustomSearchPoiFilter) {
					res = ((CustomSearchPoiFilter) searchResult.object).getName();
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
				LatLon latLon = searchResult.location;
				if (searchResult.localeRelatedObjectName == null) {
					String locationCountry = app.getRegions().getCountryName(latLon);
					searchResult.localeRelatedObjectName = locationCountry == null ? "" : locationCountry;
				}
				return searchResult.localeRelatedObjectName;
			case REGION:
				BinaryMapIndexReader binaryMapIndexReader = (BinaryMapIndexReader) searchResult.object;
				System.out.println(binaryMapIndexReader.getFile().getAbsolutePath() + " " + binaryMapIndexReader.getCountryName());
				break;
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return searchResult.objectType.name();
	}

	public Drawable getIcon() {
		return getIcon(app, searchResult);
	}

	public static int getPoiTypeIconId(SampleApplication app, AbstractPoiType abstractPoiType) {
		int res = OsmandResources.getBigDrawableId(abstractPoiType.getIconKeyName());
		if (res != 0) {
			return res;
		} else if (abstractPoiType instanceof PoiType) {
			res = OsmandResources.getBigDrawableId(((PoiType) abstractPoiType).getOsmTag()
					+ "_" + ((PoiType) abstractPoiType).getOsmValue());
			if (res != 0) {
				return res;
			}
		}
		if (abstractPoiType instanceof PoiType && ((PoiType) abstractPoiType).getParentType() != null) {
			return getPoiTypeIconId(app, ((PoiType) abstractPoiType).getParentType());
		}
		return 0;
	}

	public static int getAmenityIconId(Amenity amenity) {
		int res = 0;
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			res = OsmandResources.getBigDrawableId(st.getIconKeyName());
			if (res == 0) {
				res = OsmandResources.getBigDrawableId(st.getOsmTag() + "_" + st.getOsmValue());
			}
		}
		return res;
	}

	public static String getAmenityIconName(Amenity amenity) {
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			String id = st.getIconKeyName();
			if (OsmandResources.getBigDrawableId(id) == 0) {
				id = st.getOsmTag() + "_" + st.getOsmValue();
				if (OsmandResources.getBigDrawableId(id) != 0) {
					return id;
				}
			} else {
				return id;
			}
		}
		return null;
	}

	public static Drawable getIcon(SampleApplication app, SearchResult searchResult) {
		if (searchResult == null || searchResult.objectType == null) {
			return null;
		}

		int iconId;
		switch (searchResult.objectType) {
			case CITY:
				return app.getIconsCache().getIcon("ic_action_building2", R.color.osmand_orange);
			case VILLAGE:
				return app.getIconsCache().getIcon("ic_action_village", R.color.osmand_orange);
			case POSTCODE:
			case STREET:
				return app.getIconsCache().getIcon("ic_action_street_name", R.color.osmand_orange);
			case HOUSE:
				return app.getIconsCache().getIcon("ic_action_building", R.color.osmand_orange);
			case STREET_INTERSECTION:
				return app.getIconsCache().getIcon("ic_action_intersection", R.color.osmand_orange);
			case POI_TYPE:
				iconId = getPoiTypeIconId(app, (AbstractPoiType) searchResult.object);
				if (iconId != 0) {
					return app.getIconsCache().getOsmandIcon(iconId, R.color.osmand_orange);
				} else {
					return null;
				}
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				iconId = getAmenityIconId(amenity);
				if (iconId != 0) {
					return app.getIconsCache().getOsmandIcon(iconId, R.color.osmand_orange);
				} else {
					return null;
				}
			case LOCATION:
				return app.getIconsCache().getIcon("ic_action_world_globe", R.color.osmand_orange);
			case REGION:
				return app.getIconsCache().getIcon("ic_world_globe_dark", R.color.osmand_orange);
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return null;
	}
}
