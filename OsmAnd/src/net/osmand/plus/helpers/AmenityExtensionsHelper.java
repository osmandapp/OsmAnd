package net.osmand.plus.helpers;

import static net.osmand.data.Amenity.MAPILLARY;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIMEDIA_COMMONS;
import static net.osmand.data.Amenity.WIKIPEDIA;
import static net.osmand.gpx.GPXUtilities.OSM_PREFIX;
import static net.osmand.shared.gpx.GpxUtilities.AMENITY_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.search.AmenitySearcher;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmenityExtensionsHelper {
	public static final double MIN_UPHILL_DOWNHILL_PERCENT_TO_SHOW = 1.0;

	private static final Log LOG = PlatformUtil.getLog(AmenityExtensionsHelper.class);

	private final OsmandApplication app;

	public AmenityExtensionsHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public Amenity findAmenity(@NonNull String nameEn, double lat, double lon) {
		List<String> names = Collections.singletonList(nameEn);
		AmenitySearcher searcher = app.getResourceManager().getAmenitySearcher();
		AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();

		Amenity requestAmenity = new Amenity();
		requestAmenity.setLocation(new LatLon(lat, lon));
		AmenitySearcher.Request request = new AmenitySearcher.Request(requestAmenity, names, true);
		return searcher.searchDetailedAmenity(request, settings);
	}

	@NonNull
	public Map<String, String> getUpdatedAmenityExtensions(@NonNull Map<String, String> extensions,
			@Nullable Amenity amenity) {
		Map<String, String> updatedExtensions = new HashMap<>();
		for (Map.Entry<String, String> entry : extensions.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key.startsWith(AMENITY_PREFIX)) {
				updatedExtensions.put(key.replace(AMENITY_PREFIX, ""), value);
			} else if (key.startsWith(OSM_PREFIX)) {
				updatedExtensions.put(key.replace(OSM_PREFIX, ""), value);
			} else {
				updatedExtensions.put(key, value);
			}
		}
		if (amenity != null) {
			updatedExtensions.putAll(amenity.getAmenityExtensions(app.getPoiTypes(), false));
		}
		return updatedExtensions;
	}

	@NonNull
	public static Map<String, String> getImagesParams(@NonNull Map<String, String> extensions) {
		Map<String, String> params = new HashMap<>();
		List<String> imageTags = Arrays.asList("image", MAPILLARY, WIKIDATA, WIKIPEDIA, WIKIMEDIA_COMMONS);
		for (String imageTag : imageTags) {
			String value = extensions.get(imageTag);
			if (!Algorithms.isEmpty(value)) {
				if (imageTag.equals("image")) {
					params.put("osm_image", getDecodedAdditionalInfo(value));
				} else {
					params.put(imageTag, getDecodedAdditionalInfo(value));
				}
			}
		}
		return params;
	}

	private static String getDecodedAdditionalInfo(@NonNull String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException | IllegalArgumentException e) {
			LOG.error(e);
		}
		return value;
	}

	@Nullable
	public static String getAmenityMetricsFormatted(@NonNull Amenity amenity, @NonNull OsmandApplication app) {
		float distMeters = getAmenityDistanceMeters(amenity);
		float upMeters = Algorithms.parseFloatSilently(amenity.getAdditionalInfo(TravelGpx.DIFF_ELEVATION_UP), 0);
		float downMeters = Algorithms.parseFloatSilently(amenity.getAdditionalInfo(TravelGpx.DIFF_ELEVATION_DOWN), 0);

		String dist = OsmAndFormatter.getFormattedDistance(distMeters, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
		String uphill = OsmAndFormatter.getFormattedDistance(upMeters, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
		String downhill = OsmAndFormatter.getFormattedDistance(downMeters, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);

		List<String> metrics = new ArrayList<>();
		if (distMeters > 0) {
			metrics.add(dist);
			if (upMeters > 0 && upMeters / distMeters * 100 > MIN_UPHILL_DOWNHILL_PERCENT_TO_SHOW) {
				metrics.add(TrkSegment.SegmentSlopeType.UPHILL.getSymbol() + uphill);
			}
			if (downMeters > 0 && downMeters / distMeters * 100 > MIN_UPHILL_DOWNHILL_PERCENT_TO_SHOW) {
				metrics.add(TrkSegment.SegmentSlopeType.DOWNHILL.getSymbol() + downhill);
			}
		}

		return metrics.isEmpty() ? null : String.join(" ", metrics);
	}

	private static float getAmenityDistanceMeters(Amenity amenity) {
		String distanceTag = amenity.getAdditionalInfo(TravelGpx.DISTANCE);
		float km = Algorithms.parseFloatSilently(distanceTag, 0);
		if (km > 0 && !distanceTag.contains(".")) {
			// Before 1 Apr 2025 distance format was MMMMM (meters, no fractional part).
			// Since 1 Apr 2025 format has been fixed to KM.D (km, 1 fractional digit).
			km /= 1000;
		}
		return km * 1000;
	}
}
