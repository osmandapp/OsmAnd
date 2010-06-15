package com.osmand.data;

import com.osmand.osm.Entity;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class Building extends MapObject {
	
	private String postcode;

	public Building(Entity e){
		super(e);
		// try to extract postcode
		postcode = e.getTag(OSMTagKey.ADDR_POSTCODE);
	}
	
	public Building(){}
	
	public String getPostcode() {
		return postcode;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

}
