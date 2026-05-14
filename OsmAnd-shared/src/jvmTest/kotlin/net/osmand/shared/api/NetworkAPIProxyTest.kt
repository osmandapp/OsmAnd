package net.osmand.shared.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkAPIProxyTest {

	@Test
	fun proxyStateReflectsAppSetProxy() {
		val api = NetworkAPI()

		assertFalse(api.hasProxy())
		assertNull(api.proxyHost)
		assertEquals(0, api.proxyPort)

		api.setProxy("127.0.0.1", 8080)

		assertTrue(api.hasProxy())
		assertEquals("127.0.0.1", api.proxyHost)
		assertEquals(8080, api.proxyPort)

		api.setProxy(null, 0)

		assertFalse(api.hasProxy())
		assertNull(api.proxyHost)
		assertEquals(0, api.proxyPort)
	}
}
