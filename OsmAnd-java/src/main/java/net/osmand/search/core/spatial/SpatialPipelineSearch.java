package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

public class SpatialPipelineSearch {

	public static int MIN_RESULTS_ENLARGE = 0;
	public static int MIN_RESULTS_PARTIAL = 0; // for testing now
	
	private final SpatialPipelineContext ctx;

	public SpatialPipelineSearch(SpatialSearchContext ctx, List<SpatialSearchToken> tokens) {
		if (tokens.size() > SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS) {
			tokens = tokens.subList(0, SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS);
		}
		this.ctx = new SpatialPipelineContext(tokens, ctx);
	}

	public static class SpatialPipelineContext {
		final List<SpatialSearchToken> tokens;
		final SpatialSearchContext searchContext;
		final SpatialTextSearchSettings settings;
		final SpatialSearchStats stats;

		final PipelineMetrics metrics = new PipelineMetrics();

		final TLongObjectHashMap<SpatialPipelineObjectRes> objectsById = new TLongObjectHashMap<>();
		List<SpatialObjectsBucket> initBuckets;

		List<SpatialObjectsBucket> allBuckets = new ArrayList<>();

		final List<SpatialSearchResultsList> results = new ArrayList<SpatialSearchResultsList>();
		int overallResults = 0;
		int nonCatResults = 0;

		public SpatialPipelineContext(List<SpatialSearchToken> tokens, SpatialSearchContext ctx) {
			this.tokens = new ArrayList<SpatialSearchToken>(tokens);
			this.tokens.sort(new Comparator<SpatialSearchToken>() {

				@Override
				public int compare(SpatialSearchToken o1, SpatialSearchToken o2) {
					return Integer.compare(o1.originalOrder, o2.originalOrder);
				}
			});
			this.searchContext = ctx;
			this.settings = ctx.settings;
			this.stats = ctx.stats;
		}

		public boolean isCancelled() {
			return searchContext.isCancelled();
		}
	}

	public class SpatialObjectsBucket {
		public final SpatialObjectsBucket parent;
		public final SpatialObjectsBucket edge;
		public final int depth;
		public int edgeIndex = -1;
		// 1. virtual based on masks before materialization
		public MasksGroupInfo potentialMasks = null;

		// 2. geometric actual
		public HashSkipTileQuadTree<SpatialPipelineObjectRes> resTree = null;
		public TLongObjectHashMap<List<SpatialPipelineObjectRes>> resObjectsByMasks = null;

		// Lazy init
		private List<SpatialObjectsBucket> children = null;

		public SpatialObjectsBucket(SpatialObjectsBucket ref) {
			this.parent = ref.parent;
			this.edge = ref.edge;
			this.depth = ref.depth;
			this.edgeIndex = ref.edgeIndex;
			this.potentialMasks = new MasksGroupInfo(ref.parent != null);
		}

		public SpatialObjectsBucket(int edgeIndex) {
			// edge itself
			this.parent = null;
			this.edge = null;
			this.depth = 1;
			this.edgeIndex = edgeIndex;
			this.potentialMasks = new MasksGroupInfo(false);
		}

		public SpatialObjectsBucket(SpatialObjectsBucket parent, SpatialObjectsBucket edge) {
			this.parent = parent;
			this.edge = edge;
			this.depth = parent.depth + 1;
			this.potentialMasks = new MasksGroupInfo(true);
			this.edgeIndex = edge.edgeIndex;
		}

		public boolean isMaskComputed() {
			return resObjectsByMasks != null || potentialMasks != null;
		}

		public boolean isComputed() {
			return resObjectsByMasks != null;
		}

		public HashSkipTileQuadTree<SpatialPipelineObjectRes> ensureTreeBuilt() {
			if (resTree == null && resObjectsByMasks != null && !resObjectsByMasks.isEmpty()) {
				ctx.metrics.treeBuild.start();
				resTree = new HashSkipTileQuadTree<>();
				long[] masks = resObjectsByMasks.keys();
				for (int i = 0; i < masks.length; i++) {
					List<SpatialPipelineObjectRes> list = resObjectsByMasks.get(masks[i]);
					if (list != null) {
						for (int j = 0; j < list.size(); j++) {
							SpatialPipelineObjectRes obj = list.get(j);
							resTree.addObject(obj, obj.bbox);
						}
					}
				}
				ctx.metrics.treeBuild.finish();
			}
			if (resTree != null && !resTree.isEmpty() && !resTree.isBuilt()) {
				ctx.metrics.tree++;
				ctx.metrics.treeBuild.start();
				resTree.build();
				ctx.metrics.treeBuild.finish();
			}
			return resTree;
		}

