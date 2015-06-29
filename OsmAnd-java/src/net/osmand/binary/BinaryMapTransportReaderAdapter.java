package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.TransportStop;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

public class BinaryMapTransportReaderAdapter {
	private CodedInputStream codedIS;
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

	protected static class IndexStringTable {
		int fileOffset = 0;
		int length = 0;

		// offset from start for each SIZE_OFFSET_ARRAY elements
		// (SIZE_OFFSET_ARRAY + 1) offset = offsets[0] + skipOneString()
		TIntArrayList offsets = new TIntArrayList();

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
			if(req.isCancelled()){
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
					lastIndexResult = req.getSearchResults().size();
				}
				req.numberOfVisitedObjects++;
				TransportStop transportStop = readTransportStop(stopOffset, cleft, cright, ctop, cbottom, req);
				if(transportStop != null){
					req.publish(transportStop);
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.TransportStopsTree.SUBTREES_FIELD_NUMBER :
				// left, ... already initialized 
				length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				if (req.limit == -1 || req.limit >= req.getSearchResults().size()) {
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
					for (int i = lastIndexResult; i < req.getSearchResults().size(); i++) {
						TransportStop rs = req.getSearchResults().get(i);
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
	
	private String regStr(TIntObjectHashMap<String> stringTable) throws IOException{
		int i = codedIS.readUInt32();
		stringTable.putIfAbsent(i, "");
		return ((char) i)+"";
	}
	
	public net.osmand.data.TransportRoute getTransportRoute(int filePointer, TIntObjectHashMap<String> stringTable,
			boolean onlyDescription) throws IOException {
		codedIS.seek(filePointer);
		int routeLength = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(routeLength);
		net.osmand.data.TransportRoute dataObject = new net.osmand.data.TransportRoute();
		boolean end = false;
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
				dataObject.setType(regStr(stringTable)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRoute.NAME_EN_FIELD_NUMBER :
				dataObject.setEnName(regStr(stringTable)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRoute.NAME_FIELD_NUMBER :
				dataObject.setName(regStr(stringTable)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRoute.OPERATOR_FIELD_NUMBER:
				dataObject.setOperator(regStr(stringTable)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRoute.REVERSESTOPS_FIELD_NUMBER:
				if(onlyDescription){
					end = true;
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					break;
				}
				int length = codedIS.readRawVarint32();
				int olds = codedIS.pushLimit(length);
				TransportStop stop = readTransportRouteStop(dx, dy, did, stringTable);
				dataObject.getBackwardStops().add(stop);
				did = stop.getId();
				dx = (int) MapUtils.getTileNumberX(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, stop.getLocation().getLongitude());
				dy = (int) MapUtils.getTileNumberY(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, stop.getLocation().getLatitude());
				codedIS.popLimit(olds);
				break;
			case OsmandOdb.TransportRoute.DIRECTSTOPS_FIELD_NUMBER:
				if(onlyDescription){
					end = true;
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					break;
				}
				length = codedIS.readRawVarint32();
				olds = codedIS.pushLimit(length);
				stop = readTransportRouteStop(rx, ry, rid, stringTable);
				dataObject.getForwardStops().add(stop);
				rid = stop.getId();
				rx = (int) MapUtils.getTileNumberX(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, stop.getLocation().getLongitude());
				ry = (int) MapUtils.getTileNumberY(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, stop.getLocation().getLatitude());
				codedIS.popLimit(olds);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		codedIS.popLimit(old);
		
		
		return dataObject;
	}
	
	protected void initializeStringTable(TransportIndex ind, TIntObjectHashMap<String> stringTable) throws IOException {
		int[] values = stringTable.keys();
		Arrays.sort(values);
		codedIS.seek(ind.stringTable.fileOffset);
		int oldLimit = codedIS.pushLimit(ind.stringTable.length);
		int current = 0;
		int i = 0;
		while (i < values.length) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				break;
			case OsmandOdb.StringTable.S_FIELD_NUMBER:
				if (current == values[i]) {
					String value = codedIS.readString();
					stringTable.put(values[i], value);
					i++;
				} else {
					skipUnknownField(t);
				}
				current ++;
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		codedIS.popLimit(oldLimit);
	}

	protected void initializeNames(boolean onlyDescription, net.osmand.data.TransportRoute dataObject,
			TIntObjectHashMap<String> stringTable) throws IOException {
		if(dataObject.getName().length() > 0){
			dataObject.setName(stringTable.get(dataObject.getName().charAt(0)));
		}
		if(dataObject.getEnName(false).length() > 0){
			dataObject.setEnName(stringTable.get(dataObject.getEnName(false).charAt(0)));
		}
		if(dataObject.getName().length() > 0 && dataObject.getName("en").length() == 0){
			dataObject.setEnName(Junidecode.unidecode(dataObject.getName()));
		}
		
		if(dataObject.getOperator().length() > 0){
			dataObject.setOperator(stringTable.get(dataObject.getOperator().charAt(0)));
		}
		if(dataObject.getType().length() > 0){
			dataObject.setType(stringTable.get(dataObject.getType().charAt(0)));
		}
		for (int i = 0; i < 2 && !onlyDescription; i++) {
			List<TransportStop> stops = i == 0 ? dataObject.getForwardStops() : dataObject.getBackwardStops();
			for (TransportStop s : stops) {
				initializeNames(stringTable, s);
			}
		}
	}

	protected void initializeNames(TIntObjectHashMap<String> stringTable, TransportStop s) {
		if (s.getName().length() > 0) {
			s.setName(stringTable.get(s.getName().charAt(0)));
		}
		if (s.getEnName(false).length() > 0) {
			s.setEnName(stringTable.get(s.getEnName(false).charAt(0)));
		}
	}

	
	
	private TransportStop readTransportRouteStop(int dx, int dy, long did, TIntObjectHashMap<String> stringTable) throws IOException {
		TransportStop dataObject = new TransportStop();
		boolean end = false;
		while(!end){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				end = true;
				break;
			case OsmandOdb.TransportRouteStop.NAME_EN_FIELD_NUMBER :
				dataObject.setEnName(regStr(stringTable)); //$NON-NLS-1$
				break;
			case OsmandOdb.TransportRouteStop.NAME_FIELD_NUMBER :
				dataObject.setName(regStr(stringTable)); //$NON-NLS-1$
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
		dataObject.setLocation(MapUtils.getLatitudeFromTile(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, dy), MapUtils.getLongitudeFromTile(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, dx));
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
		dataObject.setLocation(MapUtils.getLatitudeFromTile(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, y), MapUtils.getLongitudeFromTile(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, x));
		dataObject.setFileOffset(shift);
		while(true){
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				dataObject.setReferencesToRoutes(req.cacheTypes.toArray());
				if(dataObject.getName("en").length() == 0){
					dataObject.setEnName(Junidecode.unidecode(dataObject.getName()));
				}
				return dataObject;
			case OsmandOdb.TransportStop.ROUTES_FIELD_NUMBER :
				req.cacheTypes.add(shift - codedIS.readUInt32());
				break;
			case OsmandOdb.TransportStop.NAME_EN_FIELD_NUMBER :
				if (req.stringTable != null) {
					dataObject.setEnName(regStr(req.stringTable)); //$NON-NLS-1$
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.TransportStop.NAME_FIELD_NUMBER :
				if (req.stringTable != null) {
					dataObject.setName(regStr(req.stringTable)); //$NON-NLS-1$
				} else {
					skipUnknownField(t);
				}
				
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
