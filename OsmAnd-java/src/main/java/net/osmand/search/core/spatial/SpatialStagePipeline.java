package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

public class SpatialStagePipeline {

    public static final long STATE_NO_MATCH = 0L;      // 00
    public static final long STATE_EXACT_MATCH = 1L;   // 01
    public static final long STATE_AMBIGUOUS = 2L;     // 10

    private final SpatialSearchContext ctx;

    public SpatialStagePipeline(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }

    public static class PipelinePrepResult {
        public final List<NameIndexAtom> fullyCoveredAtoms = new ArrayList<>();
        public final HashSkipTileQuadTree<NameIndexAtom> allObjectsTree = new HashSkipTileQuadTree<>();
        public final HashSkipTileQuadTree<NameIndexAtom> areaObjectsTree = new HashSkipTileQuadTree<>();
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
        public final NameIndexAtom refOwner;

        public ConcreteAssignmentPair(SpatialSearchResultPair parentPair, long resolvedMask, NameIndexAtom refOwner) {
            this.parentPair = parentPair;
            this.resolvedMask = resolvedMask;
            this.refOwner = refOwner;
        }
    }

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

    public static int countCoveredTokens(long mask, int totalTokens) {
        int count = 0;
        for (int i = 0; i < totalTokens; i++) {
            long state = (mask >> (i * 2)) & 3L;
            if (state != STATE_NO_MATCH) count++;
        }
        return count;
    }

    public static List<ConcreteAssignmentPair> expandAmbiguousPairsAllOrNothing(
            SpatialSearchResultPair pair, long mask1, long mask2, int totalTokens) {

        List<ConcreteAssignmentPair> permutations = new ArrayList<>(2);
        List<Integer> ambiguousIndices = new ArrayList<>();
        for (int i = 0; i < totalTokens; i++) {
            long state = (pair.combinedMask >> (i * 2)) & 3L;
            if (state == STATE_AMBIGUOUS) {
                ambiguousIndices.add(i);
            }
        }

        if (ambiguousIndices.isEmpty()) {
            permutations.add(new ConcreteAssignmentPair(pair, pair.combinedMask, null));
            return permutations;
        }

        long combinedMask1 = pair.combinedMask;
        for (int tokenIdx : ambiguousIndices) {
            combinedMask1 = setTokenState(combinedMask1, tokenIdx, STATE_EXACT_MATCH);
        }
        permutations.add(new ConcreteAssignmentPair(pair, combinedMask1, pair.atom1));

        long combinedMask2 = pair.combinedMask;
        for (int tokenIdx : ambiguousIndices) {
            combinedMask2 = setTokenState(combinedMask2, tokenIdx, STATE_EXACT_MATCH);
        }
        permutations.add(new ConcreteAssignmentPair(pair, combinedMask2, pair.atom2));

        return permutations;
    }

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

    private static int calculateTypeIntersection(NameIndexAtom a1, NameIndexAtom a2) {
        boolean a1Street = a1.isStreetBuilding();
        boolean a2Street = a2.isStreetBuilding();
        if (a1Street != a2Street) {
            return 1;
        }
        return 2;
    }

    private static void addCombinationResult(
            SpatialSearchResultsList list, 
            List<NameIndexAtom> atoms, 
            List<SpatialSearchToken> tokens,
            int typeIntersection) {

        int tCount = tokens.size();
        NameIndexAtom[] orderedAtoms = new NameIndexAtom[tCount];

        for (NameIndexAtom atom : atoms) {
            for (int i = 0; i < tCount; i++) {
                if (tokens.get(i).atoms.contains(atom)) {
                    orderedAtoms[i] = atom;
                }
            }
        }

        for (int i = 0; i < tCount; i++) {
            if (orderedAtoms[i] == null) {
                for (NameIndexAtom atom : atoms) {
                    if (tokens.get(i).index.containsKey(atom.id)) {
                        orderedAtoms[i] = atom;
                        break;
                    }
                }
            }
            if (orderedAtoms[i] == null && !atoms.isEmpty()) {
                orderedAtoms[i] = atoms.get(0);
            }
        }

        int maxZoom = 0;
        long mainTileId = 0;
        for (NameIndexAtom a : orderedAtoms) {
            if (a != null && a.coords != null && a.coords.bboxTileZoom > maxZoom) {
                maxZoom = a.coords.bboxTileZoom;
                mainTileId = a.coords.bboxTileId;
            }
        }

        for (int i = 0; i < tCount; i++) {
            list.linearResults.add(orderedAtoms[i]);
        }

        list.typeIntersections.add(typeIntersection);
        list.tileIds.add(mainTileId);
        list.tileZooms.add(maxZoom);

        int combinationIndex = list.getCombinations() - 1;
        list.quadTree.put(maxZoom, mainTileId, combinationIndex);
    }

