package net.osmand.search.core;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiCategory;

import java.util.LinkedHashSet;
import java.util.Map;

public interface CustomSearchPoiFilter extends SearchPoiTypeFilter {

	public String getFilterId();

	public String getName();

	public Object getIconResource();

	public ResultMatcher<Amenity> wrapResultMatcher(final ResultMatcher<Amenity> matcher);
	
	public default SearchSettings.SortType getDefaultSearchType() { return null; }

	// category - its subtypes, null subtypes - the whole category
	public default Map<PoiCategory, LinkedHashSet<String>> getAcceptedTypes() { return null; }

}
