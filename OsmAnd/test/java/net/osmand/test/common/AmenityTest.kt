package net.osmand.test.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.binary.ObfConstants
import net.osmand.data.Amenity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmenityTest : AndroidTest() {
	@Test
	fun testCreateAmenityId() {
		val amenity = Amenity();

		//House with himeras
		var id = ObfConstants.createMapObjectIdFromOsmId(5366832956, 1)
		amenity.id = id
		assert(ObfConstants.getOsmObjectId(amenity) == 5366832956)

		//sophia
		id = ObfConstants.createMapObjectIdFromOsmId(27830282L, 2)
		amenity.id = id
		assert(ObfConstants.getOsmObjectId(amenity) == 27830282L)

		//Mariinskyi Palace
		id = ObfConstants.createMapObjectIdFromOsmId(15053407L, 3)
		amenity.id = id
		assert(ObfConstants.getOsmObjectId(amenity) == 15053407L)
	}
}
