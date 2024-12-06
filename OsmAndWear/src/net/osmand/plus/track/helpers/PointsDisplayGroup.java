package net.osmand.plus.track.helpers;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

class PointsDisplayGroup extends GpxDisplayGroup {

	public PointsDisplayGroup(@NonNull GpxFile gpxFile) {
		super(gpxFile, -1);
	}

	@Override
	public void applyName(@NonNull Context context, @NonNull String name) {
		setGpxName(name);
		setName(context.getString(R.string.gpx_selection_points, name));
	}

	@Override
	@NonNull
	public GpxDisplayItemType getType() {
		return GpxDisplayItemType.TRACK_POINTS;
	}

	@Override
	@NonNull
	protected GpxDisplayGroup newInstance(@NonNull GpxFile gpxFile) {
		return new PointsDisplayGroup(gpxFile);
	}

}
