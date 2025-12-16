package net.osmand.plus.charts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public enum GpxDataSetTypeGroup {
	GENERAL(0),
	EXTERNAL_SENSORS(R.string.external_sensor_widgets),
	VEHICLE_METRICS(R.string.vehicle_metrics_chart_category);

	@StringRes
	private final int titleId;

	GpxDataSetTypeGroup(int titleId){
		this.titleId = titleId;
	}

	@Nullable
	public String getName(@NonNull OsmandApplication app){
		if(titleId != 0){
			return app.getString(titleId);
		} else{
			return null;
		}
	}
}
