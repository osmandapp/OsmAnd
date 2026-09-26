package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicInt
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.City
import net.osmand.shared.data.MapObject
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreAPI
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.matchesName
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlin.jvm.JvmStatic

/**
 * The search as the ui runs it.
 *
 * A copy of `SearchUICore` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. So far only [SearchResultMatcher], which the search apis publish their results to, is
 * here, without the statistics of time and memory java keeps for a search.
 */
class SearchUICore private constructor() {

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

		fun getExportedObjects(): List<MapObject>? = exportedObjects

		fun getExportedCities(): List<City>? = exportedCities

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

	companion object {
		private val LOG = LoggerFactory.getLogger("SearchUICore")

		private var debugMode = false

		@JvmStatic
		fun setDebugMode(debugMode: Boolean) {
			this.debugMode = debugMode
		}

		@JvmStatic
		fun isDebugMode(): Boolean = debugMode
	}
}
