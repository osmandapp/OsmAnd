package net.osmand.search.core.spatial;

import java.util.Arrays;
import java.util.List;

import gnu.trove.set.hash.TLongHashSet;
import net.osmand.binary.NameIndexReader;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;

public class SpatialPipelineObjectRes {

	// MASK: 0x 01 00 10 ... 00 01
	public static final int MAX_SUPPORTED_TOKENS = (64 - 4) / 2;
	// 0-1 bits (atomic objects):
	// 11 - 1 - atomic, 01 - 2 atomic, 00 - 0 atomic (& 01 intersection forbidden)
	// 2-3 bits (poi & poi category):
	// 11 - poi, 01 - poi category, 00 - other (& 01 intersection forbidden!)
	public static final long STATE_NO_MATCH = 0L; // 00
	public static final long STATE_EXACT_MATCH = 1L; // 01
	public static final long STATE_REF = 2L; // 10
//    public static final long STATE_ANY = 3L;           // 11
	private static final long MASK_SET_01 = 0x5555_5555_5555_555FL;
	private static final long MASK_SET_02 = 0x5555_5555_5555_5550L;
	// ALLOWED &: 0000, 0011, 1100, 1111,

	public final NameIndexAtom[] atoms;
	public NameIndexAtom[] refs1;
	public NameIndexAtom[] refs2; // max 2 refs
	
	public final int[] bbox;
	public NameIndexAtom mainAtom;
	
	public SpatialPipelineObjectRes otherVariants;

	public long mainMask = 0;

	public SpatialPipelineObjectRes(int tCount, NameIndexAtom atom, int index, boolean noPoiType) {
		atoms = new NameIndexAtom[tCount];
		bbox = atom.coords.bbox31;
		mainAtom = atom;
		int atomic = atom.atomicObject() ? 3 : 0;
		if (atom.atomicObject() && atom.sameNameAreaObj != null) {
			atomic = 1; // 01 - 2 atomic
		}
		int category = noPoiType ? 3 : 0;
		if (atom.isPoiCategory()) {
			category = 1; // 01
		}
		mainMask = atomic | (category << 2);
		setAtom(atom, index);
	}
	

	public SpatialPipelineObjectRes(long mask, SpatialPipelineObjectRes s1, SpatialPipelineObjectRes s2) {
		atoms = new NameIndexAtom[s1.atoms.length];
		this.mainMask = mask;
		this.bbox = new int[] { s1.bbox[0], s1.bbox[1], s1.bbox[2], s1.bbox[3] };
		SpatialSearchResultsList.clipBbox(this.bbox, s2.bbox);
		// any atom doesn't matter
		mainAtom = s1.mainAtom;
		if ((s1.refs1 == null && s1.refs2 != null) || (s2.refs1 == null && s2.refs2 != null)) {
			throw new IllegalStateException();
		} else if ((s1.refs1 != null && s2.refs2 != null) || (s2.refs1 != null && s1.refs2 != null)) {
			throw new IllegalStateException();
		}
		if (s1.refs1 != null && s2.refs1 != null) {
			refs1 = s1.refs1;
			refs2 = s2.refs1;
		} else if (s1.refs1 != null) {
			refs1 = s1.refs1;
			refs2 = s1.refs2;
		} else {
			refs1 = s2.refs1;
			refs2 = s2.refs2;
		}
		for (int i = 0; i < atoms.length; i++) {
			NameIndexAtom a1 = s1.atoms[i];
			NameIndexAtom a2 = s2.atoms[i];
			if (a1 != null && a2 != null) {
				throw new IllegalStateException();
			}
			if (a1 != null) {
				atoms[i] = a1;
			} else if (a2 != null) {
				atoms[i] = a2;
			}
		}
	}
	
