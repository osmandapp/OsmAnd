package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;

public class LineChartEx extends LineChart {

	private YAxisLabelView yAxisLabelView;

	public LineChartEx(Context context) {
		super(context);
	}

	public LineChartEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LineChartEx(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setYAxisLabelView(@Nullable YAxisLabelView labelView) {
		super.setYAxisLabelView(labelView);
		this.yAxisLabelView = labelView;
	}

	protected void renderYAxisLabels(@NonNull Canvas canvas) {
		if (yAxisLabelView == null) {
			super.renderYAxisLabels(canvas);
		} else if (this.checkYAxisData()) {
			float[] positions = mAxisRendererRight.getTransformedPositions();

			for (int i = 0; i < mAxisLeft.mEntryCount; ++i) {
				yAxisLabelView.updateLabel(i);
				yAxisLabelView.measure(MeasureSpec.makeMeasureSpec(0, 0), MeasureSpec.makeMeasureSpec(0, 0));
				yAxisLabelView.layout(0, 0, yAxisLabelView.getMeasuredWidth(), yAxisLabelView.getMeasuredHeight());

				boolean bottomLabel = mAxisLeft.mAxisMinimum == mAxisLeft.mEntries[i];
				float yOffset = bottomLabel ? (float) yAxisLabelView.getHeight() : (float) yAxisLabelView.getHeight() / 2.0F;
				float y = positions[i * 2 + 1] - yOffset;
				this.yAxisLabelView.draw(canvas, 0, y);
			}
		}
	}

	private boolean checkYAxisData() {
		int enabledYAxis = 0;
		if (this.mAxisLeft.isEnabled() && this.mAxisLeft.isDrawLabelsEnabled()) {
			++enabledYAxis;
		}
		if (this.mAxisRight.isEnabled() && this.mAxisRight.isDrawLabelsEnabled()) {
			++enabledYAxis;
		}
		return this.mData != null && ((LineData) this.mData).getDataSetCount() == enabledYAxis;
	}
}
