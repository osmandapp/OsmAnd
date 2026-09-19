package net.osmand.shared.travel

import net.osmand.shared.IndexConstants
import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.MapIndex
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.RouteActivityHelper
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTES_PREFIX
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_ARTICLE
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_TRACK
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.travel.PopularArticles.Companion.ARTICLES_PER_PAGE
import net.osmand.shared.travel.TravelGpx.Companion.ROUTE_TYPE
import net.osmand.shared.travel.TravelObfGpxBuilder.Companion.getSearchFilter
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.primaryCollator
import net.osmand.shared.util.synchronized
import net.osmand.shared.api.KStringMatcherMode

/**
 * Everything the travel screens ask of the obf files: which articles to offer, what a search
 * matches, and how an article or a track is fetched back from a file by its route_id.
 *
 * A copy of `TravelObfHelper` in the android app, which stays there; this copy is for iOS. Where
 * java reaches into the app this takes a [TravelObfContext] - which files to search, the language
 * and where the map is looking - and a [TravelBookmarks] for what the user saved.
 *
 * Three things went a different way from java. Reading a gpx file was an `AsyncTask` and is a
 * plain call here, so the `GpxReadCallback` is gone and the caller picks the thread.
 * `openTrackMenu` was pure android UI and is not here. And [search] builds its matcher from
 * [KCollatorStringMatcher] rather than from a `SearchPhrase`, because the search core is not in
 * shared yet - pass your own matcher to get exactly what android does.
 */
