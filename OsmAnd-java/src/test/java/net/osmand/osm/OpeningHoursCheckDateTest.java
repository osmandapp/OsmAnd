package net.osmand.osm;

import static net.osmand.gpx.GPXUtilities.OSM_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import net.osmand.data.Amenity;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpeningHoursCheckDateTest {

	private MapPoiTypes poiTypes;

	@Before
	public void setUp() {
		poiTypes = new MapPoiTypes("src/test/resources/poi_types.xml");
		poiTypes.init();
	}

	@Test
	public void checkDateOfOpeningHoursSurvivesExtensions() {
		Amenity amenity = parseCafe("2025-03-28");

		Map<String, String> extensions = amenity.getAmenityExtensions(poiTypes, false);
		assertEquals("2025-03-28", extensions.get(Amenity.CHECK_DATE_OPENING_HOURS));
		assertEquals("Mo-Fr 09:00-18:00", extensions.get(Amenity.OPENING_HOURS));
		assertEquals("2025-11-18", extensions.get(Amenity.CHECK_DATE));
		assertFalse(extensions.containsKey("xmas_opening_hours"));

		Map<String, String> prefixed = amenity.getAmenityExtensions(poiTypes, true);
		assertEquals("2025-03-28", prefixed.get(OSM_PREFIX + Amenity.CHECK_DATE_OPENING_HOURS));
		assertFalse(prefixed.containsKey(OSM_PREFIX + "xmas_opening_hours"));
	}

	@Test
	public void noCheckDateOfOpeningHoursWithoutTag() {
		Amenity amenity = parseCafe(null);

		Map<String, String> extensions = amenity.getAmenityExtensions(poiTypes, false);
		assertFalse(extensions.containsKey(Amenity.CHECK_DATE_OPENING_HOURS));
		assertEquals("Mo-Fr 09:00-18:00", extensions.get(Amenity.OPENING_HOURS));
	}

	private Amenity parseCafe(String openingHoursCheckDate) {
		Map<String, String> tags = new LinkedHashMap<>();
		tags.put("amenity", "cafe");
		tags.put("opening_hours", "Mo-Fr 09:00-18:00");
		tags.put("xmas:opening_hours", "Dec 24 10:00-14:00");
		tags.put("check_date", "2025-11-18");
		if (openingHoursCheckDate != null) {
			tags.put("check_date:opening_hours", openingHoursCheckDate);
		}
		Amenity amenity = poiTypes.parseAmenity("amenity", "cafe", false, tags);
		assertNotNull(amenity);
		return amenity;
	}
}
