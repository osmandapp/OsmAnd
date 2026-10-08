package net.osmand.shared.osm

import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.primaryCollator
import net.osmand.shared.util.synchronized
import net.osmand.shared.xml.XmlPullParser
import okio.Buffer
import okio.Source
import kotlin.jvm.JvmStatic

/**
 * poi_types.xml read into memory: the categories, the filters inside them, the types inside those,
 * and the additional attributes that can hang off any of the three. Everything that turns a tag
 * and a value read out of an obf file into something with a name and an icon goes through here.
 *
 * A copy of `MapPoiTypes` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. Two things are left out, both of which belong to writing obf files rather than reading
 * them: `parseAmenity`, which the indexer uses to turn osm tags into an amenity, and the `main`
 * and `print` helpers. The names of a type are not in the file - they come from the platform's
 * string resources through a [PoiTranslator], and without one a type falls back to its key name.
 */
class MapPoiTypes(private var resourceName: String?) {

	private var categories: List<PoiCategory> = ArrayList()
	private var forbiddenTypes: Set<String> = HashSet()
	private var otherCategory: PoiCategory? = null
	internal var textPoiAdditionals: MutableList<PoiType> = ArrayList()
	private var publicTransportTypes: MutableList<String>? = null

	private var poiTranslator: PoiTranslator? = null
	private var init: Boolean = false

	// caches
	internal var poiTypesByTag: MutableMap<String, PoiType> = LinkedHashMap()
	internal var defaultPoiTypesByTag: MutableMap<String, PoiType> = HashMap()
	internal var deprecatedTags: MutableMap<String, String> = LinkedHashMap()
	internal var poiAdditionalCategoryIconNames: MutableMap<String, String> = LinkedHashMap()
	internal var poiCategoryIndex: MutableMap<String, Int> = HashMap()

	/** Java holds this in a concurrent map; here a lock does the same job for the same reason. */
	private val dynCacheByKey: MutableMap<String, AbstractPoiType> = HashMap()
	private val dynCacheLock = KLock()

	val topIndexPoiAdditional: MutableMap<String, PoiType> = LinkedHashMap()

	fun isInit(): Boolean = init

	fun isOtherCategory(poiCategory: PoiCategory?): Boolean = otherCategory == poiCategory

	fun getOtherPoiCategory(): PoiCategory? = otherCategory

	fun getOtherMapCategory(): PoiCategory? = getPoiCategoryByName(OTHER_MAP_CATEGORY, true)

	fun getPoiAdditionalCategoryIconName(category: String): String? =
		poiAdditionalCategoryIconNames[category]

	fun getTextPoiAdditionals(): List<PoiType> = textPoiAdditionals

	fun getTopVisibleFilters(): List<AbstractPoiType> {
		val lf = ArrayList<AbstractPoiType>()
		for (pc in categories) {
			if (pc.isTopVisible()) {
				lf.add(pc)
			}
			for (p in pc.getPoiFilters()) {
				if (p.isTopVisible()) {
					lf.add(p)
				}
			}
			for (p in pc.getPoiTypes()) {
				if (p.isTopVisible()) {
					lf.add(p)
				}
			}
		}
		sortList(lf)
		return lf
	}

	fun getOsmwiki(): PoiCategory? {
		for (category in categories) {
			if (category.isWiki()) {
				return category
			}
		}
		return null
	}

	fun getRoutes(): PoiCategory? {
		for (category in categories) {
			if (category.isRoutes()) {
				return category
			}
		}
		return null
	}

	fun getAllAvailableWikiLocales(): List<String> {
		val availableWikiLocales = ArrayList<String>()
		val place = getOsmwiki()?.getPoiTypeByKeyName(WIKI_PLACE) ?: return availableWikiLocales
		for (type in place.getPoiAdditionals()) {
			val name = type.getKeyName()
			val wikiLang = "$WIKI_LANG:"
			if (name.startsWith(wikiLang)) {
				availableWikiLocales.add(name.substring(wikiLang.length))
			}
		}
		return availableWikiLocales
	}

	/**
	 * Java sorts by the default locale's collator at its default strength; this one compares at
	 * primary strength, which only differs when two names are the same but for case or accents.
	 */
	private fun <T : AbstractPoiType> sortList(lf: MutableList<T>) {
		val instance: KCollator = primaryCollator()
		lf.sortWith { object1, object2 -> instance.compare(object1.getTranslation(), object2.getTranslation()) }
	}

	fun getUserDefinedCategory(): PoiCategory? = otherCategory

	fun getPoiTypeByKey(name: String): PoiType? {
		for (pc in categories) {
			val pt = pc.getPoiTypeByKeyName(name)
			if (pt != null && !pt.isReference()) {
				return pt
			}
		}
		return null
	}

	fun getPoiTypeByKeyInCategory(category: PoiCategory?, keyName: String): PoiType? =
		category?.getPoiTypeByKeyName(keyName)

	fun getAnyPoiTypeByKey(name: String): AbstractPoiType? = getAnyPoiTypeByKey(name, true)

