package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;

// TODO ---------------
// TODO Add non maximum results as well... (surplus words +-) -  germany_langestrasse
// TODO other masks ?
// TODO common words to skip
// TODO x2 speed up by using flags
// TODO x1 implement correct mixing alternative masks!
// TODO introduce mask check into joiner index ?
public class SpatialPipelineSearch {
    
    private final SpatialSearchContext ctx;
    
    public static int FREQUENT_OBJECTS_THRESHOLD = 5000;
    public static int MAX_VIRTUAL_MASKS = 10;
    public static int MAX_STEPS = 4; // 1 - fully covered, 2 - 1 intersecction ,...
    
	public static int EXCLUDE_MASKS = 8000; // speed up
	public static boolean CHECK_EXCLUDED = false;

    public SpatialPipelineSearch(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }

    private static class MasksStats {
    	TLongObjectHashMap<Integer> masks = new TLongObjectHashMap<Integer>();
    	public final int intersections;

		public MasksStats(int intersections) {
			this.intersections = intersections;
		}

		int count(SpatialPipelineObjectRes obj) {
			Integer cnt = masks.get(obj.mainMask);
			if (cnt == null) {
				cnt = 1;
			} else {
				cnt++;
			}
			masks.put(obj.mainMask, cnt);
			return cnt;
		}
    }
    
   
    
    public static class SpatialPipelineResults {
    	public final List<SpatialSearchToken> tokens;
		public SpatialPipelineResults(List<SpatialSearchToken> tokens) {
			this.tokens = tokens;
		}

		// stage 1
		public final TLongObjectHashMap<SpatialPipelineObjectRes> objectsById = new TLongObjectHashMap<>();
		public final TLongObjectHashMap<List<SpatialPipelineObjectRes>> excludedMasks = new TLongObjectHashMap<List<SpatialPipelineObjectRes>>();
		
		public final List<MasksStats> masksStats = new ArrayList<>();
        public final HashSkipTileQuadTree<SpatialPipelineObjectRes> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<SpatialPipelineObjectRes> areaObjectsTree = new HashSkipTileQuadTree<>();
		// stage 2, 3+        
		public final List<HashSkipTileQuadTree<SpatialPipelineObjectRes>> pairsTree = new ArrayList<>();
		
		public final List<SpatialSearchResultsList> combinations = new ArrayList<SpatialSearchResultsList>();
        
    }


