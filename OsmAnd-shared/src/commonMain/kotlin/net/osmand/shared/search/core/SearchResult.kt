package net.osmand.shared.search.core

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.Street
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiFilter
import net.osmand.shared.osm.PoiType
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_DEFAULT_ZOOM
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.jvm.JvmField
import kotlin.math.max

/**
 * A result of the search, and the weight by which it is sorted.
 *
 * A copy of `SearchResult` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The fields of the spatial search, `spatialResult` and `spatialSearchVisibleLevel`, come
 * with the spatial search.
 */
class SearchResult {

	// search phrase that makes search result valid
	@JvmField
	var requiredSearchPhrase: SearchPhrase

	// internal package fields (used for sorting)
	@JvmField
	var parentSearchResult: SearchResult? = null
	internal var wordsSpan: String? = null
	internal var firstUnknownWordMatches = false
	internal var otherWordsMatch: MutableCollection<String>? = null

	@JvmField
	var `object`: Any? = null
	@JvmField
	var objectType: ObjectType? = null
	@JvmField
	var file: BinaryMapIndexReader? = null

	@JvmField
	var priority = 0.0
	@JvmField
	var priorityDistance = 0.0

	@JvmField
	var location: KLatLon? = null
	@JvmField
	var preferredZoom = PREFERRED_DEFAULT_ZOOM

	@JvmField
	var localeName: String? = null
	@JvmField
	var alternateName: String? = null
	@JvmField
	var addressName: String? = null
	@JvmField
	var cityName: String? = null
	@JvmField
	var otherNames: Collection<String>? = null

	@JvmField
	var localeRelatedObjectName: String? = null
	@JvmField
	var relatedObject: Any? = null
	@JvmField
	var distRelatedObjectName = 0.0

	private var impreciseCoordinates = false
	private var unknownPhraseMatchWeight = 0.0
	private var completeMatchRes: CheckWordsMatchCount? = null

	enum class SearchResultResource(private val weight: Int) {
		DETAILED(3),
		BASEMAP(3),
		WIKIPEDIA(2),
		TRAVEL(1);

		fun getWeight(): Int = weight
	}

	private var searchResultResource: SearchResultResource? = null

	constructor() {
		this.requiredSearchPhrase = SearchPhrase.emptyPhrase()
	}

	constructor(sp: SearchPhrase) {
		this.requiredSearchPhrase = sp
	}

	fun hasImpreciseCoordinates(): Boolean = impreciseCoordinates

	fun setFirstUnknownWordMatches(firstUnknownWordMatches: Boolean) {
		this.firstUnknownWordMatches = firstUnknownWordMatches
	}

	fun setImpreciseCoordinates(imprecise: Boolean) {
		this.impreciseCoordinates = imprecise
	}

	// maximum corresponds to the top entry
	fun getUnknownPhraseMatchWeight(): Double {
		if (unknownPhraseMatchWeight != 0.0) {
			return unknownPhraseMatchWeight
		}
		unknownPhraseMatchWeight = getSumPhraseMatchWeight(null)
		return unknownPhraseMatchWeight
	}

	fun getCompleteMatchRes(): CheckWordsMatchCount? {
		if (completeMatchRes != null) {
			return completeMatchRes
		}
		getSumPhraseMatchWeight(null)
		return completeMatchRes
	}

