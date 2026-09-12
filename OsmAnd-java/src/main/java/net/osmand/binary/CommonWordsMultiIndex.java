package net.osmand.binary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.osmand.util.SearchAlgorithms;

/**
 * Which words of a name become keys of the name index, decided by the language group of the map.
 * <p>
 * A group is a list of countries (map name prefixes) and the name statistics of its words:
 * <ul>
 * <li>class 1, service words ("rue", "de", "road", "вулиця"): dropped when the name keeps a word outside class 1;</li>
 * <li>class 2, frequent words ("chemin", "school"): dropped when the name has a word at least {@link #RARER_FACTOR}
 * times rarer;</li>
 * <li>every other word stays a key.</li>
 * </ul>
 * A name that has words always keeps one: the rarest word outside class 1 has nothing ten times rarer, and when no
 * word outside class 1 is left, the class 1 words stay. Numbers are returned as they are and take no part in the rules.
 * <p>
 * Groups are {@link #DEFAULT_GROUPS}: every map of the download list by language, split where a country speaks
 * several (Belgium, Canada, Switzerland, Finland). Words: {@code common_words_groups.tsv} next to this class, lines
 * {@code word <group> <class 0|1|2> <names per million> <word>}. A word of class 0 is only a frequency to compare with;
 * a word the file does not have counts as rare. Data with lines {@code group <id> <prefix,prefix...>} replaces the
 * default groups.
 */
public class CommonWordsMultiIndex {

	public static final int RARER_FACTOR = 10;
	public static final String RESOURCE = "common_words_groups.tsv";

	private static final int KEEP = 0;
	private static final int SERVICE = 1;
	private static final int FREQUENT = 2;

