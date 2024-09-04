package net.osmand.plus.search.listitems;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.util.Pair;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.Street;
import net.osmand.data.WptLocationPoint;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.poi.PoiFilterUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class QuickSearchListItem {

	protected final OsmandApplication app;
	private final SearchResult searchResult;

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
		String alternateName = searchResult.alternateName;
		if (searchResult.object instanceof Amenity) {
			Amenity amenity = (Amenity) searchResult.object;
			alternateName = amenity.getTranslation(app.getPoiTypes(), searchResult.alternateName);
		}
		return alternateName != null ? typeName + " • " + alternateName : typeName;
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
				} else if (searchResult.object instanceof SearchPoiAdditionalFilter) {
					String name = ((SearchPoiAdditionalFilter) searchResult.object).getName();
					res = name;
				}
				return res;
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				return amenity.getSubTypeStr();
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
				GpxFile gpx = (GpxFile) searchResult.relatedObject;
				if (!Algorithms.isEmpty(searchResult.localeRelatedObjectName)) {
					sb.append(searchResult.localeRelatedObjectName);
				}
				if (gpx != null && !Algorithms.isEmpty(gpx.getPath())) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(new File(gpx.getPath()).getName());
				}
				return sb.toString();
			case MAP_MARKER:
				MapMarker marker = (MapMarker) searchResult.object;
				String desc = OsmAndFormatter.getFormattedDate(app, marker.creationDate);
				String markerGroupName = marker.groupName;
				if (markerGroupName != null) {
					if (markerGroupName.isEmpty()) {
						markerGroupName = app.getString(R.string.shared_string_favorites);
					}
					desc += " • " + markerGroupName;
				}
				return desc;
			case ROUTE:
				return "";
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

	public static String getAmenityIconName(@NonNull Amenity amenity) {
		return RenderingIcons.getIconNameForAmenity(amenity);
	}

	@Nullable
	public static Drawable getIcon(OsmandApplication app, SearchResult searchResult) {
		if (searchResult == null || searchResult.objectType == null) {
			return null;
		}

		int iconId = -1;
		switch (searchResult.objectType) {
			case CITY:
				boolean town = (searchResult.object instanceof City)
						&& (((City) searchResult.object).getType() == CityType.TOWN);
				return town
						? getIcon(app, R.drawable.mx_place_town)
						: getIcon(app, R.drawable.ic_action_building2);
			case VILLAGE:
				return getIcon(app, R.drawable.mx_village);
			case POSTCODE:
			case STREET:
				return getIcon(app, R.drawable.ic_action_street_name);
			case HOUSE:
				return getIcon(app, R.drawable.ic_action_building);
			case STREET_INTERSECTION:
				return getIcon(app, R.drawable.ic_action_intersection);
			case POI_TYPE:
				if (searchResult.object instanceof AbstractPoiType) {
					String iconName = PoiFilterUtils.getPoiTypeIconName((AbstractPoiType) searchResult.object);
					if (Algorithms.isEmpty(iconName) && searchResult.object instanceof PoiType) {
						iconName = RenderingIcons.getIconNameForPoiType((PoiType) searchResult.object);
					}
					if (!Algorithms.isEmpty(iconName)) {
						iconId = RenderingIcons.getBigIconResourceId(iconName);
					}
				} else if (searchResult.object instanceof CustomSearchPoiFilter) {
					CustomSearchPoiFilter searchPoiFilter = (CustomSearchPoiFilter) searchResult.object;
					PoiUIFilter filter = app.getPoiFilters().getFilterById(searchPoiFilter.getFilterId());
					if (filter != null) {
						iconId = getCustomFilterIconRes(filter);
					}
				} else if (searchResult.object instanceof SearchPoiAdditionalFilter) {
					SearchPoiAdditionalFilter filter = (SearchPoiAdditionalFilter) searchResult.object;
					iconId = RenderingIcons.getBigIconResourceId(filter.getIconResource());
				}
				if (iconId > 0) {
					return getIcon(app, iconId);
				} else {
					return getIcon(app, R.drawable.ic_action_search_dark);
				}
			case POI:
				Amenity amenity = (Amenity) searchResult.object;
				String id = getAmenityIconName(amenity);
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
				int color = app.getFavoritesHelper().getColorWithCategory(fav, ContextCompat.getColor(app, R.color.color_favorite));
				return PointImageUtils.getFromPoint(app, color, false, fav);
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
					return getIcon(app, entry.getName().getItemIcon());
				}
			case WPT:
				WptPt wpt = (WptPt) searchResult.object;
				return PointImageUtils.getFromPoint(app, wpt.getColor(), false, wpt);
			case MAP_MARKER:
				MapMarker marker = (MapMarker) searchResult.object;
				if (!marker.history) {
					return MapMarkerDialogHelper.getMapMarkerIcon(app, marker.colorIndex);
				} else {
					return getIcon(app, R.drawable.ic_action_flag);
				}
			case ROUTE:
				return getIcon(app, R.drawable.ic_action_previous_route);
			case UNKNOWN_NAME_FILTER:
				break;
		}
		return null;
	}

	public static int getHistoryIconId(@NonNull OsmandApplication app, @NonNull HistoryEntry entry) {
		int iconId = -1;
		PointDescription name = entry.getName();
		if (name != null && !Algorithms.isEmpty(name.getIconName())) {
			String iconName = name.getIconName();
			if (RenderingIcons.containsBigIcon(iconName)) {
				iconId = RenderingIcons.getBigIconResourceId(iconName);
			} else {
				iconId = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
			}
		}
		if (iconId <= 0 && name != null) {
			iconId = name.getItemIcon();
		}
		return iconId;
	}

	@NonNull
	public static Pair<PointDescription, Object> getPointDescriptionObject(@NonNull OsmandApplication app, @NonNull SearchResult searchResult) {
		SearchSettings settings = searchResult.requiredSearchPhrase.getSettings();
		String lang;
		boolean transliterate;
		if (settings != null) {
			lang = settings.getLang();
			transliterate = settings.isTransliterate();
		} else {
			lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			transliterate = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
		}
		PointDescription pointDescription = null;
		Object object = searchResult.object;
		switch (searchResult.objectType) {
			case POI:
				Amenity a = (Amenity) object;
				String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType(a, lang, transliterate);
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI, poiSimpleFormat);
				pointDescription.setIconName(getAmenityIconName(a));
				break;
			case RECENT_OBJ:
				HistoryEntry entry = (HistoryEntry) object;
				pointDescription = entry.getName();
				if (pointDescription.isPoi()) {
					Amenity amenity = app.getSearchUICore().findAmenity(entry.getName().getName(), entry.getLat(), entry.getLon(), lang, transliterate);
					if (amenity != null) {
						object = amenity;
						pointDescription = new PointDescription(PointDescription.POINT_TYPE_POI,
								OsmAndFormatter.getPoiStringWithoutType(amenity, lang, transliterate));
						pointDescription.setIconName(getAmenityIconName(amenity));
					}
				} else if (pointDescription.isFavorite()) {
					LatLon entryLatLon = new LatLon(entry.getLat(), entry.getLon());
					List<FavouritePoint> favs = app.getFavoritesHelper().getFavouritePoints();
					for (FavouritePoint f : favs) {
						if (entryLatLon.equals(new LatLon(f.getLatitude(), f.getLongitude()))
								&& (pointDescription.getName().equals(f.getName()) ||
								pointDescription.getName().equals(f.getDisplayName(app)))) {
							object = f;
							pointDescription = f.getPointDescription(app);
							break;
						}
					}
				}
				break;
			case FAVORITE:
				FavouritePoint fav = (FavouritePoint) object;
				pointDescription = fav.getPointDescription(app);
				break;
			case VILLAGE:
			case CITY:
				String cityName = searchResult.localeName;
				String typeNameCity = getTypeName(app, searchResult);
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeNameCity, cityName);
				pointDescription.setIconName("ic_action_building_number");
				break;
			case STREET:
				String streetName = searchResult.localeName;
				String typeNameStreet = getTypeName(app, searchResult);
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeNameStreet, streetName);
				pointDescription.setIconName("ic_action_street_name");
				break;
			case HOUSE:
				String typeNameHouse = null;
				String name = searchResult.localeName;
				if (searchResult.relatedObject instanceof City) {
					name = ((City) searchResult.relatedObject).getName(lang, true) + " " + name;
				} else if (searchResult.relatedObject instanceof Street) {
					String s = ((Street) searchResult.relatedObject).getName(lang, true);
					typeNameHouse = ((Street) searchResult.relatedObject).getCity().getName(lang, true);
					name = s + " " + name;
				} else if (searchResult.localeRelatedObjectName != null) {
					name = searchResult.localeRelatedObjectName + " " + name;
				}
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeNameHouse, name);
				pointDescription.setIconName("ic_action_building");
				break;
			case LOCATION:
				pointDescription = new PointDescription(
						searchResult.location.getLatitude(), searchResult.location.getLongitude());
				pointDescription.setIconName("ic_action_world_globe");
				break;
			case STREET_INTERSECTION:
				String typeNameIntersection = getTypeName(app, searchResult);
				if (Algorithms.isEmpty(typeNameIntersection)) {
					typeNameIntersection = null;
				}
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS,
						typeNameIntersection, getName(app, searchResult));
				pointDescription.setIconName("ic_action_intersection");
				break;
			case WPT:
				WptPt wpt = (WptPt) object;
				pointDescription = new WptLocationPoint(wpt).getPointDescription(app);
				break;
		}
		return new Pair<>(pointDescription, object);
	}

	private static Drawable getIcon(OsmandApplication app, int iconId) {
		return app.getUIUtilities().getIcon(iconId,
				app.getSettings().isLightContent() ? R.color.osmand_orange : R.color.osmand_orange_dark);
	}

	@DrawableRes
	public static int getCustomFilterIconRes(@Nullable PoiUIFilter filter) {
		int iconId = 0;
		String iconName = PoiFilterUtils.getCustomFilterIconName(filter);
		if (iconName != null && RenderingIcons.containsBigIcon(iconName)) {
			iconId = RenderingIcons.getBigIconResourceId(iconName);
		}
		return iconId > 0 ? iconId : R.drawable.mx_special_custom_category;
	}
}
