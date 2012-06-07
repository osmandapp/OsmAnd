package net.osmand.binary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteDataBox;
import net.osmand.binary.OsmandOdb.OsmAndRoutingIndex.RouteEncodingRule;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapRouteReaderAdapter {
	private static final Log LOG = LogUtil.getLog(BinaryMapRouteReaderAdapter.class);
	
	public static class RouteTypeRule {
		private final static int ACCESS = 1;
		private final static int ONEWAY = 2;
		private final static int HIGHWAY_TYPE = 3;
		private final static int MAXSPEED = 4;
		private final String t;
		private final String v;
		private int intValue;
		private float floatValue;
		private int type;

		public RouteTypeRule(String t, String v) {
			this.t = t.intern();
			this.v = v == null? null : v.intern();
			analyze();
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
		
		public String highwayRoad(){
			if(type == HIGHWAY_TYPE){
				return v;
			}
			return null;
		}

		private void analyze() {
			if(t.equalsIgnoreCase("oneway")){
				type = ONEWAY;
				if("-1".equals(v)) {
					intValue = -1;
				} else if("1".equals(v) || "yes".equals(v)) {
					intValue = 1;
				}
				intValue = 0;
			} else if(t.equalsIgnoreCase("highway") && v != null){
				type = HIGHWAY_TYPE;
			} else if(t.startsWith("access") && v != null){
				type = ACCESS;
			} else if(t.equalsIgnoreCase("maxspeed") && v != null){
				floatValue = -1;
				int i = 0;
				while(i < v.length() && Character.isDigit(v.charAt(i))) {
					i++;
				}
				if(i > 0) {
					floatValue = Integer.parseInt(v.substring(0, i));
					floatValue *= 3.6; // km/h -> m/s
					if(v.contains("mph")) {
						floatValue *= 1.6;
					}
				}
				
			}
			
		}
	}
	
	
	public static class RouteRegion extends BinaryIndexPart {
		double leftLongitude;
		double rightLongitude;
		double topLatitude;
		double bottomLatitude;
		int regionsRead;
		
		List<RouteSubregion> subregions = new ArrayList<RouteSubregion>();
		List<RouteTypeRule> routeEncodingRules = new ArrayList<BinaryMapRouteReaderAdapter.RouteTypeRule>();
		
		public double getLeftLongitude() {
			return leftLongitude;
		}
		
		public double getRightLongitude() {
			return rightLongitude;
		}
		
		public double getTopLatitude() {
			return topLatitude;
		}
		
		public double getBottomLatitude() {
			return bottomLatitude;
		}
		
		public RouteTypeRule quickGetEncodingRule(int id) {
			return routeEncodingRules.get(id);
		}

		public void initRouteEncodingRule(int id, String tags, String val) {
			while(routeEncodingRules.size() <= id) {
				routeEncodingRules.add(null);
			}
			routeEncodingRules.set(id, new RouteTypeRule(tags, val));
		}
		
		public List<RouteSubregion> getSubregions(){
			return subregions;
		}
	}
	
	public static class RouteSubregion {
		private final static int INT_SIZE = 4;
		public final RouteRegion routeReg;
		public RouteSubregion(RouteRegion routeReg) {
			this.routeReg = routeReg;
		}
		public RouteSubregion(RouteSubregion parentReg) {
			this.routeReg = parentReg.routeReg;
			this.left = parentReg.left;
			this.right = parentReg.right;
			this.top = parentReg.top;
			this.bottom = parentReg.bottom;
			
		}
		public int length;
		public int filePointer;
		public int left;
		public int right;
		public int top;
		public int bottom;
		public int shiftToData;
		public List<RouteSubregion> subregions = null;
		
		public int getEstimatedSize(){
			int shallow = 7 * INT_SIZE + 4 + 4/*list*/;
			if (subregions != null) {
				shallow += 8;
				for (RouteSubregion s : subregions) {
					shallow += s.getEstimatedSize();
				}
			}
			return shallow;
		}
	}
	
	private CodedInputStreamRAF codedIS;
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
			case OsmandOdb.OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER : {
				RouteSubregion subregion = new RouteSubregion(region);
				subregion.length = readInt();
				subregion.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(subregion.length);
				readRouteTree(subregion, true);
				region.getSubregions().add(subregion);
				codedIS.popLimit(oldLimit);
				codedIS.seek(subregion.filePointer + subregion.length);
			}	break;
			
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private RouteSubregion readRouteTree(RouteSubregion routeTree, boolean readChildren) throws IOException {
		if(readChildren) {
			routeTree.subregions = new ArrayList<BinaryMapRouteReaderAdapter.RouteSubregion>();
		}
		routeTree.routeReg.regionsRead++;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return routeTree;
			case RouteDataBox.LEFT_FIELD_NUMBER :
				routeTree.left += codedIS.readSInt32();
				break;
			case RouteDataBox.RIGHT_FIELD_NUMBER :
				routeTree.right += codedIS.readSInt32();
				break;
			case RouteDataBox.TOP_FIELD_NUMBER :
				routeTree.top += codedIS.readSInt32();
				break;
			case RouteDataBox.BOTTOM_FIELD_NUMBER :
				routeTree.bottom += codedIS.readSInt32();
				break;
			case RouteDataBox.SHIFTTODATA_FIELD_NUMBER :
				routeTree.shiftToData = readInt();
				break;
			case RouteDataBox.BOXES_FIELD_NUMBER :
				if(readChildren){
					RouteSubregion subregion = new RouteSubregion(routeTree);
					subregion.length = readInt();
					subregion.filePointer = codedIS.getTotalBytesRead();
					int oldLimit = codedIS.pushLimit(subregion.length);
					readRouteTree(subregion, readChildren);
					routeTree.subregions.add(subregion);
					codedIS.popLimit(oldLimit);
					codedIS.seek(subregion.filePointer + subregion.length);
				} else {
					skipUnknownField(t);
				}
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
	
}