class TravelObfHelper(
	private val context: TravelObfContext,
	val bookmarks: TravelBookmarks = TravelBookmarks.NONE
) {

	private val lock = KLock()
	private val collator: KCollator = primaryCollator()
	private val gpxBuilder = TravelObfGpxBuilder(context)

	private var popularArticles = PopularArticles()
	private val cachedArticles = HashMap<TravelArticleIdentifier, Map<String, TravelArticle>>()
	private var searchRadius = ARTICLE_SEARCH_RADIUS
	private var foundAmenitiesIndex = 0
	private val foundAmenities = ArrayList<Pair<KFile, Amenity>>()

	/** Raised by the caller for every new query, so that a search in flight drops its results. */
	var requestNumber: Int = 0

	fun initializeDataToDisplay(resetData: Boolean) {
		if (resetData) {
			foundAmenities.clear()
			foundAmenitiesIndex = 0
			popularArticles.clear()
			searchRadius = ARTICLE_SEARCH_RADIUS
		}
		bookmarks.refreshCachedData()
		loadPopularArticles()
	}

	private fun loadPopularArticles(): Unit = synchronized(lock) {
		val lang = context.getLanguage()
		var articles = loadPopularArticlesForLang(lang)
		if (articles.isEmpty()) {
			articles = loadPopularArticlesForLang("en")
		}
		this.popularArticles = articles
	}

	/**
	 * Fills one page of articles, widening the search until a page is full or the radius runs out.
	 * Articles are preferred over tracks by counting their distance as a fifth of what it is.
	 */
	private fun loadPopularArticlesForLang(lang: String): PopularArticles {
		val popularArticles = PopularArticles(this.popularArticles)
		val location = context.getMapLocation()
		if (isAnyTravelBookPresent() && location != null) {
			var articlesLimitReached = false
			do {
				if (foundAmenities.size - foundAmenitiesIndex < ARTICLES_PER_PAGE) {
					for (repo in context.getWikivoyageRepositories()) {
						searchAmenity(foundAmenities, location, repo, searchRadius, -1, ROUTE_ARTICLE, lang)
						searchAmenity(foundAmenities, location, repo, searchRadius / 5, 15, ROUTE_TRACK, null)
					}
					if (foundAmenities.size > 0) {
						foundAmenities.sortBy { distanceToOffer(it.second, location) }
					}
					searchRadius *= 2
				}
				while (foundAmenitiesIndex < foundAmenities.size - 1) {
					val (file, amenity) = foundAmenities[foundAmenitiesIndex]
					if (!KAlgorithms.isEmpty(amenity.getName(lang))) {
						val routeId = amenity.getAdditionalInfo(Amenity.ROUTE_ID)
						if (routeId == null || !popularArticles.containsByRouteId(routeId)) {
							val article = cacheTravelArticles(file, amenity, lang, false)
							if (article != null && !popularArticles.contains(article)) {
								if (!popularArticles.add(article)) {
									articlesLimitReached = true
									break
								}
							}
						}
					}
					foundAmenitiesIndex++
				}
			} while (!articlesLimitReached && searchRadius < MAX_SEARCH_RADIUS)
		}
		return popularArticles
	}

	private fun distanceToOffer(amenity: Amenity, location: KLatLon): Double {
		val amenityLocation = amenity.getLocation() ?: return Double.MAX_VALUE
		val distance = KMapUtils.getDistance(amenityLocation, location)
		return distance / (if (ROUTE_ARTICLE == amenity.getSubType()) 5 else 1)
	}

	fun isTravelGpxTags(tags: Map<String, String>): Boolean {
		return tags.containsKey(Amenity.ROUTE_ID)
				&& ("segment" == tags[Amenity.ROUTE] || tags.containsKey(ROUTE_TYPE))
	}

	/**
	 * The tracks drawn under [location], of the given osm route types. Android calls this
	 * `searchTravelGpx(LatLon, NetworkRouteSelectorFilter)` and passes `OsmRouteType`s, which are
	 * not in shared; [osmRouteTypeTags] are their names, e.g. "hiking", "bicycle".
	 */
	fun searchTravelGpxByRouteTypes(
		location: KLatLon, osmRouteTypeTags: Set<String>
	): List<TravelGpx> = synchronized(lock) {
		val routeIds = HashSet<String>()

		val mapRequest = SearchRequest.buildSearchRequest(
			0, 0, 0, 0, ROUTES_ON_LINE_ZOOM,
			getRoutesSearchFilter(osmRouteTypeTags), getRoutesResultMatcher(routeIds, location)
		)
		mapRequest.setBBoxRadius(location.latitude, location.longitude, ROUTES_ON_LINE_RADIUS)

		if (!KAlgorithms.isEmpty(osmRouteTypeTags)) {
			for (repo in context.getTravelGpxRepositories()) {
				repo.searchMapIndex(mapRequest)
			}
			if (routeIds.isNotEmpty()) {
				return@synchronized searchTravelGpx(location, routeIds)
			}
		}

		emptyList()
	}

	private fun getRoutesSearchFilter(osmRouteTypeTags: Set<String>): SearchFilter {
		val enabledRouteTypes = HashSet<String>()
		for (tag in osmRouteTypeTags) {
			val activity = RouteActivityHelper.findActivityByTag(tag)
			if (activity != null) {
				enabledRouteTypes.add(activity.group.id)
			}
		}

		return object : SearchFilter {
			override fun accept(types: KTIntArrayList, index: MapIndex): Boolean {
				for (type in enabledRouteTypes) {
					val routeTypeRuleIndex = index.getRule(ROUTE_TYPE, type)
					if (routeTypeRuleIndex != null && types.contains(routeTypeRuleIndex)) {
						return true
					}
				}
				return false
			}
		}
	}

	private fun getRoutesResultMatcher(
		routeIds: MutableSet<String>, pointLatLon: KLatLon
	): ResultMatcher<BinaryMapDataObject> {
		return object : ResultMatcher<BinaryMapDataObject> {
			override fun publish(obj: BinaryMapDataObject): Boolean {
				if (obj.getPointsLength() > 1 && !TravelObfGpxBuilder.isDeletedBinaryMapDataObject(obj)) {
					val routeIdPrefixed = obj.getTagValue(Amenity.ROUTE_ID)
					if (ObfConstants.getOsmIdFromPrefixedRouteId(routeIdPrefixed) > 0
						&& matchByDistance(obj, pointLatLon)) {
						routeIds.add(routeIdPrefixed)
					}
				}
				return false
			}

			private fun matchByDistance(obj: BinaryMapDataObject, location: KLatLon): Boolean {
				for (i in 0 until obj.getPointsLength() - 1) {
					if (KMapUtils.getOrthogonalDistance(
							location.latitude, location.longitude,
							KMapUtils.get31LatitudeY(obj.getPoint31YTile(i)),
							KMapUtils.get31LongitudeX(obj.getPoint31XTile(i)),
							KMapUtils.get31LatitudeY(obj.getPoint31YTile(i + 1)),
							KMapUtils.get31LongitudeX(obj.getPoint31XTile(i + 1))
						) <= ROUTES_ON_LINE_RADIUS) {
						return true
					}
				}
				return false
			}

			override fun isCancelled(): Boolean = false
		}
	}

	/** The tracks with these route_ids, searched outward from [location] until all are found. */
	fun searchTravelGpx(location: KLatLon, routeIds: Set<String>): List<TravelGpx> = synchronized(lock) {
		val foundAmenities = ArrayList<Pair<KFile, Amenity>>()
		val singleRouteIdRequested = routeIds.size == 1
		var userGpxCollectionSearchRequested = false
		for (routeIdPrefixed in routeIds) {
			if (ObfConstants.getOsmIdFromPrefixedRouteId(routeIdPrefixed) == 0L) {
				userGpxCollectionSearchRequested = true
				break
			}
		}
		val routes = LinkedHashMap<String, TravelGpx>()
		var searchRadius = TRAVEL_GPX_SEARCH_RADIUS
		do {
			for (repo in context.getTravelGpxRepositories()) {
				if (repo.isWorldMap()) {
					continue
				}
				if (!isLocationIntersectsWithRepo(repo, location) && !userGpxCollectionSearchRequested) {
					// User GPX collections may have insufficient POI bbox and should never be skipped
					continue
				}
				val firstSearchCycle = searchRadius == TRAVEL_GPX_SEARCH_RADIUS
				if (firstSearchCycle && singleRouteIdRequested) {
					val singleRouteId = routeIds.iterator().next()
					// indexed
					searchTravelGpxAmenityByRouteId(foundAmenities, repo, singleRouteId, location, searchRadius)
					if (foundAmenities.size >= routeIds.size) {
						break // optimization
					}
				} else {
					// fallback to slow non-indexed route_id (compatibility with old files)
					searchAmenity(foundAmenities, location, repo, searchRadius, 15, ROUTE_TRACK, null)
				}
			}
			for ((file, amenity) in foundAmenities) {
				val lcRouteId = amenity.getRouteId()?.lowercase()
				for (routeId in routeIds) {
					if (routeId.lowercase() == lcRouteId) {
						routes[lcRouteId!!] = getTravelGpx(file, amenity)
					}
				}
				if (routes.size == routeIds.size) {
					break // optimization
				}
			}
			searchRadius *= 2
		} while (routes.size < routeIds.size && searchRadius < MAX_TRAVEL_GPX_SEARCH_RADIUS)
		if (routes.isEmpty()) {
			log.error("searchTravelGpx($location, $routeIds) failed")
		}
		ArrayList(routes.values)
	}

	fun searchTravelGpx(location: KLatLon, routeId: String?): TravelGpx? {
		if (KAlgorithms.isEmpty(routeId)) {
			log.error("searchTravelGpx($location, null) failed due to empty routeId")
			return null
		}
		val routes = searchTravelGpx(location, setOf(routeId!!))
		return routes.firstOrNull()
	}

	private fun searchTravelGpxAmenityByRouteId(
		amenitiesList: MutableList<Pair<KFile, Amenity>>, repo: AmenityIndexRepository,
		routeId: String, location: KLatLon, searchRadius: Int
	) {
		val poiTypeFilter = object : SearchPoiTypeFilter {
			override fun accept(type: PoiCategory?, subcategory: String): Boolean =
				subcategory.startsWith(ROUTES_PREFIX) || subcategory.contains(";$ROUTES_PREFIX")
						|| ROUTE_TRACK == subcategory

			override fun isEmpty(): Boolean = false
		}
		val pointRequest = SearchRequest.buildSearchPoiRequest(
			0, 0, routeId, 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, poiTypeFilter,
			object : ResultMatcher<Amenity> {
				override fun publish(obj: Amenity): Boolean {
					if (routeId == obj.getRouteId()) {
						amenitiesList.add(Pair(repo.getFile(), obj))
					}
					return false
				}

				override fun isCancelled(): Boolean = false
			}, null
		)
		pointRequest.setBBoxRadius(location.latitude, location.longitude, searchRadius)
		repo.searchPoiByName(pointRequest)
	}

	private fun isLocationIntersectsWithRepo(repo: AmenityIndexRepository, location: KLatLon): Boolean {
		val x31 = KMapUtils.get31TileNumberX(location.longitude).toDouble()
		val y31 = KMapUtils.get31TileNumberY(location.latitude).toDouble()
		for (poiIndex in repo.getReaderPoiIndexes()) {
			val bbox = KQuadRect(
				poiIndex.left31.toDouble(), poiIndex.top31.toDouble(),
				poiIndex.right31.toDouble(), poiIndex.bottom31.toDouble()
			)
			if (bbox.contains(x31, y31, x31, y31)) {
				return true
			}
		}
		return false
	}

	private fun searchAmenity(
		amenitiesList: MutableList<Pair<KFile, Amenity>>, location: KLatLon,
		repo: AmenityIndexRepository, searchRadius: Int, zoom: Int, searchFilter: String, lang: String?
	) {
		repo.searchPoi(
			SearchRequest.buildSearchPoiRequest(
				location, searchRadius, zoom, getSearchFilter(searchFilter),
				object : ResultMatcher<Amenity> {
					override fun publish(obj: Amenity): Boolean {
						if (lang == null || obj.getNamesMap(true).containsKey(lang)) {
							amenitiesList.add(Pair(repo.getFile(), obj))
						}
						return false
					}

					override fun isCancelled(): Boolean = false
				})
		)
	}

	private fun cacheTravelArticles(
		file: KFile, amenity: Amenity, lang: String?, readPoints: Boolean
	): TravelArticle? {
		var article: TravelArticle? = null
		val articles = if (amenity.isRouteTrack()) {
			readRoutePoint(file, amenity)
		} else {
			readArticles(file, amenity)
		}
		if (!KAlgorithms.isEmpty(articles)) {
			val newArticleId = articles.values.iterator().next().generateIdentifier()
			cachedArticles[newArticleId] = articles
			article = getCachedArticle(newArticleId, lang, readPoints)
		}
		return article
	}

	private fun readRoutePoint(file: KFile, amenity: Amenity): Map<String, TravelArticle> =
		mapOf("" to getTravelGpx(file, amenity))

	private fun getTravelGpx(file: KFile, amenity: Amenity): TravelGpx {
		val travelGpx = TravelGpx(amenity)
		travelGpx.file = file
		return travelGpx
	}

	private fun readArticles(file: KFile, amenity: Amenity): Map<String, TravelArticle> {
		val articles = LinkedHashMap<String, TravelArticle>()
		for (lang in getLanguages(amenity)) {
			articles[lang] = readArticle(file, amenity, lang)
		}
		return articles
	}

	private fun readArticle(file: KFile, amenity: Amenity, lang: String?): TravelArticle {
		val res = TravelArticle()
		res.file = file
		val title = amenity.getName(lang)
		res.title = if (KAlgorithms.isEmpty(title)) amenity.getName() else title
		res.content = amenity.getDescription(lang)
		res.isPartOf = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.IS_PART, lang))
		res.isParentOf = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.IS_PARENT_OF, lang))
		res.lat = amenity.getLocation()?.latitude ?: Double.NaN
		res.lon = amenity.getLocation()?.longitude ?: Double.NaN
		res.imageTitle = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.IMAGE_TITLE))
		res.routeId = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID))
		res.routeSource = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE))
		res.originalId = 0
		res.lang = lang
		res.contentsJson = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.CONTENT_JSON, lang))
		res.aggregatedPartOf = KAlgorithms.emptyIfNull(amenity.getStrictTagContent(Amenity.IS_AGGR_PART, lang))
		return res
	}

	fun isAnyTravelBookPresent(): Boolean = context.getWikivoyageRepositories().isNotEmpty()

	/**
	 * Articles whose name matches [searchQuery], in the app language and then in english.
	 * [matcher] decides what matching means; the default treats the query as an unfinished word.
	 */
	fun search(
		searchQuery: String, reqNumber: Int,
		matcher: KStringMatcher = defaultNameMatcher(searchQuery)
	): List<WikivoyageSearchResult> = synchronized(lock) {
		val appLang = context.getLanguage()
		var res = searchWithLang(searchQuery, appLang, reqNumber, matcher)
		if (KAlgorithms.isEmpty(res)) {
			res = searchWithLang(searchQuery, "en", reqNumber, matcher)
		}
		res
	}

	private fun defaultNameMatcher(searchQuery: String): KStringMatcher =
		KCollatorStringMatcher(searchQuery.trim(), KStringMatcherMode.CHECK_STARTS_FROM_SPACE)

	private fun searchWithLang(
		searchQuery: String, appLang: String, reqNumber: Int, matcher: KStringMatcher
	): List<WikivoyageSearchResult> {
		val res = ArrayList<WikivoyageSearchResult>()
		val amenityMap = LinkedHashMap<KFile, List<Amenity>>()
		val empty = ArrayList<WikivoyageSearchResult>()

		for (repo in context.getWikivoyageRepositories()) {
			if (requestNumber != reqNumber) {
				return empty
			}
			val bbox = KQuadRect()
			for (poiRegion in repo.getReaderPoiIndexes()) {
				bbox.expand(
					poiRegion.left31.toDouble(), poiRegion.top31.toDouble(),
					poiRegion.right31.toDouble(), poiRegion.bottom31.toDouble()
				)
			}
			val searchRequest = SearchRequest.buildSearchPoiRequest(
				0, 0, searchQuery, bbox.left.toInt(), bbox.right.toInt(), bbox.top.toInt(), bbox.bottom.toInt(),
				getSearchFilter(ROUTE_ARTICLE), object : ResultMatcher<Amenity> {
					override fun publish(obj: Amenity): Boolean {
						val localeName = obj.getName(appLang)
						return matcher.matches(localeName) || obj.getOtherNames(false).any { matcher.matches(it) }
					}

					override fun isCancelled(): Boolean = requestNumber != reqNumber
				}, null
			)

			val amenities = repo.searchPoiByName(searchRequest)
			if (requestNumber != reqNumber) {
				return empty
			}
			if (!KAlgorithms.isEmpty(amenities)) {
				amenityMap[repo.getFile()] = amenities
			}
		}
		if (!KAlgorithms.isEmpty(amenityMap)) {
			val appLangEn = "en" == appLang
			val uniqueIds = HashSet<Long>()
			for ((file, amenities) in amenityMap) {
				for (amenity in amenities) {
					val routeId = KAlgorithms.parseLongSilently(
						KAlgorithms.emptyIfNull(amenity.getRouteId()).replace("Q", ""), -1
					)
					if (!uniqueIds.add(routeId)) {
						continue
					}
					val nameLangs = getLanguages(amenity)
					if (nameLangs.contains(appLang) || KAlgorithms.isEmpty(appLang)) {
						val article = readArticle(file, amenity, appLang)
						val langs = ArrayList(nameLangs)
						langs.sortWith { l1, l2 ->
							rankLang(l1, appLang, appLangEn).compareTo(rankLang(l2, appLang, appLangEn))
						}
						res.add(WikivoyageSearchResult(article, langs))
					}
				}
			}
			sortSearchResults(res, searchQuery)
		}
		return res
	}

	/** The app language first, english next when it is not the app language, then alphabetical. */
	private fun rankLang(lang: String, appLang: String, appLangEn: Boolean): String = when {
		lang == appLang -> "1"
		!appLangEn && lang == "en" -> "2"
		else -> lang
	}

	/** The languages an article is written in, read off its description and is_part tags. */
	private fun getLanguages(amenity: Amenity): Set<String> {
		val langs = LinkedHashSet<String>()
		val descrStart = Amenity.DESCRIPTION + ":"
		val partStart = Amenity.IS_PART + ":"
		for (infoTag in amenity.getAdditionalInfoKeys()) {
			if (infoTag.startsWith(descrStart)) {
				if (infoTag.length > descrStart.length) {
					langs.add(infoTag.substring(descrStart.length))
				}
			} else if (infoTag.startsWith(partStart)) {
				if (infoTag.length > partStart.length) {
					langs.add(infoTag.substring(partStart.length))
				}
			}
		}
		return langs
	}

	private fun sortSearchResults(results: MutableList<WikivoyageSearchResult>, searchQuery: String) {
		val searchQueryLC = searchQuery.lowercase()
		val trimmedQuery = searchQuery.trim()
		results.sortWith { sr1, sr2 ->
			compareSearchResults(sr1, sr2, trimmedQuery, searchQueryLC)
		}
	}

	/** An exact title first, then a title containing the query, then by title. */
	private fun compareSearchResults(
		sr1: WikivoyageSearchResult, sr2: WikivoyageSearchResult,
		trimmedQuery: String, searchQueryLC: String
	): Int {
		val articleTitle1 = KAlgorithms.emptyIfNull(sr1.getArticleTitle())
		val articleTitle2 = KAlgorithms.emptyIfNull(sr2.getArticleTitle())

		val sr1NotEqual = !collator.equals(articleTitle1, trimmedQuery)
		val sr2NotEqual = !collator.equals(articleTitle2, trimmedQuery)
		if (sr1NotEqual != sr2NotEqual) {
			return if (sr1NotEqual) 1 else -1
		}

		val title1contains = articleTitle1.lowercase().contains(searchQueryLC)
		val title2contains = articleTitle2.lowercase().contains(searchQueryLC)
		if (title1contains != title2contains) {
			return if (title1contains) -1 else 1
		}

		val comp = collator.compare(articleTitle1, articleTitle2)
		return if (comp != 0) {
			comp
		} else {
			collator.compare(KAlgorithms.emptyIfNull(sr1.isPartOf), KAlgorithms.emptyIfNull(sr2.isPartOf))
		}
	}

	fun getPopularArticles(): List<TravelArticle> = popularArticles.getArticles()

	/**
	 * The parents of an article and their children, the rows of the navigation panel, keyed by the
	 * parent they hang under and kept in the order the parents were read.
	 */
	fun getNavigationMap(
		article: TravelArticle
	): Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> = synchronized(lock) {
		val lang = article.getLang()
		val title = article.getTitle()
		if (KAlgorithms.isEmpty(lang) || KAlgorithms.isEmpty(title)) {
			return@synchronized emptyMap()
		}
		val aggregatedPartOf = article.getAggregatedPartOf()
		var parts: List<String>? = null
		if (!KAlgorithms.isEmpty(aggregatedPartOf)) {
			val originalParts = aggregatedPartOf!!.split(",")
			parts = if (originalParts.size > 1) originalParts.reversed() else originalParts
		}
		val navMap = LinkedHashMap<String, MutableList<WikivoyageSearchResult>>()
		val headers = LinkedHashSet<String>()
		val headerObjs = LinkedHashMap<String, WikivoyageSearchResult>()
		if (parts != null && parts.isNotEmpty()) {
			headers.addAll(parts)
			if (!KAlgorithms.isEmpty(article.isParentOf)) {
				headers.add(title!!)
			}
		}

		for (rawHeader in headers) {
			val parentLang = if (rawHeader.startsWith(EN_LANG_PREFIX)) "en" else lang
			val header = getTitleWithoutPrefix(rawHeader)
			val parentArticle = getParentArticleByTitle(header, parentLang) ?: continue
			navMap[header] = ArrayList()
			for (childTitle in parentArticle.isParentOf.split(";")) {
				if (childTitle.isNotEmpty()) {
					val searchResult = WikivoyageSearchResult("", childTitle, null, null, listOf(parentLang!!))
					navMap.getOrPut(header) { ArrayList() }.add(searchResult)
					if (headers.contains(childTitle)) {
						headerObjs[childTitle] = searchResult
					}
				}
			}
		}

		val res = LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>>()
		for (rawHeader in headers) {
			val parentLang = if (rawHeader.startsWith(EN_LANG_PREFIX)) "en" else lang
			val header = getTitleWithoutPrefix(rawHeader)
			val results = navMap[header]
			if (results != null) {
				sortSearchResults(results, header)
				val emptyResult = WikivoyageSearchResult("", header, null, null, listOf(parentLang!!))
				res[headerObjs[header] ?: emptyResult] = results
			}
		}
		res
	}

	private fun getParentArticleByTitle(title: String, lang: String?): TravelArticle? {
		var article: TravelArticle? = null
		val amenities = ArrayList<Amenity>()
		for (repo in context.getWikivoyageRepositories()) {
			val req = SearchRequest.buildSearchPoiRequest(
				0, 0, title, 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, getSearchFilter(ROUTE_ARTICLE),
				object : ResultMatcher<Amenity> {
					var done = false

					override fun publish(obj: Amenity): Boolean {
						if (KAlgorithms.stringsEqual(title, KAlgorithms.emptyIfNull(obj.getName(lang)))) {
							amenities.add(obj)
							done = true
						}
						return false
					}

					override fun isCancelled(): Boolean = done
				}, null
			)
			repo.searchPoiByName(req)
			if (!KAlgorithms.isEmpty(amenities)) {
				article = readArticle(repo.getFile(), amenities[0], lang)
				break
			}
		}
		return article
	}

	/**
	 * @param readGpx builds the gpx file of the article right here - it reads from every travel
	 * file and is not for a screen thread.
	 */
	fun getArticleById(
		articleId: TravelArticleIdentifier, lang: String?, readGpx: Boolean
	): TravelArticle? {
		var article = getCachedArticle(articleId, lang, readGpx)
		if (article == null) {
			article = bookmarks.getSavedArticle(articleId.file, articleId.routeId, lang)
		}
		return article
	}

	private fun getCachedArticle(
		articleId: TravelArticleIdentifier, lang: String?, readGpx: Boolean
	): TravelArticle? {
		var article: TravelArticle? = null
		val articles = cachedArticles[articleId]
		if (articles != null) {
			article = if (KAlgorithms.isEmpty(lang)) {
				articles.values.firstOrNull()
			} else {
				articles[lang] ?: articles[""]
			}
		}
		if (article == null && articles == null) {
			article = findArticleById(articleId, lang, readGpx)
		}
		if (article != null && readGpx && (!KAlgorithms.isEmpty(lang) || article is TravelGpx)) {
			readGpxFile(article)
		}
		return article
	}

	/** Builds the article's gpx file, unless it is built already. */
	fun readGpxFile(
		article: TravelArticle,
		isCancelled: TravelObfGpxBuilder.Cancellable = TravelObfGpxBuilder.Cancellable { false }
	): GpxFile? {
		if (!article.gpxFileRead) {
			val gpxFile = gpxBuilder.buildGpxFile(context.getTravelGpxRepositories(), article, isCancelled)
			article.gpxFileRead = gpxFile != null
			article.gpxFile = gpxFile
		}
		return article.gpxFile
	}

	private fun findArticleById(
		articleId: TravelArticleIdentifier, lang: String?, readGpx: Boolean
	): TravelArticle? = synchronized(lock) {
		var article: TravelArticle? = null
		val isDbArticle = articleId.file?.name()?.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT) == true
		val amenities = ArrayList<Amenity>()
		for (repo in context.getWikivoyageRepositories()) {
			if (articleId.file != null && articleId.file != repo.getFile() && !isDbArticle) {
				continue
			}
			val matcher = object : ResultMatcher<Amenity> {
				var done = false

				override fun publish(obj: Amenity): Boolean {
					if (KAlgorithms.stringsEqual(
							articleId.routeId, KAlgorithms.emptyIfNull(obj.getTagContent(Amenity.ROUTE_ID))
						) || isDbArticle) {
						amenities.add(obj)
						done = true
					}
					return false
				}

				override fun isCancelled(): Boolean = done
			}
			val hasLocation = !articleId.lat.isNaN()
			val searchByRouteId = !isDbArticle && KAlgorithms.isEmpty(articleId.title)
					&& !KAlgorithms.isEmpty(articleId.routeId)
			val searchName = if (searchByRouteId) articleId.routeId!! else KAlgorithms.emptyIfNull(articleId.title)
			val req = SearchRequest.buildSearchPoiRequest(
				0, 0, searchName, 0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
				getSearchFilter(ROUTE_ARTICLE), matcher, null
			)
			if (hasLocation) {
				req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS)
			}
			if (searchByRouteId) {
				// route_id is in the POI name index: a spatial scan of ARTICLE_SEARCH_RADIUS decodes
				// thousands of articles (hundreds of MB) while holding the repository lock
				repo.searchPoiByName(req)
				if (amenities.isEmpty() && hasLocation) {
					// travel files without the route_id name index
					val nearbyReq = SearchRequest.buildSearchPoiRequest(
						0, 0, "", 0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
						getSearchFilter(ROUTE_ARTICLE), matcher, null
					)
					nearbyReq.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS)
					repo.searchPoi(nearbyReq)
				}
			} else if (hasLocation && !KAlgorithms.isEmpty(articleId.title)) {
				repo.searchPoiByName(req)
			} else {
				repo.searchPoi(req)
			}
			if (!KAlgorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(repo.getFile(), amenities[0], lang, readGpx)
			}
		}
		article
	}

	/** Finds a saved article again in whatever travel file is open now. */
	fun findSavedArticle(savedArticle: TravelArticle): TravelArticle? = synchronized(lock) {
		val amenities = ArrayList<Pair<KFile, Amenity>>()
		var article: TravelArticle? = null
		val articleId = savedArticle.generateIdentifier()
		val lang = savedArticle.getLang()
		val lastModified = savedArticle.getLastModified()
		var req: SearchRequest<Amenity>? = null
		for (repo in context.getWikivoyageRepositories()) {
			if (articleId.file != null && articleId.file == repo.getFile()) {
				if (lastModified == repo.getFile().lastModified()) {
					req = SearchRequest.buildSearchPoiRequest(
						0, 0, KAlgorithms.emptyIfNull(articleId.title), 0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
						getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK),
						getEqualsRouteIdMatcher(articleId, amenities, repo.getFile()), null
					)
					req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS)
				} else if (!KAlgorithms.isEmpty(articleId.title)) {
					req = getEqualsTitleRequest(articleId, lang, amenities, repo.getFile())
					req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS / 10)
				}
			}
			if (req != null) {
				if (!articleId.lat.isNaN() && !KAlgorithms.isEmpty(articleId.title)) {
					repo.searchPoiByName(req)
				} else {
					repo.searchPoi(req)
				}
				break
			}
		}
		if (amenities.isEmpty() && !KAlgorithms.isEmpty(articleId.title)) {
			for (repo in context.getWikivoyageRepositories()) {
				req = getEqualsTitleRequest(articleId, lang, amenities, repo.getFile())
				req.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS)
				if (!articleId.lat.isNaN()) {
					repo.searchPoiByName(req)
				} else {
					repo.searchPoi(req)
				}
			}
		}
		if (amenities.isEmpty()) {
			for (repo in context.getWikivoyageRepositories()) {
				req = SearchRequest.buildSearchPoiRequest(
					0, 0, KAlgorithms.emptyIfNull(articleId.title), 0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
					getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK),
					getEqualsRouteIdAndSourceMatcher(articleId, amenities, repo.getFile()), null
				)
				req.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS)
				if (!articleId.lat.isNaN() && !KAlgorithms.isEmpty(articleId.title)) {
					repo.searchPoiByName(req)
				} else {
					repo.searchPoi(req)
				}
			}
		}
		if (!KAlgorithms.isEmpty(amenities)) {
			article = cacheTravelArticles(amenities[0].first, amenities[0].second, lang, false)
		}
		article
	}

	private fun getEqualsRouteIdMatcher(
		articleId: TravelArticleIdentifier, amenities: MutableList<Pair<KFile, Amenity>>, readerFile: KFile
	): ResultMatcher<Amenity> = object : ResultMatcher<Amenity> {
		var done = false

		override fun publish(obj: Amenity): Boolean {
			if (KAlgorithms.stringsEqual(
					articleId.routeId, KAlgorithms.emptyIfNull(obj.getTagContent(Amenity.ROUTE_ID))
				)) {
				amenities.add(Pair(readerFile, obj))
				done = true
			}
			return false
		}

		override fun isCancelled(): Boolean = done
	}

	private fun getEqualsRouteIdAndSourceMatcher(
		articleId: TravelArticleIdentifier, amenities: MutableList<Pair<KFile, Amenity>>, readerFile: KFile
	): ResultMatcher<Amenity> = object : ResultMatcher<Amenity> {
		var done = false

		override fun publish(obj: Amenity): Boolean {
			if (KAlgorithms.stringsEqual(
					articleId.routeId, KAlgorithms.emptyIfNull(obj.getTagContent(Amenity.ROUTE_ID))
				) && KAlgorithms.stringsEqual(
					articleId.routeSource, KAlgorithms.emptyIfNull(obj.getTagContent(Amenity.ROUTE_SOURCE))
				)) {
				amenities.add(Pair(readerFile, obj))
				done = true
			}
			return false
		}

		override fun isCancelled(): Boolean = done
	}

	private fun getEqualsTitleRequest(
		articleId: TravelArticleIdentifier, lang: String?,
		amenities: MutableList<Pair<KFile, Amenity>>, readerFile: KFile
	): SearchRequest<Amenity> {
		return SearchRequest.buildSearchPoiRequest(
			0, 0, KAlgorithms.emptyIfNull(articleId.title), 0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
			getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), object : ResultMatcher<Amenity> {
				var done = false

				override fun publish(obj: Amenity): Boolean {
					if (KAlgorithms.stringsEqual(
							KAlgorithms.emptyIfNull(articleId.title), KAlgorithms.emptyIfNull(obj.getName(lang))
						)) {
						amenities.add(Pair(readerFile, obj))
						done = true
					}
					return false
				}

				override fun isCancelled(): Boolean = done
			}, null
		)
	}

	fun getArticleByTitle(title: String, lang: String, readGpx: Boolean): TravelArticle? =
		getArticleByTitle(title, KQuadRect(), lang, readGpx)

	fun getArticleByTitle(title: String, latLon: KLatLon, lang: String, readGpx: Boolean): TravelArticle? {
		val rect = KMapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, ARTICLE_SEARCH_RADIUS)
		return getArticleByTitle(title, rect, lang, readGpx)
	}

	fun getArticleByTitle(
		title: String, rect: KQuadRect, lang: String, readGpx: Boolean
	): TravelArticle? = synchronized(lock) {
		var article: TravelArticle? = null
		val amenities = ArrayList<Amenity>()
		var x = 0
		var y = 0
		var left = 0
		var right = Int.MAX_VALUE
		var top = 0
		var bottom = Int.MAX_VALUE
		if (rect.height() > 0 && rect.width() > 0) {
			x = rect.centerX().toInt()
			y = rect.centerY().toInt()
			left = rect.left.toInt()
			right = rect.right.toInt()
			top = rect.top.toInt()
			bottom = rect.bottom.toInt()
		}
		for (repo in context.getWikivoyageRepositories()) {
			val req = SearchRequest.buildSearchPoiRequest(
				x, y, title, left, right, top, bottom, getSearchFilter(ROUTE_ARTICLE),
				object : ResultMatcher<Amenity> {
					var done = false

					override fun publish(obj: Amenity): Boolean {
						if (KAlgorithms.stringsEqual(title, KAlgorithms.emptyIfNull(obj.getName(lang)))) {
							amenities.add(obj)
							done = true
						}
						return false
					}

					override fun isCancelled(): Boolean = done
				}, null
			)
			repo.searchPoiByName(req)
			if (!KAlgorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(repo.getFile(), amenities[0], lang, readGpx)
				break
			}
		}
		article
	}

	fun getArticleId(title: String, lang: String): TravelArticleIdentifier? {
		var a: TravelArticle? = null
		for (articles in cachedArticles.values) {
			for (article in articles.values) {
				if (article.getTitle() == title) {
					a = article
					break
				}
			}
		}
		if (a == null) {
			a = getArticleByTitle(title, lang, false)
		}
		return a?.generateIdentifier()
	}

	fun getArticleLangs(articleId: TravelArticleIdentifier): List<String> =
		ArrayList(getArticleByLangs(articleId).keys)

	fun getArticleByLangs(articleId: TravelArticleIdentifier): Map<String, TravelArticle> {
		val res = LinkedHashMap<String, TravelArticle>()
		val article = getArticleById(articleId, "", false)
		if (article != null) {
			cachedArticles[article.generateIdentifier()]?.let { res.putAll(it) }
		} else {
			for (a in bookmarks.getSavedArticles(articleId.file, articleId.routeId)) {
				a.getLang()?.let { res[it] = a }
			}
		}
		return res
	}

	fun getGPXName(article: TravelArticle): String = article.getGpxFileName() + IndexConstants.GPX_FILE_EXT

	/**
	 * Writes the article's gpx file into [dir]. Android puts it under `GPX_TRAVEL_DIR`, which is
	 * its own layout, so the directory is asked for here.
	 */
	fun createGpxFile(article: TravelArticle, dir: KFile): KFile {
		val gpx = article.getGpxFile()
		val file = KFile(dir, getGPXName(article))
		if (gpx != null) {
			net.osmand.shared.gpx.GpxUtilities.writeGpxFile(file, gpx)
		}
		return file
	}

	fun getWikivoyageFileName(): String = WORLD_WIKIVOYAGE_FILE_NAME

	fun saveOrRemoveArticle(article: TravelArticle, save: Boolean) {
		if (save) {
			bookmarks.addArticleToSaved(article)
		} else {
			bookmarks.removeArticleFromSaved(article)
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger("TravelObfHelper")

		private const val WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf"
		private const val ARTICLE_SEARCH_RADIUS = 500 * 1000
		private const val SAVED_ARTICLE_SEARCH_RADIUS = 30 * 1000
		private const val MAX_SEARCH_RADIUS = 800 * 1000

		/** Ref: POI_SEARCH_POINTS_INTERVAL_M in tools */
		private const val TRAVEL_GPX_SEARCH_RADIUS = 10 * 1000
		private const val MAX_TRAVEL_GPX_SEARCH_RADIUS = 50 * 1000
		private const val ROUTES_ON_LINE_RADIUS = 25
		private const val ROUTES_ON_LINE_ZOOM = 17

		/** `WikivoyageUtils.EN_LANG_PREFIX` in the android app. */
		private const val EN_LANG_PREFIX = "en:"

		private fun getTitleWithoutPrefix(title: String): String =
			if (title.startsWith(EN_LANG_PREFIX)) title.substring(EN_LANG_PREFIX.length) else title
	}
}
