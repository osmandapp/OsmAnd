package net.osmand.plus.charts;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.data.LatLon;

import java.util.List;

public class TrackChartPoints {

	private List<LatLon> xAxisPoints;
	private LatLon highlightedPoint;
	private int segmentColor;
	private GpxFile gpx;

	public List<LatLon> getXAxisPoints() {
		return xAxisPoints;
	}

	public LatLon getHighlightedPoint() {
		return highlightedPoint;
	}

	public int getSegmentColor() {
		return segmentColor;
	}

	public GpxFile getGpx() {
		return gpx;
	}

	public void setXAxisPoints(List<LatLon> xAxisPoints) {
		this.xAxisPoints = xAxisPoints;
	}

	public void setHighlightedPoint(LatLon highlightedPoint) {
		this.highlightedPoint = highlightedPoint;
	}

	public void setSegmentColor(int segmentColor) {
		this.segmentColor = segmentColor;
	}

	public void setGpx(GpxFile gpx) {
		this.gpx = gpx;
	}
}