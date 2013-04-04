package net.osmand.plus.resources;

import java.util.List;

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


	public void clearCache();

	public boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			String filterId, List<Amenity> toFill, boolean fillFound);

	public void evaluateCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			PoiFilter filter, ResultMatcher<Amenity> matcher);

	public boolean hasChange();

	public void clearChange();
	
}
