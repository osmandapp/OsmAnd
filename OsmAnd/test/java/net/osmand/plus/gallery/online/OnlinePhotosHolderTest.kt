package net.osmand.plus.gallery.online

import net.osmand.data.LatLon
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlinePhotosHolderTest {

	@Test
	fun aFreshHolderIsCompleteUntilAProviderFails() {
		val holder = OnlinePhotosHolder(LatLon(0.0, 0.0), emptyMap())
		assertTrue(holder.isComplete)

		holder.markIncomplete()
		assertFalse(holder.isComplete)
	}

	@Test
	fun aSuccessfulLoadWithoutPhotosStaysComplete() {
		val holder = OnlinePhotosHolder(LatLon(0.0, 0.0), emptyMap())

		assertTrue(holder.getItems().isEmpty())
		assertTrue(holder.isComplete)
	}

	@Test
	fun anIncompleteHolderStaysIncompleteWhateverItHolds() {
		val holder = OnlinePhotosHolder(LatLon(0.0, 0.0), emptyMap())

		holder.markIncomplete()
		holder.clear()

		assertFalse(holder.isComplete)
	}
}
