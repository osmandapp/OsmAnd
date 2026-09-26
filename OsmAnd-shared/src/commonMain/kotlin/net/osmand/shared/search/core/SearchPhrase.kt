package net.osmand.shared.search.core

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.binary.Abbreviations
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CommonWords
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KArabicNormalizer
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KLocationParser
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTreeSet
import net.osmand.shared.util.primaryCollator
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * What was typed into the search: the words already selected as results, and the unknown words
 * after them that are still searched for. Immutable object!
 *
 * A copy of `SearchPhrase` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. Java nests `NameStringMatcher` here; the copy has it in a file of its own.
 */
class SearchPhrase private constructor(private val settings: SearchSettings?, private val clt: KCollator) {

	private var indexes: MutableList<BinaryMapIndexReader>? = null

	private var fileRequest: BinaryMapIndexReader? = null

	// Object consists of 2 part [known + unknown]
	private var fullTextSearchPhrase = ""
	private var unknownSearchPhrase = ""

	// words to be used for words span
	private var words: MutableList<SearchWord> = ArrayList()

	// Words of 2 parts
	private var firstUnknownSearchWord = ""
	private val otherUnknownWords: MutableList<String> = ArrayList()
	private var lastUnknownSearchWordComplete = false

	// Main unknown word used for search
	private var mainUnknownWordToSearch: String? = null
	private var mainUnknownSearchWordComplete = false

	// Name Searchers
	private var firstUnknownNameStringMatcher: NameStringMatcher? = null
	private var mainUnknownNameStringMatcher: NameStringMatcher? = null
	private val unknownWordsMatcher: MutableList<NameStringMatcher> = ArrayList()

	private var unselectedPoiType: AbstractPoiType? = null
	private var acceptPrivate = false
	private var cache1kmRect: KQuadRect? = null

	enum class SearchPhraseDataType {
		MAP, ADDRESS, ROUTING, POI
	}

	init {
		settings?.updateRegionPriorityProvider(this)
	}

	fun getCollator(): KCollator = clt

	fun getFileRequest(): BinaryMapIndexReader? = fileRequest

	fun generateNewPhrase(text: String, settings: SearchSettings?): SearchPhrase {
		var settings = settings
		var textToSearch = KSearchAlgorithms.canonicalizePunctuation(text)
		var leftWords: List<SearchWord> = this.words
		var thisTxt = getText(true)
		val foundWords: MutableList<SearchWord> = ArrayList()
		thisTxt = KSearchAlgorithms.canonicalizePunctuation(thisTxt)
		if (textToSearch.startsWith(thisTxt)) {
			// string is longer
			textToSearch = textToSearch.substring(getText(false).length)
			foundWords.addAll(this.words)
			leftWords = leftWords.subList(leftWords.size, leftWords.size)
		}

		for (w in leftWords) {
			if (textToSearch.startsWith(w.getWord() + DELIMITER)) {
				foundWords.add(w)
				textToSearch = textToSearch.substring(w.getWord().length + DELIMITER.length)
			} else {
				break
			}
		}
		for (w in foundWords) {
			val specialSorting = w.getResult()?.`object`
			if (w.getResult() != null && specialSorting is CustomSearchPoiFilter
				&& specialSorting.getDefaultSearchType() != null
			) {
//						settings.getSortType() == null
				settings = SearchSettings(settings)
				settings.setSortType(specialSorting.getDefaultSearchType())
			}
		}
		return createNewSearchPhrase(settings, text, foundWords, textToSearch)
	}

	// init search phrase
	private fun createNewSearchPhrase(
		settings: SearchSettings?, fullText: String, foundWords: MutableList<SearchWord>,
		textToSearch: String
	): SearchPhrase {
		val sp = SearchPhrase(settings, this.clt)
		sp.words = foundWords
		sp.fullTextSearchPhrase = fullText
		sp.unknownSearchPhrase = textToSearch

		sp.lastUnknownSearchWordComplete = isTextComplete(fullText)
		if (!reg.containsMatchIn(textToSearch)) {
			sp.firstUnknownSearchWord = sp.unknownSearchPhrase.trim { it <= ' ' }
		} else {
			sp.firstUnknownSearchWord = ""
			// java's split drops the empty parts at the end
			val ws = textToSearch.split(reg).dropLastWhile { it.isEmpty() }
			var first = true
			for (i in ws.indices) {
				val wd = ws[i].trim { it <= ' ' }
				val conjunction = Abbreviations.isConjunction(wd.lowercase())
				val lastAndIncomplete = i == ws.size - 1 && !sp.lastUnknownSearchWordComplete
				val decryptAbbreviations = needDecryptAbbreviations()
				if (wd.length > 0 && (!conjunction || lastAndIncomplete)) {
					if (first) {
						sp.firstUnknownSearchWord = if (decryptAbbreviations) Abbreviations.replace(wd) else wd
						first = false
					} else {
						sp.otherUnknownWords.add(if (decryptAbbreviations) Abbreviations.replace(wd) else wd)
					}
				}
			}
		}
		return sp
	}

