package net.osmand.plus.views.mapwidgets.widgets.speedometer;

import android.graphics.Color;

public enum SpeedState {
	SAFE(Color.TRANSPARENT),
	WARNING(Color.parseColor("#FFFFD700")),
	EXCEED(Color.parseColor("#FFD92A2A"));

	final int alertColor;

	SpeedState(int alertColor) {
		this.alertColor = alertColor;
	}

	public int getAlertColor() {
		return alertColor;
	}
}