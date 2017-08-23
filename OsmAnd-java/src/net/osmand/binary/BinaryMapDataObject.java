package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

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
	

	
	public BinaryMapDataObject(long id, int[] coordinates, int[][] polygonInnerCoordinates, int objectType, boolean area, 
			int[] types, int[] additionalTypes){
		this.polygonInnerCoordinates = polygonInnerCoordinates;
		this.coordinates = coordinates;
		this.additionalTypes = additionalTypes;
		this.types = types;
		this.id = id;
		this.objectType = objectType;
		this.area = area;
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
		if (namesOrder == null) {
			return null;
		}
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
	
	
	public boolean compareBinary(BinaryMapDataObject thatObj) {
		if(this.objectType == thatObj.objectType
				&& this.id == thatObj.id
				&& this.area == thatObj.area 
				&& Arrays.equals(this.polygonInnerCoordinates, thatObj.polygonInnerCoordinates)
				&& Arrays.equals(this.coordinates, thatObj.coordinates) ) {
			boolean equals = true;
			if(equals) {
				if(types == null || thatObj.types == null) {
					equals = types == thatObj.types; 
				} else if(types.length != thatObj.types.length){
					equals = false;
				} else {
					for(int i = 0; i < types.length && equals; i++) {
						TagValuePair o = mapIndex.decodeType(types[i]);
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.types[i]);
						equals = o.equals(s);
					}
				}
			}
			if(equals) {
				if(additionalTypes == null || thatObj.additionalTypes == null) {
					equals = additionalTypes == thatObj.additionalTypes; 
				} else if(additionalTypes.length != thatObj.additionalTypes.length){
					equals = false;
				} else {
					for(int i = 0; i < additionalTypes.length && equals; i++) {
						TagValuePair o = mapIndex.decodeType(additionalTypes[i]);
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.additionalTypes[i]);
						equals = o.equals(s);
					}
				}
			}
			if(equals) {
				if(namesOrder == null || thatObj.namesOrder == null) {
					equals = namesOrder == thatObj.namesOrder; 
				} else if(namesOrder.size() != thatObj.namesOrder.size()){
					equals = false;
				} else {
					for(int i = 0; i < namesOrder.size() && equals; i++) {
						TagValuePair o = mapIndex.decodeType(namesOrder.get(i));
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.namesOrder.get(i));
						equals = o.equals(s);
					}
				}
			}
			if(equals) {
				// here we know that name indexes are equal & it is enough to check the value sets
				if(objectNames == null || thatObj.objectNames == null) {
					equals = objectNames == thatObj.objectNames; 
				} else if(objectNames.size() != thatObj.objectNames.size()){
					equals = false;
				} else {
					for(int i = 0; i < namesOrder.size() && equals; i++) {
						String o = objectNames.get(namesOrder.get(i));
						String s = thatObj.objectNames.get(thatObj.namesOrder.get(i));
						equals = Algorithms.objectEquals(o, s);
					}
				}
			}
			
			return equals;
		}
		
		return false;
	}


	public int[] getCoordinates() {
		return coordinates;
	}
	
	
	public int getObjectType() {
		return objectType;
	}
}
