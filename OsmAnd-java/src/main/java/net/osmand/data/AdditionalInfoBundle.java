package net.osmand.data;

import static net.osmand.data.Amenity.ALT_NAME_WITH_LANG_PREFIX;
import static net.osmand.data.Amenity.COLLAPSABLE_PREFIX;
import static net.osmand.data.Amenity.LANG_YES;
import static net.osmand.data.Amenity.ROUTE;
import static net.osmand.data.Amenity.SUBTYPE;
import static net.osmand.data.Amenity.TYPE;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIMEDIA_COMMONS;
import static net.osmand.data.Amenity.WIKI_PHOTO;
import static net.osmand.shared.gpx.GpxUtilities.*;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.shared.util.MergeLocalizedTagsAlgorithm;
import net.osmand.shared.util.PoiAdditionalLangLookup;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
					result.put(key, get(origKey));
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

	public Map<String, Object> getVisibleTagInfo(boolean allowNoteTag) {
		boolean showDefaultTags = isDefaultForCategory();
		PoiCategory category = getCategory();
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : getFilteredLocalizedInfo().entrySet()) {
			String key = entry.getKey();
			if (!shouldDisplayKey(key) || isKeyToSkip(key)) {
				continue;
			}
			Object value = entry.getValue();
			if (!allowNoteTag && "note".equals(key) && value instanceof String) {
				continue;
			}
			String strValue = value instanceof String str ? str : null;

			ResolvedPoiType resolved = resolvePoiType(category, key, strValue);
			if (resolved.pType != null && resolved.pType.isFilterOnly()) {
				continue;
			}
			if (resolved.pType == null && resolved.poiType == null && !showDefaultTags) {
				continue;
			}
			if (value instanceof Map) {
				value = filterLocalizations((Map<String, Object>) value);
				if (value == null) {
					continue;
				}
			}
			result.put(key, value);
		}
		for (Map.Entry<String, String> entry : getFilteredInfo().entrySet()) {
			if (entry.getKey().startsWith(COLLAPSABLE_PREFIX)) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	private static final String CUISINE_INFO_ID = COLLAPSABLE_PREFIX + Amenity.CUISINE;
	private static final String DISH_INFO_ID = COLLAPSABLE_PREFIX + Amenity.DISH;

	public List<AmenityRowData> getVisibleTags(boolean allowNoteTag) {
		PoiCategory category = getCategory();
		Map<String, Object> filteredInfo = getVisibleTagInfo(allowNoteTag);
		List<AmenityRowData> rows = new ArrayList<>();
		Map<String, List<PoiType>> collectedPoiTypes = new LinkedHashMap<>();
		AmenityRowData cuisineRow = null;

		for (Map.Entry<String, Object> entry : filteredInfo.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith(COLLAPSABLE_PREFIX)) {
				continue;
			}
			Object value = entry.getValue();
			if (value instanceof String strValue) {
				ResolvedPoiType resolved = resolvePoiType(category, key, strValue);
				PoiType pType = resolved.pType;
				PoiType poiType = resolved.poiType;
				if (pType != null && !pType.isText() && !Algorithms.isEmpty(pType.getPoiAdditionalCategory())) {
					continue;
				}
				if (pType != null) {
					AmenityRowData row = new AmenityRowData.Builder(key).setValue(strValue).setOrder(pType.getOrder()).build();
					if (Amenity.CUISINE.equals(key)) {
						cuisineRow = row;
					} else {
						rows.add(row);
					}
				} else if (poiType != null) {
					String categoryKey = poiType.getCategory().getKeyName();
					if (!MapPoiTypes.OTHER_MAP_CATEGORY.equals(categoryKey)) {
						collectedPoiTypes.computeIfAbsent(categoryKey, c -> new ArrayList<>()).add(poiType);
					}
				} else {
					rows.add(new AmenityRowData.Builder(key).setValue(strValue).setOrder(PoiType.DEFAULT_ORDER).build());
				}
			} else if (value instanceof Map) {
				AmenityRowData row = toLocalizedAmenityRowData(category, key, value);
				if (row != null) {
					rows.add(row);
				}
			}
		}

		if (cuisineRow != null && !containsAny(CUISINE_INFO_ID, DISH_INFO_ID)) {
			rows.add(cuisineRow);
		}

		for (Map.Entry<String, Object> entry : filteredInfo.entrySet()) {
			String key = entry.getKey();
			if (!key.startsWith(COLLAPSABLE_PREFIX)) {
				continue;
			}
			String rawValue = (String) entry.getValue();
			if (Algorithms.isEmpty(rawValue)) {
				continue;
			}
			List<PoiType> categoryTypes = new ArrayList<>();
			for (String record : rawValue.split(Amenity.SEPARATOR)) {
				AbstractPoiType type = poiTypes.getPoiAdditionalType(category, record);
				if (type == null) {
					type = poiTypes.getAnyPoiAdditionalTypeByKey(record);
				}
				if (type instanceof PoiType pt) {
					categoryTypes.add(pt);
				}
			}
			if (categoryTypes.isEmpty()) {
				continue;
			}
			String poiAdditionalCategoryName = categoryTypes.get(0).getPoiAdditionalCategory();
			// NB: intentionally comparing the collapsable_-prefixed key against bare Amenity.CUISINE/DISH,
			// which never matches - reproduces a pre-existing bug from the pre-split code so this row
			// stays byte-for-byte identical to the committed baseline (AmenityUIHelperSnapshotTest).
			boolean cuisineOrDish = key.equals(Amenity.CUISINE) || key.equals(Amenity.DISH);
			rows.add(new AmenityRowData.Builder(poiAdditionalCategoryName)
					.setCollapsableRowType(AmenityRowData.CollapsableRowType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(categoryTypes)
					.setCollapsableCategory(category)
					.setCollapsableExtraRow(cuisineOrDish ? cuisineRow : null)
					.setPoiAdditional(true)
					.setOrder(categoryTypes.get(0).getOrder())
					.build());
		}

		for (List<PoiType> poiTypeList : collectedPoiTypes.values()) {
			PoiCategory groupCategory = category;
			for (PoiType pt : poiTypeList) {
				groupCategory = pt.getCategory();
			}
			rows.add(new AmenityRowData.Builder(groupCategory.getKeyName())
					.setCollapsableRowType(AmenityRowData.CollapsableRowType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(poiTypeList)
					.setCollapsableCategory(category)
					.setPoiAdditional(false)
					.setOrder(PoiType.DEFAULT_GROUP_ORDER)
					.build());
		}

		return rows;
	}

	public Map<String, Object> getVisibleTagsAsMap(boolean allowNoteTag) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<AmenityRowData> infoRows = getVisibleTags(allowNoteTag);
		AmenityRowsBuilder.sortByOrderThenName(infoRows);
		for (AmenityRowData row : infoRows) {
			result.put(row.key, toMapValue(row));
		}
		return result;
	}

	private Object toMapValue(AmenityRowData row) {
		if (row.collapsableRowType == AmenityRowData.CollapsableRowType.POI_TYPE_GROUP) {
			StringBuilder sb = new StringBuilder();
			for (PoiType pt : row.collapsablePoiTypes) {
				if (!sb.isEmpty()) {
					sb.append(Amenity.SEPARATOR);
				}
				sb.append(pt.getKeyName());
			}
			return sb.toString();
		}
		if (!Algorithms.isEmpty(row.collapsableRows)) {
			Map<String, String> localizations = new LinkedHashMap<>();
			for (AmenityRowData child : row.collapsableRows) {
				localizations.put(child.key, child.value);
			}
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("localizations", localizations);
			return wrapper;
		}
		return row.value;
	}

	@SuppressWarnings("unchecked")
	private AmenityRowData toLocalizedAmenityRowData(PoiCategory category, String key, Object value) {
		Object localizationsObj = ((Map<String, Object>) value).get("localizations");
		if (!(localizationsObj instanceof Map)) {
			return null;
		}
		List<AmenityRowData> children = new ArrayList<>();
		for (Map.Entry<String, String> loc : ((Map<String, String>) localizationsObj).entrySet()) {
			children.add(new AmenityRowData.Builder(loc.getKey()).setValue(loc.getValue()).build());
		}
		if (children.isEmpty()) {
			return null;
		}
		PoiType pType = resolvePoiType(category, key, null).pType;
		int order = pType != null ? pType.getOrder() : PoiType.DEFAULT_ORDER;
		return new AmenityRowData.Builder(key).setCollapsableRows(children).setOrder(order).build();
	}

	private boolean isDefaultForCategory() {
		PoiCategory category = getCategory();
		String subtype = get(SUBTYPE);
		if (category == null || Algorithms.isEmpty(subtype)) {
			return false;
		}
		PoiType poiType = category.getPoiTypeByKeyName(subtype);
		return poiType != null && poiType.isDefaultForCategory();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> filterLocalizations(Map<String, Object> value) {
		Object localizationsObj = value.get("localizations");
		if (!(localizationsObj instanceof Map)) {
			return value;
		}
		Map<String, String> filtered = new LinkedHashMap<>();
		boolean anySkipped = false;
		for (Map.Entry<String, String> loc : ((Map<String, String>) localizationsObj).entrySet()) {
			if (isKeyToSkip(loc.getKey())) {
				anySkipped = true;
			} else {
				filtered.put(loc.getKey(), loc.getValue());
			}
		}
		if (anySkipped) {
			filtered.keySet().removeIf(key -> !key.contains(":"));
		}
		if (filtered.isEmpty()) {
			return null;
		}
		Map<String, Object> result = new HashMap<>();
		result.put("localizations", filtered);
		return result;
	}

	private boolean shouldDisplayKey(String key) {
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

	public PoiCategory getCategory() {
		PoiCategory poiCategory = null;
		if (additionalInfo != null) {
			String typeTag = additionalInfo.get(TYPE);
			if (!Algorithms.isEmpty(typeTag)) {
				poiCategory = MapPoiTypes.getDefault().getPoiCategoryByName(typeTag);
			}
			if (poiCategory == null) {
				poiCategory = MapPoiTypes.getDefault().getOtherPoiCategory();
			}
		}
		return poiCategory;
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

	public PoiType getPoiAdditionalType(String key, String vl) {
		AbstractPoiType pt = poiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (pt == null && !Algorithms.isEmpty(vl) && vl.length() < 50) {
			pt = poiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + vl);
		}
		return pt instanceof PoiType poiType ? poiType : null;
	}

	public ResolvedPoiType resolvePoiType(PoiCategory category, String key, String vl) {
		PoiType pType = getPoiAdditionalType(key, vl);
		PoiType poiType = category != null ? category.getPoiTypeByKeyName(key) : null;
		if (poiType == null && pType == null) {
			poiType = poiTypes.getPoiTypeByKey(key);
		}
		if (pType == null) {
			String altKey = key.replace(':', '_');
			pType = getPoiAdditionalType(altKey, vl);
			poiType = category != null ? category.getPoiTypeByKeyName(altKey) : null;
			if (poiType == null && pType == null) {
				poiType = poiTypes.getPoiTypeByKey(altKey);
			}
		}
		return new ResolvedPoiType(pType, poiType);
	}

	public boolean isKeyToSkip(String key) {
		return CollectionUtils.startsWithAny(key, COLLAPSABLE_PREFIX, ALT_NAME_WITH_LANG_PREFIX, LANG_YES)
				|| CollectionUtils.equalsToAny(key, WIKI_PHOTO, WIKIDATA, WIKIMEDIA_COMMONS, "image", "mapillary", "subway_region")
				|| MapObject.isNameLangTag(key)
				|| key.contains(ROUTE);
	}

	public static final class ResolvedPoiType {
		public final PoiType pType;
		public final PoiType poiType;

		private ResolvedPoiType(PoiType pType, PoiType poiType) {
			this.pType = pType;
			this.poiType = poiType;
		}
	}
}
