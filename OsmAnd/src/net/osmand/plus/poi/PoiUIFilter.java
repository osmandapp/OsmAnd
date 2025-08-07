package net.osmand.plus.poi;


import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
import static net.osmand.osm.MapPoiTypes.OSM_WIKI_CATEGORY;
import static net.osmand.osm.MapPoiTypes.ROUTES;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.WIKI_PLACE;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.poi.PoiFilterUtils.AmenityNameFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.POIMapLayer.PoiUIFilterResultMatcher;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class PoiUIFilter implements Comparable<PoiUIFilter>, CustomSearchPoiFilter {

	public static final String STD_PREFIX = "std_";
	public static final String ONLINE_PREFIX = "online_";
	public static final String USER_PREFIX = "user_";
	public static final String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id";
	public static final String BY_NAME_FILTER_ID = USER_PREFIX + "by_name";
	public static final String TOP_WIKI_FILTER_ID = STD_PREFIX + OSM_WIKI_CATEGORY;
	public static final int INVALID_ORDER = -1;

	private Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<>();
	private Map<PoiCategory, LinkedHashSet<String>> acceptedTypesOrigin = new LinkedHashMap<>();
	private Map<String, PoiType> poiAdditionals = new HashMap<>();

	protected String filterId;
	protected String standardIconId = "";
	protected String name;
	protected boolean isStandardFilter;
	protected int order = INVALID_ORDER;
	protected boolean isActive = true;

	protected final OsmandApplication app;

	protected int distanceInd;
	// in kilometers
	protected double[] distanceToSearchValues = {1, 2, 5, 10, 20, 50, 100, 200, 500};

	private final MapPoiTypes poiTypes;

	protected String filterByName;
	protected String savedFilterByName;
	protected List<Amenity> currentSearchResult;
	protected String filterByKey = null;

	private boolean deleted;

	SearchPoiAdditionalFilter additionalFilter;

	private final PoiUIFilterDataProvider dataProvider;

	// constructor for standard filters
	public PoiUIFilter(@Nullable AbstractPoiType type, @NonNull OsmandApplication app, @NonNull String idSuffix) {
		this.app = app;
		this.dataProvider = new PoiUIFilterDataProvider(app, this);
		isStandardFilter = true;
		standardIconId = type == null ? null : type.getKeyName();
		filterId = STD_PREFIX + standardIconId + idSuffix;

		poiTypes = app.getPoiTypes();
		name = type == null ? app.getString(R.string.poi_filter_closest_poi) : (type.getTranslation() + idSuffix);
		if (type == null) {
			initSearchAll();
			updatePoiAdditionals();
		} else {
			if (type.isAdditional()) {
				setSavedFilterByName(type.getKeyName().replace('_', ':'));
			}
			updateTypesToAccept(type);
		}
	}

	public PoiUIFilter(TopIndexFilter topIndexFilter, @Nullable Map<PoiCategory, LinkedHashSet<String>> acceptedTypes, @NonNull OsmandApplication app) {
		this.app = app;
		this.dataProvider = new PoiUIFilterDataProvider(app, this);
		isStandardFilter = true;
		standardIconId = topIndexFilter.getIconResource();
		filterId = topIndexFilter.getFilterId();
		this.name = topIndexFilter.getName();
		poiTypes = app.getPoiTypes();
		additionalFilter = topIndexFilter;

		if (acceptedTypes == null) {
			initSearchAll();
		} else {
			this.acceptedTypes.putAll(acceptedTypes);
		}
		updatePoiAdditionals();
		updateAcceptedTypeOrigins();
	}

	// search by name standard
	protected PoiUIFilter(@NonNull OsmandApplication app) {
		this.app = app;
		this.dataProvider = new PoiUIFilterDataProvider(app, this);
		isStandardFilter = true;
		filterId = STD_PREFIX; // overridden
		poiTypes = app.getPoiTypes();
	}

	// constructor for user defined filters
	public PoiUIFilter(@NonNull String name, @Nullable String filterId,
	                   @Nullable Map<PoiCategory, LinkedHashSet<String>> acceptedTypes,
	                   @NonNull OsmandApplication app) {
		this.app = app;
		this.dataProvider = new PoiUIFilterDataProvider(app, this);
		isStandardFilter = false;
		poiTypes = app.getPoiTypes();
		if (filterId == null) {
			filterId = USER_PREFIX + name.replace(' ', '_').toLowerCase();
		}
		this.filterId = filterId;
		this.name = name;
		if (acceptedTypes == null) {
			initSearchAll();
		} else {
			this.acceptedTypes.putAll(acceptedTypes);
		}
		updatePoiAdditionals();
		updateAcceptedTypeOrigins();
	}

	public PoiUIFilter(@NonNull Set<PoiUIFilter> filtersToMerge, @NonNull OsmandApplication app) {
		this(app);
		combineWithPoiFilters(filtersToMerge);
		filterId = STD_PREFIX + "combined";
		name = app.getPoiFilters().getFiltersName(filtersToMerge);
	}

	public PoiUIFilter(@NonNull PoiUIFilter filter, @NonNull String name, @NonNull String filterId) {
		this.app = filter.app;
		this.dataProvider = new PoiUIFilterDataProvider(app, this);
		this.name = name;
		this.filterId = filterId;
		isStandardFilter = false;
		poiTypes = filter.poiTypes;
		acceptedTypes = filter.getAcceptedTypes();
		poiAdditionals = filter.getPoiAdditionals();
		filterByName = filter.filterByName;
		savedFilterByName = filter.savedFilterByName;
		updateAcceptedTypeOrigins();
	}

	@NonNull
	public List<Amenity> getCurrentSearchResult() {
		return currentSearchResult == null ? Collections.emptyList() : new ArrayList<>(currentSearchResult);
	}

	public DataSourceType getDataSourceType() {
		return dataProvider.getDataSourceType();
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isWikiFilter() {
		return filterId.startsWith(STD_PREFIX + WIKI_PLACE) || isTopWikiFilter();
	}

	public boolean isRatingSorted() {
		return isWikiFilter();
	}

	public boolean isTopWikiFilter() {
		return filterId.equals(TOP_WIKI_FILTER_ID);
	}

	public boolean isTopImagesFilter() {
		return isTopWikiFilter();
	}

	public boolean isRoutesFilter() {
		return filterId.startsWith(STD_PREFIX + ROUTES);
	}

	public boolean isRouteArticleFilter() {
		return filterId.startsWith(STD_PREFIX + ROUTE_ARTICLE);
	}

	public boolean isRouteArticlePointFilter() {
		return filterId.startsWith(STD_PREFIX + ROUTE_ARTICLE_POINT);
	}

	public boolean isShowPrivateNeeded() {
		return filterByName != null && filterByName.contains("access:private");
	}

	public String getFilterByName() {
		return filterByName;
	}

	public void setFilterByName(String filterByName) {
		this.filterByName = filterByName;
		updateFilterResults();
	}

	public void setFilterByKey(String key) {
		filterByKey = key;
	}

	public void removeUnsavedFilterByName() {
		filterByName = savedFilterByName;
		updateFilterResults();
	}

	public void updateFilterResults() {
		List<Amenity> prev = currentSearchResult;
		if (prev != null) {
			AmenityNameFilter nameFilter = getNameFilter();
			List<Amenity> newResults = new ArrayList<>();
			for (Amenity a : prev) {
				if (nameFilter.accept(a)) {
					newResults.add(a);
				}
			}
			currentSearchResult = newResults;
		}
	}

	public void setSavedFilterByName(String filterByName) {
		this.filterByName = filterByName;
		this.savedFilterByName = filterByName;
	}

	public String getSavedFilterByName() {
		return savedFilterByName;
	}

	public List<Amenity> searchAgain(double lat, double lon) {
		List<Amenity> amenityList;
		if (currentSearchResult != null) {
			amenityList = currentSearchResult;
		} else {
			amenityList = searchAmenities(lat, lon, null);
		}
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		return amenityList;
	}


	public List<Amenity> searchFurther(double latitude, double longitude, ResultMatcher<Amenity> matcher) {
		if (distanceInd < distanceToSearchValues.length - 1) {
			distanceInd++;
		}
		List<Amenity> amenityList = searchAmenities(latitude, longitude, matcher);
		MapUtils.sortListOfMapObject(amenityList, latitude, longitude);
		return amenityList;
	}

	private void initSearchAll() {
		for (PoiCategory t : poiTypes.getCategories(false)) {
			acceptedTypes.put(t, null);
		}
		distanceToSearchValues = new double[] {0.5, 1, 2, 5, 10, 20, 50, 100};
	}

	public boolean isSearchFurtherAvailable() {
		return distanceInd < distanceToSearchValues.length - 1;
	}

	public String getSearchArea(boolean next) {
		int distInd = distanceInd;
		if (next && (distanceInd < distanceToSearchValues.length - 1)) {
			//This is workaround for the SearchAmenityTask.onPreExecute() case
			distInd = distanceInd + 1;
		}
		double val = distanceToSearchValues[distInd];
		if (val >= 1) {
			return " < " + OsmAndFormatter.getFormattedDistance(((int) val * 1000), app);
		} else {
			return " < " + OsmAndFormatter.getFormattedDistance(500, app);
		}
	}

	public void clearPreviousZoom() {
		distanceInd = 0;
	}

	public void clearCurrentResults() {
		if (currentSearchResult != null) {
			currentSearchResult = new ArrayList<>();
		}
	}

	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit,
	                                         ResultMatcher<Amenity> matcher, int radius) {
		if (radius < 0) {
			clearPreviousZoom();
		} else if (radius < distanceToSearchValues.length) {
			distanceInd = radius;
		} else {
			distanceInd = distanceToSearchValues.length - 1;
		}
		List<Amenity> amenityList = searchAmenities(lat, lon, matcher);
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		if (firstTimeLimit > 0) {
			while (amenityList.size() > firstTimeLimit) {
				amenityList.remove(amenityList.size() - 1);
			}
		}
		if (amenityList.size() == 0 && isAutomaticallyIncreaseSearch()) {
			int step = 5;
			while (amenityList.size() == 0 && step-- > 0 && isSearchFurtherAvailable()) {
				if (matcher != null && matcher.isCancelled()) {
					break;
				}
				amenityList = searchFurther(lat, lon, matcher);
			}
		}
		return amenityList;
	}

	public double getSearchRadius(int radius) {
		if (radius < 0) {
			distanceInd = 0;
		} else if (radius < distanceToSearchValues.length) {
			distanceInd = radius;
		} else {
			distanceInd = distanceToSearchValues.length - 1;
		}
		return distanceToSearchValues[distanceInd] * 1000;
	}

	public int getMaxSearchRadiusIndex() {
		return distanceToSearchValues.length - 1;
	}

	public boolean isAutomaticallyIncreaseSearch() {
		return true;
	}

	private List<Amenity> searchAmenities(double lat, double lon, ResultMatcher<Amenity> matcher) {
		double baseDistY = MapUtils.getDistance(lat, lon, lat - 1, lon);
		double baseDistX = MapUtils.getDistance(lat, lon, lat, lon - 1);
		double distance = distanceToSearchValues[distanceInd] * 1000;
		double topLatitude = Math.min(lat + (distance / baseDistY), 84.);
		double bottomLatitude = Math.max(lat - (distance / baseDistY), -84.);
		double leftLongitude = Math.max(lon - (distance / baseDistX), -180);
		double rightLongitude = Math.min(lon + (distance / baseDistX), 180);
		return searchAmenitiesInternal(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude, -1, matcher);
	}

	public List<Amenity> searchAmenities(double top, double left, double bottom, double right, int zoom, ResultMatcher<Amenity> matcher) {
		Set<Amenity> results = new HashSet<>();
		if (currentSearchResult != null) {
			List<Amenity> tempResults = new ArrayList<>(currentSearchResult);
			for (Amenity a : tempResults) {
				LatLon l = a.getLocation();
				if (l != null && l.getLatitude() <= top && l.getLatitude() >= bottom
						&& l.getLongitude() >= left && l.getLongitude() <= right) {
					if (matcher == null || matcher.publish(a)) {
						results.add(a);
					}
				}
			}
		}
		List<Amenity> amenities = searchAmenitiesInternal(top / 2 + bottom / 2, left / 2 + right / 2,
				top, bottom, left, right, zoom, matcher);
		results.addAll(amenities);
		ArrayList<Amenity> resultList = new ArrayList<>(results);
		if(isTopWikiFilter()) {
			Collections.sort(resultList, (p1, p2) -> p2.getTravelEloNumber() - p1.getTravelEloNumber());
		}
		return resultList;
	}

	public List<Amenity> searchAmenitiesOnThePath(List<Location> locs, int poiSearchDeviationRadius) {
		return app.getResourceManager().getAmenitySearcher().searchAmenitiesOnThePath(locs, poiSearchDeviationRadius, this, wrapResultMatcher(null));
	}

	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
	                                                double bottomLatitude, double leftLongitude,
	                                                double rightLongitude, int zoom,
	                                                ResultMatcher<Amenity> matcher) {
		currentSearchResult = dataProvider.searchAmenities(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude, zoom, matcher);
		if (isTopWikiFilter()) {
			Collections.sort(currentSearchResult, (p1, p2) -> p2.getTravelEloNumber() - p1.getTravelEloNumber());
		}
		return currentSearchResult;
	}

	public PoiFilterUtils.AmenityNameFilter getNameFilter() {
		if (Algorithms.isEmpty(filterByName)) {
			return a -> true;
		}
		if (!Algorithms.isEmpty(filterByKey)) {
			return getKeyNameFilter(filterByKey, filterByName);
		}
		String[] items = filterByName.split(" ");
		boolean allTime = false;
		boolean open = false;
		List<PoiType> poiAdditionalsFilter = null;
		List<String> unknownFilters = null;
		for (String s : items) {
			s = s.trim();
			if (!Algorithms.isEmpty(s)) {
				if (getNameToken24H().equalsIgnoreCase(s)) {
					allTime = true;
				} else if (getNameTokenOpen().equalsIgnoreCase(s)) {
					open = true;
				} else if (poiAdditionals.containsKey(s.toLowerCase())) {
					if (poiAdditionalsFilter == null) {
						poiAdditionalsFilter = new ArrayList<>();
					}
					PoiType pt = poiAdditionals.get(s.toLowerCase());
					if (pt != null) {
						poiAdditionalsFilter.add(pt);
					}
				} else {
					if (unknownFilters == null) {
						unknownFilters = new ArrayList<>();
					}
					unknownFilters.add(s);
				}
			}
		}
		return getNameFilterInternal(unknownFilters, allTime, open, poiAdditionalsFilter);
	}

	@NonNull
	private PoiFilterUtils.AmenityNameFilter getNameFilterInternal(List<String> unknownFilters,
	                                                               boolean shouldBeAllTime,
	                                                               boolean shouldBeOpened,
	                                                               List<PoiType> selectedFilters) {
		return amenity -> {
			if (shouldBeAllTime) {
				if (!"24/7".equalsIgnoreCase(amenity.getOpeningHours())
						&& !"Mo-Su 00:00-24:00".equalsIgnoreCase(amenity.getOpeningHours())) {
					return false;
				}
			}
			if (shouldBeOpened && !isOpened(amenity)) {
				return false;
			}
			String nameFilter = extractNameFilter(amenity, unknownFilters);
			if (!matchesAnyAmenityName(amenity, nameFilter)) {
				return false;
			}
			return acceptedAnyFilterOfEachCategory(amenity, selectedFilters);
		};
	}

	public PoiFilterUtils.AmenityNameFilter getKeyNameFilter(String key, String value) {
		return amenity -> {
			String val = amenity.getAdditionalInfo(key);
			return val != null && val.equalsIgnoreCase(value);
		};
	}

	private boolean isOpened(@NonNull Amenity amenity) {
		OpeningHours openedHours = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
		if (openedHours == null) {
			return false;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());

		return openedHours.isOpenedForTime(calendar);
	}

	private String extractNameFilter(@NonNull Amenity amenity, @Nullable List<String> unknownFilters) {
		if (unknownFilters == null) {
			return "";
		}

		StringBuilder nameFilter = new StringBuilder();
		for (String filter : unknownFilters) {
			String formattedFilter = filter.replace(':', '_').toLowerCase();
			if (amenity.getAdditionalInfo(formattedFilter) == null) {
				nameFilter.append(filter).append(" ");
			}
		}

		return nameFilter.toString();
	}

	private boolean matchesAnyAmenityName(@NonNull Amenity amenity, @NonNull String nameFilter) {
		if (nameFilter.length() == 0) {
			return true;
		}
		CollatorStringMatcher matcher = new CollatorStringMatcher(nameFilter.trim(), CHECK_STARTS_FROM_SPACE);
		OsmandSettings settings = app.getSettings();
		List<String> names = OsmAndFormatter.getPoiStringsWithoutType(amenity,
				settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get());

		for (String name : names) {
			if (matcher.matches(name)) {
				return true;
			}
		}
		return false;
	}

	private boolean acceptedAnyFilterOfEachCategory(@NonNull Amenity amenity, @Nullable List<PoiType> selectedFilters) {
		if (selectedFilters == null) {
			return true;
		}

		Map<String, List<PoiType>> filterCategories = new HashMap<>();
		Map<PoiType, PoiType> textFilters = new HashMap<>();

		fillFilterCategories(selectedFilters, filterCategories, textFilters);

		for (List<PoiType> category : filterCategories.values()) {
			if (!acceptedAnyFilterOfCategory(amenity, category, textFilters)) {
				return false;
			}
		}

		return true;
	}

	private void fillFilterCategories(@NonNull List<PoiType> selectedFilters,
	                                  @NonNull Map<String, List<PoiType>> filterCategories,
	                                  @NonNull Map<PoiType, PoiType> textFilters) {
		for (PoiType filter : selectedFilters) {
			String category = filter.getPoiAdditionalCategory();
			List<PoiType> filtersOfCategory = filterCategories.get(category);
			if (filtersOfCategory == null) {
				filtersOfCategory = new ArrayList<>();
				filterCategories.put(category, filtersOfCategory);
			}
			filtersOfCategory.add(filter);

			String osmTag = filter.getOsmTag();
			if (osmTag.length() < filter.getKeyName().length()) {
				PoiType textFilter = poiTypes.getTextPoiAdditionalByKey(osmTag);
				if (textFilter != null) {
					textFilters.put(filter, textFilter);
				}
			}
		}
	}

	private boolean acceptedAnyFilterOfCategory(@NonNull Amenity amenity, @NonNull List<PoiType> category,
	                                            @NonNull Map<PoiType, PoiType> textFilters) {
		for (PoiType filter : category) {
			if (acceptedFilter(amenity, filter, textFilters)) {
				return true;
			}
		}

		return false;
	}

	private boolean acceptedFilter(@NonNull Amenity amenity, @NonNull PoiType filter,
	                               @NonNull Map<PoiType, PoiType> textFilterCategories) {
		String filterValue = amenity.getAdditionalInfo(filter.getKeyName());

		if (filterValue != null) {
			return true;
		}

		PoiType textPoiType = textFilterCategories.get(filter);
		if (textPoiType == null) {
			return false;
		}

		filterValue = amenity.getAdditionalInfo(textPoiType.getKeyName());
		if (Algorithms.isEmpty(filterValue)) {
			return false;
		}

		String[] items = filterValue.split(";");
		String val = filter.getOsmValue().trim().toLowerCase();
		for (String item : items) {
			if (item.trim().toLowerCase().equals(val)) {
				return true;
			}
		}

		return false;
	}

	public String getNameToken24H() {
		return app.getString(R.string.shared_string_is_open_24_7).replace(' ', '_').toLowerCase();
	}

	public String getNameTokenOpen() {
		return app.getString(R.string.shared_string_is_open).replace(' ', '_').toLowerCase();
	}

	@Override
	public Object getIconResource() {
		return getIconId();
	}

	@Override
	public ResultMatcher<Amenity> wrapResultMatcher(@Nullable ResultMatcher<Amenity> matcher) {
		// Deduplication might break live-updates consistency in case of multiple maps.
		// Ensure correct readers order, such as returned by getAmenityRepositories()
		Set<String> distinctAmenities = new TreeSet<>();

		PoiFilterUtils.AmenityNameFilter nm = getNameFilter();

		return new PoiUIFilterResultMatcher<Amenity>() {

			@Override
			public void defferedResults() {
				if (matcher instanceof PoiUIFilterResultMatcher) {
					((PoiUIFilterResultMatcher<?>) matcher).defferedResults();
				}
			}

			@Override
			public boolean publish(Amenity a) {
				if (nm.accept(a)) {
					if (matcher == null || matcher.publish(a)) {
						String amenityDistinctId = a.getType().getKeyName() + a.getId();
						if (!distinctAmenities.add(amenityDistinctId)) {
							return false;
						}
						return !a.isClosed();
					}
				}
				return false;
			}

			@Override
			public boolean isCancelled() {
				return matcher != null && matcher.isCancelled();
			}
		};
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGeneratedName(int chars) {
		if (!isCustomPoiFilter() || areAllTypesAccepted() || acceptedTypes.isEmpty()) {
			return getName();
		}
		StringBuilder res = new StringBuilder();
		for (Entry<PoiCategory, LinkedHashSet<String>> entry : acceptedTypes.entrySet()) {
			LinkedHashSet<String> set = entry.getValue();
			if (set == null) {
				if (res.length() > 0) {
					res.append(", ");
				}
				res.append(entry.getKey().getTranslation());
			}
			if (res.length() > chars) {
				return res.toString();
			}
		}
		for (LinkedHashSet<String> set : acceptedTypes.values()) {
			if (set != null) {
				for (String st : set) {
					if (res.length() > 0) {
						res.append(", ");
					}
					PoiType pt = poiTypes.getPoiTypeByKey(st);
					if (pt != null) {
						res.append(pt.getTranslation());
						if (res.length() > chars) {
							return res.toString();
						}
					}
				}
			}
		}
		return res.toString();
	}

	@NonNull
	public String getTypesName() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<PoiCategory, LinkedHashSet<String>> entry : acceptedTypes.entrySet()) {
			LinkedHashSet<String> set = entry.getValue();
			if (set != null && !set.isEmpty()) {
				for (String key : set) {
					PoiType pt = poiTypes.getPoiTypeByKey(key);
					if (pt != null) {
						appendWithSeparator(sb, pt.getTranslation());
					}
				}
			} else {
				appendWithSeparator(sb, entry.getKey().getTranslation());
			}
		}
		return sb.toString();
	}

	private void appendWithSeparator(@NonNull StringBuilder sb, String s) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(s);
	}

	/**
	 * @param type
	 * @return null if all subtypes are accepted/ empty list if type is not accepted at all
	 */
	public Set<String> getAcceptedSubtypes(@NonNull PoiCategory type) {
		if (!acceptedTypes.containsKey(type)) {
			return Collections.emptySet();
		}
		return acceptedTypes.get(type);
	}

	public boolean isTypeAccepted(@NonNull PoiCategory t) {
		return acceptedTypes.containsKey(t);
	}

	public void clearFilter() {
		acceptedTypes = new LinkedHashMap<>();
		poiAdditionals.clear();
		filterByName = null;
		clearCurrentResults();
	}

	public boolean areAllTypesAccepted() {
		if (poiTypes.getCategories(false).size() == acceptedTypes.size()) {
			for (LinkedHashSet<String> strings : acceptedTypes.values()) {
				if (strings != null) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public void updateTypesToAccept(@NonNull AbstractPoiType pt) {
		pt.putTypes(acceptedTypes);
		if (pt instanceof PoiType && pt.isAdditional() && ((PoiType) pt).getParentType() != null) {
			fillPoiAdditionals(((PoiType) pt).getParentType(), true);
		} else {
			fillPoiAdditionals(pt, true);
		}
		addOtherPoiAdditionals();
	}

	private void fillPoiAdditionals(@NonNull AbstractPoiType pt, boolean allFromCategory) {
		for (PoiType add : pt.getPoiAdditionals()) {
			poiAdditionals.put(add.getKeyName().replace('_', ':').replace(' ', ':'), add);
			poiAdditionals.put(add.getTranslation().replace(' ', ':').toLowerCase(), add);
		}
		if (pt instanceof PoiCategory && allFromCategory) {
			for (PoiFilter pf : ((PoiCategory) pt).getPoiFilters()) {
				fillPoiAdditionals(pf, true);
			}
			for (PoiType ps : ((PoiCategory) pt).getPoiTypes()) {
				fillPoiAdditionals(ps, false);
			}
		} else if (pt instanceof PoiFilter && !(pt instanceof PoiCategory)) {
			for (PoiType ps : ((PoiFilter) pt).getPoiTypes()) {
				fillPoiAdditionals(ps, false);
			}
		}
	}

	private void updatePoiAdditionals() {
		Iterator<Entry<PoiCategory, LinkedHashSet<String>>> e = acceptedTypes.entrySet().iterator();
		poiAdditionals.clear();
		while (e.hasNext()) {
			Entry<PoiCategory, LinkedHashSet<String>> pc = e.next();
			fillPoiAdditionals(pc.getKey(), pc.getValue() == null);
			if (pc.getValue() != null) {
				for (String s : pc.getValue()) {
					PoiType subtype = poiTypes.getPoiTypeByKey(s);
					if (subtype != null) {
						fillPoiAdditionals(subtype, false);
					}
				}
			}
		}
		addOtherPoiAdditionals();
	}

	private void updateAcceptedTypeOrigins() {
		Map<PoiCategory, LinkedHashSet<String>> acceptedTypesOrigin = new LinkedHashMap<>();
		for (Entry<PoiCategory, LinkedHashSet<String>> e : acceptedTypes.entrySet()) {
			if (e.getValue() != null) {
				for (String s : e.getValue()) {
					PoiType subtype = poiTypes.getPoiTypeByKey(s);
					if (subtype != null) {
						PoiCategory c = subtype.getCategory();
						String typeName = subtype.getKeyName();
						Set<String> acceptedSubtypes = getAcceptedSubtypes(c);
						if (acceptedSubtypes != null && !acceptedSubtypes.contains(typeName)) {
							LinkedHashSet<String> typeNames = acceptedTypesOrigin.get(c);
							if (typeNames == null) {
								typeNames = new LinkedHashSet<>();
								acceptedTypesOrigin.put(c, typeNames);
							}
							typeNames.add(typeName);
						}
					}
				}
			}
		}
		this.acceptedTypesOrigin = acceptedTypesOrigin;
	}

	private void addOtherPoiAdditionals() {
		for (PoiType add : poiTypes.getOtherMapCategory().getPoiAdditionalsCategorized()) {
			poiAdditionals.put(add.getKeyName().replace('_', ':').replace(' ', ':'), add);
			poiAdditionals.put(add.getTranslation().replace(' ', ':').toLowerCase(), add);
		}
	}

	public void combineWithPoiFilter(@NonNull PoiUIFilter f) {
		putAllAcceptedTypes(f.acceptedTypes);
		poiAdditionals.putAll(f.poiAdditionals);
	}

	private void putAllAcceptedTypes(@NonNull Map<PoiCategory, LinkedHashSet<String>> types) {
		for (Entry<PoiCategory, LinkedHashSet<String>> entry : types.entrySet()) {
			PoiCategory category = entry.getKey();
			LinkedHashSet<String> typesSet = entry.getValue();
			if (acceptedTypes.containsKey(category)) {
				if (acceptedTypes.get(category) != null && typesSet != null) {
					acceptedTypes.get(category).addAll(typesSet);
				} else {
					acceptedTypes.put(category, null);
				}
			} else {
				if (typesSet != null) {
					acceptedTypes.put(category, new LinkedHashSet<>(typesSet));
				} else {
					acceptedTypes.put(category, null);
				}
			}
		}
	}

	public void combineWithPoiFilters(@NonNull Set<PoiUIFilter> filters) {
		for (PoiUIFilter f : filters) {
			combineWithPoiFilter(f);
		}
	}

	public void replaceWithPoiFilter(PoiUIFilter f) {
		clearFilter();
		combineWithPoiFilter(f);
	}

	public int getAcceptedTypesCount() {
		return acceptedTypes.size();
	}

	public Map<PoiCategory, LinkedHashSet<String>> getAcceptedTypes() {
		return new LinkedHashMap<>(acceptedTypes);
	}

	public void selectSubTypesToAccept(PoiCategory t, LinkedHashSet<String> accept) {
		acceptedTypes.put(t, accept);
		updatePoiAdditionals();
		updateAcceptedTypeOrigins();
	}

	public void setTypeToAccept(PoiCategory poiCategory, boolean b) {
		if (b) {
			acceptedTypes.put(poiCategory, null);
		} else {
			acceptedTypes.remove(poiCategory);
		}
		updatePoiAdditionals();
		updateAcceptedTypeOrigins();
	}

	@Override
	public String getFilterId() {
		return filterId;
	}

	public Map<String, PoiType> getPoiAdditionals() {
		return poiAdditionals;
	}

	public String getIconId() {
		String iconName = null;
		if (filterId.startsWith(STD_PREFIX)) {
			iconName = standardIconId;
		} else if (filterId.startsWith(USER_PREFIX)) {
			iconName = filterId.substring(USER_PREFIX.length()).toLowerCase();
		}
		if (RenderingIcons.containsBigIcon(iconName)) {
			return iconName;
		} else {
			iconName = PoiFilterUtils.getCustomFilterIconName(this);
			return RenderingIcons.containsBigIcon(iconName) ? iconName : filterId;
		}
	}

	public boolean isStandardFilter() {
		return isStandardFilter;
	}

	public void setStandardFilter(boolean isStandardFilter) {
		this.isStandardFilter = isStandardFilter;
	}

	public boolean isCustomPoiFilter() {
		return CUSTOM_FILTER_ID.equals(filterId);
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public Context getApplication() {
		return app;
	}

	@Override
	public boolean accept(PoiCategory type, String subtype) {
		if (type == null) {
			return true;
		}
		if (!poiTypes.isRegisteredType(type)) {
			type = poiTypes.getOtherPoiCategory();
		}
		if (acceptedTypes.containsKey(type)) {
			LinkedHashSet<String> acceptedTypesSet = acceptedTypes.get(type);
			if (acceptedTypesSet == null || acceptedTypesSet.contains(subtype)) {
				return true;
			}
		}
		if (acceptedTypesOrigin.containsKey(type)) {
			LinkedHashSet<String> acceptedTypesSet = acceptedTypesOrigin.get(type);
			return acceptedTypesSet == null || acceptedTypesSet.contains(subtype);
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return acceptedTypes.isEmpty() && (currentSearchResult == null || currentSearchResult.isEmpty());
	}

	@Override
	public int compareTo(@NonNull PoiUIFilter another) {
		if (this.order != INVALID_ORDER && another.order != INVALID_ORDER) {
			return Integer.compare(this.order, another.order);
		} else if (another.filterId.equals(this.filterId)) {
			String thisFilterByName = this.filterByName == null ? "" : this.filterByName;
			String anotherFilterByName = another.filterByName == null ? "" : another.filterByName;
			return thisFilterByName.compareToIgnoreCase(anotherFilterByName);
		} else {
			return this.name.compareToIgnoreCase(another.name);
		}
	}

	public boolean showLayoutWithImages() {
		OsmandSettings settings = app.getSettings();
		return isTopImagesFilter() && settings.WIKI_SHOW_IMAGE_PREVIEWS.get();
	}

	@NonNull
	@Override
	public String toString() {
		return getFilterId();
	}
}