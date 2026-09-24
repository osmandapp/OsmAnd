package net.osmand.shared.map

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.MapIndex
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.search.core.NameStringMatcher
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KMapAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTroveHashOrder
import net.osmand.shared.util.primaryCollator
import net.osmand.shared.util.synchronized
import okio.IOException
import kotlin.jvm.JvmStatic
import kotlin.math.abs

/**
 * The regions of the world out of `regions.ocbf`: the tree of [WorldRegion]s the downloads offer,
 * and the boundaries that say which of them a point is in.
 *
 * `regions.ocbf` is an obf file with a map section and nothing else. Each downloadable region is
 * one or more objects of it, whose names carry the region's names and parameters; the objects
 * tagged `boundary` are extra polygons of a region, its inclusions and exclusions.
 *
 * A copy of `OsmandRegions` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. Left out:
 * - `prepareFile()` without a path, which copies the file out of the java resources; a platform
 *   hands this copy the path of its own `regions.ocbf`;
 * - `cacheAllCountries`, with the quad tree and the country lists it fills and `containsCountry`,
 *   which reads them. Only `main()` calls it, so `query` always searches the file, as it does in
 *   the apps;
 * - `main()` and `testCountry`.
 *
 * The regions keep the order the file lists them in, on every platform: [getAllRegionData] comes
 * out of a `HashMap` in java, whose order is its own, and here it is the file's.
 */
class OsmandRegions {

	private var reader: BinaryMapIndexReader? = null
	private var locale = "en"

	// locale including region
	private var locale2: String? = null

	private var worldRegion = WorldRegion(WorldRegion.WORLD)
	private val fullNamesToRegionData = LinkedHashMap<String, WorldRegion>()
	private val downloadNamesToFullNames = HashMap<String, String>()

	private val mapIndexFields = MapIndexFields()
	private var translator: RegionTranslation? = null
	private var isInit = false
	private val lock = KLock()

	/** The encoding rules of the names a region object carries, looked up once from the first object. */
	private class MapIndexFields {
		var initialized = false
		var parentFullName: Int? = null
		var fullNameType: Int? = null
		var downloadNameType: Int? = null
		var nameEnType: Int? = null
		var nameType: Int? = null
		var nameLocaleType: Int? = null
		var nameLocale2Type: Int? = null
		var langType: Int? = null
		var metricType: Int? = null
		var leftHandDrivingType: Int? = null
		var roadSignsType: Int? = null
		var wikiLinkType: Int? = null
		var populationType: Int? = null

		fun get(tp: Int?, obj: BinaryMapDataObject): String? {
			if (tp == null) {
				return null
			}
			return obj.getNameByType(tp)
		}
	}

	constructor(isInitialized: Boolean) {
		this.isInit = isInitialized
	}

	constructor(fileName: String) {
		prepareFile(fileName)
	}

	fun isInitialized(): Boolean = isInit

	fun setTranslator(translator: RegionTranslation?) {
		this.translator = translator
	}

	fun prepareFile(fileName: String): BinaryMapIndexReader {
		val reader = BinaryMapIndexReader(fileName)
		this.reader = reader
		val parentRelations = LinkedHashMap<String, String>()
		val unattachedBoundaryMapObjectsByRegions = HashMap<String?, MutableList<BinaryMapDataObject>>()
		val resultMatcher = object : ResultMatcher<BinaryMapDataObject> {

			override fun publish(obj: BinaryMapDataObject): Boolean {
				initTypes(obj)

				var boundary = false
				val mapIndex = obj.getMapIndex()
				for (type in obj.getTypes() ?: IntArray(0)) {
					val tp = mapIndex?.decodeType(type)
					if ("boundary" == tp?.value) {
						boundary = true
						break
					}
				}

				if (boundary) {
					val fullRegionName = getFullName(obj)
					val region = fullNamesToRegionData[fullRegionName]
					if (region != null) {
						addPolygonToRegion(obj, region)
					} else {
						unattachedBoundaryMapObjectsByRegions.getOrPut(fullRegionName) { ArrayList() }.add(obj)
					}
					return false
				}

				val region = initRegionData(parentRelations, obj) ?: return false

				val unattachedMapObjects = unattachedBoundaryMapObjectsByRegions[region.regionFullName]
				if (unattachedMapObjects != null) {
					for (mapObject in unattachedMapObjects) {
						addPolygonToRegion(mapObject, region)
					}
					unattachedBoundaryMapObjectsByRegions.remove(region.regionFullName)
				}

				val regionDownloadName = region.regionDownloadName
				if (regionDownloadName != null) {
					downloadNamesToFullNames[regionDownloadName] = region.regionFullName
				}
				fullNamesToRegionData[region.regionFullName] = region
				return false
			}

			override fun isCancelled(): Boolean = false
		}
		iterateOverAllObjects(resultMatcher)
		// post process download names
		for ((fullName, parentFullName) in parentRelations) {
			// String parentParentFulName = parentRelations.get(parentFullName); // could be used for japan/russia
			val rd = fullNamesToRegionData[fullName]
			val parent = fullNamesToRegionData[parentFullName]
			if (parent != null && rd != null) {
				parent.addSubregion(rd)
			}
		}
		structureWorldRegions(ArrayList(fullNamesToRegionData.values))
		this.isInit = true
		return reader
	}

