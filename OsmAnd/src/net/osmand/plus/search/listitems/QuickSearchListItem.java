package net.osmand.plus.search.listitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class QuickSearchListItem {

	protected OsmandApplication app;
	private SearchResult searchResult;

	public QuickSearchListItem(OsmandApplication app, SearchResult searchResult) {
		this.app = app;
		this.searchResult = searchResult;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.SEARCH_RESULT;
	}

	public SearchResult getSearchResult() {
		return searchResult;
	}

	public static String getCityTypeStr(Context ctx, CityType type) {
		switch (type) {
			case CITY:
				return ctx.getString(R.string.city_type_city);
			case TOWN:
				return ctx.getString(R.string.city_type_town);
			case VILLAGE:
				return ctx.getString(R.string.city_type_village);
			case HAMLET:
				return ctx.getString(R.string.city_type_hamlet);
			case SUBURB:
				return ctx.getString(R.string.city_type_suburb);
			case DISTRICT:
				return ctx.getString(R.string.city_type_district);
			case NEIGHBOURHOOD:
				return ctx.getString(R.string.city_type_neighbourhood);
			default:
				return ctx.getString(R.string.city_type_city);
		}
	}

	public String getName() {
		return getName(app, searchResult);
	}

	public Spannable getSpannableName() {
		return null;
	}

	public static String getName(OsmandApplication app, SearchResult searchResult) {
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
			case RECENT_OBJ:
				HistoryEntry historyEntry = (HistoryEntry) searchResult.object;
				PointDescription pd = historyEntry.getName();
				return pd.getSimpleName(app, false);
			case LOCATION:
				LatLon latLon = searchResult.location;
				return PointDescription.getLocationNamePlain(app, latLon.getLatitude(), latLon.getLongitude());
		}
		return searchResult.localeName;
	}

	public String getTypeName() {
		String typeName = getTypeName(app, searchResult);
		return (searchResult.alternateName != null ? searchResult.alternateName + " • " : "") + typeName;
	}

	public static String getTypeName(OsmandApplication app, SearchResult searchResult) {
		switch (searchResult.objectType) {
			case CITY:
				City city = (City) searchResult.object;
				return getCityTypeStr(app, city.getType());
			case POSTCODE:
				return app.getString(R.string.postcode);
			case VILLAGE:
				city = (City) searchResult.object;
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					if (searchResult.distRelatedObjectName > 0) {
						return getCityTypeStr(app, city.getType())
								+ " • "
								+ OsmAndFormatter.getFormattedDistance((float) searchResult.distRelatedObjectName, app)
								+ " " + app.getString(R.string.shared_string_from) + " "
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
				if (latLon != null && searchResult.localeRelatedObjectName == null) {
					String locationCountry = app.getRegions().getCountryName(latLon);
					searchResult.localeRelatedObjectName = locationCountry == null ? "" : locationCountry;
				}
				return searchResult.localeRelatedObjectName;
			case FAVORITE:
				FavouritePoint fav = (FavouritePoint) searchResult.object;
				return fav.getCategory().length() == 0 ?
						app.getString(R.string.shared_string_favorites) : fav.getCategoryDisplayName(app);
			case FAVORITE_GROUP:
				return app.getString(R.string.shared_string_my_favorites);
			case REGION:
				BinaryMapIndexReader binaryMapIndexReader = (BinaryMapIndexReader) searchResult.object;
				System.out.println(binaryMapIndexReader.getFile().getAbsolutePath() + " " + binaryMapIndexReader.getCountryName());
				break;
			case RECENT_OBJ:
				HistoryEntry entry = (HistoryEntry) searchResult.object;
				boolean hasTypeInDescription = !Algorithms.isEmpty(entry.getName().getTypeName());
				if (hasTypeInDescription) {
					return entry.getName().getTypeName();
				} else {
					return "";
				}
			case WPT:
				StringBuilder sb = new StringBuilder();
				GPXFile gpx = (GPXFile) searchResult.relatedObject;
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					sb.append(searchResult.localeRelatedObjectName);
				}
				if (gpx != null && !Algorithms.isEmpty(gpx.path)) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(new File(gpx.path).getName());
				}
				return sb.toString();
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return searchResult.objectType.name();
	}

	public Drawable getTypeIcon() {
		return getTypeIcon(app, searchResult);
	}

	public static Drawable getTypeIcon(OsmandApplication app, SearchResult searchResult) {
		switch (searchResult.objectType) {
			case FAVORITE:
			case FAVORITE_GROUP:
				return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_group_name_16);
			case RECENT_OBJ:
				HistoryEntry historyEntry = (HistoryEntry) searchResult.object;
				String typeName = historyEntry.getName().getTypeName();
				if (typeName != null && !typeName.isEmpty()) {
					return app.getUIUtilities().getThemedIcon(R.drawable.ic_action_group_name_16);
				} else {
					return null;
				}
		}
		return null;
	}

	public Drawable getIcon() {
		return getIcon(app, searchResult);
	}

	@Nullable
	public static String getPoiTypeIconName(AbstractPoiType abstractPoiType) {
		if (abstractPoiType != null && RenderingIcons.containsBigIcon(abstractPoiType.getIconKeyName())) {
			return abstractPoiType.getIconKeyName();
		} else if (abstractPoiType instanceof PoiType
				&& RenderingIcons.containsBigIcon(
				((PoiType) abstractPoiType).getOsmTag() + "_" + ((PoiType) abstractPoiType).getOsmValue())) {
			return ((PoiType) abstractPoiType).getOsmTag() + "_" + ((PoiType) abstractPoiType).getOsmValue();
		}
		if (abstractPoiType instanceof PoiType && ((PoiType) abstractPoiType).getParentType() != null) {
			return getPoiTypeIconName(((PoiType) abstractPoiType).getParentType());
		}
		return null;
	}

	public static String getAmenityIconName(OsmandApplication app, Amenity amenity) {
		PoiType st = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		if (st != null) {
			if (RenderingIcons.containsBigIcon(st.getIconKeyName())) {
				return st.getIconKeyName();
			} else if (RenderingIcons.containsBigIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
				return st.getOsmTag() + "_" + st.getOsmValue();
			}
		}
		return null;
	}

	public static Drawable getIcon(OsmandApplication app, SearchResult searchResult) {
		if (searchResult == null || searchResult.objectType == null) {
			return null;
		}

		int iconId = -1;
		switch (searchResult.objectType) {
			case CITY:
				return getIcon(app, R.drawable.ic_action_building2);
			case VILLAGE:
				return getIcon(app, R.drawable.ic_action_village);
			case POSTCODE:
			case STREET:
				return getIcon(app, R.drawable.ic_action_street_name);
			case HOUSE:
				return getIcon(app, R.drawable.ic_action_building);
			case STREET_INTERSECTION:
				return getIcon(app, R.drawable.ic_action_intersection);
			case POI_TYPE:
				if (searchResult.object instanceof AbstractPoiType) {
					String iconName = getPoiTypeIconName((AbstractPoiType) searchResult.object);
					if (!Algorithms.isEmpty(iconName)) {
						iconId = RenderingIcons.getBigIconResourceId(iconName);
					}
				} else if (searchResult.object instanceof CustomSearchPoiFilter) {
					CustomSearchPoiFilter searchPoiFilter = (CustomSearchPoiFilter) searchResult.object;
					PoiUIFilter filter = app.getPoiFilters().getFilterById(searchPoiFilter.getFilterId());
					if (filter != null) {
						iconId = getCustomFilterIconRes(filter);
					}
				}
				if (iconId > 0) {
					return getIcon(app, iconId);
				} else {
					return getIcon(app, R.drawable.ic_action_search_dark);
				}
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				String id = getAmenityIconName(app, amenity);
				Drawable icon = null;
				if (id != null) {
					iconId = RenderingIcons.getBigIconResourceId(id);
					if (iconId > 0) {
						icon = getIcon(app, iconId);
					}
				}
				if (icon == null) {
					return getIcon(app, R.drawable.ic_action_search_dark);
				} else {
					return icon;
				}
			case GPX_TRACK:
				return getIcon(app, R.drawable.ic_action_polygom_dark);
			case LOCATION:
				return getIcon(app, R.drawable.ic_action_world_globe);
			case FAVORITE:
				FavouritePoint fav = (FavouritePoint) searchResult.object;
				int color = app.getFavorites().getColorWithCategory(fav, app.getResources().getColor(R.color.color_favorite));
				return PointImageDrawable.getFromFavorite(app, color, false, fav);
			case FAVORITE_GROUP:
				FavoriteGroup group = (FavoriteGroup) searchResult.object;
				color = group.getColor() == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : group.getColor();
				return app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_favorite, color | 0xff000000);
			case REGION:
				return getIcon(app, R.drawable.ic_world_globe_dark);
			case RECENT_OBJ:
				HistoryEntry entry = (HistoryEntry) searchResult.object;
				iconId = getHistoryIconId(app, entry);
				try {
					return getIcon(app, iconId);
				} catch (Exception e) {
					return getIcon(app, SearchHistoryFragment.getItemIcon(entry.getName()));
				}
			case WPT:
				WptPt wpt = (WptPt) searchResult.object;
				return PointImageDrawable.getFromWpt(app, wpt.getColor(), false, wpt);
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return null;
	}

	public static int getHistoryIconId(OsmandApplication app, HistoryEntry entry) {
		int iconId = -1;
		if (entry.getName() != null && !Algorithms.isEmpty(entry.getName().getIconName())) {
			String iconName = entry.getName().getIconName();
			if (RenderingIcons.containsBigIcon(iconName)) {
				iconId = RenderingIcons.getBigIconResourceId(iconName);
			} else {
				iconId = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
			}
		}
		if (iconId <= 0) {
			iconId = SearchHistoryFragment.getItemIcon(entry.getName());
		}
		return iconId;
	}

	private static Drawable getIcon(OsmandApplication app, int iconId) {
		return app.getUIUtilities().getIcon(iconId,
				app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
	}

	@DrawableRes
	public static int getCustomFilterIconRes(PoiUIFilter filter) {
		int iconId = 0;
		if (filter != null) {
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = filter.getAcceptedTypes();
			List<PoiCategory> categories = new ArrayList<>(acceptedTypes.keySet());
			if (categories.size() == 1) {
				String res = "";
				PoiCategory category = categories.get(0);
				LinkedHashSet<String> filters = acceptedTypes.get(category);
				if (filters == null || filters.size() > 1) {
					res = category.getIconKeyName();
				} else {
					res = getPoiTypeIconName(category.getPoiTypeByKeyName(filters.iterator().next()));
				}
				if (res != null && RenderingIcons.containsBigIcon(res)) {
					iconId = RenderingIcons.getBigIconResourceId(res);
				}
			}
		}
		return iconId > 0 ? iconId : R.drawable.mx_special_custom_category;
	}
}
