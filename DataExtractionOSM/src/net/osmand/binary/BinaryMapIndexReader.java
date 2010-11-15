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
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.data.TransportStop;
import net.osmand.data.City.CityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	public final static int TRANSPORT_STOP_ZOOM = 24;
	
	private final RandomAccessFile raf;
	private int version;
	private List<MapIndex> mapIndexes = new ArrayList<MapIndex>();
	private List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	private List<TransportIndex> transportIndexes = new ArrayList<TransportIndex>();
	private List<BinaryIndexPart> indexes = new ArrayList<BinaryIndexPart>();
	
	private CodedInputStreamRAF codedIS;
	
	private final static Log log = LogUtil.getLog(BinaryMapIndexReader.class);
	

	
	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024);
		codedIS.setSizeLimit(Integer.MAX_VALUE); // 2048 MB
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
				readAddressIndex(region);
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
				readTransportIndex(ind);
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
	

	private void skipUnknownField(int tag) throws IOException{
		int wireType = WireFormat.getTagWireType(tag);
		if(wireType == WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED){
			int length = readInt();
			codedIS.skipRawBytes(length);
		} else {
			codedIS.skipField(tag);
		}
	}
	
	private void readTransportIndex(TransportIndex ind) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndTransportIndex.ROUTES_FIELD_NUMBER :
				skipUnknownField(t);
				break;
			case OsmandOdb.OsmAndTransportIndex.NAME_FIELD_NUMBER :
				ind.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndTransportIndex.STOPS_FIELD_NUMBER :
				ind.stopsFileLength = readInt();
				ind.stopsFileOffset = codedIS.getTotalBytesRead();
				int old = codedIS.pushLimit(ind.stopsFileLength);
				readTransportBounds(ind);
				codedIS.popLimit(old);
				break;
			case OsmandOdb.OsmAndTransportIndex.STRINGTABLE_FIELD_NUMBER :
				IndexStringTable st = new IndexStringTable();
				st.length = codedIS.readRawVarint32();
				st.fileOffset = codedIS.getTotalBytesRead();
				readStringTable(st, 0, 20, true);
				ind.stringTable = st;
				codedIS.seek(st.length + st.fileOffset);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	// if cache false put into window
	private int readStringTable(IndexStringTable st, int startOffset, int length, boolean cache) throws IOException {
		
		int toSkip = seekUsingOffsets(st, startOffset);
		if(!cache){
			st.window.clear();
			st.windowOffset = startOffset;
		}
		int old = codedIS.pushLimit(st.fileOffset + st.length - codedIS.getTotalBytesRead());
		while (length > 0) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				length = 0;
				break;
			case OsmandOdb.StringTable.S_FIELD_NUMBER:
				if (toSkip > 0) {
					toSkip--;
					skipUnknownField(t);
				} else {
					String string = codedIS.readString();
					if(cache){
						st.cacheOfStrings.put(startOffset, string);
					} else {
						st.window.add(string);
					}
					startOffset++;
					length--;
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		codedIS.popLimit(old);
		return startOffset;
	}
	
	protected String getStringFromStringTable(IndexStringTable st, int ind) throws IOException {
		int lastRead = Integer.MAX_VALUE;
		while (lastRead >= ind) {
			if (st.cacheOfStrings.containsKey(ind)) {
				return st.cacheOfStrings.get(ind);
			}
			if (ind >= st.windowOffset && (ind - st.windowOffset) < st.window.size()) {
				return st.window.get(ind - st.windowOffset);
			}
			lastRead = readStringTable(st, ind - IndexStringTable.WINDOW_SIZE / 4, IndexStringTable.WINDOW_SIZE, false);
		}
		return null;
	}
	
	private int seekUsingOffsets(IndexStringTable st, int index) throws IOException {
		initStringOffsets(st, index);
		int shift = 0;
		int a = index / IndexStringTable.SIZE_OFFSET_ARRAY;
		if (a > st.offsets.size()) {
			a = st.offsets.size();
		}
		if (a > 0) {
			shift = st.offsets.get(a - 1);
		}
		codedIS.seek(st.fileOffset + shift);
		return index - a * IndexStringTable.SIZE_OFFSET_ARRAY;
	}
	
	private void initStringOffsets(IndexStringTable st, int index) throws IOException {
		if (index > IndexStringTable.SIZE_OFFSET_ARRAY * (st.offsets.size() + 1)) {
			int shift = 0;
			if (!st.offsets.isEmpty()) {
				shift = st.offsets.get(st.offsets.size() - 1);
			}
			codedIS.seek(st.fileOffset + shift);
			int old = codedIS.pushLimit(st.length - shift);
			while (index > IndexStringTable.SIZE_OFFSET_ARRAY * (st.offsets.size() + 1)) {
				int ind = 0;
				while (ind < IndexStringTable.SIZE_OFFSET_ARRAY && ind != -1) {
					int t = codedIS.readTag();
					int tag = WireFormat.getTagFieldNumber(t);
					switch (tag) {
					case 0:
						ind = -1;
						break;
					case OsmandOdb.StringTable.S_FIELD_NUMBER:
						skipUnknownField(t);
						ind++;
						break;
					default:
						skipUnknownField(t);
						break;
					}
				}
				if(ind == IndexStringTable.SIZE_OFFSET_ARRAY){
					st.offsets.add(codedIS.getTotalBytesRead() - st.fileOffset);
				} else {
					// invalid index
					break;
				}
			}
			codedIS.popLimit(old);
		}

	}
	
	private void readTransportBounds(TransportIndex ind) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.TransportStopsTree.LEFT_FIELD_NUMBER :
				ind.left = codedIS.readSInt32();
				break;
			case OsmandOdb.TransportStopsTree.RIGHT_FIELD_NUMBER :
				ind.right = codedIS.readSInt32(); 
				break;
			case OsmandOdb.TransportStopsTree.TOP_FIELD_NUMBER :
				ind.top = codedIS.readSInt32();
				break;
			case OsmandOdb.TransportStopsTree.BOTTOM_FIELD_NUMBER :
				ind.bottom = codedIS.readSInt32(); 
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void searchTransportTreeBounds(int pleft, int pright, int ptop, int pbottom,
			SearchRequest<TransportStop> req) throws IOException {
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
				// coordinates are init
				init = 0;
				if(cright < req.left || cleft > req.right || ctop > req.bottom || cbottom < req.top){
					return;
				} else {
					req.numberOfAcceptedSubtrees++;
				}
			}
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.TransportStopsTree.BOTTOM_FIELD_NUMBER :
				cbottom = codedIS.readSInt32() + pbottom;
				init |= 1;
				break;
			case OsmandOdb.TransportStopsTree.LEFT_FIELD_NUMBER :
				cleft = codedIS.readSInt32() + pleft;
				init |= 2;
				break;
			case OsmandOdb.TransportStopsTree.RIGHT_FIELD_NUMBER :
				cright = codedIS.readSInt32() + pright;
				init |= 4;
				break;
			case OsmandOdb.TransportStopsTree.TOP_FIELD_NUMBER :
				ctop = codedIS.readSInt32() + ptop;
				init |= 8;
				break;
			case OsmandOdb.TransportStopsTree.LEAFS_FIELD_NUMBER :
				int stopOffset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				if(lastIndexResult == -1){
					lastIndexResult = req.searchResults.size();
				}
				req.numberOfVisitedObjects++;
				TransportStop transportStop = readTransportStop(stopOffset, cleft, cright, ctop, cbottom, req);
				if(transportStop != null){
					req.searchResults.add(transportStop);
					
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.TransportStopsTree.SUBTREES_FIELD_NUMBER :
				// left, ... already initialized 
				length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				if (req.limit == -1 || req.limit >= req.searchResults.size()) {
					oldLimit = codedIS.pushLimit(length);
					searchTransportTreeBounds(cleft, cright, ctop, cbottom, req);
					codedIS.popLimit(oldLimit);
				}
				codedIS.seek(filePointer + length);
				
				if(lastIndexResult >= 0){
					throw new IllegalStateException();
				}
				break;
			case OsmandOdb.TransportStopsTree.BASEID_FIELD_NUMBER :
				long baseId = codedIS.readUInt64();
				if (lastIndexResult != -1) {
					for (int i = lastIndexResult; i < req.searchResults.size(); i++) {
						TransportStop rs = req.searchResults.get(i);
						rs.setId(rs.getId() + baseId);
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
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
	
	public net.osmand.data.TransportRoute getTransportRoute(int filePointer) throws IOException {
		TransportIndex ind = getTransportIndex(filePointer);
		if(ind == null){
			return null;
		}
		codedIS.seek(filePointer);
		int routeLength = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(routeLength);
		net.osmand.data.TransportRoute dataObject = new net.osmand.data.TransportRoute();
		boolean end = false;
		int name = -1;
		int nameEn = -1;
		int operator = -1;
		int type = -1;
		long rid = 0;
		int rx = 0;
		int ry = 0;
		long did = 0;
		int dx = 0;
		int dy = 0;
		while(!end){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				end = true;
				break;
			case OsmandOdb.TransportRoute.DISTANCE_FIELD_NUMBER :
				dataObject.setDistance(codedIS.readUInt32());
				break;
			case OsmandOdb.TransportRoute.ID_FIELD_NUMBER :
				dataObject.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.TransportRoute.REF_FIELD_NUMBER :
				dataObject.setRef(codedIS.readString());
				break;
			case OsmandOdb.TransportRoute.TYPE_FIELD_NUMBER :
				type = codedIS.readUInt32();
				break;
			case OsmandOdb.TransportRoute.NAME_EN_FIELD_NUMBER :
				nameEn = codedIS.readUInt32();
				break;
			case OsmandOdb.TransportRoute.NAME_FIELD_NUMBER :
				name = codedIS.readUInt32();
				break;
			case OsmandOdb.TransportRoute.OPERATOR_FIELD_NUMBER:
				operator = codedIS.readUInt32();
				break;
			case OsmandOdb.TransportRoute.REVERSESTOPS_FIELD_NUMBER:
				int length = codedIS.readRawVarint32();
				int olds = codedIS.pushLimit(length);
				TransportStop stop = readTransportRouteStop(dx, dy, did);
				dataObject.getBackwardStops().add(stop);
				did = stop.getId();
				dx = (int) MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, stop.getLocation().getLongitude());
				dy = (int) MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, stop.getLocation().getLatitude());
				codedIS.popLimit(olds);
				break;
			case OsmandOdb.TransportRoute.DIRECTSTOPS_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				olds = codedIS.pushLimit(length);
				stop = readTransportRouteStop(rx, ry, rid);
				dataObject.getForwardStops().add(stop);
				rid = stop.getId();
				rx = (int) MapUtils.getTileNumberX(TRANSPORT_STOP_ZOOM, stop.getLocation().getLongitude());
				ry = (int) MapUtils.getTileNumberY(TRANSPORT_STOP_ZOOM, stop.getLocation().getLatitude());
				codedIS.popLimit(olds);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		codedIS.popLimit(old);
		if(name != -1){
			dataObject.setName(getStringFromStringTable(ind.stringTable, name));
		}
		if(nameEn != -1){
			dataObject.setEnName(getStringFromStringTable(ind.stringTable, nameEn));
		} else {
			dataObject.setEnName(Junidecode.unidecode(dataObject.getName()));
		}
		
		if(operator != -1){
			dataObject.setOperator(getStringFromStringTable(ind.stringTable, operator));
		}
		if(type != -1){
			dataObject.setType(getStringFromStringTable(ind.stringTable, type));
		}
		for (int i = 0; i < 2; i++) {
			List<TransportStop> stops = i == 0 ? dataObject.getForwardStops() : dataObject.getBackwardStops();
			for (TransportStop s : stops) {
				if (s.getName().length() > 0) {
					s.setName(getStringFromStringTable(ind.stringTable, s.getName().charAt(0)));
				}
				if (s.getEnName().length() > 0) {
					s.setEnName(getStringFromStringTable(ind.stringTable, s.getEnName().charAt(0)));
				} else {
					s.setEnName(Junidecode.unidecode(s.getName()));
				}

			}
		}
		
		
		return dataObject;
	}

	
	
	private TransportStop readTransportRouteStop(int dx, int dy, long did) throws IOException {
		TransportStop dataObject = new TransportStop();
		boolean end = false;
		while(!end){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(dataObject.getEnName().length() == 0){
					dataObject.setEnName(Junidecode.unidecode(dataObject.getName()));
				}
				end = true;
				break;
			case OsmandOdb.TransportRouteStop.NAME_EN_FIELD_NUMBER :
				dataObject.setEnName(""+((char) codedIS.readUInt32())); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRouteStop.NAME_FIELD_NUMBER :
				dataObject.setName(""+((char) codedIS.readUInt32())); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRouteStop.ID_FIELD_NUMBER :
				did += codedIS.readSInt64();
				break;
			case OsmandOdb.TransportRouteStop.DX_FIELD_NUMBER :
				dx += codedIS.readSInt32();
				break;
			case OsmandOdb.TransportRouteStop.DY_FIELD_NUMBER :
				dy += codedIS.readSInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		dataObject.setId(did);
		dataObject.setLocation(MapUtils.getLatitudeFromTile(TRANSPORT_STOP_ZOOM, dy), MapUtils.getLongitudeFromTile(TRANSPORT_STOP_ZOOM, dx));
		return dataObject;
	}
	
	private TransportStop readTransportStop(int shift, int cleft, int cright, int ctop, int cbottom, SearchRequest<TransportStop> req) throws IOException {
		int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.TransportStop.DX_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		int x = codedIS.readSInt32() + cleft;
		
		tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.TransportStop.DY_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		int y = codedIS.readSInt32() + ctop;
		if(req.right < x || req.left > x || req.top > y || req.bottom < y){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		req.numberOfAcceptedObjects++;
		req.cacheTypes.clear();
		
		TransportStop dataObject = new TransportStop();
		dataObject.setLocation(MapUtils.getLatitudeFromTile(TRANSPORT_STOP_ZOOM, y), MapUtils.getLongitudeFromTile(TRANSPORT_STOP_ZOOM, x));
		dataObject.setFileOffset(shift);
		while(true){
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				dataObject.setReferencesToRoutes(req.cacheTypes.toArray());
				if(dataObject.getEnName().length() == 0){
					dataObject.setEnName(Junidecode.unidecode(dataObject.getName()));
				}
				return dataObject;
			case OsmandOdb.TransportStop.ROUTES_FIELD_NUMBER :
				req.cacheTypes.add(shift - codedIS.readUInt32());
				break;
			case OsmandOdb.TransportStop.NAME_EN_FIELD_NUMBER :
				dataObject.setEnName(""+((char) codedIS.readUInt32())); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportStop.NAME_FIELD_NUMBER :
				int i = codedIS.readUInt32();
				dataObject.setName(""+((char) i)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportStop.ID_FIELD_NUMBER :
				dataObject.setId(codedIS.readSInt64());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readAddressIndex(AddressRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndAddressIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				if(region.enName == null){
					region.enName = Junidecode.unidecode(region.name);
				}
				break;
			case OsmandOdb.OsmAndAddressIndex.NAME_EN_FIELD_NUMBER :
				region.enName = codedIS.readString();
				break;
			case OsmandOdb.OsmAndAddressIndex.CITIES_FIELD_NUMBER :
				region.citiesOffset = codedIS.getTotalBytesRead();
				int length = readInt();
				codedIS.seek(region.citiesOffset + length + 4);
				break;
			case OsmandOdb.OsmAndAddressIndex.VILLAGES_FIELD_NUMBER :
				region.villagesOffset = codedIS.getTotalBytesRead();
				length = readInt();
				codedIS.seek(region.villagesOffset + length + 4);
				break;
			case OsmandOdb.OsmAndAddressIndex.POSTCODES_FIELD_NUMBER :
				region.postcodesOffset = codedIS.getTotalBytesRead();
				length = readInt();
				codedIS.seek(region.postcodesOffset + length + 4);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readMapIndex(MapIndex index) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
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
				if(!index.encodingRules.containsKey(tags)){
					index.encodingRules.put(tags, new LinkedHashMap<String, Integer>());
				}
				index.encodingRules.get(tags).put(val, ((subtype << 5) | type));
				return;
			case OsmandOdb.MapEncodingRule.VALUE_FIELD_NUMBER :
				val = codedIS.readString();
				break;
			case OsmandOdb.MapEncodingRule.TAG_FIELD_NUMBER :
				tags = codedIS.readString();
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
						searchMapTreeBounds(index.left, index.right, index.top, index.bottom, req);
						codedIS.popLimit(oldLimit);
					}
				}
			}
		}
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
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
			searchTransportTreeBounds(0, 0, 0, 0, req);
			codedIS.popLimit(oldLimit);
			for (int i = offset; i < req.searchResults.size(); i++) {
				TransportStop st = req.searchResults.get(i);
				if (st.getName().length() != 0) {
					st.setName(getStringFromStringTable(index.stringTable, st.getName().charAt(0)));
				}
				if (st.getEnName().length() != 0) {
					st.setEnName(getStringFromStringTable(index.stringTable, st.getEnName().charAt(0)));
				} else {
					st.setEnName(Junidecode.unidecode(st.getName()));
				}
			}
		}
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
	}
	
	public boolean hasTransportData(){
		return transportIndexes.size() > 0;
	}
	
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
			readPostcodes(postcodes);
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
			PostCode p = findPostcode(name);
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
			readCities(cities, null, false);
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
			readCities(cities, nameContains, useEn);
			codedIS.popLimit(old);
		}
		return cities;
	}
	
	private void readPostcodes(List<PostCode> postcodes) throws IOException{
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.PostcodesIndex.POSTCODES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				postcodes.add(readPostcode(null, offset, false, null));
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private PostCode findPostcode(String name) throws IOException{
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return null;
			case OsmandOdb.PostcodesIndex.POSTCODES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				PostCode p = readPostcode(null, offset, true, name);
				codedIS.popLimit(oldLimit);
				if(p != null){
					return p;
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readCities(List<City> cities, String nameContains, boolean useEn) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.CitiesIndex.CITIES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				
				int oldLimit = codedIS.pushLimit(length);
				if(nameContains != null){
					String name = null;
					int read = 0;
					int toRead = useEn ? 3 : 2;
					int seek = codedIS.getTotalBytesRead();
					while(read++ < toRead){
						int ts = codedIS.readTag();
						int tags = WireFormat.getTagFieldNumber(ts);
						switch (tags) {
						case OsmandOdb.CityIndex.NAME_EN_FIELD_NUMBER :
							name = codedIS.readString();
							break;
						case OsmandOdb.CityIndex.NAME_FIELD_NUMBER :
							name = codedIS.readString();
							if(useEn){
								name = Junidecode.unidecode(name);
							}
							break;
						case OsmandOdb.CityIndex.CITY_TYPE_FIELD_NUMBER :
							codedIS.readUInt32();
							break;
						}
					}
					if(name == null || !name.toLowerCase().contains(nameContains)){
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						codedIS.popLimit(oldLimit);
						break;
					}
					codedIS.seek(seek);
				}
				
				City c = readCity(null, offset, false);
				if(c != null){
					cities.add(c);
				}
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	public void preloadStreets(City c) throws IOException {
		checkAddressIndex(c.getFileOffset());
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		readCity(c, c.getFileOffset(), true);
		codedIS.popLimit(old);
	}
	
	public List<Street> findIntersectedStreets(City c, Street s, List<Street> streets) throws IOException {
		findIntersectedStreets(c, s, null, streets);
		return streets;
	}
	
	public LatLon findStreetIntersection(City c, Street s, Street s2) throws IOException {
		return findIntersectedStreets(c, s, s2, null);
	}
	
	// do not preload streets in city
	protected  LatLon findIntersectedStreets(City c, Street s, Street s2, List<Street> streets) throws IOException {
		checkAddressIndex(c.getFileOffset());
		if(s.getIndexInCity() == -1){
			return null;
		}
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				codedIS.popLimit(old);
				return null;
			case OsmandOdb.CityIndex.INTERSECTIONS_FIELD_NUMBER :
				Street[] cityStreets = new Street[c.getStreets().size()];
				for(Street st : c.getStreets()){
					if(st.getIndexInCity() >= 0 && st.getIndexInCity() < cityStreets.length){
						cityStreets[st.getIndexInCity()] = st;
					}
				}
				LatLon ret = readIntersectedStreets(cityStreets, s, s2, c.getLocation(), streets);
				codedIS.popLimit(old);
				return ret;
			default:
				skipUnknownField(t);
			}
		}
		
	}
	
	// 2 different quires : s2 == null -> fill possible streets, s2 != null return LatLon intersection 
	private LatLon readIntersectedStreets(Street[] cityStreets, Street s, Street s2, LatLon parent, List<Street> streets) throws IOException {
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		boolean e = false;
		while(!e){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				e = true;
				break;
			case OsmandOdb.InteresectedStreets.INTERSECTIONS_FIELD_NUMBER:
				int nsize = codedIS.readRawVarint32();
				int nold = codedIS.pushLimit(nsize);
				int st1 = -1;
				int st2 = -1;
				int cx = 0;
				int cy = 0;
				boolean end = false;
				while (!end) {
					int nt = codedIS.readTag();
					int ntag = WireFormat.getTagFieldNumber(nt);
					switch (ntag) {
					case 0:
						end = true;
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDSTREET1_FIELD_NUMBER:
						st1 = codedIS.readUInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDSTREET2_FIELD_NUMBER:
						st2 = codedIS.readUInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDX_FIELD_NUMBER:
						cx = codedIS.readSInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDY_FIELD_NUMBER:
						cy = codedIS.readSInt32();
						break;
					default:
						skipUnknownField(nt);
					}
				}
				codedIS.popLimit(nold);
				if (s2 == null) {
					// find all intersections
					if (st1 == s.getIndexInCity() && st2 != -1 && st2 < cityStreets.length && cityStreets[st2] != null) {
						streets.add(cityStreets[st2]);
					} else if (st2 == s.getIndexInCity() && st1 != -1 && st1 < cityStreets.length && cityStreets[st1] != null) {
						streets.add(cityStreets[st1]);
					}
				} else {
					if((st1 == s.getIndexInCity() && st2 == s2.getIndexInCity() ) || 
							(st2 == s.getIndexInCity() && st1 == s2.getIndexInCity())) {
						int x = (int) (MapUtils.getTileNumberX(24, parent.getLongitude()) + cx);
						int y = (int) (MapUtils.getTileNumberY(24, parent.getLatitude()) + cy);
						codedIS.popLimit(old);
						return new LatLon(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
					}
				}
				
				break;
			default:
				skipUnknownField(t);
			}
		}
		codedIS.popLimit(old);
		return null;
	}

	public void preloadStreets(PostCode p) throws IOException {
		checkAddressIndex(p.getFileOffset());
		
		codedIS.seek(p.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		readPostcode(p, p.getFileOffset(), true, null);
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
		readStreet(s, true, 0, 0, null);
		codedIS.popLimit(old);
	}
	
	
	private PostCode readPostcode(PostCode p, int fileOffset, boolean loadStreets, String postcodeFilter) throws IOException{
		int x = 0;
		int y = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				p.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				p.setFileOffset(fileOffset);
				return p;
			case OsmandOdb.PostcodeIndex.POSTCODE_FIELD_NUMBER :
				String name = codedIS.readString();
				if(postcodeFilter != null && !postcodeFilter.equalsIgnoreCase(name)){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
				if(p == null){
					p = new PostCode(name);
				}
				p.setName(name);
				break;
			case OsmandOdb.PostcodeIndex.X_FIELD_NUMBER :
				x = codedIS.readFixed32();
				break;
			case OsmandOdb.PostcodeIndex.Y_FIELD_NUMBER :
				y = codedIS.readFixed32();
				break;
			case OsmandOdb.PostcodeIndex.STREETS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadStreets){
					Street s = new Street(null);
					int oldLimit = codedIS.pushLimit(length);
					s.setFileOffset(offset);
					readStreet(s, true, x >> 7, y >> 7, p.getName());
					p.registerStreet(s, false);
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
	private City readCity(City c, int fileOffset, boolean loadStreets) throws IOException{
		int x = 0;
		int y = 0;
		int streetInd = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				c.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				return c;
			case OsmandOdb.CityIndex.CITY_TYPE_FIELD_NUMBER :
				int type = codedIS.readUInt32();
				if(c == null){
					c = new City(CityType.values()[type]);
					c.setFileOffset(fileOffset);
				}
				break;
			case OsmandOdb.CityIndex.ID_FIELD_NUMBER :
				c.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.CityIndex.NAME_EN_FIELD_NUMBER :
				c.setEnName(codedIS.readString());
				break;
			case OsmandOdb.CityIndex.NAME_FIELD_NUMBER :
				c.setName(codedIS.readString());
				if(c.getEnName().length() == 0){
					c.setEnName(Junidecode.unidecode(c.getName()));
				}
				break;
			case OsmandOdb.CityIndex.X_FIELD_NUMBER :
				x = codedIS.readFixed32();
				break;
			case OsmandOdb.CityIndex.Y_FIELD_NUMBER :
				y = codedIS.readFixed32();
				break;
			case OsmandOdb.CityIndex.INTERSECTIONS_FIELD_NUMBER :
				codedIS.skipRawBytes(codedIS.readRawVarint32());
				break;
			case OsmandOdb.CityIndex.STREETS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadStreets){
					Street s = new Street(c);
					int oldLimit = codedIS.pushLimit(length);
					s.setFileOffset(offset);
					s.setIndexInCity(streetInd++);
					readStreet(s, false, x >> 7, y >> 7, null);
					c.registerStreet(s);
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private Street readStreet(Street s, boolean loadBuildings, int city24X, int city24Y, String postcodeFilter) throws IOException{
		int x = 0;
		int y = 0;
		boolean loadLocation = city24X != 0 || city24Y != 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(loadLocation){
					s.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				}
				return s;
			case OsmandOdb.StreetIndex.ID_FIELD_NUMBER :
				s.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.StreetIndex.NAME_EN_FIELD_NUMBER :
				s.setEnName(codedIS.readString());
				break;
			case OsmandOdb.StreetIndex.NAME_FIELD_NUMBER :
				s.setName(codedIS.readString());
				if(s.getEnName().length() == 0){
					s.setEnName(Junidecode.unidecode(s.getName()));
				}
				break;
			case OsmandOdb.StreetIndex.X_FIELD_NUMBER :
				int sx = codedIS.readSInt32();
				if(loadLocation){
					x =  sx + city24X;
				} else {
					x = (int) MapUtils.getTileNumberX(24, s.getLocation().getLongitude());
				}
				break;
			case OsmandOdb.StreetIndex.Y_FIELD_NUMBER :
				int sy = codedIS.readSInt32();
				if(loadLocation){
					y =  sy + city24Y;
				} else {
					y = (int) MapUtils.getTileNumberY(24, s.getLocation().getLatitude());
				}
				break;
			case OsmandOdb.StreetIndex.BUILDINGS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadBuildings){
					int oldLimit = codedIS.pushLimit(length);
					Building b = readBuilding(offset, x, y);
					if (postcodeFilter == null || postcodeFilter.equalsIgnoreCase(b.getPostcode())) {
						s.registerBuilding(b);
					}
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private Building readBuilding(int fileOffset, int street24X, int street24Y) throws IOException{
		int x = 0;
		int y = 0;
		Building b = new Building();
		b.setFileOffset(fileOffset);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				b.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				return b;
			case OsmandOdb.BuildingIndex.ID_FIELD_NUMBER :
				b.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.BuildingIndex.NAME_EN_FIELD_NUMBER :
				b.setEnName(codedIS.readString());
				break;
			case OsmandOdb.BuildingIndex.NAME_FIELD_NUMBER :
				b.setName(codedIS.readString());
				if(b.getEnName().length() == 0){
					b.setEnName(Junidecode.unidecode(b.getName()));
				}
				break;
			case OsmandOdb.BuildingIndex.X_FIELD_NUMBER :
				x =  codedIS.readSInt32() + street24X;
				break;
			case OsmandOdb.BuildingIndex.Y_FIELD_NUMBER :
				y =  codedIS.readSInt32() + street24Y;
				break;
			case OsmandOdb.BuildingIndex.POSTCODE_FIELD_NUMBER :
				b.setPostcode(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void searchMapTreeBounds(int pleft, int pright, int ptop, int pbottom,
			SearchRequest<BinaryMapDataObject> req) throws IOException {
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
				BinaryMapDataObject mapObject = readMapDataObject(cleft, cright, ctop, cbottom, req);
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
				searchMapTreeBounds(cleft, cright, ctop, cbottom, req);
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
	private BinaryMapDataObject readMapDataObject(int left, int right, int top, int bottom, SearchRequest<BinaryMapDataObject> req) throws IOException {
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
			accept = req.searchFilter.accept(req.cacheTypes);
		}
		
		
		if(!accept){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		req.numberOfAcceptedObjects++;
		
		BinaryMapDataObject dataObject = new BinaryMapDataObject();		
		dataObject.coordinates = req.cacheCoordinates.toArray();
		dataObject.types = req.cacheTypes.toArray();
		
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
		
		public boolean accept(TIntArrayList types);
		
	}
	
	public static class SearchRequest<T> {
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
	}
	
	
	public static class MapIndex extends BinaryIndexPart {
		List<MapRoot> roots = new ArrayList<MapRoot>();
		Map<String, Map<String, Integer>> encodingRules = new LinkedHashMap<String, Map<String,Integer>>();
		
		public List<MapRoot> getRoots() {
			return roots;
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
	
	public static class TransportIndex extends BinaryIndexPart {
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		int stopsFileOffset = 0;
		int stopsFileLength = 0;
		
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
		
		IndexStringTable stringTable = null;
	}
	
	private static class IndexStringTable {
		private static final int SIZE_OFFSET_ARRAY = 100; 
		private static final int WINDOW_SIZE = 25;
		int fileOffset = 0;
		int length = 0;
		
		// offset from start for each SIZE_OFFSET_ARRAY elements
		// (SIZE_OFFSET_ARRAY + 1) offset : offsets[0] + skipOneString()
		TIntArrayList offsets = new TIntArrayList();
		Map<Integer, String> cacheOfStrings = new LinkedHashMap<Integer, String>();
		
		int windowOffset = 0;
		List<String> window = new ArrayList<String>();
	}
	
	
	public static class AddressRegion extends BinaryIndexPart {
		String enName;
		
		int postcodesOffset = -1;
		int villagesOffset = -1;
		int citiesOffset = -1;
	}
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Minsk.obf"), "r");
//		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Belarus_4.obf"), "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		System.out.println("VERSION " + reader.getVersion());
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
		
		System.out.println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
		System.out.println("Time " + (System.currentTimeMillis() - time));
	}
	
}