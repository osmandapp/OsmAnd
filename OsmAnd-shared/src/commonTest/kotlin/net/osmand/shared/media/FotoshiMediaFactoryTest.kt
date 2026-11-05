package net.osmand.shared.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FotoshiMediaFactoryTest {
	private val photo = """{"id":"abc123","properties":{"username":"mapper","creator_name":"Photo author","image_license":"CC-BY-4.0","captured_at":"2026-07-29","assets":{"thumbnail":{"href":"https://cdn.example/thumb.jpeg"},"image-1280":{"href":"https://cdn.example/display.jpeg?token=preserve"}}}}"""
	private fun collection(vararg photos: String) = """{"type":"FeatureCollection","features":[${photos.joinToString(",") }]}"""

	@Test fun typedIdentityOnly() {
		for (id in listOf("n123", "w123", "r123")) {
			assertTrue(FotoshiMediaFactory.requestUrl(id)!!.contains("osm_id=$id&"))
		}
		for (id in listOf(null, "", "123", "n0", "w-1", "node123", "n1&radius=50", "n01")) {
			assertNull(FotoshiMediaFactory.requestUrl(id))
		}
	}

	@Test fun preservesCreditLicenseDateAndAssets() {
		val item = FotoshiMediaFactory.fromResponse(collection(photo)).single()
		assertEquals("Photo author · mapper", item.details?.author)
		assertEquals("CC-BY-4.0", item.details?.license)
		assertEquals("2026-07-29", item.details?.date)
		assertEquals("https://cdn.example/display.jpeg?token=preserve", item.mediaUri)
		assertEquals("https://cdn.example/thumb.jpeg", item.previewUris.thumbnailUri)
		assertEquals("https://fotoshi.com/photo/abc123", item.sourceUri)
		assertNull(item.metadata.timestamp) // Date-only capture is not an exact instant.
	}

	@Test fun malformedOrErrorResponsesProduceNoPhotos() {
		for (body in listOf("", "not JSON", "[]", "{}", "{\"error\":\"quota\"}", collection("null", "42", "{}"))) {
			assertTrue(FotoshiMediaFactory.fromResponse(body).isEmpty())
		}
	}

	@Test fun unsafeIncompleteOrUnattributedPhotosAreSkipped() {
		for (bad in listOf(
			photo.replace("https://cdn.example/thumb", "http://cdn.example/thumb"),
			photo.replace("image-1280", "missing"),
			photo.replace("image_license", "missing"),
			photo.replace("creator_name", "missing").replace("username", "also_missing"),
			photo.replace("abc123", "../injected")
		)) assertTrue(FotoshiMediaFactory.fromResponse(collection(bad)).isEmpty())
	}

	@Test fun deduplicatesCapsAndKeepsOtherValidRows() {
		assertEquals(1, FotoshiMediaFactory.fromResponse(collection(photo, "null", photo)).size)
		val many = (1..8).map { photo.replace("abc123", "photo$it") }.toTypedArray()
		assertEquals(5, FotoshiMediaFactory.fromResponse(collection(*many)).size)
	}

	@Test fun unknownCaptureDateStaysUnknown() {
		val item = FotoshiMediaFactory.fromResponse(collection(photo.replace("\"2026-07-29\"", "null"))).single()
		assertNull(item.details?.date)
		assertNull(item.metadata.timestamp)
	}
	@Test fun keepsCreatorUploaderAndInstitutionWithoutRepeatingIdenticalCredit() {
		val supplied = photo.replace("\"creator_name\":", "\"source_organization\":\"City Archive\",\"creator_name\":")
		assertEquals("Photo author · mapper · City Archive", FotoshiMediaFactory.fromResponse(collection(supplied)).single().details?.author)
		val same = photo.replace("Photo author", "mapper")
		assertEquals("mapper", FotoshiMediaFactory.fromResponse(collection(same)).single().details?.author)
	}

	@Test fun detailsAndBrowserUsePhotoPageWhileDownloadKeepsImage() {
		val item = FotoshiMediaFactory.fromResponse(collection(photo)).single()
		assertEquals("https://fotoshi.com/photo/abc123", MediaUriResolver.getDetailsLink(item))
		assertEquals("https://fotoshi.com/photo/abc123", MediaUriResolver.getBrowserUri(item))
		assertEquals("https://cdn.example/display.jpeg?token=preserve", MediaUriResolver.getDownloadUri(item))
	}

}
