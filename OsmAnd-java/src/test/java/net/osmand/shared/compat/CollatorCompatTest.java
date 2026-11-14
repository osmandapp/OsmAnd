package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.RouteDataObject;
import net.osmand.shared.api.KStringMatcherMode;
import net.osmand.shared.util.KCollatorStringMatcher;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link net.osmand.shared.util.KCollatorStringMatcher} is a copy of {@link CollatorStringMatcher}
 * that reduces each string to a desktop-derived collation key instead of invoking a collator.
 * CHECK_CONTAINS additionally preserves Android-equivalent spaces.
 * Its reference below tailors only that space rule; it is not a full Android ICU implementation.
 * This asks both the same questions: every mode, over the road
 * names of the obf files the routing tests ship with, and over names picked to carry the things a
 * primary collator folds - the german sharp s, the ligatures, apostrophes, dashes, arabic
 * diacritics, greek final sigma.
 *
 * The names are real ones rather than invented, because invented names are the easy ones.
 */
public class CollatorCompatTest {

	/** Enough names to cover the alphabets in the test maps, few enough for the java matcher. */
	private static final int NAMES = 250;

	private static final String[] TRICKY_NAMES = {
			"Straße des 17. Juni", "Strasse des 17. Juni", "Auhofstraße", "Auhofstrasse",
			"Rue de l'Église", "Rue de lEglise", "L'Aquila", "OʼConnell Street",
			"Ærøskøbing", "Aeroskobing", "Þingvellir", "Thingvellir", "Œuvre", "Oeuvre",
			"Йошкар-Ола", "Йошкар Ола", "ЙошкарОла", "Ёлкино", "Елкино",
			"Ελληνικά", "Οδός", "ΟΔΟΣ", "στάσις", "στάσιΣ",
			"شارع الملك فهد", "شارع ٱلملك فهد", "طريق ٢٥",
			"İstanbul", "Istanbul", "Kraków", "Krakow", "Nová Ves", "Nova Ves",
			"New York", "New\u00A0York", "New\u1680York", "Café New\u2009York Deli",
			"New\u202FYork", "New\u205FYork", "New\u3000York", "Grifo Uris Bell",
			"A-1", "A 1", "A1", "B-2 north", "no.", "no", "st.", "st",
			"East 42-nd Street", "East 42 nd Street", "  double  space  ", "-", "--", "",
			"Đường Nguyễn Huệ", "Duong Nguyen Hue", "東京駅", "서울역",
	};

	private static final String[] TRICKY_QUERIES = {
			"new york", "newyork", "four", "", " ", "-", ".", "..", "a.", "st.", "no.", "strasse", "straße", "STRASSE",
			"église", "eglise", "l'a", "la", "ae", "æ", "th", "þ", "oe", "œ",
			"йошкар", "иошкар", "ола", "ёлкино", "елкино", "ellhnika", "ελλην", "οδος",
			"شارع", "istanbul", "ıstanbul", "krakow", "nova", "42", "42-nd", "42 nd",
			"a1", "a-1", "a 1", "東京", "nguyen", "nguyễn", "double space", "  ",
	};

	private static List<String> names;
	private static List<String> queries;

	@BeforeClass
	public static void corpus() throws IOException {
		Set<String> collectedNames = new LinkedHashSet<>();
		for (String name : TRICKY_NAMES) {
			collectedNames.add(name);
		}
		for (RouteDataObject road : TestObf.roads(2000)) {
			if (road == null || road.names == null) {
				continue;
			}
			for (String name : road.names.valueCollection()) {
				if (name != null && !name.isEmpty()) {
					collectedNames.add(name);
				}
				if (collectedNames.size() >= NAMES) {
					break;
				}
			}
			if (collectedNames.size() >= NAMES) {
				break;
			}
		}
		names = new ArrayList<>(collectedNames);

		Set<String> collectedQueries = new LinkedHashSet<>();
		for (String query : TRICKY_QUERIES) {
			collectedQueries.add(query);
		}
		// what someone typing one of these names would have entered along the way
		for (int i = 0; i < names.size(); i += 5) {
			String name = names.get(i);
			if (name.length() > 3) {
				collectedQueries.add(name.substring(0, 3));
			}
			if (name.length() > 6) {
				collectedQueries.add(name.substring(2, 6));
			}
			int space = name.indexOf(' ');
			if (space > 0) {
				collectedQueries.add(name.substring(0, space));
				collectedQueries.add(name.substring(space + 1));
			}
		}
		queries = new ArrayList<>(collectedQueries);
		assertTrue("names: " + names.size(), names.size() >= NAMES);
		assertTrue("queries: " + queries.size(), queries.size() > 100);
	}

