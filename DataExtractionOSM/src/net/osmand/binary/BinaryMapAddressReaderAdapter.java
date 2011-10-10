package net.osmand.binary;

import java.io.IOException;
import java.util.List;

import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.sf.junidecode.Junidecode;

import com.google.protobuf.CodedInputStreamRAF;
import com.google.protobuf.WireFormat;

public class BinaryMapAddressReaderAdapter {
	
	public static class AddressRegion extends BinaryIndexPart {
		String enName;
		
		int postcodesOffset = -1;
		int villagesOffset = -1;
		int citiesOffset = -1;
		
		LatLon calculatedCenter = null;
	}
	
	private CodedInputStreamRAF codedIS;
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
	
	protected void readCities(List<City> cities, SearchRequest<MapObject> resultMatcher, StringMatcher matcher, boolean useEn) throws IOException {
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
				City c = readCity(null, offset, false, null, matcher, useEn);
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
	

	protected PostCode readPostcode(PostCode p, int fileOffset, SearchRequest<Street> resultMatcher, boolean loadStreets, String postcodeFilter) throws IOException{
		int x = 0;
		int y = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return p;
			case OsmandOdb.PostcodeIndex.POSTCODE_FIELD_NUMBER :
				String name = codedIS.readString();
				if(postcodeFilter != null && !postcodeFilter.equalsIgnoreCase(name)){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
				if(p == null){
					p = new PostCode(name);
					p.setFileOffset(fileOffset);
				}
				p.setName(name);
				break;
			case OsmandOdb.PostcodeIndex.X_FIELD_NUMBER :
				x = codedIS.readFixed32();
				break;
			case OsmandOdb.PostcodeIndex.Y_FIELD_NUMBER :
				y = codedIS.readFixed32();
				p.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				if(!loadStreets){
					// skip everything
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return p;
				}
				break;
			case OsmandOdb.PostcodeIndex.STREETS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadStreets){
					Street s = new Street(null);
					int oldLimit = codedIS.pushLimit(length);
					s.setFileOffset(offset);
					readStreet(s, null, true, x >> 7, y >> 7, p.getName());
					if (resultMatcher == null || resultMatcher.publish(s)) {
						p.registerStreet(s, false);
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
	
	
	protected City readCity(City c, int fileOffset, boolean loadStreets, SearchRequest<Street> resultMatcher, 
			StringMatcher nameMatcher, boolean useEn) throws IOException{
		int x = 0;
		int y = 0;
		int streetInd = 0;
		boolean englishNameMatched = false;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
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
				if(nameMatcher != null && useEn && !englishNameMatched){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
				break;
			case OsmandOdb.CityIndex.NAME_EN_FIELD_NUMBER :
				String enName = codedIS.readString();
				if (nameMatcher != null && enName.length() > 0) {
					if(!nameMatcher.matches(enName)){
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return null;
					} else {
						englishNameMatched = true;
					}
				}
				c.setEnName(enName);
				break;
			case OsmandOdb.CityIndex.NAME_FIELD_NUMBER :
				String name = codedIS.readString();
				c.setName(name);
				if(nameMatcher != null){
					if(!useEn){
						if(!nameMatcher.matches(name)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
							return null;
						}
					} else {
						if(nameMatcher.matches(Junidecode.unidecode(name))){
							englishNameMatched = true;
						}
					}
				}
				break;
			case OsmandOdb.CityIndex.X_FIELD_NUMBER :
				x = codedIS.readFixed32();
				break;
			case OsmandOdb.CityIndex.Y_FIELD_NUMBER :
				y = codedIS.readFixed32();
				c.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				if(c.getEnName().length() == 0){
					c.setEnName(Junidecode.unidecode(c.getName()));
				}
				if(!loadStreets){
					// skip everything
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return c;
				}
				break;
			case OsmandOdb.CityIndex.INTERSECTIONS_FIELD_NUMBER :
				codedIS.skipRawBytes(codedIS.readRawVarint32());
				break;
			case OsmandOdb.CityIndex.STREETS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadStreets){
					Street s = new Street(c);
					int oldLimit = codedIS.pushLimit(length);
					s.setFileOffset(offset);
					s.setIndexInCity(streetInd++);
					readStreet(s, null, false, x >> 7, y >> 7, null);
					if (resultMatcher == null || resultMatcher.publish(s)) {
						c.registerStreet(s);
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
	
	protected Street readStreet(Street s, SearchRequest<Building> resultMatcher, boolean loadBuildings, int city24X, int city24Y, String postcodeFilter) throws IOException{
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
			case OsmandOdb.StreetIndex.BUILDINGS_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				if(loadBuildings){
					int oldLimit = codedIS.pushLimit(length);
					Building b = readBuilding(offset, x, y);
					if (postcodeFilter == null || postcodeFilter.equalsIgnoreCase(b.getPostcode())) {
						if (resultMatcher == null || resultMatcher.publish(b)) {
							s.registerBuilding(b);
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
	
	protected Building readBuilding(int fileOffset, int street24X, int street24Y) throws IOException{
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
	
	// 2 different quires : s2 == null -> fill possible streets, s2 != null return LatLon intersection 
	private LatLon readIntersectedStreets(Street[] cityStreets, Street s, Street s2, LatLon parent, List<Street> streets) throws IOException {
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		boolean e = false;
		while(!e){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				e = true;
				break;
			case OsmandOdb.InteresectedStreets.INTERSECTIONS_FIELD_NUMBER:
				int nsize = codedIS.readRawVarint32();
				int nold = codedIS.pushLimit(nsize);
				int st1 = -1;
				int st2 = -1;
				int cx = 0;
				int cy = 0;
				boolean end = false;
				while (!end) {
					int nt = codedIS.readTag();
					int ntag = WireFormat.getTagFieldNumber(nt);
					switch (ntag) {
					case 0:
						end = true;
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDSTREET1_FIELD_NUMBER:
						st1 = codedIS.readUInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDSTREET2_FIELD_NUMBER:
						st2 = codedIS.readUInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDX_FIELD_NUMBER:
						cx = codedIS.readSInt32();
						break;
					case OsmandOdb.StreetIntersection.INTERSECTEDY_FIELD_NUMBER:
						cy = codedIS.readSInt32();
						break;
					default:
						skipUnknownField(nt);
					}
				}
				codedIS.popLimit(nold);
				if (s2 == null) {
					// find all intersections
					if (st1 == s.getIndexInCity() && st2 != -1 && st2 < cityStreets.length && cityStreets[st2] != null) {
						streets.add(cityStreets[st2]);
					} else if (st2 == s.getIndexInCity() && st1 != -1 && st1 < cityStreets.length && cityStreets[st1] != null) {
						streets.add(cityStreets[st1]);
					}
				} else {
					if((st1 == s.getIndexInCity() && st2 == s2.getIndexInCity() ) || 
							(st2 == s.getIndexInCity() && st1 == s2.getIndexInCity())) {
						int x = (int) (MapUtils.getTileNumberX(24, parent.getLongitude()) + cx);
						int y = (int) (MapUtils.getTileNumberY(24, parent.getLatitude()) + cy);
						codedIS.popLimit(old);
						return new LatLon(MapUtils.getLatitudeFromTile(24, y), MapUtils.getLongitudeFromTile(24, x));
					}
				}
				
				break;
			default:
				skipUnknownField(t);
			}
		}
		codedIS.popLimit(old);
		return null;
	}
	
	// do not preload streets in city
	protected LatLon findIntersectedStreets(City c, Street s, Street s2, List<Street> streets) throws IOException {
		if(s.getIndexInCity() == -1){
			return null;
		}
		codedIS.seek(c.getFileOffset());
		int size = codedIS.readRawVarint32();
		int old = codedIS.pushLimit(size);
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				codedIS.popLimit(old);
				return null;
			case OsmandOdb.CityIndex.INTERSECTIONS_FIELD_NUMBER :
				Street[] cityStreets = new Street[c.getStreets().size()];
				for(Street st : c.getStreets()){
					if(st.getIndexInCity() >= 0 && st.getIndexInCity() < cityStreets.length){
						cityStreets[st.getIndexInCity()] = st;
					}
				}
				LatLon ret = readIntersectedStreets(cityStreets, s, s2, c.getLocation(), streets);
				codedIS.popLimit(old);
				return ret;
			default:
				skipUnknownField(t);
			}
		}
		
	}
	
	
	
	protected void readPostcodes(List<PostCode> postcodes, SearchRequest<MapObject> resultMatcher, StringMatcher nameMatcher) throws IOException{
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.PostcodesIndex.POSTCODES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				final PostCode postCode = readPostcode(null, offset, null, false, null);
				// support getEnName??
				if (nameMatcher == null || nameMatcher.matches(postCode.getName())) {
					if (resultMatcher == null || resultMatcher.publish(postCode)) {
						postcodes.add(postCode);
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
	
	protected PostCode findPostcode(String name) throws IOException{
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return null;
			case OsmandOdb.PostcodesIndex.POSTCODES_FIELD_NUMBER :
				int offset = codedIS.getTotalBytesRead();
				int length = codedIS.readRawVarint32();
				int oldLimit = codedIS.pushLimit(length);
				PostCode p = readPostcode(null, offset, null, false, name);
				codedIS.popLimit(oldLimit);
				if(p != null){
					return p;
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	

}
