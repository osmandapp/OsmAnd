package net.osmand.binary;


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

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

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.osm.MapUtils;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public class BinaryInspector {
	

	public static final int BUFFER_SIZE = 1 << 20;
	
	public static void main(String[] args) throws IOException {
		inspector(args);
		// test cases show info
		
		
		 inspector(new String[]{/*"-vaddress", "-bbox=-121.785,37.35,-121.744,37.33", */"/home/victor/projects/OsmAnd/data/osm-gen/Netherlands_europe.obf"});
		// test case extract parts
		// test case 
	}
	
	private static void println(String s) {
		System.out.println(s);
	}
	
	private static void print(String s) {
		System.out.print(s);
	}
	
	protected static class VerboseInfo {
		boolean vaddress;
		boolean vtransport;
		boolean vpoi;
		boolean vmap;
		double lattop = 85;
		double latbottom = -85;
		double lonleft = -180;
		double lonright = 180;
		int zoom = 15;
		
		public boolean isVaddress() {
			return vaddress;
		}
		
		public int getZoom() {
			return zoom;
		}
		
		public boolean isVmap() {
			return vmap;
		}
		public boolean isVpoi() {
			return vpoi;
		}
		
		public boolean isVtransport() {
			return vtransport;
		}
		
		public VerboseInfo(String[] params){
			for(int i=0;i<params.length;i++){
				if(params[i].equals("-vaddress")){
					vaddress = true;
				} else if(params[i].equals("-vmap")){
					vmap = true;
				} else if(params[i].equals("-vpoi")){
					vpoi = true;
				} else if(params[i].equals("-vtransport")){
					vtransport = true;
				} else if(params[i].startsWith("-zoom=")){
					zoom = Integer.parseInt(params[i].substring("-zoom=".length()));
				} else if(params[i].startsWith("-bbox=")){
					String[] values = params[i].substring("-bbox=".length()).split(",");
					lonleft = Double.parseDouble(values[0]);
					lattop = Double.parseDouble(values[1]);
					lonright = Double.parseDouble(values[2]);
					latbottom = Double.parseDouble(values[3]);
				}
			}
		}
		
		public boolean contains(MapObject o){
			return lattop >= o.getLocation().getLatitude() && latbottom <= o.getLocation().getLatitude()
					&& lonleft <= o.getLocation().getLongitude() && lonright >= o.getLocation().getLongitude();
			
		}
	}

	public static void inspector(String[] args) throws IOException {
		if(args == null || args.length == 0){
			printUsage(null);
			return;
		}
		String f = args[0];
		if (f.charAt(0) == '-') {
			// command
			if (f.equals("-c") || f.equals("-combine")) {
				if (args.length < 4) {
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
						if (i < args.length - 1) {
							if (args[i + 1].startsWith("-") || args[i + 1].startsWith("+")) {
								parts.put(file, args[i + 1]);
								i++;
							}
						}
					}
					List<Float> extracted = combineParts(new File(args[1]), parts);
					if (extracted != null) {
						println("\n" + extracted.size() + " parts were successfully extracted to " + args[1]);
					}
				}
			} else if (f.startsWith("-v")) {
				if (args.length < 2) {
					printUsage("Missing file parameter");
				} else {
					VerboseInfo vinfo = new VerboseInfo(args);
					printFileInformation(args[args.length - 1], vinfo);
				}
			} else {
				printUsage("Unknown command : " + f);
			}
		} else {
			printFileInformation(f, null);
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
	public static List<Float> combineParts(File fileToExtract, Map<File, String> partsToExtractFrom) throws IOException {
		BinaryMapIndexReader[] indexes = new BinaryMapIndexReader[partsToExtractFrom.size()];
		RandomAccessFile[] rafs = new RandomAccessFile[partsToExtractFrom.size()];
		
		LinkedHashSet<Float>[] partsSet = new LinkedHashSet[partsToExtractFrom.size()];
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
			partsSet[c] = new LinkedHashSet<Float>();
			if(version == -1){
				version = indexes[c].getVersion();
			} else {
				if(indexes[c].getVersion() != version){
					System.err.println("Error : Different input files has different input versions " + indexes[c].getVersion() + " != " + version);
					return null;
				}
			}
			
			LinkedHashSet<Float> temp = new LinkedHashSet<Float>();
			String pattern = partsToExtractFrom.get(f);
			boolean minus = true;
			for (int i = 0; i < indexes[c].getIndexes().size(); i++) {
				partsSet[c].add(i + 1f);
				BinaryIndexPart part = indexes[c].getIndexes().get(i);
				if(part instanceof MapIndex){
					List<MapRoot> roots = ((MapIndex) part).getRoots();
					int rsize = roots.size(); 
					for(int j=0; j<rsize; j++){
						partsSet[c].add((i+1f)+(j+1)/10f);
					}
				}
			}
			if(pattern != null){
				minus = pattern.startsWith("-");
				String[] split = pattern.substring(1).split(",");
				for(String s : split){
					temp.add(Float.parseFloat(s));
				}
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
		List<Float> list = new ArrayList<Float>();
		byte[] BUFFER_TO_READ = new byte[BUFFER_SIZE];
		
		ous.writeInt32(OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER, version);
		ous.writeInt64(OsmandOdb.OsmAndStructure.DATECREATED_FIELD_NUMBER, System.currentTimeMillis());
		
		
		for (int k = 0; k < indexes.length; k++) {
			LinkedHashSet<Float> partSet = partsSet[k];
			BinaryMapIndexReader index = indexes[k];
			RandomAccessFile raf = rafs[k];
			for (int i = 0; i < index.getIndexes().size(); i++) {
				if (!partSet.contains(i + 1f)) {
					continue;
				}
				list.add(i + 1f);

				BinaryIndexPart part = index.getIndexes().get(i);
				String map;
				
				if (part instanceof MapIndex) {
					ous.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Map";
				} else if (part instanceof AddressRegion) {
					ous.writeTag(OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Address";
					if (addressNames.contains(part.getName())) {
						System.err.println("Error : going to merge 2 addresses with same names. Skip " + part.getName());
						continue;
					}
					addressNames.add(part.getName());
				} else if (part instanceof TransportIndex) {
					ous.writeTag(OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Transport";
				} else if (part instanceof PoiRegion) {
					ous.writeTag(OsmandOdb.OsmAndStructure.POIINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "POI";
				} else if (part instanceof RouteRegion) {
					ous.writeTag(OsmandOdb.OsmAndStructure.ROUTINGINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
					map = "Routing";
				} else {
					throw new UnsupportedOperationException();
				}
				writeInt(ous, part.getLength());
				copyBinaryPart(ous, BUFFER_TO_READ, raf, part.getFilePointer(), part.getLength());
				println(MessageFormat.format("{2} part {0} is extracted {1} bytes", part.getName(), part.getLength(), map));
				
			}
		}
		
		ous.writeInt32(OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER, version);
		ous.flush();
		fout.close();
		
		
		return list;
	}


	private static void copyBinaryPart(CodedOutputStream ous, byte[] BUFFER, RandomAccessFile raf, long fp, int length)
			throws IOException {
		raf.seek(fp);
		int toRead = length;
		while (toRead > 0) {
			int read = raf.read(BUFFER);
			if (read == -1) {
				throw new IllegalArgumentException("Unexpected end of file");
			}
			if (toRead < read) {
				read = toRead;
			}
			ous.writeRawBytes(BUFFER, 0, read);
			toRead -= read;
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
	
	protected static String formatLatBounds(double l, double r, double t, double b){
		MessageFormat format = new MessageFormat("(left top - right bottom) : {0}, {1} NE - {2}, {3} NE", Locale.US);
		return format.format(new Object[]{l, t, r, b}); 
	}
	
	public static void printFileInformation(String fileName,VerboseInfo verbose) throws IOException {
		File file = new File(fileName);
		if(!file.exists()){
			println("Binary OsmAnd index " + fileName + " was not found.");
			return;
		}
		printFileInformation(file,verbose);
	}
	
	

	public static void printFileInformation(File file, VerboseInfo verbose) throws IOException {
		RandomAccessFile r = new RandomAccessFile(file.getAbsolutePath(), "r");
		try {
			BinaryMapIndexReader index = new BinaryMapIndexReader(r);
			int i = 1;
			println("Binary index " + file.getName() + " version = " + index.getVersion());
			for(BinaryIndexPart p : index.getIndexes()){
				String partname = "";
				if(p instanceof MapIndex ){
					partname = "Map";
				} else if(p instanceof TransportIndex){
					partname = "Transport";
				} else if(p instanceof RouteRegion){
					partname = "Route";
				} else if(p instanceof PoiRegion){
					partname = "Poi";
				} else if(p instanceof AddressRegion){
					partname = "Address";
				}
				String name = p.getName() == null ? "" : p.getName(); 
				println(MessageFormat.format("{0}. {1} data {3} - {2} bytes", i, partname, p.getLength(), name));
				if(p instanceof TransportIndex){
					TransportIndex ti = ((TransportIndex) p);
					int sh = (31 - BinaryMapIndexReader.TRANSPORT_STOP_ZOOM);
					println("\t Bounds " + formatBounds(ti.getLeft() << sh, ti.getRight() << sh, 
							ti.getTop() << sh, ti.getBottom() << sh));
				} else if(p instanceof RouteRegion){
					RouteRegion ri = ((RouteRegion) p);
					println("\t Bounds " + formatLatBounds(ri.getLeftLongitude(), ri.getRightLongitude(), 
							ri.getTopLatitude(), ri.getBottomLatitude()));
				} else if(p instanceof MapIndex){
					MapIndex m = ((MapIndex) p);
					int j = 1;
					for(MapRoot mi : m.getRoots()){
						println(MessageFormat.format("\t{4}.{5} Map level minZoom = {0}, maxZoom = {1}, size = {2} bytes \n\t\tBounds {3}",
								mi.getMinZoom(), mi.getMaxZoom(), mi.getLength(), 
								formatBounds(mi.getLeft(), mi.getRight(), mi.getTop(), mi.getBottom()), 
								i, j++));
					}
					if((verbose != null && verbose.isVmap())){
						printMapDetailInfo(verbose, index);
					}
				} else if(p instanceof PoiRegion && (verbose != null && verbose.isVpoi())){
					printPOIDetailInfo(verbose, index, (PoiRegion) p);
				} else if (p instanceof AddressRegion && (verbose != null && verbose.isVaddress())) {
					printAddressDetailedInfo(verbose, index);
				}
				i++;
			}
			
			
		} catch (IOException e) {
			System.err.println("File is not valid index : " + file.getAbsolutePath());
			throw e;
		}
		
	}

	private static void printAddressDetailedInfo(VerboseInfo verbose, BinaryMapIndexReader index) throws IOException {
		for(String region : index.getRegionNames()){
			println("\tRegion:" + region);
			int[] cityType = new int[] {BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE,
					BinaryMapAddressReaderAdapter.POSTCODES_TYPE, 
					BinaryMapAddressReaderAdapter.VILLAGES_TYPE};
			for (int j = 0; j < cityType.length; j++) {
				int type = cityType[j];
				for (City c : index.getCities(region, null, type)) {
					println("\t\t" + c + " " + c.getId() + " (" + c.getStreets().size() + ")");
					index.preloadStreets(c, null);
					for (Street t : c.getStreets()) {
						if (verbose.contains(t)) {
							index.preloadBuildings(t, null);
							print("\t\t\t" + t.getName() + getId(t) + " (" + t.getBuildings().size() + ")");
							// if (type == BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE) {
							List<Building> buildings = t.getBuildings();
							if (buildings != null && !buildings.isEmpty()) {
								print("\t\t (");
								for (Building b : buildings) {
									print(b.toString() + ",");
								}
								print(")");
							}
							List<Street> streets = t.getIntersectedStreets();
							if (streets != null && !streets.isEmpty()) {
								print("\n\t\t\t\t\t\t\t x (");
								for (Street s : streets) {
									print(s.getName() + ", ");
								}
								print(")");
							}
							// }
							println("");
						}

					}
				}
			}
		}
	}

	private static void printMapDetailInfo(VerboseInfo verbose, BinaryMapIndexReader index) throws IOException {
		final StringBuilder b = new StringBuilder();
		SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(MapUtils.get31TileNumberX(verbose.lonleft),
				MapUtils.get31TileNumberX(verbose.lonright),
				MapUtils.get31TileNumberY(verbose.lattop),
				MapUtils.get31TileNumberY(verbose.latbottom), verbose.getZoom(),
				new SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, MapIndex index) {
						return true;
					}
				},
				new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject obj) {
						b.setLength(0);
						boolean multipolygon = obj.getPolygonInnerCoordinates() != null && obj.getPolygonInnerCoordinates().length > 0;
						if(multipolygon ) {
							b.append("Multipolygon");
						} else {
							b.append(obj.area? "Area" : (obj.getPointsLength() > 1? "Way" : "Point"));
						}
						int[] types = obj.getTypes();
						b.append(" types [");
						for(int j = 0; j<types.length; j++){
							if(j > 0) {
								b.append(", ");
							}
							TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
							if(pair == null) {
								System.err.println("Type " + types[j] + "was not found");
								continue;
//								throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
							}
							b.append(pair.toSimpleString()+" ("+types[j]+")");
						}
						b.append("]");
						if(obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0){
							b.append(" add_types [");
							for(int j = 0; j<obj.getAdditionalTypes().length; j++){
								if(j > 0) {
									b.append(", ");
								}
								TagValuePair pair = obj.getMapIndex().decodeType(obj.getAdditionalTypes()[j]);
								if(pair == null) {
									System.err.println("Type " + obj.getAdditionalTypes()[j] + "was not found");
									continue;
//									throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
								}
								b.append(pair.toSimpleString()+"("+obj.getAdditionalTypes()[j]+")");
								
							}
							b.append("]");
						}
						TIntObjectHashMap<String> names = obj.getObjectNames();
						if(names != null && !names.isEmpty()) {
							b.append(" Names [");
							int[] keys = names.keys();
							for(int j = 0; j<keys.length; j++){
								if(j > 0) {
									b.append(", ");
								}
								TagValuePair pair = obj.getMapIndex().decodeType(keys[j]);
								if(pair == null) {
									throw new NullPointerException("Type " + keys[j] + "was not found");
								}
								b.append(pair.toSimpleString()+"("+keys[j]+")");
								b.append(" - ").append(names.get(keys[j]));
							}
							b.append("]");
						}
						
						b.append(" id ").append((obj.getId() >> 1));
						b.append(" lat/lon : ");
						for(int i=0; i<obj.getPointsLength(); i++) {
							float x = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
							float y = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
							b.append(x).append(" / ").append(y).append(" , ");
						}
						println(b.toString());
						return false;
					}
					@Override
					public boolean isCancelled() {
						return false;
					}
				});
		index.searchMapIndex(req);
	}
	
	private static void printPOIDetailInfo(VerboseInfo verbose, BinaryMapIndexReader index, PoiRegion p) throws IOException {
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(MapUtils.get31TileNumberX(verbose.lonleft),
				MapUtils.get31TileNumberX(verbose.lonright),
				MapUtils.get31TileNumberY(verbose.lattop),
				MapUtils.get31TileNumberY(verbose.latbottom), verbose.getZoom(),
				new SearchPoiTypeFilter() {
					@Override
					public boolean accept(AmenityType type, String subcategory) {
						return true;
					}
				},
				new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						println(object.toString() + " " + object.getLocation());
						return false;
					}
					@Override
					public boolean isCancelled() {
						return false;
					}
				});
		index.searchPoi(req);
		println("Categories : ");
		for(int i =0; i< p.categories.size(); i++) {
			println(p.categories.get(i) + " - " + p.subcategories.get(i));	
		}
		
		
	}
	
	private static String getId(MapObject o ){
		if(o.getId() == null) {
			return " no id " ;
		}
		return " " + (o.getId() >> 1);
	}

	public static void printUsage(String warning) {
		if(warning != null){
			println(warning);
		}
		println("Inspector is console utility for working with binary indexes of OsmAnd.");
		println("It allows print info about file, extract parts and merge indexes.");
		println("\nUsage for print info : inspector [-vaddress] [-vmap] [-vpoi] [-vtransport] [-zoom=Zoom] [-bbox=LeftLon,TopLat,RightLon,BottomLan] [file]");
		println("  Prints information about [file] binary index of OsmAnd.");
		println("  -v.. more verbouse output (like all cities and their streets or all map objects with tags/values and coordinates)");
		println("\nUsage for combining indexes : inspector -c file_to_create (file_from_extract ((+|-)parts_to_extract)? )*");
		println("\tCreate new file of extracted parts from input file. [parts_to_extract] could be parts to include or exclude.");
		println("  Example : inspector -c output_file input_file +1,2,3\n\tExtracts 1, 2, 3 parts (could be find in print info)");
		println("  Example : inspector -c output_file input_file -2,3\n\tExtracts all  parts excluding 2, 3");
		println("  Example : inspector -c output_file input_file1 input_file2 input_file3\n\tSimply combine 3 files");
		println("  Example : inspector -c output_file input_file1 input_file2 -4\n\tCombine all parts of 1st file and all parts excluding 4th part of 2nd file");

		
	}

}
