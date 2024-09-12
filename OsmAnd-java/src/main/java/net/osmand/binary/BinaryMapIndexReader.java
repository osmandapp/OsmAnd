package net.osmand.binary;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CitiesBlock;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapDataBox;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapEncodingRule;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapRootLevel;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.Way;
import net.osmand.router.HHRouteDataStructure.HHRouteRegionPointsCtx;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class BinaryMapIndexReader {

	public final static int DETAILED_MAP_MIN_ZOOM = 9;
	public final static int TRANSPORT_STOP_ZOOM = 24;
	public static final int SHIFT_COORDINATES = 5;
	public static final int LABEL_ZOOM_ENCODE = 31 - SHIFT_COORDINATES;
	private final static Log log = PlatformUtil.getLog(BinaryMapIndexReader.class);
	public static boolean READ_STATS = false;
	public static final SearchPoiTypeFilter ACCEPT_ALL_POI_TYPE_FILTER = new SearchPoiTypeFilter() {
		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public boolean accept(PoiCategory type, String subcategory) {
			return true;
		}
	};
	
	
	private final RandomAccessFile raf;
	protected final File file;
	/*private*/ int version;
	/*private*/ long dateCreated;
	/*private*/ OsmAndOwner owner;
	// keep them immutable inside
	/*private*/ boolean basemap = false;
	/*private*/ List<MapIndex> mapIndexes = new ArrayList<MapIndex>();
	/*private*/ List<PoiRegion> poiIndexes = new ArrayList<PoiRegion>();
	/*private*/ List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	/*private*/ List<TransportIndex> transportIndexes = new ArrayList<TransportIndex>();
	/*private*/ List<RouteRegion> routingIndexes = new ArrayList<RouteRegion>();
	/*private*/ List<HHRouteRegion> hhIndexes = new ArrayList<HHRouteRegion>();
	/*private*/ List<BinaryIndexPart> indexes = new ArrayList<BinaryIndexPart>();
	TLongObjectHashMap<IncompleteTransportRoute> incompleteTransportRoutes = null;
	
	protected CodedInputStream codedIS;

	private final BinaryMapTransportReaderAdapter transportAdapter;
	private final BinaryMapPoiReaderAdapter poiAdapter;
	private final BinaryMapAddressReaderAdapter addressAdapter;
	private final BinaryMapRouteReaderAdapter routeAdapter;
	private final BinaryHHRouteReaderAdapter hhAdapter;

	private static final String BASEMAP_NAME = "basemap";


	public BinaryMapIndexReader(final RandomAccessFile raf, File file) throws IOException {
		this.raf = raf;
		this.file = file;
		codedIS = CodedInputStream.newInstance(raf);
		codedIS.setSizeLimit(CodedInputStream.MAX_DEFAULT_SIZE_LIMIT);
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		hhAdapter = new BinaryHHRouteReaderAdapter(this);
		init();
	}

	public BinaryMapIndexReader(final RandomAccessFile raf, File file, boolean init) throws IOException {
		this.raf = raf;
		this.file = file;
		codedIS = CodedInputStream.newInstance(raf);
		codedIS.setSizeLimit(CodedInputStream.MAX_DEFAULT_SIZE_LIMIT);
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		hhAdapter = new BinaryHHRouteReaderAdapter(this);
		if (init) {
			init();
		}
	}

	public BinaryMapIndexReader(final RandomAccessFile raf, BinaryMapIndexReader referenceToSameFile) throws IOException {
		this.raf = raf;
		this.file = referenceToSameFile.file;
		codedIS = CodedInputStream.newInstance(raf);
		codedIS.setSizeLimit(CodedInputStream.MAX_DEFAULT_SIZE_LIMIT);
		version = referenceToSameFile.version;
		dateCreated = referenceToSameFile.dateCreated;
		transportAdapter = new BinaryMapTransportReaderAdapter(this);
		addressAdapter = new BinaryMapAddressReaderAdapter(this);
		poiAdapter = new BinaryMapPoiReaderAdapter(this);
		routeAdapter = new BinaryMapRouteReaderAdapter(this);
		hhAdapter = new BinaryHHRouteReaderAdapter(this);
		mapIndexes = new ArrayList<BinaryMapIndexReader.MapIndex>(referenceToSameFile.mapIndexes);
		poiIndexes = new ArrayList<PoiRegion>(referenceToSameFile.poiIndexes);
		addressIndexes = new ArrayList<AddressRegion>(referenceToSameFile.addressIndexes);
		transportIndexes = new ArrayList<TransportIndex>(referenceToSameFile.transportIndexes);
		routingIndexes = new ArrayList<RouteRegion>(referenceToSameFile.routingIndexes);
		hhIndexes = new ArrayList<HHRouteRegion>(referenceToSameFile.hhIndexes);
		indexes = new ArrayList<BinaryIndexPart>(referenceToSameFile.indexes);
		basemap = referenceToSameFile.basemap;
		calculateCenterPointForRegions();
	}


	public long getDateCreated() {
		return dateCreated;
	}

	public OsmAndOwner getOwner() {
		return owner;
	}

	private void init() throws IOException {
		boolean initCorrectly = false;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if (!initCorrectly) {
					//throw new IOException("Corrupted file. It should be ended as it starts with version"); //$NON-NLS-1$
					throw new IOException("Corrupt file, it should have ended as it starts with version: " + file.getAbsolutePath()); //$NON-NLS-1$
				}
				return;
			case OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER :
				version = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndStructure.DATECREATED_FIELD_NUMBER :
				dateCreated = codedIS.readInt64();
				break;
			case OsmandOdb.OsmAndStructure.OWNER_FIELD_NUMBER:
				long len = codedIS.readInt32();
				long oldLimit = codedIS.pushLimitLong((long) len);
				owner = new OsmAndOwner();
				readOsmAndOwner();
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER:
				MapIndex mapIndex = new MapIndex();
				mapIndex.length = readInt();
				mapIndex.filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimitLong((long) mapIndex.length);
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
					oldLimit = codedIS.pushLimitLong((long) region.length);
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
					oldLimit = codedIS.pushLimitLong((long) ind.length);
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
					oldLimit = codedIS.pushLimitLong((long) routeReg.length);
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
					oldLimit = codedIS.pushLimitLong((long) poiInd.length);
					poiAdapter.readPoiIndex(poiInd, false);
					codedIS.popLimit(oldLimit);
					poiIndexes.add(poiInd);
					indexes.add(poiInd);
				}
				codedIS.seek(poiInd.filePointer + poiInd.length);
				break;
			case OsmandOdb.OsmAndStructure.HHROUTINGINDEX_FIELD_NUMBER:
				HHRouteRegion hhreg = new HHRouteRegion();
				hhreg.length = readInt();
				hhreg.filePointer = codedIS.getTotalBytesRead();
				if (hhAdapter != null) {
					oldLimit = codedIS.pushLimitLong((long) hhreg.length);
					hhAdapter.readHHIndex(hhreg, false);
					codedIS.popLimit(oldLimit);
					indexes.add(hhreg);
					hhIndexes.add(hhreg);
				}
				codedIS.seek(hhreg.filePointer + hhreg.length);
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

	private void calculateCenterPointForRegions() {
		for (AddressRegion reg : addressIndexes) {
			for (MapIndex map : mapIndexes) {
				if (Algorithms.objectEquals(reg.name, map.name)) {
					if (map.getRoots().size() > 0) {
						reg.calculatedCenter = map.getCenterLatLon();
						break;
					}
				}
			}
			if (reg.calculatedCenter == null) {
				for (RouteRegion map : routingIndexes) {
					if (Algorithms.objectEquals(reg.name, map.name)) {
						reg.calculatedCenter = new LatLon(map.getTopLatitude() / 2 + map.getBottomLatitude() / 2,
								map.getLeftLongitude() / 2 + map.getRightLongitude() / 2);
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
	
	public List<HHRouteRegion> getHHRoutingIndexes() {
		return hhIndexes;
	}

	public boolean isBasemap() {
		return basemap;
	}

	public boolean containsMapData() {
		return mapIndexes.size() > 0;
	}

	public boolean containsPoiData() {
		return poiIndexes.size() > 0;
	}

	public boolean containsRouteData() {
		return routingIndexes.size() > 0;
	}
	
	public boolean containsActualRouteData(int x31, int y31, Set<String> checkedRegions) throws IOException {
		int zoomToLoad = 14;
		int x = x31 >> zoomToLoad;
		int y = y31 >> zoomToLoad;
		SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(x << zoomToLoad,
				(x + 1) << zoomToLoad, y << zoomToLoad, (y + 1) << zoomToLoad, null);
		for (RouteRegion reg : getRoutingIndexes()) {
			if (checkedRegions != null) {
				if (checkedRegions.contains(reg.getName())) {
					continue;
				}
				checkedRegions.add(reg.getName());
			}
			List<RouteSubregion> res = searchRouteIndexTree(request, reg.getSubregions());
			if (!res.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public boolean containsRouteData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
		for (RouteRegion ri : routingIndexes) {
			List<RouteSubregion> sr = ri.getSubregions();
			for (RouteSubregion r : sr) {
				if (right31x >= r.left && left31x <= r.right && r.top <= bottom31y && r.bottom >= top31y) {
					return true;
				}
			}
		}
		return false;
	}


	
	public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
		for (PoiRegion index : poiIndexes) {
			if (right31x >= index.left31 && left31x <= index.right31 && index.top31 <= bottom31y && index.bottom31 >= top31y) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsAddressData(int left31x, int top31y, int right31x, int bottom31y) {
		for (AddressRegion index : addressIndexes) {
			if (right31x >= index.left31 && left31x <= index.right31 && index.top31 <= bottom31y && index.bottom31 >= top31y) {
				return true;
			}
		}
		return false;
	}

	public boolean containsMapData(int tile31x, int tile31y, int zoom) {
		for (MapIndex mapIndex : mapIndexes) {
			for (MapRoot root : mapIndex.getRoots()) {
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (tile31x >= root.left && tile31x <= root.right && root.top <= tile31y && root.bottom >= tile31y) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsMapData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
		for (MapIndex mapIndex : mapIndexes) {
			for (MapRoot root : mapIndex.getRoots()) {
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (right31x >= root.left && left31x <= root.right && root.top <= bottom31y && root.bottom >= top31y) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsAddressData() {
		return addressIndexes.size() > 0;
	}

	public boolean hasTransportData() {
		return transportIndexes.size() > 0;
	}


	public RandomAccessFile getRaf() {
		return raf;
	}

	public File getFile() {
		return file;
	}

	public String getCountryName() {
		List<String> rg = getRegionNames();
		if(rg.size() > 0) {
			return rg.get(0).split("_")[0];			
		}
		return "";
	}
	
	public <T extends NetworkDBPoint> TLongObjectHashMap<T> initHHPoints(HHRouteRegion reg, short mapId, Class<T> cl) throws IOException {
		return hhAdapter.initRegionAndLoadPoints(reg, mapId, cl);
	}

	public <T extends NetworkDBPoint> int loadNetworkSegmentPoint(HHRoutingContext<T>  ctx, HHRouteRegionPointsCtx<T> reg,
			T point, boolean reverse) throws IOException {
		return hhAdapter.loadNetworkSegmentPoint(ctx, reg, point, reverse);
	}
	
	public String getRegionName() {
		List<String> rg = getRegionNames();
		if (rg.size() == 0) {
			rg.add(file.getName());
		}
		String ls = rg.get(0);
		if (ls.lastIndexOf('_') != -1) {
			if (ls.matches("([a-zA-Z-]+_)+([0-9]+_){2}[0-9]+\\.obf")) {
				Pattern osmDiffDateEnding = Pattern.compile("_([0-9]+_){2}[0-9]+\\.obf");
				Matcher m = osmDiffDateEnding.matcher(ls);
				if (m.find()) {
					ls = ls.substring(0, m.start());
					if (ls.lastIndexOf('_') != -1) {
						return ls.substring(0, ls.lastIndexOf('_')).replace('_', ' ');
					} else {
						return ls;
					}
					
				}
			} else {
				if (ls.contains(".")) {
					ls = ls.substring(0, ls.indexOf("."));
				}
				if (ls.endsWith("_" + IndexConstants.BINARY_MAP_VERSION)) {
					ls = ls.substring(0, ls.length() - ("_" + IndexConstants.BINARY_MAP_VERSION).length());
				}
				if (ls.lastIndexOf('_') != -1) {
					ls = ls.substring(0, ls.lastIndexOf('_')).replace('_', ' ');
				}
				return ls;
			}

		}
		return ls;
	}


	public int readByte() throws IOException {
		byte b = codedIS.readRawByte();
		if (b < 0) {
			return b + 256;
		} else {
			return b;
		}
	}

	public final long readInt() throws IOException {
		long l = readByte();
		boolean _8byte = l > 0x7f;
		if (_8byte) {
			l = l & 0x7f;
		}
		l = (l << 8) + readByte();
		l = (l << 8) + readByte();
		l = (l << 8) + readByte();
		if (_8byte) {
			l = (l << 8) + readByte();
			l = (l << 8) + readByte();
			l = (l << 8) + readByte();
			l = (l << 8) + readByte();
			
		}
		return l;
	}


	public int getVersion() {
		return version;
	}


	protected void skipUnknownField(int tag) throws IOException {
		int wireType = WireFormat.getTagWireType(tag);
		if (wireType == WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED) {
			long length = readInt();
			codedIS.skipRawBytes(length);
		} else {
			codedIS.skipField(tag);
		}
	}


	public TLongObjectHashMap<TransportRoute> getTransportRoutes(long[] filePointers) throws IOException {
		TLongObjectHashMap<TransportRoute> result = new TLongObjectHashMap<TransportRoute>();
		loadTransportRoutes(filePointers, result);
		return result;
	}
	/**
	 * Transport public methods
	 */
	public void loadTransportRoutes(long[] filePointers, TLongObjectHashMap<TransportRoute> result) throws IOException {
		Map<TransportIndex, TLongArrayList> groupPoints = new HashMap<TransportIndex, TLongArrayList>();
		for (long filePointer : filePointers) {
			TransportIndex ind = getTransportIndex(filePointer);
			if (ind != null) {
				if (!groupPoints.containsKey(ind)) {
					groupPoints.put(ind, new TLongArrayList());
				}
				groupPoints.get(ind).add(filePointer);
			}
		}
		Iterator<Entry<TransportIndex, TLongArrayList>> it = groupPoints.entrySet().iterator();
		while (it.hasNext()) {
			Entry<TransportIndex, TLongArrayList> e = it.next();
			TransportIndex ind = e.getKey();
			TLongArrayList pointers = e.getValue();
			pointers.sort();
			TIntObjectHashMap<String> stringTable = new TIntObjectHashMap<String>();
			List<TransportRoute> finishInit = new ArrayList<TransportRoute>();
			
			for (int i = 0; i < pointers.size(); i++) {
				long filePointer = pointers.get(i);
				TransportRoute transportRoute = transportAdapter.getTransportRoute(filePointer, stringTable, false);
				result.put(filePointer, transportRoute);
				finishInit.add(transportRoute);	
			}
			TIntObjectHashMap<String> indexedStringTable = transportAdapter.initializeStringTable(ind, stringTable);
			for (TransportRoute transportRoute : finishInit) {
				transportAdapter.initializeNames(false, transportRoute, indexedStringTable);
			}
			
		}
	}

	

	public boolean transportStopBelongsTo(TransportStop s) {
		return getTransportIndex(s.getFileOffset()) != null;
	}

	public List<TransportIndex> getTransportIndexes() {
		return transportIndexes;
	}

	private TransportIndex getTransportIndex(long filePointer) {
		TransportIndex ind = null;
		for (TransportIndex i : transportIndexes) {
			if (i.filePointer <= filePointer && (filePointer - i.filePointer) < i.length) {
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
			if (index.right >= x && index.left <= x && index.top <= y && index.bottom >= y) {
				return true;
			}
		}
		return false;
	}

	public boolean containTransportData(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		double leftX = MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, leftLongitude);
		double topY = MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, topLatitude);
		double rightX = MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, rightLongitude);
		double bottomY = MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, bottomLatitude);
		for (TransportIndex index : transportIndexes) {
			if (index.right >= leftX && index.left <= rightX && index.top <= bottomY && index.bottom >= topY) {
				return true;
			}
		}
		return false;
	}
	
	public List<TransportStop> searchTransportIndex(TransportIndex index, SearchRequest<TransportStop> req) throws IOException {
		if (index.stopsFileLength == 0 || index.right < req.left || index.left > req.right || index.top > req.bottom
				|| index.bottom < req.top) {
			return req.getSearchResults();
		}
		codedIS.seek(index.stopsFileOffset);
		long oldLimit = codedIS.pushLimitLong((long) index.stopsFileLength);
		int offset = req.searchResults.size();
		TIntObjectHashMap<String> stringTable = new TIntObjectHashMap<String>();
		transportAdapter.searchTransportTreeBounds(0, 0, 0, 0, req, stringTable);
		codedIS.popLimit(oldLimit);
		TIntObjectHashMap<String> indexedStringTable = transportAdapter.initializeStringTable(index, stringTable);
		for (int i = offset; i < req.searchResults.size(); i++) {
			TransportStop st = req.searchResults.get(i);
			transportAdapter.initializeNames(indexedStringTable, st);
		}
		return req.getSearchResults();
	}
	
	public List<TransportStop> searchTransportIndex(SearchRequest<TransportStop> req) throws IOException {
		for (TransportIndex index : transportIndexes) {
			searchTransportIndex(index, req);
		}
		if (req.numberOfVisitedObjects > 0 && req.log) {
			log.debug("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			log.debug("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return req.getSearchResults();
	}

	/**
	 * Address public methods
	 */
	public List<String> getRegionNames() {
		List<String> names = new ArrayList<String>();
		for (AddressRegion r : addressIndexes) {
			names.add(r.name);
		}
		return names;
	}

	public LatLon getRegionCenter() {
		for (AddressRegion r : addressIndexes) {
			if (r.calculatedCenter != null)
				return r.calculatedCenter;
		}
		return null;
	}

	public List<City> getCities(SearchRequest<City> resultMatcher,
	                            int cityType) throws IOException {
		return getCities(resultMatcher, null, null, cityType);
	}


	public List<City> getCities(SearchRequest<City> resultMatcher, StringMatcher matcher, String lang, int cityType)
			throws IOException {
		List<City> cities = new ArrayList<City>();
		for (AddressRegion r : addressIndexes) {
			for (CitiesBlock block : r.cities) {
				if (block.type == cityType) {
					codedIS.seek(block.filePointer);
					long old = codedIS.pushLimitLong((long) block.length);
					addressAdapter.readCities(cities, resultMatcher, matcher, r.attributeTagsTable);
					codedIS.popLimit(old);
				}
			}
		}
		return cities;
	}
	
	public List<City> getCities(AddressRegion region, SearchRequest<City> resultMatcher,  
			int cityType) throws IOException {
		return getCities(region, resultMatcher, null, cityType);
	}
	
	public List<City> getCities(AddressRegion region, SearchRequest<City> resultMatcher, StringMatcher matcher,  
			int cityType) throws IOException {
		List<City> cities = new ArrayList<City>();
		for (CitiesBlock block : region.cities) {
			if (block.type == cityType) {
				codedIS.seek(block.filePointer);
				long old = codedIS.pushLimitLong((long) block.length);
				addressAdapter.readCities(cities, resultMatcher, matcher, region.attributeTagsTable);
				codedIS.popLimit(old);
			}
		}
		return cities;
	}

	public int preloadStreets(City c, SearchRequest<Street> resultMatcher) throws IOException {
		AddressRegion reg;
		try {
			reg = checkAddressIndex(c.getFileOffset());
		} catch (IllegalArgumentException e) {
			throw new IOException(e.getMessage() + " while reading " + c + " (id: " + c.getId() + ")");
		}
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		long old = codedIS.pushLimitLong((long) size);
		addressAdapter.readCityStreets(resultMatcher, c, reg.attributeTagsTable);
		codedIS.popLimit(old);
		return size;
	}

	private AddressRegion checkAddressIndex(long offset) {
		for (AddressRegion r : addressIndexes) {
			if (offset >= r.filePointer && offset <= (r.length + r.filePointer)) {
				return r;
			}
		}
		
		throw new IllegalArgumentException("Illegal offset " + offset); //$NON-NLS-1$
	}

	public void preloadBuildings(Street s, SearchRequest<Building> resultMatcher) throws IOException {
		AddressRegion reg = checkAddressIndex(s.getFileOffset());
		codedIS.seek(s.getFileOffset());
		long size = codedIS.readRawVarint32();
		long old = codedIS.pushLimitLong((long) size);
		City city = s.getCity();
		addressAdapter.readStreet(s, resultMatcher, true, 0, 0, city != null && city.isPostcode() ? city.getName() : null,
				reg.attributeTagsTable);
		codedIS.popLimit(old);
	}


	/**
	 * Map public methods
	 */

	private void readMapIndex(MapIndex index, boolean onlyInitEncodingRules) throws IOException {
		int defaultId = 1;
		long oldLimit;
		long encodingRulesSize = 0;
		while (true) {
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
					if (encodingRulesSize == 0) {
						encodingRulesSize = codedIS.getTotalBytesRead();
					}
					int len = codedIS.readInt32();
					oldLimit = codedIS.pushLimitLong((long) len);
					readMapEncodingRule(index, defaultId++);
					codedIS.popLimit(oldLimit);
					index.encodingRulesSizeBytes = (int) (codedIS.getTotalBytesRead() - encodingRulesSize);
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER :
				long length = readInt();
				long filePointer = codedIS.getTotalBytesRead();
				if (!onlyInitEncodingRules) {
					oldLimit = codedIS.pushLimitLong((long) length);
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
		while (true) {
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
		while (true) {
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
				long length = readInt();
				long filePointer = codedIS.getTotalBytesRead();
				if (root.trees != null) {
					MapTree r = new MapTree();
					// left, ... already initialized
					r.length = length;
					r.filePointer = filePointer;
					long oldLimit = codedIS.pushLimitLong((long) r.length);
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
		while (true) {
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
		return searchMapIndex(req, null);
	}
	
	public List<BinaryMapDataObject> searchMapIndex(SearchRequest<BinaryMapDataObject> req, MapIndex filterMapIndex) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		List<MapTree> foundSubtrees = new ArrayList<MapTree>();
		for (MapIndex mapIndex : mapIndexes) {
			if(filterMapIndex != null && mapIndex != filterMapIndex) {
				continue;
			}
			// lazy initializing rules
			if (mapIndex.encodingRules.isEmpty()) {
				codedIS.seek(mapIndex.filePointer);
				long oldLimit = codedIS.pushLimitLong((long) mapIndex.length);
				readMapIndex(mapIndex, true);
				codedIS.popLimit(oldLimit);
			}
			for (MapRoot index : mapIndex.getRoots()) {
				if (index.minZoom <= req.zoom && index.maxZoom >= req.zoom) {
					if (index.right < req.left || index.left > req.right || index.top > req.bottom || index.bottom < req.top) {
						continue;
					}



					// lazy initializing trees
					if (index.trees == null) {
						index.trees = new ArrayList<MapTree>();
						codedIS.seek(index.filePointer);
						long oldLimit = codedIS.pushLimitLong((long) index.length);
						readMapLevel(index);
						codedIS.popLimit(oldLimit);
					}

					for (MapTree tree : index.trees) {
						if (tree.right < req.left || tree.left > req.right || tree.top > req.bottom || tree.bottom < req.top) {
							continue;
						}
						codedIS.seek(tree.filePointer);
						long oldLimit = codedIS.pushLimitLong((long) tree.length);
						searchMapTreeBounds(tree, index, req, foundSubtrees);
						codedIS.popLimit(oldLimit);
					}

					Collections.sort(foundSubtrees, new Comparator<MapTree>() {
						@Override
						public int compare(MapTree o1, MapTree o2) {
							return o1.mapDataBlock < o2.mapDataBlock ? -1 : (o1.mapDataBlock == o2.mapDataBlock ? 0 : 1);
						}
					});
					for (MapTree tree : foundSubtrees) {
						if (!req.isCancelled()) {
							codedIS.seek(tree.mapDataBlock);
							int length = codedIS.readRawVarint32();
							long oldLimit = codedIS.pushLimitLong((long) length);
							readMapDataBlocks(req, tree, mapIndex);
							codedIS.popLimit(oldLimit);
						}
					}
					foundSubtrees.clear();
				}

			}
		}
		if (req.numberOfVisitedObjects > 0 && req.log) {
			log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return req.getSearchResults();
	}

	

	protected void readMapDataBlocks(SearchRequest<BinaryMapDataObject> req, MapTree tree, MapIndex root) throws IOException {
		List<BinaryMapDataObject> tempResults = null;
		long baseId = 0;
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
				if(READ_STATS) {
					req.stat.addBlockHeader(MapDataBlock.BASEID_FIELD_NUMBER, 0);
				}
				break;
			case MapDataBlock.DATAOBJECTS_FIELD_NUMBER:
				int length = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) length);
				if(READ_STATS) {
					req.stat.lastObjectSize += length;
					req.stat.addBlockHeader(MapDataBlock.DATAOBJECTS_FIELD_NUMBER, length);
				}
				BinaryMapDataObject mapObject = readMapDataObject(tree, req, root);
				if (mapObject != null) {
					mapObject.setId(mapObject.getId() + baseId);
					if (READ_STATS) {
						req.publish(mapObject);
					}
					if (tempResults == null) {
						tempResults = new ArrayList<BinaryMapDataObject>();
					}
					tempResults.add(mapObject);
				}
				codedIS.popLimit(oldLimit);
				break;
			case MapDataBlock.STRINGTABLE_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				if(READ_STATS) {
					req.stat.addBlockHeader(MapDataBlock.STRINGTABLE_FIELD_NUMBER, length);
					req.stat.lastBlockStringTableSize += length;
				}
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
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			if (init == 0xf) {
				init = 0;
				// coordinates are init
				if (current.right < req.left || current.left > req.right || current.top > req.bottom || current.bottom < req.top) {
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
				long oldLimit = codedIS.pushLimitLong((long) child.length);
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
		if (!area && OsmandOdb.MapData.COORDINATES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheCoordinates.clear();
		int size = codedIS.readRawVarint32();
		if (READ_STATS) {
			req.stat.lastObjectCoordinates += size;
			req.stat.addTagHeader(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER,
					size);
		}
		long old = codedIS.pushLimitLong((long) size);
		int px = tree.left & MASK_TO_READ;
		int py = tree.top & MASK_TO_READ;
		boolean contains = false;
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		req.numberOfVisitedObjects++;
		while (codedIS.getBytesUntilLimit() > 0) {
			int x = (codedIS.readSInt32() << SHIFT_COORDINATES) + px;
			int y = (codedIS.readSInt32() << SHIFT_COORDINATES) + py;
			req.cacheCoordinates.add(x);
			req.cacheCoordinates.add(y);
			px = x;
			py = y;
			if (!contains && req.left <= x && req.right >= x && req.top <= y && req.bottom >= y) {
				contains = true;
			}
			if (!contains) {
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		if (!contains) {
			if (maxX >= req.left && minX <= req.right && minY <= req.bottom && maxY >= req.top) {
				contains = true;
			}

		}
		codedIS.popLimit(old);
		if (!contains) {
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}

		// read 

		List<TIntArrayList> innercoordinates = null;
		TIntArrayList additionalTypes = null;
		TIntObjectHashMap<String> stringNames = null;
		TIntArrayList stringOrder = null;
		long id = 0;
		int labelX = 0, labelY = 0;

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
				if(READ_STATS) {
					req.stat.lastObjectCoordinates += size;
					req.stat.addTagHeader(OsmandOdb.MapData.POLYGONINNERCOORDINATES_FIELD_NUMBER,
							size);
				}
				old = codedIS.pushLimitLong((long) size);
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
				old = codedIS.pushLimitLong((long) sizeL);
				if(READ_STATS) {
					req.stat.lastObjectAdditionalTypes += sizeL;
					req.stat.addTagHeader(OsmandOdb.MapData.ADDITIONALTYPES_FIELD_NUMBER,
							sizeL);
				}
				while (codedIS.getBytesUntilLimit() > 0) {
					additionalTypes.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(old);

				break;
			case OsmandOdb.MapData.TYPES_FIELD_NUMBER:
				req.cacheTypes.clear();
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				if(READ_STATS) {
					req.stat.addTagHeader(OsmandOdb.MapData.TYPES_FIELD_NUMBER, sizeL);
					req.stat.lastObjectTypes += sizeL;
				}
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
				if(READ_STATS) {
					req.stat.addTagHeader(OsmandOdb.MapData.ID_FIELD_NUMBER, 0);
					req.stat.lastObjectHeaderInfo -= 1;
					req.stat.lastObjectIdSize += CodedOutputStream.computeSInt64SizeNoTag(id);
				}
				break;
			case OsmandOdb.MapData.STRINGNAMES_FIELD_NUMBER:
				stringNames = new TIntObjectHashMap<String>();
				stringOrder = new TIntArrayList();
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					int stag = codedIS.readRawVarint32();
					int pId = codedIS.readRawVarint32();
					stringNames.put(stag, ((char)pId)+"");
					stringOrder.add(stag);
				}
				codedIS.popLimit(old);
				if(READ_STATS) {
					req.stat.addTagHeader(OsmandOdb.MapData.STRINGNAMES_FIELD_NUMBER, sizeL);
					req.stat.lastStringNamesSize += sizeL;
				}
				break;
			case OsmandOdb.MapData.LABELCOORDINATES_FIELD_NUMBER:
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				int i = 0;
				while (codedIS.getBytesUntilLimit() > 0) {
					if (i == 0) {
						labelX = codedIS.readSInt32();
					} else if (i == 1) {
						labelY = codedIS.readSInt32();
					} else {
						codedIS.readRawVarint32();
					}
					i++;
				}
				codedIS.popLimit(old);
				if (READ_STATS) {
					req.stat.addTagHeader(OsmandOdb.MapData.LABELCOORDINATES_FIELD_NUMBER, sizeL);
					req.stat.lastObjectLabelCoordinates += sizeL;
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		BinaryMapDataObject dataObject = new BinaryMapDataObject();
		dataObject.area = area;
		dataObject.coordinates = req.cacheCoordinates.toArray();
		dataObject.objectNames = stringNames;
		dataObject.namesOrder = stringOrder;
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
		dataObject.labelX = labelX;
		dataObject.labelY = labelY;
		
		return dataObject;
	}

	public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req, List<Integer> typeFilter) throws IOException {
		for (AddressRegion reg : addressIndexes) {
			if (reg.indexNameOffset != -1) {
				codedIS.seek(reg.indexNameOffset);
				long len = readInt();
				long old = codedIS.pushLimitLong((long) len);
				addressAdapter.searchAddressDataByName(reg, req, typeFilter);
				codedIS.popLimit(old);
			}
		}
		return req.getSearchResults();
	}

	public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) throws IOException {
		return searchAddressDataByName(req, null);
	}

	public void initCategories(PoiRegion poiIndex) throws IOException {
		poiAdapter.initCategories(poiIndex);
	}

	public void initCategories() throws IOException {
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
		}
	}

	public List<Amenity> searchPoiByName(SearchRequest<Amenity> req) throws IOException {
		if (req.nameQuery == null || req.nameQuery.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			codedIS.seek(poiIndex.filePointer);
			long old = codedIS.pushLimitLong((long) poiIndex.length);
			poiAdapter.searchPoiByName(poiIndex, req);
			codedIS.popLimit(old);
		}
		return req.getSearchResults();
	}

	public Map<PoiCategory, List<String>> searchPoiCategoriesByName(String query, Map<PoiCategory, List<String>> map) throws IOException {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException();
		}
		Collator collator = OsmAndCollator.primaryCollator();
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			for (int i = 0; i < poiIndex.categories.size(); i++) {
				String cat = poiIndex.categories.get(i);
				PoiCategory catType = poiIndex.categoriesType.get(i);
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

	public List<PoiSubType> searchPoiSubTypesByPrefix(String query) throws IOException {
		if (query == null || query.length() == 0) {
			throw new IllegalArgumentException();
		}
		List<PoiSubType> list = new ArrayList<>();
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			for (int i = 0; i < poiIndex.subTypes.size(); i++) {
				PoiSubType subType = poiIndex.subTypes.get(i);
				if (subType.name.startsWith(query)) {
					list.add(subType);
				}
			}
		}
		return list;
	}

	public List<PoiSubType> getTopIndexSubTypes() throws IOException {
		List<PoiSubType> list = new ArrayList<>();
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			list.addAll(poiIndex.topIndexSubTypes);
		}
		return list;
	}

	public List<Amenity> searchPoi(SearchRequest<Amenity> req) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		for (PoiRegion poiIndex : poiIndexes) {
			poiAdapter.initCategories(poiIndex);
			codedIS.seek(poiIndex.filePointer);
			long old = codedIS.pushLimitLong((long) poiIndex.length);
			poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
			codedIS.popLimit(old);
		}

		return req.getSearchResults();
	}

	public List<Amenity> searchPoi(PoiRegion poiIndex, SearchRequest<Amenity> req) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;

		poiAdapter.initCategories(poiIndex);
		codedIS.seek(poiIndex.filePointer);
		long old = codedIS.pushLimitLong((long) poiIndex.length);
		poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
		codedIS.popLimit(old);
		return req.getSearchResults();
	}

	protected List<String> readStringTable() throws IOException {
		List<String> list = new ArrayList<String>();
		while (true) {
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

	public List<PoiRegion> getPoiIndexes() {
		return poiIndexes;
	}


	public static SearchRequest<BinaryMapDataObject> buildSearchRequest(int sleft, int sright, int stop, int sbottom, int zoom, SearchFilter searchFilter) {
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

	public static <T> SearchRequest<T> buildAddressRequest(ResultMatcher<T> resultMatcher) {
		SearchRequest<T> request = new SearchRequest<T>();
		request.resultMatcher = resultMatcher;
		return request;
	}


	public static <T> SearchRequest<T> buildAddressByNameRequest(ResultMatcher<T> resultMatcher, String nameRequest, 
			StringMatcherMode matcherMode) {
		return buildAddressByNameRequest(resultMatcher, null, nameRequest, matcherMode);
	}

	public static <T> SearchRequest<T> buildAddressByNameRequest(ResultMatcher<T> resultMatcher, ResultMatcher<T> rawDataCollector, String nameRequest,
			StringMatcherMode matcherMode) {
		SearchRequest<T> request = new SearchRequest<T>();
		request.resultMatcher = resultMatcher;
		request.rawDataCollector = rawDataCollector;
		request.nameQuery = nameRequest.trim();
		request.matcherMode = matcherMode;
		return request;
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(List<Location> route, double radius,
			SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> resultMatcher) {
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		float coeff = (float) (radius / MapUtils.getTileDistanceWidth(SearchRequest.ZOOM_TO_SEARCH_POI));
		TLongObjectHashMap<List<Location>> zooms = new TLongObjectHashMap<List<Location>>();
		for (int i = 1; i < route.size(); i++) {
			Location cr = route.get(i);
			Location pr = route.get(i - 1);
			double tx = MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, cr.getLongitude());
			double ty = MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, cr.getLatitude());
			double px = MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, pr.getLongitude());
			double py = MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, pr.getLatitude());
			double topLeftX = Math.min(tx, px) - coeff;
			double topLeftY = Math.min(ty, py) - coeff;
			double bottomRightX = Math.max(tx, px) + coeff;
			double bottomRightY = Math.max(ty, py) + coeff;
			for (int x = (int) topLeftX; x <= bottomRightX; x++) {
				for (int y = (int) topLeftY; y <= bottomRightY; y++) {
					long hash = (((long) x) << SearchRequest.ZOOM_TO_SEARCH_POI) + y;
					if (!zooms.containsKey(hash)) {
						zooms.put(hash, new LinkedList<Location>());
					}
					List<Location> ll = zooms.get(hash);
					ll.add(pr);
					ll.add(cr);
				}
			}

		}
		int sleft = Integer.MAX_VALUE, sright = 0, stop = Integer.MAX_VALUE, sbottom = 0;
		for (long vl : zooms.keys()) {
			long x = (vl >> SearchRequest.ZOOM_TO_SEARCH_POI) << (31 - SearchRequest.ZOOM_TO_SEARCH_POI);
			long y = (vl & ((1 << SearchRequest.ZOOM_TO_SEARCH_POI) - 1)) << (31 - SearchRequest.ZOOM_TO_SEARCH_POI);
			sleft = (int) Math.min(x, sleft);
			stop = (int) Math.min(y, stop);
			sbottom = (int) Math.max(y, sbottom);
			sright = (int) Math.max(x, sright);
		}
		request.radius = radius;
		request.left = sleft;
		request.zoom = -1;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.tiles = zooms;
		request.poiTypeFilter = poiTypeFilter;
		request.resultMatcher = resultMatcher;
		return request;
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(int sleft, int sright, int stop, int sbottom, int zoom,
	                                                           SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> matcher) {
		return 	buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom, poiTypeFilter, null, matcher);
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(int sleft, int sright, int stop, int sbottom, int zoom,
	                                                           SearchPoiTypeFilter poiTypeFilter, SearchPoiAdditionalFilter poiTopIndexAdditionalFilter, ResultMatcher<Amenity> matcher){
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		request.poiTypeFilter = poiTypeFilter;
		request.poiAdditionalFilter = poiTopIndexAdditionalFilter;
		request.resultMatcher = matcher;

		return request;
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(LatLon latLon, int radius, int zoom,
	                                                           SearchPoiTypeFilter poiTypeFilter,
	                                                           ResultMatcher<Amenity> matcher) {
		SearchRequest<Amenity> request = new SearchRequest<>();
		request.setBBoxRadius(latLon.getLatitude(), latLon.getLongitude(), radius);
		request.zoom = zoom;
		request.poiTypeFilter = poiTypeFilter;
		request.resultMatcher = matcher;
		return request;
	}

	public static SearchRequest<RouteDataObject> buildSearchRouteRequest(int sleft, int sright, int stop, int sbottom,
	                                                                     ResultMatcher<RouteDataObject> matcher) {
		SearchRequest<RouteDataObject> request = new SearchRequest<RouteDataObject>();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.resultMatcher = matcher;

		return request;
	}


	public static SearchRequest<Amenity> buildSearchPoiRequest(int x, int y, String nameFilter, int sleft, int sright, int stop, int sbottom, ResultMatcher<Amenity> resultMatcher) {
		return buildSearchPoiRequest(x, y, nameFilter, sleft, sright, stop, sbottom, resultMatcher, null);
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(int x, int y, String nameFilter, int sleft, int sright, int stop, int sbottom, ResultMatcher<Amenity> resultMatcher, ResultMatcher<Amenity> rawDataCollector) {
		return buildSearchPoiRequest(x, y, nameFilter, sleft, sright, stop, sbottom, null, resultMatcher, null);
	}

	public static SearchRequest<Amenity> buildSearchPoiRequest(int x, int y, String nameFilter, int sleft, int sright, int stop, int sbottom, SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> resultMatcher, ResultMatcher<Amenity> rawDataCollector) {
		SearchRequest<Amenity> request = new SearchRequest<Amenity>();
		request.x = x;
		request.y = y;
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.poiTypeFilter = poiTypeFilter;
		request.resultMatcher = resultMatcher;
		request.rawDataCollector = rawDataCollector;
		request.nameQuery = nameFilter.trim();
		return request;
	}


	public static SearchRequest<TransportStop> buildSearchTransportRequest(int sleft, int sright, int stop, int sbottom, int limit, List<TransportStop> stops) {
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

	public void close() throws IOException {
		if (codedIS != null) {
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

		public boolean accept(PoiCategory type, String subcategory);

		public boolean isEmpty();
	}

	public static interface SearchPoiAdditionalFilter {
		public boolean accept(PoiSubType poiSubType, String value);
		String getName();
		String getIconResource();
	}

	public static class MapObjectStat {
		public int lastStringNamesSize;
		public int lastObjectIdSize;
		public int lastObjectHeaderInfo;
		public int lastObjectAdditionalTypes;
		public int lastObjectTypes;
		public int lastObjectCoordinates;
		public int lastObjectLabelCoordinates;

		public int lastObjectSize;
		public int lastBlockStringTableSize;
		public int lastBlockHeaderInfo;

		public void addBlockHeader(int typesFieldNumber, int sizeL) {
			lastBlockHeaderInfo +=
					CodedOutputStream.computeTagSize(typesFieldNumber) +
							CodedOutputStream.computeRawVarint32Size(sizeL);
		}

		public void addTagHeader(int typesFieldNumber, int sizeL) {
			lastObjectHeaderInfo +=
					CodedOutputStream.computeTagSize(typesFieldNumber) +
							CodedOutputStream.computeRawVarint32Size(sizeL);
		}

		public void clearObjectStats() {
			lastStringNamesSize = 0;
			lastObjectIdSize = 0;
			lastObjectHeaderInfo = 0;
			lastObjectAdditionalTypes = 0;
			lastObjectTypes = 0;
			lastObjectCoordinates = 0;
			lastObjectLabelCoordinates = 0;
		}
	}

	public static class SearchRequest<T> {
		public final static int ZOOM_TO_SEARCH_POI = 16;
		private List<T> searchResults = new ArrayList<T>();
		private boolean land = false;
		private boolean ocean = false;

		private ResultMatcher<T> resultMatcher;
		private ResultMatcher<T> rawDataCollector;

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

		// search on the path
		// stores tile of 16 index and pairs (even length always) of points intersecting tile
		TLongObjectHashMap<List<Location>> tiles = null;
		double radius = -1;


		String nameQuery = null;
		StringMatcherMode matcherMode = StringMatcherMode.CHECK_STARTS_FROM_SPACE;
		SearchFilter searchFilter = null;

		SearchPoiTypeFilter poiTypeFilter = null;
		SearchPoiAdditionalFilter poiAdditionalFilter;

		// cache information
		TIntArrayList cacheCoordinates = new TIntArrayList();
		TIntArrayList cacheTypes = new TIntArrayList();
		TLongArrayList cacheIdsA = new TLongArrayList();
		TLongArrayList cacheIdsB = new TLongArrayList();
		TLongArrayList cacheIdsC = new TLongArrayList();

		MapObjectStat stat = new MapObjectStat();


		// TRACE INFO
		public boolean log = true;
		int numberOfVisitedObjects = 0;
		int numberOfAcceptedObjects = 0;
		int numberOfReadSubtrees = 0;
		int numberOfAcceptedSubtrees = 0;
		boolean interrupted = false;

		public MapObjectStat getStat() {
			return stat;
		}

		protected SearchRequest() {
		}

		public long getTileHashOnPath(double lat, double lon) {
			long x = (int) MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, lon);
			long y = (int) MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, lat);
			return (x << SearchRequest.ZOOM_TO_SEARCH_POI) | y;
		}

		public void setBBoxRadius(double lat, double lon, int radiusMeters) {
			double dx = MapUtils.getTileNumberX(16, lon);
			double half16t = MapUtils.getDistance(lat, MapUtils.getLongitudeFromTile(16, ((int) dx) + 0.5), 
					lat, MapUtils.getLongitudeFromTile(16, (int) dx));
			double cf31 = ((double) radiusMeters / (half16t * 2)) * (1 << 15);
			y = MapUtils.get31TileNumberY(lat);
			x = MapUtils.get31TileNumberX(lon);
			left = (int) (x - cf31);
			right = (int) (x + cf31);
			top = (int) (y - cf31);
			bottom = (int) (y + cf31);
		}

		public boolean publish(T obj) {
			if (resultMatcher == null || resultMatcher.publish(obj)) {
				searchResults.add(obj);
				return true;
			}
			return false;
		}

		public void collectRawData(T obj) {
			if (rawDataCollector != null) {
				rawDataCollector.publish(obj);
			}
		}

		protected void publishOceanTile(boolean ocean) {
			if (ocean) {
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

		public void setLimit(int limit) {
			this.limit = limit;
		}

		public boolean isCancelled() {
			if (this.interrupted) {
				return interrupted;
			}
			if (resultMatcher != null) {
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

		public boolean intersects(int l, int t, int r, int b) {
			return r >= left && l <= right && t <= bottom && b >= top;
		}

		public boolean contains(int l, int t, int r, int b) {
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

		public int getZoom() {
			return zoom;
		}

		public void clearSearchResults() {
			// recreate whole list to allow GC collect old data 
			searchResults = new ArrayList<T>();
			cacheCoordinates.clear();
			cacheTypes.clear();
			cacheIdsA.clear();
			cacheIdsB.clear();
			cacheIdsC.clear();
			land = false;
			ocean = false;
			numberOfVisitedObjects = 0;
			numberOfAcceptedObjects = 0;
			numberOfReadSubtrees = 0;
			numberOfAcceptedSubtrees = 0;
		}

		public boolean isBboxSpecified() {
			return left != 0 || right != 0;
		}
	}


	public static class MapIndex extends BinaryIndexPart {
		List<MapRoot> roots = new ArrayList<MapRoot>();

		Map<String, Map<String, Integer>> encodingRules = new HashMap<String, Map<String, Integer>>();
		public TIntObjectMap<TagValuePair> decodingRules = new TIntObjectHashMap<TagValuePair>();
		public int nameEncodingType = 0;
		public int nameEnEncodingType = -1;
		public int refEncodingType = -1;
		public int coastlineEncodingType = -1;
		public int coastlineBrokenEncodingType = -1;
		public int landEncodingType = -1;
		public int onewayAttribute = -1;
		public int onewayReverseAttribute = -1;
		public TIntHashSet positiveLayers = new TIntHashSet(2);
		public TIntHashSet negativeLayers = new TIntHashSet(2);
		public int encodingRulesSizeBytes;

		// to speed up comparision
		private MapIndex referenceMapIndex;

		public Integer getRule(String t, String v) {
			Map<String, Integer> m = encodingRules.get(t);
			if (m != null) {
				return m.get(v);
			}
			return null;
		}
		

		public LatLon getCenterLatLon() {
			if(roots.size() == 0) {
				return null;
			}
			MapRoot mapRoot = roots.get(roots.size() - 1);
			double cy = (MapUtils.get31LatitudeY(mapRoot.getBottom()) + MapUtils.get31LatitudeY(mapRoot.getTop())) / 2;
			double cx = (MapUtils.get31LongitudeX(mapRoot.getLeft()) + MapUtils.get31LongitudeX(mapRoot.getRight())) / 2;
			return  new LatLon(cy, cx);
		}

		public List<MapRoot> getRoots() {
			return roots;
		}

		public TagValuePair decodeType(int type) {
			return decodingRules.get(type);
		}
		
		public Integer getRule(TagValuePair tv) {
			Map<String, Integer> m = encodingRules.get(tv.tag);
			if (m != null) {
				return m.get(tv.value);
			}
			return null;
		}

		public void finishInitializingTags() {
			int free = decodingRules.size();
			coastlineBrokenEncodingType = free++;
			initMapEncodingRule(0, coastlineBrokenEncodingType, "natural", "coastline_broken");
			if (landEncodingType == -1) {
				landEncodingType = free++;
				initMapEncodingRule(0, landEncodingType, "natural", "land");
			}
		}

		public boolean isRegisteredRule(int id) {
			return decodingRules.containsKey(id);
		}

		public void initMapEncodingRule(int type, int id, String tag, String val) {
			if (!encodingRules.containsKey(tag)) {
				encodingRules.put(tag, new HashMap<String, Integer>());
			}
			encodingRules.get(tag).put(val, id);
			if (!decodingRules.containsKey(id)) {
				decodingRules.put(id, new TagValuePair(tag, val, type));
			}

			if ("name".equals(tag)) {
				nameEncodingType = id;
			} else if ("natural".equals(tag) && "coastline".equals(val)) {
				coastlineEncodingType = id;
			} else if ("natural".equals(tag) && "land".equals(val)) {
				landEncodingType = id;
			} else if ("oneway".equals(tag) && "yes".equals(val)) {
				onewayAttribute = id;
			} else if ("oneway".equals(tag) && "-1".equals(val)) {
				onewayReverseAttribute = id;
			} else if ("ref".equals(tag)) {
				refEncodingType = id;
			} else if ("name:en".equals(tag)) {
				nameEnEncodingType = id;
			} else if ("tunnel".equals(tag)) {
				negativeLayers.add(id);
			} else if ("bridge".equals(tag)) {
				positiveLayers.add(id);
			} else if ("layer".equals(tag)) {
				if (val != null && !val.equals("0") && val.length() > 0) {
					if (val.startsWith("-")) {
						negativeLayers.add(id);
					} else {
						positiveLayers.add(id);
					}
				}
			}
		}

		public boolean isBaseMap() {
			return name != null && name.toLowerCase().contains(BASEMAP_NAME);
		}

		public String getPartName() {
			return "Map";
		}

		public int getFieldNumber() {
			return OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER;
		}

		public BinaryMapDataObject adoptMapObject(BinaryMapDataObject o) {
			if (o.mapIndex == this || o.mapIndex == referenceMapIndex) {
				return o;
			}
			if (encodingRules.isEmpty()) {
				encodingRules.putAll(o.mapIndex.encodingRules);
				decodingRules.putAll(o.mapIndex.decodingRules);
				referenceMapIndex = o.mapIndex;
				return o;
			}
			TIntArrayList types = new TIntArrayList();
			TIntArrayList additionalTypes = new TIntArrayList();
			if (o.types != null) {
				for (int i = 0; i < o.types.length; i++) {
					TagValuePair tp = o.mapIndex.decodeType(o.types[i]);
					Integer r = getRule(tp);
					if (r != null) {
						types.add(r);
					} else {
						int nid = decodingRules.size() + 1;
						initMapEncodingRule(tp.additionalAttribute, nid, tp.tag, tp.value);
						types.add(nid);
					}
				}
			}
			if (o.additionalTypes != null) {
				for (int i = 0; i < o.additionalTypes.length; i++) {
					TagValuePair tp = o.mapIndex.decodeType(o.additionalTypes[i]);
					Integer r = getRule(tp);
					if (r != null) {
						additionalTypes.add(r);
					} else {
						int nid = decodingRules.size() + 1;
						initMapEncodingRule(tp.additionalAttribute, nid, tp.tag, tp.value);
						additionalTypes.add(nid);
					}
				}
			}

			BinaryMapDataObject bm = new BinaryMapDataObject(o.id, o.coordinates, o.polygonInnerCoordinates,
					o.objectType, o.area, types.toArray(), additionalTypes.isEmpty() ? null : additionalTypes.toArray(),
					o.labelX, o.labelY);
			if (o.namesOrder != null) {
				bm.objectNames = new TIntObjectHashMap<>();
				bm.namesOrder = new TIntArrayList();
				for (int i = 0; i < o.namesOrder.size(); i++) {
					int nameType = o.namesOrder.get(i);
					String name = o.objectNames.get(nameType);
					TagValuePair tp = o.mapIndex.decodeType(nameType);
					Integer nameKeyId = getRule(tp);
					if (nameKeyId == null) {
						nameKeyId = decodingRules.size() + 1;
						initMapEncodingRule(tp.additionalAttribute, nameKeyId, tp.tag, tp.value);
						additionalTypes.add(nameKeyId);
					}
					bm.objectNames.put(nameKeyId, name);
					bm.namesOrder.add(nameKeyId);
				}
			}
			return bm;
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

		public boolean isAdditional() {
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

		public String toSimpleString() {
			if (value == null) {
				return tag;
			}
			return tag + "-" + value;
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


		public MapZooms.MapZoomPair getMapZoom() {
			return new MapZooms.MapZoomPair(minZoom, maxZoom);
		}
	}

	private static class MapTree {
		long filePointer = 0;
		long length = 0;

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

		public long getLength() {
			return length;
		}

		public long getFilePointer() {
			return filePointer;
		}

		@Override
		public String toString() {
			return "Top Lat " + ((float) MapUtils.get31LatitudeY(top)) + " lon " + ((float) MapUtils.get31LongitudeX(left))
					+ " Bottom lat " + ((float) MapUtils.get31LatitudeY(bottom)) + " lon " + ((float) MapUtils.get31LongitudeX(right));
		}

	}


	private static boolean testMapSearch = false;
	private static boolean testAddressSearch = false;
	private static boolean testAddressSearchName = false;
	private static boolean testAddressJustifySearch = false;
	private static boolean testPoiSearch = true;
	private static boolean testPoiSearchOnPath = false;
	private static boolean testTransportSearch = false;
	
	private static int sleft = MapUtils.get31TileNumberX(27.55079);
	private static int sright = MapUtils.get31TileNumberX(27.55317);
	private static int stop = MapUtils.get31TileNumberY(53.89378);
	private static int sbottom = MapUtils.get31TileNumberY(53.89276);
	private static int szoom = 15;

	private static void println(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws IOException {
		File fl = new File(System.getProperty("maps") + "/Synthetic_test_rendering.obf");
		fl = new File(System.getProperty("maps") +"/Wikivoyage.obf__");
		
		RandomAccessFile raf = new RandomAccessFile(fl, "r");

		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
		println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		long time = System.currentTimeMillis();

		if (testMapSearch) {
			testMapSearch(reader);
		}
		if (testAddressSearchName) {
			testAddressSearchByName(reader);
		}
		if (testAddressSearch) {
			testAddressSearch(reader);
		}
		if (testAddressJustifySearch) {
			testAddressJustifySearch(reader);
		}
		if (testTransportSearch) {
			testTransportSearch(reader);
		}

		if (testPoiSearch || testPoiSearchOnPath) {
			PoiRegion poiRegion = reader.getPoiIndexes().get(0);
			if (testPoiSearch) {
				testPoiSearch(reader, poiRegion);
				testPoiSearchByName(reader);
			}
			if (testPoiSearchOnPath) {
				testSearchOnthePath(reader);
			}
		}

		println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())); //$NON-NLS-1$
		println("Time " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
	}

	private static void testSearchOnthePath(BinaryMapIndexReader reader) throws IOException {
		float radius = 1000;
		final MapPoiTypes poiTypes = MapPoiTypes.getDefault();
		long now = System.currentTimeMillis();
		println("Searching poi on the path...");
		final List<Location> locations = readGPX(new File(
				"/Users/victorshcherb/osmand/maps/2015-03-07_19-07_Sat.gpx"));
		SearchRequest<Amenity> req = buildSearchPoiRequest(locations, radius, new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				if (type == poiTypes.getPoiCategoryByName("shop") && subcategory.contains("super")) {
					return true;
				}
				return false;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}

		}, null);
		req.zoom = -1;
		List<Amenity> results = reader.searchPoi(req);
		int k = 0;
		println("Search done in " + (System.currentTimeMillis() - now) + " ms ");
		now = System.currentTimeMillis();

		for (Amenity a : results) {
			final float dds = dist(a.getLocation(), locations);
			if (dds <= radius) {
				println("+ " + a.getType() + " " + a.getSubType() + " Dist " + dds + " (=" + (float) a.getRoutePoint().deviateDistance + ") " + a.getName() + " " + a.getLocation());
				k++;
			} else {
				println(a.getType() + " " + a.getSubType() + " Dist " + dds + " " + a.getName() + " " + a.getLocation());
			}
		}
		println("Filtered in " + (System.currentTimeMillis() - now) + "ms " + k + " of " + results.size());
	}

	private static float dist(LatLon l, List<Location> locations) {
		float dist = Float.POSITIVE_INFINITY;
		for (int i = 1; i < locations.size(); i++) {
			dist = Math.min(dist, (float) MapUtils.getOrthogonalDistance(l.getLatitude(), l.getLongitude(),
					locations.get(i - 1).getLatitude(), locations.get(i - 1).getLongitude(),
					locations.get(i).getLatitude(), locations.get(i).getLongitude()));
		}
		return dist;
	}

	private static Reader getUTF8Reader(InputStream f) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(f);
		assert bis.markSupported();
		bis.mark(3);
		boolean reset = true;
		byte[] t = new byte[3];
		bis.read(t);
		if (t[0] == ((byte) 0xef) && t[1] == ((byte) 0xbb) && t[2] == ((byte) 0xbf)) {
			reset = false;
		}
		if (reset) {
			bis.reset();
		}
		return new InputStreamReader(bis, "UTF-8");
	}

	private static List<Location> readGPX(File f) {
		List<Location> res = new ArrayList<Location>();
		try {
			BufferedReader reader = new BufferedReader(getUTF8Reader(new FileInputStream(f)));
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dom = factory.newDocumentBuilder();
//				{
//					String s = null;
//					boolean fist = true;
//					while ((s = reader.readLine()) != null) {
//						if (fist) {
//							fist = false;
//						}
//						content.append(s).append("\n");
//					}
//				}
//				Document doc = dom.parse(new InputSource(new StringReader(content.toString())));
			Document doc = dom.parse(new InputSource(reader));
			NodeList list = doc.getElementsByTagName("trkpt");
			for (int i = 0; i < list.getLength(); i++) {
				Element item = (Element) list.item(i);
				try {
					double lon = Double.parseDouble(item.getAttribute("lon"));
					double lat = Double.parseDouble(item.getAttribute("lat"));
					final Location o = new Location("");
					o.setLatitude(lat);
					o.setLongitude(lon);
					res.add(o);
				} catch (NumberFormatException e) {
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		return res;
	}

	private static void testPoiSearchByName(BinaryMapIndexReader reader) throws IOException {
		println("Searching by name...");
		SearchRequest<Amenity> req = buildSearchPoiRequest(0, 0, "central ukraine",
				0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);
		
		reader.searchPoiByName(req);
		for (Amenity a : req.getSearchResults()) {
			println(a.getType().getTranslation() +
					" " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
		}
	}

	private static void testPoiSearch(BinaryMapIndexReader reader, PoiRegion poiRegion) throws IOException {
		
		println(MapUtils.get31LongitudeX(poiRegion.left31) + " " + MapUtils.get31LongitudeX(poiRegion.right31) +
				" " +MapUtils.get31LatitudeY( poiRegion.bottom31 )+ " "
				+ MapUtils.get31LatitudeY(poiRegion.top31));
		for (int i = 0; i < poiRegion.categories.size(); i++) {
			println(poiRegion.categories.get(i));
			println(" " + poiRegion.subcategories.get(i));
		}

		SearchRequest<Amenity> req = buildSearchPoiRequest(sleft, sright, stop, sbottom, -1, ACCEPT_ALL_POI_TYPE_FILTER, null);
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
		for (TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom,
				-1, null))) {
			println(s.getName());
			TLongObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());
			for (net.osmand.data.TransportRoute route : routes.valueCollection()) {
				println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " "
						+ route.getAvgBothDistance());
				StringBuilder b = new StringBuilder();
				if(route.getForwardWays() == null) {
					continue;
				}
				for(Way w : route.getForwardWays()) {
					b.append(w.getNodes()).append(" ");
				}
				println("  forward ways: " + b.toString());
			}
		}
	}

	private static void updateFrequence(Map<String, Integer> street, String key) {
		if (!street.containsKey(key)) {
			street.put(key, 1);
		} else {
			street.put(key, street.get(key) + 1);
		}

	}

	void readIndexedStringTable(Collator instance, List<String> queries, String prefix, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters) throws IOException {
		String key = null;
		boolean[] matched = new boolean[matchedCharacters.size()];
		boolean shouldWeReadSubtable = false;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.IndexedStringTable.KEY_FIELD_NUMBER :
				key = codedIS.readString();
				if (prefix.length() > 0) {
					key = prefix + key;
				}
				shouldWeReadSubtable = false;
				for (int i = 0; i < queries.size(); i++) {
					int charMatches = matchedCharacters.get(i);
					String query = queries.get(i);
					matched[i] = false;
					if (query == null) {
						continue;
					}
					
					// check query is part of key (the best matching)
					if (CollatorStringMatcher.cmatches(instance, key, query, StringMatcherMode.CHECK_ONLY_STARTS_WITH)) {
						if (query.length() >= charMatches) {
							if (query.length() > charMatches) {
								matchedCharacters.set(i, query.length());
								listOffsets.get(i).clear();
							}
							matched[i] = true;
						}
						// check key is part of query
					} else if (CollatorStringMatcher.cmatches(instance, query, key, StringMatcherMode.CHECK_ONLY_STARTS_WITH)) {
						if (key.length() >= charMatches) {
							if (key.length() > charMatches) {
								matchedCharacters.set(i, key.length());
								listOffsets.get(i).clear();
							}
							matched[i] = true;
						}
					}
					shouldWeReadSubtable |= matched[i];
				}
				break;
			case OsmandOdb.IndexedStringTable.VAL_FIELD_NUMBER :
				int val = (int) readInt(); // FIXME
				for (int i = 0; i < queries.size(); i++) {
					if (matched[i]) {
						listOffsets.get(i).add(val);
					}
				}
				break;
			case OsmandOdb.IndexedStringTable.SUBTABLES_FIELD_NUMBER :
				long len = codedIS.readRawVarint32();
				long oldLim = codedIS.pushLimitLong((long) len);
				if (shouldWeReadSubtable && key != null) {
					List<String> subqueries = new ArrayList<>(queries);
					// reset query so we don't search what was not matched
					for(int i = 0; i < queries.size(); i++) {
						if(!matched[i]) {
							subqueries.set(i, null);
						}
					}
					readIndexedStringTable(instance, subqueries, key, listOffsets, matchedCharacters);
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
				if (object instanceof Street) {
					System.out.println(object + " " + ((Street) object).getCity());
				} else {
					System.out.println(object + " " + object.getId());
				}
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		}, "Guy'", StringMatcherMode.CHECK_ONLY_STARTS_WITH);
//		req.setBBoxRadius(52.276142, 4.8608723, 15000);
		reader.searchAddressDataByName(req);
	}

	/**
	 * @param reader
	 * @throws IOException
	 */
	/**
	 * @param reader
	 * @throws IOException
	 */
	private static void testAddressJustifySearch(BinaryMapIndexReader reader) throws IOException {
		final String streetName = "Logger";
		final double lat = 52.28212d;
		final double lon = 4.86269d;
		// test address index search
		final List<Street> streetsList = new ArrayList<Street>();
		SearchRequest<MapObject> req = buildAddressByNameRequest(new ResultMatcher<MapObject>() {
			@Override
			public boolean publish(MapObject object) {
				if (object instanceof Street && object.getName().equalsIgnoreCase(streetName)) {
					if (MapUtils.getDistance(object.getLocation(), lat, lon) < 20000) {
						streetsList.add((Street) object);
						return true;
					}
					return false;
				}
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		}, streetName, StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
		reader.searchAddressDataByName(req);
		TreeMap<MapObject, Street> resMap = new TreeMap<MapObject, Street>(new Comparator<MapObject>() {

			@Override
			public int compare(MapObject o1, MapObject o2) {
				LatLon l1 = o1.getLocation();
				LatLon l2 = o2.getLocation();
				if (l1 == null || l2 == null) {
					return l2 == l1 ? 0 : (l1 == null ? -1 : 1);
				}
				return Double.compare(MapUtils.getDistance(l1, lat, lon), MapUtils.getDistance(l2, lat, lon));
			}
		});
		for (Street s : streetsList) {
			resMap.put(s, s);
			reader.preloadBuildings(s, null);
			for (Building b : s.getBuildings()) {
				if (MapUtils.getDistance(b.getLocation(), lat, lon) < 100) {
					resMap.put(b, s);
				}
			}
		}
		for (Entry<MapObject, Street> entry : resMap.entrySet()) {
			MapObject e = entry.getKey();
			Street s = entry.getValue();
			if (e instanceof Building && MapUtils.getDistance(e.getLocation(), lat, lon) < 40) {
				Building b = (Building) e;
				System.out.println(b.getName() + "   " + s);
			} else if (e instanceof Street) {
				System.out.println(s + "   " + ((Street) s).getCity());
			}
		}

	}

	private static void testAddressSearch(BinaryMapIndexReader reader) throws IOException {
		// test address index search
		final Map<String, Integer> streetFreq = new HashMap<String, Integer>();
		List<City> cs = reader.getCities(null, BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
		for (City c : cs) {
			int buildings = 0;
			reader.preloadStreets(c, null);
			for (Street s : c.getStreets()) {
				updateFrequence(streetFreq, s.getName());
				reader.preloadBuildings(s, buildAddressRequest((ResultMatcher<Building>) null));
				buildings += s.getBuildings().size();
				println(s.getName() + " " + s.getName("ru"));
			}
			println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings + " " + c.getEnName(true) + " " + c.getName("ru"));
		}
//		int[] count = new int[1];
		List<City> villages = reader.getCities(buildAddressRequest((ResultMatcher<City>) null), BinaryMapAddressReaderAdapter.VILLAGES_TYPE);
		for (City v : villages) {
			reader.preloadStreets(v, null);
			for (Street s : v.getStreets()) {
				updateFrequence(streetFreq, s.getName());
			}
		}
		System.out.println("Villages " + villages.size());

		List<String> sorted = new ArrayList<String>(streetFreq.keySet());
		Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return -streetFreq.get(o1) + streetFreq.get(o2);
			}
		});
		System.out.println(streetFreq.size());
		for (String s : sorted) {
			System.out.println(s + "   " + streetFreq.get(s));
			if (streetFreq.get(s) < 10) {
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
				b.append(obj.area ? "Area" : (obj.getPointsLength() > 1 ? "Way" : "Point"));
				int[] types = obj.getTypes();
				b.append(" types [");
				for (int j = 0; j < types.length; j++) {
					if (j > 0) {
						b.append(", ");
					}
					TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
					if (pair == null) {
						throw new NullPointerException("Type " + types[j] + "was not found");
					}
					b.append(pair.toSimpleString()).append("(").append(types[j]).append(")");
				}
				b.append("]");
				if (obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0) {
					b.append(" add_types [");
					for (int j = 0; j < obj.getAdditionalTypes().length; j++) {
						if (j > 0) {
							b.append(", ");
						}
						TagValuePair pair = obj.getMapIndex().decodeType(obj.getAdditionalTypes()[j]);
						if (pair == null) {
							throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
						}
						b.append(pair.toSimpleString()).append("(").append(obj.getAdditionalTypes()[j]).append(")");

					}
					b.append("]");
				}
				TIntObjectHashMap<String> names = obj.getObjectNames();
				if (names != null && !names.isEmpty()) {
					b.append(" Names [");
					int[] keys = names.keys();
					for (int j = 0; j < keys.length; j++) {
						if (j > 0) {
							b.append(", ");
						}
						TagValuePair pair = obj.getMapIndex().decodeType(keys[j]);
						if (pair == null) {
							throw new NullPointerException("Type " + keys[j] + "was not found");
						}
						b.append(pair.toSimpleString()).append("(").append(keys[j]).append(")");
						b.append(" - ").append(names.get(keys[j]));
					}
					b.append("]");
				}

				b.append(" id ").append((obj.getId() >> 1));
				b.append(" lat/lon : ");
				for (int i = 0; i < obj.getPointsLength(); i++) {
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


	public List<RouteSubregion> searchRouteIndexTree(SearchRequest<?> req, List<RouteSubregion> list) throws IOException {
		req.numberOfVisitedObjects = 0;
		req.numberOfAcceptedObjects = 0;
		req.numberOfAcceptedSubtrees = 0;
		req.numberOfReadSubtrees = 0;
		if (routeAdapter != null) {
			routeAdapter.initRouteTypesIfNeeded(req, list);
			return routeAdapter.searchRouteRegionTree(req, list,
					new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>());
		}
		return Collections.emptyList();
	}

	public void loadRouteIndexData(List<RouteSubregion> toLoad, ResultMatcher<RouteDataObject> matcher) throws IOException {
		if (routeAdapter != null) {
			routeAdapter.loadRouteRegionData(toLoad, matcher);
		}
	}

	public List<RouteDataObject> loadRouteIndexData(RouteSubregion rs) throws IOException {
		if (routeAdapter != null) {
			return routeAdapter.loadRouteRegionData(rs);
		}
		return Collections.emptyList();
	}

	public void initRouteRegion(RouteRegion routeReg) throws IOException {
		if (routeAdapter != null) {
			routeAdapter.initRouteRegion(routeReg);
		}
	}

	
	public TLongObjectHashMap<IncompleteTransportRoute> getIncompleteTransportRoutes() throws InvalidProtocolBufferException, IOException {
		if (incompleteTransportRoutes == null) {
			incompleteTransportRoutes = new TLongObjectHashMap<>();
			for (TransportIndex ti : transportIndexes) {
				if (ti.incompleteRoutesLength > 0) {
					codedIS.seek(ti.incompleteRoutesOffset);
					long oldLimit = codedIS.pushLimitLong((long) ti.incompleteRoutesLength);
					transportAdapter.readIncompleteRoutesList(incompleteTransportRoutes, ti.filePointer);
					codedIS.popLimit(oldLimit);
				}
			}
		}
		return incompleteTransportRoutes;
	}

	public static class OsmAndOwner {
		String name = "";
		String pluginid = "";
		String description = "";
		String resource = "";

		public OsmAndOwner() {
		}

		public OsmAndOwner(String name, String resource, String pluginid, String description) {
			this.name = name;
			this.resource = resource;
			this.pluginid = pluginid;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getPluginid() {
			return pluginid;
		}

		public String getDescription() {
			return description;
		}

		public String getResource() {
			return resource;
		}

		public String toString() {
			String owner = " owner=name:" + name;
			if (!resource.isEmpty()) {
				owner += ", resource:" + resource;
			}
			if (!description.isEmpty()) {
				owner += ", description:" + description;
			}
			if (!pluginid.isEmpty()) {
				owner += ", pluginid:" + pluginid;
			}
			return owner;
		}
	}

	private void readOsmAndOwner() throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);

			switch (tag) {
				case 0:
					return;
				case OsmandOdb.OsmAndOwner.NAME_FIELD_NUMBER :
					owner.name = codedIS.readString();
					break;
				case OsmandOdb.OsmAndOwner.RESOURCE_FIELD_NUMBER :
					owner.resource = codedIS.readString();
					break;
				case OsmandOdb.OsmAndOwner.PLUGINID_FIELD_NUMBER :
					owner.pluginid = codedIS.readString();
					break;
				case OsmandOdb.OsmAndOwner.DESCRIPTION_FIELD_NUMBER :
					owner.description = codedIS.readString();
					break;
				default:
					skipUnknownField(t);
					break;
			}
		}
	}

	
	@Override
	public String toString() {
		return file.getName();
	}
}
