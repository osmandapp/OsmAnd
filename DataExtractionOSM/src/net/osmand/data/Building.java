package net.osmand.data;

import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.OSMSettings.OSMTagKey;

public class Building extends MapObject {
	
	private String postcode;
	private LatLon latLon2;
	private BuildingInterpolation interpolationType;
	private int interpolationInterval;
	private String name2;
	
	public enum BuildingInterpolation {
		ALL(-1), EVEN(-2), ODD(-3), ALPHABETIC(-4);
		private final int val;

		BuildingInterpolation(int val) {
			this.val = val;
		}
		public int getValue() {
			return val;
		}
	}

	public Building(Entity e){
		super(e);
		// try to extract postcode
		postcode = e.getTag(OSMTagKey.ADDR_POSTCODE);
	}
	
	public Building(){}
	
	public String getPostcode() {
		return postcode;
	}
	
	public int getInterpolationInterval() {
		return interpolationInterval;
	}
	public void setInterpolationInterval(int interpolationNumber) {
		this.interpolationInterval = interpolationNumber;
	}
	
	public BuildingInterpolation getInterpolationType() {
		return interpolationType;
	}
	
	public void setInterpolationType(BuildingInterpolation interpolationType) {
		this.interpolationType = interpolationType;
	}
	
	public LatLon getLatLon2() {
		return latLon2;
	}
	public void setLatLon2(LatLon latlon2) {
		this.latLon2 = latlon2;
	}
	public String getName2() {
		return name2;
	}
	
	public void setName2(String name2) {
		this.name2 = name2;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

}
