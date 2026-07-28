package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

// TODO ---------------
// TODO Add non maximum results as well... (surplus words +-) -  germany_langestrasse
// TODO other masks ?
// TODO introduce mask into joiner index
// TODO increase space
// TODO x2 speed up by using flags
// TODO x1 implement correct mixing alternative masks!
public class SpatialStagePipeline {

    public static final long STATE_NO_MATCH = 0L;      // 00
    public static final long STATE_EXACT_MATCH = 1L;   // 01
    public static final long STATE_AMBIGUOUS = 2L;     // 10
    public static final long STATE_ANY = 3L;           // 11
    private static final long MASK_SET_01 = 0x5555_5555_5555_5555L;
    
    
    private final SpatialSearchContext ctx;
    public static final int MAX_SUPPORTED_TOKENS = 31;
    public static int MAX_STEPS = 3; // 1 - fully covered, 2 - 1 intersecction ,...

    public SpatialStagePipeline(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }


    public static class SpatialObjectRes {
    	public final NameIndexAtom[] atoms;  
    	public NameIndexAtom mainAtom1;
    	public NameIndexAtom mainAtom2;
    	
    	public long mainMask = 0;
    	public TLongArrayList otherMasks = new TLongArrayList(); 
    	
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
    	
    	public SpatialObjectRes(int tCount, NameIndexAtom atom, int index) {
    		atoms = new NameIndexAtom[tCount];
    		mainAtom1 = atom;
    		setAtom(atom, index);
    	}

		void setAtom(NameIndexAtom atom, int index) {
			atoms[index] = atom;
    		mainMask = setTokenState(mainMask, index, atom.isBuilding() || atom.isPOIRef() ? STATE_AMBIGUOUS : STATE_EXACT_MATCH);
		}

		public void merge(NameIndexAtom atom, int tokenIdx) {
			// TODO x1 implement correct mixing alternative masks!
			// we need to separately process situation duplicate words in object and in query
			if (mainAtom1.isPOIRef() || mainAtom1.isBuilding()) {
				mainAtom1 = atom;
			}
			setAtom(atom, tokenIdx);
		}
    	
		
		public static long setTokenState(long currentMask, int tokenIdx, long state) {
			int shift = tokenIdx * 2;
			long clearMask = ~(3L << shift);
			return (currentMask & clearMask) | ((state & 3L) << shift);
		}

	    public static int countCoveredTokens(long mask) {
	        long activeTokensMask = (mask | (mask >>> 1)) & MASK_SET_01;
	        return Long.bitCount(activeTokensMask);
	    }
	    
	    public static boolean allowed(long m1, long m2) {
	    	return (m1 & m2 & MASK_SET_01) == 0;
	    }
	    
	    public static long combine2BitMasks(long mask1, long mask2, int totalTokens) {
			long result = 0L;
			for (int i = 0; i < totalTokens; i++) {
				int shift = i * 2;
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

    }

    
    public static class SpatialPipelineResults {
    	public final List<SpatialSearchToken> tokens;
		public SpatialPipelineResults(List<SpatialSearchToken> tokens) {
			this.tokens = tokens;
		}
		// stage 1
		public final TLongObjectHashMap<SpatialObjectRes> objectsById = new TLongObjectHashMap<>();
        public final HashSkipTileQuadTree<SpatialObjectRes> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<SpatialObjectRes> areaObjectsTree = new HashSkipTileQuadTree<>();
		// stage 2, 3+        
		public final List<HashSkipTileQuadTree<SpatialObjectRes>> pairsTree = new ArrayList<>();
		
		public final List<SpatialSearchResultsList> combinations = new ArrayList<SpatialSearchResultsList>();
        
    }


    public static class SpatialSearchResultChain {
        public final long chainId;
        public final List<NameIndexAtom> atoms;
        public final int[] bbox31;
        public final long combinedMask;

        public SpatialSearchResultChain(List<NameIndexAtom> atoms, int[] bbox31, long combinedMask) {
            this.atoms = atoms;
            this.bbox31 = bbox31;
            this.combinedMask = combinedMask;

            long idAcc = 0;
            for (NameIndexAtom a : atoms) {
                idAcc ^= a.id;
            }
            this.chainId = idAcc ^ combinedMask;
        }

        public boolean containsAtom(long atomId) {
            for (NameIndexAtom a : atoms) {
                if (a.id == atomId) return true;
            }
            return false;
        }

        public SpatialSearchResultChain extend(NameIndexAtom newArea, int[] newBBox, long newMask) {
            List<NameIndexAtom> extendedAtoms = new ArrayList<>(this.atoms);
            extendedAtoms.add(newArea);
            return new SpatialSearchResultChain(extendedAtoms, newBBox, newMask);
        }
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
				// TODO duplicate words!
				SpatialObjectRes existing = prep.objectsById.get(atom.id);
				if (existing != null) {
					existing.merge(atom, tokenIdx);
				} else {
					SpatialObjectRes obj = new SpatialObjectRes(totalTokens, atom, tokenIdx);
					prep.objectsById.put(atom.id, obj);
				}
			}
		}
        
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
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
		if (stops.length > 0 && nonCategoryRes > stops[Math.min(stops.length, stage) - 1]) {
			return true;
		}
		return false;
	}
	
	
    // =========================================================================
    // Execution Engine with Mode Control
    // =========================================================================
	public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) throws IOException {
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
//		SpatialStagePipelineStats.printTree(prep);
		SpatialStagePipelineStats.printTokenTree(prep);
		
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

		return prep.combinations;
	}


	private boolean join(SpatialPipelineResults prep, int stage,
			HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> joiner, long time) throws IOException {
		List<SpatialObjectRes> pairResults = new ArrayList<>();
		final int tokensSize = prep.tokens.size();
		HashSkipTileQuadTree<SpatialObjectRes> pairsTree = new HashSkipTileQuadTree<>();
		prep.pairsTree.add(pairsTree);
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
			// TODO this is covered by mask
//			if (e1.objId <= e2.objId) {
//				return; // skip 1 side pairs by id
//			} else 
			itStats[1]++;
			long combinedMask = SpatialObjectRes.combine2BitMasks(e1.obj.mainMask, e2.obj.mainMask, tokensSize);
			SpatialObjectRes res = new SpatialObjectRes(combinedMask, e1.obj, e2.obj);
			if (stage == 1 && !acceptPairSemantic(ctx, res)) {
				return;
			}
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
        SpatialTextSearchSettings settings = ctx.settings;
        NameIndexAtom a1 = pair.mainAtom1;
        NameIndexAtom a2 = pair.mainAtom2;
        // TODO x2 speed up by using flags
		if (a1.isPoiCategory() && (a2.isPoiCategory() || a2.isPOI())) {
			return false;
		} else if (a2.isPoiCategory() && (a1.isPoiCategory() || a1.isPOI())) {
			return false;
		}
        if (settings.OPTIM_FLAG_POI_SAME_AS_CITY_STREET && a1.atomicObject() && a2.atomicObject()) {
            if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) {
                return false;
            }
        }
        if (!settings.SEARCH_STREET_INTERSECTIONS && a1.isStreetBuilding() && a2.isStreetBuilding()) {
            return false;
        }
        if (!settings.SEARCH_POI_INTERSECTIONS && a1.isPOI() && a2.isPOI()) {
            return false;
        }

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