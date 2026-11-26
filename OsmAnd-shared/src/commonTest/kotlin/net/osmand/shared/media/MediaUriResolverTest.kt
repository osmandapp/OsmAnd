package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.media.domain.MediaPreviewUris
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.panoramax.PanoramaxApi
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaUriResolverTest {

	private fun remote(origin: MediaOrigin, externalUri: String?, downloadUri: String?) = MediaItem.Remote(
		id = "id",
		sourceUri = null,
		mediaUri = "https://host/sd.jpg",
		title = "",
		type = MediaType.PHOTO,
		origin = origin,
		previewUris = MediaPreviewUris(null, null, null),
		details = null,
		externalUri = externalUri,
		downloadUri = downloadUri
	)

	@Test
	fun panoramaxDetailsLinkOpensTheViewerPageWhileDownloadKeepsTheJpeg() {
		val item = remote(MediaOrigin.PANORAMAX, "https://viewer/page", "https://host/hd.jpg")

		assertEquals("https://viewer/page", MediaUriResolver.getDetailsLink(item))
		assertEquals("https://host/hd.jpg", MediaUriResolver.getDownloadUri(item))
		assertEquals("https://host/hd.jpg", MediaUriResolver.getShareUri(item))
	}

	@Test
	fun panoramaxDetailsLinkFallsBackToTheImageWithoutAViewerPage() {
		val item = remote(MediaOrigin.PANORAMAX, null, "https://host/hd.jpg")

		assertEquals("https://host/sd.jpg", MediaUriResolver.getDetailsLink(item))
	}

	@Test
	fun otherProvidersKeepTheirDetailsLink() {
		assertEquals(
			"https://viewer/page",
			MediaUriResolver.getDetailsLink(remote(MediaOrigin.WIKIPEDIA, "https://viewer/page", "https://host/hd.jpg"))
		)
		assertEquals(
			"https://host/hd.jpg",
			MediaUriResolver.getDetailsLink(remote(MediaOrigin.OTHER, "https://viewer/page", "https://host/hd.jpg"))
		)
	}

	@Test
	fun parsedPanoramaxPhotoLinksToItsViewerPage() {
		val id = "cafb0ec8-51dd-43ac-836c-8cd1f7cb8725"
		val response = """{"features":[{"id":"$id","assets":{"thumb":{"href":"https://m.example/t.jpg"},"sd":{"href":"https://m.example/sd.jpg"},"hd":{"href":"https://m.example/hd.jpg"}}}]}"""
		val item = PanoramaxMediaFactory.fromSearchResponse(response, listOf(id)).single()

		assertEquals(PanoramaxApi.getViewerUrl(id), MediaUriResolver.getDetailsLink(item))
		assertEquals("https://m.example/hd.jpg", MediaUriResolver.getDownloadUri(item))
	}

	@Test
	fun mapillaryPhotoLinksToItsPageAndShowsAttribution() {
		val response = """{"type":"mapillary-photo","key":"5569015689807461","url":"https://osmand.net/api/mapillary/photo-viewer?photo_id=5569015689807461","imageUrl":"https://cdn.example/256.jpg","imageHiresUrl":"https://cdn.example/1024.jpg","timestamp":"1585383644963","username":"dentonny"}"""
		val item = RemoteMediaFactory.fromJson(response, MediaOrigin.MAPILLARY)!!

		assertEquals("https://www.mapillary.com/app/?pKey=5569015689807461", MediaUriResolver.getDetailsLink(item))
		assertEquals("https://www.mapillary.com/app/?pKey=5569015689807461", MediaUriResolver.getBrowserUri(item))
		assertEquals("https://cdn.example/1024.jpg", MediaUriResolver.getDownloadUri(item))
		assertEquals("dentonny", item.details?.author)
		assertEquals("2020-03-28T08:20:44.963Z", item.details?.date)
		assertEquals("CC BY-SA 4.0", item.details?.license)
	}

	@Test
	fun otherRemotePhotosKeepNoAttribution() {
		val response = """{"key":"k","imageUrl":"https://cdn.example/256.jpg","username":"someone"}"""
		val item = RemoteMediaFactory.fromJson(response, MediaOrigin.OTHER)!!

		assertEquals(null, item.details)
		assertEquals(null, MediaUriResolver.getBrowserUri(item))
	}
}
