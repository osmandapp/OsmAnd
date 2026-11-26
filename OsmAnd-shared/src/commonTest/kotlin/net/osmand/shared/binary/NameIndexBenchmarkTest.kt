package net.osmand.shared.binary

import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes over the name indexes of real maps, keeping the blocks raw as the
 * spatial search does: every block of every name index of a map, 20 words of the map on a fresh
 * reader, the same words again on the reader that cached their blocks, and the parse of every block.
 * The java half, which puts the java reader next to this copy on the jvm, is `NameIndexBenchmarkTest`
 * in OsmAnd-java, over the same files, phases and words.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_NAME_INDEX_BENCHMARK` is set, and
 * the maps are named in `OSMAND_OBF_CORPUS`, separated by `:`. On Kotlin/Native only the **release**
 * binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_NAME_INDEX_BENCHMARK=1 SIMCTL_CHILD_OSMAND_OBF_CORPUS=/maps/Andorra_europe.obf \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.binary.NameIndexBenchmarkTest.*'
 * ```
 * The java half lists what four regional maps took in java, in the copy on the jvm and here.
 */
class NameIndexBenchmarkTest {

	@Test
	fun benchmarkNameIndexes() {
		if (testEnvironment("OSMAND_NAME_INDEX_BENCHMARK") == null) {
			println("NameIndexBenchmarkTest: set OSMAND_NAME_INDEX_BENCHMARK to run")
			return
		}
		val corpus = testEnvironment("OSMAND_OBF_CORPUS")
		if (corpus.isNullOrEmpty()) {
			println("NameIndexBenchmarkTest: name the maps in OSMAND_OBF_CORPUS")
			return
		}
		println("### name index on ${testPlatformName()}, best of $MEASURED_ROUNDS in ms")
		println("map                                          whole      cold      warm     parse")
		for (path in corpus.split(":")) {
			benchmark(path)
		}
	}

	private fun benchmark(path: String) {
		val reader = BinaryMapIndexReader(path)
		val queries = queries(reader)
		val indexes = reader.getAddressIndexes().size + reader.getPoiIndexes().size
		val best = LongArray(4) { Long.MAX_VALUE }
		var found = 0L
		var atoms = 0L
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val t = LongArray(4)
			found = 0
			atoms = 0
			for (i in 0 until indexes) {
				var mark = TimeSource.Monotonic.markNow()
				val k = nameIndexReader(reader, i)
				reader.readFullNameIndex(k)
				t[0] += mark.elapsedNow().inWholeNanoseconds
				val kq = nameIndexReader(reader, i)
				for (pass in 0 until 2) {
					mark = TimeSource.Monotonic.markNow()
					for (q in queries) {
						found += reader.readFullNameIndex(kq.setQuery(q))?.size ?: 0
					}
					t[1 + pass] += mark.elapsedNow().inWholeNanoseconds
				}
				val all = reader.readFullNameIndex(k.setQuery("all", everyKey()))
				mark = TimeSource.Monotonic.markNow()
				if (all != null) {
					for (p in all) {
						val a = p.getAddr()
						atoms += a?.getAtomCount() ?: p.getPoi()!!.getAtomsCount()
					}
				}
				t[3] += mark.elapsedNow().inWholeNanoseconds
			}
			if (round >= WARMUP_ROUNDS) {
				for (n in t.indices) {
					best[n] = minOf(best[n], t[n])
				}
			}
		}
		val line = StringBuilder(path.substringAfterLast('/').padEnd(40))
		for (b in best) {
			val ms = (b / 10_000).toDouble() / 100
			line.append(ms.toString().padStart(10))
		}
		println("$line   blocks found $found, atoms $atoms, words $queries")
		reader.close()
	}

	companion object {
		private const val WARMUP_ROUNDS = 3
		private const val MEASURED_ROUNDS = 5
		/** `NameIndexBenchmarkTest.QUERIES`. */
		private const val QUERIES = 20

		private fun nameIndexReader(reader: BinaryMapIndexReader, i: Int): NameIndexReader {
			val addresses = reader.getAddressIndexes()
			val r = if (i < addresses.size) NameIndexReader(addresses[i]) else NameIndexReader(reader.getPoiIndexes()[i - addresses.size])
			return r.setCacheRawBlocks(true)
		}

		private fun everyKey(): NameIndexReader.NameIndexReaderMatcher =
			object : NameIndexReader.NameIndexReaderMatcher("") {
				override fun matchKey(key: String): Boolean = true
			}

		/** `NameIndexBenchmarkTest.queries`. */
		private fun queries(reader: BinaryMapIndexReader): List<String> {
			val words = LinkedHashSet<String>()
			val indexes = reader.getAddressIndexes().size + reader.getPoiIndexes().size
			for (i in 0 until indexes) {
				val all = reader.readFullNameIndex(nameIndexReader(reader, i).setQuery("all", everyKey())) ?: continue
				for (p in all.sortedBy { it.shift }) {
					val key = p.key!!
					val dictionary = p.getAddr()?.getSuffixesDictionaryList() ?: p.getPoi()!!.getSuffixesDictionaryList()
					if (dictionary.isEmpty() && key.length >= 3 && key.indexOf(' ') == -1) {
						// a map from before the suffix dictionaries: the keys are the words
						words.add(key)
					}
					var suffix: String? = null
					for (s in dictionary) {
						suffix = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(suffix, s)
						val name = key + suffix
						if (name.length >= 3 && name.indexOf(' ') == -1) {
							words.add(name)
						}
					}
				}
			}
			val list = words.toList()
			val count = minOf(QUERIES, list.size)
			return (0 until count).map { list[(it.toLong() * list.size / count).toInt()] }
		}
	}
}