	private fun getSumPhraseMatchWeight(exactResult: SearchResult?): Double {
		var res = 1.0
		val completeMatchRes = CheckWordsMatchCount()
		this.completeMatchRes = completeMatchRes
		if (requiredSearchPhrase.getUnselectedPoiType() != null) {
			// search phrase matches poi type, then we lower all POI matches and don't check allWordsMatched
		} else if (objectType == ObjectType.POI_TYPE) {
			// don't overload with poi types
		} else if (isPublicTransport()) {
			res -= 0.1
		} else {
			var matched = localeName != null && allWordsMatched(localeName!!, exactResult, completeMatchRes)
			// incorrect fix
//			if (!matched && object instanceof Street s) { // parentSearchResult == null &&
//				matched = allWordsMatched(localeName + " " + s.getCity().getName(requiredSearchPhrase.getSettings().getLang()), exactResult, completeMatchRes);
//			}
			if (!matched && alternateName != null && cityName != alternateName) {
				matched = allWordsMatched(alternateName!!, exactResult, completeMatchRes)
			}
			val otherNames = otherNames
			if (!matched && otherNames != null) {
				for (otherName in otherNames) {
					if (allWordsMatched(otherName, exactResult, completeMatchRes)) {
						matched = true
						break
					}
				}
			}
			var selectedCity: City? = null
			val exactObject = exactResult?.`object`
			val exactParentObject = exactResult?.parentSearchResult?.`object`
			if (exactResult != null && exactObject is Street) {
				selectedCity = exactObject.getCity()
			} else if (exactResult != null && exactResult.parentSearchResult != null && exactParentObject is Street) {
				selectedCity = exactParentObject.getCity()
			}
			val c = `object`
			if (matched && selectedCity != null && c is City) {
				// city don't match because of boundary search -> lower priority
				if (selectedCity.getName() != c.getName()) {
					matched = false
					// for unmatched cities calculate how close street is to boundary
					// 1 - very close, 0 - very far
					val bbox31 = selectedCity.getBbox31()
					var latlon = selectedCity.getLocation()
					if (bbox31 != null) {
						// even center is shifted probably best to do combination of bbox & center
						val lon = KMapUtils.get31LongitudeX(bbox31[0] / 2 + bbox31[2] / 2)
						val lat = KMapUtils.get31LatitudeY(bbox31[1] / 2 + bbox31[3] / 2)
						latlon = KLatLon(lat, lon)
					}
					res += 100 / max(100.0, KMapUtils.getDistance(location!!, latlon!!))
				}
			}
			// if all words from search phrase match (<) the search result words - we prioritize it higher
			if (matched) {
				res = getPhraseWeightForCompleteMatch(completeMatchRes, exactResult)
			}
			val a = `object`
			if (a is Amenity) {
				val elo = a.getTravelEloNumber()
				if (elo > MIN_ELO_RATING) {
					val rat = (elo.toDouble() - MIN_ELO_RATING) / (MAX_ELO_RATING - MIN_ELO_RATING)
					res += rat * MAX_PHRASE_WEIGHT_TOTAL * 2 / 3
				}
			}
		}
		val parentSearchResult = parentSearchResult
		if (parentSearchResult != null) {
			// parent search result should not change weight of current result, so we divide by MAX_TYPES_BASE_10^2
			res = res + parentSearchResult.getSumPhraseMatchWeight(exactResult ?: this) / (MAX_PHRASE_WEIGHT_TOTAL)
		}
		return res
	}

	private fun getPhraseWeightForCompleteMatch(completeMatchRes: CheckWordsMatchCount, exactResult: SearchResult?): Double {
		var res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10 // range 10 - 40
		var closeDistance = false
		val searchLocation = requiredSearchPhrase.getSettings()!!.getOriginalLocation()
		val location = this.location
		if (searchLocation != null && location != null) {
			val dist = KMapUtils.getDistance(searchLocation, location)
			if (dist <= NEAREST_METERS_LIMIT) {
				// will sort in groups by object type each ~2 km
				val coef = (((NEAREST_METERS_LIMIT - dist) / NEAREST_METERS_LIMIT) * 15).toInt()
				res = ObjectType.getTypeWeight(objectType) + MAX_TYPES_BASE_10 * 4 + coef
				closeDistance = true
				// range 41 - 59
			}
		}
		if (completeMatchRes.allWordsEqual) {
			// if all words from search phrase == the search result words - we prioritize it even higher
			if (objectType != ObjectType.POI || closeDistance) {
				res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10 + MAX_PHRASE_WEIGHT_TOTAL / 2
			}
			if (closeDistance) {
				res += 1
			}
			if (objectType == ObjectType.CITY && exactResult == null) {
				res += MAX_PHRASE_WEIGHT_TOTAL / 2
			}
			// range 60 - 91
		}
		if (res < MAX_TYPES_BASE_10 * 4) {
			// equalize unmatched results
			res = MAX_TYPES_BASE_10
			if (getResourceType() == SearchResultResource.BASEMAP) {
				res += 1
			}
			val am = `object`
			if (am != null && am is Amenity && am.isRouteArticle()) {
				res += 0.5
			}
			if (objectType == ObjectType.STREET_INTERSECTION) {
				res -= 1
			}
		}
		return res
	}

