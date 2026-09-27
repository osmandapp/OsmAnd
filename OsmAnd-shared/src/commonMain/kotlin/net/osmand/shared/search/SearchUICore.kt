package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Amenity.Companion.ROUTE_ID
import net.osmand.shared.data.BaseDetailsObject
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.io.DispatcherProvider
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.search.core.CustomSearchPoiFilter
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreAPI
import net.osmand.shared.search.core.SearchCoreFactory
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import net.osmand.shared.search.core.matchesName
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTreeSet
import okio.IOException
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * The search as the ui runs it: the apis in the order of their priority for a phrase, what they
 * find sorted, united by osm id or wikidata and cleared of duplicates, and a search of each phrase
 * typed in, one after another, cancelled by the next one.
 *
 * A copy of `SearchUICore` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. What serves the spatial search, its apis, `init(true)` and the levels of the results
 * shown, comes with the spatial search. Left out: the statistics of time and memory java keeps
 * for a search, and the test json it makes of the objects a search exports.
 */
class SearchUICore(
	private var poiTypes: MapPoiTypes,
	locale: String?,
	transliterate: Boolean,
	internetConnectionAvailable: (() -> Boolean)?
) {

	private var phrase: SearchPhrase
	private var currentSearchResult: SearchResultCollection

	private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)
	private val executor = lazy {
		val queue = tasks
		CoroutineScope(DispatcherProvider.singleThread()).launch {
			for (task in queue) {
				try {
					task()
				} catch (e: Throwable) {
					// java's executor keeps it in a future nobody reads
				}
			}
		}
	}
	private var onSearchStart: (() -> Unit)? = null
	private var onResultsComplete: (() -> Unit)? = null
	private val requestNumber = AtomicInt(0)
	private var totalLimit = -1 // -1 unlimited - not used

	internal val apis: MutableList<SearchCoreAPI> = ArrayList()
	private var searchSettings: SearchSettings
	private val internetConnectionAvailable: () -> Boolean = internetConnectionAvailable ?: { true }

	constructor(poiTypes: MapPoiTypes, locale: String?, transliterate: Boolean) :
			this(poiTypes, locale, transliterate, { true })

	init {
		var searchSettings = SearchSettings(ArrayList<BinaryMapIndexReader>())
		searchSettings = searchSettings.setLang(locale, transliterate)
		this.searchSettings = searchSettings
		phrase = SearchPhrase.emptyPhrase(searchSettings)
		currentSearchResult = SearchResultCollection(phrase)
	}

	class SearchResultCollection(private val phrase: SearchPhrase) {
		internal val searchResults: MutableList<SearchResult> = ArrayList()
		private var useLimit = false

		fun combineWithCollection(collection: SearchResultCollection, resort: Boolean, removeDuplicates: Boolean): SearchResultCollection {
			val src = SearchResultCollection(phrase)
			src.addSearchResults(searchResults, false, false)
			src.addSearchResults(collection.searchResults, resort, removeDuplicates)
			return src
		}

		fun getUseLimit(): Boolean = useLimit

		fun setUseLimit(useLimit: Boolean) {
			this.useLimit = useLimit
		}

		fun addSearchResults(sr: List<SearchResult>, resortAll: Boolean, removeDuplicates: Boolean): SearchResultCollection {
			if (isDebugMode()) {
				LOG.info("Add search results resortAll=$resortAll removeDuplicates=$removeDuplicates Results=${sr.size} Current results=${searchResults.size}")
			}
			if (KAlgorithms.isEmpty(sr)) {
				return this
			}
			if (resortAll) {
				searchResults.addAll(sr)
				if (removeDuplicates) {
					val start = currentTimeMillis()
					val size = searchResults.size
					uniteSearchResultsByOsmIdOrWikidata(searchResults)
					if (isDebugMode()) {
						LOG.info("Deduplicate time ${currentTimeMillis() - start} ms (removed ${size - searchResults.size} results=$size-${searchResults.size})\n")
					}
				}
				sortSearchResults()
				if (removeDuplicates) {
					filterSearchDuplicateResults()
				}
			} else {
				if (!removeDuplicates) {
					searchResults.addAll(sr)
				} else {
					val addedResults = ArrayList(sr)
					val cmp = SearchResultComparator(phrase)
					addedResults.sortWith(cmp)
					filterSearchDuplicateResults(addedResults)
					var i = 0
					var j = 0
					while (j < addedResults.size) {
						val addedResult = addedResults[j]
						if (i >= searchResults.size) {
							var k = 0
							var same = false
							while (searchResults.size > k && k < DEPTH_TO_CHECK_SAME_SEARCH_RESULTS) {
								if (sameSearchResult(addedResult, searchResults[searchResults.size - k - 1])) {
									same = true
									break
								}
								k++
							}
							if (!same) {
								searchResults.add(addedResult)
							}
							j++
							continue
						}
						val existingResult = searchResults[i]
						if (sameSearchResult(addedResult, existingResult)) {
							j++
							continue
						}
						val compare = cmp.compare(existingResult, addedResult)
						if (compare == 0) {
							// existingResult == addedResult
							j++
						} else if (compare > 0) {
							// existingResult > addedResult
							searchResults.add(addedResults[j])
							j++
						} else {
							// existingResult < addedResult
							i++
						}
					}
				}
			}
			calculateAddressString()
			if (isDebugMode()) {
				LOG.info("Search results added. Current results=${searchResults.size}")
			}
			return this
		}

		fun hasSearchResults(): Boolean = !KAlgorithms.isEmpty(searchResults)

		fun getCurrentSearchResults(): List<SearchResult> = searchResults

		fun getPhrase(): SearchPhrase = phrase

		fun calculateAddressString() {
			var dominatedCity = ""
			val cityCounter = HashMap<String, Int>()
			for (s in searchResults) {
				val cityName = s.cityName
				if (!KAlgorithms.isEmpty(cityName)) {
					val mainCity = getMainCityName(cityName!!)
					val domCity = getDominatedCity(cityCounter, mainCity)
					if (domCity != null) {
						dominatedCity = domCity
						break
					}
				}
			}
			for (s in searchResults) {
				val amenity = s.`object`
				if (amenity is Amenity && KAlgorithms.isEmpty(s.addressName)) {
					updateSearchResultAddress(s, amenity, dominatedCity)
				}
			}
		}

		fun sortSearchResults() {
			if (debugMode) {
				LOG.info("Sorting search results <$phrase> Results=${searchResults.size}")
			}
			searchResults.sortWith(SearchResultComparator(phrase))
			if (debugMode) {
				LOG.info("Search results sorted <$phrase>")
			}
		}

		fun filterSearchDuplicateResults() {
			if (debugMode) {
				LOG.info("Filter duplicate results <$phrase> Results=${searchResults.size}")
			}
			filterSearchDuplicateResults(searchResults)
			if (debugMode) {
				LOG.info("Duplicate results filtered <$phrase> Results=${searchResults.size}")
			}
		}

		private fun filterSearchDuplicateResults(lst: MutableList<SearchResult>) {
			var i = 0
			while (i < lst.size) {
				val current = lst[i]
				var duplicate = false
				for (j in i - 1 downTo maxOf(i - DEPTH_TO_CHECK_SAME_SEARCH_RESULTS, 0)) {
					val prevAdded = lst[j]
					if (sameSearchResult(prevAdded, current)) {
						duplicate = true
						val wDiff = kotlin.math.abs(current.getUnknownPhraseMatchWeight() - prevAdded.getUnknownPhraseMatchWeight())
						if (ObjectType.getTypeWeight(current.objectType) > ObjectType.getTypeWeight(prevAdded.objectType) && wDiff <= 1) {
							lst[j] = current
						}
					}
				}
				if (duplicate) {
					lst.removeAt(i)
				} else {
					i++
				}
			}
		}

		private fun uniteData(list: MutableList<SearchResult>): SearchResult {
			val unique = list.removeAt(0)
			val base = BaseDetailsObject(unique.`object`, phrase.getSettings()!!.getLang())
			for (iterated in list) {
				base.addObject(iterated.`object`)

				unique.`object` = base.getSyntheticAmenity()
				val iteratedNames = iterated.otherNames
				if (iteratedNames != null) {
					if (iterated.localeName!! != unique.localeName) {
						(iteratedNames as MutableCollection<String>).add(iterated.localeName!!)
					}
					if (unique.otherNames == null) {
						unique.otherNames = ArrayList()
					}
					val uniqueNames = unique.otherNames as MutableCollection<String>
					for (name in iteratedNames) {
						if (!uniqueNames.contains(name)) {
							uniqueNames.add(name)
						}
					}
				}
				val iteratedWords = iterated.getOtherWordsMatch()
				if (iteratedWords != null) {
					if (unique.getOtherWordsMatch() == null) {
						unique.setOtherWordsMatch(KTreeSet(naturalOrder()))
					}
					unique.getOtherWordsMatch()!!.addAll(iteratedWords)
				}
				if (iterated.getUnknownPhraseMatchWeight() > unique.getUnknownPhraseMatchWeight()) {
					unique.setUnknownPhraseMatchWeight(iterated.getUnknownPhraseMatchWeight())
				}
			}
			return unique
		}

		private fun uniteSearchResultsByOsmIdOrWikidata(input: MutableList<SearchResult>) {
			val output = ArrayList<SearchResult>()
			val osmIdMap = HashMap<Long, Int>()
			val wikidataMap = HashMap<String, Int>()
			val copyDataMap = HashMap<Int, MutableList<SearchResult>>()
			for (sr in input) {
				val that = sr.`object`
				if (that is Amenity) {
					var osmId = that.getOsmId()
					var wikidata = that.getWikidata()

					if (osmId != null && osmId < 0) {
						osmId = null // do not merge synthetic osmId such as wiki
					}
					if (that.isRouteTrack()) {
						osmId = null
						wikidata = null // do not merge routes
					}

					val foundOsmIdIndex = if (osmId == null) null else osmIdMap[osmId]
					val foundWikidataIndex = if (wikidata == null) null else wikidataMap[wikidata]

					var indexToUpdate = -1 // unique

					if (foundOsmIdIndex != null && foundWikidataIndex != null && foundOsmIdIndex != foundWikidataIndex) {
						LOG.info("foundOsmIdIndex != foundWikidataIndex (should never happens)")
					} else if (foundOsmIdIndex != null || foundWikidataIndex != null) {
						indexToUpdate = foundOsmIdIndex ?: foundWikidataIndex!!
					}

					if (indexToUpdate == -1) {
						output.add(sr)
						indexToUpdate = output.size - 1
					} else {
						copyDataMap.getOrPut(indexToUpdate) { ArrayList() }.add(sr)
					}

					if (osmId != null) {
						osmIdMap[osmId] = indexToUpdate
					}
					if (wikidata != null) {
						wikidataMap[wikidata] = indexToUpdate
					}
				} else {
					output.add(sr)
				}
			}
			if (copyDataMap.isNotEmpty()) {
				val lang = phrase.getSettings()!!.getLang()
				for ((indexToUpdate, sr) in copyDataMap) {
					val r = output[indexToUpdate]
					sr.add(0, r)
					sr.sortWith { s1, s2 ->
						val r1 = s1.getResourceType()
						val r2 = s2.getResourceType()
						if (r1.getWeight() != r2.getWeight()) {
							return@sortWith if (r1.getWeight() > r2.getWeight()) -1 else 1
						}
						val am1 = s1.`object`
						val am2 = s2.`object`
						if (am1 is Amenity && am1.isRouteArticle() && am2 is Amenity && am2.isRouteArticle()) {
							val l1 = BaseDetailsObject.getLangForTravel(am1)
							val l2 = BaseDetailsObject.getLangForTravel(am2)
							if (l1 != l2) {
								return@sortWith if (l1 == lang) -1 else 1
							}
						}
						0
					}
					output[indexToUpdate] = uniteData(sr)
				}
			}
			if (input.size != output.size) {
				input.clear()
				input.addAll(output)
			}
		}

		fun sameSearchResult(r1: SearchResult, r2: SearchResult): Boolean {
			val isSameType = r1.objectType == r2.objectType
			var interpolated = false
			if (isSameType) {
				val type = r1.objectType
				if (type == ObjectType.INDEX_ITEM || type == ObjectType.GPX_TRACK) {
					return r1.localeName == r2.localeName
				}
				val building = r2.`object`
				if (r2.objectType == ObjectType.HOUSE && building is Building) {
					val streetEquals = r1.localeRelatedObjectName!! == r2.localeRelatedObjectName
					interpolated = streetEquals && building.getInterpolationType() != null
				}
			}
			val location1 = r1.location
			val location2 = r2.location
			if (location1 != null && location2 != null &&
				!ObjectType.isTopVisible(r1.objectType) && !ObjectType.isTopVisible(r2.objectType)
			) {
				if (isSameType) {
					if (r1.objectType == ObjectType.STREET) {
						val st1 = r1.`object` as Street
						val st2 = r2.`object` as Street
						return st1.getLocation()!! == st2.getLocation()
					}
				}
				val a1 = r1.`object` as? Amenity
				val a2 = r2.`object` as? Amenity
				if (r1.localeName!! == r2.localeName) {
					var similarityRadius = 30.0
					if (a1 != null && a2 != null && a1.getId() != null && a2.getId() != null) {
						// here 2 points are amenity
						val type1 = a1.getType()!!.getKeyName()
						val type2 = a2.getType()!!.getKeyName()
						val subType1 = a1.getSubType()
						val subType2 = a2.getSubType()

						val isEqualId = ObfConstants.getOsmObjectId(a1) == ObfConstants.getOsmObjectId(a2)

						if (isEqualId && (FILTER_DUPLICATE_POI_SUBTYPE.contains(subType1!!)
									|| FILTER_DUPLICATE_POI_SUBTYPE.contains(subType2!!))
						) {
							return true
						} else if (type1 != type2) {
							return false
						}

						if (type1 == "natural") {
							similarityRadius = 50000.0
						} else if (subType1!! == subType2) {
							if (subType1.contains("cn_ref") || subType1.contains("wn_ref")
								|| (subType1.startsWith("route_hiking_") && subType1.endsWith("n_poi"))
							) {
								similarityRadius = 50000.0
							}
							if (a1.getAdditionalInfo(ROUTE_ID) != null && KAlgorithms.stringsEqual(a1.getAdditionalInfo(ROUTE_ID), a2.getAdditionalInfo(ROUTE_ID))) {
								similarityRadius = 1_000_000.0
							}
						}
					} else if (ObjectType.isAddress(r1.objectType) && ObjectType.isAddress(r2.objectType)) {
						similarityRadius = if (interpolated) 1000.0 else 100.0
					}
					return KMapUtils.getDistance(location1, location2) < similarityRadius
				}
			} else if (r1.`object` != null && r2.`object` != null) {
				return r1.`object` == r2.`object`
			}
			return false
		}

		companion object {
			private const val DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20
			internal const val DOMINATED_CITY_CRITERIA = 5
		}
	}

	fun getPoiTypes(): MapPoiTypes = poiTypes

	fun setPoiTypes(poiTypes: MapPoiTypes) {
		this.poiTypes = poiTypes
	}

	fun getTotalLimit(): Int = totalLimit

	fun setTotalLimit(totalLimit: Int) {
		this.totalLimit = totalLimit
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getApiByClass(cl: KClass<T>): T? {
		for (a in apis) {
			if (cl.isInstance(a)) {
				return a as T
			}
		}
		return null
	}

	fun <T : SearchCoreAPI> shallowSearch(cl: KClass<T>, text: String, matcher: ResultMatcher<SearchResult>?): SearchResultCollection? {
		return shallowSearch(cl, text, matcher, true, true)
	}

	fun <T : SearchCoreAPI> shallowSearch(
		cl: KClass<T>, text: String, matcher: ResultMatcher<SearchResult>?,
		resortAll: Boolean, removeDuplicates: Boolean
	): SearchResultCollection? {
		return shallowSearch(cl, text, matcher, resortAll, removeDuplicates, searchSettings)
	}

	fun <T : SearchCoreAPI> shallowSearch(
		cl: KClass<T>, text: String, matcher: ResultMatcher<SearchResult>?,
		resortAll: Boolean, removeDuplicates: Boolean, searchSettings: SearchSettings?
	): SearchResultCollection? {
		val api = getApiByClass(cl)
		if (api != null) {
			if (debugMode) {
				LOG.info("Start shallow search <$phrase> API=<$api>")
			}
			val sphrase = phrase.generateNewPhrase(text, searchSettings)
			preparePhrase(sphrase)
			val ai = AtomicInt(0)
			val rm = SearchResultMatcher(matcher, sphrase, ai.get(), ai, totalLimit)
			api.search(sphrase, rm)

			val collection = SearchResultCollection(sphrase)
			if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
				collection.setUseLimit(true)
			}
			collection.addSearchResults(rm.getRequestResults(), resortAll, removeDuplicates)
			if (debugMode) {
				LOG.info("Finish shallow search <$sphrase> Results=${rm.getRequestResults().size}")
			}
			return collection
		}
		return null
	}

	fun <T : SearchCoreAPI> shallowSearchAsync(
		cl: KClass<T>, text: String, matcher: ResultMatcher<SearchResult>?,
		resortAll: Boolean, removeDuplicates: Boolean, searchSettings: SearchSettings?,
		callback: ((SearchResultCollection?) -> Boolean)?
	) {
		submit {
			try {
				val collection = shallowSearch(cl, text, matcher, resortAll, removeDuplicates, searchSettings)
				callback?.invoke(collection)
			} catch (e: IOException) {
				LOG.error(e.message, e)
			}
		}
	}

	fun init() {
		val amenitiesApi = SearchCoreFactory.SearchAmenityByNameAPI()
		apis.add(amenitiesApi)
		apis.add(SearchCoreFactory.SearchLocationAndUrlAPI(amenitiesApi, internetConnectionAvailable))
		val searchAmenityTypesAPI = SearchCoreFactory.SearchAmenityTypesAPI(poiTypes)
		apis.add(searchAmenityTypesAPI)
		apis.add(SearchCoreFactory.SearchAmenityByTypeAPI(poiTypes, searchAmenityTypesAPI))
		val streetsApi = SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI()
		apis.add(streetsApi)
		val cityApi = SearchCoreFactory.SearchStreetByCityAPI(streetsApi)
		apis.add(cityApi)
		val townCitiesCache = SearchCoreFactory.TownCitiesCache()
		apis.add(SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, false, townCitiesCache))
		apis.add(SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, true, townCitiesCache))
	}

	fun clearAPIs() {
		apis.clear()
	}

	fun clearCustomSearchPoiFilters() {
		for (capi in apis) {
			if (capi is SearchCoreFactory.SearchAmenityTypesAPI) {
				capi.clearCustomFilters()
			}
		}
	}

	fun addCustomSearchPoiFilter(poiFilter: CustomSearchPoiFilter, priority: Int) {
		for (capi in apis) {
			if (capi is SearchCoreFactory.SearchAmenityTypesAPI) {
				capi.addCustomFilter(poiFilter, priority)
			}
		}
	}

	fun setActivePoiFiltersByOrder(filterOrders: List<String>) {
		for (capi in apis) {
			if (capi is SearchCoreFactory.SearchAmenityTypesAPI) {
				capi.setActivePoiFiltersByOrder(filterOrders)
			}
		}
	}

	fun registerAPI(api: SearchCoreAPI) {
		apis.add(api)
	}

	fun getCurrentSearchResult(): SearchResultCollection = currentSearchResult

	fun getPhrase(): SearchPhrase = phrase

	fun setOnSearchStart(onSearchStart: (() -> Unit)?) {
		this.onSearchStart = onSearchStart
	}

	fun setOnResultsComplete(onResultsComplete: (() -> Unit)?) {
		this.onResultsComplete = onResultsComplete
	}

	fun getSearchSettings(): SearchSettings = searchSettings

	fun updateSettings(settings: SearchSettings) {
		searchSettings = settings
	}

	private fun filterCurrentResults(phrase: SearchPhrase, matcher: ResultMatcher<SearchResult>?) {
		if (matcher == null) {
			return
		}
		val l = currentSearchResult.searchResults
		for (r in l) {
			if (filterOneResult(r, phrase)) {
				matcher.publish(r)
			}
			if (matcher.isCancelled()) {
				return
			}
		}
	}

	private fun filterOneResult(obj: SearchResult, phrase: SearchPhrase): Boolean {
		val nameStringMatcher = phrase.getFirstUnknownNameStringMatcher()
		return nameStringMatcher.matchesName(obj.localeName) || nameStringMatcher.matches(obj.otherNames)
	}

	fun selectSearchResult(r: SearchResult): Boolean {
		val newSettings = phrase.getSettings()
		phrase = phrase.selectWord(r, newSettings)
		return true
	}

	fun resetSearch() {
		phrase = SearchPhrase.emptyPhrase(searchSettings)
		currentSearchResult = SearchResultCollection(phrase)
	}

	fun resetPhrase(): SearchPhrase {
		phrase = phrase.generateNewPhrase("", searchSettings)
		return phrase
	}

	fun resetPhrase(text: String): SearchPhrase {
		phrase = phrase.generateNewPhrase(text, searchSettings)
		return phrase
	}

	fun resetPhrase(result: SearchResult): SearchPhrase {
		phrase = phrase.generateNewPhrase("", searchSettings)
		phrase.addResult(result, phrase)
		return phrase
	}

	fun immediateSearch(text: String, loc: KLatLon?): SearchResultCollection {
		if (loc != null) {
			searchSettings = searchSettings.setOriginalLocation(loc)
		}
		val searchPhrase = phrase.generateNewPhrase(text, searchSettings)
		val rm = SearchResultMatcher(null, searchPhrase, requestNumber.get(), requestNumber, totalLimit)
		searchInternal(searchPhrase, rm)
		val resultCollection = SearchResultCollection(searchPhrase)
		if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
			resultCollection.setUseLimit(true)
		}
		resultCollection.addSearchResults(rm.getRequestResults(), true, true)
		val settings = phrase.getSettings()!!
		if (settings.isExportObjects()) {
			settings.setExportedCities(rm.getExportedCities())
			settings.setExportedObjects(rm.getExportedObjects())
		}
		return resultCollection
	}

	fun search(text: String, delayedExecution: Boolean, matcher: ResultMatcher<SearchResult>?) {
		search(text, delayedExecution, matcher, null)
	}

	fun search(text: String, delayedExecution: Boolean, matcher: ResultMatcher<SearchResult>?, overrideSettings: SearchSettings?) {
		if (overrideSettings != null) {
			searchSettings = overrideSettings
		}
		val request = requestNumber.incrementAndGet()
		val phrase = this.phrase.generateNewPhrase(text, searchSettings)
		phrase.setAcceptPrivate(this.phrase.isAcceptPrivate())
		this.phrase = phrase
		if (debugMode) {
			LOG.info("Prepare search <$phrase>")
		}
		submit {
			onSearchStart?.invoke()
			val rm = SearchResultMatcher(matcher, phrase, request, requestNumber, totalLimit)
			if (debugMode) {
				LOG.info("Starting search <$phrase>")
			}
			rm.searchStarted(phrase)
			if (debugMode) {
				LOG.info("Search started <$phrase>")
			}
			if (delayedExecution) {
				val startTime = currentTimeMillis()
				if (debugMode) {
					LOG.info("Wait for next char <$phrase>")
				}

				var filtered = false
				while (currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
					if (rm.isCancelled()) {
						if (debugMode) {
							LOG.info("Search cancelled <$phrase>")
						}
						return@submit
					}
					delay(TIMEOUT_BEFORE_FILTER)

					if (!filtered) {
						val quickRes = SearchResultCollection(phrase)
						if (debugMode) {
							LOG.info("Filtering current data <$phrase> Results=${currentSearchResult.searchResults.size}")
						}
						filterCurrentResults(phrase, object : ResultMatcher<SearchResult> {
							override fun publish(obj: SearchResult): Boolean {
								quickRes.searchResults.add(obj)
								return true
							}

							override fun isCancelled(): Boolean = rm.isCancelled()
						})
						if (debugMode) {
							LOG.info("Current data filtered <$phrase> Results=${quickRes.searchResults.size}")
						}
						if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
							quickRes.setUseLimit(true)
						}
						if (!rm.isCancelled()) {
							currentSearchResult = quickRes
							rm.filterFinished(phrase)
						}
						filtered = true
					}
				}
			} else {
				delay(TIMEOUT_BEFORE_SEARCH)
			}
			if (rm.isCancelled()) {
				if (debugMode) {
					LOG.info("Search cancelled <$phrase>")
				}
				return@submit
			}
			searchInternal(phrase, rm)
			if (!rm.isCancelled()) {
				val collection = SearchResultCollection(phrase)
				if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
					collection.setUseLimit(true)
				}
				if (debugMode) {
					LOG.info("Processing search results <$phrase>")
				}
				collection.addSearchResults(rm.getRequestResults(), true, true)
				if (debugMode) {
					LOG.info("Finishing search <$phrase> Results=${rm.getRequestResults().size}")
				}
				currentSearchResult = collection
				rm.searchFinished(phrase)
				onResultsComplete?.invoke()
				if (debugMode) {
					LOG.info("Search finished <$phrase> Results=${rm.getRequestResults().size}")
				}
			} else {
				if (debugMode) {
					LOG.info("Search cancelled <$phrase>")
				}
			}
		}
	}

	/** Runs [task] after the ones before it, one at a time, as java's executor of one thread. */
	private fun submit(task: suspend () -> Unit) {
		executor.value
		tasks.trySend(task)
	}

	fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
		for (api in apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) >= 0
				&& api.isSearchMoreAvailable(phrase)
			) {
				return true
			}
		}
		return false
	}

	fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
		var radius = Int.MAX_VALUE
		for (api in apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
				val apiMinimalRadius = api.getMinimalSearchRadius(phrase)
				if (apiMinimalRadius > 0 && apiMinimalRadius < radius) {
					radius = apiMinimalRadius
				}
			}
		}
		return radius
	}

	fun getNextSearchRadius(phrase: SearchPhrase): Int {
		var radius = Int.MAX_VALUE
		for (api in apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
				val apiNextSearchRadius = api.getNextSearchRadius(phrase)
				if (apiNextSearchRadius > 0 && apiNextSearchRadius < radius) {
					radius = apiNextSearchRadius
				}
			}
		}
		return radius
	}

	fun getUnselectedPoiType(): AbstractPoiType? {
		for (capi in apis) {
			if (capi is SearchCoreFactory.SearchAmenityByTypeAPI) {
				return capi.getUnselectedPoiType()
			}
		}
		return null
	}

	fun getCustomNameFilter(): String? {
		for (capi in apis) {
			if (capi is SearchCoreFactory.SearchAmenityByTypeAPI) {
				return capi.getNameFilter()
			}
		}
		return null
	}

	internal fun searchInternal(phrase: SearchPhrase, matcher: SearchResultMatcher) {
		preparePhrase(phrase)
		val lst = ArrayList(apis)
		lst.sortWith { o1, o2 -> o1.getSearchPriority(phrase).compareTo(o2.getSearchPriority(phrase)) }
		for (api in lst) {
			val start = if (debugMode) currentTimeMillis() else 0L
			if (matcher.isCancelled()) {
				break
			}
			if (!api.isSearchAvailable(phrase) || api.getSearchPriority(phrase) == -1) {
				continue
			}
			try {
				if (debugMode) {
					LOG.info("Run API search <$phrase> API=<$api>")
				}
				api.search(phrase, matcher)
				if (debugMode) {
					LOG.info("API search finishing <$phrase> API=<$api>")
				}
				matcher.apiSearchFinished(api, phrase)
				if (debugMode) {
					LOG.info("API search done <$phrase> API=<$api>, time=${currentTimeMillis() - start}")
				}
			} catch (e: Throwable) {
				LOG.error(e.message, e)
			}
		}
	}

	private fun preparePhrase(phrase: SearchPhrase) {
		if (debugMode) {
			LOG.info("Preparing search phrase <$phrase>")
		}
		for (sw in phrase.getWords()) {
			val file = sw.getResult()?.file
			if (file != null) {
				phrase.selectFile(file)
			}
		}
		phrase.sortFiles()
		if (debugMode) {
			LOG.info("Search phrase prepared <$phrase>")
		}
	}

	class SearchResultMatcher(
		private val matcher: ResultMatcher<SearchResult>?,
		private var phrase: SearchPhrase?,
		private val request: Int,
		private val requestNumber: AtomicInt,
		internal var totalLimit: Int
	) : ResultMatcher<SearchResult> {

		private val requestResults: MutableList<SearchResult> = ArrayList()
		private var parentSearchResult: SearchResult? = null
		internal var count = 0
		private var exportedObjects: MutableList<MapObject>? = null
		private var exportedCities: MutableList<City>? = null

		fun setParentSearchResult(parentSearchResult: SearchResult?): SearchResult? {
			val prev = this.parentSearchResult
			this.parentSearchResult = parentSearchResult
			return prev
		}

		fun getParentSearchResult(): SearchResult? = parentSearchResult

		fun getRequestResults(): List<SearchResult> = requestResults

		fun getCount(): Int = requestResults.size

		fun searchStarted(phrase: SearchPhrase) {
			if (matcher != null) {
				val sr = SearchResult(phrase)
				sr.objectType = ObjectType.SEARCH_STARTED
				matcher.publish(sr)
			}
		}

		fun filterFinished(phrase: SearchPhrase) {
			if (matcher != null) {
				val sr = SearchResult(phrase)
				sr.objectType = ObjectType.FILTER_FINISHED
				matcher.publish(sr)
			}
		}

		fun searchFinished(phrase: SearchPhrase) {
			if (matcher != null) {
				val sr = SearchResult(phrase)
				sr.objectType = ObjectType.SEARCH_FINISHED
				matcher.publish(sr)
			}
		}

		fun apiSearchFinished(api: SearchCoreAPI, phrase: SearchPhrase) {
			if (matcher != null) {
				val sr = SearchResult(phrase)
				sr.objectType = ObjectType.SEARCH_API_FINISHED
				sr.`object` = api
				sr.parentSearchResult = parentSearchResult
				matcher.publish(sr)
			}
		}

		fun apiSearchRegionFinished(api: SearchCoreAPI, region: BinaryMapIndexReader, phrase: SearchPhrase) {
			if (matcher != null) {
				val sr = SearchResult(phrase)
				sr.objectType = ObjectType.SEARCH_API_REGION_FINISHED
				sr.`object` = api
				sr.parentSearchResult = parentSearchResult
				sr.file = region
				matcher.publish(sr)
				if (debugMode) {
					LOG.info("API region search done <$phrase> API=<$api> Region=<${region.getFile().name()}>")
				}
			}
		}

		override fun publish(obj: SearchResult): Boolean {
			// disable boundary for end results
			if (obj.objectType == ObjectType.BOUNDARY) {
				return false
			}
			val phrase = phrase
			if (phrase != null && !phrase.getFirstUnknownNameStringMatcher().matchesName(obj.localeName)
				&& KAlgorithms.isEmpty(obj.alternateName)
			) {
				var updateName = false
				val otherNames = obj.otherNames
				if (otherNames != null) {
					for (s in otherNames) {
						if (phrase.getFirstUnknownNameStringMatcher().matches(s)) {
							// previous implementation didn't fit enough
//							object.localeName = s;
							obj.alternateName = s
							updateName = true
							break
						}
					}
				}
				val amenity = obj.`object`
				if (!updateName && amenity is Amenity) {
					for (key in amenity.getAdditionalInfoKeys()) {
						if ((!ObfConstants.isTagIndexedForSearchAsId(key) &&
								!ObfConstants.isTagNonIndexedForSearchAsName(key) &&
								!ObfConstants.isTagIndexedForSearchAsName(key))
						) {
							continue
						}
						val vl = amenity.getAdditionalInfo(key)
						if (phrase.getFirstUnknownNameStringMatcher().matchesName(vl)) {
							obj.alternateName = vl
							break
						}
					}
				}
			}
			if (KAlgorithms.isEmpty(obj.localeName) && obj.alternateName != null) {
				obj.localeName = obj.alternateName
				obj.alternateName = null
			}
			obj.parentSearchResult = parentSearchResult
			if (matcher == null || matcher.publish(obj)) {
				count++
				if (totalLimit == -1 || count < totalLimit) {
					requestResults.add(obj)
				}
				return true
			}
			return false
		}

		override fun isCancelled(): Boolean {
			val cancelled = request != requestNumber.get()
			return cancelled || (matcher != null && matcher.isCancelled())
		}

		fun getExportedObjects(): MutableList<MapObject>? = exportedObjects

		fun getExportedCities(): MutableList<City>? = exportedCities

		fun exportObject(phrase: SearchPhrase, obj: MapObject) {
			val settings = phrase.getSettings()!!
			val maxDistance = settings.getExportSettings()!!.getMaxDistance()
			if (maxDistance > 0) {
				val distance = KMapUtils.getDistance(settings.getOriginalLocation()!!, obj.getLocation()!!)
				if (distance > maxDistance) {
					return
				}
			}
			var exportedObjects = exportedObjects
			if (exportedObjects == null) {
				exportedObjects = ArrayList()
				this.exportedObjects = exportedObjects
			}
			exportedObjects.add(obj)
		}

		fun exportCity(phrase: SearchPhrase, city: City) {
			val settings = phrase.getSettings()!!
			val maxDistance = settings.getExportSettings()!!.getMaxDistance()
			if (maxDistance > 0) {
				val distance = KMapUtils.getDistance(settings.getOriginalLocation()!!, city.getLocation()!!)
				if (distance > maxDistance) {
					return
				}
			}
			var exportedCities = exportedCities
			if (exportedCities == null) {
				exportedCities = ArrayList()
				this.exportedCities = exportedCities
			}
			exportedCities.add(city)
		}
	}

	private enum class ResultCompareStep {
		TOP_VISIBLE,
		FOUND_WORD_COUNT, // more is better (top)
		UNKNOWN_PHRASE_MATCH_WEIGHT, // more is better (top)
		SEARCH_DISTANCE_IF_NOT_BY_NAME,
		COMPARE_FIRST_NUMBER_IN_NAME,
		COMPARE_INTERPOLATED,
		COMPARE_DISTANCE_TO_PARENT_SEARCH_RESULT, // makes sense only for inner subqueries
		COMPARE_BY_NAME,
		COMPARE_BY_DISTANCE,
		AMENITY_LAST_AND_SORT_BY_SUBTYPE;

		// -1 - means 1st is less (higher) than 2nd
		fun compare(o1: SearchResult, o2: SearchResult, c: SearchResultComparator): Int {
			when (this) {
				TOP_VISIBLE -> {
					val topVisible1 = ObjectType.isTopVisible(o1.objectType)
					val topVisible2 = ObjectType.isTopVisible(o2.objectType)
					if (topVisible1 != topVisible2) {
						// -1 - means 1st is less than 2nd
						return if (topVisible1) -1 else 1
					}
				}
				FOUND_WORD_COUNT -> {
					if (o1.getFoundWordCount() != o2.getFoundWordCount()) {
						return -o1.getFoundWordCount().compareTo(o2.getFoundWordCount())
					}
				}
				UNKNOWN_PHRASE_MATCH_WEIGHT -> {
					// here we check how much each sub search result matches the phrase
					// also we sort it by type house -> street/poi -> city/postcode/village/other
					var o1PhraseWeight = o1.getUnknownPhraseMatchWeight()
					var o2PhraseWeight = o2.getUnknownPhraseMatchWeight()
					if (o1PhraseWeight == o2PhraseWeight && o1PhraseWeight / SearchResult.MAX_PHRASE_WEIGHT_TOTAL > 1) {
						if (!o1.requiredSearchPhrase.getUnknownWordToSearchBuildingNameMatcher().matchesName(SearchPhrase.stripBraces(o1.localeName))) {
							o1PhraseWeight--
						}
						if (!o2.requiredSearchPhrase.getUnknownWordToSearchBuildingNameMatcher().matchesName(SearchPhrase.stripBraces(o2.localeName))) {
							o2PhraseWeight--
						}
					}
					if (o1PhraseWeight != o2PhraseWeight) {
						return -o1PhraseWeight.compareTo(o2PhraseWeight)
					}
				}
				SEARCH_DISTANCE_IF_NOT_BY_NAME -> {
					if (c.sortType != SearchSettings.SortType.IGNORE_DISTANCE) {
						val s1 = o1.getSearchDistance(c.loc)
						val s2 = o2.getSearchDistance(c.loc)
						if (s1 != s2) {
							return s1.compareTo(s2)
						}
					}
				}
				COMPARE_FIRST_NUMBER_IN_NAME -> {
					val localeName1 = o1.localeName ?: ""
					val localeName2 = o2.localeName ?: ""
					val st1 = KAlgorithms.extractFirstIntegerNumber(localeName1)
					val st2 = KAlgorithms.extractFirstIntegerNumber(localeName2)
					if (st1 != st2) {
						return st1.compareTo(st2)
					}
				}
				COMPARE_INTERPOLATED -> {
					val building1 = o1.`object`
					val building2 = o2.`object`
					if (building1 is Building && building2 is Building) {
						val interpolated1 = building1.getInterpolationType() != null
						val interpolated2 = building2.getInterpolationType() != null
						if (interpolated1 != interpolated2) {
							// interpolated second
							return if (interpolated1) 1 else -1
						}
					}
				}
				COMPARE_DISTANCE_TO_PARENT_SEARCH_RESULT -> {
					val ps1 = o1.parentSearchResult?.getSearchDistance(c.loc) ?: 0.0
					val ps2 = o2.parentSearchResult?.getSearchDistance(c.loc) ?: 0.0
					if (ps1 != ps2) {
						return ps1.compareTo(ps2)
					}
				}
				COMPARE_BY_NAME -> {
					val localeName1 = o1.localeName ?: ""
					val localeName2 = o2.localeName ?: ""
					val cmp = c.collator.compare(localeName1, localeName2)
					if (cmp != 0) {
						return cmp
					}
				}
				COMPARE_BY_DISTANCE -> {
					val s1 = o1.getSearchDistance(c.loc, 1.0)
					val s2 = o2.getSearchDistance(c.loc, 1.0)
					if (s1 != s2) {
						return s1.compareTo(s2)
					}
				}
				AMENITY_LAST_AND_SORT_BY_SUBTYPE -> {
					val am1 = o1.`object` is Amenity
					val am2 = o2.`object` is Amenity
					if (am1 != am2) {
						// amenity second
						return if (am1) 1 else -1
					}
				}
			}
			return 0
		}
	}

	fun isOnlineSearch(): Boolean = searchSettings.hasCustomSearchType(ObjectType.ONLINE_SEARCH)

	class SearchResultComparator(sp: SearchPhrase) : Comparator<SearchResult> {
		internal val collator: KCollator = sp.getCollator()
		internal val loc: KLatLon? = sp.getLastTokenLocation()
		internal val sortType: SearchSettings.SortType? = sp.getSettings()!!.getSortType()

		override fun compare(a: SearchResult, b: SearchResult): Int {
			if (sortType == SearchSettings.SortType.ONLY_BY_DISTANCE) {
				return ResultCompareStep.COMPARE_BY_DISTANCE.compare(a, b, this)
			}
			for (step in ResultCompareStep.entries) {
				val r = step.compare(a, b, this)
				if (r != 0) {
					return r
				}
			}
			return 0
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("SearchUICore")

		private const val TIMEOUT_BETWEEN_CHARS = 700L
		private const val TIMEOUT_BEFORE_SEARCH = 50L
		private const val TIMEOUT_BEFORE_FILTER = 20L

		private var debugMode = false

		private val FILTER_DUPLICATE_POI_SUBTYPE = setOf("building", "internet_access_yes")

		@JvmStatic
		fun setDebugMode(debugMode: Boolean) {
			this.debugMode = debugMode
		}

		@JvmStatic
		fun isDebugMode(): Boolean = debugMode

		@JvmStatic
		fun updateSearchResultAddress(s: SearchResult, amenity: Amenity, dominatedCity: String?) {
			val city = s.cityName ?: ""
			val mainCity = getMainCityName(city)
			if (KAlgorithms.isEmpty(amenity.getStreetName())) {
				s.addressName = city
			} else {
				val hno = amenity.getHousenumber()
				val addr = amenity.getStreetName() + (if (KAlgorithms.isEmpty(hno)) "" else " $hno")
				s.addressName = createAddressString(s.cityName, mainCity, dominatedCity, addr)
			}
		}

		@JvmStatic
		fun formatSearchResultForTest(simpleTest: Boolean, r: SearchResult, phrase: SearchPhrase): String {
			if (simpleTest) {
				return r.toString().trim { it <= ' ' }
			}
			var dist = 0.0
			val location = r.location
			if (location != null) {
				dist = KMapUtils.getDistance(location, phrase.getLastTokenLocation()!!)
			}
			var subType = ""
			if (r.objectType == ObjectType.POI) {
				val am = r.`object` as Amenity
				val subtype = am.getSubType()
				if ("town" == subtype || "city" == subtype) {
					subType = " ($subtype)"
				}
			}
			return "$r [[${r.getFoundWordCount()}, ${r.objectType!!}$subType, ${fixed(r.getUnknownPhraseMatchWeight(), 3)}, " +
					"${fixed(dist / 1000, 2)} km]]"
		}

		/** `%.nf` of java's `Formatter`: the digits of the double as java writes it, rounded half up. */
		private fun fixed(value: Double, digits: Int): String {
			if (value.isNaN()) {
				return "NaN"
			}
			if (value.isInfinite()) {
				return if (value > 0) "Infinity" else "-Infinity"
			}
			val negative = value < 0 || (value == 0.0 && 1 / value < 0)
			val text = kotlin.math.abs(value).toString()
			val e = text.indexOfFirst { it == 'E' || it == 'e' }
			val mantissa = if (e >= 0) text.substring(0, e) else text
			val dot = mantissa.indexOf('.')
			val all = if (dot >= 0) mantissa.substring(0, dot) + mantissa.substring(dot + 1) else mantissa
			val point = (if (dot >= 0) dot else mantissa.length) + (if (e >= 0) text.substring(e + 1).toInt() else 0)
			// the digits of the integer part and the fraction that stay, at least one before the point
			val kept = IntArray(maxOf(point, 1) + digits)
			val shift = kept.size - digits - point
			for (i in all.indices) {
				val at = i + shift
				if (at in kept.indices) {
					kept[at] = all[i] - '0'
				}
			}
			val next = point + digits
			if (next in all.indices && next >= 0 && all[next] >= '5') {
				var i = kept.size - 1
				while (i >= 0) {
					if (kept[i] < 9) {
						kept[i]++
						break
					}
					kept[i] = 0
					i--
				}
				if (i < 0) {
					return (if (negative) "-1" else "1") + format(kept, digits)
				}
			}
			return (if (negative) "-" else "") + format(kept, digits)
		}

		private fun format(kept: IntArray, digits: Int): String {
			val sb = StringBuilder()
			for (i in kept.indices) {
				if (i == kept.size - digits) {
					sb.append('.')
				}
				sb.append(kept[i])
			}
			return sb.toString()
		}

		@JvmStatic
		fun getMainCityName(cityName: String): String {
			var mainCity = cityName
			if (cityName.contains(",")) {
				mainCity = mainCity.substring(0, mainCity.indexOf(",")).trim { it <= ' ' }
			}
			return mainCity
		}

		@JvmStatic
		fun getDominatedCity(cities: MutableMap<String, Int>, mainCity: String): String? {
			var freq = cities[mainCity] ?: 0
			freq++
			if (freq >= SearchResultCollection.DOMINATED_CITY_CRITERIA) {
				return mainCity
			}
			cities[mainCity] = freq
			return null
		}

		@JvmStatic
		fun createAddressString(cityName: String?, mainCity: String?, dominatedCity: String?, addr: String): String {
			return if (dominatedCity != null && dominatedCity == mainCity) {
				addr + (if (KAlgorithms.isEmpty(cityName)) "" else ", $cityName")
			} else {
				(if (KAlgorithms.isEmpty(cityName)) "" else "$cityName, ") + addr
			}
		}
	}
}
