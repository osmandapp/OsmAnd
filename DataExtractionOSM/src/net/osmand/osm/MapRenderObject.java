package net.osmand.osm;

import net.osmand.Algoritms;

public class MapRenderObject {
	private String name = null;
	private int type;
	private byte[] data = null;
	private long id;
	private float order = -1;
	private boolean multitype = false;
	
	public MapRenderObject(long id){
		this.id = id;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public void setType(int type) {
		this.type = type;
		multitype = (type & 1) > 0; 
		order = -1;
	}
	
	public int getWholeType() {
		return type;
	}
	
	public long getId() {
		return id;
	}
	
	public boolean isMultitype() {
		return multitype;
	}
	
	public byte getMultiTypes(){
		return multitype ? data[0] : 0;
	}
	
	public int getAdditionalType(int k){
		return Algoritms.parseSmallIntFromBytes(data, k * 2 + 1);
	}
	
	public int getMainType(){
		return (type >> 1);
	}
	
	public int getSecondType(){
		int pr = type >> 1;
		if((pr & 3) == MapRenderingTypes.POLYLINE_TYPE && MapRenderingTypes.getMainObjectType(pr) == MapRenderingTypes.HIGHWAY){
			return 0;
		}
		return type >> 16;
				
	}
	
	public int getPointsLength(){
		if(data == null || data.length == 0){
			return 0;
		}
		if(multitype){
			return (data.length - data[0] * 2 - 1) / 8;
		} else {
			return data.length / 8;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public int getPoint31YTile(int ind){
		if(multitype){
			return Algoritms.parseIntFromBytes(data, ind * 8 + data[0] * 2 + 1);
		} else {
			return Algoritms.parseIntFromBytes(data, ind * 8);
		}
	}
	
	public int getPoint31XTile(int ind) {
		if(multitype){
			return Algoritms.parseIntFromBytes(data, ind * 8 + 4 + data[0] * 2 + 1);
		} else {
			return Algoritms.parseIntFromBytes(data, ind * 8 + 4);
		}
	}
	
	public float getMapOrder(){
		if (order == -1) {
			order = getOrder(getMainType());
		}
		return order;
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
