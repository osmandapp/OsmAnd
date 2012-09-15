package net.osmand.data;

import net.osmand.Algoritms;
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
		
		public static BuildingInterpolation fromValue(int i){
			for(BuildingInterpolation b : values()) {
				if(b.val == i) {
					return b;
				}
			}
			return null;
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
	
	@Override
	public String getName(boolean en) {
		String fname = super.getName(en);
		if(interpolationInterval !=0){
			return fname+"-"+name2 +" (+"+interpolationInterval+") ";
		} else if(interpolationType != null) {
			return fname+"-"+name2 +" ("+interpolationType.toString().toLowerCase()+") ";
		}
		return name;
	}	
	

	public float interpolation(String hno) {
		if(getInterpolationType() != null || getInterpolationInterval() > 0) {
			int num = Algoritms.extractFirstIntegerNumber(hno);
			int numB = Algoritms.extractFirstIntegerNumber(super.getName());
			int numT = numB; 
			if (num >= numB) {
				if (getName2() != null) {
					numT = Algoritms.extractFirstIntegerNumber(getName2());
					if(numT < num) {
						return -1;
					}
				}
				if (getInterpolationType() == BuildingInterpolation.EVEN && num % 2 == 1) {
					return -1;
				}
				if (getInterpolationType() == BuildingInterpolation.ODD && num % 2 == 0) {
					return -1;
				}
				if (getInterpolationInterval() != 0 && (num - numB) % getInterpolationInterval() != 0) {
					return -1;
				}
			} else {
				return -1;
			}
			if(numT > numB){
				if(getInterpolationType() == BuildingInterpolation.EVEN || getInterpolationType() == BuildingInterpolation.ODD){
					return ((float)num - numB) / (((float)numT - numB)*2f);
				}
				if (getInterpolationInterval() > 0) {
					return ((float) num - numB) / (((float) numT - numB) * getInterpolationInterval());
				}
				return ((float)num - numB) / (((float)numT - numB));
			}
			return 0;
		}
		return -1;
	}
	public boolean belongsToInterpolation(String hno) {
		return interpolation(hno) >= 0;
	}
	
	@Override
	public String toString() {
		if(interpolationInterval !=0){
			return name+"-"+name2 +" (+"+interpolationInterval+") ";
		} else if(interpolationType != null) {
			return name+"-"+name2 +" ("+interpolationType+") ";
		}
		return name;
	}

}
