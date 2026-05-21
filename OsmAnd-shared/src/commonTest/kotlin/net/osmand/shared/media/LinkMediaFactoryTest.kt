package net.osmand.shared.media

import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkMediaFactoryTest {

	@Test
	fun testFromLinksConvertsSupportedMediaLinks() {
		val items = LinkMediaFactory.fromLinks(listOf(
			Link("https://example.com/view.jpg", "Online View", "image/jpeg"),
			Link("content://media/external/images/media/1024", "Gallery Photo", "image/jpeg"),
			Link("file://gallery/photo-1024.jpg", "iOS Gallery Photo", "image/jpeg"),
			Link("osmand://avnotes/audio-note.3gp", "Audio Note", "audio/3gpp"),
			Link("osmand://avnotes/video-note.mp4", "Video Note", "video/mp4"),
			Link("osmand://avnotes/photo.jpg", "Internal Photo", "image/jpeg"),
			Link("osmand://../photo.jpg", "Unsafe Parent", "image/jpeg"),
			Link("osmand://avnotes/../photo.jpg", "Unsafe Nested Parent", "image/jpeg"),
			Link("osmand:///photo.jpg", "Unsafe Absolute", "image/jpeg"),
			Link("osmand://C:/photo.jpg", "Unsafe Drive", "image/jpeg"),
			Link("osmand://?photo.jpg", "Unsafe Query", "image/jpeg"),
			Link("https://example.com/page", "Page", "text/html"),
			Link("geo:50.45,30.52", "Geo", "image/jpeg")
		))

		assertEquals(6, items.size)
		assertTrue(items[0] is MediaItem.Remote)
		assertTrue(items[1] is MediaItem.Gallery)
		assertTrue(items[2] is MediaItem.Gallery)
		assertTrue(items[3] is MediaItem.Internal)
		assertTrue(items[4] is MediaItem.Internal)
		assertTrue(items[5] is MediaItem.Internal)
		assertEquals(MediaType.PHOTO, items[0].type)
		assertEquals(MediaType.PHOTO, items[1].type)
		assertEquals(MediaType.PHOTO, items[2].type)
		assertEquals(MediaType.AUDIO, items[3].type)
		assertEquals(MediaType.VIDEO, items[4].type)
		assertEquals(MediaType.PHOTO, items[5].type)
		assertEquals("https://example.com/view.jpg", items[0].mediaUri)
		assertEquals("content://media/external/images/media/1024", items[1].mediaUri)
		assertEquals("file://gallery/photo-1024.jpg", items[2].mediaUri)
		assertEquals("avnotes/audio-note.3gp", items[3].mediaUri)
		assertEquals("avnotes/video-note.mp4", items[4].mediaUri)
		assertEquals("avnotes/photo.jpg", items[5].mediaUri)
	}
}