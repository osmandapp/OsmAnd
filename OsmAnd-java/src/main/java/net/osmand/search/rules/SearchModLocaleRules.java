package net.osmand.search.rules;

import net.osmand.search.rules.SearchModRules.SearchModRuleOwner;

import net.osmand.util.SearchAlgorithms;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * the data ({@link SearchModLocales#forMap}, {@link SearchModLocales#forName}); {@link SearchModRules} loads the rules of a locale, not the language of the user interface.
 */
public final class SearchModLocaleRules {
	public static final String ANY_OBJECT = "*";
	public static final String BUILDING_OBJECT = "building";
	public static final String VERSION = "5";
	public static final String BASE_FILE = "rules.xml";
	/** classes of words of {@code <index>}, the classes of {@code common_words_groups.tsv} */
	public static final int CLASS_ALWAYS = 0;
	public static final int CLASS_SERVICE = 1;
	public static final int CLASS_FREQUENT = 2;


	// the object of the identity of an <unglue> rule: it applies to every name
	static final String UNGLUE_OBJECT = "unglue";


	private final List<Rule> index;
	private final List<Unglue> unglues;
	// aligned word -> class of <class0>, <class1>, <class2>
	private final Map<String, Integer> classes;
	private final List<WordRule> query;
	// aligned word -> owners of the <skipPenalty> entries of the word
	private final Map<String, List<String>> skipPenalty;
	// word -> its forms and then the reverse forms that lead to it
	private final Map<String, List<Form>> forms;
	private final Set<String> buildings;
	private final Set<String> ignorables;

	/** @param layers rules files from general to specific: a lower layer replaces or disables rules of upper ones */
	SearchModLocaleRules(String locale, List<SearchModRulesParser.Layer> layers) {
		Map<String, Rule> index = new LinkedHashMap<>();
		Map<String, Unglue> unglues = new LinkedHashMap<>();
		Map<String, Integer> classes = new LinkedHashMap<>();
		Map<String, WordRule> query = new LinkedHashMap<>();
		Map<String, SkipPenalty> skipPenalty = new LinkedHashMap<>();
		for (SearchModRulesParser.Layer layer : layers) {
			disable(index, layer.disabledIndex);
			disable(unglues, layer.disabledUnglues);
			disable(classes, layer.disabledClasses);
			disable(query, layer.disabledQuery);
			disable(skipPenalty, layer.disabledSkipPenalty);
			index.putAll(layer.index);
			unglues.putAll(layer.unglues);
			classes.putAll(layer.classes);
			query.putAll(layer.query);
			skipPenalty.putAll(layer.skipPenalty);
		}
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
				forms.computeIfAbsent(form.word(), k -> new LinkedHashSet<>()).add(new Form(form.object(), rule.word));
			}
		}
		Map<String, List<String>> skip = new LinkedHashMap<>();
		for (SkipPenalty entry : skipPenalty.values()) {
			skip.computeIfAbsent(entry.word(), k -> new ArrayList<>()).add(entry.object());
		}
		List<Rule> indexRules = new ArrayList<>(index.values());
		for (WordRule rule : query.values()) {
			if (rule.mirror == null || rule.forms.isEmpty()) {
				continue;
			}
			Form form = rule.forms.get(0);
			indexRules.add(new Rule(form.object(), rule.word, rule.mirror.to, rule.mirror.file, true));
			indexRules.add(new Rule(form.object(), rule.mirror.to, rule.word, rule.mirror.file, false));
		}
		Map<String, List<Form>> lists = new LinkedHashMap<>();
		for (Map.Entry<String, Set<Form>> e : forms.entrySet()) {
			lists.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		this.index = indexRules;
		this.unglues = new ArrayList<>(unglues.values());
		this.classes = classes;
		this.query = new ArrayList<>(query.values());
		this.skipPenalty = skip;
		this.forms = lists;
		this.buildings = buildings;
		this.ignorables = ignorables;
	}

	private void disable(Map<String, ?> rules, Map<String, String> disabled) {
		for (String key : disabled.keySet()) {
			rules.remove(key);
		}
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

	/** Words of {@code <skipPenalty>} aligned ({@link SearchAlgorithms#alignChars}) -> the owners they apply to. */
	public Map<String, List<String>> skipPenalty() {
		return skipPenalty;
	}

	/**
	 * An alternative name with the glued words of a name split by {@code <unglue>} and the rules that split a word of it.
	 */
	public record Unglued(String name, List<Unglue> rules) {
		/** the identities in the statistics of the OBF writer: every rule that splits a word counts the name */
		public List<RuleId> ids() {
			List<RuleId> ids = new ArrayList<>(rules.size());
			for (Unglue u : rules) {
				ids.add(u.id());
			}
			return ids;
		}
	}

	/**
	 * Every {@code <unglue>} of a word splits it in one pass, so a word of two glues gives one alternative name
	 * (rules-spec.md, 3.2): "L'Atelier d'Anaïs" -> "Atelier Anaïs", "Wijkopenauto's.nl" -> "Wijkopenauto nl". A rule
	 * splits only words of its script without digits; a part is kept when it is as long as the minPart of the glues
	 * around it.
	 *
	 * @return the alternative name, null when no word is glued
	 */
	public Unglued unglue(String name) {
		if (unglues.isEmpty() || name == null) {
			return null;
		}
		List<String> result = new ArrayList<>();
		Set<Unglue> applied = new LinkedHashSet<>();
		for (String word : SearchAlgorithms.canonicalizePunctuation(name).split(" ")) {
			if (word.isEmpty()) {
				continue;
			}
			List<String> parts = unglueWord(word, applied);
			if (parts == null) {
				result.add(word);
			} else {
				result.addAll(parts);
			}
		}
		String unglued = String.join(" ", result).trim();
		return applied.isEmpty() || unglued.isEmpty() ? null : new Unglued(unglued, new ArrayList<>(applied));
	}

	// the parts of a glued word, null when no rule splits it; the rules that split it go to applied
	private List<String> unglueWord(String word, Set<Unglue> applied) {
		if (word.chars().anyMatch(Character::isDigit)) {
			return null;
		}
		Map<Character, Unglue> glues = new LinkedHashMap<>();
		for (Unglue u : unglues) {
			if (word.indexOf(u.glue()) >= 0 && u.appliesToScript(word)) {
				glues.put(u.glue(), u);
			}
		}
		if (glues.isEmpty()) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		boolean letterDropped = false;
		int start = 0;
		Unglue before = null;
		for (int i = 0; i <= word.length(); i++) {
			Unglue after = i == word.length() ? null : glues.get(word.charAt(i));
			if (i == word.length() || after != null) {
				String part = word.substring(start, i);
				int minPart = Math.max(before == null ? 0 : before.minPart(), after == null ? 0 : after.minPart());
				if (part.length() >= Math.max(minPart, 1)) {
					parts.add(part);
				} else if (!part.isEmpty()) {
					letterDropped = true;
				}
				start = i + 1;
				before = after;
			}
		}
		if (parts.size() <= 1 && !letterDropped) {
			return null;
		}
		applied.addAll(glues.values());
		return parts;
	}

	/** A rule part with an {@code object}: the owners of names it applies to ("street", "*", "street,poi"). */
	public interface Scoped {
		String object();

		/** @return the owners of {@link #object()}; a part checked per name keeps them, see {@link Rule} */
		default Set<SearchModRuleOwner> owners() {
			return parseOwners();
		}

		default Set<SearchModRuleOwner> parseOwners() {
			EnumSet<SearchModRuleOwner> owners = EnumSet.noneOf(SearchModRuleOwner.class);
			for (String type : object().split(",")) {
				String t = type.trim();
				for (SearchModRuleOwner o : SearchModRuleOwner.values()) {
					if (t.equals(ANY_OBJECT) || t.equals(o.tag)) {
						owners.add(o);
					}
				}
			}
			return owners;
		}

		/** @return true when it applies to the owner of a name */
		default boolean appliesTo(SearchModRuleOwner owner) {
			return owners().contains(owner);
		}

		/** @return true when both apply to some owner */
		default boolean overlaps(Scoped other) {
			Set<SearchModRuleOwner> owners = owners();
			for (SearchModRuleOwner o : other.owners()) {
				if (owners.contains(o)) {
					return true;
				}
			}
			return false;
		}
	}

	/** The owners of names of a {@code <skipPenalty>} entry or of a word that never penalizes a name. */
	public record Scope(String object) implements Scoped {
	}

	record SkipPenalty(String word, String object) implements Scoped {
		String key() {
			return word + '\n' + object;
		}
	}

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to. An empty word is
	 * a house-number qualifier ({@code object="building"}) or an ignorable word.
	 */
	public record Form(String object, String word) implements Scoped {
		public boolean isBuilding() {
			return object.equals(BUILDING_OBJECT);
		}

		public boolean isIgnorable() {
			return word.isEmpty() && !isBuilding();
		}

	}

	/** The file and the form as written of a mirror pair, for its alternative names (rules-spec.md, 3.4). */
	public record Mirror(String file, String to) {
	}

	/**
	 * The rule of a query word: every meaning of the word, the first is the main one.
	 *
	 * @param mirror the pair is a mirror pair, its alternative names are rules of {@code <index>} too; null for a rule
	 *               of {@code <query>}
	 */
	public record WordRule(String word, List<Form> forms, Mirror mirror) {
		WordRule(String word, List<Form> forms) {
			this(word, forms, null);
		}

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
	public record Unglue(char glue, Character.UnicodeScript script, int minPart, String file) {
		boolean appliesToScript(String word) {
			return script == null || word.codePoints().filter(Character::isLetter)
					.allMatch(c -> Character.UnicodeScript.of(c) == script);
		}

		/** the identity of the rule in the statistics of the OBF writer: file and glue */
		public RuleId id() {
			return new RuleId(file, UNGLUE_OBJECT, String.valueOf(glue));
		}

		@Override
		public String toString() {
			return "<unglue glue=\"" + glue + "\">";
		}
	}

	/**
	 * The identity of a rule of {@code <index>} in the statistics of generation (rules-spec.md, 4.3): the file that
	 * defines it, its object and its from; for {@code <unglue>} the object is {@code unglue} and from is the glue.
	 */
	public record RuleId(String file, String object, String from) implements Comparable<RuleId> {
		@Override
		public int compareTo(RuleId o) {
			return toString().compareTo(o.toString());
		}

		@Override
		public String toString() {
			return file + " " + object + " " + from;
		}
	}

	/** A rule of {@code <index>}: a regexp of names of an owner and the alternative name it adds. */
	public static final class Rule implements Scoped {
		static final String KEYS_STATS = "stats";
		static final String KEYS_ALWAYS = "always";

		public final String object;
		// parsed object: an index rule is checked for every name of the map
		private final Set<SearchModRuleOwner> owners;
		private final boolean all;
		private final boolean alwaysKeys;
		private final Pattern from;
		private final String fromText;
		private final String to;
		private final String file;
		// the from of the identity: the regexp, or "blvd→Boulevard" of a mirror pair
		private final String idFrom;

		Rule(String object, String from, String to, String mode, boolean alwaysKeys, boolean enabled,
				String file) {
			this(object, from, to, mode, alwaysKeys, enabled, file, from);
		}

		private Rule(String object, String from, String to, String mode, boolean alwaysKeys, boolean enabled,
				String file, String idFrom) {
			this.object = object;
			this.owners = parseOwners();
			this.file = file;
			this.idFrom = idFrom;
			this.all = "All".equals(mode);
			this.alwaysKeys = alwaysKeys;
			this.fromText = from;
			this.to = to;
			this.from = Pattern.compile(from);
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

		/** the identity of the rule in the statistics of the OBF writer: file, object and from */
		public RuleId id() {
			return new RuleId(file, object, idFrom);
		}

		/**
		 * One direction of a mirror pair: the word as a whole word of a name, in any case, an abbreviation with its dot
		 * ("Blvd." -> "Boulevard"), is replaced by the other word of the pair.
		 *
		 * @param abbreviation the word is the abbreviation of the pair (the from of the mirror rule)
		 */
		Rule(String object, String word, String other, String file, boolean abbreviation) {
			this(object, "(?iu)(?<![\\p{L}\\p{M}\\p{N}])" + Pattern.quote(word) + "(?![\\p{L}\\p{M}\\p{N}])"
					+ (abbreviation ? "\\.?" : ""), Matcher.quoteReplacement(other), "All", false, true, file,
					word + "\u2192" + other);
		}

		@Override
		public String object() {
			return object;
		}

		@Override
		public Set<SearchModRuleOwner> owners() {
			return owners;
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
