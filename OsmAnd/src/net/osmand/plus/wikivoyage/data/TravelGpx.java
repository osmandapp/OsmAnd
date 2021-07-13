package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;

import static net.osmand.GPXUtilities.*;

public class TravelGpx extends TravelArticle {

	public static final String DISTANCE = "distance";
	public static final String DIFF_ELEVATION_UP = "diff_ele_up";
	public static final String DIFF_ELEVATION_DOWN = "diff_ele_down";
	public static final String MAX_ELEVATION = "max_ele";
	public static final String MIN_ELEVATION = "min_ele";
	public static final String AVERAGE_ELEVATION = "avg_ele";
	public static final String USER = "user";
	public static final String ACTIVITY_TYPE = "route_activity_type";
	public static final String ROUTE_TRACK_POINT = "route_track_point";

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
		if(gpxFile.hasAltitude){
			analysis =  gpxFile.getAnalysis(0);
		} else {
			analysis.diffElevationDown = diffElevationDown;
			analysis.diffElevationUp = diffElevationUp;
			analysis.maxElevation = maxElevation;
			analysis.minElevation = minElevation;
			analysis.totalDistance = totalDistance;
			analysis.totalDistanceWithoutGaps = totalDistance;
			analysis.avgElevation = avgElevation;
			if (!Double.isNaN(maxElevation) || !Double.isNaN(minElevation)) {
				analysis.hasElevationData = true;
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