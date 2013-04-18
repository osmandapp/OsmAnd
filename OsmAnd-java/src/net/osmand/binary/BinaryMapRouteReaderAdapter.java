package net.osmand.binary;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.OsmandOdb.IdTable;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteBorderBox;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteBorderLine;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteBorderLine.Builder;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteBorderPoint;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteBorderPointsBlock;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBox;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteEncodingRule;
import net.osmand.binary.OsmandOdb.RestrictionData;
import net.osmand.binary.OsmandOdb.RouteData;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;

public class BinaryMapRouteReaderAdapter {
	protected static final Log LOG = PlatformUtil.getLog(BinaryMapRouteReaderAdapter.class);
	private static final int SHIFT_COORDINATES = 4;
	
	public static class RouteTypeRule {
		private final static int ACCESS = 1;
		private final static int ONEWAY = 2;
		private final static int HIGHWAY_TYPE = 3;
		private final static int MAXSPEED = 4;
		private final static int ROUNDABOUT = 5;
		public final static int TRAFFIC_SIGNALS = 6;
		public final static int RAILWAY_CROSSING = 7;
		private final static int LANES = 8;
		private final String t;
		private final String v;
		private int intValue;
		private float floatValue;
		private int type;

		public RouteTypeRule(String t, String v) {
			this.t = t.intern();
			if("true".equals(v)) {
				v = "yes";
			}
			if("false".equals(v)) {
				v = "no";
			}
			this.v = v == null? null : v.intern();
			analyze();
		}
		
		public String getTag() {
			return t;
		}
		
		public String getValue(){
			return v;
		}
		
		public boolean roundabout(){
			return type == ROUNDABOUT;
		}
		
		public int getType() {
			return type;
		}
		
		public int onewayDirection(){
			if(type == ONEWAY){
				return intValue;
			}
			return 0;
		}
		
		public float maxSpeed(){
			if(type == MAXSPEED){
				return floatValue;
			}
			return -1;
		}
		
		public int lanes(){
			if(type == LANES){
				return intValue;
			}
			return -1;
		}
		
		public String highwayRoad(){
			if(type == HIGHWAY_TYPE){
				return v;
			}
			return null;
		}

		private void analyze() {
			if(t.equalsIgnoreCase("oneway")){
				type = ONEWAY;
				if("-1".equals(v) || "reverse".equals(v)) {
					intValue = -1;
				} else if("1".equals(v) || "yes".equals(v)) {
					intValue = 1;
				} else {
					intValue = 0;
				}
			} else if(t.equalsIgnoreCase("highway") && "traffic_signals".equals(v)){
				type = TRAFFIC_SIGNALS;
			} else if(t.equalsIgnoreCase("railway") && ("crossing".equals(v) || "level_crossing".equals(v))){
				type = RAILWAY_CROSSING;
			} else if(t.equalsIgnoreCase("roundabout") && v != null){
				type = ROUNDABOUT;
			} else if(t.equalsIgnoreCase("junction") && "roundabout".equalsIgnoreCase(v)){
				type = ROUNDABOUT;
			} else if(t.equalsIgnoreCase("highway") && v != null){
				type = HIGHWAY_TYPE;
			} else if(t.startsWith("access") && v != null){
				type = ACCESS;
			} else if(t.equalsIgnoreCase("maxspeed") && v != null){
				type = MAXSPEED;
				floatValue = -1;
				if(v.equals("none")) {
					floatValue = RouteDataObject.NONE_MAX_SPEED;
				} else {
					int i = 0;
					while (i < v.length() && Character.isDigit(v.charAt(i))) {
						i++;
					}
					if (i > 0) {
						floatValue = Integer.parseInt(v.substring(0, i));
						floatValue /= 3.6; // km/h -> m/s
						if (v.contains("mph")) {
							floatValue *= 1.6;
						}
					}
				}
			} else if (t.equalsIgnoreCase("lanes") && v != null) {
				intValue = -1;
				int i = 0;
				type = LANES;
				while (i < v.length() && Character.isDigit(v.charAt(i))) {
					i++;
				}
				if (i > 0) {
					intValue = Integer.parseInt(v.substring(0, i));
				}
			}
			
		}
	}
	
