package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;

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
		int x = MapUtils.get31TileNumberX(longitude);
		int y = MapUtils.get31TileNumberY(latitude);
		for(BinaryIndexPart i : index.getIndexes()){
			if(i instanceof MapIndex){
				List<MapRoot> rs = ((MapIndex) i).getRoots();
				if(!rs.isEmpty()){
					MapRoot rt = rs.get(0);
					if(rt.getLeft() <= x && rt.getRight() >= x &&
							rt.getTop() <= y && rt.getBottom() >= y){
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		int leftX = MapUtils.get31TileNumberX(leftLongitude);
		int rightX = MapUtils.get31TileNumberX(rightLongitude);
		int bottomY = MapUtils.get31TileNumberY(bottomLatitude);
		int topY = MapUtils.get31TileNumberY(topLatitude);
		for(BinaryIndexPart i : index.getIndexes()){
			if(i instanceof MapIndex){
				List<MapRoot> rs = ((MapIndex) i).getRoots();
				if(!rs.isEmpty()){
					MapRoot rt = rs.get(0);
					if(rightX < rt.getLeft() || leftX > rt.getRight()){
						continue;
					}
					if(topY >  rt.getBottom() || bottomY < rt.getTop()){
						continue;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public List<Amenity> searchAmenities(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int limit,
			final PoiFilter filter, final List<Amenity> amenities) {
		long now = System.currentTimeMillis();
		int sleft = MapUtils.get31TileNumberX(leftLongitude);
		int sright = MapUtils.get31TileNumberX(rightLongitude);
		int sbottom = MapUtils.get31TileNumberY(bottomLatitude);
		int stop = MapUtils.get31TileNumberY(topLatitude);
		
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(sleft, sright, stop, sbottom, 16);
		req.setSearchFilter(new SearchFilter(){

			@Override
			public boolean accept(TIntArrayList types, MapIndex root) {
				for (int j = 0; j < types.size(); j++) {
					int wholeType = types.get(j);
					TagValuePair pair = root.decodeType(wholeType);
					if (pair != null) {
						AmenityType type = MapRenderingTypes.getAmenityType(pair.tag, pair.value);
						if (type != null) {
							if(filter.acceptTypeSubtype(type, MapRenderingTypes.getAmenitySubtype(pair.tag, pair.value))){
								return true;
							}
						}
					}
				}
				return false;
			}
			
		});
		try {
			index.searchMapIndex(req);
			for(BinaryMapDataObject o : req.getSearchResults()){
				if(o.getPointsLength() == 0){
					continue;
				}
				
				int xTile = 0;
				int yTile = 0;
				for(int i=0; i<o.getPointsLength();i++){
					xTile += o.getPoint31XTile(i);
					yTile += o.getPoint31YTile(i);
				}
				double lat = MapUtils.get31LatitudeY(yTile/o.getPointsLength());
				double lon = MapUtils.get31LongitudeX(xTile/o.getPointsLength());
				
				
				for (int j = 0; j < o.getTypes().length; j++) {
					TagValuePair pair = o.getMapIndex().decodeType(o.getTypes()[j]);
					if(pair != null){
						Amenity am = new Amenity();
						am.setId(o.getId());
						am.setLocation(lat, lon);
						am.setName(o.getName());
						am.setEnName(Junidecode.unidecode(am.getName()));
						AmenityType type = MapRenderingTypes.getAmenityType(pair.tag, pair.value);
						String subtype = MapRenderingTypes.getAmenitySubtype(pair.tag, pair.value);
						am.setType(type);
						am.setSubType(subtype);
						am.setOpeningHours(null);
						am.setPhone(null); 
						am.setSite(null);
						amenities.add(am);
						break;
					}
				}
				
				
			}
		} catch (IOException e) {
			log.error("Error searching amenities", e); //$NON-NLS-1$
			return amenities;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, amenities.size())); //$NON-NLS-1$
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
			int limitPoi, PoiFilter filter, List<Amenity> toFill) {
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cFilterId = filter == null ? null : filter.getFilterId();
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<Amenity> tempList = new ArrayList<Amenity>();
		searchAmenities(cTopLatitude, cLeftLongitude, cBottomLatitude, cRightLongitude, limitPoi, filter, tempList);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}

		checkCachedAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude, cZoom, filter.getFilterId(), toFill, true);

	}

	
}
