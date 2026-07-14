package net.osmand.data;

import static net.osmand.data.Amenity.SUBTYPE;
import static net.osmand.data.Amenity.TYPE;
import static net.osmand.shared.gpx.GpxUtilities.*;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.shared.util.MergeLocalizedTagsAlgorithm;
import net.osmand.shared.util.PoiAdditionalLangLookup;
import net.osmand.util.Algorithms;
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
			TYPE, SUBTYPE, ORIGIN_EXTENSION
	);

	private final Map<String, String> additionalInfo;
	private final MapPoiTypes poiTypes;
	private final PoiAdditionalLangLookup langLookup;
	private Map<String, String> filteredAdditionalInfo = null;
	private Map<String, Object> localizedAdditionalInfo = null;

	private List<String> customHiddenExtensions;

	public AdditionalInfoBundle(MapPoiTypes poiTypes, Map<String, String> additionalInfo) {
		this.additionalInfo = additionalInfo;
		this.poiTypes = poiTypes;
		this.langLookup = key -> {
			AbstractPoiType type = poiTypes.getAnyPoiAdditionalTypeByKey(key);
			return type != null && type.getLang() != null;
		};
	}

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
					key = origKey.replace(OSM_PREFIX, "");
				}
				if (!HIDDEN_EXTENSIONS.contains(key) && (Algorithms.isEmpty(customHiddenExtensions)
						|| !customHiddenExtensions.contains(key))) {
					result.put(key, get(key));
				}
			}
			filteredAdditionalInfo = result;
		}
		return filteredAdditionalInfo;
	}

	public Map<String, Object> getFilteredLocalizedInfo() {
		if (localizedAdditionalInfo == null) {
			localizedAdditionalInfo = MergeLocalizedTagsAlgorithm.Companion.execute(langLookup, getFilteredInfo());
		}
		return localizedAdditionalInfo;
	}

	public boolean shouldDisplayKey(String key) {
		AbstractPoiType t = poiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (t instanceof PoiType poiType && poiType.isHidden()) {
			return false;
		}
		if (key.contains(Amenity.WIKIPEDIA)
				|| key.contains(Amenity.CONTENT)
				|| key.contains(Amenity.SHORT_DESCRIPTION)
				|| key.contains(MapPoiTypes.WIKI_LANG)) {
			return false;
		}
		if (MapPoiTypes.ROUTE_ARTICLE.equals(get(SUBTYPE)) && key.contains(Amenity.DESCRIPTION)) {
			return false;
		}
		return !Amenity.NAME.equals(key);
	}

	public boolean containsAny(String... keys) {
		return CollectionUtils.containsAny(getAdditionalInfoKeys(), keys);
	}

	public boolean contains(String key) {
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

	public void setCustomHiddenExtensions(List<String> customHiddenExtensions) {
		this.filteredAdditionalInfo = null;
		this.localizedAdditionalInfo = null;
		this.customHiddenExtensions = customHiddenExtensions;
	}
}
