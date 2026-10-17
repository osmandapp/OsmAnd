package net.osmand.osm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PoiAdditionalLabelsTest {

	@Test
	public void splitsNameAndValue() {
		assertArrayEquals(new String[] {"Fridge", "yes"}, PoiAdditionalLabels.splitNameValue("Fridge: yes"));
		assertArrayEquals(new String[] {"Payant", "non"}, PoiAdditionalLabels.splitNameValue("Payant : non"));
		assertArrayEquals(new String[] {"吸菸", "允許"}, PoiAdditionalLabels.splitNameValue("吸菸: 允許"));
		assertNull(PoiAdditionalLabels.splitNameValue("Yes"));
		assertNull(PoiAdditionalLabels.splitNameValue("Diesel"));
		assertNull(PoiAdditionalLabels.splitNameValue("1:25 fuel"));
		assertNull(PoiAdditionalLabels.splitNameValue(""));
		assertNull(PoiAdditionalLabels.splitNameValue(null));
	}

	@Test
	public void singleYesGroupIsASwitch() {
		assertTrue(PoiAdditionalLabels.isSingleYes(Collections.singletonList("yes")));
		assertFalse(PoiAdditionalLabels.isSingleYes(Collections.singletonList("designated")));
		assertFalse(PoiAdditionalLabels.isSingleYes(Arrays.asList("yes", "no")));

		assertEquals("Delivery", PoiAdditionalLabels.getSwitchLabel("Delivery: yes", "Delivery"));
		assertEquals("Cafe", PoiAdditionalLabels.getSwitchLabel("Cafe: yes", "Cafe"));
		// current strings: "Yes;Delivery" is translated as "Yes"
		assertEquals("Delivery", PoiAdditionalLabels.getSwitchLabel("Yes", "Delivery"));
	}

	@Test
	public void sameNameShowsTheValue() {
		assertEquals(Arrays.asList("Yes", "Limited", "No"), PoiAdditionalLabels.getGroupLabels(
				Arrays.asList("Changing table: yes", "Changing table: limited", "Changing table: no"),
				Arrays.asList("yes", "limited", "no")));
		assertEquals(Arrays.asList("Allowed", "Not allowed"), PoiAdditionalLabels.getGroupLabels(
				Arrays.asList("Smoking: allowed", "smoking: not allowed"),
				Arrays.asList("yes", "no")));
	}

	@Test
	public void differentNamesShowTheNameForYes() {
		assertEquals(Arrays.asList("Kayaks", "Canoe"), PoiAdditionalLabels.getGroupLabels(
				Arrays.asList("Kayaks: yes", "Canoe: yes"),
				Arrays.asList("yes", "yes")));
		// a name with other values than yes keeps the value
		assertEquals(Arrays.asList("Adoption: yes", "Adoption: no", "Release"), PoiAdditionalLabels.getGroupLabels(
				Arrays.asList("Adoption: yes", "Adoption: no", "Release: yes"),
				Arrays.asList("yes", "no", "yes")));
	}

	@Test
	public void searchTakesAcceptedValuesOnly() {
		assertTrue(PoiAdditionalLabels.isAcceptedValue("yes"));
		assertTrue(PoiAdditionalLabels.isAcceptedValue("only"));
		assertTrue(PoiAdditionalLabels.isAcceptedValue("designated"));
		// options are not states: fuel:diesel, healthcare:speciality, station=subway
		assertTrue(PoiAdditionalLabels.isAcceptedValue("gynaecology"));
		assertTrue(PoiAdditionalLabels.isAcceptedValue("subway"));
		assertFalse(PoiAdditionalLabels.isAcceptedValue("no"));
		assertFalse(PoiAdditionalLabels.isAcceptedValue("limited"));
		assertFalse(PoiAdditionalLabels.isAcceptedValue("outside"));
	}

	@Test
	public void searchNameIsThePartBeforeTheColon() {
		assertEquals("Станция метро", PoiAdditionalLabels.getSearchName("Станция метро: да", "Subway station: yes", "метро", "Станция метро"));
		// older strings: "Да;Станция метро" translates to "Да"
		assertEquals("Станция метро", PoiAdditionalLabels.getSearchName("Да", "Yes", "Станция метро", "Станция метро"));
		assertEquals("Питьевая вода", PoiAdditionalLabels.getSearchName("Да", "Yes", "", "Питьевая вода"));
		assertEquals("Takeaway", PoiAdditionalLabels.getSearchName("Takeaway", "Takeaway", "", "Takeaway"));
		assertEquals("Vegan", PoiAdditionalLabels.getSearchName("Vegan", "Vegan", "", "Diet"));
	}

	@Test
	public void translationsWithoutNameStayAsTheyAre() {
		List<String> current = Arrays.asList("Limited", "No", "Yes");
		assertEquals(current, PoiAdditionalLabels.getGroupLabels(current, Arrays.asList("limited", "no", "yes")));
		assertEquals(Arrays.asList("Diesel", "Octane 95", "Reverse charge calls"), PoiAdditionalLabels.getGroupLabels(
				Arrays.asList("Diesel", "Octane 95", "Reverse charge calls: yes"),
				Arrays.asList("yes", "yes", "yes")));
	}
}
