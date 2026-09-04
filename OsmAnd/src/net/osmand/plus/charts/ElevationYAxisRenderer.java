package net.osmand.plus.charts;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class ElevationYAxisRenderer extends YAxisRenderer {
	private final ElevationChart mChart;

	public ElevationYAxisRenderer(ElevationChart mChart, ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
		super(viewPortHandler, yAxis, trans);
		this.mChart = mChart;
	}

	@Override
	public void renderAxisLabels(Canvas c) {
		if (mYAxis.isEnabled() && mYAxis.isDrawLabelsEnabled()) {
			float[] positions = getTransformedPositions();
			mAxisLabelPaint.setTypeface(mYAxis.getTypeface());
			mAxisLabelPaint.setTextSize(mYAxis.getTextSize());
			mAxisLabelPaint.setColor(mYAxis.getTextColor());
			float xoffset = mYAxis.getXOffset();
			float yoffset = (float) Utils.calcTextHeight(mAxisLabelPaint, "A") / 2.5F + mYAxis.getYOffset();
			YAxis.AxisDependency dependency = mYAxis.getAxisDependency();
			YAxis.YAxisLabelPosition labelPosition = mYAxis.getLabelPosition();
			float xPos = 0.0F;
			if (dependency == YAxis.AxisDependency.LEFT) {
				if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
					mAxisLabelPaint.setTextAlign(Paint.Align.RIGHT);
					xPos = mViewPortHandler.offsetLeft() - xoffset;
				} else {
					mAxisLabelPaint.setTextAlign(Paint.Align.LEFT);
					xPos = mViewPortHandler.offsetLeft() + xoffset;
				}
			} else if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
				mAxisLabelPaint.setTextAlign(Paint.Align.LEFT);
				xPos = mViewPortHandler.contentRight() + xoffset;
			} else {
				mAxisLabelPaint.setTextAlign(Paint.Align.RIGHT);
				xPos = c.getWidth() - mChart.getExtraRightOffset() - xoffset;
			}

			drawYLabels(c, xPos, positions, yoffset);
		}
	}

	@Override
	protected void drawYLabels(Canvas c, float fixedPosition, float[] positions, float offset) {
		int from = mYAxis.isDrawBottomYLabelEntryEnabled() ? 0 : 1;
		int to = mYAxis.isDrawTopYLabelEntryEnabled() ? mYAxis.mEntryCount : mYAxis.mEntryCount - 1;
		float xOffset = mYAxis.getLabelXOffset();

		LineData chartData = mChart.getLineData();
		int dataSetCount = chartData.getDataSetCount();
		LineDataSet lastDataSet = dataSetCount > 0 ? (LineDataSet) chartData.getDataSetByIndex(dataSetCount - 1) : null;
		if (lastDataSet != null && !mChart.shouldShowLastSet()) {
			dataSetCount--;
		}

		for (int i = from; i < to; ++i) {
			String leftText;
			if (dataSetCount == 1) {
				leftText = this.mChart.getAxisLeft().getFormattedLabel(i);
				if (lastDataSet instanceof OrderedLineDataSet) {
					leftText = ((OrderedLineDataSet) lastDataSet).isLeftAxis() ? leftText : mChart.getAxisRight().getFormattedLabel(i);
				}
				int color = ((ILineDataSet) chartData.getDataSetByIndex(0)).getColor();
				mAxisLabelPaint.setColor(color);
				c.drawText(leftText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, mAxisLabelPaint);
			} else {
				leftText = mChart.getAxisLeft().getFormattedLabel(i) + ", ";
				String rightText = mChart.getAxisRight().getFormattedLabel(i);
				ILineDataSet startDataSet = getDataSet(chartData, true);
				ILineDataSet endDataSet = getDataSet(chartData, false);
				float rightTextWidth = mAxisLabelPaint.measureText(rightText);
				if (startDataSet != null && endDataSet != null) {
					int leftTextColor = startDataSet.getColor();
					int rightTextColor = endDataSet.getColor();
					if (startDataSet instanceof OrderedLineDataSet) {
						if (((OrderedLineDataSet) startDataSet).isLeftAxis()) {
							leftTextColor = startDataSet.getColor();
							rightTextColor = endDataSet.getColor();
						} else {
							leftTextColor = endDataSet.getColor();
							rightTextColor = startDataSet.getColor();
						}
					}
					mAxisLabelPaint.setColor(rightTextColor);
					c.drawText(rightText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, mAxisLabelPaint);
					mAxisLabelPaint.setColor(leftTextColor);
					c.drawText(leftText, fixedPosition + xOffset - rightTextWidth, positions[i * 2 + 1] + offset, mAxisLabelPaint);
				} else {
					c.drawText(rightText, fixedPosition + xOffset, positions[i * 2 + 1] + offset, mAxisLabelPaint);
					c.drawText(leftText, fixedPosition + xOffset - rightTextWidth, positions[i * 2 + 1] + offset, mAxisLabelPaint);
				}
			}
		}
	}

	@Nullable
	private ILineDataSet getDataSet(@NonNull LineData lineData, boolean firstSet) {
		if (lineData.getDataSets().size() == 1) {
			return lineData.getDataSetByIndex(0);
		} else if (lineData.getDataSets().size() > 1) {
			return lineData.getDataSetByIndex(firstSet ? 0 : 1);
		}
		return null;
	}

	@Override
	public RectF getGridClippingRect() {
		RectF rectF = new RectF(mChart.getExtraLeftOffset(), mChart.getExtraTopOffset(), mChart.getWidth() - mChart.getExtraRightOffset(), mChart.getHeight() - mChart.getExtraBottomOffset());
		mGridClippingRect.set(rectF);
		mGridClippingRect.inset(0.0F, -mAxis.getGridLineWidth());
		return mGridClippingRect;
	}

	@Override
	public void renderGridLines(Canvas c) {
		if (mYAxis.isEnabled()) {
			if (mYAxis.isDrawGridLinesEnabled()) {
				float[] positions = getTransformedPositions();
				mGridPaint.setColor(mYAxis.getGridColor());
				mGridPaint.setStrokeWidth(mYAxis.getGridLineWidth());
				mGridPaint.setPathEffect(mYAxis.getGridDashPathEffect());
				Path gridLinePath = mRenderGridLinesPath;
				gridLinePath.reset();
				int start = mChart.isDrawBottomYGridLine() ? 0 : 2;

				for (int i = start; i < positions.length; i += 2) {
					c.drawPath(linePath(gridLinePath, i, positions), mGridPaint);
					gridLinePath.reset();
				}
			}

			if (mYAxis.isDrawZeroLineEnabled()) {
				drawZeroLine(c);
			}
		}
	}

	@Override
	protected Path linePath(Path p, int i, float[] positions) {
		float y = positions[i + 1];
		float correctedY = y;
		float paintWidth = mGridPaint.getStrokeWidth();
		if (y + paintWidth > mViewPortHandler.contentBottom()) {
			correctedY = mViewPortHandler.contentBottom() - paintWidth;
		} else if (y - paintWidth < mViewPortHandler.contentTop()) {
			correctedY = mViewPortHandler.contentTop() + paintWidth;
		}

		p.moveTo(mChart.getExtraLeftOffset(), correctedY);
		p.lineTo(mChart.getWidth() - mChart.getExtraRightOffset(), correctedY);
		return p;
	}
}