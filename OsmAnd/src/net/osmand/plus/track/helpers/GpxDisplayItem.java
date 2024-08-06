package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;

public class GpxDisplayItem {

	public GpxTrackAnalysis analysis;
	public GpxDisplayGroup group;

	public WptPt locationStart;
	public WptPt locationEnd;

	public double splitMetric = -1;
	public double secondarySplitMetric = -1;

	public String trackSegmentName;
	public String splitName;
	public String name;
	public String description;
	public String url;
	public Bitmap image;

	public boolean expanded;
	public boolean wasHidden = true;

	public WptPt locationOnMap;
	public GPXDataSetType[] chartTypes;
	public GPXDataSetAxisType chartAxisType = GPXDataSetAxisType.DISTANCE;
	public ChartPointLayer chartPointLayer = ChartPointLayer.GPX;

	public Matrix chartMatrix;
	public float chartHighlightPos = -1f;

	public GpxDisplayItem(@Nullable GpxTrackAnalysis analysis) {
		this.analysis = analysis;
	}

	public boolean isGeneralTrack() {
		TrackDisplayGroup trackGroup = getTrackDisplayGroup(group);
		return trackGroup != null && trackGroup.isGeneralTrack();
	}
}
