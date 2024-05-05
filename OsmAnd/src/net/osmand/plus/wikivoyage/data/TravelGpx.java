package net.osmand.plus.wikivoyage.data;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.WptPt;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.gpx.GPXTrackAnalysis;

public class TravelGpx extends TravelArticle {

	public static final String DISTANCE = "distance";
	public static final String DIFF_ELEVATION_UP = "diff_ele_up";
	public static final String DIFF_ELEVATION_DOWN = "diff_ele_down";
	public static final String MAX_ELEVATION = "max_ele";
	public static final String MIN_ELEVATION = "min_ele";
	public static final String AVERAGE_ELEVATION = "avg_ele";
	public static final String ROUTE_RADIUS = "route_radius";
	public static final String USER = "user";
	public static final String ACTIVITY_TYPE = "route_activity_type";

	public String user;
	public String activityType;
	public float totalDistance;
	public double diffElevationUp;
	public double diffElevationDown;
	public double maxElevation = Double.NaN;
	public double minElevation = Double.NaN;
	public double avgElevation;

	@Nullable
	@Override
	public GPXTrackAnalysis getAnalysis() {
		GPXTrackAnalysis analysis = new GPXTrackAnalysis();
		if (gpxFile.hasAltitude) {
			analysis = gpxFile.getAnalysis(0);
		} else {
			analysis.setDiffElevationDown(diffElevationDown);
			analysis.setDiffElevationUp(diffElevationUp);
			analysis.setMaxElevation(maxElevation);
			analysis.setMinElevation(minElevation);
			analysis.setTotalDistance(totalDistance);
			analysis.totalDistanceWithoutGaps = totalDistance;
			analysis.setAvgElevation(avgElevation);

			if (!Double.isNaN(maxElevation) || !Double.isNaN(minElevation)) {
				analysis.setHasData(POINT_ELEVATION, true);
			}
		}
		return analysis;
	}

	@NonNull
	@Override
	public WptPt createWptPt(@NonNull Amenity amenity, @Nullable String lang) {
		WptPt wptPt = new WptPt();
		wptPt.lat = amenity.getLocation().getLatitude();
		wptPt.lon = amenity.getLocation().getLongitude();
		wptPt.name = amenity.getName();
		return wptPt;
	}

	@NonNull
	@Override
	public String getPointFilterString() {
		return ROUTE_TRACK_POINT;
	}
}