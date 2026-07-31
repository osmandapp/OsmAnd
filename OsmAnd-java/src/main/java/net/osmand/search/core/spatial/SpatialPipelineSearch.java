package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

// DONE Add non maximum results as well... (surplus words +-) -  germany_remstal!

// TODO x1 implement correct mixing alternative masks! Portugal!
// TODO other masks ?fix
// TODO enlarge bbox if failed 
// TODO common words to skip - 14-45, West 31st Road 
// TODO store bbox inside res to not use atoms at all
// TODO introduce mask check into joiner index ?
public class SpatialPipelineSearch {
    
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
		
		List<SpatialObjectsBucket> computedBuckets = new ArrayList<>();
		
		final List<SpatialSearchResultsList> results = new ArrayList<SpatialSearchResultsList>();
		int overallResults = 0;
		
		public SpatialPipelineContext(List<SpatialSearchToken> tokens, SpatialSearchContext ctx) {
			this.tokens = tokens;
			this.searchContext = ctx;
			this.settings = ctx.settings;
			this.stats = ctx.stats;
		}

		public boolean isCancelled() {
			return searchContext.isCancelled();
		}
    }

	
	// REVIEW
	public class SpatialObjectsBucket {
	    public final SpatialObjectsBucket parent;
	    public final SpatialObjectsBucket singleEdgeGroup;
	    public final int depth;
	    public int edgeIndex = -1;
	    // 1. virtual based on masks before materialization
	    // TODO is it better to use MasksGroupInfo?
	    public TLongHashSet potentialMasks = null;

	    // 2. geometric actual
	    public HashSkipTileQuadTree<SpatialPipelineObjectRes> resTree = null;
	    public TLongObjectHashMap<List<SpatialPipelineObjectRes>> resObjectsByMasks = null;

	    // Lazy init
	    private List<SpatialObjectsBucket> children = null;

	    public SpatialObjectsBucket(int edgeIndex) {
	        this.parent = null;
	        this.singleEdgeGroup = null;
	        this.depth = 1;
	        this.edgeIndex = edgeIndex;
	    }

	    public SpatialObjectsBucket(SpatialObjectsBucket parent, SpatialObjectsBucket singleEdgeGroup, 
	                                TLongHashSet potentialMasks, int edgeIndex) {
	        this.parent = parent;
	        this.singleEdgeGroup = singleEdgeGroup;
	        this.depth = parent.depth + 1;
	        this.potentialMasks = potentialMasks;
	        this.edgeIndex = edgeIndex;
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
							// TODO store bbox inside res to not use atoms at all
							int[] bb = obj.mainAtom1.coords.bbox31;
							int[] clippedBBox = new int[] { bb[0], bb[1], bb[2], bb[3] };
							if (obj.mainAtom2 != null) {
								SpatialSearchResultsList.clipBbox(clippedBBox, obj.mainAtom2.coords.bbox31);
							}
							resTree.addObject(obj, clippedBBox, obj.mainAtom1.id);
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
	        if (resObjectsByMasks != null) {
	            return resObjectsByMasks.isEmpty();
	        }
	        return potentialMasks == null || potentialMasks.isEmpty();
	    }

	    public int getMasksCount() {
	        if (resObjectsByMasks != null) {
	            return resObjectsByMasks.size();
	        }
	        return potentialMasks != null ? potentialMasks.size() : 0;
	    }

	    public boolean hasFullCovered(int totalTokens) {
	        if (resObjectsByMasks != null) {
	            long[] keys = resObjectsByMasks.keys();
	            for (int i = 0; i < keys.length; i++) {
	                if (SpatialPipelineObjectRes.countCoveredTokens(keys[i]) == totalTokens) {
	                    return true;
	                }
	            }
	            return false;
	        }

	        if (potentialMasks != null && !potentialMasks.isEmpty()) {
	            long[] masks = potentialMasks.toArray();
	            for (int i = 0; i < masks.length; i++) {
	                if (SpatialPipelineObjectRes.countCoveredTokens(masks[i]) == totalTokens) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    }

	    public long[] getMasks() {
	        if (resObjectsByMasks != null) {
	            return resObjectsByMasks.keys();
	        }
	        return potentialMasks != null ? potentialMasks.toArray() : new long[0];
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

	    public void clear() {
	        if (potentialMasks != null) {
	            potentialMasks.clear();
	            potentialMasks = null;
	        }
	        if (resObjectsByMasks != null) {
	            resObjectsByMasks.clear();
	            resObjectsByMasks = null;
	        }
	        if (resTree != null) {
	            resTree = null;
	        }
	        if (children != null) {
	            children.clear();
	            children = null;
	        }
	    }
	    
	    @Override
	    public String toString() {
	        StringBuilder sb = new StringBuilder("Bucket{count=");
	        sb.append(getMasksCount());
	        if (resObjectsByMasks != null) {
	            int totalObjs = 0;
	            for (List<?> list : resObjectsByMasks.valueCollection()) {
	                if (list != null) {
	                    totalObjs += list.size();
	                }
	            }
	            sb.append(", objs=").append(totalObjs);
	        }
	        sb.append(", masks=[");
	        TLongSet masks = (resObjectsByMasks != null) ? resObjectsByMasks.keySet() : potentialMasks;
	        if (masks != null) {
	            boolean first = true;
	            for (long m : masks.toArray()) {
	                if (!first) {
	                    sb.append(", ");
	                }
	                sb.append("0x").append(Long.toHexString(m));
	                first = false;
	            }
	        }

	        return sb.append("]}").toString();
	    }
	}
	
	
	// REVIEW
	private void pruneChildrenPotentialMasks(SpatialObjectsBucket parent, int totalTokens) {
	    if (parent.getChildren().isEmpty()) {
	        return;
	    }
	    long[] aliveParentMasks = parent.getMasks();
	    if (aliveParentMasks.length == 0) {
	        for (SpatialObjectsBucket child : parent.getChildren()) {
	            clearSubtree(child);
	        }
	        return;
	    }
	    for (SpatialObjectsBucket child : parent.getChildren()) {
	        if (child.isComputed()) {
	            continue;
	        }
	        long[] edgeMasks = child.singleEdgeGroup.getMasks();
	        TLongHashSet updatedChildMasks = new TLongHashSet();
	        for (long pMask : aliveParentMasks) {
	            for (long eMask : edgeMasks) {
	                if (!SpatialPipelineObjectRes.allowed(pMask, eMask)) {
	                    continue;
	                }
	                long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(pMask, eMask, totalTokens);
	                if (combinedMask != pMask) {
	                    updatedChildMasks.add(combinedMask);
	                }
	            }
	        }
	        child.potentialMasks = updatedChildMasks;
	        if (child.isEmpty()) {
	            clearSubtree(child);
	        } else {
	            pruneChildrenPotentialMasks(child, totalTokens);
	        }
	    }
	}
	
	// REVIEW
	private void clearSubtree(SpatialObjectsBucket node) {
	    for (SpatialObjectsBucket child : node.getChildren()) {
	        clearSubtree(child);
	    }
	    node.clear();
	}
	
	// REVIEW
	private void computeGeoCross(SpatialObjectsBucket b, List<SpatialPipelineObjectRes> res) {
	    if (b == null || b.isEmpty()) {
	        return;
	    }
	    if (b.isComputed()) {
			if (res != null) {
				collectCoverageResults(b, res, ctx.tokens.size());
			}
	        return;
	    }
	    if (!b.parent.isComputed()) {
	        computeGeoCross(b.parent, null);
	    }
	    HashSkipTileQuadTree<SpatialPipelineObjectRes> parentTree = b.parent.ensureTreeBuilt();
	    HashSkipTileQuadTree<SpatialPipelineObjectRes> edgeTree = b.singleEdgeGroup.ensureTreeBuilt();

	    HashSkipTileQuadTree<SpatialPipelineObjectRes> resTree = null;
	    TLongObjectHashMap<List<SpatialPipelineObjectRes>> resObjectsByMasks = new TLongObjectHashMap<>();
	    if (parentTree != null && edgeTree != null && !parentTree.isEmpty() && !edgeTree.isEmpty()) {
	    	ctx.metrics.joins++;
	    	ctx.metrics.join.start();
	        final int totalTokens = ctx.tokens.size();
	        HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> joiner =
	                new HashSkipTileQuadTreeJoiner<>(parentTree, edgeTree);
	        ctx.metrics.joinCross += parentTree.getSize() * edgeTree.getSize();
	        joiner.joinAllBuckets((e1, e2) -> {
	        	ctx.metrics.pairsChecked++;
	            SpatialPipelineObjectRes obj1 = e1.obj;
	            SpatialPipelineObjectRes obj2 = e2.obj;
	            if (!SpatialPipelineObjectRes.allowed(obj1.mainMask, obj2.mainMask)) {
	                return;
	            }
	            ctx.metrics.pairsAccepted++;
	            long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(obj1.mainMask, obj2.mainMask, totalTokens);
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
	    ctx.computedBuckets.add(b);
	    b.markComputed(resTree, resObjectsByMasks);
	    if (!b.getChildren().isEmpty()) {
	    	ctx.metrics.prune.start();
	        pruneChildrenPotentialMasks(b, ctx.tokens.size());
	        ctx.metrics.prune.finish();
	    }
	    if (res != null) {
	    	collectCoverageResults(b, res, ctx.tokens.size());
	    }
	}

	// REVIEW
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
	
	// REVIEW
	private static class MasksGroupInfo {
		TLongObjectHashMap<MaskGroupInfo> map = new TLongObjectHashMap<MaskGroupInfo>();

		public long[] keys() {
			return map.keys();
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public void add(long combinedMask, SpatialPipelineObjectRes obj) {
			MaskGroupInfo info = map.get(combinedMask);
			if (info == null) {
				info = new MaskGroupInfo(combinedMask, 0);
				map.put(combinedMask, info);
			}
			info.count++;
			if (obj != null) {
				if (info.objects == null) {
					info.objects = new ArrayList<>();
				}
				info.objects.add(obj);
			}
		}
	}
	
	// REVIEW
	private static class MaskGroupInfo {
//	    final long mask;
	    int count;
	    List<SpatialPipelineObjectRes> objects; 

	    MaskGroupInfo(long mask, int count) {
//	        this.mask = mask;
	        this.count = count;
	    }
	}
	
	// REVIEW
	private void splitGroupInfoToBuckets(SpatialObjectsBucket parent, SpatialObjectsBucket singleEdgeGroup,
	        int edgeIndex, MasksGroupInfo maskMap, List<SpatialObjectsBucket> outputList) {
	    if (maskMap == null || maskMap.isEmpty()) {
	        return;
	    }
	    SpatialObjectsBucket fullCoveredBucket = null;
	    SpatialObjectsBucket combinedRareBucket = null;
	    long[] masks = maskMap.keys();
	    int totalTokens = ctx.tokens.size();
	    for (int i = 0; i < masks.length; i++) {
	        long mask = masks[i];
	        MaskGroupInfo info = maskMap.map.get(mask);
	        boolean isFullCovered = (SpatialPipelineObjectRes.countCoveredTokens(mask) == totalTokens);
	        if (isFullCovered) {
	            if (fullCoveredBucket == null) {
	                fullCoveredBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	            }
	            populateBucketMask(fullCoveredBucket, mask, info);
	            if (fullCoveredBucket.getMasksCount() >= ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS) {
	                finalizeAndAddBucket(parent, fullCoveredBucket, outputList);
	                fullCoveredBucket = null;
	            }
	            continue;
	        }
	        if (info.count >= ctx.settings.PIPELINE_FREQUENT_OBJECTS_THRESHOLD) {
	            SpatialObjectsBucket freqBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	            populateBucketMask(freqBucket, mask, info);
	            finalizeAndAddBucket(parent, freqBucket, outputList);
	            continue;
	        }

	        if (combinedRareBucket == null) {
	            combinedRareBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	        }
	        populateBucketMask(combinedRareBucket, mask, info);
	        if (combinedRareBucket.getMasksCount() >= ctx.settings.PIPELINE_MAX_VIRTUAL_MASKS) {
	            finalizeAndAddBucket(parent, combinedRareBucket, outputList);
	            combinedRareBucket = null;
	        }
	    }
	    if (fullCoveredBucket != null && !fullCoveredBucket.isEmpty()) {
	        finalizeAndAddBucket(parent, fullCoveredBucket, outputList);
	    }
	    if (combinedRareBucket != null && !combinedRareBucket.isEmpty()) {
	        finalizeAndAddBucket(parent, combinedRareBucket, outputList);
	    }
	}
	
	// REVIEW
	private SpatialObjectsBucket createBucketNode(SpatialObjectsBucket parent, SpatialObjectsBucket singleEdgeGroup,
	        int edgeIndex) {
	    if (parent == null) {
	        SpatialObjectsBucket b = new SpatialObjectsBucket(edgeIndex);
	        b.potentialMasks = new TLongHashSet();
	        b.resObjectsByMasks = new TLongObjectHashMap<>();
	        return b;
	    } else {
	        return new SpatialObjectsBucket(parent, singleEdgeGroup, new TLongHashSet(), edgeIndex);
	    }
	}

	// REVIEW
	private void populateBucketMask(SpatialObjectsBucket b, long mask, MaskGroupInfo info) {
	    if (b.potentialMasks == null) {
	        b.potentialMasks = new TLongHashSet();
	    }
	    b.potentialMasks.add(mask);
	    if (info.objects != null && !info.objects.isEmpty()) {
	        if (b.resObjectsByMasks == null) {
	            b.resObjectsByMasks = new TLongObjectHashMap<>();
	        }
	        List<SpatialPipelineObjectRes> list = b.resObjectsByMasks.get(mask);
	        if (list == null) {
	            list = new ArrayList<>(info.objects.size());
	            b.resObjectsByMasks.put(mask, list);
	        }
	        list.addAll(info.objects);
	    }
	}
	
	// REVIEW
	private void finalizeAndAddBucket(SpatialObjectsBucket parent, SpatialObjectsBucket bucket,
			List<SpatialObjectsBucket> outputList) {
		if (parent != null) {
			parent.addChild(bucket);
		}
		outputList.add(bucket);
	}

	// REVIEW
	private void computeMasksCross(SpatialObjectsBucket b, List<SpatialObjectsBucket> edges,
			List<SpatialObjectsBucket> nextLevel, SpatialPipelineContext prep) {
		if (b == null || b.isEmpty()) {
			return;
		}
		long[] parentMasks = b.getMasks();
		if (parentMasks.length == 0) {
			return;
		}
		final int totalTokens = prep.tokens.size();
		for (int i = 0; i < edges.size(); i++) {
			if (i < b.edgeIndex) {
				continue;
			}
			SpatialObjectsBucket edge = edges.get(i);
			long[] edgeMasks = edge.getMasks();
			if (edgeMasks.length == 0) {
				continue;
			}
			MasksGroupInfo combinedMaskMap = new MasksGroupInfo();
			for (long pMask : parentMasks) {
				for (long eMask : edgeMasks) {
					ctx.metrics.maskPairsEval++;
					if (!SpatialPipelineObjectRes.allowed(pMask, eMask)) {
						continue;
					}
					ctx.metrics.maskPairsAllowed++;
					long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(pMask, eMask, totalTokens);
					if (combinedMask == pMask) {
						throw new IllegalStateException();
					}
					combinedMaskMap.add(combinedMask, null);
				}
			}
			if (combinedMaskMap.isEmpty()) {
				continue;
			}
			splitGroupInfoToBuckets(b, edge, i, combinedMaskMap, nextLevel);
		}
	}
	
	// REVIEW
	private SpatialPipelineContext prepareInitialBuckets() {
		int totalTokens = ctx.tokens.size();
		// combine & merge by tokens
		for (int tokenIdx = 0; tokenIdx < totalTokens; tokenIdx++) {
			SpatialSearchToken token = ctx.tokens.get(tokenIdx);
			TIntHashSet deleted = token.getDeletedAtoms();
			for (NameIndexAtom atom : token.atoms) {
				if (deleted.contains(atom.indexInToken)) {
					continue;
				}
				SpatialPipelineObjectRes existing = ctx.objectsById.get(atom.id);
				if (existing != null) {
					existing.mergeSame(atom, tokenIdx);
				} else {
					SpatialPipelineObjectRes obj = new SpatialPipelineObjectRes(totalTokens, atom, tokenIdx);
					ctx.objectsById.put(atom.id, obj);
				}
			}
		}
		// combine by masks
	    MasksGroupInfo maskMap = new MasksGroupInfo();
	    for (SpatialPipelineObjectRes obj : ctx.objectsById.valueCollection()) {
	        long mask = obj.mainMask;
	        maskMap.add(mask, obj);
	    }
	    // split into groups
	    List<SpatialObjectsBucket> buckets = new ArrayList<>();
	    splitGroupInfoToBuckets(null, null, 0, maskMap, buckets);
	    // ???
	    for (int i = 0; i < buckets.size(); i++) {
	    	SpatialObjectsBucket bucket = buckets.get(i);
	        bucket.edgeIndex = i;
	        ctx.computedBuckets.add(bucket);
	        bucket.markComputed(null, bucket.resObjectsByMasks);
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
		int tsize = ctx.tokens.size();
		for (SpatialSearchResult r : res) {
			if (!r.isPoiCategory() && r.surplusWords + r.matchedTokens() == tsize) {
				nonCategoryRes++;
			}
		}
		if (res.size() > 0) {
			ctx.results.add(stageList);
			ctx.overallResults += res.size();
		}
		if (ctx.stats.printLogs) {
			String compString = tokens.size() == ctx.tokens.size() ? "complete"
					: ("partial-" + (ctx.tokens.size() - tokens.size()));
			System.out.printf("PIPELINE %d LOAD RESULTS (%.1f ms): %d %s results.\n", stage,
					(System.nanoTime() - time) / 1e6, compString, nonCategoryRes);
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
	
	private SpatialSearchResultsList createResultList(List<SpatialSearchToken> tokens, List<SpatialPipelineObjectRes> r) {
		SpatialSearchResultsList singleResults = new SpatialSearchResultsList(tokens);
		for (SpatialPipelineObjectRes res : r) {
			int nonNull = 0; 
			singleResults.tileIds.add(res.mainAtom1.coords.bboxTileId);
			for (int i = 0; i < res.atoms.length; i++) {
				if (res.atoms[i] != null) {
					nonNull++;
					singleResults.linearResults.add(res.atoms[i]);
				}
			}
			if(nonNull != tokens.size()) {
				throw new IllegalStateException(String.format("%s - %d != %d", res, nonNull, tokens.size()));
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
		List<SpatialObjectsBucket> currentLevel = ctx.initBuckets;
		while (depth < ctx.settings.PIPELINE_MAX_STEPS && !currentLevel.isEmpty()) {
			ctx.metrics.resetDepth();
			if (depth > 0) {
				ctx.metrics.masksComputeTimer.start();
				List<SpatialObjectsBucket> nextLevel = new ArrayList<>();
				for (SpatialObjectsBucket b : currentLevel) {
					computeMasksCross(b, ctx.initBuckets, nextLevel, ctx);
				}
				ctx.metrics.logMasksCross(ctx, currentLevel, nextLevel, depth);
				currentLevel = nextLevel;
			}
			ctx.metrics.geoComputeTimer.start();
			List<SpatialPipelineObjectRes> res = new ArrayList<>();
			int fullCoverageCount = 0;
			for (SpatialObjectsBucket b : currentLevel) {
				if (b.hasFullCovered(tokensSize) || b.parent == null) {
					fullCoverageCount++;
					computeGeoCross(b, res);
				}
			}
			ctx.metrics.logGeoCross(ctx, currentLevel, depth, fullCoverageCount, res.size());
			if (validateResultsAndFinish(res, depth, ctx.tokens)) {
				return ctx.results;
			}
			depth++;
		}
		if (ctx.overallResults == 0 && tokensSize > 1) {
			calculatePartialCoverage(depth);
		}
		return ctx.results;
	}

	private List<SpatialSearchResultsList> calculatePartialCoverage(int depth) throws IOException {
		int tokensSize = ctx.tokens.size();
		for (int targetTokens = tokensSize - 1; targetTokens >= 1; targetTokens--, depth++) {
		    List<SpatialPipelineObjectRes> partialRes = new ArrayList<>();
		    for (SpatialObjectsBucket b : ctx.computedBuckets) {
		        collectCoverageResults(b, partialRes, targetTokens);
		    }
			if (partialRes.isEmpty()) {
				continue;
			}
			TLongObjectHashMap<List<SpatialPipelineObjectRes>> groupByTokens = new TLongObjectHashMap<List<SpatialPipelineObjectRes>>();
			for (SpatialPipelineObjectRes r : partialRes) {
				long mask = r.calculateMaskByTokens();
				if (!groupByTokens.contains(mask)) {
					groupByTokens.put(mask, new ArrayList<>());
				}
				groupByTokens.get(mask).add(r);
			}
			TLongObjectIterator<List<SpatialPipelineObjectRes>> it = groupByTokens.iterator();
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
				if (validateResultsAndFinish(res, depth, lst)) {
					return ctx.results;
				}
			}
		}
		return ctx.results;
	}
	
	private static class PipelineMetrics {
		boolean doTiming = true;
		Timer prepareTimer = new Timer();
		Timer masksComputeTimer = new Timer();
		long maskPairsEval, maskPairsAllowed;
		
		Timer geoComputeTimer = new Timer();

		Timer treeBuild = new Timer();
		int tree = 0;
		Timer join = new Timer();
		int joins = 0;
		long joinCross = 0;
		
		Timer prune = new Timer();
		long pairsChecked, pairsAccepted;
		
	    

	    int countMasks(List<SpatialObjectsBucket> list) {
		    int sum = 0;
		    for (SpatialObjectsBucket b : list) sum += b.getMasksCount();
		    return sum;
		}
	    

		void logGeoCross(SpatialPipelineContext ctx, List<SpatialObjectsBucket> buckets, int level, int fcCount,
				int resCount) {
			geoComputeTimer.finish();
			if (ctx.stats.printLogs) {
				String s = String.format(
						"Tree Built %,d (%.1f ms) | Joins %,d (%.1f ms) | Cross objs %,dK, check %,d, accept %,d | Prune (%.1f ms)",
						tree, treeBuild.ms(), joins, join.ms(), joinCross / 1024, pairsChecked, pairsAccepted, prune.ms());
				System.out.printf(
						"PIPELINE %d GEO CROSS (%.1f ms) - %,d RESULTS | %,d (%,d full) Buckets | %s\n",
						level, geoComputeTimer.ms(), resCount, buckets.size(), fcCount, s);
			}
		}
		
		
	    public void logMasksCross(SpatialPipelineContext ctx, List<SpatialObjectsBucket> currentLevel,
				List<SpatialObjectsBucket> nextLevel, int stage) {
	    	ctx.metrics.masksComputeTimer.finish();
			if (ctx.stats.printLogs) {
				System.out.printf("PIPELINE %d MASKS CROSS (%.1f ms) | %,d -> %,d Buckets | %,d -> %,d Masks | Masks Cross %,d eval -> %,d accept\n",
						stage, masksComputeTimer.ms(), currentLevel.size(), nextLevel.size(), countMasks(currentLevel),
						countMasks(nextLevel), maskPairsEval, maskPairsAllowed);
			}
		}

		public void logPrepareTime(SpatialPipelineContext ctx) {
	    	SpatialPipelineStats.printTree(ctx);
	    	prepareTimer.finish();
			if (ctx.stats.printLogs) {
				System.out.printf("PIPELINE 0 PREPARE (%.1f ms) %,d Buckets | %,d masks | %,d objects \n",
						prepareTimer.ms(), ctx.initBuckets.size(), countMasks(ctx.initBuckets), ctx.objectsById.size());
			}
		}
	    
		void resetDepth() {
			treeBuild.reset();
			joins = 0;
			tree = 0;
			join.reset();
			prune.reset();
			
			geoComputeTimer.reset();
			masksComputeTimer.reset();
			joinCross = pairsChecked = pairsAccepted = 0;
			maskPairsEval = maskPairsAllowed = 0;
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