		public boolean isEmpty() {
			TLongObjectHashMap<?> maskMaps = getMaskMaps();
			if (maskMaps != null) {
				return maskMaps.isEmpty();
			}
			return true;
		}

		public int getMasksCount() {
			TLongObjectHashMap<?> maskMaps = getMaskMaps();
			if (maskMaps != null) {
				return maskMaps.size();
			}
			return 0;
		}

		public boolean hasFullCovered(int totalTokens) {
			TLongObjectHashMap<?> maskMaps = getMaskMaps();
			if (maskMaps != null) {
				TLongObjectIterator<?> it = maskMaps.iterator();
				while (it.hasNext()) {
					it.advance();
					if (SpatialPipelineObjectRes.countCoveredTokens(it.key()) == totalTokens) {
						return true;
					}
				}
			}
			return false;
		}

		public TLongObjectHashMap<?> getMaskMaps() {
			if (resObjectsByMasks != null) {
				return resObjectsByMasks;
			}
			if (potentialMasks != null) {
				return potentialMasks.map;
			}
			return null;
		}

		public void markInitialComputed() {
			if (this.potentialMasks.onlyCounts) {
				throw new IllegalStateException();
			}
			this.resObjectsByMasks = new TLongObjectHashMap<List<SpatialPipelineObjectRes>>();
			TLongObjectIterator<MaskGroupInfo> it = potentialMasks.map.iterator();
			while (it.hasNext()) {
				it.advance();
				this.resObjectsByMasks.put(it.key(), it.value().objects);
			}
			this.potentialMasks = null;
		}

		public void markComputed(HashSkipTileQuadTree<SpatialPipelineObjectRes> tree,
				TLongObjectHashMap<List<SpatialPipelineObjectRes>> objectsByMasks) {
			this.resTree = tree;
			this.resObjectsByMasks = objectsByMasks;
			this.potentialMasks = null;
		}

		public void addChild(SpatialObjectsBucket child) {
			if (children == null) {
				children = new ArrayList<>(4);
			}
			children.add(child);
		}

		public List<SpatialObjectsBucket> getChildren() {
			return children != null ? children : Collections.emptyList();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Bucket [" + depth + "] {count=");
			sb.append(getMasksCount());
			long totalObjs = 0;
			if (resObjectsByMasks != null) {
				for (List<?> list : resObjectsByMasks.valueCollection()) {
					totalObjs += list.size();
				}
			} else if (potentialMasks != null) {
				for (MaskGroupInfo list : potentialMasks.map.valueCollection()) {
					totalObjs += list.count;
				}
			}
			sb.append(", objs=").append(totalObjs);
			sb.append(", masks=[");
			TLongObjectHashMap<?> masks = getMaskMaps();
			if (masks != null) {
				boolean first = true;
				for (long m : masks.keys()) {
					if (!first) {
						sb.append(", ");
					}
					sb.append(SpatialPipelineObjectRes.formatMaskTokens(m, null));
					first = false;
				}
			}
			return sb.append("]}").toString();
		}
	}

