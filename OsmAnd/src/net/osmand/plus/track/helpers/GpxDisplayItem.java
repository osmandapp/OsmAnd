package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;

public class GpxDisplayItem {

	public GPXTrackAnalysis analysis;
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

	public GpxDisplayItem() {
	}

	public GpxDisplayItem(@NonNull GPXTrackAnalysis analysis) {
		this.analysis = analysis;
	}

	public boolean isGeneralTrack() {
		TrackDisplayGroup trackGroup = getTrackDisplayGroup(group);
		return trackGroup != null && trackGroup.isGeneralTrack();
	}
}
