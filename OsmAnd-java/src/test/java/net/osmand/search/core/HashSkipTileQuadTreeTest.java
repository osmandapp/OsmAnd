package net.osmand.search.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;

public class HashSkipTileQuadTreeTest {

	/**
	 * Boxes added again after a build take the place of their old entries in the order they were added: the joins of
	 * the spatial search walk a tile in that order, and the order of the hashes of their ids changed its results.
	 */
	@Test
	public void entriesAddedAfterBuildKeepTheirOrder() {
		int[] box = {1 << 30, 1 << 29, (1 << 30) + 8, (1 << 29) + 8};
		long[] ids = {1000, 3, 77, 15, 512, 9, 64, 200};
		HashSkipTileQuadTree<Long> tree = new HashSkipTileQuadTree<>();
		for (long id : ids) {
			tree.addObject(id, box, id);
		}
		tree.build();
		List<Long> again = new ArrayList<>();
		for (int i = ids.length - 1; i >= 0; i -= 2) {
			tree.addObject(ids[i], box, ids[i]);
			again.add(ids[i]);
		}
		// added twice before the build: once in the order
		tree.addObject(ids[ids.length - 1], box, ids[ids.length - 1]);
		tree.build();
		tree.build();
		List<Long> expected = new ArrayList<>();
		for (long id : ids) {
			if (!again.contains(id)) {
				expected.add(id);
			}
		}
		expected.addAll(again);
		expected.add(expected.indexOf(ids[ids.length - 1]) + 1, ids[ids.length - 1]);
		List<Long> entries = new ArrayList<>();
		for (TileEntry<Long> e : tree.getTileEntries()) {
			entries.add(e.objId);
		}
		assertEquals(expected, entries);
	}
}