	public static class RouteRegion extends BinaryIndexPart {
		public int regionsRead;
		public int borderBoxPointer = 0;
		public int baseBorderBoxPointer = 0;
		public int borderBoxLength = 0;
		public int baseBorderBoxLength = 0;
		
		List<RouteSubregion> subregions = new ArrayList<RouteSubregion>();
		List<RouteSubregion> basesubregions = new ArrayList<RouteSubregion>();
		List<RouteTypeRule> routeEncodingRules = new ArrayList<BinaryMapRouteReaderAdapter.RouteTypeRule>();
		
		int nameTypeRule = -1;
		int refTypeRule = -1;
		int destinationTypeRule = -1;
		
		public RouteTypeRule quickGetEncodingRule(int id) {
			return routeEncodingRules.get(id);
		}

		private void initRouteEncodingRule(int id, String tags, String val) {
			while(routeEncodingRules.size() <= id) {
				routeEncodingRules.add(null);
			}
			routeEncodingRules.set(id, new RouteTypeRule(tags, val));
			if(tags.equals("name")) {
				nameTypeRule = id;
			} else if(tags.equals("ref")) {
				refTypeRule = id;
			} else if(tags.equals("destination")) {
				destinationTypeRule = id;
			}
		}
		
		public List<RouteSubregion> getSubregions(){
			return subregions;
		}
		
		public List<RouteSubregion> getBaseSubregions(){
			return basesubregions;
		}

		public double getLeftLongitude() {
			double l = 180;
			for(RouteSubregion s : subregions) {
				l = Math.min(l, MapUtils.get31LongitudeX(s.left));
			}
			return l;
		}

		public double getRightLongitude() {
			double l = -180;
			for(RouteSubregion s : subregions) {
				l = Math.max(l, MapUtils.get31LongitudeX(s.right));
			}
			return l;
		}

		public double getBottomLatitude() {
			double l = 90;
			for(RouteSubregion s : subregions) {
				l = Math.min(l, MapUtils.get31LatitudeY(s.bottom));
			}
			return l;
		}

		public double getTopLatitude() {
			double l = -90;
			for(RouteSubregion s : subregions) {
				l = Math.max(l, MapUtils.get31LatitudeY(s.top));
			}
			return l;
		}
	}
	
	// Used in C++
	public static class RouteSubregion {
		private final static int INT_SIZE = 4;
		public final RouteRegion routeReg;
		public RouteSubregion(RouteSubregion copy) {
			this.routeReg = copy.routeReg;
			this.left = copy.left;
			this.right = copy.right;
			this.top = copy.top;
			this.bottom = copy.bottom;
			this.filePointer = copy.filePointer;
			this.length = copy.length;
			
		}
		public RouteSubregion(RouteRegion routeReg) {
			this.routeReg = routeReg;
		}
		public int length;
		public int filePointer;
		public int left;
		public int right;
		public int top;
		public int bottom;
		public int shiftToData;
		public List<RouteSubregion> subregions = null;
		public List<RouteDataObject> dataObjects = null;
		
		public int getEstimatedSize(){
			int shallow = 7 * INT_SIZE + 4*3;
			if (subregions != null) {
				shallow += 8;
				for (RouteSubregion s : subregions) {
					shallow += s.getEstimatedSize();
				}
			}
			return shallow;
		}
		
		public int countSubregions(){
			int cnt = 1;
			if (subregions != null) {
				for (RouteSubregion s : subregions) {
					cnt += s.countSubregions();
				}
			}
			return cnt;
		}
	}
	
