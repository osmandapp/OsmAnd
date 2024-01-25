package net.osmand.plus.track.helpers;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

class PointsDisplayGroup extends GpxDisplayGroup {

	public PointsDisplayGroup(@NonNull GPXFile gpxFile) {
		super(gpxFile, -1);
	}

	@Override
	public void applyName(@NonNull Context context, @NonNull String name) {
		setGpxName(name);
		setName(context.getString(R.string.gpx_selection_points, name));
	}

	@Override
	public GpxDisplayItemType getType() {
		return GpxDisplayItemType.TRACK_POINTS;
	}

	@Override
	protected GpxDisplayGroup newInstance(@NonNull GPXFile gpxFile, int index) {
		return new PointsDisplayGroup(gpxFile);
	}

}
