package net.osmand.plus.charts;

import static net.osmand.plus.charts.ElevationChart.GRID_LINE_LENGTH_X_AXIS_DP;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Pair;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.plus.utils.AndroidUtils;

public class ElevationXAxisRenderer extends XAxisRenderer {
	private final LineChart mChart;

	public ElevationXAxisRenderer(LineChart mChart, ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
		super(viewPortHandler, xAxis, trans);
		this.mChart = mChart;
	}

	@Override
	public RectF getGridClippingRect() {
		RectF rectF = new RectF(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop(), mViewPortHandler.contentRight(), mViewPortHandler.contentBottom() + AndroidUtils.dpToPx(mChart.getContext(), GRID_LINE_LENGTH_X_AXIS_DP / 2));
		mGridClippingRect.set(mViewPortHandler.getContentRect());
		mGridClippingRect.inset(-mAxis.getGridLineWidth(), 0.0F);
		return rectF;
	}

	@Override
	public void renderAxisLine(Canvas c) {
		if (mXAxis.isDrawAxisLineEnabled() && mXAxis.isEnabled()) {
			mAxisLinePaint.setColor(mXAxis.getAxisLineColor());
			mAxisLinePaint.setStrokeWidth(mXAxis.getAxisLineWidth());
			mAxisLinePaint.setPathEffect(mXAxis.getAxisLineDashPathEffect());

			c.drawLine(mChart.getExtraLeftOffset(), mViewPortHandler.contentBottom(), c.getWidth() - mChart.getExtraRightOffset(), mViewPortHandler.contentBottom(), mAxisLinePaint);
		}
	}

	@Override
	protected void drawGridLine(Canvas c, float x, float y, Path gridLinePath) {
		gridLinePath.moveTo(x, mViewPortHandler.contentBottom() + AndroidUtils.dpToPx(mChart.getContext(), GRID_LINE_LENGTH_X_AXIS_DP / 2));
		gridLinePath.lineTo(x, mViewPortHandler.contentTop());
		c.drawPath(gridLinePath, mGridPaint);
		gridLinePath.reset();
	}

	@Override
	protected void drawLabels(Canvas c, float pos, MPPointF anchor) {
		float labelRotationAngleDegrees = mXAxis.getLabelRotationAngle();
		boolean centeringEnabled = mXAxis.isCenterAxisLabelsEnabled();
		float[] positions = new float[mXAxis.mEntryCount * 2];
		int i;
		for (i = 0; i < positions.length; i += 2) {
			if (centeringEnabled) {
				positions[i] = mXAxis.mCenteredEntries[i / 2];
			} else {
				positions[i] = mXAxis.mEntries[i / 2];
			}
		}

		this.mTrans.pointValuesToPixel(positions);

		for (i = 0; i < positions.length; i += 2) {
			float x = positions[i];
			if (mViewPortHandler.isInBoundsX(x)) {
				String label = mXAxis.getValueFormatter().getFormattedValue(mXAxis.mEntries[i / 2], mXAxis);
				if (mXAxis.isAvoidFirstLastClippingEnabled()) {
					float width;
					if (i / 2 == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
						width = Utils.calcTextWidth(mAxisLabelPaint, label);
						int labelEndPosition = (int) (x + (width / 2));
						if (!mViewPortHandler.isInBoundsX(labelEndPosition)) {
							x = mViewPortHandler.contentRight() - (width / 2);
						}
					} else if (i == 0) {
						width = Utils.calcTextWidth(mAxisLabelPaint, label);
						int labelStartPosition = (int) (x - (width / 2));
						if (!mViewPortHandler.isInBoundsX(labelStartPosition)) {
							x = mViewPortHandler.contentLeft() + (width / 2);
						}
					}
				}
				drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees);
			}
		}
	}

	@Override
	protected double calculateInterval(double range, int labelCount) {
		int labelCountForFullInterval = labelCount - 1;
		double interval = range / labelCountForFullInterval;
		if (mAxis.isGranularityEnabled())
			interval = interval < mAxis.getGranularity() ? mAxis.getGranularity() : interval;

		double intervalMagnitude = Utils.roundToNextSignificant(Math.pow(10, (int) Math.log10(interval)));
		int intervalSigDigit = (int) (interval / intervalMagnitude);
		if (intervalSigDigit > 5) {
			interval = Math.floor(10.0 * intervalMagnitude) == 0.0
					? interval
					: Math.floor(10.0 * intervalMagnitude);

		}
		return interval;
	}

	@Override
	protected Pair<Double, Integer> calculateNoForcedLabelCount(double interval, int n, float yMin, float yMax) {
		double first = interval == 0.0 ? 0.0 : Math.ceil(yMin / interval) * interval;
		if (mAxis.isCenterAxisLabelsEnabled()) {
			first -= interval;
		}

		double last = interval == 0.0 ? 0.0 : yMax;

		double f;
		int i;

		if (interval != 0.0 && last != first) {
			for (f = first; f <= last; f += interval) {
				++n;
			}
		} else if (last == first && n == 0) {
			n = 1;
		}

		mAxis.mEntryCount = n;

		if (mAxis.mEntries.length < n) {
			// Ensure stops contains at least numStops elements.
			mAxis.mEntries = new float[n];
		}

		for (f = first, i = 0; i < n; f += interval, ++i) {

			if (f == 0.0) // Fix for negative zero case (Where value == -0.0, and 0.0 == -0.0)
				f = 0.0;

			mAxis.mEntries[i] = (float) f;
		}
		return new Pair<>(interval, n);
	}

	@Override
	public void renderGridLines(Canvas c) {
		if (mXAxis.isDrawGridLinesEnabled() && mXAxis.isEnabled()) {
			int clipRestoreCount = c.save();
			c.clipRect(getGridClippingRect());
			if (mRenderGridLinesBuffer.length != mAxis.mEntryCount * 2) {
				mRenderGridLinesBuffer = new float[mXAxis.mEntryCount * 2];
			}

			float[] positions = mRenderGridLinesBuffer;

			for (int i = 0; i < positions.length; i += 2) {
				positions[i] = mXAxis.mEntries[i / 2];
				positions[i + 1] = mXAxis.mEntries[i / 2];
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