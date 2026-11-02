package net.osmand.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneNumberFormatterTest {

	private fun check(expected: String, value: String) {
		assertEquals(expected, PhoneNumberFormatter.format(value))
	}

	@Test
	fun testKnownPlans() {
		check("+33 6 12 34 56 78", "+33612345678")
		check("+33 6 12 34 56 78", "+33 612 345 678")
		check("+33 6 12 34 56 78", "0033 6 12 34 56 78")
		check("+1 415 555 1234", "+1 (415) 555-1234")
		check("+7 912 345 67 89", "+79123456789")
	}

	@Test
	fun testGenericGroups() {
		check("+49 301 234 5678", "+493012345678")
		check("+380 441 234 567", "+380441234567")
		check("+44 207 946 0958", "+44 2079460958")
	}

	@Test
	fun testKeptAsIs() {
		check("+49 30 1234567", "+49 30 1234567")
		check("+44 (0)20 7946 0958", "+44 (0)20 7946 0958")
		check("+33 (0)6 12 34 56 78", "+33 (0)6 12 34 56 78")
		check("0612345678", "0612345678")
		check("+33 6 12 34 56 78 ext. 12", "+33 6 12 34 56 78 ext. 12")
		check("+3312", "+3312")
		check("", "")
	}

	@Test
	fun testSeveralNumbers() {
		check("+33 6 12 34 56 78; +33 1 23 45 67 89", "+33612345678;+33123456789")
		check("+33 6 12 34 56 78; 112", "+33612345678; 112")
	}
}
