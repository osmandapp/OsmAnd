package net.osmand.plus.wikivoyage.data;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.WptPt;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.util.Algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
		wptPt.name = amenity.getName();
		wptPt.lat = amenity.getLocation().getLatitude();
		wptPt.lon = amenity.getLocation().getLongitude();
		for (String obfTag : amenity.getAdditionalInfoKeys()) {
			String gpxTag = allowedPointObfToGpxTags.get(obfTag);
			if (gpxTag != null) {
				String amenityAdditionalInfo = amenity.getAdditionalInfo(obfTag);
				if (!Algorithms.isEmpty(amenityAdditionalInfo)) {
					wptPt.getExtensionsToWrite().put(gpxTag, amenityAdditionalInfo);
				}
			}
		}
		return wptPt;
	}

	public final static Set<String> allowedTrackGpxTags = new HashSet<>(Arrays.asList(
			"show_arrows",
			"show_start_finish",
			"split_interval",
			"split_type",
			"line_3d_visualization_by_type",
			"line_3d_visualization_wall_color_type",
			"line_3d_visualization_position_type",
			"vertical_exaggeration_scale",
			"elevation_meters",
			"color_palette",
			"color",
			"width",
			"coloring_type",
			"gpx_color" // special plain copy of osmand:color
			// "points_groups" - how to pass as a single tag - by special encode/decode ?
	));

	public final static Map<String, String> renamedObfToGpxTags = new HashMap<>();
	private final static Map<String, String> allowedPointObfToGpxTags = new HashMap<>();

	static {
		renamedObfToGpxTags.put("gpx_color", "color");

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
