package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.search.core.SearchPhraseTest
import okio.FileSystem
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
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The readers [BinaryMapReaderResource] and [ObfReaderRegistry] hand out, on copies of
 * `Turn_lanes_test.obf` that the tests rename, change, cut short or remove, wherever the tests run:
 * on Kotlin/Native a reader is closed through a file descriptor, not a java stream.
 */
class ObfReaderRegistryTest {

	private val fs = FileSystem.SYSTEM
	private lateinit var dir: Path

	@BeforeTest
	fun createDir() {
		dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "obf-reader-registry-${Random.nextLong().toULong()}"
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

	private fun cutShort(path: String) {
		val bytes = fs.read(path.toPath()) { readByteArray() }
		fs.write(path.toPath()) { write(bytes, 0, bytes.size / 2) }
	}

	private fun append(path: String) {
		fs.appendingSink(path.toPath()).use { it.write(okio.Buffer().writeByte(0), 1) }
	}

	private fun file(path: String, useForRouting: Boolean = false, useForPublicTransport: Boolean = false) =
		ObfReaderRegistry.ObfFile(path, useForRouting, useForPublicTransport)

	private fun cities(reader: BinaryMapIndexReader): Int =
		reader.getCities(null, CityBlocks.CITY_TOWN_TYPE).size

	private fun mapObjects(reader: BinaryMapIndexReader): Int =
		reader.searchMapIndex(SearchRequest.buildSearchRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, 15, null)).size

	private fun roads(reader: BinaryMapIndexReader): Int {
		val request = SearchRequest.buildSearchRouteRequest(0, Int.MAX_VALUE, 0, Int.MAX_VALUE)
		var roads = 0
		for (region in reader.getRoutingIndexes()) {
			for (box in reader.searchRouteIndexTree(request, region.getSubregions())) {
				roads += reader.loadRouteIndexData(box).count { it != null }
			}
		}
		return roads
	}

	private fun miguelGalindo(): SearchRequest<Amenity> =
		SearchRequest.buildSearchPoiRequest(0, 0, "Miguel", 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, null, null)

	@Test
	fun eachTypeReadsThroughACopyOverOneStructure() {
		val resource = BinaryMapReaderResource(copy("a.obf"))
		val shallow = resource.getShallowReader()
		val search = resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
		val address = resource.getReader(BinaryMapReaderResourceType.ADDRESS)
		assertNotNull(shallow)
		assertNotNull(search)
		assertNotNull(address)
		assertNotSame(shallow, search, "the reader that walked the file is not read with")
		assertNotSame(search, address)
		assertSame(address, resource.getReader(BinaryMapReaderResourceType.ADDRESS))
		assertSame(shallow.getPoiIndexes()[0], search.getPoiIndexes()[0], "the copies share the structure")
		resource.close()
	}

