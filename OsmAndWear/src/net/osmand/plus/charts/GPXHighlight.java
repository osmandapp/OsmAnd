package net.osmand.plus.charts;

import com.github.mikephil.charting.highlight.Highlight;

public class GPXHighlight extends Highlight {

	private final boolean showIcon;

	public GPXHighlight(float x, int dataSetIndex, boolean showIcon) {
		super(x, Float.NaN, dataSetIndex);
		this.showIcon = showIcon;
	}

	public GPXHighlight(float x, float y, int dataSetIndex, boolean showIcon) {
		super(x, y, dataSetIndex);
		this.showIcon = showIcon;
	}

	public boolean shouldShowLocationIcon() {
		return showIcon;
	}
}
