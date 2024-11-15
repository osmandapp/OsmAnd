package net.osmand.plus.wikivoyage.data;

import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.shared.gpx.GpxUtilities.POINT_ELEVATION;

import net.osmand.shared.gpx.primitives.WptPt;

import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_CATEGORY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

import net.osmand.shared.gpx.GpxTrackAnalysis;

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
	public GpxTrackAnalysis getAnalysis() {
		GpxTrackAnalysis analysis = new GpxTrackAnalysis();
		if (gpxFile.hasAltitude()) {
			analysis = gpxFile.getAnalysis(0);
		} else {
			analysis.setDiffElevationDown(diffElevationDown);
			analysis.setDiffElevationUp(diffElevationUp);
			analysis.setMaxElevation(maxElevation);
			analysis.setMinElevation(minElevation);
			analysis.setTotalDistance(totalDistance);
			analysis.setTotalDistanceWithoutGaps(totalDistance);
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
		wptPt.setLat(amenity.getLocation().getLatitude());
		wptPt.setLon(amenity.getLocation().getLongitude());
		wptPt.setName(amenity.getName());
		for (String obfTag : amenity.getAdditionalInfoKeys()) {
			String value = amenity.getAdditionalInfo(obfTag);
			if (!Algorithms.isEmpty(value)) {
				String gpxTag = allowedPointObfToGpxTags.get(obfTag);
				if (gpxTag != null) {
					wptPt.getExtensionsToWrite().put(gpxTag, value);
				}
				if (OBF_POINTS_GROUPS_CATEGORY.equals(obfTag)) {
					wptPt.setCategory(value);
				}
			}
		}
		return wptPt;
	}

	private final static Map<String, String> allowedPointObfToGpxTags = new HashMap<>();

	static {
		allowedPointObfToGpxTags.put("color", "color");
		allowedPointObfToGpxTags.put("gpx_icon", "icon");
		allowedPointObfToGpxTags.put("gpx_bg", "background");
	}

	@NonNull
	@Override
	public String getPointFilterString() {
		return ROUTE_TRACK_POINT;
	}

	@NonNull
	@Override
	public String getMainFilterString() {
		return ROUTE_TRACK;
	}
}