	private CodedInputStream codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapRouteReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private int readInt() throws IOException {
		return map.readInt();
	}
	
	
	protected void readRouteIndex(RouteRegion region) throws IOException {
		int routeEncodingRule =1;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndRoutingIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndRoutingIndex.RULES_FIELD_NUMBER: {
				int len = codedIS.readInt32();
				int oldLimit = codedIS.pushLimit(len);
				readRouteEncodingRule(region, routeEncodingRule++);
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				codedIS.popLimit(oldLimit);
			}  break;
			case OsmandOdb.OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER :
			case OsmandOdb.OsmAndRoutingIndex.BASEMAPBOXES_FIELD_NUMBER :{
				RouteSubregion subregion = new RouteSubregion(region);
				subregion.length = readInt();
				subregion.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(subregion.length);
				readRouteTree(subregion, null, 0, true);
				if(tag == OsmandOdb.OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER) {
					region.subregions.add(subregion);
				} else {
					region.basesubregions.add(subregion);
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				codedIS.popLimit(oldLimit);
				break;
			}
			case OsmandOdb.OsmAndRoutingIndex.BASEBORDERBOX_FIELD_NUMBER: 
			case OsmandOdb.OsmAndRoutingIndex.BORDERBOX_FIELD_NUMBER: {
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				if(tag == OsmandOdb.OsmAndRoutingIndex.BORDERBOX_FIELD_NUMBER) {
					region.borderBoxLength = length;
					region.borderBoxPointer = filePointer;
				} else {
					region.baseBorderBoxLength = length;
					region.baseBorderBoxPointer = filePointer;
				}
				codedIS.skipRawBytes(length);
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
		while (true) {
			int ts = codedIS.readTag();
			int tags = WireFormat.getTagFieldNumber(ts);
			switch (tags) {
			case 0:
				o.pointsX = pointsX.toArray();
				o.pointsY = pointsY.toArray();
				o.types = types.toArray();
				if(globalpointTypes.size() > 0){
					o.pointTypes = new int[globalpointTypes.size()][];
					for(int k=0; k<o.pointTypes.length; k++) {
						TIntArrayList l = globalpointTypes.get(k);
						if(l != null) {
							o.pointTypes[k] = l.toArray();
						}
					}
				}
				return o;
			case RouteData.TYPES_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(len);
				while(codedIS.getBytesUntilLimit() > 0) {
					types.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(oldLimit);
				break;
			case RouteData.STRINGNAMES_FIELD_NUMBER:
				o.names = new TIntObjectHashMap<String>();
				int sizeL = codedIS.readRawVarint32();
				int old = codedIS.pushLimit(sizeL);
				while (codedIS.getBytesUntilLimit() > 0) {
					int stag = codedIS.readRawVarint32();
					int pId = codedIS.readRawVarint32();
					o.names.put(stag, ((char)pId)+"");
				}
				codedIS.popLimit(old);
				break;
			case RouteData.POINTS_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(len);
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
			case RouteData.POINTTYPES_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(len);
				while (codedIS.getBytesUntilLimit() > 0) {
					int pointInd = codedIS.readRawVarint32();
					TIntArrayList pointTypes = new TIntArrayList();
					int lens = codedIS.readRawVarint32();
					int oldLimits = codedIS.pushLimit(lens);
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
			TLongObjectHashMap<TLongArrayList> restrictions) throws IOException {
		routeTree.dataObjects = new ArrayList<RouteDataObject>();
		idTables.clear();
		restrictions.clear();
		List<String> stringTable = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				TLongObjectIterator<TLongArrayList> it = restrictions.iterator();
				while (it.hasNext()) {
					it.advance();
					int from = (int) it.key();
					RouteDataObject fromr = routeTree.dataObjects.get(from);
					fromr.restrictions = new long[it.value().size()];
					for (int k = 0; k < fromr.restrictions.length; k++) {
						int to = (int) (it.value().get(k) >> RouteDataObject.RESTRICTION_SHIFT);
						long valto = (idTables.get(to) << RouteDataObject.RESTRICTION_SHIFT) | ((long) it.value().get(k) & RouteDataObject.RESTRICTION_MASK);
						fromr.restrictions[k] = valto;
					}
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
					}
				}
				return;
			case RouteDataBlock.DATAOBJECTS_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				RouteDataObject obj = readRouteDataObject(routeTree.routeReg, routeTree.left, routeTree.top);
				while(obj.id >= routeTree.dataObjects.size()) {
					routeTree.dataObjects.add(null);
				}
				routeTree.dataObjects.set((int) obj.id,obj);
				codedIS.popLimit(oldLimit);
				break;
			case RouteDataBlock.IDTABLE_FIELD_NUMBER :
				long routeId = 0;
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				idLoop : while(true){
					int ts = codedIS.readTag();
					int tags = WireFormat.getTagFieldNumber(ts);
					switch (tags) {
					case 0:
						break idLoop;
					case IdTable.ROUTEID_FIELD_NUMBER  :
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
			case RouteDataBlock.RESTRICTIONS_FIELD_NUMBER :
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				long from = 0;
				long to = 0;
				long type = 0;
				idLoop : while(true){
					int ts = codedIS.readTag();
					int tags = WireFormat.getTagFieldNumber(ts);
					switch (tags) {
					case 0:
						break idLoop;
					case RestrictionData.FROM_FIELD_NUMBER  :
						from = codedIS.readInt32();
						break;
					case RestrictionData.TO_FIELD_NUMBER  :
						to = codedIS.readInt32();
						break;
					case RestrictionData.TYPE_FIELD_NUMBER  :
						type = codedIS.readInt32();
						break;
					default:
						skipUnknownField(ts);
						break;
					}
				}
				if(!restrictions.containsKey(from)) {
					restrictions.put(from, new TLongArrayList());
				}
				restrictions.get(from).add((to << RouteDataObject.RESTRICTION_SHIFT) + type);
				codedIS.popLimit(oldLimit);
				break;
			case RouteDataBlock.STRINGTABLE_FIELD_NUMBER :
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
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
			thisTree.subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
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
					thisTree.subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
					readChildren = true;
				}
				break;
			case RouteDataBox.BOXES_FIELD_NUMBER :
				if(readChildren){
					RouteSubregion subregion = new RouteSubregion(thisTree.routeReg);
					subregion.length = readInt();
					subregion.filePointer = codedIS.getTotalBytesRead();
					int oldLimit = codedIS.pushLimit(subregion.length);
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
	
	public void initRouteTypesIfNeeded(SearchRequest<RouteDataObject> req, List<RouteSubregion> list) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				initRouteRegion(rs.routeReg);
			}
		}
	}

	public void initRouteRegion(RouteRegion routeReg) throws IOException, InvalidProtocolBufferException {
		if (routeReg.routeEncodingRules.isEmpty()) {
			codedIS.seek(routeReg.filePointer);
			int oldLimit = codedIS.pushLimit(routeReg.length);
			readRouteIndex(routeReg);
			codedIS.popLimit(oldLimit);
		}
	}

	
	public List<RouteDataObject> loadRouteRegionData(RouteSubregion rs) throws IOException {
		TLongArrayList idMap = new TLongArrayList();
		TLongObjectHashMap<TLongArrayList> restrictionMap = new TLongObjectHashMap<TLongArrayList>();
		if (rs.dataObjects == null) {
			codedIS.seek(rs.filePointer + rs.shiftToData);
			int limit = codedIS.readRawVarint32();
			int oldLimit = codedIS.pushLimit(limit);
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
				int p1 = o1.filePointer + o1.shiftToData;
				int p2 = o2.filePointer + o2.shiftToData;
				return p1 == p2 ? 0 : (p1 < p2 ? -1 : 1);
			}
		});
		TLongArrayList idMap = new TLongArrayList();
		TLongObjectHashMap<TLongArrayList> restrictionMap = new TLongObjectHashMap<TLongArrayList>();
		for (RouteSubregion rs : toLoad) {
			if (rs.dataObjects == null) {
				codedIS.seek(rs.filePointer + rs.shiftToData);
				int limit = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(limit);
				readRouteTreeData(rs, idMap, restrictionMap);
				codedIS.popLimit(oldLimit);
			}
			for (RouteDataObject ro : rs.dataObjects) {
				if (ro != null) {
					matcher.publish(ro);
				}
			}
			// free objects
			rs.dataObjects = null;
		}
	}

	public List<RouteSubregion> searchRouteRegionTree(SearchRequest<RouteDataObject> req, List<RouteSubregion> list, 
			List<RouteSubregion> toLoad) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				if (rs.subregions == null) {
					codedIS.seek(rs.filePointer);
					int old = codedIS.pushLimit(rs.length);
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
	
	public List<RouteDataBorderLinePoint> searchBorderPoints(SearchRequest<RouteDataBorderLinePoint> req, RouteRegion r) throws IOException {
		if(r.borderBoxPointer != 0) {
			codedIS.seek(r.borderBoxPointer);
			int old = codedIS.pushLimit(r.borderBoxLength);
			TIntArrayList blocksToRead = new TIntArrayList();
			readBorderLines(req, blocksToRead);
			
			blocksToRead.sort();
			for(int j = 0; j< blocksToRead.size() ; j++) {
				codedIS.seek(blocksToRead.get(j));
				int len = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(len);
				readBorderLinePoints(req, r);
				codedIS.popLimit(oldLimit);
			}
			codedIS.popLimit(old);
			
		}
		return req.getSearchResults();
	}
	
	
	private void readBorderLinePoints(SearchRequest<RouteDataBorderLinePoint> req, RouteRegion r) throws IOException {
		int x = 0;
		int y = 0;
		long id = 0;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case RouteBorderPointsBlock.X_FIELD_NUMBER: {
				x = codedIS.readInt32();
				break;
			}
			case RouteBorderPointsBlock.Y_FIELD_NUMBER: {
				y = codedIS.readInt32();
				break;
			}
			case RouteBorderPointsBlock.BASEID_FIELD_NUMBER: {
				id = codedIS.readInt64();
				break;
			}
			case RouteBorderPointsBlock.POINTS_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(len);
				RouteDataBorderLinePoint p = readBorderLinePoint(new RouteDataBorderLinePoint(r), x, y, id);
				codedIS.popLimit(oldLimit);
				x = p.x;
				y = p.y;
				id = p.id;
				req.publish(p);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}
	
	private RouteDataBorderLinePoint readBorderLinePoint(RouteDataBorderLinePoint p, int x, int y, long id) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return p;
			case RouteBorderPoint.DX_FIELD_NUMBER:
				p.x =  x + codedIS.readInt32();
				break;
			case RouteBorderPoint.DY_FIELD_NUMBER:
				p.y =  y + codedIS.readInt32();
				break;
			case RouteBorderPoint.ROADID_FIELD_NUMBER:
				p.id =  id + codedIS.readSInt64();
				break;
			case RouteBorderPoint.DIRECTION_FIELD_NUMBER:
				p.direction = codedIS.readBool();
				break;
			case RouteBorderPoint.TYPES_FIELD_NUMBER:
				TIntArrayList types = new TIntArrayList();
				int len = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(len);
				while(codedIS.getBytesUntilLimit() > 0) {
					types.add(codedIS.readRawVarint32());
				}
				codedIS.popLimit(oldLimit);
				p.types = types.toArray();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readBorderLines(SearchRequest<RouteDataBorderLinePoint> req, TIntArrayList blocksToRead) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case RouteBorderBox.BORDERLINES_FIELD_NUMBER: {
				int fp = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int old = codedIS.pushLimit(length);
				
				RouteBorderLine ln = readBorderLine();
				if(ln.hasTox() && req.intersects(ln.getX(), ln.getY(), ln.getTox(), ln.getY())) {
					blocksToRead.add(ln.getShiftToPointsBlock() + fp);
					// FIXME borders approach
//				} else if(ln.hasToy() && req.intersects(ln.getX(), ln.getY(), ln.getX(), ln.getToy())) {
//					blocksToRead.add(ln.getShiftToPointsBlock() + fp);
				} 
				codedIS.popLimit(old);
				break;
			}
			case RouteBorderBox.BLOCKS_FIELD_NUMBER:
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}

	private RouteBorderLine readBorderLine() throws IOException {
		Builder bld = RouteBorderLine.newBuilder();
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return bld.build();
			case RouteBorderLine.X_FIELD_NUMBER:
				bld.setX(codedIS.readInt32());
				break;
			case RouteBorderLine.Y_FIELD_NUMBER:
				bld.setY(codedIS.readInt32());
				break;
			case RouteBorderLine.TOX_FIELD_NUMBER:
				bld.setTox(codedIS.readInt32());
				break;
			case RouteBorderLine.TOY_FIELD_NUMBER:
				bld.setToy(codedIS.readInt32());
				break;
			case RouteBorderLine.SHIFTTOPOINTSBLOCK_FIELD_NUMBER:
				bld.setShiftToPointsBlock(readInt());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	public List<RouteSubregion> loadInteresectedPoints(SearchRequest<RouteDataObject> req, List<RouteSubregion> list, 
			List<RouteSubregion> toLoad) throws IOException {
		for (RouteSubregion rs : list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				if (rs.subregions == null) {
					codedIS.seek(rs.filePointer);
					int old = codedIS.pushLimit(rs.length);
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
