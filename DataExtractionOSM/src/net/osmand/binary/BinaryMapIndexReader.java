package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.CollatorStringMatcher;
import net.osmand.LogUtil;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapRenderingTypes.MapRulType;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	public final static int TRANSPORT_STOP_ZOOM = 24;
	public static final int SHIFT_COORDINATES = 5;
	private final static Log log = LogUtil.getLog(BinaryMapIndexReader.class);
	
	private final RandomAccessFile raf;
	private int version;
	private List<MapIndex> mapIndexes = new ArrayList<MapIndex>();
	private List<PoiRegion> poiIndexes = new ArrayList<PoiRegion>();
	private List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	private List<TransportIndex> transportIndexes = new ArrayList<TransportIndex>();
	private List<BinaryIndexPart> indexes = new ArrayList<BinaryIndexPart>();
	
	protected CodedInputStreamRAF codedIS;
	
	private final BinaryMapTransportReaderAdapter transportAdapter;
	private final BinaryMapPoiReaderAdapter poiAdapter;
	private final BinaryMapAddressReaderAdapter addressAdapter;
	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this(raf, false);
	}
	
	public BinaryMapIndexReader(final RandomAccessFile raf, boolean readOnlyMapData) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024 * 5);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
		if(!readOnlyMapData){
			transportAdapter = new BinaryMapTransportReaderAdapter(this);
			addressAdapter = new BinaryMapAddressReaderAdapter(this);
			poiAdapter = new BinaryMapPoiReaderAdapter(this);
		} else {
			transportAdapter = null;
			addressAdapter = null;
			poiAdapter = null;
		}
		init();
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
			case OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER:
				MapIndex mapIndex = new MapIndex();
				mapIndex.length = readInt();
				mapIndex.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(mapIndex.length);
				readMapIndex(mapIndex);
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
			case OsmandOdb.OsmAndStructure.POIINDEX_FIELD_NUMBER:
				PoiRegion poiInd = new PoiRegion();
				poiInd.length = readInt();
				poiInd.filePointer = codedIS.getTotalBytesRead();
				if (poiAdapter != null) {
					oldLimit = codedIS.pushLimit(poiInd.length);
					poiAdapter.readPoiIndex(poiInd);
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
	
	public boolean containsMapData(){
		return mapIndexes.size() > 0;
	}
	
	public boolean containsPoiData(){
		return poiIndexes.size() > 0;
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
	
	public List<PostCode> getPostcodes(String region, SearchRequest<MapObject> resultMatcher, StringMatcher nameMatcher) throws IOException {
		List<PostCode> postcodes = new ArrayList<PostCode>();
		AddressRegion r = getRegionByName(region);
		if(r.postcodesOffset != -1){
			codedIS.seek(r.postcodesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readPostcodes(postcodes, resultMatcher, nameMatcher);
			codedIS.popLimit(old);
		}
		return postcodes;
	}
	
	public PostCode getPostcodeByName(String region, String name) throws IOException {
		AddressRegion r = getRegionByName(region);
		if (r.postcodesOffset != -1) {
			codedIS.seek(r.postcodesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			PostCode p = addressAdapter.findPostcode(name);
			if (p != null) {
				return p;
			}
			codedIS.popLimit(old);
		}
		return null;
	}
	
	public List<City> getCities(String region, SearchRequest<MapObject> resultMatcher) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.citiesOffset != -1){
			codedIS.seek(r.citiesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readCities(cities, resultMatcher, null, false);
			codedIS.popLimit(old);
		}
		return cities;
	}
	
	public List<City> getVillages(String region, SearchRequest<MapObject> resultMatcher, StringMatcher nameMatcher, boolean useEn) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.villagesOffset != -1){
			codedIS.seek(r.villagesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readCities(cities, resultMatcher, nameMatcher, useEn);
			codedIS.popLimit(old);
		}
		return cities;
	}

	
	public void preloadStreets(City c, SearchRequest<Street> resultMatcher) throws IOException {
		checkAddressIndex(c.getFileOffset());
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readCity(c, c.getFileOffset(), true, resultMatcher, null, false);
		codedIS.popLimit(old);
	}
	
	public List<Street> findIntersectedStreets(City c, Street s, List<Street> streets) throws IOException {
		checkAddressIndex(c.getFileOffset());
		addressAdapter.findIntersectedStreets(c, s, null, streets);
		return streets;
	}
	
	public LatLon findStreetIntersection(City c, Street s, Street s2) throws IOException {
		checkAddressIndex(c.getFileOffset());
		return addressAdapter.findIntersectedStreets(c, s, s2, null);
	}
	
	
	public void preloadStreets(PostCode p, SearchRequest<Street> resultMatcher) throws IOException {
		checkAddressIndex(p.getFileOffset());
		
		codedIS.seek(p.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readPostcode(p, p.getFileOffset(), null, true, null);
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
		addressAdapter.readStreet(s, resultMatcher, true, 0, 0, null);
		codedIS.popLimit(old);
	}
	
	/**
	 * Map public methods 
	 */

	private void readMapIndex(MapIndex index) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(index.encodingRules.isEmpty()){
					// init encoding rules by default
					Map<String, MapRulType> map = MapRenderingTypes.getDefault().getEncodingRuleTypes();
					for(String tags : map.keySet()){
						MapRulType rt = map.get(tags);
						if(rt.getType(null) != 0){
							initMapEncodingRule(index, rt.getType(null), rt.getSubType(null), tags, null);
						}
						for (String value : rt.getValuesSet()) {
							initMapEncodingRule(index, rt.getType(value), rt.getSubType(value), tags, value);
						}
					}
				}
				return;
			case OsmandOdb.OsmAndMapIndex.NAME_FIELD_NUMBER :
				index.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndMapIndex.RULES_FIELD_NUMBER :
				int len = codedIS.readInt32();
				int oldLimit = codedIS.pushLimit(len);
				readMapEncodingRule(index);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER :
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimit(length);
				MapRoot mapRoot = readMapLevel(new MapRoot());
				mapRoot.length = length;
				mapRoot.filePointer = filePointer;
				index.getRoots().add(mapRoot);
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void initMapEncodingRule(MapIndex index, int type, int subtype, String tag, String val) {
		int ind = ((subtype << 5) | type);
		if(!index.encodingRules.containsKey(tag)){
			index.encodingRules.put(tag, new LinkedHashMap<String, Integer>());
		}
		index.encodingRules.get(tag).put(val, ind);
		if(!index.decodingRules.containsKey(ind)){
			index.decodingRules.put(ind, new TagValuePair(tag, val));
		}
	}
	
	private void readMapEncodingRule(MapIndex index) throws IOException {
		int subtype = 0;
		int type = 0;
		String tags = null;
		String val = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				initMapEncodingRule(index, type, subtype, tags, val);
				return;
			case OsmandOdb.MapEncodingRule.VALUE_FIELD_NUMBER :
				val = codedIS.readString().intern();
				break;
			case OsmandOdb.MapEncodingRule.TAG_FIELD_NUMBER :
				tags = codedIS.readString().intern();
				break;
			case OsmandOdb.MapEncodingRule.SUBTYPE_FIELD_NUMBER :
				subtype = codedIS.readUInt32();
				break;
			case OsmandOdb.MapEncodingRule.TYPE_FIELD_NUMBER :
				type = codedIS.readUInt32();
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
			case OsmandOdb.MapRootLevel.BOTTOM_FIELD_NUMBER :
				root.bottom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.LEFT_FIELD_NUMBER :
				root.left = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.RIGHT_FIELD_NUMBER :
				root.right = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.TOP_FIELD_NUMBER :
				root.top = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.MAXZOOM_FIELD_NUMBER :
				root.maxZoom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.MINZOOM_FIELD_NUMBER :
				root.minZoom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.ROOT_FIELD_NUMBER :
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
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}
	
	private void readMapTreeBounds(MapTree tree, int aleft, int aright, int atop, int abottom) throws IOException {
		int init = 0;
		while(true){
			if(init == 0xf){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER :
				tree.bottom = codedIS.readSInt32() + abottom;
				init |= 1;
				break;
			case OsmandOdb.MapTree.LEFT_FIELD_NUMBER :
				tree.left = codedIS.readSInt32() + aleft;
				init |= 2;
				break;
			case OsmandOdb.MapTree.RIGHT_FIELD_NUMBER :
				tree.right = codedIS.readSInt32() + aright;
				init |= 4;
				break;
			case OsmandOdb.MapTree.TOP_FIELD_NUMBER :
				tree.top = codedIS.readSInt32() + atop;
				init |= 8;
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
		for (MapIndex mapIndex : mapIndexes) {
			for (MapRoot index : mapIndex.getRoots()) {
				if (index.minZoom <= req.zoom && index.maxZoom >= req.zoom) {
					if (index.right < req.left || index.left > req.right || index.top > req.bottom || index.bottom < req.top) {
						continue;
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
						searchMapTreeBounds(index.left, index.right, index.top, index.bottom, req, mapIndex);
						codedIS.popLimit(oldLimit);
					}
				}
			}
		}
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
	}
	
	
	
	
	protected void searchMapTreeBounds(int pleft, int pright, int ptop, int pbottom,
			SearchRequest<BinaryMapDataObject> req, MapIndex root) throws IOException {
		int init = 0;
		int lastIndexResult = -1;
		int cright = 0;
		int cleft = 0;
		int ctop = 0;
		int cbottom = 0;
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
				if(cright < req.left || cleft > req.right || ctop > req.bottom || cbottom < req.top){
					return;
				} else {
					req.numberOfAcceptedSubtrees++;
				}
			}
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER :
				cbottom = codedIS.readSInt32() + pbottom;
				init |= 1;
				break;
			case OsmandOdb.MapTree.LEFT_FIELD_NUMBER :
				cleft = codedIS.readSInt32() + pleft;
				init |= 2;
				break;
			case OsmandOdb.MapTree.RIGHT_FIELD_NUMBER :
				cright = codedIS.readSInt32() + pright;
				init |= 4;
				break;
			case OsmandOdb.MapTree.TOP_FIELD_NUMBER :
				ctop = codedIS.readSInt32() + ptop;
				init |= 8;
				break;
			case OsmandOdb.MapTree.LEAFS_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				if(lastIndexResult == -1){
					lastIndexResult = req.searchResults.size();
				}
				BinaryMapDataObject mapObject = readMapDataObject(cleft, cright, ctop, cbottom, req, root);
				if(mapObject != null){
					req.searchResults.add(mapObject);
					
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.MapTree.SUBTREES_FIELD_NUMBER :
				// left, ... already initialized 
				length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimit(length);
				searchMapTreeBounds(cleft, cright, ctop, cbottom, req, root);
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				if(lastIndexResult >= 0){
					throw new IllegalStateException();
				}
				break;
			case OsmandOdb.MapTree.BASEID_FIELD_NUMBER :
			case OsmandOdb.MapTree.OLDBASEID_FIELD_NUMBER :
				long baseId = codedIS.readUInt64();
				if (lastIndexResult != -1) {
					for (int i = lastIndexResult; i < req.searchResults.size(); i++) {
						BinaryMapDataObject rs = req.searchResults.get(i);
						rs.id += baseId;
						if (rs.restrictions != null) {
							for (int j = 0; j < rs.restrictions.length; j++) {
								rs.restrictions[j] += baseId;
							}
						}
					}
				}
				break;
			case OsmandOdb.MapTree.STRINGTABLE_FIELD_NUMBER :
			case OsmandOdb.MapTree.OLDSTRINGTABLE_FIELD_NUMBER :
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				List<String> stringTable = readStringTable();
				codedIS.popLimit(oldLimit);

				if (lastIndexResult != -1) {
					for (int i = lastIndexResult; i < req.searchResults.size(); i++) {
						BinaryMapDataObject rs = req.searchResults.get(i);
						if (rs.stringId != -1) {
							rs.name = stringTable.get(rs.stringId);
						}
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private int MASK_TO_READ = ~((1 << SHIFT_COORDINATES) - 1);
	private BinaryMapDataObject readMapDataObject(int left, int right, int top, int bottom, SearchRequest<BinaryMapDataObject> req, 
			MapIndex root) throws IOException {
		int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheCoordinates.clear();
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		int px = left & MASK_TO_READ;
		int py = top & MASK_TO_READ;
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
		
		// READ types
		tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.MapData.TYPES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheTypes.clear();
		int sizeL = codedIS.readRawVarint32();
		byte[] types = codedIS.readRawBytes(sizeL);
		for(int i=0; i<sizeL/2; i++){
			req.cacheTypes.add(Algoritms.parseSmallIntFromBytes(types, i*2));
		}
		
		boolean accept = true;
		if (req.searchFilter != null) {
			accept = req.searchFilter.accept(req.cacheTypes, root);
		}
		
		
		if(!accept){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		req.numberOfAcceptedObjects++;
		
		BinaryMapDataObject dataObject = new BinaryMapDataObject();		
		dataObject.coordinates = req.cacheCoordinates.toArray();
		dataObject.types = req.cacheTypes.toArray();
		dataObject.mapIndex = root;
		
		while(true){
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return dataObject;
			case OsmandOdb.MapData.RESTRICTIONS_FIELD_NUMBER :
				sizeL = codedIS.readRawVarint32();
				TLongArrayList list = new TLongArrayList();
				old = codedIS.pushLimit(sizeL);
				while(codedIS.getBytesUntilLimit() > 0){
					list.add(codedIS.readSInt64());
				}
				codedIS.popLimit(old);
				dataObject.restrictions = list.toArray();
				break;
			case OsmandOdb.MapData.HIGHWAYMETA_FIELD_NUMBER :
				dataObject.highwayAttributes = codedIS.readInt32();
				break;
			case OsmandOdb.MapData.ID_FIELD_NUMBER :
				dataObject.id = codedIS.readSInt64();
				break;
			case OsmandOdb.MapData.STRINGID_FIELD_NUMBER :
				dataObject.stringId = codedIS.readUInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}
	
	public List<Amenity> searchPoiByName(SearchRequest<Amenity> req) throws IOException {
		if (req.nameQuery == null || req.nameQuery.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (PoiRegion poiIndex : poiIndexes) {
			codedIS.seek(poiIndex.filePointer);
			int old = codedIS.pushLimit(poiIndex.length);
			poiAdapter.searchPoiByName(poiIndex, req);
			codedIS.popLimit(old);
		}
		return req.getSearchResults();
	}
	
	public Map<AmenityType, List<String>> searchPoiCategoriesByName(String query, Map<AmenityType, List<String>> map) {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException();
		}
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
		for (PoiRegion poiIndex : poiIndexes) {
			for(int i= 0; i< poiIndex.categories.size(); i++){
				String cat = poiIndex.categories.get(i);
				AmenityType catType = poiIndex.categoriesType.get(i);
				if(CollatorStringMatcher.cmatches(collator, cat, query, StringMatcherMode.CHECK_STARTS_FROM_SPACE)){
					map.put(catType, null);
				} else {
					List<String> subcats = poiIndex.subcategories.get(i);
					for(int j=0; j< subcats.size(); j++){
						if(CollatorStringMatcher.cmatches(collator, subcats.get(j), query, StringMatcherMode.CHECK_STARTS_FROM_SPACE)){
							if(!map.containsKey(catType)){
								map.put(catType, new ArrayList<String>());
							}
							List<String> list = map.get(catType);
							if(list != null){
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
			codedIS.seek(poiIndex.filePointer);
			int old = codedIS.pushLimit(poiIndex.length);
			poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
			codedIS.popLimit(old);
		}
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		log.info("Search poi is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		return req.getSearchResults();
	}
	
	private List<String> readStringTable() throws IOException{
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
		SearchRequest<BinaryMapDataObject> request = new SearchRequest<BinaryMapDataObject>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		request.searchFilter = searchFilter;
		return request;
	}
	
	public static <T> SearchRequest<T> buildAddressRequest(ResultMatcher<T> resultMatcher){
		SearchRequest<T> request = new SearchRequest<T>();
		request.resultMatcher = resultMatcher;
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
	
	public static SearchRequest<Amenity> buildSearchPoiRequest(int x, int y, String nameFilter, ResultMatcher<Amenity> resultMatcher){
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		request.x = x;
		request.y = y;
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
		
		
		protected boolean publish(T obj){
			if(resultMatcher == null || resultMatcher.publish(obj)){
				searchResults.add(obj);
				return true;
			}
			return false;
		}
		
		public List<T> getSearchResults() {
			return searchResults;
		}
		
		public void setInterrupted(boolean interrupted) {
			this.interrupted = interrupted;
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
		
		public void clearSearchResults(){
			// recreate whole list to allow GC collect old data 
			searchResults = new ArrayList<T>();
			cacheCoordinates.clear();
			cacheTypes.clear();
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
		
		public List<MapRoot> getRoots() {
			return roots;
		}
		
		public TagValuePair decodeType(int type, int subtype){
			return decodingRules.get(((subtype << 5) | type));
		}
		
		public TagValuePair decodeType(int wholeType){
			if((wholeType & 3) != MapRenderingTypes.POINT_TYPE ){
				wholeType = (wholeType >> 2) & MapRenderingTypes.MASK_10;
			} else {
				wholeType >>= 2;
			}
			return decodingRules.get(wholeType);
		}
		
	}
	
	public static class TagValuePair {
		public String tag;
		public String value;
		public int additionalAttribute;
		public TagValuePair(String tag, String value) {
			super();
			this.tag = tag;
			this.value = value;
		}
		
		public TagValuePair(String tag, String value, int additionalAttribute) {
			super();
			this.tag = tag;
			this.value = value;
			this.additionalAttribute = additionalAttribute;
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
	
	
	public static class MapRoot extends BinaryIndexPart {
		int minZoom = 0;
		int maxZoom = 0;
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		public int getMinZoom() {
			return minZoom;
		}
		
		public int getMaxZoom() {
			return maxZoom;
		}
		
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
		
		private List<MapTree> trees = null;
	}
	
	private static class MapTree {
		int filePointer = 0;
		int length = 0;
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
	}

	
	private static boolean testMapSearch = false;
	private static boolean testAddressSearch = false;
	private static boolean testPoiSearch = true;
	private static boolean testTransportSearch = false;
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("/home/victor/projects/OsmAnd/data/osm-gen/POI/Ru-mow.poi.obf"), "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		System.out.println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		long time = System.currentTimeMillis();

		if (testMapSearch) {
			testMapSearch(reader);
		}
		if(testAddressSearch) {
			testAddressSearch(reader);
		}
		if(testTransportSearch) {
			testTransportSearch(reader);
		}

		Locale.setDefault(new Locale("RU"));
		if (testPoiSearch) {
			PoiRegion poiRegion = reader.getPoiIndexes().get(0);
			System.out.println(poiRegion.leftLongitude + " " + poiRegion.rightLongitude + " " + poiRegion.bottomLatitude + " "
					+ poiRegion.topLatitude);
			for (int i = 0; i < poiRegion.categories.size(); i++) {
				System.out.println(poiRegion.categories.get(i));
				System.out.println(" " + poiRegion.subcategories.get(i));
			}

			int sleft = MapUtils.get31TileNumberX(37.5);
			int sright = MapUtils.get31TileNumberX(37.9);
			int stop = MapUtils.get31TileNumberY(55.814);
			int sbottom = MapUtils.get31TileNumberY(55.81);
			SearchRequest<Amenity> req = buildSearchPoiRequest(sleft, sright, stop, sbottom, -1, new SearchPoiTypeFilter() {
				@Override
				public boolean accept(AmenityType type, String subcategory) {
					return type == AmenityType.TRANSPORTATION && "fuel".equals(subcategory);
				}
			}, null);
			List<Amenity> results = reader.searchPoi(req);
			for (Amenity a : results) {
				System.out.println(a.getType() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
			}

			System.out.println("Searching by name...");
			req = buildSearchPoiRequest(sleft, sright, "kolie", null);
			reader.searchPoiByName(req);
			for (Amenity a : req.getSearchResults()) {
				System.out.println(a.getType() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
			}
		}

		System.out.println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())); //$NON-NLS-1$
		System.out.println("Time " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}

	private static void testTransportSearch(BinaryMapIndexReader reader) throws IOException {
		// test transport
		for (TransportIndex i : reader.transportIndexes) {
			System.out.println(i.left + " " + i.right + " " + i.top + " " + i.bottom);
			System.out.println(i.stringTable.offsets);
		}
		{
			int sleft = MapUtils.get31TileNumberX(27.573);
			int sright = MapUtils.get31TileNumberX(27.581);
			int stop = MapUtils.get31TileNumberY(53.912);
			int sbottom = MapUtils.get31TileNumberY(53.908);
			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 15, null))) {
				System.out.println(s.getName());
				TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());
				for (net.osmand.data.TransportRoute  route : routes.values()) {
					System.out.println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
							+ route.getAvgBothDistance());
				}
			}
		}
		{
			int sleft = MapUtils.get31TileNumberX(27.473);
			int sright = MapUtils.get31TileNumberX(27.681);
			int stop = MapUtils.get31TileNumberY(53.912);
			int sbottom = MapUtils.get31TileNumberY(53.708);
			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 16, null))) {
				System.out.println(s.getName());
				TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());
				for (net.osmand.data.TransportRoute  route : routes.values()) {
					System.out.println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
							+ route.getAvgBothDistance());
				}
			}
		}
	}

	private static void testAddressSearch(BinaryMapIndexReader reader) throws IOException {
		// test address index search
		String reg = reader.getRegionNames().get(0);
		List<City> cs = reader.getCities(reg, null);
		for(City c : cs){
			int buildings = 0;
			reader.preloadStreets(c, null);
			for(Street s : c.getStreets()){
				reader.preloadBuildings(s, buildAddressRequest((ResultMatcher<Building>) null));
				buildings += s.getBuildings().size();
			}
			System.out.println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings + " " + c.getEnName());
		}
		List<PostCode> postcodes = reader.getPostcodes(reg, buildAddressRequest((ResultMatcher<MapObject>) null), null);
		for(PostCode c : postcodes){
			reader.preloadStreets(c, buildAddressRequest((ResultMatcher<Street>) null));
			System.out.println(c.getName());
		}
		List<City> villages = reader.getVillages(reg, buildAddressRequest((ResultMatcher<MapObject>) null), new StringMatcher() {
			
			@Override
			public boolean matches(String name) {
				return false;
			}
		}, true);
		System.out.println("Villages " + villages.size());
	}

	private static void testMapSearch(BinaryMapIndexReader reader) throws IOException {
		System.out.println(reader.mapIndexes.get(0).encodingRules);
		int sleft = MapUtils.get31TileNumberX(27.596);
		int sright = MapUtils.get31TileNumberX(27.599);
		int stop = MapUtils.get31TileNumberY(53.921);
		int sbottom = MapUtils.get31TileNumberY(53.919);
		System.out.println("SEARCH " + sleft + " " + sright + " " + stop + " " + sbottom);

		for (BinaryMapDataObject obj : reader.searchMapIndex(buildSearchRequest(sleft, sright, stop, sbottom, 8, null))) {
			if (obj.getName() != null) {
				System.out.println(" " + obj.getName());
			}
		}
	}
	
}