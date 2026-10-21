package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, String> abbreviations = new HashMap<>();
	private static final Set<String> conjunctions = new TreeSet<>();

	private static void addDirectionWord(String key, String full) {
		abbreviations.put(key, full);
	}

	private static void addStreetStatus(String key, String full) {
		abbreviations.put(key, full);
	}

	private static void addConjunction(String key) {
		conjunctions.add(key);
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
