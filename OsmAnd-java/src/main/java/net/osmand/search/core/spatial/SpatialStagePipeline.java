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
public class SpatialStagePipeline {
    
    private final SpatialSearchContext ctx;
    
    public static int FREQUENT_OBJECTS_THRESHOLD = 5000;
    public static int MAX_VIRTUAL_MASKS = 10;
    
	public static int EXCLUDE_MASKS = 8000; // speed up
	public static boolean CHECK_EXCLUDED = false;
    public static int MAX_STEPS = 4; // 1 - fully covered, 2 - 1 intersecction ,...

    public SpatialStagePipeline(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }


	// MASK: 0x 01 00 10 ... 00 01
	public static final int MAX_SUPPORTED_TOKENS = (64 - 4) / 2;
	// 0-1 bits (atomic objects):
	// 11 - 1 - atomic, 01 - 2 atomic, 00 - 0 atomic (& 01 intersection forbidden)
	// 2-3 bits (poi & poi category):
	// 11 - poi, 01 - poi category, 00 - other (& 01 intersection forbidden!)
    public static final long STATE_NO_MATCH = 0L;      // 00
    public static final long STATE_EXACT_MATCH = 1L;   // 01
    public static final long STATE_AMBIGUOUS = 2L;     // 10
//    public static final long STATE_ANY = 3L;           // 11
    private static final long MASK_SET_01 = 0x5555_5555_5555_555FL;
    private static final long MASK_SET_02 = 0x5555_5555_5555_5550L;
    // ALLOWED &: 0000, 0011, 1100, 1111, 


    
    public static class SpatialObjectRes {
    	public final NameIndexAtom[] atoms;  
    	public NameIndexAtom mainAtom1;
    	public NameIndexAtom mainAtom2;

    	public long mainMask = 0;
    	public TLongArrayList otherMasks = new TLongArrayList(); 
    	

    	public SpatialObjectRes(int tCount, NameIndexAtom atom, int index) {
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
			// we need to separately process situation duplicate words in object and in query
			if (mainAtom1.isPOIRef() || mainAtom1.isBuilding()) {
				mainAtom1 = atom;
			}
			setAtom(atom, tokenIdx);
		}
    	