    private SpatialPipelineResults prepare(List<SpatialSearchToken> tokens) {
    	if (tokens.size() > SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS) {
			tokens = tokens.subList(0, SpatialPipelineObjectRes.MAX_SUPPORTED_TOKENS);
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
				SpatialPipelineObjectRes existing = prep.objectsById.get(atom.id);
				if (existing != null) {
					existing.mergeSame(atom, tokenIdx);
				} else {
					SpatialPipelineObjectRes obj = new SpatialPipelineObjectRes(totalTokens, atom, tokenIdx);
					if(tokenIdx == 0 && atom.isGeoArea()) {
						System.out.println(atom + " " + SpatialPipelineObjectRes.formatMaskTokens(obj.mainMask, tokens));
					}
					prep.objectsById.put(atom.id, obj);
				}
			}
		}
		// calculate excluded masks
		MasksStats masksStats = new MasksStats(1);
		for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
			masksStats.count(obj);
		}
		prep.masksStats.add(masksStats);
		
		for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
			Integer cnt = masksStats.masks.get(obj.mainMask);
			if (cnt > EXCLUDE_MASKS) {
				List<SpatialPipelineObjectRes> elst = prep.excludedMasks.get(obj.mainMask);
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
			List<SpatialPipelineObjectRes> preResults, int stage, long ptime) throws IOException {
		
		long time = System.nanoTime();
		if (ctx.stats.printLogs) {
			String intersections = "";
			if (intStats != null) {
				intersections = String.format(" (cross %,d, partial %,d, full %,d)", intStats[0], intStats[1] - intStats[2],
						intStats[2]);
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
		int[] stops = ctx.settings.MAX_PIPELINE_RES_TO_STOP;
		if (stops.length > 0 && nonCategoryRes > stops[Math.min(stops.length, stage + 1) - 1]) {
			return true;
		}
		return false;
	}

    // =========================================================================
    // Execution Engine with Mode Control v2
    // =========================================================================
	public class SpatialObjectsBucket {
	    public final SpatialObjectsBucket parent;
	    public final SpatialObjectsBucket singleEdgeGroup;
	    public final int depth;
	    public int edgeIndex = -1;
	    // 1. virtual based on masks before materialization
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
	        	long t0 = System.nanoTime();
	            resTree = new HashSkipTileQuadTree<>();
	            long[] masks = resObjectsByMasks.keys();
	            for (int i = 0; i < masks.length; i++) {
	                List<SpatialPipelineObjectRes> list = resObjectsByMasks.get(masks[i]);
	                if (list != null) {
	                    for (int j = 0; j < list.size(); j++) {
	                        SpatialPipelineObjectRes obj = list.get(j);
	                        int[] bb = obj.mainAtom1.coords.bbox31;
	                        int[] clippedBBox = new int[] { bb[0], bb[1], bb[2], bb[3] };
	                        if (obj.mainAtom2 != null) {
	                            SpatialSearchResultsList.clipBbox(clippedBBox, obj.mainAtom2.coords.bbox31);
	                        }
	                        resTree.addObject(obj, clippedBBox, obj.mainAtom1.id);
	                    }
	                }
	            }
	            metrics.treeBuildNanos += (System.nanoTime() - t0);
	        }
	        if (resTree != null && !resTree.isEmpty() && !resTree.isBuilt()) {
	            resTree.build();
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
	
	private static class PipelineMetrics {
	    long treeBuildNanos, joinNanos, pruneNanos;
	    long pairsChecked, pairsAccepted;
	    long maskPairsEval, maskPairsAllowed;
	    void resetLevel() {
	        treeBuildNanos = joinNanos = pruneNanos = 0;
	        pairsChecked = pairsAccepted = 0;
	    }
	    void resetSplit() {
	        maskPairsEval = maskPairsAllowed = 0;
	    }
	    void logLevel(int level, int buckets, int totalMasks, double computeMs, int fcCount, int matCount, int resCount) {
	        System.out.printf("[D=%d] Buckets: %d | Masks: %,d | Comp: %.1fms (Tree: %.1fms, Join: %.1fms [%,d/%,d], Prune: %.1fms) | Mat: %d | Res: %,d\n",
	                level, buckets, totalMasks, computeMs, 
	                treeBuildNanos / 1e6, joinNanos / 1e6, pairsAccepted, pairsChecked, pruneNanos / 1e6, 
	                matCount, resCount);
	    }
	    void logSplit(double splitMs, int nextBuckets, int nextMasks) {
	        System.out.printf("      Split: %.1fms | Next Buckets: %d | Next Masks: %,d | Mask Pairs: %,d/%,d\n",
	                splitMs, nextBuckets, nextMasks, maskPairsAllowed, maskPairsEval);
	    }
	}

	private final PipelineMetrics metrics = new PipelineMetrics();
	private static int countMasks(List<SpatialObjectsBucket> list) {
	    int sum = 0;
	    for (SpatialObjectsBucket b : list) sum += b.getMasksCount();
	    return sum;
	}
	
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
	
	private void clearSubtree(SpatialObjectsBucket node) {
	    for (SpatialObjectsBucket child : node.getChildren()) {
	        clearSubtree(child);
	    }
	    node.clear();
	}
	
	private void compute(SpatialObjectsBucket b, List<SpatialPipelineObjectRes> res, SpatialPipelineResults prep) {
	    if (b == null || b.isEmpty()) {
	        return;
	    }
	    if (b.isComputed()) {
	        collectFullCoverageResults(b, res, prep.tokens.size());
	        return;
	    }
	    if (b.parent == null) {
	        b.markComputed(null, b.resObjectsByMasks);
	        collectFullCoverageResults(b, res, prep.tokens.size());
	        return;
	    }
	    if (!b.parent.isComputed()) {
	        compute(b.parent, res, prep);
	    }
	    if (!b.singleEdgeGroup.isComputed()) {
	        compute(b.singleEdgeGroup, res, prep);
	    }
	    HashSkipTileQuadTree<SpatialPipelineObjectRes> parentTree = b.parent.ensureTreeBuilt();
	    HashSkipTileQuadTree<SpatialPipelineObjectRes> edgeTree = b.singleEdgeGroup.ensureTreeBuilt();

	    HashSkipTileQuadTree<SpatialPipelineObjectRes> resTree = null;
	    TLongObjectHashMap<List<SpatialPipelineObjectRes>> resObjectsByMasks = new TLongObjectHashMap<>();

	    if (parentTree != null && edgeTree != null && !parentTree.isEmpty() && !edgeTree.isEmpty()) {
	    	long tJoin = System.nanoTime();
	        final int totalTokens = prep.tokens.size();
	        HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> joiner =
	                new HashSkipTileQuadTreeJoiner<>(parentTree, edgeTree);

	        joiner.joinAllBuckets((e1, e2) -> {
	        	metrics.pairsChecked++;
	            SpatialPipelineObjectRes obj1 = e1.obj;
	            SpatialPipelineObjectRes obj2 = e2.obj;
	            if (!SpatialPipelineObjectRes.allowed(obj1.mainMask, obj2.mainMask)) {
	                return;
	            }
	            metrics.pairsAccepted++;
	            long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(obj1.mainMask, obj2.mainMask, totalTokens);
	            SpatialPipelineObjectRes combinedObj = new SpatialPipelineObjectRes(combinedMask, obj1, obj2);
	            List<SpatialPipelineObjectRes> list = resObjectsByMasks.get(combinedMask);
	            if (list == null) {
	                list = new ArrayList<>();
	                resObjectsByMasks.put(combinedMask, list);
	            }
	            list.add(combinedObj);
	        });
	        metrics.joinNanos += (System.nanoTime() - tJoin); 
	    }
	    b.markComputed(resTree, resObjectsByMasks);
	    if (!b.getChildren().isEmpty()) {
	    	long tPrune = System.nanoTime();
	        pruneChildrenPotentialMasks(b, prep.tokens.size());
	        metrics.pruneNanos += (System.nanoTime() - tPrune);
	    }
	    collectFullCoverageResults(b, res, prep.tokens.size());
	}

	private void collectFullCoverageResults(SpatialObjectsBucket b, List<SpatialPipelineObjectRes> res, int totalTokens) {
	    if (b == null || b.resObjectsByMasks == null || b.resObjectsByMasks.isEmpty()) {
	        return;
	    }
	    long[] masks = b.resObjectsByMasks.keys();
	    for (int i = 0; i < masks.length; i++) {
	        long mask = masks[i];
	        if (SpatialPipelineObjectRes.countCoveredTokens(mask) == totalTokens) {
	            List<SpatialPipelineObjectRes> list = b.resObjectsByMasks.get(mask);
	            if (list != null && !list.isEmpty()) {
	                res.addAll(list);
	            }
	        }
	    }
	}
	
	private static class MaskGroupInfo {
//	    final long mask;
	    int count;
	    List<SpatialPipelineObjectRes> objects; 

	    MaskGroupInfo(long mask, int count, List<SpatialPipelineObjectRes> objects) {
//	        this.mask = mask;
	        this.count = count;
	        this.objects = objects;
	    }
	}
	
	private void splitGroupInfoToBuckets(SpatialObjectsBucket parent, SpatialObjectsBucket singleEdgeGroup,
	        int edgeIndex, TLongObjectHashMap<MaskGroupInfo> maskMap, int totalTokens,
	        List<SpatialObjectsBucket> outputList) {
	    if (maskMap == null || maskMap.isEmpty()) {
	        return;
	    }
	    SpatialObjectsBucket fullCoveredBucket = null;
	    SpatialObjectsBucket combinedRareBucket = null;
	    long[] masks = maskMap.keys();
	    for (int i = 0; i < masks.length; i++) {
	        long mask = masks[i];
	        MaskGroupInfo info = maskMap.get(mask);
	        boolean isFullCovered = (SpatialPipelineObjectRes.countCoveredTokens(mask) == totalTokens);
	        if (isFullCovered) {
	            if (fullCoveredBucket == null) {
	                fullCoveredBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	            }
	            populateBucketMask(fullCoveredBucket, mask, info);
	            if (fullCoveredBucket.getMasksCount() >= MAX_VIRTUAL_MASKS) {
	                finalizeAndAddBucket(parent, fullCoveredBucket, outputList);
	                fullCoveredBucket = null;
	            }
	            continue;
	        }
	        if (info.count >= FREQUENT_OBJECTS_THRESHOLD) {
	            SpatialObjectsBucket freqBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	            populateBucketMask(freqBucket, mask, info);
	            finalizeAndAddBucket(parent, freqBucket, outputList);
	            continue;
	        }

	        if (combinedRareBucket == null) {
	            combinedRareBucket = createBucketNode(parent, singleEdgeGroup, edgeIndex);
	        }
	        populateBucketMask(combinedRareBucket, mask, info);
	        if (combinedRareBucket.getMasksCount() >= MAX_VIRTUAL_MASKS) {
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

	private void finalizeAndAddBucket(SpatialObjectsBucket parent, SpatialObjectsBucket bucket,
			List<SpatialObjectsBucket> outputList) {
		if (parent != null) {
			parent.addChild(bucket);
		}
		outputList.add(bucket);
	}

	
	private void virtualComputeSplit(SpatialObjectsBucket b, List<SpatialObjectsBucket> edges,
			List<SpatialObjectsBucket> nextLevel, SpatialPipelineResults prep) {
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
			TLongObjectHashMap<MaskGroupInfo> combinedMaskMap = new TLongObjectHashMap<>();
			for (long pMask : parentMasks) {
				for (long eMask : edgeMasks) {
					metrics.maskPairsEval++;
					if (!SpatialPipelineObjectRes.allowed(pMask, eMask)) {
						continue;
					}
					metrics.maskPairsAllowed++;
					long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(pMask, eMask, totalTokens);
					if (combinedMask == pMask) {
						continue; 
					}
					MaskGroupInfo info = combinedMaskMap.get(combinedMask);
					if (info == null) {
						info = new MaskGroupInfo(combinedMask, 0, null);
						combinedMaskMap.put(combinedMask, info);
					}
					info.count++; 
				}
			}
			if (combinedMaskMap.isEmpty()) {
				continue;
			}
			splitGroupInfoToBuckets(b, edge, i, combinedMaskMap, totalTokens, nextLevel);
		}
	}
	private List<SpatialObjectsBucket> createInitialEdgeBuckets(SpatialPipelineResults prep) {
	    TLongObjectHashMap<MaskGroupInfo> maskMap = new TLongObjectHashMap<>();
	    for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
	        long mask = obj.mainMask;
	        MaskGroupInfo info = maskMap.get(mask);
	        if (info == null) {
	            info = new MaskGroupInfo(mask, 0, new ArrayList<>());
	            maskMap.put(mask, info);
	        }
	        info.objects.add(obj);
	        info.count++;
	    }

	    List<SpatialObjectsBucket> edges = new ArrayList<>();
	    splitGroupInfoToBuckets(null, null, 0, maskMap, prep.tokens.size(), edges);
	    for (int i = 0; i < edges.size(); i++) {
	        edges.get(i).edgeIndex = i;
	    }

	    return edges;
	}

	
	public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) throws IOException {
	    if (tokens == null || tokens.isEmpty()) return Collections.emptyList();
	    SpatialPipelineResults prep = prepare(tokens);
	    int tokensSize = prep.tokens.size();
	    SpatialPipelineStats.printTree(prep);
	    List<SpatialObjectsBucket> edges = createInitialEdgeBuckets(prep);
	    System.out.println("INITIAL - ");
	    for(SpatialObjectsBucket e : edges) {
	    	System.out.println("\t" + e);
	    }
	    List<SpatialObjectsBucket> currentLevel = new ArrayList<>(edges);
	    int level = 0;
	    while (level < MAX_STEPS && !currentLevel.isEmpty()) {
	        metrics.resetLevel();
	        long levelStart = System.nanoTime();
	        List<SpatialPipelineObjectRes> res = new ArrayList<>();
	        int fcCount = 0, matCount = 0;
	        for (SpatialObjectsBucket b : currentLevel) {
	            if (b.hasFullCovered(tokensSize)) {
	                fcCount++;
	                boolean wasComputed = b.isComputed();
	                compute(b, res, prep);
	                if (!wasComputed && b.isComputed()) matCount++;
	            }
	        }
	        if (ctx.stats.printLogs) {
	            metrics.logLevel(level + 1, currentLevel.size(), countMasks(currentLevel), 
	                    (System.nanoTime() - levelStart) / 1e6, fcCount, matCount, res.size());
	        }
	        if (validateStageAndFinish(prep, null, res, level, levelStart)) {
	            return prep.combinations;
	        }
	        System.out.println("LEVEL PREPARE " + level);
	        metrics.resetSplit();
	        long splitStart = System.nanoTime();
	        List<SpatialObjectsBucket> nextLevel = new ArrayList<>();
	        for (SpatialObjectsBucket b : currentLevel) {
	            virtualComputeSplit(b, edges, nextLevel, prep);
	        }

	        if (ctx.stats.printLogs) {
	            metrics.logSplit((System.nanoTime() - splitStart) / 1e6, nextLevel.size(), countMasks(nextLevel));
	        }
	        level++;
	        currentLevel = nextLevel;
	    }
	    return prep.combinations;
	}
	



	// =========================================================================
    // Execution Engine with Mode Control
    // =========================================================================
	public List<SpatialSearchResultsList> runPipeline1(List<SpatialSearchToken> tokens) throws IOException {
		if (tokens == null || tokens.isEmpty()) {
			return Collections.emptyList();
		}
		final int tokensSize = tokens.size();
		long time = System.nanoTime();

		// STEP 0 PREPARE
		int stage = 0;
		SpatialPipelineResults prep = prepare(tokens);
		if (ctx.stats.printLogs) {
			System.out.printf("PIPELINE PREPARE tokens (%.1f ms): %,d objects\n", (System.nanoTime() - time) / 1e6, 
					prep.allObjectsTree.getSize());
		}
		time = System.nanoTime();
		if (stage++ >= MAX_STEPS || ctx.isCancelled()) {
			return prep.combinations;
		}
//		SpatialStagePipelineStats.evaluateMaskIntersections(prep);
		SpatialPipelineStats.printTree(prep);
//		SpatialStagePipelineStats.printTokenTree(prep);
		
		// STEP 1
		List<SpatialPipelineObjectRes> singleResults = new ArrayList<>();
		for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
			if (SpatialPipelineObjectRes.countCoveredTokens(obj.mainMask) == tokensSize) {
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
		
		// STEP 2: Spatial Join Pairs
		HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> selfJoiner = new HashSkipTileQuadTreeJoiner<>(
				prep.allObjectsTree, prep.allObjectsTree);// prep.allObjectsTree);
		
		boolean exit = join(prep, stage, selfJoiner, time);
		if (stage++ >= MAX_STEPS || ctx.isCancelled() || exit) {
			return prep.combinations;
		}
		time = System.nanoTime();
		
		// STEP 3: Spatial Join Pairs
		for (; stage <= MAX_STEPS && !ctx.isCancelled() && !exit; stage++) {
			HashSkipTileQuadTree<SpatialPipelineObjectRes> lastTree = prep.pairsTree.get(prep.pairsTree.size() - 1);
			if (lastTree.isEmpty()) {
				break;
			}
			lastTree.build();
			HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> joiner = new HashSkipTileQuadTreeJoiner<>(
					lastTree, prep.areaObjectsTree);
			exit = join(prep, stage, joiner, time);
			time = System.nanoTime();
		}
		// check potential missing results
		if (CHECK_EXCLUDED) {
			checkExcluded(tokensSize, prep);
		}

		return prep.combinations;
	}


	// To be deleted
	private void checkExcluded(final int tokensSize, SpatialPipelineResults prep) throws IOException {
		long[] excl = prep.excludedMasks.keys();
		System.out.println("Excluded masks: " + excl.length);
		MasksStats baseMasksStats = prep.masksStats.get(0);
		long time = System.nanoTime();
		for (int stage = 1; stage < MAX_STEPS && stage < prep.masksStats.size(); stage++) {
			for (int i = 0; i < prep.masksStats.size(); i++) {
				MasksStats masksStats = prep.masksStats.get(i);
				if (masksStats.intersections != stage) {
					continue;
				}
				HashSkipTileQuadTree<SpatialPipelineObjectRes> partialTree = i == 0 ? prep.allObjectsTree
						: prep.pairsTree.get(i - 1);

				TLongHashSet found = new TLongHashSet();
				for (int k = 0; k < excl.length; k++) {
					long maskExcl = excl[k];
					for (long m : masksStats.masks.keys()) {
						if (!SpatialPipelineObjectRes.allowed(m, maskExcl)) {
							continue;
						}
						long combined = SpatialPipelineObjectRes.combine2BitMasks(m, maskExcl, tokensSize);
						if (SpatialPipelineObjectRes.countCoveredTokens(combined) == tokensSize) {
							Integer c1 = baseMasksStats.masks.get(maskExcl);
							Integer c2 = masksStats.masks.get(m);
							System.out.printf("Potential results %d intersections - missing %s (%,d) x %s (%,d < %,d ) = %,d \n",
									stage + 1, SpatialPipelineObjectRes.formatMaskTokens(maskExcl, prep.tokens), c1,
									SpatialPipelineObjectRes.formatMaskTokens(m, prep.tokens), c2, partialTree.getSize(), c1 * c2);
							found.add(maskExcl);
						}
					}
				}
				if (found.size() == 0) {
					continue;
				}
				HashSkipTileQuadTree<SpatialPipelineObjectRes> exclTree = new HashSkipTileQuadTree<>();
				for (long exclMask : found.toArray()) {
					for (SpatialPipelineObjectRes r : prep.excludedMasks.get(exclMask)) {
						exclTree.addObject(r, r.mainAtom1.coords.bbox31, r.mainAtom1.id);
					}
				}
				exclTree.build();

				partialTree.build();
				HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> tailJoiner = new HashSkipTileQuadTreeJoiner<>(
						partialTree, exclTree);
				boolean exit = join(prep, stage + 1, tailJoiner, time);
				if (ctx.isCancelled() || exit) {
					return;
				}
				time = System.nanoTime();
			}
		}
	}


	private boolean join(SpatialPipelineResults prep, int stage,
			HashSkipTileQuadTreeJoiner<SpatialPipelineObjectRes, SpatialPipelineObjectRes> joiner, long time) throws IOException {
		List<SpatialPipelineObjectRes> pairResults = new ArrayList<>();
		final int tokensSize = prep.tokens.size();
		HashSkipTileQuadTree<SpatialPipelineObjectRes> pairsTree = new HashSkipTileQuadTree<>();
		prep.pairsTree.add(pairsTree);
		final MasksStats ms = new MasksStats(stage);
		prep.masksStats.add(ms);
		int[] itStats = new int[] {0, 0, 0};
		if (ctx.stats.printLogs) {
			System.out.printf("PIPELINE STAGE %d INTERSECT - %,d x %,d tree...\n", stage, 
					joiner.getTree1().getSize(), joiner.getTree2().getSize());
		}
		joiner.joinAllBuckets((e1, e2) -> {
			itStats[0]++;
			if (!SpatialPipelineObjectRes.allowed(e1.obj.mainMask, e2.obj.mainMask)) {
				return;
			}
			// TODO check preformance this is covered by mask
//			if (e1.objId <= e2.objId) {
//				return; // skip 1 side pairs by id
//			} else 
			itStats[1]++;
			long combinedMask = SpatialPipelineObjectRes.combine2BitMasks(e1.obj.mainMask, e2.obj.mainMask, tokensSize);
			SpatialPipelineObjectRes res = new SpatialPipelineObjectRes(combinedMask, e1.obj, e2.obj);
			ms.count(res);
			if (SpatialPipelineObjectRes.countCoveredTokens(combinedMask) == tokensSize) {
				// TODO add combinations from combined mask
				itStats[2]++;
				pairResults.add(res);
				return;
			}
			int[] bb = res.mainAtom1.coords.bbox31;
			int[] clippedBBox = new int[] { bb[0], bb[1], bb[2], bb[3] };
			SpatialSearchResultsList.clipBbox(clippedBBox, res.mainAtom2.coords.bbox31);
			pairsTree.addObject(res, clippedBBox, -1);
		});

		if (validateStageAndFinish(prep, itStats, pairResults, stage, time)) {
			return true;
		}
		return false;
	}

	private SpatialSearchResultsList createResultList(List<SpatialSearchToken> tokens, List<SpatialPipelineObjectRes> r) {
		SpatialSearchResultsList singleResults = new SpatialSearchResultsList(tokens);
		for (SpatialPipelineObjectRes res : r) {
			singleResults.tileIds.add(res.atoms[0].coords.bboxTileId);
			for (int i = 0; i < res.atoms.length; i++) {
				singleResults.linearResults.add(res.atoms[i]);
			}
		}
		return singleResults;
	}
	
    public static boolean acceptPairSemantic(SpatialSearchContext ctx, SpatialPipelineObjectRes pair) {
//        SpatialTextSearchSettings settings = ctx.settings;
//        NameIndexAtom a1 = pair.mainAtom1;
//        NameIndexAtom a2 = pair.mainAtom2;
//		if (a1.isPoiCategory() && (a2.isPoiCategory() || a2.isPOI())) {
//			return false;
//		} else if (a2.isPoiCategory() && (a1.isPoiCategory() || a1.isPOI())) {
//			return false;
//		}
        // TODO x2 speed up by using flags
//        if (settings.OPTIM_FLAG_POI_SAME_AS_CITY_STREET && a1.atomicObject() && a2.atomicObject()) {
//            if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) {
//                return false;
//            }
//        }
        // TODO other accept
//        if (!settings.SEARCH_STREET_INTERSECTIONS && a1.isStreetBuilding() && a2.isStreetBuilding()) {
//            return false;
//        }
//        if (!settings.SEARCH_POI_INTERSECTIONS && a1.isPOI() && a2.isPOI()) {
//            return false;
//        }
//        boolean a1Building = a1.isBuilding() || (a1.buildingOrRefInd >= 0);
//        boolean a2Building = a2.isBuilding() || (a2.buildingOrRefInd >= 0);
//        if (a1Building || a2Building) {
//            NameIndexAtom building = a1Building ? a1 : a2;
//            NameIndexAtom other = a1Building ? a2 : a1;
//            if (other.isBuilding() || other.buildingOrRefInd >= 0) {
//                return false;
//            }
//            if (other.isStreetBuilding() || other.isCity() || other.isBoundary()) {
//                return true;
//            }
//            return false;
//        }
//
//        int atomicCount = (a1.atomicObject() ? 1 : 0) + (a2.atomicObject() ? 1 : 0);
//        if (atomicCount > settings.LIMIT_ATOMIC_OBJECTS) {
//            return false;
//        }


        return true;
    }
    
}