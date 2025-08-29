package net.osmand.search.core;

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

	void searchMapIndex(SearchRequest<BinaryMapDataObject> searchRequest);

	void searchPoi(SearchRequest<Amenity> searchRequest);

	List<Amenity> searchPoiByName(SearchRequest<Amenity> searchRequest);

	boolean isPoiSectionIntersects(SearchRequest<?> searchRequest);

	List<Amenity> searchAmenitiesByName(int x, int y, int l, int t, int r, int b, String query, ResultMatcher<Amenity> resulMatcher);

	/**
	 * @return {@code true} if this repository contains auxiliary data such as reviews, rather than the basic POI information.
	 */
	boolean isAuxiliaryData();
}