    	public SpatialObjectRes(long mask, SpatialObjectRes s1, SpatialObjectRes s2) {
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
    		mainMask = setTokenState(mainMask, index, atom.isBuilding() || atom.isPOIRef() ? STATE_AMBIGUOUS : STATE_EXACT_MATCH);
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
			    //combinedAtomic = b1 == 3 && b2 == 3 ? 1 : 2; // 1 atomic + 1 atomic : overflow
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
			    //combinedPoi = (p1 == 3 && p2 == 3) ? 3 : 1;
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
		    if (atomicState == 3L) {        // 11
		        res.add("A1");
		    } else if (atomicState == 1L) { // 01
		        res.add("A2");
		    } else if (atomicState == 0L) { // 01
		        res.add("A0");
		    } else if (atomicState == 0L) { // 01
		        res.add("A?");
		    }
		    long poiState = (mask >> 2) & 3L;
		    if (poiState == 3L) {        // 11
		        res.add("POI");
		    } else if (poiState == 1L) { // 01
		        res.add("POICAT");
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
    
    
    private static class MasksStats {
    	TLongObjectHashMap<Integer> masks = new TLongObjectHashMap<Integer>();
    	public final int intersections;

		public MasksStats(int intersections) {
			this.intersections = intersections;
		}

		int count(SpatialObjectRes obj) {
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
		public final TLongObjectHashMap<SpatialObjectRes> objectsById = new TLongObjectHashMap<>();
		public final TLongObjectHashMap<List<SpatialObjectRes>> excludedMasks = new TLongObjectHashMap<List<SpatialObjectRes>>();
		
		public final List<MasksStats> masksStats = new ArrayList<>();
        public final HashSkipTileQuadTree<SpatialObjectRes> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<SpatialObjectRes> areaObjectsTree = new HashSkipTileQuadTree<>();
		// stage 2, 3+        
		public final List<HashSkipTileQuadTree<SpatialObjectRes>> pairsTree = new ArrayList<>();
		
		public final List<SpatialSearchResultsList> combinations = new ArrayList<SpatialSearchResultsList>();
        
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
					SpatialObjectRes obj = new SpatialObjectRes(totalTokens, atom, tokenIdx);
					if(tokenIdx == 0 && atom.isGeoArea()) {
						System.out.println(atom + " " + SpatialObjectRes.formatMaskTokens(obj.mainMask, tokens));
					}
					prep.objectsById.put(atom.id, obj);
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
		int[] stops = ctx.settings.MAX_PIPELINE_STAGE_TO_STOP;
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
	    public HashSkipTileQuadTree<SpatialObjectRes> resTree = null;
	    public TLongObjectHashMap<List<SpatialObjectRes>> resObjectsByMasks = null;

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
	    
	    public HashSkipTileQuadTree<SpatialObjectRes> ensureTreeBuilt() {
	        if (resTree == null && resObjectsByMasks != null && !resObjectsByMasks.isEmpty()) {
	        	long t0 = System.nanoTime();
	            resTree = new HashSkipTileQuadTree<>();
	            long[] masks = resObjectsByMasks.keys();
	            for (int i = 0; i < masks.length; i++) {
	                List<SpatialObjectRes> list = resObjectsByMasks.get(masks[i]);
	                if (list != null) {
	                    for (int j = 0; j < list.size(); j++) {
	                        SpatialObjectRes obj = list.get(j);
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
	                if (SpatialObjectRes.countCoveredTokens(keys[i]) == totalTokens) {
	                    return true;
	                }
	            }
	            return false;
	        }

	        if (potentialMasks != null && !potentialMasks.isEmpty()) {
	            long[] masks = potentialMasks.toArray();
	            for (int i = 0; i < masks.length; i++) {
	                if (SpatialObjectRes.countCoveredTokens(masks[i]) == totalTokens) {
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

	    public void markComputed(HashSkipTileQuadTree<SpatialObjectRes> tree, 
	                             TLongObjectHashMap<List<SpatialObjectRes>> objectsByMasks) {
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
	                if (!SpatialObjectRes.allowed(pMask, eMask)) {
	                    continue;
	                }
	                long combinedMask = SpatialObjectRes.combine2BitMasks(pMask, eMask, totalTokens);
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
	
	private void compute(SpatialObjectsBucket b, List<SpatialObjectRes> res, SpatialPipelineResults prep) {
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
	    HashSkipTileQuadTree<SpatialObjectRes> parentTree = b.parent.ensureTreeBuilt();
	    HashSkipTileQuadTree<SpatialObjectRes> edgeTree = b.singleEdgeGroup.ensureTreeBuilt();

	    HashSkipTileQuadTree<SpatialObjectRes> resTree = null;
	    TLongObjectHashMap<List<SpatialObjectRes>> resObjectsByMasks = new TLongObjectHashMap<>();

	    if (parentTree != null && edgeTree != null && !parentTree.isEmpty() && !edgeTree.isEmpty()) {
	    	long tJoin = System.nanoTime();
	        final int totalTokens = prep.tokens.size();
	        HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner =
	                new HashSkipTileQuadTreeJoiner<>(parentTree, edgeTree);

	        joiner.joinAllBuckets((e1, e2) -> {
	        	metrics.pairsChecked++;
	            SpatialObjectRes obj1 = e1.obj;
	            SpatialObjectRes obj2 = e2.obj;
	            if (!SpatialObjectRes.allowed(obj1.mainMask, obj2.mainMask)) {
	                return;
	            }
	            metrics.pairsAccepted++;
	            long combinedMask = SpatialObjectRes.combine2BitMasks(obj1.mainMask, obj2.mainMask, totalTokens);
	            SpatialObjectRes combinedObj = new SpatialObjectRes(combinedMask, obj1, obj2);
	            List<SpatialObjectRes> list = resObjectsByMasks.get(combinedMask);
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

	private void collectFullCoverageResults(SpatialObjectsBucket b, List<SpatialObjectRes> res, int totalTokens) {
	    if (b == null || b.resObjectsByMasks == null || b.resObjectsByMasks.isEmpty()) {
	        return;
	    }
	    long[] masks = b.resObjectsByMasks.keys();
	    for (int i = 0; i < masks.length; i++) {
	        long mask = masks[i];
	        if (SpatialObjectRes.countCoveredTokens(mask) == totalTokens) {
	            List<SpatialObjectRes> list = b.resObjectsByMasks.get(mask);
	            if (list != null && !list.isEmpty()) {
	                res.addAll(list);
	            }
	        }
	    }
	}
	
	private static class MaskGroupInfo {
//	    final long mask;
	    int count;
	    List<SpatialObjectRes> objects; 

	    MaskGroupInfo(long mask, int count, List<SpatialObjectRes> objects) {
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
	        boolean isFullCovered = (SpatialObjectRes.countCoveredTokens(mask) == totalTokens);
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
	        List<SpatialObjectRes> list = b.resObjectsByMasks.get(mask);
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
					if (!SpatialObjectRes.allowed(pMask, eMask)) {
						continue;
					}
					metrics.maskPairsAllowed++;
					long combinedMask = SpatialObjectRes.combine2BitMasks(pMask, eMask, totalTokens);
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
	    for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
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
	    SpatialStagePipelineStats.printTree(prep);
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
	        List<SpatialObjectRes> res = new ArrayList<>();
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
		SpatialStagePipelineStats.printTree(prep);
//		SpatialStagePipelineStats.printTokenTree(prep);
		
		// STEP 1
		List<SpatialObjectRes> singleResults = new ArrayList<>();
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			if (SpatialObjectRes.countCoveredTokens(obj.mainMask) == tokensSize) {
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
		HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> selfJoiner = new HashSkipTileQuadTreeJoiner<>(
				prep.allObjectsTree, prep.allObjectsTree);// prep.allObjectsTree);
		
		boolean exit = join(prep, stage, selfJoiner, time);
		if (stage++ >= MAX_STEPS || ctx.isCancelled() || exit) {
			return prep.combinations;
		}
		time = System.nanoTime();
		
		// STEP 3: Spatial Join Pairs
		for (; stage <= MAX_STEPS && !ctx.isCancelled() && !exit; stage++) {
			HashSkipTileQuadTree<SpatialObjectRes> lastTree = prep.pairsTree.get(prep.pairsTree.size() - 1);
			if (lastTree.isEmpty()) {
				break;
			}
			lastTree.build();
			HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner = new HashSkipTileQuadTreeJoiner<>(
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
				HashSkipTileQuadTree<SpatialObjectRes> partialTree = i == 0 ? prep.allObjectsTree
						: prep.pairsTree.get(i - 1);

				TLongHashSet found = new TLongHashSet();
				for (int k = 0; k < excl.length; k++) {
					long maskExcl = excl[k];
					for (long m : masksStats.masks.keys()) {
						if (!SpatialObjectRes.allowed(m, maskExcl)) {
							continue;
						}
						long combined = SpatialObjectRes.combine2BitMasks(m, maskExcl, tokensSize);
						if (SpatialObjectRes.countCoveredTokens(combined) == tokensSize) {
							Integer c1 = baseMasksStats.masks.get(maskExcl);
							Integer c2 = masksStats.masks.get(m);
							System.out.printf("Potential results %d intersections - missing %s (%,d) x %s (%,d < %,d ) = %,d \n",
									stage + 1, SpatialObjectRes.formatMaskTokens(maskExcl, prep.tokens), c1,
									SpatialObjectRes.formatMaskTokens(m, prep.tokens), c2, partialTree.getSize(), c1 * c2);
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
				boolean exit = join(prep, stage + 1, tailJoiner, time);
				if (ctx.isCancelled() || exit) {
					return;
				}
				time = System.nanoTime();
			}
		}
	}


	private boolean join(SpatialPipelineResults prep, int stage,
			HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner, long time) throws IOException {
		List<SpatialObjectRes> pairResults = new ArrayList<>();
		final int tokensSize = prep.tokens.size();
		HashSkipTileQuadTree<SpatialObjectRes> pairsTree = new HashSkipTileQuadTree<>();
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
			if (!SpatialObjectRes.allowed(e1.obj.mainMask, e2.obj.mainMask)) {
				return;
			}
			// TODO check preformance this is covered by mask
//			if (e1.objId <= e2.objId) {
//				return; // skip 1 side pairs by id
//			} else 
			itStats[1]++;
			long combinedMask = SpatialObjectRes.combine2BitMasks(e1.obj.mainMask, e2.obj.mainMask, tokensSize);
			SpatialObjectRes res = new SpatialObjectRes(combinedMask, e1.obj, e2.obj);
			ms.count(res);
			if (SpatialObjectRes.countCoveredTokens(combinedMask) == tokensSize) {
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
	
    public static boolean acceptPairSemantic(SpatialSearchContext ctx, SpatialObjectRes pair) {
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