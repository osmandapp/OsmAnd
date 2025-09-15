package net.osmand.plus.wikivoyage.data;

import static net.osmand.osm.MapPoiTypes.ROUTES_PREFIX;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL_TEXT;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.WPT_EXTRA_TAGS;
import static net.osmand.shared.gpx.GpxUtilities.POINT_ELEVATION;

import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.WptPt;

import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_CATEGORY;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_1;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.MapUtils;

public class TravelGpx extends TravelArticle {

	public static final String DISTANCE = "distance";
	public static final String MAX_ELEVATION = GpxUtilities.MAX_ELEVATION;
	public static final String MIN_ELEVATION = GpxUtilities.MIN_ELEVATION;
	public static final String AVG_ELEVATION = GpxUtilities.AVG_ELEVATION;
	public static final String DIFF_ELEVATION_UP = GpxUtilities.DIFF_ELEVATION_UP;
	public static final String DIFF_ELEVATION_DOWN = GpxUtilities.DIFF_ELEVATION_DOWN;
	public static final String START_ELEVATION = "start_ele";
	public static final String ELE_GRAPH = "ele_graph";
	public static final String ROUTE_BBOX_RADIUS = "route_bbox_radius";
	public static final String ROUTE_SHORTLINK_TILES = "route_shortlink_tiles";
	public static final String ROUTE_SEGMENT_INDEX = "route_segment_index";
	public static final String USER = "user";
	public static final String ROUTE_TYPE = "route_type";
	public static final String ROUTE_ACTIVITY_TYPE = "route_activity_type";
	public static final String TRAVEL_MAP_TO_POI_TAG = "route_id";

	public String user;
	public String activityType;
	public float totalDistance;
	public double diffElevationUp;
	public double diffElevationDown;
	public double maxElevation = Double.NaN;
	public double minElevation = Double.NaN;
	public double avgElevation;

	public boolean isSuperRoute = false;

	@Nullable
	private Amenity amenity;

	private String amenitySubType;
	private String amenityRegionName;

	public TravelGpx() {
	}

	public TravelGpx(@NonNull Amenity amenity) {
		this.amenity = amenity;
		amenitySubType = amenity.getSubType();
		amenityRegionName = amenity.getRegionName();
		String enTitle = amenity.getName("en");
		title = Algorithms.isEmpty(title) ? amenity.getName() : enTitle;
		lat = amenity.getLocation().getLatitude();
		lon = amenity.getLocation().getLongitude();
		description = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.DESCRIPTION));
		routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		user = Algorithms.emptyIfNull(amenity.getTagContent(USER));
		activityType = Algorithms.emptyIfNull(amenity.getTagContent(ROUTE_ACTIVITY_TYPE));
		ref = Algorithms.emptyIfNull(amenity.getRef());
		totalDistance = Algorithms.parseFloatSilently(amenity.getTagContent(DISTANCE), 0);
		diffElevationUp = Algorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_UP), 0);
		diffElevationDown = Algorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_DOWN), 0);
		minElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(MIN_ELEVATION), 0);
		avgElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(AVG_ELEVATION), 0);
		maxElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(MAX_ELEVATION), 0);
		String radius = amenity.getTagContent(ROUTE_BBOX_RADIUS);
		if (radius != null) {
			routeRadius = MapUtils.convertCharToDist(radius.charAt(0), TRAVEL_GPX_CONVERT_FIRST_LETTER,
					TRAVEL_GPX_CONVERT_FIRST_DIST, TRAVEL_GPX_CONVERT_MULT_1, TRAVEL_GPX_CONVERT_MULT_2);
		} else if (!Algorithms.isEmpty(routeId)) {
			routeRadius = TRAVEL_GPX_DEFAULT_SEARCH_RADIUS;
		}
		String shortLinkTiles = amenity.getTagContent(ROUTE_SHORTLINK_TILES);
		if (shortLinkTiles != null) {
			initShortLinkTiles(shortLinkTiles);
		}
		if (activityType.isEmpty()) {
			for (String key : amenity.getAdditionalInfoKeys()) {
				if (key.startsWith(ROUTE_ACTIVITY_TYPE)) {
					activityType = amenity.getTagContent(key);
				}
			}
		}
		if (!Algorithms.isEmpty(amenity.getAdditionalInfo(Amenity.ROUTE_MEMBERS_IDS))) {
			isSuperRoute = true;
		}
	}

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

	private static final Set<String> doNotSaveWptTags = Set.of("route_id", "route_name");

	@NonNull
	@Override
	public WptPt createWptPt(@NonNull Amenity amenity, @Nullable String lang) {
		WptPt wptPt = new WptPt();
		wptPt.setName(amenity.getName());
		wptPt.setLat(amenity.getLocation().getLatitude());
		wptPt.setLon(amenity.getLocation().getLongitude());

		Map<String, String> wptPtExtensions = wptPt.getExtensionsToWrite();
		amenity.getNamesMap(true).forEach((key, value) -> wptPtExtensions.put("name:" + key, value));

		String linkHref = null, linkText = null;

		for (String obfTag : amenity.getAdditionalInfoKeys()) {
			String value = amenity.getAdditionalInfo(obfTag);
			if (!Algorithms.isEmpty(value)) {
				if (OBF_POINTS_GROUPS_CATEGORY.equals(obfTag)) {
					wptPt.setCategory(value);
				} else if ("name".equals(obfTag)) {
					wptPt.setName(value);
				} else if ("description".equals(obfTag)) {
					wptPt.setDesc(value);
				} else if ("note".equals(obfTag)) {
					wptPt.setComment(value);
				} else if (TAG_URL.equals(obfTag)) {
					linkHref = value;
				} else if (TAG_URL_TEXT.equals(obfTag)) {
					linkText = value;
				} else if ("colour".equals(obfTag) && amenity.getAdditionalInfoKeys().contains("color")) {
					// ignore "colour" if "color" exists
				} else if (WPT_EXTRA_TAGS.equals(obfTag)) {
					Gson gson = new Gson();
					Type type = new TypeToken<Map<String, String>>() {}.getType();
					wptPtExtensions.putAll(gson.fromJson(value, type));
				} else if (!doNotSaveWptTags.contains(obfTag)) {
					wptPtExtensions.put(obfTag, value);
				}
			}
		}

		if (linkHref != null || linkText != null) {
			wptPt.setLink(new Link(linkHref, linkText)); // nullable href/text
		}

		return wptPt;
	}

	@NonNull
	@Override
	public String getPointFilterString() {
		return ROUTE_TRACK_POINT;
	}

	@NonNull
	@Override
	public String getMainFilterString() {
		return ROUTE_TRACK; // considered together with ROUTES_PREFIX
	}

	@Nullable
	public String getAmenitySubType() {
		return amenitySubType;
	}

	@Nullable
	public String getAmenityRegionName() {
		return amenityRegionName;
	}

	@Nullable
	public String getRouteType() {
		if (amenitySubType != null && amenitySubType.startsWith(ROUTES_PREFIX)) {
			return amenitySubType.replace(ROUTES_PREFIX, "").split(";")[0];
		}
		return null;
	}

	@Nullable
	public Amenity getAmenity() {
		return amenity;
	}
}