	fun getLocaleName(downloadName: String, includingParent: Boolean): String =
		getLocaleName(downloadName, includingParent, false)

	fun getLocaleName(downloadName: String, includingParent: Boolean, reversed: Boolean): String =
		getLocaleName(downloadName, includingParent, null, reversed)

	fun getLocaleName(
		downloadName: String, includingParent: Boolean, baseParentRegion: WorldRegion?, reversed: Boolean
	): String {
		val divider = if (reversed) ", " else " "
		return getLocaleName(downloadName, divider, includingParent, baseParentRegion, reversed)
	}

	fun getLocaleName(downloadName: String, divider: String, includingParent: Boolean, reversed: Boolean): String =
		getLocaleName(downloadName, divider, includingParent, null, reversed)

	fun getLocaleName(
		downloadName: String, divider: String, includingParent: Boolean, baseParentRegion: WorldRegion?,
		reversed: Boolean
	): String {
		val lc = downloadName.lowercase()
		val fullName = downloadNamesToFullNames[lc]
		if (fullName != null) {
			return getLocaleNameByFullName(fullName, divider, includingParent, baseParentRegion, reversed)
		}
		return downloadName.replace('_', ' ')
	}

	fun getLocaleNameByFullName(fullName: String, divider: String, includingParent: Boolean, reversed: Boolean): String =
		getLocaleNameByFullName(fullName, divider, includingParent, null, reversed)

	fun getLocaleNameByFullName(
		fullName: String, divider: String, includingParent: Boolean, baseParentRegion: WorldRegion?,
		reversed: Boolean
	): String {
		val region = fullNamesToRegionData[fullName] ?: return fullName.replace('_', ' ')
		val regionName = region.getLocaleName()
		val parent = region.getSuperregion()
		if (includingParent && parent != null) {
			val parentParent = parent.getSuperregion()

			if (parentParent != null) {
				val parentParentId = parentParent.getRegionId()
				if (WorldRegion.WORLD == parentParentId && parent.getRegionId() != WorldRegion.RUSSIA_REGION_ID) {
					return regionName
				}
				if (WorldRegion.RUSSIA_REGION_ID == parentParentId || WorldRegion.JAPAN_REGION_ID == parentParentId) {
					val format = if (reversed) LOCALE_NAME_REVERSED_FORMAT else LOCALE_NAME_DEFAULT_FORMAT
					return format.replace("%1\$s", parentParent.getLocaleName()).replace("%2\$s", regionName)
				}
			}
			val superRegions = region.getSuperRegions(baseParentRegion)
			if (superRegions.isNotEmpty()) {
				return getLocaleNameWithParent(superRegions, regionName, divider, reversed)
			}
		}
		return regionName
	}

