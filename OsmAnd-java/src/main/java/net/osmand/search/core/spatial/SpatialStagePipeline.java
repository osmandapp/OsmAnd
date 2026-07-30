package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;

public class SpatialStagePipeline {

	private final SpatialSearchContext ctx;

	public static int EXCLUDE_MASKS = 8000; // speed up
	public static boolean CHECK_EXCLUDED = false;
	public static int MAX_STEPS = 5; // 1 - fully covered, 2 - 1 intersection, ...

	public SpatialStagePipeline(SpatialSearchContext ctx) {
		this.ctx = ctx;
	}

	public static final int MAX_SUPPORTED_TOKENS = SpatialTokenMask.MAX_TOKENS;
	public static final long STATE_NO_MATCH = SpatialTokenMask.STATE_NO_MATCH;
	public static final long STATE_EXACT_MATCH = SpatialTokenMask.STATE_EXACT;
	public static final long STATE_AMBIGUOUS = SpatialTokenMask.STATE_AMBIGUOUS;

	public static class SpatialObjectRes {
		public final NameIndexAtom[] atoms;
		public NameIndexAtom mainAtom1;
		public NameIndexAtom mainAtom2;

		public long mainMask = 0;
		/** Alternative masks when duplicate query words hit the same object. */
		long[] variants;

		public SpatialObjectRes(int tCount, NameIndexAtom atom, int index) {
			atoms = new NameIndexAtom[tCount];
			mainAtom1 = atom;
			long atomic = atom.atomicObject() ? SpatialTokenMask.ATOMIC_ONE : SpatialTokenMask.ATOMIC_NONE;
			if (atom.atomicObject() && atom.sameNameAreaObj != null) {
				// POI named like its city/street: saturate so it can not pair with another atomic
				atomic = SpatialTokenMask.ATOMIC_TWO;
			}
			long category = atom.isPOI() ? SpatialTokenMask.POI_OBJECT : SpatialTokenMask.POI_NONE;
			if (atom.isPoiCategory()) {
				category = SpatialTokenMask.POI_CATEGORY;
			}
			mainMask = atomic | (category << 2);
			setAtom(atom, index);
		}

		public void mergeSame(NameIndexAtom atom, int tokenIdx) {
			if ((mainAtom1.isPOIRef() || mainAtom1.isBuilding()) && !atom.isPOIRef() && !atom.isBuilding()) {
				mainAtom1 = atom;
			}
			setAtom(atom, tokenIdx);
		}

