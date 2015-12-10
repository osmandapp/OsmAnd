package net.osmand.binary;

import java.util.LinkedHashMap;
import java.util.Map;


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.render.RenderingRulesStorage;

public class BinaryMapDataObject {
	protected int[] coordinates = null;
	protected int[][] polygonInnerCoordinates = null;
	protected boolean area = false;
	protected int[] types = null;
	protected int[] additionalTypes = null;
	protected int objectType = RenderingRulesStorage.POINT_RULES;
	
	protected TIntObjectHashMap<String> objectNames = null;
	protected TIntArrayList namesOrder = null;
	protected long id = 0;
	
	protected MapIndex mapIndex = null;
	
	
	public BinaryMapDataObject(){
	}
	
	public BinaryMapDataObject(int[] coordinates, int[] types, int[][] polygonInnerCoordinates, long id){
		this.polygonInnerCoordinates = polygonInnerCoordinates;
		this.coordinates = coordinates;
		this.additionalTypes = new int[0];
		this.types = types;
		this.id = id;
	}
	
	protected void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	
	public String getName(){
		if(objectNames == null){
			return "";
		}
		String name = objectNames.get(mapIndex.nameEncodingType);
		if(name == null){
			return "";
		}
		return name;
	}
	
	
	public TIntObjectHashMap<String> getObjectNames() {
		return objectNames;
	}
	
	public Map<Integer, String> getOrderedObjectNames() {
		LinkedHashMap<Integer, String> lm = new LinkedHashMap<Integer, String> ();
		for (int i = 0; i < namesOrder.size(); i++) {
			int nm = namesOrder.get(i);
			lm.put(nm, objectNames.get(nm));
		}
		return lm;
	}
	
	public void putObjectName(int type, String name){
		if(objectNames == null){
			objectNames = new TIntObjectHashMap<String>();
			namesOrder = new TIntArrayList();
		}
		objectNames.put(type, name);
		namesOrder.add(type);
	}
	
	public int[][] getPolygonInnerCoordinates() {
		return polygonInnerCoordinates;
	}
	
	public int[] getTypes(){
		return types;
	}
	
	public boolean containsType(int cachedType) {
		if(cachedType != -1) {
			for(int i=0; i<types.length; i++){
				if(types[i] == cachedType) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsAdditionalType(int cachedType) {
		if (cachedType != -1) {
			for (int i = 0; i < additionalTypes.length; i++) {
				if (additionalTypes[i] == cachedType) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String getNameByType(int type) {
		if(type != -1 && objectNames != null) {
			return objectNames.get(type);
		}
		return null;
	}
	
	public int[] getAdditionalTypes() {
		return additionalTypes;
	}
	
	public boolean isArea() {
		return area;
	}
	
	public boolean isCycle(){
		if(coordinates == null || coordinates.length < 2) {
			return false;
		}
		return coordinates[0] == coordinates[coordinates.length - 2] && 
				coordinates[1] == coordinates[coordinates.length - 1];
	}
	
	public void setArea(boolean area) {
		this.area = area;
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
	
	
	public int getSimpleLayer(){
		if(mapIndex != null) {
			for (int i = 0; i < additionalTypes.length; i++) {
				if (mapIndex.positiveLayers.contains(additionalTypes[i])) {
					return 1;
				} else if (mapIndex.negativeLayers.contains(additionalTypes[i])) {
					return -1;
				}
			}
			
		}
		return 0;
	}
	
	public TIntArrayList getNamesOrder() {
		return namesOrder;
	}
	
	public MapIndex getMapIndex() {
		return mapIndex;
	}
	
	public void setMapIndex(MapIndex mapIndex) {
		this.mapIndex = mapIndex;
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
	

}
