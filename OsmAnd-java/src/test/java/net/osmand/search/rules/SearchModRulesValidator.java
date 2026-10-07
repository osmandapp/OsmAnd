package net.osmand.search.rules;

import static net.osmand.search.rules.SearchModLocaleRules.ANY_OBJECT;
import static net.osmand.search.rules.SearchModLocaleRules.BASE_FILE;
import static net.osmand.search.rules.SearchModLocaleRules.BUILDING_OBJECT;
import static net.osmand.search.rules.SearchModLocaleRules.CLASS_ALWAYS;
import static net.osmand.search.rules.SearchModLocaleRules.VERSION;

import net.osmand.PlatformUtil;
import net.osmand.search.rules.SearchModLocaleRules.Form;
import net.osmand.search.rules.SearchModLocaleRules.Mirror;
import net.osmand.search.rules.SearchModLocaleRules.Rule;
import net.osmand.search.rules.SearchModRules.SearchModRuleOwner;
import net.osmand.search.rules.SearchModLocaleRules.SkipPenalty;
import net.osmand.search.rules.SearchModLocaleRules.Unglue;
import net.osmand.search.rules.SearchModLocaleRules.WordRule;
import net.osmand.util.SearchAlgorithms;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Checks of the rules files for their authors, run by the unit tests only: the search reads the files without checks
 * ({@link SearchModRulesParser}). {@link #check} reads one file strictly: structure, attributes, values, duplicates;
 * {@link #checkLayers} and {@link #checkConsistency} check the rules of one locale together.
 */
final class SearchModRulesValidator {

	private static final Set<String> INDEX_ATTRIBUTES = Set.of("from", "to", "object", "mode", "enabled", "keys");
	private static final Set<String> QUERY_ATTRIBUTES = Set.of("from", "to", "object", "enabled");

	private final String file;
	private final Layer layer;
	// the table that <locales> of rules.xml fills, null for a file without it
	private final SearchModLocales locales;

	SearchModRulesValidator(String file, SearchModLocales locales) {
		this.file = file;
		this.layer = new Layer(file);
		this.locales = locales;
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
			add(query, disabledQuery, rule.word(), rule, rule.toString(), enabled);
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
			for (String w : plainWords(words, "<skipPenalty>")) {
				// a word of a name is looked up aligned, as the words of the classes ("straße" is "strasse")
				String word = SearchAlgorithms.alignChars(w);
				SkipPenalty entry = new SkipPenalty(word, object);
				for (SkipPenalty other : skipPenalty.values()) {
					if (other.word().equals(word) && other.overlaps(entry)) {
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
				String w = word.toLowerCase(Locale.ROOT);
				if (result.contains(w)) {
					throw new IllegalArgumentException("The word '" + w + "' of " + what + " is listed twice in " + file);
				}
				result.add(w);
			}
			return result;
		}
	}

	/** @throws IllegalArgumentException the first error of the file */
	void check(InputStream input) throws Exception {
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(input, "UTF-8");
		String section = null;
		boolean localesRead = false;
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
					if (localesRead) {
						throw new IllegalArgumentException("Duplicate <locales> in " + file);
					}
					checkAttributes(parser, Set.of(), "<locales>", " in " + file);
					localesRead = true;
					section = tag;
				} else if (depth == 2 && "common".equals(tag)) {
					throw new IllegalArgumentException("<common> is replaced by <skipPenalty> of <query> in " + file);
				} else if (depth == 2 && "rule".equals(tag)) {
					parseMirrorRule(parser);
				} else if (depth == 3 && "rule".equals(tag) && "index".equals(section)) {
					parseIndexRule(parser);
				} else if (depth == 3 && "unglue".equals(tag) && "index".equals(section)) {
					parseUnglue(parser);
				} else if (depth == 3 && tag.matches("class[012]") && "index".equals(section)) {
					checkAttributes(parser, Set.of("enabled"), "<" + tag + ">", " in " + file);
					boolean enabled = booleanAttribute(parser, "enabled", true, " (<" + tag + "> of " + file + ")");
					layer.addClass(tag, tag.charAt(5) - '0', parser.nextText(), enabled);
				} else if (depth == 3 && "rule".equals(tag) && "query".equals(section)) {
					parseQueryRule(parser);
				} else if (depth == 3 && "skipPenalty".equals(tag) && "query".equals(section)) {
					parseSkipPenalty(parser);
				} else if (depth == 3 && "group".equals(tag) && "locales".equals(section)) {
					parseGroup(parser);
				} else if (depth == 3 && "map".equals(tag) && "locales".equals(section)) {
					parseMap(parser);
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
		if (localesRead) {
			locales.build();
		}
	}

	private void parseIndexRule(XmlPullParser parser) throws Exception {
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
		if (parser.nextTag() == XmlPullParser.START_TAG) {
			throw new IllegalArgumentException("An index rule has one form, the attribute to: keys of several meanings "
					+ "cannot be told apart in the OBF, the meanings belong to <query>" + where);
		}
		if (!enabled) {
			if (to != null || mode != null || keys != null) {
				throw new IllegalArgumentException("A disabled rule holds only from and object" + where);
			}
			layer.add(newRule(object, from, "", "Single", false, false, file, where), false);
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
				file, where), true);
	}

	private void parseUnglue(XmlPullParser parser) throws Exception {
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
			layer.add(new Unglue(glue.charAt(0), null, 0, file), false);
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
		layer.add(new Unglue(glue.charAt(0), unicodeScript, min, file), true);
	}

	private void parseSkipPenalty(XmlPullParser parser) throws Exception {
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
	/**
	 * A mirror pair, a {@code <rule>} directly under {@code <rules>} (rules-spec.md, 3.4): one word and its one form,
	 * a query form both ways and alternative names both ways. It shares the key of a query rule in the layers.
	 */
	private void parseMirrorRule(XmlPullParser parser) throws Exception {
		String from = parser.getAttributeValue(null, "from");
		String where = where(from, "rules", file);
		checkRemovedAttributes(parser, where);
		checkAttributes(parser, QUERY_ATTRIBUTES, "a mirror rule", where);
		if (from == null || from.isEmpty()) {
			throw new IllegalArgumentException("Missing from in " + file);
		}
		String to = parser.getAttributeValue(null, "to");
		String object = parser.getAttributeValue(null, "object");
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		if (parser.nextTag() == XmlPullParser.START_TAG) {
			throw new IllegalArgumentException("A mirror rule has one form, the attribute to: through a shared word "
					+ "the meanings of a word of several would match each other" + where);
		}
		String word = from.toLowerCase(Locale.ROOT);
		if (!enabled) {
			if (to != null || object != null) {
				throw new IllegalArgumentException("A disabled rule holds only from" + where);
			}
			layer.add(new WordRule(word, List.of(), new Mirror(file, null)), false);
			return;
		}
		if (BUILDING_OBJECT.equals(object)) {
			throw new IllegalArgumentException("A house-number qualifier belongs to <query>: it has no alternative "
					+ "name" + where);
		}
		if (to == null || to.trim().isEmpty()) {
			throw new IllegalArgumentException("A mirror rule needs a form, an ignorable word belongs to <query>"
					+ where);
		}
		String text = to.trim();
		List<Form> forms = List.of(parseForm(word, object, text, where));
		validateForms(word, forms, where);
		layer.add(new WordRule(word, forms, new Mirror(file, text)), true);
	}

	private void parseQueryRule(XmlPullParser parser) throws Exception {
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
		layer.add(new WordRule(word, forms), true);
	}

	private Form parseForm(String word, String object, String text, String where) {
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

	private void validateForms(String word, List<Form> forms, String where) {
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
				if (other.word().equals(form.word()) && other.overlaps(form)) {
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

	private String where(String from, String section, String file) {
		return " (from=\"" + from + "\" in <" + section + "> of " + file + ")";
	}

	private void checkRemovedAttributes(XmlPullParser parser, String where) {
		if (parser.getAttributeValue(null, "replace") != null) {
			throw new IllegalArgumentException("replace is removed: a stored name keeps the words of OSM" + where);
		}
		if (parser.getAttributeValue(null, "common") != null) {
			throw new IllegalArgumentException("Common words are listed in <skipPenalty> of <query>" + where);
		}
	}

	private void checkAttributes(XmlPullParser parser, Set<String> allowed, String what, String where) {
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			if (!allowed.contains(parser.getAttributeName(i))) {
				throw new IllegalArgumentException("Unknown attribute " + parser.getAttributeName(i) + " of " + what
						+ where);
			}
		}
	}

	private Rule newRule(String object, String from, String to, String mode, boolean alwaysKeys,
			boolean enabled, String file, String where) {
		if (!"All".equals(mode) && !"Single".equals(mode)) {
			throw new IllegalArgumentException("Invalid mode '" + mode + "' of " + from + ", expected All or Single"
					+ where);
		}
		try {
			Pattern.compile(from);
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException("Invalid regexp '" + from + "': " + e.getDescription() + where, e);
		}
		return new Rule(object, from, to, mode, alwaysKeys, enabled, file);
	}

	private void validateObject(String object, boolean query, String where) {
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
			if (!t.equals(ANY_OBJECT) && Arrays.stream(SearchModRuleOwner.values()).noneMatch(o -> o.tag.equals(t))) {
				throw new IllegalArgumentException("Unknown object '" + t + "', expected an owner, '*' or "
						+ "'building'" + where);
			}
		}
		if (any && types.length > 1) {
			throw new IllegalArgumentException("'*' is every object, it is not listed with others" + where);
		}
	}

	private boolean booleanAttribute(XmlPullParser parser, String key, boolean defaultValue, String where) {
		String value = parser.getAttributeValue(null, key);
		if (value == null) {
			return defaultValue;
		}
		if ("true".equals(value) || "false".equals(value)) {
			return "true".equals(value);
		}
		throw new IllegalArgumentException("Invalid " + key + " '" + value + "', expected true or false" + where);
	}

	private void parseGroup(XmlPullParser parser) throws Exception {
		String id = parser.getAttributeValue(null, "id");
		String where = " (<group id=\"" + id + "\"> of " + file + ")";
		checkAttributes(parser, Set.of("id", "languages", "locales"), "<group>", where);
		locales.addGroup(id, list(parser.getAttributeValue(null, "languages")),
				list(parser.getAttributeValue(null, "locales")));
		endElement(parser, where);
	}

	private void parseMap(XmlPullParser parser) throws Exception {
		String locale = parser.getAttributeValue(null, "locale");
		String where = " (<map locale=\"" + locale + "\"> of " + file + ")";
		checkAttributes(parser, Set.of("locale", "prefixes", "group", "translit"), "<map>", where);
		locales.addMap(locale, list(parser.getAttributeValue(null, "prefixes")), parser.getAttributeValue(null, "group"),
				parser.getAttributeValue(null, "translit"));
		endElement(parser, where);
	}

	private List<String> list(String value) {
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

	private void endElement(XmlPullParser parser, String where) throws Exception {
		if (parser.nextTag() == XmlPullParser.START_TAG) {
			throw new IllegalArgumentException("Unexpected element " + parser.getName() + where);
		}
	}

	/** A lower layer disables only a rule that an upper layer defines. */
	void checkLayers(List<SearchModRulesParser.Layer> layers) {
		Set<String> index = new LinkedHashSet<>();
		Set<String> unglues = new LinkedHashSet<>();
		Set<String> classes = new LinkedHashSet<>();
		Set<String> query = new LinkedHashSet<>();
		Set<String> skipPenalty = new LinkedHashSet<>();
		for (SearchModRulesParser.Layer l : layers) {
			checkDisabled(index, l.disabledIndex, l);
			checkDisabled(unglues, l.disabledUnglues, l);
			checkDisabled(classes, l.disabledClasses, l);
			checkDisabled(query, l.disabledQuery, l);
			checkDisabled(skipPenalty, l.disabledSkipPenalty, l);
			index.addAll(l.index.keySet());
			unglues.addAll(l.unglues.keySet());
			classes.addAll(l.classes.keySet());
			query.addAll(l.query.keySet());
			skipPenalty.addAll(l.skipPenalty.keySet());
		}
	}

	private void checkDisabled(Set<String> defined, Map<String, String> disabled, SearchModRulesParser.Layer l) {
		for (Map.Entry<String, String> e : disabled.entrySet()) {
			if (!defined.remove(e.getKey())) {
				throw new IllegalArgumentException("Cannot disable " + e.getValue() + " in " + l.file
						+ ": no upper layer defines it");
			}
		}
	}

	/** The rules of one locale do not contradict each other. */
	void checkConsistency(SearchModLocaleRules rules, String locale) {
		String where = " in the rules of locale '" + locale + "'";
		Map<String, WordRule> query = new LinkedHashMap<>();
		for (WordRule rule : rules.query()) {
			query.put(rule.word(), rule);
		}
		Set<String> buildings = rules.buildings();
		Set<String> ignorables = rules.ignorables();
		for (WordRule rule : query.values()) {
			for (Form form : rule.forms()) {
				if (form.isBuilding() || form.isIgnorable()) {
					continue;
				}
				WordRule other = query.get(form.word());
				if (other != null && other.hasForm(rule.word())) {
					throw new IllegalArgumentException("The rule of '" + other.word() + "' repeats the reverse form of '"
							+ rule.word() + "' -> '" + form.word() + "': reverse forms are generated" + where);
				}
				if (buildings.contains(form.word()) || ignorables.contains(form.word())) {
					throw new IllegalArgumentException("The form '" + form.word() + "' of '" + rule.word()
							+ "' is a house-number qualifier or an ignorable word" + where);
				}
			}
		}
		Set<String> alignedIgnorables = new LinkedHashSet<>();
		Set<String> alignedBuildings = new LinkedHashSet<>();
		for (String word : ignorables) {
			alignedIgnorables.add(SearchAlgorithms.alignChars(word));
		}
		for (String word : buildings) {
			alignedBuildings.add(SearchAlgorithms.alignChars(word));
		}
		for (String word : rules.skipPenalty().keySet()) {
			if (alignedBuildings.contains(word) || alignedIgnorables.contains(word)) {
				throw new IllegalArgumentException("The <skipPenalty> word '" + word + "' is a house-number "
						+ "qualifier or an ignorable word: an ignorable word never penalizes a name" + where);
			}
			Integer wordClass = rules.classes().get(word);
			if (wordClass != null && wordClass != CLASS_ALWAYS) {
				throw new IllegalArgumentException("The <skipPenalty> word '" + word + "' is a word of <class"
						+ wordClass + ">, which never penalizes a name" + where);
			}
		}
		for (String word : ignorables) {
			Integer wordClass = rules.classes().get(SearchAlgorithms.alignChars(word));
			if (wordClass != null && wordClass == CLASS_ALWAYS) {
				throw new IllegalArgumentException("The ignorable word '" + word + "' is a word of <class0>" + where);
			}
		}
		for (WordRule rule : query.values()) {
			if (rule.mirror() == null || rule.forms().isEmpty()) {
				continue;
			}
			Form form = rule.forms().get(0);
			String mirror = " of the mirror pair '" + rule.word() + "' -> '" + form.word() + "' ("
					+ rule.mirror().file() + ")";
			// the word of a mirror means one thing: another rule reaching it would add its meaning to the pair
			for (WordRule other : query.values()) {
				if (other != rule && other.hasForm(rule.word())) {
					throw new IllegalArgumentException("The word '" + rule.word() + "'" + mirror + " is a form of "
							+ other + " too" + where);
				}
			}
			if (query.containsKey(form.word())) {
				throw new IllegalArgumentException("The form '" + form.word() + "'" + mirror + " has its own rule: a "
						+ "mirror pair works both ways and is written once" + where);
			}
		}
	}
}
