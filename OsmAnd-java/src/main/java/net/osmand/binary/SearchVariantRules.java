package net.osmand.binary;

import net.osmand.PlatformUtil;
import net.osmand.util.SearchAlgorithms;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Every rule is a {@code <rule>}. Where it stands tells where it works: inside {@code <index>} the OBF writer applies
 * it to the names of objects, inside {@code <query>} the search applies it to the words of a query, directly inside
 * {@code <rules>} it works on both sides. Rules are layered from general to specific: {@code rules.xml} (every map),
 * {@code rules_<language>.xml}, then {@code rules_<language>_<COUNTRY>.xml}. The key of a rule is {@code object},
 * {@code from} and {@code to}: a rule of a lower layer with the key of an upper one replaces it,
 * {@code enabled="false"} removes it, and two rules with one key in one file are an error. The locale is the locale of
 * the data ({@link SearchLocales#forMap}, {@link SearchLocales#forName}), not the language of the user interface.
 * <p>
 * Attributes: {@code from} (a plain word, matched as the whole word ignoring case, or a Java regexp), {@code to}
 * (replacement; empty in {@code <query>} makes the word ignorable), {@code object} (owner of the name, {@code *} by
 * default, {@code building} for house number qualifiers), {@code mode} ({@code All} or the default {@code Single},
 * a regexp only), {@code replace} (outside {@code <index>} and {@code <query>}: the stored name is rewritten, the
 * default adds the form to the index) and {@code common} (a plain word and its {@code to} do not count as unmatched
 * words of a name; not in {@code <index>}).
 */
public final class SearchVariantRules {
	public static final String ANY_OBJECT = "*";
	public static final String BUILDING_OBJECT = "building";
	public static final String VERSION = "3";

	/** Where a rule works. */
	public enum Scope {
		INDEX, QUERY, BOTH
	}

	private static final Set<String> OWNERS = Set.of("street", "locality", "boundary", "postcode", "poi");
	private static final Set<String> ATTRIBUTES = Set.of("from", "to", "object", "mode", "replace", "common", "enabled");
	private static final Map<String, SearchVariantRules> CACHE = new ConcurrentHashMap<>();
	private static final Pattern PLAIN_WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");
	private static final Pattern GROUP_REFERENCE = Pattern.compile("\\$(\\d+)");

	private final List<Rule> index;
	private final List<Rule> normalizations;
	private final List<Rule> query;
	private final List<Rule> buildings;
	private final List<Rule> ignorables;

	private SearchVariantRules(String locale, Map<String, Rule> rules) {
		List<Rule> index = new ArrayList<>();
		List<Rule> normalizations = new ArrayList<>();
		List<Rule> query = new ArrayList<>();
		List<Rule> buildings = new ArrayList<>();
		List<Rule> ignorables = new ArrayList<>();
		for (Rule rule : rules.values()) {
			switch (rule.scope) {
				case INDEX -> index.add(rule);
				case QUERY -> {
					if (rule.isBuilding()) {
						buildings.add(rule);
					} else if (rule.isIgnorable()) {
						ignorables.add(rule);
					} else {
						query.add(rule);
					}
				}
				case BOTH -> {
					query.add(rule);
					(rule.replace ? normalizations : index).add(rule);
				}
			}
		}
		validate(locale, normalizations, query, buildings, ignorables);
		this.index = Collections.unmodifiableList(index);
		this.normalizations = Collections.unmodifiableList(normalizations);
		this.query = Collections.unmodifiableList(query);
		this.buildings = Collections.unmodifiableList(buildings);
		this.ignorables = Collections.unmodifiableList(ignorables);
	}

	/** Checks that hold for the rules of one locale after the layers are merged. */
	private static void validate(String locale, List<Rule> normalizations, List<Rule> query, List<Rule> buildings,
			List<Rule> ignorables) {
		String where = " in the rules of locale '" + locale + "'";
		for (int i = 0; i < normalizations.size(); i++) {
			for (int j = i + 1; j < normalizations.size(); j++) {
				Rule a = normalizations.get(i);
				Rule b = normalizations.get(j);
				if (a.word.equals(b.word) && objectsOverlap(a.object, b.object)) {
					throw new IllegalArgumentException("The word '" + a.word + "' is replaced twice: by '" + a.to
							+ "' and '" + b.to + "'" + where);
				}
			}
		}
		Set<String> ignorable = new HashSet<>();
		for (Rule rule : ignorables) {
			ignorable.add(rule.word);
		}
		for (Rule rule : buildings) {
			if (ignorable.contains(rule.word)) {
				throw new IllegalArgumentException("The word '" + rule.word + "' is a building qualifier and an "
						+ "ignorable word" + where);
			}
		}
		for (Rule rule : query) {
			if (rule.word != null && ignorable.contains(rule.word)) {
				throw new IllegalArgumentException("The word '" + rule.word + "' is an ignorable word and has the "
						+ "form '" + rule.to + "'" + where);
			}
		}
	}

	/** @param locale rules locale; null, empty or malformed selects only the base rules */
	public static SearchVariantRules forLocale(String locale) {
		return CACHE.computeIfAbsent(SearchLocales.normalize(locale), SearchVariantRules::load);
	}

	/** Rules that add an alternative name of an object to the name index. */
	public List<Rule> index() {
		return index;
	}

	/** Rules with {@code replace="true"}: the stored name is rewritten, the query word gets the form. */
	public List<Rule> normalizations() {
		return normalizations;
	}

	/** Rules that give a query word one more form to look for. */
	public List<Rule> query() {
		return query;
	}

	/** Rules ({@code object="building"}) that mark a query word as a part of a house number. */
	public List<Rule> buildings() {
		return buildings;
	}

	/** Rules with an empty {@code to}: words that are not a part of a name (conjunctions, articles). */
	public List<Rule> ignorables() {
		return ignorables;
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
		Map<String, Rule> rules = new LinkedHashMap<>();
		for (Layer layer : layers) {
			for (Map.Entry<String, String> disabled : layer.disabled.entrySet()) {
				if (rules.remove(disabled.getKey()) == null) {
					throw new IllegalArgumentException("Cannot disable " + disabled.getValue() + " in " + layer.file
							+ ": no upper layer defines it");
				}
			}
			rules.putAll(layer.rules);
		}
		return new SearchVariantRules(locale, rules);
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
		final Map<String, Rule> rules = new LinkedHashMap<>();
		// key -> text of the rule for messages
		final Map<String, String> disabled = new LinkedHashMap<>();

		Layer(String file) {
			this.file = file;
		}

		void add(Rule rule) {
			String key = rule.key();
			if (rules.containsKey(key) || disabled.containsKey(key)) {
				throw new IllegalArgumentException("Duplicate rule " + rule + " in " + file);
			}
			if (rule.enabled) {
				rules.put(key, rule);
			} else {
				disabled.put(key, rule.toString());
			}
		}
	}

	static Layer parseLayer(InputStream input, String file) throws Exception {
		Layer layer = new Layer(file);
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		parser.setInput(input, "UTF-8");
		Scope section = null;
		int depth = 0;
		boolean root = false;
		int event;
		while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG) {
				depth++;
				String tag = parser.getName();
				if (depth == 1 && "rules".equals(tag)) {
					if (!VERSION.equals(parser.getAttributeValue(null, "version"))) {
						throw new IllegalArgumentException("Unsupported search rules version in " + file
								+ ", expected " + VERSION);
					}
					root = true;
				} else if (depth == 2 && ("index".equals(tag) || "query".equals(tag))) {
					section = "index".equals(tag) ? Scope.INDEX : Scope.QUERY;
				} else if ("rule".equals(tag) && depth == (section == null ? 2 : 3)) {
					layer.add(parseRule(parser, section == null ? Scope.BOTH : section, file));
				} else {
					throw new IllegalArgumentException("Unexpected search rule tag " + tag + " in " + file);
				}
			} else if (event == XmlPullParser.END_TAG) {
				depth--;
				if (depth == 1) {
					section = null;
				}
			}
		}
		if (!root) {
			throw new IllegalArgumentException("Missing rules root in " + file);
		}
		return layer;
	}

	private static Rule parseRule(XmlPullParser parser, Scope scope, String file) {
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			if (!ATTRIBUTES.contains(parser.getAttributeName(i))) {
				throw new IllegalArgumentException("Unknown attribute " + parser.getAttributeName(i) + " of a rule in "
						+ file);
			}
		}
		String from = parser.getAttributeValue(null, "from");
		if (from == null || from.isEmpty()) {
			throw new IllegalArgumentException("Missing from in " + file);
		}
		String where = " (from=\"" + from + "\" in <" + (scope == Scope.BOTH ? "rules" : scope.name().toLowerCase())
				+ "> of " + file + ")";
		String object = parser.getAttributeValue(null, "object");
		object = object == null || object.isEmpty() ? ANY_OBJECT : object;
		validateObject(object, where);
		String to = parser.getAttributeValue(null, "to");
		String mode = parser.getAttributeValue(null, "mode");
		boolean enabled = booleanAttribute(parser, "enabled", true, where);
		boolean replace = booleanAttribute(parser, "replace", false, where);
		boolean common = booleanAttribute(parser, "common", false, where);
		boolean plain = PLAIN_WORD.matcher(from).matches();
		boolean building = object.equals(BUILDING_OBJECT);
		if (!enabled) {
			if (mode != null || parser.getAttributeValue(null, "replace") != null
					|| parser.getAttributeValue(null, "common") != null) {
				throw new IllegalArgumentException("A disabled rule holds only from, to and object" + where);
			}
			return newRule(scope, object, from, to == null ? "" : to, "Single", false, false, false, where);
		}
		if (replace && scope != Scope.BOTH) {
			throw new IllegalArgumentException("replace belongs to a rule outside <index> and <query>" + where);
		}
		if (common && scope == Scope.INDEX) {
			throw new IllegalArgumentException("common does not apply to <index>" + where);
		}
		if (building) {
			if (scope != Scope.QUERY) {
				throw new IllegalArgumentException("A building rule belongs to <query>" + where);
			}
			if (to != null || mode != null || common) {
				throw new IllegalArgumentException("A building rule holds only from" + where);
			}
			if (!plain) {
				throw new IllegalArgumentException("A building rule needs a plain word" + where);
			}
			to = "";
		} else {
			if (to == null) {
				throw new IllegalArgumentException("Missing to" + where);
			}
			validateTarget(scope, object, to, from, plain, mode, common, where);
		}
		if (common && !plain) {
			throw new IllegalArgumentException("common needs a plain word" + where);
		}
		if (mode != null && plain) {
			throw new IllegalArgumentException("mode applies to a regexp" + where);
		}
		return newRule(scope, object, from, to, mode == null ? "Single" : mode, replace, common, true, where);
	}

	private static Rule newRule(Scope scope, String object, String from, String to, String mode, boolean replace,
			boolean common, boolean enabled, String where) {
		try {
			return new Rule(scope, object, from, to, mode, replace, common, enabled);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + where, e);
		}
	}

	private static void validateTarget(Scope scope, String object, String to, String from, boolean plain, String mode,
			boolean common, String where) {
		if (to.isEmpty()) {
			if (scope != Scope.QUERY) {
				throw new IllegalArgumentException("An empty to (an ignorable word) belongs to <query>" + where);
			}
			if (!object.equals(ANY_OBJECT) || !plain || mode != null) {
				throw new IllegalArgumentException("An ignorable word is a plain word of any object" + where);
			}
			if (common) {
				throw new IllegalArgumentException("An ignorable word is common already" + where);
			}
			return;
		}
		if (scope != Scope.INDEX && !to.contains("$") && SearchAlgorithms.splitAndNormalize(to, false).size() != 1) {
			// a token is matched with one word of a name: several words would mean "or", not a phrase
			throw new IllegalArgumentException("A query form is one word, a phrase belongs to <index>" + where);
		}
		if (scope == Scope.BOTH && !plain) {
			throw new IllegalArgumentException("A rule outside <index> and <query> needs a plain word" + where);
		}
		if (plain && from.equalsIgnoreCase(to)) {
			throw new IllegalArgumentException("The rule changes nothing" + where);
		}
	}

	private static void validateObject(String object, String where) {
		if (object.equals(BUILDING_OBJECT)) {
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

	public static final class Rule {
		public final Scope scope;
		public final String object;
		public final boolean replace;
		public final boolean common;
		/** plain word of {@code from} in lower case, null when {@code from} is a regexp */
		public final String word;
		final boolean enabled;
		private final boolean all;
		private final Pattern from;
		private final String fromText;
		private final String to;
		private final String replacement;

		private Rule(Scope scope, String object, String from, String to, String mode, boolean replace,
				boolean common, boolean enabled) {
			this.scope = scope;
			this.object = object;
			if (!"All".equals(mode) && !"Single".equals(mode)) {
				throw new IllegalArgumentException("Invalid mode '" + mode + "' of " + from + ", expected All or Single");
			}
			this.all = "All".equals(mode);
			this.replace = replace;
			this.common = common;
			this.enabled = enabled;
			this.fromText = from;
			this.to = to;
			if (PLAIN_WORD.matcher(from).matches()) {
				this.word = from.toLowerCase(Locale.ROOT);
				this.from = Pattern.compile("^" + Pattern.quote(from) + "$",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
				this.replacement = Matcher.quoteReplacement(to);
			} else {
				this.word = null;
				this.replacement = to;
				try {
					this.from = Pattern.compile(from);
				} catch (PatternSyntaxException e) {
					throw new IllegalArgumentException("Invalid regexp '" + from + "': " + e.getDescription(), e);
				}
				if (enabled) {
					checkRegexp(from, to);
				}
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

		/** the key of a rule in the layers: object, from and to */
		String key() {
			return object + '\n' + fromText + '\n' + to;
		}

		/** the form that is rewritten: a plain word or a regexp */
		public String from() {
			return fromText;
		}

		/** replacement of the rewritten form */
		public String to() {
			return to;
		}

		public boolean isBuilding() {
			return object.equals(BUILDING_OBJECT);
		}

		/** an empty target: the word is not a part of a name */
		public boolean isIgnorable() {
			return to.isEmpty() && !isBuilding();
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
			String result = all ? matcher.replaceAll(replacement) : matcher.replaceFirst(replacement);
			return result.equals(text) ? null : result;
		}

		@Override
		public String toString() {
			return "<rule from=\"" + fromText + "\" to=\"" + to + "\" object=\"" + object + "\">";
		}
	}
}
