package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Timing comparison of {@link net.osmand.shared.osm.MapPoiTypes} against {@link MapPoiTypes}.
 *
 * <b>Disabled on purpose.</b> The copy is a straight port, so unlike the collator there is no
 * design decision here to defend; this is startup cost, paid once before anything can be read out
 * of a poi section. The cross platform half, which gives the Kotlin/Native number, is in
 * OsmAnd-shared/src/commonTest/kotlin/net/osmand/shared/osm/MapPoiTypesBenchmarkTest.kt over the
 * same file. Neither is a regression gate, and this one asserts only that both registries came out
 * the same size. What the copy has to answer is covered by {@link MapPoiTypesCompatTest}, which
 * does run.
 *
 * To measure, remove the {@code @Ignore} below, run it, and put the annotation back:
 * <pre>
 * ./gradlew :OsmAnd-java:test --tests "*MapPoiTypesBenchmarkTest" -i
 * </pre>
 *
 * Reading the 470 KB poi_types.xml of the tests:
 * <pre>
 * read      java   4.7 ms   copy   4.8 ms   13256 types
 * </pre>
 * The two are the same on the jvm. On Kotlin/Native the same file costs 13.0 ms, see the cross
 * platform benchmark; that is paid once at startup and is the price of parsing the file through
 * libxml2 rather than of anything the copy does differently.
 */
@Ignore("benchmark, run manually")
public class MapPoiTypesBenchmarkTest {

	private static final String POI_TYPES = "src/test/resources/poi_types.xml";
	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	@Test
	public void readingPoiTypes() {
		long java = Long.MAX_VALUE;
		long copy = Long.MAX_VALUE;
		int javaTypes = 0;
		int copyTypes = 0;
		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			long started = System.nanoTime();
			MapPoiTypes javaTypesRegistry = new MapPoiTypes(POI_TYPES);
			javaTypesRegistry.init();
			long javaRound = System.nanoTime() - started;

			started = System.nanoTime();
			net.osmand.shared.osm.MapPoiTypes copyRegistry = new net.osmand.shared.osm.MapPoiTypes(POI_TYPES);
			copyRegistry.init();
			long copyRound = System.nanoTime() - started;

			if (round >= WARMUP_ROUNDS) {
				java = Math.min(java, javaRound);
				copy = Math.min(copy, copyRound);
				javaTypes = count(javaTypesRegistry);
				copyTypes = countCopy(copyRegistry);
			}
		}
		assertEquals("types", javaTypes, copyTypes);
		System.out.printf("read      java %5.1f ms   copy %5.1f ms   %d types%n",
				java / 1e6, copy / 1e6, javaTypes);
	}

	private static int count(MapPoiTypes registry) {
		int types = 0;
		for (PoiCategory category : registry.getCategories()) {
			types += category.getPoiTypes().size();
			for (PoiType type : category.getPoiTypes()) {
				types += type.getPoiAdditionals().size();
			}
			types += category.getPoiAdditionals().size();
		}
		return types;
	}

	private static int countCopy(net.osmand.shared.osm.MapPoiTypes registry) {
		int types = 0;
		for (net.osmand.shared.osm.PoiCategory category : registry.getCategories()) {
			types += category.getPoiTypes().size();
			for (net.osmand.shared.osm.PoiType type : category.getPoiTypes()) {
				types += type.getPoiAdditionals().size();
			}
			types += category.getPoiAdditionals().size();
		}
		return types;
	}
}