	private fun getLocaleNameWithParent(
		superRegions: List<WorldRegion>, regionName: String, divider: String, reversed: Boolean
	): String {
		val builder = StringBuilder()
		val topRegionsIds = getTopRegionsIds()
		if (reversed) {
			builder.append(regionName)
			for (region in superRegions) {
				val regionId = region.getRegionId()
				if ((!topRegionsIds.contains(regionId) || WorldRegion.RUSSIA_REGION_ID == regionId)
					&& WorldRegion.WORLD != regionId
				) {
					builder.append(divider).append(region.getLocaleName())
				}
			}
		} else {
			for (i in superRegions.indices.reversed()) {
				val region = superRegions[i]
				val regionId = region.getRegionId()
				if ((!topRegionsIds.contains(regionId) || WorldRegion.RUSSIA_REGION_ID == regionId)
					&& WorldRegion.WORLD != regionId
				) {
					builder.append(region.getLocaleName()).append(divider)
				}
			}
			builder.append(regionName)
		}
		return builder.toString()
	}

	fun getWorldRegion(): WorldRegion = worldRegion

	fun getReader(): BinaryMapIndexReader? = reader

	fun getCountryName(ll: KLatLon): String? {
		val y = KMapUtils.get31TileNumberY(ll.latitude)
		val x = KMapUtils.get31TileNumberX(ll.longitude)
		try {
			val list = query(x, y)
			for (o in list) {
				if (contain(o, x, y)) {
					val name = getLocaleName(o)
					if (name != null) {
						return name
					}
				}
			}
		} catch (e: IOException) {
			LOG.error(e.message, e)
		}
		return null
	}

	private fun getLocaleName(o: BinaryMapDataObject): String? {
		val name = mapIndexFields.get(mapIndexFields.nameType, o) ?: return null
		val region = fullNamesToRegionData[getFullName(o)]
		return region?.getLocaleName() ?: name
	}

	fun query(lx: Int, rx: Int, ty: Int, by: Int): List<BinaryMapDataObject> = query(lx, rx, ty, by, true)

	fun query(lx: Int, rx: Int, ty: Int, by: Int, checkCenter: Boolean): List<BinaryMapDataObject> =
		queryBboxNoInit(lx, rx, ty, by, checkCenter)

	fun query(tile31x: Int, tile31y: Int): List<BinaryMapDataObject> =
		queryBboxNoInit(tile31x, tile31x, tile31y, tile31y, true)

	private fun queryBboxNoInit(
		lx: Int, rx: Int, ty: Int, by: Int, checkCenter: Boolean
	): List<BinaryMapDataObject> = synchronized(lock) {
		val result = ArrayList<BinaryMapDataObject>()
		val mx = lx / 2 + rx / 2
		val my = ty / 2 + by / 2
		val sr = SearchRequest.buildSearchRequest(lx, rx, ty, by, 5, ACCEPT_ALL,
			object : ResultMatcher<BinaryMapDataObject> {

				override fun publish(obj: BinaryMapDataObject): Boolean {
					if (obj.getPointsLength() < 1) {
						return false
					}
					initTypes(obj)
					if (!checkCenter || contain(obj, mx, my)) {
						result.add(obj)
					}
					return false
				}

				override fun isCancelled(): Boolean = false
			}
		)
		sr.log = false
		val reader = this.reader ?: throw IOException("Reader == null")
		reader.searchMapIndex(sr)
		result
	}

	fun setLocale(locale: String) {
		setLocale(locale, null)
	}

	fun setLocale(locale: String, country: String?) {
		this.locale = locale
		// Check locale and give 2 locale names
		if ("zh" == locale) {
			if ("TW".equals(country, ignoreCase = true)) {
				this.locale2 = "zh-hant"
			} else if ("CN".equals(country, ignoreCase = true)) {
				this.locale2 = "zh-hans"
			}
		}
	}

	fun getRegionData(fullName: String?): WorldRegion? {
		if (WorldRegion.WORLD == fullName) {
			return worldRegion
		}
		return fullNamesToRegionData[fullName]
	}

	fun getCountryRegionDataByDownloadName(downloadName: String?): WorldRegion? =
		getRegionDataByDownloadName(downloadName)?.getCountryRegion()

	fun getRegionDataByDownloadName(downloadName: String?): WorldRegion? {
		if (downloadName == null) {
			return null
		}
		return getRegionData(downloadNamesToFullNames[downloadName.lowercase()])
	}

	fun getDownloadName(o: BinaryMapDataObject): String? = mapIndexFields.get(mapIndexFields.downloadNameType, o)

	fun getFullName(o: BinaryMapDataObject): String? = mapIndexFields.get(mapIndexFields.fullNameType, o)

