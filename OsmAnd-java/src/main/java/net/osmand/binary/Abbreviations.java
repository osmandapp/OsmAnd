package net.osmand.binary;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import static net.osmand.search.core.SearchPhrase.DELIMITER;

public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, String> abbreviations = new TreeMap<>();

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
        return abbreviations.containsKey(word.toLowerCase()) ?
                abbreviations.get(word.toLowerCase()) : word;
    }

    public static String replaceAll(String phrase) {
        String[] words = phrase.split(DELIMITER);
        ArrayList<String> result = new ArrayList<>();
        for (String word : words) {
            result.add(replace(word));
        }
        return StringUtils.join(result, DELIMITER);
    }
}
