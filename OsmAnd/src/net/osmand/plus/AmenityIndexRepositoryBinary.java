package net.osmand.plus;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

public class AmenityIndexRepositoryBinary implements AmenityIndexRepository {

	private final static Log log = LogUtil.getLog(AmenityIndexRepositoryBinary.class);
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
	
	@Override
	public List<Amenity> searchAmenities(int stop, int sleft, int sbottom, int sright, int zoom, 
			final PoiFilter filter, final List<Amenity> amenities) {
		long now = System.currentTimeMillis();
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom);
		req.setPoiTypeFilter(new SearchPoiTypeFilter(){
			@Override
			public boolean accept(AmenityType type, String subcategory) {
				return filter.acceptTypeSubtype(type, subcategory);
			}
		});
		try {
			List<Amenity> result = index.searchPoi(req);
			for(Amenity am : result){
				if(filter.acceptTypeSubtype(am.getType(), am.getSubType())){
					amenities.add(am);
				}
			}
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
	
	public synchronized boolean checkCachedAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, 
			int zoom, String filterId, List<Amenity> toFill, boolean fillFound){
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && zoom == cZoom;
		boolean noNeedToSearch = inside &&  Algoritms.objectEquals(filterId, cFilterId);
		if((inside || fillFound) && toFill != null && Algoritms.objectEquals(filterId, cFilterId)){
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
			PoiFilter filter, List<Amenity> toFill) {
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cFilterId = filter == null ? null : filter.getFilterId();
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<Amenity> tempList = new ArrayList<Amenity>();
		int sleft = MapUtils.get31TileNumberX(cLeftLongitude);
		int sright = MapUtils.get31TileNumberX(cRightLongitude);
		int sbottom = MapUtils.get31TileNumberY(cBottomLatitude);
		int stop = MapUtils.get31TileNumberY(cTopLatitude);
		searchAmenities(stop, sleft, sbottom, sright, zoom, filter, tempList);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}

		checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, cZoom, filter.getFilterId(), toFill, true);

	}

	
}