	fun getDepth(): Int {
		val parentSearchResult = parentSearchResult
		if (parentSearchResult != null) {
			return 1 + parentSearchResult.getDepth()
		}
		return 1
	}

	fun getFoundWordCount(): Int {
		var inc = getSelfWordCount()
		val parentSearchResult = parentSearchResult
		if (parentSearchResult != null) {
			inc += parentSearchResult.getFoundWordCount()
		}
		return inc
	}

	private fun allWordsMatched(name: String, exactResult: SearchResult?, cnt: CheckWordsMatchCount): Boolean {
		val searchPhraseNames = getSearchPhraseNames()
		var name = KSearchAlgorithms.alignChars(name)
		val localResultNames: MutableList<String>
		if (!KAlgorithms.isEmpty(name) && name.indexOf('(') != -1) {
			name = SearchPhrase.stripBraces(name)!!
		}
		if (!requiredSearchPhrase.getFullSearchPhrase().contains(HYPHEN)) {
			// we split '-' words in result, so user can input same without '-'
			localResultNames = SearchPhrase.splitWords(name, ArrayList(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN)
		} else {
			localResultNames = SearchPhrase.splitWords(name, ArrayList(), SearchPhrase.ALLDELIMITERS)
		}

		var wordMatched: Boolean
		if (searchPhraseNames.isEmpty()) {
			return false
		}
		var exact = exactResult
		while (exact != null && exact !== this) {
			val lst = exact.getSearchPhraseNames()
			for (l in lst) {
				val i = searchPhraseNames.indexOf(l)
				if (i != -1) {
					searchPhraseNames.removeAt(i)
				}
			}
			exact = exact.parentSearchResult
		}

		var idxMatchedWord = -1
		for (searchPhraseName in searchPhraseNames) {
			wordMatched = false
			for (i in idxMatchedWord + 1 until localResultNames.size) {
				val r = requiredSearchPhrase.getCollator().compare(searchPhraseName, localResultNames[i])
				if (r == 0) {
					wordMatched = true
					idxMatchedWord = i
					break
				}
			}
			if (!wordMatched) {
//				cnt.allWordsInPhraseAreInResult = false;
				return false
			}
		}
		if (searchPhraseNames.size == localResultNames.size) {
			cnt.allWordsEqual = true
		}
		cnt.allWordsInPhraseAreInResult = true
		return true
	}

	class CheckWordsMatchCount {
		@JvmField
		var allWordsEqual = false
		@JvmField
		var allWordsInPhraseAreInResult = false
	}

	private fun getSearchPhraseNames(): MutableList<String> {
		val searchPhraseNames = ArrayList<String>()

		val fw = requiredSearchPhrase.getFirstUnknownSearchWord()
		val ow = requiredSearchPhrase.getUnknownSearchWords()
		if (fw.length > 0) {
			searchPhraseNames.add(KSearchAlgorithms.alignChars(fw))
		}
		for (o in ow) {
			searchPhraseNames.add(KSearchAlgorithms.alignChars(o))
		}
		// when parent result was recreated with same phrase (it doesn't have preselected word)
		// SearchCoreFactory.subSearchApiOrPublish
		val parentSearchResult = parentSearchResult
		val parentOtherWordsMatch = parentSearchResult?.getOtherWordsMatch()
		if (parentSearchResult != null && requiredSearchPhrase === parentSearchResult.requiredSearchPhrase
			&& parentOtherWordsMatch != null
		) {
			for (s in parentOtherWordsMatch) {
				val i = searchPhraseNames.indexOf(KSearchAlgorithms.alignChars(s))
				if (i != -1) {
					searchPhraseNames.removeAt(i)
				}
			}
		}

		return searchPhraseNames
	}

	private fun getSelfWordCount(): Int {
		var inc = 0
		if (firstUnknownWordMatches) {
			inc = 1
		}
		val otherWordsMatch = otherWordsMatch
		if (otherWordsMatch != null) {
			inc += otherWordsMatch.size
		}
		return inc
	}

	fun getSearchDistance(location: KLatLon?): Double {
		var distance = 0.0
		val thisLocation = this.location
		if (location != null && thisLocation != null) {
			distance = KMapUtils.getDistance(location, thisLocation)
		}
		return priority - 1 / (1 + priorityDistance * distance)
	}

	fun getSearchDistance(location: KLatLon?, pd: Double): Double {
		var distance = 0.0
		val thisLocation = this.location
		if (location != null && thisLocation != null) {
			distance = KMapUtils.getDistance(location, thisLocation)
		}
		return priority - 1 / (1 + pd * distance)
	}

	override fun toString(): String {
		val b = StringBuilder()
		if (!KAlgorithms.isEmpty(localeName)) {
			b.append(localeName)
		}
		val relatedObject = relatedObject
		val obj = `object`
		if (!KAlgorithms.isEmpty(localeRelatedObjectName)) {
			if (b.length > 0) {
				b.append(", ")
			}
			b.append(localeRelatedObjectName)
			if (relatedObject is Street) {
				val city = relatedObject.getCity()
				if (city != null) {
					val settings = requiredSearchPhrase.getSettings()!!
					b.append(", ").append(city.getName(settings.getLang(), settings.isTransliterate()))
				}
			}
		} else if (obj is AbstractPoiType) {
			if (b.length > 0) {
				b.append(" ")
			}
			val poiType: AbstractPoiType = obj
			if (poiType is PoiCategory) {
				b.append("(Category)")
			} else if (poiType is PoiFilter) {
				b.append("(Filter)")
			} else if (poiType is PoiType) {
				val p: PoiType = poiType
				val parentType = p.getParentType()
				if (parentType != null) {
					val translation = parentType.getTranslation()
					b.append("(").append(translation)
					if (parentType is PoiCategory) {
						b.append(" / Category)")
					} else if (parentType is PoiFilter) {
						b.append(" / Filter)")
					} else if (parentType is PoiType) {
						val pp: PoiType = poiType
						val filter = pp.getFilter()
						val category = pp.getCategory()
						if (filter != null && filter.getTranslation() != translation) {
							b.append(" / ").append(filter.getTranslation()).append(")")
						} else if (category != null && category.getTranslation() != translation) {
							b.append(" / ").append(category.getTranslation()).append(")")
						} else {
							b.append(")")
						}
					}
				} else if (p.getFilter() != null) {
					b.append("(").append(p.getFilter()!!.getTranslation()).append(")")
				} else if (p.getCategory() != null) {
					b.append("(").append(p.getCategory()!!.getTranslation()).append(")")
				}
			}
		}
		return b.toString()
	}

	fun getResourceType(): SearchResultResource {
		var searchResultResource = searchResultResource
		if (searchResultResource == null) {
			searchResultResource = SearchResultResource.DETAILED
			val amenity = `object`
			if (amenity != null && amenity is Amenity) {
				searchResultResource = if (amenity.getType()!!.isWiki()) SearchResultResource.WIKIPEDIA else searchResultResource
			}
			val file = file
			if (file != null) {
				searchResultResource = if (file.getFile().name().contains(".travel")) SearchResultResource.TRAVEL else searchResultResource
				searchResultResource = if (file.isBasemap()) SearchResultResource.BASEMAP else searchResultResource
			}
			this.searchResultResource = searchResultResource
		}
		return searchResultResource
	}

	fun getOtherWordsMatch(): MutableCollection<String>? = otherWordsMatch

	fun setOtherWordsMatch(set: MutableCollection<String>?) {
		otherWordsMatch = set
	}

	fun setUnknownPhraseMatchWeight(weight: Double) {
		unknownPhraseMatchWeight = weight
	}

	fun isFullPhraseEqualLocaleName(): Boolean {
		return requiredSearchPhrase.getFullSearchPhrase().equals(localeName, ignoreCase = true)
	}

	fun filterUnknownSearchWord(leftUnknownSearchWords: MutableList<String>?): MutableList<String> {
		var left = leftUnknownSearchWords
		if (left == null) {
			left = ArrayList(requiredSearchPhrase.getUnknownSearchWords())
			left.add(0, requiredSearchPhrase.getFirstUnknownSearchWord())
		}
		if (firstUnknownWordMatches) {
			left.remove(requiredSearchPhrase.getFirstUnknownSearchWord())
		}
		val otherWordsMatch = otherWordsMatch
		if (otherWordsMatch != null) {
//			removeAll(res.otherWordsMatch); // incorrect
			for (otherWord in otherWordsMatch) {
				val ind = if (firstUnknownWordMatches) left.indexOf(otherWord) else left.lastIndexOf(otherWord)
				if (ind != -1) {
					left.removeAt(ind) // remove 1 by 1
				}
			}
		}

		return left
	}

	fun restoreBraceNames(backup: Array<String?>?) {
		if (backup != null) {
			if (backup[0] != null) {
				localeName = backup[0]
			}
			if (backup[1] != null) {
				localeName = backup[1]
			}
			if (backup.size > 2) {
				val oth = ArrayList<String>()
				for (i in 2 until backup.size) {
					oth.add(backup[i]!!)
				}
				otherNames = oth
			}
		}
	}

	fun stripBracesNames(): Array<String?>? {
		val brace = charArrayOf('(')
		var noBrace = true
		noBrace = noBrace and !KAlgorithms.containsChar(localeName, brace)
		noBrace = noBrace and !KAlgorithms.containsChar(alternateName, brace)
		val otherNames = otherNames
		if (otherNames != null) {
			for (o in otherNames) {
				noBrace = noBrace and !KAlgorithms.containsChar(o, brace)
				if (!noBrace) {
					break
				}
			}
		}
		if (noBrace) {
			return null
		}

		val backup = arrayOfNulls<String>(2 + (otherNames?.size ?: 0))
		if (localeName != null) {
			backup[0] = localeName
			localeName = SearchPhrase.stripBraces(localeName)
		}
		if (alternateName != null) {
			backup[1] = alternateName
			alternateName = SearchPhrase.stripBraces(alternateName)
		}
		if (otherNames != null) {
			val it = otherNames.iterator()
			val oth = ArrayList<String>()
			for (i in 0 until otherNames.size) {
				val o = SearchPhrase.stripBraces(it.next())!!
				backup[2 + i] = o
				oth.add(o)
			}
			this.otherNames = oth
		}
		return backup
	}

	private fun isPublicTransport(): Boolean {
		if (objectType != ObjectType.POI) {
			return false
		}
		val am = `object` as Amenity
		val transportTypes = MapPoiTypes.getDefault().getPublicTransportTypes()!!
		return transportTypes.contains(am.getSubType())
	}

	companion object {
		const val DELIMITER = " "
		private const val HYPHEN = "-"
		internal const val NEAREST_METERS_LIMIT = 30000

		// MAX_TYPES_BASE_10 should be > ObjectType.getTypeWeight(objectType) = 5
		const val MAX_TYPES_BASE_10 = 10.0
		// MAX_PHRASE_WEIGHT_TOTAL should be  > getSumPhraseMatchWeight
		const val MAX_PHRASE_WEIGHT_TOTAL = MAX_TYPES_BASE_10 * MAX_TYPES_BASE_10

		private const val MIN_ELO_RATING = 1800
		private const val MAX_ELO_RATING = 4300
	}
}