	private fun needDecryptAbbreviations(): Boolean {
		val langs = settings?.getRegionLang()
		if (langs != null) {
			val langArr = langs.split(",")
			for (lang in langArr) {
				if (lang == "en") {
					return true
				}
			}
		}
		return false
	}

	internal fun selectWord(res: SearchResult, unknownWords: List<String>?, lastComplete: Boolean): SearchPhrase {
		return selectWord(res, this.settings, unknownWords, lastComplete)
	}

	internal fun selectWord(
		res: SearchResult, settings: SearchSettings?, unknownWords: List<String>?, lastComplete: Boolean
	): SearchPhrase {
		val sp = SearchPhrase(settings, this.clt)
		addResult(res, sp)
		var prnt = res.parentSearchResult
		while (prnt != null) {
			addResult(prnt, sp)
			prnt = prnt.parentSearchResult
		}
		sp.words.addAll(0, this.words)
		if (unknownWords != null) {
			sp.lastUnknownSearchWordComplete = lastComplete
			val genUnknownSearchPhrase = StringBuilder()
			for (i in unknownWords.indices) {
				if (i == 0) {
					sp.firstUnknownSearchWord = unknownWords[0]
				} else {
					sp.otherUnknownWords.add(unknownWords[i])
				}
				genUnknownSearchPhrase.append(unknownWords[i]).append(" ")
			}
			sp.fullTextSearchPhrase = fullTextSearchPhrase
			sp.unknownSearchPhrase = genUnknownSearchPhrase.toString().trim { it <= ' ' }
		}
		return sp
	}

	private fun calcMainUnknownWordToSearch() {
		if (mainUnknownWordToSearch != null) {
			return
		}
		val unknownSearchWords = otherUnknownWords
		var mainUnknownWordToSearch = firstUnknownSearchWord
		mainUnknownSearchWordComplete = lastUnknownSearchWordComplete
		if (!unknownSearchWords.isEmpty()) {
			mainUnknownSearchWordComplete = true
			val searchWords: MutableList<String> = ArrayList(unknownSearchWords)
			searchWords.add(0, getFirstUnknownSearchWord())
			searchWords.sortWith(commonWordsComparator)
			for (s in searchWords) {
				if (s.length > 0 && !KLocationParser.isValidOLC(s)) {
					mainUnknownWordToSearch = s.trim { it <= ' ' }
					if (mainUnknownWordToSearch.endsWith(".")) {
						mainUnknownWordToSearch = mainUnknownWordToSearch.substring(0, mainUnknownWordToSearch.length - 1)
						mainUnknownSearchWordComplete = false
					}
					val unknownInd = unknownSearchWords.indexOf(s)
					if (!lastUnknownSearchWordComplete && unknownSearchWords.size - 1 == unknownInd) {
						mainUnknownSearchWordComplete = false
					}
					break
				}
			}
		}
		if (KArabicNormalizer.isSpecialArabic(mainUnknownWordToSearch)) {
			val normalized = KArabicNormalizer.normalize(mainUnknownWordToSearch)
			mainUnknownWordToSearch = normalized ?: mainUnknownWordToSearch
		}
		this.mainUnknownWordToSearch = mainUnknownWordToSearch
	}

	fun getWords(): MutableList<SearchWord> = words

	fun getUnselectedPoiType(): AbstractPoiType? = unselectedPoiType

	fun setUnselectedPoiType(unselectedPoiType: AbstractPoiType?) {
		this.unselectedPoiType = unselectedPoiType
	}

	fun isMainUnknownSearchWordComplete(): Boolean {
		// return lastUnknownSearchWordComplete || otherUnknownWords.size() > 0 || unknownSearchWordPoiType != null;
		return mainUnknownSearchWordComplete
	}

