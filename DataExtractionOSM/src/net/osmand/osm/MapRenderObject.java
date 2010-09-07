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
				order = 64;
			} else {
				order = 1;
			}
		} else if((type & MapRenderingTypes.TYPE_MASK) == MapRenderingTypes.POLYLINE_TYPE){
			int oType = MapRenderingTypes.getObjectType(type);
			int sType = MapRenderingTypes.getPolylineSubType(type);
			if(oType == MapRenderingTypes.HIGHWAY){
				order = 32 - sType + 24;
			} else if(oType == MapRenderingTypes.RAILWAY){
				order = 58;
			} else if(oType == MapRenderingTypes.WATERWAY){
				order = 18;
			}
		} else {
			order = 128;
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