	fun getFlattenedWorldRegionIds(): List<String> {
		val regionIds = ArrayList<String>()
		for (region in getFlattenedWorldRegions()) {
			regionIds.add(region.getRegionId())
		}
		return regionIds
	}

	fun getFlattenedWorldRegions(): List<WorldRegion> {
		val result = ArrayList<WorldRegion>()
		result.add(getWorldRegion())
		result.addAll(getAllRegionData())
		return result
	}

	fun getAllRegionData(): List<WorldRegion> = ArrayList(fullNamesToRegionData.values)

	private fun initRegionData(parentRelations: MutableMap<String, String>, obj: BinaryMapDataObject): WorldRegion? {
		val regionDownloadName = mapIndexFields.get(mapIndexFields.downloadNameType, obj)
		val regionFullName = mapIndexFields.get(mapIndexFields.fullNameType, obj)
		if (regionFullName.isNullOrEmpty()) {
			return null
		}
		val rd = WorldRegion(regionFullName, regionDownloadName)
		var cx = 0.0
		var cy = 0.0
		for (i in 0 until obj.getPointsLength()) {
			cx += obj.getPoint31XTile(i)
			cy += obj.getPoint31YTile(i)
		}
		if (obj.getPointsLength() > 0) {
			cx /= obj.getPointsLength()
			cy /= obj.getPointsLength()
			rd.regionCenter = KLatLon(KMapUtils.get31LatitudeY(cy.toInt()), KMapUtils.get31LongitudeX(cx.toInt()))
			findBoundaries(rd, obj)
		}

		val regionParentFullName = mapIndexFields.get(mapIndexFields.parentFullName, obj)
		rd.regionParentFullName = regionParentFullName
		if (!regionParentFullName.isNullOrEmpty()) {
			parentRelations[rd.regionFullName] = regionParentFullName
		}
		rd.regionName = mapIndexFields.get(mapIndexFields.nameType, obj)
		if (mapIndexFields.nameLocale2Type != null) {
			rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocale2Type, obj)
		}
		if (rd.regionNameLocale == null) {
			rd.regionNameLocale = mapIndexFields.get(mapIndexFields.nameLocaleType, obj)
		}
		rd.regionNameEn = mapIndexFields.get(mapIndexFields.nameEnType, obj)
		rd.params.regionLang = mapIndexFields.get(mapIndexFields.langType, obj)
		rd.params.regionLeftHandDriving = mapIndexFields.get(mapIndexFields.leftHandDrivingType, obj)
		rd.params.regionMetric = mapIndexFields.get(mapIndexFields.metricType, obj)
		rd.params.regionRoadSigns = mapIndexFields.get(mapIndexFields.roadSignsType, obj)
		rd.params.wikiLink = mapIndexFields.get(mapIndexFields.wikiLinkType, obj)
		rd.params.population = mapIndexFields.get(mapIndexFields.populationType, obj)
		rd.regionSearchText = getSearchIndex(obj)
		rd.regionMapDownload = isDownloadOfType(obj, MAP_TYPE)
		rd.regionRoadsDownload = isDownloadOfType(obj, ROADS_TYPE)
		rd.regionJoinMapDownload = isDownloadOfType(obj, MAP_JOIN_TYPE)
		rd.regionJoinRoadsDownload = isDownloadOfType(obj, ROADS_JOIN_TYPE)

