package net.osmand.osm;

import net.osmand.Algoritms;

public class MapRenderObject {
	private String name = null;
	private int type;
	private byte[] data = null;
	private long id;
	
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
	
	public float getPointLatitude(int ind) {
		return Float.intBitsToFloat(Algoritms.parseIntFromBytes(data, ind * 8));
	}

	public float getPointLongitude(int ind) {
		return Float.intBitsToFloat(Algoritms.parseIntFromBytes(data, ind * 8 + 4));
	}
	
	public int getMapOrder(){
		int order = -1;
		if((type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYGON_TYPE){
			if(MapRenderingTypes.isPolygonBuilding(type)){
				order = 34;
			} else {
				order = 1;
			}
		} else if((type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYLINE_TYPE){
			if(MapRenderingTypes.isHighway(type)){
				order = 32 - MapRenderingTypes.getHighwayType(type) + 1;
			} else {
				order = 2;
			}
		} else {
			order = 64;
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
