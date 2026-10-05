package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.osmand.shared.util.OpeningHoursParser;
import net.osmand.shared.util.OpeningHoursTime;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link OpeningHoursParser} is a copy of {@link net.osmand.util.OpeningHoursParser}; this parses the
 * same rules with both and asks both the same questions at the same moments.
 *
 * The rules are the ones the java test suite has collected over the years
 * ({@code opening_hours_rules.txt}), and the moments are a grid of three years at an odd step, so
 * every weekday, month, hour and a spread of minutes come up, on either side of every boundary a
 * rule can name - year, month, day of month, nth weekday, holiday, midnight.
 */
public class OpeningHoursCompatTest {

	private static final int STEP_MINUTES = 7 * 60 + 13;

	@BeforeClass
	public static void sameLocaleOnBothSides() {
		net.osmand.util.OpeningHoursParser.initLocalStrings(Locale.ENGLISH);
		net.osmand.util.OpeningHoursParser.setTwelveHourFormattingEnabled(false, Locale.ENGLISH);
		OpeningHoursParser.INSTANCE.initLocalStrings("en");
		OpeningHoursParser.INSTANCE.setTwelveHourFormattingEnabled(false, "en");
	}

	static List<String> rules() throws IOException {
		List<String> rules = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(OpeningHoursCompatTest.class.getResourceAsStream("/opening_hours_rules.txt")),
				StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty()) {
					rules.add(line.replace("\\\"", "\""));
				}
			}
		}
		return rules;
	}

	@Test
	public void testEveryRuleParsesAndPrintsTheSame() throws IOException {
		for (String rule : rules()) {
			net.osmand.util.OpeningHoursParser.OpeningHours j = net.osmand.util.OpeningHoursParser.parseOpenedHours(rule);
			OpeningHoursParser.OpeningHours k = OpeningHoursParser.INSTANCE.parseOpenedHours(rule);
			assertEquals(rule, j == null, k == null);
			if (j != null) {
				assertEquals(rule, j.toString(), k.toString());
				assertEquals(rule, j.getRules().size(), k.getRules().size());
			}
		}
	}

	@Test
	public void testEveryRuleAnswersTheSameAtEveryMoment() throws IOException {
		List<String> rules = rules();
		List<net.osmand.util.OpeningHoursParser.OpeningHours> js = new ArrayList<>();
		List<OpeningHoursParser.OpeningHours> ks = new ArrayList<>();
		for (String rule : rules) {
			js.add(net.osmand.util.OpeningHoursParser.parseOpenedHours(rule));
			ks.add(OpeningHoursParser.INSTANCE.parseOpenedHours(rule));
		}
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
		int moments = 0;
		while (cal.get(Calendar.YEAR) < 2025) {
			OpeningHoursTime time = OpeningHoursTime.Companion.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
					cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
			for (int i = 0; i < rules.size(); i++) {
				net.osmand.util.OpeningHoursParser.OpeningHours j = js.get(i);
				OpeningHoursParser.OpeningHours k = ks.get(i);
				if (j == null) {
					continue;
				}
				String m = rules.get(i) + " at " + cal.getTime();
				assertEquals(m, j.isOpenedForTime(cal), k.isOpenedForTime(time));
				assertEquals(m, j.getCurrentRuleTime(cal), k.getCurrentRuleTime(time));
				List<net.osmand.util.OpeningHoursParser.OpeningHours.Info> ji = j.getInfo(cal);
				List<OpeningHoursParser.OpeningHours.Info> ki = k.getInfo(time);
				assertNotNull(m, ki);
				assertEquals(m, ji.size(), ki.size());
				for (int s = 0; s < ji.size(); s++) {
					assertEquals(m + " #" + s, ji.get(s).isOpened(), ki.get(s).isOpened());
					assertEquals(m + " #" + s, ji.get(s).isOpened24_7(), ki.get(s).isOpened24_7());
					assertEquals(m + " #" + s, ji.get(s).isFallback(), ki.get(s).isFallback());
					assertEquals(m + " #" + s, ji.get(s).getInfo(), ki.get(s).getInfo());
				}
			}
			cal.add(Calendar.MINUTE, STEP_MINUTES);
			moments++;
		}
		assertEquals(true, moments > 3000);
	}
}
