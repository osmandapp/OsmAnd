package net.osmand.plus.measurementtool.graph;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;

public class BaseCommonGraphAdapter extends BaseGraphAdapter<LineChart, LineData, GpxDisplayItem>{

	public BaseCommonGraphAdapter(@NonNull OsmandApplication app, @NonNull LineChart chart, boolean usedOnMap) {
		super(app, chart, usedOnMap);
	}

	@Override
	public void updateView() {
		chart.setData(chartData);
		updateHighlight();
	}

}
