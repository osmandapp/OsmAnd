package net.osmand.binary;


import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CitiesBlock;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapObjectStat;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.util.MapUtils;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

public class BinaryInspector {


	public static final int BUFFER_SIZE = 1 << 20;
	public static final int SHIFT_ID = 6;
	private VerboseInfo vInfo;
	public static void main(String[] args) throws IOException {
		BinaryInspector in = new BinaryInspector();
		if (args == null || args.length == 0) {
			BinaryInspector.printUsage(null);
			return;
		}
		// test cases show info
		if ("test".equals(args[0])) {
			in.inspector(new String[] {

//					"-vpoi",
					"-vmap", "-vmapobjects", // "-vmapcoordinates",
//					"-vrouting",
//					"-vtransport",
//					"-vaddress", "-vcities","-vstreetgroups",
//					"-vstreets", "-vbuildings", "-vintersections",
//					"-lang=ru",
//					"-bbox=30.4981,50.4424,30.5195,50.4351",
//					"-osm="+System.getProperty("maps.dir")+"/map_full.obf.osm",
					System.getProperty("maps.dir")+"/Map.obf"
//					System.getProperty("maps.dir")+"../temp/kiev/Ukraine_kiev-city_europe_17_06_05.obf",
//					System.getProperty("maps.dir")+"Ukraine_kiev-city_europe_2.obf",
			});
		} else {
			in.inspector(args);
		}
	}

	private void printToFile(String s) throws IOException {
		if (vInfo.osmOut != null) {
			vInfo.osmOut.write(s.getBytes());
		} else {
			System.out.println(s);
		}
	}

	private void println(String s) {
		if (vInfo != null && vInfo.osm && vInfo.osmOut == null) {
			// ignore
		} else {
			System.out.println(s);
		}

	}

	private void print(String s) {
		if (vInfo != null && vInfo.osm && vInfo.osmOut == null) {
			// ignore
		} else {
			System.out.print(s);
		}
	}

	protected static class VerboseInfo {
		boolean vaddress;
		boolean vcities;
		boolean vcitynames;
		boolean vstreetgroups;
		boolean vstreets;
		boolean vbuildings;
		boolean vintersections;
		boolean vtransport;
		boolean vpoi;
		boolean vmap;
		boolean vrouting;
		boolean vmapObjects;
		boolean vmapCoordinates;
		boolean vstats;
		boolean osm;
		FileOutputStream osmOut = null;
		double lattop = 85;
		double latbottom = -85;
		double lonleft = -179.9;
		double lonright = 179.9;
		String lang = null;
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

		public boolean isVrouting() {
			return vrouting;
		}

		public boolean isVpoi() {
			return vpoi;
		}

		public boolean isVtransport() {
			return vtransport;
		}

		public boolean isVStats() {
			return vstats;
		}

		public VerboseInfo(String[] params) throws FileNotFoundException {
			for (int i = 0; i < params.length; i++) {
				if (params[i].equals("-vaddress")) {
					vaddress = true;
				} else if (params[i].equals("-vstreets")) {
					vstreets = true;
				} else if (params[i].equals("-vstreetgroups")) {
					vstreetgroups = true;
				} else if (params[i].equals("-vcities")) {
					vcities = true;
				} else if (params[i].equals("-vcitynames")) {
					vcitynames = true;
				} else if (params[i].equals("-vbuildings")) {
					vbuildings = true;
				} else if (params[i].equals("-vintersections")) {
					vintersections = true;
				} else if (params[i].equals("-vmap")) {
					vmap = true;
				} else if (params[i].equals("-vstats")) {
					vstats = true;
				} else if (params[i].equals("-vrouting")) {
					vrouting = true;
				} else if (params[i].equals("-vmapobjects")) {
					vmapObjects = true;
				} else if (params[i].equals("-vmapcoordinates")) {
					vmapCoordinates = true;
				} else if (params[i].equals("-vpoi")) {
					vpoi = true;
				} else if (params[i].startsWith("-osm")) {
					osm = true;
					if (params[i].startsWith("-osm=")) {
						osmOut = new FileOutputStream(params[i].substring(5));
					}
				} else if (params[i].equals("-vtransport")) {
					vtransport = true;
				} else if (params[i].startsWith("-lang=")) {
					lang = params[i].substring("-lang=".length());
				} else if (params[i].startsWith("-zoom=")) {
					zoom = Integer.parseInt(params[i].substring("-zoom=".length()));
				} else if (params[i].startsWith("-bbox=")) {
					String[] values = params[i].substring("-bbox=".length()).split(",");
					lonleft = Double.parseDouble(values[0]);
					lattop = Double.parseDouble(values[1]);
					lonright = Double.parseDouble(values[2]);
					latbottom = Double.parseDouble(values[3]);
				}
			}
		}

		public boolean contains(MapObject o) {
			return lattop >= o.getLocation().getLatitude() && latbottom <= o.getLocation().getLatitude()
					&& lonleft <= o.getLocation().getLongitude() && lonright >= o.getLocation().getLongitude();

		}

		public void close() throws IOException {
			if (osmOut != null) {
				osmOut.close();
				osmOut = null;
			}

		}
	}

