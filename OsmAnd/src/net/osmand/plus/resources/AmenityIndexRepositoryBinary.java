package net.osmand.plus.resources;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.LatLon;
import net.osmand.plus.PoiFilter;
import net.osmand.util.MapUtils;
import net.osmand.util.Algorithms;

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
	public boolean hasChange() {
		return false; //no change ever
	}
	
	@Override
	public void clearChange() {
		//nothing to do
	}
	
	@Override
	public boolean checkContains(double latitude, double longitude) {
		return index.containsPoiData(latitude, longitude);
	}

	@Override
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		return index.containsPoiData(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
	}
	
	
	public Map<AmenityType, List<String>> searchAmenityCategoriesByName(String query, Map<AmenityType, List<String>> map) {
		try {
			return index.searchPoiCategoriesByName(query, map);
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
		}
		return map;
	}
	
	
	public List<Amenity> searchAmenitiesByName(int x, int y, int l, int t, int r, int b, String query, ResultMatcher<Amenity> resulMatcher) {
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
	public List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom, 
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

	



	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private String cFilterId;
	protected List<Amenity> cachedObjects = new ArrayList<Amenity>(); 
	protected double cTopLatitude;
	protected double cBottomLatitude;
	protected double cLeftLongitude;
	protected double cRightLongitude;
	protected int cZoom;
	
	@Override
	public synchronized boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, 
			int zoom, String filterId, List<Amenity> toFill, boolean fillFound){
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && zoom == cZoom;
		boolean noNeedToSearch = inside &&  Algorithms.objectEquals(filterId, cFilterId);
		if((inside || fillFound) && toFill != null && Algorithms.objectEquals(filterId, cFilterId)){
			for(Amenity a : cachedObjects){
				LatLon location = a.getLocation();
				if (location.getLatitude() <= topLatitude && location.getLongitude() >= leftLongitude && location.getLongitude() <= rightLongitude
						&& location.getLatitude() >= bottomLatitude) {
					toFill.add(a);
				}
			}
		}
		return noNeedToSearch;
	}
	
	@Override
	public void clearCache() {
		cachedObjects.clear();
		cTopLatitude = 0;
		cBottomLatitude = 0;
		cRightLongitude = 0;
		cLeftLongitude = 0;
		cZoom = 0;
		cFilterId = null;
	}

	@Override
	public void evaluateCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom,
			PoiFilter filter, ResultMatcher<Amenity> matcher) {
		cTopLatitude = topLatitude ;
		cBottomLatitude = bottomLatitude ;
		cLeftLongitude = leftLongitude ;
		cRightLongitude = rightLongitude ;
		cFilterId = filter == null ? null : filter.getFilterId();
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<Amenity> tempList = new ArrayList<Amenity>();
		int sleft = MapUtils.get31TileNumberX(cLeftLongitude);
		int sright = MapUtils.get31TileNumberX(cRightLongitude);
		int sbottom = MapUtils.get31TileNumberY(cBottomLatitude);
		int stop = MapUtils.get31TileNumberY(cTopLatitude);
		searchAmenities(stop, sleft, sbottom, sright, zoom, filter, tempList, matcher);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}

	}

	
}
