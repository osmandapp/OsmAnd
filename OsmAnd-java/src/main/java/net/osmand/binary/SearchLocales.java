package net.osmand.binary;

import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

import java.util.HashMap;
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
 * wins. A map this table does not cover gets "" and only the base rules.
 * <p>
 * This is not the language group of {@link CommonWordsMultiIndex}: a group ("esl", "nor", "cjk") pools the word
 * statistics of several languages, while a rules locale names one language and one country.
 */
public final class SearchLocales {

	private static final Log LOG = PlatformUtil.getLog(SearchLocales.class);

	private static final Pattern LANGUAGE = Pattern.compile("[a-z]{2,3}");
	private static final Pattern LOCALE_PART = Pattern.compile("[A-Za-z0-9]{2,8}");

	// locale, then the map name prefixes it covers; a locale without a country is used for maps of several countries
	private static final String[][] MAP_LOCALES = {
		{ "fr_FR", "france" }, { "fr_BE", "belgium_wallonia" }, { "fr_LU", "luxembourg" }, { "fr_MC", "monaco" },
		{ "fr_CA", "canada_quebec" }, { "fr_CH", "switzerland_lake-geneva" }, { "fr_RE", "reunion" },
		{ "fr_GP", "guadeloupe" }, { "fr_MQ", "martinique" }, { "fr_YT", "mayotte" }, { "fr_GF", "french-guiana" },
		{ "fr_BL", "saint-barthelemy" }, { "fr_MF", "saint-martin" }, { "fr_PM", "saint-pierre-and-miquelon" },
		{ "fr_TF", "french-southern-and-antarctic-lands" }, { "fr_HT", "haiti" }, { "fr_SN", "senegal" },
		{ "fr_CI", "ivory-coast" }, { "fr_ML", "mali" }, { "fr_BF", "burkina-faso" }, { "fr_NE", "niger" },
		{ "fr_GN", "guinea" }, { "fr_BJ", "benin" }, { "fr_TG", "togo" }, { "fr_CM", "cameroon" },
		{ "fr_GA", "gabon" }, { "fr_CG", "congo-brazzaville" }, { "fr_CD", "congo-democratic-republic" },
		{ "fr_CF", "central-african-republic" }, { "fr_TD", "chad" }, { "fr_MG", "madagascar" },
		{ "fr_KM", "comoros" }, { "fr_DJ", "djibouti" }, { "fr_BI", "burundi" }, { "fr_RW", "rwanda" },
		{ "fr_SC", "seychelles" }, { "fr_MU", "mauritius" },

		{ "en_US", "us" }, { "en_GB", "gb" }, { "en_IE", "ireland" }, { "en_IM", "isle-of-man" },
		{ "en", "channel-islands", "oceania", "carribean-archipelago-all" }, { "en_AU", "australia-oceania" },
		{ "en_NZ", "new-zealand" }, { "en_CA", "canada" }, { "en_IN", "india" }, { "en_ZA", "south-africa" },
		{ "en_NG", "nigeria" }, { "en_GH", "ghana" }, { "en_KE", "kenya" }, { "en_UG", "uganda" },
		{ "en_TZ", "tanzania" }, { "en_ZM", "zambia" }, { "en_ZW", "zimbabwe" }, { "en_MW", "malawi" },
		{ "en_BW", "botswana" }, { "en_NA", "namibia" }, { "en_LS", "lesotho" }, { "en_SZ", "swaziland" },
		{ "en_LR", "liberia" }, { "en_SL", "sierra-leone" }, { "en_GM", "gambia" }, { "en_JM", "jamaica" },
		{ "en_BS", "bahamas" }, { "en_BB", "barbados" }, { "en_TT", "trinidad-and-tobago" }, { "en_BZ", "belize" },
		{ "en_GY", "guyana" }, { "en_BM", "bermuda" }, { "en_KY", "cayman-islands" }, { "en_VI", "virgin-islands-us" },
		{ "en_VG", "virgin-islands-british" }, { "en_TC", "turks-and-caicos-islands" }, { "en_AI", "anguilla" },
		{ "en_AG", "antigua-and-barbuda" }, { "en_DM", "dominica" }, { "en_GD", "grenada" },
		{ "en_KN", "saint-kitts-and-nevis" }, { "en_LC", "saint-lucia" },
		{ "en_VC", "saint-vincent-and-the-grenadines" }, { "en_MS", "montserrat" }, { "en_FK", "falkland-islands" },
		{ "en_SH", "saint-helena-ascension-and-tristan-da-cunha" }, { "en_IO", "british-indian-ocean-territory" },
		{ "en_GS", "south-georgia-and-south-sandwich-islands" }, { "en_MT", "malta" }, { "en_SG", "singapore" },
		{ "en_PH", "philippines" }, { "en_PG", "papua-new-guinea" }, { "en_CX", "christmas-island" },
		{ "en_SS", "south-sudan" },

		{ "de_DE", "germany" }, { "de_AT", "austria" }, { "de_LI", "liechtenstein" }, { "de_CH", "switzerland" },

		{ "nl_NL", "netherlands" }, { "nl_BE", "belgium_flanders" }, { "nl", "netherlands-antilles" },
		{ "nl_AW", "aruba" }, { "nl_SR", "suriname" },

		{ "es_ES", "spain" }, { "ca_AD", "andorra" }, { "es_MX", "mexico" }, { "es_PE", "peru" },
		{ "es_AR", "argentina" }, { "es_CL", "chile" }, { "es_CO", "colombia" }, { "es_VE", "venezuela" },
		{ "es_EC", "ecuador" }, { "es_BO", "bolivia" }, { "es_PY", "paraguay" }, { "es_UY", "uruguay" },
		{ "es_CU", "cuba" }, { "es_DO", "dominican-republic" }, { "es_PR", "puerto-rico" }, { "es_GT", "guatemala" },
		{ "es_HN", "honduras" }, { "es_SV", "el-salvador" }, { "es_NI", "nicaragua" }, { "es_CR", "costa-rica" },
		{ "es_PA", "panama" }, { "es_GQ", "equatorial-guinea" },

		{ "pt_PT", "portugal", "azores", "madeira" }, { "pt_BR", "brazil" }, { "pt_AO", "angola" },
		{ "pt_MZ", "mozambique" }, { "pt_CV", "cape-verde" }, { "pt_GW", "guinea-bissau" },
		{ "pt_ST", "sao-tome-and-principe" }, { "pt_TL", "east-timor" },

		{ "it_IT", "italy" }, { "it_SM", "san-marino" }, { "it_CH", "switzerland_ticino" },

		{ "uk_UA", "ukraine" }, { "be_BY", "belarus" }, { "ru_RU", "russia" }, { "ru_MD", "transnistria" },
		{ "kk_KZ", "kazakhstan" }, { "ky_KG", "kyrgyzstan" }, { "tg_TJ", "tajikistan" }, { "tk_TM", "turkmenistan" },
		{ "uz_UZ", "uzbekistan" }, { "mn_MN", "mongolia" },

		{ "pl_PL", "poland" }, { "cs_CZ", "czech-republic" }, { "sk_SK", "slovakia" },

		{ "sr_RS", "serbia" }, { "hr_HR", "croatia" }, { "bs_BA", "bosnia-herzegovina" }, { "sr_ME", "montenegro" },
		{ "sl_SI", "slovenia" }, { "mk_MK", "macedonia" }, { "bg_BG", "bulgaria" }, { "sq_XK", "kosovo" },
		{ "sq_AL", "albania" },

		{ "ro_RO", "romania" }, { "ro_MD", "moldova" }, { "hu_HU", "hungary" }, { "el_GR", "greece" },
		{ "el_CY", "cyprus" }, { "lt_LT", "lithuania" }, { "lv_LV", "latvia" },

		{ "nb_NO", "norway" }, { "sv_SE", "sweden" }, { "da_DK", "denmark" }, { "is_IS", "iceland" },
		{ "fo_FO", "faroe-islands" }, { "kl_GL", "greenland" }, { "sv_AX", "finland_aland" },
		{ "fi_FI", "finland" }, { "et_EE", "estonia" }, { "tr_TR", "turkey" }, { "az_AZ", "azerbaijan" },

		{ "ar_JO", "jordan" }, { "ar_EG", "egypt" }, { "ar_SA", "saudi-arabia" }, { "ar_IQ", "iraq" },
		{ "ar_SY", "syria" }, { "ar_LB", "lebanon" }, { "ar_PS", "palestine" }, { "ar_YE", "yemen" },
		{ "ar_OM", "oman" }, { "ar_AE", "united-arab-emirates" }, { "ar_QA", "qatar" }, { "ar_BH", "bahrain" },
		{ "ar_KW", "kuwait" }, { "ar_LY", "libya" }, { "ar_SD", "sudan" }, { "ar_MR", "mauritania" },
		{ "so_SO", "somalia" }, { "ar_DZ", "algeria" }, { "ar_MA", "morocco" }, { "ar_TN", "tunisia" },
		{ "ar_EH", "western-sahara" }, { "fa_IR", "iran" }, { "fa_AF", "afghanistan" }, { "he_IL", "israel" },

		{ "zh_CN", "china" }, { "ja_JP", "japan" }, { "zh_TW", "taiwan" }, { "zh_HK", "hong-kong" },
		{ "zh_MO", "macao" }, { "ko_KR", "south-korea" }, { "ko_KP", "north-korea" }, { "vi_VN", "vietnam" },
		{ "id_ID", "indonesia" }, { "ms_MY", "malaysia" }, { "ms_BN", "brunei" }, { "th_TH", "thailand" },
		{ "lo_LA", "laos" }, { "km_KH", "cambodia" }, { "my_MM", "myanmar" },
		{ "ur_PK", "pakistan" }, { "bn_BD", "bangladesh" }, { "ne_NP", "nepal" }, { "si_LK", "sri-lanka" },
		{ "dz_BT", "bhutan" }, { "dv_MV", "maldives" }, { "ka_GE", "georgia" }, { "hy_AM", "armenia" },
		{ "am_ET", "ethiopia" }, { "ti_ER", "eritrea" },
	};

	private static final Map<String, String> LOCALE_BY_PREFIX = new HashMap<>();
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	static {
		for (String[] entry : MAP_LOCALES) {
			for (int i = 1; i < entry.length; i++) {
				if (LOCALE_BY_PREFIX.put(entry[i], entry[0]) != null) {
					throw new IllegalStateException("Duplicate map prefix " + entry[i]);
				}
			}
		}
	}

	private SearchLocales() {
	}

	/**
	 * @return rules locale of a map ("Us_new-york_northamerica_2.obf" -> "en_US") or "" when no prefix covers the
	 * map name; accepts a download name, a file name or a path
	 */
	public static String forMap(String mapName) {
		if (mapName == null) {
			return "";
		}
		String name = mapName.toLowerCase(Locale.ROOT);
		int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		// the longest covering prefix: "switzerland_ticino" before "switzerland"
		for (int end = name.length(); end > 0; end = name.lastIndexOf('_', end - 1)) {
			String locale = LOCALE_BY_PREFIX.get(name.substring(0, end));
			if (locale != null) {
				return locale;
			}
		}
		return "";
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