	private void computeGeoCross(SpatialObjectsBucket b, List<SpatialPipelineObjectRes> res, int targetTokens) {
		if (b == null || b.isEmpty()) {
			return;
		}
		if (b.isComputed()) {
			if (res != null) {
				collectCoverageResults(b, res, targetTokens);
			}
			return;
		}
		if (!b.parent.isComputed()) {
			computeGeoCross(b.parent, null, 0);
		}
		HashSkipTileQuadTree<SpatialPipelineObjectRes> parentTree = b.parent.ensureTreeBuilt();
		HashSkipTileQuadTree<SpatialPipelineObjectRes> edgeTree = b.edge.ensureTreeBuilt();

		HashSkipTileQuadTree<SpatialPipelineObjectRes> resTree = null;
		TLongObjectHashMap<List<SpatialPipelineObjectRes>> resObjectsByMasks = new TLongObjectHashMap<>();
		if (parentTree != null && edgeTree != null && !parentTree.isEmpty() && !edgeTree.isEmpty()) {
			ctx.metrics.joins++;
			ctx.metrics.join.start();
			HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> joiner = new HashSkipTileQuadTreeJoiner<>(
					parentTree, edgeTree);
			ctx.metrics.joinCross += parentTree.getSize() * edgeTree.getSize();
			joiner.joinAllBuckets((e1, e2) -> {
				ctx.metrics.pairsChecked++;
				SpatialPipelineObjectRes obj1 = e1.obj;
				SpatialPipelineObjectRes obj2 = e2.obj;
				if (!SpatialPipelineObjectRes.allowed(obj1.mainMask, obj2.mainMask)) {
					return;
				}
				if (!SpatialPipelineObjectRes.extraCheck(obj1, obj2)) {
					return; // alternatives
				}
				ctx.metrics.pairsAccepted++;
				long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(obj1.mainMask, obj2.mainMask);
				SpatialPipelineObjectRes combinedObj = new SpatialPipelineObjectRes(combinedMask, obj1, obj2);
				List<SpatialPipelineObjectRes> list = resObjectsByMasks.get(combinedMask);
				if (list == null) {
					list = new ArrayList<>();
					resObjectsByMasks.put(combinedMask, list);
				}
				list.add(combinedObj);
			});
			ctx.metrics.join.finish();
		}
		b.markComputed(resTree, resObjectsByMasks);
		if (!b.getChildren().isEmpty()) {
			ctx.metrics.recalcMasks.start();
			for (SpatialObjectsBucket ch : b.children) {
				computeMaskCross(ch);
			}
			ctx.metrics.recalcMasks.finish();
		}
		if (res != null) {
			collectCoverageResults(b, res, targetTokens);
		}
	}

	private void collectCoverageResults(SpatialObjectsBucket b, List<SpatialPipelineObjectRes> res, int targetTokens) {
		if (b == null || b.resObjectsByMasks == null || b.resObjectsByMasks.isEmpty()) {
			return;
		}
		long[] masks = b.resObjectsByMasks.keys();
		for (int i = 0; i < masks.length; i++) {
			long mask = masks[i];
			if (SpatialPipelineObjectRes.countCoveredTokens(mask) == targetTokens) {
				List<SpatialPipelineObjectRes> list = b.resObjectsByMasks.get(mask);
				if (list != null && !list.isEmpty()) {
					res.addAll(list);
				}
			}
		}
	}

	private static class MasksGroupInfo {
		final TLongObjectHashMap<MaskGroupInfo> map = new TLongObjectHashMap<MaskGroupInfo>();
		final boolean onlyCounts;

		MasksGroupInfo(boolean onlyCounts) {
			this.onlyCounts = onlyCounts;
		}

		public long totalCounts() {
			TLongObjectIterator<MaskGroupInfo> it = map.iterator();
			long t = 0;
			while (it.hasNext()) {
				it.advance();
				t += it.value().count;
			}
			return t;
		}

		public void add(long combinedMask, long sz) {
			MaskGroupInfo info = map.get(combinedMask);
			if (info == null) {
				info = new MaskGroupInfo(combinedMask, 0);
				map.put(combinedMask, info);
			}
			info.count += sz;
		}

		public void add(long combinedMask, SpatialPipelineObjectRes obj) {
			MaskGroupInfo info = map.get(combinedMask);
			if ((obj == null) != onlyCounts) {
				throw new IllegalStateException();
			}
			if (info == null) {
				info = new MaskGroupInfo(combinedMask, 0);
				if (!onlyCounts) {
					info.objects = new ArrayList<>();
				}
				map.put(combinedMask, info);
			}
			info.count++;
			if (obj != null) {
				info.objects.add(obj);
			}
		}
	}

	private static class MaskGroupInfo {
		long count;
		List<SpatialPipelineObjectRes> objects;

		MaskGroupInfo(long mask, int count) {
			this.count = count;
		}
	}

