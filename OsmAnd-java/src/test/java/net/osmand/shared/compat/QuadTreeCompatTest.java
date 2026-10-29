package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree.TileEntry;
import net.osmand.search.core.HashSkipTileQuadTreeJoiner;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.MapUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * The trees of tiles the spatial search keeps its boxes in: {@link net.osmand.shared.search.core.HashSkipTileQuadTree},
 * its joiner and {@link net.osmand.shared.search.core.HashQuadTree} against the java originals, fed the same boxes.
 *
 * The boxes are those of every polygon of {@code regions.ocbf}, the data the benchmarks of the java tree use, and 20 000
 * random ones of every size: points, boxes of a tile, of a region, on the edges of the world, and a few of the world. Each tree
 * of three settings of zooms takes them, half with ids of their own, and is built; the tile entries in their order,
 * the boxes found for each query box, in their order, at all zooms and at some, whether any is there, and every pair
 * the joiner calls back, in its order, between a tree and itself and between two trees of other settings, are the same
 * as java's, and so are the buckets of each zoom and the index each bucket skips with. Then more boxes are added to the built trees, some replacing those of an id, and the trees built again:
 * the entries of a tile may then come in another order, as both take the replaced entries in the hash order of their
 * own map, so the entries, the boxes found and the pairs are compared sorted. The pairs the joiner finds are also held
 * to the pairs of boxes that intersect, each once. {@code HashQuadTree} runs the same random puts, deletes and lookups.
 *
 * It writes what java answered, and the boxes it asked about, to {@code build/quad-tree-java.txt}, for
 * {@code HashSkipTileQuadTreeTest} in OsmAnd-shared to ask the copy wherever it runs.
 */
public class QuadTreeCompatTest {

	private static final File REGIONS = new File("src/main/resources/net/osmand/map/regions.ocbf");
	private static final File JAVA_DUMP = new File("build/quad-tree-java.txt");
	private static final int RANDOM_BOXES = 20_000;
	private static final int QUERIES = 1_500;
	/** minZoom, maxZoom and the indexed zooms of each tree compared. */
	static final int[][][] SETTINGS = {
			{{HashSkipTileQuadTree.DEFALT_MIN_ZOOM, HashSkipTileQuadTree.DEFALT_MAX_ZOOM}, HashSkipTileQuadTree.DEFAULT_INDEXED_ZOOMS},
			{{2, 14}, {}},
			{{0, 18}, {2, 4, 6, 8, 10, 12, 14}}};

	private static List<int[]> boxes;
	private static List<int[]> queries;
	private static List<int[]> added;
	private static Writer dump;
	private static int compared;

	@BeforeClass
	public static void setUp() throws IOException {
		SearchCompat.assumeEnabled();
		boxes = new ArrayList<>(regionBoxes());
		Random random = new Random(21102026);
		boxes.addAll(randomBoxes(random, RANDOM_BOXES));
		queries = new ArrayList<>();
		for (int i = 0; i < QUERIES; i++) {
			queries.add(i % 3 == 0 ? boxes.get(random.nextInt(boxes.size())) : randomBoxes(random, 1).get(0));
		}
		added = randomBoxes(random, 2_000);
		JAVA_DUMP.getParentFile().mkdirs();
		dump = Files.newBufferedWriter(new File(JAVA_DUMP.getPath() + ".part").toPath(), StandardCharsets.UTF_8);
		writeBoxes("B", boxes);
		writeBoxes("Q", queries);
		writeBoxes("A", added);
	}

	@AfterClass
	public static void written() throws IOException {
		if (!SearchCompat.enabled() || dump == null) {
			return;
		}
		dump.close();
		dump = null;
		Files.move(new File(JAVA_DUMP.getPath() + ".part").toPath(), JAVA_DUMP.toPath(), StandardCopyOption.REPLACE_EXISTING);
		System.out.println("QuadTreeCompatTest: " + boxes.size() + " boxes, " + queries.size() + " queries, " + compared
				+ " answers compared");
	}

	private static void writeBoxes(String kind, List<int[]> list) throws IOException {
		for (int[] b : list) {
			dump.write(kind + "\t" + b[0] + "\t" + b[1] + "\t" + b[2] + "\t" + b[3] + "\n");
		}
	}

