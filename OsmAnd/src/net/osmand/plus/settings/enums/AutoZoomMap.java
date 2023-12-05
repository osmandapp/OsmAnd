package net.osmand.plus.settings.enums;

import net.osmand.plus.R;

public enum AutoZoomMap {
	FARTHEST(R.string.auto_zoom_farthest, 1f, 400),
	FAR(R.string.auto_zoom_far, 1.4f, 200),
	CLOSE(R.string.auto_zoom_close, 2f, 50);

	public final float coefficient;
	public final int name;
	public final float minDistanceToDrive;

	AutoZoomMap(int name, float coefficient, float minDistanceToDrive) {
		this.name = name;
		this.coefficient = coefficient;
		this.minDistanceToDrive = minDistanceToDrive;
	}
}