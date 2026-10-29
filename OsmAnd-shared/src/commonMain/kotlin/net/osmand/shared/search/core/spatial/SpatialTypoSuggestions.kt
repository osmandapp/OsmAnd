package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher
import net.osmand.shared.binary.NameIndexReader.PrefixNameValue
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.KUnicode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * "Did you mean": the query with its first misspelled word corrected. A complete word of at least MIN_LETTERS letters
 * that names at most MAX_OBJECTS objects is corrected to a word one edit away that names RATIO times as many objects
 * (any number when it names none). The rule was chosen on query logs and synthetic typos, see osmandapp/OsmAnd#5409.
 *
 * A copy of `SpatialTypoSuggestions` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The neighbours of the words are kept by the word itself, as java's `IdentityHashMap`: a word
 * of a query is equal only to itself.
 */
internal class SpatialTypoSuggestions(private val ctx: SpatialSearchContext) {

	// words one edit away from a token and the number of objects they name, counted while its atoms are read
	private val neighbours = HashMap<SpatialSearchToken, MutableMap<String, IntArray>>()

	fun clear() {
		neighbours.clear()
	}

	private fun isCandidate(t: SpatialSearchToken): Boolean =
		ctx.settings.TYPO_SUGGESTION && !t.broad && t.wordNoDot.length >= MIN_LETTERS &&
				KSearchAlgorithms.letters(t.wordNoDot) == t.wordNoDot.length

	/**
	 * The words of the name index blocks one edit away from the token (a word still being typed: its beginning). The
	 * blocks are the ones the token itself was read from, so only typos after its key letters are seen.
	 */
	fun countNeighbours(t: SpatialSearchToken, prefixes: List<PrefixNameValue>) {
		if (isCandidate(t)) {
			for (prefix in prefixes) {
				countNeighbours(t, prefix)
			}
		}
	}

	/**
	 * A word that names at most MAX_OBJECTS objects is looked up once more with a typo in its first letters: the
	 * blocks of the index keys those variants fall into are read and their words one edit away are counted.
	 */
	fun readKeyNeighbours() {
		for (t in ctx.tokens!!) {
			if (!isCandidate(t) || t.atoms.size > MAX_OBJECTS || t.hasPoiCategoryKeys()) {
				continue
			}
			// every beginning of every variant: a key of the index table matches when it is one of them
			val beginnings = HashSet<String>()
			for (v in keyVariants(typoKey(t.wordNoDot), KEY_LETTERS)) {
				for (i in 1..v.length) {
					beginnings.add(v.substring(0, i))
				}
			}
			val matcher = object : NameIndexReaderMatcher(t.word) {
				override fun matchKey(key: String): Boolean = beginnings.contains(typoKey(key))
			}
			val query = "typo-key " + t.word
			var bytes = 0L
			for (fileInd in filesNearestFirst()) {
				if (bytes > KEY_MAX_BYTES) {
					break
				}
				for (typoReader in ctx.internalFile[fileInd].typoReaders) {
					if (if (typoReader.poiRegion != null) !ctx.settings.SEARCH_POI else !ctx.settings.SEARCH_ADDR) {
						continue
					}
					typoReader.resetBytesStat()
					try {
						val prefixes = ctx.files[fileInd].readFullNameIndex(typoReader.setQuery(query, matcher))
						if (prefixes != null) {
							for (prefix in prefixes) {
								countNeighbours(t, prefix)
							}
						}
					} finally {
						val read = typoReader.getBytesStat()
						bytes += read.readTableBytes - read.skipTableBytes + read.readAtomBytes
						typoReader.clearQuery()
						typoReader.clearPrefixes()
					}
				}
			}
		}
	}

	private fun filesNearestFirst(): List<Int> {
		val order = ArrayList<Int>()
		val dist = LongArray(ctx.files.size)
		for (i in ctx.files.indices) {
			order.add(i)
			dist[i] = Long.MAX_VALUE
			val location = ctx.location
			if (location != null) {
				val x = KMapUtils.get31TileNumberX(location.longitude).toLong()
				val y = KMapUtils.get31TileNumberY(location.latitude).toLong()
				for (r in ctx.files[i].getPoiIndexes()) {
					val dx = max(0L, max(r.getLeft31() - x, x - r.getRight31()))
					val dy = max(0L, max(r.getTop31() - y, y - r.getBottom31()))
					dist[i] = min(dist[i], dx * dx + dy * dy)
				}
			}
		}
		order.sortBy { dist[it] }
		return order
	}

