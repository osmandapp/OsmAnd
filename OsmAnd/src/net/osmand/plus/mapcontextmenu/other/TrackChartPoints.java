package net.osmand.plus.mapcontextmenu.other;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.LatLon;

import java.util.List;

public class TrackChartPoints {

	private List<LatLon> xAxisPoints;
	private LatLon highlightedPoint;
	private int segmentColor;
	private GPXFile gpx;

	public List<LatLon> getXAxisPoints() {
		return xAxisPoints;
	}

	public LatLon getHighlightedPoint() {
		return highlightedPoint;
	}

	public int getSegmentColor() {
		return segmentColor;
	}

	public GPXFile getGpx() {
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

	public void setGpx(GPXFile gpx) {
		this.gpx = gpx;
	}
}