package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;
import net.osmand.util.SearchAlgorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, Dictionary> DICTIONARIES = new ConcurrentHashMap<>();

	private static Dictionary dictionary(String locale) {
		Objects.requireNonNull(locale, "locale");
		return DICTIONARIES.computeIfAbsent(locale, key -> new Dictionary(SearchVariantRules.forLocale(key)));
	}

	private static final class Dictionary {
		final Map<String, String> abbreviations = new HashMap<>();
		final Map<String, String> searchAbbreviations = new HashMap<>();
		final Set<String> buildingAbbreviations = new TreeSet<>();
		final Set<String> conjunctions = new TreeSet<>();
		final Set<String> commonSkipOtherCnt = new TreeSet<>();

		Dictionary(SearchVariantRules rules) {
			for (SearchVariantRules.Entry entry : rules.indexEntries()) {
				abbreviations.put(entry.key, entry.value);
				commonSkipOtherCnt.add(entry.key);
				commonSkipOtherCnt.add(entry.value.toLowerCase(Locale.ROOT));
			}
			searchAbbreviations.putAll(abbreviations);
			for (SearchVariantRules.Entry entry : rules.queryEntries()) {
				switch (entry.kind) {
					case "search" -> searchAbbreviations.put(entry.key, entry.value);
					case "building" -> buildingAbbreviations.add(entry.key);
					case "conjunction" -> {
						conjunctions.add(entry.key);
						commonSkipOtherCnt.add(entry.key);
					}
				}
			}
		}
	}

	public static boolean likelyPartOfRef(String word, Set<String> wordSplit) {
		int limit = 2;
		int letters = SearchAlgorithms.letters(word, limit + 1);
		if (letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(word))) {
			return true;
		}
		for (String s : wordSplit) {
			letters = SearchAlgorithms.letters(s, limit + 1);
			if (!(letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(s)))) {
				return false;
			}
		}
		return true;
	}
	
	// search v-2
	public static boolean likelyPartOfBuilding(String word, Set<String> wordSplit, String locale) {
		Dictionary rules = dictionary(locale);
		boolean bldNum = (SearchAlgorithms.isNumber2Letters(word) || word.length() == 1
				|| rules.buildingAbbreviations.contains(word));
		if (bldNum) {
			return true;
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (String w : wordSplit) {
				boolean likely = likelyPartOfBuilding(w, null, locale);
				if (!likely) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
    
    
    // search-v2
    public static Map<String, String> getSearchabbreviations(String locale) {
		return dictionary(locale).searchAbbreviations;
	}

	public record QueryForm(SearchVariantRules.Variant rule, String word) {
	}

	/** Only one-word replacements can be matched against one name-index atom. */
	public static List<QueryForm> getQueryForms(String word, String locale) {
		Objects.requireNonNull(locale, "locale");
		List<QueryForm> forms = new ArrayList<>();
		for (SearchVariantRules.Variant rule : SearchVariantRules.forLocale(locale).query()) {
			String replacement = rule.apply(word);
			if (replacement != null) {
				List<String> words = SearchAlgorithms.splitAndNormalize(replacement, false);
				if (words.size() == 1 && !words.get(0).equals(word)) {
					forms.add(new QueryForm(rule, words.get(0)));
				}
			}
		}
		return forms;
	}
    
    // search-v2
	public static boolean isCommonSkipOtherCnt(String lowerCase, String locale) {
		return dictionary(locale).commonSkipOtherCnt.contains(lowerCase);
	}

    // Indexing data
    public static String replaceAll(String phrase, String locale) {
        Map<String, String> abbreviations = dictionary(locale).abbreviations;
        String[] words = phrase.split(SearchPhrase.DELIMITER);
        StringBuilder r = new StringBuilder();
        boolean changed = false;
        for (String w : words) {
            if (r.length() > 0) {
                r.append(SearchPhrase.DELIMITER);
            }
            String abbrRes = abbreviations.get(w.toLowerCase(Locale.ROOT));
            if (abbrRes == null) {
                r.append(w);
            } else {
                changed = true;
                r.append(abbrRes);
            }
        }
        return changed ? r.toString() : phrase;
    }
    
	// search-v1
    public static Map<String, String> getAbbreviations(String locale) {
		return dictionary(locale).abbreviations;
	}

	// search v-1
	public static String replace(String word, String locale) {
		String value = dictionary(locale).abbreviations.get(word.toLowerCase(Locale.ROOT));
        return value != null ? value : word;
    }
    
    // search-v1
	public static boolean isConjunction(String lowerCase, String locale) {
		return dictionary(locale).conjunctions.contains(lowerCase);
	}
	
    
}
