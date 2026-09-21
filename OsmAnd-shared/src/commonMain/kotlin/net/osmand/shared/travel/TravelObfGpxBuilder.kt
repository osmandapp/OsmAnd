package net.osmand.shared.travel

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.MapIndex
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_BACKGROUNDS
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_COLORS
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_DELIMITER
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_EMPTY_NAME_STUB
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_ICONS
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_NAMES
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_PREFIX
import net.osmand.shared.gpx.RouteActivityHelper
import net.osmand.shared.gpx.TravelObfGpxTrackOptimizer
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTES_PREFIX
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_TRACK
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_TRACK_POINT
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.travel.TravelGpx.Companion.ELE_GRAPH
import net.osmand.shared.travel.TravelGpx.Companion.ROUTE_ACTIVITY_TYPE
import net.osmand.shared.travel.TravelGpx.Companion.ROUTE_SEGMENT_INDEX
import net.osmand.shared.travel.TravelGpx.Companion.ROUTE_TYPE
import net.osmand.shared.travel.TravelGpx.Companion.START_ELEVATION
import net.osmand.shared.travel.TravelGpx.Companion.TAG_URL
import net.osmand.shared.travel.TravelGpx.Companion.TAG_URL_TEXT
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KMapAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.primaryCollator

/**
 * Turns an article into a gpx file: the track comes from the map section of the obf files, the
 * waypoints and every tag of the route come from their poi section, and the two are tied together
 * by route_id.
 *
 * A copy of `TravelObfGpxFileReader` in the android app, which stays there; this copy is for iOS.
 * Three things are left behind with it. It was an `AsyncTask` - this is a plain call, and the
 * caller decides what thread it runs on and shows its own progress. It approximated the altitudes
 * of a track against the routing graph afterwards, which needs the router - do that to the file
 * this returns. And it sorted waypoints with the ICU collator in numeric mode, which no platform
 * here has; [compareWaypointNames] is a hand-written stand-in, so "Station 9" still comes before
 * "Station 15", but two names the collator calls equal may end up the other way round.
 */
class TravelObfGpxBuilder(private val context: TravelObfContext) {

	private val collator: KCollator = primaryCollator()

	/** Asked between files, so that a build can be dropped when the screen behind it is gone. */
	fun interface Cancellable {
		fun isCancelled(): Boolean
	}

