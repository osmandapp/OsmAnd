package net.osmand.plus.api;

import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.PoiFilter;

public interface InternalOsmAndAPI {

	public List<Amenity> searchAmenities(PoiFilter filter,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			double lat, double lon, ResultMatcher<Amenity> matcher);
	
	public List<Amenity> searchAmenitiesByName(String searchQuery,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, 
			double lat, double lon, ResultMatcher<Amenity> matcher);

	
	public boolean isNavigationServiceStarted();
	
	public boolean isNavigationServiceStartedForNavigation();
	
	public void startNavigationService(boolean forNavigation);
	
	public void stopNavigationService();
	
}
