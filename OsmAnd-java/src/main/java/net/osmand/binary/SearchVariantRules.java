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
 * Name variants shared by the OBF writer and spatial search.
 * <p>
 * A stored name keeps the words of OSM: no rule rewrites it. Inside {@code <index>} the OBF writer adds an
 * alternative name to the names of objects: a phrase, a glued or a tokenizer-lost form that a query word cannot
 * produce ({@code Strada Statale 42} -> {@code SS42}, {@code Hauptstraße} -> {@code Hauptstr}); such a rule is a regexp
 * with one form, the attribute {@code to}. Inside {@code <query>} the search gives a query word its forms: a word with
 * one meaning has the attributes {@code to} and {@code object} ({@code <rule from="pl" to="Place" object="street"/>}),
 * a word with several meanings lists them as {@code <to>} elements with their own {@code object}, the first is the
 * main one ({@code st}: Street of a street, Saint of any name). Every form gets a reverse form ({@code street} ->
 * {@code st}, {@code saint} -> {@code st}) of the same owner; a reverse form is one step, it never takes the forms of
 * the word it leads to. {@code object="building"} without {@code to} marks the word as a part of a house number,
 * {@code to=""} makes it ignorable (an article, a conjunction). {@code <common>} lists the words that do not count as
 * unmatched words of a name.
 * <p>
 * Rules are layered from general to specific: {@code rules.xml} (every map), {@code rules_<language>.xml}, then
 * {@code rules_<language>_<COUNTRY>.xml}. The key of a query rule is its word ({@code from}, ignoring case), the key of
 * an index rule is {@code object} and {@code from}: a rule of a lower layer with the key of an upper one replaces it
 * whole, {@code enabled="false"} removes it, and two rules with one key in one file are an error. The locale is the
 * locale of the data ({@link SearchLocales#forMap}, {@link SearchLocales#forName}), not the language of the user
 * interface.
 */
public final class SearchVariantRules {
	public static final String ANY_OBJECT = "*";
	public static final String BUILDING_OBJECT = "building";
	public static final String VERSION = "4";

	private static final Set<String> OWNERS = Set.of("street", "locality", "boundary", "postcode", "poi");
	private static final Set<String> INDEX_ATTRIBUTES = Set.of("from", "to", "object", "mode", "enabled");
	private static final Set<String> QUERY_ATTRIBUTES = Set.of("from", "to", "object", "enabled");
	private static final Map<String, SearchVariantRules> CACHE = new ConcurrentHashMap<>();
	private static final Pattern PLAIN_WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");
	private static final Pattern GROUP_REFERENCE = Pattern.compile("\\$(\\d+)");

	private final List<Rule> index;
	private final List<WordRule> query;
	private final Set<String> common;
	// word -> its forms and then the reverse forms that lead to it
	private final Map<String, List<Form>> forms;
	private final Set<String> buildings;
	private final Set<String> ignorables;

	private SearchVariantRules(String locale, Map<String, Rule> index, Map<String, WordRule> query, Set<String> common) {
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
		for (String word : common) {
			if (buildings.contains(word) || ignorables.contains(word)) {
				throw new IllegalArgumentException("The common word '" + word
						+ "' is a house-number qualifier or an ignorable word" + where);
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
		this.index = List.copyOf(index.values());
		this.query = List.copyOf(query.values());
		this.common = Collections.unmodifiableSet(new LinkedHashSet<>(common));
		this.forms = Collections.unmodifiableMap(lists);
		this.buildings = Collections.unmodifiableSet(buildings);
		this.ignorables = Collections.unmodifiableSet(ignorables);
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public static SearchVariantRules forLocale(String locale) {
		return CACHE.computeIfAbsent(SearchLocales.normalize(locale), SearchVariantRules::load);
	}

	/** Rules that add an alternative name of an object to the name index. */
	public List<Rule> index() {
		return index;
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

	/** Words with an empty {@code to}: not a part of a name (conjunctions, articles). */
	public Set<String> ignorables() {
		return ignorables;
	}

	/** Words of {@code <common>} in lower case. */
	public Set<String> common() {
		return common;
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
		layers.add(read("rules.xml", true));
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
		Map<String, WordRule> query = new LinkedHashMap<>();
		Set<String> common = new LinkedHashSet<>();
		for (Layer layer : layers) {
			disable(index, layer.disabledIndex, layer);
			disable(query, layer.disabledQuery, layer);
			for (String word : layer.uncommon) {
				if (!common.remove(word)) {
					throw new IllegalArgumentException("Cannot remove the common word '" + word + "' in " + layer.file
							+ ": no upper layer lists it");
				}
			}
			index.putAll(layer.index);
			query.putAll(layer.query);
			common.addAll(layer.common);
		}
		return new SearchVariantRules(locale, index, query, common);
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

	/** The rules of one file. */
	static final class Layer {
		final String file;
		final Map<String, Rule> index = new LinkedHashMap<>();
		final Map<String, WordRule> query = new LinkedHashMap<>();
		final Set<String> common = new LinkedHashSet<>();
		final Set<String> uncommon = new LinkedHashSet<>();
		// key -> text of the rule for messages
		final Map<String, String> disabledIndex = new LinkedHashMap<>();
		final Map<String, String> disabledQuery = new LinkedHashMap<>();

		Layer(String file) {
			this.file = file;
		}

		void add(Rule rule, boolean enabled) {
			add(index, disabledIndex, rule.key(), rule, rule.toString(), enabled);
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

		void addCommon(String words, boolean enabled) {
			for (String word : words.trim().split("\\s+")) {
				if (word.isEmpty()) {
					continue;
				}
				if (!PLAIN_WORD.matcher(word).matches()) {
					throw new IllegalArgumentException("A common word is a plain word: '" + word + "' in " + file);
				}
				String w = word.toLowerCase(Locale.ROOT);
				if (common.contains(w) || uncommon.contains(w)) {
					throw new IllegalArgumentException("The common word '" + w + "' is listed twice in " + file);
				}
				(enabled ? common : uncommon).add(w);
			}
		}
	}

	static Layer parseLayer(InputStream input, String file) throws Exception {
		Layer layer = new Layer(file);
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(input, "UTF-8");
		String section = null;
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
				} else if (depth == 2 && "common".equals(tag)) {
					checkAttributes(parser, Set.of("enabled"), "<common>", file);
					boolean enabled = booleanAttribute(parser, "enabled", true, " (<common> of " + file + ")");
					layer.addCommon(parser.nextText(), enabled);
				} else if (depth == 2 && "rule".equals(tag)) {
					throw new IllegalArgumentException("A rule belongs to <index> or <query> (from=\""
							+ parser.getAttributeValue(null, "from") + "\" in " + file + ")");
				} else if (depth == 3 && "rule".equals(tag) && "index".equals(section)) {
					parseIndexRule(parser, layer, file);
				} else if (depth == 3 && "rule".equals(tag) && "query".equals(section)) {
					parseQueryRule(parser, layer, file);
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
			if (to != null || mode != null) {
				throw new IllegalArgumentException("A disabled rule holds only from and object" + where);
			}
			layer.add(newRule(object, from, "", "Single", false, where), false);
			return;
		}
		if (to == null) {
			throw new IllegalArgumentException("Missing to" + where);
		}
		if (to.isEmpty()) {
			throw new IllegalArgumentException("An empty to (an ignorable word) belongs to <query>" + where);
		}
		layer.add(newRule(object, from, to, mode == null ? "Single" : mode, true, where), true);
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
				throw new IllegalArgumentException("An ignorable word (an empty to) applies to every object" + where);
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
			// an ignorable word is dropped from names (removeCommonWords): its forms would never match
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
			throw new IllegalArgumentException("Common words are listed in <common>" + where);
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

	private static Rule newRule(String object, String from, String to, String mode, boolean enabled, String where) {
		try {
			return new Rule(object, from, to, mode, enabled);
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

	/** A rule of {@code <index>}: a regexp of names of an owner and the alternative name it adds. */
	public static final class Rule {
		public final String object;
		private final boolean all;
		private final Pattern from;
		private final String fromText;
		private final String to;

		private Rule(String object, String from, String to, String mode, boolean enabled) {
			this.object = object;
			if (!"All".equals(mode) && !"Single".equals(mode)) {
				throw new IllegalArgumentException("Invalid mode '" + mode + "' of " + from + ", expected All or Single");
			}
			this.all = "All".equals(mode);
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
}
