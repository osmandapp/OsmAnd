package net.osmand.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import net.osmand.Algoritms;
import net.osmand.data.index.IndexConstants;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexWriter {

	private RandomAccessFile raf;
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
	private Stack<Long> stackSizes = new Stack<Long>();
	
	private final static int OSMAND_STRUCTURE_INIT = 1;
	private final static int MAP_INDEX_INIT = 2;
	private final static int MAP_ROOT_LEVEL_INIT = 3;
	private final static int MAP_TREE = 4;

	public BinaryMapIndexWriter(final RandomAccessFile raf) throws IOException{
		this.raf = raf;
		codedOutStream = CodedOutputStream.newInstance(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				raf.write(b);
			}
			
			@Override
			public void write(byte[] b) throws IOException {
				raf.write(b);
			}
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				raf.write(b, off, len);
			}
			
		});
		codedOutStream.writeInt32(OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER, IndexConstants.BINARY_MAP_VERSION);
		state.push(OSMAND_STRUCTURE_INIT);
	}

	private void preserveInt32Size() throws IOException {
		codedOutStream.flush();
		stackSizes.push(raf.getFilePointer());
		codedOutStream.writeFixed32NoTag(0);
	}
	
	private void writeInt32Size() throws IOException{
		codedOutStream.flush();
		long filePointer = raf.getFilePointer();
		Long old = stackSizes.pop();
		int length = (int) (filePointer - old - 4);
		raf.seek(old);
		raf.writeInt(length);
		raf.seek(filePointer);
	}
	
	public void startWriteMapIndex() throws IOException{
		assert state.peek() == OSMAND_STRUCTURE_INIT;
		state.push(MAP_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
	}
	
	
	public void endWriteMapIndex() throws IOException{
		Integer st = state.pop();
		assert st == MAP_INDEX_INIT;
		writeInt32Size();
	}
		
	public void startWriteMapLevelIndex(int minZoom, int maxZoom, int leftX, int rightX, int topY, int bottomY) throws IOException{
		assert state.peek() == MAP_INDEX_INIT;
		state.push(MAP_ROOT_LEVEL_INIT);
		
		codedOutStream.writeTag(OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		
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
		writeInt32Size();
	}
	
	public void startMapTreeElement(int leftX, int rightX, int topY, int bottomY) throws IOException{
		startMapTreeElement(-1L, leftX, rightX, topY, bottomY);
	}
	
	public void startMapTreeElement(long baseId, int leftX, int rightX, int topY, int bottomY) throws IOException{
		assert state.peek() == MAP_ROOT_LEVEL_INIT || state.peek() == MAP_TREE;
		if(state.peek() == MAP_ROOT_LEVEL_INIT){
			codedOutStream.writeTag(OsmandOdb.MapRootLevel.ROOT_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		} else {
			codedOutStream.writeTag(OsmandOdb.MapTree.SUBTREES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		}
		preserveInt32Size();
		state.push(MAP_TREE);
		
		
		Bounds bounds = stackBounds.peek();
		codedOutStream.writeSInt32(OsmandOdb.MapTree.LEFT_FIELD_NUMBER, leftX - bounds.leftX);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.RIGHT_FIELD_NUMBER, rightX - bounds.rightX);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.TOP_FIELD_NUMBER, topY - bounds.topY);
		codedOutStream.writeSInt32(OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER, bottomY - bounds.bottomY);
		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
		stackBaseIds.push(baseId);
		stackStringTable.push(null);
	}
	
	public void endWriteMapTreeElement() throws IOException{
		assert state.peek() == MAP_TREE;
		state.pop();
		
		stackBounds.pop();
		Long l = stackBaseIds.pop();
		if(l != -1){
			codedOutStream.writeTag(OsmandOdb.MapTree.BASEID_FIELD_NUMBER, WireFormat.FieldType.UINT64.getWireType());
			codedOutStream.writeUInt64NoTag(l);
		}
		Map<String, Integer> map = stackStringTable.peek();
		if(map != null){
			
			int i = 0;
			int size = 0;
			for(String s : map.keySet()){
				Integer integer = map.get(s);
				if(integer != i){
					throw new IllegalStateException();
				}
				i++;
				size += CodedOutputStream.computeStringSize(OsmandOdb.StringTable.S_FIELD_NUMBER, s);
			}
			codedOutStream.writeTag(OsmandOdb.MapTree.STRINGTABLE_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
			STRING_TABLE_SIZE += CodedOutputStream.computeTagSize(OsmandOdb.MapTree.STRINGTABLE_FIELD_NUMBER) + 
						CodedOutputStream.computeRawVarint32Size(size) + size;
			codedOutStream.writeRawVarint32(size);
			for(String s : map.keySet()){
				codedOutStream.writeString(OsmandOdb.StringTable.S_FIELD_NUMBER, s);
			}
		}
		writeInt32Size();
	}
	
	public static int COORDINATES_SIZE = 0;
	public static int ID_SIZE = 0;
	public static int TYPES_SIZE = 0;
	public static int MAP_DATA_SIZE = 0;
	public static int STRING_TABLE_SIZE = 0;
	
	public void writeMapData(long id, byte[] nodes, byte[] types, String name, int highwayAttributes, byte[] restrictions) throws IOException{
		assert state.peek() == MAP_TREE;
		
		
		Bounds bounds = stackBounds.peek();
		if(stackBaseIds.peek() == -1){
			stackBaseIds.pop();
			stackBaseIds.push(id);
		}
		// calculate size
		int sizeCoordinates = 0;
		int allSize = 0;
		int px = bounds.leftX;
		int py = bounds.topY;
		for(int i=0; i< nodes.length / 8; i++){
			int x = Algoritms.parseIntFromBytes(nodes, i*8);
			int y = Algoritms.parseIntFromBytes(nodes, i*8 + 4);
			sizeCoordinates += CodedOutputStream.computeSInt32SizeNoTag(x - px);
			sizeCoordinates += CodedOutputStream.computeSInt32SizeNoTag(y - py);
			px = x;
			py = y;
		}
		allSize += CodedOutputStream.computeRawVarint32Size(sizeCoordinates) + 
				CodedOutputStream.computeTagSize(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER) + sizeCoordinates;
		// DEBUG
		COORDINATES_SIZE += allSize;

		
		allSize += CodedOutputStream.computeTagSize(OsmandOdb.MapData.TYPES_FIELD_NUMBER);
		allSize += CodedOutputStream.computeRawVarint32Size(types.length);
		allSize += types.length;
		// DEBUG
		TYPES_SIZE += CodedOutputStream.computeTagSize(OsmandOdb.MapData.TYPES_FIELD_NUMBER) + 
				CodedOutputStream.computeRawVarint32Size(types.length) + types.length; 
		
		
		allSize += CodedOutputStream.computeSInt64Size(OsmandOdb.MapData.ID_FIELD_NUMBER, id - stackBaseIds.peek());
		// DEBUG 
		ID_SIZE += CodedOutputStream.computeSInt64Size(OsmandOdb.MapData.ID_FIELD_NUMBER, id - stackBaseIds.peek());
		
		
		int nameId = 0;
		if(name != null){
			if(stackStringTable.peek() == null) {
				stackStringTable.pop();
				stackStringTable.push(new LinkedHashMap<String, Integer>());
			}
			Map<String, Integer> map = stackStringTable.peek();
			if(map.containsKey(name)) {
				nameId = map.get(name);
			} else {
				nameId = map.size();
				map.put(name, nameId);
			}
			allSize += CodedOutputStream.computeUInt32Size(OsmandOdb.MapData.STRINGID_FIELD_NUMBER, nameId);
		}
		
		int restrictionsSize = 0;
		if(restrictions.length > 0){
			allSize += CodedOutputStream.computeTagSize(OsmandOdb.MapData.RESTRICTIONS_FIELD_NUMBER);
			for (int i = 0; i < restrictions.length / 8; i++) {
				long l = Algoritms.parseLongFromBytes(restrictions, i * 8) - stackBaseIds.peek();
				restrictionsSize += CodedOutputStream.computeSInt64SizeNoTag(l);
			}
			allSize += CodedOutputStream.computeRawVarint32Size(restrictionsSize);
			allSize += restrictionsSize;
		}
		if(highwayAttributes != 0){
			allSize += CodedOutputStream.computeInt32Size(OsmandOdb.MapData.HIGHWAYMETA_FIELD_NUMBER, highwayAttributes);
		}
		
		// DEBUG
		MAP_DATA_SIZE += allSize;
		
		// writing data
		codedOutStream.writeTag(OsmandOdb.MapTree.LEAFS_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		codedOutStream.writeRawVarint32(allSize);
		
		
		codedOutStream.writeTag(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
		codedOutStream.writeRawVarint32(sizeCoordinates);
		
		px =  bounds.leftX;
		py = bounds.topY;
		for (int i = 0; i < nodes.length / 8; i++) {
			int x = Algoritms.parseIntFromBytes(nodes, i*8);
			int y = Algoritms.parseIntFromBytes(nodes, i*8 + 4);
			codedOutStream.writeSInt32NoTag(x - px);
			codedOutStream.writeSInt32NoTag(y - py);
			px = x;
			py = y;
		}
		
		
		codedOutStream.writeTag(OsmandOdb.MapData.TYPES_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
		codedOutStream.writeRawVarint32(types.length);
		codedOutStream.writeRawBytes(types);
		
		codedOutStream.writeSInt64(OsmandOdb.MapData.ID_FIELD_NUMBER, id - stackBaseIds.peek());
		
		
		if(name != null){
			codedOutStream.writeUInt32(OsmandOdb.MapData.STRINGID_FIELD_NUMBER, nameId);
		} 
		
		if(restrictions.length > 0){
			codedOutStream.writeTag(OsmandOdb.MapData.RESTRICTIONS_FIELD_NUMBER, WireFormat.FieldType.BYTES.getWireType());
			codedOutStream.writeRawVarint32(restrictionsSize);
			for (int i = 0; i < restrictions.length / 8; i++) {
				long l = Algoritms.parseLongFromBytes(restrictions, i * 8) - stackBaseIds.peek();
				codedOutStream.writeSInt64NoTag(l);
			}
		}
		if(highwayAttributes != 0){
			codedOutStream.writeInt32(OsmandOdb.MapData.HIGHWAYMETA_FIELD_NUMBER, highwayAttributes);
		}
	}
	 
	
	public void close() throws IOException{
		assert state.peek() == OSMAND_STRUCTURE_INIT;
		codedOutStream.flush();
	}
}
