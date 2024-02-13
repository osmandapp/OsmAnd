package net.osmand.binary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

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
	
	private long readInt() throws IOException {
		return map.readInt();
	}

	public static class TransportIndex extends BinaryIndexPart {
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;

		long stopsFileOffset = 0;
		long stopsFileLength = 0;
		long incompleteRoutesOffset = 0;
		long incompleteRoutesLength = 0;
		
		public String getPartName() {
			return "Transport";
		}

		public int getFieldNumber() {
			return OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER;
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
		
		IndexStringTable stringTable = null;
	}

	protected static class IndexStringTable {
		long fileOffset = 0;
		int length = 0;
		TIntObjectHashMap<String> stringTable = null;

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
				long old = codedIS.pushLimitLong((long) ind.stopsFileLength);
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
			case OsmandOdb.OsmAndTransportIndex.INCOMPLETEROUTES_FIELD_NUMBER :
				ind.incompleteRoutesLength = codedIS.readRawVarint32();
				ind.incompleteRoutesOffset = codedIS.getTotalBytesRead();
				codedIS.seek(ind.incompleteRoutesLength + ind.incompleteRoutesOffset);
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
			SearchRequest<TransportStop> req, TIntObjectHashMap<String> stringTable) throws IOException {
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
				long stopOffset = codedIS.getTotalBytesRead();
				long length = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) length);
				if(lastIndexResult == -1){
					lastIndexResult = req.getSearchResults().size();
				}
				req.numberOfVisitedObjects++;
				TransportStop transportStop = readTransportStop(stopOffset, cleft, cright, ctop, cbottom, req, stringTable);
				if(transportStop != null){
					req.publish(transportStop);
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.TransportStopsTree.SUBTREES_FIELD_NUMBER :
				// left, ... already initialized 
				length = readInt();
				long filePointer = codedIS.getTotalBytesRead();
				if (req.limit == -1 || req.limit >= req.getSearchResults().size()) {
					oldLimit = codedIS.pushLimitLong((long) length);
					searchTransportTreeBounds(cleft, cright, ctop, cbottom, req, stringTable);
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

	private String regStr(TIntObjectHashMap<String> stringTable, int i) throws IOException{
		stringTable.putIfAbsent(i, "");
		return ((char) i)+"";
	}
	
	public void readIncompleteRoutesList(TLongObjectHashMap<net.osmand.data.IncompleteTransportRoute> incompleteRoutes, long transportIndexStart) throws IOException {
		boolean end = false;
		while (!end) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				end = true;
				break;
			case OsmandOdb.IncompleteTransportRoutes.ROUTES_FIELD_NUMBER:
				int l = codedIS.readRawVarint32();
				long olds = codedIS.pushLimitLong((long) l);
				net.osmand.data.IncompleteTransportRoute ir = readIncompleteRoute(transportIndexStart);
				net.osmand.data.IncompleteTransportRoute itr = incompleteRoutes.get(ir.getRouteId());
				if(itr != null) {
					itr.setNextLinkedRoute(ir);
				} else {
					incompleteRoutes.put(ir.getRouteId(), ir);
				}
				codedIS.popLimit(olds);
				break;
			default:
				skipUnknownField(t);
				break;
			}
			
		}
		
	}
	
	public net.osmand.data.IncompleteTransportRoute readIncompleteRoute(long transportIndexStart) throws IOException {
		net.osmand.data.IncompleteTransportRoute dataObject = new net.osmand.data.IncompleteTransportRoute();
		boolean end = false;
		while(!end){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				end = true;
				break;
			case OsmandOdb.IncompleteTransportRoute.ID_FIELD_NUMBER :
				dataObject.setRouteId(codedIS.readUInt64());
				break;
			case OsmandOdb.IncompleteTransportRoute.ROUTEREF_FIELD_NUMBER :
				int delta = codedIS.readRawVarint32();
				if (delta > transportIndexStart) {
					dataObject.setRouteOffset(delta);
				} else {
					dataObject.setRouteOffset(transportIndexStart + delta);
				}
				break;
			case OsmandOdb.IncompleteTransportRoute.OPERATOR_FIELD_NUMBER :
				skipUnknownField(t);
//				dataObject.setOperator(regStr(stringTable));
				break;
			case OsmandOdb.IncompleteTransportRoute.REF_FIELD_NUMBER :
				skipUnknownField(t);
//				dataObject.setRef(regStr(stringTable));
				break;
			case OsmandOdb.IncompleteTransportRoute.TYPE_FIELD_NUMBER :
				skipUnknownField(t);
//				dataObject.setType(regStr(stringTable));
				break;
			case OsmandOdb.IncompleteTransportRoute.MISSINGSTOPS_FIELD_NUMBER :
// 			    dataObject.getMissingStops().add(codedIS.readSInt32()); //skip for now
				skipUnknownField(t);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		return dataObject;
	}
	
	public net.osmand.data.TransportRoute getTransportRoute(long filePointer, TIntObjectHashMap<String> stringTable,
			boolean onlyDescription) throws IOException {
		codedIS.seek(filePointer);
		int routeLength = codedIS.readRawVarint32();
		long old = codedIS.pushLimitLong((long) routeLength);
		net.osmand.data.TransportRoute dataObject = new net.osmand.data.TransportRoute();
		dataObject.setFileOffset(filePointer);
		boolean end = false;
		long rid = 0;
		int[] rx = new int[] {0};
		int[] ry = new int[] {0};
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
			case OsmandOdb.TransportRoute.COLOR_FIELD_NUMBER:
				dataObject.setColor(regStr(stringTable));
				break;
			case OsmandOdb.TransportRoute.GEOMETRY_FIELD_NUMBER:
				int sizeL = codedIS.readRawVarint32();
				long pold = codedIS.pushLimitLong((long) sizeL);
				int px = 0; 
				int py = 0;
				Way w = new Way(-1);
				while (codedIS.getBytesUntilLimit() > 0) {
					int ddx = (codedIS.readSInt32() << BinaryMapIndexReader.SHIFT_COORDINATES);
					int ddy = (codedIS.readSInt32() << BinaryMapIndexReader.SHIFT_COORDINATES);
					if(ddx == 0 && ddy == 0) {
						if(w.getNodes().size() > 0) {
							dataObject.addWay(w);
						}
						w = new Way(-1);
					} else {
						int x = ddx + px;
						int y = ddy + py;
						w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1));
						px = x;
						py = y;
					}
				}
				if(w.getNodes().size() > 0) {
					dataObject.addWay(w);
				}
				codedIS.popLimit(pold);
				break;
			// deprecated
