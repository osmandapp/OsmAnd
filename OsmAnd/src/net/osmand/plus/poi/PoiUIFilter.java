package net.osmand.plus.poi;


import android.content.Context;
import android.support.annotation.NonNull;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class PoiUIFilter implements SearchPoiTypeFilter, Comparable<PoiUIFilter>, CustomSearchPoiFilter {

	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	public final static String BY_NAME_FILTER_ID = USER_PREFIX + "by_name"; //$NON-NLS-1$

	private Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<>();
	private Map<String, PoiType> poiAdditionals = new HashMap<>();

	protected String filterId;
	protected String standardIconId = "";
	protected String name;
	protected boolean isStandardFilter;

	protected final OsmandApplication app;

	protected int distanceInd = 0;
	// in kilometers
	protected double[] distanceToSearchValues = new double[] { 1, 2, 5, 10, 20, 50, 100, 200, 500 };

	private final MapPoiTypes poiTypes;

	protected String filterByName = null;
	protected String savedFilterByName = null;
	protected List<Amenity> currentSearchResult = null;

	// constructor for standard filters
	public PoiUIFilter(AbstractPoiType type, OsmandApplication application, String idSuffix) {
		this.app = application;
		isStandardFilter = true;
		standardIconId = (type == null ? null : type.getKeyName());
		filterId = STD_PREFIX + standardIconId + idSuffix;

		poiTypes = application.getPoiTypes();
		name = type == null ? application.getString(R.string.poi_filter_closest_poi) : (type.getTranslation() + idSuffix); //$NON-NLS-1$
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


	// search by name standard
	protected PoiUIFilter(OsmandApplication application) {
		this.app = application;
		isStandardFilter = true;
		filterId = STD_PREFIX; // overridden
		poiTypes = application.getPoiTypes();
	}

	// constructor for user defined filters
	public PoiUIFilter(String name, String filterId,
	                   Map<PoiCategory, LinkedHashSet<String>> acceptedTypes, OsmandApplication app) {
		this.app = app;
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
	}

	public PoiUIFilter(Set<PoiUIFilter> filtersToMerge, OsmandApplication app) {
		this(app);
		combineWithPoiFilters(filtersToMerge);
		filterId = PoiUIFilter.STD_PREFIX + "combined";
		name = app.getPoiFilters().getFiltersName(filtersToMerge);
	}

	public String getFilterByName() {
		return filterByName;
	}

	public void setFilterByName(String filterByName) {
		this.filterByName = filterByName;
		updateFilterResults();
	}

	public void updateFilterResults() {
		List<Amenity> prev = currentSearchResult;
		if (prev != null) {
			AmenityNameFilter nameFilter = getNameFilter(filterByName);
			List<Amenity> newResults = new ArrayList<Amenity>();
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
		distanceToSearchValues = new double[]{0.5, 1, 2, 5, 10, 20, 50, 100};
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
			return " < " + OsmAndFormatter.getFormattedDistance(((int) val * 1000), app);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < " + OsmAndFormatter.getFormattedDistance(500, app);  //$NON-NLS-1$
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

	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit, ResultMatcher<Amenity> matcher) {
		clearPreviousZoom();
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

	public List<Amenity> searchAmenities(double top, double left, double bottom, double right, int zoom,
			ResultMatcher<Amenity> matcher) {
		List<Amenity> results = new ArrayList<Amenity>();
		List<Amenity> tempResults = currentSearchResult;
		if (tempResults != null) {
			for (Amenity a : tempResults) {
				LatLon l = a.getLocation();
				if (l != null && l.getLatitude() <= top && l.getLatitude() >= bottom && l.getLongitude() >= left
						&& l.getLongitude() <= right) {
					if (matcher == null || matcher.publish(a)) {
						results.add(a);
					}
				}
			}
		}
		List<Amenity> amenities = searchAmenitiesInternal(top / 2 + bottom / 2, left / 2 + right / 2,
				top, bottom, left, right, zoom, matcher);
		results.addAll(amenities);
		return results;
	}

	public List<Amenity> searchAmenitiesOnThePath(List<Location> locs, int poiSearchDeviationRadius) {
		return app.getResourceManager().searchAmenitiesOnThePath(locs, poiSearchDeviationRadius, this, wrapResultMatcher(null));
	}

	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, int zoom, final ResultMatcher<Amenity> matcher) {
		return app.getResourceManager().searchAmenities(this, 
				topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, wrapResultMatcher(matcher));
	}

	public AmenityNameFilter getNameFilter(String filter) {
		if (Algorithms.isEmpty(filter)) {
			return new AmenityNameFilter() {

				@Override
				public boolean accept(Amenity a) {
					return true;
				}
			};
		}
		StringBuilder nmFilter = new StringBuilder();
		String[] items = filter.split(" ");
		boolean allTime = false;
		boolean open = false;
		List<PoiType> poiAdditionalsFilter = null;
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
					nmFilter.append(s).append(" ");
				}
			}
		}
		return getNameFilterInternal(nmFilter, allTime, open, poiAdditionalsFilter);
	}

	private AmenityNameFilter getNameFilterInternal(StringBuilder nmFilter, 
			final boolean allTime, final boolean open, final List<PoiType> poiAdditionals) {
		final CollatorStringMatcher sm =
				nmFilter.length() > 0 ?
				new CollatorStringMatcher(nmFilter.toString().trim(), StringMatcherMode.CHECK_CONTAINS) : null;
		return new AmenityNameFilter() {

			@Override
			public boolean accept(Amenity a) {
				if (sm != null) {
					String lower = OsmAndFormatter.getPoiStringWithoutType(a, 
							app.getSettings().MAP_PREFERRED_LOCALE.get(), app.getSettings().MAP_TRANSLITERATE_NAMES.get());
					if (!sm.matches(lower)) {
						return false;
					}
				}
				if (poiAdditionals != null) {
					Map<PoiType, PoiType> textPoiAdditionalsMap = new HashMap<>();
					Map<String, List<PoiType>> poiAdditionalCategoriesMap = new HashMap<>();
 					for (PoiType pt : poiAdditionals) {
 						String category = pt.getPoiAdditionalCategory();
						List<PoiType> types = poiAdditionalCategoriesMap.get(category);
						if (types == null) {
							types = new ArrayList<>();
							poiAdditionalCategoriesMap.put(category, types);
						}
						types.add(pt);

						String osmTag = pt.getOsmTag();
						if (osmTag.length() < pt.getKeyName().length()) {
							PoiType textPoiType = poiTypes.getTextPoiAdditionalByKey(osmTag);
							if (textPoiType != null) {
								textPoiAdditionalsMap.put(pt, textPoiType);
							}
						}
					}
					for (List<PoiType> types : poiAdditionalCategoriesMap.values()) {
						boolean acceptedAnyInCategory = false;
						for (PoiType p : types) {
							String inf = a.getAdditionalInfo(p.getKeyName());
							if (inf != null) {
								acceptedAnyInCategory = true;
								break;
							} else {
								PoiType textPoiType = textPoiAdditionalsMap.get(p);
								if (textPoiType != null) {
									inf = a.getAdditionalInfo(textPoiType.getKeyName());
									if (!Algorithms.isEmpty(inf)) {
										String[] items = inf.split(";");
										String val = p.getOsmValue().trim().toLowerCase();
										for (String item : items) {
											if (item.trim().toLowerCase().equals(val)) {
												acceptedAnyInCategory = true;
												break;
											}
										}
										if (acceptedAnyInCategory) {
											break;
										}
									}
								}
							}
						}
						if (!acceptedAnyInCategory) {
							return false;
						}
					}
				}
				if (allTime) {
					if (!"24/7".equalsIgnoreCase(a.getOpeningHours()) && !"Mo-Su 00:00-24:00".equalsIgnoreCase(a.getOpeningHours())) {
						return false;
					}
				}
				if (open) {
					OpeningHours rs = OpeningHoursParser.parseOpenedHours(a.getOpeningHours());
					if (rs != null) {
						Calendar inst = Calendar.getInstance();
						inst.setTimeInMillis(System.currentTimeMillis());
						boolean work = rs.isOpenedForTime(inst);
						if (!work) {
							return false;
						}
					} else {
						return false;
					}
				}
				return true;
			}
		};
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
	public ResultMatcher<Amenity> wrapResultMatcher(final ResultMatcher<Amenity> matcher) {
		final AmenityNameFilter nm = getNameFilter(filterByName);
		return new ResultMatcher<Amenity>() {

			@Override
			public boolean publish(Amenity a) {
				if (nm.accept(a)) {
					if (matcher == null || matcher.publish(a)) {
						return true;
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

	public String getGeneratedName(int chars) {
		if (!filterId.equals(CUSTOM_FILTER_ID) ||
				areAllTypesAccepted() || acceptedTypes.isEmpty()) {
			return getName();
		}
		StringBuilder res = new StringBuilder();
		for (PoiCategory p : acceptedTypes.keySet()) {
			LinkedHashSet<String> set = acceptedTypes.get(p);
			if (set == null) {
				if (res.length() > 0) {
					res.append(", ");
				}
				res.append(p.getTranslation());
			}
			if (res.length() > chars) {
				return res.toString();
			}
		}
		for (PoiCategory p : acceptedTypes.keySet()) {
			LinkedHashSet<String> set = acceptedTypes.get(p);
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

	/**
	 * @param type
	 * @return null if all subtypes are accepted/ empty list if type is not accepted at all
	 */
	public Set<String> getAcceptedSubtypes(PoiCategory type) {
		if (!acceptedTypes.containsKey(type)) {
			return Collections.emptySet();
		}
		return acceptedTypes.get(type);
	}

	public boolean isTypeAccepted(PoiCategory t) {
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
			for (PoiCategory a : acceptedTypes.keySet()) {
				if (acceptedTypes.get(a) != null) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public void updateTypesToAccept(AbstractPoiType pt) {
		pt.putTypes(acceptedTypes);
		if (pt instanceof PoiType && ((PoiType) pt).isAdditional() && ((PoiType) pt).getParentType() != null) {
			fillPoiAdditionals(((PoiType) pt).getParentType(), true);
		} else {
			fillPoiAdditionals(pt, true);
		}
		addOtherPoiAdditionals();
	}

	private void fillPoiAdditionals(AbstractPoiType pt, boolean allFromCategory) {
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

	private void addOtherPoiAdditionals() {
		for (PoiType add : poiTypes.getOtherMapCategory().getPoiAdditionalsCategorized()) {
			poiAdditionals.put(add.getKeyName().replace('_', ':').replace(' ', ':'), add);
			poiAdditionals.put(add.getTranslation().replace(' ', ':').toLowerCase(), add);
		}
	}

	public void combineWithPoiFilter(PoiUIFilter f) {
		putAllAcceptedTypes(f.acceptedTypes);
		poiAdditionals.putAll(f.poiAdditionals);
	}

	private void putAllAcceptedTypes(Map<PoiCategory, LinkedHashSet<String>> types) {
		for (PoiCategory category : types.keySet()) {
			if (acceptedTypes.containsKey(category)) {
				if (acceptedTypes.get(category) != null && types.get(category) != null) {
					acceptedTypes.get(category).addAll(types.get(category));
				} else {
					acceptedTypes.put(category, null);
				}
			} else {
				acceptedTypes.put(category, types.get(category));
			}
		}
	}

	public void combineWithPoiFilters(Set<PoiUIFilter> filters) {
		for (PoiUIFilter f : filters) {
			combineWithPoiFilter(f);
		}
	}

	public static void combineStandardPoiFilters(Set<PoiUIFilter> filters, OsmandApplication app) {
		Set<PoiUIFilter> standardFilters = new TreeSet<>();
		for (PoiUIFilter filter : filters) {
			if (((filter.isStandardFilter() && filter.filterId.startsWith(PoiUIFilter.STD_PREFIX))
					|| filter.filterId.startsWith(PoiUIFilter.CUSTOM_FILTER_ID))
					&& (filter.getFilterByName() == null)
					&& (filter.getSavedFilterByName() == null)) {
				standardFilters.add(filter);
			}
		}
		if (standardFilters.size() > 1) {
			PoiUIFilter standardFiltersCombined = new PoiUIFilter(standardFilters, app);
			filters.removeAll(standardFilters);
			filters.add(standardFiltersCombined);
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
	}

	public void setTypeToAccept(PoiCategory poiCategory, boolean b) {
		if (b) {
			acceptedTypes.put(poiCategory, null);
		} else {
			acceptedTypes.remove(poiCategory);
		}
		updatePoiAdditionals();
	}

	public String getFilterId() {
		return filterId;
	}

	public Map<String, PoiType> getPoiAdditionals() {
		return poiAdditionals;
	}

	public String getIconId() {
		if (filterId.startsWith(STD_PREFIX)) {
			return standardIconId;
		} else if (filterId.startsWith(USER_PREFIX)) {
			return filterId.substring(USER_PREFIX.length()).toLowerCase();
		}
		return filterId;
	}

	public boolean isStandardFilter() {
		return isStandardFilter;
	}

	public void setStandardFilter(boolean isStandardFilter) {
		this.isStandardFilter = isStandardFilter;
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
		if (!acceptedTypes.containsKey(type)) {
			return false;
		}
		LinkedHashSet<String> set = acceptedTypes.get(type);
		if (set == null) {
			return true;
		}
		return set.contains(subtype);
	}
	
	@Override
	public boolean isEmpty() {
		return acceptedTypes.isEmpty() &&
				(currentSearchResult == null || currentSearchResult.isEmpty());
	}

	@Override
	public int compareTo(@NonNull PoiUIFilter another) {
		if (another.filterId.equals(this.filterId)) {
			String thisFilterByName = this.filterByName == null ? "" : this.filterByName;
			String anotherFilterByName = another.filterByName == null ? "" : another.filterByName;
			return thisFilterByName.compareToIgnoreCase(anotherFilterByName);
		} else {
			return this.name.compareTo(another.name);
		}
	}

	public interface AmenityNameFilter {

		public boolean accept(Amenity a);
	}

}
