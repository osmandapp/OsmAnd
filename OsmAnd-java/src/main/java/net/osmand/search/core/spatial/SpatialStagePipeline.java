package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

public class SpatialStagePipeline {

    // 2-Bit Token States
    public static final long STATE_NO_MATCH = 0L;      // Token does not belong to object
    public static final long STATE_EXACT_MATCH = 1L;   // Token explicitly belongs to object
    public static final long STATE_AMBIGUOUS = 2L;     // Token is isBuilding or isPOIRef

    private final SpatialSearchContext ctx;

    public SpatialStagePipeline(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }

    // =========================================================================
    // DTO Models
    // =========================================================================

    public static class PipelinePrepResult {
        public final List<NameIndexAtom> fullyCoveredAtoms = new ArrayList<>();
        public final HashSkipTileQuadTree<NameIndexAtom> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<NameIndexAtom> areaObjectsTree = new HashSkipTileQuadTree<>();
        // Unique atom.id -> List of token masks (handles duplicate words in query)
        public final TLongObjectHashMap<List<Long>> objectMasks = new TLongObjectHashMap<>();
    }

    public static class SpatialSearchResultPair {
        public final long pairId;
        public final NameIndexAtom atom1;
        public final NameIndexAtom atom2;
        public final int[] bbox31;
        public final long combinedMask;

        public SpatialSearchResultPair(NameIndexAtom atom1, NameIndexAtom atom2, int[] bbox31, long combinedMask) {
            this.atom1 = atom1;
            this.atom2 = atom2;
            this.bbox31 = bbox31;
            this.combinedMask = combinedMask;
            this.pairId = atom1.id ^ (atom2.id << 32) ^ combinedMask;
        }
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

    public static class ConcreteAssignmentPair {
        public final SpatialSearchResultPair parentPair;
        public final long resolvedMask;
        public final long assignedToA1Mask;
        public final long assignedToA2Mask;
        public final NameIndexAtom refOwner;

        public ConcreteAssignmentPair(SpatialSearchResultPair parentPair, long resolvedMask, 
                                      long assignedToA1Mask, long assignedToA2Mask, NameIndexAtom refOwner) {
            this.parentPair = parentPair;
            this.resolvedMask = resolvedMask;
            this.assignedToA1Mask = assignedToA1Mask;
            this.assignedToA2Mask = assignedToA2Mask;
            this.refOwner = refOwner;
        }
    }

    // =========================================================================
    // Bitmask Helper Utilities
    // =========================================================================