	private fun countNeighbours(t: SpatialSearchToken, prefix: PrefixNameValue) {
		val addrData = prefix.getAddr()
		val poiData = if (addrData == null) prefix.getPoi() else null
		if (addrData == null && poiData == null) {
			return
		}
		val typed = typoKey(t.wordNoDot)
		val names = ArrayList<String?>()
		var curSuffix: String? = null
		var any = false
		for (s in addrData?.getSuffixesDictionaryList() ?: poiData!!.getSuffixesDictionaryList()) {
			curSuffix = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s)
			val name = typoKey(prefix.key + curSuffix)
			val near = name.indexOf(' ') == -1 &&
					(isOneEdit(typed, name) || (t.incomplete && isOneEditPrefix(typed, name)))
			names.add(if (near) name else null)
			any = any or near
		}
		if (!any) {
			return
		}
		val inAtom = HashSet<String>()
		if (addrData != null) {
			for (a in addrData.getAtomList()) {
				countNeighbours(t, names, a.getSuffixesBitsetIndexList(), inAtom)
			}
		} else {
			for (a in poiData!!.getAtomsList()) {
				if (a.getPoiIndInBlockCount() > 0) {
					countNeighbours(t, names, a.getSuffixesBitsetIndexList(), inAtom)
				}
			}
		}
	}

	private fun countNeighbours(t: SpatialSearchToken, names: List<String?>, suffixBits: List<Int>, inAtom: MutableSet<String>) {
		inAtom.clear()
		for (bit in suffixBits) {
			val ind = bit / 2 - 1
			if (bit != 0 && bit % 2 == 0 && ind < names.size) {
				val name = names[ind]
				if (name != null && inAtom.add(name)) {
					neighbours.getOrPut(t) { HashMap() }.getOrPut(name) { IntArray(1) }[0]++
				}
			}
		}
	}

	/**
	 * The input with its first misspelled word corrected, null when there is none.
	 */
	fun suggestion(input: String, tokens: List<SpatialSearchToken>): String? {
		val ordered = ArrayList(tokens)
		ordered.sortBy { it.originalOrder }
		for (t in ordered) {
			val near = neighbours[t]
			if (!isCandidate(t) || near == null || t.hasPoiCategoryKeys()) {
				continue
			}
			val objects = t.atoms.size
			if (objects > MAX_OBJECTS) {
				continue
			}
			// the word naming most objects; at a tie a word one edit away before a name whose beginning is (typing)
			val typed = typoKey(t.wordNoDot)
			var best: String? = null
			var bestCount = 0
			var bestWhole = false
			for ((key, value) in near) {
				val c = value[0]
				val whole = isOneEdit(typed, key)
				if (best == null || c > bestCount || (c == bestCount && ((whole && !bestWhole)
							|| (whole == bestWhole && key < best)))
				) {
					best = key
					bestCount = c
					bestWhole = whole
				}
			}
			if (best != null && (objects == 0 || bestCount >= RATIO.toLong() * objects)) {
				return replaceWord(input, t.originalWord, best)
			}
		}
		return null
	}

	companion object {

		// a shorter word has too many neighbours
		const val MIN_LETTERS = 4
		// a word naming more objects is a real name
		const val MAX_OBJECTS = 1
		// a word naming one object is corrected only to a neighbour naming this many times more
		const val RATIO = 300
		// Typos after the key letters are seen in the blocks the word was read from. A typo inside the key of the name
		// index puts the word into another block, so a word naming at most MAX_OBJECTS objects is looked up once more
		// with every one-edit variant of its first KEY_LETTERS letters. The key holds the first
		// charsToBuildPoiNameIndex / charsToBuildAddressNameIndex (4) code points of a word, see IndexCreatorSettings
		// in OsmAnd-tools (java-tools/OsmAndMapCreatorUtilities/src/main/java/net/osmand/obf/preparation/, options
		// --chars-build-poi-nameindex, --chars-build-addr-nameindex). KEY_LETTERS must be at least the longest key of
		// the opened maps: typos in the letters between the two lengths are not found; a shorter key only costs variants.
		const val KEY_LETTERS = 4
		// the maps are read nearest first until this many bytes of their name index are read for a word, so the
		// suggestion does not depend on the device speed
		const val KEY_MAX_BYTES = 1024 * 1024

		// the word replaced in the input; a capital letter of the word stays
		fun replaceWord(input: String, word: String, replacement: String): String? {
			val text = stripIncompleteDot(input)
			val w = stripIncompleteDot(word)
			var i = text.indexOf(w)
			if (i < 0) {
				i = text.lowercase().indexOf(w.lowercase())
			}
			if (i < 0) {
				return null
			}
			var r = replacement
			if (text[i].isUpperCase() && r.isNotEmpty()) {
				r = r[0].uppercaseChar() + r.substring(1)
			}
			return text.substring(0, i) + r + text.substring(i + w.length)
		}

		private fun stripIncompleteDot(s: String): String =
			if (s.endsWith(SpatialSearchToken.DOT_INCOMPLETE_STRING)) s.substring(0, s.length - 1) else s

		// the words one edit away from a word whose edit is in its first letters (the edits after them keep its index key)
		fun keyVariants(w: String, keyLetters: Int): Set<String> {
			val res = HashSet<String>()
			val alphabet = if (w.any { it.code in 0x400..0x4ff })
				"абвгдеёжзийклмнопрстуфхцчшщъыьэюяіїєґ" else "abcdefghijklmnopqrstuvwxyz"
			val n = min(keyLetters, w.length)
			for (i in 0 until n) {
				res.add(w.substring(0, i) + w.substring(i + 1))
				if (i + 1 < w.length) {
					res.add(w.substring(0, i) + w[i + 1] + w[i] + w.substring(i + 2))
				}
				for (c in alphabet) {
					res.add(w.substring(0, i) + c + w.substring(i + 1))
					res.add(w.substring(0, i) + c + w.substring(i))
				}
			}
			res.remove(w)
			return res
		}

		// letters only, lower case, without accents: the form in which a typo and its correction are compared
		fun typoKey(s: String): String {
			var plain = true
			var i = 0
			while (i < s.length && plain) {
				val c = s[i]
				plain = (c in 'a'..'z') || c == ' '
				i++
			}
			if (plain) {
				return s
			}
			val n = KUnicode.normalizeNFD(s.lowercase().replace("ß", "ss"))
			val b = StringBuilder(n.length)
			for (c in n) {
				val category = c.category
				if (category != CharCategory.NON_SPACING_MARK && category != CharCategory.ENCLOSING_MARK
					&& category != CharCategory.COMBINING_SPACING_MARK
				) {
					b.append(c)
				}
			}
			return b.toString()
		}

		// a word still being typed: the beginning of the name is one edit away from it
		fun isOneEditPrefix(typed: String, name: String): Boolean {
			if (name.startsWith(typed)) {
				return false // a continuation of the typed letters, not a typo
			}
			val n = typed.length
			for (len in n - 1..n + 1) {
				if (len > 0 && len < name.length && isOneEdit(typed, name.substring(0, len))) {
					return true
				}
			}
			return false
		}

		// one insertion, deletion, substitution or swap of two neighbouring letters
		fun isOneEdit(a: String, b: String): Boolean {
			val la = a.length
			val lb = b.length
			if (abs(la - lb) > 1 || a == b) {
				return false
			}
			var i = 0
			while (i < la && i < lb && a[i] == b[i]) {
				i++
			}
			if (la == lb) {
				if (a.substring(i + 1) == b.substring(i + 1)) {
					return true
				}
				return i + 1 < la && a[i] == b[i + 1] && a[i + 1] == b[i] && a.substring(i + 2) == b.substring(i + 2)
			}
			return if (la > lb) a.substring(i + 1) == b.substring(i) else a.substring(i) == b.substring(i + 1)
		}
	}
}
