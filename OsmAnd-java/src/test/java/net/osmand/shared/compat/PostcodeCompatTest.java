package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.data.Postcode;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@link net.osmand.shared.data.Postcode} against {@link Postcode}: every country the rules know,
 * and two they do not, over postcodes written the ways people type them - with and without the
 * country prefix, spaced, dashed, in either case, cut short, and some that are not postcodes.
 */
public class PostcodeCompatTest {

	private static final List<String> INPUTS = Arrays.asList(
			"", "1", "12", "123", "1234", "12345", "123456", "1234567", "12345678", "123456789", "1234567890",
			"12 345", "12-345", "123 45", "01-234", "12345-6789", "12345 6789", "123-4567",
			"1101 DL", "1101-dl", "1101dl", "b288qp", "GIR 0AA", "IV21 2LR", "sw1a 1aa", "K1A 0B1", "k1a0b1",
			"STHL 1ZZ", "TKCA 1ZZ", "SEOUL 110-000", "AB 12", "ab1234", "A1234BCD", "c1234abc",
			"DE-10115", "de10115", "NL-1101 DL", "US-12345", "FR-75001", "RU-101000", "UA-01001", "01 001",
			"975 00", "97500", "47890", "9731", "97123", "98765", "street", "main 1", "1st", "10th avenue");

	@Test
	public void normalizeIsTheSame() throws Exception {
		int compared = 0;
		List<String> countries = countries();
		for (String country : countries) {
			for (String input : INPUTS) {
				String m = country + " '" + input + "'";
				assertEquals(m + " normalize", Postcode.normalize(input, country),
						net.osmand.shared.data.Postcode.INSTANCE.normalize(input, country));
				assertEquals(m + " looksLikePostcodeStart", Postcode.looksLikePostcodeStart(input, country),
						net.osmand.shared.data.Postcode.INSTANCE.looksLikePostcodeStart(input, country));
				compared++;
			}
		}
		System.out.println("PostcodeCompatTest: " + countries.size() + " countries, " + compared + " postcodes compared");
		assertTrue("countries: " + countries.size(), countries.size() > 100);
	}

	@SuppressWarnings("unchecked")
	private static List<String> countries() throws Exception {
		Field rules = Postcode.class.getDeclaredField("rules");
		rules.setAccessible(true);
		List<String> countries = new ArrayList<>(((Map<String, List<String>>) rules.get(null)).keySet());
		countries.add("Narnia");
		countries.add("");
		return countries;
	}
}
