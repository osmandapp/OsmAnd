package net.osmand.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import net.osmand.Algoritms;
import net.osmand.data.index.IndexConstants;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public class BinaryIndexWriter {

	private final OutputStream out;
	private CodedOutputStream codedOutStream;
	
	private static class Bounds {
		public Bounds(int leftX, int rightX, int topY, int bottomY) {
			super();
			this.bottomY = bottomY;
			this.leftX = leftX;
			this.rightX = rightX;
			this.topY = topY;
		}
		int leftX = 0;
		int rightX = 0;
		int topY = 0;
		int bottomY = 0;
		
		
	}
	private Stack<Bounds> stackBounds = new Stack<Bounds>();
	// needed for map tree
	private Stack<Long> stackBaseIds = new Stack<Long>();
	private Stack<Map<String, Integer>> stackStringTable = new Stack<Map<String, Integer>>();
	
	// internal constants to track state of index writing
	private Stack<Integer> state = new Stack<Integer>();
	private final static int OSMAND_STRUCTURE_INIT = 1;
	private final static int MAP_INDEX_INIT = 2;
	private final static int MAP_ROOT_LEVEL_INIT = 3;
	private final static int MAP_TREE = 4;

	public BinaryIndexWriter(OutputStream out) throws IOException{
		this.out = out;
		codedOutStream = CodedOutputStream.newInstance(out);
		codedOutStream.writeInt32(OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER, IndexConstants.BINARY_MAP_VERSION);
		state.push(OSMAND_STRUCTURE_INIT);
	}
//		message MapTree {
//		   required sint32 left = 1; // delta encoded
//		   required sint32 right = 2; // delta encoded
//		   required sint32 top = 3; // delta encoded
//		   required sint32 bottom = 4; // delta encoded
//		   
//		   optional StringTable stringTable = 5;
//		   optional uint64 baseId = 6;
//		   
//		   repeated MapTree subtrees = 7;
//		   repeated MapData leafs = 8;
//		   
//		}
//	/// Simple messages
//	message MapData {
//		  required bytes coordinates = 1; // array of delta x,y uin32 could be read by codedinputstream
//		  required bytes types = 2; // array of fixed int16
//		  
//		  required sint64 id = 3; // delta encoded
//		  optional uint32 stringId = 4;
//		  
//		  repeated sint64 restrictions = 5; // delta encoded 3 bytes for type and other for id
//		  optional int32 highwayMeta = 6; 
//		}

	public void startWriteMapIndex() throws IOException{
		assert state.peek() == OSMAND_STRUCTURE_INIT;
		state.push(MAP_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		// TODO write size of map index 
		codedOutStream.writeFixed32NoTag(0);
		
	}
	
	public void endWriteMapIndex() throws IOException{
		Integer st = state.pop();
		assert st == MAP_INDEX_INIT;
		codedOutStream.writeRawVarint32(0);
	}
		
	public void startWriteMapLevelIndex(int maxZoom, int minZoom, int leftX, int rightX, int topY, int bottomY) throws IOException{
		assert state.peek() == MAP_INDEX_INIT;
		state.push(MAP_ROOT_LEVEL_INIT);
		
		codedOutStream.writeTag(OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		// TODO write size of level map index 
		codedOutStream.writeFixed32NoTag(0);
		
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.MAXZOOM_FIELD_NUMBER, maxZoom);
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.MINZOOM_FIELD_NUMBER, minZoom);
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.LEFT_FIELD_NUMBER, leftX);
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.RIGHT_FIELD_NUMBER, rightX);
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.TOP_FIELD_NUMBER, topY);
		codedOutStream.writeInt32(OsmandOdb.MapRootLevel.BOTTOM_FIELD_NUMBER, bottomY);
		
		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
	}
	
	public void endWriteMapLevelIndex() throws IOException{
		assert state.peek() == MAP_ROOT_LEVEL_INIT;
		state.pop();
		
		stackBounds.pop();
		codedOutStream.writeRawVarint32(0);
	}
	
	public void startMapTreeElement(int leftX, int rightX, int topY, int bottomY) throws IOException{
		assert state.peek() == MAP_ROOT_LEVEL_INIT || state.peek() == MAP_TREE;
		if(state.peek() == MAP_ROOT_LEVEL_INIT){
			codedOutStream.writeTag(OsmandOdb.MapRootLevel.ROOT_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		} else {
			codedOutStream.writeTag(OsmandOdb.MapTree.SUBTREES_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		}
		// TODO write size of level map index 
		codedOutStream.writeFixed32NoTag(0);
		state.push(MAP_TREE);
		
		
		Bounds bounds = stackBounds.peek();
		codedOutStream.writeSInt32(OsmandOdb.MapTree.LEFT_FIELD_NUMBER, leftX - bounds.leftX);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.RIGHT_FIELD_NUMBER, rightX - bounds.rightX);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.TOP_FIELD_NUMBER, topY - bounds.topY);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER, bottomY - bounds.bottomY);
		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
		stackBaseIds.push(0L);
		stackStringTable.push(null);
	}
	
	public void endWriteMapTreeElement() throws IOException{
		assert state.peek() == MAP_TREE;
		state.pop();
		
		stackBounds.pop();
		Long l = stackBaseIds.pop();
		if(l != 0){
			codedOutStream.writeTag(OsmandOdb.MapTree.BASEID_FIELD_NUMBER, WireFormat.FieldType.UINT64.getWireType());
			codedOutStream.writeUInt64NoTag(l);
		}
		Map<String, Integer> map = stackStringTable.peek();
		if(map != null){
			codedOutStream.writeTag(OsmandOdb.MapTree.STRINGTABLE_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
			// TODO write size
			codedOutStream.writeFixed32NoTag(0);
			
			int i = 0;
			for(String s : map.keySet()){
				Integer integer = map.get(s);
				if(integer != i){
					throw new IllegalStateException();
				}
				i++;
				codedOutStream.writeTag(OsmandOdb.StringTable.S_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
				codedOutStream.writeStringNoTag(s);
			}
		}
	}
	
	public void writeMapData(long id, byte[] nodes, byte[] types, String name, int highwayAttributes, byte[] restrictions) throws IOException{
		assert state.peek() == MAP_TREE;
		codedOutStream.writeTag(OsmandOdb.MapTree.LEAFS_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		// TODO write size of map data  !!! here
		
		Bounds bounds = stackBounds.peek();	
		codedOutStream.writeTag(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
		int size = 0;
		for(int i=0; i< nodes.length / 8; i++){
			int x = Algoritms.parseIntFromBytes(nodes, i*8) - bounds.leftX;
			int y = Algoritms.parseIntFromBytes(nodes, i*8 + 4) - bounds.topY;
			size += CodedOutputStream.computeInt32SizeNoTag(x);
			size += CodedOutputStream.computeInt32SizeNoTag(y);
		}
		codedOutStream.writeRawVarint32(size);
		for(int i=0; i< nodes.length / 8; i++){
			int x = Algoritms.parseIntFromBytes(nodes, i*8) - bounds.leftX;
			int y = Algoritms.parseIntFromBytes(nodes, i*8 + 4) - bounds.topY;
			codedOutStream.writeInt32NoTag(x);
			codedOutStream.writeInt32NoTag(y);
		}
		
		codedOutStream.writeTag(OsmandOdb.MapData.TYPES_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
		codedOutStream.writeRawVarint32(types.length);
		codedOutStream.writeRawBytes(types);
		
		if(stackBaseIds.peek() == 0){
			stackBaseIds.pop();
			stackBaseIds.push(id);
		}
		codedOutStream.writeTag(OsmandOdb.MapData.ID_FIELD_NUMBER, WireFormat.FieldType.SINT64.getWireType());
		codedOutStream.writeSInt64NoTag(id - stackBaseIds.peek());
		
		if(name != null){
			if(stackStringTable.peek() == null) {
				stackStringTable.pop();
				stackStringTable.push(new LinkedHashMap<String, Integer>());
			}
			Map<String, Integer> map = stackStringTable.peek();
			int s;
			if(map.containsKey(name)) {
				s = map.get(name);
			} else {
				s = map.size();
				map.put(name, s);
			}
			codedOutStream.writeTag(OsmandOdb.MapData.STRINGID_FIELD_NUMBER, WireFormat.FieldType.UINT32.getWireType());
			codedOutStream.writeUInt32NoTag(s);
		}
		
		if(restrictions.length > 0){
			// TODO restrictions delta?
			codedOutStream.writeTag(OsmandOdb.MapData.RESTRICTIONS_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
			codedOutStream.writeRawVarint32(restrictions.length);
			codedOutStream.writeRawBytes(restrictions);
		}
		if(highwayAttributes != 0){
			codedOutStream.writeTag(OsmandOdb.MapData.HIGHWAYMETA_FIELD_NUMBER, WireFormat.FieldType.UINT32.getWireType());
			codedOutStream.writeRawVarint32(highwayAttributes);
		}
		
	}
	 
	
	public void close() throws IOException{
		assert state.peek() == OSMAND_STRUCTURE_INIT;
		codedOutStream.flush();
		out.close();
	}
}
