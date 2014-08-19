package net.osmand.plus.resources;


import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.plus.PoiFilter;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class AmenityIndexRepositoryBinary implements AmenityIndexRepository {

	private final static Log log = PlatformUtil.getLog(AmenityIndexRepositoryBinary.class);
	private final BinaryMapIndexReader index;

	public AmenityIndexRepositoryBinary(BinaryMapIndexReader index) {
		this.index = index;
	}

	@Override
	public void close() {
		try {
			index.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkContains(double latitude, double longitude) {
		return index.containsPoiData(latitude, longitude);
	}

	@Override
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		return index.containsPoiData(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
	}
	
	
	public synchronized Map<AmenityType, List<String>> searchAmenityCategoriesByName(String query, Map<AmenityType, List<String>> map) {
		try {
			return index.searchPoiCategoriesByName(query, map);
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		return map;
	}
	
	
	public synchronized List<Amenity> searchAmenitiesByName(int x, int y, int l, int t, int r, int b, String query, ResultMatcher<Amenity> resulMatcher) {
		long now = System.currentTimeMillis();
		List<Amenity> amenities = Collections.emptyList();
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(x, y, query, l, r, t, b,resulMatcher);
		try {
			amenities = index.searchPoiByName(req);
			if (log.isDebugEnabled()) {
				String nm = "";
				List<MapIndex> mi = index.getMapIndexes();
				if(mi.size() > 0) {
					nm = mi.get(0).getName();
				}
				log.debug(String.format("Search for %s done in %s ms found %s (%s).",  //$NON-NLS-1$
						query, System.currentTimeMillis() - now, amenities.size(), nm)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		
		return amenities;
	}
	
	@Override
	public synchronized List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom, 
			final PoiFilter filter, final List<Amenity> amenities, ResultMatcher<Amenity> matcher) {
		long now = System.currentTimeMillis();
		SearchPoiTypeFilter poiTypeFilter = new SearchPoiTypeFilter(){
			@Override
			public boolean accept(AmenityType type, String subcategory) {
				return filter.acceptTypeSubtype(type, subcategory);
			}
		};
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom,
				poiTypeFilter, filter == null ? matcher : filter.getResultMatcher(matcher));
		try {
			List<Amenity> result = index.searchPoi(req);
			amenities.addAll(result);
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
			return amenities;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					MapUtils.get31LatitudeY(stop) + " " + MapUtils.get31LongitudeX(sleft), System.currentTimeMillis() - now, amenities.size())); //$NON-NLS-1$
		}
		return amenities;
	}

	@Override
	public synchronized List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, final PoiFilter filter, ResultMatcher<Amenity> matcher) {
		long now = System.currentTimeMillis();
		SearchPoiTypeFilter poiTypeFilter = new SearchPoiTypeFilter(){
			@Override
			public boolean accept(AmenityType type, String subcategory) {
				return filter.acceptTypeSubtype(type, subcategory);
			}
		};
		List<Amenity> result = null;
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(locations, radius,
				poiTypeFilter, filter == null ? matcher : filter.getResultMatcher(matcher));
		try {
			result = index.searchPoi(req);
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
			return result;
		}
		if (log.isDebugEnabled() && result != null) {
			log.debug(String.format("Search done in %s ms found %s.",  (System.currentTimeMillis() - now), result.size())); //$NON-NLS-1$
		}
		return result;
		
	}
	
}
