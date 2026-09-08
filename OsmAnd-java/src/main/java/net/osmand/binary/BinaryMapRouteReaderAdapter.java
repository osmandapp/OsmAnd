package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.OsmandOdb.IdTable;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBox;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteEncodingRule;
import net.osmand.binary.OsmandOdb.RestrictionData;
import net.osmand.binary.OsmandOdb.RouteData;
import net.osmand.shared.routing.RouteDataObject.RestrictionInfo;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.RouteRegion;
import net.osmand.shared.routing.RouteSubregion;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.shared.util.collections.KTIntObjectMap;

public class BinaryMapRouteReaderAdapter {
	protected static final Log LOG = PlatformUtil.getLog(BinaryMapRouteReaderAdapter.class);
	private static final int SHIFT_COORDINATES = 4;

	private CodedInputStream codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapRouteReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private long readInt() throws IOException {
		return map.readInt();
	}
	
	
	protected void readRouteIndex(RouteRegion region) throws IOException {
		int routeEncodingRule = 1;
		long routeEncodingRulesSize = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				region.completeRouteEncodingRules();
				return;
			case OsmandOdb.OsmAndRoutingIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndRoutingIndex.RULES_FIELD_NUMBER: {
				int len = codedIS.readInt32();
				if(routeEncodingRulesSize == 0) {
					routeEncodingRulesSize = codedIS.getTotalBytesRead();	
				}
				long oldLimit = codedIS.pushLimitLong((long) len);
				readRouteEncodingRule(region, routeEncodingRule++);
				codedIS.popLimit(oldLimit);
				region.routeEncodingRulesBytes = (int) (codedIS.getTotalBytesRead() - routeEncodingRulesSize);
			}  break;
			case OsmandOdb.OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER :
			case OsmandOdb.OsmAndRoutingIndex.BASEMAPBOXES_FIELD_NUMBER :{
				RouteSubregion subregion = new RouteSubregion(region);
				subregion.length = readInt();
				subregion.filePointer = codedIS.getTotalBytesRead();
				long oldLimit = codedIS.pushLimitLong((long) subregion.length);
				readRouteTree(subregion, null, 0, true);
				if (tag == OsmandOdb.OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER) {
					boolean exist = false;
					for (RouteSubregion s : region.subregions) {
						if (s.filePointer == subregion.filePointer) {
							exist = true;
							break;
						}
					}
					if (!exist) {
						region.subregions.add(subregion);
					}
				} else {
					boolean exist = false;
					for (RouteSubregion s : region.basesubregions) {
						if (s.filePointer == subregion.filePointer) {
							exist = true;
							break;
						}
					}
					if (!exist) {
						region.basesubregions.add(subregion);
					}
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				codedIS.popLimit(oldLimit);
				break;
			}
			case OsmandOdb.OsmAndRoutingIndex.BLOCKS_FIELD_NUMBER : {
				// Finish reading file!
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				break;
			}	
				
			
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private RouteDataObject readRouteDataObject(RouteRegion reg, int pleftx, int ptopy) throws IOException {
		RouteDataObject o = new RouteDataObject(reg);
		TIntArrayList pointsX = new TIntArrayList();
		TIntArrayList pointsY = new TIntArrayList();
		TIntArrayList types = new TIntArrayList();
		List<TIntArrayList> globalpointTypes = new ArrayList<TIntArrayList>();
		List<TIntArrayList> globalpointNames = new ArrayList<TIntArrayList>();
		while (true) {
			int ts = codedIS.readTag();
			int tags = WireFormat.getTagFieldNumber(ts);
			switch (tags) {
			case 0:
				o.pointsX = pointsX.toArray();
				o.pointsY = pointsY.toArray();
				o.types = types.toArray();
				if (globalpointTypes.size() > 0) {
					o.pointTypes = new int[globalpointTypes.size()][];
					for (int k = 0; k < o.pointTypes.length; k++) {
						TIntArrayList l = globalpointTypes.get(k);
						if (l != null) {
							o.pointTypes[k] = l.toArray();
						}
					}
				}
				if (globalpointNames.size() > 0) {
					o.pointNames = new String[globalpointNames.size()][];
					o.pointNameTypes = new int[globalpointNames.size()][];
					for (int k = 0; k < o.pointNames.length; k++) {
						TIntArrayList l = globalpointNames.get(k);
						if (l != null) {
							o.pointNameTypes[k] = new int[l.size() / 2];
							o.pointNames[k] = new String[l.size() / 2];
							for (int ik = 0; ik < l.size(); ik += 2) {
								o.pointNameTypes[k][ik / 2] = l.get(ik);
								o.pointNames[k][ik / 2] = ((char) l.get(ik + 1)) + "";
							}
						}
					}
				}
				return o;
			case RouteData.TYPES_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) len);
				while(codedIS.getBytesUntilLimit() > 0) {
					types.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteData.STRINGNAMES_FIELD_NUMBER:
				o.names = new KTIntObjectMap<String>();
				int sizeL = codedIS.readRawVarint32();
				long old = codedIS.pushLimitLong((long) sizeL);
				TIntArrayList list = new TIntArrayList();
				while (codedIS.getBytesUntilLimit() > 0) {
					int stag = codedIS.readRawVarint32();
					int pId = codedIS.readRawVarint32();
					o.names.put(stag, ((char)pId)+"");
					list.add(stag);
				}
				o.nameIds = list.toArray();
				codedIS.popLimit(old);
				break;
			case RouteData.POINTS_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) len);
				int px = pleftx >> SHIFT_COORDINATES;
				int py = ptopy >> SHIFT_COORDINATES;
				while(codedIS.getBytesUntilLimit() > 0){
					int x = (codedIS.readSInt32() ) + px;
					int y = (codedIS.readSInt32() ) + py;
					pointsX.add(x << SHIFT_COORDINATES);
					pointsY.add(y << SHIFT_COORDINATES);
					px = x;
					py = y;
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteData.POINTNAMES_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) len);
				while (codedIS.getBytesUntilLimit() > 0) {
					int pointInd = codedIS.readRawVarint32();
					int pointNameType = codedIS.readRawVarint32();
					int nameInd = codedIS.readRawVarint32();
					while (pointInd >= globalpointNames.size()) {
						globalpointNames.add(null);
					}
					if(globalpointNames.get(pointInd)== null) {
						TIntArrayList pointTypes = new TIntArrayList();
						globalpointNames.set(pointInd, pointTypes);
					}
					globalpointNames.get(pointInd).add(pointNameType);
					globalpointNames.get(pointInd).add(nameInd);
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteData.POINTTYPES_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) len);
				while (codedIS.getBytesUntilLimit() > 0) {
					int pointInd = codedIS.readRawVarint32();
					TIntArrayList pointTypes = new TIntArrayList();
					int lens = codedIS.readRawVarint32();
					long oldLimits = codedIS.pushLimitLong((long) lens);
					while (codedIS.getBytesUntilLimit() > 0) {
						pointTypes.add(codedIS.readRawVarint32());
					}
					codedIS.popLimit(oldLimits);
					while (pointInd >= globalpointTypes.size()) {
						globalpointTypes.add(null);
					}
					globalpointTypes.set(pointInd, pointTypes);
					
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteData.ROUTEID_FIELD_NUMBER:
				o.id = codedIS.readInt32();
				break;
			default:
				skipUnknownField(ts);
				break;
			}
		}
	}
	private void readRouteTreeData(RouteSubregion routeTree,  TLongArrayList idTables,
			TLongObjectHashMap<RestrictionInfo> restrictions) throws IOException {
		routeTree.dataObjects = new ArrayList<RouteDataObject>();
		idTables.clear();
		restrictions.clear();
		List<String> stringTable = null;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				TLongObjectIterator<RestrictionInfo> it = restrictions.iterator();
				while (it.hasNext()) {
					it.advance();
					int from = (int) it.key();
					RouteDataObject fromr = routeTree.dataObjects.get(from);
					fromr.restrictions = new long[it.value().length()];
					RestrictionInfo val = it.value();
					for (int k = 0; k < fromr.restrictions.length; k++) {
						if (val != null) {
							long via = 0;
							if (val.viaWay != 0) {
								via = idTables.get((int) val.viaWay);
							}
							fromr.setRestriction(k, idTables.get((int) val.toWay), val.type, via);
						}
						val = val.next;
					}
//					fromr.restrictionsVia = new 
				}
				for (RouteDataObject o : routeTree.dataObjects) {
					if (o != null) {
						if (o.id < idTables.size()) {
							o.id = idTables.get((int) o.id);
						}
						if (o.names != null && stringTable != null) {
							int[] keys = o.names.keys();
							for (int j = 0; j < keys.length; j++) {
								o.names.put(keys[j], stringTable.get(o.names.get(keys[j]).charAt(0)));
							}
						}
						if (o.pointNames != null && stringTable != null) {
							for (String[] ar : o.pointNames) {
								if (ar != null) {
									for (int j = 0; j < ar.length; j++) {
										ar[j] = stringTable.get(ar[j].charAt(0));
									}
								}
							}
						}
					}
				}
				return;
			case RouteDataBlock.DATAOBJECTS_FIELD_NUMBER:
				int length = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) length);
				RouteDataObject obj = readRouteDataObject(routeTree.routeReg, routeTree.left, routeTree.top);
				while (obj.id >= routeTree.dataObjects.size()) {
					routeTree.dataObjects.add(null);
				}
				routeTree.dataObjects.set((int) obj.id, obj);
				codedIS.popLimit(oldLimit);
				break;
			case RouteDataBlock.IDTABLE_FIELD_NUMBER:
				long routeId = 0;
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				idLoop: while (true) {
					int ts = codedIS.readTag();
					int tags = WireFormat.getTagFieldNumber(ts);
					switch (tags) {
					case 0:
						break idLoop;
					case IdTable.ROUTEID_FIELD_NUMBER:
						routeId += codedIS.readSInt64();
						idTables.add(routeId);
						break;
					default:
						skipUnknownField(ts);
						break;
					}
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteDataBlock.RESTRICTIONS_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				RestrictionInfo ri = new RestrictionInfo();
				long from = 0;
				idLoop: while (true) {
					int ts = codedIS.readTag();
					int tags = WireFormat.getTagFieldNumber(ts);
					switch (tags) {
					case 0:
						break idLoop;
					case RestrictionData.FROM_FIELD_NUMBER:
						from = codedIS.readInt32();
						break;
					case RestrictionData.TO_FIELD_NUMBER:
						ri.toWay = codedIS.readInt32();
						break;
					case RestrictionData.TYPE_FIELD_NUMBER:
						ri.type = codedIS.readInt32();
						break;
					case RestrictionData.VIA_FIELD_NUMBER:
						ri.viaWay = codedIS.readInt32();
						break;
					default:
						skipUnknownField(ts);
						break;
					}
				}
				RestrictionInfo prev = restrictions.get(from);
				if (prev != null) {
					while (prev.next != null) {
						prev = prev.next;
					}
					prev.next = ri;
				} else {
					restrictions.put(from, ri);
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteDataBlock.STRINGTABLE_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				stringTable = map.readStringTable();
//				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	

	private void readRouteEncodingRule(RouteRegion index, int id) throws IOException {
		String tags = null;
		String val = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				index.initRouteEncodingRule(id, tags, val);
				return;
			case RouteEncodingRule.VALUE_FIELD_NUMBER :
				val = codedIS.readString().intern();
				break;
			case RouteEncodingRule.TAG_FIELD_NUMBER :
				tags = codedIS.readString().intern();
				break;
			case RouteEncodingRule.ID_FIELD_NUMBER :
				id = codedIS.readUInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private RouteSubregion readRouteTree(RouteSubregion thisTree, RouteSubregion parentTree, int depth,
			boolean readCoordinates) throws IOException {
		boolean readChildren = depth != 0; 
		if(readChildren) {
			thisTree.subregions = new ArrayList<RouteSubregion>();
		}
		thisTree.routeReg.regionsRead++;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return thisTree;
			case RouteDataBox.LEFT_FIELD_NUMBER :
				int i = codedIS.readSInt32();
				if (readCoordinates) {
					thisTree.left = i + (parentTree != null ? parentTree.left : 0);
				}
				break;
			case RouteDataBox.RIGHT_FIELD_NUMBER :
				i = codedIS.readSInt32();
				if (readCoordinates) {
					thisTree.right = i + (parentTree != null ? parentTree.right : 0);
				}
				break;
			case RouteDataBox.TOP_FIELD_NUMBER :
				i = codedIS.readSInt32();
				if (readCoordinates) {
					thisTree.top = i + (parentTree != null ? parentTree.top : 0);
				}
				break;
			case RouteDataBox.BOTTOM_FIELD_NUMBER :
				i = codedIS.readSInt32();
				if (readCoordinates) {
					thisTree.bottom = i + (parentTree != null ? parentTree.bottom : 0);
				}
				break;
			case RouteDataBox.SHIFTTODATA_FIELD_NUMBER :
				thisTree.shiftToData = readInt();
				if(!readChildren) {
					// usually 0
					thisTree.subregions = new ArrayList<RouteSubregion>();
					readChildren = true;
				}
				break;
			case RouteDataBox.BOXES_FIELD_NUMBER :
				if(readChildren){
					RouteSubregion subregion = new RouteSubregion(thisTree.routeReg);
					subregion.length = readInt();
					subregion.filePointer = codedIS.getTotalBytesRead();
					long oldLimit = codedIS.pushLimitLong((long) subregion.length);
					readRouteTree(subregion, thisTree, depth - 1, true);
					thisTree.subregions.add(subregion);
					codedIS.popLimit(oldLimit);
					codedIS.seek(subregion.filePointer + subregion.length);
				} else {
					codedIS.seek(thisTree.filePointer + thisTree.length);
					// skipUnknownField(t);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	public void initRouteTypesIfNeeded(SearchRequest<?> req, List<RouteSubregion> list) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				initRouteRegion(rs.routeReg);
			}
		}
	}

	public void initRouteRegion(RouteRegion routeReg) throws IOException, InvalidProtocolBufferException {
		if (routeReg.routeEncodingRules.isEmpty()) {
			codedIS.seek(routeReg.filePointer);
			long oldLimit = codedIS.pushLimitLong((long) routeReg.length);
			readRouteIndex(routeReg);
			codedIS.popLimit(oldLimit);
		}
	}

	
	public List<RouteDataObject> loadRouteRegionData(RouteSubregion rs) throws IOException {
		TLongArrayList idMap = new TLongArrayList();
		TLongObjectHashMap<RestrictionInfo> restrictionMap = new TLongObjectHashMap<RestrictionInfo>();
		if (rs.dataObjects == null) {
			codedIS.seek(rs.filePointer + rs.shiftToData);
			int limit = codedIS.readRawVarint32();
			long oldLimit = codedIS.pushLimitLong((long) limit);
			readRouteTreeData(rs, idMap, restrictionMap);
			codedIS.popLimit(oldLimit);
		}
		List<RouteDataObject> res = rs.dataObjects;
		rs.dataObjects = null;
		return res;
	}
	
	public void loadRouteRegionData(List<RouteSubregion> toLoad, ResultMatcher<RouteDataObject> matcher) throws IOException {
		Collections.sort(toLoad, new Comparator<RouteSubregion>() {
			@Override
			public int compare(RouteSubregion o1, RouteSubregion o2) {
				long p1 = o1.filePointer + o1.shiftToData;
				long p2 = o2.filePointer + o2.shiftToData;
				return p1 == p2 ? 0 : (p1 < p2 ? -1 : 1);
			}
		});
		TLongArrayList idMap = new TLongArrayList();
		TLongObjectHashMap<RestrictionInfo> restrictionMap = new TLongObjectHashMap<RestrictionInfo>();
		for (RouteSubregion rs : toLoad) {
			if (rs.dataObjects == null) {
				codedIS.seek(rs.filePointer + rs.shiftToData);
				int limit = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) limit);
				readRouteTreeData(rs, idMap, restrictionMap);
				codedIS.popLimit(oldLimit);
			}
			for (RouteDataObject ro : rs.dataObjects) {
				if (ro != null) {
					matcher.publish(ro);
				}
				if (matcher.isCancelled()) {
					break;
				}
			}
			// free objects
			rs.dataObjects = null;
			if (matcher.isCancelled()) {
				break;
			}
		}
	}

	public List<RouteSubregion> searchRouteRegionTree(SearchRequest<?> req, List<RouteSubregion> list, 
			List<RouteSubregion> toLoad) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				if (rs.subregions == null) {
					codedIS.seek(rs.filePointer);
					long old = codedIS.pushLimitLong((long) rs.length);
					readRouteTree(rs, null, req.contains(rs.left, rs.top, rs.right, rs.bottom) ? -1 : 1, false);
					codedIS.popLimit(old);
				}
				searchRouteRegionTree(req, rs.subregions, toLoad);

				if (rs.shiftToData != 0) {
					toLoad.add(rs);
				}
			}
		}
		return toLoad;
	}
	

	public List<RouteSubregion> loadInteresectedPoints(SearchRequest<RouteDataObject> req, List<RouteSubregion> list, 
			List<RouteSubregion> toLoad) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				if (rs.subregions == null) {
					codedIS.seek(rs.filePointer);
					long old = codedIS.pushLimitLong((long) rs.length);
					readRouteTree(rs, null, req.contains(rs.left, rs.top, rs.right, rs.bottom) ? -1 : 1, false);
					codedIS.popLimit(old);
				}
				searchRouteRegionTree(req, rs.subregions, toLoad);
				if (rs.shiftToData != 0) {
					toLoad.add(rs);
				}
			}
		}
		return toLoad;
	}
	
}