	// language groups: the group id, then the map name prefixes it covers (the longest prefix of a map name wins)
	private static final String[][] DEFAULT_GROUPS = {
		// French
		{ "fr", "france", "belgium_wallonia", "luxembourg", "monaco", "canada_quebec", "switzerland_lake-geneva",
			"reunion", "guadeloupe", "martinique", "mayotte", "french-guiana", "saint-barthelemy",
			"saint-martin", "saint-pierre-and-miquelon", "french-southern-and-antarctic-lands", "haiti",
			"senegal", "ivory-coast", "mali", "burkina-faso", "niger", "guinea", "benin", "togo", "cameroon",
			"gabon", "congo-brazzaville", "congo-democratic-republic", "central-african-republic", "chad",
			"madagascar", "comoros", "djibouti", "burundi", "rwanda", "seychelles", "mauritius" },
		// English
		{ "en", "us", "gb", "ireland", "isle-of-man", "channel-islands", "australia-oceania", "new-zealand",
			"oceania", "canada", "india", "south-africa", "nigeria", "ghana", "kenya", "uganda", "tanzania",
			"zambia", "zimbabwe", "malawi", "botswana", "namibia", "lesotho", "swaziland", "liberia",
			"sierra-leone", "gambia", "jamaica", "bahamas", "barbados", "trinidad-and-tobago", "belize",
			"guyana", "bermuda", "cayman-islands", "virgin-islands-us", "virgin-islands-british",
			"turks-and-caicos-islands", "anguilla", "antigua-and-barbuda", "dominica", "grenada",
			"saint-kitts-and-nevis", "saint-lucia", "saint-vincent-and-the-grenadines", "montserrat",
			"falkland-islands", "saint-helena-ascension-and-tristan-da-cunha", "british-indian-ocean-territory",
			"south-georgia-and-south-sandwich-islands", "malta", "singapore", "philippines", "papua-new-guinea",
			"christmas-island", "carribean-archipelago-all", "south-sudan" },
		// German
		{ "de", "germany", "austria", "liechtenstein", "switzerland" },
		// Dutch
		{ "nl", "netherlands", "belgium_flanders", "netherlands-antilles", "aruba", "suriname" },
		// Spanish
		{ "es", "spain", "andorra", "mexico", "peru", "argentina", "chile", "colombia", "venezuela", "ecuador",
			"bolivia", "paraguay", "uruguay", "cuba", "dominican-republic", "puerto-rico", "guatemala",
			"honduras", "el-salvador", "nicaragua", "costa-rica", "panama", "equatorial-guinea" },
		// Portuguese
		{ "pt", "portugal", "azores", "madeira", "brazil", "angola", "mozambique", "cape-verde", "guinea-bissau",
			"sao-tome-and-principe", "east-timor" },
		// Italian
		{ "it", "italy", "san-marino", "switzerland_ticino" },
		// East Slavic and Central Asia
		{ "esl", "ukraine", "belarus", "russia", "transnistria", "kazakhstan", "kyrgyzstan", "tajikistan",
			"turkmenistan", "uzbekistan", "mongolia" },
		// West Slavic
		{ "wsl", "poland", "czech-republic", "slovakia" },
		// South Slavic
		{ "ssl", "serbia", "croatia", "bosnia-herzegovina", "montenegro", "slovenia", "macedonia", "bulgaria",
			"kosovo", "albania" },
		// Romanian
		{ "ro", "romania", "moldova" },
		// Hungarian
		{ "hu", "hungary" },
		// Greek
		{ "el", "greece", "cyprus" },
		// Baltic
		{ "bal", "lithuania", "latvia" },
		// Scandinavian
		{ "nor", "norway", "sweden", "denmark", "iceland", "faroe-islands", "greenland", "finland_aland" },
		// Finnish and Estonian
		{ "fi", "finland", "estonia" },
		// Turkish
		{ "tr", "turkey", "azerbaijan" },
		// Arabic
		{ "ar", "jordan", "egypt", "saudi-arabia", "iraq", "syria", "lebanon", "palestine", "yemen", "oman",
			"united-arab-emirates", "qatar", "bahrain", "kuwait", "libya", "sudan", "mauritania", "somalia" },
		// Maghreb, Arabic and French
		{ "mag", "algeria", "morocco", "tunisia", "western-sahara" },
		// Persian
		{ "fa", "iran", "afghanistan" },
		// Hebrew
		{ "he", "israel" },
		// Chinese and Japanese
		{ "cjk", "china", "japan", "taiwan", "hong-kong", "macao" },
		// Korean
		{ "ko", "south-korea", "north-korea" },
		// Vietnamese
		{ "vi", "vietnam" },
		// Malay and Indonesian
		{ "ms", "indonesia", "malaysia", "brunei" },
		// Thai
		{ "th", "thailand" },
		// Indochina
		{ "ind", "laos", "cambodia", "myanmar" },
		// South Asia
		{ "sas", "pakistan", "bangladesh", "nepal", "sri-lanka", "bhutan", "maldives" },
		// Caucasus
		{ "cau", "georgia", "armenia" },
		// Others
		{ "oth", "ethiopia", "eritrea", "spratly-islands", "antarctica" },
	};

	private static CommonWordsMultiIndex instance;

	private final Map<String, WordsGroup> groupsById = new HashMap<>();
	private final Map<String, WordsGroup> groupsByCountry = new HashMap<>();

	private static class WordsGroup {
		final String id;
		final Map<String, int[]> words = new HashMap<>(); // word -> class, names per million

		WordsGroup(String id) {
			this.id = id;
		}
	}

