package net.osmand.search.core.spatial;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import org.junit.Assert;

/**
 * Checks the engine against recorded human preferences instead of recorded output.
 *
 * <p>The golden corpus in {@code test-resources/spatial_search/*.json} stores a whole ordered
 * result list, comparator key and distance included, so every ranking change rewrites all of it
 * and a diff says nothing about whether the change was good. {@code preferences.jsonl} stores
 * the other thing - what a person said about two specific objects:
 *
 * <pre>
 *   order: for query Q from position L, object B should come before object A
 *   merge: these N objects are one place and belong in one row
 * </pre>
 *
 * Both sentences survive any rewrite of the ranker, so adding a preference adds a constraint
 * instead of invalidating the existing ones, and the suite reports one number that is comparable
 * across versions rather than a pile of golden-file diffs.
 *
 * <p>Objects are addressed by OSM id. A record whose objects are no longer returned at all is
 * NOT APPLICABLE, never a failure: that is a recall problem and mixing it into a ranking number
 * would hide both.
 *
 * <p>Maps are not in the repository. It looks in {@code ~/osmand/maps} by default; point it
 * elsewhere with {@code OSMAND_MAPS_DIR} (or {@code -Dosmand.maps.dir} from an IDE). Without
 * maps the test skips rather than fails.
 */
public class SpatialSearchPreferencesTest {

	private static final String PREFERENCES = "/spatial_search/preferences.jsonl";

	/**
	 * Ratchet, not a target. It is the number of preferences the engine satisfied when this
	 * test was written; raise it when a change earns more. The point of a ratchet is that an
	 * unrelated reordering cannot break the build - only contradicting a recorded human
	 * judgement can.
	 */
	private static final int MIN_SATISFIED = 40;

	/**
	 * {@code OSMAND_SPATIAL_SCORE_RANKING=false} runs the same preferences against the old
	 * lexicographic ladder. That comparison is the reason this file exists: the number is
	 * comparable across ranker versions, which a golden result list can never be.
	 *
	 * <p>An environment variable rather than a system property because gradle passes the
	 * environment through to the test JVM and does not pass {@code -D} (the property is still
	 * read, so it works when the test is launched from an IDE).
	 */
	private static final boolean SCORE_RANKING = !"false".equalsIgnoreCase(
			setting("osmand.spatial.scoreRanking", "OSMAND_SPATIAL_SCORE_RANKING"));

	private static String setting(String property, String env) {
		String v = System.getProperty(property);
		return v != null ? v : System.getenv(env);
	}

	static class Pref {
		String id, kind, query, map, prefer, verdict, note;
		LatLon location;
		List<Long> objects = new ArrayList<>();
		long a, b;
		boolean asserted = true;
	}

	static class Score {
		int satisfied, violated, notApplicable, notAsserted, noMap;
		List<String> failures = new ArrayList<>();
	}

	@Test
	public void testRecordedPreferences() throws IOException {
		List<Pref> prefs = readPreferences();
		Assert.assertFalse("preferences.jsonl is empty", prefs.isEmpty());

		File mapsDir = mapsDir();
		Assume.assumeTrue("no maps directory - set OSMAND_MAPS_DIR=<dir with .obf files>",
				mapsDir != null);
		Score sc = check(prefs, mapsDir);

		System.out.printf("ranking: %s%n", SCORE_RANKING ? "score" : "ladder (old)");
		System.out.printf("preferences: %d satisfied, %d violated, %d not applicable "
						+ "(object not returned), %d not asserted, %d without a map%n",
				sc.satisfied, sc.violated, sc.notApplicable, sc.notAsserted, sc.noMap);
		for (String f : sc.failures) {
			System.out.println("  violated " + f);
		}
		Assume.assumeTrue("none of the maps named by preferences.jsonl are present",
				sc.satisfied + sc.violated + sc.notApplicable > 0);
		Assume.assumeTrue("old ranking is measured for comparison, not gated", SCORE_RANKING);
		Assert.assertTrue(String.format(
				"satisfied preferences dropped to %d, the recorded floor is %d - a human "
						+ "judgement was contradicted, see the list above",
				sc.satisfied, MIN_SATISFIED), sc.satisfied >= MIN_SATISFIED);
	}