//			case OsmandOdb.TransportRoute.REVERSESTOPS_FIELD_NUMBER:
//				break;
			case OsmandOdb.TransportRoute.SCHEDULETRIP_FIELD_NUMBER:
				sizeL = codedIS.readRawVarint32();
				pold = codedIS.pushLimitLong((long) sizeL);
				readTransportSchedule(dataObject.getOrCreateSchedule());
				codedIS.popLimit(pold);
				break;
			case OsmandOdb.TransportRoute.DIRECTSTOPS_FIELD_NUMBER:
				if(onlyDescription){
					end = true;
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					break;
				}
				int length = codedIS.readRawVarint32();
				long olds = codedIS.pushLimitLong((long) length);
				TransportStop stop = readTransportRouteStop(rx, ry, rid, stringTable, filePointer);
				dataObject.getForwardStops().add(stop);
				rid = stop.getId();
				codedIS.popLimit(olds);
				break;
			case OsmandOdb.TransportRoute.ATTRIBUTETAGIDS_FIELD_NUMBER:
				String str = regStr(stringTable);
				dataObject.addTag(str, "");
				break;
			case OsmandOdb.TransportRoute.ATTRIBUTETEXTTAGVALUES_FIELD_NUMBER:
				TByteArrayList buf = new TByteArrayList();
				sizeL = codedIS.readRawVarint32();
				olds = codedIS.pushLimitLong((long) sizeL);
				String key = regStr(stringTable);
				while (codedIS.getBytesUntilLimit() > 0) {
					buf.add(codedIS.readRawByte());
				}
				codedIS.popLimit(olds);
				dataObject.addTag(key, new String(buf.toArray(), StandardCharsets.UTF_8));
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		codedIS.popLimit(old);
		
		
		return dataObject;
	}
	
	private void readTransportSchedule(TransportSchedule schedule) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int interval;
			long sizeL, old;
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.TransportRouteSchedule.TRIPINTERVALS_FIELD_NUMBER:
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					interval = codedIS.readRawVarint32();
					schedule.tripIntervals.add(interval);
				}
				codedIS.popLimit(old);				
				break;
			case OsmandOdb.TransportRouteSchedule.AVGSTOPINTERVALS_FIELD_NUMBER:
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					interval = codedIS.readRawVarint32();
					schedule.avgStopIntervals.add(interval);
				}
				codedIS.popLimit(old);
				break;
			case OsmandOdb.TransportRouteSchedule.AVGWAITINTERVALS_FIELD_NUMBER:
				sizeL = codedIS.readRawVarint32();
				old = codedIS.pushLimitLong((long) sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					interval = codedIS.readRawVarint32();
					schedule.avgWaitIntervals.add(interval);
				}
				codedIS.popLimit(old);
				break;