	private void splitBucketIntoSmallBuckets(SpatialObjectsBucket toSplit, List<SpatialObjectsBucket> outputList) {
		if (toSplit.isEmpty()) {
			return;
		}
		int totalTokens = ctx.tokens.size();
		boolean splitByFreq = toSplit.parent == null;
		boolean split = false;
		MasksGroupInfo pMasks = toSplit.potentialMasks;
		if (!toSplit.isComputed()) {
			if (toSplit.hasFullCovered(totalTokens) && pMasks.map.size() > 1) {
				split = true;
			} else if (ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS < pMasks.map.size()) {
				split = true;
			} else if (splitByFreq && ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD < pMasks.totalCounts()) {
				split = true;
			}
		}
		if (!split || toSplit.parent != null) {
			finalizeAndAddBucket(toSplit, outputList);
			return;
		}
		SpatialObjectsBucket fullCoveredBucket = new SpatialObjectsBucket(toSplit);
		SpatialObjectsBucket combinedRareBucket = new SpatialObjectsBucket(toSplit);
		long[] masks = pMasks.map.keys();
		for (int i = 0; i < masks.length; i++) {
			long mask = masks[i];
			boolean isFullCovered = (SpatialPipelineObjectRes.countCoveredTokens(mask) == totalTokens);
			boolean single = splitByFreq
					&& pMasks.map.get(mask).count > ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD;
			if (single) {
				SpatialObjectsBucket bucket = new SpatialObjectsBucket(toSplit);
				bucket.potentialMasks.map.put(mask, pMasks.map.get(mask));
				finalizeAndAddBucket(bucket, outputList);
				continue;
			}

			SpatialObjectsBucket bucket = isFullCovered ? fullCoveredBucket : combinedRareBucket;
			bucket.potentialMasks.map.put(mask, pMasks.map.get(mask));
			boolean close = false;
			if (bucket.getMasksCount() >= ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS) {
				close = true;
			} else if (splitByFreq
					&& bucket.potentialMasks.totalCounts() >= ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD) {
				close = true;
			}
			if (close) {
				finalizeAndAddBucket(bucket, outputList);
				if (isFullCovered) {
					fullCoveredBucket = new SpatialObjectsBucket(toSplit);
				} else {
					combinedRareBucket = new SpatialObjectsBucket(toSplit);
				}
			}
		}
		finalizeAndAddBucket(fullCoveredBucket, outputList);
		finalizeAndAddBucket(combinedRareBucket, outputList);
	}

	private void finalizeAndAddBucket(SpatialObjectsBucket bucket, List<SpatialObjectsBucket> outputList) {
		if (bucket != null && !bucket.isEmpty()) {
			if (bucket.parent != null) {
				bucket.parent.addChild(bucket);
			}
			outputList.add(bucket);
		}
	}

	long CROSS_INTERSECT_MAX = 50; // almost never intersect more

	private void computeChildrenMasksCross(SpatialObjectsBucket bucket, List<SpatialObjectsBucket> edges,
			List<SpatialObjectsBucket> nextLevel) {
		TLongObjectHashMap<?> parentMasks = bucket.getMaskMaps();
		if (parentMasks == null) {
//			computeMasksCross(b, edges, null, prep, false);
			throw new IllegalStateException();
		}
		for (int i = 0; i < edges.size(); i++) {
			if (i < bucket.edgeIndex) {
				continue;
			}
			SpatialObjectsBucket edge = edges.get(i);
			if (edge.isEmpty()) {
				continue;
			}
			SpatialObjectsBucket child = new SpatialObjectsBucket(bucket, edge);
			computeMaskCross(child);
			splitBucketIntoSmallBuckets(child, nextLevel);
		}
	}

	private void computeMaskCross(SpatialObjectsBucket bucket) {
		if (bucket.isComputed()) {
			return;
		}
		int masks = bucket.potentialMasks.map.size();
		bucket.potentialMasks.map.clear();
		TLongObjectIterator<?> pIterator = bucket.parent.getMaskMaps().iterator();
		while (pIterator.hasNext()) {
			pIterator.advance();
			long pMask = pIterator.key();
			TLongObjectIterator<?> eIterator = bucket.edge.getMaskMaps().iterator();
			while (eIterator.hasNext()) {
				eIterator.advance();
				long eMask = eIterator.key();
				ctx.metrics.maskPairsEval++;
				if (!SpatialPipelineObjectRes.allowed(pMask, eMask)) {
					continue;
				}
				ctx.metrics.maskPairsAllowed++;
				long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(pMask, eMask);
				long estSize1 = estInterSize(pIterator.value());
				long estSize2 = estInterSize(eIterator.value());
				long estCross = Math.max(Math.min(estSize1, CROSS_INTERSECT_MAX) * estSize2,
						Math.min(estSize2, CROSS_INTERSECT_MAX) * estSize1);
				bucket.potentialMasks.add(combinedMask, estCross);
			}
		}
		ctx.metrics.maskDelta += (bucket.potentialMasks.map.size() - masks);
		if (bucket.children != null) {
			for (SpatialObjectsBucket ch : bucket.children) {
				computeMaskCross(ch);
			}
		}
	}