	fun buildGpxFile(
		repos: List<AmenityIndexRepository>, article: TravelArticle, isCancelled: Cancellable
	): GpxFile? {
		val segmentList = ArrayList<BinaryMapDataObject>()
		val gpxFileExtensions = LinkedHashMap<String, String>()
		val pointList = ArrayList<Amenity>()
		val pgNames = ArrayList<String>()
		val pgIcons = ArrayList<String>()
		val pgColors = ArrayList<String>()
		val pgBackgrounds = ArrayList<String>()

		val loaded = fetchSegmentsAndPoints(
			repos, article, segmentList, pointList, gpxFileExtensions,
			pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled
		)

		if (!loaded || isCancelled.isCancelled()) {
			return null
		}

		val gpxFile: GpxFile
		var isSuperRoute = false
		if (article is TravelGpx) {
			gpxFile = GpxFile(context.getAppVersion())
			gpxFile.metadata.name = article.title ?: article.routeId // path is name
			if (!KAlgorithms.isEmpty(article.title) && article.hasOsmRouteId()) {
				if (!gpxFileExtensions.containsKey("name")) {
					gpxFileExtensions["name"] = article.title!!
				}
			}
			if (!KAlgorithms.isEmpty(article.description)) {
				gpxFile.metadata.desc = article.description
			}
			isSuperRoute = article.isSuperRoute
		} else {
			val description = article.getDescription()
			val title = if (isValidFileName(description)) description else article.getTitle()
			gpxFile = GpxFile(title, article.getLang(), article.getContent())
			gpxFile.author = context.getAppVersion()
		}

		val url = gpxFileExtensions[TAG_URL]
		val urlText = gpxFileExtensions[TAG_URL_TEXT]
		if (url != null && urlText != null) {
			gpxFile.metadata.link = Link(url, urlText, null)
			gpxFileExtensions.remove(TAG_URL_TEXT)
			gpxFileExtensions.remove(TAG_URL)
		} else if (url != null) {
			gpxFile.metadata.link = Link(url)
			gpxFileExtensions.remove(TAG_URL)
		}

		val imageTitle = article.getImageTitle()
		if (!KAlgorithms.isEmpty(imageTitle)) {
			gpxFile.metadata.link = Link(TravelArticle.getImageUrl(imageTitle!!, false))
		}

		if (segmentList.isNotEmpty() || isSuperRoute) {
			var hasAltitude = false
			val track = Track()
			for (segment in segmentList) {
				val trkSegment = TrkSegment()
				for (i in 0 until segment.getPointsLength()) {
					val point = WptPt()
					point.lat = KMapUtils.get31LatitudeY(segment.getPoint31YTile(i))
					point.lon = KMapUtils.get31LongitudeX(segment.getPoint31XTile(i))
					trkSegment.points.add(point)
				}
				val eleGraph = segment.getTagValue(ELE_GRAPH)
				if (!KAlgorithms.isEmpty(eleGraph)) {
					hasAltitude = true
					val heightRes = KMapAlgorithms.decodeIntHeightArrayGraph(eleGraph, 3)
					val startEle = KAlgorithms.parseDoubleSilently(segment.getTagValue(START_ELEVATION), 0.0)
					KMapAlgorithms.augmentTrkSegmentWithAltitudes(trkSegment, heightRes, startEle)
				}
				track.segments.add(trkSegment)
			}
			gpxFile.tracks = mutableListOf()
			gpxFile.tracks.add(TravelObfGpxTrackOptimizer.mergeOverlappedSegmentsAtEdges(track))

			if (article !is TravelGpx) {
				article.ref?.let { gpxFile.setRef(it) }
			}
			gpxFile.hasAltitude = hasAltitude
			val activityType = gpxFileExtensions[GpxUtilities.ACTIVITY_TYPE]
			if (activityType != null) {
				gpxFile.metadata.getExtensionsToWrite()[GpxUtilities.ACTIVITY_TYPE] = activityType

				// cleanup type and activity tags
				gpxFileExtensions.remove(ROUTE_TYPE)
				gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE)
				gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE + "_" + activityType)
				gpxFileExtensions.remove(GpxUtilities.ACTIVITY_TYPE) // moved to the metadata
			}

			gpxFileExtensions.remove(EXTENSIONS_EXTRA_TAGS)?.let {
				gpxFile.getExtensionsToWrite().putAll(parseExtraTags(it))
			}
			gpxFileExtensions.remove(METADATA_EXTRA_TAGS)?.let {
				gpxFile.metadata.getExtensionsToWrite().putAll(parseExtraTags(it))
			}

