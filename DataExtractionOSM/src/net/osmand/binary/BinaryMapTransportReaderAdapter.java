package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import static net.osmand.binary.BinaryMapIndexReader.TRANSPORT_STOP_ZOOM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.TransportStop;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapTransportReaderAdapter {
	private CodedInputStreamRAF codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapTransportReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private int readInt() throws IOException {
		return map.readInt();
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
		// (SIZE_OFFSET_ARRAY + 1) offset = offsets[0] + skipOneString()
		TIntArrayList offsets = new TIntArrayList();
		TIntObjectMap<String> cacheOfStrings = new TIntObjectHashMap<String>();

		int windowOffset = 0;
		List<String> window = new ArrayList<String>();
	}
	
	
	protected void readTransportIndex(TransportIndex ind) throws IOException {
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
				// Do not cache for now save memory
				// readStringTable(st, 0, 20, true);
				ind.stringTable = st;
				codedIS.seek(st.length + st.fileOffset);
				break;
			default:
				skipUnknownField(t);
				break;
			}
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
			int startOffset = ind - IndexStringTable.WINDOW_SIZE / 4;
			if(startOffset < 0){
				startOffset = 0;
			}
			lastRead = readStringTable(st, startOffset, IndexStringTable.WINDOW_SIZE, false);
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
	
	protected void searchTransportTreeBounds(int pleft, int pright, int ptop, int pbottom,
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
	
	public net.osmand.data.TransportRoute getTransportRoute(int filePointer, TransportIndex ind) throws IOException {
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
	
}
