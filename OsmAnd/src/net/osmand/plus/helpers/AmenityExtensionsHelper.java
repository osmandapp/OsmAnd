package net.osmand.plus.helpers;

import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.data.Amenity.NAME;
import static net.osmand.data.Amenity.OPENING_HOURS;
import static net.osmand.data.Amenity.SEPARATOR;
import static net.osmand.data.Amenity.SUBTYPE;
import static net.osmand.data.Amenity.TYPE;
import static net.osmand.gpx.GPXUtilities.AMENITY_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmenityExtensionsHelper {

	private static final String COLLAPSABLE_PREFIX = "collapsable_";
	private static final List<String> HIDING_EXTENSIONS_AMENITY_TAGS = Arrays.asList("phone", "website");

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
		Map<String, String> updatedExtensions = new HashMap<>(savedExtensions);
		if (amenityOriginName != null) {
			Amenity amenity = findAmenity(amenityOriginName, lat, lon);
			if (amenity != null) {
				updatedExtensions.putAll(getAmenityExtensions(amenity));
			}
		}
		return updatedExtensions;
	}

	public Map<String, String> getAmenityExtensions(@NonNull Amenity amenity) {
		Map<String, String> result = new HashMap<String, String>();
		Map<String, List<PoiType>> collectedPoiAdditionalCategories = new HashMap<>();

		String name = amenity.getName();
		if (name != null) {
			result.put(AMENITY_PREFIX + NAME, name);
		}
		String subType = amenity.getSubType();
		if (subType != null) {
			result.put(AMENITY_PREFIX + SUBTYPE, subType);
		}
		PoiCategory type = amenity.getType();
		if (type != null) {
			result.put(AMENITY_PREFIX + TYPE, type.getKeyName());
		}
		String openingHours = amenity.getOpeningHours();
		if (openingHours != null) {
			result.put(AMENITY_PREFIX + OPENING_HOURS, openingHours);
		}
		if (amenity.hasAdditionalInfo()) {
			result.putAll(amenity.getAdditionalInfoAndCollectCategories(app.getPoiTypes(),
					HIDING_EXTENSIONS_AMENITY_TAGS, collectedPoiAdditionalCategories, null));

			//join collected tags by category into one string
			for (Map.Entry<String, List<PoiType>> entry : collectedPoiAdditionalCategories.entrySet()) {
				String categoryName = COLLAPSABLE_PREFIX + entry.getKey();
				List<PoiType> categoryTypes = entry.getValue();
				if (categoryTypes.size() > 0) {
					StringBuilder builder = new StringBuilder();
					for (PoiType poiType : categoryTypes) {
						if (builder.length() > 0) {
							builder.append(SEPARATOR);
						}
						builder.append(poiType.getKeyName());
					}
					result.put(categoryName, builder.toString());
				}
			}
		}
		return result;
	}
}