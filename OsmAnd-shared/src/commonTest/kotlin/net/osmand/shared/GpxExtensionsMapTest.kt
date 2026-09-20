package net.osmand.shared

import net.osmand.shared.gpx.primitives.WptPt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GpxExtensionsMapTest {

	@Test
	fun testPutGetAndSize() {
		val point = WptPt()
		assertNull(point.extensions)
		assertTrue(point.getExtensionsToRead().isEmpty())

		val extensions = point.getExtensionsToWrite()
		assertNull(extensions.put("speed_sensor", "5.5"))
		assertNull(extensions.put("gpxtpx:hr", "145"))
		assertEquals(2, extensions.size)
		assertEquals("145", extensions["gpxtpx:hr"])
		assertEquals("5.5", extensions["speed_sensor"])
		assertNull(extensions["missing"])
		assertTrue(extensions.containsKey("gpxtpx:hr"))
		assertFalse(extensions.containsKey("missing"))
		assertEquals(2, point.getExtensionsToRead().size)
	}

	@Test
	fun testKeysAreKeptSorted() {
		val point = WptPt()
		val extensions = point.getExtensionsToWrite()
		for (key in listOf("speed", "color", "heading", "bearing", "amenity_type")) {
			extensions[key] = key.uppercase()
		}
		assertEquals(listOf("amenity_type", "bearing", "color", "heading", "speed"), extensions.keys.toList())
		assertEquals(listOf("AMENITY_TYPE", "BEARING", "COLOR", "HEADING", "SPEED"), extensions.values.toList())
	}

	@Test
	fun testPutReplacesValue() {
		val point = WptPt()
		val extensions = point.getExtensionsToWrite()
		extensions["speed"] = "1.0"
		assertEquals("1.0", extensions.put("speed", "2.0"))
		assertEquals(1, extensions.size)
		assertEquals("2.0", extensions["speed"])
	}

	@Test
	fun testRemove() {
		val point = WptPt()
		val extensions = point.getExtensionsToWrite()
		extensions["a"] = "1"
		extensions["b"] = "2"
		assertNull(extensions.remove("missing"))
		assertEquals("1", extensions.remove("a"))
		assertEquals(listOf("b"), extensions.keys.toList())
		assertEquals("2", extensions.remove("b"))
		// nothing left to keep, so the point holds no extensions again
		assertTrue(extensions.isEmpty())
		assertNull(point.extensions)
	}

	@Test
	fun testEntriesIteratorRemovesAndWritesThrough() {
		val point = WptPt()
		val extensions = point.getExtensionsToWrite()
		extensions["a"] = "1"
		extensions["b"] = "2"
		extensions["c"] = "3"

		val iterator = extensions.entries.iterator()
		while (iterator.hasNext()) {
			val entry = iterator.next()
			if (entry.key == "b") {
				iterator.remove()
			} else {
				entry.setValue(entry.value + "!")
			}
		}
		assertEquals(mapOf("a" to "1!", "c" to "3!"), point.getExtensionsToRead())
	}

	@Test
	fun testViewsOfTheSamePointShareTheData() {
		val point = WptPt()
		val first = point.getExtensionsToWrite()
		val second = point.getExtensionsToWrite()
		first["speed"] = "5.5"
		assertEquals("5.5", second["speed"])
		second.remove("speed")
		assertTrue(first.isEmpty())
	}

	@Test
	fun testExtensionsPropertyRoundTrip() {
		val point = WptPt()
		point.extensions = linkedMapOf("speed" to "5.5", "color" to "#ff0000")
		assertEquals(mapOf("color" to "#ff0000", "speed" to "5.5"), point.getExtensionsToRead())
		point.extensions = null
		assertNull(point.extensions)
		assertTrue(point.getExtensionsToRead().isEmpty())
	}

	@Test
	fun testDeferredExtensionsAreSeparate() {
		val point = WptPt()
		point.getExtensionsToWrite()["speed"] = "5.5"
		point.getDeferredExtensionsToWrite()["rpm"] = "2400"
		assertEquals(mapOf("speed" to "5.5"), point.getExtensionsToRead())
		assertEquals(mapOf("rpm" to "2400"), point.getDeferredExtensionsToRead())
	}

	@Test
	fun testTagNamesAreShared() {
		val first = WptPt()
		val second = WptPt()
		first.getExtensionsToWrite()[keyCopy()] = "1"
		second.getExtensionsToWrite()[keyCopy()] = "2"
		val firstKey = first.getExtensionsToRead().keys.single()
		val secondKey = second.getExtensionsToRead().keys.single()
		assertEquals(firstKey, secondKey)
		assertSame(firstKey, secondKey)
	}

	/** A name built at runtime, the way the parser produces it - never the same instance. */
	private fun keyCopy(): String = StringBuilder().append("gpxtpx:").append("cad").toString()
}