	private boolean fromPoiCategory(NameIndexAtom a) {
		return a.name != null && a.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX);
	}

	public void alignOtherVariants() {
		SpatialPipelineObjectRes obj = this;
		long cat = ((mainMask >> 2) & 3) << 2;
		while (obj.otherVariants != null) {
			obj = obj.otherVariants;
			obj.mainMask |= cat;
			for (int i = 0; refs1 != null && i < refs1.length; i++) {
				if (refs1[i] != null) {
					obj.setAtom(refs1[i], i);
				}
			}
			
		}		
	}
	
	public void mergeSame(int tCount, NameIndexAtom atom, int tokenIdx, boolean noPoiType, int lastDupToken) {
		// we need to separately process situation duplicate words in object and in query
		if (mainAtom.isPOIRef() || mainAtom.isBuilding()) {
			mainAtom = atom;
		}
		if (noPoiType && !atom.isPoiCategory()) {
			mainMask |= (3 << 2);
		}
		boolean ref = atom.isPOIRef() || atom.isBuilding() || atom.isPoiCategory();
		if (ref) {
			setAtom(atom, tokenIdx);
			return;
		}
		int firstInd = tokenIdx;
		int lastInd = tokenIdx;
		for (int ind = 0; ind < tokenIdx; ind++) {
			if (getTokenState(mainMask, ind) == STATE_EXACT_MATCH) {
				if (firstInd == tokenIdx) {
					firstInd = ind;
				}
				lastInd = ind;
			} else if (getTokenState(mainMask, ind) == STATE_REF) {
				// allow to swap 1 ind for duplicate name 8 8 ave
				if (firstInd == lastInd && firstInd == ind - 1) {
					lastInd = ind;
					firstInd = ind;
				}

			}
		}
		// test '2nd new street'
		if (mainAtom.otherFoundCnt + mainAtom.otherWordsCnt < atom.otherFoundCnt + atom.otherWordsCnt) {
			mainAtom = atom;
		}

		int otherWrds = mainAtom.otherFoundCnt + mainAtom.otherWordsCnt;
		boolean joinSymbolsOk = (firstInd + otherWrds >= tokenIdx && (tokenIdx - lastInd) <= 1);
		boolean joinCategoryOk = mainAtom.isPOI() && (fromPoiCategory(mainAtom) || fromPoiCategory(atom));
		if ((joinCategoryOk || joinSymbolsOk)  && lastDupToken != tokenIdx - 1
				) {
			setAtom(atom, tokenIdx);
		} else if (otherVariants != null) {
			otherVariants.mergeSame(tCount, atom, tokenIdx, noPoiType, lastDupToken);
		} else {
			otherVariants = new SpatialPipelineObjectRes(tCount, atom, tokenIdx, noPoiType);
		}
	}
	
	public long maskWithoutRefs() {
		long mask = 0;
		for (int i = 0; i < atoms.length; i++) {
			if (atoms[i] != null) {
				mask = setTokenState(mask, i, STATE_EXACT_MATCH);
			}
		}
		return mask;
	}
	
	public int distinctObjects() {
		TLongHashSet ids = new TLongHashSet();
		for (int i = 0; i < atoms.length; i++) {
			if (atoms[i] != null) {
				ids.add(atoms[i].id);
			}
		}
		return ids.size();
	}
	
	public long maskOnlyByTokens() {
		long mask = 0;
		for (int i = 0; i < atoms.length; i++) {
			if (atoms[i] != null || (refs1 != null && refs1[i] != null) || (refs2 != null && refs2[i] != null)) {
				mask = setTokenState(mask, i, STATE_EXACT_MATCH);
			}
		}
		return mask;
	}

	void setAtom(NameIndexAtom atom, int index) {
		boolean ref = atom.isBuilding() || atom.isPOIRef();
		if (ref) {
			if (refs1 == null) {
				refs1 = new NameIndexAtom[atoms.length];
			} else if (refs1[index] != null) {
				throw new IllegalStateException();
			}
			refs1[index] = atom;
		} else {
			atoms[index] = atom;
		}
		mainMask = setTokenState(mainMask, index, ref ? STATE_REF : STATE_EXACT_MATCH);
	}

	public static long setTokenState(long currentMask, int tokenIdx, long state) {
		int shift = tokenIdx * 2 + 4;
		long clearMask = ~(3L << shift);
		return (currentMask & clearMask) | ((state & 3L) << shift);
	}

	public static int countCoveredTokens(long mask) {
		long activeTokensMask = (mask | (mask >>> 1)) & MASK_SET_02;
		return Long.bitCount(activeTokensMask);
	}
	
	public static boolean extraCheck(SpatialPipelineObjectRes obj1, SpatialPipelineObjectRes obj2) {
		if (obj1.mainAtom.id == obj2.mainAtom.id) {
			return false;
		}
		if (obj1.hasId(obj2.mainAtom.id) || obj2.hasId(obj1.mainAtom.id)) {
			// alternatives for same object
			return false;
		}
		// no fast check to test street intersection with house - no need for now
//		if(checkBuildingVsStreet(obj1, obj2)) { return false; }
		
		return true;
	}

	private boolean hasId(long id) {
		for (NameIndexAtom a : atoms) {
			if (a != null && a.id == id) {
				return true;
			}
		}
		return false;
	}


	static boolean checkBuildingVsStreet(SpatialPipelineObjectRes obj1, SpatialPipelineObjectRes obj2) {
		int minType1 = obj1.minType();
		int minType2 = obj2.minType();
		if (minType1 == SpatialSearchToken.BUILDING_TYPE) {
			if (minType2 == SpatialSearchToken.STREET_TYPE || minType2 == SpatialSearchToken.BUILDING_TYPE) {
				return true;
			}
		}
		if (minType2 == SpatialSearchToken.BUILDING_TYPE) {
			if (minType1 == SpatialSearchToken.STREET_TYPE) {
				return true;
			}
		}
		return false;
	}
	
	public int minType() {
		int minType = 100;
		for (int i = 0; i < atoms.length; i++) {
			if (atoms[i] != null) {
				minType = Math.min(minType, atoms[i].type);
			}
			if (refs1 != null && refs1[i] != null) {
				minType = Math.min(minType, refs1[i].type);
			}
			if (refs2 != null && refs2[i] != null) {
				minType = Math.min(minType, refs2[i].type);
			}
		}
		return minType;
	}

	public static boolean allowed(long m1, long m2) {
		return allowedFast(m1, m2);
	}

	public static boolean allowedFast(long m1, long m2) {
		long i = m1 & m2 & MASK_SET_01;
		// 0x22F2 - mask that encodes incorect states as positions in long
		return i < 16 && ((0x22F2 >> i) & 1L) == 0;
	}

	public static boolean allowedSlow(long m1, long m2) {
		long i = (m1 & m2 & MASK_SET_01);
		if ((i & 3) == 1) {
			return false;
		} else if (((i >> 2) & 3) == 1) {
			return false;
		}
		return (i >> 4) == 0;
	}

	public static long getTokenState(long mask, int tokenIdx) {
		int shift = tokenIdx * 2 + 4;
		return (mask >> shift) & 3L;
	}

	public static long combine2BitMasks(long mask1, long mask2) {
		long result = 0L;
		int b1 = (int) (mask1 & 3L);
		int b2 = (int) (mask2 & 3L);
		int combinedAtomic;
		if (b1 == 0) {
			combinedAtomic = b2;
		} else if (b2 == 0) {
			combinedAtomic = b1;
		} else {
			// combinedAtomic = b1 == 3 && b2 == 3 ? 1 : 2; // 1 atomic + 1 atomic :
			// overflow
			combinedAtomic = 1; // 1 atomic + 1 atomic : overflow
		}
		int p1 = (int) ((mask1 >> 2) & 3L);
		int p2 = (int) ((mask2 >> 2) & 3L);
		int combinedPoi;
		if (p1 == 0) {
			combinedPoi = p2;
		} else if (p2 == 0) {
			combinedPoi = p1;
		} else {
			combinedPoi = (p1 == 3 && p2 == 3) ? 3 : 1;
//			combinedPoi = 1; // forbid category on all intersections
		}
		result |= (combinedPoi << 2) + combinedAtomic;
		mask1 >>= 4;
		mask2 >>= 4;
		int shift = 4;
		while (mask1 != 0 || mask2 != 0) {
			long state1 = mask1 & 3L;
			long state2 = mask2 & 3L;
			long finalState;
			if (state1 == STATE_EXACT_MATCH || state2 == STATE_EXACT_MATCH) {
				finalState = STATE_EXACT_MATCH;
			} else if (state1 == STATE_REF || state2 == STATE_REF) {
				finalState = STATE_REF;
			} else {
				finalState = STATE_NO_MATCH;
			}
			result |= (finalState << shift);
			mask1 >>= 2;
			mask2 >>= 2;
			shift += 2;
		}
		return result;
	}

	/**
	 * Helper method to format bitmask bits into a readable list of token words.
	 * Accommodates the 2-bits-per-token indexing scheme.
	 */
	static String formatMaskTokens(long mask, List<SpatialSearchToken> tokens) {
		long atomicState = mask & 3L;
		StringBuilder sb = new StringBuilder();
		sb.append("_");
		if (atomicState == 3L) { // 11
			sb.append("A1");
		} else if (atomicState == 1L) { // 01
			sb.append("A2");
		} else if (atomicState == 0L) { // 01
			sb.append("A0");
		} else if (atomicState == 2L) { // 01
			sb.append("BG");
		}
		long poiState = (mask >> 2) & 3L;
		if (poiState == 3L) { // 11
			sb.append("PO");
		} else if (poiState == 1L) { // 01
			sb.append("PC");
		} else if (atomicState == 2L) { // 01
			sb.append("BG");
		}  else {
			sb.append("__");
		}
		sb.append("_");
		int maxTokens = MAX_SUPPORTED_TOKENS; // 30 tokens
		mask = mask >> 4;
		int c = 0;
		for (int tokenIndex = 0; tokenIndex < maxTokens & mask != 0; tokenIndex++) {
			long tokenState = mask & 3L;
			String symbol = "_";
			mask >>= 2;
			if (tokenState != STATE_NO_MATCH) {
				c++;
				symbol = tokenState == 1 ? "W" : "B";
			}
			sb.append(symbol);
		}
		return c + sb.toString();
	}

	@Override
	public String toString() {
		return formatMaskTokens(mainMask, null) + " " + Arrays.toString(atoms);
	}


}