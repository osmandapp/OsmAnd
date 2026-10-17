package net.osmand.osm;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Labels of additional property values written as "Name: value" in phrases.xml ("Fridge: yes",
 * "Smoking: not allowed", "Kayaks: yes"). A translation without the separator is shown as it is.
 */
public class PoiAdditionalLabels {

	public static final String YES = "yes";

	// ':' and the full-width forms used by the CJK translations
	private static final String NAME_VALUE_SEPARATORS = ":\uFF1A\uFE30";

	/**
	 * @return {name, value} or null if the translation is not "Name: value"
	 */
	public static String[] splitNameValue(String translation) {
		if (Algorithms.isEmpty(translation)) {
			return null;
		}
		for (int i = 0; i < translation.length(); i++) {
			if (NAME_VALUE_SEPARATORS.indexOf(translation.charAt(i)) >= 0) {
				String name = translation.substring(0, i).trim();
				String value = translation.substring(i + 1).trim();
				return name.isEmpty() || value.isEmpty() ? null : new String[] {name, value};
			}
		}
		return null;
	}

	/**
	 * Search shows and indexes "yes" values only: OSM value yes, or an English translation "Name: yes"
	 * ("Subway station: yes" for station=subway) or the older "Yes;Name".
	 */
	public static boolean isYesValue(String osmValue, String enTranslation) {
		if (YES.equals(osmValue)) {
			return true;
		}
		String[] nameValue = splitNameValue(enTranslation);
		String value = nameValue != null ? nameValue[1] : enTranslation;
		return YES.equalsIgnoreCase(value);
	}

	public static boolean isYesValue(PoiType poiType) {
		return isYesValue(poiType.getOsmValue(), poiType.getEnTranslation());
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