	public long estInterSize(Object val) {
		if (val instanceof List l) {
			return l.size();
		} else if (val instanceof MaskGroupInfo m) {
			return m.count;
		}
		return 1;
	}
	

	private boolean disallowPoiType(NameIndexAtom atom, SpatialSearchToken token) {
		if (atom.isPOI() && !atom.isPOIRef()) {
			if (atom.poiTypes == null) {
				return true;
			}
			boolean match = false;
			for (int k = 0; k < atom.poiTypes.size(); k++) {
				int pType = atom.poiTypes.get(k);
//				if ( pType == poiType.id) { should be handled by atom associated directly
				if (ctx.searchContext.poiSearch.getById(pType).isPlace()) {
					match = true;
					break;
				}
			}
			if (!match) {
				return true;
			}
		}
		if (atom.isBuilding()) {
			return true;
		}
		if (token.likelyPartOfBuilding() || token.getMainNumber() > 0) {
//			return true; // bar 4 avenue
		}
		return false;
	}
	

	private SpatialPipelineContext prepareInitialBuckets() {
		int totalTokens = ctx.tokens.size();
		// combine & merge by tokens
		Map<String, Integer> dupTokens = new HashMap<>();
		for (int tokenIdx = 0; tokenIdx < totalTokens; tokenIdx++) {
			SpatialSearchToken token = ctx.tokens.get(tokenIdx);
			Integer lastDupToken = dupTokens.get(token.word);
			dupTokens.put(token.word, tokenIdx);
			if (lastDupToken == null) {
				lastDupToken = tokenIdx;
			}
//			if (!SearchAlgorithms.isNumber2Letters(token.wordAligned)) {
				// fixes 'Am Remsufer Remseck am Neckar' but incorrect for '138 138 Scott Avenue Bellefonte' & 'W&W'
//				token = ctx.tokens.get(lastDupToken); 
//			}
			TIntHashSet deleted = token.getDeletedAtoms();
			for (NameIndexAtom atom : token.atoms) {
				if (deleted.contains(atom.indexInToken)) {
					continue;
				}
				SpatialPipelineObjectRes existing = ctx.objectsById.get(atom.id);
				boolean noPoiType = disallowPoiType(atom, token);
				if (existing != null) {
					existing.mergeSame(totalTokens, atom, tokenIdx, noPoiType, lastDupToken);
				} else {
					SpatialPipelineObjectRes obj = new SpatialPipelineObjectRes(totalTokens, atom, tokenIdx, noPoiType);
					ctx.objectsById.put(atom.id, obj);
				}
			}
		}

		// create initial bucket & split it
		SpatialObjectsBucket mainBucket = new SpatialObjectsBucket(0);
		for (SpatialPipelineObjectRes obj : ctx.objectsById.valueCollection()) {
			long mask = obj.mainMask;
			mainBucket.potentialMasks.add(mask, obj);
			obj.alignOtherVariants();
			while (obj.otherVariants != null) {
				obj = obj.otherVariants;
				mainBucket.potentialMasks.add(obj.mainMask, obj);
			}
		}
		List<SpatialObjectsBucket> buckets = new ArrayList<>();
		splitBucketIntoSmallBuckets(mainBucket, buckets);
		for (int i = 0; i < buckets.size(); i++) {
			SpatialObjectsBucket bucket = buckets.get(i);
			// assign correct index
			bucket.edgeIndex = i;
			bucket.markInitialComputed();
		}
		ctx.initBuckets = buckets;
		return ctx;
	}

