package net.osmand.binary;

import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Locale of search variant rules ({@code rules_<language>_<COUNTRY>.xml}) for a map and for a name of that map.
 * <p>
 * The rules follow the data, not the user interface: a map is written in the language of its country, and a name
 * tagged {@code name:xx} is written in language xx in the country of the map. The map locale comes from the download
 * name of the map ("Us_new-york_northamerica" is en_US, "Switzerland_ticino_europe" is it_CH); the longest prefix
 * wins. A map this table does not cover gets "" and only the base rules. The table is {@code <locales>} of
 * {@code rules.xml}: it gives every map its rules locale, its statistics group and its transliteration of names.
 * <p>
 * This is not the language group of {@link CommonWordsMultiIndex}: a group ("esl", "nor", "cjk") pools the word
 * statistics of several languages, while a rules locale names one language and one country.
 */
public final class SearchLocales {

	private static final Log LOG = PlatformUtil.getLog(SearchLocales.class);

	private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}");
	private static final Pattern LOCALE_PART = Pattern.compile("[A-Za-z0-9]{2,8}");

	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	// <locales> of rules.xml, read on first use: the rules parser itself normalizes locales with this class
	private static final class Holder {
		static final SearchVariantRules.LocaleTable TABLE = SearchVariantRules.localeTable();
	}

	private SearchLocales() {
	}

	/**
	 * @return rules locale of a map ("Us_new-york_northamerica_2.obf" -> "en_US") or "" when no prefix covers the
	 * map name; accepts a download name, a file name or a path
	 */
	public static String forMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? "" : Holder.TABLE.locale(prefix.toLowerCase(Locale.ROOT));
	}

	/**
	 * @return statistics group of {@code common_words_groups.tsv} of a map ("Russia_moscow_asia" -> "esl"), null when
	 * no prefix with a group covers the map name
	 */
	public static String groupForMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? null : Holder.TABLE.group(prefix.toLowerCase(Locale.ROOT));
	}

	/** @return transliteration of names of a map ("ja", "zh"), null when the map has none */
	public static String translitForMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? null : Holder.TABLE.translit(prefix.toLowerCase(Locale.ROOT));
	}

	/** @return map name prefix (lower case) -> statistics group, for every prefix with a group */
	public static Map<String, String> groupsByPrefix() {
		return Holder.TABLE.groups();
	}

	/** @return map name prefix (lower case) -> transliteration of names, for every prefix with one */
	public static Map<String, String> translitsByPrefix() {
		return Holder.TABLE.translits();
	}

	/**
	 * @return the longest prefix of a map name that has a rules locale, in the case of the name
	 * ("Switzerland_ticino_europe_2.obf" -> "Switzerland_ticino"), null when no prefix covers the map name
	 */
	public static String mapPrefix(String mapName) {
		if (mapName == null) {
			return null;
		}
		String name = mapName;
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		String lower = name.toLowerCase(Locale.ROOT);
		// the longest covering prefix: "switzerland_ticino" before "switzerland"
		for (int end = lower.length(); end > 0; end = lower.lastIndexOf('_', end - 1)) {
			if (Holder.TABLE.hasPrefix(lower.substring(0, end))) {
				return name.substring(0, end);
			}
		}
		return null;
	}

	/**
	 * Locale of one name of an object of a map.
	 *
	 * @param nameTag   language of the name: null or a tag without a language suffix ("alt_name", "official_name")
	 *                  for a name in the language of the map; "de", "name:de" or "alt_name:de-CH" for a name in that
	 *                  language; "int_name" for an international name that follows no local rules
	 * @param mapLocale locale of the map, see {@link #forMap(String)}
	 * @return "" when the name follows only the base rules
	 */
	public static String forName(String nameTag, String mapLocale) {
		String map = normalize(mapLocale);
		if (nameTag == null || nameTag.isEmpty()) {
			return map;
		}
		if (nameTag.equals("int_name")) {
			return "";
		}
		int colon = nameTag.lastIndexOf(':');
		String suffix = colon >= 0 ? nameTag.substring(colon + 1) : nameTag;
		String[] parts = suffix.replace('-', '_').split("_");
		String language = parts[0].toLowerCase(Locale.ROOT);
		boolean tagWithoutLanguage = colon < 0 && suffix.length() > 3;
		if (tagWithoutLanguage || !LANGUAGE.matcher(language).matches()) {
			// "alt_name", "old_name", "name:etymology": the language of the map
			return map;
		}
		if (language.equals(language(map))) {
			return map;
		}
		String country = country(map);
		return country.isEmpty() ? language : language + '_' + country;
	}

	/** @return "en" of "en_US", "" of "" */
	public static String language(String locale) {
		int i = locale == null ? -1 : locale.indexOf('_');
		return locale == null ? "" : i < 0 ? locale : locale.substring(0, i);
	}

	/** @return "US" of "en_US", "" when the locale has no country */
	public static String country(String locale) {
		if (locale == null) {
			return "";
		}
		String[] parts = locale.split("_");
		for (int i = 1; i < parts.length; i++) {
			if (parts[i].length() == 2 || parts[i].length() == 3 && Character.isDigit(parts[i].charAt(0))) {
				return parts[i];
			}
		}
		return "";
	}

	/**
	 * @return canonical form ("en-us" -> "en_US", "zh-hant-tw" -> "zh_Hant_TW"); "" for null, empty or malformed
	 * input, so a bad locale from settings falls back to the base rules instead of failing the search
	 */
	public static String normalize(String locale) {
		if (locale == null || locale.isEmpty()) {
			return "";
		}
		String[] parts = locale.trim().replace('-', '_').split("_");
		StringBuilder normalized = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (!LOCALE_PART.matcher(parts[i]).matches() || (i == 0 && !LANGUAGE.matcher(
					parts[i].toLowerCase(Locale.ROOT)).matches())) {
				if (WARNED.add(locale)) {
					LOG.warn("Invalid search rules locale '" + locale + "', base rules are used");
				}
				return "";
			}
			if (i > 0) {
				normalized.append('_');
			}
			if (i == 0) {
				normalized.append(parts[i].toLowerCase(Locale.ROOT));
			} else if (parts[i].length() == 4) {
				normalized.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT))
						.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
			} else {
				normalized.append(parts[i].toUpperCase(Locale.ROOT));
			}
		}
		return normalized.toString();
	}
}
