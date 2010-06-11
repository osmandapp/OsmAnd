package com.osmand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;

public class PoiFilter {
	
	public static String STD_PREFIX = "std_";
	public static String USER_PREFIX = "user_";
	
	private Map<AmenityType, List<String>> acceptedTypes = new LinkedHashMap<AmenityType, List<String>>();
	private String filterId;
	private String name;
	private final boolean isStandardFilter;
	
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
		return false;
	}
	
	public List<Amenity> searchFurther(){
		return null;
	}
	
	public String getSearchArea(){
		return null;
	}
	
	public List<Amenity> getLastSearchedResults(){
		return null;
	}
	
	public List<Amenity> initializeNewSearch(double lat, double lon){
		return null; 
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
	
	
	public void setTypeToAccept(AmenityType type, boolean accept){
		if(accept){
			acceptedTypes.put(type, null);
		} else {
			acceptedTypes.remove(type);
		}
	}
	
	public void selectSubTypesToAccept(AmenityType t, List<String> accept){
		acceptedTypes.put(t, accept);
	}
	
	public String getFilterId(){
		return filterId;
	}
	
	public boolean isStandardFilter(){
		return isStandardFilter;
	}
	
}
