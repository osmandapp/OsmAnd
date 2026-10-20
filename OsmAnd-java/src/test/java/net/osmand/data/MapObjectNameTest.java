package net.osmand.data;

import static org.junit.Assert.assertEquals;

import net.osmand.util.TransliterationHelper;

import org.junit.Test;

public class MapObjectNameTest {

	private static Amenity amenity(String name, String enName, String lang, String localName) {
		Amenity a = new Amenity();
		a.setName(name);
		if (enName != null) {
			a.setEnName(enName);
		}
		if (lang != null) {
			a.setName(lang, localName);
		}
		return a;
	}

	@Test
	public void testTransliterationPrefersEnglishName() {
		// #26647: German map language, Kyoto street with name:en but no name:de
		Amenity street = amenity("河原町通", "Kawaramachi Street", "ja", "河原町通");
		assertEquals("Kawaramachi Street", street.getName("de", true));
		assertEquals("河原町通", street.getName("de", false));
		assertEquals("Kawaramachi Street", street.getName("en", true));
	}

	@Test
	public void testTransliterationPrefersEnglishNameWithoutOtherNames() {
		Amenity shop = amenity("横大路車庫前", "Yokooji Shako-mae", null, null);
		assertEquals("Yokooji Shako-mae", shop.getName("de", true));
		assertEquals("横大路車庫前", shop.getName("de", false));
	}

	@Test
	public void testLocalNameStillWins() {
		Amenity city = amenity("Москва", "Moscow", "de", "Moskau");
		assertEquals("Moskau", city.getName("de", true));
		assertEquals("Moscow", city.getName("fr", true));
	}

	@Test
	public void testTransliterationWithoutEnglishName() {
		Amenity a = amenity("Улица Ленина", null, "ru", "Улица Ленина");
		assertEquals(TransliterationHelper.transliterate("Улица Ленина"), a.getName("de", true));
		Amenity b = amenity("Улица Ленина", null, null, null);
		assertEquals(TransliterationHelper.transliterate("Улица Ленина"), b.getName("de", true));
		assertEquals("Улица Ленина", b.getName("de", false));
	}
}
