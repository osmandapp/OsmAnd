package net.osmand.plus.charts;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class OsmYAxisRenderer extends YAxisRenderer {
	private final ElevationChart mChart;

	public OsmYAxisRenderer(ElevationChart mChart, ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
		super(viewPortHandler, yAxis, trans);
		this.mChart = mChart;
	}

	@Override
	public void renderAxisLabels(Canvas c) {
		if (this.mYAxis.isEnabled() && this.mYAxis.isDrawLabelsEnabled()) {
			float[] positions = this.getTransformedPositions();
			this.mAxisLabelPaint.setTypeface(this.mYAxis.getTypeface());
			this.mAxisLabelPaint.setTextSize(this.mYAxis.getTextSize());
			this.mAxisLabelPaint.setColor(this.mYAxis.getTextColor());
			float xoffset = this.mYAxis.getXOffset();
			float yoffset = (float) Utils.calcTextHeight(this.mAxisLabelPaint, "A") / 2.5F + this.mYAxis.getYOffset();
			YAxis.AxisDependency dependency = this.mYAxis.getAxisDependency();
			YAxis.YAxisLabelPosition labelPosition = this.mYAxis.getLabelPosition();
			float xPos = 0.0F;
			if (dependency == YAxis.AxisDependency.LEFT) {
				if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
					this.mAxisLabelPaint.setTextAlign(Paint.Align.RIGHT);
					xPos = this.mViewPortHandler.offsetLeft() - xoffset;
				} else {
					this.mAxisLabelPaint.setTextAlign(Paint.Align.LEFT);
					xPos = this.mViewPortHandler.offsetLeft() + xoffset;
				}
			} else if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
				this.mAxisLabelPaint.setTextAlign(Paint.Align.LEFT);
				xPos = this.mViewPortHandler.contentRight() + xoffset;
			} else {
				this.mAxisLabelPaint.setTextAlign(Paint.Align.RIGHT);
				xPos = c.getWidth() - mChart.getExtraRightOffset() - xoffset;
			}

			this.drawYLabels(c, xPos, positions, yoffset);
		}
	}

	@Override
	protected void drawYLabels(Canvas c, float fixedPosition, float[] positions, float offset) {
		int from = this.mYAxis.isDrawBottomYLabelEntryEnabled() ? 0 : 1;
		int to = this.mYAxis.isDrawTopYLabelEntryEnabled() ? this.mYAxis.mEntryCount : this.mYAxis.mEntryCount - 1;
		float xOffset = this.mYAxis.getLabelXOffset();

		LineData chartData = mChart.getLineData();
		int dataSetCount = chartData.getDataSetCount();
		OrderedLineDataSet lastDataSet = dataSetCount > 0 ? (OrderedLineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && lastDataSet.getDataSetType() == GPXDataSetType.ALTITUDE_EXTRM) {
			dataSetCount--;
		}
		for (int i = from; i < to; ++i) {
			if (dataSetCount == 1) {
				String plainText = mChart.getAxisRight().getFormattedLabel(i);
				int color = chartData.getDataSetByIndex(0).getColor();
				mAxisLabelPaint.setColor(color);
				c.drawText(plainText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, this.mAxisLabelPaint);
			} else {
				String leftText = mChart.getAxisLeft().getFormattedLabel(i) + ", ";
				String rightText = mChart.getAxisRight().getFormattedLabel(i);
				ILineDataSet startDataSet = getDataSet(chartData, true);
				ILineDataSet endDataSet = getDataSet(chartData, false);
				float rightTextWidth = mAxisLabelPaint.measureText(rightText);
				if (startDataSet != null && endDataSet != null) {
					mAxisLabelPaint.setColor(endDataSet.getColor());
					c.drawText(rightText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, mAxisLabelPaint);
					mAxisLabelPaint.setColor(startDataSet.getColor());
					c.drawText(leftText, fixedPosition + xOffset - rightTextWidth, positions[i * 2 + 1] + offset, mAxisLabelPaint);
				} else {
					c.drawText(rightText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, mAxisLabelPaint);
					c.drawText(leftText, fixedPosition + xOffset - rightTextWidth, positions[i * 2 + 1] + offset, mAxisLabelPaint);
				}
			}
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

	@Override
	public RectF getGridClippingRect() {
		RectF rectF = new RectF(mChart.getExtraLeftOffset(), mChart.getExtraTopOffset(), mChart.getWidth() - mChart.getExtraRightOffset(), mChart.getHeight() - mChart.getExtraBottomOffset());
		this.mGridClippingRect.set(rectF);
		this.mGridClippingRect.inset(0.0F, -this.mAxis.getGridLineWidth());
		return this.mGridClippingRect;
	}

	@Override
	public void renderGridLines(Canvas c) {
		if (this.mYAxis.isEnabled()) {
			if (this.mYAxis.isDrawGridLinesEnabled()) {
				float[] positions = this.getTransformedPositions();
				this.mGridPaint.setColor(this.mYAxis.getGridColor());
				this.mGridPaint.setStrokeWidth(this.mYAxis.getGridLineWidth());
				this.mGridPaint.setPathEffect(this.mYAxis.getGridDashPathEffect());
				Path gridLinePath = this.mRenderGridLinesPath;
				gridLinePath.reset();
				int start = this.mYAxis.isDrawBottomYGridLine() ? 0 : 2;

				for (int i = start; i < positions.length; i += 2) {
					c.drawPath(this.linePath(gridLinePath, i, positions), this.mGridPaint);
					gridLinePath.reset();
				}
			}

			if (this.mYAxis.isDrawZeroLineEnabled()) {
				this.drawZeroLine(c);
			}
		}
	}

	@Override
	protected Path linePath(Path p, int i, float[] positions) {
		float y = positions[i + 1];
		float correctedY = y;
		float paintWidth = this.mGridPaint.getStrokeWidth();
		if (y + paintWidth > this.mViewPortHandler.contentBottom()) {
			correctedY = this.mViewPortHandler.contentBottom() - paintWidth;
		} else if (y - paintWidth < this.mViewPortHandler.contentTop()) {
			correctedY = this.mViewPortHandler.contentTop() + paintWidth;
		}

		p.moveTo(mChart.getExtraLeftOffset(), correctedY);
		p.lineTo(mChart.getWidth() - mChart.getExtraRightOffset(), correctedY);
		return p;
	}
}
