package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;
import net.osmand.util.SearchAlgorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, String> abbreviations = new HashMap<>();
    // 2nd version search abbrevations for spatial search
    private static final Map<String, String> searchAbbreviations = new HashMap<>();
    // set of words to check for buidlings
    private static final Map<String, String> buildingAbbreviations = new HashMap<>();
	private static final Set<String> conjunctions = new TreeSet<>();
	
	private static final Set<String> commonSkipOtherCnt = new TreeSet<>();

	private static void addDirectionWord(String key, String full) {
		abbreviations.put(key, full);
		commonSkipOtherCnt.add(key);
		commonSkipOtherCnt.add(full.toLowerCase());
	}

	private static void addStreetStatus(String key, String full) {
		abbreviations.put(key, full);
		commonSkipOtherCnt.add(key);
		commonSkipOtherCnt.add(full.toLowerCase());
	}

	private static void addConjunction(String key) {
		conjunctions.add(key);
		commonSkipOtherCnt.add(key);
	}

	static {
		// articles
		addConjunction("the");
		addConjunction("de");
		addConjunction("du");
		addConjunction("der");
		addConjunction("den");
		addConjunction("die");
		addConjunction("das");
		addConjunction("la");
		addConjunction("le");
		addConjunction("el");
		addConjunction("il");
		addConjunction("of");

		// and
		addConjunction("and");
		addConjunction("und");
		addConjunction("en");
		addConjunction("et");
		addConjunction("y");
		addConjunction("и");

		// direction
		addDirectionWord("e", "East");
		addDirectionWord("w", "West");
		addDirectionWord("s", "South");
		addDirectionWord("n", "North");
		addDirectionWord("sw", "Southwest");
		addDirectionWord("se", "Southeast");
		addDirectionWord("nw", "Northwest");
		addDirectionWord("ne", "Northeast");

		// street status
		addStreetStatus("ln", "Lane");
		addStreetStatus("dr", "Drive");
		addStreetStatus("rd", "Road");
		addStreetStatus("av", "Avenue");
		addStreetStatus("st", "Street"); // 2 values could be saint
		addStreetStatus("hwy", "Highway");
		addStreetStatus("blvd", "Boulevard");
	}

	static {
		searchAbbreviations.putAll(abbreviations);
		searchAbbreviations.put("ave", "Avenue"); // extra
		searchAbbreviations.put("st", "Street Saint"); // 2 values could be saint
		// duplicates - synonyms and not abbrevations actually
		searchAbbreviations.put("о", "Остров");
		searchAbbreviations.put("остров", "о.");
		searchAbbreviations.put("1st", "First");
		searchAbbreviations.put("2nd", "Second");
		searchAbbreviations.put("3rd", "Third");
		searchAbbreviations.put("first", "1st");
		searchAbbreviations.put("second", "2nd");
		searchAbbreviations.put("third", "3rd");
	}

	// common housenumber additions
	static {
		// french
		buildingAbbreviations.put("bis", "Bis");
		buildingAbbreviations.put("ter", "Ter");
		buildingAbbreviations.put("quater", "Quater");
		// american
		buildingAbbreviations.put("bldg", "Building");
		buildingAbbreviations.put("ste", "Suite");
		buildingAbbreviations.put("unt", "Unit");
		buildingAbbreviations.put("apt", "Apartment");
		buildingAbbreviations.put("fl", "Floor");
		buildingAbbreviations.put("flr", "Floor");
		buildingAbbreviations.put("bsmt", "Basement");
	}

	// search v-2
	public static boolean likelyPartOfBuilding(String word, Set<String> wordSplit) {
		boolean bldNum = (SearchAlgorithms.isNumber2Letters(word) || word.length() == 1
				|| buildingAbbreviations.containsKey(word));
		if (bldNum) {
			return true;
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (String w : wordSplit) {
				boolean likely = likelyPartOfBuilding(w, null);
				if (!likely) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
    
    
    // search-v2
    public static Map<String, String> getSearchabbreviations() {
		return searchAbbreviations;
	}
    
    // search-v2
 	public static boolean isCommonSkipOtherCnt(String lowerCase) {
 		return commonSkipOtherCnt.contains(lowerCase);
 	}

    // Indexing data
    public static String replaceAll(String phrase) {
        String[] words = phrase.split(SearchPhrase.DELIMITER);
        StringBuilder r = new StringBuilder();
        boolean changed = false;
        for (String w : words) {
            if (r.length() > 0) {
                r.append(SearchPhrase.DELIMITER);
            }
            String abbrRes = abbreviations.get(w.toLowerCase());
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
    public static Map<String, String> getAbbreviations() {
		return abbreviations;
	}

	// search v-1
    public static String replace(String word) {
        String value = abbreviations.get(word.toLowerCase());
        return value != null ? value : word;
    }
    
    // search-v1
	public static boolean isConjunction(String lowerCase) {
		return conjunctions.contains(lowerCase);
	}
	
    
}
