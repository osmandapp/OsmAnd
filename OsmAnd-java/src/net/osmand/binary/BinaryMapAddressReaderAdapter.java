package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndAddressIndex.CitiesIndex;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.data.Building;
import net.osmand.data.Building.BuildingInterpolation;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

public class BinaryMapAddressReaderAdapter {
	
	public final static int CITY_TOWN_TYPE = 1;
	public final static int POSTCODES_TYPE = 2;
	public final static int VILLAGES_TYPE = 3;
	public final static int STREET_TYPE = 4;
	
	private static final Log LOG = PlatformUtil.getLog(BinaryMapAddressReaderAdapter.class);
	
	public static class AddressRegion extends BinaryIndexPart {
		String enName;
		int indexNameOffset = -1;
		List<CitiesBlock> cities = new ArrayList<BinaryMapAddressReaderAdapter.CitiesBlock>();
		
		LatLon calculatedCenter = null;
		
		public String getEnName() {
			return enName;
		}
		
		public List<CitiesBlock> getCities() {
			return cities;
		}
		
		public int getIndexNameOffset() {
			return indexNameOffset;
		}
	}
	
	public static class CitiesBlock extends BinaryIndexPart {
		int type;
	}
	
	private CodedInputStream codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapAddressReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private int readInt() throws IOException {
		return map.readInt();
	}
	

