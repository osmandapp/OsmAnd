package net.osmand.plus.gallery.online

import net.osmand.data.LatLon
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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

	@Test
	fun anIncompleteResultKeepsThePreviousPhotosItIsMissing() {
		val previous = holder(OnlinePhotosGroup.PANORAMAX_AMENITY to photo("p"), OnlinePhotosGroup.WIKIMEDIA to photo("w1"))
		val fresh = photo("w1", "fresh")
		val result = holder(OnlinePhotosGroup.WIKIMEDIA to fresh, OnlinePhotosGroup.WIKIMEDIA to photo("w2"))
		result.markIncomplete()

		result.keepItemsFrom(previous)

		assertEquals(listOf("p", "w1", "w2"), result.getItems().map { it.id })
		assertSame(fresh, result.getItems()[1])
		assertFalse(result.isComplete)
	}

	@Test
	fun aCompleteResultReplacesThePreviousPhotos() {
		val previous = holder(OnlinePhotosGroup.PANORAMAX_AMENITY to photo("p"))
		val result = holder(OnlinePhotosGroup.WIKIMEDIA to photo("w"))

		result.keepItemsFrom(previous)

		assertEquals(listOf("w"), result.getItems().map { it.id })
	}

	@Test
	fun anIncompleteResultWithoutPreviousPhotosStaysAsLoaded() {
		val result = OnlinePhotosHolder(LatLon(0.0, 0.0), emptyMap())
		result.markIncomplete()

		result.keepItemsFrom(null)

		assertTrue(result.getItems().isEmpty())
	}

	private fun holder(vararg items: Pair<OnlinePhotosGroup, MediaItem>): OnlinePhotosHolder {
		val holder = OnlinePhotosHolder(LatLon(0.0, 0.0), emptyMap())
		items.forEach { (group, item) -> holder.addItem(group, item) }
		return holder
	}

	private fun photo(id: String, title: String = id) = MediaItem.Remote(
		id = id,
		sourceUri = null,
		mediaUri = "https://host/$id.jpg",
		title = title,
		type = MediaType.PHOTO,
		previewUris = MediaPreviewUris(null, null, null),
		details = null,
		externalUri = null,
		downloadUri = null
	)
}
