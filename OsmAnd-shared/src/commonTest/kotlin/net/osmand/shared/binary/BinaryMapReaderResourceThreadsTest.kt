package net.osmand.shared.binary

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.osmand.shared.io.KFile
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.SearchPhraseTest
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.use
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [BinaryMapReaderResource] asked for readers by several threads at once, closed while they ask, and
 * its file changed under it: what the search, the routing and the poi threads of an app do to the
 * same file. Its readers share one structure of the file, which they fill in as they read it; this
 * runs on Kotlin/Native too, where the locks are other locks.
 */
class BinaryMapReaderResourceThreadsTest {

	private val fs = FileSystem.SYSTEM
	private lateinit var dir: Path

	@BeforeTest
	fun createDir() {
		dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "obf-reader-threads-${Random.nextLong().toULong()}"
		fs.createDirectories(dir)
	}

	@AfterTest
	fun deleteDir() {
		fs.deleteRecursively(dir)
	}

	private fun fixture(): String = RoutingTestFixtures.resource("Turn_lanes_test.obf")

	private fun copy(name: String): String {
		val path = dir / name
		fs.copy(fixture().toPath(), path)
		return path.toString()
	}

	private fun cities(reader: BinaryMapIndexReader): Int = reader.getCities(null, CityBlocks.CITY_TOWN_TYPE).size