	fun getAnyPoiTypeByKey(name: String, skipAdditional: Boolean): AbstractPoiType? {
		for (pc in categories) {
			if (pc.getKeyName() == name) {
				return pc
			}
			for (pf in pc.getPoiFilters()) {
				if (pf.getKeyName() == name) {
					return pf
				}
				// search in poi additional
				if (!skipAdditional) {
					for (type in pf.getPoiTypes()) {
						if (type.getKeyName() == name) {
							return type
						}
						val foundType = findInAdds(type.getPoiAdditionals(), name)
						if (foundType != null) {
							return foundType
						}
					}
				}
			}
			val pt = pc.getPoiTypeByKeyName(name)
			if (pt != null && !pt.isReference()) {
				return pt
			}
		}
		return null
	}

	private fun findInAdds(adds: List<PoiType>, name: String): AbstractPoiType? {
		for (additional in adds) {
			if (additional.getKeyName() == name) {
				return additional
			}
			val foundType = findInAdds(additional.getPoiAdditionals(), name)
			if (foundType != null) {
				return foundType
			}
		}
		return null
	}

	fun getAllTranslatedNames(skipNonEditable: Boolean): Map<String, PoiType> {
		val translation = HashMap<String, PoiType>()
		for (pc in categories) {
			if (skipNonEditable && pc.isNotEditableOsm()) {
				continue
			}
			addPoiTypesTranslation(skipNonEditable, translation, pc)
		}
		return translation
	}

	private fun addPoiTypesTranslation(
		skipNonEditable: Boolean, translation: MutableMap<String, PoiType>, pf: PoiFilter
	) {
		for (pt in pf.getPoiTypes()) {
			if (pt.isReference()) {
				continue
			}
			if (pt.getBaseLangType() != null) {
				continue
			}
			if (skipNonEditable && pt.isNotEditableOsm()) {
				continue
			}
			translation[pt.getKeyName().replace('_', ' ').lowercase()] = pt
			translation[pt.getTranslation().lowercase()] = pt
		}
	}

	fun getAllTypesTranslatedNames(matcher: KStringMatcher): List<AbstractPoiType> {
		val tm = ArrayList<AbstractPoiType>()
		for (pc in categories) {
			if (pc == getOtherMapCategory()) {
				continue
			}
			addIf(tm, pc, matcher)
			for (pt in pc.getPoiFilters()) {
				addIf(tm, pt, matcher)
			}
			for (pt in pc.getPoiTypes()) {
				if (pt.isReference()) {
					continue
				}
				addIf(tm, pt, matcher)
			}
		}
		return tm
	}

	private fun addIf(tm: MutableList<AbstractPoiType>, pc: AbstractPoiType, matcher: KStringMatcher) {
		if (matcher.matches(pc.getTranslation()) || matcher.matches(pc.getKeyName().replace('_', ' '))) {
			tm.add(pc)
		}
		for (a in pc.getPoiAdditionals()) {
			addIf(tm, a, matcher)
		}
	}

	/** Sorted by name, as java's `TreeMap` leaves it. */
	fun getAllTranslatedNames(pc: PoiCategory, onlyTranslation: Boolean): Map<String, PoiType> {
		val collected = LinkedHashMap<String, PoiType>()
		for (pt in pc.getPoiTypes()) {
			collected[pt.getTranslation()] = pt
			if (!onlyTranslation) {
				collected[capitalizeFirstLetterAndLowercase(pt.getKeyName().replace('_', ' '))] = pt
			}
		}
		val translation = LinkedHashMap<String, PoiType>()
		for (key in collected.keys.sorted()) {
			translation[key] = collected[key]!!
		}
		return translation
	}

	fun getPoiCategoryByName(name: String): PoiCategory? = getPoiCategoryByName(name, false)

	fun getPoiCategoryByName(name: String, create: Boolean): PoiCategory? {
		var searched = name
		if (searched == "leisure" && !create) {
			searched = "entertainment"
		}
		if (searched == "historic" && !create) {
			searched = "tourism"
		}
		val index = poiCategoryIndex[searched]
		if (index != null) {
			return categories[index]
		}
		val size = categories.size
		for (category in categories) {
			if (category.getKeyName().equals(searched, ignoreCase = true)) {
				return category
			}
		}
		if (create) {
			val lastCategory = PoiCategory(this, searched, size)
			if (lastCategory.getKeyName() != OTHER_MAP_CATEGORY) {
				lastCategory.setTopVisible(true)
			}
			addCategory(lastCategory)
			return lastCategory
		}
		return otherCategory
	}

	private fun addCategory(category: PoiCategory) {
		val categories = ArrayList(this.categories)
		categories.add(category)
		this.categories = categories
		reindexCategories()
	}

	fun getCategories(): List<PoiCategory> = categories

	fun getPoiTranslator(): PoiTranslator? = poiTranslator

