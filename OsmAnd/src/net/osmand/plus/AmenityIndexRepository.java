package net.osmand.plus;

import java.util.List;

import net.osmand.data.Amenity;

public interface AmenityIndexRepository {

	public void close();
	
	public boolean checkContains(double latitude, double longitude);

	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude);

	/**
	 * Search amenities in the specified box doesn't cache results 
	 */
	public List<Amenity> searchAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int limit,
			PoiFilter filter, List<Amenity> amenities);


	public void clearCache();

	public boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			String filterId, List<Amenity> toFill, boolean fillFound);

	public void evaluateCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			int limitPoi, PoiFilter filter, List<Amenity> toFill);
}
