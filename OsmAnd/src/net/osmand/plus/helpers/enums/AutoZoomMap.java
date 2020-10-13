package net.osmand.plus.helpers.enums;

import net.osmand.plus.R;

public enum AutoZoomMap {
	FARTHEST(R.string.auto_zoom_farthest, 1f, 15.5f),
	FAR(R.string.auto_zoom_far, 1.4f, 17f),
	CLOSE(R.string.auto_zoom_close, 2f, 19f);

	public final float coefficient;
	public final int name;
	public final float maxZoom;

	AutoZoomMap(int name, float coefficient, float maxZoom) {
		this.name = name;
		this.coefficient = coefficient;
		this.maxZoom = maxZoom;
	}
}