package net.osmand.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.osmand.data.Amenity;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class PanoramaxPoiTypeTest {

	@Test
	public void declaredTagSurvivesAmenityParsing() {
		MapPoiTypes poiTypes = new MapPoiTypes("src/test/resources/poi_types.xml");
		poiTypes.init();

		String id = "cafb0ec8-51dd-43ac-836c-8cd1f7cb8725";
		Map<String, String> tags = new LinkedHashMap<>();
		tags.put("amenity", "cafe");
		tags.put("panoramax", id);
		tags.put("image", "https://example.com/cafe.jpg");

		Amenity amenity = poiTypes.parseAmenity("amenity", "cafe", false, tags);
		assertNotNull(amenity);
		assertEquals(id, amenity.getAdditionalInfo(Amenity.PANORAMAX));
		assertEquals(id, amenity.getAmenityExtensions(poiTypes, false).get(Amenity.PANORAMAX));
		assertEquals("https://example.com/cafe.jpg", amenity.getAdditionalInfo("image"));
	}
}
