package net.osmand.osm;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Labels of additional property values written as "Name: value" in phrases.xml ("Fridge: yes",
 * "Smoking: not allowed", "Kayaks: yes"). A translation without the separator is shown as it is.
 */
public class PoiAdditionalLabels {

	public static final String YES = "yes";
	// values that mean the property is there; search finds an additional value only by one of them
	private static final Set<String> ACCEPTED_VALUES = Set.of(YES, "only", "designated");

	/**
	 * @return {name, value} or null if the translation is not "Name: value"
	 */
	public static String[] splitNameValue(String translation) {
		if (Algorithms.isEmpty(translation)) {
			return null;
		}
		int index = translation.indexOf(':');
		if (index < 0) {
			return null;
		}
		String name = translation.substring(0, index).trim();
		String value = translation.substring(index + 1).trim();
		// a name has letters: "1:25 fuel" is a value, not "1" + "25 fuel"
		return !hasLetter(name) || value.isEmpty() ? null : new String[] {name, value};
	}

	private static boolean hasLetter(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isLetter(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Search shows and indexes accepted values only (yes, only, designated): by the OSM value, or by the value of
	 * the English translation "Name: yes" ("Subway station: yes" for station=subway) or the older "Yes;Name".
	 */
	public static boolean isAcceptedValue(String osmValue, String enTranslation) {
		if (osmValue != null && ACCEPTED_VALUES.contains(osmValue)) {
			return true;
		}
		if (enTranslation == null) {
			return false;
		}
		String[] nameValue = splitNameValue(enTranslation);
		String value = nameValue != null ? nameValue[1] : enTranslation;
		return ACCEPTED_VALUES.contains(value.toLowerCase(Locale.ROOT));
	}

	public static boolean isAcceptedValue(PoiType poiType) {
		return isAcceptedValue(poiType.getOsmValue(), poiType.getEnTranslation());
	}

	/**
	 * Name of an additional value in search, the part before ":" ("Subway station: yes" -> "Subway station").
	 * The older "Yes;Subway station" gives the first synonym, a translation without a name stays as it is.
	 */
	public static String getSearchName(String translation, String enTranslation, String synonyms, String groupName) {
		String[] nameValue = splitNameValue(translation);
		if (nameValue != null) {
			return nameValue[0];
		}
		if (YES.equalsIgnoreCase(enTranslation)) {
			if (!Algorithms.isEmpty(synonyms)) {
				String first = synonyms.split(";")[0].trim();
				if (!first.isEmpty()) {
					return first;
				}
			}
			if (!Algorithms.isEmpty(groupName)) {
				return groupName;
			}
		}
		return translation;
	}

	public static String getSearchName(AbstractPoiType poiType) {
		if (!poiType.isAdditional()) {
			return poiType.getTranslation();
		}
		return getSearchName(poiType.getTranslation(), poiType.getEnTranslation(), poiType.getSynonyms(),
				poiType.getPoiAdditionalCategoryTranslation());
	}

	public static String getEnSearchName(AbstractPoiType poiType) {
		String enTranslation = poiType.getEnTranslation();
		if (!poiType.isAdditional()) {
			return enTranslation;
		}
		String[] nameValue = splitNameValue(enTranslation);
		if (nameValue != null) {
			return nameValue[0];
		}
		// the older "Yes;Name" has no English name, only the value
		return YES.equalsIgnoreCase(enTranslation) ? null : enTranslation;
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