	fun isLastUnknownSearchWordComplete(): Boolean = lastUnknownSearchWordComplete

	fun hasMoreThanOneUnknownSearchWord(): Boolean = otherUnknownWords.size > 0

	fun getUnknownSearchWords(): MutableList<String> = otherUnknownWords

	fun getFirstUnknownSearchWord(): String = firstUnknownSearchWord

	fun isFirstUnknownSearchWordComplete(): Boolean {
		return hasMoreThanOneUnknownSearchWord() || isLastUnknownSearchWordComplete()
	}

	fun isAcceptPrivate(): Boolean = acceptPrivate

	fun setAcceptPrivate(acceptPrivate: Boolean) {
		this.acceptPrivate = acceptPrivate
	}

	fun getFullSearchPhrase(): String = fullTextSearchPhrase

	fun getUnknownSearchPhrase(): String = unknownSearchPhrase

	fun isUnknownSearchWordPresent(): Boolean = firstUnknownSearchWord.length > 0

	fun getRadiusBBoxToSearch(radius: Int): KQuadRect? {
		val searchBBox31 = this.settings!!.getSearchBBox31()
		if (searchBBox31 != null) {
			return searchBBox31
		}

		val radiusInMeters = getRadiusSearch(radius)
		val cache1kmRect = get1km31Rect()
		if (cache1kmRect == null) {
			return null
		}
		val max = (1 shl 31) - 1
		val dx = (cache1kmRect.width() / 2) * radiusInMeters / 1000
		val dy = (cache1kmRect.height() / 2) * radiusInMeters / 1000
		val topLeftX = max(0.0, cache1kmRect.left - dx)
		val topLeftY = max(0.0, cache1kmRect.top - dy)
		val bottomRightX = min(max.toDouble(), cache1kmRect.right + dx)
		val bottomRightY = min(max.toDouble(), cache1kmRect.bottom + dy)
		return KQuadRect(topLeftX, topLeftY, bottomRightX, bottomRightY)
	}

	fun get1km31Rect(): KQuadRect? {
		val cached = cache1kmRect
		if (cached != null) {
			return cached
		}
		val l = getLastTokenLocation()
		if (l == null) {
			return null
		}
		val rect = KMapUtils.calculate31BboxUsingRhumb(1000, l)
		cache1kmRect = rect
		return rect
	}

	fun getRadiusOfflineIndexes(meters: Int, dt: SearchPhraseDataType): Iterator<BinaryMapIndexReader> {
		val rect = if (meters > 0) getRadiusBBoxToSearch(meters) else null
		return getOfflineIndexes(rect, dt)
	}

	fun getRadiusOfflineIndexes(minMeters: Int, maxMeters: Int, dataType: SearchPhraseDataType): Iterator<BinaryMapIndexReader> {
		val settings = settings!!
		val list: List<BinaryMapIndexReader>
		if (settings.hasRegionPriority()) {
			settings.updateRegionPriorityProvider(this)
			list = settings.getRegionPriorityIndexesWithMinRadius(minMeters, maxMeters)
		} else {
			list = indexes ?: settings.getOfflineIndexes()
		}
		val rect = getRadiusBBoxToSearch(maxMeters)
		return getOfflineIndexes(rect, dataType, list)
	}

	fun getOfflineIndexes(rect: KQuadRect?, dataType: SearchPhraseDataType): Iterator<BinaryMapIndexReader> {
		val settings = settings!!
		val list: Collection<BinaryMapIndexReader>
		if (settings.hasRegionPriority()) {
			settings.updateRegionPriorityProvider(this)
			list = settings.getRegionPriorityIndexes()
		} else {
			list = indexes ?: settings.getOfflineIndexes()
		}
		return getOfflineIndexes(rect, dataType, list)
	}

	fun getOfflineIndexes(): List<BinaryMapIndexReader> {
		val indexes = indexes
		if (indexes != null) {
			return indexes
		}
		return settings!!.getOfflineIndexes()
	}

	fun getSettings(): SearchSettings? = settings

	fun getRadiusLevel(): Int = settings!!.getRadiusLevel()

	fun getSearchTypes(): Array<out ObjectType>? = settings?.getSearchTypes()

	fun isCustomSearch(): Boolean = getSearchTypes() != null

	fun hasCustomSearchType(type: ObjectType): Boolean = settings!!.hasCustomSearchType(type)

