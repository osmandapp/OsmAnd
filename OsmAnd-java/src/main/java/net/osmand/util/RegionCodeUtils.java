package net.osmand.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Short readable codes for region download names to pass a list of maps in an url parameter.
 * Code = first letters of all name words except the last one + as many letters of the last word
 * as needed to be unique in the given list. The last name part (continent) is dropped.
 * More general regions are processed first and get the shorter code.
 * <p>
 * us_northamerica -> us, ukraine_sumy_europe -> usu, germany_bayern_europe -> gba
 */
public class RegionCodeUtils {

	public static final String MAPS_DELIMITER = ",";

	public static String encode(Collection<String> selected, Collection<String> downloadNames) {
		Map<String, String> codes = buildCodes(downloadNames);
		List<String> res = new ArrayList<>();
		for (String name : selected) {
			String code = codes.get(name.toLowerCase());
			if (code != null && !res.contains(code)) {
				res.add(code);
			}
		}
		return String.join(MAPS_DELIMITER, res);
	}

	public static Set<String> decode(String codes, Collection<String> downloadNames) {
		Set<String> res = new LinkedHashSet<>();
		if (Algorithms.isEmpty(codes)) {
			return res;
		}
		List<String> requested = List.of(codes.split(MAPS_DELIMITER));
		for (Map.Entry<String, String> entry : buildCodes(downloadNames).entrySet()) {
			if (requested.contains(entry.getValue())) {
				res.add(entry.getKey());
			}
		}
		return res;
	}

	public static Map<String, String> buildCodes(Collection<String> downloadNames) {
		List<String> names = new ArrayList<>();
		for (String name : downloadNames) {
			names.add(name.toLowerCase());
		}
		names.sort(Comparator.comparingInt((String name) -> getWords(name).size()).thenComparing(name -> name));

		Map<String, String> codes = new LinkedHashMap<>();
		Set<String> used = new HashSet<>();
		for (String name : names) {
			for (String code : getCandidates(name)) {
				if (used.add(code)) {
					codes.put(name, code);
					break;
				}
			}
		}
		return codes;
	}

	private static List<String> getCandidates(String name) {
		List<String> words = getWords(name);
		List<String> candidates = new ArrayList<>();
		StringBuilder head = new StringBuilder();
		for (int i = 0; i < words.size() - 1; i++) {
			head.append(words.get(i).charAt(0));
		}
		String last = words.get(words.size() - 1);
		for (int i = 1; i <= last.length(); i++) {
			candidates.add(head + last.substring(0, i));
		}
		candidates.add(getRegion(name));
		return candidates;
	}

	private static List<String> getWords(String name) {
		List<String> words = new ArrayList<>();
		for (String word : getRegion(name).split("[_-]")) {
			if (!word.isEmpty()) {
				words.add(word);
			}
		}
		return words;
	}

	private static String getRegion(String name) {
		int continent = name.lastIndexOf('_');
		return continent > 0 ? name.substring(0, continent) : name;
	}
}
