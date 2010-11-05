package net.osmand.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.osmand.Algoritms;
import net.osmand.binary.OsmandOdb.CityIndex;
import net.osmand.binary.OsmandOdb.InteresectedStreets;
import net.osmand.binary.OsmandOdb.OsmAndTransportIndex;
import net.osmand.binary.OsmandOdb.PostcodeIndex;
import net.osmand.binary.OsmandOdb.StreetIndex;
import net.osmand.binary.OsmandOdb.StreetIntersection;
import net.osmand.binary.OsmandOdb.TransportRoute;
import net.osmand.binary.OsmandOdb.TransportRouteStop;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportStop;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.sf.junidecode.Junidecode;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import com.google.protobuf.WireFormat.FieldType;

public class BinaryMapIndexWriter {

	private RandomAccessFile raf;
	private CodedOutputStream codedOutStream;
	protected static final int SHIFT_COORDINATES = 5;
	
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
	
	private final static int ADDRESS_INDEX_INIT = 5;
	private final static int CITY_INDEX_INIT = 6;
	private final static int POSTCODES_INDEX_INIT = 7;
	private final static int VILLAGES_INDEX_INIT = 8;
	
	private final static int TRANSPORT_INDEX_INIT = 9;
	private final static int TRANSPORT_STOPS_TREE = 10;

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
	
	public void finishWriting(){
		
	}
	

	private void preserveInt32Size() throws IOException {
		codedOutStream.flush();
		stackSizes.push(raf.getFilePointer());
		codedOutStream.writeFixed32NoTag(0);
	}
	
	private int writeInt32Size() throws IOException{
		codedOutStream.flush();
		long filePointer = raf.getFilePointer();
		Long old = stackSizes.pop();
		int length = (int) (filePointer - old - 4);
		raf.seek(old);
		raf.writeInt(length);
		raf.seek(filePointer);
		return length;
	}
	
