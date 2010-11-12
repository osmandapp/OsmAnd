package net.osmand.binary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Locale;

import net.osmand.binary.BinaryMapIndexReader.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.TransportIndex;
import net.osmand.osm.MapUtils;

public class BinaryInspector {
	
	
	
	public static void main(String[] args) throws IOException {
		args = new String[]{"E:\\Information\\OSM maps\\osmand\\Luxembourg.map.pbf"};
//		args = new String[]{"E:\\Information\\OSM maps\\osmand\\Minsk.map.pbf"};
//		args = new String[]{"E:\\Information\\OSM maps\\osmand\\Belarus_4.map.pbf"};
//		args = new String[]{"E:\\Information\\OSM maps\\osmand\\Netherlands.map.pbf"};
//		args = new String[]{"E:\\Information\\OSM maps\\osm_map\\Netherlands\\Netherlands_trans.map.pbf"};
		
		
		inspector(args);
	}

	public static void inspector(String[] args) throws IOException {
		if(args == null || args.length == 0){
			printUsage(null);
		}
		String f = args[0];
		if(f.charAt(0) == '-'){
			// command
		} else {
			File file = new File(f);
			if(!file.exists()){
				System.out.println("Binary OsmAnd index " + f + " was not found.");
				return;
			}
			printFileInformation(file);
		}
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
		System.out.println("\nUsage : inspector [file]");
		System.out.println("\tPrints information about [file] binary index of OsmAnd.");
		
	}

}