//			case OsmandOdb.TransportRouteSchedule.EXCEPTIONS_FIELD_NUMBER:
//				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}

	protected TIntObjectHashMap<String> initializeStringTable(TransportIndex ind,
			TIntObjectHashMap<String> requested) throws IOException {
		if (ind.stringTable.stringTable == null) {
			ind.stringTable.stringTable = new TIntObjectHashMap<>();
			codedIS.seek(ind.stringTable.fileOffset);
			long oldLimit = codedIS.pushLimitLong((long) ind.stringTable.length);
			int current = 0;
			while (codedIS.getBytesUntilLimit() > 0) {
				int t = codedIS.readTag();
				int tag = WireFormat.getTagFieldNumber(t);
				switch (tag) {
				case 0:
					break;
				case OsmandOdb.StringTable.S_FIELD_NUMBER:
					String value = codedIS.readString();
					ind.stringTable.stringTable.put(current, value);
					current++;
					break;
				default:
					skipUnknownField(t);
					break;
				}
			}
			codedIS.popLimit(oldLimit);
		}
		return ind.stringTable.stringTable;
	}

	protected void initializeNames(boolean onlyDescription, net.osmand.data.TransportRoute dataObject,
	                               TIntObjectHashMap<String> stringTable) throws IOException {
		if (dataObject.getName().length() > 0) {
			dataObject.setName(stringTable.get(dataObject.getName().charAt(0)));
		}
		if (dataObject.getEnName(false).length() > 0) {
			dataObject.setEnName(stringTable.get(dataObject.getEnName(false).charAt(0)));
		}
		if (dataObject.getName().length() > 0 && dataObject.getName("en").length() == 0) {
			dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
		}
		if (dataObject.getOperator() != null && dataObject.getOperator().length() > 0) {
			dataObject.setOperator(stringTable.get(dataObject.getOperator().charAt(0)));
		}
		if (dataObject.getColor() != null && dataObject.getColor().length() > 0) {
			dataObject.setColor(stringTable.get(dataObject.getColor().charAt(0)));
		}
		if (dataObject.getType() != null && dataObject.getType().length() > 0) {
			dataObject.setType(stringTable.get(dataObject.getType().charAt(0)));
		}
		if (!onlyDescription) {
			for (TransportStop s : dataObject.getForwardStops()) {
				initializeNames(stringTable, s);
			}
		}
		if (dataObject.getTags() != null && dataObject.getTags().size() > 0) {
			dataObject.setTags(initializeTags(stringTable, dataObject));
		}
	}

	private Map<String, String> initializeTags(TIntObjectHashMap<String> stringTable,
	                                           net.osmand.data.TransportRoute dataObject) {
		Map<String, String> newMap = new HashMap<>();
		for (Map.Entry<String, String> entry : dataObject.getTags().entrySet()) {
			String string = stringTable.get(entry.getKey().charAt(0));
			if (entry.getValue().length() > 0) {
				newMap.put(string, entry.getValue());
			} else {
				int index = string.indexOf('/');
				if (index > 0) {
					newMap.put(string.substring(0, index), string.substring(index + 1));
				}
			}
		}
		return newMap;
	}

	protected void initializeNames(TIntObjectHashMap<String> stringTable, TransportStop s) {
		for (TransportStopExit exit : s.getExits()) {
			if (exit.getRef().length() > 0) {
				exit.setRef(stringTable.get(exit.getRef().charAt(0)));
			}
		}
		if (s.getName().length() > 0) {
			s.setName(stringTable.get(s.getName().charAt(0)));
		}
		if (s.getEnName(false).length() > 0) {
			s.setEnName(stringTable.get(s.getEnName(false).charAt(0)));
		}
		Map<String, String> namesMap = new HashMap<>(s.getNamesMap(false));
		if (!s.getNamesMap(false).isEmpty()) {
			s.getNamesMap(false).clear();
		}
		Iterator<Map.Entry<String, String>> it = namesMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> e = it.next();
			s.setName(stringTable.get(e.getKey().charAt(0)), stringTable.get(e.getValue().charAt(0)));
		}
	}
	
	private TransportStop readTransportRouteStop(int[] dx, int[] dy, long did, TIntObjectHashMap<String> stringTable, 
			long filePointer) throws IOException {
		TransportStop dataObject = new TransportStop();
		dataObject.setFileOffset(codedIS.getTotalBytesRead());
		dataObject.setReferencesToRoutes(new long[] {filePointer});
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
				dx[0] += codedIS.readSInt32();
				break;
			case OsmandOdb.TransportRouteStop.DY_FIELD_NUMBER :
				dy[0] += codedIS.readSInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		dataObject.setId(did);
		dataObject.setLocation(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, dx[0], dy[0]);
		return dataObject;
	}
	
	private TransportStop readTransportStop(long shift, int cleft, int cright, int ctop, int cbottom, 
			SearchRequest<TransportStop> req, TIntObjectHashMap<String> stringTable) throws IOException {
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
		req.cacheIdsA.clear();
		req.cacheIdsB.clear();
		req.cacheIdsC.clear();

		TransportStop dataObject = new TransportStop();
		dataObject.setLocation(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, x, y);
		dataObject.setFileOffset(shift);
		while(true){
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				dataObject.setReferencesToRoutes(req.cacheIdsC.toArray());
				dataObject.setDeletedRoutesIds(req.cacheIdsA.toArray());
				dataObject.setRoutesIds(req.cacheIdsB.toArray());
				if(dataObject.getName("en").length() == 0){
					dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
				}
				return dataObject;
			case OsmandOdb.TransportStop.ROUTES_FIELD_NUMBER :
				req.cacheIdsC.add(shift - codedIS.readUInt32());
				break;
			case OsmandOdb.TransportStop.DELETEDROUTESIDS_FIELD_NUMBER :
				req.cacheIdsA.add(codedIS.readUInt64());
				break;
			case OsmandOdb.TransportStop.ROUTESIDS_FIELD_NUMBER :
				req.cacheIdsB.add(codedIS.readUInt64());
				break;
			case OsmandOdb.TransportStop.NAME_EN_FIELD_NUMBER :
				if (stringTable != null) {
					dataObject.setEnName(regStr(stringTable)); //$NON-NLS-1$
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.TransportStop.NAME_FIELD_NUMBER :
				if (stringTable != null) {
					dataObject.setName(regStr(stringTable)); //$NON-NLS-1$
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.TransportStop.ADDITIONALNAMEPAIRS_FIELD_NUMBER :
				if (stringTable != null) {
					int sizeL = codedIS.readRawVarint32();
					long oldRef = codedIS.pushLimitLong((long) sizeL);
					while (codedIS.getBytesUntilLimit() > 0) {
						dataObject.setName(regStr(stringTable,codedIS.readRawVarint32()),
								regStr(stringTable,codedIS.readRawVarint32()));
					}
					codedIS.popLimit(oldRef);
				} else {
					skipUnknownField(t);
				}
				break;
			case OsmandOdb.TransportStop.ID_FIELD_NUMBER :
				dataObject.setId(codedIS.readSInt64());
				break;
			case OsmandOdb.TransportStop.EXITS_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) length);

				TransportStopExit transportStopExit = readTransportStopExit(cleft, ctop, req, stringTable);
				dataObject.addExit(transportStopExit);
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private TransportStopExit readTransportStopExit(int cleft, int ctop, SearchRequest<TransportStop> req,
			TIntObjectHashMap<String> stringTable) throws IOException {

		TransportStopExit dataObject = new TransportStopExit();
		int x = 0;
		int y = 0;

		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);

			switch (tag) {
				case 0:
					if (dataObject.getName("en").length() == 0) {
						dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
					}
					if (x != 0 || y != 0) {
						dataObject.setLocation(BinaryMapIndexReader.TRANSPORT_STOP_ZOOM, x, y);
					}
					return dataObject;
				case OsmandOdb.TransportStopExit.REF_FIELD_NUMBER:
					if (stringTable != null) {
						dataObject.setRef(regStr(stringTable));
					} else {
						skipUnknownField(t);
					}
					break;
				case OsmandOdb.TransportStopExit.DX_FIELD_NUMBER:
					x = codedIS.readSInt32() + cleft;
					break;
				case OsmandOdb.TransportStopExit.DY_FIELD_NUMBER:
					y = codedIS.readSInt32() + ctop;
					break;
				default:
					skipUnknownField(t);
					break;
			}
		}
	}
}
