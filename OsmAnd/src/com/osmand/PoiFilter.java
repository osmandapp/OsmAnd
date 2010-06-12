package com.osmand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.data.index.IndexConstants.IndexPoiTable;
import com.osmand.osm.MapUtils;

public class PoiFilter {
	
	public static String STD_PREFIX = "std_";
	public static String USER_PREFIX = "user_";
	
	private Map<AmenityType, List<String>> acceptedTypes = new LinkedHashMap<AmenityType, List<String>>();
	private String filterByName = null;

	private String filterId;
	private String name;
	private final boolean isStandardFilter;
	
	private final static int finalZoom = 8;
	private final static int initialZoom = 13;
	private final static int maxCount = 200;
	private int zoom = initialZoom;
	
	
	// constructor for standard filters
	public PoiFilter(AmenityType type){
		isStandardFilter = true;
		filterId = STD_PREFIX + type;
		name = type == null ? "Closest poi" : Algoritms.capitalizeFirstLetterAndLowercase(type.name()).replace('_', ' ');
		if(type == null){
			for(AmenityType t : AmenityType.values()){
				acceptedTypes.put(t, null);
			}
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
		this.acceptedTypes.putAll(acceptedTypes);
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
		if(zoom <= 15){
			int d = (int) (1.5 * (1 << (zoom - 15)));
			return " < " + d + " km";
		} else {
			return " < 500 m";
		}
	}
	
	public List<Amenity> initializeNewSearch(double lat, double lon, int firstTimeLimit){
		zoom = initialZoom;
		if(areAllTypesAccepted()){
			zoom += 2;
		}
		List<Amenity> amenityList = ResourceManager.getResourceManager().searchAmenities(this, lat, lon, zoom, maxCount);
		MapUtils.sortListOfMapObject(amenityList, lat, lon);
		while (amenityList.size() > firstTimeLimit) {
			amenityList.remove(amenityList.size() - 1);
		}
		
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
		if(acceptedTypes.containsKey(type)){
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
			acceptedTypes.put(type, null);
		} else {
			acceptedTypes.remove(type);
		}
	}
	
	public String buildSqlWhereFilter(){
		if(areAllTypesAccepted()){
			return null;
		}
		if(acceptedTypes.size() == 0){
			return "1 > 1"; 
		}
		StringBuilder b = new StringBuilder();
		b.append("(");
		boolean first = true;
		for(AmenityType a : acceptedTypes.keySet()){
			if(first){
				first = false;
			} else {
				b.append(" OR ");
			}
			b.append("(");
			b.append(IndexPoiTable.TYPE.name().toLowerCase()).append(" = '").append(AmenityType.valueToString(a)).append("'");
			if(acceptedTypes.get(a) != null){
				List<String> list = acceptedTypes.get(a);
				b.append(" AND ");
				b.append(IndexPoiTable.SUBTYPE.name().toLowerCase()).append(" IN (");
				boolean bfirst = true;
				for(String s : list){
					if(bfirst){
						bfirst = false;
					} else {
						b.append(", ");
					}
					b.append("'").append(s).append("'");
				}
				b.append(")");
			}
			b.append(")");
		}
			
		b.append(")");
		return b.toString();
		
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
