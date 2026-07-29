package net.osmand.search.core.spatial;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SpatialTokenMask}. Mirrors the scenarios in
 * {@code src/test/resources/spatial/mask_verify.py}.
 */
public class SpatialTokenMaskTest {

	private static final long HDR_TOK = 0x5555_5555_5555_555FL;
	private static final long TOKEN_LOW = 0x5555_5555_5555_5550L;

	/** Reference allowedSlow from PR #25534 (spatialPipeline branch). */
	static boolean refAllowedSlow(long m1, long m2) {
		long i = m1 & m2 & HDR_TOK;
		if ((i & 3) == 1) {
			return false;
		}
		if (((i >> 2) & 3) == 1) {
			return false;
		}
		return (i >> 4) == 0;
	}

	/** Reference combine2BitMasks loop from PR #25534. */
	static long refCombineLoop(long m1, long m2, int totalTokens) {
		long result = 0L;
		int b1 = (int) (m1 & 3L);
		int b2 = (int) (m2 & 3L);
		int combinedAtomic;
		if (b1 == 0) {
			combinedAtomic = b2;
		} else if (b2 == 0) {
			combinedAtomic = b1;
		} else {
			combinedAtomic = b1 == 3 && b2 == 3 ? 1 : 2;
		}
		int p1 = (int) ((m1 >> 2) & 3L);
		int p2 = (int) ((m2 >> 2) & 3L);
		int combinedPoi;
		if (p1 == 0) {
			combinedPoi = p2;
		} else if (p2 == 0) {
			combinedPoi = p1;
		} else {
			combinedPoi = (p1 == 3 && p2 == 3) ? 3 : 1;
		}
		result |= (combinedPoi << 2) + combinedAtomic;
		for (int i = 0; i < totalTokens; i++) {
			int shift = i * 2 + 4;
			long s1 = (m1 >> shift) & 3L;
			long s2 = (m2 >> shift) & 3L;
			long finalState;
			if (s1 == 1 || s2 == 1) {
				finalState = 1;
			} else if (s1 == 2 || s2 == 2) {
				finalState = 2;
			} else {
				finalState = 0;
			}
			result |= finalState << shift;
		}
		return result;
	}

	static long randMask(Random rng, int ntok) {
		long atomic = new long[] { 0, 0, 0, 3, 3, 1 }[rng.nextInt(6)];
		long poi = new long[] { 0, 0, 0, 3, 1 }[rng.nextInt(5)];
		long m = atomic | (poi << 2);
		for (int t = 0; t < ntok; t++) {
			double r = rng.nextDouble();
			long st = r < 0.18 ? 1 : (r < 0.30 ? 2 : 0);
			m |= st << (t * 2 + 4);
		}
		return m;
	}

	@Test
	public void allowedMatchesReferenceOnRandomMasks() {
		Random rng = new Random(42);
		for (int i = 0; i < 50_000; i++) {
			long m1 = randMask(rng, SpatialTokenMask.MAX_TOKENS);
			long m2 = randMask(rng, SpatialTokenMask.MAX_TOKENS);
			Assert.assertEquals(refAllowedSlow(m1, m2), SpatialTokenMask.allowed(m1, m2));
		}
	}

	@Test
	public void combineMatchesReferenceOnAllowedPairs() {
		Random rng = new Random(7);
		for (int i = 0; i < 50_000; i++) {
			long m1 = randMask(rng, SpatialTokenMask.MAX_TOKENS);
			long m2 = randMask(rng, SpatialTokenMask.MAX_TOKENS);
			if (!SpatialTokenMask.allowed(m1, m2)) {
				continue;
			}
			long ref = refCombineLoop(m1, m2, SpatialTokenMask.MAX_TOKENS);
			long neu = SpatialTokenMask.combine(m1, m2);
			Assert.assertEquals(ref, neu);
		}
	}

	@Test
	public void combineNeverEmitsUndefinedAtomicState() {
		Random rng = new Random(8);
		for (int i = 0; i < 10_000; i++) {
			long c = SpatialTokenMask.combine(randMask(rng, 30), randMask(rng, 30));
			Assert.assertNotEquals(2L, c & 3L);
		}
	}

