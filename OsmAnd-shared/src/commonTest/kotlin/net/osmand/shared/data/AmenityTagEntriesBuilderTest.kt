package net.osmand.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmenityTagEntriesBuilderTest {

	@Test
	fun splitsNameAndValue() {
		assertEquals(Pair("Smoking", "no"), AmenityTagEntriesBuilder.splitNameValue("Smoking: no"))
		assertEquals(Pair("Payant", "non"), AmenityTagEntriesBuilder.splitNameValue("Payant : non"))
		assertEquals(Pair("吸菸", "允許"), AmenityTagEntriesBuilder.splitNameValue("吸菸: 允許"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("Cash"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("1:25 fuel"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue("Yes"))
		assertNull(AmenityTagEntriesBuilder.splitNameValue(""))
		assertNull(AmenityTagEntriesBuilder.splitNameValue(null))
	}
}
