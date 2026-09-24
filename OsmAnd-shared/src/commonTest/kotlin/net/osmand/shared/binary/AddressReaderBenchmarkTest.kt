package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.data.City
import net.osmand.shared.data.MapObject
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes over the address section of real maps: opening the file, every
 * settlement, every street, every house, and a search by name. The java half, which puts the java
 * reader next to this copy on the jvm, is `AddressReaderBenchmarkTest` in OsmAnd-java, over the same
 * files and phases.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_ADDRESS_BENCHMARK` is set, and
 * the maps are named in `OSMAND_OBF_CORPUS`, separated by `:`. On the jvm, from `android`:
 * ```
 * OSMAND_ADDRESS_BENCHMARK=1 OSMAND_OBF_CORPUS=/maps/Slovakia_europe.obf \
 *   ./gradlew :OsmAnd-shared:jvmTest --tests "*AddressReaderBenchmarkTest" --rerun -i
 * ```
 * On Kotlin/Native only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_ADDRESS_BENCHMARK=1 SIMCTL_CHILD_OSMAND_OBF_CORPUS=/maps/Slovakia_europe.obf \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.binary.AddressReaderBenchmarkTest.*'
 * ```
 *
 * On three regional maps, best of five in ms, next to the java half:
 * ```
 * what                         Noord-Holland (395 MB)     Kyiv (204 MB)          Slovakia (754 MB)
 *                              java  copy  native   java  copy  native   java  copy  native
 * open the file                12.6   7.1     6.0    8.5   4.3     2.7   38.4  20.4    13.5
 * every settlement             28.6  21.9    24.5    3.0   2.2     2.8   11.3   6.6     7.3
 * every street                 67.5  51.6    71.2   17.6  14.8    24.7   58.7  47.1    60.8
 * every house                   494   409     687   54.7  49.8    96.2    394   314     591
 * find by name, 20 queries     23.2   7.6    13.5   25.1   8.7    15.0   23.0   9.4    19.1
 * ```
 * settlements, streets, houses, found: Noord-Holland 75 587, 122 172, 4 268 244, 11 624; Kyiv 2 910,
 * 28 186, 177 312, 969; Slovakia 9 055, 85 483, 3 582 428, 1 487.
 *
 * The copy on the jvm is faster than java in every phase, 0.32 to 0.91 of its time; the name search
 * gains most, from the collation key of `KCollatorStringMatcher` and from the walk over the
 * string table of the name index preparing each key once. Opening costs the copy the same as before
 * the address header was read: 7.02, 4.29 and 20.11 ms on master. Kotlin/Native reads settlements
 * at about the jvm's speed, streets and houses at 1.3 to 1.9 times the copy on the jvm, and searches
 * by name at 1.7 to 2.0 times, still below java on the jvm. Before the string table walk prepared
 * its keys the native name search took 17.8, 35.6 and 52.4 ms.
 */
class AddressReaderBenchmarkTest {

	@Test
	fun benchmarkAddressReader() {
		if (testEnvironment("OSMAND_ADDRESS_BENCHMARK") == null) {
			println("AddressReaderBenchmarkTest: set OSMAND_ADDRESS_BENCHMARK to run")
			return
		}
		val corpus = testEnvironment("OSMAND_OBF_CORPUS")
		if (corpus.isNullOrEmpty()) {
			println("AddressReaderBenchmarkTest: name the maps in OSMAND_OBF_CORPUS")
			return
		}
		for (path in corpus.split(":")) {
			benchmark(path)
		}
	}

	private fun benchmark(path: String) {
		val queries = queries(path)
		val best = DoubleArray(5) { Double.MAX_VALUE }
		var cities = 0
		var streets = 0
		var houses = 0
		var found = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			var mark = TimeSource.Monotonic.markNow()
			repeat(OPEN_PASSES_PER_ROUND) {
				BinaryMapIndexReader(path).close()
			}
			val open = mark.elapsedNow().inWholeMicroseconds / 1000.0 / OPEN_PASSES_PER_ROUND

			val reader = BinaryMapIndexReader(path)
			mark = TimeSource.Monotonic.markNow()
			val all = ArrayList<City>()
			for (type in CityBlocks.allTypes()) {
				all.addAll(reader.getCities(null, type))
			}
			val citiesMs = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var readStreets = 0
			for (c in all) {
				reader.preloadStreets(c, null)
				readStreets += c.getStreets().size
			}
			val streetsMs = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var readHouses = 0
			for (c in all) {
				for (s in c.getStreets()) {
					reader.preloadBuildings(s, null)
					readHouses += s.getBuildings().size
				}
			}
			val housesMs = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var readFound = 0
			for (query in queries) {
				val req = SearchRequest.buildAddressByNameRequest<MapObject>(
					null, query, KStringMatcherMode.CHECK_STARTS_FROM_SPACE
				)
				readFound += reader.searchAddressDataByName(req).size
			}
			val searchMs = mark.elapsedNow().inWholeMicroseconds / 1000.0
			reader.close()

			if (round >= WARMUP_ROUNDS) {
				val times = doubleArrayOf(open, citiesMs, streetsMs, housesMs, searchMs)
				for (i in times.indices) {
					if (times[i] < best[i]) {
						best[i] = times[i]
					}
				}
				cities = all.size
				streets = readStreets
				houses = readHouses
				found = readFound
			}
		}
		println("")
		println("### ${path.substringAfterLast('/')}, shared copy on ${testPlatformName()}, best of $MEASURED_ROUNDS")
		println("  ${"what".padEnd(34)} ${"ms".padStart(9)} ${"count".padStart(9)}")
		row("open the file", best[0], 1)
		row("read every settlement", best[1], cities)
		row("read every street", best[2], streets)
		row("read every house", best[3], houses)
		row("find by name (${queries.size} queries)", best[4], found)
	}

	/** Names the file holds, lowercased as the search hands them over, and their first three letters. */
	private fun queries(path: String): List<String> {
		val reader = BinaryMapIndexReader(path)
		val names = ArrayList<String>()
		for (type in CityBlocks.allTypes()) {
			for (c in reader.getCities(null, type)) {
				names.add(c.getName())
			}
		}
		reader.close()
		val queries = LinkedHashSet<String>()
		val step = maxOf(1, names.size / (QUERIES / 2))
		var i = 0
		while (i < names.size && queries.size < QUERIES) {
			val name = names[i].trim().lowercase()
			if (name.length > 3) {
				queries.add(name)
				queries.add(name.substring(0, 3))
			}
			i += step
		}
		return queries.toList()
	}

	private fun row(what: String, ms: Double, count: Int) {
		val rounded = (kotlin.math.round(ms * 10) / 10).toString()
		println("  ${what.padEnd(34)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5
		const val QUERIES = 20
		const val OPEN_PASSES_PER_ROUND = 10
	}
}
