package net.osmand.osm;

import net.osmand.Algoritms;

public class MapRenderObject {
	private String name = null;
	private int type;
	private byte[] data = null;
	private long id;
	private int order = -1;
	
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
		order = -1;
	}
	
	public int getType() {
		return type;
	}
	
	public long getId() {
		return id;
	}
	
	public int getPointsLength(){
		if(data == null){
			return 0;
		}
		return data.length / 8;
	}
	
	public String getName() {
		return name;
	}
	
	public int getPoint31YTile(int ind){
		return Algoritms.parseIntFromBytes(data, ind * 8);
	}
	
	public float getPoint31XTile(int ind) {
		return Algoritms.parseIntFromBytes(data, ind * 8 + 4);
	}
	
	public int getMapOrder(){
		if (order == -1) {
			int oType = MapRenderingTypes.getObjectType(type);
			int sType = MapRenderingTypes.getPolylineSubType(type);
			if ((type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYGON_TYPE) {
				if (MapRenderingTypes.isPolygonBuilding(type)) {
					order = 64;
				} else if (oType == MapRenderingTypes.POWER) {
					order = 60;
				} else if (oType == MapRenderingTypes.WATERWAY || oType == MapRenderingTypes.NATURAL) {
					order = 7;
				} else {
					order = 1;
				}
			} else if ((type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYLINE_TYPE) {
				int layer = MapRenderingTypes.getWayLayer(type);
				if(layer == 1){
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
		}
		return order;
	}
	

	public boolean isPolygon(){
		return (type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYGON_TYPE;
	}
	
	public boolean isPolyLine(){
		return (type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYLINE_TYPE;
	}
	public boolean isPoint(){
		return (type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POINT_TYPE;
	}
	

}