	private static void expect(String name, String java) throws IOException {
		dump.write("E\t" + name + "\t" + java.length() + "\t" + NameIndexCompatTest.fnv(java) + "\n");
	}

	/** The box of every polygon of every region, regions by their download name. */
	static List<int[]> regionBoxes() throws IOException {
		OsmandRegions regions = new OsmandRegions(REGIONS.getPath());
		Map<String, LinkedList<BinaryMapDataObject>> all = regions.cacheAllCountries();
		// in the order of the names, the polygons of no region first
		List<String> names = new ArrayList<>(all.keySet());
		names.sort(java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
		List<int[]> res = new ArrayList<>();
		for (String name : names) {
			LinkedList<BinaryMapDataObject> objects = all.get(name);
			for (BinaryMapDataObject o : objects) {
				int[] c = o.getCoordinates();
				if (c == null || c.length < 2) {
					continue;
				}
				int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
				for (int i = 0; i + 1 < c.length; i += 2) {
					minX = Math.min(minX, c[i]);
					maxX = Math.max(maxX, c[i]);
					minY = Math.min(minY, c[i + 1]);
					maxY = Math.max(maxY, c[i + 1]);
				}
				res.add(new int[] {minX, minY, maxX, maxY});
			}
		}
		return res;
	}

	/** Boxes of every size, inside the world of 31 bits. */
	static List<int[]> randomBoxes(Random random, int count) {
		List<int[]> res = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			int kind = random.nextInt(8);
			if (kind == 6 && random.nextInt(50) != 0) {
				// a box of the world is in every answer: a few of them
				kind = 5;
			}
			int size;
			switch (kind) {
			case 0 -> size = 0;
			case 1 -> size = random.nextInt(1 << 12);
			case 2 -> size = random.nextInt(1 << 16);
			case 3 -> size = random.nextInt(1 << 20);
			case 4 -> size = random.nextInt(1 << 24);
			case 5 -> size = random.nextInt(1 << 28);
			case 6 -> size = Integer.MAX_VALUE;
			default -> size = random.nextInt(1 << 18);
			}
			int x = random.nextInt(Integer.MAX_VALUE);
			int y = random.nextInt(Integer.MAX_VALUE);
			if (kind == 7) {
				// on an edge of the world
				x = random.nextBoolean() ? 0 : Integer.MAX_VALUE - size;
			}
			int x2 = (int) Math.min((long) x + size, Integer.MAX_VALUE);
			int y2 = (int) Math.min((long) y + random.nextInt(Math.max(1, size)), Integer.MAX_VALUE);
			if (kind == 6) {
				x = 0;
				y = 0;
				x2 = Integer.MAX_VALUE;
				y2 = Integer.MAX_VALUE;
			}
			res.add(new int[] {x, y, x2, y2});
		}
		return res;
	}

	private static HashSkipTileQuadTree<Integer> javaTree(int s, List<int[]> list) {
		HashSkipTileQuadTree<Integer> t = new HashSkipTileQuadTree<>(SETTINGS[s][0][0], SETTINGS[s][0][1], SETTINGS[s][1]);
		for (int i = 0; i < list.size(); i++) {
			if (i % 2 == 0) {
				t.addObject(i, list.get(i));
			} else {
				t.addObject(i, list.get(i), 1_000_000L + i);
			}
		}
		return t;
	}

	private static net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> copyTree(int s, List<int[]> list) {
		net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> t = new net.osmand.shared.search.core.HashSkipTileQuadTree<>(
				SETTINGS[s][0][0], SETTINGS[s][0][1], SETTINGS[s][1]);
		for (int i = 0; i < list.size(); i++) {
			if (i % 2 == 0) {
				t.addObject(i, list.get(i));
			} else {
				t.addObject(i, list.get(i), 1_000_000L + i);
			}
		}
		return t;
	}

