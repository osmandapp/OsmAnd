package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.data.TransportStop;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.MapRenderingTypes.MapRulType;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	public final static int TRANSPORT_STOP_ZOOM = 24;
	private final static Log log = LogUtil.getLog(BinaryMapIndexReader.class);
	
	private final RandomAccessFile raf;
	private int version;
	private List<MapIndex> mapIndexes = new ArrayList<MapIndex>();
	private List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	private List<TransportIndex> transportIndexes = new ArrayList<TransportIndex>();
	private List<BinaryIndexPart> indexes = new ArrayList<BinaryIndexPart>();
	
	protected CodedInputStreamRAF codedIS;
	
	private final BinaryMapTransportReaderAdapter transportAdapter;
	private final BinaryMapAddressReaderAdapter addressAdapter;
	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
		
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
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
				oldLimit = codedIS.pushLimit(region.length);
				addressAdapter.readAddressIndex(region);
				codedIS.popLimit(oldLimit);
				codedIS.seek(region.filePointer + region.length);
				if(region.name != null){
					addressIndexes.add(region);
					indexes.add(region);
				}
				break;
			case OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER:
				TransportIndex ind = new TransportIndex();
				ind.length = readInt();
				ind.filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimit(ind.length);
				transportAdapter.readTransportIndex(ind);
				codedIS.popLimit(oldLimit);
				codedIS.seek(ind.filePointer + ind.length);
				transportIndexes.add(ind);
				indexes.add(ind);
				break;
			case OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER :
				int cversion = codedIS.readUInt32();
				initCorrectly = cversion == version;
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	public List<BinaryIndexPart> getIndexes() {
		return indexes;
	}
	
	public boolean containsMapData(){
		return mapIndexes.size() > 0;
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
	public net.osmand.data.TransportRoute getTransportRoute(int filePointer) throws IOException {
		TransportIndex ind = getTransportIndex(filePointer);
		if(ind == null){
			return null;
		}
		return transportAdapter.getTransportRoute(filePointer, ind);
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
			for (int i = offset; i < req.searchResults.size(); i++) {
				TransportStop st = req.searchResults.get(i);
				if (st.getName().length() != 0) {
					st.setName(transportAdapter.getStringFromStringTable(index.stringTable, st.getName().charAt(0)));
				}
				if (st.getEnName().length() != 0) {
					st.setEnName(transportAdapter.getStringFromStringTable(index.stringTable, st.getEnName().charAt(0)));
				} else {
					st.setEnName(Junidecode.unidecode(st.getName()));
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
	
	private AddressRegion getRegionByName(String name){
		for(AddressRegion r : addressIndexes){
			if(r.name.equals(name)){
				return r;
			}
		}
		throw new IllegalArgumentException(name);
	}
	
	public List<PostCode> getPostcodes(String region) throws IOException {
		List<PostCode> postcodes = new ArrayList<PostCode>();
		AddressRegion r = getRegionByName(region);
		if(r.postcodesOffset != -1){
			codedIS.seek(r.postcodesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readPostcodes(postcodes);
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
	
	public List<City> getCities(String region) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.citiesOffset != -1){
			codedIS.seek(r.citiesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readCities(cities, null, false);
			codedIS.popLimit(old);
		}
		return cities;
	}
	
	public List<City> getVillages(String region) throws IOException {
		return getVillages(region, null, false);
	}
	public List<City> getVillages(String region, String nameContains, boolean useEn) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.villagesOffset != -1){
			codedIS.seek(r.villagesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			addressAdapter.readCities(cities, nameContains, useEn);
			codedIS.popLimit(old);
		}
		return cities;
	}

	
	public void preloadStreets(City c) throws IOException {
		checkAddressIndex(c.getFileOffset());
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readCity(c, c.getFileOffset(), true);
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
	
	
	public void preloadStreets(PostCode p) throws IOException {
		checkAddressIndex(p.getFileOffset());
		
		codedIS.seek(p.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readPostcode(p, p.getFileOffset(), true, null);
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
	
	public void preloadBuildings(Street s) throws IOException {
		checkAddressIndex(s.getFileOffset());
		codedIS.seek(s.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		addressAdapter.readStreet(s, true, 0, 0, null);
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
				MapRoot mapRoot = readMapLevel();
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


	private MapRoot readMapLevel() throws IOException {
		MapRoot root = new MapRoot();
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
				MapTree r = new MapTree();
				// left, ... already initialized 
				r.length = readInt();
				r.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(r.length);
				readMapTreeBounds(r, root.left, root.right, root.top, root.bottom);
				root.trees.add(r);
				codedIS.popLimit(oldLimit);
				codedIS.seek(r.filePointer + r.length);
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
			if(req.isInterrupted()){
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
	
	private int MASK_TO_READ = ~((1 << BinaryMapIndexWriter.SHIFT_COORDINATES) - 1);
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
			int x = (codedIS.readSInt32() << BinaryMapIndexWriter.SHIFT_COORDINATES) + px;
			int y = (codedIS.readSInt32() << BinaryMapIndexWriter.SHIFT_COORDINATES) + py;
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

	
	public static SearchRequest<BinaryMapDataObject> buildSearchRequest(int sleft, int sright, int stop, int sbottom, int zoom){
		SearchRequest<BinaryMapDataObject> request = new SearchRequest<BinaryMapDataObject>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		return request;
	}
	
	
	public static SearchRequest<TransportStop> buildSearchTransportRequest(int sleft, int sright, int stop, int sbottom, int limit, List<TransportStop> stops){
		SearchRequest<TransportStop> request = new SearchRequest<TransportStop>();
		if (stops != null) {
			request.searchResults = stops;
		}
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
	
	public static class SearchRequest<T> {
		// 31 zoom tiles
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		int zoom = 15;
		int limit = -1;
		List<T> searchResults = new ArrayList<T>();
		TIntArrayList cacheCoordinates = new TIntArrayList();
		TIntArrayList cacheTypes = new TIntArrayList();
		SearchFilter searchFilter = null;
		
		// TRACE INFO
		int numberOfVisitedObjects = 0;
		int numberOfAcceptedObjects = 0;
		int numberOfReadSubtrees = 0;
		int numberOfAcceptedSubtrees = 0;
		boolean interrupted = false;
		
		protected SearchRequest(){
		}
		
		public SearchFilter getSearchFilter() {
			return searchFilter;
		}
		public void setSearchFilter(SearchFilter searchFilter) {
			this.searchFilter = searchFilter;
		}
		
		public List<T> getSearchResults() {
			return searchResults;
		}
		
		public void setInterrupted(boolean interrupted) {
			this.interrupted = interrupted;
		}
		
		public boolean isInterrupted() {
			return interrupted;
		}
		
		public void clearSearchResults(){
			// recreate whole list to allow GC collect old data 
			searchResults = new ArrayList<T>();
		}
	}
	
	
	public static class MapIndex extends BinaryIndexPart {
		List<MapRoot> roots = new ArrayList<MapRoot>();
		Map<String, Map<String, Integer>> encodingRules = new LinkedHashMap<String, Map<String, Integer>>();
		Map<Integer, TagValuePair> decodingRules = new LinkedHashMap<Integer, TagValuePair>();
		
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
		
		List<MapTree> trees = new ArrayList<MapTree>();
	}
	
	private static class MapTree {
		int filePointer = 0;
		int length = 0;
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		long baseId = 0;
		
		List<String> stringTable = null;
		List<MapTree> subTrees = null;
		
	}

	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Minsk.obf"), "r"); //$NON-NLS-1$ //$NON-NLS-2$
//		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Belarus_4.obf"), "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		System.out.println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		long time = System.currentTimeMillis();
		
		System.out.println(reader.mapIndexes.get(0).encodingRules);
		
		// test search
//		int sleft = MapUtils.get31TileNumberX(27.596);
//		int sright = MapUtils.get31TileNumberX(27.599);
//		int stop = MapUtils.get31TileNumberY(53.921);
//		int sbottom = MapUtils.get31TileNumberY(53.919);
//		System.out.println("SEARCH " + sleft + " " + sright + " " + stop + " " + sbottom);
//
//		for (BinaryMapDataObject obj : reader.searchMapIndex(buildSearchRequest(sleft, sright, stop, sbottom, 8))) {
//			if (obj.getName() != null) {
//				System.out.println(" " + obj.getName());
//			}
//		}
		
		// test address index search
//		String reg = reader.getRegionNames().get(0);
//		List<City> cs = reader.getCities(reg);
//		for(City c : cs){
//			reader.preloadStreets(c);
//			int buildings = 0;
//			for(Street s : c.getStreets()){
//				reader.preloadBuildings(s);
//				buildings += s.getBuildings().size();
//			}
//			System.out.println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings);
//		}
//		List<PostCode> postcodes = reader.getPostcodes(reg);
//		for(PostCode c : postcodes){
//			reader.preloadStreets(c);
////			System.out.println(c.getName());
//		}
//		List<City> villages = reader.getVillages(reg, "коче", false);
//		System.out.println("Villages " + villages.size());
		
		// test transport
//		for(TransportIndex i : reader.transportIndexes){
//			System.out.println(i.left + " " + i.right + " " + i.top + " " + i.bottom);
//			System.out.println(i.stringTable.cacheOfStrings);
//			System.out.println(i.stringTable.offsets);
//			System.out.println(i.stringTable.window);
//		}
//		{
//			int sleft = MapUtils.get31TileNumberX(27.573);
//			int sright = MapUtils.get31TileNumberX(27.581);
//			int stop = MapUtils.get31TileNumberY(53.912);
//			int sbottom = MapUtils.get31TileNumberY(53.908);
//			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 15, null))) {
//				System.out.println(s.getName());
//				for (int i : s.getReferencesToRoutes()) {
//					TransportRoute route = reader.getTransportRoute(i);
//					System.out.println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
//							+ route.getAvgBothDistance());
//				}
//			}
//		}
//		{
//			int sleft = MapUtils.get31TileNumberX(27.473);
//			int sright = MapUtils.get31TileNumberX(27.681);
//			int stop = MapUtils.get31TileNumberY(53.912);
//			int sbottom = MapUtils.get31TileNumberY(53.708);
//			for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, 16, null))) {
//				System.out.println(s.getName());
//				for (int i : s.getReferencesToRoutes()) {
//					TransportRoute route = reader.getTransportRoute(i);
//					System.out.println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
//							+ route.getAvgBothDistance());
//				}
//			}
//		}
		
		System.out.println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())); //$NON-NLS-1$
		System.out.println("Time " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}
	
}