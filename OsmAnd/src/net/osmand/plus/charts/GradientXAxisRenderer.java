package net.osmand.plus.charts;

import android.graphics.Canvas;
import android.graphics.Path;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class GradientXAxisRenderer extends ElevationXAxisRenderer {
	private final GradientChart mChart;

	public GradientXAxisRenderer(GradientChart mChart, ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
		super(mChart, viewPortHandler, xAxis, trans);
		this.mChart = mChart;
	}

	@Override
	protected void drawLabels(Canvas c, float pos, MPPointF anchor) {
		float labelRotationAngleDegrees = mXAxis.getLabelRotationAngle();
		float splitInterval = (mXAxis.mAxisMaximum - mXAxis.mAxisMinimum) / (float) (mXAxis.mEntryCount - 1);
		float[] positions = new float[mXAxis.mEntryCount * 2];
		for (int i = 0; i < positions.length; i += 2) {
			positions[i] = mXAxis.mAxisMinimum + ((float) (i / 2) * splitInterval);
		}

		this.mTrans.pointValuesToPixel(positions);

		for (int i = 0; i < positions.length; i += 2) {
			float x = positions[i];
			if (mViewPortHandler.isInBoundsX(x)) {
				String label = mXAxis.getValueFormatter().getFormattedValue(mXAxis.mEntries[i / 2], mXAxis);
				if (mXAxis.isAvoidFirstLastClippingEnabled()) {
					float width;
					if (i / 2 == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
						width = Utils.calcTextWidth(mAxisLabelPaint, label);
						x -= width / 2.0F;
					} else if (i == 0) {
						width = Utils.calcTextWidth(mAxisLabelPaint, label);
						x += width / 2.0F;
					}
				}
				drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees);
			}
		}
	}

	@Override
	protected void computeAxisValues(float min, float max) {
		float yMin = min;
		float yMax = max;
		if (mChart.getLineData() != null) {
			DataSet dataSet = (DataSet) mChart.getLineData().getDataSetByIndex(0);
			int labelCount = dataSet.getEntries().size();
			double range = Math.abs(yMax - yMin);

			if (labelCount == 0 || range <= 0 || Double.isInfinite(range)) {
				mAxis.mEntries = new float[]{};
				mAxis.mCenteredEntries = new float[]{};
				mAxis.mEntryCount = 0;
				return;
			}

			mAxis.mEntryCount = labelCount;
			mAxis.mEntries = new float[labelCount];
			mAxis.mCenteredEntries = new float[labelCount];
			for (int i = 0; i < dataSet.getEntries().size(); i++) {
				Entry entry = (Entry) dataSet.getEntries().get(i);
				mAxis.mEntries[i] = entry.getX();
				mAxis.mCenteredEntries[i] = entry.getX();
			}
		}
		computeSize();
	}

	@Override
	public void renderGridLines(Canvas c) {
		if (mXAxis.isDrawGridLinesEnabled() && mXAxis.isEnabled()) {
			int clipRestoreCount = c.save();
			c.clipRect(getGridClippingRect());
			float splitInterval = (mXAxis.mAxisMaximum - mXAxis.mAxisMinimum) / (float) (mXAxis.mEntryCount - 1);
			float[] positions = new float[mXAxis.mEntryCount * 2];
			for (int i = 0; i < positions.length; i += 2) {
				positions[i] = mXAxis.mAxisMinimum + ((float) (i / 2) * splitInterval);
			}

			mTrans.pointValuesToPixel(positions);
			setupGridPaint();
			Path gridLinePath = mRenderGridLinesPath;
			gridLinePath.reset();

			for (int i = 0; i < positions.length; i += 2) {
				float x = positions[i];
				if (i / 2 == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
					x = x - mXAxis.getGridLineWidth() / 2;
				} else if (i == 0) {
					x = x + mXAxis.getGridLineWidth() / 2;
				}
				drawGridLine(c, x, positions[i + 1], gridLinePath);
			}
			c.restoreToCount(clipRestoreCount);
		}
	}
}