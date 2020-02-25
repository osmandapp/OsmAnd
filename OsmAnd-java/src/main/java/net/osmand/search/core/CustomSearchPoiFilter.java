package net.osmand.search.core;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;

public interface CustomSearchPoiFilter extends SearchPoiTypeFilter {

	public String getFilterId();

	public String getName();

	public Object getIconResource();

	public ResultMatcher<Amenity> wrapResultMatcher(final ResultMatcher<Amenity> matcher);

}