	private boolean validateResultsAndFinish(List<SpatialPipelineObjectRes> preResults, int stage,
			List<SpatialSearchToken> tokens) throws IOException {
		if (ctx.isCancelled()) {
			return true;
		}
		if (preResults.isEmpty()) {
			return false;
		}
		long time = System.nanoTime();
		int nonCategoryRes = 0;
		SpatialSearchResultsList stageList = createResultList(tokens, preResults);
		stageList.loadObjectsAndCalcBuildings(ctx.searchContext);
		if (ctx.isCancelled()) {
			return true;
		}
		List<SpatialSearchResult> res = stageList.sortResults(ctx.searchContext, ctx.settings.DEDUPLICATE_RES);
		int tsize = tokens.size();
		for (SpatialSearchResult r : res) {
			if (!r.isPoiCategory() && r.surplusWords + r.matchedTokens() == tsize) {
				nonCategoryRes++;
			}
		}
		if (res.size() > 0) {
			ctx.results.add(stageList);
			ctx.overallResults += res.size();
			ctx.nonCatResults += nonCategoryRes;
		}
		if (ctx.stats.printLogs) {
			String compString = tokens.size() == ctx.tokens.size() ? "complete"
					: ("partial-" + (ctx.tokens.size() - tokens.size()));
			System.out.printf("PIPELINE %d LOAD RESULTS (%.1f ms): %d %s results.\n", stage,
					(System.nanoTime() - time) / 1e6, nonCategoryRes, compString);
		}
		int[] stops = ctx.settings.MAX_PIPELINE_RES_TO_STOP;
		if (stops.length > 0 && nonCategoryRes >= stops[Math.min(stops.length, stage + 1) - 1]) {
			return true;
		}
		if (ctx.overallResults >= ctx.settings.MAX_PIPELINE_ANY_RES && ctx.settings.MAX_PIPELINE_ANY_RES > 0) {
			return true;
		}
		return false;
	}

	private SpatialSearchResultsList createResultList(List<SpatialSearchToken> tokens,
			List<SpatialPipelineObjectRes> r) {
		SpatialSearchResultsList singleResults = new SpatialSearchResultsList(ctx.searchContext, tokens);
		List<NameIndexAtom> atoms = new ArrayList<>();
		for (SpatialPipelineObjectRes res : r) {
			// actual size doesn't matter any more
			int z = 15;
			long tileId = HashQuadTree.encodeTileId(z, res.bbox[0] >> (31 - z), res.bbox[1] >> (31 - z));
			
			for (int refs = 0; refs < (res.refs2 != null ? 2 : 1); refs++) {
				atoms.clear();
				for (int i = 0; i < res.atoms.length; i++) {
					NameIndexAtom atom = res.atoms[i];
					if (atom == null && res.refs1 != null && refs == 0) {
						atom = res.refs1[i];
					} else if (atom == null && res.refs2 != null && refs == 1) {
						atom = res.refs2[i];
					}
					if (atom != null) {
						boolean skip = false;
						if (atom.isPoiCategory() && res.distinctObjects() > 1 && i < tokens.size()) {
							// skip incomplete (not efficient to do by masks)
							skip = tokens.get(i).incomplete;
							//|| tokens.get(i).hasPoiCategoryKeys(); // incorrect 'brand street'
							if(ctx.searchContext.poiSearch.getById((int)atom.id).isPlace()) {
								skip = true;
							}
						} 
						if(!skip) {
							atoms.add(atom);
						}
					}
				}
				if (atoms.size() == tokens.size()) {
					singleResults.tileIds.add(tileId);
					singleResults.linearResults.addAll(atoms);
				}
			}
		}
		return singleResults;
	}

	public List<SpatialSearchResultsList> runPipeline() throws IOException {
		int tokensSize = ctx.tokens.size();
		ctx.metrics.prepareTimer.start();
		prepareInitialBuckets();
		ctx.metrics.logPrepareTime(ctx);

		int depth = 0;
		depth = runSearch(tokensSize, depth, ctx.initBuckets);

		if (ctx.nonCatResults <= MIN_RESULTS_ENLARGE && tokensSize > 1 && !ctx.isCancelled()) {
			if (ctx.stats.printLogs) {
				System.out.printf("PIPELINE Enlarge bboxes on stage %d\n", depth);
			}
			ctx.allBuckets = new ArrayList<>(ctx.initBuckets); // 1st stage is skipped
			List<SpatialObjectsBucket> currentLevel = ctx.initBuckets;
			enlargeBboxes(currentLevel);
			depth = runSearch(tokensSize, depth, currentLevel);
		}
		if (ctx.overallResults <= MIN_RESULTS_PARTIAL && tokensSize > 1 && !ctx.isCancelled()) {
			if (ctx.stats.printLogs) {
				System.out.printf("PIPELINE LOOKUP %d partial results\n", depth);
			}
			calculatePartialCoverage(depth);
		}
		return ctx.results;
	}

