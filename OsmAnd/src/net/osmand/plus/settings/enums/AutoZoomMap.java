package net.osmand.plus.settings.enums;

import net.osmand.plus.R;

public enum AutoZoomMap {
	FARTHEST(R.string.auto_zoom_farthest, 1f, 15.5f, 14, 18),
	FAR(R.string.auto_zoom_far, 1.4f, 17f, 15, 19),
	CLOSE(R.string.auto_zoom_close, 2f, 19f, 17, 21);

	public final float coefficient;
	public final int name;
	public final float maxZoomFromSpeed;
	public final int minZoomBaseToFocus;
	public final int maxZoomBaseToFocus;

	AutoZoomMap(int name, float coefficient, float maxZoomFromSpeed, int minZoomBaseToFocus, int maxZoomBaseToFocus) {
		this.name = name;
		this.coefficient = coefficient;
		this.maxZoomFromSpeed = maxZoomFromSpeed;
		this.minZoomBaseToFocus = minZoomBaseToFocus;
		this.maxZoomBaseToFocus = maxZoomBaseToFocus;
	}
}