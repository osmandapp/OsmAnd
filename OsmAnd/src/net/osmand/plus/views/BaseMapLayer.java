package net.osmand.plus.views;

public abstract class BaseMapLayer implements OsmandMapLayer {

	
	public int getMaximumShownMapZoom(){
		return 21;
	}
	
	public int getMinimumShownMapZoom(){
		return 1;
	}
	
	
}