	/**
	 * More boxes on top of a built tree: new ids, and every 7th box under the id of a box already there. Only the boxes
	 * smaller than a region when {@code small}, for the joins.
	 */
	private static void addMore(HashSkipTileQuadTree<Integer> j, net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k,
			boolean small) {
		for (int i = 0; i < added.size(); i++) {
			int[] b = added.get(i);
			if (small && ((long) b[2] - b[0] >= (1 << 26) || (long) b[3] - b[1] >= (1 << 26))) {
				continue;
			}
			long id = i % 7 == 0 ? 1_000_000L + 2 * i + 1 : 2_000_000L + i;
			j.addObject(-i, b, id);
			k.addObject(-i, b, id);
		}
	}

	static String javaEntries(List<TileEntry<Integer>> list, boolean skips) {
		StringBuilder sb = new StringBuilder();
		for (TileEntry<Integer> e : list) {
			sb.append(e.objId).append(':').append(e.obj).append(':').append(e.z).append(':').append(e.tileId);
			if (skips) {
				sb.append(':').append(e.skipNextTileId);
			}
			sb.append(';');
		}
		return sb.toString();
	}

	static String copyEntries(List<net.osmand.shared.search.core.HashSkipTileQuadTree.TileEntry<Integer>> list, boolean skips) {
		StringBuilder sb = new StringBuilder();
		for (net.osmand.shared.search.core.HashSkipTileQuadTree.TileEntry<Integer> e : list) {
			sb.append(e.getObjId()).append(':').append(e.getObj()).append(':').append(e.getZ()).append(':').append(e.getTileId());
			if (skips) {
				sb.append(':').append(e.getSkipNextTileId());
			}
			sb.append(';');
		}
		return sb.toString();
	}

	/**
	 * The buckets of a built tree and the index of each: zoom, start and length of the bucket, and per indexed zoom the
	 * starts of the blocks of a parent tile, their starts in the zoom below and their parent tiles. Read by reflection,
	 * the same names in java and in the copy, as neither shows them: what the walks skip with, which their answers do
	 * not tell.
	 */
	static String buckets(Object tree) {
		StringBuilder sb = new StringBuilder();
		Object[] buckets = (Object[]) field(tree, "zoomBuckets");
		for (Object b : buckets) {
			if (b == null) {
				sb.append("-;");
				continue;
			}
			sb.append(field(b, "z")).append(':').append(field(b, "start")).append(':').append(field(b, "len")).append(':')
					.append(Arrays.toString((int[]) field(b, "indexedZooms")));
			for (Object t = field(b, "indx"); t != null; t = field(t, "subTree")) {
				sb.append('/').append(field(t, "indxZoom")).append(':').append(list(field(t, "skipIndexes"))).append(':')
						.append(list(field(t, "skipInSubIndexes"))).append(':').append(list(field(t, "tileIds")));
			}
			sb.append(';');
		}
		return sb.toString();
	}

	private static Object field(Object o, String name) {
		try {
			java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return f.get(o);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(o.getClass() + "." + name, e);
		}
	}

	/** A trove list or a list of the copy, as java prints an array. */
	private static String list(Object l) {
		try {
			Object a = l.getClass().getMethod("toArray").invoke(l);
			return a instanceof int[] ? Arrays.toString((int[]) a) : Arrays.toString((long[]) a);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(String.valueOf(l), e);
		}
	}

	/** The items of a description, sorted: for the answers whose order within a tile is the hash order of a map. */
	static String sorted(String described) {
		List<String> items = new ArrayList<>(Arrays.asList(described.split(";")));
		items.remove("");
		Collections.sort(items);
		return String.join(";", items);
	}

	/** Java's answer goes to the dump first: the copy elsewhere is held to java, whatever the copy here answers. */
	private void same(String name, String java, String copy) throws IOException {
		expect(name, java);
		assertEquals(name, java, copy);
		compared++;
	}