		/**
		 * Combination of two objects. resolved1/resolved2 are the masks actually
		 * used after variant / ownership resolution; atoms are taken only from
		 * the side that owns each token.
		 */
		public SpatialObjectRes(long mask, long resolved1, long resolved2, SpatialObjectRes s1, SpatialObjectRes s2) {
			atoms = new NameIndexAtom[s1.atoms.length];
			this.mainMask = mask;
			for (int i = 0; i < atoms.length; i++) {
				boolean own1 = SpatialTokenMask.getTokenState(resolved1, i) != STATE_NO_MATCH;
				boolean own2 = SpatialTokenMask.getTokenState(resolved2, i) != STATE_NO_MATCH;
				NameIndexAtom a1 = own1 ? s1.atoms[i] : null;
				NameIndexAtom a2 = own2 ? s2.atoms[i] : null;
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
			if (mainAtom1 == null) {
				for (NameIndexAtom a : atoms) {
					if (a != null) {
						mainAtom1 = a;
						break;
					}
				}
			}
		}

		void setAtom(NameIndexAtom atom, int index) {
			atoms[index] = atom;
			mainMask = SpatialTokenMask.setTokenState(mainMask, index,
					atom.isBuilding() || atom.isPOIRef() ? STATE_AMBIGUOUS : STATE_EXACT_MATCH);
		}

		long[] variants() {
			return variants != null ? variants : new long[] { mainMask };
		}

		// --- thin wrappers kept for SpatialStagePipelineStats compatibility ---

		public static long setTokenState(long currentMask, int tokenIdx, long state) {
			return SpatialTokenMask.setTokenState(currentMask, tokenIdx, state);
		}

		public static int countCoveredTokens(long mask) {
			return SpatialTokenMask.countCoveredTokens(mask);
		}

		/** Delegates to {@link SpatialTokenMask#allowed(long, long)}. */
		public static boolean allowed(long m1, long m2) {
			return SpatialTokenMask.allowed(m1, m2);
		}

		/** Loop-free; totalTokens kept for call-site compatibility. */
		public static long combine2BitMasks(long mask1, long mask2, int totalTokens) {
			return SpatialTokenMask.combine(mask1, mask2);
		}

		/**
		 * Helper method to format bitmask bits into a readable list of token words.
		 */
		static String formatMaskTokens(long mask, List<SpatialSearchToken> tokens) {
			List<String> res = new ArrayList<String>();
			long atomicState = mask & 3L;
			if (atomicState == SpatialTokenMask.ATOMIC_ONE) {
				res.add("A1");
			} else if (atomicState == SpatialTokenMask.ATOMIC_TWO) {
				res.add("A2");
			} else if (atomicState == SpatialTokenMask.ATOMIC_NONE) {
				res.add("A0");
			} else {
				res.add("A?"); // undefined atomic state 10
			}
			long poiState = (mask >> 2) & 3L;
			if (poiState == SpatialTokenMask.POI_OBJECT) {
				res.add("POI");
			} else if (poiState == SpatialTokenMask.POI_CATEGORY) {
				res.add("POICAT");
			}
			for (int tokenIndex = 0; tokenIndex < SpatialTokenMask.MAX_TOKENS; tokenIndex++) {
				long tokenState = SpatialTokenMask.getTokenState(mask, tokenIndex);
				if (tokenState != STATE_NO_MATCH) {
					String symbol = tokenState == STATE_EXACT_MATCH ? "W" : "B";
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

	private static class MasksStats {
		TLongObjectHashMap<Integer> masks = new TLongObjectHashMap<Integer>();
		public final int intersections;

		public MasksStats(int intersections) {
			this.intersections = intersections;
		}

		int count(long mask) {
			Integer cnt = masks.get(mask);
			if (cnt == null) {
				cnt = 1;
			} else {
				cnt++;
			}
			masks.put(mask, cnt);
			return cnt;
		}

		int count(SpatialObjectRes obj) {
			return count(obj.mainMask);
		}
	}

	public static class SpatialPipelineResults {
		public final List<SpatialSearchToken> tokens;

		public SpatialPipelineResults(List<SpatialSearchToken> tokens) {
			this.tokens = tokens;
		}

		// stage 1
		public final TLongObjectHashMap<SpatialObjectRes> objectsById = new TLongObjectHashMap<>();
		public final TLongObjectHashMap<List<SpatialObjectRes>> excludedMasks = new TLongObjectHashMap<List<SpatialObjectRes>>();

		public final List<MasksStats> masksStats = new ArrayList<>();
		public final HashSkipTileQuadTree<SpatialObjectRes> allObjectsTree = new HashSkipTileQuadTree<>();
		public final HashSkipTileQuadTree<SpatialObjectRes> areaObjectsTree = new HashSkipTileQuadTree<>();
		// stage 2, 3+
		public final List<HashSkipTileQuadTree<SpatialObjectRes>> pairsTree = new ArrayList<>();

		public final List<SpatialSearchResultsList> combinations = new ArrayList<SpatialSearchResultsList>();
	}

	/**
	 * Groups of query token indices with the same word. Objects exact-matched
	 * on several positions of one group get alternative masks so the joiner can
	 * split duplicate words between both sides of a pair.
	 */
	private static int[][] duplicateTokenGroups(List<SpatialSearchToken> tokens) {
		Map<String, List<Integer>> byWord = new LinkedHashMap<>();
		for (int i = 0; i < tokens.size(); i++) {
			String w = tokens.get(i).word;
			if (w != null) {
				byWord.computeIfAbsent(w, k -> new ArrayList<>()).add(i);
			}
		}
		List<int[]> groups = new ArrayList<>();
		for (List<Integer> g : byWord.values()) {
			if (g.size() > 1) {
				int[] arr = new int[g.size()];
				for (int i = 0; i < arr.length; i++) {
					arr[i] = g.get(i);
				}
				groups.add(arr);
			}
		}
		return groups.toArray(new int[0][]);
	}

	private SpatialPipelineResults prepare(List<SpatialSearchToken> tokens) {
		if (tokens.size() > MAX_SUPPORTED_TOKENS) {
			tokens = tokens.subList(0, MAX_SUPPORTED_TOKENS);
		}
		SpatialPipelineResults prep = new SpatialPipelineResults(tokens);
		int totalTokens = tokens.size();
		for (int tokenIdx = 0; tokenIdx < totalTokens; tokenIdx++) {
			SpatialSearchToken token = tokens.get(tokenIdx);
			TIntHashSet deleted = token.getDeletedAtoms();
			for (NameIndexAtom atom : token.atoms) {
				if (deleted.contains(atom.indexInToken)) {
					continue;
				}
				SpatialObjectRes existing = prep.objectsById.get(atom.id);
				if (existing != null) {
					existing.mergeSame(atom, tokenIdx);
				} else {
					prep.objectsById.put(atom.id, new SpatialObjectRes(totalTokens, atom, tokenIdx));
				}
			}
		}
		// alternative masks for duplicate query words
		int[][] dupGroups = duplicateTokenGroups(tokens);
		if (dupGroups.length > 0) {
			long allDupBits = 0;
			for (int[] group : dupGroups) {
				for (int t : group) {
					allDupBits |= 1L << (t * 2 + SpatialTokenMask.HEADER_BITS);
				}
			}
			for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
				// cheap pre-filter: alternatives only matter for objects with
				// two or more exact matches inside duplicate-word positions
				if (Long.bitCount(SpatialTokenMask.exactLowBits(obj.mainMask) & allDupBits) < 2) {
					continue;
				}
				long[] vars = SpatialTokenMask.duplicateWordAlternatives(obj.mainMask, dupGroups);
				if (vars.length > 1) {
					obj.variants = vars;
				}
			}
		}
		// calculate excluded masks
		MasksStats masksStats = new MasksStats(1);
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			masksStats.count(obj);
		}
		prep.masksStats.add(masksStats);

		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			Integer cnt = masksStats.masks.get(obj.mainMask);
			if (cnt > EXCLUDE_MASKS) {
				List<SpatialObjectRes> elst = prep.excludedMasks.get(obj.mainMask);
				if (elst == null) {
					elst = new ArrayList<>();
					prep.excludedMasks.put(obj.mainMask, elst);
				}
				elst.add(obj);
				continue;
			}
			prep.allObjectsTree.addObject(obj, obj.mainAtom1.coords.bbox31, obj.mainAtom1.id);
			if (obj.mainAtom1.isGeoArea()) {
				prep.areaObjectsTree.addObject(obj, obj.mainAtom1.coords.bbox31, obj.mainAtom1.id);
			}
		}
		prep.allObjectsTree.build();
		prep.areaObjectsTree.build();

		return prep;
	}

	private boolean validateStageAndFinish(SpatialPipelineResults prep, int[] intStats,
			List<SpatialObjectRes> preResults, int stage, long ptime) throws IOException {

		long time = System.nanoTime();
		if (ctx.stats.printLogs) {
			String intersections = "";
			if (intStats != null) {
				intersections = String.format(" (cross %,d, partial %,d, full %,d)", intStats[0],
						intStats[1] - intStats[2], intStats[2]);
			}
			System.out.printf("PIPELINE STAGE %d FIND (%.1f ms) - %,d results %s \n", stage, (time - ptime) / 1e6,
					preResults.size(), intersections);
		}
		if (ctx.isCancelled()) {
			return true;
		}
		int nonCategoryRes = 0;
		if (!preResults.isEmpty()) {
			SpatialSearchResultsList stageList = createResultList(prep.tokens, preResults);
			stageList.loadObjectsAndCalcBuildings(ctx);
			if (ctx.isCancelled()) {
				return true;
			}
			List<SpatialSearchResult> res = stageList.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
			int tsize = prep.tokens.size();
			for (SpatialSearchResult r : res) {
				if (!r.isPoiCategory() && r.surplusWords + r.matchedTokens() == tsize) {
					nonCategoryRes++;
				}
			}
			if ((res.size() > 0 && stage == 0) || nonCategoryRes > 0) {
				prep.combinations.add(stageList);
			}
		}
		if (ctx.stats.printLogs) {
			System.out.printf("PIPELINE STAGE %d LOAD (%.1f ms): %d complete results.\n", stage,
					(System.nanoTime() - time) / 1e6, nonCategoryRes);
		}
		int[] stops = ctx.settings.MAX_PIPELINE_STAGE_TO_STOP;
		if (stops.length > 0 && nonCategoryRes > stops[Math.min(stops.length, stage) - 1]) {
			return true;
		}
		return false;
	}

	// =========================================================================
	// Execution Engine
	// =========================================================================
	public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) throws IOException {
		if (tokens == null || tokens.isEmpty()) {
			return Collections.emptyList();
		}
		final int tokensSize = Math.min(tokens.size(), MAX_SUPPORTED_TOKENS);
		long time = System.nanoTime();

		// STEP 0 PREPARE
		int stage = 0;
		SpatialPipelineResults prep = prepare(tokens);
		if (ctx.stats.printLogs) {
			System.out.printf("PIPELINE PREPARE tokens (%.1f ms): %,d objects\n", (System.nanoTime() - time) / 1e6,
					prep.allObjectsTree.getSize());
			SpatialStagePipelineStats.printTree(prep);
		}
		time = System.nanoTime();
		if (stage++ >= MAX_STEPS || ctx.isCancelled()) {
			return prep.combinations;
		}

		// STEP 1: single objects covering all tokens
		List<SpatialObjectRes> singleResults = new ArrayList<>();
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			if (SpatialTokenMask.countCoveredTokens(obj.mainMask) == tokensSize) {
				singleResults.add(obj);
			}
		}
		if (validateStageAndFinish(prep, null, singleResults, stage, time)) {
			return prep.combinations;
		}
		time = System.nanoTime();
		if (stage++ >= MAX_STEPS || ctx.isCancelled()) {
			return prep.combinations;
		}

