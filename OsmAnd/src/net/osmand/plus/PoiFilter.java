package net.osmand.plus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.OsmAndFormatter;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.IndexConstants;
import net.osmand.osm.MapUtils;
import net.osmand.plus.activities.OsmandApplication;

public class PoiFilter {
	
	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	
	private Map<AmenityType, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<AmenityType, LinkedHashSet<String>>();
	private String filterByName = null;

	protected String filterId;
	protected String name;
	private final boolean isStandardFilter;
	
	private final static int finalZoom = 6;
	private final static int initialZoom = 14;
	private int zoom = initialZoom;
	private final OsmandApplication application;
	
	
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
	
	// constructor for standard filters
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
	
	private void initSearchAll(){
		for(AmenityType t : AmenityType.values()){
			acceptedTypes.put(t, null);
		}
	}
	
	
	public boolean isSearchFurtherAvailable(){
		return zoom > finalZoom;
	}
	
	public List<Amenity> searchFurther(double latitude, double longitude){
		zoom --;
		List<Amenity> amenityList = application.getResourceManager().searchAmenities(this, latitude, longitude, zoom, -1);
		MapUtils.sortListOfMapObject(amenityList, latitude, longitude);
		
		return amenityList;
	}
	
	public String getSearchArea(){
		if(zoom <= 14){
			int d = (int) (1 * (1 << (14 - zoom)));
			return " < " + d + " " + application.getString(R.string.km);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < 500 " + application.getString(R.string.m);  //$NON-NLS-1$
		}
	}
	
	public void clearPreviousZoom(){
		zoom = getInitialZoom();
	}
	
	private int getInitialZoom(){
		int zoom = initialZoom;
		if(areAllTypesAccepted()){
			zoom += 1;
		}
		return zoom; 
	}
	
	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit){
		zoom = getInitialZoom();
		List<Amenity> amenityList = application.getResourceManager().searchAmenities(this, lat, lon, zoom, -1);
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		while (amenityList.size() > firstTimeLimit) {
			amenityList.remove(amenityList.size() - 1);
		}
		
		return amenityList; 
	}
	
	public List<Amenity> searchAgain(double lat, double lon){
		List<Amenity> amenityList = application.getResourceManager().searchAmenities(this, lat, lon, zoom, -1);
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
	
	public OsmandApplication getApplication() {
		return application;
	}
	
}
