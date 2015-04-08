package net.osmand.plus.poi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.content.Context;

public class PoiLegacyFilter implements SearchPoiTypeFilter {
	
	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	public final static String BY_NAME_FILTER_ID = USER_PREFIX + "by_name"; //$NON-NLS-1$
	
	private Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<PoiCategory,
			LinkedHashSet<String>>();

	protected String filterId;
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
	private Collator collator;
	
	// constructor for standard filters
	public PoiLegacyFilter(AbstractPoiType type, OsmandApplication application) {
		this.app = application;
		isStandardFilter = true;
		filterId = STD_PREFIX + (type == null ? null : type.getKeyName());
		poiTypes = application.getPoiTypes();
		name = type == null ? application.getString(R.string.poi_filter_closest_poi) : type.getTranslation(); //$NON-NLS-1$
		if (type == null) {
			initSearchAll();
		} else {
			type.putTypes(acceptedTypes);
		}
	}
	
	// search by name standard
	protected PoiLegacyFilter(OsmandApplication application) {
		this.app = application;
		isStandardFilter = true;
		filterId = STD_PREFIX; // overridden
		poiTypes = application.getPoiTypes();
	}

	// constructor for user defined filters
	public PoiLegacyFilter(String name, String filterId, 
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
	}

	public String getFilterByName() {
		return filterByName;
	}
	
	public void setFilterByName(String filterByName) {
		this.filterByName = filterByName;
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
		for(PoiCategory t : poiTypes.getCategories()){
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
	
	public boolean checkNameFilter(Amenity object, String filter, boolean en) {
		boolean publish = false;
		if (Algorithms.isEmpty(filter)) {
			publish = true;
		} else {
			String lower = OsmAndFormatter.getPoiStringWithoutType(object, en);
			publish = CollatorStringMatcher.ccontains(getCollator(), lower, filter);
		}
		return publish;
	}

	private Collator getCollator() {
		if (collator == null) {
			collator = OsmAndCollator.primaryCollator();
		}
		return collator;
	}
	
	private ResultMatcher<Amenity> wrapResultMatcher(final ResultMatcher<Amenity> matcher) {
		final boolean en = app.getSettings().usingEnglishNames();
		return new ResultMatcher<Amenity>() {
			
			@Override
			public boolean publish(Amenity a) {
				if (checkNameFilter(a, filterByName, en)) {
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
	}
	
	public boolean areAllTypesAccepted(){
		if(poiTypes.getCategories().size() == acceptedTypes.size()){
			for(PoiCategory a : acceptedTypes.keySet()){
				if(acceptedTypes.get(a) != null){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	
	public void setTypeToAccept(PoiCategory type, boolean accept){
		if(accept){
			acceptedTypes.put(type, new LinkedHashSet<String>());
		} else {
			acceptedTypes.remove(type);
		}
	}
	
	
	public Map<PoiCategory, LinkedHashSet<String>> getAcceptedTypes(){
		return new LinkedHashMap<PoiCategory, LinkedHashSet<String>>(acceptedTypes);
	}
	
	public void selectSubTypesToAccept(PoiCategory t, LinkedHashSet<String> accept){
		acceptedTypes.put(t, accept);
	}
	
	public String getFilterId(){
		return filterId;
	}
	
	public String getSimplifiedId(){
		if(filterId.startsWith(STD_PREFIX)) {
			return filterId.substring(STD_PREFIX.length()).toLowerCase();
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

}
