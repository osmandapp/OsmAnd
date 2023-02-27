package net.osmand.plus.charts;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import net.osmand.plus.charts.ChartUtils.GPXDataSetAxisType;
import net.osmand.plus.charts.ChartUtils.GPXDataSetType;

import java.util.List;

public class OrderedLineDataSet extends LineDataSet {

	private final GPXDataSetType dataSetType;
	private final GPXDataSetAxisType dataSetAxisType;

	private final boolean leftAxis;

	float priority;
	String units;
	float divX = 1f;
	float divY = 1f;
	float mulY = 1f;

	OrderedLineDataSet(List<Entry> yVals, String label, ChartUtils.GPXDataSetType dataSetType,
	                   ChartUtils.GPXDataSetAxisType dataSetAxisType, boolean leftAxis) {
		super(yVals, label);
		setHighlightLineWidth(1);
		this.dataSetType = dataSetType;
		this.dataSetAxisType = dataSetAxisType;
		this.leftAxis = leftAxis;
	}

	public ChartUtils.GPXDataSetType getDataSetType() {
		return dataSetType;
	}

	public ChartUtils.GPXDataSetAxisType getDataSetAxisType() {
		return dataSetAxisType;
	}

	public float getPriority() {
		return priority;
	}

	public float getDivX() {
		return divX;
	}

	public float getDivY() {
		return divY;
	}

	public float getMulY() {
		return mulY;
	}

	public String getUnits() {
		return units;
	}

	public boolean isLeftAxis() {
		return leftAxis;
	}
}
