package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.search.core.spatial.SpatialPipelineSearch.SpatialPipelineContext;

public class SpatialPipelineStats {
	
	
	public static void printTokenTree(SpatialPipelineContext prep) {
	    if (prep == null || prep.objectsById == null) {
	        System.out.println("=== TOKEN TREE: Prep or objectsById is null ===");
	        return;
	    }

	    List<SpatialSearchToken> tokens = prep.tokens;
	    int totalObjects = prep.objectsById.size();
	    int totalTokens = tokens.size();

	    // 1. Group objects by 2-bit mask and calculate global token frequencies
	    TLongObjectHashMap<Integer> maskFrequencies = new TLongObjectHashMap<>();
	    long[] tokenGlobalCounts = new long[totalTokens];

	    for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
	        long mask = obj.mainMask;
	        
	        Integer currentCount = maskFrequencies.get(mask);
	        maskFrequencies.put(mask, currentCount == null ? 1 : currentCount + 1);

	        // Extract active tokens using 2-bit state logic (shift = t * 2)
	        for (int t = 0; t < totalTokens; t++) {
	            if (SpatialPipelineObjectRes.getTokenState(mask, t) != 0) { // 0 = STATE_NO_MATCH
	                tokenGlobalCounts[t]++;
	            }
	        }
	    }

	    // 2. Group and sort masks by number of active tokens
	    List<MaskEntry> sortedMasks = new ArrayList<>();
	    int sumOfAllMaskObjects = 0;

	    long[] maskKeys = maskFrequencies.keys();
	    for (long mask : maskKeys) {
	        int count = maskFrequencies.get(mask);
	        sumOfAllMaskObjects += count;
	        int activeCount = SpatialPipelineObjectRes.countCoveredTokens(mask);
	        sortedMasks.add(new MaskEntry(mask, count, activeCount));
	    }
	    
	    // Sort by mask length (number of active tokens) ascending: 1 token, 2 tokens, etc.
	    sortedMasks.sort(Comparator.comparingInt(m -> m.tokenLength));

	    // 3. Build Trie using Rare-First token ordering
	    TreeNode root = new TreeNode("ROOT");

	    for (MaskEntry entry : sortedMasks) {
	        long mask = entry.mask;
	        int count = entry.count;

	        // Extract indices of active tokens for this mask
	        List<Integer> activeTokenIndices = new ArrayList<>();
	        for (int t = 0; t < totalTokens; t++) {
	            if (SpatialPipelineObjectRes.getTokenState(mask, t) != 0) {
	                activeTokenIndices.add(t);
	            }
	        }

	        if (activeTokenIndices.isEmpty()) {
	            continue;
	        }

	        // Rare-First order: sort tokens in this mask by global frequency ascending
	        activeTokenIndices.sort((a, b) -> {
	            int cmp = Long.compare(tokenGlobalCounts[a], tokenGlobalCounts[b]);
	            if (cmp != 0) return cmp;
	            return Integer.compare(a, b); // deterministic tie-breaker
	        });

	        // Traverse / insert into Trie
	        TreeNode current = root;
	        current.totalCount += count;

	        for (int tokenIdx : activeTokenIndices) {
	            String tokenName = tokens.get(tokenIdx).getToken();
	            current = current.children.computeIfAbsent(tokenName, TreeNode::new);
	            current.totalCount += count;
	        }

	        // Set exact count for this exact 2-bit mask combination
	        current.exactCount += count;
	    }

	    // 4. Verification Check
	    int treeSum = 0;
	    for (TreeNode child : root.children.values()) {
	        treeSum += child.totalCount;
	    }

	    System.out.println("==========================================================================================");
	    System.out.println("=== RARE-FIRST TOKEN TREE (2-Bit Encoded Masks) ===");
	    System.out.printf("Total Pipeline Objects : %,d\n", totalObjects);
	    System.out.printf("Sum of Mask Frequencies: %,d\n", sumOfAllMaskObjects);
	    System.out.printf("Sum of Root Tree Nodes : %,d\n", treeSum);
	    System.out.printf("Unaccounted Objects    : %,d %s\n", (totalObjects - treeSum), 
	            (totalObjects == treeSum ? "✓ (PERFECT MATCH)" : "✗ (MISMATCH!)"));
	    System.out.println("==========================================================================================");

