package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.Street;
import net.osmand.data.City.CityType;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapIndexReader {
	
	private final RandomAccessFile raf;
	private int version;
	private List<MapRoot> mapIndexes = new ArrayList<MapRoot>();
	private List<AddressRegion> addressIndexes = new ArrayList<AddressRegion>();
	private CodedInputStreamRAF codedIS;
	
	private final static Log log = LogUtil.getLog(BinaryMapIndexReader.class);
	

	
	
	public BinaryMapIndexReader(final RandomAccessFile raf) throws IOException {
		this.raf = raf;
		codedIS = CodedInputStreamRAF.newInstance(raf, 1024);
		init();
	}

	private void init() throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
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
			case OsmandOdb.OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER:
				length = readInt();
				filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimit(length);
				readAddressIndex();
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void skipUnknownField(int tag) throws IOException{
		int wireType = WireFormat.getTagWireType(tag);
		if(wireType == WireFormat.WIRETYPE_FIXED32_LENGTH_DELIMITED){
			int length = readInt();
			codedIS.skipRawBytes(length);
		} else {
			codedIS.skipField(tag);
		}
	}
	
	private void readAddressIndex() throws IOException {
		AddressRegion region = new AddressRegion();
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(region.name != null){
					addressIndexes.add(region);
				}
				return;
			case OsmandOdb.OsmAndAddressIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndAddressIndex.NAME_EN_FIELD_NUMBER :
				region.enName = codedIS.readString();
				break;
			case OsmandOdb.OsmAndAddressIndex.CITIES_FIELD_NUMBER :
				region.citiesOffset = codedIS.getTotalBytesRead();
				int length = readInt();
				codedIS.seek(region.citiesOffset + length + 4);
				break;
			case OsmandOdb.OsmAndAddressIndex.VILLAGES_FIELD_NUMBER :
				region.villagesOffset = codedIS.getTotalBytesRead();
				length = readInt();
				codedIS.seek(region.villagesOffset + length + 4);
				break;
			case OsmandOdb.OsmAndAddressIndex.POSTCODES_FIELD_NUMBER :
				region.postcodesOffset = codedIS.getTotalBytesRead();
				length = readInt();
				codedIS.seek(region.postcodesOffset + length + 4);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readMapIndex() throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
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
				skipUnknownField(t);
				break;
			}
		}
	}

	private MapRoot readMapLevel() throws IOException {
		MapRoot root = new MapRoot();
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
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
				skipUnknownField(t);
				break;
			}
		}
		
	}
	
	private void readMapTreeBounds(MapTree tree, int aleft, int aright, int atop, int abottom) throws IOException {
		int init = 0;
		while(true){
			if(init == 0xf){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER :
				tree.bottom = codedIS.readSInt32() + abottom;
				init |= 1;
				break;
			case OsmandOdb.MapTree.LEFT_FIELD_NUMBER :
				tree.left = codedIS.readSInt32() + aleft;
				init |= 2;
				break;
			case OsmandOdb.MapTree.RIGHT_FIELD_NUMBER :
				tree.right = codedIS.readSInt32() + aright;
				init |= 4;
				break;
			case OsmandOdb.MapTree.TOP_FIELD_NUMBER :
				tree.top = codedIS.readSInt32() + atop;
				init |= 8;
				break;
		
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
	
	public List<BinaryMapDataObject> searchMapIndex(SearchRequest req) throws IOException {
		for (MapRoot index : mapIndexes) {
			if (index.minZoom <= req.zoom && index.maxZoom >= req.zoom) {
				if(index.right < req.left || index.left > req.right || index.top > req.bottom || index.bottom < req.top){
					continue;
				}
				for (MapTree tree : index.trees) {
					if(tree.right < req.left || tree.left > req.right || tree.top > req.bottom || tree.bottom < req.top){
						continue;
					}
					codedIS.seek(tree.filePointer);
					int oldLimit = codedIS.pushLimit(tree.length);
					searchMapTreeBounds(index.left, index.right, index.top, index.bottom, req);
					codedIS.popLimit(oldLimit);
				}
			}
		}
		log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		return req.getSearchResults();
	}
	
	
	public List<String> getRegionNames(){
		List<String> names = new ArrayList<String>();
		for(AddressRegion r : addressIndexes){
			names.add(r.name);
		}
		return names;
	}
	
	private AddressRegion getRegionByName(String name){
		for(AddressRegion r : addressIndexes){
			if(r.name.equals(name)){
				return r;
			}
		}
		throw new IllegalArgumentException(name);
	}
	
	public List<City> getCities(String region) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.citiesOffset != -1){
			codedIS.seek(r.citiesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			readCities(cities);
			codedIS.popLimit(old);
		}
		return cities;
	}
	
	public List<City> getVillages(String region) throws IOException {
		List<City> cities = new ArrayList<City>();
		AddressRegion r = getRegionByName(region);
		if(r.villagesOffset != -1){
			codedIS.seek(r.villagesOffset);
			int len = readInt();
			int old = codedIS.pushLimit(len);
			readCities(cities);
			codedIS.popLimit(old);
		}
		return cities;
	}
	
	private void readCities(List<City> cities) throws IOException{
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.CitiesIndex.CITIES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				
				int oldLimit = codedIS.pushLimit(length);
				cities.add(readCity(null, offset, false));
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	public void preloadStreets(City c) throws IOException {
	    // TODO check city belongs to that address repository
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		readCity(c, c.getFileOffset(), true);
		codedIS.popLimit(old);
	}
	
	public void preloadBuildings(Street s) throws IOException {
	    // TODO check street belongs to that address repository
		codedIS.seek(s.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		readStreet(s, true, 0, 0);
		codedIS.popLimit(old);
	}
	
	
	
	private City readCity(City c, int fileOffset, boolean loadStreets) throws IOException{
		int x = 0;
		int y = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				c.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				return c;
			case OsmandOdb.CityIndex.CITY_TYPE_FIELD_NUMBER :
				int type = codedIS.readUInt32();
				if(c == null){
					c = new City(CityType.values()[type]);
					c.setFileOffset(fileOffset);
				}
				break;
			case OsmandOdb.CityIndex.ID_FIELD_NUMBER :
				c.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.CityIndex.NAME_EN_FIELD_NUMBER :
				c.setEnName(codedIS.readString());
				break;
			case OsmandOdb.CityIndex.NAME_FIELD_NUMBER :
				c.setName(codedIS.readString());
				break;
			case OsmandOdb.CityIndex.X_FIELD_NUMBER :
				x = codedIS.readFixed32();
				break;
			case OsmandOdb.CityIndex.Y_FIELD_NUMBER :
				y = codedIS.readFixed32();
				break;
			case OsmandOdb.CityIndex.STREETS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadStreets){
					Street s = new Street(c);
					int oldLimit = codedIS.pushLimit(length);
					s.setFileOffset(offset);
					readStreet(s, false, x >> 7, y >> 7);
					c.registerStreet(s);
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private Street readStreet(Street s, boolean loadBuildings, int city24X, int city24Y) throws IOException{
		int x = 0;
		int y = 0;
		boolean loadLocation = city24X != 0 || city24Y != 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(loadLocation){
					s.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				}
				return s;
			case OsmandOdb.StreetIndex.ID_FIELD_NUMBER :
				s.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.StreetIndex.NAME_EN_FIELD_NUMBER :
				s.setEnName(codedIS.readString());
				break;
			case OsmandOdb.StreetIndex.NAME_FIELD_NUMBER :
				s.setName(codedIS.readString());
				break;
			case OsmandOdb.StreetIndex.X_FIELD_NUMBER :
				int sx = codedIS.readSInt32();
				if(loadLocation){
					x =  sx + city24X;
				} else {
					x = (int) MapUtils.getTileNumberX(24, s.getLocation().getLongitude());
				}
				break;
			case OsmandOdb.StreetIndex.Y_FIELD_NUMBER :
				int sy = codedIS.readSInt32();
				if(loadLocation){
					y =  sy + city24Y;
				} else {
					y = (int) MapUtils.getTileNumberY(24, s.getLocation().getLatitude());
				}
				break;
			case OsmandOdb.StreetIndex.BUILDINGS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadBuildings){
					int oldLimit = codedIS.pushLimit(length);
					Building b = readBuilding(offset, x, y);
					s.registerBuilding(b);
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private Building readBuilding(int fileOffset, int street24X, int street24Y) throws IOException{
		int x = 0;
		int y = 0;
		Building b = new Building();
		b.setFileOffset(fileOffset);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				b.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				return b;
			case OsmandOdb.BuildingIndex.ID_FIELD_NUMBER :
				b.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.BuildingIndex.NAME_EN_FIELD_NUMBER :
				b.setEnName(codedIS.readString());
				break;
			case OsmandOdb.BuildingIndex.NAME_FIELD_NUMBER :
				b.setName(codedIS.readString());
				break;
			case OsmandOdb.BuildingIndex.X_FIELD_NUMBER :
				x =  codedIS.readSInt32() + street24X;
				break;
			case OsmandOdb.BuildingIndex.Y_FIELD_NUMBER :
				y =  codedIS.readSInt32() + street24Y;
				break;
			case OsmandOdb.BuildingIndex.POSTCODE_FIELD_NUMBER :
				b.setPostcode(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void searchMapTreeBounds(int pleft, int pright, int ptop, int pbottom,
			SearchRequest req) throws IOException {
		int init = 0;
		int lastIndexResult = -1;
		int cright = 0;
		int cleft = 0;
		int ctop = 0;
		int cbottom = 0;
		req.numberOfReadSubtrees++;
		while(true){
			if(req.isInterrupted()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			if(init == 0xf){
				init = 0;
				// coordinates are init
				if(cright < req.left || cleft > req.right || ctop > req.bottom || cbottom < req.top){
					return;
				} else {
					req.numberOfAcceptedSubtrees++;
				}
			}
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.MapTree.BOTTOM_FIELD_NUMBER :
				cbottom = codedIS.readSInt32() + pbottom;
				init |= 1;
				break;
			case OsmandOdb.MapTree.LEFT_FIELD_NUMBER :
				cleft = codedIS.readSInt32() + pleft;
				init |= 2;
				break;
			case OsmandOdb.MapTree.RIGHT_FIELD_NUMBER :
				cright = codedIS.readSInt32() + pright;
				init |= 4;
				break;
			case OsmandOdb.MapTree.TOP_FIELD_NUMBER :
				ctop = codedIS.readSInt32() + ptop;
				init |= 8;
				break;
			case OsmandOdb.MapTree.LEAFS_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				if(lastIndexResult == -1){
					lastIndexResult = req.searchResults.size();
				}
				BinaryMapDataObject mapObject = readMapDataObject(cleft, cright, ctop, cbottom, req);
				if(mapObject != null){
					req.searchResults.add(mapObject);
					
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.MapTree.SUBTREES_FIELD_NUMBER :
				// left, ... already initialized 
				length = readInt();
				int filePointer = codedIS.getTotalBytesRead();
				oldLimit = codedIS.pushLimit(length);
				searchMapTreeBounds(cleft, cright, ctop, cbottom, req);
				codedIS.popLimit(oldLimit);
				codedIS.seek(filePointer + length);
				if(lastIndexResult >= 0){
					throw new IllegalStateException();
				}
				break;
			case OsmandOdb.MapTree.BASEID_FIELD_NUMBER :
				long baseId = codedIS.readUInt64();

				if (lastIndexResult != -1) {
					for (int i = lastIndexResult; i < req.searchResults.size(); i++) {
						BinaryMapDataObject rs = req.searchResults.get(i);
						rs.id += baseId;
						if (rs.restrictions != null) {
							for (int j = 0; j < rs.restrictions.length; j++) {
								rs.restrictions[j] += baseId;
							}
						}
					}
				}
				break;
			case OsmandOdb.MapTree.STRINGTABLE_FIELD_NUMBER :
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				List<String> stringTable = readStringTable();
				codedIS.popLimit(oldLimit);

				if (lastIndexResult != -1) {
					for (int i = lastIndexResult; i < req.searchResults.size(); i++) {
						BinaryMapDataObject rs = req.searchResults.get(i);
						if (rs.stringId != -1) {
							rs.name = stringTable.get(rs.stringId);
						}
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private int MASK_TO_READ = ~((1 << BinaryMapIndexWriter.SHIFT_COORDINATES) - 1);
	private BinaryMapDataObject readMapDataObject(int left, int right, int top, int bottom, SearchRequest req) throws IOException {
		int tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.MapData.COORDINATES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheCoordinates.clear();
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		int px = left & MASK_TO_READ;
		int py = top & MASK_TO_READ;
		boolean contains = false;
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;
		req.numberOfVisitedObjects++;
		while(codedIS.getBytesUntilLimit() > 0){
			int x = (codedIS.readSInt32() << BinaryMapIndexWriter.SHIFT_COORDINATES) + px;
			int y = (codedIS.readSInt32() << BinaryMapIndexWriter.SHIFT_COORDINATES) + py;
			req.cacheCoordinates.add(x);
			req.cacheCoordinates.add(y);
			px = x;
			py = y;
			if(!contains && req.left <= x && req.right >= x && req.top <= y && req.bottom >= y){
				contains = true;
			}
			if(!contains){
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
			}
		}
		if(!contains){
			if(maxX >= req.left && minX <= req.right && minY <= req.bottom && maxY >= req.top){
				contains = true;
			}
			
		}
		codedIS.popLimit(old);
		if(!contains){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		// READ types
		tag = WireFormat.getTagFieldNumber(codedIS.readTag());
		if(OsmandOdb.MapData.TYPES_FIELD_NUMBER != tag) {
			throw new IllegalArgumentException();
		}
		req.cacheTypes.clear();
		int sizeL = codedIS.readRawVarint32();
		byte[] types = codedIS.readRawBytes(sizeL);
		for(int i=0; i<sizeL/2; i++){
			req.cacheTypes.add(Algoritms.parseSmallIntFromBytes(types, i*2));
		}
		
		boolean accept = true;
		if (req.searchFilter != null) {
			accept = req.searchFilter.accept(req.cacheTypes);
		}
		
		
		if(!accept){
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
			return null;
		}
		
		req.numberOfAcceptedObjects++;
		
		BinaryMapDataObject dataObject = new BinaryMapDataObject();		
		dataObject.coordinates = req.cacheCoordinates.toArray();
		dataObject.types = req.cacheTypes.toArray();
		
		while(true){
			int t = codedIS.readTag();
			tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return dataObject;
			case OsmandOdb.MapData.RESTRICTIONS_FIELD_NUMBER :
				sizeL = codedIS.readRawVarint32();
				TLongArrayList list = new TLongArrayList();
				old = codedIS.pushLimit(sizeL);
				while(codedIS.getBytesUntilLimit() > 0){
					list.add(codedIS.readSInt64());
				}
				codedIS.popLimit(old);
				dataObject.restrictions = list.toArray();
				break;
			case OsmandOdb.MapData.HIGHWAYMETA_FIELD_NUMBER :
				dataObject.highwayAttributes = codedIS.readInt32();
				break;
			case OsmandOdb.MapData.ID_FIELD_NUMBER :
				dataObject.id = codedIS.readSInt64();
				break;
			case OsmandOdb.MapData.STRINGID_FIELD_NUMBER :
				dataObject.stringId = codedIS.readUInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}

	private List<String> readStringTable() throws IOException{
		List<String> list = new ArrayList<String>();
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return list;
			case OsmandOdb.StringTable.S_FIELD_NUMBER :
				list.add(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
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
	
	
	public int getVersion() {
		return version;
	}
	

	protected List<AddressRegion> getAddressIndexes() {
		return addressIndexes;
	}

	
	public static SearchRequest buildSearchRequest(int sleft, int sright, int stop, int sbottom, int zoom){
		SearchRequest request = new SearchRequest();
		request.left = sleft;
		request.right = sright;
		request.top = stop;
		request.bottom = sbottom;
		request.zoom = zoom;
		return request;
	}
	
	public void close() throws IOException{
		if(codedIS != null){
			raf.close();
			codedIS = null;
			mapIndexes.clear();
		}
	}
	
	public static interface SearchFilter {
		
		public boolean accept(TIntArrayList types);
	}
	
	public static class SearchRequest {
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		int zoom = 15;
		List<BinaryMapDataObject> searchResults = new ArrayList<BinaryMapDataObject>();
		TIntArrayList cacheCoordinates = new TIntArrayList();
		TIntArrayList cacheTypes = new TIntArrayList();
		SearchFilter searchFilter = null;
		
		// TRACE INFO
		int numberOfVisitedObjects = 0;
		int numberOfAcceptedObjects = 0;
		int numberOfReadSubtrees = 0;
		int numberOfAcceptedSubtrees = 0;
		boolean interrupted = false;
		
		protected SearchRequest(){
		}
		
		public SearchFilter getSearchFilter() {
			return searchFilter;
		}
		public void setSearchFilter(SearchFilter searchFilter) {
			this.searchFilter = searchFilter;
		}
		
		public List<BinaryMapDataObject> getSearchResults() {
			return searchResults;
		}
		
		public void setInterrupted(boolean interrupted) {
			this.interrupted = interrupted;
		}
		
		public boolean isInterrupted() {
			return interrupted;
		}
	}
	
	private static class MapRoot {
		int minZoom = 0;
		int maxZoom = 0;
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		List<MapTree> trees = new ArrayList<MapTree>();
	}
	
	private static class MapTree {
		int filePointer = 0;
		int length = 0;
		
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
		
		long baseId = 0;
		
		List<String> stringTable = null;
		List<MapTree> subTrees = null;
		
	}
	
	
	private static class AddressRegion {
		String name;
		String enName;
		
		int postcodesOffset = -1;
		int villagesOffset = -1;
		int citiesOffset = -1;
	}
	
	public static void main(String[] args) throws IOException {
//		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Minsk.map.pbf"), "r");
		RandomAccessFile raf = new RandomAccessFile(new File("e:\\Information\\OSM maps\\osmand\\Belarus.map.pbf"), "r");
		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
		System.out.println("VERSION " + reader.getVersion());
//		int sleft = MapUtils.get31TileNumberX(27.596);
//		int sright = MapUtils.get31TileNumberX(27.599);
//		int stop = MapUtils.get31TileNumberY(53.921);
//		int sbottom = MapUtils.get31TileNumberY(53.919);
//		System.out.println("SEARCH " + sleft + " " + sright + " " + stop + " " + sbottom);
//
//		for (BinaryMapDataObject obj : reader.searchMapIndex(buildSearchRequest(sleft, sright, stop, sbottom, 8))) {
//			if (obj.getName() != null) {
//				System.out.println(" " + obj.getName());
//			}
//		}
		String reg = reader.getRegionNames().get(0);
		long time = System.currentTimeMillis();
		List<City> cs = reader.getCities(reg);
		for(City c : cs){
			reader.preloadStreets(c);
			int buildings = 0;
			for(Street s : c.getStreets()){
				reader.preloadBuildings(s);
				buildings += s.getBuildings().size();
			}
			System.out.println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings);
		}
		List<City> villages = reader.getVillages(reg);
		System.out.println(villages.size());
		System.out.println(System.currentTimeMillis() - time);
	}
	
}
