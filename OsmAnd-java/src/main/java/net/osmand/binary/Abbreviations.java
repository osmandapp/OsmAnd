package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;

import java.util.HashMap;
import java.util.Map;


public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, String> abbreviations = new HashMap<>();

    static {
        abbreviations.put("e", "East");
        abbreviations.put("w", "West");
        abbreviations.put("s", "South");
        abbreviations.put("n", "North");
        abbreviations.put("sw", "Southwest");
        abbreviations.put("se", "Southeast");
        abbreviations.put("nw", "Northwest");
        abbreviations.put("ne", "Northeast");
        abbreviations.put("ln", "Lane");
    }

    public static String replace(String word) {
        String value = abbreviations.get(word.toLowerCase());
        return value != null ? value : word;
    }

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
}
