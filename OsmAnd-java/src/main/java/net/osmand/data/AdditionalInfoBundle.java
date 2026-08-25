package net.osmand.data;

import static net.osmand.data.Amenity.ALT_NAME_WITH_LANG_PREFIX;
import static net.osmand.data.Amenity.COLLAPSABLE_PREFIX;
import static net.osmand.data.Amenity.LANG_YES;
import static net.osmand.data.Amenity.NOTE;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdditionalInfoBundle {

	private static final List<String> HIDDEN_EXTENSIONS = Arrays.asList(
			COLOR_NAME_EXTENSION, ICON_NAME_EXTENSION, BACKGROUND_TYPE_EXTENSION,
			PROFILE_TYPE_EXTENSION, ADDRESS_EXTENSION, AMENITY_ORIGIN_EXTENSION,
			TYPE, SUBTYPE, ORIGIN_EXTENSION, OSM_URL_EXTENSION
	);
	public static final String LOCALIZATIONS = "localizations";

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
			Map<String, String> result = new LinkedHashMap<>();
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

	private static final String CUISINE_INFO_ID = COLLAPSABLE_PREFIX + Amenity.CUISINE;
	private static final String DISH_INFO_ID = COLLAPSABLE_PREFIX + Amenity.DISH;

	public List<AmenityTagEntry> getVisibleTags(boolean allowNoteTag, List<String> preferredLangs) {
		PoiCategory category = getCategory();
		Map<String, List<PoiType>> collectedPoiTypes = new LinkedHashMap<>();

		List<AmenityTagEntry> entries = collectPlainRows(allowNoteTag, preferredLangs, category, collectedPoiTypes);
		entries.addAll(collectCollapsableGroups(category));
		entries.addAll(collectPoiTypeGroups(category, collectedPoiTypes));
		return entries;
	}

	private List<AmenityTagEntry> collectPlainRows(boolean allowNoteTag, List<String> preferredLangs,
	                                               PoiCategory category,
	                                               Map<String, List<PoiType>> collectedPoiTypes) {
		boolean showDefaultTags = isDefaultForCategory();
		List<AmenityTagEntry> entries = new ArrayList<>();
		AmenityTagEntry cuisineEntry = null;

		for (Map.Entry<String, Object> entry : getFilteredLocalizedInfo().entrySet()) {
			String key = entry.getKey();
			if (isKeyToSkip(key) || !shouldDisplayKey(key)) {
				continue;
			}
			Object value = entry.getValue();
			if (!allowNoteTag && NOTE.equals(key) && value instanceof String) {
				continue;
			}
			String strValue = value instanceof String str ? str : null;

			ResolvedPoiType resolvedType = resolvePoiType(category, key, strValue);
			PoiType additionalType = resolvedType.additionalType();
			PoiType categoryType = resolvedType.categoryType();
			if (isFilterOnlyOrGrouped(additionalType)) {
				continue;
			}
			if (additionalType == null && categoryType == null && !showDefaultTags) {
				continue;
			}

			if (strValue != null) {
				if (additionalType != null) {
					AmenityTagEntry tagEntry = new AmenityTagEntry.Builder(key)
							.setValue(strValue)
							.setOrder(additionalType.getOrder())
							.setResolvedType(resolvedType)
							.setIsDescription(key.contains(Amenity.DESCRIPTION))
							.build();
					if (Amenity.CUISINE.equals(key)) {
						cuisineEntry = tagEntry;
					} else {
						entries.add(tagEntry);
					}
				} else if (categoryType != null) {
					String categoryKey = categoryType.getCategory().getKeyName();
					if (!MapPoiTypes.OTHER_MAP_CATEGORY.equals(categoryKey)) {
						collectedPoiTypes.computeIfAbsent(categoryKey, c -> new ArrayList<>()).add(categoryType);
					}
				} else {
					entries.add(new AmenityTagEntry.Builder(key)
							.setValue(strValue)
							.setOrder(PoiType.DEFAULT_ORDER)
							.setResolvedType(resolvedType)
							.setIsDescription(key.contains(Amenity.DESCRIPTION))
							.build());
				}
			} else if (value instanceof Map<?, ?> mapValue) {
				Map<String, String> localizations = extractLocalizations(mapValue);
				if (localizations == null) {
					continue;
				}
				AmenityTagEntry tagEntry = toLocalizedAmenityTagEntry(key, localizations, resolvedType, preferredLangs);
				if (tagEntry != null) {
					entries.add(tagEntry);
				}
			}
		}

		if (cuisineEntry != null && !containsAny(CUISINE_INFO_ID, DISH_INFO_ID)) {
			entries.add(cuisineEntry);
		}
		return entries;
	}

	private List<AmenityTagEntry> collectCollapsableGroups(PoiCategory category) {
		List<AmenityTagEntry> entries = new ArrayList<>();
		for (Map.Entry<String, String> entry : getFilteredInfo().entrySet()) {
			String key = entry.getKey();
			if (!key.startsWith(COLLAPSABLE_PREFIX)) {
				continue;
			}
			String rawValue = entry.getValue();
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
			entries.add(new AmenityTagEntry.Builder(poiAdditionalCategoryName)
					.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(categoryTypes)
					.setCollapsableCategory(category)
					.setPoiAdditional(true)
					.setOrder(categoryTypes.get(0).getOrder())
					.build());
		}
		return entries;
	}

	private List<AmenityTagEntry> collectPoiTypeGroups(PoiCategory category, Map<String,
			List<PoiType>> collectedPoiTypes) {
		List<AmenityTagEntry> entries = new ArrayList<>();
		for (List<PoiType> poiTypeList : collectedPoiTypes.values()) {
			PoiCategory groupCategory = poiTypeList.get(0).getCategory();
			entries.add(new AmenityTagEntry.Builder(groupCategory.getKeyName())
					.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(poiTypeList)
					.setCollapsableCategory(category)
					.setPoiAdditional(false)
					.setOrder(PoiType.DEFAULT_GROUP_ORDER)
					.build());
		}
		return entries;
	}

	private AmenityTagEntry toLocalizedAmenityTagEntry(String key, Map<String, String> localizations,
	                                                   ResolvedPoiType resolvedType, List<String> preferredLangs) {
		List<AmenityTagEntry> children = new ArrayList<>();
		for (Map.Entry<String, String> loc : localizations.entrySet()) {
			children.add(new AmenityTagEntry.Builder(loc.getKey()).setValue(loc.getValue()).build());
		}
		if (children.isEmpty()) {
			return null;
		}
		AmenityTagEntry header = pickHeader(children, preferredLangs);
		List<AmenityTagEntry> otherLangs = new ArrayList<>(children);
		otherLangs.remove(header);
		int order = resolvedType.additionalType() != null
				? resolvedType.additionalType().getOrder()
				: PoiType.DEFAULT_ORDER;
		return new AmenityTagEntry.Builder(header.key)
				.setValue(header.value)
				.setCollapsableEntries(otherLangs)
				.setResolvedType(resolvedType)
				.setOrder(order)
				.setIsDescription(key.contains(Amenity.DESCRIPTION))
				.build();
	}

	private AmenityTagEntry pickHeader(List<AmenityTagEntry> children, List<String> preferredLangs) {
		if (preferredLangs != null) {
			for (String lang : preferredLangs) {
				if (Algorithms.isEmpty(lang)) {
					continue;
				}
				String suffix = ":" + lang;
				for (AmenityTagEntry child : children) {
					if (child.key.endsWith(suffix)) {
						return child;
					}
				}
			}
		}
		return children.get(0);
	}

	private boolean isFilterOnlyOrGrouped(PoiType pType) {
		return pType != null && (pType.isFilterOnly()
				|| (!pType.isText() && Algorithms.isNotEmpty(pType.getPoiAdditionalCategory())));
	}

	private boolean isDefaultForCategory() {
		PoiCategory category = getCategory();
		if (category == null) {
			return false;
		}
		String subtype = get(SUBTYPE);
		if (Algorithms.isEmpty(subtype)) {
			return false;
		}
		PoiType poiType = category.getPoiTypeByKeyName(subtype);
		return poiType != null && poiType.isDefaultForCategory();
	}

	private Map<String, String> extractLocalizations(Map<?, ?> value) {
		if (!(value.get(LOCALIZATIONS) instanceof Map<?, ?> localizations)) {
			return null;
		}
		Map<String, String> filtered = new LinkedHashMap<>();
		for (Map.Entry<?, ?> loc : localizations.entrySet()) {
			if (loc.getKey() instanceof String locKey && loc.getValue() instanceof String locValue
					&& !isKeyToSkip(locKey)) {
				filtered.put(locKey, locValue);
			}
		}
		return filtered.isEmpty() ? null : filtered;
	}

	private boolean shouldDisplayKey(String key) {
		if (key.contains(Amenity.WIKIPEDIA)
				|| key.contains(Amenity.CONTENT)
				|| key.contains(Amenity.SHORT_DESCRIPTION)
				|| key.contains(MapPoiTypes.WIKI_LANG)) {
			return false;
		}
		if (MapPoiTypes.ROUTE_ARTICLE.equals(get(SUBTYPE)) && key.contains(Amenity.DESCRIPTION)) {
			return false;
		}
		AbstractPoiType t = poiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (t instanceof PoiType poiType && poiType.isHidden()) {
			return false;
		}
		return !Amenity.NAME.equals(key);
	}

	public PoiCategory getCategory() {
		PoiCategory poiCategory = null;
		if (additionalInfo != null) {
			String typeTag = additionalInfo.get(TYPE);
			if (Algorithms.isNotEmpty(typeTag)) {
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
		if (pt == null && Algorithms.isNotEmpty(vl) && vl.length() < 50) {
			pt = poiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + vl);
		}
		return pt instanceof PoiType poiType ? poiType : null;
	}

	public ResolvedPoiType resolvePoiType(PoiCategory category, String key, String vl) {
		PoiType additionalType = getPoiAdditionalType(key, vl);
		PoiType categoryType = category != null ? category.getPoiTypeByKeyName(key) : null;
		if (categoryType == null && additionalType == null) {
			categoryType = poiTypes.getPoiTypeByKey(key);
		}
		if (additionalType == null) {
			String altKey = key.replace(':', '_');
			additionalType = getPoiAdditionalType(altKey, vl);
			categoryType = category != null ? category.getPoiTypeByKeyName(altKey) : null;
			if (categoryType == null && additionalType == null) {
				categoryType = poiTypes.getPoiTypeByKey(altKey);
			}
		}
		return new ResolvedPoiType(additionalType, categoryType);
	}

	public boolean isKeyToSkip(String key) {
		return CollectionUtils.startsWithAny(key, COLLAPSABLE_PREFIX, ALT_NAME_WITH_LANG_PREFIX, LANG_YES)
				|| CollectionUtils.equalsToAny(key, WIKI_PHOTO, WIKIDATA, WIKIMEDIA_COMMONS, "image", "mapillary",
				"subway_region")
				|| MapObject.isNameLangTag(key)
				|| key.contains(ROUTE);
	}

	public record ResolvedPoiType(PoiType additionalType, PoiType categoryType) {
	}
}
