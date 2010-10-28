package net.osmand.binary;

import net.osmand.osm.MapRenderingTypes;

public class BinaryMapDataObject {
	protected int[] coordinates = null;
	protected int[] types = null;
	
	protected int stringId = -1;
	protected long id = 0;
	
	protected long[] restrictions = null;
	protected int highwayAttributes = 0;
	
	protected String name;
	
	
	public BinaryMapDataObject(){
	}
	
	protected void setStringId(int stringId) {
		this.stringId = stringId;
	}
	
	protected void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	protected int getStringId() {
		return stringId;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int[] getTypes(){
		return types;
	}
	
	public long getId() {
		return id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
	
	protected void setTypes(int[] types) {
		this.types = types;
	}
	
	
	public int getHighwayAttributes() {
		return highwayAttributes;
	}
	
	protected void setHighwayAttributes(int highwayAttributes) {
		this.highwayAttributes = highwayAttributes;
	}

	public int getPointsLength(){
		if(coordinates == null){
			return 0;
		}
		return coordinates.length / 2;
	}
	public int getPoint31YTile(int ind) {
		return coordinates[2 * ind + 1];
	}

	public int getPoint31XTile(int ind) {
		return coordinates[2 * ind];
	}
	
	public int getRestrictionCount(){
		if(restrictions == null){
			return 0;
		}
		return restrictions.length;
	}
	
	
	protected void setRestrictions(long[] restrictions) {
		this.restrictions = restrictions;
	}
	
	protected long[] getRestrictions() {
		return restrictions;
	}
	
	public byte getRestrictionType(int k){
		return (byte) (restrictions[k] & 7);
	}
	
	public long getRestriction(int k){
		long l = restrictions[k];
		return (l & ~7l) | (id & 7l);
	} 
	

	public static float getOrder(int wholeType) {
		float order = 0;
		int t = wholeType & 3;					
		int oType = MapRenderingTypes.getMainObjectType(wholeType);
		int sType = MapRenderingTypes.getObjectSubType(wholeType);
		int layer = MapRenderingTypes.getWayLayer(wholeType);
		if (t == MapRenderingTypes.MULTY_POLYGON_TYPE || t == MapRenderingTypes.POLYGON_TYPE) {
			// 1 - 9
			if (oType == MapRenderingTypes.MAN_MADE && sType == MapRenderingTypes.SUBTYPE_BUILDING) {
				// draw over lines
				if(layer != 1){
					order = 64;
				} else {
					order = 2;
				}
			} else {
				if(layer == 1){
					order = 0.5f;
				} else if(layer == 2){
					// over lines
					order = 64;
				} else if (oType == MapRenderingTypes.LANDUSE) {
					switch (sType) {
					case 5:
					case 6:
					case 15:
					case 18:
					case 20:
					case 23:
						order = 1;
						break;
					case 22:
						order = 5;
						break;
					default:
						order = 1f;
						break;
					}
				} else if (oType == MapRenderingTypes.LEISURE) {
					switch (sType) {
					case 3:
					case 10:
					case 13:
						order = 2;
						break;
					case 6:
						order = 4;
					default:
						order = 2;
						break;
					}
				} else if (oType == MapRenderingTypes.POWER) {
					order = 4;
				} else if (oType == MapRenderingTypes.NATURAL) {
					if (order == 5) {
						// coastline
						order = 0.5f;
					} else if (order == 21) {
						// water
						order = 5;
					} else {
						order = 1;
					}
				} else if (oType == MapRenderingTypes.WATERWAY) {
					// water 5
					order = 5;
				} else {
					order = 1;
				}
			}
		} else if (t == MapRenderingTypes.POLYLINE_TYPE) {
			// 10 - 68
			if(layer == 1 && oType != MapRenderingTypes.RAILWAY){
				// not subway especially
				order = 10;
			} else if(layer == 2) {
				order = 67; // over buildings
			} else if (oType == MapRenderingTypes.HIGHWAY) {
				order = 32 - sType + 24;
				if(sType == MapRenderingTypes.PL_HW_MOTORWAY){
					// TODO ? that was done only to have good overlay
					// but really it should be motorway_link have -= 10
					order -= 2;
				}
			} else if (oType == MapRenderingTypes.RAILWAY) {
				order = 58;
			} else if (oType == MapRenderingTypes.AERIALWAY) {
				order = 68; // over buildings
			} else if (oType == MapRenderingTypes.POWER) {
				order = 68; // over buildings
			} else if (oType == MapRenderingTypes.ADMINISTRATIVE) {
				order = 62;
			} else if (oType == MapRenderingTypes.WATERWAY) {
				order = 18;
			} else {
				order = 10;
			}
		} else {
			order = 128;
		}
		return order;
	}
	
	
	
}
