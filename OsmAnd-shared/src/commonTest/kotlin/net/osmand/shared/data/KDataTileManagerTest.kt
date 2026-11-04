package net.osmand.shared.data

import net.osmand.shared.util.KMapUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KDataTileManagerTest {

	@Test
	fun testRegisterAndCount() {
		val manager = KDataTileManager<String>(15)
		assertTrue(manager.isEmpty())
		assertEquals(0, manager.getObjectsCount())

		manager.registerObject(50.45, 30.52, "kyiv")
		manager.registerObject(50.451, 30.521, "kyiv2")
		manager.registerObject(48.85, 2.35, "paris")

		assertFalse(manager.isEmpty())
		assertEquals(3, manager.getObjectsCount())
		assertEquals(3, manager.getAllObjects().size)
		assertTrue(manager.getTilesCount() in 2..3)
	}

	@Test
	fun testGetObjectsByLatLonBox() {
		val manager = KDataTileManager<String>(15)
		manager.registerObject(50.45, 30.52, "kyiv")
		manager.registerObject(48.85, 2.35, "paris")

		// arguments are (latitudeUp, longitudeUp, latitudeDown, longitudeDown)
		val around = manager.getObjects(50.5, 30.4, 50.4, 30.6)
		assertTrue(around.contains("kyiv"))
		assertFalse(around.contains("paris"))
	}

	@Test
	fun testGetObjectsBy31Box() {
		val manager = KDataTileManager<String>(15)
		val x31 = KMapUtils.get31TileNumberX(30.52)
		val y31 = KMapUtils.get31TileNumberY(50.45)
		manager.registerObjectXY(x31, y31, "kyiv")

		val shift = 1 shl 16
		val found = manager.getObjects(x31 - shift, y31 - shift, x31 + shift, y31 + shift)
		assertTrue(found.contains("kyiv"))

		val far = manager.getObjects(0, 0, 1000, 1000)
		assertFalse(far.contains("kyiv"))
	}

	@Test
	fun testUnregister() {
		val manager = KDataTileManager<String>(15)
		manager.registerObject(50.45, 30.52, "a")
		manager.registerObject(50.45, 30.52, "b")
		assertEquals(2, manager.getObjectsCount())

		manager.unregisterObject(50.45, 30.52, "a")
		assertEquals(1, manager.getObjectsCount())
		assertEquals(listOf("b"), manager.getAllObjects())

		// removing something that was never there must be a no-op
		manager.unregisterObject(0.0, 0.0, "c")
		assertEquals(1, manager.getObjectsCount())
	}

	@Test
	fun testGetClosestObjects() {
		val manager = KDataTileManager<String>(15)
		manager.registerObject(50.45, 30.52, "center")
		manager.registerObject(50.46, 30.53, "near")
		manager.registerObject(48.85, 2.35, "far")

		val closest = manager.getClosestObjects(50.45, 30.52, 5000.0)
		assertTrue(closest.contains("center"))
		assertTrue(closest.contains("near"))
		assertFalse(closest.contains("far"))

		assertTrue(KDataTileManager<String>().getClosestObjects(0.0, 0.0, 100.0).isEmpty())
	}

	@Test
	fun testEvaluateTileIsStableForTheSameTile() {
		val manager = KDataTileManager<String>(15)
		// start from the centre of a tile, otherwise a tiny offset can legitimately cross a boundary
		val tileX = KMapUtils.getTileNumberX(15.0, 30.52).toInt()
		val tileY = KMapUtils.getTileNumberY(15.0, 50.45).toInt()
		val lon = KMapUtils.getLongitudeFromTile(15.0, tileX + 0.5)
		val lat = KMapUtils.getLatitudeFromTile(15.0, tileY + 0.5)

		val a = manager.evaluateTile(lat, lon)
		val b = manager.evaluateTile(lat + 0.00001, lon + 0.00001)
		val c = manager.evaluateTile(48.85, 2.35)
		assertEquals(a, b)
		assertFalse(a == c)
	}

	@Test
	fun testLatLonAndX31TileKeysAgree() {
		val manager = KDataTileManager<String>(15)
		val random = net.osmand.shared.util.collections.XorShiftRandom(4711L)
		repeat(500) {
			val lat = 40.0 + random.nextInt(200_000) / 10_000.0
			val lon = -20.0 + random.nextInt(600_000) / 10_000.0
			val byLatLon = manager.evaluateTile(lat, lon)
			val byX31 = manager.evaluateTileXY(
				KMapUtils.get31TileNumberX(lon),
				KMapUtils.get31TileNumberY(lat)
			)
			assertEquals(byLatLon, byX31, "tile key mismatch at $lat/$lon")
		}
	}

	@Test
	fun testClear() {
		val manager = KDataTileManager<String>(15)
		manager.registerObject(50.45, 30.52, "a")
		manager.clear()
		assertTrue(manager.isEmpty())
		assertEquals(0, manager.getObjectsCount())
		assertTrue(manager.getAllObjects().isEmpty())
	}

	@Test
	fun testEveryRegisteredObjectIsFoundInItsOwnTile() {
		val manager = KDataTileManager<Int>(15)
		val random = net.osmand.shared.util.collections.XorShiftRandom(555L)
		val points = ArrayList<DoubleArray>()
		for (i in 0 until 2000) {
			val lat = 45.0 + random.nextInt(100_000) / 10_000.0
			val lon = 5.0 + random.nextInt(100_000) / 10_000.0
			points.add(doubleArrayOf(lat, lon))
			manager.registerObject(lat, lon, i)
		}
		assertEquals(2000, manager.getObjectsCount())
		for (i in points.indices) {
			val (lat, lon) = points[i]
			val found = manager.getObjects(lat + 0.001, lon - 0.001, lat - 0.001, lon + 0.001)
			assertTrue(found.contains(i), "object $i missing at $lat/$lon")
		}
	}
}