	/** The values come from the boxes, which the dump holds, so that the copy can be asked the same anywhere. */
	@Test
	public void tileIdsAreTheSame() throws IOException {
		StringBuilder java = new StringBuilder();
		StringBuilder copy = new StringBuilder();
		for (int[] b : boxes) {
			long x = ((long) b[0] << 32) ^ b[2];
			long y = ((long) b[1] << 32) ^ b[3];
			int z = Math.abs((b[0] ^ b[1]) % 22);
			int x31 = b[0];
			int y31 = b[1];
			// and the complements, whose top bits are set
			java.append(MapUtils.interleaveBits(x, y)).append(':').append(MapUtils.interleaveBits(~x, ~y)).append(':')
					.append(MapUtils.deinterleaveX(x)).append(':').append(MapUtils.deinterleaveY(x)).append(':')
					.append(MapUtils.deinterleaveX(~x)).append(':').append(MapUtils.deinterleaveY(~x)).append(':');
			copy.append(KMapUtils.INSTANCE.interleaveBits(x, y)).append(':').append(KMapUtils.INSTANCE.interleaveBits(~x, ~y))
					.append(':').append(KMapUtils.INSTANCE.deinterleaveX(x)).append(':').append(KMapUtils.INSTANCE.deinterleaveY(x))
					.append(':').append(KMapUtils.INSTANCE.deinterleaveX(~x)).append(':').append(KMapUtils.INSTANCE.deinterleaveY(~x))
					.append(':');
			java.append(outcome(() -> HashQuadTree.encodeTileId31(z, x31, y31))).append(':')
					.append(outcome(() -> HashQuadTree.encodeTileId(z, x31 >> 15, y31 >> 15))).append(':')
					.append(HashSkipTileQuadTree.encodeTileId(x31 >> 10, y31 >> 10)).append(';');
			copy.append(outcome(() -> net.osmand.shared.search.core.HashQuadTree.Companion.encodeTileId31(z, x31, y31))).append(':')
					.append(outcome(() -> net.osmand.shared.search.core.HashQuadTree.Companion.encodeTileId(z, x31 >> 15, y31 >> 15)))
					.append(':')
					.append(net.osmand.shared.search.core.HashSkipTileQuadTree.Companion.encodeTileId(x31 >> 10, y31 >> 10)).append(';');
		}
		same("tile ids", java.toString(), copy.toString());
		java.setLength(0);
		copy.setLength(0);
		for (int i = 0; i + 1 < queries.size(); i++) {
			java.append(HashSkipTileQuadTree.intersectsBBox(queries.get(i), queries.get(i + 1)));
			copy.append(net.osmand.shared.search.core.HashSkipTileQuadTree.Companion.intersectsBBox(queries.get(i), queries.get(i + 1)));
		}
		same("intersectsBBox", java.toString(), copy.toString());
	}

	private interface Call {
		Object get();
	}

	private static String outcome(Call call) {
		try {
			return String.valueOf(call.get());
		} catch (UnsupportedOperationException e) {
			return "unsupported";
		}
	}

	/** The defaults, and the tree the spatial search builds with them. */
	@Test
	public void defaultTreesAreTheSame() throws IOException {
		assertEquals(HashSkipTileQuadTree.DEFALT_MIN_ZOOM, net.osmand.shared.search.core.HashSkipTileQuadTree.DEFALT_MIN_ZOOM);
		assertEquals(HashSkipTileQuadTree.DEFALT_MAX_ZOOM, net.osmand.shared.search.core.HashSkipTileQuadTree.DEFALT_MAX_ZOOM);
		assertEquals(Arrays.toString(HashSkipTileQuadTree.DEFAULT_INDEXED_ZOOMS),
				Arrays.toString(net.osmand.shared.search.core.HashSkipTileQuadTree.Companion.getDEFAULT_INDEXED_ZOOMS()));
		HashSkipTileQuadTree<Integer> j = new HashSkipTileQuadTree<>();
		net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k = new net.osmand.shared.search.core.HashSkipTileQuadTree<>();
		for (int i = 0; i < boxes.size(); i++) {
			j.addObject(i, boxes.get(i));
			k.addObject(i, boxes.get(i));
		}
		assertEquals("size", j.build(), k.build());
		same("tree default entries", javaEntries(j.getTileEntries(), true), copyEntries(k.getTileEntries(), true));
		same("tree default buckets", buckets(j), buckets(k));
	}

