package net.osmand.plus.routing;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public enum AlarmInfoType {

	SPEED_CAMERA(1, R.string.traffic_warning_speed_camera),
	SPEED_LIMIT(2, R.string.traffic_warning_speed_limit),
	BORDER_CONTROL(3, R.string.traffic_warning_border_control),
	RAILWAY(4, R.string.traffic_warning_railways),
	TRAFFIC_CALMING(5, R.string.traffic_warning_calming),
	TOLL_BOOTH(6, R.string.traffic_warning_payment),
	STOP(7, R.string.traffic_warning_stop),
	PEDESTRIAN(8, R.string.traffic_warning_pedestrian),
	HAZARD(9, R.string.traffic_warning_hazard),
	MAXIMUM(10, R.string.traffic_warning),
	TUNNEL(11, R.string.tunnel_warning);

	private final int priority;
	private final int titleId;

	AlarmInfoType(int priority, int titleId) {
		this.priority = priority;
		this.titleId = titleId;
	}

	public int getPriority() {
		return priority;
	}

	@NonNull
	public String getVisualName(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}
}
