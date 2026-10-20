package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaOrigin
import net.osmand.shared.panoramax.PanoramaxApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PanoramaxMediaFactoryTest {
	private val first = "cafb0ec8-51dd-43ac-836c-8cd1f7cb8725"
	private val second = "40714a31-b4ad-4d44-8ba1-82691538d32c"

	@Test
	fun parsesAndDeduplicatesOsmValues() {
		assertEquals(listOf(first), PanoramaxMediaFactory.parseIds(first))
		assertEquals(
			listOf(first, second),
			PanoramaxMediaFactory.parseIds(" ; $first ; bad ; $second; " + first.uppercase() + "; ")
		)
		assertTrue(PanoramaxMediaFactory.parseIds(null).isEmpty())
		assertTrue(PanoramaxMediaFactory.parseIds("").isEmpty())
		assertTrue(PanoramaxMediaFactory.parseIds(" ; invalid ; ").isEmpty())
	}

	@Test
	fun buildsBatchedCatalogRequest() {
		assertEquals(
			PanoramaxApi.API_URL + "search?ids=$first,$second&limit=2",
			PanoramaxApi.getSearchUrl(listOf(first, second))
		)
	}

	@Test
	fun convertsCatalogAssetsAndPreservesOsmOrder() {
		val response = """
			{"features":[
				{"id":"$second","assets":{"thumb":{"href":"https://member.example/second/thumb.jpg"},"sd":{"href":"https://member.example/second/sd.jpg"}}},
				{"id":"$first","assets":{"thumb":{"href":"https://member.example/first/thumb.jpg"},"sd":{"href":"https://member.example/first/sd.jpg"},"hd":{"href":"https://member.example/first/hd.jpg"}},
				 "geometry":{"type":"Point","coordinates":[7.72,48.59]},
				 "providers":[{"name":"Mapper","roles":["producer"]}],
				 "properties":{"datetime":"2024-11-28T14:53:01.301+02:00","license":"CC BY-SA 4.0","view:azimuth":11,"pers:interior_orientation":{"field_of_view":360}}}
			]}
		""".trimIndent()
		val items = PanoramaxMediaFactory.fromSearchResponse(response, listOf(first, second))
		assertEquals(listOf(first, second), items.map { it.id })
		val item = items[0]
		assertEquals(MediaOrigin.PANORAMAX, item.origin)
		assertEquals("https://member.example/first/thumb.jpg", item.previewUris.thumbnailUri)
		assertEquals("https://member.example/first/thumb.jpg", item.previewUris.standardSizeUri)
		assertEquals("https://member.example/first/sd.jpg", item.previewUris.fullSizeUri)
		assertEquals("https://member.example/first/sd.jpg", item.mediaUri)
		assertEquals("https://member.example/first/hd.jpg", item.downloadUri)
		assertEquals("Mapper", item.details?.author)
		assertEquals("CC BY-SA 4.0", item.details?.license)
		assertEquals("2024-11-28", item.details?.date)
		assertEquals(48.59, item.metadata.latitude)
		assertEquals(7.72, item.metadata.longitude)
		assertEquals(11.0, item.metadata.cameraAngle)
		assertEquals(1732798381301L, item.metadata.timestamp)
		assertTrue(item.metadata.is360)
		assertEquals(PanoramaxApi.getViewerUrl(first), item.sourceUri)
		assertEquals(PanoramaxApi.getViewerUrl(first), item.externalUri)
		assertEquals("https://member.example/second/sd.jpg", items[1].downloadUri)
	}

	@Test
	fun ignoresUnavailableUnexpectedAndMalformedFeatures() {
		val missing = "11111111-1111-1111-1111-111111111111"
		val response = """
			{"features":[
				{"id":"$first","assets":{"thumb":{"href":"https://member.example/thumb.jpg"}}},
				{"id":"$first","assets":{"thumb":{"href":"http://insecure.example/thumb.jpg"},"sd":{"href":"https://member.example/sd.jpg"}}},
				{"id":"$second","assets":{"thumb":{"href":"https://member.example/thumb.jpg"},"sd":{"href":"https://member.example/sd.jpg"}}},
				{"id":"00000000-0000-0000-0000-000000000000","assets":{"thumb":{"href":"https://member.example/thumb.jpg"},"sd":{"href":"https://member.example/sd.jpg"}}}
			]}
		""".trimIndent()
		assertEquals(
			listOf(second),
			PanoramaxMediaFactory.fromSearchResponse(response, listOf(first, missing, second)).map { it.id }
		)
	}

	@Test
	fun keepsFirstValidFeatureWhenCatalogContainsDuplicates() {
		val response = """
			{"features":[
				null,
				{"id":"$first"},
				{
					"id":"$first",
					"properties":{"title":"First valid photo"},
					"assets":{
						"thumb":{"href":"https://member.example/thumb.jpg"},
						"sd":{"href":"https://member.example/sd.jpg"}
					}
				},
				{
					"id":"${first.uppercase()}",
					"properties":{"title":"Duplicate"},
					"assets":{
						"thumb":{"href":"https://member.example/duplicate/thumb.jpg"},
						"sd":{"href":"https://member.example/duplicate/sd.jpg"}
					}
				}
			]}
		""".trimIndent()
		val item = PanoramaxMediaFactory.fromSearchResponse(response, listOf(first)).single()
		assertEquals(first, item.id)
		assertEquals("First valid photo", item.title)
		assertEquals("https://member.example/sd.jpg", item.mediaUri)
	}

	@Test
	fun keepsImageWhenOptionalMetadataIsMalformed() {
		val response = """
			{"features":[{
				"id":"$first",
				"assets":{
					"thumb":{"href":"https://member.example/thumb.jpg"},
					"sd":{"href":"https://member.example/sd.jpg"},
					"hd":{"href":"http://insecure.example/hd.jpg"}
				},
				"geometry":{"coordinates":["invalid",{}]},
				"providers":[null,{"roles":"producer"}],
				"properties":{
					"datetime":"invalid",
					"license":{},
					"view:azimuth":[],
					"pers:interior_orientation":false
				}
			}]}
		""".trimIndent()
		val item = PanoramaxMediaFactory.fromSearchResponse(response, listOf(first)).single()
		assertEquals("https://member.example/sd.jpg", item.downloadUri)
		assertNull(item.details?.author)
		assertNull(item.details?.date)
		assertNull(item.details?.license)
		assertNull(item.metadata.timestamp)
		assertNull(item.metadata.latitude)
		assertNull(item.metadata.longitude)
		assertTrue(item.metadata.cameraAngle.isNaN())
		assertFalse(item.metadata.is360)
	}

	@Test
	fun ignoresMissingOrMalformedCatalogResponse() {
		for (response in listOf(null, "", "{", "null", "[]", "{}", "{\"features\":{}}")) {
			assertTrue(PanoramaxMediaFactory.fromSearchResponse(response, listOf(first)).isEmpty())
		}
	}
}
