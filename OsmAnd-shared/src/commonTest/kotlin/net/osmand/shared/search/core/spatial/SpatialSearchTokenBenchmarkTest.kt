package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.binary.NameIndexReader.PrefixNameValue
import net.osmand.shared.data.Building
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.AmenitySearcherTest
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtomXY
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.search.rules.SearchModRules
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy of the words of a query of the spatial search takes wherever it runs, over what
 * `SpatialSearchTokenCompatTest` in OsmAnd-java wrote to `../OsmAnd-java/build/search-tokens-java.txt`: the words
 * made, each word held to the keys of its name index by the prefix matcher and to its names by `matchName` for a
 * street and a poi, the names split by `matchSplitName`, the place of every atom of the indexes read, and the atoms
 * added to words. `SpatialSearchTokenBenchmarkTest` in OsmAnd-java times java over the same lines.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_SEARCH_TOKENS_BENCHMARK` is set. On Kotlin/Native
 * only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_SEARCH_TOKENS_BENCHMARK=1 SIMCTL_CHILD_OSMAND_SEARCH_TOKENS_JAVA_DUMP=<dump> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.search.core.spatial.SpatialSearchTokenBenchmarkTest.*'
 * ```
 */
class SpatialSearchTokenBenchmarkTest {

	private class Index(val mapLocale: String, val address: Boolean, val blocks: Map<Long, PrefixNameValue>) {
		var keys: List<String> = emptyList()
		var names: List<String> = emptyList()
		val words = ArrayList<List<String>>()
		val atoms = ArrayList<ArrayList<List<String>>>()
	}

