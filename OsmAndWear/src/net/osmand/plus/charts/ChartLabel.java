package net.osmand.plus.charts;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.LineChart.YAxisLabelView;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.plus.R;

public class ChartLabel extends YAxisLabelView {

	private static final int SPAN_FLAG = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;

	private final TextView label;

	public ChartLabel(@NonNull Context context, int layoutId) {
		super(context, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int layoutId) {
		super(context, attrs, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int layoutId) {
		super(context, attrs, defStyleAttr, layoutId);
		label = findViewById(R.id.label);
	}

	public ChartLabel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes, int layoutId) {
		super(context, attrs, defStyleAttr, defStyleRes, layoutId);
		label = findViewById(R.id.label);
	}

	@Override
	public void updateLabel(int labelIndex) {
		LineChart lineChart = getChart();
		if (lineChart == null) {
			return;
		}

		LineData chartData = lineChart.getLineData();
		int dataSetCount = chartData.getDataSetCount();
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet)chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		if (dataSetCount == 1) {
			String plainText = lineChart.getAxisLeft().getFormattedLabel(labelIndex);
			SpannableString displayText = new SpannableString(plainText);
			int color = chartData.getDataSetByIndex(0).getColor();
			displayText.setSpan(new ForegroundColorSpan(color), 0, displayText.length(), SPAN_FLAG);
			label.setText(displayText);
		} else {
			String leftText = lineChart.getAxisLeft().getFormattedLabel(labelIndex);
			String rightText = lineChart.getAxisRight().getFormattedLabel(labelIndex);
			String combinedPlainText = getContext().getString(R.string.ltr_or_rtl_combine_via_comma,
					leftText, rightText);
			SpannableString displayText = new SpannableString(combinedPlainText);

			ILineDataSet startDataSet = getDataSet(chartData, true);
			ILineDataSet endDataSet = getDataSet(chartData, false);
			if (startDataSet != null && endDataSet != null) {
				int edge = leftText.length() + 1;
				displayText.setSpan(new ForegroundColorSpan(startDataSet.getColor()), 0, edge, SPAN_FLAG);
				displayText.setSpan(new ForegroundColorSpan(endDataSet.getColor()), edge,
						displayText.length(), SPAN_FLAG);
			}

			label.setText(displayText);
		}
	}

	@Nullable
	private ILineDataSet getDataSet(@NonNull LineData lineData, boolean left) {
		for (ILineDataSet dataSet : lineData.getDataSets()) {
			if (((OrderedLineDataSet) dataSet).isLeftAxis() == left) {
				return dataSet;
			}
		}
		return null;
	}
}