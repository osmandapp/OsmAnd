package net.osmand.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class OpeningHoursTimeTest {

	// 2024-06-01T12:00:00Z
	private val instant = 1717243200000L

	@Test
	fun testOfEpochMillisInZone() {
		val utc = OpeningHoursTime.ofEpochMillis(instant, "UTC")
		assertEquals(2024, utc.year)
		assertEquals(5, utc.month) // zero based, June
		assertEquals(1, utc.dayOfMonth)
		assertEquals(12, utc.hourOfDay)

		assertEquals(15, OpeningHoursTime.ofEpochMillis(instant, "Europe/Moscow").hourOfDay)
		// summer time is applied, New York is at UTC-4 in June
		assertEquals(8, OpeningHoursTime.ofEpochMillis(instant, "America/New_York").hourOfDay)
	}

	@Test
	fun testOfEpochMillisFallsBackToUtc() {
		// TimeZone.getTimeZone answers GMT for an unknown id, this keeps that behaviour
		val unknown = OpeningHoursTime.ofEpochMillis(instant, "Not/AZone")
		assertEquals(12, unknown.hourOfDay)
		assertEquals(1, unknown.dayOfMonth)
	}
}
