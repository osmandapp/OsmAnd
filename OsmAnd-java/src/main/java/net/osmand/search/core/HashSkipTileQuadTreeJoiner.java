package net.osmand.search.core;

import java.util.List;

import net.osmand.search.core.HashSkipTileQuadTree.SkipStats;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.search.core.HashSkipTileQuadTree.ZoomBucket;
import net.osmand.search.core.HashSkipTileQuadTree.ZoomBucketIndexTreeIterator;
import static net.osmand.search.core.HashSkipTileQuadTree.encodeTileId;

/**
 * High-performance spatial join algorithm leveraging
 * {@link ZoomBucketIndexTreeIterator} for fast block skipping between different
 * zoom buckets (e.g. Z3 vs Z15).
 *
 * @param <T> Payload type for the first tree.
 * @param <R> Payload type for the second tree.
 */
public class HashSkipTileQuadTreeJoiner<T, R> {

	@FunctionalInterface
	public interface JoinCallback<T, R> {
		void onIntersection(TileEntry<T> e1, TileEntry<R> e2);
	}

	private final HashSkipTileQuadTree<T> tree1;
	private final HashSkipTileQuadTree<R> tree2;

	public HashSkipTileQuadTreeJoiner(HashSkipTileQuadTree<T> tree1, HashSkipTileQuadTree<R> tree2) {
		this.tree1 = tree1;
		this.tree2 = tree2;
	}

	/**
	 * Performs spatial join across all zoom bucket pairs between tree1 and tree2.
	 */
	public void joinAllBuckets(JoinCallback<T, R> callback, HashSkipTileQuadTree.SkipStats stats1,
			HashSkipTileQuadTree.SkipStats stats2) {
		for (int z1 = tree1.minZoom; z1 <= tree1.maxZoom; z1++) {
			ZoomBucket b1 = tree1.getZoomBucket(z1);
			if (b1 == null || b1.len == 0) {
				continue;
			}
			for (int z2 = tree2.minZoom; z2 <= tree2.maxZoom; z2++) {
				ZoomBucket b2 = tree2.getZoomBucket(z2);
				if (b2 == null || b2.len == 0) {
					continue;
				}
				joinBuckets(z1, z2, callback, stats1, stats2);
			}
		}
	}

	/**
	 * Joins two specific zoom buckets z1 and z2 using tree skipping iterators.
	 */
	public void joinBuckets(int z1, int z2, JoinCallback<T, R> callback, HashSkipTileQuadTree.SkipStats stats1,
			HashSkipTileQuadTree.SkipStats stats2) {
		ZoomBucket b1 = tree1.getZoomBucket(z1);
		ZoomBucket b2 = tree2.getZoomBucket(z2);

		if (b1 == null || b2 == null || b1.len == 0 || b2.len == 0) {
			return;
		}
		if (stats1 != null) {
			stats1.totalSize(b1.len);
		}
		if (stats2 != null) {
			stats2.totalSize(b2.len);
		}
		ZoomBucketIndexTreeIterator itA = new ZoomBucketIndexTreeIterator(b1);
	    ZoomBucketIndexTreeIterator itB = new ZoomBucketIndexTreeIterator(b2);

		if (z1 > z2) {
			this.<R, T>joinBucketsOrdered(b2, tree2.getTileEntries(), itB, 
					b1, tree1.getTileEntries(), itA,
					(e2, e1) -> callback.onIntersection(e1, e2), stats2, stats1);
		} else {
			this.<T, R>joinBucketsOrdered(b1, tree1.getTileEntries(), itA, b2, tree2.getTileEntries(), itB, callback, 
					stats1, stats2);
		}
	}

	private <A, B> void joinBucketsOrdered(
	        ZoomBucket bA, List<TileEntry<A>> entriesA, ZoomBucketIndexTreeIterator itA,
	        ZoomBucket bB, List<TileEntry<B>> entriesB, ZoomBucketIndexTreeIterator itB,
	        JoinCallback<A, B> callback, SkipStats statsA, SkipStats statsB) {

	    int zDiff = bB.z - bA.z; 
	    int shiftBits = Math.abs(zDiff) * 2;

	    int iA = bA.start, endA = bA.start + bA.len;
	    int iB = bB.start, endB = bB.start + bB.len;

	    while (iA < endA && iB < endB) {
	        TileEntry<A> eA = entriesA.get(iA);
	        TileEntry<B> eB = entriesB.get(iB);
	        long codeA = eA.tileId;
	        long codeB = eB.tileId;

	        long normA = (zDiff < 0) ? (codeA >> shiftBits) : codeA;
	        long normB = (zDiff > 0) ? (codeB >> shiftBits) : codeB;

	        if (normA == normB) {
	            int endMatchB = iB;
	            while (endMatchB < endB) {
	                long cB = entriesB.get(endMatchB).tileId;
	                long nB = (zDiff > 0) ? (cB >> shiftBits) : cB;
					if (statsB != null) {
						statsB.recordInspection(entriesB.get(endMatchB));
					}
	                if (nB != normA) break;
	                endMatchB++;
	            }

	            int currA = iA;
	            while (currA < endA) {
	                TileEntry<A> entryA = entriesA.get(currA);
					if (statsA != null) {
						statsA.recordInspection(entryA);
					}
	                long cA = entryA.tileId;
	                long nA = (zDiff < 0) ? (cA >> shiftBits) : cA;
	                if (nA != normA) break;

	                for (int mB = iB; mB < endMatchB; mB++) {
	                    TileEntry<B> entryB = entriesB.get(mB);
						if (HashSkipTileQuadTree.intersectsBBox(entryA.bbox31, entryB.bbox31)) {
							int px = Math.max(entryA.bbox31[0], entryB.bbox31[0]);
							int py = Math.max(entryA.bbox31[1], entryB.bbox31[1]);
							long intersectAId = encodeTileId(px >> (31 - entryA.z), py >> (31 - entryA.z));
							long intersectBId = encodeTileId(px >> (31 - entryB.z), py >> (31 - entryB.z));
							if (intersectAId == entryA.tileId && intersectBId == entryB.tileId) {
								callback.onIntersection(entryA, entryB);
							}
						}
	                }
	                currA++;
	            }

	            iA = currA;
	            iB = endMatchB;
	        } else if (normA < normB) {
	            itA.sync(iA);
	            long targetA = (zDiff < 0) ? (normB << shiftBits) : normB;
	            int nextA = itA.skipToTileId(iA, targetA, statsA);
	            iA = (nextA > iA && nextA < endA) ? nextA : iA + 1;
	        } else {
	            itB.sync(iB);
	            long targetB = (zDiff > 0) ? (normA << shiftBits) : normA;
	            int nextB = itB.skipToTileId(iB, targetB, statsB);
	            iB = (nextB > iB && nextB < endB) ? nextB : iB + 1;
	        }
	    }
	}

}