	fun isSearchTypeAllowed(searchType: ObjectType): Boolean = isSearchTypeAllowed(searchType, false)

	fun isSearchTypeAllowed(searchType: ObjectType, exclusive: Boolean): Boolean {
		val searchTypes = getSearchTypes()
		if (searchTypes == null) {
			return !exclusive
		} else {
			if (exclusive && searchTypes.size > 1) {
				return false
			}
			for (type in searchTypes) {
				if (type == searchType) {
					return true
				}
			}
			return false
		}
	}

	fun isEmptyQueryAllowed(): Boolean = settings!!.isEmptyQueryAllowed()

	fun selectWord(res: SearchResult, settings: SearchSettings?): SearchPhrase {
		return selectWord(res, settings, null, false)
	}

	fun addResult(res: SearchResult, sp: SearchPhrase) {
		val sw = SearchWord(res.wordsSpan ?: res.localeName!!.trim { it <= ' ' }, res)
		sp.words.add(0, sw)
	}

	fun isLastWord(vararg p: ObjectType): Boolean {
		for (i in words.size - 1 downTo 0) {
			val sw = words[i]
			for (o in p) {
				if (sw.getType() == o) {
					return true
				}
			}
			if (sw.getType() != ObjectType.UNKNOWN_NAME_FILTER) {
				return false
			}
		}
		return false
	}

	fun getExclusiveSearchType(): ObjectType? {
		val lastWord = getLastSelectedWord()
		if (lastWord != null) {
			return ObjectType.getExclusiveSearchType(lastWord.getType())
		}
		return null
	}

	fun getMainUnknownNameStringMatcher(): NameStringMatcher {
		calcMainUnknownWordToSearch()
		var matcher = mainUnknownNameStringMatcher
		if (matcher == null) {
			matcher = getNameStringMatcher(mainUnknownWordToSearch!!, mainUnknownSearchWordComplete)
			mainUnknownNameStringMatcher = matcher
		}
		return matcher
	}

	fun getFirstUnknownNameStringMatcher(): NameStringMatcher {
		var matcher = firstUnknownNameStringMatcher
		if (matcher == null) {
			matcher = getNameStringMatcher(firstUnknownSearchWord, isFirstUnknownSearchWordComplete())
			firstUnknownNameStringMatcher = matcher
		}
		return matcher
	}

	fun getUnknownNameStringMatcher(i: Int): NameStringMatcher {
		while (unknownWordsMatcher.size <= i) {
			val ind = unknownWordsMatcher.size
			val completeMatch = ind < otherUnknownWords.size - 1 || isLastUnknownSearchWordComplete()
			unknownWordsMatcher.add(getNameStringMatcher(otherUnknownWords[ind], completeMatch))
		}
		return unknownWordsMatcher[i]
	}

	private fun getNameStringMatcher(word: String, complete: Boolean): NameStringMatcher {
		return NameStringMatcher(
			word,
			(if (complete)
				KStringMatcherMode.CHECK_EQUALS_FROM_SPACE
			else
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE)
		)
	}

	fun hasObjectType(p: ObjectType): Boolean {
		for (s in words) {
			if (s.getType() == p) {
				return true
			}
		}
		return false
	}

	fun syncWordsWithResults() {
		for (w in words) {
			w.syncWordWithResult()
		}
	}

	fun getText(includeUnknownPart: Boolean): String {
		val sb = StringBuilder()
		for (s in words) {
			sb.append(s.getWord()).append(DELIMITER)
		}
		if (includeUnknownPart) {
			sb.append(unknownSearchPhrase)
		}
		return sb.toString()
	}

	fun getTextWithoutLastWord(): String {
		val sb = StringBuilder()
		val words: MutableList<SearchWord> = ArrayList(this.words)
		if (KAlgorithms.isEmpty(unknownSearchPhrase.trim { it <= ' ' }) && words.size > 0) {
			words.removeAt(words.size - 1)
		}
		for (s in words) {
			sb.append(s.getWord()).append(DELIMITER)
		}
		return sb.toString()
	}

	fun getStringRerpresentation(): String {
		val sb = StringBuilder()
		for (s in words) {
			sb.append(s.getWord()).append(" [" + s.getType() + "], ")
		}
		sb.append(unknownSearchPhrase)
		return sb.toString()
	}

	override fun toString(): String = getStringRerpresentation()

