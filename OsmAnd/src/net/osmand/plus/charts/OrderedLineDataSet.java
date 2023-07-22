package net.osmand.plus.charts;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

public class OrderedLineDataSet extends LineDataSet {

	private final GPXDataSetType dataSetType;
	private final GPXDataSetAxisType dataSetAxisType;

	private final boolean leftAxis;

	private String units;
	private float priority;
	private float divX = 1f;

	public OrderedLineDataSet(List<Entry> yVals, String label, GPXDataSetType dataSetType,
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

	public void setPriority(float priority) {
		this.priority = priority;
	}

	public float getDivX() {
		return divX;
	}

	public void setDivX(float divX) {
		this.divX = divX;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public boolean isLeftAxis() {
		return leftAxis;
	}
}
