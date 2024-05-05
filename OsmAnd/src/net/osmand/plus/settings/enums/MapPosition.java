package net.osmand.plus.settings.enums;

import android.graphics.PointF;

import androidx.annotation.NonNull;

public enum MapPosition {

	MIDDLE_TOP(0.25f),
	CENTER(0.5f),
	MIDDLE_BOTTOM(0.70f),
	BOTTOM(0.85f),
	LANDSCAPE_MIDDLE_RIGHT(0.5f);

	private final float ratioY;

	MapPosition(float ratioY) {
		this.ratioY = ratioY;
	}

	@NonNull
	public PointF getRatio(boolean shifted, boolean rtl) {
		return new PointF(getRatioX(shifted, rtl), getRatioY());
	}
	public float getRatioY() {
		return ratioY;
	}

	public float getRatioX(boolean shifted, boolean rtl) {
		if (this == LANDSCAPE_MIDDLE_RIGHT) {
			return 0.7f;
		}

		if (shifted) {
			return rtl ? 0.25f : 0.75f;
		} else {
			return 0.5f;
		}
	}
}