package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;

import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Timing of the trees of tiles of the spatial search, {@link net.osmand.shared.search.core.HashSkipTileQuadTree},
 * its joiner and {@link net.osmand.shared.search.core.HashQuadTree} against the java originals, over the boxes of
 * {@link QuadTreeCompatTest}: the polygons of the regions and 20 000 random boxes. Each round builds a tree of all of
 * them, asks it for 1 500 boxes and whether anything is there, joins the boxes smaller than a region with every third
 * of them, and puts every box into a {@code HashQuadTree} and looks the queries up in it.
 *
 * <b>Not part of a normal run</b>: it only does anything when {@code OSMAND_QUAD_TREE_BENCHMARK} is set. It asserts
 * only that both find as many, so a slow machine never turns the build red. The native half is
 * {@code QuadTreeBenchmarkTest.kt} in OsmAnd-shared, over the boxes {@code QuadTreeCompatTest} wrote out.
 * <pre>
 * OSMAND_QUAD_TREE_BENCHMARK=1 OSMAND_TEST_HEAP=1g ./gradlew :OsmAnd-java:test --tests "*QuadTreeBenchmarkTest" --rerun -i
 * </pre>
 *
 * Best of five in ms; the native column is the cross platform half on the release simulator binary:
 * <pre>
 * what                            java    copy  native
 * build of 22 579 boxes           5.49    5.55    6.27
 * get 1 500                        110    24.6    47.4
 * contains 1 500                  0.10    0.03    0.06
 * join 18 683 x 6 228             6.34    6.37    13.2
 * hash quad tree                  1.46    1.36    1.97
 * </pre>
 * Most of a query is the tiles the index walk checks the box against: java takes the tile apart bit by
 * bit, the copy by masks. A join on Kotlin/Native still walks the index of each bucket twice as slowly
 * as java.
 */