    public static long setTokenState(long currentMask, int tokenIdx, long state) {
        int shift = tokenIdx * 2;
        long clearMask = ~(3L << shift);
        return (currentMask & clearMask) | ((state & 3L) << shift);
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

    public static boolean isFullyCovered(long mask, int totalTokens) {
        for (int i = 0; i < totalTokens; i++) {
            long state = (mask >> (i * 2)) & 3L;
            if (state == STATE_NO_MATCH) return false;
        }
        return true;
    }

    public static List<ConcreteAssignmentPair> expandAmbiguousPairsAllOrNothing(
            SpatialSearchResultPair pair, 
            long mask1, 
            long mask2, 
            int totalTokens) {

        List<ConcreteAssignmentPair> permutations = new ArrayList<>(2);

        List<Integer> ambiguousIndices = new ArrayList<>();
        for (int i = 0; i < totalTokens; i++) {
            long state = (pair.combinedMask >> (i * 2)) & 3L;
            if (state == STATE_AMBIGUOUS) {
                ambiguousIndices.add(i);
            }
        }

        if (ambiguousIndices.isEmpty()) {
            permutations.add(new ConcreteAssignmentPair(pair, pair.combinedMask, mask1, mask2, null));
            return permutations;
        }

        // Option A: Assign ALL ambiguous tokens to Atom 1
        long combinedMask1 = pair.combinedMask;
        long resolvedMaskA1 = mask1;
        long resolvedMaskA2 = mask2;

        for (int tokenIdx : ambiguousIndices) {
            combinedMask1 = setTokenState(combinedMask1, tokenIdx, STATE_EXACT_MATCH);
            resolvedMaskA1 = setTokenState(resolvedMaskA1, tokenIdx, STATE_EXACT_MATCH);
            resolvedMaskA2 = setTokenState(resolvedMaskA2, tokenIdx, STATE_NO_MATCH);
        }
        permutations.add(new ConcreteAssignmentPair(pair, combinedMask1, resolvedMaskA1, resolvedMaskA2, pair.atom1));

        // Option B: Assign ALL ambiguous tokens to Atom 2
        long combinedMask2 = pair.combinedMask;
        long resolvedMaskB1 = mask1;
        long resolvedMaskB2 = mask2;

        for (int tokenIdx : ambiguousIndices) {
            combinedMask2 = setTokenState(combinedMask2, tokenIdx, STATE_EXACT_MATCH);
            resolvedMaskB1 = setTokenState(resolvedMaskB1, tokenIdx, STATE_NO_MATCH);
            resolvedMaskB2 = setTokenState(resolvedMaskB2, tokenIdx, STATE_EXACT_MATCH);
        }
        permutations.add(new ConcreteAssignmentPair(pair, combinedMask2, resolvedMaskB1, resolvedMaskB2, pair.atom2));

        return permutations;
    }

    // =========================================================================
    // Pipeline Preparation
    // =========================================================================

    private PipelinePrepResult prepare(List<SpatialSearchToken> tokens) {
        PipelinePrepResult prep = new PipelinePrepResult();
        int totalTokens = tokens.size();

        TLongObjectHashMap<NameIndexAtom> atomMap = new TLongObjectHashMap<>();

        for (int tokenIdx = 0; tokenIdx < totalTokens; tokenIdx++) {
            SpatialSearchToken token = tokens.get(tokenIdx);

            for (NameIndexAtom atom : token.atoms) {
                long id = atom.id;
                long state = (atom.isBuilding() || atom.isPOIRef()) ? STATE_AMBIGUOUS : STATE_EXACT_MATCH;

                List<Long> maskList = prep.objectMasks.get(id);
                if (maskList == null) {
                    maskList = new ArrayList<>(1);
                    prep.objectMasks.put(id, maskList);
                }

                if (maskList.isEmpty()) {
                    maskList.add(setTokenState(0L, tokenIdx, state));
                } else {
                    long updated = setTokenState(maskList.get(0), tokenIdx, state);
                    maskList.set(0, updated);
                }

                if (!atomMap.containsKey(id)) {
                    atomMap.put(id, atom);
                }
            }
        }

        prep.objectMasks.forEachEntry((objId, masks) -> {
            NameIndexAtom atom = atomMap.get(objId);
            long primaryMask = masks.get(0);

            if (isFullyCovered(primaryMask, totalTokens)) {
                prep.fullyCoveredAtoms.add(atom);
            }

            if (atom.coords.bbox31 != null) {
                prep.allObjectsTree.addObject(atom, atom.coords.bbox31, atom.id);
            }

            if (!atom.atomicObject() && atom.coords.bbox31 != null) {
                prep.areaObjectsTree.addObject(atom, atom.coords.bbox31, atom.id);
            }

            return true;
        });

        prep.allObjectsTree.build();
        prep.areaObjectsTree.build();

        return prep;
    }

    // =========================================================================
    // Pipeline Core Execution
    // =========================================================================

    public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) {
        List<SpatialSearchResultsList> combinations = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) {
            return combinations;
        }

        PipelinePrepResult prep = prepare(tokens);
        int totalTokens = tokens.size();

        // ---------------------------------------------------------------------
        // STEP 1: Single objects fully covering all search tokens
        // ---------------------------------------------------------------------
        if (!prep.fullyCoveredAtoms.isEmpty()) {
            SpatialSearchResultsList step1List = new SpatialSearchResultsList(tokens);
            for (NameIndexAtom atom : prep.fullyCoveredAtoms) {
                step1List.addResult(null, 0, atom, 0, ctx.settings);
            }
            combinations.add(step1List);
        }

        // ---------------------------------------------------------------------
        // STEP 2: Spatial Self-Join (Pairs)
        // ---------------------------------------------------------------------
        HashSkipTileQuadTree<SpatialSearchResultPair> step2PairsTree = new HashSkipTileQuadTree<>();
        HashSkipTileQuadTreeJoiner<NameIndexAtom, NameIndexAtom> selfJoiner =
                new HashSkipTileQuadTreeJoiner<>(prep.allObjectsTree, prep.allObjectsTree);

        selfJoiner.joinAllBuckets((e1, e2) -> {
            if (e1.objId == e2.objId) return;

            NameIndexAtom a1 = e1.obj;
            NameIndexAtom a2 = e2.obj;

            List<Long> masks1 = prep.objectMasks.get(a1.id);
            List<Long> masks2 = prep.objectMasks.get(a2.id);

            for (long m1 : masks1) {
                for (long m2 : masks2) {
                    long combinedMask = combine2BitMasks(m1, m2, totalTokens);

                    int[] clippedBBox = new int[]{ a1.coords.bbox31[0], a1.coords.bbox31[1], a1.coords.bbox31[2], a1.coords.bbox31[3] };
                    SpatialSearchResultsList.clipBbox(clippedBBox, a2.coords.bbox31);

                    SpatialSearchResultPair pair = new SpatialSearchResultPair(a1, a2, clippedBBox, combinedMask);
                    step2PairsTree.addObject(pair, clippedBBox, pair.pairId);
                }
            }
        }, null, null);

        step2PairsTree.build();