		// STEP 2: spatial self-join pairs
		HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> selfJoiner = new HashSkipTileQuadTreeJoiner<>(
				prep.allObjectsTree, prep.allObjectsTree);

		boolean exit = join(prep, stage, selfJoiner, true, time);
		if (stage++ >= MAX_STEPS || ctx.isCancelled() || exit) {
			return prep.combinations;
		}
		time = System.nanoTime();

		// STEP 3+: partial pairs x area objects
		for (; stage <= MAX_STEPS && !ctx.isCancelled() && !exit; stage++) {
			HashSkipTileQuadTree<SpatialObjectRes> lastTree = prep.pairsTree.get(prep.pairsTree.size() - 1);
			if (lastTree.isEmpty()) {
				break;
			}
			lastTree.build();
			HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner = new HashSkipTileQuadTreeJoiner<>(
					lastTree, prep.areaObjectsTree);
			exit = join(prep, stage, joiner, false, time);
			time = System.nanoTime();
		}
		// check potential missing results
		if (CHECK_EXCLUDED) {
			checkExcluded(tokensSize, prep);
		}

		return prep.combinations;
	}

	private void checkExcluded(final int tokensSize, SpatialPipelineResults prep) throws IOException {
		long[] excl = prep.excludedMasks.keys();
		if (ctx.stats.printLogs) {
			System.out.println("Excluded masks: " + excl.length);
		}
		MasksStats baseMasksStats = prep.masksStats.get(0);
		long time = System.nanoTime();
		// join() below appends to masksStats/pairsTree — iterate a snapshot
		final int statsSnapshot = prep.masksStats.size();
		for (int stage = 1; stage < MAX_STEPS && stage < statsSnapshot; stage++) {
			for (int i = 0; i < statsSnapshot; i++) {
				MasksStats masksStats = prep.masksStats.get(i);
				if (masksStats.intersections != stage) {
					continue;
				}
				HashSkipTileQuadTree<SpatialObjectRes> partialTree = i == 0 ? prep.allObjectsTree
						: prep.pairsTree.get(i - 1);

				TLongHashSet found = new TLongHashSet();
				for (int k = 0; k < excl.length; k++) {
					long maskExcl = excl[k];
					for (long m : masksStats.masks.keys()) {
						if (!SpatialTokenMask.allowed(m, maskExcl)) {
							continue;
						}
						long combined = SpatialTokenMask.combine(m, maskExcl);
						if (SpatialTokenMask.countCoveredTokens(combined) == tokensSize) {
							if (ctx.stats.printLogs) {
								Integer c1 = baseMasksStats.masks.get(maskExcl);
								Integer c2 = masksStats.masks.get(m);
								System.out.printf(
										"Potential results %d intersections - missing %s (%,d) x %s (%,d < %,d ) = %,d \n",
										stage + 1, SpatialObjectRes.formatMaskTokens(maskExcl, prep.tokens), c1,
										SpatialObjectRes.formatMaskTokens(m, prep.tokens), c2, partialTree.getSize(),
										c1 * c2);
							}
							found.add(maskExcl);
						}
					}
				}
				if (found.size() == 0) {
					continue;
				}
				HashSkipTileQuadTree<SpatialObjectRes> exclTree = new HashSkipTileQuadTree<>();
				for (long exclMask : found.toArray()) {
					for (SpatialObjectRes r : prep.excludedMasks.get(exclMask)) {
						exclTree.addObject(r, r.mainAtom1.coords.bbox31, r.mainAtom1.id);
					}
				}
				exclTree.build();

				partialTree.build();
				HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> tailJoiner = new HashSkipTileQuadTreeJoiner<>(
						partialTree, exclTree);
				boolean exit = join(prep, stage + 1, tailJoiner, false, time);
				if (ctx.isCancelled() || exit) {
					return;
				}
				time = System.nanoTime();
			}
		}
	}