	public static CommonWordsMultiIndex getInstance() {
		if (instance == null) {
			try (InputStream is = CommonWordsMultiIndex.class.getResourceAsStream(RESOURCE)) {
				if (is == null) {
					throw new IllegalStateException(RESOURCE + " is not found next to " + CommonWordsMultiIndex.class.getName());
				}
				instance = load(is);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return instance;
	}

	public static CommonWordsMultiIndex load(InputStream is) throws IOException {
		CommonWordsMultiIndex index = new CommonWordsMultiIndex();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] p = line.split("\t");
			if (p[0].equals("group") && p.length >= 3) {
				WordsGroup g = index.groupsById.computeIfAbsent(p[1], WordsGroup::new);
				for (String country : p[2].split(",")) {
					index.groupsByCountry.put(country.trim().toLowerCase(Locale.ROOT), g);
				}
			} else if (p[0].equals("word") && p.length >= 5) {
				WordsGroup g = index.groupsById.computeIfAbsent(p[1], WordsGroup::new);
				// the index keeps words aligned ("rû" is "ru"): spellings of one aligned word share its frequency,
				// and the class of the more frequent spelling
				int[] v = new int[] { Integer.parseInt(p[2]), Integer.parseInt(p[3]) };
				int[] old = g.words.get(SearchAlgorithms.alignChars(p[4]));
				if (old != null) {
					v = new int[] { old[1] >= v[1] ? old[0] : v[0], old[1] + v[1] };
				}
				g.words.put(SearchAlgorithms.alignChars(p[4]), v);
			}
		}
		if (index.groupsByCountry.isEmpty()) {
			// the data names no groups: the language groups of this class apply
			for (String[] g : DEFAULT_GROUPS) {
				WordsGroup group = index.groupsById.computeIfAbsent(g[0], WordsGroup::new);
				for (int i = 1; i < g.length; i++) {
					index.groupsByCountry.put(g[i], group);
				}
			}
		}
		return index;
	}

	/**
	 * @return group id for a map ("France_ile-de-france_europe_2.obf", "Belgium_flanders_europe") or null when no
	 * group covers it; the longest country prefix wins, so "belgium_flanders" is chosen before "belgium"
	 */
	public String getGroupId(String mapName) {
		WordsGroup g = getGroup(mapName);
		return g == null ? null : g.id;
	}

	private WordsGroup getGroup(String mapName) {
		if (mapName == null) {
			return null;
		}
		String name = mapName.toLowerCase(Locale.ROOT);
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		for (int end = name.length(); end > 0; end = name.lastIndexOf('_', end - 1)) {
			WordsGroup g = groupsByCountry.get(name.substring(0, end));
			if (g != null) {
				return g;
			}
		}
		return null;
	}

	/**
	 * @param words normalized words of one name (SearchAlgorithms.splitAndNormalize)
	 * @return the words to index, in their order; the same list when the map has no group
	 */
	public List<String> getWordsToIndex(String mapName, List<String> words) {
		return getWordsToIndex(mapName, words, false);
	}

	/**
	 * @param notable an object people know by any word of its name (a travel rating, a wikipedia article): "national"
	 * has to find Tongass National Forest, so every word stays a key
	 */
	public List<String> getWordsToIndex(String mapName, List<String> words, boolean notable) {
		WordsGroup g = notable ? null : getGroup(mapName);
		if (g == null || words.size() < 2) {
			return words;
		}
		int size = words.size();
		int[] cls = new int[size];
		int[] freq = new int[size];
		boolean[] number = new boolean[size];
		for (int i = 0; i < size; i++) {
			String w = words.get(i);
			// "cityasstreetcommon" marks a street that is a place: a word of the index itself, never a word of the name
			number[i] = SearchAlgorithms.isNumber2Letters(w) || NameIndexReader.CITY_AS_STREET_COMMON.equalsIgnoreCase(w);
			int[] v = number[i] ? null : g.words.get(SearchAlgorithms.alignChars(w));
			cls[i] = v == null ? KEEP : v[0];
			freq[i] = v == null ? 0 : v[1];
		}
		boolean[] drop = new boolean[size];
		boolean otherThanService = false;
		for (int i = 0; i < size; i++) {
			if (number[i] || cls[i] == SERVICE) {
				continue;
			}
			if (cls[i] == FREQUENT) {
				for (int j = 0; j < size; j++) {
					// strictly rarer: words of one frequency, zero included, never drop each other, so the rarest word stays
					if (j != i && !number[j] && freq[j] < freq[i] && (long) freq[j] * RARER_FACTOR <= freq[i]) {
						drop[i] = true;
						break;
					}
				}
			}
			otherThanService |= !drop[i];
		}
		List<String> result = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			boolean dropService = cls[i] == SERVICE && !number[i] && otherThanService;
			if (!drop[i] && !dropService) {
				result.add(words.get(i));
			}
		}
		return result;
	}
}
