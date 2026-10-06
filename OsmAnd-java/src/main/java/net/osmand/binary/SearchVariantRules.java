package net.osmand.binary;

import net.osmand.PlatformUtil;
import net.osmand.util.SearchAlgorithms;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Name variants and word classes shared by the OBF writer and spatial search (schema v5, see
 * {@code resources/abbr/rules-spec.md}).
 * <p>
 * A stored name keeps the words of OSM: no rule rewrites it. {@code <locales>} (only in {@code rules.xml}) is the one
 * table of map name prefixes, rules locales and statistics groups. Inside {@code <index>} the OBF writer gets the
 * classes of words ({@code <class0>} always a key, {@code <class1>} a service word, {@code <class2>} a frequent word),
 * which override {@code common_words_groups.tsv}, and the alternative names of objects: {@code <unglue>} splits glued
 * words, a {@code <rule>} is a regexp with one form, the attribute {@code to} ({@code Strada Statale 42} ->
 * {@code SS42}, {@code Hauptstraße} -> {@code Hauptstr}). Inside {@code <query>} the search gives a query word its forms: a word with
 * one meaning has the attributes {@code to} and {@code object} ({@code <rule from="pl" to="Place" object="street"/>}),
 * a word with several meanings lists them as {@code <to>} elements with their own {@code object}, the first is the
 * main one. Every form gets a reverse form of the same owner; a reverse form is one step. {@code object="building"}
 * without {@code to} marks the word as a part of a house number, {@code to=""} makes it an ignorable word of every
 * owner (an optional query token that never penalizes a name). {@code <skipPenalty>} lists the words of names that
 * do not penalize an object when the query does not have them, optionally for some owners only.
 * <p>
 * Rules are layered from general to specific: {@code rules.xml} (every map), {@code rules_<language>.xml}, then
 * {@code rules_<language>_<COUNTRY>.xml}. A rule of a lower layer with the key of an upper one replaces it whole,
 * {@code enabled="false"} removes it, and two rules with one key in one file are an error. The locale is the locale of
 * the data ({@link SearchLocales#forMap}, {@link SearchLocales#forName}), not the language of the user interface.
 */
public final class SearchVariantRules {
	public static final String ANY_OBJECT = "*";
	public static final String BUILDING_OBJECT = "building";
	public static final String VERSION = "5";
	public static final String BASE_FILE = "rules.xml";
	/** classes of words of {@code <index>}, the classes of {@code common_words_groups.tsv} */
	public static final int CLASS_ALWAYS = 0;
	public static final int CLASS_SERVICE = 1;
	public static final int CLASS_FREQUENT = 2;

	private static final Set<String> OWNERS = Set.of("street", "locality", "boundary", "postcode", "poi");
	private static final Set<String> INDEX_ATTRIBUTES = Set.of("from", "to", "object", "mode", "enabled", "keys");
	private static final Set<String> QUERY_ATTRIBUTES = Set.of("from", "to", "object", "enabled");
	private static final Set<String> TRANSLITS = Set.of("ja", "zh");
	private static final Map<String, SearchVariantRules> CACHE = new ConcurrentHashMap<>();
	private static final Pattern PLAIN_WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");
	private static final Pattern GROUP_REFERENCE = Pattern.compile("\\$(\\d+)");
	private static final Pattern GROUP_ID = Pattern.compile("[a-z]{2,4}");
	private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}");
	private static final Pattern MAP_PREFIX = Pattern.compile("[a-z0-9-]+(_[a-z0-9-]+)*");
	private static volatile LocaleTable localeTable;

	private final List<Rule> index;
	private final List<Unglue> unglues;
	// aligned word -> class of <class0>, <class1>, <class2>
	private final Map<String, Integer> classes;
	private final List<WordRule> query;
	// word -> owners of the <skipPenalty> entries of the word
	private final Map<String, List<String>> skipPenalty;
	// word -> its forms and then the reverse forms that lead to it
	private final Map<String, List<Form>> forms;
	private final Set<String> buildings;
	private final Set<String> ignorables;

	private SearchVariantRules(String locale, Map<String, Rule> index, Map<String, Unglue> unglues,
			Map<String, Integer> classes, Map<String, WordRule> query, Map<String, SkipPenalty> skipPenalty) {
		String where = " in the rules of locale '" + locale + "'";
		Set<String> buildings = new LinkedHashSet<>();
		Set<String> ignorables = new LinkedHashSet<>();
		Map<String, Set<Form>> forms = new LinkedHashMap<>();
		for (WordRule rule : query.values()) {
			for (Form form : rule.forms) {
				if (form.isBuilding()) {
					buildings.add(rule.word);
				} else if (form.isIgnorable()) {
					ignorables.add(rule.word);
				} else {
					forms.computeIfAbsent(rule.word, k -> new LinkedHashSet<>()).add(form);
				}
			}
		}
		for (WordRule rule : query.values()) {
			for (Form form : rule.forms()) {
				if (form.isBuilding() || form.isIgnorable()) {
					continue;
				}
				WordRule other = query.get(form.word());
				if (other != null && other.hasForm(rule.word)) {
					throw new IllegalArgumentException("The rule of '" + other.word + "' repeats the reverse form of '"
							+ rule.word + "' -> '" + form.word() + "': reverse forms are generated" + where);
				}
				if (buildings.contains(form.word()) || ignorables.contains(form.word())) {
					throw new IllegalArgumentException("The form '" + form.word() + "' of '" + rule.word
							+ "' is a house-number qualifier or an ignorable word" + where);
				}
				forms.computeIfAbsent(form.word(), k -> new LinkedHashSet<>()).add(new Form(form.object(), rule.word));
			}
		}
		Map<String, List<String>> skip = new LinkedHashMap<>();
		for (SkipPenalty entry : skipPenalty.values()) {
			if (buildings.contains(entry.word) || ignorables.contains(entry.word)) {
				throw new IllegalArgumentException("The <skipPenalty> word '" + entry.word + "' is a house-number "
						+ "qualifier or an ignorable word: an ignorable word never penalizes a name" + where);
			}
			Integer wordClass = classes.get(SearchAlgorithms.alignChars(entry.word));
			if (wordClass != null && wordClass != CLASS_ALWAYS) {
				throw new IllegalArgumentException("The <skipPenalty> word '" + entry.word + "' is a word of <class"
						+ wordClass + ">, which never penalizes a name" + where);
			}
			skip.computeIfAbsent(entry.word, k -> new ArrayList<>()).add(entry.object);
		}
		for (String word : ignorables) {
			Integer wordClass = classes.get(SearchAlgorithms.alignChars(word));
			if (wordClass != null && wordClass == CLASS_ALWAYS) {
				throw new IllegalArgumentException("The ignorable word '" + word + "' is a word of <class0>" + where);
			}
		}
		for (Rule rule : index.values()) {
			// an index pair repeated by the query would match one name twice: "Saint" -> "st" -> key "st" of "Street"
			String to = rule.to;
			if (!to.contains("$") && PLAIN_WORD.matcher(to).matches()) {
				String word = to.toLowerCase(Locale.ROOT);
				if (query.containsKey(word) || forms.containsKey(word)) {
					throw new IllegalArgumentException("The index form '" + to + "' of " + rule
							+ " is a word of <query> too: a pair belongs to one side" + where);
				}
			}
		}
		Map<String, List<Form>> lists = new LinkedHashMap<>();
		for (Map.Entry<String, Set<Form>> e : forms.entrySet()) {
			lists.put(e.getKey(), List.copyOf(e.getValue()));
		}
		Map<String, List<String>> skipLists = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> e : skip.entrySet()) {
			skipLists.put(e.getKey(), List.copyOf(e.getValue()));
		}
		this.index = List.copyOf(index.values());
		this.unglues = List.copyOf(unglues.values());
		this.classes = Collections.unmodifiableMap(new LinkedHashMap<>(classes));
		this.query = List.copyOf(query.values());
		this.skipPenalty = Collections.unmodifiableMap(skipLists);
		this.forms = Collections.unmodifiableMap(lists);
		this.buildings = Collections.unmodifiableSet(buildings);
		this.ignorables = Collections.unmodifiableSet(ignorables);
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public static SearchVariantRules forLocale(String locale) {
		return CACHE.computeIfAbsent(SearchLocales.normalize(locale), SearchVariantRules::load);
	}

	/** The table of {@code <locales>} of {@code rules.xml}: map name prefixes, rules locales and statistics groups. */
	public static LocaleTable localeTable() {
		LocaleTable table = localeTable;
		if (table == null) {
			synchronized (SearchVariantRules.class) {
				table = localeTable;
				if (table == null) {
					table = read(BASE_FILE, true).locales;
					if (table == null) {
						throw new IllegalStateException("Missing <locales> in " + BASE_FILE);
					}
					localeTable = table;
				}
			}
		}
		return table;
	}

	/** Rules that add an alternative name of an object to the name index. */
	public List<Rule> index() {
		return index;
	}

	/** Rules that split words glued by a character into an alternative name. */
	public List<Unglue> unglues() {
		return unglues;
	}

	/** Classes of words of {@code <index>}: aligned word -> 0 (always a key), 1 (service), 2 (frequent). */
	public Map<String, Integer> classes() {
		return classes;
	}

	/** @return class of a word of {@code <index>}, null when the rules do not set it */
	public Integer wordClass(String word) {
		return classes.get(SearchAlgorithms.alignChars(word.toLowerCase(Locale.ROOT)));
	}

	/** Rules of query words in the order of the files. */
	public List<WordRule> query() {
		return query;
	}

	/** Forms of a query word: the forms of its rule, then the reverse forms of the rules whose form it is. */
	public List<Form> forms(String word) {
		return forms.getOrDefault(word.toLowerCase(Locale.ROOT), List.of());
	}

	/** Words with a form or a reverse form. */
	public Set<String> formWords() {
		return forms.keySet();
	}

	/** Words ({@code object="building"}) that are a part of a house number. */
	public Set<String> buildings() {
		return buildings;
	}

	/** Words with an empty {@code to}: optional query words that never penalize a name (conjunctions, articles). */
	public Set<String> ignorables() {
		return ignorables;
	}

	/** Words of {@code <skipPenalty>} in lower case -> the owners they apply to. */
	public Map<String, List<String>> skipPenalty() {
		return skipPenalty;
	}

	/**
	 * @return the alternative name with the glued words of the name split ("L'Atelier d'Anaïs" -> "Atelier Anaïs"),
	 * null when no word is glued
	 */
	public String unglue(String name) {
		if (unglues.isEmpty() || name == null) {
			return null;
		}
		List<String> words = new ArrayList<>();
		boolean glued = false;
		for (String word : SearchAlgorithms.canonicalizePunctuation(name).split(" ")) {
			if (word.isEmpty()) {
				continue;
			}
			List<String> parts = unglueWord(word);
			if (parts == null) {
				words.add(word);
			} else {
				words.addAll(parts);
				glued = true;
			}
		}
		String unglued = String.join(" ", words).trim();
		return glued && !unglued.isEmpty() ? unglued : null;
	}

	private List<String> unglueWord(String word) {
		if (word.chars().anyMatch(Character::isDigit)) {
			return null;
		}
		StringBuilder glue = new StringBuilder();
		int minPart = Integer.MAX_VALUE;
		for (Unglue u : unglues) {
			if (word.indexOf(u.glue()) >= 0 && u.appliesToScript(word)) {
				glue.append(u.glue());
				minPart = Math.min(minPart, u.minPart());
			}
		}
		if (glue.length() == 0) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		boolean letterDropped = false;
		int start = 0;
		for (int i = 0; i <= word.length(); i++) {
			if (i == word.length() || glue.indexOf(String.valueOf(word.charAt(i))) >= 0) {
				String part = word.substring(start, i);
				if (part.length() >= minPart) {
					parts.add(part);
				} else if (!part.isEmpty()) {
					letterDropped = true;
				}
				start = i + 1;
			}
		}
		return parts.size() > 1 || letterDropped ? parts : null;
	}

	/** @return true when a rule of {@code object} ("street", "*", "street,poi") applies to the owner of a name */
	public static boolean appliesTo(String object, String owner) {
		for (String type : object.split(",")) {
			String t = type.trim();
			if (t.equals(owner) || t.equals(ANY_OBJECT)) {
				return true;
			}
		}
		return false;
	}

	private static boolean objectsOverlap(String a, String b) {
		if (a.equals(ANY_OBJECT) || b.equals(ANY_OBJECT)) {
			return true;
		}
		for (String type : a.split(",")) {
			if (appliesTo(b, type.trim())) {
				return true;
			}
		}
		return false;
	}

	private static SearchVariantRules load(String locale) {
		List<Layer> layers = new ArrayList<>();
		layers.add(read(BASE_FILE, true));
		if (!locale.isEmpty()) {
			String[] parts = locale.split("_");
			StringBuilder suffix = new StringBuilder();
			for (String part : parts) {
				suffix.append('_').append(part);
				Layer layer = read("rules" + suffix + ".xml", false);
				if (layer != null) {
					layers.add(layer);
				}
			}
		}
		return of(locale, layers);
	}

	static SearchVariantRules of(String locale, List<Layer> layers) {
		Map<String, Rule> index = new LinkedHashMap<>();
		Map<String, Unglue> unglues = new LinkedHashMap<>();
		Map<String, Integer> classes = new LinkedHashMap<>();
		Map<String, WordRule> query = new LinkedHashMap<>();
		Map<String, SkipPenalty> skipPenalty = new LinkedHashMap<>();
		for (Layer layer : layers) {
			disable(index, layer.disabledIndex, layer);
			disable(unglues, layer.disabledUnglues, layer);
			disable(classes, layer.disabledClasses, layer);
			disable(query, layer.disabledQuery, layer);
			disable(skipPenalty, layer.disabledSkipPenalty, layer);
			index.putAll(layer.index);
			unglues.putAll(layer.unglues);
			classes.putAll(layer.classes);
			query.putAll(layer.query);
			skipPenalty.putAll(layer.skipPenalty);
		}
		return new SearchVariantRules(locale, index, unglues, classes, query, skipPenalty);
	}

	private static void disable(Map<String, ?> rules, Map<String, String> disabled, Layer layer) {
		for (Map.Entry<String, String> e : disabled.entrySet()) {
			if (rules.remove(e.getKey()) == null) {
				throw new IllegalArgumentException("Cannot disable " + e.getValue() + " in " + layer.file
						+ ": no upper layer defines it");
			}
		}
	}

	private static Layer read(String file, boolean required) {
		try (InputStream input = SearchVariantRules.class.getResourceAsStream(file)) {
			if (input == null) {
				if (required) {
					throw new IllegalStateException("Missing search rules: " + file);
				}
				return null;
			}
			return parseLayer(input, file);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load search rules " + file, e);
		}
	}

	private record SkipPenalty(String word, String object) {
		String key() {
			return word + '\n' + object;
		}
	}

	/** The rules of one file. */
	static final class Layer {
		final String file;
		final Map<String, Rule> index = new LinkedHashMap<>();
		final Map<String, Unglue> unglues = new LinkedHashMap<>();
		final Map<String, Integer> classes = new LinkedHashMap<>();
		final Map<String, WordRule> query = new LinkedHashMap<>();
		final Map<String, SkipPenalty> skipPenalty = new LinkedHashMap<>();
		// key -> text of the rule for messages
		final Map<String, String> disabledIndex = new LinkedHashMap<>();
		final Map<String, String> disabledUnglues = new LinkedHashMap<>();
		final Map<String, String> disabledClasses = new LinkedHashMap<>();
		final Map<String, String> disabledQuery = new LinkedHashMap<>();
		final Map<String, String> disabledSkipPenalty = new LinkedHashMap<>();
		// only rules.xml
		LocaleTable locales;

		Layer(String file) {
			this.file = file;
		}

		void add(Rule rule, boolean enabled) {
			add(index, disabledIndex, rule.key(), rule, rule.toString(), enabled);
		}

		void add(Unglue unglue, boolean enabled) {
			add(unglues, disabledUnglues, String.valueOf(unglue.glue()), unglue, unglue.toString(), enabled);
		}

		void add(WordRule rule, boolean enabled) {
			add(query, disabledQuery, rule.word, rule, rule.toString(), enabled);
		}

		private <T> void add(Map<String, T> rules, Map<String, String> disabled, String key, T rule, String text,
				boolean enabled) {
			if (rules.containsKey(key) || disabled.containsKey(key)) {
				throw new IllegalArgumentException("Duplicate rule " + text + " in " + file);
			}
			if (enabled) {
				rules.put(key, rule);
			} else {
				disabled.put(key, text);
			}
		}

		void addClass(String tag, int wordClass, String words, boolean enabled) {
			for (String word : plainWords(words, "<" + tag + ">")) {
				String w = SearchAlgorithms.alignChars(word);
				if (classes.containsKey(w) || disabledClasses.containsKey(w)) {
					throw new IllegalArgumentException("The word '" + w + "' of <" + tag + "> is listed twice in the "
							+ "classes of " + file);
				}
				if (enabled) {
					classes.put(w, wordClass);
				} else {
					disabledClasses.put(w, "the class of '" + w + "'");
				}
			}
		}

		void addSkipPenalty(String object, String words, boolean enabled) {
			for (String word : plainWords(words, "<skipPenalty>")) {
				SkipPenalty entry = new SkipPenalty(word, object);
				for (SkipPenalty other : skipPenalty.values()) {
					if (other.word.equals(word) && objectsOverlap(other.object, object)) {
						throw new IllegalArgumentException("The <skipPenalty> word '" + word + "' is listed twice with "
								+ "overlapping object in " + file);
					}
				}
				add(skipPenalty, disabledSkipPenalty, entry.key(), entry,
						"<skipPenalty object=\"" + object + "\">" + word + "</skipPenalty>", enabled);
			}
		}

		private List<String> plainWords(String words, String what) {
			List<String> result = new ArrayList<>();
			for (String word : words.trim().split("\\s+")) {
				if (word.isEmpty()) {
					continue;
				}
				if (!PLAIN_WORD.matcher(word).matches()) {
					throw new IllegalArgumentException("A word of " + what + " is a plain word: '" + word + "' in "
							+ file);
				}
				String w = word.toLowerCase(Locale.ROOT);
				if (result.contains(w)) {
					throw new IllegalArgumentException("The word '" + w + "' of " + what + " is listed twice in " + file);
				}
				result.add(w);
			}
			return result;
		}
	}

	static Layer parseLayer(InputStream input, String file) throws Exception {
		Layer layer = new Layer(file);
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(input, "UTF-8");
		String section = null;
		LocaleTable.Builder locales = null;
		boolean root = false;
		int event;
		while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG) {
				String tag = parser.getName();
				int depth = parser.getDepth();
				if (depth == 1 && "rules".equals(tag)) {
					if (!VERSION.equals(parser.getAttributeValue(null, "version"))) {
						throw new IllegalArgumentException("Unsupported search rules version in " + file
								+ ", expected " + VERSION);
					}
					root = true;
				} else if (depth == 2 && ("index".equals(tag) || "query".equals(tag))) {
					section = tag;
				} else if (depth == 2 && "locales".equals(tag)) {
					if (!BASE_FILE.equals(file)) {
						throw new IllegalArgumentException("<locales> belongs to " + BASE_FILE + ", not to " + file);
					}
					if (locales != null) {
						throw new IllegalArgumentException("Duplicate <locales> in " + file);
					}
					checkAttributes(parser, Set.of(), "<locales>", " in " + file);
					locales = new LocaleTable.Builder(file);
					section = tag;
				} else if (depth == 2 && "common".equals(tag)) {
					throw new IllegalArgumentException("<common> is replaced by <skipPenalty> of <query> in " + file);
				} else if (depth == 2 && "rule".equals(tag)) {
					throw new IllegalArgumentException("A rule belongs to <index> or <query> (from=\""
							+ parser.getAttributeValue(null, "from") + "\" in " + file + ")");
				} else if (depth == 3 && "rule".equals(tag) && "index".equals(section)) {
					parseIndexRule(parser, layer, file);
				} else if (depth == 3 && "unglue".equals(tag) && "index".equals(section)) {
					parseUnglue(parser, layer, file);
				} else if (depth == 3 && tag.matches("class[012]") && "index".equals(section)) {
					checkAttributes(parser, Set.of("enabled"), "<" + tag + ">", " in " + file);
					boolean enabled = booleanAttribute(parser, "enabled", true, " (<" + tag + "> of " + file + ")");
					layer.addClass(tag, tag.charAt(5) - '0', parser.nextText(), enabled);
				} else if (depth == 3 && "rule".equals(tag) && "query".equals(section)) {
					parseQueryRule(parser, layer, file);
				} else if (depth == 3 && "skipPenalty".equals(tag) && "query".equals(section)) {
					parseSkipPenalty(parser, layer, file);
				} else if (depth == 3 && "group".equals(tag) && "locales".equals(section)) {
					locales.addGroup(parser);
				} else if (depth == 3 && "map".equals(tag) && "locales".equals(section)) {
					locales.addMap(parser);
				} else {
					throw new IllegalArgumentException("Unexpected search rule tag " + tag + " in " + file);
				}
			} else if (event == XmlPullParser.END_TAG && parser.getDepth() == 2) {
				section = null;
			}
		}
		if (!root) {
			throw new IllegalArgumentException("Missing rules root in " + file);
		}
		if (locales != null) {
			layer.locales = locales.build();
		}
		return layer;
	}

	private static void parseIndexRule(XmlPullParser parser, Layer layer, String file) throws Exception {
		String from = parser.getAttributeValue(null, "from");
		String where = where(from, "index", file);
		checkRemovedAttributes(parser, where);
		checkAttributes(parser, INDEX_ATTRIBUTES, "an index rule", where);
		if (from == null || from.isEmpty()) {
			throw new IllegalArgumentException("Missing from in " + file);
		}
		String object = parser.getAttributeValue(null, "object");
		object = object == null || object.isEmpty() ? ANY_OBJECT : object;
		validateObject(object, false, where);
		String mode = parser.getAttributeValue(null, "mode");
		String to = parser.getAttributeValue(null, "to");
		String keys = parser.getAttributeValue(null, "keys");
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		if (PLAIN_WORD.matcher(from).matches()) {
			throw new IllegalArgumentException("A plain word belongs to <query>: the index adds nothing that its "
					+ "query form does not" + where);
		}
		if (parser.nextTag() == XmlPullParser.START_TAG) {
			throw new IllegalArgumentException("An index rule has one form, the attribute to: keys of several meanings "
					+ "cannot be told apart in the OBF, the meanings belong to <query>" + where);
		}
		if (!enabled) {
			if (to != null || mode != null || keys != null) {
				throw new IllegalArgumentException("A disabled rule holds only from and object" + where);
			}
			layer.add(newRule(object, from, "", "Single", false, false, where), false);
			return;
		}
		if (to == null) {
			throw new IllegalArgumentException("Missing to" + where);
		}
		if (to.isEmpty()) {
			throw new IllegalArgumentException("An empty to (an ignorable word) belongs to <query>" + where);
		}
		if (keys != null && !Rule.KEYS_STATS.equals(keys) && !Rule.KEYS_ALWAYS.equals(keys)) {
			throw new IllegalArgumentException("Invalid keys '" + keys + "', expected " + Rule.KEYS_STATS + " or "
					+ Rule.KEYS_ALWAYS + where);
		}
		layer.add(newRule(object, from, to, mode == null ? "Single" : mode, Rule.KEYS_ALWAYS.equals(keys), true,
				where), true);
	}

	private static void parseUnglue(XmlPullParser parser, Layer layer, String file) throws Exception {
		String glue = parser.getAttributeValue(null, "glue");
		String where = " (<unglue glue=\"" + glue + "\"> of " + file + ")";
		checkAttributes(parser, Set.of("glue", "script", "minPart", "enabled"), "<unglue>", where);
		if (glue == null || glue.length() != 1 || Character.isLetterOrDigit(glue.charAt(0))
				|| Character.isWhitespace(glue.charAt(0))) {
			throw new IllegalArgumentException("The glue of <unglue> is one character, not a letter, a digit or a "
					+ "space" + where);
		}
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		String script = parser.getAttributeValue(null, "script");
		String minPart = parser.getAttributeValue(null, "minPart");
		if (parser.nextTag() == XmlPullParser.START_TAG) {
			throw new IllegalArgumentException("<unglue> has no elements" + where);
		}
		if (!enabled) {
			if (script != null || minPart != null) {
				throw new IllegalArgumentException("A disabled rule holds only glue" + where);
			}
			layer.add(new Unglue(glue.charAt(0), null, 0), false);
			return;
		}
		Character.UnicodeScript unicodeScript = null;
		if (script != null) {
			try {
				unicodeScript = Character.UnicodeScript.forName(script);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown script '" + script + "'" + where, e);
			}
		}
		int min = 2;
		if (minPart != null) {
			try {
				min = Integer.parseInt(minPart);
			} catch (NumberFormatException e) {
				min = 0;
			}
			if (min < 1) {
				throw new IllegalArgumentException("Invalid minPart '" + minPart + "', expected a number >= 1" + where);
			}
		}
		layer.add(new Unglue(glue.charAt(0), unicodeScript, min), true);
	}

	private static void parseSkipPenalty(XmlPullParser parser, Layer layer, String file) throws Exception {
		String where = " (<skipPenalty> of " + file + ")";
		checkAttributes(parser, Set.of("object", "enabled"), "<skipPenalty>", where);
		String object = parser.getAttributeValue(null, "object");
		object = object == null || object.isEmpty() ? ANY_OBJECT : object;
		if (BUILDING_OBJECT.equals(object)) {
			throw new IllegalArgumentException("A part of a house number is no word of a name: <skipPenalty> takes no "
					+ "object=\"building\"" + where);
		}
		validateObject(object, false, where);
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		layer.addSkipPenalty(object, parser.nextText(), enabled);
	}

	/**
	 * One form is the attributes of the rule ({@code to}, {@code object}; {@code object="building"} without {@code to}),
	 * several forms are {@code <to>} elements with their own {@code object}.
	 */
	private static void parseQueryRule(XmlPullParser parser, Layer layer, String file) throws Exception {
		String from = parser.getAttributeValue(null, "from");
		String where = where(from, "query", file);
		checkRemovedAttributes(parser, where);
		if (parser.getAttributeValue(null, "mode") != null) {
			throw new IllegalArgumentException("mode applies to a regexp of <index>" + where);
		}
		checkAttributes(parser, QUERY_ATTRIBUTES, "a query rule", where);
		if (from == null || from.isEmpty()) {
			throw new IllegalArgumentException("Missing from in " + file);
		}
		if (!PLAIN_WORD.matcher(from).matches()) {
			throw new IllegalArgumentException("A query rule needs a plain word without a dot, a regexp belongs to "
					+ "<index>" + where);
		}
		String to = parser.getAttributeValue(null, "to");
		String object = parser.getAttributeValue(null, "object");
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		String word = from.toLowerCase(Locale.ROOT);
		List<Form> forms = new ArrayList<>();
		while (parser.nextTag() == XmlPullParser.START_TAG) {
			if (!"to".equals(parser.getName())) {
				throw new IllegalArgumentException("Unexpected search rule tag " + parser.getName() + where);
			}
			checkAttributes(parser, Set.of("object"), "<to>", where);
			forms.add(parseForm(word, parser.getAttributeValue(null, "object"), parser.nextText().trim(), where));
		}
		if (!enabled) {
			if (to != null || object != null || !forms.isEmpty()) {
				throw new IllegalArgumentException("A disabled rule holds only from" + where);
			}
			layer.add(new WordRule(word, List.of()), false);
			return;
		}
		if (!forms.isEmpty()) {
			if (to != null) {
				throw new IllegalArgumentException("A rule has the attribute to or several <to>, not both" + where);
			}
			if (object != null) {
				throw new IllegalArgumentException("The object of a rule with several <to> is an attribute of each "
						+ "<to>" + where);
			}
			if (forms.size() == 1) {
				throw new IllegalArgumentException("One form is the attribute to (object=\"building\" for a "
						+ "house-number qualifier), <to> elements are for several" + where);
			}
		} else if (to != null) {
			if (BUILDING_OBJECT.equals(object)) {
				throw new IllegalArgumentException("A house-number qualifier (object=\"building\") has no form, it "
						+ "holds no to" + where);
			}
			forms.add(parseForm(word, object, to.trim(), where));
		} else if (BUILDING_OBJECT.equals(object)) {
			forms.add(new Form(BUILDING_OBJECT, ""));
		} else {
			throw new IllegalArgumentException("Missing to" + where);
		}
		validateForms(word, forms, where);
		layer.add(new WordRule(word, List.copyOf(forms)), true);
	}

	private static Form parseForm(String word, String object, String text, String where) {
		object = object == null || object.isEmpty() ? ANY_OBJECT : object;
		validateObject(object, true, where);
		boolean building = object.equals(BUILDING_OBJECT);
		if (building) {
			if (!text.isEmpty()) {
				throw new IllegalArgumentException("A house-number qualifier (object=\"building\") has no form" + where);
			}
			return new Form(object, "");
		}
		if (text.isEmpty()) {
			if (!object.equals(ANY_OBJECT)) {
				throw new IllegalArgumentException("An ignorable word (an empty to) applies to every object: a word that "
						+ "only does not penalize names of some owners is <skipPenalty object=\"" + object + "\">"
						+ where);
			}
			return new Form(object, "");
		}
		List<String> words = SearchAlgorithms.splitAndNormalize(text, false);
		if (words.size() != 1) {
			// a token is matched with one word of a name: several words would mean "or", not a phrase
			throw new IllegalArgumentException("A query form is one word, a phrase belongs to <index>" + where);
		}
		String form = words.get(0);
		if (form.equals(word)) {
			throw new IllegalArgumentException("The form '" + text + "' repeats the word" + where);
		}
		return new Form(object, form);
	}

	private static void validateForms(String word, List<Form> forms, String where) {
		int buildings = 0;
		int ignorables = 0;
		for (int i = 0; i < forms.size(); i++) {
			Form form = forms.get(i);
			if (form.isBuilding()) {
				buildings++;
				continue;
			}
			if (form.isIgnorable()) {
				ignorables++;
				continue;
			}
			// the reverse form of a one-letter word would match that letter in every name
			if (word.codePointCount(0, word.length()) == 1 && form.object().equals(ANY_OBJECT)) {
				throw new IllegalArgumentException("A one-letter word needs an object for its form '" + form.word()
						+ "': the reverse form would match every '" + word + "'" + where);
			}
			for (int j = 0; j < i; j++) {
				Form other = forms.get(j);
				if (other.word().equals(form.word()) && objectsOverlap(other.object(), form.object())) {
					throw new IllegalArgumentException("The form '" + form.word() + "' is listed twice" + where);
				}
			}
		}
		if (buildings > 1 || ignorables > 1) {
			throw new IllegalArgumentException("A house-number qualifier or an empty to is listed twice" + where);
		}
		if (buildings > 0 && ignorables > 0) {
			throw new IllegalArgumentException("The word is a house-number qualifier and an ignorable word" + where);
		}
		if (ignorables > 0 && forms.size() > 1) {
			// an ignorable word is an optional query word that never penalizes a name: its forms would mean nothing
			throw new IllegalArgumentException("An ignorable word has no forms" + where);
		}
	}

	private static String where(String from, String section, String file) {
		return " (from=\"" + from + "\" in <" + section + "> of " + file + ")";
	}

	private static void checkRemovedAttributes(XmlPullParser parser, String where) {
		if (parser.getAttributeValue(null, "replace") != null) {
			throw new IllegalArgumentException("replace is removed: a stored name keeps the words of OSM" + where);
		}
		if (parser.getAttributeValue(null, "common") != null) {
			throw new IllegalArgumentException("Common words are listed in <skipPenalty> of <query>" + where);
		}
	}

	private static void checkAttributes(XmlPullParser parser, Set<String> allowed, String what, String where) {
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			if (!allowed.contains(parser.getAttributeName(i))) {
				throw new IllegalArgumentException("Unknown attribute " + parser.getAttributeName(i) + " of " + what
						+ where);
			}
		}
	}

	private static Rule newRule(String object, String from, String to, String mode, boolean alwaysKeys,
			boolean enabled, String where) {
		try {
			return new Rule(object, from, to, mode, alwaysKeys, enabled);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + where, e);
		}
	}

	private static void validateObject(String object, boolean query, String where) {
		if (object.equals(BUILDING_OBJECT)) {
			if (!query) {
				throw new IllegalArgumentException("A house-number qualifier belongs to <query>" + where);
			}
			return;
		}
		boolean any = false;
		String[] types = object.split(",");
		for (String type : types) {
			String t = type.trim();
			any |= t.equals(ANY_OBJECT);
			if (!t.equals(ANY_OBJECT) && !OWNERS.contains(t)) {
				throw new IllegalArgumentException("Unknown object '" + t + "', expected one of " + OWNERS + ", '*' or "
						+ "'building'" + where);
			}
		}
		if (any && types.length > 1) {
			throw new IllegalArgumentException("'*' is every object, it is not listed with others" + where);
		}
	}

	private static boolean booleanAttribute(XmlPullParser parser, String key, boolean defaultValue, String where) {
		String value = parser.getAttributeValue(null, key);
		if (value == null) {
			return defaultValue;
		}
		if ("true".equals(value) || "false".equals(value)) {
			return "true".equals(value);
		}
		throw new IllegalArgumentException("Invalid " + key + " '" + value + "', expected true or false" + where);
	}

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to. An empty word is
	 * a house-number qualifier ({@code object="building"}) or an ignorable word.
	 */
	public record Form(String object, String word) {
		public boolean isBuilding() {
			return object.equals(BUILDING_OBJECT);
		}

		public boolean isIgnorable() {
			return word.isEmpty() && !isBuilding();
		}

		public boolean appliesTo(String owner) {
			return SearchVariantRules.appliesTo(object, owner);
		}
	}

	/** The rule of a query word: every meaning of the word, the first is the main one. */
	public record WordRule(String word, List<Form> forms) {
		boolean hasForm(String form) {
			for (Form f : forms) {
				if (f.word().equals(form)) {
					return true;
				}
			}
			return false;
		}

		/** the first form with a word, null for a house-number qualifier or an ignorable word */
		public Form mainForm() {
			for (Form f : forms) {
				if (!f.word().isEmpty()) {
					return f;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return "<rule from=\"" + word + "\">";
		}
	}

	/** A rule of {@code <unglue>}: words glued by a character, for words of a script only when {@code script} is set. */
	public record Unglue(char glue, Character.UnicodeScript script, int minPart) {
		boolean appliesToScript(String word) {
			return script == null || word.codePoints().filter(Character::isLetter)
					.allMatch(c -> Character.UnicodeScript.of(c) == script);
		}

		/** the key of the rule in the statistics of the OBF writer */
		public String name() {
			return "unglue " + glue;
		}

		@Override
		public String toString() {
			return "<unglue glue=\"" + glue + "\">";
		}
	}

	/** A rule of {@code <index>}: a regexp of names of an owner and the alternative name it adds. */
	public static final class Rule {
		static final String KEYS_STATS = "stats";
		static final String KEYS_ALWAYS = "always";

		public final String object;
		private final boolean all;
		private final boolean alwaysKeys;
		private final Pattern from;
		private final String fromText;
		private final String to;

		private Rule(String object, String from, String to, String mode, boolean alwaysKeys, boolean enabled) {
			this.object = object;
			if (!"All".equals(mode) && !"Single".equals(mode)) {
				throw new IllegalArgumentException("Invalid mode '" + mode + "' of " + from + ", expected All or Single");
			}
			this.all = "All".equals(mode);
			this.alwaysKeys = alwaysKeys;
			this.fromText = from;
			this.to = to;
			try {
				this.from = Pattern.compile(from);
			} catch (PatternSyntaxException e) {
				throw new IllegalArgumentException("Invalid regexp '" + from + "': " + e.getDescription(), e);
			}
			if (enabled) {
				checkRegexp(from, to);
			}
		}

		private void checkRegexp(String from, String to) {
			Matcher empty = this.from.matcher("");
			if (empty.find()) {
				throw new IllegalArgumentException("The regexp '" + from + "' matches an empty text");
			}
			Matcher references = GROUP_REFERENCE.matcher(to);
			while (references.find()) {
				if (Integer.parseInt(references.group(1)) > empty.groupCount()) {
					throw new IllegalArgumentException("The target '" + to + "' refers to a group that the regexp '"
							+ from + "' does not have");
				}
			}
		}

		/** the key of an index rule in the layers: object and from */
		String key() {
			return object + '\n' + fromText;
		}

		/** the regexp of names */
		public String from() {
			return fromText;
		}

		/** the alternative name ($1, etc. for groups of the regexp) */
		public String to() {
			return to;
		}

		/** true ({@code keys="always"}): the new words of the alternative name are keys without the statistics */
		public boolean alwaysKeys() {
			return alwaysKeys;
		}

		/** the key of the rule in the statistics of the OBF writer */
		public String name() {
			return object + " " + to.trim();
		}

		public boolean appliesTo(String owner) {
			return SearchVariantRules.appliesTo(object, owner);
		}

		/** Returns null when the expression does not apply or does not change the text. */
		public String apply(String text) {
			Matcher matcher = from.matcher(text);
			if (!matcher.find()) {
				return null;
			}
			if (!all && matcher.find()) {
				return null;
			}
			matcher.reset();
			String result = all ? matcher.replaceAll(to) : matcher.replaceFirst(to);
			return result.equals(text) ? null : result;
		}

		@Override
		public String toString() {
			return "<rule object=\"" + object + "\" from=\"" + fromText + "\">";
		}
	}

	/**
	 * {@code <locales>} of {@code rules.xml}: map name prefix (lower case) -> rules locale, statistics group of
	 * {@code common_words_groups.tsv} and transliteration of names.
	 */
	public static final class LocaleTable {
		private final Map<String, String> localeByPrefix;
		private final Map<String, String> groupByPrefix;
		private final Map<String, String> translitByPrefix;

		private LocaleTable(Map<String, String> localeByPrefix, Map<String, String> groupByPrefix,
				Map<String, String> translitByPrefix) {
			this.localeByPrefix = Collections.unmodifiableMap(localeByPrefix);
			this.groupByPrefix = Collections.unmodifiableMap(groupByPrefix);
			this.translitByPrefix = Collections.unmodifiableMap(translitByPrefix);
		}

		/** @return true when the lower-case prefix of a map name is in the table */
		public boolean hasPrefix(String prefix) {
			return localeByPrefix.containsKey(prefix);
		}

		/** @return rules locale of a lower-case prefix, "" for a map without one, null when the table lacks it */
		public String locale(String prefix) {
			return localeByPrefix.get(prefix);
		}

		/** @return statistics group of a lower-case prefix, null when none covers it */
		public String group(String prefix) {
			return groupByPrefix.get(prefix);
		}

		/** @return transliteration of names ("ja", "zh") of a lower-case prefix, null when none */
		public String translit(String prefix) {
			return translitByPrefix.get(prefix);
		}

		/** map name prefix -> statistics group, for every prefix with a group */
		public Map<String, String> groups() {
			return groupByPrefix;
		}

		/** map name prefix -> transliteration, for every prefix with one */
		public Map<String, String> translits() {
			return translitByPrefix;
		}

		static final class Builder {
			private final String file;
			private final Map<String, String> groupByLanguage = new LinkedHashMap<>();
			private final Map<String, String> groupByLocale = new LinkedHashMap<>();
			private final Set<String> groups = new LinkedHashSet<>();
			private final Map<String, String> localeByPrefix = new LinkedHashMap<>();
			private final Map<String, String> explicitGroup = new LinkedHashMap<>();
			private final Map<String, String> translitByPrefix = new LinkedHashMap<>();

			Builder(String file) {
				this.file = file;
			}

			void addGroup(XmlPullParser parser) throws Exception {
				String id = parser.getAttributeValue(null, "id");
				String where = " (<group id=\"" + id + "\"> of " + file + ")";
				checkAttributes(parser, Set.of("id", "languages", "locales"), "<group>", where);
				if (id == null || !GROUP_ID.matcher(id).matches()) {
					throw new IllegalArgumentException("A group id is 2-4 lower-case letters" + where);
				}
				if (!groups.add(id)) {
					throw new IllegalArgumentException("Duplicate group" + where);
				}
				for (String language : list(parser.getAttributeValue(null, "languages"))) {
					if (!LANGUAGE.matcher(language).matches()) {
						throw new IllegalArgumentException("Invalid language '" + language + "'" + where);
					}
					if (groupByLanguage.put(language, id) != null) {
						throw new IllegalArgumentException("The language '" + language + "' is in two groups" + where);
					}
				}
				for (String locale : list(parser.getAttributeValue(null, "locales"))) {
					String normalized = SearchLocales.normalize(locale);
					if (normalized.isEmpty() || SearchLocales.country(normalized).isEmpty()) {
						throw new IllegalArgumentException("Invalid locale '" + locale + "', expected a language and a "
								+ "country" + where);
					}
					if (groupByLocale.put(normalized, id) != null) {
						throw new IllegalArgumentException("The locale '" + locale + "' is in two groups" + where);
					}
				}
				endElement(parser, where);
			}

			void addMap(XmlPullParser parser) throws Exception {
				String locale = parser.getAttributeValue(null, "locale");
				String where = " (<map locale=\"" + locale + "\"> of " + file + ")";
				checkAttributes(parser, Set.of("locale", "prefixes", "group", "translit"), "<map>", where);
				if (locale == null) {
					throw new IllegalArgumentException("Missing locale" + where);
				}
				String normalized = SearchLocales.normalize(locale);
				if (!locale.isEmpty() && normalized.isEmpty()) {
					throw new IllegalArgumentException("Invalid locale '" + locale + "'" + where);
				}
				String group = parser.getAttributeValue(null, "group");
				String translit = parser.getAttributeValue(null, "translit");
				if (translit != null && !TRANSLITS.contains(translit)) {
					throw new IllegalArgumentException("Unknown translit '" + translit + "', expected one of " + TRANSLITS
							+ where);
				}
				List<String> prefixes = list(parser.getAttributeValue(null, "prefixes"));
				if (prefixes.isEmpty()) {
					throw new IllegalArgumentException("Missing prefixes" + where);
				}
				for (String prefix : prefixes) {
					if (!MAP_PREFIX.matcher(prefix).matches()) {
						throw new IllegalArgumentException("A map prefix is lower case: '" + prefix + "'" + where);
					}
					if (localeByPrefix.put(prefix, normalized) != null) {
						throw new IllegalArgumentException("Duplicate map prefix '" + prefix + "'" + where);
					}
					if (group != null) {
						explicitGroup.put(prefix, group);
					}
					if (translit != null) {
						translitByPrefix.put(prefix, translit);
					}
				}
				endElement(parser, where);
			}

			LocaleTable build() {
				Map<String, String> groupByPrefix = new LinkedHashMap<>();
				for (Map.Entry<String, String> e : localeByPrefix.entrySet()) {
					String group = explicitGroup.get(e.getKey());
					if (group != null && !groups.contains(group)) {
						throw new IllegalArgumentException("Unknown group '" + group + "' of the map prefix '"
								+ e.getKey() + "' in " + file);
					}
					if (group == null) {
						group = groupByLocale.get(e.getValue());
					}
					if (group == null) {
						group = groupByLanguage.get(SearchLocales.language(e.getValue()));
					}
					if (group != null) {
						groupByPrefix.put(e.getKey(), group);
					}
				}
				return new LocaleTable(localeByPrefix, groupByPrefix, translitByPrefix);
			}

			private static List<String> list(String value) {
				List<String> result = new ArrayList<>();
				if (value != null) {
					for (String s : value.trim().split("\\s+")) {
						if (!s.isEmpty()) {
							result.add(s);
						}
					}
				}
				return result;
			}

			private static void endElement(XmlPullParser parser, String where) throws Exception {
				if (parser.nextTag() == XmlPullParser.START_TAG) {
					throw new IllegalArgumentException("Unexpected element " + parser.getName() + where);
				}
			}
		}
	}
}
