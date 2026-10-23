package net.osmand.shared.binary

import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testAppCall
import net.osmand.shared.search.AmenitySearcherTest
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.dumpFnv
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [NameIndexReader] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `NameIndexCompatTest.javaDump` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/name-index-java.txt`, or wherever `OSMAND_NAME_INDEX_JAVA_DUMP` points: for
 * each name index of the files it read, the bytes java counted after each read, every block by its
 * key, its offset and a hash of all it holds, then a run of queries on the raw blocks with the
 * blocks each returned and the cache collected on the way, the answers of the matcher of the keys
 * and of the marker words. It asks the copy the same, in the same order over the same files, and
 * holds it to java exactly.
 */
class NameIndexReaderTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"NameIndexReaderTest", "OSMAND_NAME_INDEX_JAVA_DUMP", "name-index-java.txt", "NameIndexCompatTest"
		) ?: return
		val compared = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var reader: BinaryMapIndexReader? = null
		var index = 0
		var whole: NameIndexReader? = null
		var queries: NameIndexReader? = null
		var all: List<NameIndexReader.PrefixNameValue>? = null
		var blocks: List<NameIndexReader.PrefixNameValue> = emptyList()
		var block = 0
		var keys: List<String> = emptyList()
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				val kind = f[0][0]
				val copy: String = testAppCall {
					when (kind) {
						'X' -> {
							val w = dumpUnhex(f[1])
							"X\t${f[1]}\t${NameIndexReader.altNameVariant(w)}\t${NameIndexReader.isIndexMarker(w)}"
						}
						'F' -> {
							reader?.close()
							reader = BinaryMapIndexReader(AmenitySearcherTest.path(dumpUnhex(f[1])))
							line
						}
						'I' -> {
							index = f[1].toInt()
							whole = nameIndexReader(reader!!, index).setCacheRawBlocks(f[2] == "1")
							queries = null
							line
						}
						'A' -> {
							val j = whole!!
							"A\t" + outcome { reader!!.readFullNameIndex(j) } + "\t" + bytes(j)
						}
						'C' -> {
							val j = whole!!
							val res = "C\t" + dumpFnv(commonWords(j))
							all = reader!!.readFullNameIndex(j.setQuery("all", everyKey()))
							blocks = sorted(all)
							block = 0
							res
						}
						'B' -> {
							val p = blocks.getOrNull(block++)
							if (p == null) "B no block" else "B\t" + dumpHex(p.key!!) + "\t" + p.shift + "\t" + dumpFnv(describe(p))
						}
						'L' -> {
							val j = whole!!
							val res = "L\t" + shifts(all) + "\t" + shifts(j.getMatchedPrefixes("all")) + "\t" + bytes(j)
							if (block != blocks.size) "$res and ${blocks.size - block} blocks more" else res
						}
						'Q' -> {
							val q = queries ?: nameIndexReader(reader!!, index).setCacheRawBlocks(true).also { queries = it }
							val query = dumpUnhex(f[1])
							if (f[2] == "1") {
								q.resetBytesStat()
							}
							val res = reader!!.readFullNameIndex(q.setQuery(query))
							"Q\t" + f[1] + "\t" + f[2] + "\t" + shifts(res) + "\t" + shifts(q.getMatchedPrefixes(query)) + "\t" + bytes(q)
						}
						'G' -> {
							val q = queries!!
							when (f[1]) {
								"clear" -> q.clearPrefixes()
								"gc" -> q.gcPrefixes(20)
							}
							"G\t" + f[1] + "\t" + bytes(q)
						}
						'R' -> {
							val q = queries!!
							q.clearQuery()
							val res = "R\t" + outcome { reader!!.readFullNameIndex(q) }
							reader!!.resetReadLimits()
							q.clearPrefixes()
							res
						}
						'K' -> {
							keys = f.drop(1).map { dumpUnhex(it) }
							val own = sample(blocks.map { it.key!! }, MATCHER_KEYS)
							if (own == keys) line else "K the copy samples other keys: $own"
						}
						'M' -> {
							val matcher = NameIndexReader.NameIndexReaderMatcher(dumpUnhex(f[1]))
							val bits = StringBuilder()
							for (key in keys) {
								bits.append(if (matcher.matchKey(key)) '1' else '0')
							}
							"M\t" + f[1] + "\t" + bits
						}
						'W' -> {
							val words = names(blocks, whole!!)
							val answers = StringBuilder()
							for (w in words) {
								answers.append(NameIndexReader.altNameVariant(w)).append(if (NameIndexReader.isIndexMarker(w)) "m" else "").append(';')
							}
							"W\t" + words.size + "\t" + dumpFnv(answers.toString())
						}
						else -> line
					}
				}
				compared[kind] = (compared[kind] ?: 0) + 1
				if (copy != line && different.size < 20) {
					different.add("java $line\ncopy $copy")
				}
			}
		} finally {
			source.close()
			reader?.close()
		}
		if (different.isNotEmpty()) {
			fail("different from java:\n" + different.joinToString("\n"))
		}
		assertTrue((compared['B'] ?: 0) > 0, "blocks compared: $compared")
		println("NameIndexReaderTest: the same as java, lines by kind $compared")
	}

	/**
	 * The blocks of a query come in the order of the file, read now, read before or both, and from the cache of the
	 * query: the atoms of a word of the spatial search follow them. Java gives the blocks read before in the order
	 * of a hash set from the cache, and first from a read.
	 */
	@Test
	fun blocksComeInTheOrderOfTheFile() {
		val reader = BinaryMapIndexReader(RoutingTestFixtures.resource("Turn_lanes_test.obf"))
		try {
			val index = NameIndexReader(reader.getAddressIndexes()[0]).setCacheRawBlocks(true)
			val some = reader.readFullNameIndex(index.setQuery("some", keys { it.length % 2 == 0 }))!!
			val all = reader.readFullNameIndex(index.setQuery("all", keys { true }))!!
			assertTrue(some.isNotEmpty() && all.size > some.size, "blocks: ${some.size} of ${all.size}")
			assertInFileOrder("read now", some)
			assertInFileOrder("read before and now", all)
			val again = reader.readFullNameIndex(index.setQuery("all", keys { true }))!!
			assertEquals(all.map { it.shift }, again.map { it.shift }, "read before")
			assertEquals(all.map { it.shift }, index.getMatchedPrefixes("all")!!.map { it.shift }, "from the cache")
		} finally {
			reader.close()
		}
	}

	companion object {

		private fun keys(match: (String) -> Boolean): NameIndexReader.NameIndexReaderMatcher =
			object : NameIndexReader.NameIndexReaderMatcher("") {
				override fun matchKey(key: String): Boolean = match(key)
			}

		private fun assertInFileOrder(what: String, blocks: List<NameIndexReader.PrefixNameValue>) {
			for (i in 1 until blocks.size) {
				assertTrue(blocks[i - 1].shift < blocks[i].shift, "$what: block $i at ${blocks[i].shift} after ${blocks[i - 1].shift}")
			}
		}

		/** `NameIndexCompatTest.MATCHER_KEYS`. */
		private const val MATCHER_KEYS = 300

		/** Index [i] of the file: the address sections first, then the poi sections. */
		private fun nameIndexReader(reader: BinaryMapIndexReader, i: Int): NameIndexReader {
			val addresses = reader.getAddressIndexes()
			return if (i < addresses.size) NameIndexReader(addresses[i]) else NameIndexReader(reader.getPoiIndexes()[i - addresses.size])
		}

		private fun everyKey(): NameIndexReader.NameIndexReaderMatcher =
			object : NameIndexReader.NameIndexReaderMatcher("") {
				override fun matchKey(key: String): Boolean = true
			}

		private fun outcome(read: () -> List<*>?): String =
			try {
				val value = read()
				if (value == null) "null" else "a list of " + value.size
			} catch (e: Throwable) {
				"threw"
			}

		private fun bytes(j: NameIndexReader): String {
			val b = j.getBytesStat()
			return "${b.readTableBytes}\t${b.skipTableBytes}\t${b.readAtomBytes}\t${b.skipAtomBytes}\t" +
					"${j.getCachedBytes()}\t${j.readAll()}"
		}

		private fun sorted(list: List<NameIndexReader.PrefixNameValue>?): List<NameIndexReader.PrefixNameValue> =
			list?.sortedBy { it.shift } ?: emptyList()

		/** `NameIndexCompatTest.shifts`. */
		private fun shifts(list: List<NameIndexReader.PrefixNameValue>?): String {
			if (list == null) {
				return "null"
			}
			val sb = StringBuilder()
			for (p in sorted(list)) {
				sb.append(p.shift).append(',')
			}
			return list.size.toString() + ":" + dumpFnv(sb.toString())
		}

		/** `NameIndexCompatTest.sample`. */
		private fun sample(values: List<String>, count: Int): List<String> {
			if (values.size <= count) {
				return values
			}
			return (0 until count).map { values[(it.toLong() * values.size / count).toInt()] }
		}

		/** `NameIndexCompatTest.names`, sorted and without the empty word. */
		private fun names(blocks: List<NameIndexReader.PrefixNameValue>, j: NameIndexReader): Set<String> {
			val names = HashSet<String>()
			for (p in blocks) {
				val key = p.key!!
				names.add(key)
				val a = p.getAddr()
				val d = p.getPoi()
				var suffix: String? = null
				for (s in a?.getSuffixesDictionaryList() ?: d!!.getSuffixesDictionaryList()) {
					suffix = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(suffix, s)
					names.add(key + suffix)
					names.addAll((key + suffix).split(" "))
				}
				val extras = a?.getAtomList()?.flatMap { it.getExtraSuffixList() }
					?: d!!.getAtomsList().flatMap { it.getExtraSuffixList() }
				for (s in extras) {
					names.addAll(s.split(" "))
				}
			}
			j.getCommonWordsStats()?.let { names.addAll(it.keys) }
			names.remove("")
			return names.sorted().toCollection(LinkedHashSet())
		}

		private fun strings(sb: StringBuilder, list: List<String>) {
			for (v in list) {
				sb.append(dumpHex(v)).append(',')
			}
		}

		private fun bytesHex(b: ByteArray): String {
			val sb = StringBuilder()
			for (v in b) {
				sb.append(HEX[(v.toInt() shr 4) and 15]).append(HEX[v.toInt() and 15])
			}
			return sb.toString()
		}

		private const val HEX = "0123456789abcdef"

		/** `NameIndexCompatTest.commonWords`. */
		private fun commonWords(j: NameIndexReader): String {
			val cs = j.getCommonStats() ?: return "null"
			val sb = StringBuilder()
			strings(sb, cs.getValueList())
			sb.append('|').append(cs.getMatchedList()).append('|').append(cs.getNonindexedList()).append('|')
			for (n in 0 until cs.getValueCount()) {
				sb.append(dumpHex(j.getCommonIndexed(n))).append(',')
			}
			val words = j.getCommonWordsStats()!!
			for (key in words.keys.sorted()) {
				val v = words[key]!!
				sb.append('|').append(dumpHex(key)).append(',').append(v.freq).append(',').append(v.extra)
			}
			return sb.toString()
		}

		/** `NameIndexCompatTest.describe`. */
		private fun describe(p: NameIndexReader.PrefixNameValue): String {
			val sb = StringBuilder()
			sb.append(if (p.poi != null) 'p' else '-').append(if (p.addr != null) 'a' else '-').append(if (p.data != null) 'r' else '-')
			p.data?.let { sb.append(bytesHex(it)) }
			val a = p.getAddr()
			if (a != null) {
				sb.append("|A").append(a.getSerializedSize()).append('|').append(a.hasAtomsLength()).append('|')
					.append(a.getAtomsLength()).append('|')
				strings(sb, a.getSuffixesDictionaryList())
				sb.append('|').append(a.getSuffixesCommonDictionaryList())
				for (t in a.getAtomList()) {
					sb.append("|a").append(t.getType()).append(';').append(t.getSuffixesBitsetIndexList()).append(';')
					strings(sb, t.getExtraSuffixList())
					sb.append(';').append(t.getOtherWordsCountList()).append(';').append(t.hasBbox()).append(';')
						.append(bytesHex(t.getBbox())).append(';').append(t.hasEnclosingObjects()).append(';')
						.append(t.getEnclosingObjects()).append(';').append(t.getShiftToIndexList()).append(';')
						.append(t.getShiftToCityIndexList()).append(';').append(t.getXy16List())
					if (t.getXy16Count() > 0) {
						val xy16 = t.getXy16(0)
						val bbox = KSearchAlgorithms.decodeBboxForNameAtomsBytes(
							if (t.hasBbox()) t.getBbox() else null, xy16 ushr 16, xy16 and ((1 shl 16) - 1)
						)
						sb.append(';').append(bbox?.contentToString() ?: "null")
					}
				}
			}
			val d = p.getPoi()
			if (d != null) {
				sb.append("|P").append(d.getSerializedSize()).append('|').append(d.hasAtomsLength()).append('|')
					.append(d.getAtomsLength()).append('|')
				strings(sb, d.getSuffixesDictionaryList())
				sb.append('|').append(d.getSuffixesCommonDictionaryList())
				for (t in d.getAtomsList()) {
					sb.append("|p").append(t.hasZoom()).append(';').append(t.getZoom()).append(';').append(t.hasX()).append(';')
						.append(t.getX()).append(';').append(t.hasY()).append(';').append(t.getY()).append(';')
						.append(t.getSuffixesBitsetIndexList()).append(';')
					strings(sb, t.getExtraSuffixList())
					val bbox = KSearchAlgorithms.decodeBboxForNameAtomsBytes(if (t.hasBbox()) t.getBbox() else null, t.getX(), t.getY())
					sb.append(';').append(t.getOtherWordsCountList()).append(';').append(t.getPoiIndInBlockList()).append(';')
						.append(t.hasBbox()).append(';').append(bytesHex(t.getBbox())).append(';')
						.append(t.getPoiCategoriesList()).append(';').append(t.getEloRatingList()).append(';')
						.append(t.hasShiftTo()).append(';').append(t.getShiftTo()).append(';').append(t.getShiftToNextZ8Tile())
						.append(';').append(t.getShiftToNextZ10Tile()).append(';').append(t.getShiftToNextZ12Tile()).append(';')
						.append(bbox?.contentToString() ?: "null")
				}
			}
			return sb.toString()
		}
	}
}
