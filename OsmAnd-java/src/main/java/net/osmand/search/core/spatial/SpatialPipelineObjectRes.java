package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TLongArrayList;
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
	public static final long STATE_AMBIGUOUS = 2L; // 10
//    public static final long STATE_ANY = 3L;           // 11
	private static final long MASK_SET_01 = 0x5555_5555_5555_555FL;
	private static final long MASK_SET_02 = 0x5555_5555_5555_5550L;
	// ALLOWED &: 0000, 0011, 1100, 1111,

	public final NameIndexAtom[] atoms;
	public NameIndexAtom mainAtom1;
	public NameIndexAtom mainAtom2;

	public long mainMask = 0;
	public TLongArrayList otherMasks = new TLongArrayList();

	public SpatialPipelineObjectRes(int tCount, NameIndexAtom atom, int index) {
		atoms = new NameIndexAtom[tCount];
		mainAtom1 = atom;
		// TODO OPTIM_FLAG_POI_SAME_AS_CITY_STREET
		int atomic = atom.atomicObject() ? 3 : 0;
		if (atom.atomicObject() && atom.sameNameAreaObj != null) {
			atomic = 1; // 01 - 2 atomic
		}
		int category = atom.isPOI() ? 3 : 0;
		if (atom.isPoiCategory()) {
			category = 1; // 01
		}
		mainMask = atomic | (category << 2);
		setAtom(atom, index);
	}

	public void mergeSame(NameIndexAtom atom, int tokenIdx) {
		// TODO x1 (duplicate words) implement correct mixing alternative masks!
		// we need to separately process situation duplicate words in object and in
		// query
		if (mainAtom1.isPOIRef() || mainAtom1.isBuilding()) {
			mainAtom1 = atom;
		}
		setAtom(atom, tokenIdx);
	}

	public SpatialPipelineObjectRes(long mask, SpatialPipelineObjectRes s1, SpatialPipelineObjectRes s2) {
		atoms = new NameIndexAtom[s1.atoms.length];
		this.mainMask = mask;
		for (int i = 0; i < atoms.length; i++) {
			NameIndexAtom a1 = s1.atoms[i];
			NameIndexAtom a2 = s2.atoms[i];
			if (a1 != null && !a1.isPOIRef() && !a1.isBuilding()) {
				atoms[i] = a1;
				mainAtom1 = a1;
			} else if (a2 != null && !a2.isPOIRef() && !a2.isBuilding()) {
				// couldn't be both same time
				atoms[i] = a2;
				mainAtom2 = a2;
			} else if (a1 != null) {
				atoms[i] = a1;
			} else if (a2 != null) {
				atoms[i] = a2;
			}
		}
	}

	void setAtom(NameIndexAtom atom, int index) {
		atoms[index] = atom;
		mainMask = setTokenState(mainMask, index,
				atom.isBuilding() || atom.isPOIRef() ? STATE_AMBIGUOUS : STATE_EXACT_MATCH);
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

	public static long combine2BitMasks(long mask1, long mask2, int totalTokens) {
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
			// combinedPoi = (p1 == 3 && p2 == 3) ? 3 : 1;
			combinedPoi = 1;
		}
		result |= (combinedPoi << 2) + combinedAtomic;
		for (int i = 0; i < totalTokens; i++) {
			int shift = i * 2 + 4;
			long state1 = (mask1 >> shift) & 3L;
			long state2 = (mask2 >> shift) & 3L;

			long finalState;
			if (state1 == STATE_EXACT_MATCH || state2 == STATE_EXACT_MATCH) {
				finalState = STATE_EXACT_MATCH;
			} else if (state1 == STATE_AMBIGUOUS || state2 == STATE_AMBIGUOUS) {
				finalState = STATE_AMBIGUOUS;
			} else {
				finalState = STATE_NO_MATCH;
			}
			result |= (finalState << shift);
		}
		return result;
	}

	/**
	 * Helper method to format bitmask bits into a readable list of token words.
	 * Accommodates the 2-bits-per-token indexing scheme.
	 */
	static String formatMaskTokens(long mask, List<SpatialSearchToken> tokens) {
		List<String> res = new ArrayList<String>();
		long atomicState = mask & 3L;
		if (atomicState == 3L) { // 11
			res.add("A1");
		} else if (atomicState == 1L) { // 01
			res.add("A2");
		} else if (atomicState == 0L) { // 01
//			res.add("A0");
		} else if (atomicState == 2L) { // 01
			res.add("ABUG");
		}
		long poiState = (mask >> 2) & 3L;
		if (poiState == 3L) { // 11
			res.add("POI");
		} else if (poiState == 1L) { // 01
			res.add("POICAT");
		} else if (atomicState == 2L) { // 01
			res.add("POIBUG");
		} 

		int maxTokens = (64 - 4) / 2; // 30 токенов

		for (int tokenIndex = 0; tokenIndex < maxTokens; tokenIndex++) {
			int bitShift = 4 + (tokenIndex * 2);
			long tokenState = (mask >> bitShift) & 3L;
			if (tokenState != STATE_NO_MATCH) {
				String symbol = tokenState == 1 ? "W" : "B";
				if (tokens != null && tokenIndex < tokens.size() && tokens.get(tokenIndex) != null) {
					String word = tokens.get(tokenIndex).word;
					res.add(word != null ? word : symbol + tokenIndex);
				} else {
					res.add(symbol + tokenIndex);
				}
			}
		}
		return res.toString();
	}

}