	public void inspector(String[] args) throws IOException {
		String f = args[0];
		if (f.charAt(0) == '-') {
			// command
			if (f.equals("-c") || f.equals("-combine")) {
				if (args.length < 4) {
					printUsage("Too few parameters to extract (require minimum 4)");
				} else {
					Map<File, String> parts = new HashMap<File, String>();
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
			} else if (f.startsWith("-v") || f.startsWith("-osm")) {
				if (args.length < 2) {
					printUsage("Missing file parameter");
				} else {
					vInfo = new VerboseInfo(args);
					printFileInformation(args[args.length - 1]);
					vInfo.close();
				}
			} else {
				printUsage("Unknown command : " + f);
			}
		} else {
			vInfo = null;
			printFileInformation(f);
		}
	}

	public static final void writeInt(CodedOutputStream ous, int v) throws IOException {
		ous.writeRawByte((v >>> 24) & 0xFF);
		ous.writeRawByte((v >>> 16) & 0xFF);
		ous.writeRawByte((v >>>  8) & 0xFF);
		ous.writeRawByte(v & 0xFF);
		//written += 4;
	}

	@SuppressWarnings("unchecked")
	public  static List<Float> combineParts(File fileToExtract, Map<File, String> partsToExtractFrom) throws IOException {
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
			rafs[c] = new RandomAccessFile(f.getAbsolutePath(), "r");
			indexes[c] = new BinaryMapIndexReader(rafs[c], f);
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
						partsSet[c].add((i + 1f) + (j + 1) / 10f);
					}
				}
			}
			if(pattern != null){
				minus = pattern.startsWith("-");
				String[] split = pattern.substring(1).split(",");
				for(String s : split){
					temp.add(Float.valueOf(s));
				}
			}

			Iterator<Float> p = partsSet[c].iterator();
			while (p.hasNext()) {
				Float part = p.next();
				if (minus) {
					if (temp.contains(part)) {
						p.remove();
					}
				} else {
					if (!temp.contains(part)) {
						p.remove();
					}
				}
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
				if (!partSet.contains(Float.valueOf(i + 1f))) {
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
				System.out.println(MessageFormat.format("{2} part {0} is extracted {1} bytes",
						new Object[]{part.getName(), part.getLength(), map}));

			}
		}

		ous.writeInt32(OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER, version);
		ous.flush();
		fout.close();


		return list;
	}


	public static void copyBinaryPart(CodedOutputStream ous, byte[] BUFFER, RandomAccessFile raf, long fp, int length)
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


	protected String formatBounds(int left, int right, int top, int bottom) {
		double l = MapUtils.get31LongitudeX(left);
		double r = MapUtils.get31LongitudeX(right);
		double t = MapUtils.get31LatitudeY(top);
		double b = MapUtils.get31LatitudeY(bottom);
		return formatLatBounds(l, r, t, b);
	}

	protected String formatLatBounds(double l, double r, double t, double b) {
		MessageFormat format = new MessageFormat("(left top - right bottom) : {0,number,#.####}, {1,number,#.####} NE - {2,number,#.####}, {3,number,#.####} NE", new Locale("EN", "US"));
		return format.format(new Object[]{l, t, r, b});
	}

	public void printFileInformation(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			println("Binary OsmAnd index " + fileName + " was not found.");
			return;
		}
		printFileInformation(file);
	}

	public void printFileInformation(File file) throws IOException {
		RandomAccessFile r = new RandomAccessFile(file.getAbsolutePath(), "r");
		printFileInformation(r, file);
	}

	public void printFileInformation(RandomAccessFile r, File file) throws IOException {
		String filename = file.getName();
		try {
			BinaryMapIndexReader index = new BinaryMapIndexReader(r, file);
			int i = 1;
			println("Binary index " + filename + " version = " + index.getVersion() + " edition = " + new Date(index.getDateCreated()));
			for (BinaryIndexPart p : index.getIndexes()) {
				String partname = "";
				if (p instanceof MapIndex) {
					partname = "Map";
				} else if (p instanceof TransportIndex) {
					partname = "Transport";
				} else if (p instanceof RouteRegion) {
					partname = "Routing";
				} else if (p instanceof PoiRegion) {
					partname = "Poi";
				} else if (p instanceof AddressRegion) {
					partname = "Address";
				}
				String name = p.getName() == null ? "" : p.getName();
				println(MessageFormat.format("{0} {1} data {3} - {2,number,#} bytes",
						new Object[]{i, partname, p.getLength(), name}));
				if(p instanceof TransportIndex){
					TransportIndex ti = ((TransportIndex) p);
					int sh = (31 - BinaryMapIndexReader.TRANSPORT_STOP_ZOOM);
					println("\tBounds " + formatBounds(ti.getLeft() << sh, ti.getRight() << sh,
							ti.getTop() << sh, ti.getBottom() << sh));
					if ((vInfo != null && vInfo.isVtransport())) {
						printTransportDetailInfo(vInfo, index, (TransportIndex) p);
					}
				} else if (p instanceof RouteRegion) {
					RouteRegion ri = ((RouteRegion) p);
					println("\tBounds " + formatLatBounds(ri.getLeftLongitude(), ri.getRightLongitude(),
							ri.getTopLatitude(), ri.getBottomLatitude()));
					if ((vInfo != null && vInfo.isVrouting())) {
						printRouteDetailInfo(index, (RouteRegion) p);
					}
				} else if (p instanceof MapIndex) {
					MapIndex m = ((MapIndex) p);
					int j = 1;
					for (MapRoot mi : m.getRoots()) {
						println(MessageFormat.format("\t{4}.{5} Map level minZoom = {0}, maxZoom = {1}, size = {2,number,#} bytes \n\t\tBounds {3}",
								new Object[] {
								mi.getMinZoom(), mi.getMaxZoom(), mi.getLength(), 
								formatBounds(mi.getLeft(), mi.getRight(), mi.getTop(), mi.getBottom()), 
								i, j++}));
					}
					if ((vInfo != null && vInfo.isVmap())) {
						printMapDetailInfo(index, m);
					}
				} else if (p instanceof PoiRegion && (vInfo != null && vInfo.isVpoi())) {
					printPOIDetailInfo(vInfo, index, (PoiRegion) p);
				} else if (p instanceof TransportIndex && (vInfo != null && vInfo.isVtransport())) {

				} else if (p instanceof AddressRegion) {
					List<CitiesBlock> cities = ((AddressRegion) p).cities;
					for (CitiesBlock c : cities) {
						println("\t" + i + "." + c.type + " Address part size=" + c.length + " bytes");
					}
					if (vInfo != null && vInfo.isVaddress()) {
						printAddressDetailedInfo(vInfo, index, (AddressRegion) p);
					}
				}
				i++;
			}


		} catch (IOException e) {
			System.err.println("File doesn't have valid structure : " + filename + " " + e.getMessage());
			throw e;
		}

	}

	private void printRouteDetailInfo(BinaryMapIndexReader index, RouteRegion p) throws IOException {
		final DamnCounter mapObjectsCounter = new DamnCounter();
		final StringBuilder b = new StringBuilder();
		List<RouteSubregion> regions = index.searchRouteIndexTree(
				BinaryMapIndexReader.buildSearchRequest(MapUtils.get31TileNumberX(vInfo.lonleft),
						MapUtils.get31TileNumberX(vInfo.lonright), MapUtils.get31TileNumberY(vInfo.lattop),
						MapUtils.get31TileNumberY(vInfo.latbottom), vInfo.getZoom(), null),
				p.getSubregions());
		if (vInfo.osm) {
			printToFile("<?xml version='1.0' encoding='UTF-8'?>\n" +
					"<osm version='0.6'>\n");
		}
		index.loadRouteIndexData(regions, new ResultMatcher<RouteDataObject>() {
			@Override
			public boolean publish(RouteDataObject obj) {
				mapObjectsCounter.value++;
				if (vInfo.osm) {
					b.setLength(0);
					printOsmRouteDetails(obj, b);
					try {
						printToFile(b.toString());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				b.setLength(0);
				b.append("Road ");
				b.append(obj.id);
				b.append(" osmid ").append(obj.getId() >> (SHIFT_ID));

				for (int i = 0; i < obj.getTypes().length; i++) {
					RouteTypeRule rr = obj.region.quickGetEncodingRule(obj.getTypes()[i]);
					b.append(" ").append(rr.getTag()).append("='").append(rr.getValue()).append("'");
				}
				int[] nameIds = obj.getNameIds();
				if (nameIds != null) {
					for (int key : nameIds) {
						RouteTypeRule rr = obj.region.quickGetEncodingRule(key);
						b.append(" ").append(rr.getTag()).append("='").append(obj.getNames().get(key)).append("'");
					}
				}
				int pointsLength = obj.getPointsLength();
				if(obj.hasPointNames() || obj.hasPointTypes()) {
					b.append(" pointtypes [");
					for (int i = 0; i < pointsLength; i++) {
						String[] names = obj.getPointNames(i);
						int[] nametypes = obj.getPointNameTypes(i);
						int[] types = obj.getPointTypes(i);
						if (types != null || names != null) {
							b.append("[" + (i + 1) + ". ");
							if (names != null) {
								for (int k = 0; k < names.length; k++) {
									RouteTypeRule rr = obj.region.quickGetEncodingRule(nametypes[k]);
									b.append(rr.getTag()).append("='").append(names[k]).append("' ");
								}
							}
							if (types != null) {
								for (int k = 0; k < types.length; k++) {
									RouteTypeRule rr = obj.region.quickGetEncodingRule(types[k]);
									b.append(rr.getTag()).append("='").append(rr.getValue()).append("' ");
								}
							}
							if (vInfo.vmapCoordinates) {
								float x = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
								float y = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
								b.append(y).append(" / ").append(x).append(" ");
							}
							b.append("]");
						}
					}
					b.append("]");
				}
				if (obj.restrictions != null) {
					b.append(" restrictions [");
					for (int i = 0; i < obj.restrictions.length; i++) {
						if (i > 0) {
							b.append(", ");
						}
						b.append(obj.getRestrictionId(i)).append(" (").append(
								MapRenderingTypes.getRestrictionValue(
								obj.getRestrictionType(i))).append(") ");

					}
					b.append("] ");
				}
				if (vInfo.vmapCoordinates) {
					b.append(" lat/lon : ");
					for (int i = 0; i < obj.getPointsLength(); i++) {
						float x = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
						float y = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
						b.append(y).append(" / ").append(x).append(" , ");
					}
				}
				println(b.toString());
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		});
		println("\tTotal map objects: " + mapObjectsCounter.value);
		if (vInfo.osm) {
			printToFile("</osm >\n");
		}
	}

	private void printAddressDetailedInfo(VerboseInfo verbose, BinaryMapIndexReader index, AddressRegion region) throws IOException {
		String[] cityType_String = new String[]{
				"Cities/Towns section",
				"Postcodes section",
				"Villages section",
		};
		for (int j = 0; j < BinaryMapAddressReaderAdapter.CITY_TYPES.length; j++) {
			int type = BinaryMapAddressReaderAdapter.CITY_TYPES[j];
			final List<City> cities = index.getCities(region, null, type);

			print(MessageFormat.format("\t{0}, {1,number,#} group(s)", cityType_String[j], cities.size()));
			if (BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE == type) {
				if (!verbose.vstreetgroups && !verbose.vcities) {
					println("");
					continue;
				}
			} else if (!verbose.vstreetgroups) {
				println("");
				continue;
			}
			println(":");

			for (City c : cities) {
				int size = index.preloadStreets(c, null);
				List<Street> streets = new ArrayList<Street>(c.getStreets());
				String name = c.getName(verbose.lang);
				if (verbose.vcitynames) {
					boolean includeEnName = verbose.lang == null || !verbose.lang.equals("en");
					name += " " + c.getNamesMap(includeEnName).toString();
				}
				String cityDescription = (type == BinaryMapAddressReaderAdapter.POSTCODES_TYPE ?
						MessageFormat.format("\t\t''{0}'' {1,number,#} street(s) size {2,number,#} bytes", name, streets.size(), size) :
						MessageFormat.format("\t\t''{0}'' [{1,number,#}], {2,number,#} street(s) size {3,number,#} bytes", name, c.getId(), streets.size(), size));
				print(cityDescription);
				if (!verbose.vstreets) {
					println("");
		            continue;
		        }
				println(":");
				if (!verbose.contains(c))
					continue;

				for (Street t : streets) {
					if (!verbose.contains(t))
						continue;
					index.preloadBuildings(t, null);
					final List<Building> buildings = t.getBuildings();
					final List<Street> intersections = t.getIntersectedStreets();

					println(MessageFormat.format("\t\t\t''{0}'' [{1,number,#}], {2,number,#} building(s), {3,number,#} intersections(s)",
							new Object[]{t.getName(verbose.lang), t.getId(), buildings.size(), intersections.size()}));
					if (buildings != null && !buildings.isEmpty() && verbose.vbuildings) {
						println("\t\t\t\tBuildings:");
						for (Building b : buildings) {
							println(MessageFormat.format("\t\t\t\t{0} [{1,number,#} {2} ]",
									new Object[]{b.getName(verbose.lang), b.getId(), b.getPostcode() == null ? "" : b.getPostcode()}));
						}
					}

					if (intersections != null && !intersections.isEmpty() && verbose.vintersections) {
						print("\t\t\t\tIntersects with:");
						for (Street s : intersections) {
							println("\t\t\t\t\t" + s.getName(verbose.lang));
						}
					}
				}
			}
		}
	}

	private static class DamnCounter {
		int value;
	}

	private static class MapStatKey {
		String key = "";
		long statCoordinates;
		long statCoordinatesCount;
		long statObjectSize;
		int count;
		int namesLength;
	}

	private class MapStats {
		public int lastStringNamesSize;
		public int lastObjectIdSize;
		public int lastObjectHeaderInfo;
		public int lastObjectAdditionalTypes;
		public int lastObjectTypes;
		public int lastObjectCoordinates;
		public int lastObjectCoordinatesCount;

		public int lastObjectSize;

		private Map<String, MapStatKey> types = new LinkedHashMap<String, BinaryInspector.MapStatKey>();
		private SearchRequest<BinaryMapDataObject> req;

		public void processKey(String simpleString, MapObjectStat st, TIntObjectHashMap<String> objectNames,
		                       int coordinates, boolean names) {
			TIntObjectIterator<String> it = objectNames.iterator();
			int nameLen = 0;
			while (it.hasNext()) {
				it.advance();
				nameLen++;
				nameLen += it.value().length();
			}
			if (!types.containsKey(simpleString)) {
				MapStatKey stt = new MapStatKey();
				stt.key = simpleString;
				types.put(simpleString, stt);
			}
			MapStatKey key = types.get(simpleString);
			if (names) {
				key.namesLength += nameLen;
			} else {
				key.statCoordinates += st.lastObjectCoordinates;
				key.statCoordinatesCount += coordinates;
				key.statObjectSize += st.lastObjectSize;
				key.count++;
			}
		}


		public void process(BinaryMapDataObject obj) {
			MapObjectStat st = req.stat;
			int cnt = 0;
			boolean names = st.lastObjectCoordinates == 0;
			if (!names) {
				this.lastStringNamesSize += st.lastStringNamesSize;
				this.lastObjectIdSize += st.lastObjectIdSize;
				this.lastObjectHeaderInfo += st.lastObjectHeaderInfo;
				this.lastObjectAdditionalTypes += st.lastObjectAdditionalTypes;
				this.lastObjectTypes += st.lastObjectTypes;
				this.lastObjectCoordinates += st.lastObjectCoordinates;
				cnt = obj.getPointsLength();
				this.lastObjectSize += st.lastObjectSize;
				if (obj.getPolygonInnerCoordinates() != null) {
					for (int[] i : obj.getPolygonInnerCoordinates()) {
						cnt += i.length;
					}
				}
				this.lastObjectCoordinatesCount += cnt;
			}
			for (int i = 0; i < obj.getTypes().length; i++) {
				int tp = obj.getTypes()[i];
				TagValuePair pair = obj.mapIndex.decodeType(tp);
				if (pair == null) {
					continue;
				}
				processKey(pair.toSimpleString(), st, obj.getObjectNames(), cnt, names);
			}
			st.clearObjectStats();
			st.lastObjectSize = 0;

		}

		public void print() {
			MapObjectStat st = req.stat;
			println("MAP BLOCK INFO:");
			long b = 0;
			b += out("Header", st.lastBlockHeaderInfo);
			b += out("String table", st.lastBlockStringTableSize);
			b += out("Map Objects", lastObjectSize);
			out("TOTAL", b);
			println("\nMAP OBJECTS INFO:");
			b = 0;
			b += out("Header", lastObjectHeaderInfo);
			b += out("Coordinates", lastObjectCoordinates);
			out("Coordinates Count(pair)", lastObjectCoordinatesCount);
			b += out("Types", lastObjectTypes);
			b += out("Additonal Types", lastObjectAdditionalTypes);
			b += out("Ids", lastObjectIdSize);
			b += out("String names", lastStringNamesSize);
			out("TOTAL", b);

			println("\n\nOBJECT BY TYPE STATS: ");
			ArrayList<MapStatKey> stats = new ArrayList<MapStatKey>(types.values());
			Collections.sort(stats, new Comparator<MapStatKey>() {

				@Override
				public int compare(MapStatKey o1, MapStatKey o2) {
					return compare(o1.statObjectSize, o2.statObjectSize);
				}

				public int compare(long x, long y) {
			        return (x < y) ? -1 : ((x == y) ? 0 : 1);
			    }
			});

			for (MapStatKey s : stats) {
				println(s.key + " (" + s.count + ") \t " + s.statObjectSize + " bytes \t coord=" +
						s.statCoordinatesCount +
						" (" + s.statCoordinates + " bytes) " +
						" names " + s.namesLength + " bytes");
			}

		}

		private long out(String s, long i) {
			while (s.length() < 25) {
				s += " ";
			}
			DecimalFormat df = new DecimalFormat("0,000,000,000");
			println(s + ": " + df.format(i));
			return i;
		}


		public void setReq(SearchRequest<BinaryMapDataObject> req) {
			this.req = req;
		}

	}

	private void printMapDetailInfo(BinaryMapIndexReader index, MapIndex mapIndex) throws IOException {
		final StringBuilder b = new StringBuilder();
		final DamnCounter mapObjectsCounter = new DamnCounter();
		final MapStats mapObjectStats = new MapStats();
		if (vInfo.osm) {
			printToFile("<?xml version='1.0' encoding='UTF-8'?>\n" +
					"<osm version='0.6'>\n");
		}
		if (vInfo.isVStats()) {
			BinaryMapIndexReader.READ_STATS = true;
		}
		final SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
				MapUtils.get31TileNumberX(vInfo.lonleft),
				MapUtils.get31TileNumberX(vInfo.lonright),
				MapUtils.get31TileNumberY(vInfo.lattop),
				MapUtils.get31TileNumberY(vInfo.latbottom),
				vInfo.getZoom(),
				new SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, MapIndex index) {
						return true;
					}
				},
				new ResultMatcher<BinaryMapDataObject>() {
					@Override
					public boolean publish(BinaryMapDataObject obj) {
						mapObjectsCounter.value++;
						if (vInfo.isVStats()) {
							mapObjectStats.process(obj);
						} else if (vInfo.vmapObjects) {
							b.setLength(0);
							if (vInfo.osm) {
								printOsmMapDetails(obj, b);
								try {
									printToFile(b.toString());
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
//							} else if(obj.getId() >> 1 == 205743436l) {
							} else {
								printMapDetails(obj, b, vInfo.vmapCoordinates);
								println(b.toString());
							}
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
		if (vInfo.vstats) {
			mapObjectStats.setReq(req);
		}
		index.searchMapIndex(req, mapIndex);
		if (vInfo.osm) {
			printToFile("</osm >\n");
		}
		if (vInfo.vstats) {
			mapObjectStats.print();
		}
		println("\tTotal map objects: " + mapObjectsCounter.value);
	}


	private static void printMapDetails(BinaryMapDataObject obj, StringBuilder b, boolean vmapCoordinates) {
		boolean multipolygon = obj.getPolygonInnerCoordinates() != null && obj.getPolygonInnerCoordinates().length > 0;
		if (multipolygon) {
			b.append("Multipolygon");
		} else {
			b.append(obj.area ? "Area" : (obj.getPointsLength() > 1 ? "Way" : "Point"));
		}
		int[] types = obj.getTypes();
		b.append(" types [");
		for (int j = 0; j < types.length; j++) {
			if (j > 0) {
				b.append(", ");
			}
			TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
			if (pair == null) {
				System.err.println("Type " + types[j] + "was not found");
				continue;
//								throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
			}
			b.append(pair.toSimpleString() + " (" + types[j] + ")");
		}
		b.append("]");
		if (obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0) {
			b.append(" add_types [");
			for (int j = 0; j < obj.getAdditionalTypes().length; j++) {
				if (j > 0) {
					b.append(", ");
				}
				TagValuePair pair = obj.getMapIndex().decodeType(obj.getAdditionalTypes()[j]);
				if (pair == null) {
					System.err.println("Type " + obj.getAdditionalTypes()[j] + "was not found");
					continue;
//									throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
				}
				b.append(pair.toSimpleString() + "(" + obj.getAdditionalTypes()[j] + ")");

			}
			b.append("]");
		}
		TIntObjectHashMap<String> names = obj.getObjectNames();
		TIntArrayList order = obj.getNamesOrder();
		if (names != null && !names.isEmpty()) {
			b.append(" Names [");
			// int[] keys = names.keys();
			for (int j = 0; j < order.size(); j++) {
				if (j > 0) {
					b.append(", ");
				}
				TagValuePair pair = obj.getMapIndex().decodeType(order.get(j));
				if (pair == null) {
					throw new NullPointerException("Type " + order.get(j) + "was not found");
				}
				b.append(pair.toSimpleString() + "(" + order.get(j) + ")");
				b.append(" - ").append(names.get(order.get(j)));
			}
			b.append("]");
		}

		b.append(" id ").append(obj.getId());
		b.append(" osmid ").append((obj.getId() >> (SHIFT_ID + 1)));
		if (vmapCoordinates) {
			b.append(" lat/lon : ");
			for (int i = 0; i < obj.getPointsLength(); i++) {
				float x = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
				float y = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
				b.append(y).append(" / ").append(x).append(" , ");
			}
		}
	}


	private static int OSM_ID = 1;

	private void printOsmRouteDetails(RouteDataObject obj, StringBuilder b) {
		StringBuilder tags = new StringBuilder();
		int[] types = obj.getTypes();
		for (int j = 0; j < types.length; j++) {
			RouteTypeRule rt = obj.region.quickGetEncodingRule(types[j]);
			if (rt == null) {
				throw new NullPointerException("Type " + types[j] + "was not found");
			}
			tags.append("\t<tag k='").append(rt.getTag()).append("' v='").append(rt.getValue()).append("' />\n");
		}
		TIntObjectHashMap<String> names = obj.getNames();
		if (names != null && !names.isEmpty()) {
			int[] keys = names.keys();
			for (int j = 0; j < keys.length; j++) {
				RouteTypeRule rt = obj.region.quickGetEncodingRule(keys[j]);
				if (rt == null) {
					throw new NullPointerException("Type " + keys[j] + "was not found");
				}
				String name = quoteName(names.get(keys[j]));
				tags.append("\t<tag k='").append(rt.getTag()).append("' v='").append(name).append("' />\n");
			}
		}

		tags.append("\t<tag k=\'").append("original_id").append("' v='").append(obj.getId() >> (SHIFT_ID))
				.append("'/>\n");
		tags.append("\t<tag k=\'").append("osmand_id").append("' v='").append(obj.getId()).append("'/>\n");

		TLongArrayList ids = new TLongArrayList();
		for (int i = 0; i < obj.getPointsLength(); i++) {
			float lon = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
			float lat = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
			int id = OSM_ID++;
			b.append("\t<node id = '" + id + "' version='1' lat='" + lat + "' lon='" + lon + "' >\n");
			if (obj.getPointNames(i) != null) {
				String[] vs = obj.getPointNames(i);
				int[] keys = obj.getPointNameTypes(i);
				for (int j = 0; j < keys.length; j++) {
					RouteTypeRule rt = obj.region.quickGetEncodingRule(keys[j]);
					String name = quoteName(vs[j]);
					tags.append("\t\t<tag k='").append(rt.getTag()).append("' v='").append(name).append("' />\n");
				}
			}
			if (obj.getPointTypes(i) != null) {
				int[] keys = obj.getPointTypes(i);
				for (int j = 0; j < keys.length; j++) {
					RouteTypeRule rt = obj.region.quickGetEncodingRule(keys[j]);
					tags.append("\t\t<tag k='").append(rt.getTag()).append("' v='").append(rt.getValue()).append("' />\n");
				}
			}
			b.append("\t</node >\n");
			ids.add(id);
		}
		long idway = printWay(ids, b, tags);
		if(obj.getRestrictionLength() > 0) {
			for(int i = 0; i < obj.getRestrictionLength(); i ++) {
				long ld = obj.getRestrictionId(i);
				String tp = MapRenderingTypes.getRestrictionValue(obj.getRestrictionType(i));
				int id = OSM_ID++;
				b.append("<relation id = '" + id + "' version='1'>\n");
				b.append("\t<member ref='").append(idway).append("' role='from' type='way' />\n");
				b.append("\t<tag k='").append("from_osmand_id").append("' v='").append(obj.getId()).append("' />\n");
				b.append("\t<tag k='").append("from_id").append("' v='").append(obj.getId() >> SHIFT_ID).append("' />\n");
				b.append("\t<tag k='").append("to_osmand_id").append("' v='").append(ld).append("' />\n");
				b.append("\t<tag k='").append("to_id").append("' v='").append(ld >> SHIFT_ID).append("' />\n");
				b.append("\t<tag k='").append("type").append("' v='").append("restriction").append("' />\n");
				b.append("\t<tag k='").append("restriction").append("' v='").append(tp).append("' />\n");
				b.append("</relation>\n");
			}
		}
	}

	private String quoteName(String name) {
		name = name.replace("'", "&apos;");
		name = name.replace("&", "&amp;");
		return name;
	}

	private void printOsmMapDetails(BinaryMapDataObject obj, StringBuilder b) {
		boolean multipolygon = obj.getPolygonInnerCoordinates() != null && obj.getPolygonInnerCoordinates().length > 0;
		boolean point = obj.getPointsLength() == 1;
		StringBuilder tags = new StringBuilder();
		int[] types = obj.getTypes();
		for (int j = 0; j < types.length; j++) {
			TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
			if (pair == null) {
				throw new NullPointerException("Type " + types[j] + "was not found");
			}
			tags.append("\t<tag k='").append(pair.tag).append("' v='").append(pair.value).append("' />\n");
		}
		if (obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0) {
			for (int j = 0; j < obj.getAdditionalTypes().length; j++) {
				int addtype = obj.getAdditionalTypes()[j];
				TagValuePair pair = obj.getMapIndex().decodeType(addtype);
				if (pair == null) {
					throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
				}
				tags.append("\t<tag k='").append(pair.tag).append("' v='").append(pair.value).append("' />\n");
			}
		}
		TIntObjectHashMap<String> names = obj.getObjectNames();
		if (names != null && !names.isEmpty()) {
			int[] keys = names.keys();
			for (int j = 0; j < keys.length; j++) {
				TagValuePair pair = obj.getMapIndex().decodeType(keys[j]);
				if (pair == null) {
					throw new NullPointerException("Type " + keys[j] + "was not found");
				}
				String name = names.get(keys[j]);
				name = quoteName(name);
				tags.append("\t<tag k='").append(pair.tag).append("' v='").append(name).append("' />\n");
			}
		}

		tags.append("\t<tag k=\'").append("original_id").append("' v='").append(obj.getId() >> (SHIFT_ID + 1)).append("'/>\n");
		tags.append("\t<tag k=\'").append("osmand_id").append("' v='").append(obj.getId()).append("'/>\n");

		if(point) {
			float lon= (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(0));
			float lat = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(0));
			b.append("<node id = '" + OSM_ID++ + "' version='1' lat='" + lat + "' lon='" + lon + "' >\n");
			b.append(tags);
			b.append("</node>\n");
		} else {
			TLongArrayList innerIds = new TLongArrayList();
			TLongArrayList ids = new TLongArrayList();
			for (int i = 0; i < obj.getPointsLength(); i++) {
				float lon = (float) MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
				float lat = (float) MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
				int id = OSM_ID++;
				b.append("\t<node id = '" + id + "' version='1' lat='" + lat + "' lon='" + lon + "' />\n");
				ids.add(id);
			}
			long outerId = printWay(ids, b, multipolygon ? null : tags);
			if (multipolygon) {
				int[][] polygonInnerCoordinates = obj.getPolygonInnerCoordinates();
				for (int j = 0; j < polygonInnerCoordinates.length; j++) {
					ids.clear();
					for (int i = 0; i < polygonInnerCoordinates[j].length; i += 2) {
						float lon = (float) MapUtils.get31LongitudeX(polygonInnerCoordinates[j][i]);
						float lat = (float) MapUtils.get31LatitudeY(polygonInnerCoordinates[j][i + 1]);
						int id = OSM_ID++;
						b.append("<node id = '" + id + "' version='1' lat='" + lat + "' lon='" + lon + "' />\n");
						ids.add(id);
					}
					innerIds.add(printWay(ids, b, null));
				}
				int id = OSM_ID++;
				b.append("<relation id = '" + id + "' version='1'>\n");
				b.append(tags);
				b.append("\t<member type='way' role='outer' ref= '" + outerId + "'/>\n");
				TLongIterator it = innerIds.iterator();
				while (it.hasNext()) {
					long ref = it.next();
					b.append("<member type='way' role='inner' ref= '" + ref + "'/>\n");
				}
				b.append("</relation>\n");
			}
		}
	}


	private long printWay(TLongArrayList ids, StringBuilder b, StringBuilder tags) {
		int id = OSM_ID++;
		b.append("<way id = '" + id + "' version='1'>\n");
		if (tags != null) {
			b.append(tags);
		}
		TLongIterator it = ids.iterator();
		while (it.hasNext()) {
			long ref = it.next();
			b.append("\t<nd ref = '" + ref + "'/>\n");
		}
		b.append("</way>\n");
		return id;
	}

	private void printTransportDetailInfo(VerboseInfo verbose, BinaryMapIndexReader index, TransportIndex p) throws IOException {
		SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(
				MapUtils.get31TileNumberX(verbose.lonleft),
				MapUtils.get31TileNumberX(verbose.lonright),
				MapUtils.get31TileNumberY(verbose.lattop),
				MapUtils.get31TileNumberY(verbose.latbottom),
				-1, null);
		List<TransportStop> stops = index.searchTransportIndex(sr);
		Map<Long, TransportRoute> rs = new LinkedHashMap<>();
		List<String> lrs = new ArrayList<>();
		println("\nStops:");
		for (TransportStop s : stops) {
			lrs.clear();
			for (int pnt : s.getReferencesToRoutes()) {
				TransportRoute route;
				if (!lrs.contains(pnt)) {
					TIntObjectHashMap<TransportRoute> pts = index.getTransportRoutes(new int[] { pnt });
					route = pts.valueCollection().iterator().next();
					rs.put((long) pnt, route);
				} else {
					route = rs.get(pnt);
				}
				if (route != null) {
					//lrs.add(route.getRef() + " " + route.getName(verbose.lang));
					lrs.add(route.getRef() + " " + route.getType());
				}
			}

			println("  " + s.getName(verbose.lang) + ": " + lrs + " " + s.getLocation());
		}
		println("\nRoutes:");
		for(TransportRoute s : rs.values()) {
			List<String> stopsString = new ArrayList<>();
			for(TransportStop st : s.getForwardStops()) {
				stopsString.add(st.getName(verbose.lang));
			}
			println("  " + s.getRef() + " " + s.getType() + " " + s.getName(verbose.lang) + ": " + stopsString);
		}
	}


	private void printPOIDetailInfo(VerboseInfo verbose, BinaryMapIndexReader index, PoiRegion p) throws IOException {
		SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
				MapUtils.get31TileNumberX(verbose.lonleft),
				MapUtils.get31TileNumberX(verbose.lonright),
				MapUtils.get31TileNumberY(verbose.lattop),
				MapUtils.get31TileNumberY(verbose.latbottom),
				verbose.getZoom(),
				BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
				new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						Iterator<Entry<String, String>> it = object.getAdditionalInfo().entrySet().iterator();
						String s = "";
						while (it.hasNext()) {
							Entry<String, String> e = it.next();
							if (e.getValue().startsWith(" gz ")) {
								s += " " + e.getKey() + "=...";
							} else {
								s += " " + e.getKey() + "=" + e.getValue();
							}
						}

						println(object.getType().getKeyName() + " : " + object.getSubType() + " " + object.getName() + " " + object.getLocation() + " id=" + (object.getId() >> 1) + " " + s);
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});

		index.initCategories(p);
		println("\tRegion: " + p.name);

		println("\t\tBounds " + formatLatBounds(MapUtils.get31LongitudeX(p.left31),
				MapUtils.get31LongitudeX(p.right31),
				MapUtils.get31LatitudeY(p.top31),
				MapUtils.get31LatitudeY(p.bottom31)));
		println("\t\tCategories:");
		for (int i = 0; i < p.categories.size(); i++) {
			println("\t\t\t" + p.categories.get(i));
			for (int j = 0; j < p.subcategories.get(i).size(); j++)
				println("\t\t\t\t" + p.subcategories.get(i).get(j));
		}
		println("\t\tSubtypes:");
		for (int i = 0; i < p.subTypes.size(); i++) {
			PoiSubType st = p.subTypes.get(i);
			println("\t\t\t" + st.name + " " + (st.text ? "text" : (" encoded " + st.possibleValues.size())));
		}
//		req.poiTypeFilter = null;//for test only
		index.searchPoi(p, req);

	}

	public static void printUsage(String warning) {
		if (warning != null) {
			System.out.println(warning);
		}
		System.out.println("Inspector is console utility for working with binary indexes of OsmAnd.");
		System.out.println("It allows print info about file, extract parts and merge indexes.");
		System.out.println("\nUsage for print info : inspector [-vaddress] [-vcitynames] [-vstreetgroups] [-vstreets] [-vbuildings] [-vintersections] [-vmap] [-vmapobjects] [-vmapcoordinates] [-osm] [-vpoi] [-vrouting] [-vtransport] [-zoom=Zoom] [-bbox=LeftLon,TopLat,RightLon,BottomLat] [file]");
		System.out.println("  Prints information about [file] binary index of OsmAnd.");
		System.out.println("  -v.. more verbose output (like all cities and their streets or all map objects with tags/values and coordinates)");
		System.out.println("\nUsage for combining indexes : inspector -c file_to_create (file_from_extract ((+|-)parts_to_extract)? )*");
		System.out.println("\tCreate new file of extracted parts from input file. [parts_to_extract] could be parts to include or exclude.");
		System.out.println("  Example : inspector -c output_file input_file +1,2,3\n\tExtracts 1, 2, 3 parts (could be find in print info)");
		System.out.println("  Example : inspector -c output_file input_file -2,3\n\tExtracts all parts excluding 2, 3");
		System.out.println("  Example : inspector -c output_file input_file1 input_file2 input_file3\n\tSimply combine 3 files");
		System.out.println("  Example : inspector -c output_file input_file1 input_file2 -4\n\tCombine all parts of 1st file and all parts excluding 4th part of 2nd file");
	}

}
