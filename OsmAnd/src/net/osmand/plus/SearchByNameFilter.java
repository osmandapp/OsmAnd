package net.osmand.plus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.MapUtils;

public class SearchByNameFilter extends PoiFilter {

	public static final String FILTER_ID = PoiFilter.BY_NAME_FILTER_ID; //$NON-NLS-1$
	
	List<Amenity> searchedAmenities = new ArrayList<Amenity>();
	
	private String query = ""; //$NON-NLS-1$
	
	public SearchByNameFilter(OsmandApplication application) {
		super(application.getString(R.string.poi_filter_by_name), FILTER_ID, new LinkedHashMap<AmenityType, LinkedHashSet<String>>(), application);
		this.distanceToSearchValues = new double[] {100, 1000, 5000};
		this.isStandardFilter = true;
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
	protected List<Amenity> searchAmenities(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, final ResultMatcher<Amenity> matcher) {
		searchedAmenities.clear();
		final int limit = distanceInd == 0 ? 500 : -1;
		
		List<Amenity> result = application.getResourceManager().searchAmenitiesByName(query, 
				topLatitude, leftLongitude, bottomLatitude, rightLongitude, lat, lon, new ResultMatcher<Amenity>() {
					boolean elimit = false;
					@Override
					public boolean publish(Amenity object) {
						if(limit != -1 && searchedAmenities.size() > limit) {
							elimit = true;
						}
						if(matcher.publish(object)) {
							searchedAmenities.add(object);
							return true;
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return matcher.isCancelled() || elimit;
					}
				});
		MapUtils.sortListOfMapObject(result, lat, lon);
		searchedAmenities = result;
		return searchedAmenities;
	}
	
	
	public List<Amenity> getSearchedAmenities() {
		return searchedAmenities;
	}

	

}
