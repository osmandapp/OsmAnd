package net.osmand.plus.resources;

import java.io.File;
import java.util.List;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;

public interface AmenityIndexRepository {

	void close();
	
	boolean checkContains(double latitude, double longitude);

	boolean checkContainsInt(int top31, int left31, int bottom31, int right31);

	/**
	 * Search amenities in the specified box doesn't cache results 
	 */
	List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom,
								  SearchPoiTypeFilter filter, SearchPoiAdditionalFilter additionalFilter,
			 ResultMatcher<Amenity> matcher);

	List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, SearchPoiTypeFilter filter, 
			ResultMatcher<Amenity> matcher);

	File getFile();

	List<BinaryMapPoiReaderAdapter.PoiRegion> getReaderPoiIndexes();

	void searchMapIndex(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr);

	void searchPoi(BinaryMapIndexReader.SearchRequest<Amenity> amenitySearchRequest);

	List<Amenity> searchPoiByName(BinaryMapIndexReader.SearchRequest<Amenity> searchRequest);
}
