package net.osmand.shared.data

import net.osmand.shared.data.Amenity.Companion.ALT_NAME_WITH_LANG_PREFIX
import net.osmand.shared.data.Amenity.Companion.COLLAPSABLE_PREFIX
import net.osmand.shared.data.Amenity.Companion.LANG_YES
import net.osmand.shared.data.Amenity.Companion.NOTE
import net.osmand.shared.data.Amenity.Companion.ROUTE
import net.osmand.shared.data.Amenity.Companion.SUBTYPE
import net.osmand.shared.data.Amenity.Companion.TYPE
import net.osmand.shared.data.Amenity.Companion.WIKIDATA
import net.osmand.shared.data.Amenity.Companion.WIKIMEDIA_COMMONS
import net.osmand.shared.data.Amenity.Companion.WIKI_PHOTO
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.MergeLocalizedTagsAlgorithm
import net.osmand.shared.util.PoiAdditionalLangLookup
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

class AdditionalInfoBundle(
	private val poiTypes: MapPoiTypes,
	val additionalInfo: Map<String, String>?
) {

	// the default registry, empty until the platform has read poi_types.xml, as the java registry was
	constructor(additionalInfo: Map<String, String>?) : this(MapPoiTypes.getDefaultNoInit(), additionalInfo)

	data class ResolvedPoiType(val additionalType: PoiType?, val categoryType: PoiType?)

	private val langLookup = object : PoiAdditionalLangLookup {
		override fun hasLang(key: String): Boolean = poiTypes.getAnyPoiAdditionalTypeByKey(key)?.getLang() != null
	}
	private var filteredAdditionalInfo: Map<String, String>? = null
	private var localizedAdditionalInfo: Map<String, Any>? = null
	private var customHiddenExtensions: List<String>? = null

	fun getFilteredInfo(): Map<String, String> {
		var result = filteredAdditionalInfo
		if (result == null) {
			val filtered = LinkedHashMap<String, String>()
			for (origKey in getAdditionalInfoKeys()) {
				val key = when {
					origKey == GpxUtilities.AMENITY_PREFIX + Amenity.OPENING_HOURS ->
						origKey.replace(GpxUtilities.AMENITY_PREFIX, "")
					origKey.startsWith(GpxUtilities.AMENITY_PREFIX) -> continue
					else -> origKey.replace(GpxUtilities.OSM_PREFIX, "")
				}
				val hidden = customHiddenExtensions
				if (!HIDDEN_EXTENSIONS.contains(key) && (hidden.isNullOrEmpty() || !hidden.contains(key))) {
					filtered[key] = get(origKey) ?: continue
				}
			}
			result = filtered
			filteredAdditionalInfo = result
		}
		return result
	}

	fun getFilteredLocalizedInfo(): Map<String, Any> {
		var result = localizedAdditionalInfo
		if (result == null) {
			result = MergeLocalizedTagsAlgorithm.execute(langLookup, getFilteredInfo())
			localizedAdditionalInfo = result
		}
		return result
	}

	/**
	 * @param genericRowKeys keys that must still get a generic row when the category does not
	 *                       show default tags, see getGenericRowKeys().
	 */
	@JvmOverloads
	fun getVisibleTags(
		allowNoteTag: Boolean, preferredLangs: List<String>?,
		genericRowKeys: Set<String> = emptySet()
	): MutableList<AmenityTagEntry> {
		val category = getCategory()
		val collectedPoiTypes = LinkedHashMap<String, MutableList<PoiType>>()
		val entries = collectPlainRows(allowNoteTag, preferredLangs, category, collectedPoiTypes, genericRowKeys)
		entries.addAll(collectCollapsableGroups(category))
		entries.addAll(collectPoiTypeGroups(category, collectedPoiTypes))
		return entries
	}

	private fun collectPlainRows(
		allowNoteTag: Boolean, preferredLangs: List<String>?, category: PoiCategory?,
		collectedPoiTypes: MutableMap<String, MutableList<PoiType>>, genericRowKeys: Set<String>
	): MutableList<AmenityTagEntry> {
		val showDefaultTags = isDefaultForCategory()
		val entries = ArrayList<AmenityTagEntry>()
		var cuisineEntry: AmenityTagEntry? = null

		for ((key, value) in getFilteredLocalizedInfo()) {
			if (isKeyToSkip(key) || !shouldDisplayKey(key)) {
				continue
			}
			if (!allowNoteTag && NOTE == key && value is String) {
				continue
			}
			val strValue = value as? String

			val resolvedType = resolvePoiType(category, key, strValue)
			val additionalType = resolvedType.additionalType
			val categoryType = resolvedType.categoryType
			if (isFilterOnlyOrGrouped(additionalType)) {
				continue
			}
			if (additionalType == null && categoryType == null && !showDefaultTags && !genericRowKeys.contains(key)) {
				continue
			}

			if (strValue != null) {
				if (additionalType != null) {
					val tagEntry = AmenityTagEntry.Builder(key)
						.setValue(strValue)
						.setOrder(additionalType.getOrder())
						.setResolvedType(resolvedType)
						.setIsDescription(key.contains(Amenity.DESCRIPTION))
						.build()
					if (Amenity.CUISINE == key) {
						cuisineEntry = tagEntry
					} else {
						entries.add(tagEntry)
					}
				} else if (categoryType != null) {
					val categoryKey = categoryType.getCategory()?.getKeyName()
					if (categoryKey != null && MapPoiTypes.OTHER_MAP_CATEGORY != categoryKey) {
						collectedPoiTypes.getOrPut(categoryKey) { ArrayList() }.add(categoryType)
					}
				} else {
					entries.add(
						AmenityTagEntry.Builder(key)
							.setValue(strValue)
							.setOrder(PoiType.DEFAULT_ORDER)
							.setResolvedType(resolvedType)
							.setIsDescription(key.contains(Amenity.DESCRIPTION))
							.build()
					)
				}
			} else if (value is Map<*, *>) {
				val localizations = extractLocalizations(value) ?: continue
				toLocalizedAmenityTagEntry(key, localizations, resolvedType, preferredLangs)?.let { entries.add(it) }
			}
		}

		if (cuisineEntry != null && !containsAny(CUISINE_INFO_ID, DISH_INFO_ID)) {
			entries.add(cuisineEntry)
		}
		return entries
	}

	private fun collectCollapsableGroups(category: PoiCategory?): List<AmenityTagEntry> {
		val entries = ArrayList<AmenityTagEntry>()
		for ((key, rawValue) in getFilteredInfo()) {
			if (!key.startsWith(COLLAPSABLE_PREFIX) || KAlgorithms.isEmpty(rawValue)) {
				continue
			}
			val categoryTypes = ArrayList<PoiType>()
			for (record in rawValue.split(Amenity.SEPARATOR)) {
				val type = (category?.let { poiTypes.getPoiAdditionalType(it, record) })
					?: poiTypes.getAnyPoiAdditionalTypeByKey(record)
				if (type is PoiType) {
					categoryTypes.add(type)
				}
			}
			if (categoryTypes.isEmpty()) {
				continue
			}
			val poiAdditionalCategoryName = categoryTypes[0].getPoiAdditionalCategory() ?: continue
			entries.add(
				AmenityTagEntry.Builder(poiAdditionalCategoryName)
					.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(categoryTypes)
					.setCollapsableCategory(category)
					.setPoiAdditional(true)
					.setOrder(categoryTypes[0].getOrder())
					.build()
			)
		}
		return entries
	}

	private fun collectPoiTypeGroups(
		category: PoiCategory?, collectedPoiTypes: Map<String, List<PoiType>>
	): List<AmenityTagEntry> {
		val entries = ArrayList<AmenityTagEntry>()
		for (poiTypeList in collectedPoiTypes.values) {
			val groupCategory = poiTypeList[0].getCategory() ?: continue
			entries.add(
				AmenityTagEntry.Builder(groupCategory.getKeyName())
					.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
					.setCollapsablePoiTypes(poiTypeList)
					.setCollapsableCategory(category)
					.setPoiAdditional(false)
					.setOrder(PoiType.DEFAULT_GROUP_ORDER)
					.build()
			)
		}
		return entries
	}

	private fun toLocalizedAmenityTagEntry(
		key: String, localizations: Map<String, String>, resolvedType: ResolvedPoiType,
		preferredLangs: List<String>?
	): AmenityTagEntry? {
		val children = localizations.map { (locKey, locValue) ->
			AmenityTagEntry.Builder(locKey).setValue(locValue).build()
		}
		if (children.isEmpty()) {
			return null
		}
		val header = pickHeader(children, preferredLangs)
		val otherLangs = children.filter { it !== header }
		val order = resolvedType.additionalType?.getOrder() ?: PoiType.DEFAULT_ORDER
		return AmenityTagEntry.Builder(header.key)
			.setValue(header.value)
			.setCollapsableEntries(otherLangs)
			.setResolvedType(resolvedType)
			.setOrder(order)
			.setIsDescription(key.contains(Amenity.DESCRIPTION))
			.build()
	}

	private fun pickHeader(children: List<AmenityTagEntry>, preferredLangs: List<String>?): AmenityTagEntry {
		preferredLangs?.forEach { lang ->
			if (!KAlgorithms.isEmpty(lang)) {
				children.firstOrNull { it.key.endsWith(":$lang") }?.let { return it }
			}
		}
		return children[0]
	}

	private fun isFilterOnlyOrGrouped(pType: PoiType?): Boolean =
		pType != null && (pType.isFilterOnly()
				|| (!pType.isText() && !KAlgorithms.isEmpty(pType.getPoiAdditionalCategory())))

	private fun isDefaultForCategory(): Boolean {
		val category = getCategory() ?: return false
		val subtype = get(SUBTYPE)
		if (KAlgorithms.isEmpty(subtype)) {
			return false
		}
		return category.getPoiTypeByKeyName(subtype!!)?.isDefaultForCategory() == true
	}

	private fun extractLocalizations(value: Map<*, *>): Map<String, String>? {
		val localizations = value[LOCALIZATIONS] as? Map<*, *> ?: return null
		val filtered = LinkedHashMap<String, String>()
		for ((locKey, locValue) in localizations) {
			if (locKey is String && locValue is String && !isKeyToSkip(locKey)) {
				filtered[locKey] = locValue
			}
		}
		return filtered.ifEmpty { null }
	}

	private fun shouldDisplayKey(key: String): Boolean {
		if (key.contains(Amenity.WIKIPEDIA) || key.contains(Amenity.CONTENT)
			|| key.contains(Amenity.SHORT_DESCRIPTION) || key.contains(MapPoiTypes.WIKI_LANG)
		) {
			return false
		}
		if (MapPoiTypes.ROUTE_ARTICLE == get(SUBTYPE) && key.contains(Amenity.DESCRIPTION)) {
			return false
		}
		val t = poiTypes.getAnyPoiAdditionalTypeByKey(key)
		if (t is PoiType && t.isHidden()) {
			return false
		}
		return Amenity.NAME != key
	}

	fun getCategory(): PoiCategory? {
		if (additionalInfo == null) {
			return null
		}
		val typeTag = additionalInfo[TYPE]
		val poiCategory = if (!KAlgorithms.isEmpty(typeTag)) poiTypes.getPoiCategoryByName(typeTag!!) else null
		return poiCategory ?: poiTypes.getOtherPoiCategory()
	}

	fun containsAny(vararg keys: String): Boolean {
		val infoKeys = getAdditionalInfoKeys()
		return keys.any { infoKeys.contains(it) }
	}

	fun contains(key: String): Boolean = getAdditionalInfoKeys().contains(key)

	fun getAdditionalInfoKeys(): Collection<String> = additionalInfo?.keys ?: emptyList()

	fun get(key: String): String? = MapObject.unzipContent(additionalInfo?.get(key))

	fun setCustomHiddenExtensions(customHiddenExtensions: List<String>?) {
		this.filteredAdditionalInfo = null
		this.localizedAdditionalInfo = null
		this.customHiddenExtensions = customHiddenExtensions
	}

	fun getPoiAdditionalType(key: String, vl: String?): PoiType? {
		var pt = poiTypes.getAnyPoiAdditionalTypeByKey(key)
		if (pt == null && vl != null && !KAlgorithms.isEmpty(vl) && vl.length < 50) {
			pt = poiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + vl)
		}
		return pt as? PoiType
	}

	fun resolvePoiType(category: PoiCategory?, key: String, vl: String?): ResolvedPoiType {
		var additionalType = getPoiAdditionalType(key, vl)
		var categoryType = category?.getPoiTypeByKeyName(key)
		if (categoryType == null && additionalType == null) {
			categoryType = poiTypes.getPoiTypeByKey(key)
		}
		if (additionalType == null) {
			val altKey = key.replace(':', '_')
			additionalType = getPoiAdditionalType(altKey, vl)
			categoryType = category?.getPoiTypeByKeyName(altKey)
			if (categoryType == null && additionalType == null) {
				categoryType = poiTypes.getPoiTypeByKey(altKey)
			}
		}
		return ResolvedPoiType(additionalType, categoryType)
	}

	fun isKeyToSkip(key: String): Boolean =
		KCollectionUtils.startsWithAny(key, COLLAPSABLE_PREFIX, ALT_NAME_WITH_LANG_PREFIX, LANG_YES)
				|| KCollectionUtils.equalsToAny(key, WIKI_PHOTO, WIKIDATA, WIKIMEDIA_COMMONS, "image", "mapillary", "subway_region")
				|| MapObject.isNameLangTag(key)
				|| key.contains(ROUTE)

	companion object {
		const val LOCALIZATIONS = "localizations"

		private val HIDDEN_EXTENSIONS = setOf(
			GpxUtilities.COLOR_NAME_EXTENSION, GpxUtilities.ICON_NAME_EXTENSION,
			GpxUtilities.BACKGROUND_TYPE_EXTENSION, GpxUtilities.PROFILE_TYPE_EXTENSION,
			GpxUtilities.ADDRESS_EXTENSION, GpxUtilities.AMENITY_ORIGIN_EXTENSION,
			TYPE, SUBTYPE, GpxUtilities.ORIGIN_EXTENSION, GpxUtilities.OSM_URL_EXTENSION
		)

		// OsmAnd's own point fields that are not in HIDDEN_EXTENSIONS: never a generic row
		private val SERVICE_KEYS = setOf(
			GpxUtilities.HIDDEN_EXTENSION, GpxUtilities.PINNED_EXTENSION, GpxUtilities.POINT_TYPE_EXTENSION,
			GpxUtilities.LINE_WIDTH_EXTENSION, GpxUtilities.TRKPT_INDEX_EXTENSION, GpxUtilities.POINT_ELEVATION,
			GpxUtilities.POINT_SPEED, GpxUtilities.POINT_BEARING, GpxUtilities.POINT_HEADING,
			GpxUtilities.MIN_ELEVATION, GpxUtilities.MAX_ELEVATION, GpxUtilities.AVG_ELEVATION,
			GpxUtilities.DIFF_ELEVATION_UP, GpxUtilities.DIFF_ELEVATION_DOWN,
			PointAttributes.DEV_INTERPOLATION_OFFSET_N,
			"visited_date", "creation_date", "pickup_date", "calendar_event"
		)

		private const val CUISINE_INFO_ID = COLLAPSABLE_PREFIX + Amenity.CUISINE
		private const val DISH_INFO_ID = COLLAPSABLE_PREFIX + Amenity.DISH

		/**
		 * A point shows all its data: every stored extension the POI logic does not know ("hr", "my_note",
		 * "test:country") gets a generic row, only OsmAnd's own point fields and namespaces are skipped.
		 */
		@JvmStatic
		fun getGenericRowKeys(storedExtensions: Map<String, String>): Set<String> =
			storedExtensions.keys.filterTo(HashSet()) { key ->
				!HIDDEN_EXTENSIONS.contains(key) && !SERVICE_KEYS.contains(key)
						&& !key.startsWith(GpxUtilities.AMENITY_PREFIX) && !key.startsWith(GpxUtilities.OSM_PREFIX)
						&& !key.startsWith(GpxUtilities.OSMAND_EXTENSIONS_PREFIX)
						&& !key.startsWith(GpxUtilities.GPXTPX_PREFIX)
			}
	}
}
