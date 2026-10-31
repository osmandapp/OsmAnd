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
 * `Turn_lanes_test.obf` that the tests change, cut short or remove, wherever the tests run: on
 * Kotlin/Native a reader is closed through a file descriptor, not a java stream.
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

	private fun copy(name: String): String {
		val path = dir / name
		fs.copy(RoutingTestFixtures.resource("Turn_lanes_test.obf").toPath(), path)
		return path.toString()
	}

	private fun cutShort(path: String) {
		val bytes = fs.read(path.toPath()) { readByteArray() }
		fs.write(path.toPath()) { write(bytes, 0, bytes.size / 2) }
	}

	private fun append(path: String) {
		fs.appendingSink(path.toPath()).use { it.write(okio.Buffer().writeByte(0), 1) }
	}

	private fun read(reader: BinaryMapIndexReader): Int =
		reader.getCities(null, CityBlocks.CITY_TOWN_TYPE).size

	private fun miguelGalindo(): SearchRequest<Amenity> =
		SearchRequest.buildSearchPoiRequest(0, 0, "Miguel", 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, null, null)

	@Test
	fun eachTypeHasAReaderOfItsOwn() {
		val resource = BinaryMapReaderResource(copy("a.obf"))
		val shallow = resource.getShallowReader()
		val search = resource.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
		val address = resource.getReader(BinaryMapReaderResourceType.ADDRESS)
		assertNotNull(shallow)
		assertSame(shallow, search, "the first type asked for takes the reader that was already open")
		assertNotNull(address)
		assertNotSame(search, address)
		assertSame(address, resource.getReader(BinaryMapReaderResourceType.ADDRESS))
		assertEquals(read(search!!), read(address))
		resource.close()
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
	fun aClosedFileHasNoReadersAndItsReadersFailToRead() {
		val resource = BinaryMapReaderResource(copy("a.obf"))
		val reader = resource.getReader(BinaryMapReaderResourceType.ADDRESS)!!
		resource.close()
		assertTrue(resource.isClosed())
		assertNull(resource.getReader(BinaryMapReaderResourceType.ADDRESS))
		assertNull(resource.getShallowReader())
		// a search still holding the reader gets an exception the shared code can catch
		assertFails { read(reader) }
	}

	@Test
	fun theFilesAreInTheOrderTheyWereHandedOver() {
		val registry = ObfReaderRegistry()
		val b = copy("b.obf")
		val a = copy("a.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(b, false), ObfReaderRegistry.ObfFile(a, false)))
		assertEquals(listOf("b.obf", "a.obf"), registry.getFileReaders().map { it.getFileName() })
		registry.close()
	}

	@Test
	fun anUnchangedFileKeepsItsReaders() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false)))
		val reader = registry.getFileReader(a)!!.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, true)))
		assertSame(reader, registry.getFileReader(a)!!.getReader(BinaryMapReaderResourceType.QUICK_SEARCH))
		assertTrue(registry.getFileReader(a)!!.isUseForRouting())
		registry.close()
	}

	@Test
	fun aChangedFileIsClosedAndOpenedAgain() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false)))
		val before = registry.getFileReader(a)!!
		val reader = before.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)!!
		append(a)
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false)))
		val after = registry.getFileReader(a)!!
		assertNotSame(before, after)
		assertTrue(before.isClosed())
		assertFails { read(reader) }
		assertEquals(read(after.getReader(BinaryMapReaderResourceType.QUICK_SEARCH)!!), read(after.getShallowReader()!!))
		registry.close()
	}

	@Test
	fun aRemovedFileIsClosed() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		val b = copy("b.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false), ObfReaderRegistry.ObfFile(b, false)))
		val resource = registry.getFileReader(a)!!
		resource.getShallowReader()
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(b, false)))
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
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, true), ObfReaderRegistry.ObfFile(b, false)))
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
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false), ObfReaderRegistry.ObfFile(travel, false)))
		assertEquals(listOf("a.obf"), registry.getQuickSearchFiles(listOf(".travel.obf")).map { it.getFile().name() })
		assertEquals(2, registry.getQuickSearchFiles(emptyList()).size)
		registry.close()
	}

	@Test
	fun aFileHasOneAmenityRepositoryUntilItChanges() {
		val registry = ObfReaderRegistry()
		val a = copy("a.obf")
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false)))
		val repository = registry.getAmenityRepository(a)!!
		assertSame(repository, registry.getAmenityRepository(a))
		assertNull(registry.getAmenityRepository((dir / "missing.obf").toString()))
		append(a)
		registry.setFiles(listOf(ObfReaderRegistry.ObfFile(a, false)))
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