	@Test
	public void treesAreTheSame() throws IOException {
		for (int s = 0; s < SETTINGS.length; s++) {
			HashSkipTileQuadTree<Integer> j = javaTree(s, boxes);
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k = copyTree(s, boxes);
			StringBuilder jz = new StringBuilder();
			StringBuilder kz = new StringBuilder();
			for (int[] b : boxes) {
				jz.append(j.getNativeZoom(b)).append(',');
				kz.append(k.getNativeZoom(b)).append(',');
			}
			same("tree " + s + " native zooms", jz.toString(), kz.toString());
			assertEquals("built " + s, j.isBuilt(), k.isBuilt());
			assertEquals("size " + s, j.build(), k.build());
			assertEquals("built " + s, j.isBuilt(), k.isBuilt());
			same("tree " + s + " entries", javaEntries(j.getTileEntries(), true), copyEntries(k.getTileEntries(), true));
			same("tree " + s + " buckets", buckets(j), buckets(k));
			sameQueries("tree " + s, j, k, false);

			addMore(j, k, false);
			assertEquals("size after more " + s, j.build(), k.build());
			assertEquals("empty " + s, j.isEmpty(), k.isEmpty());
			assertEquals("getSize " + s, j.getSize(), k.getSize());
			same("tree " + s + " entries after more", sorted(javaEntries(j.getTileEntries(), false)),
					sorted(copyEntries(k.getTileEntries(), false)));
			same("tree " + s + " buckets after more", buckets(j), buckets(k));
			sameQueries("tree " + s + " after more", j, k, true);
		}
	}

	private void sameQueries(String m, HashSkipTileQuadTree<Integer> j,
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k, boolean anyOrder) throws IOException {
		// each answer compared on its own, the dump holds a hash of each: a box of the world finds every entry
		StringBuilder jh = new StringBuilder();
		String firstDifferent = null;
		for (int i = 0; i < queries.size(); i++) {
			int[] q = queries.get(i);
			String jr = javaEntries(i % 5 == 4 ? j.get(q, 3, 12, null) : j.get(q, null), false);
			String kr = copyEntries(i % 5 == 4 ? k.get(q, 3, 12) : k.get(q), false);
			String ja = (anyOrder ? sorted(jr) : jr) + "|" + j.contains(q);
			String ka = (anyOrder ? sorted(kr) : kr) + "|" + k.contains(q);
			if (firstDifferent == null && !ja.equals(ka)) {
				firstDifferent = m + " query " + i + "\njava " + ja + "\ncopy " + ka;
			}
			jh.append(NameIndexCompatTest.fnv(ja)).append(';');
		}
		expect(m + " queries", jh.toString());
		assertNull(firstDifferent, firstDifferent);
		compared++;
	}

	@Test
	public void joinsAreTheSame() throws IOException {
		int[][] pairs = {{0, 0}, {0, 1}, {1, 2}, {2, 0}};
		for (int[] p : pairs) {
			List<int[]> first = joinBoxes();
			List<int[]> second = new ArrayList<>();
			for (int i = 0; i < first.size(); i += 3) {
				second.add(first.get(i));
			}
			HashSkipTileQuadTree<Integer> j1 = javaTree(p[0], first);
			HashSkipTileQuadTree<Integer> j2 = javaTree(p[1], second);
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k1 = copyTree(p[0], first);
			net.osmand.shared.search.core.HashSkipTileQuadTree<Integer> k2 = copyTree(p[1], second);
			for (int pass = 0; pass < 2; pass++) {
				if (pass == 1) {
					addMore(j1, k1, true);
					addMore(j2, k2, true);
				}
				j1.build();
				j2.build();
				k1.build();
				k2.build();
				StringBuilder jp = new StringBuilder();
				StringBuilder kp = new StringBuilder();
				new HashSkipTileQuadTreeJoiner<>(j1, j2).joinAllBuckets((e1, e2) -> jp.append(e1.objId).append(':')
						.append(e1.z).append(':').append(e1.tileId).append('-').append(e2.objId).append(':').append(e2.z)
						.append(':').append(e2.tileId).append(';'));
				new net.osmand.shared.search.core.HashSkipTileQuadTreeJoiner<>(k1, k2).joinAllBuckets((e1, e2) -> kp
						.append(e1.getObjId()).append(':').append(e1.getZ()).append(':').append(e1.getTileId()).append('-')
						.append(e2.getObjId()).append(':').append(e2.getZ()).append(':').append(e2.getTileId()).append(';'));
				String m = "join " + p[0] + " x " + p[1] + (pass == 1 ? " after more" : "");
				same(m, pass == 1 ? sorted(jp.toString()) : jp.toString(), pass == 1 ? sorted(kp.toString()) : kp.toString());
				if (pass == 0) {
					intersectingPairsOnce(m, j1.getTileEntries(), j2.getTileEntries(), jp.toString());
				}
			}
		}
	}

