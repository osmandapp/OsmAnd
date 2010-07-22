package com.osmand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.data.index.IndexConstants.IndexPoiTable;
import com.osmand.osm.MapUtils;

public class PoiFilter {
	
	public final static String STD_PREFIX = "std_"; //$NON-NLS-1$
	public final static String USER_PREFIX = "user_"; //$NON-NLS-1$
	public final static String CUSTOM_FILTER_ID = USER_PREFIX + "custom_id"; //$NON-NLS-1$
	
	private Map<AmenityType, List<String>> acceptedTypes = new LinkedHashMap<AmenityType, List<String>>();
	private String filterByName = null;

	protected String filterId;
	protected String name;
	private final boolean isStandardFilter;
	
	private final static int finalZoom = 6;
	private final static int initialZoom = 13;
	private final static int maxInitialCount = 200;
	private int zoom = initialZoom;
	
	
	// constructor for standard filters
	public PoiFilter(AmenityType type){
		isStandardFilter = true;
		filterId = STD_PREFIX + type;
		name = type == null ? Messages.getMessage("poi_filter_closest_poi") : AmenityType.toPublicString(type); //$NON-NLS-1$
		if(type == null){
			initSearchAll();
		} else {
			acceptedTypes.put(type, null);
		}
	}
	
	// constructor for standard filters
	public PoiFilter(String name, String filterId, Map<AmenityType, List<String>> acceptedTypes){
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
		List<Amenity> amenityList = ResourceManager.getResourceManager().searchAmenities(this, latitude, longitude, zoom, -1);
		MapUtils.sortListOfMapObject(amenityList, latitude, longitude);
		
		return amenityList;
	}
	
	public String getSearchArea(){
		if(zoom <= 14){
			int d = (int) (1 * (1 << (14 - zoom)));
			return " < " + d + " " + Messages.getMessage(Messages.KEY_KM);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			return " < 500 " + Messages.getMessage(Messages.KEY_M);  //$NON-NLS-1$
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
		List<Amenity> amenityList = ResourceManager.getResourceManager().searchAmenities(this, lat, lon, zoom, maxInitialCount);
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		while (amenityList.size() > firstTimeLimit) {
			amenityList.remove(amenityList.size() - 1);
		}
		
		return amenityList; 
	}
	
	public List<Amenity> searchAgain(double lat, double lon){
		int limit = -1;
		if(zoom == getInitialZoom()){
			limit = maxInitialCount;
		}
		List<Amenity> amenityList = ResourceManager.getResourceManager().searchAmenities(this, lat, lon, zoom, limit);
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
	public List<String> getAcceptedSubtypes(AmenityType type){
		if(!acceptedTypes.containsKey(type)){
			return Collections.emptyList();
		}
		return acceptedTypes.get(type);
	}
	
	public boolean isTypeAccepted(AmenityType t){
		return acceptedTypes.containsKey(t);
	}
	
	public boolean isWholeTypeAccepted(AmenityType type){
		return acceptedTypes.get(type) == null;
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
			acceptedTypes.put(type, new ArrayList<String>());
		} else {
			acceptedTypes.remove(type);
		}
	}
	
	public String buildSqlWhereFilter(){
		if(areAllTypesAccepted()){
			return null;
		}
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
			b.append("("); //$NON-NLS-1$
			b.append(IndexPoiTable.TYPE.name().toLowerCase()).append(" = '").append(AmenityType.valueToString(a)).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			if(acceptedTypes.get(a) != null){
				List<String> list = acceptedTypes.get(a);
				b.append(" AND "); //$NON-NLS-1$
				b.append(IndexPoiTable.SUBTYPE.name().toLowerCase()).append(" IN ("); //$NON-NLS-1$
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
	
	public Map<AmenityType, List<String>> getAcceptedTypes(){
		return new LinkedHashMap<AmenityType, List<String>>(acceptedTypes);
	}
	
	public void selectSubTypesToAccept(AmenityType t, List<String> accept){
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
	
}