	/** What a reader reads that fills the structure in: poi categories, map rules and levels, routing rules and boxes. */
	private fun readAll(reader: BinaryMapIndexReader): List<Int> {
		reader.searchPoiCategoriesByName("a", HashMap())
		val categories = reader.getPoiIndexes().sumOf { it.categories.size }
		val mapObjects = reader.searchMapIndex(
			SearchRequest.buildSearchRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, 15, null)
		).size
		val request = SearchRequest.buildSearchRouteRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
		var roads = 0
		for (region in reader.getRoutingIndexes()) {
			for (box in reader.searchRouteIndexTree(request, region.getSubregions())) {
				roads += reader.loadRouteIndexData(box).count { it != null }
			}
		}
		return listOf(categories, mapObjects, roads)
	}

	/** Runs [block] on [threads] threads released together and returns what each returned. */
	private fun <T> runAtOnce(threads: Int, block: (Int) -> T): List<T> = runBlocking {
		val start = CompletableDeferred<Unit>()
		val tasks = (0 until threads).map { thread ->
			async(Dispatchers.Default) {
				start.await()
				block(thread)
			}
		}
		start.complete(Unit)
		tasks.awaitAll()
	}

	@Test
	fun threadsAskingForTheSameTypeGetTheSameReader() {
		// fewer types than threads, so that several threads ask for each
		val types = listOf(BinaryMapReaderResourceType.QUICK_SEARCH, BinaryMapReaderResourceType.POI, BinaryMapReaderResourceType.ROUTING)
		repeat(ROUNDS) {
			val resource = BinaryMapReaderResource(fixture())
			val got = runAtOnce(THREADS) { thread ->
				val type = types[thread % types.size]
				Triple(type, resource.getReader(type), resource.getShallowReader())
			}
			val shallow = got[0].third ?: fail("no reader walked the file")
			assertTrue(got.all { it.third === shallow }, "one reader walks the file")
			val readers = HashSet<BinaryMapIndexReader>()
			for (type in types) {
				val ofType = got.filter { it.first == type }.map { it.second }
				val first = ofType[0] ?: fail("no ${type.name} reader")
				assertTrue(ofType.all { it === first }, "every thread asking for ${type.name} gets one reader")
				assertTrue(first !== shallow && readers.add(first), "${type.name} shares a reader")
			}
			resource.close()
		}
	}

	/** The readers of the eight types read the same file for the first time at once, as the threads of an app do. */
	@Test
	fun readersOfOneFileFillTheStructureOnce() {
		SearchPhraseTest.setUp
		val expected = BinaryMapIndexReader(fixture()).let { reader -> readAll(reader).also { reader.close() } }
		assertTrue(expected.all { it > 0 }, "$expected")
		val types = BinaryMapReaderResourceType.entries
		repeat(ROUNDS) { round ->
			val resource = BinaryMapReaderResource(fixture())
			val readers = types.map { resource.getReader(it)!! }
			for (read in runAtOnce(types.size) { thread -> readAll(readers[thread]) }) {
				assertEquals(expected, read, "categories, map objects and roads in round $round")
			}
			resource.close()
		}
	}

	/** Routing adds rules to a region while it calculates, as it does for the direction points it places. */
	@Test
	fun rulesAddedWhileRoutingAreReadWhole() {
		repeat(ROUNDS) { round ->
			val resource = BinaryMapReaderResource(fixture())
			val region = resource.getReader(BinaryMapReaderResourceType.ROUTING)!!.getRoutingIndexes()[0]
			val before = region.quickGetEncodingRulesSize()
			val writing = AtomicInt(WRITERS)
			val ids = runAtOnce(WRITERS + 1) { thread ->
				if (thread == WRITERS) {
					// decodes roads meanwhile, without the lock: the last rule of the table is there
					while (writing.get() > 0) {
						val size = region.quickGetEncodingRulesSize()
						if (size > before && region.quickGetEncodingRule(size - 1) == null) {
							fail("rule ${size - 1} of $size is not in the table")
						}
					}
					emptyList()
				} else {
					val created = (0 until RULES).map { i -> region.findOrCreateRouteType("osmand_test", "t${thread}_$i") }
					writing.decrementAndGet()
					created
				}
			}.flatten()
			assertEquals(before + WRITERS * RULES, region.quickGetEncodingRulesSize(), "round $round")
			assertEquals(ids.size, ids.toSet().size, "every rule got an id of its own")
			for (thread in 0 until WRITERS) {
				for (i in 0 until RULES) {
					val id = region.searchRouteEncodingRule("osmand_test", "t${thread}_$i")
					assertEquals("t${thread}_$i", region.quickGetEncodingRule(id)?.getValue())
				}
			}
			resource.close()
		}
	}

	@Test
	fun aReaderOpenedWhileTheFileIsClosedIsClosedToo() {
		repeat(ROUNDS) {
			val resource = BinaryMapReaderResource(fixture())
			val got = runAtOnce(THREADS) { thread ->
				if (thread == 0) {
					resource.close()
					null
				} else {
					resource.getReader(BinaryMapReaderResourceType.entries[thread % 3])
				}
			}
			assertTrue(resource.isClosed())
			assertNull(resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
			for (reader in got.filterNotNull()) {
				assertFails("a reader handed out before or during close is closed") { cities(reader) }
			}
		}
	}

	@Test
	fun aReplacedFileIsNotReadWithTheOldStructure() {
		val path = copy("replaced.obf")
		val resource = BinaryMapReaderResource(path)
		val search = resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
		assertNotNull(search)
		// an update writes another file at the same path
		fs.appendingSink(path.toPath()).use { it.write(okio.Buffer().writeByte(0), 1) }
		assertNull(resource.getReader(BinaryMapReaderResourceType.POI))
		assertNull(resource.getReader(BinaryMapReaderResourceType.ROUTING), "not opened again")
		assertSame(search, resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
		val handedOverAgain = BinaryMapReaderResource(path)
		assertNotNull(handedOverAgain.getReader(BinaryMapReaderResourceType.POI))
		handedOverAgain.close()
		resource.close()
	}

	@Test
	fun aReaderThatFailsToOpenIsAskedForAgain() {
		val path = copy("missing-for-a-moment.obf")
		val resource = BinaryMapReaderResource(path)
		assertNotNull(resource.getShallowReader())
		// the file cannot be opened for a moment, as when the app has too many files open
		val moved = (dir / "moved.obf")
		fs.atomicMove(path.toPath(), moved)
		assertNull(resource.getReader(BinaryMapReaderResourceType.POI))
		fs.atomicMove(moved, path.toPath())
		assertNotNull(resource.getReader(BinaryMapReaderResourceType.POI), "asked for again")
		resource.close()
	}

	@Test
	fun anInitialReaderFinishingAfterARecordedFailureIsClosed() = lateReaderAfterFailure(initial = true)

	@Test
	fun aCopyFinishingAfterARecordedFailureIsClosed() = lateReaderAfterFailure(initial = false)

	private fun lateReaderAfterFailure(initial: Boolean) = runBlocking {
		val opening = CompletableDeferred<BinaryMapIndexReader>()
		val release = CompletableDeferred<Unit>()
		val gate = AtomicInt(0)
		val attempts = AtomicInt(0)
		val resource = BinaryMapReaderResource(KFile(fixture())) { reference ->
			if (gate.get() == 0) {
				BinaryMapIndexReader(fixture())
			} else if (attempts.incrementAndGet() == 1) {
				val reader = if (reference == null) BinaryMapIndexReader(fixture()) else BinaryMapIndexReader(reference)
				opening.complete(reader)
				runBlocking { withTimeout(10000) { release.await() } }
				reader
			} else {
				if (initial) throw IOException("initial read failed")
				throw ObfFileChangedException("file changed while a copy was opening")
			}
		}
		try {
			if (!initial) assertNotNull(resource.getShallowReader())
			gate.set(1)
			val pending = async(Dispatchers.Default) {
				if (initial) resource.getShallowReader() else resource.getReader(BinaryMapReaderResourceType.POI)
			}
			val reader = withTimeout(10000) { opening.await() }
			assertNull(if (initial) resource.getShallowReader() else resource.getReader(BinaryMapReaderResourceType.ROUTING))
			release.complete(Unit)
			assertNull(withTimeout(10000) { pending.await() }, "a late open cannot undo the recorded failure")
			assertFails("the discarded reader is closed") { cities(reader) }
			assertNull(resource.getReader(BinaryMapReaderResourceType.ADDRESS))
		} finally {
			release.complete(Unit)
			resource.close()
		}
	}

	/** A read of the categories that breaks off half way leaves them unread for every reader of the file. */
	@Test
	fun aReadThatBreaksOffLeavesNoHalfOfTheStructure() {
		SearchPhraseTest.setUp
		val path = copy("cut.obf")
		val resource = BinaryMapReaderResource(path)
		val shallow = resource.getShallowReader()!!
		val poi = resource.getReader(BinaryMapReaderResourceType.POI)!!
		val region = shallow.getPoiIndexes()[0]
		val bytes = fs.read(path.toPath()) { readByteArray() }
		// cut inside the category table, which is at the start of the poi section
		fs.write(path.toPath()) { write(bytes, 0, (region.filePointer + region.length / 8).toInt()) }
		assertFails { poi.searchPoiCategoriesByName("a", HashMap()) }
		assertTrue(region.categories.isEmpty(), "${region.categories.size} categories left behind")
		assertTrue(region.subTypes.isEmpty())
		resource.close()
	}

	companion object {
		private const val ROUNDS = 30
		private const val THREADS = 8
		private const val WRITERS = 7
		private const val RULES = 200
	}
}