	@Test
	fun tokens() {
		if (testEnvironment("OSMAND_SEARCH_TOKENS_BENCHMARK") == null) {
			println("SpatialSearchTokenBenchmarkTest: set OSMAND_SEARCH_TOKENS_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"SpatialSearchTokenBenchmarkTest", "OSMAND_SEARCH_TOKENS_JAVA_DUMP", "search-tokens-java.txt",
			"SpatialSearchTokenCompatTest"
		) ?: return
		lateinit var rules: SearchModRules
		var locales: List<String> = emptyList()
		val indexes = ArrayList<Index>()
		val readers = ArrayList<BinaryMapIndexReader>()
		var reader: BinaryMapIndexReader? = null
		var mapLocale = ""
		var index: Index? = null
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"D" -> rules = SearchModRules.forSearch(dumpUnhex(f[1]))
					"L" -> locales = dumpUnhexList(f[1])
					"F" -> {
						val r = BinaryMapIndexReader(AmenitySearcherTest.path(dumpUnhex(f[1])))
						readers.add(r)
						reader = r
						mapLocale = dumpUnhex(f[2])
					}
					"I" -> {
						val r = reader!!
						val i = f[1].toInt()
						val address = i < r.getAddressIndexes().size
						val nameReader = if (address) NameIndexReader(r.getAddressIndexes()[i])
						else NameIndexReader(r.getPoiIndexes()[i - r.getAddressIndexes().size])
						val all = r.readFullNameIndex(nameReader.setQuery("all", EveryKey())) ?: emptyList()
						index = Index(mapLocale, address, all.associateBy { it.shift }).also { indexes.add(it) }
					}
					"K" -> index!!.keys = dumpUnhexList(f[1])
					"N" -> index!!.names = dumpUnhexList(f[1])
					"T" -> index!!.words.add(listOf(dumpUnhex(f[1]), dumpUnhex(f[2]), f[3], f[4]))
					"W" -> index!!.atoms.add(ArrayList())
					"A" -> index!!.atoms.last().add(f)
				}
			}
		} finally {
			source.close()
		}
		val settings = SpatialTextSearchSettings.defaultSettings()
		val best = LongArray(6) { Long.MAX_VALUE }
		val found = LongArray(6)
		var words = 0
		var keys = 0
		var names = 0
		var places = 0
		var atoms = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val t = LongArray(6)
			found.fill(0)
			words = 0; keys = 0; names = 0; places = 0; atoms = 0
			val tokens = ArrayList<List<SpatialSearchToken>>()
			var mark = TimeSource.Monotonic.markNow()
			for (x in indexes) {
				val list = ArrayList<SpatialSearchToken>()
				for (w in x.words) {
					val token = SpatialSearchToken(rules, w[2].toInt(), w[0], w[1], w[3].toInt())
					found[0] += if (token.likelyPartOfBuilding(x.mapLocale)) 1 else 0
					list.add(token)
				}
				words += list.size
				tokens.add(list)
			}
			t[0] = mark.elapsedNow().inWholeNanoseconds

			val stats = SpatialSearchStats()
			mark = TimeSource.Monotonic.markNow()
			for (n in indexes.indices) {
				val x = indexes[n]
				for (token in tokens[n]) {
					val m = token.getPrefixMatcher(stats, x.mapLocale)
					for (key in x.keys) {
						found[1] += if (m.matchKey(key)) 1 else 0
					}
					keys += x.keys.size
				}
			}
			t[1] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (n in indexes.indices) {
				val x = indexes[n]
				for (token in tokens[n]) {
					for (name in x.names) {
						found[2] += if (token.matchName(name, null, x.mapLocale, STREET)) 1 else 0
						found[2] += if (token.matchName(name, null, x.mapLocale, POI)) 1 else 0
					}
					names += 2 * x.names.size
				}
			}
			t[2] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (n in indexes.indices) {
				val x = indexes[n]
				for (token in tokens[n]) {
					for (name in x.names) {
						found[3] += try {
							if (token.matchSplitName(name) == null) 0 else 1
						} catch (e: IndexOutOfBoundsException) {
							2
						}
					}
				}
			}
			t[3] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (x in indexes) {
				for (p in x.blocks.values) {
					if (x.address) {
						val data = p.getAddr()!!
						for (ai in 0 until data.getAtomCount()) {
							found[4] += NameIndexAtomXY(data.getAtom(ai), null, settings).bboxTileZoom
							places++
						}
					} else {
						val data = p.getPoi()!!
						for (ai in 0 until data.getAtomsCount()) {
							found[4] += NameIndexAtomXY(null, data.getAtoms(ai), settings).bboxTileZoom
							places++
						}
					}
				}
			}
			t[4] = mark.elapsedNow().inWholeNanoseconds

			val built = ArrayList<List<NameIndexAtom>>()
			for (x in indexes) {
				for (specs in x.atoms) {
					built.add(specs.map { atom(x, it, settings, locales) })
				}
			}
			mark = TimeSource.Monotonic.markNow()
			for (list in built) {
				val token = SpatialSearchToken(rules, 2, "main", "main", 0)
				for (a in list) {
					found[5] += if (token.addAtom(a)) 1 else 0
				}
				atoms += list.size
			}
			t[5] = mark.elapsedNow().inWholeNanoseconds
			if (round >= WARMUP_ROUNDS) {
				for (n in t.indices) {
					best[n] = minOf(best[n], t[n])
				}
			}
		}
		for (r in readers) {
			r.close()
		}
		println("### spatial search words on ${testPlatformName()}, best of $MEASURED_ROUNDS in ms")
		val titles = listOf("words made $words", "keys matched $keys", "names matched $names", "names split ${names / 2}",
			"places read $places", "atoms added $atoms")
		for (n in titles.indices) {
			println("${titles[n].padEnd(28)} ${(best[n] / 1e4).toLong() / 100.0}   found ${found[n]}")
		}
	}

	// the atom of a line A of the dump, as SpatialSearchTokenCompatTest made it
	private fun atom(x: Index, f: List<String>, settings: SpatialTextSearchSettings, locales: List<String>): NameIndexAtom {
		val place = f[15].split(":")
		val p = x.blocks[place[0].toLong()]!!
		val ai = place[1].toInt()
		val coords = if (x.address) NameIndexAtomXY(p.getAddr()!!.getAtom(ai), null, settings)
		else NameIndexAtomXY(null, p.getPoi()!!.getAtoms(ai), settings)
		val obj: MapObject? = when (f[5]) {
			"0" -> Building()
			"1" -> Street(null)
			else -> null
		}
		obj?.setId(f[6].toLong())
		val locale = f[14].toInt()
		val atom = NameIndexAtom(dumpUnhex(f[1]), f[2].toInt(), f[3].toLong(), f[4].toLong(), obj, f[7] == "1",
			f[8].toInt(), f[9].toInt(), coords, f[10].toInt(), f[11].toInt())
		atom.locale = if (locale < locales.size) locales[locale] else x.mapLocale
		atom.ruleSpelling = f[12] == "1"
		atom.distinctFoundCnt = f[13].toInt()
		return atom
	}

	private companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5
		const val STREET = 4
		const val POI = -1

	}
}
