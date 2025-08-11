package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxDisplayGroup.getTrackDisplayGroup;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment.SegmentSlopeType;
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

	@NonNull
	public WptPt getLabelPoint() {
		if (group instanceof TrackDisplayGroup trackDisplayGroup && trackDisplayGroup.isSplitUphillDownhill()) {
			return locationStart;
		} else {
			return locationEnd;
		}
	}

	@ColorInt
	public int getLabelColor(int trackColor, int ascColor, int descColor) {
		int color = trackColor;

		if (analysis == null) {
			return color;
		}

		SegmentSlopeType slopeType = analysis.getSegmentSlopeType();
		if (slopeType != null) {
			if (slopeType == SegmentSlopeType.UPHILL) {
				color = ascColor;
			} else if (slopeType == SegmentSlopeType.DOWNHILL) {
				color = descColor;
			}
		}
		return color;
	}

	@Nullable
	public String getLabelName(@NonNull OsmandApplication app) {
		if (analysis == null) {
			return getSplitName();
		}

		SegmentSlopeType slopeType = analysis.getSegmentSlopeType();
		Integer slopeCount = analysis.getSlopeCount();
		Double slopeValue = analysis.getSlopeValue();

		if (slopeType == null || slopeCount == null || slopeValue == null)
			return getSplitName();

		String icon = slopeType.getSymbol();


		if (slopeType == SegmentSlopeType.FLAT) {
			return app.getString(R.string.ltr_or_rtl_combine_via_space, icon, OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app));
		} else {
			String slopeCountText = slopeCount + ".";
			String slopePercentText = Math.round(slopeValue) + "%";
			return app.getString(R.string.ltr_or_rtl_triple_combine_via_space, icon, slopeCountText, slopePercentText);
		}
	}

	@Nullable
	private String getSplitName() {
		String name = splitName;
		if (name != null) {
			int ind = name.indexOf(' ');
			if (ind > 0) {
				name = name.substring(0, ind);
			}
		}
		return name;
	}
}