	fun isNoSelectedType(): Boolean = words.isEmpty()

	fun isEmpty(): Boolean = words.isEmpty() && unknownSearchPhrase.isEmpty()

	fun getLastSelectedWord(): SearchWord? {
		if (words.isEmpty()) {
			return null
		}
		return words[words.size - 1]
	}

	fun getWordLocation(): KLatLon? {
		for (i in words.size - 1 downTo 0) {
			val sw = words[i]
			if (sw.getLocation() != null) {
				return sw.getLocation()
			}
		}
		return null
	}

	fun getLastTokenLocation(): KLatLon? {
		for (i in words.size - 1 downTo 0) {
			val sw = words[i]
			if (sw.getLocation() != null) {
				return sw.getLocation()
			}
		}
		// last token or myLocationOrVisibleMap if not selected
		if (settings != null) {
			return settings.getOriginalLocation()
		}
		return null
	}

	fun selectFile(obj: BinaryMapIndexReader) {
		var indexes = indexes
		if (indexes == null) {
			indexes = ArrayList()
			this.indexes = indexes
		}
		if (!indexes.contains(obj)) {
			indexes.add(obj)
		}
	}

	fun sortFiles() {
		var indexes = indexes
		if (indexes == null) {
			indexes = ArrayList(getOfflineIndexes())
			this.indexes = indexes
		}
		val diffsByRegion = getDiffsByRegion(indexes)
		val ll = getLastTokenLocation()
		if (ll != null) {
			indexes.sortWith(object : Comparator<BinaryMapIndexReader> {
				val locations = HashMap<BinaryMapIndexReader, KLatLon?>()

				override fun compare(o1: BinaryMapIndexReader, o2: BinaryMapIndexReader): Int {
					val rc1 = getLocation(o1)
					val rc2 = getLocation(o2)
					val d1 = if (rc1 == null) 10000000.0 else KMapUtils.getDistance(rc1, ll)
					val d2 = if (rc2 == null) 10000000.0 else KMapUtils.getDistance(rc2, ll)
					return d1.compareTo(d2)
				}

				private fun getLocation(o1: BinaryMapIndexReader): KLatLon? {
					if (locations.containsKey(o1)) {
						return locations[o1]
					}
					val rc1: KLatLon?
					if (o1.containsMapData()) {
						rc1 = o1.getMapIndexes()[0].getCenterLatLon()
					} else {
						rc1 = o1.getRegionCenter()
					}
					locations[o1] = rc1
					return rc1
				}
			})
			if (!diffsByRegion.isEmpty()) {
				val finalSort: MutableList<BinaryMapIndexReader> = ArrayList()
				for (i in indexes.indices) {
					val currFile = indexes[i]
					val diffs = diffsByRegion[currFile.getRegionName()]
					if (diffs != null) {
						finalSort.addAll(diffs)
						finalSort.add(currFile)
					} else {
						finalSort.add(currFile)
					}
				}
				indexes.clear()
				indexes.addAll(finalSort)
			}
		}
	}

	private fun getDiffsByRegion(indexes: MutableList<BinaryMapIndexReader>): Map<String, MutableList<BinaryMapIndexReader>> {
		val result = HashMap<String, MutableList<BinaryMapIndexReader>>()
		val it = indexes.iterator()
		while (it.hasNext()) {
			val r = it.next()
			val filename = r.getFile().name()
			if (DIFF_FILE_NAME.matches(filename)) {
				val currRegionName = r.getRegionName()
				val list = result[currRegionName]
				if (list != null) {
					list.add(r)
				} else {
					result[currRegionName] = arrayListOf(r)
				}
				it.remove()
			}
		}
		return result
	}

	fun countUnknownWordsMatchMainResult(sr: SearchResult): Int {
		return countUnknownWordsMatchInternal(sr, null, 0)
	}

	fun countUnknownWordsMatchMainResult(sr: SearchResult, amountMatchingWords: Int): Int {
		return countUnknownWordsMatchInternal(sr, null, amountMatchingWords)
	}

	fun countUnknownWordsMatchMainResult(sr: SearchResult, name: String?, amountMatchingWords: Int): Int {
		return countUnknownWordsMatchInternal(sr, name, amountMatchingWords)
	}

