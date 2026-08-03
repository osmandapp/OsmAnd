package net.osmand.plus.charts;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IOrderedLineDataSet;

import net.osmand.plus.charts.GpxMarkerView.MarkerValueFormatter;
import net.osmand.plus.utils.OsmAndFormatter;

import java.util.List;

import androidx.annotation.NonNull;

public class OrderedLineDataSet extends LineDataSet implements IOrderedLineDataSet {

	private final GPXDataSetType dataSetType;
	private final GPXDataSetAxisType dataSetAxisType;

	private final boolean leftAxis;

	private String units;
	private float priority;
	private float divX = 1f;

	@NonNull
	private MarkerValueFormatter markerValueFormatter;

	public OrderedLineDataSet(List<Entry> yVals, String label, GPXDataSetType dataSetType,
	                          GPXDataSetAxisType dataSetAxisType, boolean leftAxis) {
		super(yVals, label);
		setHighlightLineWidth(1);
		this.dataSetType = dataSetType;
		this.dataSetAxisType = dataSetAxisType;
		this.leftAxis = leftAxis;
		this.markerValueFormatter = (app, value) ->
				OsmAndFormatter.formatIntegerValue((int) (value + 0.5f), "", app).value + " ";
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

	@Override
	public boolean isLeftAxis() {
		return leftAxis;
	}

	@NonNull
	public MarkerValueFormatter getMarkerValueFormatter() {
		return markerValueFormatter;
	}

	public void setAxisValueFormatter(@NonNull MarkerValueFormatter markerValueFormatter) {
		this.markerValueFormatter = markerValueFormatter;
	}
}
