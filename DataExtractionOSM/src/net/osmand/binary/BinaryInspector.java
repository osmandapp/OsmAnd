package net.osmand.binary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.osmand.binary.BinaryMapIndexReader.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.TransportIndex;
import net.osmand.osm.MapUtils;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public class BinaryInspector {
	

	public static final int BUFFER_SIZE = 1 << 20;
	
	public static void main(String[] args) throws IOException {
		inspector(args);
		// test cases show info
//		inspector(new String[]{"E:\\Information\\OSM maps\\osmand\\Minsk.map.pbf"});
//		inspector(new String[]{"E:\\Information\\OSM maps\\osmand\\Belarus_4.map.pbf"});
//		inspector(new String[]{"E:\\Information\\OSM maps\\osm_map\\Poland.obf"});
//		inspector(new String[]{"E:\\Information\\OSM maps\\osm_map\\Netherlands\\Netherlands_trans.map.pbf"});
		
		// test case extract parts
//		inspector(new String[]{"-c", "E:\\Information\\OSM maps\\osmand\\Netherlands-addr-trans.map.pbf", 
//				"E:\\Information\\OSM maps\\osmand\\Netherlands.map.pbf", "-1"});
		
		// test case 
//		inspector(new String[]{"-c", "E:\\Information\\OSM maps\\osmand\\Netherlands-addr-trans.map.pbf", 
//				"E:\\Information\\OSM maps\\osmand\\Netherlands.map.pbf", "-1",
//				"E:\\Information\\OSM maps\\osmand\\Belarus_4.map.pbf", "E:\\Information\\OSM maps\\osmand\\Minsk.map.pbf"});
//		inspector(new String[]{"E:\\Information\\OSM maps\\osmand\\Netherlands-addr-trans.map.pbf"});
	}
	

	public static void inspector(String[] args) throws IOException {
		if(args == null || args.length == 0){
			printUsage(null);
			return;
		}
		String f = args[0];
		if(f.charAt(0) == '-'){
			// command
			if(f.equals("-c") || f.equals("-combine")) {
				if(args.length < 4){
					printUsage("Too few parameters to extract (require minimum 4)");
				} else {
					Map<File, String> parts = new LinkedHashMap<File, String>();
					for (int i = 2; i < args.length; i++) {
						File file = new File(args[i]);
						if (!file.exists()) {
							System.err.println("File to extract from doesn't exist " + args[i]);
							return;
						}
						parts.put(file, null);
						if(i < args.length - 1){
							if(args[i+1].startsWith("-") || args[i+1].startsWith("+")){
								parts.put(file, args[i+1]);
								i++;
							} 
						}
					}
					List<Integer> extracted = combineParts(new File(args[1]), parts);
					if(extracted != null){
						System.out.println("\n"+extracted.size()+" parts were successfully extracted to " + args[1]);
					}
				} 
			} else {
				printUsage("Unknown command : "+ f);
			}
		} else {
			File file = new File(f);
			if(!file.exists()){
				System.out.println("Binary OsmAnd index " + f + " was not found.");
				return;
			}
			printFileInformation(file);
		}
	}
	public static final void writeInt(CodedOutputStream ous, int v) throws IOException {
		ous.writeRawByte((v >>> 24) & 0xFF);
		ous.writeRawByte((v >>> 16) & 0xFF);
		ous.writeRawByte((v >>>  8) & 0xFF);
		ous.writeRawByte((v >>>  0) & 0xFF);
		//written += 4;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Integer> combineParts(File fileToExtract, Map<File, String> partsToExtractFrom) throws IOException {
		BinaryMapIndexReader[] indexes = new BinaryMapIndexReader[partsToExtractFrom.size()];
		RandomAccessFile[] rafs = new RandomAccessFile[partsToExtractFrom.size()];
		
		LinkedHashSet<Integer>[] partsSet = new LinkedHashSet[partsToExtractFrom.size()];
		int c = 0;
		Set<String> addressNames = new LinkedHashSet<String>();
		
		int version = -1;
		// Go through all files and validate conistency 
		for(File f : partsToExtractFrom.keySet()){
			if(f.getAbsolutePath().equals(fileToExtract.getAbsolutePath())){
				System.err.println("Error : Input file is equal to output file " + f.getAbsolutePath());
				return null;
			}
			rafs[c] = new RandomAccessFile(f, "r");
			indexes[c] = new BinaryMapIndexReader(rafs[c]);
			partsSet[c] = new LinkedHashSet<Integer>();
			if(version == -1){
				version = indexes[c].getVersion();
			} else {
				if(indexes[c].getVersion() != version){
					System.err.println("Error : Different input files has different input versions " + indexes[c].getVersion() + " != " + version);
					return null;
				}
			}
			
			LinkedHashSet<Integer> temp = new LinkedHashSet<Integer>();
			String pattern = partsToExtractFrom.get(f);
			boolean minus = true;
			if(pattern != null){
				minus = pattern.startsWith("-");
				String[] split = pattern.substring(1).split(",");
				for(String s : split){
					temp.add(Integer.parseInt(s));
				}
			}
			
			for (int i = 0; i < indexes[c].getIndexes().size(); i++) {
				partsSet[c].add(i + 1);
			}
			if(minus){
				partsSet[c].removeAll(temp);
			} else {
				partsSet[c].retainAll(temp);
			}
			
			c++;
		}
		
		// write files 
		FileOutputStream fout = new FileOutputStream(fileToExtract);
		CodedOutputStream ous = CodedOutputStream.newInstance(fout, BUFFER_SIZE);
		List<Integer> list = new ArrayList<Integer>();
		byte[] BUFFER_TO_READ = new byte[BUFFER_SIZE];
		
		ous.writeInt32(OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER, version);
		
		
		for (int k = 0; k < indexes.length; k++) {
			LinkedHashSet<Integer> partSet = partsSet[k];
			BinaryMapIndexReader index = indexes[k];
			RandomAccessFile raf = rafs[k];
			for (int i = 0; i < index.getIndexes().size(); i++) {
				if (!partSet.contains(i + 1)) {
					continue;
				}
				list.add(i + 1);

				BinaryIndexPart part = index.getIndexes().get(i);
				String map;
				if (part instanceof AddressRegion) {
					ous.writeTag(OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Address";
					if (addressNames.contains(part.getName())) {
						System.err.println("Error : going to merge 2 same addresses skip " + part.getName());
						continue;
					}
					addressNames.add(part.getName());
				} else if (part instanceof TransportIndex) {
					ous.writeTag(OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Transport";
				} else if (part instanceof MapIndex) {
					ous.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Map";
				} else {
					throw new UnsupportedOperationException();
				}
				writeInt(ous, part.getLength());
				raf.seek(part.getFilePointer());
				int toRead = part.getLength();
				while (toRead > 0) {
					int read = raf.read(BUFFER_TO_READ);
					if (read == -1) {
						throw new IllegalArgumentException("Unexpected end of file");
					}
					if (toRead < read) {
						read = toRead;
					}
					ous.writeRawBytes(BUFFER_TO_READ, 0, read);
					toRead -= read;
				}
				System.out.println(MessageFormat.format("{2} part {0} is extracted {1} bytes", part.getName(), part.getLength(), map));
			}
		}
		
		ous.writeInt32(OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER, version);
		ous.flush();
		fout.close();
		
		
		return list;
	}
	

	protected static String formatBounds(int left, int right, int top, int bottom){
		double l = MapUtils.get31LongitudeX(left);
		double r = MapUtils.get31LongitudeX(right);
		double t = MapUtils.get31LatitudeY(top);
		double b = MapUtils.get31LatitudeY(bottom);
		MessageFormat format = new MessageFormat("(left top - right bottom) : {0}, {1} NE - {2}, {3} NE", Locale.US);
		return format.format(new Object[]{l, t, r, b}); 
	}
	
	
	public static void printFileInformation(File file) throws IOException {
		RandomAccessFile r = new RandomAccessFile(file.getAbsolutePath(), "r");
		try {
			BinaryMapIndexReader index = new BinaryMapIndexReader(r);
			int i = 1;
			System.out.println("Binary index " + file.getName() + " version = " + index.getVersion());
			for(BinaryIndexPart p : index.getIndexes()){
				String partname = "";
				if(p instanceof MapIndex ){
					partname = "Map";
				} else if(p instanceof TransportIndex){
					partname = "Transport";
				} else if(p instanceof AddressRegion){
					partname = "Address";
				}
				String name = p.getName() == null ? "" : p.getName(); 
				System.out.println(MessageFormat.format("{0}. {1} data {3} - {2} bytes", i, partname, p.getLength(), name));
				if(p instanceof TransportIndex){
					TransportIndex ti = ((TransportIndex) p);
					int sh = (31 - BinaryMapIndexReader.TRANSPORT_STOP_ZOOM);
					System.out.println("\t Bounds " + formatBounds(ti.getLeft() << sh, ti.getRight() << sh, 
							ti.getTop() << sh, ti.getBottom() << sh));
				} else if(p instanceof MapIndex){
					MapIndex m = ((MapIndex) p);
					for(MapRoot mi : m.getRoots()){
						System.out.println(MessageFormat.format("\tMap level minZoom = {0}, maxZoom = {1}, size = {2} bytes \n\t\tBounds {3}",
								mi.getMinZoom(), mi.getMaxZoom(), mi.getLength(), 
								formatBounds(mi.getLeft(), mi.getRight(), mi.getTop(), mi.getBottom())));
					}
				}
				i++;
			}
			
			
		} catch (IOException e) {
			System.err.println("File is not valid index : " + file.getAbsolutePath());
			throw e;
		}
		
	}

	public static void printUsage(String warning) {
		if(warning != null){
			System.out.println(warning);
		}
		System.out.println("Inspector is console utility for working with binary indexes of OsmAnd.");
		System.out.println("It allows print info about file, extract parts and merge indexes.");
		System.out.println("\nUsage for print info : inspector [file]");
		System.out.println("  Prints information about [file] binary index of OsmAnd.");
		System.out.println("\nUsage for combining indexes : inspector -c file_to_create (file_from_extract ((+|-)parts_to_extract)? )*");
		System.out.println("\tCreate new file of extracted parts from input file. [parts_to_extract] could be parts to include or exclude.");
		System.out.println("  Example : inspector -c output_file input_file +1,2,3\n\tExtracts 1, 2, 3 parts (could be find in print info)");
		System.out.println("  Example : inspector -c output_file input_file -2,3\n\tExtracts all  parts excluding 2, 3");
		System.out.println("  Example : inspector -c output_file input_file1 input_file2 input_file3\n\tSimply combine 3 files");
		System.out.println("  Example : inspector -c output_file input_file1 input_file2 -4\n\tCombine all parts of 1st file and all parts excluding 4th part of 2nd file");
		
	}

}
