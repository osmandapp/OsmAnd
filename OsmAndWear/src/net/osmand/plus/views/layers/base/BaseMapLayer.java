package net.osmand.plus.views.layers.base;

import android.content.Context;

import androidx.annotation.NonNull;

public abstract class BaseMapLayer extends OsmandMapLayer {

	public static final int DEFAULT_MAX_ZOOM = 21;
	public static final int DEFAULT_MIN_ZOOM = 1;
	private int alpha = 255;
	protected int warningToSwitchMapShown;

	public int getMaximumShownMapZoom(){
		return DEFAULT_MAX_ZOOM;
	}
	
	public int getMinimumShownMapZoom(){
		return DEFAULT_MIN_ZOOM;
	}
	
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
	
	public int getAlpha() {
		return alpha;
	}

	public BaseMapLayer(@NonNull Context ctx) {
		super(ctx);
	}
}
