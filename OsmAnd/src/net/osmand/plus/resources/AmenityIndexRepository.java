package net.osmand.plus.resources;

import java.util.List;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.PoiFilter;

public interface AmenityIndexRepository {

	public void close();
	
	public boolean checkContains(double latitude, double longitude);

	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude);

	/**
	 * Search amenities in the specified box doesn't cache results 
	 */
	public List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom, PoiFilter filter, List<Amenity> amenities,
			ResultMatcher<Amenity> matcher);

	public List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, PoiFilter filter, ResultMatcher<Amenity> matcher);

	
}
