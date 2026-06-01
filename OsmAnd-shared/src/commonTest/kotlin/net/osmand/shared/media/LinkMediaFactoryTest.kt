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
			Link("content://media/external/video/media/2048", "Gallery Video", "video/mp4"),
			Link("content://media/external/audio/media/4096", "Gallery Audio", "audio/mpeg"),
			Link("file://gallery/photo-1024.jpg", "iOS Gallery Photo", "image/jpeg"),
			Link("https://example.com/generic-photo.jpg", "Generic MIME Photo", "application/octet-stream"),
			Link("osmand://avnotes/audio-note.3gp", "Audio Note", "audio/3gpp"),
			Link("osmand://avnotes/video-note.mp4", "Video Note", "video/mp4"),
			Link("osmand://avnotes/photo.jpg", "Internal Photo", "image/jpeg"),
			Link("osmand://../photo.jpg", "Unsafe Parent", "image/jpeg"),
			Link("osmand://avnotes/../photo.jpg", "Unsafe Nested Parent", "image/jpeg"),
			Link("osmand:///photo.jpg", "Unsafe Absolute", "image/jpeg"),
			Link("osmand://C:/photo.jpg", "Unsafe Drive", "image/jpeg"),
			Link("osmand://?photo.jpg", "Unsafe Query", "image/jpeg"),
			Link("https://example.com/page", "Page", "text/html"),
			Link("https://example.com/page.jpg", "Html Page", "text/html"),
			Link("geo:50.45,30.52", "Geo", "image/jpeg")
		))

		assertEquals(9, items.size)
		assertTrue(items[0] is MediaItem.Remote)
		assertTrue(items[1] is MediaItem.Gallery)
		assertTrue(items[2] is MediaItem.Gallery)
		assertTrue(items[3] is MediaItem.Gallery)
		assertTrue(items[4] is MediaItem.Gallery)
		assertTrue(items[5] is MediaItem.Remote)
		assertTrue(items[6] is MediaItem.Internal)
		assertTrue(items[7] is MediaItem.Internal)
		assertTrue(items[8] is MediaItem.Internal)
		assertEquals(MediaType.PHOTO, items[0].type)
		assertEquals(MediaType.PHOTO, items[1].type)
		assertEquals(MediaType.VIDEO, items[2].type)
		assertEquals(MediaType.AUDIO, items[3].type)
		assertEquals(MediaType.PHOTO, items[4].type)
		assertEquals(MediaType.PHOTO, items[5].type)
		assertEquals(MediaType.AUDIO, items[6].type)
		assertEquals(MediaType.VIDEO, items[7].type)
		assertEquals(MediaType.PHOTO, items[8].type)
		assertEquals("https://example.com/view.jpg", items[0].mediaUri)
		assertEquals("content://media/external/images/media/1024", items[1].mediaUri)
		assertEquals("content://media/external/video/media/2048", items[2].mediaUri)
		assertEquals("content://media/external/audio/media/4096", items[3].mediaUri)
		assertEquals("file://gallery/photo-1024.jpg", items[4].mediaUri)
		assertEquals("https://example.com/generic-photo.jpg", items[5].mediaUri)
		assertEquals("avnotes/audio-note.3gp", items[6].mediaUri)
		assertEquals("avnotes/video-note.mp4", items[7].mediaUri)
		assertEquals("avnotes/photo.jpg", items[8].mediaUri)
	}
}
