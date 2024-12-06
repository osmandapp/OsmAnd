package net.osmand.plus.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class SearchByNameFilter extends PoiUIFilter {

	public static final String FILTER_ID = PoiUIFilter.BY_NAME_FILTER_ID; //$NON-NLS-1$
	
	public SearchByNameFilter(OsmandApplication application) {
		super(application);
		this.name = application.getString(R.string.poi_filter_by_name);
		this.filterId = FILTER_ID;
		this.distanceToSearchValues = new double[] {100, 1000, 20000};
	}
	
	@Override
	public boolean isAutomaticallyIncreaseSearch() {
		return false;
	}
	
	@Override
	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, int zoom, ResultMatcher<Amenity> matcher) {
		currentSearchResult = new ArrayList<Amenity>();
		int limit = distanceInd == 0 ? 500 : -1;
		List<Amenity> result = Collections.emptyList();
		if (!Algorithms.isBlank(getFilterByName())) {
			result = app.getResourceManager().searchAmenitiesByName(getFilterByName(), topLatitude,
					leftLongitude, bottomLatitude, rightLongitude, lat, lon, new ResultMatcher<Amenity>() {
						boolean elimit;

						@Override
						public boolean publish(Amenity object) {
							if (limit != -1 && currentSearchResult.size() > limit) {
								elimit = true;
							}
							if (matcher.publish(object)) {
								// Causes concurrent modification exception (below)
//								currentSearchResult.add(object);
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
		}
		currentSearchResult = result;
		return currentSearchResult;
	}

}