	private fun countUnknownWordsMatchInternal(sr: SearchResult, extraName: String?, amountMatchingWords: Int): Int {
		var r = 0
		if (otherUnknownWords.size > 0) {
			for (i in otherUnknownWords.indices) {
				var match = false
				if (i < amountMatchingWords - 1) {
					match = true
				} else {
					val ms = getUnknownNameStringMatcher(i)
					if (ms.matchesName(sr.localeName) || ms.matches(sr.otherNames)
						|| ms.matchesName(sr.alternateName) || ms.matchesName(extraName)
					) {
						match = true
					}
				}
				if (match) {
					var otherWordsMatch = sr.otherWordsMatch
					if (otherWordsMatch == null) {
						otherWordsMatch = KTreeSet(Comparator { o1, o2 -> getCollator().compare(o1, o2) })
						sr.otherWordsMatch = otherWordsMatch
					}
					otherWordsMatch.add(otherUnknownWords[i])
					r++
				}
			}
		}
		if (amountMatchingWords > 0) {
			sr.firstUnknownWordMatches = true
			r++
		} else {
			val match =
				getFirstUnknownNameStringMatcher().matchesName(sr.localeName)
						|| getFirstUnknownNameStringMatcher().matches(sr.otherNames)
						|| getFirstUnknownNameStringMatcher().matchesName(sr.alternateName)
						|| getFirstUnknownNameStringMatcher().matchesName(extraName)
			if (match) {
				r++
			}
			sr.firstUnknownWordMatches = match || sr.firstUnknownWordMatches
		}
		return r
	}

	// java's matches takes a null name
	private fun NameStringMatcher.matchesName(name: String?): Boolean = name != null && matches(name)

	fun getLastUnknownSearchWord(): String {
		if (otherUnknownWords.size > 0) {
			return otherUnknownWords[otherUnknownWords.size - 1]
		}
		return firstUnknownSearchWord
	}

	fun getRadiusSearch(meters: Int, radiusLevel: Int): Int {
		var res = meters
		for (k in 0 until radiusLevel) {
			res = res * (if (k % 2 == 0) 2 else 3)
		}
		return res
	}

	fun getRadiusSearch(meters: Int): Int = getRadiusSearch(meters, getRadiusLevel() - 1)

	fun getNextRadiusSearch(meters: Int): Int = getRadiusSearch(meters, getRadiusLevel())

	private fun getUnknownWordToSearchBuildingInd(): Int {
		if (otherUnknownWords.size > 0 && KAlgorithms.extractFirstIntegerNumber(getFirstUnknownSearchWord()) == 0) {
			var ind = 0
			for (wrd in otherUnknownWords) {
				ind++
				if (KAlgorithms.extractFirstIntegerNumber(wrd) != 0) {
					return ind
				}
			}
		}
		return 0
	}

	fun getUnknownWordToSearchBuildingNameMatcher(): NameStringMatcher {
		val ind = getUnknownWordToSearchBuildingInd()
		if (ind > 0) {
			return getUnknownNameStringMatcher(ind - 1)
		} else {
			return getFirstUnknownNameStringMatcher()
		}
	}

	fun getUnknownWordToSearchBuilding(): String {
		val ind = getUnknownWordToSearchBuildingInd()
		if (ind > 0) {
			return otherUnknownWords[ind - 1]
		} else {
			return firstUnknownSearchWord
		}
	}

	fun getUnknownWordToSearch(): String {
		calcMainUnknownWordToSearch()
		return mainUnknownWordToSearch!!
	}

	private fun isTextComplete(fullText: String): Boolean {
		var lastUnknownSearchWordComplete = false
		if (fullText.length > 0) {
			val ch = fullText[fullText.length - 1]
			lastUnknownSearchWordComplete = ch == ' ' || ch == ',' || ch == '\r' || ch == '\n'
					|| ch == ';'
		}
		return lastUnknownSearchWordComplete
	}

	fun getRegionPriority(reader: BinaryMapIndexReader?): Int {
		if (settings != null) {
			return settings.getRegionPriority(reader)
		}
		return 0
	}