	public void startWriteMapIndex() throws IOException{
		pushState(MAP_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
	}
	
	
	public void endWriteMapIndex() throws IOException{
		popState(MAP_INDEX_INIT);
		int len = writeInt32Size();
		System.out.println("MAP INDEX SIZE : " + len);
	}
		
	public void startWriteMapLevelIndex(int minZoom, int maxZoom, int leftX, int rightX, int topY, int bottomY) throws IOException{
		pushState(MAP_ROOT_LEVEL_INIT, MAP_INDEX_INIT);
		
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
		popState(MAP_ROOT_LEVEL_INIT);
		stackBounds.pop();
		writeInt32Size();
	}
	
	public void startMapTreeElement(int leftX, int rightX, int topY, int bottomY) throws IOException{
		startMapTreeElement(-1L, leftX, rightX, topY, bottomY);
	}
	
	public void startMapTreeElement(long baseId, int leftX, int rightX, int topY, int bottomY) throws IOException{
		checkPeekState(MAP_ROOT_LEVEL_INIT, MAP_TREE);
		if(state.peek() == MAP_ROOT_LEVEL_INIT){
			codedOutStream.writeTag(OsmandOdb.MapRootLevel.ROOT_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		} else {
			codedOutStream.writeTag(OsmandOdb.MapTree.SUBTREES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		}
		state.push(MAP_TREE);
		preserveInt32Size();
		
		
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
		popState(MAP_TREE);
		
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
	
	// debug data about size of map index
	public static int COORDINATES_SIZE = 0;
	public static int COORDINATES_COUNT = 0;
	public static int ID_SIZE = 0;
	public static int TYPES_SIZE = 0;
	public static int MAP_DATA_SIZE = 0;
	public static int STRING_TABLE_SIZE = 0;
	
	
	protected static int codeCoordinateDifference(int x, int px){
		// shift absolute coordinates first and get truncated
		return (x >> SHIFT_COORDINATES) - (px >> SHIFT_COORDINATES);
	}
	

	
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
			int x = Algoritms.parseIntFromBytes(nodes, i * 8);
			int y = Algoritms.parseIntFromBytes(nodes, i * 8 + 4);
			sizeCoordinates += CodedOutputStream.computeSInt32SizeNoTag(codeCoordinateDifference(x, px));
			sizeCoordinates += CodedOutputStream.computeSInt32SizeNoTag(codeCoordinateDifference(y, py));
			px = x;
			py = y;
			COORDINATES_COUNT += 2;
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
		
		px = bounds.leftX;
		py = bounds.topY;
		for (int i = 0; i < nodes.length / 8; i++) {
			int x = Algoritms.parseIntFromBytes(nodes, i * 8);
			int y = Algoritms.parseIntFromBytes(nodes, i * 8 + 4);
			codedOutStream.writeSInt32NoTag(codeCoordinateDifference(x, px));
			codedOutStream.writeSInt32NoTag(codeCoordinateDifference(y, py));
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
	
	public void startWriteAddressIndex(String name) throws IOException {
		pushState(ADDRESS_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		
		codedOutStream.writeString(OsmandOdb.OsmAndAddressIndex.NAME_FIELD_NUMBER, name);
		
	}
	
	public void endWriteAddressIndex() throws IOException {
		popState(ADDRESS_INDEX_INIT);
		int len = writeInt32Size();
		System.out.println("ADDRESS INDEX SIZE : " + len);
	}
	
	private boolean checkEnNameToWrite(MapObject obj){
		if(obj.getEnName() == null){
			return false;
		}
		return !obj.getEnName().equals(Junidecode.unidecode(obj.getName()));
	}
	
	public void writeCityIndex(City city, List<Street> streets, Map<Street, List<Node>> wayNodes) throws IOException {
		if(city.getType() == City.CityType.CITY || city.getType() == City.CityType.TOWN){
			checkPeekState(CITY_INDEX_INIT);
		} else {
			checkPeekState(VILLAGES_INDEX_INIT);
		}
		CityIndex.Builder cityInd = OsmandOdb.CityIndex.newBuilder();
		cityInd.setCityType(city.getType().ordinal());
		cityInd.setId(city.getId());
		
		cityInd.setName(city.getName());
		if(checkEnNameToWrite(city)){
			cityInd.setNameEn(city.getEnName());
		}
		int cx = MapUtils.get31TileNumberX(city.getLocation().getLongitude());
		int cy = MapUtils.get31TileNumberY(city.getLocation().getLatitude());
		cityInd.setX(cx);
		cityInd.setY(cy);
		
		if(wayNodes != null){
			InteresectedStreets.Builder sbuilders = OsmandOdb.InteresectedStreets.newBuilder();
			Map<Long, Set<Integer>> reverseMap = new LinkedHashMap<Long, Set<Integer>>();
			for (int i = 0; i < streets.size(); i++) {
				streets.get(i).setIndexInCity(i);
				for (Node n : wayNodes.get(streets.get(i))) {
					if(!reverseMap.containsKey(n.getId())){
						reverseMap.put(n.getId(), new LinkedHashSet<Integer>(3));
					}
					reverseMap.get(n.getId()).add(i);
				}
			}
			Set<Integer> checkedStreets = new LinkedHashSet<Integer>();
			for (int i = 0; i < streets.size(); i++) {
				Street s1 = streets.get(i);
				checkedStreets.clear();
				for(Node intersection : wayNodes.get(s1)){
					for(Integer j : reverseMap.get(intersection.getId())){
						if(i >= j || checkedStreets.contains(j)){
							continue;
						}
						checkedStreets.add(j);
						StreetIntersection.Builder builder = OsmandOdb.StreetIntersection.newBuilder();
						builder.setIntersectedStreet1(i);
						builder.setIntersectedStreet2(j);
						int sx = MapUtils.get31TileNumberX(intersection.getLongitude());
						int sy = MapUtils.get31TileNumberY(intersection.getLatitude());
						builder.setIntersectedX((sx - cx) >> 7);
						builder.setIntersectedY((sy - cy) >> 7);
						sbuilders.addIntersections(builder.build());
					}
				}
			}
			cityInd.setIntersections(sbuilders.build());
		}
		
		for(Street s : streets){
			StreetIndex streetInd = createStreetAndBuildings(s, cx, cy, null);
			cityInd.addStreets(streetInd);
		}
		codedOutStream.writeMessage(OsmandOdb.CitiesIndex.CITIES_FIELD_NUMBER, cityInd.build());
	}
	
	public void startCityIndexes(boolean villages) throws IOException {
		pushState(villages ? VILLAGES_INDEX_INIT : CITY_INDEX_INIT, ADDRESS_INDEX_INIT);
		codedOutStream.writeTag(villages ? OsmandOdb.OsmAndAddressIndex.VILLAGES_FIELD_NUMBER
				: OsmandOdb.OsmAndAddressIndex.CITIES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
	}
	
	public void endCityIndexes(boolean villages) throws IOException {
		popState(villages ? VILLAGES_INDEX_INIT : CITY_INDEX_INIT);
		int length = writeInt32Size();
		System.out.println("CITIES size " + length + " " + villages);
	}
	
	
	public void startPostcodes() throws IOException {
		pushState(POSTCODES_INDEX_INIT, ADDRESS_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndAddressIndex.POSTCODES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
	}
	
	public void endPostcodes() throws IOException {
		popState(POSTCODES_INDEX_INIT);
		int postcodes = writeInt32Size();
		System.out.println("POSTCODES size " + postcodes);
	}
	

	
	public void writePostcode(String postcode, Collection<Street> streets) throws IOException {
		checkPeekState(POSTCODES_INDEX_INIT);
		
		if(streets.isEmpty()){
			return;
		}
		postcode = postcode.toUpperCase();
		LatLon loc = streets.iterator().next().getLocation();
		PostcodeIndex.Builder post = OsmandOdb.PostcodeIndex.newBuilder();
		post.setPostcode(postcode);
		int cx = MapUtils.get31TileNumberX(loc.getLongitude());
		int cy = MapUtils.get31TileNumberY(loc.getLatitude());
		post.setX(cx);
		post.setY(cy);

		for(Street s : streets){
			StreetIndex streetInd = createStreetAndBuildings(s, cx, cy, postcode);
			post.addStreets(streetInd);
		}
		codedOutStream.writeMessage(OsmandOdb.PostcodesIndex.POSTCODES_FIELD_NUMBER, post.build());
	}
	

	protected StreetIndex createStreetAndBuildings(Street street, int cx, int cy, String postcodeFilter) throws IOException {
		checkPeekState(CITY_INDEX_INIT, VILLAGES_INDEX_INIT, POSTCODES_INDEX_INIT);
		boolean inCity = state.peek() == CITY_INDEX_INIT || state.peek() == VILLAGES_INDEX_INIT;
		StreetIndex.Builder streetBuilder = OsmandOdb.StreetIndex.newBuilder();
		streetBuilder.setName(street.getName());
		if(checkEnNameToWrite(street)){
			streetBuilder.setNameEn(street.getEnName());
		}
		streetBuilder.setId(street.getId());
		
		
		int sx = MapUtils.get31TileNumberX(street.getLocation().getLongitude());
		int sy = MapUtils.get31TileNumberY(street.getLocation().getLatitude());
		streetBuilder.setX((sx - cx) >> 7);
		streetBuilder.setY((sy - cy) >> 7);
		
		for(Building b : street.getBuildings()){
			if(postcodeFilter != null && !postcodeFilter.equalsIgnoreCase(b.getPostcode())){
				continue;
			}
			OsmandOdb.BuildingIndex.Builder bbuilder= OsmandOdb.BuildingIndex.newBuilder();
			int bx = MapUtils.get31TileNumberX(b.getLocation().getLongitude());
			int by = MapUtils.get31TileNumberY(b.getLocation().getLatitude());
			bbuilder.setX((bx - sx) >> 7);
			bbuilder.setY((by - sy) >> 7);
			bbuilder.setId(b.getId());
			bbuilder.setName(b.getName());
			if(checkEnNameToWrite(b)){
				bbuilder.setNameEn(b.getEnName());
			}
			if(inCity && b.getPostcode() != null){
				bbuilder.setPostcode(b.getPostcode());
			}
			streetBuilder.addBuildings(bbuilder.build());
		}
		
		return streetBuilder.build();
	}
	
	public void startWriteTransportIndex() throws IOException {
		pushState(TRANSPORT_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		stackBounds.push(new Bounds(0, 0, 0, 0)); // for transport stops tree
		preserveInt32Size();
	}
	
	public void endWriteTransportIndex() throws IOException {
		popState(TRANSPORT_INDEX_INIT);
		int len = writeInt32Size();
		stackBounds.pop();
		System.out.println("TRANSPORT INDEX SIZE : " + len);
	}

	private int registerString(Map<String, Integer> stringTable, String s) {
		if(s == null){
			s = "";
		}
		if (stringTable.containsKey(s)) {
			return stringTable.get(s);
		}
		int size = stringTable.size();
		stringTable.put(s, size);
		return size;
	}
	
	public void writeTransportRoute(long idRoute, String routeName, String routeEnName, String ref, String operator, String type,
			int dist, List<TransportStop> directStops, List<TransportStop> reverseStops, 
			Map<String, Integer> stringTable, Map<Long, Long> transportRoutesRegistry) throws IOException {
		checkPeekState(TRANSPORT_INDEX_INIT);
		TransportRoute.Builder tRoute = OsmandOdb.TransportRoute.newBuilder();
		tRoute.setRef(ref);
		tRoute.setOperator(registerString(stringTable, operator));
		tRoute.setType(registerString(stringTable, type));
		tRoute.setId(idRoute);
		tRoute.setName(registerString(stringTable, routeName));
		tRoute.setDistance(dist);
		
		if(routeEnName != null){
			tRoute.setNameEn(registerString(stringTable, routeEnName));
		}
		for (int i = 0; i < 2; i++) {
			List<TransportStop> stops = i == 0 ? directStops : reverseStops;
			long id = 0;
			int x24 = 0;
			int y24 = 0;
			for (TransportStop st : stops) {
				TransportRouteStop.Builder tStop = OsmandOdb.TransportRouteStop.newBuilder();
				tStop.setId(st.getId() - id);
				id = st.getId();
				int x = (int) MapUtils.getTileNumberX(24, st.getLocation().getLongitude());
				int y = (int) MapUtils.getTileNumberY(24, st.getLocation().getLatitude());
				tStop.setDx(x - x24);
				tStop.setDy(y - y24);
				x24 = x;
				y24 = y;
				tStop.setName(registerString(stringTable, st.getName()));
				if (st.getEnName() != null) {
					tStop.setNameEn(registerString(stringTable, st.getEnName()));
				}
				if (i == 0) {
					tRoute.addDirectStops(tStop.build());
				} else {
					tRoute.addReverseStops(tStop.build());
				}
			}
		}
		codedOutStream.writeTag(OsmandOdb.OsmAndTransportIndex.ROUTES_FIELD_NUMBER, FieldType.MESSAGE.getWireType());
		codedOutStream.flush();
		if(transportRoutesRegistry != null){
			transportRoutesRegistry.put(idRoute, raf.getFilePointer());
		}
		codedOutStream.writeMessageNoTag(tRoute.build());
	}

	public void startTransportTreeElement(int leftX, int rightX, int topY, int bottomY) throws IOException {
		checkPeekState(TRANSPORT_STOPS_TREE, TRANSPORT_INDEX_INIT);
		if(state.peek() == TRANSPORT_STOPS_TREE){
			codedOutStream.writeTag(OsmandOdb.TransportStopsTree.SUBTREES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		} else {
			codedOutStream.writeTag(OsmandOdb.OsmAndTransportIndex.STOPS_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		}
		state.push(TRANSPORT_STOPS_TREE);
		preserveInt32Size();
		
		
		Bounds bounds = stackBounds.peek();
		
		codedOutStream.writeSInt32(OsmandOdb.TransportStopsTree.LEFT_FIELD_NUMBER, leftX - bounds.leftX);
		codedOutStream.writeSInt32(OsmandOdb.TransportStopsTree.RIGHT_FIELD_NUMBER, rightX - bounds.rightX);
		codedOutStream.writeSInt32(OsmandOdb.TransportStopsTree.TOP_FIELD_NUMBER, topY - bounds.topY);
		codedOutStream.writeSInt32(OsmandOdb.TransportStopsTree.BOTTOM_FIELD_NUMBER, bottomY - bounds.bottomY);
		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
		stackBaseIds.push(-1L);
		
		
	}

	public void endWriteTransportTreeElement() throws IOException {
		Long baseId = stackBaseIds.pop();
		if(baseId >= 0){
			codedOutStream.writeUInt64(OsmandOdb.TransportStopsTree.BASEID_FIELD_NUMBER, baseId);
		}
		popState(TRANSPORT_STOPS_TREE);
		stackBounds.pop();
		writeInt32Size();
	}
	
	public long getFilePointer() throws IOException {
		codedOutStream.flush();
		return raf.getFilePointer();
	}
	
	public void writeTransportStop(long id, int x24, int y24, String name, String nameEn, 
			Map<String, Integer> stringTable, List<Long> routes) throws IOException {
		checkPeekState(TRANSPORT_STOPS_TREE);

		Bounds bounds = stackBounds.peek();
		if (stackBaseIds.peek() == -1) {
			stackBaseIds.pop();
			stackBaseIds.push(id);
		}
		codedOutStream.flush();
		long fp = raf.getFilePointer();
		OsmandOdb.TransportStop.Builder ts = OsmandOdb.TransportStop.newBuilder();
		ts.setName(registerString(stringTable, name));
		if(nameEn != null){
			ts.setNameEn(registerString(stringTable, nameEn));
		}
		ts.setDx(x24 - bounds.leftX);
		ts.setDy(y24 - bounds.topY);
		ts.setId(id -stackBaseIds.peek());
		for(Long i : routes){
			ts.addRoutes((int)(fp - i));
		}
		
		codedOutStream.writeMessage(OsmandOdb.TransportStopsTree.LEAFS_FIELD_NUMBER, ts.build());
	}
	
	
	public void writeTransportStringTable(Map<String, Integer> stringTable) throws IOException {
		checkPeekState(TRANSPORT_INDEX_INIT);
		// expect linked hash map
		int i = 0;
		OsmandOdb.StringTable.Builder st = OsmandOdb.StringTable.newBuilder();
		for(String s : stringTable.keySet()){
			if(stringTable.get(s) != i++){
				throw new IllegalStateException();
			}
			st.addS(s);
		}
		codedOutStream.writeMessage(OsmAndTransportIndex.STRINGTABLE_FIELD_NUMBER, st.build());
	}
	
	 
	private void pushState(int push, int peek){
		if(state.peek() != peek){
			throw new IllegalStateException("expected " + peek+ " != "+ state.peek());
		}
		state.push(push);
	}
	
	private void checkPeekState(int... states) {
		for (int i = 0; i < states.length; i++) {
			if (states[i] == state.peek()) {
				return;
			}
		}
		throw new IllegalStateException("Note expected state : " + state.peek());
	}
	
	private void popState(int state){
		Integer st = this.state.pop();
		if(st != state){
			throw new IllegalStateException("expected " + state + " != "+ st);
		}
	}
	
	public void close() throws IOException{
		checkPeekState(OSMAND_STRUCTURE_INIT);
		codedOutStream.writeInt32(OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER, IndexConstants.BINARY_MAP_VERSION);
		codedOutStream.flush();
	}




}
