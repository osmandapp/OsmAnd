package net.osmand.plus.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

public class SearchTrackData {

	public static final SearchTrackData UNRESOLVED = new SearchTrackData(null, null, null, 0, 0, 0);

	@Nullable
	private final LatLon startLocation;
	@Nullable
	private final String activityName;
	@Nullable
	private final String address;
	private final float length;
	private final double uphill;
	private final double downhill;

	private SearchTrackData(@Nullable LatLon startLocation, @Nullable String activityName,
	                        @Nullable String address, float length, double uphill, double downhill) {
		this.startLocation = startLocation;
		this.activityName = activityName;
		this.address = address;
		this.length = length;
		this.uphill = uphill;
		this.downhill = downhill;
	}

	@NonNull
	public static SearchTrackData create(@NonNull OsmandApplication app, @NonNull GpxDataItem item) {
		LatLon startLocation = null;
		float length = 0;
		double uphill = 0;
		double downhill = 0;

		GpxTrackAnalysis analysis = item.getAnalysis();
		if (analysis != null) {
			KLatLon latLonStart = analysis.getLatLonStart();
			if (latLonStart != null) {
				startLocation = SharedUtil.jLatLon(latLonStart);
			}
			length = analysis.getTotalDistance();
			uphill = analysis.getDiffElevationUp();
			downhill = analysis.getDiffElevationDown();
		}
		String activityId = item.getParameter(GpxParameter.ACTIVITY_TYPE);
		RouteActivity activity = Algorithms.isEmpty(activityId)
				? null : app.getRouteActivityHelper().findRouteActivity(activityId);
		String address = item.getParameter(GpxParameter.NEAREST_CITY_NAME);

		return new SearchTrackData(startLocation, activity != null ? activity.getLabel() : null,
				address, length, uphill, downhill);
	}

	@Nullable
	public LatLon getStartLocation() {
		return startLocation;
	}

	@Nullable
	public String getActivityName() {
		return activityName;
	}

	@Nullable
	public String getAddress() {
		return address;
	}

	public float getLength() {
		return length;
	}

	public double getUphill() {
		return uphill;
	}

	public double getDownhill() {
		return downhill;
	}
}