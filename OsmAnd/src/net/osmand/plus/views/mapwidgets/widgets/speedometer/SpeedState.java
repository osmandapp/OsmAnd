package net.osmand.plus.views.mapwidgets.widgets.speedometer;

import android.content.Context;
import android.graphics.Color;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;

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

	public int getSpeedTextColor(Context context, boolean nightMode) {
		int color = 0;
		switch (this) {
			case SAFE -> color = ColorUtilities.getPrimaryTextColor(context, nightMode);
			case WARNING -> color = context.getColor(R.color.speed_limit_tolerance_value);
			case EXCEED -> color = context.getColor(R.color.speeding_value);
		}
		return color;
	}

	public int getSpeedUnitTextColor(Context context, boolean nightMode) {
		int color = 0;
		switch (this) {
			case SAFE -> color = ColorUtilities.getPrimaryTextColor(context, nightMode);
			case WARNING -> color = context.getColor(R.color.speed_limit_tolerance_units);
			case EXCEED -> color = context.getColor(R.color.speeding_units);
		}
		return color;
	}

}