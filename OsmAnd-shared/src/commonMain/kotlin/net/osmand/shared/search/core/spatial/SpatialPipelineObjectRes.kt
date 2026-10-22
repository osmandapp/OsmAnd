package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.util.collections.KTLongHashSet

/**
 * An object of the pipeline of the spatial search, or two of them joined, with the words of the query
 * each one matched and the mask of their states.
 *
 * A copy of `SpatialPipelineObjectRes` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Without `allowedSlow`, `checkBuildingVsStreet` and `minType`, which nothing calls.
 */
class SpatialPipelineObjectRes {

	val atoms: Array<NameIndexAtom?>
	var refs1: Array<NameIndexAtom?>? = null
	var refs2: Array<NameIndexAtom?>? = null // max 2 refs

	val bbox: IntArray
	var mainAtom: NameIndexAtom

	var otherVariants: SpatialPipelineObjectRes? = null

	var mainMask = 0L

	constructor(tCount: Int, atom: NameIndexAtom, index: Int, noPoiType: Boolean) {
		atoms = arrayOfNulls(tCount)
		bbox = atom.coords.bbox31!!
		mainAtom = atom
		var atomic = if (atom.atomicObject()) 3 else 0
		if (atom.atomicObject() && atom.sameNameAreaObj != null) {
			atomic = 1 // 01 - 2 atomic
		}
		var category = if (noPoiType) 3 else 0
		if (atom.isPoiCategory()) {
			category = 1 // 01
		}
		mainMask = (atomic or (category shl 2)).toLong()
		setAtom(atom, index)
	}

	constructor(mask: Long, s1: SpatialPipelineObjectRes, s2: SpatialPipelineObjectRes) {
		atoms = arrayOfNulls(s1.atoms.size)
		this.mainMask = mask
		this.bbox = intArrayOf(s1.bbox[0], s1.bbox[1], s1.bbox[2], s1.bbox[3])
		SpatialSearchResultsList.clipBbox(this.bbox, s2.bbox)
		// any atom doesn't matter
		mainAtom = s1.mainAtom
		if ((s1.refs1 == null && s1.refs2 != null) || (s2.refs1 == null && s2.refs2 != null)) {
			throw IllegalStateException()
		} else if ((s1.refs1 != null && s2.refs2 != null) || (s2.refs1 != null && s1.refs2 != null)) {
			throw IllegalStateException()
		}
		if (s1.refs1 != null && s2.refs1 != null) {
			refs1 = s1.refs1
			refs2 = s2.refs1
		} else if (s1.refs1 != null) {
			refs1 = s1.refs1
			refs2 = s1.refs2
		} else {
			refs1 = s2.refs1
			refs2 = s2.refs2
		}
		for (i in atoms.indices) {
			val a1 = s1.atoms[i]
			val a2 = s2.atoms[i]
			if (a1 != null && a2 != null) {
				throw IllegalStateException()
			}
			if (a1 != null) {
				atoms[i] = a1
			} else if (a2 != null) {
				atoms[i] = a2
			}
		}
	}