	private boolean join(SpatialPipelineResults prep, int stage,
			HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner, boolean selfJoin, long time)
			throws IOException {
		List<SpatialObjectRes> pairResults = new ArrayList<>();
		final int tokensSize = prep.tokens.size();
		HashSkipTileQuadTree<SpatialObjectRes> pairsTree = new HashSkipTileQuadTree<>();
		prep.pairsTree.add(pairsTree);
		final MasksStats ms = new MasksStats(stage);
		prep.masksStats.add(ms);
		int[] itStats = new int[] { 0, 0, 0 };
		if (ctx.stats.printLogs) {
			System.out.printf("PIPELINE STAGE %d INTERSECT - %,d x %,d tree...\n", stage,
					joiner.getTree1().getSize(), joiner.getTree2().getSize());
		}
		joiner.joinAllBuckets((e1, e2) -> {
			itStats[0]++;
			// skip identity pairs in self-join (ambiguous-only objects pass allowed(m,m))
			if (selfJoin && e1.objId == e2.objId) {
				return;
			}
			SpatialObjectRes o1 = e1.obj;
			SpatialObjectRes o2 = e2.obj;
			long m1, m2, combinedMask;
			if (o1.variants == null && o2.variants == null) {
				m1 = o1.mainMask;
				m2 = o2.mainMask;
				if (!SpatialTokenMask.allowed(m1, m2)) {
					return;
				}
				combinedMask = SpatialTokenMask.combine(m1, m2);
			} else {
				// duplicate-word alternatives: pick best allowed combination
				long[] best = SpatialTokenMask.bestAllowedCombine(o1.variants(), o2.variants());
				if (best == null) {
					return;
				}
				combinedMask = best[0];
				m1 = best[1];
				m2 = best[2];
			}
			itStats[1]++;
			ms.count(combinedMask);
			if (SpatialTokenMask.countCoveredTokens(combinedMask) == tokensSize) {
				itStats[2]++;
				// fork contested ambiguous tokens (house number on both sides)
				for (long[] resolved : SpatialTokenMask.expandContestedTokens(m1, m2)) {
					long resolvedMask = SpatialTokenMask.combine(resolved[0], resolved[1]);
					SpatialObjectRes res = new SpatialObjectRes(resolvedMask, resolved[0], resolved[1], o1, o2);
					if (acceptPairSemantic(ctx, res)) {
						pairResults.add(res);
					}
				}
				return;
			}
			SpatialObjectRes res = new SpatialObjectRes(combinedMask, m1, m2, o1, o2);
			if (res.mainAtom1 == null) {
				return; // no owned atoms left after variant resolution
			}
			int[] bb = res.mainAtom1.coords.bbox31;
			int[] clippedBBox = new int[] { bb[0], bb[1], bb[2], bb[3] };
			if (res.mainAtom2 != null) {
				SpatialSearchResultsList.clipBbox(clippedBBox, res.mainAtom2.coords.bbox31);
			}
			pairsTree.addObject(res, clippedBBox, -1);
		});

		return validateStageAndFinish(prep, itStats, pairResults, stage, time);
	}

