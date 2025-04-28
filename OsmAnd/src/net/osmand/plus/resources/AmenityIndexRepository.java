package net.osmand.plus.resources;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.data.Amenity;

import java.io.File;
import java.util.List;

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

	List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius,
			SearchPoiTypeFilter filter, ResultMatcher<Amenity> matcher);

	File getFile();

	boolean isWorldMap();

	List<PoiRegion> getReaderPoiIndexes();

	void searchMapIndex(@NonNull SearchRequest<BinaryMapDataObject> searchRequest);

	void searchPoi(@NonNull SearchRequest<Amenity> searchRequest);

	List<Amenity> searchPoiByName(@NonNull SearchRequest<Amenity> searchRequest);

	boolean isPoiSectionIntersects(@NonNull SearchRequest<?> searchRequest);
}
