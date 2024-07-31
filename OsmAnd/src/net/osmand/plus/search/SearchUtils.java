package net.osmand.plus.search;

import static net.osmand.plus.settings.enums.HistorySource.SEARCH;
import static net.osmand.search.core.ObjectType.CITY;
import static net.osmand.search.core.ObjectType.HOUSE;
import static net.osmand.search.core.ObjectType.LOCATION;
import static net.osmand.search.core.ObjectType.ONLINE_SEARCH;
import static net.osmand.search.core.ObjectType.PARTIAL_LOCATION;
import static net.osmand.search.core.ObjectType.POI_TYPE;
import static net.osmand.search.core.ObjectType.POSTCODE;
import static net.osmand.search.core.ObjectType.STREET;
import static net.osmand.search.core.ObjectType.STREET_INTERSECTION;
import static net.osmand.search.core.ObjectType.VILLAGE;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_AMENITY_TYPE_PRIORITY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

public class SearchUtils {

	@Nullable
	public static PoiUIFilter getShowOnMapFilter(@NonNull OsmandApplication app, @NonNull SearchPhrase searchPhrase) {
		PoiUIFilter filter = null;
		if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(POI_TYPE)) {
			SearchUICore searchUICore = app.getSearchUICore().getCore();
			if (searchPhrase.isNoSelectedType()) {
				AbstractPoiType unselectedPoiType = searchUICore.getUnselectedPoiType();
				if (isOnlineSearch(searchUICore) && !Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					app.getPoiFilters().resetNominatimFilters();
					filter = app.getPoiFilters().getNominatimAddressFilter();
					filter.setFilterByName(searchPhrase.getUnknownSearchPhrase());
					filter.clearCurrentResults();
				} else if (unselectedPoiType != null) {
					filter = new PoiUIFilter(unselectedPoiType, app, "");
					String customName = searchUICore.getCustomNameFilter();
					if (!Algorithms.isEmpty(customName)) {
						filter.setFilterByName(customName);
					}
				} else {
					filter = app.getPoiFilters().getSearchByNamePOIFilter();
					if (!Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
						filter.setFilterByName(searchPhrase.getFirstUnknownSearchWord());
						filter.clearCurrentResults();
					}
				}
			} else if (searchPhrase.getLastSelectedWord().getResult().object instanceof AbstractPoiType) {
				if (searchPhrase.isNoSelectedType()) {
					filter = new PoiUIFilter(null, app, "");
				} else {
					AbstractPoiType abstractPoiType = (AbstractPoiType) searchPhrase.getLastSelectedWord().getResult().object;
					filter = new PoiUIFilter(abstractPoiType, app, "");
				}
				if (!Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					filter.setFilterByName(searchPhrase.getFirstUnknownSearchWord());
				}
			} else if (searchPhrase.getLastSelectedWord().getResult().object instanceof PoiUIFilter) {
				filter = (PoiUIFilter) searchPhrase.getLastSelectedWord().getResult().object;
				if (!Algorithms.isEmpty(searchPhrase.getFirstUnknownSearchWord())) {
					filter.setFilterByName(searchPhrase.getFirstUnknownSearchWord());
				}
			}
		}
		return filter;
	}

	public static boolean isOnlineSearch(@NonNull SearchUICore searchUICore) {
		return searchUICore.getSearchSettings().hasCustomSearchType(ONLINE_SEARCH);
	}

	public static void selectSearchResult(@NonNull OsmandApplication app, @NonNull SearchResult result) {
		if (result.object instanceof AbstractPoiType) {
			SearchHistoryHelper.getInstance(app).addNewItemToHistory((AbstractPoiType) result.object, SEARCH);
		} else if (result.object instanceof PoiUIFilter) {
			SearchHistoryHelper.getInstance(app).addNewItemToHistory((PoiUIFilter) result.object, SEARCH);
		}
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		if (result.object instanceof PoiType && ((PoiType) result.object).isAdditional()) {
			PoiType additional = (PoiType) result.object;
			AbstractPoiType parent = additional.getParentType();
			if (parent != null) {
				PoiUIFilter custom = app.getPoiFilters().getFilterById(PoiUIFilter.STD_PREFIX + parent.getKeyName());
				if (custom != null) {
					custom.clearFilter();
					custom.updateTypesToAccept(parent);
					custom.setFilterByName(additional.getKeyName().replace('_', ':').toLowerCase());

					SearchPhrase phrase = searchUICore.getPhrase();
					result = new SearchResult(phrase);
					result.localeName = custom.getName();
					result.object = custom;
					result.priority = SEARCH_AMENITY_TYPE_PRIORITY;
					result.priorityDistance = 0;
					result.objectType = ObjectType.POI_TYPE;
				}
			}
		}
		searchUICore.selectSearchResult(result);
	}

	@NonNull
	public static SearchSettings setupOnlineSearchSettings(@NonNull SearchSettings settings) {
		return settings.setSearchTypes(ONLINE_SEARCH)
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupAddressSearchSettings(@NonNull SearchSettings settings) {
		return settings.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY, VILLAGE, POSTCODE, HOUSE, STREET_INTERSECTION, STREET, LOCATION, PARTIAL_LOCATION)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupCitySearchSettings(@NonNull SearchSettings settings) {
		return settings.setEmptyQueryAllowed(true)
				.setSortByName(true)
				.setSearchTypes(CITY, VILLAGE)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupNearestCitySearchSettings(@NonNull SearchSettings settings) {
		return settings.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupLastCitySearchSettings(@NonNull SearchSettings settings, @NonNull LatLon latLon) {
		return settings.setEmptyQueryAllowed(true)
				.setSortByName(false)
				.setSearchTypes(CITY)
				.setOriginalLocation(latLon)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupPostcodeSearchSettings(@NonNull SearchSettings settings) {
		return settings.setSearchTypes(POSTCODE)
				.setEmptyQueryAllowed(false)
				.setSortByName(true)
				.setRadiusLevel(1);
	}

	@NonNull
	public static SearchSettings setupStopAddressSearchSettings(@NonNull SearchSettings settings) {
		return settings.resetSearchTypes()
				.setEmptyQueryAllowed(false)
				.setSortByName(false)
				.setRadiusLevel(1);
	}
}