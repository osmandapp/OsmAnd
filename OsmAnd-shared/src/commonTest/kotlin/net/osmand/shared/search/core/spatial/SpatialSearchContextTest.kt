package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.AddressRegion
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.CodedInputStream
import net.osmand.shared.binary.CommonIndexedStats
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTLongObjectMap
import okio.Buffer
import okio.FileSystem
import okio.use
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * `SpatialSearchContextTest` of OsmAnd-java. Its test of a settlement read with its street replaces the reads of a
 * `BinaryMapIndexReader`, which the copy does not let a test replace: here it reads a street of a real map.
 */
class SpatialSearchContextTest {

	@Test
	fun testStreetParentCityReferenceFileWithoutCachedCity() {
		val reader = BinaryMapIndexReader(RoutingTestFixtures.resource("Turn_lanes_test.obf"))
		val rulesDir = SpatialSearchQueryRulesTest.rulesDir()
		// the poi index of the file reads its categories by the default types
		val defaultTypes = runCatching { MapPoiTypes.getDefault() }.getOrNull()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		try {
			val region = reader.getAddressIndexes()[0]
			val context = SpatialSearchContext(
				SpatialTextSearchSettings.defaultSettings(), listOf(reader), SpatialPoiSearch(types), KLatLon(0.0, 0.0)
			)
			context.initFiles(SpatialSearchGlobalCache(rulesDir.toString()))
			val file = context.internalFile[0]
			val indexInd = file.indexInd + file.indexReaders.indexOfFirst { it.addressRegion === region }
			// the ids of the first street of the name index and of its settlement, as the search makes them: the
			// offsets back from the block, with the number of the name index in their lower 14 bits
			val blocks = reader.readFullNameIndex(NameIndexReader(region).setQuery("all", object : NameIndexReader.NameIndexReaderMatcher("") {
				override fun matchKey(key: String): Boolean = true
			}))!!
			var streetId = 0L
			var cityId = 0L
			for (block in blocks) {
				val atom = block.getAddr()!!.getAtomList().firstOrNull { it.getType() == CityBlocks.STREET_TYPE.index } ?: continue
				streetId = ((block.shift - atom.getShiftToIndex(0)) shl 14) + indexInd
				cityId = ((block.shift - atom.getShiftToCityIndex(0)) shl 14) + indexInd
				break
			}
			assertTrue(streetId != 0L, "a street in the name index")

			val read = context.readAddrObject(streetId, cityId, KTLongObjectMap<MapObject>())
			assertTrue(read is Street, "a street: $read")
			assertSame(reader, read.getCity()!!.getReferenceFile(), "a parent city read with its street must retain the source reader")
		} finally {
			defaultTypes?.let { MapPoiTypes.setDefault(it) }
			reader.close()
			FileSystem.SYSTEM.deleteRecursively(rulesDir)
		}
	}

	/**
	 * Common words of the address index as the 2026-09 maps store them: word, frequency, non-indexed.
	 * A kind word ranks a house whose street the query never named last, so a street name taken for
	 * one sends "707 John Street Elmira" below "601 Johnson Street" and "9 Lange Straße Stuttgart"
	 * below "9 Stuttgarter Straße".
	 */
	@Test
	fun testStreetNameWordsAreNotKindWords() {
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		val context = SpatialSearchContext(
			SpatialTextSearchSettings.defaultSettings(), emptyList(), SpatialPoiSearch(types), KLatLon(0.0, 0.0)
		)
		val pennsylvania = addressIndex(
			"street", 75284, 72691,
			"avenue", 34308, 33753,
			"john", 432, 157,
			"main", 2113, 122
		)
		val stuttgart = addressIndex(
			"strasse", 15140, 15126,
			"weg", 5465, 5458,
			"lange", 377, 125,
			"hohe", 327, 86
		)

		assertTrue(context.isKindWord(pennsylvania, "street"), "street")
		assertTrue(context.isKindWord(pennsylvania, "avenue"), "avenue")
		assertTrue(context.isKindWord(stuttgart, "straße"), "straße")
		assertTrue(context.isKindWord(stuttgart, "weg"), "weg")

		assertFalse(context.isKindWord(pennsylvania, "john"), "john")
		assertFalse(context.isKindWord(pennsylvania, "main"), "main")
		assertFalse(context.isKindWord(stuttgart, "lange"), "lange")
		assertFalse(context.isKindWord(stuttgart, "hohe"), "hohe")
	}

	// the words unencoded: the dictionary decodes a word that starts with no marker as it is
	private fun addressIndex(vararg wordFreqNonindexed: Any): NameIndexReader {
		val message = Buffer()
		for (i in wordFreqNonindexed.indices step 3) {
			val word = KSearchAlgorithms.alignChars(wordFreqNonindexed[i] as String).encodeToByteArray()
			message.writeByte(4 shl 3 or 2)
			writeVarint(message, word.size.toLong())
			message.write(word)
			message.writeByte(5 shl 3)
			writeVarint(message, (wordFreqNonindexed[i + 1] as Int).toLong())
			message.writeByte(6 shl 3)
			writeVarint(message, (wordFreqNonindexed[i + 2] as Int).toLong())
		}
		val size = message.size
		val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "common-indexed-${Random.nextLong().toULong()}.bin"
		FileSystem.SYSTEM.write(file) { writeAll(message) }
		try {
			val handle = FileSystem.SYSTEM.openReadOnly(file)
			handle.use {
				val codedIS = CodedInputStream(it)
				codedIS.pushLimitLong(size)
				val indx = NameIndexReader(AddressRegion())
				indx.setCommonIndexed(CommonIndexedStats.parseFrom(codedIS))
				return indx
			}
		} finally {
			FileSystem.SYSTEM.delete(file)
		}
	}

	private fun writeVarint(sink: Buffer, value: Long) {
		var v = value
		while (v >= 0x80) {
			sink.writeByte((v and 0x7f or 0x80).toInt())
			v = v ushr 7
		}
		sink.writeByte(v.toInt())
	}
}
