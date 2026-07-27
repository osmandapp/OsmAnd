package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

public class SpatialStagePipeline {

    public static final long STATE_NO_MATCH = 0L;      // 00
    public static final long STATE_EXACT_MATCH = 1L;   // 01
    public static final long STATE_AMBIGUOUS = 2L;     // 10
    public static final long STATE_ANY = 3L;           // 11
    private static final long MASK_SET_01 = 0x5555_5555_5555_5555L;
    
    
    private final SpatialSearchContext ctx;
    public static final int MAX_SUPPORTED_TOKENS = 31;
    public static int MAX_STEPS = 2; // 1 - fully covered, 2 - 1 intersecction ,...

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
			// TODO implement correct mixing alternative masks!
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
    
    public static class PipelinePrepResult {
		public PipelinePrepResult(List<SpatialSearchToken> tokens) {
			this.tokens = tokens;
		}
		
		public final List<SpatialSearchToken> tokens;
		public final TLongObjectHashMap<SpatialObjectRes> objectsById = new TLongObjectHashMap<>();
    	
        public final HashSkipTileQuadTree<SpatialObjectRes> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<SpatialObjectRes> areaObjectsTree = new HashSkipTileQuadTree<>();
        
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


    private PipelinePrepResult prepare(List<SpatialSearchToken> tokens) {
    	if (tokens.size() > MAX_SUPPORTED_TOKENS) {
			tokens = tokens.subList(0, MAX_SUPPORTED_TOKENS);
		}
        PipelinePrepResult prep = new PipelinePrepResult(tokens);
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
					existing.merge(atom, tokenIdx);
				} else {
					SpatialObjectRes obj = new SpatialObjectRes(totalTokens, atom, tokenIdx);
					prep.objectsById.put(atom.id, obj);
				}
			}
		}
        
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			prep.allObjectsTree.addObject(obj, obj.mainAtom1.coords.bbox31, obj.mainAtom1.id);
			if (obj.mainAtom1.isBoundary() || obj.mainAtom1.isCityVillage() || obj.mainAtom1.isPostcode()) {
				prep.areaObjectsTree.addObject(obj, obj.mainAtom1.coords.bbox31, obj.mainAtom1.id);
			}
 		}
        prep.allObjectsTree.build();
        prep.areaObjectsTree.build();

        return prep;
	}


