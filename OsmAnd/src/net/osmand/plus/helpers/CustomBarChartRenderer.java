package net.osmand.plus.helpers;

import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;

import net.osmand.AndroidUtils;

public class CustomBarChartRenderer extends HorizontalBarChartRenderer {
	private float highlightHalfWidth;

	public CustomBarChartRenderer(@NonNull BarChart chart) {
		this(chart, AndroidUtils.dpToPx(chart.getContext(), 1f) / 2f);
	}

	public CustomBarChartRenderer(@NonNull BarChart chart, float highlightHalfWidth) {
		super(chart, chart.getAnimator(), chart.getViewPortHandler());
		this.highlightHalfWidth = highlightHalfWidth;
	}

	@Override
	protected void setHighlightDrawPos(Highlight high, RectF bar) {
		bar.left = high.getDrawX() - highlightHalfWidth;
		bar.right = high.getDrawX() + highlightHalfWidth;
	}
}
