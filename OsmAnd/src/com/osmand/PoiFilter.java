package com.osmand;

import java.util.List;

import com.osmand.data.Amenity;

public class PoiFilter {
	
	public boolean isSearchFurtherAvailable(){
		return false;
	}
	
	public List<Amenity> searchFurther(){
		return null;
	}
	
	public List<Amenity> getLastSearchedResults(){
		return null;
	}
	
	public List<Amenity> initializeNewSearch(double lat, double lon){
		return null; 
	}
	
	public String getName(){
		return null;
	}
	
	public String getFilterId(){
		return null;
	}
	
	public boolean isStandardFilter(){
		return true;
	}

}