	private SpatialSearchResultsList createResultList(List<SpatialSearchToken> tokens, List<SpatialObjectRes> r) {
		SpatialSearchResultsList singleResults = new SpatialSearchResultsList(tokens);
		for (SpatialObjectRes res : r) {
			singleResults.tileIds.add(res.atoms[0].coords.bboxTileId);
			for (int i = 0; i < res.atoms.length; i++) {
				singleResults.linearResults.add(res.atoms[i]);
			}
		}
		return singleResults;
	}

	/**
	 * Semantic rules not already enforced by the mask header in
	 * {@link SpatialTokenMask#allowed(long, long)}.
	 */
	public static boolean acceptPairSemantic(SpatialSearchContext ctx, SpatialObjectRes pair) {
		NameIndexAtom a1 = pair.mainAtom1;
		NameIndexAtom a2 = pair.mainAtom2;
		if (a1 == null || a2 == null) {
			return true; // deep combination - sides were validated pairwise before
		}
		SpatialTextSearch.SpatialTextSearchSettings settings = ctx.settings;
		if (!settings.SEARCH_STREET_INTERSECTIONS && a1.isStreetBuilding() && a2.isStreetBuilding()) {
			return false;
		}
		if (!settings.SEARCH_POI_INTERSECTIONS && a1.isPOI() && a2.isPOI()) {
			return false;
		}
		return true;
	}
}
