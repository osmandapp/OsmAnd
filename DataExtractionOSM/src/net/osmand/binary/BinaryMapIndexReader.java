package net.osmand.binary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	private final RandomAccessFile raf;
	private int version;
	private List<MapRoot> mapIndexes = new ArrayList<MapRoot>();
	private CodedInputStreamRAF codedIS;
	
	public class MapRoot {
		int minZoom = 0;
		int maxZoom = 0;
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		List<MapTree> trees = new ArrayList<MapTree>();
	}
	
	public class MapTree {
		int filePointer = 0;
		int length = 0;
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		
		List<String> stringTable = null;
		List<MapTree> subTrees = null;
		List<MapObject> children = null;
		
	}
	
	public class MapObject {
		
	}

	
	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 256);
		init();
	}

	private void init() throws IOException {
		while(true){
			int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER :
				version = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER:
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(length);
				readMapIndex();
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				break;
			default:
				// TODO skip unknown fields
				return;
			}
		}
	}
	
	private void readMapIndex() throws IOException {
		while(true){
			int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER :
				int length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(length);
				MapRoot mapRoot = readMapLevel();
				mapIndexes.add(mapRoot);
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				break;
			default:
				// TODO skip unknown fields
				return;
			}
		}
	}

	private MapRoot readMapLevel() throws IOException {
		MapRoot root = new MapRoot();
		while(true){
			int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
			switch (tag) {
			case 0:
				return root;
			case OsmandOdb.MapRootLevel.BOTTOM_FIELD_NUMBER :
				root.bottom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.LEFT_FIELD_NUMBER :
				root.left = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.RIGHT_FIELD_NUMBER :
				root.right = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.TOP_FIELD_NUMBER :
				root.top = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.MAXZOOM_FIELD_NUMBER :
				root.maxZoom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.MINZOOM_FIELD_NUMBER :
				root.minZoom = codedIS.readInt32();
				break;
			case OsmandOdb.MapRootLevel.ROOT_FIELD_NUMBER :
				MapTree r = new MapTree();
				// left, ... already initialized 
				r.length = readInt();
				r.filePointer = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(r.length);
				readMapTreeBounds(r, root.left, root.right, root.top, root.bottom);
				root.trees.add(r);
				codedIS.popLimit(oldLimit);
				codedIS.seek(r.filePointer + r.length);
				break;
			default:
				// TODO skip unknown fields
				return root;
			}
		}
		
	}
	
	private void readMapTreeBounds(MapTree tree, int aleft, int aright, int atop, int abottom) throws IOException {
		while(true){
			int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER :
				tree.bottom = codedIS.readSInt32() + abottom;
				break;
			case OsmandOdb.MapTree.LEFT_FIELD_NUMBER :
				tree.left = codedIS.readSInt32() + aleft;
				break;
			case OsmandOdb.MapTree.RIGHT_FIELD_NUMBER :
				tree.right = codedIS.readSInt32() + aright;
				break;
			case OsmandOdb.MapTree.TOP_FIELD_NUMBER :
				tree.top = codedIS.readSInt32() + atop;
				break;
			default:
				// TODO skip unknown fields
				return;
			}
		}
	}

	public RandomAccessFile getRaf() {
		return raf;
	}
	
	public int readByte() throws IOException{
		byte b = codedIS.readRawByte();
		if(b < 0){
			return b + 256;
		} else {
			return b;
		}
	}
	
	public final int readInt() throws IOException {
		int ch1 = readByte();
		int ch2 = readByte();
		int ch3 = readByte();
		int ch4 = readByte();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	public List<MapRoot> getMapIndexes() {
		return mapIndexes;
	}
	
	public int getVersion() {
		return version;
	}
	
	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Minsk.map.pbf"), "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		System.out.println(reader.getVersion());
		for(MapRoot b : reader.getMapIndexes()) {
			System.out.println(b.minZoom + " " + b.maxZoom + " " +b.trees.size() + " " 
					+ b.left + " " + b.right + " " + b.top + " " + b.bottom);
		}
	}
	
}