	/** A copy reads what a reader walking the file itself reads: addresses, map objects, roads and pois. */
	@Test
	fun aCopyReadsWhatAFreshReaderReads() {
		SearchPhraseTest.setUp
		val fresh = BinaryMapIndexReader(fixture())
		val resource = BinaryMapReaderResource(fixture())
		try {
			val copy = assertNotNull(resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
			assertEquals(cities(fresh), cities(copy))
			assertEquals(mapObjects(fresh), mapObjects(copy))
			assertEquals(roads(fresh), roads(copy))
			assertTrue(roads(copy) > 0)
			assertEquals(fresh.searchPoiByName(miguelGalindo()).size, copy.searchPoiByName(miguelGalindo()).size)
		} finally {
			fresh.close()
			resource.close()
		}
	}

	@Test
	fun aFileThatCannotBeReadHasNoReaders() {
		val damaged = copy("damaged.obf")
		cutShort(damaged)
		val resource = BinaryMapReaderResource(damaged)
		assertNull(resource.getShallowReader())
		assertNull(resource.getReader(BinaryMapReaderResourceType.POI))
		val missing = BinaryMapReaderResource((dir / "missing.obf").toString())
		assertNull(missing.getReader(BinaryMapReaderResourceType.ROUTING))
	}

	@Test
	fun aFileOfAnotherFormatVersionHasNoReaders() {
		val path = copy("version3.obf")
		val bytes = fs.read(path.toPath()) { readByteArray() }
		// the first field of an obf file is its version: tag 0x08, then the varint 2
		assertEquals(listOf<Byte>(0x08, 0x02), bytes.take(2))
		bytes[1] = 0x03
		fs.write(path.toPath()) { write(bytes) }
		val resource = BinaryMapReaderResource(path)
		assertNull(resource.getShallowReader())
		assertNull(resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
	}

	@Test
	fun aClosedFileHasNoReadersAndItsReadersFailToRead() {
		val resource = BinaryMapReaderResource(copy("a.obf"))
		val reader = resource.getReader(BinaryMapReaderResourceType.ADDRESS)!!
		resource.close()
		assertTrue(resource.isClosed())
		assertNull(resource.getReader(BinaryMapReaderResourceType.ADDRESS))
		assertNull(resource.getShallowReader())
		// a search still holding the reader gets an exception the shared code can catch
		assertFails { cities(reader) }
	}

	@Test
	fun theFilesAreInAndroidsOrder() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		val b = copy("b.obf")
		val region = copy("germany_berlin_europe.obf")
		val update = copy("germany_berlin_europe_25_10_28.live.obf")
		registry.setFiles(listOf(file(a), file(region), file(b), file(update)))
		// descending by name, a live update before its region: KAlgorithms.compareFileVersions
		assertEquals(
			listOf("germany_berlin_europe_25_10_28.live.obf", "germany_berlin_europe.obf", "b.obf", "a.obf"),
			registry.getFileReaders().map { it.getFileName() }
		)
		registry.close()
	}

	@Test
	fun anUnchangedFileKeepsItsReaders() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(file(a)))
		val reader = registry.getFileReader(a)!!.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
		registry.setFiles(listOf(file(a, useForRouting = true, useForPublicTransport = true)))
		val resource = registry.getFileReader(a)!!
		assertSame(reader, resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
		assertTrue(resource.isUseForRouting())
		assertTrue(resource.isUseForPublicTransport())
		registry.close()
	}

	@Test
	fun aChangedFileIsClosedAndOpenedAgain() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(file(a)))
		val before = registry.getFileReader(a)!!
		val reader = before.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)!!
		append(a)
		registry.setFiles(listOf(file(a)))
		val after = registry.getFileReader(a)!!
		assertNotSame(before, after)
		assertTrue(before.isClosed())
		assertFails { cities(reader) }
		val fresh = BinaryMapIndexReader(fixture())
		try {
			assertEquals(cities(fresh), cities(after.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)!!))
		} finally {
			fresh.close()
		}
		registry.close()
	}

	@Test
	fun aRemovedFileIsClosed() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		val b = copy("b.obf")
		registry.setFiles(listOf(file(a), file(b)))
		val resource = registry.getFileReader(a)!!
		resource.getShallowReader()
		registry.setFiles(listOf(file(b)))
		assertTrue(resource.isClosed())
		assertNull(registry.getFileReader(a))
		registry.close()
		assertTrue(registry.getFileReaders().isEmpty())
	}

	@Test
	fun routesAreReadOnlyFromTheFilesRoutingMayUse() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		val b = copy("b.obf")
		registry.setFiles(listOf(file(a, useForRouting = true), file(b)))
		val readers = registry.getRoutingMapFiles()
		assertEquals(listOf("a.obf"), readers.map { it.getFile().name() })
		assertSame(registry.getFileReader(a)!!.getReader(BinaryMapReaderResourceType.ROUTING), readers[0])
		registry.close()
	}

	@Test
	fun theQuickSearchLeavesOutTheIgnoredExtensions() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		val travel = copy("b.travel.obf")
		registry.setFiles(listOf(file(a), file(travel)))
		assertEquals(listOf("a.obf"), registry.getQuickSearchFiles(listOf(".travel.obf")).map { it.getFile().name() })
		assertEquals(2, registry.getQuickSearchFiles(emptyList()).size)
		registry.close()
	}

	@Test
	fun aFileHasOneAmenityRepositoryUntilItChanges() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(file(a)))
		val repository = registry.getAmenityRepository(a)!!
		assertSame(repository, registry.getAmenityRepository(a))
		assertNull(registry.getAmenityRepository((dir / "missing.obf").toString()))
		append(a)
		registry.setFiles(listOf(file(a)))
		val current = registry.getAmenityRepository(a)!!
		assertNotSame(repository, current)
		// the poi types every search test reads pois with
		SearchPhraseTest.setUp
		assertTrue(current.searchPoiByName(miguelGalindo()).isNotEmpty())
		// the old repository's file is closed: nothing, and no exception
		assertTrue(repository.searchPoiByName(miguelGalindo()).isEmpty())
		registry.close()
	}
}
