package net.osmand.binary;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.OsmandOdb.IdTable;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBox;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteEncodingRule;
import net.osmand.binary.OsmandOdb.RestrictionData;
import net.osmand.binary.OsmandOdb.RouteData;
import net.osmand.binary.RouteDataObject.RestrictionInfo;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;

public class BinaryMapRouteReaderAdapter {
	protected static final Log LOG = PlatformUtil.getLog(BinaryMapRouteReaderAdapter.class);
	private static final int SHIFT_COORDINATES = 4;

	private static class RouteTypeCondition {
		String condition = "";
		OpeningHoursParser.OpeningHours hours = null;
		String value;
		int ruleid;
	}

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
		private List<RouteTypeCondition> conditions = null;
		private int forward;

		public RouteTypeRule(String t, String v) {
			this.t = t.intern();
			if("true".equals(v)) {
				v = "yes";
			}
			if("false".equals(v)) {
				v = "no";
			}
			this.v = v == null? null : v.intern();
			try {
				analyze();
			} catch(RuntimeException e) {
				System.err.println("Error analyzing tag/value = " + t + "/" +v);
				throw e;
			}
		}

		public int isForward() {
			return forward;
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

		public boolean conditional() {
			return conditions != null;
		}
		
		public String getNonConditionalTag() {
			String tag = getTag();
			if(tag != null && tag.endsWith(":conditional")) {
				tag = tag.substring(0, tag.length() - ":conditional".length());
			}
			return tag;
		}

		public int onewayDirection(){
			if(type == ONEWAY){
				return intValue;
			}
			return 0;
		}

		public int conditionalValue(long time) {
			if (conditional()) {
				Calendar i = Calendar.getInstance();
				i.setTimeInMillis(time);
				for (RouteTypeCondition c : conditions) {
					if (c.hours != null && c.hours.isOpenedForTime(i)) {
						return c.ruleid;
					}
				}
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
			} else if(t.endsWith(":conditional") && v != null){
				conditions = new ArrayList<RouteTypeCondition>();
				String[] cts = v.split("\\);");
				for(String c : cts) {
					int ch = c.indexOf('@');
					if (ch > 0) {
						RouteTypeCondition cond = new RouteTypeCondition();
						cond.value = c.substring(0, ch).trim();
						cond.condition = c.substring(ch + 1).trim();
						if (cond.condition.startsWith("(")) {
							cond.condition = cond.condition.substring(1, cond.condition.length()).trim();
						}
						if(cond.condition.endsWith(")")) {
							cond.condition = cond.condition.substring(0, cond.condition.length() - 1).trim();
						}
						cond.hours = OpeningHoursParser.parseOpenedHours(cond.condition);
						conditions.add(cond);
					}
				}
				// we don't set type for condtiional so they are not used directly
//				if(t.startsWith("maxspeed")) {
//					type = MAXSPEED;
//				} else if(t.startsWith("oneway")) {
//					type = ONEWAY;
//				} else if(t.startsWith("lanes")) {
//					type = LANES;
//				} else if(t.startsWith("access")) {
//					type = ACCESS;
//				}
			} else if(t.startsWith("access") && v != null){
				type = ACCESS;
			} else if(t.equalsIgnoreCase("maxspeed") && v != null){
				type = MAXSPEED;
				floatValue = RouteDataObject.parseSpeed(v, 0);
			} else if(t.equalsIgnoreCase("maxspeed:forward") && v != null){
				type = MAXSPEED;
				forward = 1;
				floatValue = RouteDataObject.parseSpeed(v, 0);
			} else if(t.equalsIgnoreCase("maxspeed:backward") && v != null){
				type = MAXSPEED;
				forward = -1;
				floatValue = RouteDataObject.parseSpeed(v, 0);
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
		public List<RouteTypeRule> routeEncodingRules = new ArrayList<BinaryMapRouteReaderAdapter.RouteTypeRule>();
		public Map<String, Integer> decodingRules = null;
		List<RouteSubregion> subregions = new ArrayList<RouteSubregion>();
		List<RouteSubregion> basesubregions = new ArrayList<RouteSubregion>();
		
		int nameTypeRule = -1;
		int refTypeRule = -1;
		int destinationTypeRule = -1;
		int destinationRefTypeRule = -1;
		private RouteRegion referenceRouteRegion;
		
		public String getPartName() {
			return "Routing";
		}
		

		public int getFieldNumber() {
			return OsmandOdb.OsmAndStructure.ROUTINGINDEX_FIELD_NUMBER;
		}
		
		public int searchRouteEncodingRule(String tag, String value) {
			if(decodingRules == null) {
				decodingRules = new LinkedHashMap<String, Integer>();
				for(int i = 1; i < routeEncodingRules.size(); i++) {
					RouteTypeRule rt = routeEncodingRules.get(i);
					String ks = rt.getTag() +"#" + (rt.getValue() == null ? "" : rt.getValue());
					decodingRules.put(ks, i);
				}
			}
			String k = tag +"#" + (value == null ? "" : value);
			if(decodingRules.containsKey(k)) {
				return decodingRules.get(k).intValue();
			}
			return -1;
		}
		
		public RouteTypeRule quickGetEncodingRule(int id) {
			return routeEncodingRules.get(id);
		}

		public void initRouteEncodingRule(int id, String tags, String val) {
			decodingRules = null;
			while (routeEncodingRules.size() <= id) {
				routeEncodingRules.add(null);
			}
			routeEncodingRules.set(id, new RouteTypeRule(tags, val));
			if (tags.equals("name")) {
				nameTypeRule = id;
			} else if (tags.equals("ref")) {
				refTypeRule = id;
			} else if (tags.equals("destination") || tags.equals("destination:forward") || tags.equals("destination:backward") || tags.startsWith("destination:lang:")) {
				destinationTypeRule = id;
			} else if (tags.equals("destination:ref") || tags.equals("destination:ref:forward") || tags.equals("destination:ref:backward")) {
				destinationRefTypeRule = id;
			}
		}
		
		public void completeRouteEncodingRules() {
			for(int i = 0; i < routeEncodingRules.size(); i++) {
				RouteTypeRule rtr = routeEncodingRules.get(i);
				if(rtr != null && rtr.conditional()) {
					String tag = rtr.getNonConditionalTag();
					for(RouteTypeCondition c : rtr.conditions ) {
						if(tag != null && c.value != null) {
							c.ruleid = findOrCreateRouteType(tag, c.value);
						}
					}
					
				}
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

		public boolean contains(int x31, int y31) {
			for(RouteSubregion s : subregions) {
				if(s.left <= x31 && s.right >= x31 && s.top <= y31 && s.bottom >= y31) {
					return true;
				}
			}
			return false;
		}


		public RouteDataObject adopt(RouteDataObject o) {
			if(o.region == this || o.region == referenceRouteRegion) {
				return o;
			}
			
			if(routeEncodingRules.isEmpty()) {
				routeEncodingRules.addAll(o.region.routeEncodingRules);
				referenceRouteRegion= o.region;
				return o;
			}
			RouteDataObject rdo = new RouteDataObject(this);
			rdo.pointsX = o.pointsX;
			rdo.pointsY = o.pointsY;
			rdo.id = o.id;
			rdo.restrictions = o.restrictions;
			rdo.restrictionsVia = o.restrictionsVia;

			if (o.types != null) {
				rdo.types = new int[o.types.length];
				for (int i = 0; i < o.types.length; i++) {
					RouteTypeRule tp = o.region.routeEncodingRules.get(o.types[i]);
					int ruleId = findOrCreateRouteType(tp.getTag(), tp.getValue());
					rdo.types[i] = ruleId;
				}
			}
			if (o.pointTypes != null) {
				rdo.pointTypes = new int[o.pointTypes.length][];
				for (int i = 0; i < o.pointTypes.length; i++) {
					if (o.pointTypes[i] != null) {
						rdo.pointTypes[i] = new int[o.pointTypes[i].length];
						for (int j = 0; j < o.pointTypes[i].length; j++) {
							RouteTypeRule tp = o.region.routeEncodingRules.get(o.pointTypes[i][j]);
							int ruleId = searchRouteEncodingRule(tp.getTag(), tp.getValue());
							if(ruleId != -1) {
								rdo.pointTypes[i][j] = ruleId;
							} else {
								ruleId = routeEncodingRules.size() ;
								initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue());
								rdo.pointTypes[i][j] = ruleId;
							}
						}
					}
				}
			}
			if (o.nameIds != null) {
				rdo.nameIds = new int[o.nameIds.length];
				rdo.names = new TIntObjectHashMap<>();
				for (int i = 0; i < o.nameIds.length; i++) {
					RouteTypeRule tp = o.region.routeEncodingRules.get(o.nameIds[i]);
					int ruleId = searchRouteEncodingRule(tp.getTag(), null);
					if(ruleId != -1) {
						rdo.nameIds[i] = ruleId;
					} else {
						ruleId = routeEncodingRules.size() ;
						initRouteEncodingRule(ruleId, tp.getTag(), null);
						rdo.nameIds[i] = ruleId;
					}
					rdo.names.put(ruleId, o.names.get(o.nameIds[i]));
				}
			}
			rdo.pointNames = o.pointNames;
			if (o.pointNameTypes != null) {
				rdo.pointNameTypes = new int[o.pointNameTypes.length][];
				// rdo.pointNames = new String[o.pointNameTypes.length][];
				for (int i = 0; i < o.pointNameTypes.length; i++) {
					if (o.pointNameTypes[i] != null) {
						rdo.pointNameTypes[i] = new int[o.pointNameTypes[i].length];
						// rdo.pointNames[i] = new String[o.pointNameTypes[i].length];
						for (int j = 0; j < o.pointNameTypes[i].length; j++) {
							RouteTypeRule tp = o.region.routeEncodingRules.get(o.pointNameTypes[i][j]);
							int ruleId = searchRouteEncodingRule(tp.getTag(), null);
							if(ruleId != -1) {
								rdo.pointNameTypes[i][j] = ruleId;
							} else {
								ruleId = routeEncodingRules.size() ;
								initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue());
								rdo.pointNameTypes[i][j] = ruleId;
							}
							// rdo.pointNames[i][j] = o.pointNames[i][j];
						}
					}
				}
			}
			return rdo;
		}


		public int findOrCreateRouteType(String tag, String value) {
			int ruleId = searchRouteEncodingRule(tag, value);
			if(ruleId == -1) {
				ruleId = routeEncodingRules.size() ;
				initRouteEncodingRule(ruleId, tag, value);
			}
			return ruleId;
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
		int routeEncodingRule = 1;
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
			case RouteData.POINTNAMES_FIELD_NUMBER:
				len = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(len);
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
			TLongObjectHashMap<RestrictionInfo> restrictions) throws IOException {
		routeTree.dataObjects = new ArrayList<RouteDataObject>();
		idTables.clear();
		restrictions.clear();
		List<String> stringTable = null;
		while(true){
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
						if(val != null) {
							long via = 0;
							if(val.viaWay != 0) {
								via = idTables.get((int)val.viaWay);
							}
							fromr.setRestriction(k, idTables.get((int)val.toWay), val.type, via);
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
							for(String[] ar : o.pointNames) {
								if(ar != null) {
									for(int j = 0; j < ar.length; j++) {
										ar[j] = stringTable.get(ar[j].charAt(0));
									}
								}
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
				RestrictionInfo ri = new RestrictionInfo();
				long from = 0;
				idLoop : while(true){
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
				if(prev != null) {
					prev.next = ri;
				} else {
					restrictions.put(from, ri);
				}
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
			int oldLimit = codedIS.pushLimit(routeReg.length);
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
		TLongObjectHashMap<RestrictionInfo> restrictionMap = new TLongObjectHashMap<RestrictionInfo>();
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

	public List<RouteSubregion> searchRouteRegionTree(SearchRequest<?> req, List<RouteSubregion> list, 
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