	protected void readAddressIndex(AddressRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				if(region.enName == null || region.enName.length() == 0){
					region.enName = Junidecode.unidecode(region.name);
				}
				return;
			case OsmandOdb.OsmAndAddressIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndAddressIndex.NAME_EN_FIELD_NUMBER :
				region.enName = codedIS.readString();
				break;
			case OsmandOdb.OsmAndAddressIndex.CITIES_FIELD_NUMBER :
				CitiesBlock block = new CitiesBlock();
				region.cities.add(block);
				block.type = 1; 
				block.length  = readInt();
				block.filePointer = codedIS.getTotalBytesRead();
				while(true){
					int tt = codedIS.readTag();
					int ttag = WireFormat.getTagFieldNumber(tt);
					if(ttag == 0) {
						break;
					} else if(ttag == CitiesIndex.TYPE_FIELD_NUMBER){
						block.type = codedIS.readUInt32();
						break;
					} else {
						skipUnknownField(tt);
					}
				}
				
				
				codedIS.seek(block.filePointer + block.length);
				
				break;
			case OsmandOdb.OsmAndAddressIndex.NAMEINDEX_FIELD_NUMBER :
				region.indexNameOffset = codedIS.getTotalBytesRead();
				int length = readInt();
				codedIS.seek(region.indexNameOffset + length + 4);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	protected void readCities(List<City> cities, SearchRequest<City> resultMatcher, StringMatcher matcher, boolean useEn) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case CitiesIndex.CITIES_FIELD_NUMBER :
				int fp = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				City c = readCityHeader(matcher, fp, useEn);
				if(c != null){
					if (resultMatcher == null || resultMatcher.publish(c)) {
						cities.add(c);
					}
				}
				codedIS.popLimit(oldLimit);
				if(resultMatcher != null && resultMatcher.isCancelled()){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
	protected void readCityStreets(SearchRequest<Street> resultMatcher, City city) throws IOException{
		int x = MapUtils.get31TileNumberX(city.getLocation().getLongitude());
		int y = MapUtils.get31TileNumberY(city.getLocation().getLatitude());
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.CityBlockIndex.STREETS_FIELD_NUMBER :
				Street s = new Street(city);
				s.setFileOffset(codedIS.getTotalBytesRead());
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);				
				readStreet(s, null, false, x >> 7, y >> 7, city.isPostcode() ? city.getName() : null);
				if(resultMatcher == null || resultMatcher.publish(s)){
					city.registerStreet(s);
				}
				if(resultMatcher != null && resultMatcher.isCancelled()) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				}
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.CityBlockIndex.BUILDINGS_FIELD_NUMBER :
				// buildings for the town are not used now
				skipUnknownField(t);
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	protected City readCityHeader(StringMatcher nameMatcher, int filePointer, boolean useEn) throws IOException{
		int x = 0;
		int y = 0;
		City c = null;
		boolean englishNameMatched = false;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return c;
			case OsmandOdb.CityIndex.CITY_TYPE_FIELD_NUMBER :
				int type = codedIS.readUInt32();
				c = new City(CityType.values()[type]);
				break;
			case OsmandOdb.CityIndex.ID_FIELD_NUMBER :
				c.setId(codedIS.readUInt64());
				if(nameMatcher != null && useEn && !englishNameMatched){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
				break;
			case OsmandOdb.CityIndex.NAME_EN_FIELD_NUMBER :
				String enName = codedIS.readString();
				if (nameMatcher != null && enName.length() > 0 && nameMatcher.matches(enName)) {
					englishNameMatched = true;
				}
				c.setEnName(enName);
				break;
			case OsmandOdb.CityIndex.NAME_FIELD_NUMBER :
				String name = codedIS.readString();
				if(nameMatcher != null){
					if(!useEn){
						if(!nameMatcher.matches(name)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
							return null;
						}
					} else if(nameMatcher.matches(Junidecode.unidecode(name))){
						englishNameMatched = true;
					}
				}
				if(c == null) {
					c = City.createPostcode(name); 
				}
				c.setName(name);
				break;
			case OsmandOdb.CityIndex.X_FIELD_NUMBER :
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.CityIndex.Y_FIELD_NUMBER :
				y = codedIS.readUInt32();
				c.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				if(c.getEnName().length() == 0){
					c.setEnName(Junidecode.unidecode(c.getName()));
				}
				break;
			case OsmandOdb.CityIndex.SHIFTTOCITYBLOCKINDEX_FIELD_NUMBER :
				int offset = readInt();
				offset += filePointer;
				c.setFileOffset(offset);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	protected Street readStreet(Street s, SearchRequest<Building> buildingsMatcher, boolean loadBuildingsAndIntersected, int city24X, int city24Y, String postcodeFilter) throws IOException{
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
				if(s.getEnName().length() == 0){
					s.setEnName(Junidecode.unidecode(s.getName()));
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
			case OsmandOdb.StreetIndex.INTERSECTIONS_FIELD_NUMBER :
				int length = codedIS.readRawVarint32();
				if(loadBuildingsAndIntersected){
					int oldLimit = codedIS.pushLimit(length);
					Street si = readIntersectedStreet(s.getCity(), x, y);
					s.addIntersectedStreet(si);
					codedIS.popLimit(oldLimit);
				} else {
					codedIS.skipRawBytes(length);
				}
				break;
			case OsmandOdb.StreetIndex.BUILDINGS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				length = codedIS.readRawVarint32();
				if(loadBuildingsAndIntersected){
					int oldLimit = codedIS.pushLimit(length);
					Building b = readBuilding(offset, x, y);
					if (postcodeFilter == null || postcodeFilter.equalsIgnoreCase(b.getPostcode())) {
						if (buildingsMatcher == null || buildingsMatcher.publish(b)) {
							s.addBuilding(b);
						}
					}
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
	
	protected Street readIntersectedStreet(City c, int street24X, int street24Y) throws IOException{
		int x = 0;
		int y = 0;
		Street s = new Street(c);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				s.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				if(s.getEnName().length() == 0){
					s.setEnName(Junidecode.unidecode(s.getName()));
				}
				return s;
			case OsmandOdb.BuildingIndex.ID_FIELD_NUMBER :
				s.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.StreetIntersection.NAME_EN_FIELD_NUMBER:
				s.setEnName(codedIS.readString());
				break;
			case OsmandOdb.StreetIntersection.NAME_FIELD_NUMBER:
				s.setName(codedIS.readString());
				break;
			case OsmandOdb.StreetIntersection.INTERSECTEDX_FIELD_NUMBER :
				x =  codedIS.readSInt32() + street24X;
				break;
			case OsmandOdb.StreetIntersection.INTERSECTEDY_FIELD_NUMBER :
				y =  codedIS.readSInt32() + street24Y;
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	protected Building readBuilding(int fileOffset, int street24X, int street24Y) throws IOException{
		int x = 0;
		int y = 0;
		int x2 = 0;
		int y2 = 0;
		Building b = new Building();
		b.setFileOffset(fileOffset);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				b.setLocation(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
				if(x2 != 0 && y2 != 0) {
					b.setLatLon2(new LatLon(MapUtils.getLatitudeFromTile(24, y2), MapUtils.getLongitudeFromTile(24, x2)));
				}
				if(b.getEnName().length() == 0){
					b.setEnName(Junidecode.unidecode(b.getName()));
				}
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
			case OsmandOdb.BuildingIndex.NAME_EN2_FIELD_NUMBER :
				// no where to set now
				codedIS.readString();
				break;
			case OsmandOdb.BuildingIndex.NAME2_FIELD_NUMBER :
				b.setName2(codedIS.readString());
				break;
			case OsmandOdb.BuildingIndex.INTERPOLATION_FIELD_NUMBER :
				int sint = codedIS.readSInt32();
				if(sint > 0) {
					b.setInterpolationInterval(sint);
				} else {
					b.setInterpolationType(BuildingInterpolation.fromValue(sint));
				}
				break;
			case OsmandOdb.BuildingIndex.X_FIELD_NUMBER :
				x =  codedIS.readSInt32() + street24X;
				break;
			case OsmandOdb.BuildingIndex.X2_FIELD_NUMBER :
				x2 =  codedIS.readSInt32() + street24X;
				break;
			case OsmandOdb.BuildingIndex.Y_FIELD_NUMBER :
				y =  codedIS.readSInt32() + street24Y;
				break;
			case OsmandOdb.BuildingIndex.Y2_FIELD_NUMBER :
				y2 =  codedIS.readSInt32() + street24Y;
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

	public void searchAddressDataByName(AddressRegion reg, SearchRequest<MapObject> req, int[] typeFilter) throws IOException {
		TIntArrayList loffsets = new TIntArrayList();
		CollatorStringMatcher matcher = new CollatorStringMatcher( req.nameQuery, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		long time = System.currentTimeMillis();
		int indexOffset = 0;
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmAndAddressNameIndexData.TABLE_FIELD_NUMBER:
				int length = readInt();
				indexOffset = codedIS.getTotalBytesRead();
				int oldLimit = codedIS.pushLimit(length);
				// here offsets are sorted by distance
				map.readIndexedStringTable(matcher.getCollator(), req.nameQuery, "", loffsets, 0);
				codedIS.popLimit(oldLimit);
				break;
			case OsmAndAddressNameIndexData.ATOM_FIELD_NUMBER:
				// also offsets can be randomly skipped by limit
				loffsets.sort();
				TIntArrayList[] refs = new TIntArrayList[5];
				for (int i = 0; i < refs.length; i++) {
					refs[i] = new TIntArrayList();
				}

				LOG.info("Searched address structure in " + (System.currentTimeMillis() - time) + "ms. Found " + loffsets.size()
						+ " subtress");
				for (int j = 0; j < loffsets.size(); j++) {
					int fp = indexOffset + loffsets.get(j);
					codedIS.seek(fp);
					int len = codedIS.readRawVarint32();
					int oldLim = codedIS.pushLimit(len);
					int stag = 0;
					do {
						int st = codedIS.readTag();
						stag = WireFormat.getTagFieldNumber(st);
						if(stag == AddressNameIndexData.ATOM_FIELD_NUMBER) {
							int slen = codedIS.readRawVarint32();
							int soldLim = codedIS.pushLimit(slen);
							readAddressNameData(req, refs, fp);
							codedIS.popLimit(soldLim);
						} else if(stag != 0){
							skipUnknownField(st);
						}
					} while(stag != 0);
					
					codedIS.popLimit(oldLim);
					if (req.isCancelled()) {
						return;
					}
				}
				if (typeFilter == null) {
					typeFilter = new int[] { CITY_TOWN_TYPE, POSTCODES_TYPE, VILLAGES_TYPE, STREET_TYPE };
				}
				for (int i = 0; i < typeFilter.length && !req.isCancelled(); i++) {
					TIntArrayList list = refs[typeFilter[i]];
					if (typeFilter[i] == STREET_TYPE) {
						for (int j = 0; j < list.size() && !req.isCancelled(); j += 2) {
							City obj = null;
							{
								codedIS.seek(list.get(j + 1));
								int len = codedIS.readRawVarint32();
								int old = codedIS.pushLimit(len);
								obj = readCityHeader(null, list.get(j + 1), false);
								codedIS.popLimit(old);
							}
							if (obj != null) {
								codedIS.seek(list.get(j));
								int len = codedIS.readRawVarint32();
								int old = codedIS.pushLimit(len);
								LatLon l = obj.getLocation();
								Street s = new Street(obj);
								readStreet(s, null, false, MapUtils.get31TileNumberX(l.getLongitude()) >> 7,
										MapUtils.get31TileNumberY(l.getLatitude()) >> 7, obj.isPostcode() ? obj.getName() : null);

								if (matcher.matches(s.getName())) {
									req.publish(s);
								}
								codedIS.popLimit(old);
							}
						}
					} else {
						list.sort();
						for (int j = 0; j < list.size() && !req.isCancelled(); j++) {
							codedIS.seek(list.get(j));
							int len = codedIS.readRawVarint32();
							int old = codedIS.pushLimit(len);
							City obj = readCityHeader(matcher, list.get(j), false);
							if (obj != null) {
								req.publish(obj);
							}
							codedIS.popLimit(old);
						}
					}
				}
				LOG.info("Whole address search by name is done in " + (System.currentTimeMillis() - time) + "ms. Found "
						+ req.getSearchResults().size());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}

	}

	private void readAddressNameData(SearchRequest<MapObject> req, TIntArrayList[] refs, int fp) throws IOException {
		TIntArrayList toAdd = null;
		while(true){
			if(req.isCancelled()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case AddressNameIndexDataAtom.NAMEEN_FIELD_NUMBER :
				codedIS.readString();
				break;
			case AddressNameIndexDataAtom.NAME_FIELD_NUMBER :
				codedIS.readString();
				break;
			case AddressNameIndexDataAtom.SHIFTTOCITYINDEX_FIELD_NUMBER :
				if(toAdd != null) {
					toAdd.add(fp - codedIS.readInt32());
				}
				break;
			case AddressNameIndexDataAtom.SHIFTTOINDEX_FIELD_NUMBER :
				if(toAdd != null) {
					toAdd.add(fp - codedIS.readInt32());
				}
				break;
			case AddressNameIndexDataAtom.TYPE_FIELD_NUMBER :
				int type = codedIS.readInt32();
				toAdd = refs[type];
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	
}