	companion object {
		const val DELIMITER = " "
		const val ALLDELIMITERS = "\\s|,"
		const val ALLDELIMITERS_WITH_HYPHEN = "\\s|,|-"
		private val reg = Regex(ALLDELIMITERS)
		private val DIFF_FILE_NAME = Regex("([a-zA-Z-]+_)+([0-9]+_){2}[0-9]+\\.obf")

		private val commonWordsComparator: Comparator<String> = object : Comparator<String> {
			val instance = CommonWords.getInstance()

			override fun compare(o1: String, o2: String): Int {
				val i1 = instance.getCommonSearch(o1.lowercase())
				val i2 = instance.getCommonSearch(o2.lowercase())
				if (i1 != i2) {
					if (i1 == -1) {
						return -1
					} else if (i2 == -1) {
						return 1
					}
					return -icompare(i1, i2)
				}
				// compare length without numbers to not include house numbers
				return -icompare(lengthWithoutNumbers(o1), lengthWithoutNumbers(o2))
			}
		}

		@JvmStatic
		fun emptyPhrase(): SearchPhrase = emptyPhrase(null)

		@JvmStatic
		fun emptyPhrase(settings: SearchSettings?): SearchPhrase = emptyPhrase(settings, primaryCollator())

		@JvmStatic
		fun emptyPhrase(settings: SearchSettings?, clt: KCollator): SearchPhrase = SearchPhrase(settings, clt)

		@JvmStatic
		fun splitWords(w: String?, ws: MutableList<String>, delimiters: String): MutableList<String> {
			if (!KAlgorithms.isEmpty(w)) {
				val wrs = w!!.split(Regex(delimiters))
				for (wr in wrs) {
					val wd = wr.trim { it <= ' ' }
					if (wd.length > 0) {
						ws.add(wd)
					}
				}
			}
			return ws
		}

		@JvmStatic
		fun countWords(w: String?): Int {
			var cnt = 0
			if (!KAlgorithms.isEmpty(w)) {
				val ws = w!!.split(reg)
				for (i in ws.indices) {
					val wd = ws[i].trim { it <= ' ' }
					if (wd.length > 0) {
						cnt++
					}
				}
			}
			return cnt
		}

		@JvmStatic
		fun selectMainUnknownWordToSearch(searchWords: MutableList<String>): String {
			searchWords.sortWith(commonWordsComparator)
			for (s in searchWords) {
				val t = s.trim { it <= ' ' }
				if (t.length > 0) {
					return t
				}
			}
			return ""
		}

		@JvmStatic
		fun getOfflineIndexes(
			rect: KQuadRect?, dataType: SearchPhraseDataType, list: Collection<BinaryMapIndexReader>
		): Iterator<BinaryMapIndexReader> {
			val iterator = list.iterator()
			return object : Iterator<BinaryMapIndexReader> {
				var next: BinaryMapIndexReader? = null

				override fun hasNext(): Boolean {
					while (iterator.hasNext()) {
						val next = iterator.next()
						this.next = next
						if (rect != null) {
							if (dataType == SearchPhraseDataType.POI) {
								if (next.containsPoiData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())) {
									return true
								}
							} else if (dataType == SearchPhraseDataType.ADDRESS) {
								// containsAddressData not all maps supported
								if (next.containsPoiData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()) &&
									next.containsAddressData()
								) {
									return true
								}
							} else if (dataType == SearchPhraseDataType.ROUTING) {
								if (next.containsRouteData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())) {
									return true
								}
							} else {
								if (next.containsMapData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt(), 15)) {
									return true
								}
							}
						} else {
							return true
						}
					}
					return false
				}

				override fun next(): BinaryMapIndexReader = next!!
			}
		}

		@JvmStatic
		fun icompare(x: Int, y: Int): Int {
			return if (x < y) -1 else (if (x == y) 0 else 1)
		}

		private fun lengthWithoutNumbers(s: String): Int {
			var len = 0
			for (k in 0 until s.length) {
				if (s[k] >= '0' && s[k] <= '9') {

				} else {
					len++
				}
			}
			return len
		}

		@JvmStatic
		fun stripBraces(names: Collection<String>): Collection<String> {
			val lst: MutableList<String> = ArrayList()
			for (s in names) {
				lst.add(stripBraces(s)!!)
			}
			return lst
		}

		@JvmStatic
		fun stripBraces(localeName: String?): String? {
			if (localeName == null) {
				return null
			}
			val i = localeName.indexOf('(')
			var retName = localeName
			if (i > -1) {
				retName = localeName.substring(0, i)
				val j = localeName.indexOf(')', i)
				if (j > -1) {
					retName = (retName.trim { it <= ' ' } + ' ' + localeName.substring(j + 1)).trim { it <= ' ' }
				}
			}
			return retName
		}
	}
}