	private void enlargeBboxes(List<SpatialObjectsBucket> currentLevel) {
		for (SpatialObjectsBucket b : currentLevel) {
			b.children = null;
			b.resTree = null;
			for (List<SpatialPipelineObjectRes> l : b.resObjectsByMasks.valueCollection()) {
				for (SpatialPipelineObjectRes r : l) {
					if (r.mainAtom.isGeoArea()) {
						double val = ctx.settings.evalEnlargeBoundary(ctx.settings.ENLARGE_BOUNDARIES,
								NameIndexAtomXY.distanceInM(r.bbox));
						if (val > 0) {
							NameIndexAtomXY.enlargeBbox(val, r.bbox);
						}

					}
				}
			}
		}
	}

	private int runSearch(int tokensSize, int depth, List<SpatialObjectsBucket> currentLevel) throws IOException {
		while (depth < ctx.settings.PIPELINE_MAX_STEPS && !currentLevel.isEmpty()) {
			ctx.metrics.resetDepth();
			if (depth > 0) {
				ctx.metrics.masksComputeTimer.start();
				List<SpatialObjectsBucket> nextLevel = new ArrayList<>();
				for (SpatialObjectsBucket b : currentLevel) {
					computeChildrenMasksCross(b, ctx.initBuckets, nextLevel);
				}
				ctx.metrics.logMasksCross(ctx, currentLevel, nextLevel, depth);
				currentLevel = nextLevel;
			}
			ctx.allBuckets.addAll(currentLevel);
			ctx.metrics.geoComputeTimer.start();
			List<SpatialPipelineObjectRes> res = new ArrayList<>();
			int fullCoverageCount = 0;
			for (SpatialObjectsBucket b : currentLevel) {
				if (b.hasFullCovered(tokensSize) || b.parent == null) {
					fullCoverageCount++;
					computeGeoCross(b, res, tokensSize);
				}
			}
			ctx.metrics.logGeoCross(ctx, currentLevel, depth, fullCoverageCount, res.size());
			if (validateResultsAndFinish(res, depth, ctx.tokens)) {
				return depth;
			}
			depth++;
		}
		return depth;
	}

	private List<SpatialSearchResultsList> calculatePartialCoverage(int depth) throws IOException {
		int tokensSize = ctx.tokens.size();
		// collect full but without refs
		Map<Integer, List<SpatialPipelineObjectRes>> fullWithoutRefs = new HashMap<>();
		List<SpatialPipelineObjectRes> fullResults = new ArrayList<SpatialPipelineObjectRes>();
		for (SpatialObjectsBucket b : ctx.allBuckets) {
			if (b.hasFullCovered(tokensSize)) {
				collectCoverageResults(b, fullResults, tokensSize);
			}
		}
		for (SpatialPipelineObjectRes fullResult : fullResults) {
			long noRefsMask = fullResult.maskWithoutRefs();
			if (noRefsMask != fullResult.mainMask) {
				int covered = SpatialPipelineObjectRes.countCoveredTokens(noRefsMask);
				if (!fullWithoutRefs.containsKey(covered)) {
					fullWithoutRefs.put(covered, new ArrayList<>());
				}
				fullResult.mainMask = noRefsMask;
				fullResult.refs1 = null;
				fullResult.refs2 = null;
				fullWithoutRefs.get(covered).add(fullResult);
			}
		}

		// calculate partial
		for (int targetTokens = tokensSize - 1; targetTokens >= 1; targetTokens--, depth++) {
			List<SpatialPipelineObjectRes> partialRes = new ArrayList<>();
			ctx.metrics.geoComputeTimer.start();
			int proc = 0;
			List<SpatialObjectsBucket> currentLevel = new ArrayList<>();
			for (SpatialObjectsBucket b : ctx.allBuckets) {
//				if (b.isComputed()) { // speedup for partial
				if (b.hasFullCovered(targetTokens)) {
					proc++;
					currentLevel.add(b);
					computeGeoCross(b, partialRes, targetTokens);
				}
			}
			ctx.metrics.logGeoCross(ctx, currentLevel, depth, proc, partialRes.size());
			if (fullWithoutRefs.containsKey(targetTokens)) {
				partialRes.addAll(fullWithoutRefs.get(targetTokens));
			}
			if (partialRes.isEmpty()) {
				continue;
			}
			TLongObjectHashMap<List<SpatialPipelineObjectRes>> groupByTokens = new TLongObjectHashMap<List<SpatialPipelineObjectRes>>();
			for (SpatialPipelineObjectRes r : partialRes) {
				long mask = r.maskOnlyByTokens();
				if (!groupByTokens.contains(mask)) {
					groupByTokens.put(mask, new ArrayList<>());
				}
				groupByTokens.get(mask).add(r);
			}
			TLongObjectIterator<List<SpatialPipelineObjectRes>> it = groupByTokens.iterator();
			boolean ex = false;
			while (it.hasNext()) {
				it.advance();
				long mask = it.key();
				List<SpatialPipelineObjectRes> res = it.value();
				List<SpatialSearchToken> lst = new ArrayList<>();
				for (int i = 0; i < tokensSize; i++) {
					if (SpatialPipelineObjectRes.getTokenState(mask, i) != 0) {
						lst.add(ctx.tokens.get(i));
					}
				}

				ex |= validateResultsAndFinish(res, depth, lst);
			}
			if (ex) {
				return ctx.results;
			}
		}
		return ctx.results;
	}

