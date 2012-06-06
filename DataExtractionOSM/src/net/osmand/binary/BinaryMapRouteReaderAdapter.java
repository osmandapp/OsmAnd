package net.osmand.binary;

import java.io.IOException;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapRouteReaderAdapter {
	private static final Log LOG = LogUtil.getLog(BinaryMapRouteReaderAdapter.class);
	
	
	public static class RouteRegion extends BinaryIndexPart {

		double leftLongitude;
		double rightLongitude;
		double topLatitude;
		double bottomLatitude;
		
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
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndRoutingIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
}
