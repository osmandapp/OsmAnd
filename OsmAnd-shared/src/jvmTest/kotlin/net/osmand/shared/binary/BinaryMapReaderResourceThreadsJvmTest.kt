package net.osmand.shared.binary

import net.osmand.shared.routing.RoutingTestFixtures
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [BinaryMapReaderResource] asked for readers by several threads at once and closed while they ask:
 * what the search, the routing and the travel threads of the app do to the same file. The resource
 * walks a file outside its lock, so these are the cases where two walks of the same file meet.
 */
class BinaryMapReaderResourceThreadsJvmTest {

	private fun fixture(): String = RoutingTestFixtures.resource("Turn_lanes_test.obf")

	private fun read(reader: BinaryMapIndexReader): Int = reader.getCities(null, CityBlocks.CITY_TOWN_TYPE).size

	@Test
	fun threadsAskingForTheSameTypeGetTheSameReader() {
		// fewer types than threads, so that several threads ask for each
		val types = listOf(BinaryMapReaderResourceType.QUICK_SEARCH, BinaryMapReaderResourceType.POI, BinaryMapReaderResourceType.ROUTING)
		repeat(ROUNDS) {
			val resource = BinaryMapReaderResource(fixture())
			val got = Array(BinaryMapReaderResourceType.entries.size) { Collections.synchronizedList(ArrayList<BinaryMapIndexReader?>()) }
			val shallow = Collections.synchronizedList(ArrayList<BinaryMapIndexReader?>())
			runAtOnce(THREADS) { thread ->
				val type = types[thread % types.size]
				got[type.ordinal].add(resource.getReader(type))
				shallow.add(resource.getShallowReader())
			}
			val distinct = IdentityHashMap<BinaryMapIndexReader, Boolean>()
			for (type in types) {
				val readers = got[type.ordinal]
				if (readers.isEmpty()) {
					continue
				}
				val first = readers[0] ?: fail("no ${type.name} reader")
				assertTrue(readers.all { it === first }, "every thread asking for ${type.name} gets one reader")
				assertTrue(distinct.put(first, true) == null, "${type.name} shares a reader with another type")
			}
			val firstShallow = shallow[0]
			assertTrue(shallow.all { it === firstShallow }, "one shallow reader")
			resource.close()
		}
	}

	@Test
	fun aReaderOpenedWhileTheFileIsClosedIsClosedToo() {
		repeat(ROUNDS) {
			val resource = BinaryMapReaderResource(fixture())
			val got = Collections.synchronizedList(ArrayList<BinaryMapIndexReader?>())
			runAtOnce(THREADS) { thread ->
				if (thread == 0) {
					resource.close()
				} else {
					got.add(resource.getReader(BinaryMapReaderResourceType.entries[thread % 3]))
				}
			}
			assertTrue(resource.isClosed())
			assertNull(resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
			for (reader in got) {
				if (reader != null) {
					assertTrue(runCatching { read(reader) }.isFailure, "a reader handed out before or during close is closed")
				}
			}
		}
	}

	@Test
	fun aFileThatFailsToOpenForOneTypeIsNotOpenedAgain() {
		val file = File.createTempFile("cut-short", ".obf")
		try {
			val bytes = File(fixture()).readBytes()
			file.writeBytes(bytes)
			val resource = BinaryMapReaderResource(file.path)
			val search = resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
			assertNotNull(search)
			// the file is cut short after its first reader opened, as a file being replaced is
			file.writeBytes(bytes.copyOf(bytes.size / 2))
			assertNull(resource.getReader(BinaryMapReaderResourceType.POI))
			// whole again: the resource does not open it again, a new one does
			file.writeBytes(bytes)
			assertNull(resource.getReader(BinaryMapReaderResourceType.ROUTING))
			assertSame(search, resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
			assertEquals(true, BinaryMapReaderResource(file.path).getShallowReader()?.containsAddressData())
			resource.close()
		} finally {
			file.delete()
		}
	}

	/** Runs [block] on [threads] threads released together. */
	private fun runAtOnce(threads: Int, block: (Int) -> Unit) {
		val pool = Executors.newFixedThreadPool(threads)
		val start = CountDownLatch(1)
		val errors = Collections.synchronizedList(ArrayList<Throwable>())
		for (i in 0 until threads) {
			pool.execute {
				start.await()
				try {
					block(i)
				} catch (e: Throwable) {
					errors.add(e)
				}
			}
		}
		start.countDown()
		pool.shutdown()
		assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "threads finished")
		if (errors.isNotEmpty()) {
			throw errors[0]
		}
	}

	companion object {
		private const val ROUNDS = 50
		private const val THREADS = 8
	}
}
