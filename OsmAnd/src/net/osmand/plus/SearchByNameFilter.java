package net.osmand.plus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.MapUtils;
import net.osmand.plus.activities.OsmandApplication;

public class SearchByNameFilter extends PoiFilter {

	public static final String FILTER_ID = PoiFilter.BY_NAME_FILTER_ID; //$NON-NLS-1$
	
	List<Amenity> searchedAmenities = new ArrayList<Amenity>();
	
	private String query = ""; //$NON-NLS-1$
	
	public SearchByNameFilter(OsmandApplication application) {
		super(application.getString(R.string.poi_filter_by_name), FILTER_ID, new LinkedHashMap<AmenityType, LinkedHashSet<String>>(), application);
		this.distanceToSearchValues = new double[] {100, 1000, 5000};
	}
	
	@Override
	public List<Amenity> searchAgain(double lat, double lon) {
		MapUtils.sortListOfMapObject(searchedAmenities, lat, lon);
		return searchedAmenities;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	@Override
	protected List<Amenity> searchAmenities(PoiFilter poiFilter, double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, ResultMatcher<Amenity> matcher) {
		searchedAmenities.clear();
		searchedAmenities = application.getResourceManager().searchAmenitiesByName(query, 
				topLatitude, leftLongitude, bottomLatitude, rightLongitude, lat, lon, matcher);
		
		MapUtils.sortListOfMapObject(searchedAmenities, lat, lon);
		return searchedAmenities;
	}
	
	
	public List<Amenity> getSearchedAmenities() {
		return searchedAmenities;
	}

	

}
