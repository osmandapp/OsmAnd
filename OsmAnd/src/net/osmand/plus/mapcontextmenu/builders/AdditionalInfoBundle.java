package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.NAME;
import static net.osmand.data.Amenity.SUBTYPE;
import static net.osmand.data.Amenity.TYPE;
import static net.osmand.shared.gpx.GpxUtilities.ADDRESS_EXTENSION;
import static net.osmand.shared.gpx.GpxUtilities.AMENITY_ORIGIN_EXTENSION;
import static net.osmand.shared.gpx.GpxUtilities.AMENITY_PREFIX;
import static net.osmand.shared.gpx.GpxUtilities.BACKGROUND_TYPE_EXTENSION;
import static net.osmand.shared.gpx.GpxUtilities.COLOR_NAME_EXTENSION;
import static net.osmand.shared.gpx.GpxUtilities.ICON_NAME_EXTENSION;
import static net.osmand.shared.gpx.GpxUtilities.PROFILE_TYPE_EXTENSION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdditionalInfoBundle {

	private static final List<String> HIDDEN_EXTENSIONS = Arrays.asList(
			COLOR_NAME_EXTENSION, ICON_NAME_EXTENSION, BACKGROUND_TYPE_EXTENSION,
			PROFILE_TYPE_EXTENSION, ADDRESS_EXTENSION, AMENITY_ORIGIN_EXTENSION,
			NAME, TYPE, SUBTYPE
	);

	private final OsmandApplication app;
	private final Map<String, String> additionalInfo;
	private Map<String, String> filteredAdditionalInfo = null;
	private Map<String, Object> localizedAdditionalInfo = null;


	public AdditionalInfoBundle(@NonNull OsmandApplication app,
	                            @Nullable Map<String, String> additionalInfo) {
		this.app = app;
		this.additionalInfo = additionalInfo;
	}

	@NonNull
	public Map<String, Object> getFilteredLocalizedInfo() {
		if (localizedAdditionalInfo == null) {
			localizedAdditionalInfo = MergeLocalizedTagsAlgorithm.Companion.execute(app, getFilteredInfo());
		}
		return localizedAdditionalInfo;
	}

	@NonNull
	public Map<String, String> getFilteredInfo() {
		if (filteredAdditionalInfo == null) {
			Map<String, String> result = new HashMap<>();
			for (String origKey : getAdditionalInfoKeys()) {
				String key;
				if (origKey.equals(AMENITY_PREFIX + Amenity.OPENING_HOURS)) {
					key = origKey.replace(AMENITY_PREFIX, "");
				} else if (origKey.startsWith(AMENITY_PREFIX)) {
					continue;
				} else {
					key = origKey.replace(GpxUtilities.OSM_PREFIX, "");
				}
				if (!HIDDEN_EXTENSIONS.contains(key)) {
					result.put(key, get(key));
				}
			}
			filteredAdditionalInfo = result;
		}
		return filteredAdditionalInfo;
	}

	public boolean containsAny(@NonNull String ... keys) {
		return CollectionUtils.containsAny(getAdditionalInfoKeys(), keys);
	}

	public boolean contains(@NonNull String key) {
		return getAdditionalInfoKeys().contains(key);
	}

	public Collection<String> getAdditionalInfoKeys() {
		if (additionalInfo == null) {
			return Collections.emptyList();
		}
		return additionalInfo.keySet();
	}

	public String get(String key) {
		if (additionalInfo == null) {
			return null;
		}
		String str = additionalInfo.get(key);
		str = Amenity.unzipContent(str);
		return str;
	}
}
