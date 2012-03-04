package net.osmand.data.preparation;

import gnu.trove.TByteCollection;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.osmand.Algoritms;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.OsmandOdb;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.CityBlockIndex;
import net.osmand.binary.OsmandOdb.CityIndex;
import net.osmand.binary.OsmandOdb.MapData;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.binary.OsmandOdb.OsmAndAddressIndex;
import net.osmand.binary.OsmandOdb.OsmAndAddressIndex.CitiesIndex;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapDataBox;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapEncodingRule;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex.MapRootLevel;
import net.osmand.binary.OsmandOdb.IndexedStringTable;
import net.osmand.binary.OsmandOdb.OsmAndMapIndex;
import net.osmand.binary.OsmandOdb.OsmAndPoiBoxDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndTransportIndex;
import net.osmand.binary.OsmandOdb.StreetIndex;
import net.osmand.binary.OsmandOdb.StreetIntersection;
import net.osmand.binary.OsmandOdb.StringTable;
import net.osmand.binary.OsmandOdb.TransportRoute;
import net.osmand.binary.OsmandOdb.TransportRouteStop;
import net.osmand.binary.OsmandOdb.OsmAndCategoryTable.Builder;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.IndexConstants;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.data.TransportStop;
import net.osmand.data.preparation.IndexPoiCreator.PoiTileBox;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.MapRenderingTypes.MapRulType;
import net.sf.junidecode.Junidecode;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import com.google.protobuf.WireFormat;
import com.google.protobuf.WireFormat.FieldType;

public class BinaryMapIndexWriter {

	private RandomAccessFile raf;
	private CodedOutputStream codedOutStream;
	protected static final int SHIFT_COORDINATES = BinaryMapIndexReader.SHIFT_COORDINATES;
	private static Log log = LogFactory.getLog(BinaryMapIndexWriter.class);

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
	private Stack<Long> stackBaseIds = new Stack<Long>();

	// internal constants to track state of index writing
	private Stack<Integer> state = new Stack<Integer>();
	private Stack<BinaryFileReference> stackSizes = new Stack<BinaryFileReference>();

	private final static int OSMAND_STRUCTURE_INIT = 1;
	private final static int MAP_INDEX_INIT = 2;
	private final static int MAP_ROOT_LEVEL_INIT = 3;
	private final static int MAP_TREE = 4;

	private final static int ADDRESS_INDEX_INIT = 5;
	private final static int CITY_INDEX_INIT = 6;

	private final static int TRANSPORT_INDEX_INIT = 9;
	private final static int TRANSPORT_STOPS_TREE = 10;
	private final static int TRANSPORT_ROUTES = 11;

	private final static int POI_INDEX_INIT = 12;
	private final static int POI_BOX = 13;
	private final static int POI_DATA = 14;

	public BinaryMapIndexWriter(final RandomAccessFile raf) throws IOException {
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
		codedOutStream.writeUInt32(OsmandOdb.OsmAndStructure.VERSION_FIELD_NUMBER, IndexConstants.BINARY_MAP_VERSION);
		codedOutStream.writeInt64(OsmandOdb.OsmAndStructure.DATECREATED_FIELD_NUMBER, System.currentTimeMillis());
		state.push(OSMAND_STRUCTURE_INIT);
	}

	public void finishWriting() {

	}

	private BinaryFileReference preserveInt32Size() throws IOException {
		long filePointer = getFilePointer();
		BinaryFileReference ref = BinaryFileReference.createSizeReference(filePointer);
		stackSizes.push(ref);
		codedOutStream.writeFixed32NoTag(0);
		return ref;
	}

	public long getFilePointer() throws IOException {
		// codedOutStream.flush();
		// return raf.getFilePointer();
		return codedOutStream.getWrittenBytes();
	}

	private void writeFixed32(long posToWrite, int value, long currentPosition) throws IOException {
		raf.seek(posToWrite);
		raf.writeInt(value);
		raf.seek(currentPosition);
	}

	private int writeInt32Size() throws IOException {
		long filePointer = getFilePointer();
		BinaryFileReference ref = stackSizes.pop();
		int length = ref.writeReference(raf, filePointer);
		return length;
	}

