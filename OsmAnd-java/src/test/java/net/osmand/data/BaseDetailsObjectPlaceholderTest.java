package net.osmand.data;

import static org.junit.Assert.assertEquals;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BaseDetailsObjectPlaceholderTest {

	private static final LatLon VILLAGE = new LatLon(50.3716, 30.4310);

	private static MapPoiTypes poiTypes;

	@BeforeClass
	public static void loadPoiTypes() {
		poiTypes = new MapPoiTypes("src/test/resources/poi_types.xml");
		MapPoiTypes.setDefault(poiTypes);
		poiTypes.setPoiTranslator(new KeyTranslator());
	}

	@Test
	public void drawnPlaceReplacesSearchResultPlaceholder() {
		BaseDetailsObject details = new BaseDetailsObject(Arrays.asList(createPlaceholder(), createDrawnVillage()), "en");

		Amenity amenity = details.getSyntheticAmenity();
		assertEquals("village", amenity.getSubType());
		assertEquals(poiTypes.getPoiTypeByKey("village").getTranslation(), amenity.getMainSubtype());
	}

	@Test
	public void placeholderWithDrawnObjectOfUnknownTypeKeepsSubtype() {
		RenderedObject drawn = createDrawnObject("unknown_tag", "unknown_value");
		BaseDetailsObject details = new BaseDetailsObject(Arrays.asList(createPlaceholder(), drawn), "en");

		Amenity amenity = details.getSyntheticAmenity();
		assertEquals("", amenity.getSubType());
		assertEquals("", amenity.getMainSubtype());
	}

	@Test
	public void amenityReplacesSearchResultPlaceholder() {
		Amenity cafe = new Amenity();
		cafe.setType(poiTypes.getPoiCategoryByName("sustenance"));
		cafe.setSubType("cafe");
		cafe.setName("Cafe");
		cafe.setLocation(VILLAGE);
		List<Amenity> amenities = Arrays.asList(createPlaceholder(), cafe);

		Amenity amenity = new BaseDetailsObject(amenities, "en").getSyntheticAmenity();
		assertEquals("cafe", amenity.getSubType());
	}

	private static Amenity createPlaceholder() {
		Amenity placeholder = new Amenity();
		placeholder.setType(poiTypes.getOtherPoiCategory());
		placeholder.setSubType("");
		placeholder.setName("Hatne");
		placeholder.setLocation(VILLAGE);
		return placeholder;
	}

	private static RenderedObject createDrawnVillage() {
		return createDrawnObject("place", "village");
	}

	private static RenderedObject createDrawnObject(String tag, String value) {
		RenderedObject drawn = new RenderedObject();
		drawn.putTag(tag, value);
		drawn.putTag("name", "Hatne");
		drawn.setLabelLatLon(VILLAGE);
		return drawn;
	}

	private static class KeyTranslator implements PoiTranslator {

		@Override
		public String getTranslation(AbstractPoiType type) {
			return type.getKeyName();
		}

		@Override
		public String getTranslation(String keyName) {
			return keyName;
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			return getTranslation(type);
		}

		@Override
		public String getEnTranslation(String keyName) {
			return keyName;
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			return "";
		}

		@Override
		public String getSynonyms(String keyName) {
			return "";
		}

		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "";
		}
	}
}