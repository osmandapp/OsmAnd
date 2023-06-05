package net.osmand.plus.charts;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

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

	OrderedLineDataSet(List<Entry> yVals, String label, GPXDataSetType dataSetType,
	                   GPXDataSetAxisType dataSetAxisType, boolean leftAxis) {
		super(yVals, label);
		setHighlightLineWidth(1);
		this.dataSetType = dataSetType;
		this.dataSetAxisType = dataSetAxisType;
		this.leftAxis = leftAxis;
	}

	public GPXDataSetType getDataSetType() {
		return dataSetType;
	}

	public GPXDataSetAxisType getDataSetAxisType() {
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
