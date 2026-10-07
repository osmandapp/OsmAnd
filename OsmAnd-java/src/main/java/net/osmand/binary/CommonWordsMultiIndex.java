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
import java.util.Set;

import net.osmand.util.SearchAlgorithms;

/**
 * Which words of a name become keys of the name index, decided by the statistics group of the map and the classes of
 * words of the rules of its locale.
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
 * The class of a word is the class of {@code <class0>}/{@code <class1>}/{@code <class2>} of {@code <index>} of the rules
 * of the map locale ({@link SearchVariantRules#wordClass}), else the class of the group; a word of {@code <class0>} is
 * always a key. The group of a map is {@link SearchLocales#groupForMap}: the longest prefix of {@code <locales>}
 * of {@code rules.xml} decides, a map whose prefix has no group has none, whatever a shorter prefix has. Words:
 * {@code common_words_groups.tsv} next to this class, lines {@code word <group> <class 0|1|2> <names per million>
 * <word>}; every group of the file is a group of {@code <locales>}. A word of class 0 is only a frequency to compare
 * with; a word the file does not have counts as rare. Test data with lines {@code group <id> <prefix,prefix...>}
 * replaces the groups of the rules.
 */
public class CommonWordsMultiIndex {

	public static final int RARER_FACTOR = 10;
	public static final String RESOURCE = "common_words_groups.tsv";

	private static final int KEEP = SearchVariantRules.CLASS_ALWAYS;
	private static final int SERVICE = SearchVariantRules.CLASS_SERVICE;
	private static final int FREQUENT = SearchVariantRules.CLASS_FREQUENT;

	/** Why a word of a name is or is not a key of the name index. */
	public enum KeyOutcome {
		// a key by the statistics
		KEPT(true),
		// a key: the object is notable, the statistics do not apply
		NOTABLE(true),
		// a key: <class0> of the rules
		ALWAYS(true),
		// a number or a marker of the index: the statistics do not apply, the writer decides
		NUMBER(true),
		// not a key: class 1 and the name has a word outside class 1
		DROPPED_CLASS1(false),
		// not a key: class 2 and the name has a word at least RARER_FACTOR times rarer
		DROPPED_CLASS2(false);

		public final boolean key;

		KeyOutcome(boolean key) {
			this.key = key;
		}
	}

	private static CommonWordsMultiIndex instance;

	private final Map<String, WordsGroup> groupsById = new HashMap<>();
	// test data only (lines "group"): map name prefix -> group, instead of <locales> of the rules
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
			// the data names no groups: the groups of <locales> of rules.xml apply
			Set<String> declared = SearchLocales.groupIds();
			for (String id : index.groupsById.keySet()) {
				if (!declared.contains(id)) {
					throw new IllegalStateException("The group '" + id + "' of " + RESOURCE + " is not a <group> of "
							+ "<locales> of rules.xml");
				}
			}
			for (String id : declared) {
				// a group without statistics still drops the words of the classes of the rules
				index.groupsById.computeIfAbsent(id, WordsGroup::new);
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
		if (groupsByCountry.isEmpty()) {
			String id = SearchLocales.groupForMap(mapName);
			return id == null ? null : groupsById.get(id);
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
		KeyOutcome[] outcomes = selectKeys(mapName, words, notable);
		if (outcomes == null) {
			return words;
		}
		List<String> result = new ArrayList<>(words.size());
		for (int i = 0; i < words.size(); i++) {
			if (outcomes[i].key) {
				result.add(words.get(i));
			}
		}
		return result;
	}

	/**
	 * @return class and source of a word for the map ("1 tsv", "0 rules"), null when the map has no group or the word
	 * has no class (a word the statistics do not have is rare)
	 */
	public String wordClass(String mapName, String word) {
		WordsGroup g = getGroup(mapName);
		if (g == null || SearchAlgorithms.isNumber2Letters(word) || NameIndexReader.isIndexMarker(word)) {
			return null;
		}
		String aligned = SearchAlgorithms.alignChars(word);
		Integer ruleClass = SearchVariantRules.forLocale(SearchLocales.forMap(mapName)).classes().get(aligned);
		if (ruleClass != null) {
			return ruleClass + " rules";
		}
		int[] v = g.words.get(aligned);
		return v == null || v[0] == KEEP ? null : v[0] + " tsv";
	}

	/**
	 * @param words normalized words of one name (SearchAlgorithms.splitAndNormalize)
	 * @return the outcome of every word, in their order; null when the map has no group (every word is a key)
	 */
	public KeyOutcome[] selectKeys(String mapName, List<String> words, boolean notable) {
		WordsGroup g = getGroup(mapName);
		if (g == null) {
			return null;
		}
		int size = words.size();
		KeyOutcome[] outcomes = new KeyOutcome[size];
		if (notable || size < 2) {
			for (int i = 0; i < size; i++) {
				outcomes[i] = notable ? KeyOutcome.NOTABLE : KeyOutcome.KEPT;
			}
			return outcomes;
		}
		SearchVariantRules rules = SearchVariantRules.forLocale(SearchLocales.forMap(mapName));
		int[] cls = new int[size];
		int[] freq = new int[size];
		boolean[] number = new boolean[size];
		boolean[] always = new boolean[size];
		for (int i = 0; i < size; i++) {
			String w = words.get(i);
			// "cityasstreetcommon" marks a street that is a place: a word of the index itself, never a word of the name
			number[i] = SearchAlgorithms.isNumber2Letters(w) || NameIndexReader.isIndexMarker(w);
			String aligned = number[i] ? null : SearchAlgorithms.alignChars(w);
			int[] v = aligned == null ? null : g.words.get(aligned);
			Integer ruleClass = aligned == null ? null : rules.classes().get(aligned);
			cls[i] = ruleClass != null ? ruleClass : v == null ? KEEP : v[0];
			always[i] = ruleClass != null && ruleClass == KEEP;
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
		for (int i = 0; i < size; i++) {
			if (number[i]) {
				outcomes[i] = KeyOutcome.NUMBER;
			} else if (always[i]) {
				outcomes[i] = KeyOutcome.ALWAYS;
			} else if (drop[i]) {
				outcomes[i] = KeyOutcome.DROPPED_CLASS2;
			} else if (cls[i] == SERVICE && otherThanService) {
				outcomes[i] = KeyOutcome.DROPPED_CLASS1;
			} else {
				outcomes[i] = KeyOutcome.KEPT;
			}
		}
		return outcomes;
	}
}
