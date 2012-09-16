package net.osmand.plus;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.IndexConstants;
import net.osmand.osm.MapUtils;

public class PoiFilter {
	
	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	public final static String BY_NAME_FILTER_ID = USER_PREFIX + "by_name"; //$NON-NLS-1$
	
	private Map<AmenityType, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<AmenityType, LinkedHashSet<String>>();
	private String filterByName = null;

	protected String filterId;
	protected String name;
	protected String nameFilter;
	protected boolean isStandardFilter;
	
	protected final OsmandApplication application;
	
	protected int distanceInd = 1;
	// in kilometers
	protected double[] distanceToSearchValues = new double[] {1, 2, 3, 5, 10, 30, 100, 250 };
	
	
	// constructor for standard filters
	public PoiFilter(AmenityType type, OsmandApplication application){
		this.application = application;
		isStandardFilter = true;
		filterId = STD_PREFIX + type;
		name = type == null ? application.getString(R.string.poi_filter_closest_poi) : OsmAndFormatter.toPublicString(type, application); //$NON-NLS-1$
		if(type == null){
			initSearchAll();
		} else {
			acceptedTypes.put(type, null);
		}
	}
	
	// constructor for user defined filters
	public PoiFilter(String name, String filterId, Map<AmenityType, LinkedHashSet<String>> acceptedTypes, OsmandApplication app){
		application = app;
		isStandardFilter = false;
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
	
	public void setNameFilter(String nameFilter) {
		if(nameFilter != null) {
			this.nameFilter = nameFilter.toLowerCase();
		} else {
			clearNameFilter();
		}
	}
	
	public String getNameFilter() {
		return nameFilter;
	}
	
	public void clearNameFilter(){
		nameFilter = null;
	}
	
	private void initSearchAll(){
		for(AmenityType t : AmenityType.values()){
			acceptedTypes.put(t, null);
		}
		distanceToSearchValues = new double[] {0.5, 1, 2, 3, 5, 10, 15, 30, 100};
	}
	
	
	public boolean isSearchFurtherAvailable(){
		return distanceInd < distanceToSearchValues.length - 1;
	}
	
	
	public List<Amenity> searchFurther(double latitude, double longitude, ResultMatcher<Amenity> matcher){
		if(distanceInd < distanceToSearchValues.length - 1){
			distanceInd ++;
		}
		List<Amenity> amenityList = searchAmenities( latitude, longitude, matcher);
		MapUtils.sortListOfMapObject(amenityList, latitude, longitude);
		
		return amenityList;
	}
	
	public String getSearchArea(){
		double val = distanceToSearchValues[distanceInd];
		if(val >= 1){
			return " < " + ((int) val)+ " " + application.getString(R.string.km);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < 500 " + application.getString(R.string.m);  //$NON-NLS-1$
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
		return amenityList; 
	}
	
	private List<Amenity> searchAmenities(double lat, double lon, ResultMatcher<Amenity> matcher) {
		double baseDistY = MapUtils.getDistance(lat, lon, lat - 1, lon);
		double baseDistX = MapUtils.getDistance(lat, lon, lat, lon - 1);
		double distance = distanceToSearchValues[distanceInd] * 1000;
		
		double topLatitude = lat + (distance/ baseDistY );
		double bottomLatitude = lat - (distance/ baseDistY );
		double leftLongitude = lon - (distance / baseDistX);
		double rightLongitude = lon + (distance/ baseDistX);
		
		return searchAmenities(lat, lon, topLatitude, bottomLatitude, leftLongitude, rightLongitude, matcher);
	}
	
	public ResultMatcher<Amenity> getResultMatcher(final ResultMatcher<Amenity> matcher){
		final String filter = nameFilter;
		if(filter != null) {
			final boolean en = application.getSettings().USE_ENGLISH_NAMES.get();
			return new ResultMatcher<Amenity>() {
				@Override
				public boolean publish(Amenity object) {
					if(!OsmAndFormatter.getPoiStringWithoutType(object, en).toLowerCase().contains(filter) || 
							(matcher != null && !matcher.publish(object))) {
						return false;
					}
					return true;
				}
				
				@Override
				public boolean isCancelled() {
					return false || (matcher != null && matcher.isCancelled());
				}
			};
		}
		return matcher;
	}

	protected List<Amenity> searchAmenities(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, final ResultMatcher<Amenity> matcher) {
		
		return application.getResourceManager().searchAmenities(this, 
				topLatitude, leftLongitude, bottomLatitude, rightLongitude, lat, lon, matcher);
	}

	public List<Amenity> searchAgain(double lat, double lon) {
		List<Amenity> amenityList = searchAmenities(lat, lon, null);
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		return amenityList;
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * @param type
	 * @return null if all subtypes are accepted/ empty list if type is not accepted at all
	 */
	public Set<String> getAcceptedSubtypes(AmenityType type){
		if(!acceptedTypes.containsKey(type)){
			return Collections.emptySet();
		}
		return acceptedTypes.get(type);
	}
	
	public boolean isTypeAccepted(AmenityType t){
		return acceptedTypes.containsKey(t);
	}
	
	public boolean acceptTypeSubtype(AmenityType t, String subtype){
		if(!acceptedTypes.containsKey(t)){
			return false;
		}
		LinkedHashSet<String> set = acceptedTypes.get(t);
		if(set == null){
			return true;
		}
		return set.contains(subtype);
	}
	
	public void clearFilter(){
		acceptedTypes = new LinkedHashMap<AmenityType, LinkedHashSet<String>>();
	}
	
	public boolean areAllTypesAccepted(){
		if(AmenityType.values().length == acceptedTypes.size()){
			for(AmenityType a : acceptedTypes.keySet()){
				if(acceptedTypes.get(a) != null){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	
	public void setTypeToAccept(AmenityType type, boolean accept){
		if(accept){
			acceptedTypes.put(type, new LinkedHashSet<String>());
		} else {
			acceptedTypes.remove(type);
		}
	}
	
	public void setMapToAccept(Map<AmenityType, List<String>> newMap) {
		Iterator<Entry<AmenityType, List<String>>> iterator = newMap.entrySet().iterator();
		acceptedTypes.clear();
		while(iterator.hasNext()){
			Entry<AmenityType, List<String>> e = iterator.next();
			if(e.getValue() == null){
				acceptedTypes.put(e.getKey(), null);
			} else {
				acceptedTypes.put(e.getKey(), new LinkedHashSet<String>(e.getValue()));
			}
		}
	}
	
	public String buildSqlWhereFilter(){
		if(areAllTypesAccepted()){
			return null;
		}
		assert IndexConstants.POI_TABLE != null : "use constants here to show table usage "; //$NON-NLS-1$
		if(acceptedTypes.size() == 0){
			return "1 > 1";  //$NON-NLS-1$
		}
		StringBuilder b = new StringBuilder();
		b.append("("); //$NON-NLS-1$
		boolean first = true;
		for(AmenityType a : acceptedTypes.keySet()){
			if(first){
				first = false;
			} else {
				b.append(" OR "); //$NON-NLS-1$
			}
			b.append("(type = '").append(AmenityType.valueToString(a)).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			if(acceptedTypes.get(a) != null){
				LinkedHashSet<String> list = acceptedTypes.get(a);
				b.append(" AND subtype IN ("); //$NON-NLS-1$
				boolean bfirst = true;
				for(String s : list){
					if(bfirst){
						bfirst = false;
					} else {
						b.append(", "); //$NON-NLS-1$
					}
					b.append("'").append(s).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				b.append(")"); //$NON-NLS-1$
			}
			b.append(")"); //$NON-NLS-1$
		}
		b.append(")"); //$NON-NLS-1$
		return b.toString();
	}
	
	public Map<AmenityType, LinkedHashSet<String>> getAcceptedTypes(){
		return new LinkedHashMap<AmenityType, LinkedHashSet<String>>(acceptedTypes);
	}
	
	public void selectSubTypesToAccept(AmenityType t, LinkedHashSet<String> accept){
		acceptedTypes.put(t, accept);
	}
	
	public String getFilterId(){
		return filterId;
	}
	
	
	public String getFilterByName() {
		return filterByName;
	}
	
	public void setFilterByName(String filterByName) {
		this.filterByName = filterByName;
	}
	
	public boolean isStandardFilter(){
		return isStandardFilter;
	}
	
	public void setStandardFilter(boolean isStandardFilter) {
		this.isStandardFilter = isStandardFilter;
	}
	
	public OsmandApplication getApplication() {
		return application;
	}
	
}
