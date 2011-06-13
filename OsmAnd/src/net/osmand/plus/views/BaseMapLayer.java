package net.osmand.plus.views;

public abstract class BaseMapLayer implements OsmandMapLayer {

	private int alpha = 255;
	protected boolean warningToSwitchMapShown = false;
	
	public int getMaximumShownMapZoom(){
		return 21;
	}
	
	public int getMinimumShownMapZoom(){
		return 1;
	}
	
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
	
	public int getAlpha() {
		return alpha;
	}
	
	
}
