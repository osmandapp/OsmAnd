package net.osmand.shared.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkAPITest {

	@Test
	fun sendGetRequestReturnsBodyForSuccessfulResponse() {
		val requests = mutableListOf<HttpRequestData>()
		val api = NetworkAPI {
			MockEngine { request ->
				requests += request
				respond("body", HttpStatusCode.OK)
			}
		}

		val response = api.sendGetRequest("https://example.com/data", userAgent = "TestAgent")

		assertEquals("body", response.response)
		assertNull(response.error)
		assertEquals(HttpMethod.Get, requests.single().method)
		assertEquals("TestAgent", requests.single().headers[HttpHeaders.UserAgent])
	}

	@Test
	fun sendGetRequestReturnsErrorForNonOkResponse() {
		val api = NetworkAPI {
			MockEngine {
				respond("missing", HttpStatusCode.NotFound)
			}
		}

		val response = api.sendGetRequest("https://example.com/missing")

		assertNull(response.response)
		assertEquals("Not Found", response.error)
	}

	@Test
	fun sendGetRequestReturnsErrorForThrownException() {
		val api = NetworkAPI {
			MockEngine {
				error("boom")
			}
		}

		val response = api.sendGetRequest("https://example.com/error")

		assertNull(response.response)
		assertEquals("boom", response.error)
	}

	@Test
	fun sendGetRequestAddsBasicAuthHeader() {
		val requests = mutableListOf<HttpRequestData>()
		val api = NetworkAPI {
			MockEngine { request ->
				requests += request
				respond("ok", HttpStatusCode.OK)
			}
		}

		api.sendGetRequest("https://example.com/secure", auth = "user:pass")

		assertEquals("Basic dXNlcjpwYXNz", requests.single().headers[HttpHeaders.Authorization])
	}

	@Test
	fun sendGetRequestDecodesGzipResponseWhenRequested() {
		val requests = mutableListOf<HttpRequestData>()
		val gzipBytes = byteArrayOf(
			31, -117, 8, 0, 0, 0, 0, 0, 0, 3, 75, -50, -49, 45, 40, 74,
			45, 46, 78, 77, 1, 0, 30, 75, 86, -105, 10, 0, 0, 0
		)
		val api = NetworkAPI {
			MockEngine { request ->
				requests += request
				respond(
					content = gzipBytes,
					status = HttpStatusCode.OK,
					headers = headersOf(HttpHeaders.ContentEncoding, "gzip")
				)
			}
		}

		val response = api.sendGetRequest("https://example.com/gzip", useGzip = true)

		assertEquals("compressed", response.response)
		assertNull(response.error)
		assertTrue(requests.single().headers[HttpHeaders.AcceptEncoding]?.contains("gzip") == true)
	}

	@Test
	fun resolveRedirectUrlReturnsLocationForRedirect() {
		val requests = mutableListOf<HttpRequestData>()
		val api = NetworkAPI {
			MockEngine { request ->
				requests += request
				respond(
					content = "",
					status = HttpStatusCode.Found,
					headers = headersOf(HttpHeaders.Location, "https://example.com/next")
				)
			}
		}

		val redirectUrl = api.resolveRedirectUrl("https://example.com/start", "RedirectAgent")

		assertEquals("https://example.com/next", redirectUrl)
		assertEquals(HttpMethod.Head, requests.single().method)
		assertEquals("RedirectAgent", requests.single().headers[HttpHeaders.UserAgent])
	}

	@Test
	fun resolveRedirectUrlReturnsNullWhenLocationMissing() {
		val api = NetworkAPI {
			MockEngine {
				respond("", HttpStatusCode.Found)
			}
		}

		assertNull(api.resolveRedirectUrl("https://example.com/start"))
	}
}