	private fun fromPoiCategory(a: NameIndexAtom): Boolean = a.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)

	fun alignOtherVariants() {
		var obj: SpatialPipelineObjectRes = this
		val cat = ((mainMask shr 2) and 3) shl 2
		while (obj.otherVariants != null) {
			obj = obj.otherVariants!!
			obj.mainMask = obj.mainMask or cat
			val refs1 = refs1
			for (i in 0 until (refs1?.size ?: 0)) {
				val ref = refs1!![i]
				if (ref != null) {
					obj.setAtom(ref, i)
				}
			}
		}
	}

	/**
	 * @param copies for every token the tokens with the same word, null when the query repeats no word
	 * @param objectTokens the tokens that found this object
	 */
	fun mergeSame(
		tCount: Int, atom: NameIndexAtom, tokenIdx: Int, noPoiType: Boolean, lastDupToken: Int, copies: LongArray?,
		objectTokens: Long
	) {
		// we need to separately process situation duplicate words in object and in query
		if (mainAtom.isPOIRef() || mainAtom.isBuilding()) {
			mainAtom = atom
		}
		if (noPoiType && !atom.isPoiCategory()) {
			mainMask = mainMask or (3L shl 2)
		}
		val ref = atom.isPOIRef() || atom.isBuilding() || atom.isPoiCategory()
		if (ref) {
			setAtom(atom, tokenIdx)
			return
		}
		var firstInd = tokenIdx
		var lastInd = tokenIdx
		for (ind in 0 until tokenIdx) {
			if (getTokenState(mainMask, ind) == STATE_EXACT_MATCH) {
				if (firstInd == tokenIdx) {
					firstInd = ind
				}
				lastInd = ind
			} else if (getTokenState(mainMask, ind) == STATE_REF) {
				// allow to swap 1 ind for duplicate name 8 8 ave
				if (firstInd == lastInd && firstInd == ind - 1) {
					lastInd = ind
					firstInd = ind
				}
			}
		}
		// a query repeating words ("carrer de sant bartomeu de la quadra costa de sant bartomeu de la quadra"): the name
		// keeps a repeated word once, so a copy the name did not take is no gap, and a copy the name has twice is no extra word
		var repeatedSkips = 0
		val lastMatched = lastInd
		if (copies != null && firstInd < tokenIdx) {
			for (ind in firstInd + 1 until tokenIdx) {
				val stepOver = getTokenState(mainMask, ind) == STATE_NO_MATCH && canStepOver(copies, objectTokens, ind)
				if (stepOver || copyInMask(copies[ind], firstInd, ind)) {
					repeatedSkips++
				}
			}
			while (lastInd < tokenIdx - 1 && canStepOver(copies, objectTokens, lastInd + 1)) {
				lastInd++
			}
		}
		// test '2nd new street'
		if (mainAtom.otherFoundCnt + mainAtom.otherWordsCnt < atom.otherFoundCnt + atom.otherWordsCnt) {
			mainAtom = atom
		}

		val otherWrds = mainAtom.otherFoundCnt + mainAtom.otherWordsCnt
		val joinSymbolsOk = (firstInd + otherWrds + repeatedSkips >= tokenIdx && (tokenIdx - lastInd) <= 1)
		val joinCategoryOk = mainAtom.isPOI() && (fromPoiCategory(mainAtom) || fromPoiCategory(atom))
		val otherVariants = otherVariants
		if ((joinCategoryOk || joinSymbolsOk) && lastDupToken != tokenIdx - 1) {
			setAtom(atom, tokenIdx)
			// the name takes the copies it stepped over: "carrer de sant bartomeu de la quadra" has both "de"
			var ind = lastMatched + 1
			while (copies != null && ind < tokenIdx) {
				if (getTokenState(mainMask, ind) == STATE_NO_MATCH) {
					setAtom(atom, ind)
				}
				ind++
			}
		} else if (otherVariants != null) {
			otherVariants.mergeSame(tCount, atom, tokenIdx, noPoiType, lastDupToken, copies, objectTokens)
		} else {
			val variant = SpatialPipelineObjectRes(tCount, atom, tokenIdx, noPoiType)
			this.otherVariants = variant
			// a new variant takes the words right before it: the ones the query has once ("costa" before the second name)
			// and the copies of its name words no token found in it ("de" of "costa de")
			var ind = tokenIdx - 1
			while (copies != null && ind >= 0) {
				val once = copies[ind] == 0L && atoms[ind] != null
				val notFoundCopy = (objectTokens shr ind and 1L) == 0L && canStepOver(copies, objectTokens, ind)
				if (!once && !notFoundCopy) {
					break
				}
				variant.setAtom(if (once) atoms[ind]!! else atom, ind)
				ind--
			}
		}
	}

	// another copy of the token's word found this object, so its name has the word; copies side by side ("8 8 ave",
	// "2 2 sokak") are different words
	private fun canStepOver(copies: LongArray, objectTokens: Long, ind: Int): Boolean {
		val neighbours = (if (ind > 0) 1L shl (ind - 1) else 0L) or (1L shl (ind + 1))
		return (copies[ind] and objectTokens) != 0L && (copies[ind] and neighbours) == 0L
	}

	// a copy of the token's word is already in this variant: the name has the word twice
	private fun copyInMask(copies: Long, from: Int, ind: Int): Boolean {
		for (i in from until ind) {
			if ((copies shr i and 1L) != 0L && getTokenState(mainMask, i) == STATE_EXACT_MATCH) {
				return true
			}
		}
		return false
	}

	private fun hasId(id: Long): Boolean {
		for (a in atoms) {
			if (a != null && a.id == id) {
				return true
			}
		}
		return false
	}

	fun maskWithoutRefs(): Long {
		var mask = 0L
		for (i in atoms.indices) {
			if (atoms[i] != null) {
				mask = setTokenState(mask, i, STATE_EXACT_MATCH)
			}
		}
		return mask
	}

	fun distinctObjects(): Int {
		val ids = KTLongHashSet()
		for (i in atoms.indices) {
			val a = atoms[i]
			if (a != null) {
				ids.add(a.id)
			}
		}
		return ids.size()
	}

	fun maskOnlyByTokens(): Long {
		var mask = 0L
		for (i in atoms.indices) {
			if (atoms[i] != null || (refs1 != null && refs1!![i] != null) || (refs2 != null && refs2!![i] != null)) {
				mask = setTokenState(mask, i, STATE_EXACT_MATCH)
			}
		}
		return mask
	}

	internal fun setAtom(atom: NameIndexAtom, index: Int) {
		val ref = atom.isBuilding() || atom.isPOIRef()
		if (ref) {
			var refs = refs1
			if (refs == null) {
				refs = arrayOfNulls(atoms.size)
				refs1 = refs
			} else if (refs[index] != null) {
				throw IllegalStateException()
			}
			refs[index] = atom
		} else {
			atoms[index] = atom
		}
		mainMask = setTokenState(mainMask, index, if (ref) STATE_REF else STATE_EXACT_MATCH)
	}

	override fun toString(): String =
		formatMaskTokens(mainMask, null) + " " + atoms.joinToString(", ", "[", "]") { it.toString() }

	companion object {

		// MASK: 0x 01 00 10 ... 00 01
		const val MAX_SUPPORTED_TOKENS = (64 - 4) / 2
		// 0-1 bits (atomic objects):
		// 11 - 1 - atomic, 01 - 2 atomic, 00 - 0 atomic (& 01 intersection forbidden)
		// 2-3 bits (poi & poi category):
		// 11 - poi, 01 - poi category, 00 - other (& 01 intersection forbidden!)
		const val STATE_NO_MATCH = 0L // 00
		const val STATE_EXACT_MATCH = 1L // 01
		const val STATE_REF = 2L // 10
		private const val MASK_SET_01 = 0x5555_5555_5555_555FL
		private const val MASK_SET_02 = 0x5555_5555_5555_5550L
		// ALLOWED &: 0000, 0011, 1100, 1111,

		fun setTokenState(currentMask: Long, tokenIdx: Int, state: Long): Long {
			val shift = tokenIdx * 2 + 4
			val clearMask = (3L shl shift).inv()
			return (currentMask and clearMask) or ((state and 3L) shl shift)
		}

		fun countCoveredTokens(mask: Long): Int {
			val activeTokensMask = (mask or (mask ushr 1)) and MASK_SET_02
			return activeTokensMask.countOneBits()
		}

		fun extraCheck(obj1: SpatialPipelineObjectRes, obj2: SpatialPipelineObjectRes): Boolean {
			if (obj1.mainAtom.id == obj2.mainAtom.id) {
				return false
			}
			if (obj1.hasId(obj2.mainAtom.id) || obj2.hasId(obj1.mainAtom.id)) {
				// alternatives for same object
				return false
			}
			// no fast check to test street intersection with house - no need for now
			return true
		}

		fun allowed(m1: Long, m2: Long): Boolean = allowedFast(m1, m2)

		fun allowedFast(m1: Long, m2: Long): Boolean {
			val i = m1 and m2 and MASK_SET_01
			// 0x22F2 - mask that encodes incorect states as positions in long
			return i < 16 && ((0x22F2 shr i.toInt()) and 1) == 0
		}

		fun getTokenState(mask: Long, tokenIdx: Int): Long {
			val shift = tokenIdx * 2 + 4
			return (mask shr shift) and 3L
		}

		fun combine2BitMasks(m1: Long, m2: Long): Long {
			var mask1 = m1
			var mask2 = m2
			var result = 0L
			val b1 = (mask1 and 3L).toInt()
			val b2 = (mask2 and 3L).toInt()
			val combinedAtomic: Int = if (b1 == 0) {
				b2
			} else if (b2 == 0) {
				b1
			} else {
				1 // 1 atomic + 1 atomic : overflow
			}
			val p1 = ((mask1 shr 2) and 3L).toInt()
			val p2 = ((mask2 shr 2) and 3L).toInt()
			val combinedPoi: Int = if (p1 == 0) {
				p2
			} else if (p2 == 0) {
				p1
			} else {
				if (p1 == 3 && p2 == 3) 3 else 1
			}
			result = result or ((combinedPoi shl 2) + combinedAtomic).toLong()
			mask1 = mask1 shr 4
			mask2 = mask2 shr 4
			var shift = 4
			while (mask1 != 0L || mask2 != 0L) {
				val state1 = mask1 and 3L
				val state2 = mask2 and 3L
				val finalState = if (state1 == STATE_EXACT_MATCH || state2 == STATE_EXACT_MATCH) {
					STATE_EXACT_MATCH
				} else if (state1 == STATE_REF || state2 == STATE_REF) {
					STATE_REF
				} else {
					STATE_NO_MATCH
				}
				result = result or (finalState shl shift)
				mask1 = mask1 shr 2
				mask2 = mask2 shr 2
				shift += 2
			}
			return result
		}

		/**
		 * Helper method to format bitmask bits into a readable list of token words.
		 * Accommodates the 2-bits-per-token indexing scheme.
		 */
		internal fun formatMaskTokens(m: Long, tokens: List<SpatialSearchToken>?): String {
			var mask = m
			val atomicState = mask and 3L
			val sb = StringBuilder()
			sb.append("_")
			if (atomicState == 3L) { // 11
				sb.append("A1")
			} else if (atomicState == 1L) { // 01
				sb.append("A2")
			} else if (atomicState == 0L) { // 01
				sb.append("A0")
			} else if (atomicState == 2L) { // 01
				sb.append("BG")
			}
			val poiState = (mask shr 2) and 3L
			if (poiState == 3L) { // 11
				sb.append("PO")
			} else if (poiState == 1L) { // 01
				sb.append("PC")
			} else if (atomicState == 2L) { // 01
				sb.append("BG")
			} else {
				sb.append("__")
			}
			sb.append("_")
			val maxTokens = MAX_SUPPORTED_TOKENS // 30 tokens
			mask = mask shr 4
			var c = 0
			var tokenIndex = 0
			while (tokenIndex < maxTokens && mask != 0L) {
				val tokenState = mask and 3L
				var symbol = "_"
				mask = mask shr 2
				if (tokenState != STATE_NO_MATCH) {
					c++
					symbol = if (tokenState == 1L) "W" else "B"
				}
				sb.append(symbol)
				tokenIndex++
			}
			return c.toString() + sb.toString()
		}
	}
}