    private boolean finalizeAndValidateStage(SpatialSearchResultsList stageList, String stageName) throws IOException {
        System.out.printf("[PIPELINE-LOG] Finalizing %s with %d raw combinations...\n", stageName, stageList.getCombinations());
        if (stageList.getCombinations() == 0 || ctx.isCancelled()) {
            return false;
        }

        stageList.loadObjectsAndCalcBuildings(ctx);
        if (ctx.isCancelled()) {
            return false;
        }

        List<SpatialSearchResult> res = stageList.sortResults(ctx, ctx.settings.DEDUPLICATE_RES);
        int validCount = res != null ? res.size() : 0;
        System.out.printf("[PIPELINE-LOG] %s after load & deduplicate: %d valid results.\n", stageName, validCount);

        return validCount > 0;
    }

    public List<SpatialSearchResultsList> runPipeline(List<SpatialSearchToken> tokens) throws IOException {
        List<SpatialSearchResultsList> combinations = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) {
            return combinations;
        }

        PipelinePrepResult prep = prepare(tokens);
        int totalTokens = tokens.size();

        System.out.printf("[PIPELINE-LOG] Query Tokens (%d): %s\n", totalTokens, tokens);
        System.out.printf("[PIPELINE-LOG] Step 1 candidates: %d fully covered atoms.\n", prep.fullyCoveredAtoms.size());

        // STEP 1
        if (!prep.fullyCoveredAtoms.isEmpty()) {
            SpatialSearchResultsList step1List = new SpatialSearchResultsList(tokens);
            for (NameIndexAtom atom : prep.fullyCoveredAtoms) {
                addCombinationResult(step1List, List.of(atom), tokens, 0);
            }

            if (finalizeAndValidateStage(step1List, "STEP 1 (Single)")) {
                combinations.add(step1List);
            }
        }

        // STEP 2
        HashSkipTileQuadTree<SpatialSearchResultPair> step2PairsTree = new HashSkipTileQuadTree<>();
        HashSkipTileQuadTreeJoiner<NameIndexAtom, NameIndexAtom> selfJoiner =
                new HashSkipTileQuadTreeJoiner<>(prep.allObjectsTree, prep.allObjectsTree);

        TLongHashSet addedPairIds = new TLongHashSet();
        int[] rawJoinedPairsCount = new int[1];

        selfJoiner.joinAllBuckets((e1, e2) -> {
            if (e1.objId == e2.objId) return;

            NameIndexAtom a1 = e1.obj;
            NameIndexAtom a2 = e2.obj;

            List<Long> masks1 = prep.objectMasks.get(a1.id);
            List<Long> masks2 = prep.objectMasks.get(a2.id);
            if (masks1 == null || masks2 == null) return;

            for (long m1 : masks1) {
                for (long m2 : masks2) {
                    long combinedMask = combine2BitMasks(m1, m2, totalTokens);

                    // Условие: пара должна покрывать хотя бы 2 разных токена
                    if (countCoveredTokens(combinedMask, totalTokens) < 2) {
                        continue;
                    }

                    int[] clippedBBox = new int[]{ a1.coords.bbox31[0], a1.coords.bbox31[1], a1.coords.bbox31[2], a1.coords.bbox31[3] };
                    SpatialSearchResultsList.clipBbox(clippedBBox, a2.coords.bbox31);

                    SpatialSearchResultPair pair = new SpatialSearchResultPair(a1, a2, clippedBBox, combinedMask);
                    if (addedPairIds.add(pair.pairId)) {
                        rawJoinedPairsCount[0]++;
                        step2PairsTree.addObject(pair, clippedBBox, pair.pairId);
                    }
                }
            }
        }, null, null);

