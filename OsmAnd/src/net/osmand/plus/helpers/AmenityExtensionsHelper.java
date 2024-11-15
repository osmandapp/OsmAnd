package net.osmand.plus.helpers;

import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.data.Amenity.MAPILLARY;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIMEDIA_COMMONS;
import static net.osmand.data.Amenity.WIKIPEDIA;
import static net.osmand.shared.gpx.GpxUtilities.AMENITY_PREFIX;
import static net.osmand.gpx.GPXUtilities.OSM_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmenityExtensionsHelper {

	private static final Log LOG = PlatformUtil.getLog(AmenityExtensionsHelper.class);

	private final OsmandApplication app;

	public AmenityExtensionsHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public Amenity findAmenity(@NonNull String nameEn, double lat, double lon) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, true);

		for (Amenity amenity : amenities) {
			if (Algorithms.stringsEqual(amenity.toStringEn(), nameEn)) {
				return amenity;
			}
		}
		return null;
	}

	@NonNull
	public Map<String, String> getUpdatedAmenityExtensions(@NonNull Map<String, String> savedExtensions,
	                                                       @Nullable String amenityOriginName,
	                                                       double lat, double lon) {
		Map<String, String> updatedExtensions = new HashMap<>();
		for (Map.Entry<String, String> entry : savedExtensions.entrySet()) {
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
		if (amenityOriginName != null) {
			Amenity amenity = findAmenity(amenityOriginName, lat, lon);
			if (amenity != null) {
				updatedExtensions.putAll(amenity.getAmenityExtensions(app.getPoiTypes(), false));
			}
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
}