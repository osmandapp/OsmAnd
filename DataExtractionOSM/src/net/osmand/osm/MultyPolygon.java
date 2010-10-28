package net.osmand.osm;

import net.osmand.binary.BinaryMapDataObject;

public class MultyPolygon extends BinaryMapDataObject {

	// currently do not distinguish inner/outer area
	// just not fill intersecting areas
	// first 32 bits - x, second 32 bits - y  
	private long[][] lines = null;
	private String[] names = null;
	public MultyPolygon(){
		super();
		id = -1;
	}
	
	@Override
	public int getPointsLength() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getPoint31XTile(int ind) {
		throw new UnsupportedOperationException();
	}
	
	
	public void setNames(String[] names) {
		this.names = names;
	}
	
	public String getName(int bound){
		return names[bound];
	}
	
	public void setType(int type){
		types = new int[]{type};
	}
	
	@Override
	public int getPoint31YTile(int ind) {
		throw new UnsupportedOperationException();
	}
	
	public int getBoundsCount(){
		return lines == null ? 0 : lines.length;
	}
	
	public int getBoundPointsCount(int bound){
		return lines[bound].length;
	}
	
	public void setLines(long[][] lines) {
		this.lines = lines;
	}
	
	
	public int getPoint31XTile(int ind, int b) {
		return (int)(lines[b][ind] >> 32);
	}
	
	public int getPoint31YTile(int ind, int b) {
		return (int)(lines[b][ind] & Integer.MAX_VALUE);
	}
}