        step2PairsTree.build();
        System.out.printf("[PIPELINE-LOG] Step 2 Joiner found %d raw unique pairs.\n", rawJoinedPairsCount[0]);

        SpatialSearchResultsList step2List = new SpatialSearchResultsList(tokens);
        List<SpatialSearchResultPair> validStep2Pairs = new ArrayList<>();
        int semanticRejectedCount = 0;

        for (TileEntry<SpatialSearchResultPair> entry : step2PairsTree.getTileEntries()) {
            SpatialSearchResultPair pair = entry.obj;

            List<Long> m1List = prep.objectMasks.get(pair.atom1.id);
            List<Long> m2List = prep.objectMasks.get(pair.atom2.id);
            long mask1 = (m1List != null && !m1List.isEmpty()) ? m1List.get(0) : 0L;
            long mask2 = (m2List != null && !m2List.isEmpty()) ? m2List.get(0) : 0L;

            List<ConcreteAssignmentPair> assignments = expandAmbiguousPairsAllOrNothing(pair, mask1, mask2, totalTokens);

            for (ConcreteAssignmentPair assignment : assignments) {
                // Если totalTokens <= 2, пара должна полностью покрывать запрос. Если > 2, она может быть поддеревом для Step 3.
                boolean isCovered = (totalTokens <= 2) ? isFullyCovered(assignment.resolvedMask, totalTokens) 
                                                       : countCoveredTokens(assignment.resolvedMask, totalTokens) >= 2;

                if (isCovered) {
                    if (acceptPairSemantic(ctx, pair, assignment.refOwner)) {
                        validStep2Pairs.add(pair);
                        if (isFullyCovered(assignment.resolvedMask, totalTokens)) {
                            int typeIntersection = calculateTypeIntersection(pair.atom1, pair.atom2);
                            addCombinationResult(step2List, List.of(pair.atom1, pair.atom2), tokens, typeIntersection);
                        }
                    } else {
                        semanticRejectedCount++;
                    }
                }
            }
        }

        System.out.printf("[PIPELINE-LOG] Step 2 Semantic Accepted Pairs: %d (Rejected: %d)\n", validStep2Pairs.size(), semanticRejectedCount);

        if (finalizeAndValidateStage(step2List, "STEP 2 (Pairs)")) {
            combinations.add(step2List);
        }