			// java holds these in a TreeMap, so they reach the file in key order
			for (key in gpxFileExtensions.keys.sorted()) {
				gpxFile.getExtensionsToWrite()[key] = gpxFileExtensions[key]!!
			}
		}
		reconstructPointsGroups(gpxFile, pgNames, pgIcons, pgColors, pgBackgrounds) // groups before points
		if (pointList.isNotEmpty()) {
			sortPointList(pointList)
			for (wayPoint in pointList) {
				gpxFile.addPoint(article.createWptPt(wayPoint, article.getLang()))
			}
		}
		article.gpxFile = gpxFile
		return gpxFile
	}

	/**
	 * Sort mixed "String Number" names to prettify the waypoint list:
	 * [Station 5, Station 1, "", Station 15, Station 9, Station 17] =>
	 * [Station 1, Station 5, Station 9, Station 15, Station 17, ""]
	 */
	private fun sortPointList(pointList: MutableList<Amenity>) {
		pointList.sortWith { p1, p2 ->
			val a = p1.getName()
			val b = p2.getName()
			if (a.isBlank()) {
				if (b.isBlank()) 0 else 1
			} else if (b.isBlank()) {
				-1
			} else {
				compareWaypointNames(a, b)
			}
		}
	}

	/**
	 * Runs of digits count as numbers, everything else goes through the collator - what the ICU
	 * collator in numeric mode does for android. Ties are broken by the plain string order so that
	 * the same list always comes out the same way.
	 */
	internal fun compareWaypointNames(a: String, b: String): Int {
		var i = 0
		var j = 0
		while (i < a.length && j < b.length) {
			val aDigit = a[i].isDigit()
			if (aDigit != b[j].isDigit()) {
				break
			}
			if (aDigit) {
				var ie = i
				while (ie < a.length && a[ie].isDigit()) ie++
				var je = j
				while (je < b.length && b[je].isDigit()) je++
				val an = a.substring(i, ie).trimStart('0')
				val bn = b.substring(j, je).trimStart('0')
				if (an.length != bn.length) {
					return if (an.length < bn.length) -1 else 1
				}
				val cmp = an.compareTo(bn)
				if (cmp != 0) {
					return cmp
				}
				i = ie
				j = je
			} else {
				var ie = i
				while (ie < a.length && !a[ie].isDigit()) ie++
				var je = j
				while (je < b.length && !b[je].isDigit()) je++
				val cmp = collator.compare(a.substring(i, ie), b.substring(j, je))
				if (cmp != 0) {
					return cmp
				}
				i = ie
				j = je
			}
		}
		val rest = (a.length - i) - (b.length - j)
		return if (rest != 0) rest else a.compareTo(b)
	}

	private fun fetchSegmentsAndPoints(
		repos: List<AmenityIndexRepository>, article: TravelArticle,
		segmentList: MutableList<BinaryMapDataObject>, pointList: MutableList<Amenity>,
		gpxFileExtensions: MutableMap<String, String>, pgNames: MutableList<String>,
		pgIcons: MutableList<String>, pgColors: MutableList<String>,
		pgBackgrounds: MutableList<String>, isCancelled: Cancellable
	): Boolean {
		return if (article is TravelGpx) {
			// GPX files in OBF (track collections, OSM routes, etc)
			fetchTravelGpx(
				repos, article, segmentList, pointList, gpxFileExtensions,
				pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled
			)
		} else {
			// Wikivoyage
			fetchTravelArticle(
				repos, article, pointList, gpxFileExtensions,
				pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled
			)
		}
	}

	private fun fetchTravelGpx(
		repos: List<AmenityIndexRepository>, travelGpx: TravelGpx,
		segmentList: MutableList<BinaryMapDataObject>, pointList: MutableList<Amenity>,
		gpxFileExtensions: MutableMap<String, String>, pgNames: MutableList<String>,
		pgIcons: MutableList<String>, pgColors: MutableList<String>,
		pgBackgrounds: MutableList<String>, isCancelled: Cancellable
	): Boolean {
		var left = 0
		var right = Int.MAX_VALUE
		var top = 0
		var bottom = Int.MAX_VALUE
		val bbox31 = travelGpx.getBbox31()
		if (travelGpx.hasBbox31() && bbox31 != null) {
			left = bbox31.left.toInt()
			right = bbox31.right.toInt()
			top = bbox31.top.toInt()
			bottom = bbox31.bottom.toInt()
		}

		var mapRequestFilter: SearchFilter? = null
		val routeType = travelGpx.getRouteType()
		if (routeType != null) {
			mapRequestFilter = object : SearchFilter {
				override fun accept(types: KTIntArrayList, index: MapIndex): Boolean {
					val osmRouteType = index.getRule(ROUTE_TYPE, routeType)
					if (osmRouteType != null && types.contains(osmRouteType)) {
						return true
					}
					val userGpxType = index.getRule("route", "segment")
					return userGpxType != null && types.contains(userGpxType)
				}
			}
		}

		val poiTypeFilter: SearchPoiTypeFilter
		val subType = travelGpx.getAmenitySubType()
		if (!KAlgorithms.isEmpty(subType)) {
			poiTypeFilter = object : SearchPoiTypeFilter {
				override fun accept(type: PoiCategory?, subcategory: String): Boolean {
					for (t in subType!!.split(";")) {
						if (t == subcategory || ROUTE_TRACK == subcategory || ROUTE_TRACK_POINT == subcategory) {
							return true
						}
					}
					return false
				}

				override fun isEmpty(): Boolean = false
			}
		} else {
			// Non-POI-based GPX (to keep compatibility with legacy data)
			poiTypeFilter = getSearchFilter(travelGpx.getMainFilterString(), travelGpx.getPointFilterString())
		}

		val geometryMap = HashMap<Long, BinaryMapDataObject>() // live-updates
		val amenityMap = HashMap<Long, Amenity>() // live-updates
		val currentAmenities = ArrayList<Amenity>()

		val pointRequest = SearchRequest.buildSearchPoiRequest(
			0, 0, KAlgorithms.emptyIfNull(travelGpx.routeId), left, right, top, bottom, poiTypeFilter,
			getAmenityMatcher(travelGpx, amenityMap, currentAmenities, isCancelled), null
		)

		val mapRequest = SearchRequest.buildSearchRequest(
			left, right, top, bottom, 15, mapRequestFilter,
			matchSegmentsByRefTitleRouteId(travelGpx, geometryMap, isCancelled)
		)

		if (travelGpx.routeRadius > 0 && !travelGpx.hasBbox31()) {
			mapRequest.setBBoxRadius(travelGpx.lat, travelGpx.lon, travelGpx.routeRadius)
			pointRequest.setBBoxRadius(travelGpx.lat, travelGpx.lon, travelGpx.routeRadius)
		}

		// the repositories come in Z-A order; live updates have to be applied A-Z
		for (repo in repos.asReversed()) {
			if (isCancelled.isCancelled()) {
				return false
			}
			if (shouldSkipRepository(repo, travelGpx)) {
				continue // speed up reading (skip inappropriate obf files)
			}
			currentAmenities.clear()

			if (!repo.isPoiSectionIntersects(pointRequest)) {
				continue
			}

			if (!KAlgorithms.isEmpty(travelGpx.routeId)) {
				repo.searchPoiByName(pointRequest) // indexed route_id
			}
			if (currentAmenities.isEmpty()) {
				repo.searchPoi(pointRequest) // try non-indexed route_id
			}
			if (currentAmenities.isEmpty()) {
				continue
			}

			mapRequest.clearSearchBoxes()
			mapRequest.setSearchBoxes(getGroupedPoints(currentAmenities))
			repo.searchMapIndex(mapRequest)
		}

		pointList.addAll(getPointList(travelGpx, amenityMap, gpxFileExtensions, pgNames, pgIcons, pgColors, pgBackgrounds))
		segmentList.addAll(geometryMap.values)
		return !isCancelled.isCancelled()
	}

	/** The boxes of the map section worth opening: one per segment of the route, plus stray points. */
	private fun getGroupedPoints(amenities: List<Amenity>): List<KQuadRect>? {
		if (KAlgorithms.isEmpty(amenities)) {
			return null
		}
		val groups = LinkedHashMap<String, KQuadRect>()
		val result = ArrayList<KQuadRect>()
		for (am in amenities) {
			if (!am.isRouteTrack()) {
				continue
			}
			val location = am.getLocation() ?: continue
			val x31 = KMapUtils.get31TileNumberX(location.longitude).toDouble()
			val y31 = KMapUtils.get31TileNumberY(location.latitude).toDouble()
			val group = am.getAdditionalInfo(ROUTE_SEGMENT_INDEX)
			if (group == null) {
				result.add(KQuadRect(x31, y31, x31, y31))
			} else {
				val qr = groups.getOrPut(group) { KQuadRect(x31, y31, x31, y31) }
				qr.expand(x31, y31, x31, y31)
			}
		}
		result.addAll(groups.values)
		return result
	}

	private fun shouldSkipRepository(repo: AmenityIndexRepository, article: TravelArticle): Boolean {
		if (repo.isWorldMap()) {
			return true // World (basemap) files have huge bbox but never contain GPX data
		}
		if (article.hasOsmRouteId()) {
			return false // OSM routes are always supposed to read from multiple files
		}
		if (article.file != null && repo.getFile() != article.file) {
			return true // skip inappropriate File
		}
		if (article is TravelGpx) {
			val poiIndexes = repo.getReaderPoiIndexes()
			if (poiIndexes.isNotEmpty() && poiIndexes[0].name != article.getAmenityRegionName()) {
				return true // skip inappropriate RegionName
			}
		}
		return false
	}

	private fun fetchTravelArticle(
		repos: List<AmenityIndexRepository>, article: TravelArticle,
		pointList: MutableList<Amenity>, gpxFileExtensions: MutableMap<String, String>,
		pgNames: MutableList<String>, pgIcons: MutableList<String>,
		pgColors: MutableList<String>, pgBackgrounds: MutableList<String>,
		isCancelled: Cancellable
	): Boolean {
		val left = 0
		val right = Int.MAX_VALUE
		val top = 0
		val bottom = Int.MAX_VALUE
		val amenityMap = HashMap<Long, Amenity>()
		for (repo in repos) {
			try {
				if (isCancelled.isCancelled()) {
					return false
				}
				if (shouldSkipRepository(repo, article)) {
					continue
				}
				val pointRequest = SearchRequest.buildSearchPoiRequest(
					0, 0, KAlgorithms.emptyIfNull(article.title), left, right, top, bottom,
					getSearchFilter(article.getMainFilterString(), article.getPointFilterString()),
					getAmenityMatcher(article, amenityMap, ArrayList(), isCancelled), null
				)
				if (article.routeRadius > 0 && !article.hasBbox31()) {
					pointRequest.setBBoxRadius(article.lat, article.lon, article.routeRadius)
				}
				if (!KAlgorithms.isEmpty(article.title)) {
					repo.searchPoiByName(pointRequest)
				} else {
					repo.searchPoi(pointRequest)
				}
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}
		pointList.addAll(getPointList(article, amenityMap, gpxFileExtensions, pgNames, pgIcons, pgColors, pgBackgrounds))
		return !isCancelled.isCancelled()
	}

	/**
	 * Splits what the poi search found: the amenity that stands for the route itself gives the
	 * gpx its tags and its point groups, the rest become waypoints.
	 */
	private fun getPointList(
		article: TravelArticle, amenityMap: Map<Long, Amenity>,
		gpxFileExtensions: MutableMap<String, String>, pgNames: MutableList<String>,
		pgIcons: MutableList<String>, pgColors: MutableList<String>,
		pgBackgrounds: MutableList<String>
	): List<Amenity> {
		var isAlreadyProcessed = false
		val result = ArrayList<Amenity>()
		for (amenity in amenityMap.values) {
			if (amenity.isRouteTrack()) {
				if (!isAlreadyProcessed) {
					isAlreadyProcessed = true
					reconstructActivityFromAmenity(amenity, gpxFileExtensions)
					for ((lang, value) in amenity.getNamesMap(true)) {
						if ("ref" != lang && "sym" != lang) {
							gpxFileExtensions["name:$lang"] = value
						}
					}
					for (tag in amenity.getAdditionalInfoKeys()) {
						val value = amenity.getAdditionalInfo(tag) ?: continue
						if (tag.startsWith(OBF_POINTS_GROUPS_PREFIX)) {
							val values = value.split(OBF_POINTS_GROUPS_DELIMITER)
							when (tag) {
								OBF_POINTS_GROUPS_NAMES -> pgNames.addAll(values)
								OBF_POINTS_GROUPS_ICONS -> pgIcons.addAll(values)
								OBF_POINTS_GROUPS_COLORS -> pgColors.addAll(values)
								OBF_POINTS_GROUPS_BACKGROUNDS -> pgBackgrounds.addAll(values)
							}
						} else if (!doNotSaveAmenityGpxTags.contains(tag)) {
							gpxFileExtensions[tag] = value
						}
					}
				}
			} else if (ROUTE_TRACK_POINT == amenity.getSubType()) {
				result.add(amenity)
			} else {
				val amenityLang = amenity.getTagSuffix(Amenity.LANG_YES + ":")
				if (KAlgorithms.stringsEqual(article.lang, amenityLang)) {
					result.add(amenity)
				}
			}
		}
		return result
	}

	private fun getAmenityMatcher(
		article: TravelArticle, commonMap: MutableMap<Long, Amenity>,
		currentList: MutableList<Amenity>, isCancelled: Cancellable
	): ResultMatcher<Amenity> {
		return object : ResultMatcher<Amenity> {
			override fun publish(obj: Amenity): Boolean {
				if (obj.isClosed()) {
					obj.getId()?.let { commonMap.remove(it) } // live-updates
				}
				val routeId = article.getRouteId()
				if (routeId != null && routeId == obj.getRouteId()) {
					obj.getId()?.let { commonMap[it] = obj }
					currentList.add(obj)
				}
				return false
			}

			override fun isCancelled(): Boolean = isCancelled.isCancelled()
		}
	}

	/** Reads route_type and the activity back out of the subtype the route was written under. */
	private fun reconstructActivityFromAmenity(amenity: Amenity, gpxFileExtensions: MutableMap<String, String>) {
		val subTypes = amenity.getSubType()
		if (amenity.isRouteTrack() && subTypes != null) {
			for (subType in subTypes.split(";")) {
				if (subType.startsWith(ROUTES_PREFIX)) {
					val osmValue = amenity.getType()?.getPoiTypeByKeyName(subType)?.getOsmValue()
					if (!KAlgorithms.isEmpty(osmValue)) {
						if (amenity.hasOsmRouteId() || "other" != osmValue) {
							gpxFileExtensions[ROUTE_TYPE] = osmValue!! // do not litter gpx with default route_type
						}
						for (key in amenity.getAdditionalInfoKeys()) {
							if (key.startsWith(ROUTE_ACTIVITY_TYPE + "_")) {
								val activityType = amenity.getAdditionalInfo(key)
								if (!activityType.isNullOrEmpty()
									&& RouteActivityHelper.findRouteActivity(activityType) != null) {
									// osmand:activity in gpx
									gpxFileExtensions[GpxUtilities.ACTIVITY_TYPE] = activityType
									break
								}
							}
						}
					}
				}
			}
		}
	}

	private fun matchSegmentsByRefTitleRouteId(
		article: TravelArticle, binaryMapDataObjectMap: MutableMap<Long, BinaryMapDataObject>,
		isCancelled: Cancellable
	): ResultMatcher<BinaryMapDataObject> {
		return object : ResultMatcher<BinaryMapDataObject> {
			override fun publish(obj: BinaryMapDataObject): Boolean {
				if (isDeletedBinaryMapDataObject(obj)) {
					binaryMapDataObjectMap.remove(obj.getId()) // live-updates
				}
				if (obj.getPointsLength() > 1) {
					val routeId = article.getRouteId()
					val equalRouteId = !KAlgorithms.isEmpty(routeId) && routeId == obj.getTagValue(Amenity.ROUTE_ID)

					if (article is TravelGpx && equalRouteId) {
						// GPX-in-OBF requires mandatory route_id
						binaryMapDataObjectMap[obj.getId()] = obj
					} else {
						val name = article.getTitle()
						val ref = article.ref
						val equalName = !KAlgorithms.isEmpty(name) && name == obj.getName()
						val equalRef = !KAlgorithms.isEmpty(ref) && ref == obj.getTagValue(Amenity.REF)
						if (equalRouteId && (equalRef || equalName) || (equalRef && equalName)) {
							// Wikivoyage is allowed to match mixed tags
							binaryMapDataObjectMap[obj.getId()] = obj
						}
					}
				}
				return false
			}

			override fun isCancelled(): Boolean = isCancelled.isCancelled()
		}
	}

	private fun reconstructPointsGroups(
		gpxFile: GpxFile, pgNames: List<String>, pgIcons: List<String>,
		pgColors: List<String>, pgBackgrounds: List<String>
	) {
		if (pgNames.size == pgIcons.size && pgIcons.size == pgColors.size && pgColors.size == pgBackgrounds.size) {
			for (i in pgNames.indices) {
				var name = pgNames[i]
				val icon = pgIcons[i]
				val background = pgBackgrounds[i]
				val color = KAlgorithms.parseColor(pgColors[i])
				if (name.isEmpty() || OBF_POINTS_GROUPS_EMPTY_NAME_STUB == name) {
					name = GpxFile.DEFAULT_WPT_GROUP_NAME // follow current default
				}
				gpxFile.addPointsGroup(GpxUtilities.PointsGroup(name, icon, background, color))
			}
		}
	}

	private fun parseExtraTags(value: String): Map<String, String> {
		val tags = LinkedHashMap<String, String>()
		try {
			val parsed = json.parseToJsonElement(value)
			if (parsed is JsonObject) {
				for ((key, element) in parsed) {
					if (element is JsonPrimitive) {
						tags[key] = element.content
					}
				}
			}
		} catch (e: Exception) {
			log.error("Failed to parse extra tags: $value", e)
		}
		return tags
	}

	companion object {
		private val log = LoggerFactory.getLogger("TravelObfGpxBuilder")

		private val json = Json { isLenient = true; ignoreUnknownKeys = true }

		const val METADATA_EXTRA_TAGS: String = "metadata_extra_tags"
		const val EXTENSIONS_EXTRA_TAGS: String = "extensions_extra_tags"

		/** Do not clutter GPX with tags that are always generated. */
		private val doNotSaveAmenityGpxTags = setOf(
			"date", "distance", "route_name", "route_bbox_radius", "start_ele", "ele_graph",
			"avg_speed", "min_speed", "max_speed", "time_moving", "time_moving_no_gaps",
			"time_span", "time_span_no_gaps"
		)

		/** `FileUtils.ILLEGAL_FILE_NAME_CHARACTERS` in the android app. */
		private val illegalFileNameCharacters = setOf('?', ':', '"', '*', '|', '/', '<', '>', '\\')

		private fun isValidFileName(name: String?): Boolean =
			name != null && name.none { illegalFileNameCharacters.contains(it) }

		/**
		 * Accepts amenities of the given subcategories. `route_track` also takes every
		 * `routes_xxx` - a track written under an activity of its own.
		 */
		fun getSearchFilter(vararg filterSubcategories: String): SearchPoiTypeFilter {
			return object : SearchPoiTypeFilter {
				override fun accept(type: PoiCategory?, subcategory: String): Boolean {
					for (filter in filterSubcategories) {
						if (subcategory == filter) {
							return true
						}
						if (ROUTE_TRACK == filter
							&& (subcategory.startsWith(ROUTES_PREFIX) || subcategory.contains(";$ROUTES_PREFIX"))) {
							return true // include routes:routes_xxx with routes:route_track filter
						}
					}
					return false
				}

				override fun isEmpty(): Boolean = false
			}
		}

		/** True when live updates replaced this object with a deletion marker. */
		fun isDeletedBinaryMapDataObject(obj: BinaryMapDataObject): Boolean {
			val types = obj.getTypes()
			if (types == null || types.isEmpty()) {
				return false
			}
			val delete = obj.getMapIndex()?.getRule(Amenity.OSM_DELETE_TAG, Amenity.OSM_DELETE_VALUE)
				?: return false
			return types[0] == delete
		}
	}
}