	public void startWriteMapIndex(String name) throws IOException {
		pushState(MAP_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.MAPINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		if (name != null) {
			codedOutStream.writeString(OsmandOdb.OsmAndMapIndex.NAME_FIELD_NUMBER, name);
		}
	}

	public void endWriteMapIndex() throws IOException {
		popState(MAP_INDEX_INIT);
		int len = writeInt32Size();
		log.info("MAP INDEX SIZE : " + len);
	}

	public void startWriteMapLevelIndex(int minZoom, int maxZoom, int leftX, int rightX, int topY, int bottomY) throws IOException {
		pushState(MAP_ROOT_LEVEL_INIT, MAP_INDEX_INIT);

		codedOutStream.writeTag(OsmandOdb.OsmAndMapIndex.LEVELS_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();

		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.MAXZOOM_FIELD_NUMBER, maxZoom);
		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.MINZOOM_FIELD_NUMBER, minZoom);
		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.LEFT_FIELD_NUMBER, leftX);
		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.RIGHT_FIELD_NUMBER, rightX);
		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.TOP_FIELD_NUMBER, topY);
		codedOutStream.writeInt32(OsmandOdb.OsmAndMapIndex.MapRootLevel.BOTTOM_FIELD_NUMBER, bottomY);

		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
	}

	public void endWriteMapLevelIndex() throws IOException {
		popState(MAP_ROOT_LEVEL_INIT);
		stackBounds.pop();
		writeInt32Size();
	}

	public void writeMapEncodingRules(Map<String, MapRulType> types) throws IOException {
		checkPeekState(MAP_INDEX_INIT);
		long fp = getFilePointer();

		ArrayList<MapRulType> out = new ArrayList<MapRulType>();
		int highestTargetId = types.size();
		// 1. prepare map rule type to write
		for (MapRulType t : types.values()) {
			if (t.getTargetTagValue() != null || t.getFreq() == 0) {
				t.setTargetId(highestTargetId++);
			} else {
				out.add(t);
			}
		}

		// 2. sort by frequency and assign ids
		Collections.sort(out, new Comparator<MapRulType>() {
			@Override
			public int compare(MapRulType o1, MapRulType o2) {
				return o1.getFreq() - o2.getFreq();
			}
		});

		for (int i = 0; i < out.size(); i++) {
			MapEncodingRule.Builder builder = OsmandOdb.OsmAndMapIndex.MapEncodingRule.newBuilder();
			MapRulType rule = out.get(i);
			rule.setTargetId(i);

			builder.setTag(rule.getTag());
			if (rule.getValue() != null) {
				builder.setValue(rule.getValue());
			}
			builder.setMinZoom(rule.getMinzoom());
			if (rule.isAdditional()) {
				builder.setType(1);
			}
			codedOutStream.writeMessage(OsmandOdb.OsmAndMapIndex.RULES_FIELD_NUMBER, builder.build());
		}
		long newfp = getFilePointer();
		System.out.println("RENDERING SCHEMA takes " + (newfp - fp));
	}

	public BinaryFileReference startMapTreeElement(int leftX, int rightX, int topY, int bottomY, boolean containsLeaf) throws IOException {
		checkPeekState(MAP_ROOT_LEVEL_INIT, MAP_TREE);
		if (state.peek() == MAP_ROOT_LEVEL_INIT) {
			codedOutStream.writeTag(MapRootLevel.BOXES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		} else {
			codedOutStream.writeTag(MapDataBox.BOXES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		}
		state.push(MAP_TREE);
		preserveInt32Size();
		long fp = getFilePointer();

		Bounds bounds = stackBounds.peek();
		codedOutStream.writeSInt32(MapDataBox.LEFT_FIELD_NUMBER, leftX - bounds.leftX);
		codedOutStream.writeSInt32(MapDataBox.RIGHT_FIELD_NUMBER, rightX - bounds.rightX);
		codedOutStream.writeSInt32(MapDataBox.TOP_FIELD_NUMBER, topY - bounds.topY);
		codedOutStream.writeSInt32(MapDataBox.BOTTOM_FIELD_NUMBER, bottomY - bounds.bottomY);
		stackBounds.push(new Bounds(leftX, rightX, topY, bottomY));
		BinaryFileReference ref = null;
		if (containsLeaf) {
			ref = BinaryFileReference.createShiftReference(getFilePointer(), fp);
		}
		return ref;
	}

	public void endWriteMapTreeElement() throws IOException {
		popState(MAP_TREE);
		stackBounds.pop();
		writeInt32Size();
	}

	// debug data about size of map index
	public static int COORDINATES_SIZE = 0;
	public static int COORDINATES_COUNT = 0;
	public static int ID_SIZE = 0;
	public static int TYPES_SIZE = 0;
	public static int MAP_DATA_SIZE = 0;
	public static int STRING_TABLE_SIZE = 0;

	public MapDataBlock.Builder createWriteMapDataBlock(long baseid) throws IOException {
		MapDataBlock.Builder builder = MapDataBlock.newBuilder();
		builder.setBaseId(baseid);
		return builder;
	}

	public void writeMapDataBlock(MapDataBlock.Builder builder, Map<String, Integer> stringTable, BinaryFileReference ref)
			throws IOException {
		checkPeekState(MAP_INDEX_INIT);
		StringTable.Builder bs = OsmandOdb.StringTable.newBuilder();
		for (String s : stringTable.keySet()) {
			bs.addS(s);
		}
		StringTable st = bs.build();
		builder.setStringTable(st);
		int size = st.getSerializedSize();
		STRING_TABLE_SIZE += CodedOutputStream.computeTagSize(OsmandOdb.MapDataBlock.STRINGTABLE_FIELD_NUMBER)
				+ CodedOutputStream.computeRawVarint32Size(size) + size;

		ref.writeReference(raf, getFilePointer());
		MapDataBlock block = builder.build();
		MAP_DATA_SIZE += block.getSerializedSize();
		codedOutStream.writeMessage(OsmAndMapIndex.BLOCKS_FIELD_NUMBER, block);
	}

	/**
	 * Encode and write a varint. {@code value} is treated as unsigned, so it won't be sign-extended if negative.
	 */
	public void writeRawVarint32(TByteArrayList bf, int value) throws IOException {
		while (true) {
			if ((value & ~0x7F) == 0) {
				writeRawByte(bf, value);
				return;
			} else {
				writeRawByte(bf, (value & 0x7F) | 0x80);
				value >>>= 7;
			}
		}
	}

	/** Write a single byte. */
	public void writeRawByte(TByteArrayList bf, final int value) throws IOException {
		bf.add((byte) value);
	}

	private TByteArrayList mapDataBuf = new TByteArrayList();

	public void writeMapData(long diffId, int pleft, int ptop, boolean area, byte[] coordinates, byte[] innerPolygonTypes, byte[] types,
			byte[] additionalTypes, Map<MapRulType, String> names, Map<String, Integer> stringTable, MapDataBlock.Builder dataBlock)
			throws IOException {

		MapData.Builder data = MapData.newBuilder();
		// calculate size
		mapDataBuf.clear();
		int pcalcx = pleft;
		int pcalcy = ptop;
		for (int i = 0; i < coordinates.length / 8; i++) {
			int x = Algoritms.parseIntFromBytes(coordinates, i * 8);
			int y = Algoritms.parseIntFromBytes(coordinates, i * 8 + 4);
			int tx = (x - pcalcx) >> SHIFT_COORDINATES;
			int ty = (y - pcalcy) >> SHIFT_COORDINATES;

			writeRawVarint32(mapDataBuf, tx);
			writeRawVarint32(mapDataBuf, ty);

			pcalcx = pcalcx + (tx << SHIFT_COORDINATES);
			pcalcy = pcalcy + (ty << SHIFT_COORDINATES);
		}
		COORDINATES_SIZE += CodedOutputStream.computeRawVarint32Size(mapDataBuf.size())
				+ CodedOutputStream.computeTagSize(MapData.COORDINATES_FIELD_NUMBER) + mapDataBuf.size();
		if (area) {
			data.setAreaCoordinates(ByteString.copyFrom(mapDataBuf.toArray()));
		} else {
			data.setCoordinates(ByteString.copyFrom(mapDataBuf.toArray()));
		}

		if (innerPolygonTypes.length > 0) {
			mapDataBuf.clear();
			pcalcx = pleft;
			pcalcy = ptop;
			for (int i = 0; i < innerPolygonTypes.length / 8; i++) {
				int x = Algoritms.parseIntFromBytes(coordinates, i * 8);
				int y = Algoritms.parseIntFromBytes(coordinates, i * 8 + 4);
				if (x == 0 && y == 0) {
					if (mapDataBuf.size() > 0) {
						data.addPolygonInnerCoordinates(ByteString.copyFrom(mapDataBuf.toArray()));
						mapDataBuf.clear();
					}
					pcalcx = pleft;
					pcalcy = ptop;
				} else {
					int tx = (x - pcalcx) >> SHIFT_COORDINATES;
					int ty = (y - pcalcy) >> SHIFT_COORDINATES;

					writeRawVarint32(mapDataBuf, tx);
					writeRawVarint32(mapDataBuf, ty);

					pcalcx = pcalcx + (tx << SHIFT_COORDINATES);
					pcalcy = pcalcy + (ty << SHIFT_COORDINATES);
				}
			}
		}

		data.setTypes(ByteString.copyFrom(types));
		TYPES_SIZE += CodedOutputStream.computeTagSize(OsmandOdb.MapData.TYPES_FIELD_NUMBER)
				+ CodedOutputStream.computeRawVarint32Size(types.length) + types.length;
		if (additionalTypes.length > 0) {
			data.setAdditionalTypes(ByteString.copyFrom(additionalTypes));
			TYPES_SIZE += CodedOutputStream.computeTagSize(OsmandOdb.MapData.ADDITIONALTYPES_FIELD_NUMBER)
					+ CodedOutputStream.computeRawVarint32Size(additionalTypes.length) + additionalTypes.length;
		}

		mapDataBuf.clear();
		for (Entry<MapRulType, String> s : names.entrySet()) {
			writeRawVarint32(mapDataBuf, s.getKey().getTargetId());
			Integer ls = stringTable.get(s.getValue());
			if (ls == null) {
				ls = stringTable.size();
				stringTable.put(s.getValue(), ls);
			}
			writeRawVarint32(mapDataBuf, ls);
		}
		STRING_TABLE_SIZE += mapDataBuf.size();
		data.setStringNames(ByteString.copyFrom(mapDataBuf.toArray()));

		data.setId(diffId);
		ID_SIZE += CodedOutputStream.computeSInt64Size(OsmandOdb.MapData.ID_FIELD_NUMBER, diffId);

	}

	public void startWriteAddressIndex(String name) throws IOException {
		pushState(ADDRESS_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();

		codedOutStream.writeString(OsmandOdb.OsmAndAddressIndex.NAME_FIELD_NUMBER, name);
		codedOutStream.writeString(OsmandOdb.OsmAndAddressIndex.NAME_EN_FIELD_NUMBER, Junidecode.unidecode(name));

		// skip boundaries
	}

	public void endWriteAddressIndex() throws IOException {
		popState(ADDRESS_INDEX_INIT);
		int len = writeInt32Size();
		log.info("ADDRESS INDEX SIZE : " + len);
	}
	
	
	public void writeAddressNameIndex(Map<String, List<MapObject>> namesIndex) throws IOException {
		checkPeekState(CITY_INDEX_INIT);
		codedOutStream.writeTag(OsmAndAddressIndex.NAMEINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		
		Map<String, BinaryFileReference> res = writeIndexedTable(OsmAndAddressNameIndexData.TABLE_FIELD_NUMBER, namesIndex.keySet());
		for(Entry<String, List<MapObject>> entry : namesIndex.entrySet()) {
			BinaryFileReference ref = res.get(entry.getKey());
			
			codedOutStream.writeTag(OsmAndAddressNameIndexData.ATOM_FIELD_NUMBER, FieldType.MESSAGE.getWireType());
			long pointer = getFilePointer();
			if(ref != null) {
				ref.writeReference(raf, getFilePointer());
			}
			AddressNameIndexData.Builder builder = AddressNameIndexData.newBuilder();
			// collapse same name ?
			for(MapObject o : entry.getValue()){
				AddressNameIndexDataAtom.Builder atom = AddressNameIndexDataAtom.newBuilder();
				atom.setName(o.getName());
				if(checkEnNameToWrite(o)){
					atom.setNameEn(o.getEnName());
				}
				int type = 1;
				if (o instanceof City) {
					CityType ct = ((City) o).getType();
					if (ct != CityType.CITY && ct != CityType.TOWN) {
						type = 3;
					}
				} else if(o instanceof PostCode) {
					type = 2;
				} else if(o instanceof Street) {
					type = 4;
				}
				atom.setType(type); 
				atom.addShiftToIndex((int) (pointer - o.getFileOffset()));
				if(o instanceof Street){
					atom.addShiftToCityIndex((int) (pointer - ((Street) o).getCity().getFileOffset()));
				}
				builder.addAtom(atom.build());
			}
			codedOutStream.writeMessageNoTag(builder.build());
		}
		
		int len = writeInt32Size();
		log.info("ADDRESS NAME INDEX SIZE : " + len);
	}

	private boolean checkEnNameToWrite(MapObject obj) {
		if (obj.getEnName() == null || obj.getEnName().length() == 0) {
			return false;
		}
		return !obj.getEnName().equals(Junidecode.unidecode(obj.getName()));
	}

	public BinaryFileReference writeCityHeader(MapObject city, int cityType) throws IOException {
		checkPeekState(CITY_INDEX_INIT);
		codedOutStream.writeTag(CitiesIndex.CITIES_FIELD_NUMBER, FieldType.MESSAGE.getWireType());
		long startMessage = getFilePointer();
		
		
		CityIndex.Builder cityInd = OsmandOdb.CityIndex.newBuilder();
		if(cityType >= 0) {
			cityInd.setCityType(cityType);
		}
		cityInd.setId(city.getId());
		
		cityInd.setName(city.getName());
		if(checkEnNameToWrite(city)){
			cityInd.setNameEn(city.getEnName());
		}
		int cx = MapUtils.get31TileNumberX(city.getLocation().getLongitude());
		int cy = MapUtils.get31TileNumberY(city.getLocation().getLatitude());
		cityInd.setX(cx);
		cityInd.setY(cy);
		cityInd.setShiftToCityBlockIndex(0);
		codedOutStream.writeMessageNoTag(cityInd.build());
		return new BinaryFileReference(getFilePointer() - 4, startMessage);
		
	}
	
	public void writeCityIndex(MapObject cityOrPostcode, List<Street> streets, Map<Street, List<Node>> wayNodes, 
			BinaryFileReference ref) throws IOException {
		checkPeekState(CITY_INDEX_INIT);
		codedOutStream.writeTag(CitiesIndex.BLOCKS_FIELD_NUMBER, FieldType.MESSAGE.getWireType());
		long startMessage = getFilePointer();
		long startCityBlock = ref.getStartPointer();
		ref.writeReference(raf, startMessage);
		CityBlockIndex.Builder cityInd = OsmandOdb.CityBlockIndex.newBuilder();
		cityInd.setShiftToCityIndex((int) (startMessage - startCityBlock));
		long currentPointer = startMessage + 4;
		
		int cx = MapUtils.get31TileNumberX(cityOrPostcode.getLocation().getLongitude());
		int cy = MapUtils.get31TileNumberY(cityOrPostcode.getLocation().getLatitude());
		Map<Long, Set<Street>> mapNodeToStreet = new LinkedHashMap<Long, Set<Street>>();
		if (wayNodes != null) {
			for (int i = 0; i < streets.size(); i++) {
				streets.get(i).setIndexInCity(i);
				for (Node n : wayNodes.get(streets.get(i))) {
					if (!mapNodeToStreet.containsKey(n.getId())) {
						mapNodeToStreet.put(n.getId(), new LinkedHashSet<Street>(3));
					}
					mapNodeToStreet.get(n.getId()).add(streets.get(i));
				}
			}
		}
		String postcodeFilter = cityOrPostcode instanceof PostCode ? cityOrPostcode.getName() : null;
		for (Street s : streets) {
			StreetIndex streetInd = createStreetAndBuildings(s, cx, cy, postcodeFilter, mapNodeToStreet, wayNodes);
			currentPointer += CodedOutputStream.computeTagSize(CityBlockIndex.STREETS_FIELD_NUMBER);
			if(currentPointer > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("File offset > 2 GB.");
			}
			s.setFileOffset((int) currentPointer);
			currentPointer += CodedOutputStream.computeMessageSizeNoTag(streetInd);
			cityInd.addStreets(streetInd);
			
		}
		codedOutStream.writeMessageNoTag(cityInd.build());
	}

	public void startCityBlockIndex(int type) throws IOException {
		pushState(CITY_INDEX_INIT, ADDRESS_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndAddressIndex.CITIES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		codedOutStream.writeUInt32(CitiesIndex.TYPE_FIELD_NUMBER, type);
	}

	public void endCityBlockIndex() throws IOException {
		popState(CITY_INDEX_INIT);
		int length = writeInt32Size();
		log.info("CITIES size " + length);
	}

	protected StreetIndex createStreetAndBuildings(Street street, int cx, int cy, String postcodeFilter, 
			Map<Long, Set<Street>> mapNodeToStreet, Map<Street, List<Node>> wayNodes) throws IOException {
		checkPeekState(CITY_INDEX_INIT);
		StreetIndex.Builder streetBuilder = OsmandOdb.StreetIndex.newBuilder();
		streetBuilder.setName(street.getName());
		if (checkEnNameToWrite(street)) {
			streetBuilder.setNameEn(street.getEnName());
		}
		streetBuilder.setId(street.getId());

		int sx = MapUtils.get31TileNumberX(street.getLocation().getLongitude());
		int sy = MapUtils.get31TileNumberY(street.getLocation().getLatitude());
		streetBuilder.setX((sx - cx) >> 7);
		streetBuilder.setY((sy - cy) >> 7);

		for (Building b : street.getBuildings()) {
			if (postcodeFilter != null && !postcodeFilter.equalsIgnoreCase(b.getPostcode())) {
				continue;
			}
			OsmandOdb.BuildingIndex.Builder bbuilder = OsmandOdb.BuildingIndex.newBuilder();
			int bx = MapUtils.get31TileNumberX(b.getLocation().getLongitude());
			int by = MapUtils.get31TileNumberY(b.getLocation().getLatitude());
			bbuilder.setX((bx - sx) >> 7);
			bbuilder.setY((by - sy) >> 7);
			TODO;
			bbuilder.setId(b.getId());
			bbuilder.setName(b.getName());
			if (checkEnNameToWrite(b)) {
				bbuilder.setNameEn(b.getEnName());
			}
			if (postcodeFilter == null && b.getPostcode() != null) {
				bbuilder.setPostcode(b.getPostcode());
			}
			streetBuilder.addBuildings(bbuilder.build());
		}

		if(wayNodes != null) {
			Set<Street> checkedStreets = new TreeSet<Street>();
			for (Node intersection : wayNodes.get(street)) {
				for (Street streetJ : mapNodeToStreet.get(intersection.getId())) {
					if (checkedStreets.contains(streetJ)) {
						continue;
					}
					checkedStreets.add(streetJ);
					StreetIntersection.Builder builder = OsmandOdb.StreetIntersection.newBuilder();
					int ix = MapUtils.get31TileNumberX(intersection.getLongitude());
					int iy = MapUtils.get31TileNumberY(intersection.getLatitude());
					builder.setIntersectedX((ix - sx) >> 7);
					builder.setIntersectedY((iy - sy) >> 7);
					builder.setName(streetJ.getName());
					if(checkEnNameToWrite(streetJ)){
						builder.setNameEn(streetJ.getEnName());
					}
					streetBuilder.addIntersections(builder.build());
				}
			}
		}

		return streetBuilder.build();
	}

	public void startWriteTransportRoutes() throws IOException {
		pushState(TRANSPORT_ROUTES, TRANSPORT_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndTransportIndex.ROUTES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
	}

	public void endWriteTransportRoutes() throws IOException {
		popState(TRANSPORT_ROUTES);
		writeInt32Size();
	}

	private int registerString(Map<String, Integer> stringTable, String s) {
		if (s == null) {
			s = "";
		}
		if (stringTable.containsKey(s)) {
			return stringTable.get(s);
		}
		int size = stringTable.size();
		stringTable.put(s, size);
		return size;
	}

	public void startWriteTransportIndex(String name) throws IOException {
		pushState(TRANSPORT_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		stackBounds.push(new Bounds(0, 0, 0, 0)); // for transport stops tree
		preserveInt32Size();
		if (name != null) {
			codedOutStream.writeString(OsmandOdb.OsmAndTransportIndex.NAME_FIELD_NUMBER, name);
		}
	}

	public void endWriteTransportIndex() throws IOException {
		popState(TRANSPORT_INDEX_INIT);
		int len = writeInt32Size();
		stackBounds.pop();
		log.info("TRANSPORT INDEX SIZE : " + len);
	}

	public void writeTransportRoute(long idRoute, String routeName, String routeEnName, String ref, String operator, String type, int dist,
			List<TransportStop> directStops, List<TransportStop> reverseStops, Map<String, Integer> stringTable,
			Map<Long, Long> transportRoutesRegistry) throws IOException {
		checkPeekState(TRANSPORT_ROUTES);
		TransportRoute.Builder tRoute = OsmandOdb.TransportRoute.newBuilder();
		tRoute.setRef(ref);
		tRoute.setOperator(registerString(stringTable, operator));
		tRoute.setType(registerString(stringTable, type));
		tRoute.setId(idRoute);
		tRoute.setName(registerString(stringTable, routeName));
		tRoute.setDistance(dist);

		if (routeEnName != null) {
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
		codedOutStream.writeTag(OsmandOdb.TransportRoutes.ROUTES_FIELD_NUMBER, FieldType.MESSAGE.getWireType());
		if (transportRoutesRegistry != null) {
			transportRoutesRegistry.put(idRoute, getFilePointer());
		}
		codedOutStream.writeMessageNoTag(tRoute.build());
	}

	public void startTransportTreeElement(int leftX, int rightX, int topY, int bottomY) throws IOException {
		checkPeekState(TRANSPORT_STOPS_TREE, TRANSPORT_INDEX_INIT);
		if (state.peek() == TRANSPORT_STOPS_TREE) {
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
		if (baseId >= 0) {
			codedOutStream.writeUInt64(OsmandOdb.TransportStopsTree.BASEID_FIELD_NUMBER, baseId);
		}
		popState(TRANSPORT_STOPS_TREE);
		stackBounds.pop();
		writeInt32Size();
	}

	public void writeTransportStop(long id, int x24, int y24, String name, String nameEn, Map<String, Integer> stringTable,
			List<Long> routes) throws IOException {
		checkPeekState(TRANSPORT_STOPS_TREE);

		Bounds bounds = stackBounds.peek();
		if (stackBaseIds.peek() == -1) {
			stackBaseIds.pop();
			stackBaseIds.push(id);
		}
		codedOutStream.writeTag(OsmandOdb.TransportStopsTree.LEAFS_FIELD_NUMBER, WireFormat.FieldType.MESSAGE.getWireType());
		long fp = getFilePointer();

		OsmandOdb.TransportStop.Builder ts = OsmandOdb.TransportStop.newBuilder();
		ts.setName(registerString(stringTable, name));
		if (nameEn != null) {
			ts.setNameEn(registerString(stringTable, nameEn));
		}
		ts.setDx(x24 - bounds.leftX);
		ts.setDy(y24 - bounds.topY);
		ts.setId(id - stackBaseIds.peek());
		for (Long i : routes) {
			ts.addRoutes((int) (fp - i));
		}

		codedOutStream.writeMessageNoTag(ts.build());
	}

	public void writeTransportStringTable(Map<String, Integer> stringTable) throws IOException {
		checkPeekState(TRANSPORT_INDEX_INIT);
		// expect linked hash map
		int i = 0;
		OsmandOdb.StringTable.Builder st = OsmandOdb.StringTable.newBuilder();
		for (String s : stringTable.keySet()) {
			if (stringTable.get(s) != i++) {
				throw new IllegalStateException();
			}
			st.addS(s);
		}
		codedOutStream.writeMessage(OsmAndTransportIndex.STRINGTABLE_FIELD_NUMBER, st.build());
	}

	public BinaryFileReference startWritePOIIndex(String name, int left31, int right31, int bottom31, int top31) throws IOException {
		pushState(POI_INDEX_INIT, OSMAND_STRUCTURE_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndStructure.POIINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		stackBounds.push(new Bounds(0, 0, 0, 0)); // for poi index tree
		BinaryFileReference startPoiIndex = preserveInt32Size();
		if (name != null) {
			codedOutStream.writeString(OsmandOdb.OsmAndPoiIndex.NAME_FIELD_NUMBER, name);
		}
		OsmandOdb.OsmAndTileBox.Builder builder = OsmandOdb.OsmAndTileBox.newBuilder();
		builder.setLeft(left31);
		builder.setRight(right31);
		builder.setTop(top31);
		builder.setBottom(bottom31);
		codedOutStream.writeMessage(OsmandOdb.OsmAndPoiIndex.BOUNDARIES_FIELD_NUMBER, builder.build());
		return startPoiIndex;
	}

	public void endWritePOIIndex() throws IOException {
		popState(POI_INDEX_INIT);
		int len = writeInt32Size();
		stackBounds.pop();
		log.info("POI INDEX SIZE : " + len);
	}

	public Map<String, Integer> writePOICategoriesTable(Map<String, Map<String, Integer>> categories) throws IOException {
		checkPeekState(POI_INDEX_INIT);
		Map<String, Integer> catIndexes = new LinkedHashMap<String, Integer>();
		int i = 0;
		for (String cat : categories.keySet()) {
			Builder builder = OsmandOdb.OsmAndCategoryTable.newBuilder();
			builder.setCategory(cat);
			Map<String, Integer> subcatSource = categories.get(cat);
			Map<String, Integer> subcats = new LinkedHashMap<String, Integer>(subcatSource);
			int j = 0;
			for (String s : subcats.keySet()) {
				builder.addSubcategories(s);
				subcatSource.put(s, j);
				j++;
			}
			catIndexes.put(cat, i);
			codedOutStream.writeMessage(OsmandOdb.OsmAndPoiIndex.CATEGORIESTABLE_FIELD_NUMBER, builder.build());
			i++;
		}

		return catIndexes;
	}

	public void writePOICategories(TIntArrayList categories) throws IOException {
		checkPeekState(POI_BOX);
		OsmandOdb.OsmAndPoiCategories.Builder builder = OsmandOdb.OsmAndPoiCategories.newBuilder();
		int prev = -1;
		categories.sort();
		for (int i = 0; i < categories.size(); i++) {
			// avoid duplicates
			if (i == 0 || prev != categories.get(i)) {
				builder.addCategories(categories.get(i));
				prev = categories.get(i);
			}
		}
		codedOutStream.writeMessage(OsmandOdb.OsmAndPoiBox.CATEGORIES_FIELD_NUMBER, builder.build());
	}

	public Map<PoiTileBox, List<BinaryFileReference>> writePoiNameIndex(Map<String, Set<PoiTileBox>> namesIndex, BinaryFileReference startPoiIndex) throws IOException {
		checkPeekState(POI_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndPoiIndex.NAMEINDEX_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		
		Map<PoiTileBox, List<BinaryFileReference>> fpToWriteSeeks = new LinkedHashMap<PoiTileBox, List<BinaryFileReference>>();
		Map<String, BinaryFileReference> indexedTable = writeIndexedTable(OsmandOdb.OsmAndPoiNameIndex.TABLE_FIELD_NUMBER, namesIndex.keySet());
		for(Map.Entry<String, Set<PoiTileBox>> e : namesIndex.entrySet()) {
			codedOutStream.writeTag(OsmandOdb.OsmAndPoiNameIndex.DATA_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
			BinaryFileReference nameTableRef = indexedTable.get(e.getKey());
			nameTableRef.writeReference(raf, getFilePointer());
			
			OsmAndPoiNameIndex.OsmAndPoiNameIndexData.Builder builder = OsmAndPoiNameIndex.OsmAndPoiNameIndexData.newBuilder();
			List<PoiTileBox> tileBoxes = new ArrayList<PoiTileBox>(e.getValue());
			for(PoiTileBox box : tileBoxes) {
				OsmandOdb.OsmAndPoiNameIndexDataAtom.Builder bs = OsmandOdb.OsmAndPoiNameIndexDataAtom.newBuilder();
				bs.setX(box.getX());
				bs.setY(box.getY());
				bs.setZoom(box.getZoom());
				bs.setShiftTo(0);
				OsmAndPoiNameIndexDataAtom atom = bs.build();
				builder.addAtoms(atom);
			}
			OsmAndPoiNameIndex.OsmAndPoiNameIndexData msg = builder.build();
			
			codedOutStream.writeMessage(OsmandOdb.OsmAndPoiNameIndex.DATA_FIELD_NUMBER, msg);
			long endPointer = getFilePointer();
			
			// first message
			int accumulateSize = 4;
			for (int i = tileBoxes.size() - 1; i >= 0; i--) {
				PoiTileBox box = tileBoxes.get(i);
				if (!fpToWriteSeeks.containsKey(box)) {
					fpToWriteSeeks.put(box, new ArrayList<BinaryFileReference>());
				}
				fpToWriteSeeks.get(box).add(net.osmand.data.preparation.BinaryFileReference.createShiftReference(endPointer - accumulateSize, startPoiIndex.getStartPointer()));
				accumulateSize += CodedOutputStream.computeMessageSize(OsmAndPoiNameIndex.OsmAndPoiNameIndexData.ATOMS_FIELD_NUMBER,
						msg.getAtoms(i));

			}
		}
		
		writeInt32Size();
		
		
		return fpToWriteSeeks;
	}

	private Map<String, BinaryFileReference> writeIndexedTable(int tag, Collection<String> indexedTable) throws IOException {
		codedOutStream.writeTag(tag, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		preserveInt32Size();
		Map<String, BinaryFileReference> res = new LinkedHashMap<String, BinaryFileReference>();
		long init = getFilePointer();
		for (String e : indexedTable) {
			codedOutStream.writeString(OsmandOdb.IndexedStringTable.KEY_FIELD_NUMBER, e);
			codedOutStream.writeTag(OsmandOdb.IndexedStringTable.VAL_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
			BinaryFileReference ref = BinaryFileReference.createShiftReference(getFilePointer(), init);
			codedOutStream.writeFixed32NoTag(0);
			res.put(e, ref);
		}
		writeInt32Size();
		return res;
	}

	public void writePoiDataAtom(long id, int x24shift, int y24shift, String nameEn, String name, TIntArrayList types, String openingHours,
			String site, String phone, String description) throws IOException {
		checkPeekState(POI_DATA);

		OsmAndPoiBoxDataAtom.Builder builder = OsmandOdb.OsmAndPoiBoxDataAtom.newBuilder();
		builder.setDx(x24shift);
		builder.setDy(y24shift);
		for (int i = 0; i < types.size(); i++) {
			int j = types.get(i);
			builder.addCategories(j);
		}
		if (!Algoritms.isEmpty(name)) {
			builder.setName(name);
		}
		if (!Algoritms.isEmpty(nameEn)) {
			builder.setNameEn(nameEn);
		}
		builder.setId(id);

		if (!Algoritms.isEmpty(openingHours)) {
			builder.setOpeningHours(openingHours);
		}
		if (!Algoritms.isEmpty(site)) {
			builder.setSite(site);
		}
		if (!Algoritms.isEmpty(phone)) {
			builder.setPhone(phone);
		}
		if (!Algoritms.isEmpty(description)) {
			builder.setNote(description);
		}

		codedOutStream.writeMessage(OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER, builder.build());

	}

	public void startWritePoiData(int zoom, int x, int y, List<BinaryFileReference> fpPoiBox) throws IOException {
		pushState(POI_DATA, POI_INDEX_INIT);
		codedOutStream.writeTag(OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		BinaryFileReference startPoiData = preserveInt32Size();
		codedOutStream.flush();
		// write shift to that data
		for (int i = 0; i < fpPoiBox.size(); i++) {
			fpPoiBox.get(i).writeReference(raf, startPoiData.getStartPointer());
		}

		codedOutStream.writeUInt32(OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER, zoom);
		codedOutStream.writeUInt32(OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER, x);
		codedOutStream.writeUInt32(OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER, y);

	}

	public void endWritePoiData() throws IOException {
		popState(POI_DATA);
		writeInt32Size();
	}

	public BinaryFileReference startWritePoiBox(int zoom, int tileX, int tileY, BinaryFileReference startPoiIndex, boolean end)
			throws IOException {
		checkPeekState(POI_INDEX_INIT, POI_BOX);
		if (state.peek() == POI_INDEX_INIT) {
			codedOutStream.writeTag(OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		} else {
			codedOutStream.writeTag(OsmandOdb.OsmAndPoiBox.SUBBOXES_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
		}
		state.push(POI_BOX);
		preserveInt32Size();

		Bounds bounds = stackBounds.peek();
		int parentZoom = bounds.rightX;
		int parentTileX = bounds.leftX;
		int parentTileY = bounds.topY;

		int pTileX = parentTileX << (zoom - parentZoom);
		int pTileY = parentTileY << (zoom - parentZoom);
		codedOutStream.writeUInt32(OsmandOdb.OsmAndPoiBox.ZOOM_FIELD_NUMBER, (zoom - parentZoom));
		codedOutStream.writeSInt32(OsmandOdb.OsmAndPoiBox.LEFT_FIELD_NUMBER, tileX - pTileX);
		codedOutStream.writeSInt32(OsmandOdb.OsmAndPoiBox.TOP_FIELD_NUMBER, tileY - pTileY);
		stackBounds.push(new Bounds(tileX, zoom, tileY, 0));

		if (end) {
			codedOutStream.writeTag(OsmandOdb.OsmAndPoiBox.SHIFTTODATA_FIELD_NUMBER, WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED);
			BinaryFileReference shift = BinaryFileReference.createShiftReference(getFilePointer(), startPoiIndex.getStartPointer());
			codedOutStream.writeFixed32NoTag(0);
			return shift;
		}
		return null;
	}

	public void endWritePoiBox() throws IOException {
		popState(POI_BOX);
		writeInt32Size();
		stackBounds.pop();
	}

	private void pushState(int push, int peek) {
		if (state.peek() != peek) {
			throw new IllegalStateException("expected " + peek + " != " + state.peek());
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

	private void popState(int state) {
		Integer st = this.state.pop();
		if (st != state) {
			throw new IllegalStateException("expected " + state + " != " + st);
		}
	}

	public void flush() throws IOException {
		codedOutStream.flush();
	}

	public void close() throws IOException {
		checkPeekState(OSMAND_STRUCTURE_INIT);
		codedOutStream.writeInt32(OsmandOdb.OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER, IndexConstants.BINARY_MAP_VERSION);
		codedOutStream.flush();
	}

}
