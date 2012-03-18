package net.osmand.binary;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;

public class BinaryMapDataObject {
	protected int[] coordinates = null;
	protected int[][] polygonInnerCoordinates = null;
	protected boolean area = false;
	protected int[] types = null;
	protected int[] additionalTypes = null;
	
	protected TIntObjectHashMap<String> objectNames = null;
	protected long id = 0;
	
	protected MapIndex mapIndex = null;
	
	
	public BinaryMapDataObject(){
	}
	
	protected void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	public String getName(){
		if(objectNames == null){
			return null;
		}
		return objectNames.get(mapIndex.nameEncodingType);
	}
	
	public TIntObjectHashMap<String> getObjectNames() {
		return objectNames;
	}
	
	public int[][] getPolygonInnerCoordinates() {
		return polygonInnerCoordinates;
	}
	
	public int[] getTypes(){
		return types;
	}
	
	public int[] getAdditionalTypes() {
		return additionalTypes;
	}
	
	public boolean isArea() {
		return area;
	}
	
	public void setArea(boolean area) {
		this.area = area;
	}
	
	public TagValuePair getTagValue(int indType){
		if(mapIndex == null){
			return null;
		}
		return mapIndex.decodeType(types[indType]);
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
