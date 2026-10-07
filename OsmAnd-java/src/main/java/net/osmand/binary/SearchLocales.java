package net.osmand.binary;

import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Locale of search variant rules ({@code rules_<language>_<COUNTRY>.xml}) for a map and for a name of that map:
 * {@code <locales>} of {@code rules.xml}, read by {@link SearchRules}.
 * <p>
 * The rules follow the data, not the user interface: a map is written in the language of its country, and a name
 * tagged {@code name:xx} is written in language xx in the country of the map. The map locale comes from the download
 * name of the map ("Us_new-york_northamerica" is en_US, "Switzerland_ticino_europe" is it_CH); the longest prefix
 * wins. A map this table does not cover gets "" and only the base rules. The table gives every map its rules locale,
 * its statistics group and its transliteration of names.
 * <p>
 * This is not the language group of {@link CommonWordsMultiIndex}: a group ("esl", "nor", "cjk") pools the word
 * statistics of several languages, while a rules locale names one language and one country.
 */
public final class SearchLocales {

	private static final Log LOG = PlatformUtil.getLog(SearchLocales.class);

	private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}");
	private static final Pattern LOCALE_PART = Pattern.compile("[A-Za-z0-9]{2,8}");
	private static final Pattern GROUP_ID = Pattern.compile("[a-z]{2,4}");
	private static final Pattern MAP_PREFIX = Pattern.compile("[a-z0-9-]+(_[a-z0-9-]+)*");
	private static final Set<String> TRANSLITS = Set.of("ja", "zh");

	private final Set<String> warned = ConcurrentHashMap.newKeySet();

	// filled by the parser of rules.xml (addGroup, addMap, build), read only afterwards
	private final Map<String, String> localeByPrefix = new LinkedHashMap<>();
	private final Map<String, String> groupByPrefix = new LinkedHashMap<>();
	private final Map<String, String> translitByPrefix = new LinkedHashMap<>();
	private final Set<String> groupIds = new LinkedHashSet<>();
	private final Map<String, String> groupByLanguage = new LinkedHashMap<>();
	private final Map<String, String> groupByLocale = new LinkedHashMap<>();
	private final Map<String, String> explicitGroup = new LinkedHashMap<>();
	private boolean built;

	SearchLocales() {
	}

	/**
	 * @return rules locale of a map ("Us_new-york_northamerica_2.obf" -> "en_US") or "" when no prefix covers the
	 * map name; accepts a download name, a file name or a path
	 */
	public String forMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? "" : localeByPrefix.get(prefix.toLowerCase(Locale.ROOT));
	}

	/**
	 * @return statistics group of {@code common_words_groups.tsv} of a map ("Russia_moscow_asia" -> "esl"), null when
	 * no prefix with a group covers the map name
	 */
	public String groupForMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? null : groupByPrefix.get(prefix.toLowerCase(Locale.ROOT));
	}

	/** @return transliteration of names of a map ("ja", "zh"), null when the map has none */
	public String translitForMap(String mapName) {
		String prefix = mapPrefix(mapName);
		return prefix == null ? null : translitByPrefix.get(prefix.toLowerCase(Locale.ROOT));
	}

	/** @return every statistics group of {@code <group>}, a map uses it or not */
	public Set<String> groupIds() {
		return Collections.unmodifiableSet(groupIds);
	}

	/** @return map name prefix (lower case) -> statistics group, for every prefix with a group */
	public Map<String, String> groupsByPrefix() {
		return Collections.unmodifiableMap(groupByPrefix);
	}

	/** @return map name prefix (lower case) -> transliteration of names, for every prefix with one */
	public Map<String, String> translitsByPrefix() {
		return Collections.unmodifiableMap(translitByPrefix);
	}

	/**
	 * @return the longest prefix of a map name that has a rules locale, in the case of the name
	 * ("Switzerland_ticino_europe_2.obf" -> "Switzerland_ticino"), null when no prefix covers the map name
	 */
	public String mapPrefix(String mapName) {
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
			if (localeByPrefix.containsKey(lower.substring(0, end))) {
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
	public String forName(String nameTag, String mapLocale) {
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
	public String language(String locale) {
		int i = locale == null ? -1 : locale.indexOf('_');
		return locale == null ? "" : i < 0 ? locale : locale.substring(0, i);
	}

	/** @return "US" of "en_US", "" when the locale has no country */
	public String country(String locale) {
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
	public String normalize(String locale) {
		if (locale == null || locale.isEmpty()) {
			return "";
		}
		String[] parts = locale.trim().replace('-', '_').split("_");
		StringBuilder normalized = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (!LOCALE_PART.matcher(parts[i]).matches() || (i == 0 && !LANGUAGE.matcher(
					parts[i].toLowerCase(Locale.ROOT)).matches())) {
				if (warned.add(locale)) {
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

	boolean isBuilt() {
		return built;
	}

	/** {@code <group id languages locales>} */
	void addGroup(String id, List<String> languages, List<String> locales, String where) {
		if (id == null || !GROUP_ID.matcher(id).matches()) {
			throw new IllegalArgumentException("A group id is 2-4 lower-case letters" + where);
		}
		if (!groupIds.add(id)) {
			throw new IllegalArgumentException("Duplicate group" + where);
		}
		for (String language : languages) {
			if (!LANGUAGE.matcher(language).matches()) {
				throw new IllegalArgumentException("Invalid language '" + language + "'" + where);
			}
			if (groupByLanguage.put(language, id) != null) {
				throw new IllegalArgumentException("The language '" + language + "' is in two groups" + where);
			}
		}
		for (String locale : locales) {
			String normalized = normalize(locale);
			if (normalized.isEmpty() || country(normalized).isEmpty()) {
				throw new IllegalArgumentException("Invalid locale '" + locale + "', expected a language and a "
						+ "country" + where);
			}
			if (groupByLocale.put(normalized, id) != null) {
				throw new IllegalArgumentException("The locale '" + locale + "' is in two groups" + where);
			}
		}
	}

	/** {@code <map locale prefixes group translit>} */
	void addMap(String locale, List<String> prefixes, String group, String translit, String where) {
		if (locale == null) {
			throw new IllegalArgumentException("Missing locale" + where);
		}
		String normalized = normalize(locale);
		if (!locale.isEmpty() && normalized.isEmpty()) {
			throw new IllegalArgumentException("Invalid locale '" + locale + "'" + where);
		}
		if (translit != null && !TRANSLITS.contains(translit)) {
			throw new IllegalArgumentException("Unknown translit '" + translit + "', expected one of " + TRANSLITS
					+ where);
		}
		if (prefixes.isEmpty()) {
			throw new IllegalArgumentException("Missing prefixes" + where);
		}
		for (String prefix : prefixes) {
			if (!MAP_PREFIX.matcher(prefix).matches()) {
				throw new IllegalArgumentException("A map prefix is lower case: '" + prefix + "'" + where);
			}
			if (localeByPrefix.put(prefix, normalized) != null) {
				throw new IllegalArgumentException("Duplicate map prefix '" + prefix + "'" + where);
			}
			if (group != null) {
				explicitGroup.put(prefix, group);
			}
			if (translit != null) {
				translitByPrefix.put(prefix, translit);
			}
		}
	}

	/** the end of {@code <locales>}: the group of every map prefix */
	void build(String file) {
		for (Map.Entry<String, String> e : localeByPrefix.entrySet()) {
			String group = explicitGroup.get(e.getKey());
			if (group != null && !groupIds.contains(group)) {
				throw new IllegalArgumentException("Unknown group '" + group + "' of the map prefix '"
						+ e.getKey() + "' in " + file);
			}
			if (group == null) {
				group = groupByLocale.get(e.getValue());
			}
			if (group == null) {
				group = groupByLanguage.get(language(e.getValue()));
			}
			if (group != null) {
				groupByPrefix.put(e.getKey(), group);
			}
		}
		built = true;
	}
}
