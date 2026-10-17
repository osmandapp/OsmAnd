package net.osmand.osm;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Labels of additional property values written as "Name: value" in phrases.xml ("Fridge: yes",
 * "Smoking: not allowed", "Kayaks: yes"). A translation without the separator is shown as it is.
 */
public class PoiAdditionalLabels {

	public static final String YES = "yes";

	// ':' and the full-width forms used by the CJK translations, French puts spaces around it
	private static final Pattern NAME_VALUE_SEPARATOR = Pattern.compile("\\s*[:\uFF1A\uFE30]\\s*");

	/**
	 * @return {name, value} or null if the translation is not "Name: value"
	 */
	public static String[] splitNameValue(String translation) {
		if (Algorithms.isEmpty(translation)) {
			return null;
		}
		Matcher matcher = NAME_VALUE_SEPARATOR.matcher(translation);
		if (!matcher.find() || matcher.start() == 0 || matcher.end() == translation.length()) {
			return null;
		}
		return new String[] {translation.substring(0, matcher.start()).trim(), translation.substring(matcher.end()).trim()};
	}

	/**
	 * A group of one "yes" value is a single switch, not a list.
	 */
	public static boolean isSingleYes(List<String> osmValues) {
		return osmValues.size() == 1 && YES.equals(osmValues.get(0));
	}

	/**
	 * Label of the switch for a group of one "yes" value: the name, else the group name.
	 */
	public static String getSwitchLabel(String translation, String groupName) {
		String[] nameValue = splitNameValue(translation);
		if (nameValue != null) {
			return nameValue[0];
		}
		return Algorithms.isEmpty(groupName) ? translation : groupName;
	}

	/**
	 * Labels of the values of one group, in the same order:
	 * - all values have the same name ("Smoking: allowed", "Smoking: not allowed"): the value, "Allowed";
	 * - otherwise ("Kayaks: yes", "Canoe: yes"): the name for a "yes" value whose name is not repeated in the group,
	 * "Kayaks", and the whole translation for the others, "Adoption: no".
	 */
	public static List<String> getGroupLabels(List<String> translations, List<String> osmValues) {
		int size = translations.size();
		List<String[]> nameValues = new ArrayList<>(size);
		Map<String, Integer> nameCounts = new HashMap<>();
		boolean sameName = size > 0;
		String firstName = null;
		for (String translation : translations) {
			String[] nameValue = splitNameValue(translation);
			nameValues.add(nameValue);
			if (nameValue == null) {
				sameName = false;
				continue;
			}
			String name = nameValue[0].toLowerCase(Locale.ROOT);
			nameCounts.merge(name, 1, Integer::sum);
			if (firstName == null) {
				firstName = name;
			} else if (!firstName.equals(name)) {
				sameName = false;
			}
		}
		List<String> labels = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			String translation = translations.get(i);
			String[] nameValue = nameValues.get(i);
			if (nameValue == null) {
				labels.add(translation);
			} else if (sameName) {
				labels.add(Algorithms.capitalizeFirstLetter(nameValue[1]));
			} else if (YES.equals(osmValues.get(i)) && nameCounts.get(nameValue[0].toLowerCase(Locale.ROOT)) == 1) {
				labels.add(nameValue[0]);
			} else {
				labels.add(translation);
			}
		}
		return labels;
	}
}
