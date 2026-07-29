package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure bit algebra for the spatial pipeline token masks. Every method here is
 * verified by {@code SpatialTokenMaskTest} and
 * {@code src/test/resources/spatial/mask_verify.py}.
 *
 * Mask layout (64 bits):
 *   bits 0-1  atomic header: 00 none, 11 one atomic object, 01 two atomic (saturated)
 *   bits 2-3  poi header:    00 other, 11 poi, 01 poi category
 *   bits 4+   2 bits per query token:
 *             00 no match, 01 exact match, 10 ambiguous (building number / poi ref)
 *
 * Intersection rule (allowed): m1 & m2 must not produce
 *   - atomic field 01 (would exceed two atomic objects)
 *   - poi field 01    (poi x category and category x category are forbidden)
 *   - any query token consumed exactly by both sides
 */
public final class SpatialTokenMask {

	public static final int HEADER_BITS = 4;
	public static final int MAX_TOKENS = (64 - HEADER_BITS) / 2;

	public static final long STATE_NO_MATCH = 0L;   // 00
	public static final long STATE_EXACT = 1L;      // 01
	public static final long STATE_AMBIGUOUS = 2L;  // 10

	public static final long ATOMIC_NONE = 0L;      // 00
	public static final long ATOMIC_TWO = 1L;       // 01 - saturated, cannot grow
	public static final long ATOMIC_ONE = 3L;       // 11

	public static final long POI_NONE = 0L;         // 00
	public static final long POI_CATEGORY = 1L;     // 01
	public static final long POI_OBJECT = 3L;       // 11

	/** Low bit of every 2-bit token field (bits 4, 6, ..., 62). */
	static final long TOKEN_LOW_BITS = 0x5555_5555_5555_5550L;
	/** Token low bits plus the whole header nibble. */
	static final long TOKEN_LOW_BITS_AND_HEADER = 0x5555_5555_5555_555FL;

	/**
	 * Bit i is set iff header intersection value i (= m1 & m2 & 0xF) is
	 * forbidden: atomic field == 01 or poi field == 01. Evaluates to 0x22F2.
	 * Replaces the broken constant 0x12 of the old allowedFast which missed
	 * i = 5, 6, 7, 9, 13 (e.g. adding a third atomic to a saturated pair).
	 */
	static final long FORBIDDEN_HEADER = forbiddenHeaderLookup();

	private static long forbiddenHeaderLookup() {
		long lookup = 0;
		for (int i = 0; i < 16; i++) {
			if ((i & 3) == 1 || ((i >> 2) & 3) == 1) {
				lookup |= 1L << i;
			}
		}
		return lookup;
	}

	/** Ownership forks per pair are capped at 2^MAX_FORK_TOKENS variants. */
	public static final int MAX_FORK_TOKENS = 4;
	/** Cap for duplicate-word alternative masks per object. */
	public static final int MAX_DUPLICATE_VARIANTS = 8;

	private SpatialTokenMask() {
	}

	// ------------------------------------------------------------------
	// Single mask construction / inspection
	// ------------------------------------------------------------------

	public static long setTokenState(long mask, int tokenIdx, long state) {
		int shift = tokenIdx * 2 + HEADER_BITS;
		return (mask & ~(3L << shift)) | ((state & 3L) << shift);
	}

	public static long getTokenState(long mask, int tokenIdx) {
		return (mask >>> (tokenIdx * 2 + HEADER_BITS)) & 3L;
	}

	/** Number of tokens with any match (exact or ambiguous). */
	public static int countCoveredTokens(long mask) {
		return Long.bitCount((mask | (mask >>> 1)) & TOKEN_LOW_BITS);
	}

	public static int countExactTokens(long mask) {
		return Long.bitCount(exactLowBits(mask));
	}

	public static int countAmbiguousTokens(long mask) {
		return Long.bitCount(ambiguousLowBits(mask));
	}

	/** Low bit set at every token position in state 01 (exact). */
	static long exactLowBits(long mask) {
		return mask & ~(mask >>> 1) & TOKEN_LOW_BITS;
	}

	/** Low bit set at every token position in state 10 (ambiguous). */
	static long ambiguousLowBits(long mask) {
		return (mask >>> 1) & ~mask & TOKEN_LOW_BITS;
	}

	// ------------------------------------------------------------------
	// Pair algebra
	// ------------------------------------------------------------------

	/**
	 * True if two objects may be combined. O(1), branch-poor; equivalent to
	 * the reference allowedSlow for every occurring mask (verified).
	 */
	public static boolean allowed(long m1, long m2) {
		long i = m1 & m2 & TOKEN_LOW_BITS_AND_HEADER;
		if ((i >>> HEADER_BITS) != 0) {
			return false; // some query token consumed exactly by both sides
		}
		return ((FORBIDDEN_HEADER >>> i) & 1L) == 0;
	}

