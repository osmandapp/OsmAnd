package com.osmand.osm.io;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.osmand.data.Amenity;
import com.osmand.data.Building;
import com.osmand.data.City;
import com.osmand.data.MapObject;
import com.osmand.data.Region;
import com.osmand.data.Street;
import com.osmand.data.Amenity.AmenityType;
import com.osmand.data.City.CityType;
import com.osmand.osm.Entity;

public class OSMIndexStorage extends OsmBaseStorage {
	protected static final String ELEM_OSMAND = "osmand";
	protected static final String ELEM_INDEX = "index";
	protected static final String ELEM_CITY = "city";
	protected static final String ELEM_STREET = "street";
	protected static final String ELEM_BUILDING = "building";
	protected static final String ELEM_AMENITY = "amenity";
	
	protected static final String ATTR_CITYTYPE = "citytype";
	protected static final String ATTR_TYPE = "type";
	protected static final String ATTR_SUBTYPE = "subtype";
	protected static final String ATTR_NAME = "name";
	
	public static final String OSMAND_VERSION = "0.1";
	
	protected Region region;
	
	
	public OSMIndexStorage(Region region){
		this.region = region;
	}
	
	protected City currentParsedCity = null;
	protected Street currentParsedStreet = null;
	
	@Override
	protected void initRootElement(String uri, String localName, String name, Attributes attributes) throws OsmVersionNotSupported {
		if(ELEM_OSM.equals(name)){
			if(!supportedVersions.contains(attributes.getValue(ATTR_VERSION))){
				throw new OsmVersionNotSupported();
			}
		} else if(ELEM_OSMAND.equals(name)){
			if(!OSMAND_VERSION.equals(attributes.getValue(ATTR_VERSION))){
				throw new OsmVersionNotSupported();
			}
		} else {
			throw new OsmVersionNotSupported();
		}
		parseStarted = true;
	}
	
	public void parseMapObject(MapObject<? extends Entity> c, Attributes attributes){
		double lat = parseDouble(attributes, ATTR_LAT, 0);
		double lon = parseDouble(attributes, ATTR_LON, 0);
		long id = parseId(attributes, ATTR_ID, -1);
		c.setId(id);
		if(lat != 0 || lon != 0){
			c.setLocation(lat, lon);
		}
		c.setName(attributes.getValue(ATTR_NAME));
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		name = saxParser.isNamespaceAware() ? localName : name;
		if(!parseStarted){
			initRootElement(uri, localName, name, attributes);
		} else if(ELEM_INDEX.equals(name)){
		} else if(ELEM_CITY.equals(name)){
			CityType t = CityType.valueOf(attributes.getValue(ATTR_CITYTYPE));
			City c = new City(t);
			parseMapObject(c, attributes);					
			region.registerCity(c);
			currentParsedCity = c;
		} else if(ELEM_STREET.equals(name)){
			assert currentParsedCity != null;
			Street street = new Street();
			parseMapObject(street, attributes);
			currentParsedCity.registerStreet(street);
			currentParsedStreet = street;
		} else if(ELEM_BUILDING.equals(name)){
			assert currentParsedStreet != null;
			Building building = new Building();
			parseMapObject(building, attributes);
			currentParsedStreet.registerBuilding(building);
		} else if(ELEM_AMENITY.equals(name)){
			Amenity a = new Amenity();
			a.setType(AmenityType.fromString(attributes.getValue(ATTR_TYPE)));
			a.setSubType(attributes.getValue(ATTR_SUBTYPE));
			parseMapObject(a, attributes);
			region.registerAmenity(a);
		} else {
			super.startElement(uri, localName, name, attributes);
		}
		
	}
}
