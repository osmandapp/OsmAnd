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

    static {
    	// articles
    	conjunctions.add("the");
		conjunctions.add("der");
		conjunctions.add("den");
		conjunctions.add("die");
		conjunctions.add("das");
		conjunctions.add("la");
		conjunctions.add("le");
		conjunctions.add("el");
		conjunctions.add("il");
		// and
		conjunctions.add("and");
		conjunctions.add("und");
		conjunctions.add("en");
		conjunctions.add("et");
		conjunctions.add("y");
		conjunctions.add("и");
    }
    
    private static void addAbbrDirStatus(String key, String full) {
    	abbreviations.put(key, full);
    	commonSkipOtherCnt.add(key);
    	commonSkipOtherCnt.add(full.toLowerCase());
    }
    static {
    	addAbbrDirStatus("e", "East");
    	addAbbrDirStatus("w", "West");
    	addAbbrDirStatus("s", "South");
    	addAbbrDirStatus("n", "North");
    	addAbbrDirStatus("sw", "Southwest");
    	addAbbrDirStatus("se", "Southeast");
    	addAbbrDirStatus("nw", "Northwest");
    	addAbbrDirStatus("ne", "Northeast");
    	addAbbrDirStatus("ln", "Lane");
    	addAbbrDirStatus("dr", "Drive");
    	addAbbrDirStatus("rd", "Road");
    	addAbbrDirStatus("av", "Avenue");
    	addAbbrDirStatus("st", "Street"); // 2 values could be saint
    	addAbbrDirStatus("hwy", "Highway");
    	addAbbrDirStatus("blvd", "Boulevard");
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
    
    public static String replace(String word) {
        String value = abbreviations.get(word.toLowerCase());
        return value != null ? value : word;
    }
    
    public static Map<String, String> getAbbreviations() {
		return abbreviations;
	}
    
    public static Map<String, String> getSearchabbreviations() {
		return searchAbbreviations;
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

	public static boolean isConjunction(String lowerCase) {
		return conjunctions.contains(lowerCase);
	}
	
	public static boolean isCommonSkipOtherCnt(String lowerCase) {
		return commonSkipOtherCnt.contains(lowerCase);
	}
}