//	public static List<ConcreteAssignmentPair> expandAmbiguousPairsAllOrNothing(SpatialSearchResultPair pair,
//			long mask1, long mask2, int totalTokens) {
//
//		List<ConcreteAssignmentPair> permutations = new ArrayList<>(2);
//		List<Integer> ambiguousIndices = new ArrayList<>();
//		for (int i = 0; i < totalTokens; i++) {
//			long state = (pair.combinedMask >> (i * 2)) & 3L;
//			if (state == STATE_AMBIGUOUS) {
//				ambiguousIndices.add(i);
//			}
//		}
//
//		if (ambiguousIndices.isEmpty()) {
//			permutations.add(new ConcreteAssignmentPair(pair, pair.combinedMask, null));
//			return permutations;
//		}
//
//		long combinedMask1 = pair.combinedMask;
//		for (int tokenIdx : ambiguousIndices) {
//			combinedMask1 = SpatialObjectRes.setTokenState(combinedMask1, tokenIdx, STATE_EXACT_MATCH);
//		}
//		permutations.add(new ConcreteAssignmentPair(pair, combinedMask1, pair.atom1));
//
//		long combinedMask2 = pair.combinedMask;
//		for (int tokenIdx : ambiguousIndices) {
//			combinedMask2 = SpatialObjectRes.setTokenState(combinedMask2, tokenIdx, STATE_EXACT_MATCH);
//		}
//		permutations.add(new ConcreteAssignmentPair(pair, combinedMask2, pair.atom2));
//
//		return permutations;
//    }
//
//
	
	private boolean validateStageAndFinish(List<SpatialSearchResultsList> combinations, List<SpatialSearchToken> tokens, List<SpatialObjectRes> preResults, int stage) throws IOException {
		if (preResults.isEmpty()) {
			return false;
		}
		SpatialSearchResultsList stageList = createResultList(tokens, preResults);
		System.out.printf("[PIPELINE-LOG] Finalizing %d STAGE with %d raw combinations...\n", stage, stageList.getCombinations());
		if (ctx.isCancelled()) {
			return true;
		}
		stageList.loadObjectsAndCalcBuildings(ctx);
		if (ctx.isCancelled()) {
			return true;
		}
		List<SpatialSearchResult> res = stageList.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
		int nonCategoryRes = 0;
		for (SpatialSearchResult r : res) {
			if (!r.isPoiCategory()) {
				nonCategoryRes++;
			}
		}
		if ((res.size() > 0 && stage == 0) || nonCategoryRes > 0) {
			combinations.add(stageList);
		}
		System.out.printf("[PIPELINE-LOG] %d STAGE after load & deduplicate: %d valid results.\n", stage, nonCategoryRes);
		// TODO here we could have limit 10, in settings
		if (nonCategoryRes > 0) {
			return true;
		}
		return false;
	}
    

    // =========================================================================
    // Execution Engine with Mode Control
    // =========================================================================
	// TODO Add non maximum results as well... (surplus words +-) 
	// TODO other masks ?
    // TODO introduce mask into joiner index
    public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) throws IOException {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        final int tokensSize = tokens.size();
        List<SpatialSearchResultsList> combinations = new ArrayList<SpatialSearchResultsList>();
        
        // STEP 0 PREPARE
        int stage = 0;
        PipelinePrepResult prep = prepare(tokens);
        stage++;
		// STEP 1
		List<SpatialObjectRes> singleResults = new ArrayList<>();
		for (SpatialObjectRes obj : prep.objectsById.valueCollection()) {
			if (SpatialObjectRes.countCoveredTokens(obj.mainMask) == tokensSize) {
				singleResults.add(obj);
			}
		}
		if (validateStageAndFinish(combinations, tokens, singleResults, stage)) {
			return combinations;
		}
		if (stage++ >= MAX_STEPS || ctx.isCancelled()) {
			return combinations;
        }
            
        // STEP 2: Spatial Join Pairs
        HashSkipTileQuadTree<SpatialObjectRes> step2PairsTree = new HashSkipTileQuadTree<>();
        HashSkipTileQuadTreeJoiner<SpatialObjectRes, SpatialObjectRes> selfJoiner =
                new HashSkipTileQuadTreeJoiner<>(prep.allObjectsTree, prep.allObjectsTree);
        List<SpatialObjectRes> pairResults = new ArrayList<>();
        selfJoiner.joinAllBuckets((e1, e2) -> {
			if (e1.objId <= e2.objId) {
				return; // skip 1 side pairs by id
			} else if (!SpatialObjectRes.allowed(e1.obj.mainMask, e2.obj.mainMask)) {
				return;
			}
			long combinedMask = SpatialObjectRes.combine2BitMasks(e1.obj.mainMask, e2.obj.mainMask, tokensSize);
			SpatialObjectRes res = new SpatialObjectRes(combinedMask, e1.obj, e2.obj);
			if (SpatialObjectRes.countCoveredTokens(combinedMask) == tokensSize) {
				// TODO add combinations from combined mask
				pairResults.add(res);
			}
			int[] bb = res.mainAtom1.coords.bbox31;
	        int[] clippedBBox = new int[]{bb[0], bb[1], bb[2], bb[3] };
	        SpatialSearchResultsList.clipBbox(clippedBBox, res.mainAtom2.coords.bbox31);
	        step2PairsTree.addObject(res, clippedBBox, -1);
        });
        
        if (validateStageAndFinish(combinations, tokens, pairResults, stage)) {
			return combinations;
		}
        if (stage++ >= MAX_STEPS || ctx.isCancelled()) {
			return combinations;
        }
//
  
//
//        TLongHashSet addedPairIds = new TLongHashSet();
//
//        selfJoiner.joinAllBuckets((e1, e2) -> {
//            if (e1.objId == e2.objId) return;
//
//            NameIndexAtom a1 = e1.obj;
//            NameIndexAtom a2 = e2.obj;
//
//            List<Long> masks1 = prep.objectMasks.get(a1.id);
//            List<Long> masks2 = prep.objectMasks.get(a2.id);
//            if (masks1 == null || masks2 == null) return;
//
//            for (long m1 : masks1) {
//                for (long m2 : masks2) {
//                    long combinedMask = combine2BitMasks(m1, m2, totalTokens);
//
//                    if (countCoveredTokens(combinedMask, totalTokens) < 2) {
//                        continue;
//                    }
//
//                    int[] clippedBBox = new int[]{ a1.coords.bbox31[0], a1.coords.bbox31[1], a1.coords.bbox31[2], a1.coords.bbox31[3] };
//                    SpatialSearchResultsList.clipBbox(clippedBBox, a2.coords.bbox31);
//
//                    SpatialSearchResultPair pair = new SpatialSearchResultPair(a1, a2, clippedBBox, combinedMask);
//                    if (addedPairIds.add(pair.pairId)) {
//                        step2PairsTree.addObject(pair, clippedBBox, pair.pairId);
//                    }
//                }
//            }
//        }, null, null);
//
//        step2PairsTree.build();
//
//        List<SpatialSearchResultPair> validStep2Pairs = new ArrayList<>();
//
//        for (TileEntry<SpatialSearchResultPair> entry : step2PairsTree.getTileEntries()) {
//            SpatialSearchResultPair pair = entry.obj;
//
//            List<Long> m1List = prep.objectMasks.get(pair.atom1.id);
//            List<Long> m2List = prep.objectMasks.get(pair.atom2.id);
//            long mask1 = (m1List != null && !m1List.isEmpty()) ? m1List.get(0) : 0L;
//            long mask2 = (m2List != null && !m2List.isEmpty()) ? m2List.get(0) : 0L;
//
//            List<ConcreteAssignmentPair> assignments = expandAmbiguousPairsAllOrNothing(pair, mask1, mask2, totalTokens);
//
//            for (ConcreteAssignmentPair assignment : assignments) {
//                boolean isCovered = (totalTokens <= 2) ? isFullyCovered(assignment.resolvedMask, totalTokens) 
//                                                       : countCoveredTokens(assignment.resolvedMask, totalTokens) >= 2;
//
//                if (isCovered) {
//                    if (acceptPairSemantic(ctx, pair, assignment.refOwner)) {
//                        validStep2Pairs.add(pair);
//                        if (isFullyCovered(assignment.resolvedMask, totalTokens)) {
//                            int typeIntersection = calculateTypeIntersection(pair.atom1, pair.atom2);
//                            addCombinationResult(step2List, List.of(pair.atom1, pair.atom2), tokens, typeIntersection);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (stageMode == PipelineStageMode.STEPS_1_AND_2) {
//            if (finalizeAndValidateStage(step2List, "STEP 2 (Pairs Only)")) {
//                combinations.add(step2List);
//            }
//            return combinations;
//        }

        // ---------------------------------------------------------------------
        // STEP 3: Cascading Area Joins
        // ---------------------------------------------------------------------
//        SpatialSearchResultsList mergedList = step2List; // Объединяем результаты Step 2 и Step 3
//
//        if (totalTokens > 2 && !validStep2Pairs.isEmpty()) {
//            HashSkipTileQuadTree<SpatialSearchResultChain> currentChainTree = new HashSkipTileQuadTree<>();
//
//            for (SpatialSearchResultPair pair : validStep2Pairs) {
//                List<NameIndexAtom> initialAtoms = List.of(pair.atom1, pair.atom2);
//                SpatialSearchResultChain chain = new SpatialSearchResultChain(initialAtoms, pair.bbox31, pair.combinedMask);
//                currentChainTree.addObject(chain, chain.bbox31, chain.chainId);
//            }
//            currentChainTree.build();
//
//            int maxAreaDepth = Math.min(totalTokens, 4);
//            for (int depth = 3; depth <= maxAreaDepth; depth++) {
//                if (currentChainTree.getTileEntries().isEmpty() || ctx.isCancelled()) break;
//
//                HashSkipTileQuadTree<SpatialSearchResultChain> nextChainTree = new HashSkipTileQuadTree<>();
//                HashSkipTileQuadTreeJoiner<SpatialSearchResultChain, NameIndexAtom> chainJoiner =
//                        new HashSkipTileQuadTreeJoiner<>(currentChainTree, prep.areaObjectsTree);
//
//                chainJoiner.joinAllBuckets((eChain, eArea) -> {
//                    SpatialSearchResultChain chain = eChain.obj;
//                    NameIndexAtom areaAtom = eArea.obj;
//
//                    if (chain.containsAtom(areaAtom.id)) return;
//
//                    List<Long> areaMasks = prep.objectMasks.get(areaAtom.id);
//                    long areaMask = (areaMasks != null && !areaMasks.isEmpty()) ? areaMasks.get(0) : 0L;
//                    long newMask = combine2BitMasks(chain.combinedMask, areaMask, totalTokens);
//
//                    if (newMask == chain.combinedMask) return;
//
//                    int[] newBBox = chain.bbox31.clone();
//                    SpatialSearchResultsList.clipBbox(newBBox, areaAtom.coords.bbox31);
//
//                    SpatialSearchResultChain newChain = chain.extend(areaAtom, newBBox, newMask);
//                    nextChainTree.addObject(newChain, newBBox, newChain.chainId);
//                }, null, null);
//
//                nextChainTree.build();
//
//                for (TileEntry<SpatialSearchResultChain> entry : nextChainTree.getTileEntries()) {
//                    SpatialSearchResultChain chain = entry.obj;
//                    if (isFullyCovered(chain.combinedMask, totalTokens)) {
//                        addCombinationResult(mergedList, chain.atoms, tokens, 1);
//                    }
//                }
//
//                currentChainTree = nextChainTree;
//            }
//        }
//
//        if (finalizeAndValidateStage(mergedList, "MERGED STAGES (Step 2 + Step 3)")) {
//            combinations.add(mergedList);
//        }

        return combinations;
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
		if (a1.isPoiCategory() && a2.isPoiCategory()) {
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