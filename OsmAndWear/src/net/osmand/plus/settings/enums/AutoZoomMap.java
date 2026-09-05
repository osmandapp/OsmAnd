package net.osmand.plus.settings.enums;

import net.osmand.plus.R;

public enum AutoZoomMap {
	FARTHEST(R.string.auto_zoom_farthest, 1f, 16, 400),
	FAR(R.string.auto_zoom_far, 1.4f, 17, 200),
	CLOSE(R.string.auto_zoom_close, 2f, 19, 50);

	public final float coefficient;
	public final int name;
	public final int maxZoom;
	public final float minDistanceToDrive;

	AutoZoomMap(int name, float coefficient, int maxZoom, float minDistanceToDrive) {
		this.name = name;
		this.coefficient = coefficient;
		this.maxZoom = maxZoom;
		this.minDistanceToDrive = minDistanceToDrive;
	}
}