	fun setPoiTranslator(poiTranslator: PoiTranslator?) {
		this.poiTranslator = poiTranslator
		val categories = ArrayList(this.categories)
		sortList(categories)
		this.categories = categories
		reindexCategories()
	}

	fun init() {
		init(null)
	}

	/** Reads poi_types.xml from [resourceName], or from the one this registry was built with. */
	fun init(resourceName: String?) {
		if (resourceName != null) {
			this.resourceName = resourceName
		}
		val path = this.resourceName
			?: throw IllegalStateException("No poi_types.xml to read: no resource name given")
		val source = KFile(path).source()
		try {
			initFromSource(source)
		} finally {
			source.close()
		}
	}

	/** For a platform that hands the file over as text, as `OsmAndContext.getAssetAsString` does. */
	fun initFromString(xml: String) {
		val source = Buffer().writeUtf8(xml)
		try {
			initFromSource(source)
		} finally {
			source.close()
		}
	}

	fun initFromSource(source: Source) {
		val referenceTypes = ArrayList<PoiType>()
		val allTypes = LinkedHashMap<String, PoiType>()
		val categoryPoiAdditionalMap = LinkedHashMap<String, MutableList<PoiType>>()
		val abstractTypeAdditionalCategories = LinkedHashMap<AbstractPoiType, List<String>>()
		val poiTypesByTag = LinkedHashMap<String, PoiType>()
		val deprecatedTags = LinkedHashMap<String, String>()
		val poiAdditionalCategoryIconNames = LinkedHashMap<String, String>()
		val textPoiAdditionals = ArrayList<PoiType>()

		val categoriesList = ArrayList<PoiCategory>()
		val parser = XmlPullParser()
		try {
			parser.setInput(source, "UTF-8")
			var lastCategory: PoiCategory? = null
			var lastCategoryPoiAdditionalsCategories = LinkedHashSet<String>()
			var lastFilter: PoiFilter? = null
			var lastFilterPoiAdditionalsCategories = LinkedHashSet<String>()
			var lastType: PoiType? = null
			var lastTypePoiAdditionalsCategories = LinkedHashSet<String>()
			var lastPoiAdditionalCategory: String? = null
			val localOtherMapCategory = PoiCategory(this, OTHER_MAP_CATEGORY, categoriesList.size)
			categoriesList.add(localOtherMapCategory)
			while (true) {
				val tok = parser.next()
				if (tok == XmlPullParser.END_DOCUMENT) {
					break
				}
				if (tok == XmlPullParser.START_TAG) {
					when (val name = parser.getName()) {
						"poi_category" -> {
							val category = PoiCategory(
								this, parser.getAttributeValue("", "name")!!, categoriesList.size
							)
							category.setTopVisible(parser.getAttributeValue("", "top").toBoolean())
							category.setNotEditableOsm("true" == parser.getAttributeValue("", "no_edit"))
							category.setDefaultTag(parser.getAttributeValue("", "default_tag"))
							val additionalCategory = parser.getAttributeValue("", "poi_additional_category")
							if (!KAlgorithms.isEmpty(additionalCategory)) {
								lastCategoryPoiAdditionalsCategories.addAll(additionalCategory!!.split(","))
							}
							val excluded = parser.getAttributeValue("", "excluded_poi_additional_category")
							if (!KAlgorithms.isEmpty(excluded)) {
								category.addExcludedPoiAdditionalCategories(excluded!!.split(",").toTypedArray())
								category.getExcludedPoiAdditionalCategories()
									?.forEach { lastCategoryPoiAdditionalsCategories.remove(it) }
							}
							lastCategory = category
							categoriesList.add(category)
						}
						"poi_filter" -> {
							val keyName = parser.getAttributeValue("", "name")!!
							val iconName = parser.getAttributeValue("", "icon")
							val tp = PoiFilter(this, lastCategory, keyName, iconName)
							tp.setTopVisible(parser.getAttributeValue("", "top").toBoolean())
							lastFilter = tp
							lastFilterPoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories)
							val additionalCategory = parser.getAttributeValue("", "poi_additional_category")
							if (!KAlgorithms.isEmpty(additionalCategory)) {
								lastFilterPoiAdditionalsCategories.addAll(additionalCategory!!.split(","))
							}
							val excluded = parser.getAttributeValue("", "excluded_poi_additional_category")
							if (!KAlgorithms.isEmpty(excluded)) {
								tp.addExcludedPoiAdditionalCategories(excluded!!.split(",").toTypedArray())
								tp.getExcludedPoiAdditionalCategories()
									?.forEach { lastFilterPoiAdditionalsCategories.remove(it) }
							}
							lastCategory?.addPoiType(tp)
						}
						"poi_reference" -> {
							val keyName = parser.getAttributeValue("", "name")!!
							val iconName = parser.getAttributeValue("", "icon")
							val tp = PoiType(this, lastCategory, lastFilter, keyName, iconName)
							referenceTypes.add(tp)
							tp.setReferenceType(tp)
							lastFilter?.addPoiType(tp)
							lastCategory?.addPoiType(tp)
						}
						"poi_additional" -> {
							if (lastCategory == null) {
								lastCategory = localOtherMapCategory
							}
							val baseType = parsePoiAdditional(
								parser, lastCategory, lastFilter, lastType, null, null,
								lastPoiAdditionalCategory, textPoiAdditionals
							)
							if ("true" == parser.getAttributeValue("", "lang")) {
								for (lng in MapRenderingTypes.langs) {
									parsePoiAdditional(
										parser, lastCategory, lastFilter, lastType, lng, baseType,
										lastPoiAdditionalCategory, textPoiAdditionals
									)
									if (baseType.isTopIndex()) {
										topIndexPoiAdditional[TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName() + ":" + lng] = baseType
									}
								}
								parsePoiAdditional(
									parser, lastCategory, lastFilter, lastType, "en", baseType,
									lastPoiAdditionalCategory, textPoiAdditionals
								)
								if (baseType.isTopIndex()) {
									topIndexPoiAdditional[TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName() + ":en"] = baseType
								}
							}
							if (lastPoiAdditionalCategory != null) {
								val categoryAdditionals = categoryPoiAdditionalMap
									.getOrPut(lastPoiAdditionalCategory) { ArrayList() }
								categoryAdditionals.add(baseType)
							}
							if (baseType.isTopIndex()) {
								topIndexPoiAdditional[TOP_INDEX_ADDITIONAL_PREFIX + baseType.getKeyName()] = baseType
							}
						}
						"poi_additional_category" -> {
							if (lastPoiAdditionalCategory == null) {
								lastPoiAdditionalCategory = parser.getAttributeValue("", "name")
								val icon = parser.getAttributeValue("", "icon")
								if (!KAlgorithms.isEmpty(icon) && lastPoiAdditionalCategory != null) {
									poiAdditionalCategoryIconNames[lastPoiAdditionalCategory] = icon!!
								}
							}
						}
						"poi_type" -> {
							if (lastCategory == null) {
								lastCategory = localOtherMapCategory
							}
							if (!KAlgorithms.isEmpty(parser.getAttributeValue("", "deprecated_of"))) {
								val vl = parser.getAttributeValue("", "name")!!
								val target = parser.getAttributeValue("", "deprecated_of")!!
								deprecatedTags[vl] = target
							} else {
								val type = parsePoiType(allTypes, parser, lastCategory, lastFilter, null, null)
								lastType = type
								if ("true" == parser.getAttributeValue("", "lang")) {
									for (lng in MapRenderingTypes.langs) {
										parsePoiType(allTypes, parser, lastCategory, lastFilter, lng, type)
									}
								}
								lastTypePoiAdditionalsCategories.addAll(lastCategoryPoiAdditionalsCategories)
								lastTypePoiAdditionalsCategories.addAll(lastFilterPoiAdditionalsCategories)
								val additionalCategory = parser.getAttributeValue("", "poi_additional_category")
								if (!KAlgorithms.isEmpty(additionalCategory)) {
									lastTypePoiAdditionalsCategories.addAll(additionalCategory!!.split(","))
								}
								val excluded = parser.getAttributeValue("", "excluded_poi_additional_category")
								if (!KAlgorithms.isEmpty(excluded)) {
									type.addExcludedPoiAdditionalCategories(excluded!!.split(",").toTypedArray())
									type.getExcludedPoiAdditionalCategories()
										?.forEach { lastTypePoiAdditionalsCategories.remove(it) }
								}
							}
						}
						"poi_types" -> {
							// just ignore the root tag of poi_types.xml
						}
						else -> log.warn("Unknown start tag encountered: $name")
					}
				} else if (tok == XmlPullParser.END_TAG) {
					when (val name = parser.getName()) {
						"poi_filter" -> {
							if (lastFilterPoiAdditionalsCategories.isNotEmpty() && lastFilter != null) {
								abstractTypeAdditionalCategories[lastFilter] =
									lastFilterPoiAdditionalsCategories.sorted()
								lastFilterPoiAdditionalsCategories = LinkedHashSet()
							}
							lastFilter = null
						}
						"poi_type" -> {
							if (lastTypePoiAdditionalsCategories.isNotEmpty() && lastType != null) {
								abstractTypeAdditionalCategories[lastType] =
									lastTypePoiAdditionalsCategories.sorted()
								lastTypePoiAdditionalsCategories = LinkedHashSet()
							}
							lastType = null
						}
						"poi_category" -> {
							if (lastCategoryPoiAdditionalsCategories.isNotEmpty() && lastCategory != null) {
								abstractTypeAdditionalCategories[lastCategory] =
									lastCategoryPoiAdditionalsCategories.sorted()
								lastCategoryPoiAdditionalsCategories = LinkedHashSet()
							}
							lastCategory = null
						}
						"poi_additional_category" -> lastPoiAdditionalCategory = null
						else -> {
							if (name != "poi_additional" && name != "poi_reference" && name != "poi_types") {
								log.warn("Unknown end tag encountered: $name")
							}
						}
					}
				}
			}
		} finally {
			parser.close()
		}
		for (gt in referenceTypes) {
			val pt = allTypes[gt.getKeyName()]
			if (pt == null || pt.getOsmTag() == null) {
				throw IllegalStateException("Can't find poi type for poi reference '${gt.getKeyName()}'")
			} else {
				gt.setReferenceType(pt)
			}
		}
		for (entry in abstractTypeAdditionalCategories.entries) {
			for (category in entry.value) {
				val poiAdditionals = categoryPoiAdditionalMap[category]
				if (poiAdditionals != null) {
					for (poiType in poiAdditionals) {
						buildPoiAdditionalReference(poiType, entry.key, textPoiAdditionals)
					}
				}
			}
		}
		this.categories = categoriesList
		reindexCategories()
		this.poiTypesByTag = poiTypesByTag
		this.deprecatedTags = deprecatedTags
		this.poiAdditionalCategoryIconNames = poiAdditionalCategoryIconNames
		this.textPoiAdditionals = textPoiAdditionals
		otherCategory = getPoiCategoryByName("user_defined_other")
			?: throw IllegalArgumentException("No poi category other")
		init = true
	}

	/**
	 * Copies an additional attribute onto another type, so that "opening_hours" declared once can
	 * apply to every type of a category without the file repeating it.
	 */
	private fun buildPoiAdditionalReference(
		poiAdditional: PoiType, parent: AbstractPoiType, textPoiAdditionals: MutableList<PoiType>
	): PoiType? {
		var lastCategory: PoiCategory? = null
		var lastFilter: PoiFilter? = null
		var lastType: PoiType? = null
		var ref: PoiType? = null
		val keyName = poiAdditional.getKeyName()
		val iconNameAttribute = poiAdditional.getIconNameInternal()
		if (parent is PoiCategory) {
			lastCategory = parent
			ref = PoiType(this, lastCategory, null, keyName, iconNameAttribute)
		} else if (parent is PoiFilter) {
			lastFilter = parent
			ref = PoiType(this, lastFilter.getPoiCategory(), lastFilter, keyName, iconNameAttribute)
		} else if (parent is PoiType) {
			lastType = parent
			ref = PoiType(this, lastType.getCategory(), lastType.getFilter(), keyName, iconNameAttribute)
		}
		if (ref == null) {
			return null
		}
		if (poiAdditional.isReference()) {
			ref.setReferenceType(poiAdditional.getReferenceType())
		} else {
			ref.setReferenceType(poiAdditional)
		}
		ref.setBaseLangType(poiAdditional.getBaseLangType())
		ref.setLang(poiAdditional.getLang())
		ref.setAdditional(lastType ?: (lastFilter ?: lastCategory))
		ref.setTopVisible(poiAdditional.isTopVisible())
		ref.setText(poiAdditional.isText())
		ref.setOrder(poiAdditional.getOrder())
		ref.setNonIndx(poiAdditional.isNonIndx())
		ref.setHidden(poiAdditional.isHidden())
		ref.setOsmTag(poiAdditional.getOsmTag())
		ref.setNotEditableOsm(poiAdditional.isNotEditableOsm())
		ref.setOsmValue(poiAdditional.getOsmValue())
		ref.setOsmTag2(poiAdditional.getOsmTag2())
		ref.setOsmValue2(poiAdditional.getOsmValue2())
		ref.setPoiAdditionalCategory(poiAdditional.getPoiAdditionalCategory())
		ref.setFilterOnly(poiAdditional.isFilterOnly())
		if (lastType != null) {
			lastType.addPoiAdditional(ref)
		} else if (lastFilter != null) {
			lastFilter.addPoiAdditional(ref)
		} else if (lastCategory != null) {
			lastCategory.addPoiAdditional(ref)
		}
		if (ref.isText()) {
			textPoiAdditionals.add(ref)
		}
		return ref
	}

	private fun parsePoiAdditional(
		parser: XmlPullParser, lastCategory: PoiCategory?, lastFilter: PoiFilter?,
		lastType: PoiType?, lang: String?, langBaseType: PoiType?,
		poiAdditionalCategory: String?, textPoiAdditionals: MutableList<PoiType>
	): PoiType {
		var oname = parser.getAttributeValue("", "name")!!
		if (lang != null) {
			oname += ":$lang"
		}
		var otag = parser.getAttributeValue("", "tag")
		if (lang != null) {
			otag += ":$lang"
		}
		val iconName = parser.getAttributeValue("", "icon")
		val tp = PoiType(this, lastCategory, lastFilter, oname, iconName)
		tp.setBaseLangType(langBaseType)
		tp.setLang(lang)
		tp.setAdditional(lastType ?: (lastFilter ?: lastCategory))
		tp.setTopVisible(parser.getAttributeValue("", "top").toBoolean())
		tp.setNonIndx(
			!parser.getAttributeValue("", "top").toBoolean() ||
					parser.getAttributeValue("", "no_indx").toBoolean()
		)
		tp.setText("text" == parser.getAttributeValue("", "type"))
		tp.setHidden(parser.getAttributeValue("", "hidden").toBoolean())
		val orderStr = parser.getAttributeValue("", "order")
		if (!KAlgorithms.isEmpty(orderStr)) {
			tp.setOrder(orderStr!!.toInt())
		}
		tp.setOsmTag(otag)
		tp.setNotEditableOsm("true" == parser.getAttributeValue("", "no_edit"))
		tp.setOsmValue(parser.getAttributeValue("", "value"))
		tp.setOsmTag2(parser.getAttributeValue("", "tag2"))
		tp.setOsmValue2(parser.getAttributeValue("", "value2"))
		tp.setPoiAdditionalCategory(poiAdditionalCategory)
		tp.setFilterOnly(parser.getAttributeValue("", "filter_only").toBoolean())
		tp.setTopIndex(parser.getAttributeValue("", "top_index").toBoolean())
		val maxPerMap = parser.getAttributeValue("", "max_per_map")
		if (!KAlgorithms.isEmpty(maxPerMap)) {
			tp.setMaxPerMap(maxPerMap!!.toInt())
		}
		val minCount = parser.getAttributeValue("", "min_count")
		if (!KAlgorithms.isEmpty(minCount)) {
			tp.setMinCount(minCount!!.toInt())
		}
		if (lastType != null) {
			lastType.addPoiAdditional(tp)
		} else if (lastFilter != null) {
			lastFilter.addPoiAdditional(tp)
		} else if (lastCategory != null) {
			lastCategory.addPoiAdditional(tp)
		}
		if (tp.isText()) {
			textPoiAdditionals.add(tp)
		}
		return tp
	}

	private fun parsePoiType(
		allTypes: MutableMap<String, PoiType>, parser: XmlPullParser, lastCategory: PoiCategory,
		lastFilter: PoiFilter?, lang: String?, langBaseType: PoiType?
	): PoiType {
		var oname = parser.getAttributeValue("", "name")!!
		if (lang != null) {
			oname += ":$lang"
		}
		val iconName = parser.getAttributeValue("", "icon")
		val tp = PoiType(this, lastCategory, lastFilter, oname, iconName)
		var otag = parser.getAttributeValue("", "tag")
		if (lang != null) {
			otag += ":$lang"
		}
		tp.setNonIndx(parser.getAttributeValue("", "no_indx").toBoolean())
		tp.setBaseLangType(langBaseType)
		tp.setLang(lang)
		tp.setOsmTag(otag)
		tp.setOsmValue(parser.getAttributeValue("", "value"))
		tp.setOsmEditTagValue(
			parser.getAttributeValue("", "edit_tag"), parser.getAttributeValue("", "edit_value")
		)
		tp.setOsmEditTagValue2(
			parser.getAttributeValue("", "edit_tag2"), parser.getAttributeValue("", "edit_value2")
		)
		tp.setOsmTag2(parser.getAttributeValue("", "tag2"))
		tp.setOsmValue2(parser.getAttributeValue("", "value2"))
		tp.setText("text" == parser.getAttributeValue("", "type"))
		tp.setHidden(parser.getAttributeValue("", "hidden").toBoolean())
		val orderStr = parser.getAttributeValue("", "order")
		if (!KAlgorithms.isEmpty(orderStr)) {
			tp.setOrder(orderStr!!.toInt())
		}
		tp.setNameOnly("true" == parser.getAttributeValue("", "name_only"))
		tp.setNameTag(parser.getAttributeValue("", "name_tag"))
		tp.setRelation("true" == parser.getAttributeValue("", "relation"))
		tp.setNotEditableOsm("true" == parser.getAttributeValue("", "no_edit"))
		tp.setTopVisible(parser.getAttributeValue("", "top").toBoolean())
		tp.setDefaultForCategory(parser.getAttributeValue("", "defaultForCategory").toBoolean())
		lastFilter?.addPoiType(tp)
		allTypes[tp.getKeyName()] = tp
		lastCategory.addPoiType(tp)
		if ("true" == parser.getAttributeValue("", "basemap")) {
			lastCategory.addBasemapPoi(tp)
		}
		return tp
	}

	fun getCategories(includeMapCategory: Boolean): List<PoiCategory> {
		val lst = ArrayList(categories)
		if (!includeMapCategory) {
			lst.remove(getOtherMapCategory())
		}
		return lst
	}

	private fun getPoiAdditionalByKey(p: AbstractPoiType, name: String): PoiType? =
		p.getPoiAdditionalByKeyName(name)

	fun getTextPoiAdditionalByKey(name: String): PoiType? {
		for (pt in textPoiAdditionals) {
			if (pt.getKeyName() == name) {
				return pt
			}
		}
		return null
	}

	fun getPoiAdditionalType(category: PoiCategory, name: String): AbstractPoiType? {
		var add = getPoiAdditionalByKey(category, name)
		if (add != null) {
			return add
		}
		for (pf in category.getPoiFilters()) {
			add = getPoiAdditionalByKey(pf, name)
			if (add != null) {
				return add
			}
		}
		for (p in category.getPoiTypes()) {
			add = getPoiAdditionalByKey(p, name)
			if (add != null) {
				return add
			}
		}
		return null
	}

	private val emptyPoiType: AbstractPoiType = object : AbstractPoiType("EMPTY", this) {
		override fun putTypes(
			acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
		): MutableMap<PoiCategory, LinkedHashSet<String>?> = LinkedHashMap()
	}

	/** Remembers the misses too, since most keys asked for are not additional attributes. */
	fun getAnyPoiAdditionalTypeByKey(name: String): AbstractPoiType? {
		var pt = synchronized(dynCacheLock) { dynCacheByKey[name] }
		if (pt == null) {
			pt = getAnyPoiAdditionalTypeByKeyNoCache(name) ?: emptyPoiType
			synchronized(dynCacheLock) { dynCacheByKey[name] = pt }
		}
		if (pt === emptyPoiType) {
			return null
		}
		return pt
	}

	private fun getAnyPoiAdditionalTypeByKeyNoCache(name: String): AbstractPoiType? {
		for (pc in categories) {
			val add = getPoiAdditionalType(pc, name)
			if (add != null) {
				return add
			}
		}
		return null
	}

	fun getSynonyms(abstractPoiType: AbstractPoiType): String {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			val translation = poiTranslator.getSynonyms(abstractPoiType)
			if (!KAlgorithms.isEmpty(translation)) {
				return translation!!
			}
		}
		return ""
	}

	fun getEnTranslation(abstractPoiType: AbstractPoiType): String {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			val translation = poiTranslator.getEnTranslation(abstractPoiType)
			if (!KAlgorithms.isEmpty(translation)) {
				return translation!!
			}
		}
		return getBasePoiName(abstractPoiType)
	}

	fun getTranslation(abstractPoiType: AbstractPoiType): String {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			val translation = poiTranslator.getTranslation(abstractPoiType)
			if (!KAlgorithms.isEmpty(translation)) {
				return translation!!
			}
		}
		return getBasePoiName(abstractPoiType)
	}

	fun hasValidTranslation(poiType: AbstractPoiType): Boolean {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			return !KAlgorithms.isEmpty(poiTranslator.getTranslation(poiType))
		}
		return false
	}

	fun getAllLanguagesTranslationSuffix(): String? {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			return poiTranslator.getAllLanguagesTranslationSuffix()
		}
		return "all languages"
	}

	/** The key name spelled out, which is what a type is called when nothing translated it. */
	fun getBasePoiName(abstractPoiType: AbstractPoiType): String {
		var name = abstractPoiType.getKeyName()
		if (name.startsWith("osmand_")) {
			name = name.substring("osmand_".length)
		}
		if (name.startsWith("amenity_")) {
			name = name.substring("amenity_".length)
		}
		name = name.replace('_', ' ')
		return capitalizeFirstLetterAndLowercase(name)
	}

	fun getPoiTranslation(keyName: String): String? = getPoiTranslation(keyName, true)

	fun getPoiTranslation(keyName: String, withDefault: Boolean): String? {
		val poiTranslator = this.poiTranslator
		if (poiTranslator != null) {
			val translation = poiTranslator.getTranslation(keyName)
			if (!KAlgorithms.isEmpty(translation)) {
				return translation
			}
		}
		if (withDefault) {
			return KAlgorithms.capitalizeFirstLetter(keyName.replace('_', ' '))
		}
		return null
	}

	fun isRegisteredType(t: PoiCategory): Boolean = getPoiCategoryByName(t.getKeyName()) != otherCategory

	fun initPoiTypesByTag() {
		if (poiTypesByTag.isNotEmpty()) {
			return
		}
		for (poic in categories) {
			for (p in poic.getPoiTypes()) {
				initPoiType(p)
				for (pts in p.getPoiAdditionals()) {
					initPoiType(pts)
				}
			}
			for (p in poic.getPoiAdditionals()) {
				initPoiType(p)
			}
		}
	}

	private fun initPoiType(p: PoiType) {
		if (!p.isReference() && !KAlgorithms.isEmpty(p.getRawOsmTag())) {
			if (p.isDefaultForCategory()) {
				initDefaultPoiType(p)
			}
			val key = if (p.isAdditional()) {
				if (p.isText()) p.getRawOsmTag()!! else p.getRawOsmTag() + "/" + p.getOsmValue()
			} else {
				p.getRawOsmTag() + "/" + p.getOsmValue()
			}
			if (poiTypesByTag.containsKey(key)) {
				throw UnsupportedOperationException("!! Duplicate poi type $key")
			}
			poiTypesByTag[key] = p
		}
	}

	private fun initDefaultPoiType(p: PoiType) {
		if (!p.isReference() && !KAlgorithms.isEmpty(p.getRawOsmTag()) && p.isDefaultForCategory()) {
			val pc = p.getCategory()
				?: throw UnsupportedOperationException("!! Default tag is not set for category null")
			val tag = pc.getDefaultTag()
			if (KAlgorithms.isEmpty(tag)) {
				throw UnsupportedOperationException("!! Default tag is not set for category ${pc.getKeyName()}")
			}
			if (defaultPoiTypesByTag.containsKey(tag)) {
				throw UnsupportedOperationException(
					"!! Duplicate default poi type $tag for category ${pc.getKeyName()}"
				)
			}
			defaultPoiTypesByTag[tag] = p
		}
	}

	/** A subtype an older map still writes, renamed to the one in use now. */
	fun replaceDeprecatedSubtype(type: PoiCategory?, subtype: String): String =
		deprecatedTags[subtype] ?: subtype

	fun getPoiTypeByTagValue(tag: String, value: String?): PoiType? {
		initPoiTypesByTag()
		return poiTypesByTag["$tag/$value"] ?: poiTypesByTag[tag]
	}

	fun getDefaultPoiTypeByTag(tag: String): PoiType? {
		initPoiTypesByTag()
		return defaultPoiTypesByTag[tag]
	}

	fun isTextAdditionalInfo(key: String, value: String?): Boolean {
		if (key.startsWith("name:") || key == "name") {
			return true
		}
		val pat = getAnyPoiAdditionalTypeByKey(key) as? PoiType ?: return true
		return pat.isText()
	}

	fun setForbiddenTypes(forbiddenTypes: Set<String>) {
		this.forbiddenTypes = forbiddenTypes
	}

	fun isTypeForbidden(typeName: String): Boolean = forbiddenTypes.contains(typeName)

	fun getPublicTransportTypes(): List<String>? {
		if (publicTransportTypes == null && init) {
			val category = getPoiCategoryByName("transportation")
			if (category != null) {
				val types = ArrayList<String>()
				publicTransportTypes = types
				for (poiFilter in category.getPoiFilters()) {
					if (poiFilter.getKeyName() == "public_transport" || poiFilter.getKeyName() == "water_transport") {
						for (poiType in poiFilter.getPoiTypes()) {
							types.add(poiType.getKeyName())
							for (poiAdditionalType in poiType.getPoiAdditionals()) {
								types.add(poiAdditionalType.getKeyName())
							}
						}
					}
				}
			}
		}
		return publicTransportTypes
	}

	private fun reindexCategories() {
		poiCategoryIndex.clear()
		for (i in categories.indices) {
			poiCategoryIndex[categories[i].getKeyName()] = i
		}
	}

	companion object {
		const val OTHER_MAP_CATEGORY: String = "Other"

		const val WIKI_LANG: String = "wiki_lang"
		const val WIKI_PLACE: String = "wiki_place"
		const val OSM_WIKI_CATEGORY: String = "osmwiki"
		const val ADMINISTRATIVE_CATEGORY: String = "administrative"
		const val SPEED_CAMERA: String = "speed_camera"

		const val ROUTES: String = "routes"
		const val ROUTE_ARTICLE: String = "route_article"
		const val ROUTE_ARTICLE_POINT: String = "route_article_point"
		const val CATEGORY: String = "category"
		const val ROUTE_TRACK: String = "route_track" // routes:route_track (no activity)
		const val ROUTES_PREFIX: String = "routes_" // routes:routes_xxx (any activity type)
		const val ROUTE_TRACK_POINT: String = "route_track_point"

		const val TOP_INDEX_ADDITIONAL_PREFIX: String = "top_index_"

		private val log = LoggerFactory.getLogger("MapPoiTypes")

		private var DEFAULT_INSTANCE: MapPoiTypes? = null

		@JvmStatic
		fun getDefaultNoInit(): MapPoiTypes {
			var instance = DEFAULT_INSTANCE
			if (instance == null) {
				instance = MapPoiTypes(null)
				DEFAULT_INSTANCE = instance
			}
			return instance
		}

		/**
		 * The platform reads poi_types.xml once at startup and hands the registry over here. Java
		 * reads the file itself at this point; a registry that has already read it is left alone,
		 * which is how a platform that has no file to point at - iOS reads the bundle as text -
		 * gets its registry in.
		 */
		@JvmStatic
		fun setDefault(types: MapPoiTypes) {
			DEFAULT_INSTANCE = types
			if (!types.isInit()) {
				types.init()
			}
		}

		/**
		 * Java falls back to reading poi_types.xml off the classpath here; there is no classpath
		 * on Kotlin/Native, so an uninitialized registry is a mistake rather than something to
		 * paper over.
		 */
		@JvmStatic
		fun getDefault(): MapPoiTypes =
			DEFAULT_INSTANCE ?: throw IllegalStateException(
				"MapPoiTypes is not initialized: the platform has to read poi_types.xml and call setDefault"
			)

		internal fun capitalizeFirstLetterAndLowercase(s: String): String {
			if (s.length > 1) {
				// not very efficient algorithm
				return s[0].uppercaseChar() + s.substring(1).lowercase()
			}
			return s
		}
	}
}
