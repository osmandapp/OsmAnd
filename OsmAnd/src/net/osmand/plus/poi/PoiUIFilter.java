package net.osmand.plus.poi;


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
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;
import android.content.Context;

public class PoiUIFilter implements SearchPoiTypeFilter {
	
	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	public final static String BY_NAME_FILTER_ID = USER_PREFIX + "by_name"; //$NON-NLS-1$
	
	private Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<PoiCategory,
			LinkedHashSet<String>>();
	private Map<String, PoiType> poiAdditionals = new HashMap<String, PoiType>();

	protected String filterId;
	protected String standardIconId = "";
	protected String name;
	protected boolean isStandardFilter;
	
	protected final OsmandApplication app;
	
	protected int distanceInd = 1;
	// in kilometers
	protected double[] distanceToSearchValues = new double[] {1, 2, 5, 10, 20, 50, 100, 200, 500 };
	
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
			if(type.isAdditional()) {
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
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes, OsmandApplication app){
		this.app = app;
		isStandardFilter = false;
		poiTypes = app.getPoiTypes();
		if(filterId == null){
			filterId = USER_PREFIX + name.replace(' ', '_').toLowerCase();
		}
		this.filterId = filterId;
		this.name = name;
		if(acceptedTypes == null){
			initSearchAll();
		} else {
			this.acceptedTypes.putAll(acceptedTypes);
		}
		updatePoiAdditionals();
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
		if(prev != null) {
			AmenityNameFilter nameFilter = getNameFilter(filterByName);
			List<Amenity> newResults = new ArrayList<Amenity>();
			for(Amenity a : prev) {
				if(nameFilter.accept(a)) {
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
	
	public List<Amenity> getCurrentSearchResult() {
		return currentSearchResult;
	}
	
	
	public List<Amenity> searchAgain(double lat, double lon) {
		List<Amenity> amenityList ;
		if(currentSearchResult != null) {
			amenityList = currentSearchResult;
		} else {
			amenityList = searchAmenities(lat, lon, null);
		}
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		return amenityList;
	}
	

	public List<Amenity> searchFurther(double latitude, double longitude, ResultMatcher<Amenity> matcher){
		if(distanceInd < distanceToSearchValues.length - 1){
			distanceInd ++;
		}
		List<Amenity> amenityList = searchAmenities( latitude, longitude, matcher);
		MapUtils.sortListOfMapObject(amenityList, latitude, longitude);
		return amenityList;
	}
	
	private void initSearchAll(){
		for(PoiCategory t : poiTypes.getCategories(false)){
			acceptedTypes.put(t, null);
		}
		distanceToSearchValues = new double[] {0.5, 1, 2, 5, 10, 20, 50, 100};
	}
	
	
	public boolean isSearchFurtherAvailable(){
		return distanceInd < distanceToSearchValues.length - 1;
	}
	
	
	
	
	public String getSearchArea(){
		double val = distanceToSearchValues[distanceInd];
		if(val >= 1){
			return " < " + OsmAndFormatter.getFormattedDistance(((int)val * 1000), app);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < " + OsmAndFormatter.getFormattedDistance(500, app);  //$NON-NLS-1$
		}
	}
	
	public void clearPreviousZoom(){
		distanceInd = 0;
	}
	
	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit, ResultMatcher<Amenity> matcher){
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
		double topLatitude = Math.min(lat + (distance/ baseDistY ), 84.);
		double bottomLatitude = Math.max(lat - (distance/ baseDistY ), -84.);
		double leftLongitude = Math.max(lon - (distance / baseDistX), -180);
		double rightLongitude = Math.min(lon + (distance/ baseDistX), 180);
		return searchAmenitiesInternal(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude, matcher);
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
		List<Amenity> amenities = app.getResourceManager().searchAmenities(this, top, left, bottom, right, zoom,
				wrapResultMatcher(matcher));
		results.addAll(amenities);
		return results;
	}
	
	public List<Amenity> searchAmenitiesOnThePath(List<Location> locs, int poiSearchDeviationRadius) {
		return app.getResourceManager().searchAmenitiesOnThePath(locs, poiSearchDeviationRadius, this, wrapResultMatcher(null));
	}
	
	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, final ResultMatcher<Amenity> matcher) {
		return app.getResourceManager().searchAmenities(this, 
				topLatitude, leftLongitude, bottomLatitude, rightLongitude, -1, wrapResultMatcher(matcher));
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
		Map<PoiType, String> poiAdditionalsFilter =  null;
		for(String s : items) {
			s = s.trim();
			if(!Algorithms.isEmpty(s)){
				if(getNameToken24H().equalsIgnoreCase(s)){
					allTime = true;
				} else if(getNameTokenOpen().equalsIgnoreCase(s)){
					open = true;
				} else if(poiAdditionals.containsKey(s.toLowerCase())) {
					if(poiAdditionalsFilter == null) {
						poiAdditionalsFilter = new LinkedHashMap<PoiType, String>();
					}
					poiAdditionalsFilter.put(poiAdditionals.get(s.toLowerCase()), null);
				} else {
					nmFilter.append(s).append(" ");
				}
			}
		}
		return getNameFilterInternal(nmFilter, allTime, open, poiAdditionalsFilter);
	}

	private AmenityNameFilter getNameFilterInternal(StringBuilder nmFilter, 
			final boolean allTime, final boolean open, final Map<PoiType, String> poiAdditionals) {
		final CollatorStringMatcher sm =
				nmFilter.length() > 0 ?
				new CollatorStringMatcher(nmFilter.toString().trim(), StringMatcherMode.CHECK_CONTAINS) : null;
		return new AmenityNameFilter() {
			
			@Override
			public boolean accept(Amenity a) {
				if (sm != null) {
					String lower = OsmAndFormatter.getPoiStringWithoutType(a, app.getSettings().MAP_PREFERRED_LOCALE.get());
					if (!sm.matches(lower)) {
						return false;
					}
				}
				if(poiAdditionals != null) {
					Iterator<Entry<PoiType, String>> it = poiAdditionals.entrySet().iterator();
					while(it.hasNext()) {
						Entry<PoiType, String> e = it.next();
						String inf = a.getAdditionalInfo(e.getKey().getKeyName());
						if(inf == null) {
							return false;
						} else if(e.getValue() != null && !e.getValue().equalsIgnoreCase(inf)) {
							return false;
						}
					}
				}
				if (allTime) {
					if (!"24/7".equalsIgnoreCase(a.getOpeningHours())) {
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
		return "24/7";
	}
	
	public String getNameTokenOpen() {
		return app.getString(R.string.shared_string_open);
	}
	
	
	private ResultMatcher<Amenity> wrapResultMatcher(final ResultMatcher<Amenity> matcher) {
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
	
	public String getName(){
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
	public Set<String> getAcceptedSubtypes(PoiCategory type){
		if(!acceptedTypes.containsKey(type)){
			return Collections.emptySet();
		}
		return acceptedTypes.get(type);
	}
	
	public boolean isTypeAccepted(PoiCategory t){
		return acceptedTypes.containsKey(t);
	}
	
	public void clearFilter() {
		acceptedTypes = new LinkedHashMap<PoiCategory, LinkedHashSet<String>>();
		poiAdditionals.clear();
	}
	
	public boolean areAllTypesAccepted(){
		if(poiTypes.getCategories(false).size() == acceptedTypes.size()){
			for(PoiCategory a : acceptedTypes.keySet()){
				if(acceptedTypes.get(a) != null){
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
			fillPoiAdditionals(((PoiType) pt).getParentType());
		} else {
			fillPoiAdditionals(pt);
		}
	}
	
	private void fillPoiAdditionals(AbstractPoiType pt) {
		for (PoiType add : pt.getPoiAdditionals()) {
			poiAdditionals.put(add.getKeyName().replace('_', ':').replace(' ', ':'), add);
			poiAdditionals.put(add.getTranslation().replace(' ', ':').toLowerCase(), add);
		}
		if(pt instanceof PoiFilter && !(pt instanceof PoiCategory)) {
			for(PoiType ps : ((PoiFilter) pt).getPoiTypes()) {
				fillPoiAdditionals(ps);
			}
		}
	}
	
	private void updatePoiAdditionals() {
		Iterator<Entry<PoiCategory, LinkedHashSet<String>>> e = acceptedTypes.entrySet().iterator();
		poiAdditionals.clear();
		while (e.hasNext()) {
			Entry<PoiCategory, LinkedHashSet<String>> pc = e.next();
			fillPoiAdditionals(pc.getKey());
			if (pc.getValue() != null) {
				for (String s : pc.getValue()) {
					PoiType subtype = poiTypes.getPoiTypeByKey(s);
					if (subtype != null) {
						fillPoiAdditionals(subtype);
					}
				}
			}
		}
	}

	public void replaceWithPoiFilter(PoiUIFilter f) {
		acceptedTypes.clear();
		acceptedTypes.putAll(f.acceptedTypes);
		poiAdditionals.clear();
		poiAdditionals.putAll(f.poiAdditionals);
	}
	
	
	public Map<PoiCategory, LinkedHashSet<String>> getAcceptedTypes(){
		return new LinkedHashMap<PoiCategory, LinkedHashSet<String>>(acceptedTypes);
	}
	
	public void selectSubTypesToAccept(PoiCategory t, LinkedHashSet<String> accept){
		acceptedTypes.put(t, accept);
		fillPoiAdditionals(t);
	}
	
	public void setTypeToAccept(PoiCategory poiCategory, boolean b) {
		if(b) {
			acceptedTypes.put(poiCategory, null);
		} else {
			acceptedTypes.remove(poiCategory);
		}
		updatePoiAdditionals();
	}
	
	public String getFilterId(){
		return filterId;
	}
	
	public Map<String, PoiType> getPoiAdditionals() {
		return poiAdditionals;
	}
	
	public String getIconId(){
		if(filterId.startsWith(STD_PREFIX)) {
			return standardIconId;
		} else if(filterId.startsWith(USER_PREFIX)) {
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
		if(type == null) {
			return true;
		}
		if(!poiTypes.isRegisteredType(type)) {
			type = poiTypes.getOtherPoiCategory();
		}
		if(!acceptedTypes.containsKey(type)){
			return false;
		}
		LinkedHashSet<String> set = acceptedTypes.get(type);
		if(set == null){
			return true;
		}
		return set.contains(subtype);
	}

	@Override
	public boolean isEmpty() {
		return acceptedTypes.isEmpty();
	}

	
	public interface AmenityNameFilter {
		
		public boolean accept(Amenity a) ;
	}


	
}