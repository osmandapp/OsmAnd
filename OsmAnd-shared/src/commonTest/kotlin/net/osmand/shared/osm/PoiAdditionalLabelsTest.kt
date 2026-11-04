package net.osmand.shared.osm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `PoiAdditionalLabelsTest` of OsmAnd-java, on the copy. */
class PoiAdditionalLabelsTest {

	@Test
	fun splitsNameAndValue() {
		assertEquals(listOf("Fridge", "yes"), PoiAdditionalLabels.splitNameValue("Fridge: yes")?.toList())
		assertEquals(listOf("Payant", "non"), PoiAdditionalLabels.splitNameValue("Payant : non")?.toList())
		assertEquals(listOf("吸菸", "允許"), PoiAdditionalLabels.splitNameValue("吸菸: 允許")?.toList())
		assertNull(PoiAdditionalLabels.splitNameValue("Yes"))
		assertNull(PoiAdditionalLabels.splitNameValue("Diesel"))
		assertNull(PoiAdditionalLabels.splitNameValue("1:25 fuel"))
		assertNull(PoiAdditionalLabels.splitNameValue(""))
		assertNull(PoiAdditionalLabels.splitNameValue(null))
	}

	@Test
	fun singleYesGroupIsASwitch() {
		assertTrue(PoiAdditionalLabels.isSingleYes(listOf("yes")))
		assertFalse(PoiAdditionalLabels.isSingleYes(listOf("designated")))
		assertFalse(PoiAdditionalLabels.isSingleYes(listOf("yes", "no")))

		assertEquals("Delivery", PoiAdditionalLabels.getSwitchLabel("Delivery: yes", "Delivery"))
		assertEquals("Cafe", PoiAdditionalLabels.getSwitchLabel("Cafe: yes", "Cafe"))
		// current strings: "Yes;Delivery" is translated as "Yes"
		assertEquals("Delivery", PoiAdditionalLabels.getSwitchLabel("Yes", "Delivery"))
	}

	@Test
	fun sameNameShowsTheValue() {
		assertEquals(
			listOf("Yes", "Limited", "No"), PoiAdditionalLabels.getGroupLabels(
				listOf("Changing table: yes", "Changing table: limited", "Changing table: no"),
				listOf("yes", "limited", "no")
			)
		)
		assertEquals(
			listOf("Allowed", "Not allowed"), PoiAdditionalLabels.getGroupLabels(
				listOf("Smoking: allowed", "smoking: not allowed"),
				listOf("yes", "no")
			)
		)
	}

	@Test
	fun differentNamesShowTheNameForYes() {
		assertEquals(
			listOf("Kayaks", "Canoe"), PoiAdditionalLabels.getGroupLabels(
				listOf("Kayaks: yes", "Canoe: yes"),
				listOf("yes", "yes")
			)
		)
		// a name with other values than yes keeps the value
		assertEquals(
			listOf("Adoption: yes", "Adoption: no", "Release"), PoiAdditionalLabels.getGroupLabels(
				listOf("Adoption: yes", "Adoption: no", "Release: yes"),
				listOf("yes", "no", "yes")
			)
		)
	}

	@Test
	fun searchTakesAcceptedValuesOnly() {
		assertTrue(PoiAdditionalLabels.isAcceptedValue("yes"))
		assertTrue(PoiAdditionalLabels.isAcceptedValue("only"))
		assertTrue(PoiAdditionalLabels.isAcceptedValue("designated"))
		// options are not states: fuel:diesel, healthcare:speciality, station=subway
		assertTrue(PoiAdditionalLabels.isAcceptedValue("gynaecology"))
		assertTrue(PoiAdditionalLabels.isAcceptedValue("subway"))
		assertFalse(PoiAdditionalLabels.isAcceptedValue("no"))
		assertFalse(PoiAdditionalLabels.isAcceptedValue("limited"))
		assertFalse(PoiAdditionalLabels.isAcceptedValue("outside"))
	}

	@Test
	fun searchNameIsThePartBeforeTheColon() {
		assertEquals("Станция метро", PoiAdditionalLabels.getSearchName("Станция метро: да", "Subway station: yes", "метро", "Станция метро"))
		// older strings: "Да;Станция метро" translates to "Да"
		assertEquals("Станция метро", PoiAdditionalLabels.getSearchName("Да", "Yes", "Станция метро", "Станция метро"))
		assertEquals("Питьевая вода", PoiAdditionalLabels.getSearchName("Да", "Yes", "", "Питьевая вода"))
		assertEquals("Takeaway", PoiAdditionalLabels.getSearchName("Takeaway", "Takeaway", "", "Takeaway"))
		assertEquals("Vegan", PoiAdditionalLabels.getSearchName("Vegan", "Vegan", "", "Diet"))
	}

	@Test
	fun translationsWithoutNameStayAsTheyAre() {
		val current = listOf("Limited", "No", "Yes")
		assertEquals(current, PoiAdditionalLabels.getGroupLabels(current, listOf("limited", "no", "yes")))
		assertEquals(
			listOf("Diesel", "Octane 95", "Reverse charge calls"), PoiAdditionalLabels.getGroupLabels(
				listOf("Diesel", "Octane 95", "Reverse charge calls: yes"),
				listOf("yes", "yes", "yes")
			)
		)
	}
}
