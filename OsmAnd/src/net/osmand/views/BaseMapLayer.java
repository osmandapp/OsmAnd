package net.osmand.views;

public abstract class BaseMapLayer extends OsmandMapLayer {

	private int alpha = 255;
	protected int warningToSwitchMapShown = 0;

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
