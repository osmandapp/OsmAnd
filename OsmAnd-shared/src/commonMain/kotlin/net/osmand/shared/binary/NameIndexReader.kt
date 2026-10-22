package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTLongArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField

/**
 * Reads the name index of the address or the poi section of an obf file the way the spatial search
 * does: the keys of its string table that match a query, the blocks of atoms behind them, which it
 * keeps for the next query, and the common words of the index.
 *
 * A copy of `NameIndexReader` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Left out is what serves only the obf inspection tools of OsmAnd-tools: the frequencies
 * of the suffixes of a prefix, the statistics of suffixes and boundaries, the merge methods of
 * [ValueFreq] and the prefix tree of the common words. The blocks are the messages of
 * `NameIndexData.kt`.
 */
class NameIndexReader private constructor(
	// read params
	val poiRegion: PoiRegion?,
	val addressRegion: AddressRegion?
) {

	// cache for queries
	private val matchedKeys = HashMap<String, KTLongHashSet>()
	// cache for prefixes
	private val indexByRef = KTLongObjectMap<PrefixNameValue>()
	private var cachedBytes = 0L
	private var tablePointer = 0L
	private var cacheRawBlocks = false

	// common words
	private var commonStats: CommonIndexedStats? = null
	private val commonsList = ArrayList<String>()
	private var commonStatsValues: MutableMap<String, ValueFreq>? = null

	// stats
	private var bytesStat = NameIndexReaderBytes()

	internal var query: NameIndexReaderQuery? = null // read all

	constructor(p: AddressRegion) : this(null, p)

	constructor(p: PoiRegion) : this(p, null)

	class NameIndexReaderBytes {
		var readTableBytes = 0L
		var skipTableBytes = 0L
		var readAtomBytes = 0L
		var skipAtomBytes = 0L
	}

	open class NameIndexReaderMatcher(query: String) {

		protected val queryAligned: String = KSearchAlgorithms.alignChars(query)
		protected val queryIncomplete: String? =
			if (query.endsWith(KCollatorStringMatcher.INCOMPLETE_DOT)) query.substring(0, query.length - 1) else null

		open fun matchKey(key: String): Boolean {
			val alignedKey = KSearchAlgorithms.alignChars(key)
			return matchAlignedKey(alignedKey)
		}

		protected open fun matchAlignedKey(alignedKey: String): Boolean {
			// 1. simple match
			var match = KCollatorStringMatcher.cmatches(queryAligned, alignedKey, KStringMatcherMode.CHECK_ONLY_STARTS_WITH)
			// 2. match 2-xx (key) -> 2 (query) - another solution number, NC-42 (key) -> NC (query) or 'NC 42' 2 tokens
			if (!match && alignedKey.indexOf('-') != -1) {
				// check equals for any substring ('-' is space for collator) - mostly we interested in first part before '-' for equals
				match = KCollatorStringMatcher.cmatches(alignedKey, queryAligned, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
				// case data - '2-x...' query '2xyz', we check that user writes without space
				if (!match) {
					val alignedSingleWord = alignedKey.replace("-", "")
					match = KCollatorStringMatcher.cmatches(queryAligned, alignedSingleWord, KStringMatcherMode.CHECK_ONLY_STARTS_WITH)
				}
			}
			// 3. incomplete query match
			val incomplete = queryIncomplete
			if (!match && incomplete != null) {
				match = KCollatorStringMatcher.cmatches(incomplete, alignedKey, KStringMatcherMode.CHECK_ONLY_STARTS_WITH) ||
						KCollatorStringMatcher.cmatches(alignedKey, incomplete, KStringMatcherMode.CHECK_ONLY_STARTS_WITH)
			}
			return match
		}
	}

	// Active query
	internal class NameIndexReaderQuery(val query: String, val matcher: NameIndexReaderMatcher) {
		val matchedKeys = KTLongHashSet()

		fun addMatchedKey(shift: Long) {
			matchedKeys.add(shift)
		}

		fun matchKey(key: String): Boolean = matcher.matchKey(key)
	}

	inner class PrefixNameValue : Comparable<PrefixNameValue> {
		@JvmField
		var key: String? = null
		@JvmField
		var poi: OsmAndPoiNameIndexData? = null
		@JvmField
		var addr: AddressNameIndexData? = null
		@JvmField
		var data: ByteArray? = null
		@JvmField
		var shift = 0L

		fun getPoi(): OsmAndPoiNameIndexData? {
			val raw = data
			return if (poi == null && raw != null && poiRegion != null) OsmAndPoiNameIndexData.parseFrom(raw) else poi
		}

		fun getAddr(): AddressNameIndexData? {
			val raw = data
			return if (addr == null && raw != null && addressRegion != null) AddressNameIndexData.parseFrom(raw) else addr
		}

		internal fun isLoaded(): Boolean = poi != null || addr != null || data != null

		/** Java lists the frequencies of the suffixes too, which only the inspection tools read. */
		override fun toString(): String {
			val poi = poi
			val addr = addr
			return when {
				poi != null -> "$key (${poi.getAtomsCount()})"
				addr != null -> "$key (${addr.getAtomCount()})"
				else -> "$key <NOT SET>"
			}
		}

		override fun compareTo(other: PrefixNameValue): Int {
			var c = -poi!!.getAtomsCount().compareTo(other.poi!!.getAtomsCount())
			if (c == 0) {
				c = key!!.compareTo(other.key!!)
			}
			return c
		}
	}

	fun readAll(): Boolean = query == null

	fun setTablePointer(totalBytesRead: Long) {
		this.tablePointer = totalBytesRead
	}

	fun getCommonWordsStats(): Map<String, ValueFreq>? {
		commonStatsValues?.let { return it }
		val commonStats = commonStats ?: return null
		val values = HashMap<String, ValueFreq>()
		commonStatsValues = values
		var name: String? = null
		val valueCount = commonStats.getValueCount()
		for (i in 0 until valueCount) {
			name = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(name, commonStats.getValue(i))
			val vf = ValueFreq(name, commonStats.getMatched(i))
			vf.extra = (commonStats.getMatched(i) - commonStats.getNonindexed(i)).toLong()
			val old = values.put(vf.value, vf)
			if (old != null) {
				throw UnsupportedOperationException()
			}
		}
		return values
	}

	fun getCommonStats(): CommonIndexedStats? = commonStats

	fun setCommonIndexed(commonStats: CommonIndexedStats) {
		if (this.commonStats != null) {
			throw IllegalStateException()
		}
		this.commonStats = commonStats
		var name: String? = null
		for (s in commonStats.getValueList()) {
			name = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(name, s)
			commonsList.add(name)
		}
	}

	fun getCommonIndexed(ind: Int): String = commonsList[ind]

	fun resetMatchedKeys(query: String?) {
		if (query != null) {
			matchedKeys[query] = KTLongHashSet()
		}
	}

	fun matchKey(key: String): Boolean {
		val query = query ?: return true
		return query.matchKey(key)
	}

	fun putKey(key: String?, value: Int, prefix: String?) {
		val shift = tablePointer + value
		query?.addMatchedKey(shift)
		if (!indexByRef.containsKey(shift)) {
			val nameValue = PrefixNameValue()
			nameValue.key = key
			indexByRef.put(shift, nameValue)
		}
	}

	override fun toString(): String = indexByRef.toString()

	fun addData(from: OsmAndPoiNameIndexData, currentShift: Long): PrefixNameValue {
		val obj = indexByRef[currentShift]!!
		if (obj.poi != null) {
			throw IllegalStateException(obj.toString())
		}
		obj.shift = currentShift
		obj.poi = from
		cachedBytes += from.getSerializedSize()
		return obj
	}

	fun getAtomsToLoad(loffsets: KTLongArrayList): MutableList<PrefixNameValue> {
		val r = ArrayList<PrefixNameValue>()
		val it = query!!.matchedKeys.iterator()
		while (it.hasNext()) {
			val l = it.next()
			val pv = indexByRef[l]!!
			if (!pv.isLoaded()) {
				loffsets.add(l)
			} else {
				r.add(pv)
			}
		}
		return r
	}

	fun addData(from: ByteArray, currentShift: Long): PrefixNameValue {
		val obj = indexByRef[currentShift]!!
		if (obj.isLoaded()) {
			throw IllegalStateException(obj.toString())
		}
		obj.shift = currentShift
		obj.data = from
		cachedBytes += from.size
		return obj
	}

	fun isCacheRawBlocks(): Boolean = cacheRawBlocks

	fun setCacheRawBlocks(cacheRawBlocks: Boolean): NameIndexReader {
		this.cacheRawBlocks = cacheRawBlocks
		return this
	}

	fun addData(from: AddressNameIndexData, currentShift: Long): PrefixNameValue {
		val obj = indexByRef[currentShift]!!
		if (obj.addr != null) {
			throw IllegalStateException(obj.toString())
		}
		obj.shift = currentShift
		obj.addr = from
		cachedBytes += from.getSerializedSize()
		return obj
	}

	fun setQuery(qr: String): NameIndexReader = setQuery(qr, NameIndexReaderMatcher(qr))

	fun setQuery(qr: String, matcher: NameIndexReaderMatcher): NameIndexReader {
		val query = NameIndexReaderQuery(qr, matcher)
		this.query = query
		this.matchedKeys[qr] = query.matchedKeys
		return this
	}

	/**
	 * The blocks a query matched, in the order of the file as [BinaryMapIndexReader.readFullNameIndex] gives them; java
	 * gives them in the order of a hash set.
	 */
	fun getMatchedPrefixes(query: String): List<PrefixNameValue>? {
		val keys = matchedKeys[query] ?: return null
		val r = ArrayList<PrefixNameValue>()
		val shifts = keys.toArray()
		shifts.sort()
		for (l in shifts) {
			val pv = indexByRef[l]!!
			r.add(pv)
		}
		return r
	}

	fun gcPrefixes(limit: Int) {
		if (limit > 0 && indexByRef.size() > limit) {
			clearPrefixes()
		}
	}

	fun clearPrefixes() {
		indexByRef.clear()
		cachedBytes = 0
		matchedKeys.clear()
	}

	fun getCachedBytes(): Long = cachedBytes

	fun clearQuery() {
		query = null
	}

	fun resetBytesStat() {
		bytesStat = NameIndexReaderBytes()
	}

	fun getBytesStat(): NameIndexReaderBytes = bytesStat

	fun readTableBytes(bytes: Long) {
		bytesStat.readTableBytes += bytes
	}

	fun skipTableBytes(bytes: Long) {
		bytesStat.skipTableBytes += bytes
	}

	fun readAtomsBytes(bytes: Int) {
		bytesStat.readAtomBytes += bytes
	}

	fun skipAtomsBytes(bytes: Long) {
		bytesStat.skipAtomBytes += bytes
	}

	/** A word of the common words of an index with how often it occurs; java also merges and sorts these for the tools. */
	class ValueFreq(var value: String, var freq: Int) {
		var extra = 0L
	}

	companion object {
		const val CITY_AS_STREET_COMMON = "cityasstreetcommon"

		/**
		 * An alternative name of a street with another number of words carries the word altnamecommon1..7: the search
		 * reads that name as its own object, so its words never take the slots of the main name (alt_name name:zh
		 * '1945年5月8日街' vs 'Rue du 8 Mai 1945').
		 */
		const val ALT_NAME_COMMON_PREFIX = "altnamecommon"
		const val ALT_NAME_VARIANTS = 7

		/**
		 * A name the OBF writer made by a search rule (<unglue>, <index>) carries this word: another spelling of a name
		 * of the object, not a name of OSM ("Hauptstr" of "Hauptstraße", "Пасхи" of "о. Пасхи").
		 */
		const val RULE_SPELLING_COMMON = "spellvariantcommon"

		const val POI_CATEGORY_PREFIX = "#^"

		fun altNameMarker(variant: Int): String = ALT_NAME_COMMON_PREFIX + variant

		/** 1..7 for the marker word of an alternative name, 0 for any other word. */
		fun altNameVariant(word: String?): Int {
			if (word != null && word.length == ALT_NAME_COMMON_PREFIX.length + 1 && word.startsWith(ALT_NAME_COMMON_PREFIX)) {
				val c = word[ALT_NAME_COMMON_PREFIX.length]
				if (c >= '1' && c <= '0' + ALT_NAME_VARIANTS) {
					return c - '0'
				}
			}
			return 0
		}

		/** Words the generator adds to a name for the search: never keys, never counted as words of the name. */
		fun isIndexMarker(word: String?): Boolean =
			CITY_AS_STREET_COMMON.equals(word, ignoreCase = true) || RULE_SPELLING_COMMON.equals(word, ignoreCase = true)
					|| altNameVariant(word) > 0
	}
}