public class QuadTreeBenchmarkTest {

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void quadTrees() throws IOException {
		Assume.assumeTrue("set OSMAND_QUAD_TREE_BENCHMARK to run", System.getenv("OSMAND_QUAD_TREE_BENCHMARK") != null);
		List<int[]> boxes = new ArrayList<>(QuadTreeCompatTest.regionBoxes());
		Random random = new Random(21102026);
		boxes.addAll(QuadTreeCompatTest.randomBoxes(random, 20_000));
		List<int[]> queries = new ArrayList<>();
		for (int i = 0; i < 1_500; i++) {
			queries.add(i % 3 == 0 ? boxes.get(random.nextInt(boxes.size())) : QuadTreeCompatTest.randomBoxes(random, 1).get(0));
		}
		List<int[]> first = new ArrayList<>();
		for (int[] b : boxes) {
			if ((long) b[2] - b[0] < (1 << 26) && (long) b[3] - b[1] < (1 << 26)) {
				first.add(b);
			}
		}
		List<int[]> second = new ArrayList<>();
		for (int i = 0; i < first.size(); i += 3) {
			second.add(first.get(i));
		}
		long[] best = new long[10];
		java.util.Arrays.fill(best, Long.MAX_VALUE);
		long[] jfound = new long[5];
		long[] kfound = new long[5];
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long[] t = new long[10];
			java.util.Arrays.fill(jfound, 0);
			java.util.Arrays.fill(kfound, 0);

			long started = System.nanoTime();
			HashSkipTileQuadTree<Integer> j = new HashSkipTileQuadTree<>();
			for (int i = 0; i < boxes.size(); i++) {
				j.addObject(i, boxes.get(i), i);
			}
			jfound[0] = j.build();
			t[0] = System.nanoTime() - started;
			started = System.nanoTime();
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k = new net.osmand.shared.search.core.HashSkipTileQuadTree<>();
			for (int i = 0; i < boxes.size(); i++) {
				k.addObject(i, boxes.get(i), i);
			}
			kfound[0] = k.build();
			t[1] = System.nanoTime() - started;

			started = System.nanoTime();
			for (int[] q : queries) {
				jfound[1] += j.get(q, null).size();
			}
			t[2] = System.nanoTime() - started;
			started = System.nanoTime();
			for (int[] q : queries) {
				kfound[1] += k.get(q).size();
			}
			t[3] = System.nanoTime() - started;

			started = System.nanoTime();
			for (int[] q : queries) {
				jfound[2] += j.contains(q) ? 1 : 0;
			}
			t[4] = System.nanoTime() - started;
			started = System.nanoTime();
			for (int[] q : queries) {
				kfound[2] += k.contains(q) ? 1 : 0;
			}
			t[5] = System.nanoTime() - started;

			HashSkipTileQuadTree<Integer> j1 = new HashSkipTileQuadTree<>();
			HashSkipTileQuadTree<Integer> j2 = new HashSkipTileQuadTree<>();
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k1 = new net.osmand.shared.search.core.HashSkipTileQuadTree<>();
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k2 = new net.osmand.shared.search.core.HashSkipTileQuadTree<>();
			for (int i = 0; i < first.size(); i++) {
				j1.addObject(i, first.get(i), i);
				k1.addObject(i, first.get(i), i);
			}
			for (int i = 0; i < second.size(); i++) {
				j2.addObject(i, second.get(i), i);
				k2.addObject(i, second.get(i), i);
			}
			j1.build();
			j2.build();
			k1.build();
			k2.build();
			long[] jpairs = {0};
			long[] kpairs = {0};
			started = System.nanoTime();
			new HashSkipTileQuadTreeJoiner<>(j1, j2).joinAllBuckets((e1, e2) -> jpairs[0]++);
			t[6] = System.nanoTime() - started;
			started = System.nanoTime();
			new net.osmand.shared.search.core.HashSkipTileQuadTreeJoiner<>(k1, k2).joinAllBuckets((e1, e2) -> kpairs[0]++);
			t[7] = System.nanoTime() - started;
			jfound[3] = jpairs[0];
			kfound[3] = kpairs[0];

			started = System.nanoTime();
			HashQuadTree<Integer> jh = new HashQuadTree<>(16);
			for (int i = 0; i < boxes.size(); i++) {
				int z = j.getNativeZoom(boxes.get(i));
				jh.put(z, HashQuadTree.encodeTileId31(z, boxes.get(i)[0], boxes.get(i)[1]), i);
			}
			for (int[] q : queries) {
				int z = j.getNativeZoom(q);
				long[] n = {0};
				jh.forEachMatch(z, HashQuadTree.encodeTileId31(z, q[0], q[1]), l -> n[0] += l.size());
				jfound[4] += n[0];
			}
			t[8] = System.nanoTime() - started;
			started = System.nanoTime();
			net.osmand.shared.search.core.HashQuadTree<Integer> kh = new net.osmand.shared.search.core.HashQuadTree<>(16);
			for (int i = 0; i < boxes.size(); i++) {
				int z = k.getNativeZoom(boxes.get(i));
				kh.put(z, net.osmand.shared.search.core.HashQuadTree.Companion.encodeTileId31(z, boxes.get(i)[0], boxes.get(i)[1]), i);
			}
			for (int[] q : queries) {
				int z = k.getNativeZoom(q);
				long[] n = {0};
				kh.forEachMatch(z, net.osmand.shared.search.core.HashQuadTree.Companion.encodeTileId31(z, q[0], q[1]), l -> {
					n[0] += l.size();
					return kotlin.Unit.INSTANCE;
				});
				kfound[4] += n[0];
			}
			t[9] = System.nanoTime() - started;
			if (round >= WARMUP_ROUNDS) {
				for (int n = 0; n < t.length; n++) {
					best[n] = Math.min(best[n], t[n]);
				}
			}
		}
		for (int n = 0; n < jfound.length; n++) {
			assertEquals("found " + n, jfound[n], kfound[n]);
		}
		System.out.println("### quad trees, best of " + MEASURED_ROUNDS + " in ms: java / copy");
		String[] names = {"build of " + boxes.size() + " boxes", "get " + queries.size(), "contains " + queries.size(),
				"join " + first.size() + " x " + second.size(), "hash quad tree"};
		for (int n = 0; n < names.length; n++) {
			System.out.println(String.format("%-28s %9.2f %9.2f   found %d", names[n], best[2 * n] / 1e6, best[2 * n + 1] / 1e6,
					jfound[n]));
		}
	}
}