	Score check(List<Pref> prefs, File mapsDir) throws IOException {
		Score sc = new Score();
		// one engine run per distinct (query, map, position), not per preference
		Map<String, List<Pref>> byRun = new LinkedHashMap<>();
		for (Pref p : prefs) {
			byRun.computeIfAbsent(p.query + " " + p.map + " " + p.location,
					k -> new ArrayList<>()).add(p);
		}

		for (List<Pref> group : byRun.values()) {
			Pref head = group.get(0);
			File obf = new File(mapsDir, head.map);
			if (!obf.isFile()) {
				sc.noMap += group.size();
				continue;
			}
			List<MapObject> ordered = search(head.query, obf, mapsDir, head.location);
			for (Pref p : group) {
				if (!p.asserted) {
					sc.notAsserted++;
					continue;
				}
				if ("merge".equals(p.kind)) {
					int present = 0;
					for (long id : p.objects) {
						if (indexOf(ordered, id) >= 0) {
							present++;
						}
					}
					if (present == 0) {
						sc.notApplicable++;
					} else if (present == 1) {
						sc.satisfied++;
					} else {
						sc.violated++;
						sc.failures.add(String.format("%s '%s': %d rows for one place",
								p.id, p.query, present));
					}
					continue;
				}
				int ia = indexOf(ordered, p.a);
				int ib = indexOf(ordered, p.b);
				if (ia < 0 || ib < 0) {
					sc.notApplicable++;
					continue;
				}
				boolean aFirst = ia < ib;
				if (aFirst == "a".equals(p.prefer)) {
					sc.satisfied++;
				} else {
					sc.violated++;
					sc.failures.add(String.format("%s '%s': wanted %s first, it is #%d of %d",
							p.id, p.query, p.prefer, Math.max(ia, ib) + 1, ordered.size()));
				}
			}
		}

		return sc;
	}

	private int indexOf(List<MapObject> ordered, long osmId) {
		for (int i = 0; i < ordered.size(); i++) {
			if (ObfConstants.getOsmObjectId(ordered.get(i)) == osmId) {
				return i;
			}
		}
		return -1;
	}

	/** the first object of every result, in the order the engine returned them */
	private List<MapObject> search(String query, File obf, File mapsDir, LatLon location)
			throws IOException {
		List<BinaryMapIndexReader> files = new ArrayList<>();
		File regions = new File(mapsDir, OsmandRegions.REGIONS_OCBF);
		if (regions.isFile()) {
			files.add(new BinaryMapIndexReader(new RandomAccessFile(regions, "r"), regions));
		}
		files.add(new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf));
		try {
			SpatialTextSearchSettings settings = SpatialTextSearchSettings.defaultSettings();
			settings.SCORE_RANKING = SCORE_RANKING;
			SpatialSearchContext ctx = new SpatialSearchContext(settings, files,
					new SpatialPoiSearch(MapPoiTypes.getDefault()), location);
			SpatialSearchResults res = new SpatialTextSearch().searchAPI(query, ctx);
			List<MapObject> out = new ArrayList<>();
			if (res.mainResults != null) {
				for (SpatialSearchResult r : res.mainResults) {
					out.addAll(r.getObjects());
				}
			}
			return out;
		} finally {
			for (BinaryMapIndexReader f : files) {
				f.close();
			}
		}
	}

	File mapsDir() {
		String dir = setting("osmand.maps.dir", "OSMAND_MAPS_DIR");
		if (dir == null || dir.isEmpty()) {
			dir = System.getProperty("user.home") + "/osmand/maps";
		}
		File f = new File(dir);
		return f.isDirectory() ? f : null;
	}

	List<Pref> readPreferences() throws IOException {
		List<Pref> out = new ArrayList<>();
		try (InputStream is = getClass().getResourceAsStream(PREFERENCES)) {
			Assert.assertNotNull(PREFERENCES + " is not on the test classpath", is);
			Scanner sc = new Scanner(new InputStreamReader(is, StandardCharsets.UTF_8));
			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				JSONObject o = new JSONObject(line);
				Pref p = new Pref();
				p.id = o.getString("id");
				p.kind = o.getString("kind");
				p.query = o.getString("query");
				p.map = o.getString("map");
				p.note = o.optString("note", "");
				if (!o.isNull("lat") && !o.isNull("lon")) {
					p.location = new LatLon(o.getDouble("lat"), o.getDouble("lon"));
				}
				// a record contradicted by another one is kept but asserted by neither side:
				// it is the measured noise of a human judge, not a statement to test against
				p.asserted = o.optJSONArray("conflictsWith") == null
						|| o.getJSONArray("conflictsWith").length() == 0;
				if ("merge".equals(p.kind)) {
					p.verdict = o.getString("verdict");
					p.asserted &= "one_row".equals(p.verdict);
					JSONArray arr = o.getJSONArray("objects");
					for (int i = 0; i < arr.length(); i++) {
						p.objects.add(arr.getJSONObject(i).getLong("osmId"));
					}
				} else {
					p.prefer = o.getString("prefer");
					p.asserted &= !"either".equals(p.prefer);
					p.a = o.getJSONObject("a").getLong("osmId");
					p.b = o.getJSONObject("b").getLong("osmId");
				}
				out.add(p);
			}
		}
		return out;
	}
}