	@Test
	public void duplicateWordsPhiladelphiaCounty() {
		long city = SpatialTokenMask.setTokenState(
				SpatialTokenMask.setTokenState(0, 0, SpatialTokenMask.STATE_EXACT), 1,
				SpatialTokenMask.STATE_EXACT);
		long county = SpatialTokenMask.setTokenState(
				SpatialTokenMask.setTokenState(
						SpatialTokenMask.setTokenState(0, 0, SpatialTokenMask.STATE_EXACT), 1,
						SpatialTokenMask.STATE_EXACT),
				2, SpatialTokenMask.STATE_EXACT);

		Assert.assertFalse(SpatialTokenMask.allowed(city, county));

		long[] cityVars = SpatialTokenMask.duplicateWordAlternatives(city, new int[][] { { 0, 1 } });
		long[] countyVars = SpatialTokenMask.duplicateWordAlternatives(county, new int[][] { { 0, 1 } });
		Assert.assertTrue(cityVars.length > 1);

		long[] best = SpatialTokenMask.bestAllowedCombine(cityVars, countyVars);
		Assert.assertNotNull(best);
		Assert.assertEquals(3, SpatialTokenMask.countCoveredTokens(best[0]));
	}

	@Test
	public void contestedHouseNumberPaterson() {
		long sideA = SpatialTokenMask.setTokenState(
				SpatialTokenMask.setTokenState(0, 0, SpatialTokenMask.STATE_EXACT), 1,
				SpatialTokenMask.STATE_AMBIGUOUS);
		long sideB = SpatialTokenMask.setTokenState(
				SpatialTokenMask.setTokenState(
						SpatialTokenMask.setTokenState(0, 1, SpatialTokenMask.STATE_AMBIGUOUS), 2,
						SpatialTokenMask.STATE_EXACT),
				3, SpatialTokenMask.STATE_EXACT);

		Assert.assertTrue(SpatialTokenMask.allowed(sideA, sideB));
		long combined = SpatialTokenMask.combine(sideA, sideB);
		Assert.assertEquals(4, SpatialTokenMask.countCoveredTokens(combined));
		Assert.assertEquals(SpatialTokenMask.STATE_AMBIGUOUS, SpatialTokenMask.getTokenState(combined, 1));

		List<long[]> variants = SpatialTokenMask.expandContestedTokens(sideA, sideB);
		Assert.assertEquals(2, variants.size());
		for (long[] v : variants) {
			boolean aOwns = SpatialTokenMask.getTokenState(v[0], 1) == SpatialTokenMask.STATE_AMBIGUOUS
					&& SpatialTokenMask.getTokenState(v[1], 1) == SpatialTokenMask.STATE_NO_MATCH;
			boolean bOwns = SpatialTokenMask.getTokenState(v[1], 1) == SpatialTokenMask.STATE_AMBIGUOUS
					&& SpatialTokenMask.getTokenState(v[0], 1) == SpatialTokenMask.STATE_NO_MATCH;
			Assert.assertTrue(aOwns ^ bOwns);
		}
	}

	@Test
	public void selfPairExactForbiddenAmbiguousAllowed() {
		long exactObj = SpatialTokenMask.setTokenState(0, 0, SpatialTokenMask.STATE_EXACT);
		long ambObj = SpatialTokenMask.setTokenState(0, 0, SpatialTokenMask.STATE_AMBIGUOUS) | 3;
		Assert.assertFalse(SpatialTokenMask.allowed(exactObj, exactObj));
		Assert.assertTrue(SpatialTokenMask.allowed(ambObj, ambObj));
	}

	@Test
	public void countCoveredTokensMatchesNaive() {
		Random rng = new Random(11);
		for (int i = 0; i < 10_000; i++) {
			long m = randMask(rng, 30);
			int naive = 0;
			for (int t = 0; t < SpatialTokenMask.MAX_TOKENS; t++) {
				if (SpatialTokenMask.getTokenState(m, t) != 0) {
					naive++;
				}
			}
			Assert.assertEquals(naive, SpatialTokenMask.countCoveredTokens(m));
		}
	}
}