		return rd
	}

	private fun findBoundaries(region: WorldRegion, obj: BinaryMapDataObject) {
		val pointsLength = obj.getPointsLength()
		if (pointsLength == 0) {
			return
		}
		val polygon = FloatArray(pointsLength * 2)
		var x = KMapUtils.get31LongitudeX(obj.getPoint31XTile(0)).toFloat()
		var y = KMapUtils.get31LatitudeY(obj.getPoint31YTile(0)).toFloat()

		polygon[0] = y // Latitude
		polygon[1] = x // Longitude

		var minX = x
		var maxX = x
		var minY = y
		var maxY = y

		if (pointsLength > 1) {
			for (i in 1 until pointsLength) {
				x = KMapUtils.get31LongitudeX(obj.getPoint31XTile(i)).toFloat()
				y = KMapUtils.get31LatitudeY(obj.getPoint31YTile(i)).toFloat()
				if (x > maxX) {
					maxX = x
				} else if (x < minX) {
					minX = x
				}
				if (y < maxY) {
					maxY = y
				} else if (y > minY) {
					minY = y
				}
				polygon[i * 2] = y // Latitude
				polygon[i * 2 + 1] = x // Longitude
			}
		}
		region.polygon = polygon
		region.boundingBox = KQuadRect(minX.toDouble(), minY.toDouble(), maxX.toDouble(), maxY.toDouble())
	}

	/**
	 * The names a region can be searched by, lowercased and joined with spaces. A name that is part
	 * of one already taken is left out, so which names are kept depends on the order they are
	 * walked in: java walks its trove map of names, and so does this, in the same order.
	 */
	private fun getSearchIndex(obj: BinaryMapDataObject): String {
		val mi = obj.getMapIndex()
		val names = obj.getObjectNames()
		val order = obj.getNamesOrder()
		val ind = StringBuilder()
		if (mi == null || names == null || order == null) {
			return ind.toString()
		}
		for (key in KTroveHashOrder.intObjectMapKeys(order)) {
			val tag = mi.decodeType(key)?.tag ?: continue
			val value = names[key] ?: continue
			if (tag.startsWith("name") || tag == "key_name"
				|| tag.startsWith("alt_name") || tag.startsWith("short_name")
				|| tag == "name:abbreviation" || tag == "ref"
			) {
				var vl = value.lowercase()
				if (tag == "ref" || tag.startsWith("alt_name")) {
					vl = removeElementsWithNumbers(vl) // see testRegionSearchMatching()
				}
				if (vl.isNotEmpty() && (tag == "ref" || ind.indexOf(vl) == -1)) {
					ind.append(" ").append(vl)
				}
			}
		}
		return ind.toString()
	}

	private fun removeElementsWithNumbers(value: String): String {
		val values = ArrayList<String>()
		for (item in WorldRegion.splitLikeJava(value, ";")) {
			var containsNumber = false
			for (token in KSearchAlgorithms.splitAndNormalize(item.replace('-', ' '), false)) {
				if (KAlgorithms.isInt(token)) {
					containsNumber = true
					break
				}
			}
			if (!containsNumber) {
				values.add(item)
			}
		}
		return values.joinToString(";")
	}

	fun isDownloadOfType(obj: BinaryMapDataObject, type: String): Boolean {
		val mapIndex = obj.getMapIndex() ?: return false
		for (addtype in obj.getAdditionalTypes() ?: return false) {
			val tp = mapIndex.decodeType(addtype)
			if (type == tp?.tag && "yes" == tp.value) {
				return true
			}
		}
		return false
	}

	private fun iterateOverAllObjects(resultMatcher: ResultMatcher<BinaryMapDataObject>) {
		synchronized(lock) {
			val sr = SearchRequest.buildSearchRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, 5, ACCEPT_ALL, resultMatcher)
			reader?.searchMapIndex(sr)
		}
	}

	private fun initTypes(obj: BinaryMapDataObject) {
		if (!mapIndexFields.initialized) {
			val mapIndex = obj.getMapIndex() ?: return
			mapIndexFields.initialized = true
			mapIndexFields.downloadNameType = mapIndex.getRule(FIELD_DOWNLOAD_NAME, null)
			mapIndexFields.nameType = mapIndex.getRule(FIELD_NAME, null)
			mapIndexFields.nameEnType = mapIndex.getRule(FIELD_NAME_EN, null)
			mapIndexFields.nameLocaleType = mapIndex.getRule("$FIELD_NAME:$locale", null)
			val locale2 = this.locale2
			if (locale2 != null) {
				mapIndexFields.nameLocale2Type = mapIndex.getRule("$FIELD_NAME:$locale2", null)
			}
			mapIndexFields.parentFullName = mapIndex.getRule(FIELD_REGION_PARENT_NAME, null)
			mapIndexFields.fullNameType = mapIndex.getRule(FIELD_REGION_FULL_NAME, null)
			mapIndexFields.langType = mapIndex.getRule(FIELD_LANG, null)
			mapIndexFields.metricType = mapIndex.getRule(FIELD_METRIC, null)
			mapIndexFields.leftHandDrivingType = mapIndex.getRule(FIELD_LEFT_HAND_DRIVING, null)
			mapIndexFields.roadSignsType = mapIndex.getRule(FIELD_ROAD_SIGNS, null)
			mapIndexFields.wikiLinkType = mapIndex.getRule(FIELD_WIKI_LINK, null)
			mapIndexFields.populationType = mapIndex.getRule(FIELD_POPULATION, null)
		}
	}

	fun interface RegionTranslation {

		fun getTranslation(id: String): String?
	}

	private fun initWorldRegion(world: WorldRegion, id: String) {
		val rg = WorldRegion(id)
		rg.regionParentFullName = world.regionFullName
		val translator = this.translator
		if (translator != null) {
			rg.regionName = translator.getTranslation(id)
		}
		world.addSubregion(rg)
	}

	private fun getTopRegionsIds(): List<String> {
		val regionIds = ArrayList<String>()
		regionIds.add(WorldRegion.ANTARCTICA_REGION_ID)
		regionIds.add(WorldRegion.AFRICA_REGION_ID)
		regionIds.add(WorldRegion.ASIA_REGION_ID)
		regionIds.add(WorldRegion.CENTRAL_AMERICA_REGION_ID)
		regionIds.add(WorldRegion.EUROPE_REGION_ID)
		regionIds.add(WorldRegion.NORTH_AMERICA_REGION_ID)
		regionIds.add(WorldRegion.RUSSIA_REGION_ID)
		regionIds.add(WorldRegion.SOUTH_AMERICA_REGION_ID)
		regionIds.add(WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID)
		return regionIds
	}

	fun structureWorldRegions(loadedItems: MutableList<WorldRegion>) {
		if (loadedItems.size == 0) {
			return
		}
		val world = WorldRegion(WorldRegion.WORLD)
		for (regionId in getTopRegionsIds()) {
			initWorldRegion(world, regionId)
		}
		val it = loadedItems.iterator()
		while (it.hasNext()) {
			val region = it.next()
			if (region.superregion == null) {
				var found = false
				for (worldSubregion in world.subregions) {
					if (worldSubregion.getRegionId().equals(region.regionFullName, ignoreCase = true)) {
						for (rg in region.subregions) {
							worldSubregion.addSubregion(rg)
						}
						found = true
						break
					}
				}
				if (found) {
					it.remove()
				} else if (region.getRegionId().contains("basemap")) {
					it.remove()
				} else if (region.getRegionId().startsWith("World_")) {
					it.remove()
				}
			} else {
				it.remove()
			}
		}
		val collator = primaryCollator()
		val nameComparator = Comparator<WorldRegion> { w1, w2 -> collator.compare(w1.getLocaleName(), w2.getLocaleName()) }
		sortSubregions(world, nameComparator)

		this.worldRegion = world
		if (loadedItems.size > 0) {
			LOG.warn("Found orphaned regions: " + loadedItems.size)
			for (regionId in loadedItems) {
				LOG.warn("FullName = " + regionId.regionFullName + " parent=" + regionId.regionParentFullName)
			}
		}
	}

	private fun sortSubregions(region: WorldRegion, comparator: Comparator<WorldRegion>) {
		region.subregions.sortWith(comparator)
		for (r in region.subregions) {
			if (r.subregions.size > 0) {
				sortSubregions(r, comparator)
			}
		}
	}

	fun getWorldRegionsAt(latLon: KLatLon): List<WorldRegion> = getWorldRegionsAt(latLon, false)

	fun getWorldRegionsAt(latLon: KLatLon, includeRoadRegions: Boolean): List<WorldRegion> {
		val mapDataObjects = getBinaryMapDataObjectsWithRegionsAt(latLon, includeRoadRegions)
		return ArrayList(mapDataObjects.keys)
	}

	fun getSmallestBinaryMapDataObjectAt(latLon: KLatLon): Map.Entry<WorldRegion, BinaryMapDataObject>? {
		val mapDataObjectsWithRegions = getBinaryMapDataObjectsWithRegionsAt(latLon)
		return getSmallestBinaryMapDataObjectAt(mapDataObjectsWithRegions)
	}

	fun getSmallestBinaryMapDataObjectAt(
		mapDataObjectsWithRegions: Map<WorldRegion, BinaryMapDataObject>
	): Map.Entry<WorldRegion, BinaryMapDataObject>? {
		var res: Map.Entry<WorldRegion, BinaryMapDataObject>? = null
		var smallestArea = -1.0
		for (o in mapDataObjectsWithRegions.entries) {
			val area = getArea(o.value)
			if (smallestArea == -1.0) {
				smallestArea = area
				res = o
			} else if (area < smallestArea) {
				smallestArea = area
				res = o
			}
		}
		return res
	}

	private fun getBinaryMapDataObjectsWithRegionsAt(latLon: KLatLon): Map<WorldRegion, BinaryMapDataObject> =
		getBinaryMapDataObjectsWithRegionsAt(latLon, false)

	private fun getBinaryMapDataObjectsWithRegionsAt(
		latLon: KLatLon, includeRoadRegions: Boolean
	): Map<WorldRegion, BinaryMapDataObject> {
		val point31x = KMapUtils.get31TileNumberX(latLon.longitude)
		val point31y = KMapUtils.get31TileNumberY(latLon.latitude)
		val foundObjects = LinkedHashMap<WorldRegion, BinaryMapDataObject>()
		val mapDataObjects: List<BinaryMapDataObject>
		try {
			mapDataObjects = queryBboxNoInit(point31x, point31x, point31y, point31y, true)
		} catch (e: IOException) {
			throw IOException("Error while calling queryBbox")
		}
		for (o in mapDataObjects) {
			if (o.getTypes() != null) {
				val downloadRegion = getRegionData(getFullName(o))
				if (downloadRegion == null
					|| (if (includeRoadRegions) !downloadRegion.isRegionRoadsDownload() && !downloadRegion.isRegionMapDownload()
					else !downloadRegion.isRegionMapDownload())
					|| !contain(o, point31x, point31y)
				) {
					continue
				}
				foundObjects[downloadRegion] = o
			}
		}
		return foundObjects
	}

	fun getRegionsToDownload(lat: Double, lon: Double): List<BinaryMapDataObject> {
		val x31 = KMapUtils.get31TileNumberX(lon)
		val y31 = KMapUtils.get31TileNumberY(lat)
		return filterQueryResultsByPoint(query(x31, y31), x31, y31)
	}

	fun getRegionsToDownload(lat: Double, lon: Double, keyNames: MutableList<String>): List<String> {
		keyNames.clear()
		val x31 = KMapUtils.get31TileNumberX(lon)
		val y31 = KMapUtils.get31TileNumberY(lat)
		val cs = query(x31, y31)
		for (b in cs) {
			if (contain(b, x31, y31)) {
				val downloadName = getDownloadName(b)
				if (!downloadName.isNullOrEmpty()) {
					keyNames.add(downloadName)
				}
			}
		}
		return keyNames
	}

	private fun addPolygonToRegion(mapObject: BinaryMapDataObject, worldRegion: WorldRegion) {
		val pointsLength = mapObject.getPointsLength()
		if (pointsLength < 3) {
			return
		}
		val polygon = FloatArray(pointsLength * 2)
		for (i in 0 until pointsLength) {
			val x = mapObject.getPoint31XTile(i)
			val y = mapObject.getPoint31YTile(i)

			polygon[i * 2] = KMapUtils.get31LatitudeY(y).toFloat()
			polygon[i * 2 + 1] = KMapUtils.get31LongitudeX(x).toFloat()
		}
		worldRegion.additionalPolygons.add(polygon)
	}

	fun filterQueryResultsByPoint(objects: List<BinaryMapDataObject>, x: Int, y: Int): List<BinaryMapDataObject> {
		val filtered = ArrayList<BinaryMapDataObject>()
		val intersectionCounter = HashMap<String, Int>()
		for (o in objects) {
			if (contain(o, x, y)) {
				val k = getDownloadName(o)
				if (k.isNullOrEmpty()) {
					continue
				}
				intersectionCounter[k] = (intersectionCounter[k] ?: 0) + 1
			}
		}
		for (o in objects) {
			val k = getDownloadName(o)
			if (!k.isNullOrEmpty() && (intersectionCounter[k] ?: 0) % 2 == 1) {
				filtered.add(o) // odd intersections == outside exclusion polygons
			}
		}
		return filtered
	}

	fun close() {
		reader?.close()
	}

	fun getFile(): BinaryMapIndexReader? = reader

	companion object {
		const val REGIONS_OCBF = "regions.ocbf"

		const val MAP_TYPE = "region_map"
		const val ROADS_TYPE = "region_roads"
		const val MAP_JOIN_TYPE = "region_join_map"
		const val ROADS_JOIN_TYPE = "region_join_roads"

		const val FIELD_DOWNLOAD_NAME = "download_name"
		const val FIELD_NAME = "name"
		const val FIELD_NAME_EN = "name:en"
		const val FIELD_REGION_PARENT_NAME = "region_parent_name"
		const val FIELD_REGION_FULL_NAME = "region_full_name"
		const val FIELD_LANG = "region_lang"
		const val FIELD_METRIC = "region_metric"
		const val FIELD_ROAD_SIGNS = "region_road_signs"
		const val FIELD_LEFT_HAND_DRIVING = "region_left_hand_navigation"
		const val FIELD_WIKI_LINK = "region_wiki_link"
		const val FIELD_POPULATION = "region_population"
		const val LOCALE_NAME_DEFAULT_FORMAT = "%1\$s %2\$s"
		const val LOCALE_NAME_REVERSED_FORMAT = "%2\$s, %1\$s"

		private val LOG = LoggerFactory.getLogger("OsmandRegions")

		private val ACCEPT_ALL = object : SearchFilter {
			override fun accept(types: KTIntArrayList, index: MapIndex): Boolean = true
		}

		@JvmStatic
		fun contain(bo: BinaryMapDataObject, tx: Int, ty: Int): Boolean {
			var t = 0
			for (i in 1 until bo.getPointsLength()) {
				val fx = KMapAlgorithms.rayIntersectX(
					bo.getPoint31XTile(i - 1), bo.getPoint31YTile(i - 1),
					bo.getPoint31XTile(i), bo.getPoint31YTile(i), ty
				)
				if (Int.MIN_VALUE != fx && tx >= fx) {
					t++
				}
			}
			return t % 2 == 1
		}

		@JvmStatic
		fun intersect(bo: BinaryMapDataObject, lx: Int, ty: Int, rx: Int, by: Int): Boolean {
			// 1. polygon in object
			if (contain(bo, lx, ty)) {
				return true
			}
			// 2. object in polygon
			if (bo.getPointsLength() == 0) {
				return false
			}
			if (bo.getPoint31XTile(0) >= lx && bo.getPoint31XTile(0) <= rx &&
				bo.getPoint31YTile(0) >= ty && bo.getPoint31YTile(0) <= by
			) {
				return true
			}

			// 3. find intersection
			for (i in 1 until bo.getPointsLength()) {
				val px = bo.getPoint31XTile(i - 1)
				val x = bo.getPoint31XTile(i)
				val py = bo.getPoint31YTile(i - 1)
				val y = bo.getPoint31YTile(i)
				if (x < lx && px < lx) {
					continue
				} else if (x > rx && px > rx) {
					continue
				} else if (y > by && py > by) {
					continue
				} else if (y < ty && py < ty) {
					continue
				}
				val inter = KMapAlgorithms.calculateIntersection(px, py, x, y, lx, rx, by, ty)
				if (inter != -1L) {
					return true
				}
			}

			return false
		}

		@JvmStatic
		fun getArea(bo: BinaryMapDataObject): Double {
			var area = 0.0
			if (bo.getPointsLength() > 0) {
				for (i in 1 until bo.getPointsLength()) {
					val ax = bo.getPoint31XTile(i - 1).toDouble()
					val bx = bo.getPoint31XTile(i).toDouble()
					val ay = bo.getPoint31YTile(i - 1).toDouble()
					val by = bo.getPoint31YTile(i).toDouble()
					area += (bx + ax) * (by - ay) / 1.631E10
				}
			}
			return abs(area)
		}

		@JvmStatic
		fun isRegionNameMatched(query: String, regionName: String?): Boolean {
			return !regionName.isNullOrEmpty()
					&& NameStringMatcher(query, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE).matches(regionName)
		}
	}
}