	private static class PipelineMetrics {
		boolean doTiming = true;
		Timer prepareTimer = new Timer();
		Timer masksComputeTimer = new Timer();
		long maskPairsEval, maskPairsAllowed, maskDelta;
		Timer recalcMasks = new Timer();

		Timer geoComputeTimer = new Timer();

		Timer treeBuild = new Timer();
		int tree = 0;
		Timer join = new Timer();
		int joins = 0;
		long joinCross = 0;

		long pairsChecked, pairsAccepted;

		int countMasks(List<SpatialObjectsBucket> list) {
			int sum = 0;
			for (SpatialObjectsBucket b : list)
				sum += b.getMasksCount();
			return sum;
		}

		void logGeoCross(SpatialPipelineContext ctx, List<SpatialObjectsBucket> buckets, int level, int fcCount,
				int resCount) {
			geoComputeTimer.finish();
			if (ctx.stats.printLogs) {
				String s = String.format(
						"Tree Built %,d (%.1f ms) | Joins %,d (%.1f ms) | Cross objs %,dK, check %,d, accept %,d | Prune (%.1f ms) %,d del masks: %,d eval -> %,d accept ",
						tree, treeBuild.ms(), joins, join.ms(), joinCross / 1024, pairsChecked, pairsAccepted,
						recalcMasks.ms(), maskDelta, maskPairsEval, maskPairsAllowed);
				System.out.printf("PIPELINE %d GEO CROSS (%.1f ms) - %,d RESULTS | %,d (%,d full) Buckets | %s\n",
						level, geoComputeTimer.ms(), resCount, buckets.size(), fcCount, s);
			}
			maskPairsEval = 0;
			maskPairsAllowed = 0;
			maskDelta = 0;
		}

		public void logMasksCross(SpatialPipelineContext ctx, List<SpatialObjectsBucket> currentLevel,
				List<SpatialObjectsBucket> nextLevel, int stage) {
			ctx.metrics.masksComputeTimer.finish();
			if (ctx.stats.printLogs) {
				System.out.printf(
						"PIPELINE %d MASKS CROSS (%.1f ms) | %,d -> %,d Buckets | %,d -> %,d Masks | Masks Cross %,d eval -> %,d accept\n",
						stage, masksComputeTimer.ms(), currentLevel.size(), nextLevel.size(), countMasks(currentLevel),
						countMasks(nextLevel), maskPairsEval, maskPairsAllowed);
			}
			maskPairsEval = 0;
			maskPairsAllowed = 0;
			maskDelta = 0;
		}

		public void logPrepareTime(SpatialPipelineContext ctx) {
			prepareTimer.finish();
			if (ctx.stats.printLogs) {
				SpatialPipelineStats.printTree(ctx);
//				SpatialPipelineStats.printTokenTree(ctx);
				System.out.printf("PIPELINE 0 PREPARE (%.1f ms) %,d Buckets | %,d masks | %,d objects \n",
						prepareTimer.ms(), ctx.initBuckets.size(), countMasks(ctx.initBuckets), ctx.objectsById.size());
			}
		}

		void resetDepth() {
			treeBuild.reset();
			joins = 0;
			tree = 0;
			join.reset();
			recalcMasks.reset();

			geoComputeTimer.reset();
			masksComputeTimer.reset();
			joinCross = pairsChecked = pairsAccepted = 0;
			maskDelta = maskPairsEval = maskPairsAllowed = 0;
		}

		class Timer {
			long time = 0;

			void start() {
				if (doTiming) {
					time -= System.nanoTime();
				}
			}

			void finish() {
				if (doTiming) {
					time += System.nanoTime();
				}
			}

			void reset() {
				time = 0;
			}

			double ms() {
				return time / 1e6;
			}
		}
	}

}