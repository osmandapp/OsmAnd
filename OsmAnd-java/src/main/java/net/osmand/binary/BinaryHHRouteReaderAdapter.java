package net.osmand.binary;

import java.io.IOException;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStream;

import net.osmand.PlatformUtil;

public class BinaryHHRouteReaderAdapter {
	protected static final Log LOG = PlatformUtil.getLog(BinaryHHRouteReaderAdapter.class);

	public static class HHRouteRegion extends BinaryIndexPart {
		public long edition;
		public String profile;

		@Override
		public String getPartName() {
			return "Highway routing";
		}

		@Override
		public int getFieldNumber() {
			return OsmandOdb.OsmAndStructure.HHROUTINGINDEX_FIELD_NUMBER;
		}
	}

	protected CodedInputStream codedIS;
	private final BinaryMapIndexReader map;

	protected BinaryHHRouteReaderAdapter(BinaryMapIndexReader map) {
		this.codedIS = map.codedIS;
		this.map = map;
	}

	protected void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}

	protected int readInt() throws IOException {
		return map.readInt();
	}

	public void readHHIndex(HHRouteRegion hhreg) {
		// TODO Auto-generated method stub
		
	}

}