        // STEP 3 (Cascading Area Joins for 3+ Tokens)
        if (totalTokens > 2 && !validStep2Pairs.isEmpty()) {
            System.out.printf("[PIPELINE-LOG] Step 3 Starting with %d seed pairs...\n", validStep2Pairs.size());

            HashSkipTileQuadTree<SpatialSearchResultChain> currentChainTree = new HashSkipTileQuadTree<>();
            for (SpatialSearchResultPair pair : validStep2Pairs) {
                List<NameIndexAtom> initialAtoms = List.of(pair.atom1, pair.atom2);
                SpatialSearchResultChain chain = new SpatialSearchResultChain(initialAtoms, pair.bbox31, pair.combinedMask);
                currentChainTree.addObject(chain, chain.bbox31, chain.chainId);
            }
            currentChainTree.build();

            int maxAreaDepth = Math.min(totalTokens, 4);
            for (int depth = 3; depth <= maxAreaDepth; depth++) {
                if (currentChainTree.getTileEntries().isEmpty() || ctx.isCancelled()) break;

                HashSkipTileQuadTree<SpatialSearchResultChain> nextChainTree = new HashSkipTileQuadTree<>();
                HashSkipTileQuadTreeJoiner<SpatialSearchResultChain, NameIndexAtom> chainJoiner =
                        new HashSkipTileQuadTreeJoiner<>(currentChainTree, prep.areaObjectsTree);

                int[] joinedChainsCount = new int[1];

                chainJoiner.joinAllBuckets((eChain, eArea) -> {
                    SpatialSearchResultChain chain = eChain.obj;
                    NameIndexAtom areaAtom = eArea.obj;

                    if (chain.containsAtom(areaAtom.id)) return;

                    List<Long> areaMasks = prep.objectMasks.get(areaAtom.id);
                    long areaMask = (areaMasks != null && !areaMasks.isEmpty()) ? areaMasks.get(0) : 0L;
                    long newMask = combine2BitMasks(chain.combinedMask, areaMask, totalTokens);

                    if (newMask == chain.combinedMask) return;

                    int[] newBBox = chain.bbox31.clone();
                    SpatialSearchResultsList.clipBbox(newBBox, areaAtom.coords.bbox31);

                    SpatialSearchResultChain newChain = chain.extend(areaAtom, newBBox, newMask);
                    nextChainTree.addObject(newChain, newBBox, newChain.chainId);
                    joinedChainsCount[0]++;
                }, null, null);

                nextChainTree.build();
                System.out.printf("[PIPELINE-LOG] Step 3 Depth %d joined %d chains.\n", depth, joinedChainsCount[0]);

                SpatialSearchResultsList step3List = new SpatialSearchResultsList(tokens);
                for (TileEntry<SpatialSearchResultChain> entry : nextChainTree.getTileEntries()) {
                    SpatialSearchResultChain chain = entry.obj;
                    if (isFullyCovered(chain.combinedMask, totalTokens)) {
                        addCombinationResult(step3List, chain.atoms, tokens, 1);
                    }
                }

                if (finalizeAndValidateStage(step3List, "STEP 3 (Depth " + depth + ")")) {
                    combinations.add(step3List);
                }

                currentChainTree = nextChainTree;
            }
        }

        System.out.printf("[PIPELINE-LOG] Pipeline Finished. Total Combinations: %d\n", combinations.size());
        return combinations;
    }

    public static boolean acceptPairSemantic(SpatialSearchContext ctx, SpatialSearchResultPair pair, NameIndexAtom refOwner) {
        SpatialTextSearchSettings settings = ctx.settings;
        NameIndexAtom a1 = pair.atom1;
        NameIndexAtom a2 = pair.atom2;

        if (a1.id == a2.id) {
            return false;
        }

        int atomicCount = (a1.atomicObject() ? 1 : 0) + (a2.atomicObject() ? 1 : 0);
        if (atomicCount > settings.LIMIT_ATOMIC_OBJECTS) {
            return false;
        }

        if (settings.OPTIM_FLAG_POI_SAME_AS_CITY_STREET) {
            if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) {
                return false;
            }
        }

        boolean twoStreets = a1.isStreetBuilding() && a2.isStreetBuilding();
        boolean twoPOIs = !a1.isStreetBuilding() && !a2.isStreetBuilding();

        if (!settings.SEARCH_STREET_INTERSECTIONS && twoStreets) return false;
        if (!settings.SEARCH_POI_INTERSECTIONS && twoPOIs) return false;

        boolean hasPoiCategory = a1.isPoiCategory() || a2.isPoiCategory();
        boolean hasBuilding = a1.isBuilding() || a2.isBuilding();
        if (hasPoiCategory && (hasBuilding || a1.isStreetBuilding() || a2.isStreetBuilding())) {
            return false;
        }

        if ((a1.buildingOrRefInd >= 0) && a2.isStreetBuilding() && !a1.isCityStreetName()) return false;
        if ((a2.buildingOrRefInd >= 0) && a1.isStreetBuilding() && !a2.isCityStreetName()) return false;

        return true;
    }
}