package net.osmand.plus.charts;

import static net.osmand.plus.charts.ElevationChart.GRID_LINE_LENGTH_X_AXIS_DP;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.plus.utils.AndroidUtils;

public class OsmXAxisRenderer extends XAxisRenderer {
	private final ElevationChart mChart;

	public OsmXAxisRenderer(ElevationChart mChart, ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
		super(viewPortHandler, xAxis, trans);
		this.mChart = mChart;
	}

	@Override
	public RectF getGridClippingRect() {
		RectF rectF = new RectF(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop(), mViewPortHandler.contentRight(),
				mViewPortHandler.contentBottom() + AndroidUtils.dpToPx(mChart.getContext(), GRID_LINE_LENGTH_X_AXIS_DP / 2));
		this.mGridClippingRect.set(this.mViewPortHandler.getContentRect());
		this.mGridClippingRect.inset(-this.mAxis.getGridLineWidth(), 0.0F);
		return rectF;
	}

	@Override
	public void renderAxisLine(Canvas c) {
		if (this.mXAxis.isDrawAxisLineEnabled() && this.mXAxis.isEnabled()) {
			this.mAxisLinePaint.setColor(this.mXAxis.getAxisLineColor());
			this.mAxisLinePaint.setStrokeWidth(this.mXAxis.getAxisLineWidth());
			this.mAxisLinePaint.setPathEffect(this.mXAxis.getAxisLineDashPathEffect());

			c.drawLine(mChart.getExtraLeftOffset(), this.mViewPortHandler.contentBottom(), c.getWidth() - mChart.getExtraRightOffset(), this.mViewPortHandler.contentBottom(), this.mAxisLinePaint);
		}
	}

	@Override
	protected void drawGridLine(Canvas c, float x, float y, Path gridLinePath) {
		gridLinePath.moveTo(x, this.mViewPortHandler.contentBottom() + AndroidUtils.dpToPx(mChart.getContext(), 5f));
		gridLinePath.lineTo(x, this.mViewPortHandler.contentTop());
		c.drawPath(gridLinePath, this.mGridPaint);
		gridLinePath.reset();
	}
}