	/** The boxes of the regions and the random boxes smaller than a region, so that the pairs stay few enough to list. */
	static List<int[]> joinBoxes() {
		List<int[]> res = new ArrayList<>();
		for (int[] b : boxes) {
			if ((long) b[2] - b[0] < (1 << 26) && (long) b[3] - b[1] < (1 << 26)) {
				res.add(b);
			}
		}
		return res;
	}

	private static final Map<String, Set<String>> intersecting = new java.util.HashMap<>();

	/** Every pair of objects whose boxes intersect, and no other, once. */
	private static void intersectingPairsOnce(String m, List<TileEntry<Integer>> t1, List<TileEntry<Integer>> t2,
			String joined) {
		Map<Long, int[]> o1 = new TreeMap<>();
		Map<Long, int[]> o2 = new TreeMap<>();
		for (TileEntry<Integer> e : t1) {
			o1.put(e.objId, e.bbox31);
		}
		for (TileEntry<Integer> e : t2) {
			o2.put(e.objId, e.bbox31);
		}
		String key = o1.keySet() + "|" + o2.keySet();
		Set<String> expected = intersecting.get(key);
		if (expected == null) {
			// the same boxes in every pair of settings
			expected = new HashSet<>();
			for (Map.Entry<Long, int[]> a : o1.entrySet()) {
				for (Map.Entry<Long, int[]> b : o2.entrySet()) {
					if (HashSkipTileQuadTree.intersectsBBox(a.getValue(), b.getValue())) {
						expected.add(a.getKey() + "-" + b.getKey());
					}
				}
			}
			intersecting.put(key, expected);
		}
		List<String> pairs = new ArrayList<>();
		for (String pair : joined.split(";")) {
			if (!pair.isEmpty()) {
				String[] ab = pair.split("-");
				pairs.add(ab[0].split(":")[0] + "-" + ab[1].split(":")[0]);
			}
		}
		assertEquals(m + " pairs once", pairs.size(), new HashSet<>(pairs).size());
		assertEquals(m + " pairs", expected, new HashSet<>(pairs));
		assertTrue(m + " pairs found: " + pairs.size(), pairs.size() > 1000);
	}

	/** Puts, deletes and lookups made from the boxes, five rounds over them, into few tiles so that lookups find something. */
	@Test
	public void hashQuadTreesAreTheSame() throws IOException {
		HashQuadTree<Integer> j = new HashQuadTree<>(16);
		net.osmand.shared.search.core.HashQuadTree<Integer> k = new net.osmand.shared.search.core.HashQuadTree<>(16);
		StringBuilder jr = new StringBuilder();
		StringBuilder kr = new StringBuilder();
		for (int i = 0; i < 5 * boxes.size(); i++) {
			int[] b = boxes.get(i % boxes.size());
			int z = (b[0] % 17 + i % 17) % 17;
			int mask = (1 << Math.min(z, 3)) - 1;
			long tileId = HashQuadTree.encodeTileId(z, (b[1] >> 3) & mask, (b[2] >> 5) & mask);
			int value = (b[3] >> 7) % 50;
			int op = (i % 4 * 7 + b[0] % 4) % 4;
			if (op == 0) {
				j.put(z, tileId, value);
				k.put(z, tileId, value);
			} else if (op == 1) {
				j.delete(z, tileId, value);
				k.delete(z, tileId, value);
			} else if (op == 2) {
				j.forEachMatch(z, tileId, l -> jr.append(l).append(';'));
				k.forEachMatch(z, tileId, l -> {
					kr.append(l).append(';');
					return kotlin.Unit.INSTANCE;
				});
			} else {
				j.forEachMatchHigherZoom(z, tileId, l -> jr.append(l).append(';'));
				k.forEachMatchHigherZoom(z, tileId, l -> {
					kr.append(l).append(';');
					return kotlin.Unit.INSTANCE;
				});
			}
			jr.append('|');
			kr.append('|');
		}
		same("hash quad tree", jr.toString(), kr.toString());
	}
}