	    // Print roots sorted by totalCount DESCENDING
	    List<TreeNode> rootChildren = root.getChildrenSortedByCountDescending();
	    for (int i = 0; i < rootChildren.size(); i++) {
	        boolean isLast = (i == rootChildren.size() - 1);
	        printNode(rootChildren.get(i), "", isLast, totalObjects);
	    }
	    System.out.println("==========================================================================================");
	}


	private static class MaskEntry {
	    final long mask;
	    final int count;
	    final int tokenLength;

	    MaskEntry(long mask, int count, int tokenLength) {
	        this.mask = mask;
	        this.count = count;
	        this.tokenLength = tokenLength;
	    }
	}

	private static class TreeNode {
	    final String tokenName;
	    int totalCount = 0;  // Total objects in branch (Exact + Children)
	    int exactCount = 0;  // Objects ending with exact token set
	    final Map<String, TreeNode> children = new LinkedHashMap<>();

	    TreeNode(String tokenName) {
	        this.tokenName = tokenName;
	    }

	    List<TreeNode> getChildrenSortedByCountDescending() {
	        List<TreeNode> list = new ArrayList<>(children.values());
	        list.sort((a, b) -> Integer.compare(b.totalCount, a.totalCount));
	        return list;
	    }
	}

	private static void printNode(TreeNode node, String indent, boolean isLast, int totalObjects) {
	    double share = (node.totalCount * 100.0) / totalObjects;
	    String marker = isLast ? "└── " : "├── ";

	    int comboCount = node.totalCount - node.exactCount;
	    String countInfo;
	    if (comboCount > 0 && node.exactCount > 0) {
	        countInfo = String.format("[Exact: %,d + Combos: %,d = Total: %,d]", node.exactCount, comboCount, node.totalCount);
	    } else if (comboCount > 0) {
	        countInfo = String.format("[Combos: %,d = Total: %,d]", comboCount, node.totalCount);
	    } else {
	        countInfo = String.format("[Exact: %,d]", node.exactCount);
	    }

	    System.out.printf("%s%s%-18s : %6.2f%% | %s\n", 
	            indent, marker, "[" + node.tokenName + "]", share, countInfo);

	    List<TreeNode> children = node.getChildrenSortedByCountDescending();
	    for (int i = 0; i < children.size(); i++) {
	        boolean childIsLast = (i == children.size() - 1);
	        printNode(children.get(i), indent + (isLast ? "    " : "│   "), childIsLast, totalObjects);
	    }
	}
	
	
	//////////////////////////////////////////////
	////////////////////////////////////////////////
	//////////////////////////////////////////////
	public static void printTree(SpatialPipelineContext prep) {
	    if (prep == null || prep.objectsById == null) {
	        System.out.println("=== MASK DISTRIBUTION: Prep or objectsById is null ===");
	        return;
	    }

	    List<SpatialSearchToken> tokens = prep.tokens;
	    int totalObjects = prep.objectsById.size();

	    // Group masks and count their frequencies using TLongObjectHashMap
	    TLongObjectHashMap<Integer> maskFrequencies = new TLongObjectHashMap<Integer>();

	    for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
			while (obj != null) {
				long mask = obj.mainMask;
				Integer count = maskFrequencies.get(mask);
				maskFrequencies.put(mask, count == null ? 1 : count + 1);
				obj = obj.otherVariants;
			}
	    }

	    // Convert the map entries to a list for sorting by popularity
	    List<long[]> maskList = new ArrayList<>(); // Entry format: [mask, count]
	    maskFrequencies.forEachEntry((mask, count) -> {
	        maskList.add(new long[]{mask, count.longValue()});
	        return true;
	    });

	    // Sort in descending order by frequency (most frequent masks first)
	    maskList.sort((a, b) -> Long.compare(b[1], a[1]));

	    // Print summary report
	    System.out.println("==========================================================================================");
	    System.out.printf("=== PREPARE: TOP POPULAR MASKS DISTRIBUTION (Total Objects: %,d | Unique Masks: %,d) ===\n", 
	            totalObjects, maskList.size());
	    System.out.println("==========================================================================================");
	    System.out.printf("%-6s | %-8s | %-12s | %-10s | %-45s\n", 
	            "Rank", "Bits", "Count", "% Share", "Tokens (Bitmask representation)");
	    System.out.println("------------------------------------------------------------------------------------------");

	    int topN = Math.min(50, maskList.size());
	    for (int i = 0; i < topN; i++) {
	        long mask = maskList.get(i)[0];
	        long count = maskList.get(i)[1];
	        int bitCount = SpatialPipelineObjectRes.countCoveredTokens(mask);
	        double share = (count * 100.0) / totalObjects;

	        String tokensRepresentation = SpatialPipelineObjectRes.formatMaskTokens(mask, tokens);

	        System.out.printf("#%-5d | %-8d | %,12d | %6.2f%%    | %-45s\n", 
	                (i + 1), bitCount, count, share, tokensRepresentation);
	    }
	    System.out.println("==========================================================================================");
	}

	
	/////////////////////////////////////////////////
	public static void evaluateMaskIntersections(SpatialPipelineContext prep) {
	    if (prep == null || prep.objectsById == null || prep.tokens == null) {
	        System.out.println("=== MASK COMBINATION COVERAGE ANALYSIS: Invalid input ===");
	        return;
	    }

	    long startTime = System.nanoTime();
	    int totalTokens = prep.tokens.size();

	    // 1. Calculate mask frequency distribution across objects
	    TLongObjectHashMap<TLongHashSet> maskFreqs = new TLongObjectHashMap<TLongHashSet>();
	    int objs = 0;
	    TLongObjectHashMap<TLongHashSet> maskAreaFreqs = new TLongObjectHashMap<TLongHashSet>();
	    int areas = 0;
	    for (SpatialPipelineObjectRes obj : prep.objectsById.valueCollection()) {
	        long mask = obj.mainMask;
	        TLongHashSet ids = maskFreqs.get(mask);
			if (ids == null) {
				ids = new TLongHashSet();
				maskFreqs.put(mask, ids);
			}
			maskFreqs.get(mask).add(obj.mainAtom.id);
	        objs++;
			if (obj.mainAtom.isGeoArea()) {
				ids = maskAreaFreqs.get(mask);
				if (ids == null) {
					ids = new TLongHashSet();
					maskAreaFreqs.put(mask, ids);
				}
				maskAreaFreqs.get(mask).add(obj.mainAtom.id);
				areas++;
			}
	    }

	    long[] masks = maskFreqs.keys();
	    long[] areaMasks = maskAreaFreqs.keys();
	    // Map: CoveredTokens -> (ObjectCount -> CombinationCount)
	    // Example: 9 Tokens -> {1 obj -> 12 combinations, 2 objs -> 300 combinations, 3 objs -> 251 combinations}
	    Map<Integer, Map<Integer, Long>> stats = new HashMap<>();
	    Map<Integer, TLongHashSet> statByIds = new HashMap<>();
	    for (int t = totalTokens; t >= 0; t--) {
	        stats.put(t, new HashMap<Integer, Long>());
	        statByIds.put(t, new TLongHashSet());
	    }

	    // --- DEPTH 1: Single Objects (1 Mask) ---
		for (int i = 0; i < masks.length; i++) {
			long maskA = masks[i];
			int covered = SpatialPipelineObjectRes.countCoveredTokens(maskA);
			Map<Integer, Long> depthMap = stats.get(covered);
			depthMap.put(1, depthMap.getOrDefault(1, 0l) + maskFreqs.get(maskA).size());
			statByIds.get(covered).addAll(maskFreqs.get(maskA));
		}

	    // --- DEPTH 2: Pairs (2 Masks) ---
	    for (int i = 0; i < masks.length; i++) {
			long maskA = masks[i];
			TLongHashSet aIds = maskFreqs.get(maskA);
			for (int j = i; j < masks.length; j++) {
				long maskB = masks[j];
				if (!SpatialPipelineObjectRes.allowed(maskA, maskB)) {
					continue;
				}
				long combined = SpatialPipelineObjectRes.combine2BitMasks(maskA, maskB);
				TLongHashSet bIds = maskFreqs.get(maskB);
				int coveredAB = SpatialPipelineObjectRes.countCoveredTokens(combined);
				
				Map<Integer, Long> depthMap2 = stats.get(coveredAB);
				long totalAB = (long)aIds.size() * (long)bIds.size();
				depthMap2.put(2, depthMap2.getOrDefault(2, 0l) + totalAB);
				
				statByIds.get(coveredAB).addAll(maskFreqs.get(maskA));
				statByIds.get(coveredAB).addAll(maskFreqs.get(maskB));
				
				// depth 3
				for (int k = 0; k < areaMasks.length; k++) {
	                long maskC = areaMasks[k];
	                if (!SpatialPipelineObjectRes.allowed(combined, maskC)) {
						continue;
					}
	                long combinedABC = SpatialPipelineObjectRes.combine2BitMasks(combined, maskC);
	                TLongHashSet cIds = maskAreaFreqs.get(maskC);
	                int coveredABC = SpatialPipelineObjectRes.countCoveredTokens(combinedABC);
	                
	                Map<Integer, Long> depthMap3 = stats.get(coveredABC);
					long totalABC = totalAB * (long) cIds.size();
					depthMap3.put(3, depthMap3.getOrDefault(3, 0l) + totalABC);
	                
	                statByIds.get(coveredABC).addAll(maskFreqs.get(maskA));
					statByIds.get(coveredABC).addAll(maskFreqs.get(maskB));
					statByIds.get(coveredABC).addAll(maskAreaFreqs.get(maskC));
					
					// depth 4
//					for (int m = 0; m < areaMasks.length; m++) {
//						long maskD = areaMasks[m];
//						if (!SpatialObjectRes.allowed(combinedABC, maskD)) {
//							continue;
//						}
//						long combinedABCD = SpatialObjectRes.combine2BitMasks(combinedABC, maskD, totalTokens);
//						TLongHashSet dIds = maskAreaFreqs.get(maskD);
//						int coveredABCD = SpatialObjectRes.countCoveredTokens(combinedABCD);
//
//						Map<Integer, Long> depthMap4 = stats.get(coveredABCD);
//						long totalABCD = totalABC * (long) dIds.size();
//						depthMap4.put(4, depthMap4.getOrDefault(4, 0L) + totalABCD);
//
//						statByIds.get(coveredABCD).addAll(maskFreqs.get(maskA));
//						statByIds.get(coveredABCD).addAll(maskFreqs.get(maskB));
//						statByIds.get(coveredABCD).addAll(maskAreaFreqs.get(maskC));
//						statByIds.get(coveredABCD).addAll(maskAreaFreqs.get(maskD));
//					}
	            }
			}
	    }
	    long durationNs = System.nanoTime() - startTime;
	    // 2. Print Summary Report
	    System.out.println("==========================================================================================");
	    System.out.printf("=== MASK COMBINATION COVERAGE ANALYSIS (Masks: %,d (%,d), Area masks: %,d (%,d)| Time: %.3f ms) ===\n", 
	            maskFreqs.size(), objs, maskAreaFreqs.size(), areas, durationNs / 1_000_000.0);
	    System.out.println("==========================================================================================");

		for (int t = totalTokens; t > 0; t--) {
			Map<Integer, Long> depthMap = stats.get(t);

			long by1 = depthMap.getOrDefault(1, 0L);
			long by2 = depthMap.getOrDefault(2, 0L);
			long by3 = depthMap.getOrDefault(3, 0L);
			long by4 = depthMap.getOrDefault(4, 0L);
			long totalCombos = statByIds.get(t).size();
			String label = String.format("%2d Tokens Covered", t);
			System.out.printf("%-15s (%7s): by 1 obj: %6s, by 2 objs: %7s, by 3 objs: %7s, by 4 objs: %7s\n", label,
					formatCompactNumber(totalCombos), formatCompactNumber(by1), formatCompactNumber(by2),
					formatCompactNumber(by3), formatCompactNumber(by4));
		}
	    System.out.println("==========================================================================================");
	}
	
	private static String formatCompactNumber(long number) {
	    if (number < 1_000) return String.valueOf(number);
	    if (number < 1_000_000) return String.format(java.util.Locale.US, "%.1fK", number / 1_000.0);
	    if (number < 1_000_000_000L) return String.format(java.util.Locale.US, "%.1fM", number / 1_000_000.0);
	    if (number < 1_000_000_000_000L) return String.format(java.util.Locale.US, "%.1fB", number / 1_000_000_000.0);
	    return String.format(java.util.Locale.US, "%.1fT", number / 1_000_000_000_000.0);
	}
}