        // Collect and validate candidates from Step 2
        SpatialSearchResultsList step2List = new SpatialSearchResultsList(tokens);
        for (TileEntry<SpatialSearchResultPair> entry : step2PairsTree.getTileEntries()) {
            SpatialSearchResultPair pair = entry.obj;

            long mask1 = prep.objectMasks.get(pair.atom1.id).get(0);
            long mask2 = prep.objectMasks.get(pair.atom2.id).get(0);

            List<ConcreteAssignmentPair> assignments = expandAmbiguousPairsAllOrNothing(pair, mask1, mask2, totalTokens);

            for (ConcreteAssignmentPair assignment : assignments) {
                if (isFullyCovered(assignment.resolvedMask, totalTokens)) {
                    if (acceptPairSemantic(ctx, pair, assignment.refOwner)) {
                        step2List.addResult(null, 0, pair.atom1, 0, ctx.settings);
                        step2List.addResult(null, 0, pair.atom2, 0, ctx.settings);
                    }
                }
            }
        }

        if (step2List.getCombinations() > 0) {
            combinations.add(step2List);
        }

        // ---------------------------------------------------------------------
        // STEP 3: Cascading Area Joins (Chains of 3+ objects)
        // ---------------------------------------------------------------------
        HashSkipTileQuadTree<SpatialSearchResultChain> currentChainTree = new HashSkipTileQuadTree<>();

        for (TileEntry<SpatialSearchResultPair> entry : step2PairsTree.getTileEntries()) {
            SpatialSearchResultPair pair = entry.obj;
            List<NameIndexAtom> initialAtoms = List.of(pair.atom1, pair.atom2);

            SpatialSearchResultChain chain = new SpatialSearchResultChain(initialAtoms, pair.bbox31, pair.combinedMask);
            currentChainTree.addObject(chain, chain.bbox31, chain.chainId);
        }
        currentChainTree.build();

        int maxAreaDepth = 4;
        for (int depth = 2; depth <= maxAreaDepth; depth++) {
            if (currentChainTree.getTileEntries().isEmpty()) break;

            HashSkipTileQuadTree<SpatialSearchResultChain> nextChainTree = new HashSkipTileQuadTree<>();

            HashSkipTileQuadTreeJoiner<SpatialSearchResultChain, NameIndexAtom> chainJoiner =
                    new HashSkipTileQuadTreeJoiner<>(currentChainTree, prep.areaObjectsTree);

            chainJoiner.joinAllBuckets((eChain, eArea) -> {
                SpatialSearchResultChain chain = eChain.obj;
                NameIndexAtom areaAtom = eArea.obj;

                if (chain.containsAtom(areaAtom.id)) return;

                long areaMask = prep.objectMasks.get(areaAtom.id).get(0);
                long newMask = combine2BitMasks(chain.combinedMask, areaMask, totalTokens);

                int[] newBBox = chain.bbox31.clone();
                SpatialSearchResultsList.clipBbox(newBBox, areaAtom.coords.bbox31);

                SpatialSearchResultChain newChain = chain.extend(areaAtom, newBBox, newMask);
                nextChainTree.addObject(newChain, newBBox, newChain.chainId);
            }, null, null);

            nextChainTree.build();

            // Emit valid covered chains for this depth level
            SpatialSearchResultsList step3List = new SpatialSearchResultsList(tokens);
            for (TileEntry<SpatialSearchResultChain> entry : nextChainTree.getTileEntries()) {
                SpatialSearchResultChain chain = entry.obj;
                if (isFullyCovered(chain.combinedMask, totalTokens)) {
                    for (NameIndexAtom atom : chain.atoms) {
                        step3List.addResult(null, 0, atom, 0, ctx.settings);
                    }
                }
            }

            if (step3List.getCombinations() > 0) {
                combinations.add(step3List);
            }

            currentChainTree = nextChainTree;
        }

        return combinations;
    }

    
    public static boolean acceptPairSemantic(SpatialSearchContext ctx, SpatialSearchResultPair pair, NameIndexAtom refOwner) {
        SpatialTextSearchSettings settings = ctx.settings;
        NameIndexAtom a1 = pair.atom1;
        NameIndexAtom a2 = pair.atom2;

        int atomicCount = (a1.atomicObject() ? 1 : 0) + (a2.atomicObject() ? 1 : 0);
        if (atomicCount > settings.LIMIT_ATOMIC_OBJECTS) {
            return false;
        }

        boolean twoStreets = a1.isStreetBuilding() && a2.isStreetBuilding();
        boolean twoPOIs = !a1.isStreetBuilding() && !a2.isStreetBuilding();

        if (!settings.SEARCH_STREET_INTERSECTIONS && twoStreets) return false;
        if (!settings.SEARCH_POI_INTERSECTIONS && twoPOIs) return false;

        if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) return false;

        boolean hasPoiCategory = a1.isPoiCategory() || a2.isPoiCategory();
        boolean hasBuilding = a1.isBuilding() || a2.isBuilding();
        if (hasPoiCategory && hasBuilding) return false;

        if ((a1.buildingOrRefInd >= 0) && a2.isStreetBuilding() && !a1.isCityStreetName()) return false;
        if ((a2.buildingOrRefInd >= 0) && a1.isStreetBuilding() && !a2.isCityStreetName()) return false;

        return true;
    }
}