	@Test
	public void matcherAnswersTheSame() {
		int compared = 0;
		for (StringMatcherMode mode : StringMatcherMode.values()) {
			for (String query : queries) {
				CollatorStringMatcher java = new CollatorStringMatcher(query, mode);
				KCollatorStringMatcher copy = new KCollatorStringMatcher(query, copyMode(mode));
				for (String name : names) {
					assertEquals(describe(mode, query, name), referenceMatches(java, name), copy.matches(name));
					compared++;
				}
			}
		}
		System.out.println("CollatorCompatTest: " + compared + " matches compared");
	}

	/**
	 * The query is handed to java lowercased, because the copy lowercases it and java's static
	 * entry point does not: java's collator only folds case for the scripts its locale rules name,
	 * so {@code cmatches} tells apart a cyrillic or greek capital from its small letter where
	 * {@code new CollatorStringMatcher(...).matches(...)}, which lowercases in the constructor,
	 * does not. Asking both the same question is what makes the rest of the comparison mean
	 * something.
	 */
	@Test
	public void cmatchesAnswersTheSame() {
		for (StringMatcherMode mode : StringMatcherMode.values()) {
			for (String query : queries) {
				String lowercased = query.toLowerCase();
				for (String name : names) {
					assertEquals(
							describe(mode, query, name),
							CollatorStringMatcher.cmatches(referenceCollator(mode), name, lowercased, mode),
							KCollatorStringMatcher.cmatches(name, query, copyMode(mode)));
				}
			}
		}
	}

	/**
	 * The dot at the end of a query means it was cut short, and turns an equals mode into a starts
	 * with one. Checked on its own because the corpus rarely produces a query ending in a dot.
	 */
	@Test
	public void anIncompleteQueryLoosensTheModeTheSameWay() {
		String[] incomplete = {"st.", "stras.", "no.", ".", "..", "a..", "42."};
		for (StringMatcherMode mode : StringMatcherMode.values()) {
			for (String query : incomplete) {
				CollatorStringMatcher java = new CollatorStringMatcher(query, mode);
				KCollatorStringMatcher copy = new KCollatorStringMatcher(query, copyMode(mode));
				assertEquals("mode of '" + query + "' as " + mode,
						java.getMode().name(), copy.getMode().name());
				assertEquals("part of '" + query + "' as " + mode,
						java.getPart(), copy.getPart().getText());
				for (String name : names) {
					assertEquals(describe(mode, query, name), referenceMatches(java, name), copy.matches(name));
				}
			}
		}
	}

	private static final net.osmand.Collator COLLATOR = net.osmand.OsmAndCollator.primaryCollator();

	private static final net.osmand.Collator CONTAINS_COLLATOR = containsCollator();

	private static net.osmand.Collator referenceCollator(StringMatcherMode mode) {
		return mode == StringMatcherMode.CHECK_CONTAINS ? CONTAINS_COLLATOR : COLLATOR;
	}

	private static boolean referenceMatches(CollatorStringMatcher matcher, String name) {
		return matcher.getMode() == StringMatcherMode.CHECK_CONTAINS
				? CollatorStringMatcher.cmatches(CONTAINS_COLLATOR, name, matcher.getPart(), matcher.getMode())
				: matcher.matches(name);
	}

	private static net.osmand.Collator containsCollator() {
		// Android API 37, en-US/de-DE/ru-RU: these 17 spaces are primary-equal, not ignorable.
		// Give them a distinct primary weight while preserving the JVM rules for everything else.
		RuleBasedCollator desktop = (RuleBasedCollator) java.text.Collator.getInstance(java.util.Locale.US);
		StringBuilder rules = new StringBuilder(desktop.getRules()).append(" & z < ' '");
		for (char space : "\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000".toCharArray()) {
			rules.append(" = '").append(space).append("'");
		}
		try {
			RuleBasedCollator collator = new RuleBasedCollator(rules.toString());
			collator.setStrength(java.text.Collator.PRIMARY);
			return net.osmand.OsmAndCollator.wrapCollator(collator);
		} catch (ParseException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static KStringMatcherMode copyMode(StringMatcherMode mode) {
		return KStringMatcherMode.valueOf(mode.name());
	}

	private static String describe(StringMatcherMode mode, String query, String name) {
		return mode + " '" + query + "' in '" + name + "'";
	}
}
