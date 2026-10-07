package net.osmand.binary;

import net.osmand.binary.SearchVariantRules.Scope;
import net.osmand.binary.SearchVariantRules.Scoped;
import net.osmand.util.SearchAlgorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Spatial search (v2) word dictionaries of the {@link SearchVariantRules} of one locale, see
 * {@link SearchRules#dictionary(String)}. Immutable.
 */
public final class SearchRulesDictionary {

	private static final List<Scoped> EVERY_OWNER = List.of(new Scope(SearchVariantRules.ANY_OBJECT));

	private final String locale;
	// query word -> its forms and reverse forms, in the order of the rules
	private final Map<String, List<QueryForm>> forms = new HashMap<>();
	private final Set<String> buildingWords;
	// aligned ignorable words (to="")
	private final Set<String> ignorables;
	// aligned word of a name -> owners whose names it does not penalize (<skipPenalty>, ignorable words, <class1/2>)
	private final Map<String, List<Scoped>> penaltyFree;

	SearchRulesDictionary(String locale, SearchVariantRules rules) {
		this.locale = locale;
		Map<String, SearchVariantRules.WordRule> ruleOfWord = new HashMap<>();
		for (SearchVariantRules.WordRule rule : rules.query()) {
			ruleOfWord.put(rule.word(), rule);
		}
		for (String word : rules.formWords()) {
			List<QueryForm> list = new ArrayList<>();
			for (SearchVariantRules.Form form : rules.forms(word)) {
				// a reverse form leads to the word of a rule that has this word as its form ("street" -> "st")
				SearchVariantRules.WordRule other = ruleOfWord.get(form.word());
				list.add(new QueryForm(form.object(), form.word(), other != null && other.hasForm(word)));
			}
			// a token is aligned (ß -> ss, no diacritics): "Straße" asks for "strasse"
			forms.put(SearchAlgorithms.alignChars(word), list);
		}
		// the words of <skipPenalty> and the classes are aligned by the rules, the ignorable words are query words
		Map<String, Set<String>> owners = new HashMap<>();
		rules.skipPenalty().forEach((word, list) -> owners.computeIfAbsent(word, k -> new LinkedHashSet<>())
				.addAll(list));
		Set<String> ignorables = new TreeSet<>();
		for (String word : rules.ignorables()) {
			String aligned = SearchAlgorithms.alignChars(word);
			ignorables.add(aligned);
			owners.computeIfAbsent(aligned, k -> new LinkedHashSet<>()).add(SearchVariantRules.ANY_OBJECT);
		}
		for (Map.Entry<String, Integer> e : rules.classes().entrySet()) {
			if (e.getValue() != SearchVariantRules.CLASS_ALWAYS) {
				// a service or frequent word only names the kind of an object
				owners.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>()).add(SearchVariantRules.ANY_OBJECT);
			}
		}
		Map<String, List<Scoped>> penaltyFree = new HashMap<>();
		owners.forEach((word, set) -> {
			List<Scoped> scopes = new ArrayList<>();
			for (String object : set) {
				scopes.add(new Scope(object));
			}
			penaltyFree.put(word, set.contains(SearchVariantRules.ANY_OBJECT) ? EVERY_OWNER : scopes);
		});
		this.buildingWords = rules.buildings();
		this.ignorables = ignorables;
		this.penaltyFree = penaltyFree;
	}

	/** rules locale of the dictionary, "" for the base rules */
	public String locale() {
		return locale;
	}

	/**
	 * A form of a query word: the word of a name it stands for and the owners of names it applies to; a reverse form
	 * leads from a full word to its abbreviation ("street" -> "st").
	 */
	public record QueryForm(String object, String word, boolean reverse) implements Scoped {
		/** true when the form applies to every owner of a name */
		public boolean isUnscoped() {
			return SearchVariantRules.ANY_OBJECT.equals(object);
		}
	}

	/** Only one-word forms can be matched against one name-index atom, the rules load only such rules. */
	public List<QueryForm> getQueryForms(String word) {
		return forms.getOrDefault(SearchAlgorithms.alignChars(word.toLowerCase(Locale.ROOT)), List.of());
	}

	/** @return true when a query word ("12", "2b", "apt", "bis" of the locale) is a part of a house number */
	public boolean likelyPartOfBuilding(String word, Set<String> wordSplit) {
		if (SearchAlgorithms.isNumber2Letters(word) || word.length() == 1 || buildingWords.contains(word)) {
			return true;
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (String w : wordSplit) {
				if (!likelyPartOfBuilding(w, null)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * search-v2: a word of a name that the query does not have does not penalize the object: {@code <skipPenalty>}
	 * for the owner, an ignorable word ({@code to=""}) or a word of {@code <class1>}/{@code <class2>} of the locale
	 *
	 * @param owner owner of the name: street, locality, boundary, postcode, poi
	 */
	public boolean isCommonSkipOtherCnt(String lowerCase, String owner) {
		List<Scoped> scopes = penaltyFree.get(aligned(lowerCase));
		if (scopes == null) {
			return false;
		}
		for (Scoped scope : scopes) {
			if (scope.appliesTo(owner)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * search-v2: an ignorable word ({@code to=""}) of the locale of the map. Search v1 does not read the rules, its
	 * conjunctions are {@link Abbreviations#isConjunction}.
	 */
	public boolean isIgnorable(String lowerCase) {
		return ignorables.contains(aligned(lowerCase));
	}

	// the dictionaries keep aligned words: "école" is "ecole", "straße" is "strasse"; most words of names need no work
	private String aligned(String lowerCase) {
		for (int i = 0; i < lowerCase.length(); i++) {
			char c = lowerCase.charAt(i);
			if (c >= 128 || c == '\'' || c == '`') {
				return SearchAlgorithms.alignChars(lowerCase);
			}
		}
		return lowerCase;
	}

	@Override
	public String toString() {
		return "rules '" + locale + "'";
	}
}
