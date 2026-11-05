package net.osmand.plus.measurementtool.graph;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;

import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;

public class BaseCommonChartAdapter extends BaseChartAdapter<LineChart, LineData, GpxDisplayItem> {

	public BaseCommonChartAdapter(@NonNull OsmandApplication app, @NonNull LineChart chart, boolean usedOnMap) {
		super(app, chart, usedOnMap);
	}

	@Override
	protected void attachBottomInfo() {
	}
}
