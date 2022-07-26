package net.osmand.plus.track.helpers;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;

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

	public boolean isGeneralTrack() {
		return group != null && group.isGeneralTrack();
	}
}