	/**
	 * Loop-free replacement for combine2BitMasks (the "x2 speed up" TODO):
	 * per token exact wins over ambiguous wins over no-match, headers
	 * saturate. Never emits the undefined atomic state 10 the loop version
	 * produced in its overflow branch.
	 */
	public static long combine(long m1, long m2) {
		long or = m1 | m2;
		long exact = or & TOKEN_LOW_BITS;
		long ambiguous = (or >>> 1) & TOKEN_LOW_BITS & ~exact;
		long tokens = exact | (ambiguous << 1);

		long a1 = m1 & 3L, a2 = m2 & 3L;
		long atomic = a1 == 0 ? a2 : (a2 == 0 ? a1 : ATOMIC_TWO);
		long p1 = (m1 >>> 2) & 3L, p2 = (m2 >>> 2) & 3L;
		long poi = p1 == 0 ? p2
				: (p2 == 0 ? p1 : (p1 == POI_OBJECT && p2 == POI_OBJECT ? POI_OBJECT : POI_CATEGORY));
		return atomic | (poi << 2) | tokens;
	}

	// ------------------------------------------------------------------
	// Contested ambiguous tokens (per-token fork, replaces all-or-nothing)
	// ------------------------------------------------------------------

	/**
	 * A token ambiguous on BOTH sides of a pair (house number "8" matched
	 * building candidates of two different streets) can belong to either
	 * object. Enumerates per-token ownership: for every contested token one
	 * side keeps it, the other drops it. Tokens ambiguous on a single side
	 * stay where they are and are resolved later by building calculation.
	 *
	 * Per-token (instead of the old all-or-nothing) is what queries with two
	 * house numbers need: "4 8 ave paterson" assigns "4" and "8" to different
	 * streets in the same result.
	 *
	 * @return list of {resolved1, resolved2}; a single identity pair when
	 *         nothing is contested; at most 2^MAX_FORK_TOKENS variants,
	 *         tokens above the cap stay ambiguous on both sides
	 */
	public static List<long[]> expandContestedTokens(long m1, long m2) {
		long contested = ambiguousLowBits(m1) & ambiguousLowBits(m2);
		List<long[]> res = new ArrayList<>(2);
		if (contested == 0) {
			res.add(new long[] { m1, m2 });
			return res;
		}
		long[] forkBits = new long[Math.min(Long.bitCount(contested), MAX_FORK_TOKENS)];
		long rest = contested;
		for (int i = 0; i < forkBits.length; i++) {
			forkBits[i] = Long.lowestOneBit(rest);
			rest &= rest - 1;
		}
		for (int v = 0; v < (1 << forkBits.length); v++) {
			long drop1 = 0, drop2 = 0;
			for (int j = 0; j < forkBits.length; j++) {
				if (((v >> j) & 1) != 0) {
					drop1 |= forkBits[j]; // token goes to side 2
				} else {
					drop2 |= forkBits[j]; // token goes to side 1
				}
			}
			res.add(new long[] { m1 & ~(drop1 | (drop1 << 1)), m2 & ~(drop2 | (drop2 << 1)) });
		}
		return res;
	}

	// ------------------------------------------------------------------
	// Duplicate query words (the "TODO x1 alternative masks" of the pipeline)
	// ------------------------------------------------------------------

	/**
	 * A query with a repeated word ("Philadelphia Philadelphia County") marks
	 * the same object exact on several token positions, so two such objects
	 * can never pass allowed() together although each of them only needs one
	 * of the positions. For every group of equal query tokens matched more
	 * than once this adds alternatives keeping exactly one position of the
	 * group, letting the joiner hand duplicate words out between both sides.
	 *
	 * @param duplicateGroups groups of token indices holding the same word
	 * @return variants, original mask first; length 1 if nothing to do
	 */
	public static long[] duplicateWordAlternatives(long mask, int[][] duplicateGroups) {
		List<Long> variants = new ArrayList<>(2);
		variants.add(mask);
		for (int[] group : duplicateGroups) {
			long groupBits = 0;
			for (int t : group) {
				groupBits |= 1L << (t * 2 + HEADER_BITS);
			}
			int size = variants.size();
			for (int vi = 0; vi < size && variants.size() < MAX_DUPLICATE_VARIANTS; vi++) {
				long v = variants.get(vi);
				long matched = exactLowBits(v) & groupBits;
				if (Long.bitCount(matched) < 2) {
					continue;
				}
				long rest = matched;
				while (rest != 0 && variants.size() < MAX_DUPLICATE_VARIANTS) {
					long keep = Long.lowestOneBit(rest);
					rest &= rest - 1;
					long drop = matched & ~keep;
					variants.add(v & ~(drop | (drop << 1)));
				}
			}
		}
		long[] out = new long[variants.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = variants.get(i);
		}
		return out;
	}

	/**
	 * Best (max token coverage) allowed combination across mask variants of
	 * two objects. Used by the joiner instead of plain allowed()+combine()
	 * when either side has duplicate-word alternatives.
	 *
	 * @return {combined, chosenVariant1, chosenVariant2} or null if every
	 *         variant combination is forbidden
	 */
	public static long[] bestAllowedCombine(long[] variants1, long[] variants2) {
		long[] best = null;
		int bestCovered = -1;
		for (long v1 : variants1) {
			for (long v2 : variants2) {
				if (!allowed(v1, v2)) {
					continue;
				}
				long c = combine(v1, v2);
				int covered = countCoveredTokens(c);
				if (covered > bestCovered) {
					bestCovered = covered;
					best = new long[] { c, v1, v2 };
				}
			}
		}
		return best;
	}
}
