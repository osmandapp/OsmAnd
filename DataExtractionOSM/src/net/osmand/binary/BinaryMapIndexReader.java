package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.LogUtil;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CitiesBlock;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapDataBox;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapEncodingRule;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapRootLevel;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	public final static int TRANSPORT_STOP_ZOOM = 24;
	public static final int SHIFT_COORDINATES = 5;
	private final static Log log = LogUtil.getLog(BinaryMapIndexReader.class);
	
	private final RandomAccessFile raf;
	/*private*/ int version;
	/*private */long dateCreated;
	// keep them immutable inside
	/*private */ boolean basemap = false;
	/*private */List<MapIndex> mapIndexes = new ArrayList<MapIndex>();
	/*private */List<PoiRegion> poiIndexes = new ArrayList<PoiRegion>();
	/*private */List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	/*private */List<TransportIndex> transportIndexes = new ArrayList<TransportIndex>();
	/*private */List<RouteRegion> routingIndexes = new ArrayList<RouteRegion>();
	/*private */List<BinaryIndexPart> indexes = new ArrayList<BinaryIndexPart>();
	
	protected CodedInputStreamRAF codedIS;
	
	private final BinaryMapTransportReaderAdapter transportAdapter;
	private final BinaryMapPoiReaderAdapter poiAdapter;
	private final BinaryMapAddressReaderAdapter addressAdapter;
	private final BinaryMapRouteReaderAdapter routeAdapter;
	
	private static String BASEMAP_NAME = "basemap";

	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024 * 5);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		init();
	}
	
	/*private */BinaryMapIndexReader(final RandomAccessFile raf, boolean init) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024 * 5);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		if(init) {
			init();
		}
	}
	
	public BinaryMapIndexReader(final RandomAccessFile raf, BinaryMapIndexReader referenceToSameFile) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024 * 5);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
		version = referenceToSameFile.version;
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		mapIndexes = new ArrayList<BinaryMapIndexReader.MapIndex>(referenceToSameFile.mapIndexes);
		poiIndexes = new ArrayList<PoiRegion>(referenceToSameFile.poiIndexes);
		addressIndexes = new ArrayList<AddressRegion>(referenceToSameFile.addressIndexes);
		transportIndexes = new ArrayList<TransportIndex>(referenceToSameFile.transportIndexes);
		routingIndexes = new ArrayList<RouteRegion>(referenceToSameFile.routingIndexes);
		indexes = new ArrayList<BinaryIndexPart>(referenceToSameFile.indexes);
		basemap = referenceToSameFile.basemap;
	}
	
	
	public long getDateCreated() {
		return dateCreated;
	}
	
	private void init() throws IOException {
		boolean initCorrectly = false;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(!initCorrectly){
					throw new IOException("Corrupted file. It should be ended as it starts with version"); //$NON-NLS-1$
				}
				return;
			case OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER :
				version = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndStructure.DATECREATED_FIELD_NUMBER :
				dateCreated = codedIS.readInt64();
				break;
			case OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER:
				MapIndex mapIndex = new MapIndex();
				mapIndex.length = readInt();
				mapIndex.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(mapIndex.length);
				readMapIndex(mapIndex, false);
				basemap = basemap || mapIndex.isBaseMap();
				codedIS.popLimit(oldLimit);
				codedIS.seek(mapIndex.filePointer + mapIndex.length);
				mapIndexes.add(mapIndex);
				indexes.add(mapIndex);
				break;
			case OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER:
				AddressRegion region = new AddressRegion();
				region.length = readInt();
				region.filePointer = codedIS.getTotalBytesRead();
				if(addressAdapter != null){
					oldLimit = codedIS.pushLimit(region.length);
					addressAdapter.readAddressIndex(region);
					if(region.name != null){
						addressIndexes.add(region);
						indexes.add(region);
					}
					codedIS.popLimit(oldLimit);
				}
				codedIS.seek(region.filePointer + region.length);
				break;
			case OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER:
				TransportIndex ind = new TransportIndex();
				ind.length = readInt();
				ind.filePointer = codedIS.getTotalBytesRead();
				if (transportAdapter != null) {
					oldLimit = codedIS.pushLimit(ind.length);
					transportAdapter.readTransportIndex(ind);
					codedIS.popLimit(oldLimit);
					transportIndexes.add(ind);
					indexes.add(ind);
				}
				codedIS.seek(ind.filePointer + ind.length);
				break;
			case OsmandOdb.OsmAndStructure.ROUTINGINDEX_FIELD_NUMBER:
				RouteRegion routeReg = new RouteRegion();
				routeReg.length = readInt();
				routeReg.filePointer = codedIS.getTotalBytesRead();
				if (routeAdapter != null) {
					oldLimit = codedIS.pushLimit(routeReg.length);
					routeAdapter.readRouteIndex(routeReg);
					codedIS.popLimit(oldLimit);
					routingIndexes.add(routeReg);
					indexes.add(routeReg);
				}
				codedIS.seek(routeReg.filePointer + routeReg.length);
				break;
			case OsmandOdb.OsmAndStructure.POIINDEX_FIELD_NUMBER:
				PoiRegion poiInd = new PoiRegion();
				poiInd.length = readInt();
				poiInd.filePointer = codedIS.getTotalBytesRead();
				if (poiAdapter != null) {
					oldLimit = codedIS.pushLimit(poiInd.length);
					poiAdapter.readPoiIndex(poiInd, false);
					codedIS.popLimit(oldLimit);
					poiIndexes.add(poiInd);
					indexes.add(poiInd);
				}
				codedIS.seek(poiInd.filePointer + poiInd.length);
				break;
			case OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER :
				int cversion = codedIS.readUInt32();
				calculateCenterPointForRegions();
				initCorrectly = cversion == version;
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void calculateCenterPointForRegions(){
		for(AddressRegion reg : addressIndexes){
			for(MapIndex map : mapIndexes){
				if(Algoritms.objectEquals(reg.name, map.name)){
					if(map.getRoots().size() > 0){
						MapRoot mapRoot = map.getRoots().get(map.getRoots().size() - 1);
						double cy = (MapUtils.get31LatitudeY(mapRoot.getBottom()) + MapUtils.get31LatitudeY(mapRoot.getTop())) / 2;
						double cx = (MapUtils.get31LongitudeX(mapRoot.getLeft()) + MapUtils.get31LongitudeX(mapRoot.getRight())) / 2;
						reg.calculatedCenter = new LatLon(cy, cx);
						break;
					}
				}
			}
		}
	}
	
	public List<BinaryIndexPart> getIndexes() {
		return indexes;
	}
	
	public List<MapIndex> getMapIndexes() {
		return mapIndexes;
	}
	
	public List<RouteRegion> getRoutingIndexes() {
		return routingIndexes;
	}
	
	public boolean isBasemap() {
		return basemap;
	}
	
	public boolean containsMapData(){
		return mapIndexes.size() > 0;
	}
	
	public boolean containsPoiData(){
		return poiIndexes.size() > 0;
	}
	
	public boolean containsRouteData(){
		return routingIndexes.size() > 0;
	}
	
	public boolean containsPoiData(double latitude, double longitude) {
		for (PoiRegion index : poiIndexes) {
			if (index.rightLongitude >= longitude && index.leftLongitude <= longitude &&
					index.topLatitude >= latitude && index.bottomLatitude <= latitude) {
				return true;
			}
		}
		return false;
	}
	
	
	public boolean containsPoiData(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		for (PoiRegion index : poiIndexes) {
			if (index.rightLongitude >= leftLongitude && index.leftLongitude <= rightLongitude && 
					index.topLatitude >= bottomLatitude && index.bottomLatitude <= topLatitude) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsMapData(int tile31x, int tile31y, int zoom){
		for(MapIndex mapIndex :  mapIndexes){
			for(MapRoot root : mapIndex.getRoots()){
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (tile31x >= root.left && tile31x <= root.right && root.top <= tile31y &&  root.bottom >= tile31y) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean containsMapData(int left31x, int top31y, int right31x, int bottom31y, int zoom){
		for(MapIndex mapIndex :  mapIndexes){
			for(MapRoot root : mapIndex.getRoots()){
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (right31x >= root.left && left31x <= root.right && root.top <= bottom31y &&  root.bottom >= top31y) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean containsAddressData(){
		return addressIndexes.size() > 0;
	}
	
	public boolean hasTransportData(){
		return transportIndexes.size() > 0;
	}
	
	

	public RandomAccessFile getRaf() {
		return raf;
	}
	
	public int readByte() throws IOException{
		byte b = codedIS.readRawByte();
		if(b < 0){
			return b + 256;
		} else {
			return b;
		}
	}
	
	public final int readInt() throws IOException {
		int ch1 = readByte();
		int ch2 = readByte();
		int ch3 = readByte();
		int ch4 = readByte();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	
	public int getVersion() {
		return version;
	}
	
	

	protected void skipUnknownField(int tag) throws IOException {
		int wireType = WireFormat.getTagWireType(tag);
		if(wireType == WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED){
			int length = readInt();
			codedIS.skipRawBytes(length);
		} else {
			codedIS.skipField(tag);
		}
	}
	
	
	/**
	 * Transport public methods
	 */
	public TIntObjectHashMap<TransportRoute> getTransportRoutes(int[] filePointers) throws IOException {
		TIntObjectHashMap<TransportRoute> result = new TIntObjectHashMap<TransportRoute>();
		Map<TransportIndex, TIntArrayList> groupPoints = new HashMap<TransportIndex, TIntArrayList>();
		for(int filePointer : filePointers){
			TransportIndex ind = getTransportIndex(filePointer);
			if (ind != null) {
				if (!groupPoints.containsKey(ind)) {
					groupPoints.put(ind, new TIntArrayList());
				}
				groupPoints.get(ind).add(filePointer);
			}
		}
		Iterator<Entry<TransportIndex, TIntArrayList>> it = groupPoints.entrySet().iterator();
		if(it.hasNext()){
			Entry<TransportIndex, TIntArrayList> e = it.next();
			TransportIndex ind = e.getKey();
			TIntArrayList pointers = e.getValue();
			pointers.sort();
			TIntObjectHashMap<String> stringTable = new TIntObjectHashMap<String>();
			for (int i = 0; i < pointers.size(); i++) {
				int filePointer = pointers.get(i);
				TransportRoute transportRoute = transportAdapter.getTransportRoute(filePointer, stringTable, false);
				result.put(filePointer, transportRoute);
			}
			transportAdapter.initializeStringTable(ind, stringTable);
			for(TransportRoute r : result.values(new TransportRoute[result.size()])){
				transportAdapter.initializeNames(false, r, stringTable);
			}
		}
		return result;
	}
	
	/**
	 * Transport public methods
	 */
	public List<net.osmand.data.TransportRoute> getTransportRouteDescriptions(TransportStop stop) throws IOException {
		TransportIndex ind = getTransportIndex(stop.getFileOffset());
		if(ind == null){
			return null;
		}
		List<net.osmand.data.TransportRoute> list = new ArrayList<TransportRoute>();
		TIntObjectHashMap<String> stringTable = new TIntObjectHashMap<String>();
		for(int filePointer : stop.getReferencesToRoutes()){
			TransportRoute tr = transportAdapter.getTransportRoute(filePointer, stringTable, true);
			if(tr != null){
				list.add(tr);				
			}
		}
		transportAdapter.initializeStringTable(ind, stringTable);
		for(TransportRoute route : list){
			transportAdapter.initializeNames(true, route, stringTable);
		}
		return list;
	}
	
	public boolean transportStopBelongsTo(TransportStop s){
		return getTransportIndex(s.getFileOffset()) != null;
	}
	
	public List<TransportIndex> getTransportIndexes() {
		return transportIndexes;
	}
	
	private TransportIndex getTransportIndex(int filePointer) {
		TransportIndex ind = null;
		for(TransportIndex i : transportIndexes){
			if(i.filePointer <= filePointer && (filePointer - i.filePointer) < i.length){
				ind = i;
				break;
			}
		}
		return ind;
	}
	
	public boolean containTransportData(double latitude, double longitude) {
		double x = MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, longitude);
		double y = MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, latitude);
		for (TransportIndex index : transportIndexes) {
			if (index.right >= x &&  index.left <= x && index.top <= y && index.bottom >= y) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containTransportData(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude){
		double leftX = MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, leftLongitude);
		double topY = MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, topLatitude);
		double rightX = MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, rightLongitude);
		double bottomY = MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, bottomLatitude);
		for (TransportIndex index : transportIndexes) {
			if (index.right >= leftX &&  index.left <= rightX && index.top <= bottomY && index.bottom >= topY) {
				return true;
			}
		}
		return false;
	}
	
	public List<TransportStop> searchTransportIndex(SearchRequest<TransportStop> req) throws IOException {
		for (TransportIndex index : transportIndexes) {
			if (index.stopsFileLength == 0 || index.right < req.left || index.left > req.right || index.top > req.bottom
					|| index.bottom < req.top) {
				continue;
			}
			codedIS.seek(index.stopsFileOffset);
			int oldLimit = codedIS.pushLimit(index.stopsFileLength);
			int offset = req.searchResults.size();
			transportAdapter.searchTransportTreeBounds(0, 0, 0, 0, req);
			codedIS.popLimit(oldLimit);
			if (req.stringTable != null) {
				transportAdapter.initializeStringTable(index, req.stringTable);
				for (int i = offset; i < req.searchResults.size(); i++) {
					TransportStop st = req.searchResults.get(i);
					transportAdapter.initializeNames(req.stringTable, st);
				}
			}
		}
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
	}
	
	/**
	 * Address public methods
	 */
	public List<String> getRegionNames(){
		List<String> names = new ArrayList<String>();
		for(AddressRegion r : addressIndexes){
			names.add(r.name);
		}
		return names;
	}
	
	public LatLon getRegionCenter(String name) {
		AddressRegion rg = getRegionByName(name);
		if (rg != null) {
			return rg.calculatedCenter;
		}
		return null;
	}
	
	private AddressRegion getRegionByName(String name){
		for(AddressRegion r : addressIndexes){
			if(r.name.equals(name)){
				return r;
			}
		}
		throw new IllegalArgumentException(name);
	}
	
	public List<City> getCities(String region, SearchRequest<City> resultMatcher,  
			int cityType) throws IOException {
		return getCities(region, resultMatcher, null, false, cityType);
	}
	public List<City> getCities(String region, SearchRequest<City> resultMatcher, StringMatcher matcher, boolean useEn, 
			int cityType) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		for(CitiesBlock block :  r.cities) {
			if(block.type == cityType) {
				codedIS.seek(block.filePointer);
				int old = codedIS.pushLimit(block.length);
				addressAdapter.readCities(cities, resultMatcher, matcher, useEn);
				codedIS.popLimit(old);
			}
		}
		return cities;
	}
	
	
	public void preloadStreets(City c, SearchRequest<Street> resultMatcher) throws IOException {
		checkAddressIndex(c.getFileOffset());
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readCityStreets(resultMatcher, c);
		codedIS.popLimit(old);
	}
	
	private void checkAddressIndex(int offset){
		boolean ok = false;
		for(AddressRegion r : addressIndexes){
			if(offset >= r.filePointer  && offset <= (r.length + r.filePointer)){
				ok = true;
				break;
			}
		}
		if(!ok){
			throw new IllegalArgumentException("Illegal offset " + offset); //$NON-NLS-1$
		}
	}
	
	public void preloadBuildings(Street s, SearchRequest<Building> resultMatcher) throws IOException {
		checkAddressIndex(s.getFileOffset());
		codedIS.seek(s.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		City city = s.getCity();
		addressAdapter.readStreet(s, resultMatcher, true, 0, 0, city != null  && city.isPostcode() ? city.getName() : null);
		codedIS.popLimit(old);
	}
	
	
	/**
	 * Map public methods 
	 */

	private void readMapIndex(MapIndex index, boolean onlyInitEncodingRules) throws IOException {
		int defaultId = 1;
		int oldLimit ;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			
			switch (tag) {
			case 0:
				// encoding rules are required!
				if (onlyInitEncodingRules) {
					index.finishInitializingTags();
				}
				return;
			case OsmandOdb.OsmAndMapIndex.NAME_FIELD_NUMBER :
				index.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndMapIndex.RULES_FIELD_NUMBER :
				if (onlyInitEncodingRules) {
					int len = codedIS.readInt32();
					oldLimit = codedIS.pushLimit(len);
					readMapEncodingRule(index, defaultId++);
					codedIS.popLimit(oldLimit);
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER :
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				if (!onlyInitEncodingRules) {
					oldLimit = codedIS.pushLimit(length);
					MapRoot mapRoot = readMapLevel(new MapRoot());
					mapRoot.length = length;
					mapRoot.filePointer = filePointer;
					index.getRoots().add(mapRoot);
					codedIS.popLimit(oldLimit);
				}
				codedIS.seek(filePointer + length);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
	private void readMapEncodingRule(MapIndex index, int id) throws IOException {
		int type = 0;
		String tags = null;
		String val = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				index.initMapEncodingRule(type, id, tags, val);
				return;
			case MapEncodingRule.VALUE_FIELD_NUMBER :
				val = codedIS.readString().intern();
				break;
			case MapEncodingRule.TAG_FIELD_NUMBER :
				tags = codedIS.readString().intern();
				break;
			case MapEncodingRule.TYPE_FIELD_NUMBER :
				type = codedIS.readUInt32();
				break;
			case MapEncodingRule.ID_FIELD_NUMBER :
				id = codedIS.readUInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}


	private MapRoot readMapLevel(MapRoot root) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return root;
			case MapRootLevel.BOTTOM_FIELD_NUMBER :
				root.bottom = codedIS.readInt32();
				break;
			case MapRootLevel.LEFT_FIELD_NUMBER :
				root.left = codedIS.readInt32();
				break;
			case MapRootLevel.RIGHT_FIELD_NUMBER :
				root.right = codedIS.readInt32();
				break;
			case MapRootLevel.TOP_FIELD_NUMBER :
				root.top = codedIS.readInt32();
				break;
			case MapRootLevel.MAXZOOM_FIELD_NUMBER :
				root.maxZoom = codedIS.readInt32();
				break;
			case MapRootLevel.MINZOOM_FIELD_NUMBER :
				root.minZoom = codedIS.readInt32();
				break;
			case MapRootLevel.BOXES_FIELD_NUMBER :
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				if (root.trees != null) {
					MapTree r = new MapTree();
					// left, ... already initialized
					r.length = length;
					r.filePointer = filePointer;
					int oldLimit = codedIS.pushLimit(r.length);
					readMapTreeBounds(r, root.left, root.right, root.top, root.bottom);
					root.trees.add(r);
					codedIS.popLimit(oldLimit);
				}
				codedIS.seek(filePointer + length);
				break;
			case MapRootLevel.BLOCKS_FIELD_NUMBER :
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}
	
	private void readMapTreeBounds(MapTree tree, int aleft, int aright, int atop, int abottom) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case MapDataBox.BOTTOM_FIELD_NUMBER :
				tree.bottom = codedIS.readSInt32() + abottom;
				break;
			case MapDataBox.LEFT_FIELD_NUMBER :
				tree.left = codedIS.readSInt32() + aleft;
				break;
			case MapDataBox.RIGHT_FIELD_NUMBER :
				tree.right = codedIS.readSInt32() + aright;
				break;
			case MapDataBox.TOP_FIELD_NUMBER :
				tree.top = codedIS.readSInt32() + atop;
				break;
			case MapDataBox.OCEAN_FIELD_NUMBER :
				if(codedIS.readBool()) {
					tree.ocean = Boolean.TRUE;
				} else {
					tree.ocean = Boolean.FALSE;
				}
				break;
			case MapDataBox.SHIFTTOMAPDATA_FIELD_NUMBER :
				tree.mapDataBlock = readInt() + tree.filePointer;
				break;
		
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
	
	public List<BinaryMapDataObject> searchMapIndex(SearchRequest<BinaryMapDataObject> req) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		List<MapTree> foundSubtrees = new ArrayList<MapTree>();
		for (MapIndex mapIndex : mapIndexes) {
			for (MapRoot index : mapIndex.getRoots()) {
				if (index.minZoom <= req.zoom && index.maxZoom >= req.zoom) {
					if (index.right < req.left || index.left > req.right || index.top > req.bottom || index.bottom < req.top) {
						continue;
					}
					// lazy initializing rules
					if(mapIndex.encodingRules.isEmpty()) {
						codedIS.seek(mapIndex.filePointer);
						int oldLimit = codedIS.pushLimit(mapIndex.length);
						readMapIndex(mapIndex, true);
						codedIS.popLimit(oldLimit);
					}
					// lazy initializing trees
					if(index.trees == null){
						index.trees = new ArrayList<MapTree>();
						codedIS.seek(index.filePointer);
						int oldLimit = codedIS.pushLimit(index.length);
						readMapLevel(index);
						codedIS.popLimit(oldLimit);
					}
					
					for (MapTree tree : index.trees) {
						if (tree.right < req.left || tree.left > req.right || tree.top > req.bottom || tree.bottom < req.top) {
							continue;
						}
						codedIS.seek(tree.filePointer);
						int oldLimit = codedIS.pushLimit(tree.length);
						searchMapTreeBounds(tree, index, req, foundSubtrees);
						codedIS.popLimit(oldLimit);
					}
					
					Collections.sort(foundSubtrees, new Comparator<MapTree>() {
						@Override
						public int compare(MapTree o1, MapTree o2) {
							return o1.mapDataBlock < o2.mapDataBlock ? -1 : (o1.mapDataBlock == o2.mapDataBlock ? 0 : 1);
						}
					});
					for(MapTree tree : foundSubtrees) {
						if(!req.isCancelled()){
							codedIS.seek(tree.mapDataBlock);
							int length = codedIS.readRawVarint32();
							int oldLimit = codedIS.pushLimit(length);
							readMapDataBlocks(req, tree, mapIndex);
							codedIS.popLimit(oldLimit);
						}
					}
					foundSubtrees.clear();
				}
				
			}
		}
		
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
	}
	
	
	
	protected void readMapDataBlocks(SearchRequest<BinaryMapDataObject> req, MapTree tree, MapIndex root) throws IOException {
		List<BinaryMapDataObject> tempResults = null;
		long baseId  = 0;
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(tempResults != null) {
					for(BinaryMapDataObject obj : tempResults) {
						req.publish(obj);
					}
				}
				return;
			case MapDataBlock.BASEID_FIELD_NUMBER:
				baseId = codedIS.readUInt64();
				break;
			case MapDataBlock.DATAOBJECTS_FIELD_NUMBER:
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				BinaryMapDataObject mapObject = readMapDataObject(tree, req, root);
				if (mapObject != null) {
					mapObject.setId(mapObject.getId() + baseId);
					if (tempResults == null) {
						tempResults = new ArrayList<BinaryMapDataObject>();
					}
					tempResults.add(mapObject);
				}
				codedIS.popLimit(oldLimit);
				break;
			case MapDataBlock.STRINGTABLE_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				if (tempResults != null) {
					List<String> stringTable = readStringTable();
					for (int i = 0; i < tempResults.size(); i++) {
						BinaryMapDataObject rs = tempResults.get(i);
						if (rs.objectNames != null) {
							int[] keys = rs.objectNames.keys();
							for (int j = 0; j < keys.length; j++) {
								rs.objectNames.put(keys[j], stringTable.get(rs.objectNames.get(keys[j]).charAt(0)));
							}
						}
					}
				} else {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				}
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}

	}
	
	protected void searchMapTreeBounds(MapTree current, MapTree parent,
			SearchRequest<BinaryMapDataObject> req, List<MapTree> foundSubtrees) throws IOException {
		int init = 0;
		req.numberOfReadSubtrees++;
		while(true){
			if(req.isCancelled()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			if(init == 0xf){
				init = 0;
				// coordinates are init
				if(current.right < req.left || current.left > req.right || current.top > req.bottom || current.bottom < req.top){
					return;
				} else {
					req.numberOfAcceptedSubtrees++;
				}
			}
			switch (tag) {
			case 0:
				return;
			case MapDataBox.BOTTOM_FIELD_NUMBER :
				current.bottom = codedIS.readSInt32() + parent.bottom;
				init |= 1;
				break;
			case MapDataBox.LEFT_FIELD_NUMBER :
				current.left = codedIS.readSInt32() + parent.left;
				init |= 2;
				break;
			case MapDataBox.RIGHT_FIELD_NUMBER :
				current.right = codedIS.readSInt32() + parent.right;
				init |= 4;
				break;
			case MapDataBox.TOP_FIELD_NUMBER :
				current.top = codedIS.readSInt32() + parent.top;
				init |= 8;
				break;
			case MapDataBox.SHIFTTOMAPDATA_FIELD_NUMBER :
				req.numberOfAcceptedSubtrees ++;
				current.mapDataBlock = readInt() + current.filePointer;
				foundSubtrees.add(current);
				break;
			case MapDataBox.OCEAN_FIELD_NUMBER :
				if(codedIS.readBool()) {
					current.ocean = Boolean.TRUE;
				} else {
					current.ocean = Boolean.FALSE;
				}
				req.publishOceanTile(current.ocean);
				break;
			case MapDataBox.BOXES_FIELD_NUMBER :
				// left, ... already initialized
				MapTree child = new MapTree();
				child.length = readInt();
				child.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(child.length);
				if(current.ocean != null ){
					child.ocean = current.ocean;
				}
				searchMapTreeBounds(child, current, req, foundSubtrees);
				codedIS.popLimit(oldLimit);
				codedIS.seek(child.filePointer + child.length);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private int MASK_TO_READ = ~((1 << SHIFT_COORDINATES) - 1);
	private BinaryMapDataObject readMapDataObject(MapTree tree , SearchRequest<BinaryMapDataObject> req, 
			MapIndex root) throws IOException {
		int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		boolean area = OsmandOdb.MapData.AREACOORDINATES_FIELD_NUMBER == tag;
		if(!area && OsmandOdb.MapData.COORDINATES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheCoordinates.clear();
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		int px = tree.left & MASK_TO_READ;
		int py = tree.top & MASK_TO_READ;
		boolean contains = false;
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		req.numberOfVisitedObjects++;
		while(codedIS.getBytesUntilLimit() > 0){
			int x = (codedIS.readSInt32() << SHIFT_COORDINATES) + px;
			int y = (codedIS.readSInt32() << SHIFT_COORDINATES) + py;
			req.cacheCoordinates.add(x);
			req.cacheCoordinates.add(y);
			px = x;
			py = y;
			if(!contains && req.left <= x && req.right >= x && req.top <= y && req.bottom >= y){
				contains = true;
			}
			if(!contains){
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		if(!contains){
			if(maxX >= req.left && minX <= req.right && minY <= req.bottom && maxY >= req.top){
				contains = true;
			}
			
		}
		codedIS.popLimit(old);
		if(!contains){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		// read 
		
		List<TIntArrayList> innercoordinates = null;
		TIntArrayList additionalTypes = null;
		TIntObjectHashMap<String> stringNames = null;
		long id = 0;
		
		boolean loop = true; 
		while (loop) {
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				loop = false;
				break;
			case OsmandOdb.MapData.POLYGONINNERCOORDINATES_FIELD_NUMBER:
				if (innercoordinates == null) {
					innercoordinates = new ArrayList<TIntArrayList>();
				}
				TIntArrayList polygon = new TIntArrayList();
				innercoordinates.add(polygon);
				px = tree.left & MASK_TO_READ;
				py = tree.top & MASK_TO_READ;
				size = codedIS.readRawVarint32();
				old = codedIS.pushLimit(size);
				while (codedIS.getBytesUntilLimit() > 0) {
					int x = (codedIS.readSInt32() << SHIFT_COORDINATES) + px;
					int y = (codedIS.readSInt32() << SHIFT_COORDINATES) + py;
					polygon.add(x);
					polygon.add(y);
					px = x;
					py = y;
				}
				codedIS.popLimit(old);
				break;
			case OsmandOdb.MapData.ADDITIONALTYPES_FIELD_NUMBER:
				additionalTypes = new TIntArrayList();
				int sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimit(sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					additionalTypes.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(old);
				break;
			case OsmandOdb.MapData.TYPES_FIELD_NUMBER:
				req.cacheTypes.clear();
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimit(sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					req.cacheTypes.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(old);
				boolean accept = true;
				if (req.searchFilter != null) {
					accept = req.searchFilter.accept(req.cacheTypes, root);
				}
				if (!accept) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
				req.numberOfAcceptedObjects++;
				break;
			case OsmandOdb.MapData.ID_FIELD_NUMBER:
				id = codedIS.readSInt64();
				break;
			case OsmandOdb.MapData.STRINGNAMES_FIELD_NUMBER:
				stringNames = new TIntObjectHashMap<String>();
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimit(sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					int stag = codedIS.readRawVarint32();
					int pId = codedIS.readRawVarint32();
					stringNames.put(stag, ((char)pId)+"");
				}
				codedIS.popLimit(old);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
//		if(req.cacheCoordinates.size() > 500 && req.cacheTypes.size() > 0) {
//			TagValuePair p = root.decodeType(req.cacheTypes.get(0));
//			if("admin_level".equals(p.tag)) {
//				log.info("TODO Object is ignored due to performance issues " + p.tag + " "+p.value);
//				return null;
//			}
//		}
		BinaryMapDataObject dataObject = new BinaryMapDataObject();
		dataObject.area = area;
		dataObject.coordinates = req.cacheCoordinates.toArray();
		dataObject.objectNames = stringNames;
		if (innercoordinates == null) {
			dataObject.polygonInnerCoordinates = new int[0][0];
		} else {
			dataObject.polygonInnerCoordinates = new int[innercoordinates.size()][];
			for (int i = 0; i < innercoordinates.size(); i++) {
				dataObject.polygonInnerCoordinates[i] = innercoordinates.get(i).toArray();
			}
		}
		dataObject.types = req.cacheTypes.toArray();
		if (additionalTypes != null) {
			dataObject.additionalTypes = additionalTypes.toArray();
		} else {
			dataObject.additionalTypes = new int[0];
		}
		dataObject.id = id;
		dataObject.area = area;
		dataObject.mapIndex = root;
		return dataObject;
	}
	
	public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) throws IOException {
		if (req.nameQuery == null || req.nameQuery.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (AddressRegion reg : addressIndexes) {
			if(reg.indexNameOffset != -1) {
				codedIS.seek(reg.indexNameOffset);
				int len = readInt();
				int old = codedIS.pushLimit(len);
				addressAdapter.searchAddressDataByName(reg, req, null);
				codedIS.popLimit(old);
			}
		}
		return req.getSearchResults();
	}
	
	public void initCategories(PoiRegion poiIndex) throws IOException {
		poiAdapter.initCategories(poiIndex);
	}
	
	public List<Amenity> searchPoiByName(SearchRequest<Amenity> req) throws IOException {
		if (req.nameQuery == null || req.nameQuery.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			codedIS.seek(poiIndex.filePointer);
			int old = codedIS.pushLimit(poiIndex.length);
			poiAdapter.searchPoiByName(poiIndex, req);
			codedIS.popLimit(old);
		}
		return req.getSearchResults();
	}
	
	public Map<AmenityType, List<String>> searchPoiCategoriesByName(String query, Map<AmenityType, List<String>> map) throws IOException {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException();
		}
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			for (int i = 0; i < poiIndex.categories.size(); i++) {
				String cat = poiIndex.categories.get(i);
				AmenityType catType = poiIndex.categoriesType.get(i);
				if (CollatorStringMatcher.cmatches(collator, cat, query, StringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
					map.put(catType, null);
				} else {
					List<String> subcats = poiIndex.subcategories.get(i);
					for (int j = 0; j < subcats.size(); j++) {
						if (CollatorStringMatcher.cmatches(collator, subcats.get(j), query, StringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
							if (!map.containsKey(catType)) {
								map.put(catType, new ArrayList<String>());
							}
							List<String> list = map.get(catType);
							if (list != null) {
								list.add(subcats.get(j));
							}
						}

					}
				}
			}
		}
		return map;
	}
	
	public List<Amenity> searchPoi(SearchRequest<Amenity> req) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			codedIS.seek(poiIndex.filePointer);
			int old = codedIS.pushLimit(poiIndex.length);
			poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
			codedIS.popLimit(old);
		}
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		log.info("Search poi is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		return req.getSearchResults();
	}
	
	protected List<String> readStringTable() throws IOException{
		List<String> list = new ArrayList<String>();
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return list;
			case OsmandOdb.StringTable.S_FIELD_NUMBER :
				list.add(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	

	protected List<AddressRegion> getAddressIndexes() {
		return addressIndexes;
	}
	
	protected List<PoiRegion> getPoiIndexes() {
		return poiIndexes;
	}

	
	public static SearchRequest<BinaryMapDataObject> buildSearchRequest(int sleft, int sright, int stop, int sbottom, int zoom, SearchFilter searchFilter){
		return buildSearchRequest(sleft, sright, stop, sbottom, zoom, searchFilter, null);
	}
	
	public static SearchRequest<BinaryMapDataObject> buildSearchRequest(int sleft, int sright, int stop, int sbottom, int zoom, SearchFilter searchFilter, 
			ResultMatcher<BinaryMapDataObject> resultMatcher){
		SearchRequest<BinaryMapDataObject> request = new SearchRequest<BinaryMapDataObject>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		request.searchFilter = searchFilter;
		request.resultMatcher = resultMatcher; 
		return request;
	}
	
	public static <T> SearchRequest<T> buildAddressRequest(ResultMatcher<T> resultMatcher){
		SearchRequest<T> request = new SearchRequest<T>();
		request.resultMatcher = resultMatcher;
		return request;
	}
	
	
	
	public static <T> SearchRequest<T> buildAddressByNameRequest(ResultMatcher<T> resultMatcher, String nameRequest){
		SearchRequest<T> request = new SearchRequest<T>();
		request.resultMatcher = resultMatcher;
		request.nameQuery = nameRequest;
		return request;
	}
	
	
	public static SearchRequest<Amenity> buildSearchPoiRequest(int sleft, int sright, int stop, int sbottom, int zoom, 
			SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> matcher){
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		request.poiTypeFilter = poiTypeFilter;
		request.resultMatcher = matcher;
		
		return request;
	}
	
	public static SearchRequest<RouteDataObject> buildSearchRouteRequest(int sleft, int sright, int stop, int sbottom,  
			ResultMatcher<RouteDataObject> matcher){
		SearchRequest<RouteDataObject> request = new SearchRequest<RouteDataObject>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.resultMatcher = matcher;
		
		return request;
	}
	
	
	public static SearchRequest<Amenity> buildSearchPoiRequest(int x, int y, String nameFilter, int sleft, int sright, int stop, int sbottom, ResultMatcher<Amenity> resultMatcher){
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		request.x = x;
		request.y = y;
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.resultMatcher = resultMatcher;
		request.nameQuery = nameFilter;
		return request;
	}
	
	
	public static SearchRequest<TransportStop> buildSearchTransportRequest(int sleft, int sright, int stop, int sbottom, int limit, List<TransportStop> stops){
		SearchRequest<TransportStop> request = new SearchRequest<TransportStop>();
		if (stops != null) {
			request.searchResults = stops;
		}
		request.stringTable = new TIntObjectHashMap<String>();
		request.left = sleft >> (31 - TRANSPORT_STOP_ZOOM);
		request.right = sright >> (31 - TRANSPORT_STOP_ZOOM);
		request.top = stop >> (31 - TRANSPORT_STOP_ZOOM);
		request.bottom = sbottom >> (31 - TRANSPORT_STOP_ZOOM);
		request.limit = limit;
		return request;
	}
	
	public void close() throws IOException{
		if(codedIS != null){
			raf.close();
			codedIS = null;
			mapIndexes.clear();
			addressIndexes.clear();
			transportIndexes.clear();
		}
	}
	
	public static interface SearchFilter {
		
		public boolean accept(TIntArrayList types, MapIndex index);
		
	}
	
	public static interface SearchPoiTypeFilter {
		
		public boolean accept(AmenityType type, String subcategory);
		
	}
	
	public static class SearchRequest<T> {
		private List<T> searchResults = new ArrayList<T>();
		private boolean land = false;
		private boolean ocean = false;
		
		private ResultMatcher<T> resultMatcher;
		
		// 31 zoom tiles
		// common variables
		int x = 0;
		int y = 0;
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		int zoom = 15;
		int limit = -1;
		
		
		String nameQuery = null;

		SearchFilter searchFilter = null;
		
		SearchPoiTypeFilter poiTypeFilter = null;
		
		// internal read information
		TIntObjectHashMap<String> stringTable = null;
		
		// cache information
		TIntArrayList cacheCoordinates = new TIntArrayList();
		TIntArrayList cacheTypes = new TIntArrayList();
		
		
		// TRACE INFO
		int numberOfVisitedObjects = 0;
		int numberOfAcceptedObjects = 0;
		int numberOfReadSubtrees = 0;
		int numberOfAcceptedSubtrees = 0;
		boolean interrupted = false;
		
		protected SearchRequest(){
		}
		
		
		public boolean publish(T obj){
			if(resultMatcher == null || resultMatcher.publish(obj)){
				searchResults.add(obj);
				return true;
			}
			return false;
		}
		
		protected void publishOceanTile(boolean ocean){
			if(ocean) {
				this.ocean = true;
			} else {
				this.land = true;
			}
		}
		
		public List<T> getSearchResults() {
			return searchResults;
		}
		
		public void setInterrupted(boolean interrupted) {
			this.interrupted = interrupted;
		}
		
		public boolean limitExceeded() {
			return limit != -1 && searchResults.size() > limit;
		}
		public boolean isCancelled() {
			if(this.interrupted){
				return interrupted;
			}
			if(resultMatcher != null){
				return resultMatcher.isCancelled();
			}
			return false;
		}
		
		public boolean isOcean() {
			return ocean;
		}
		
		public boolean isLand() {
			return land;
		}
		
		public boolean intersects(int l, int t, int r, int b){
			return r >= left && l <= right && t <= bottom && b >= top;
		}
		
		public boolean contains(int l, int t, int r, int b){
			return r <= right && l >= left && b <= bottom && t >= top;
		}
		
		public int getLeft() {
			return left;
		}
		
		public int getRight() {
			return right;
		}
		
		public int getBottom() {
			return bottom;
		}
		
		public int getTop() {
			return top;
		}
		
		public void clearSearchResults(){
			// recreate whole list to allow GC collect old data 
			searchResults = new ArrayList<T>();
			cacheCoordinates.clear();
			cacheTypes.clear();
			land = false;
			ocean = false;
			numberOfVisitedObjects = 0;
			numberOfAcceptedObjects = 0;
			numberOfReadSubtrees = 0;
			numberOfAcceptedSubtrees = 0;
		}
	}
	
	
	public static class MapIndex extends BinaryIndexPart {
		List<MapRoot> roots = new ArrayList<MapRoot>();
		
		Map<String, Map<String, Integer>> encodingRules = new LinkedHashMap<String, Map<String, Integer>>();
		TIntObjectMap<TagValuePair> decodingRules = new TIntObjectHashMap<TagValuePair>();
		public int nameEncodingType = 0;
		public int refEncodingType = -1;
		public int coastlineEncodingType = -1;
		public int coastlineBrokenEncodingType = -1;
		public int landEncodingType = -1;
		public int onewayAttribute = -1;
		public int onewayReverseAttribute = -1;
		public TIntHashSet positiveLayers = new TIntHashSet(2);
		public TIntHashSet negativeLayers = new TIntHashSet(2);
		
		public Integer getRule(String t, String v){
			Map<String, Integer> m = encodingRules.get(t);
			if(m != null){
				return m.get(v);
			}
			return null;
		}
		
		public List<MapRoot> getRoots() {
			return roots;
		}
		
		public TagValuePair decodeType(int type){
			return decodingRules.get(type);
		}

		public void finishInitializingTags() {
			int free = decodingRules.size() * 2 + 1;
			coastlineBrokenEncodingType = free++;
			initMapEncodingRule(0, coastlineBrokenEncodingType, "natural", "coastline_broken");
			if(landEncodingType == -1){
				landEncodingType = free++;
				initMapEncodingRule(0, landEncodingType, "natural", "land");
			}
		}
		
		public void initMapEncodingRule(int type, int id, String tag, String val) {
			if(!encodingRules.containsKey(tag)){
				encodingRules.put(tag, new LinkedHashMap<String, Integer>());
			}
			encodingRules.get(tag).put(val, id);
			if(!decodingRules.containsKey(id)){
				decodingRules.put(id, new TagValuePair(tag, val, type));
			}
			
			if("name".equals(tag)){
				nameEncodingType = id;
			} else if("natural".equals(tag) && "coastline".equals(val)){
				coastlineEncodingType = id;
			} else if("natural".equals(tag) && "land".equals(val)){
				landEncodingType = id;
			} else if("oneway".equals(tag) && "yes".equals(val)){
				onewayAttribute = id;
			} else if("oneway".equals(tag) && "-1".equals(val)){
				onewayReverseAttribute = id;
			} else if("ref".equals(tag)){
				refEncodingType = id;
			} else if("tunnel".equals(tag)){
				negativeLayers.add(id);
			} else if("bridge".equals(tag)){
				positiveLayers.add(id);
			} else if("layer".equals(tag)){
				if(val != null && !val.equals("0") && val.length() > 0) {
					if(val.startsWith("-")) {
						negativeLayers.add(id);
					} else {
						positiveLayers.add(id);
					}
				}
			}
		}

		public boolean isBaseMap(){
			return name != null && name.toLowerCase().contains(BASEMAP_NAME);
		}
	}
	
	public static class TagValuePair {
		public String tag;
		public String value;
		public int additionalAttribute;
		
		
		public TagValuePair(String tag, String value, int additionalAttribute) {
			super();
			this.tag = tag;
			this.value = value;
			this.additionalAttribute = additionalAttribute;
		}
		
		public boolean isAdditional(){
			return additionalAttribute % 2 == 1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + additionalAttribute;
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}
		
		public String toSimpleString(){
			if(value == null){
				return tag;
			}
			return tag+"-"+value;
		}
		
		@Override
		public String toString() {
			return "TagValuePair : " + tag + " - " + value;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TagValuePair other = (TagValuePair) obj;
			if (additionalAttribute != other.additionalAttribute)
				return false;
			if (tag == null) {
				if (other.tag != null)
					return false;
			} else if (!tag.equals(other.tag))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}
	
	
	public static class MapRoot extends MapTree {
		int minZoom = 0;
		int maxZoom = 0;
		
		
		public int getMinZoom() {
			return minZoom;
		}
		public int getMaxZoom() {
			return maxZoom;
		}
		
		private List<MapTree> trees = null;
	}
	
	private static class MapTree {
		int filePointer = 0;
		int length = 0;
		
		long mapDataBlock = 0;
		Boolean ocean = null;
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		public int getLeft() {
			return left;
		}
		public int getRight() {
			return right;
		}
		public int getTop() {
			return top;
		}
		public int getBottom() {
			return bottom;
		}
		
		public int getLength() {
			return length;
		}
		public int getFilePointer() {
			return filePointer;
		}
		
		@Override
		public String toString(){
			return "Top Lat " + ((float) MapUtils.get31LatitudeY(top)) + " lon " + ((float) MapUtils.get31LongitudeX(left))
					+ " Bottom lat " + ((float) MapUtils.get31LatitudeY(bottom)) + " lon " + ((float) MapUtils.get31LongitudeX(right));
		}
		
	}

	
	private static boolean testMapSearch = true;
	private static boolean testAddressSearch = false;
	private static boolean testPoiSearch = false;
	private static boolean testTransportSearch = false;
	private static int sleft = MapUtils.get31TileNumberX(6.3);
	private static int sright = MapUtils.get31TileNumberX(6.5);
	private static int stop = MapUtils.get31TileNumberY(49.9);
	private static int sbottom = MapUtils.get31TileNumberY(49.7);
	private static int szoom = 15;
	
	private static void println(String s){
		System.out.println(s);
	}
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("/home/victor/projects/OsmAnd/data/osm-gen/Luxembourg.obf"), "r");
		
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		long time = System.currentTimeMillis();

		if (testMapSearch) {
			testMapSearch(reader);
		}
		if(testAddressSearch) {
			testAddressSearchByName(reader);
			testAddressSearch(reader);
		}
		if(testTransportSearch) {
			testTransportSearch(reader);
		}

		if (testPoiSearch) {
			PoiRegion poiRegion = reader.getPoiIndexes().get(0);
			testPoiSearch(reader, poiRegion);
			testPoiSearchByName(reader);
		}

		println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())); //$NON-NLS-1$
		println("Time " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}

	private static void testPoiSearchByName(BinaryMapIndexReader reader) throws IOException {
		println("Searching by name...");
		int tileZ = 31 - 14;
		SearchRequest<Amenity> req = buildSearchPoiRequest(sleft/2+sright/2, stop/2+sbottom/2, "kol",  
				sleft - 1<<tileZ, sleft + 1<<tileZ, stop - 1<<tileZ,  stop + 1<<tileZ, null);
		reader.searchPoiByName(req);
		for (Amenity a : req.getSearchResults()) {
			println(a.getType() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
		}
	}

	private static void testPoiSearch(BinaryMapIndexReader reader, PoiRegion poiRegion) throws IOException {
		println(poiRegion.leftLongitude + " " + poiRegion.rightLongitude + " " + poiRegion.bottomLatitude + " "
				+ poiRegion.topLatitude);
		for (int i = 0; i < poiRegion.categories.size(); i++) {
			println(poiRegion.categories.get(i));
			println(" " + poiRegion.subcategories.get(i));
		}

		SearchRequest<Amenity> req = buildSearchPoiRequest(sleft, sright, stop, sbottom, -1, new SearchPoiTypeFilter() {
			@Override
			public boolean accept(AmenityType type, String subcategory) {
//					return type == AmenityType.TRANSPORTATION && "fuel".equals(subcategory);
				return true;
			}
		}, null);
		List<Amenity> results = reader.searchPoi(req);
		for (Amenity a : results) {
			println(a.getType() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
		}
	}

	private static void testTransportSearch(BinaryMapIndexReader reader) throws IOException {
		// test transport
		for (TransportIndex i : reader.transportIndexes) {
			println("Transport bounds : " + i.left + " " + i.right + " " + i.top + " " + i.bottom);
		}
		{
			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 15, null))) {
				println(s.getName());
				TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());
				for (net.osmand.data.TransportRoute route : routes.valueCollection()) {
					println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
							+ route.getAvgBothDistance());
				}
			}
		}
		{
			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 16, null))) {
				println(s.getName());
				TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());
				for (net.osmand.data.TransportRoute  route : routes.valueCollection()) {
					println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
							+ route.getAvgBothDistance());
				}
			}
		}
	}
	
	private static void updateFrequence(Map<String, Integer> street , String key){
		if(!street.containsKey(key)){
			street.put(key, 1);
		} else {
			street.put(key, street.get(key) + 1);
		}
		
	}

	int readIndexedStringTable(Collator instance, String query, String prefix, TIntArrayList list, int charMatches) throws IOException {
		String key = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return charMatches;
			case OsmandOdb.IndexedStringTable.KEY_FIELD_NUMBER :
				key = codedIS.readString();
				if(prefix.length() > 0){
					key = prefix + key;
				}
				// check query is part of key (the best matching)
				if(CollatorStringMatcher.cmatches(instance, key, query, StringMatcherMode.CHECK_ONLY_STARTS_WITH)){
					if(query.length() >= charMatches){
						if(query.length() > charMatches){
							charMatches = query.length();
							list.clear();
						}
					} else {
						key = null;
					}
					// check key is part of query
				} else if (CollatorStringMatcher.cmatches(instance, query, key, StringMatcherMode.CHECK_ONLY_STARTS_WITH)) {
					if (key.length() >= charMatches) {
						if (key.length() > charMatches) {
							charMatches = key.length();
							list.clear();
						}
					} else {
						key = null;
					}
				} else {
					key = null;
				}
				break;
			case OsmandOdb.IndexedStringTable.VAL_FIELD_NUMBER :
				int val = readInt();
				if (key != null) {
					list.add(val);
				}
				break;
			case OsmandOdb.IndexedStringTable.SUBTABLES_FIELD_NUMBER :
				int len = codedIS.readRawVarint32();
				int oldLim = codedIS.pushLimit(len);
				if (key != null) {
					charMatches = readIndexedStringTable(instance, query, key, list, charMatches);
				} else {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				}
				codedIS.popLimit(oldLim);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private static void testAddressSearchByName(BinaryMapIndexReader reader) throws IOException {
		SearchRequest<MapObject> req = buildAddressByNameRequest(new ResultMatcher<MapObject>() {
			@Override
			public boolean publish(MapObject object) {
				if(object instanceof Street) {
					System.out.println(object + " " + ((Street) object).getCity());
				} else {
					System.out.println(object);
				}
				return false;
			}
			@Override
			public boolean isCancelled() {
				return false;
			}
		}, "lux");
		reader.searchAddressDataByName(req);
	}
	
	private static void testAddressSearch(BinaryMapIndexReader reader) throws IOException {
		// test address index search
		String reg = reader.getRegionNames().get(0);
		final Map<String, Integer> streetFreq = new LinkedHashMap<String, Integer>();
		List<City> cs = reader.getCities(reg, null, BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
		for(City c : cs){
			int buildings = 0;
			reader.preloadStreets(c, null);
			for(Street s : c.getStreets()){
				updateFrequence(streetFreq, s.getName());
				reader.preloadBuildings(s, buildAddressRequest((ResultMatcher<Building>) null));
				buildings += s.getBuildings().size();
			}
			println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings + " " + c.getEnName());
		}
//		int[] count = new int[1];
		List<City> villages = reader.getCities(reg, buildAddressRequest((ResultMatcher<City>) null), BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
		for(City v : villages) {
			reader.preloadStreets(v,  null);
			for(Street s : v.getStreets()){
				updateFrequence(streetFreq, s.getName());
			}
		}
		System.out.println("Villages " + villages.size());
		
		List<String> sorted = new ArrayList<String>(streetFreq.keySet());
		Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return - streetFreq.get(o1) + streetFreq.get(o2);
			}
		});
		System.out.println(streetFreq.size());
		for(String s : sorted) {
			System.out.println(s + "   " + streetFreq.get(s));
			if(streetFreq.get(s) < 10){
				break;
			}
		}
		
	}

	private static void testMapSearch(BinaryMapIndexReader reader) throws IOException {
		println(reader.mapIndexes.get(0).encodingRules + "");
		println("SEARCH " + sleft + " " + sright + " " + stop + " " + sbottom);

		reader.searchMapIndex(buildSearchRequest(sleft, sright, stop, sbottom, szoom, null, new ResultMatcher<BinaryMapDataObject>() {
			
			@Override
			public boolean publish(BinaryMapDataObject obj) {
				
				StringBuilder b = new StringBuilder();
				b.append(obj.area? "Area" : (obj.getPointsLength() > 1? "Way" : "Point"));
				int[] types = obj.getTypes();
				b.append(" types [");
				for(int j = 0; j<types.length; j++){
					if(j > 0) {
						b.append(", ");
					}
					TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
					if(pair == null) {
						throw new NullPointerException("Type " + types[j] + "was not found");
					}
					b.append(pair.toSimpleString()+"("+types[j]+")");
				}
				b.append("]");
				if(obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0){
					b.append(" add_types [");
					for(int j = 0; j<obj.getAdditionalTypes().length; j++){
						if(j > 0) {
							b.append(", ");
						}
						TagValuePair pair = obj.getMapIndex().decodeType(obj.getAdditionalTypes()[j]);
						if(pair == null) {
							throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
						}
						b.append(pair.toSimpleString()+"("+obj.getAdditionalTypes()[j]+")");
						
					}
					b.append("]");
				}
				TIntObjectHashMap<String> names = obj.getObjectNames();
				if(names != null && !names.isEmpty()) {
					b.append(" Names [");
					int[] keys = names.keys();
					for(int j = 0; j<keys.length; j++){
						if(j > 0) {
							b.append(", ");
						}
						TagValuePair pair = obj.getMapIndex().decodeType(keys[j]);
						if(pair == null) {
							throw new NullPointerException("Type " + keys[j] + "was not found");
						}
						b.append(pair.toSimpleString()+"("+keys[j]+")");
						b.append(" - ").append(names.get(keys[j]));
					}
					b.append("]");
				}
				
				b.append(" id ").append((obj.getId() >> 1));
				b.append(" lat/lon : ");
				for(int i=0; i<obj.getPointsLength(); i++) {
					float x = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
					float y = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
					b.append(x).append(" / ").append(y).append(" , ");
				}
				println(b.toString());
				return false;
			}
			
			@Override
			public boolean isCancelled() {
				return false;
			}
		}));
	}
	
	public void initRouteRegionsIfNeeded(SearchRequest<RouteDataObject> req) throws IOException {
		routeAdapter.initRouteTypesIfNeeded(req);
	}
	
	public List<RouteSubregion> searchRouteIndexTree(SearchRequest<RouteDataObject> req, List<RouteSubregion> list) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		if(routeAdapter != null){
			initRouteRegionsIfNeeded(req);
			return routeAdapter.searchRouteRegionTree(req, list, new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>());
		}
		return Collections.emptyList();
	}

	public void loadRouteIndexData(List<RouteSubregion> toLoad, ResultMatcher<RouteDataObject> matcher) throws IOException {
		if(routeAdapter != null){
			routeAdapter.loadRouteRegionData(toLoad, matcher);
		}
	}
	
	public List<RouteDataObject> loadRouteIndexData(RouteSubregion rs) throws IOException {
		if(routeAdapter != null){
			return routeAdapter.loadRouteRegionData(rs);
		}
		return Collections.emptyList();
	}
	
}