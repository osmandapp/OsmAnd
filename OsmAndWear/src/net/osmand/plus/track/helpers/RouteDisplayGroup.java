package net.osmand.plus.track.helpers;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;

class RouteDisplayGroup extends GpxDisplayGroup {

	public RouteDisplayGroup(@NonNull GpxFile gpxFile, int routeIndex) {
		super(gpxFile, routeIndex);
	}

	@Override
	public void applyName(@NonNull Context context, @NonNull String name) {
		setGpxName(name);
		int routeIndex = getIndex();
		GpxFile gpxFile = getGpxFile();
		String routeIndexStr = routeIndex == -1 || gpxFile.getRoutes().size() == 1 ? "" : String.valueOf(routeIndex + 1);
		setName(context.getString(R.string.gpx_selection_route_points, name, routeIndexStr));
	}

	@Override
	@NonNull
	public GpxDisplayItemType getType() {
		return GpxDisplayItemType.TRACK_ROUTE_POINTS;
	}

	@Override
	@NonNull
	protected GpxDisplayGroup newInstance(@NonNull GpxFile gpxFile) {
		return new RouteDisplayGroup(gpxFile, getIndex());
	}
}
