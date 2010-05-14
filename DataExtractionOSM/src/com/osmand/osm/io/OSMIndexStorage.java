package com.osmand.osm.io;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.osmand.data.Region;

public class OSMIndexStorage extends OsmBaseStorage {
	protected static final String ELEM_OSMAND = "osmand";
	protected static final String ELEM_INDEX = "index";
	protected static final String ELEM_CITY = "city";
	protected static final String ELEM_STREET = "street";
	protected static final String ELEM_BUILDING = "building";
	
	public static final String OSMAND_VERSION = "0.1";
	
	protected Region region;
	
	
	public OSMIndexStorage(Region region){
		this.region = region;
	}
	
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
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		name = saxParser.isNamespaceAware() ? localName : name;
		if(!parseStarted){
			initRootElement(uri, localName, name, attributes);
		} else if(ELEM_INDEX.equals(name)){
		} else if(ELEM_CITY.equals(name)){
		} else {
			super.startElement(uri, localName, name, attributes);
		}
		
	}
}
