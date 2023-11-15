package net.osmand.plus.settings.enums;

import net.osmand.plus.R;

public enum AutoZoomMap {
	FARTHEST(R.string.auto_zoom_farthest, 1f, 16),
	FAR(R.string.auto_zoom_far, 1.4f, 17),
	CLOSE(R.string.auto_zoom_close, 2f, 19);

	public final float coefficient;
	public final int name;
	public final int maxZoom;

	AutoZoomMap(int name, float coefficient, int maxZoom) {
		this.name = name;
		this.coefficient = coefficient;
		this.maxZoom = maxZoom;
	}
}