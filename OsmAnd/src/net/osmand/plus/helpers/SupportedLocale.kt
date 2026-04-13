package net.osmand.plus.helpers

import android.content.Context
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.plus.configmap.ConfigureMapUtils
import java.util.Locale
import java.util.TreeMap

/**
 * Represents all supported UI locales in OsmAnd.
 * Maps modern BCP-47 language tags to the legacy tags historically used in OsmAndSettings.
 *
 * See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
 * Hardy maintenance 2016-05-29:
 * 	- Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
 * 	- Mark as "incomplete" if                    < ~80%
 */

enum class SupportedLocale(
	val modernTag: String,
	val osmandTag: String,
	@StringRes val nameResId: Int,
	val incomplete: Boolean = false
) {
	// @formatter:off
	SYSTEM_DEFAULT     ("",        "",        R.string.system_locale),
	ENGLISH            ("en",      "en",      R.string.lang_en),
	AFRIKAANS          ("af",      "af",      R.string.lang_af,    incomplete = true),
	ARABIC             ("ar",      "ar",      R.string.lang_ar),
	ASTURIAN           ("ast",     "ast",     R.string.lang_ast,   incomplete = true),
	AZERBAIJANI        ("az",      "az",      R.string.lang_az),
	BELARUSIAN         ("be",      "be",      R.string.lang_be),
	BULGARIAN          ("bg",      "bg",      R.string.lang_bg),
	CATALAN            ("ca",      "ca",      R.string.lang_ca),
	CZECH              ("cs",      "cs",      R.string.lang_cs),
	WELSH              ("cy",      "cy",      R.string.lang_cy,    incomplete = true),
	DANISH             ("da",      "da",      R.string.lang_da),
	GERMAN             ("de",      "de",      R.string.lang_de),
	GREEK              ("el",      "el",      R.string.lang_el),
	ENGLISH_GB         ("en-GB",   "en_GB",   R.string.lang_en_gb),
	ESPERANTO          ("eo",      "eo",      R.string.lang_eo),
	SPANISH            ("es",      "es",      R.string.lang_es),
	SPANISH_AR         ("es-AR",   "es_AR",   R.string.lang_es_ar),
	SPANISH_US         ("es-US",   "es_US",   R.string.lang_es_us),
	BASQUE             ("eu",      "eu",      R.string.lang_eu),
	PERSIAN            ("fa",      "fa",      R.string.lang_fa),
	FINNISH            ("fi",      "fi",      R.string.lang_fi,    incomplete = true),
	FRENCH             ("fr",      "fr",      R.string.lang_fr),
	GALICIAN           ("gl",      "gl",      R.string.lang_gl),
	HEBREW             ("he",      "iw",      R.string.lang_he),
	CROATIAN           ("hr",      "hr",      R.string.lang_hr,    incomplete = true),
	UPPER_SORBIAN      ("hsb",     "hsb",     R.string.lang_hsb,   incomplete = true),
	HUNGARIAN          ("hu",      "hu",      R.string.lang_hu),
	ARMENIAN           ("hy",      "hy",      R.string.lang_hy),
	INDONESIAN         ("id",      "id",      R.string.lang_id),
	ICELANDIC          ("is",      "is",      R.string.lang_is),
	ITALIAN            ("it",      "it",      R.string.lang_it),
	JAPANESE           ("ja",      "ja",      R.string.lang_ja),
	GEORGIAN           ("ka",      "ka",      R.string.lang_ka,    incomplete = true),
	KABYLE             ("kab",     "kab",     R.string.lang_kab,   incomplete = true),
	KANNADA            ("kn",      "kn",      R.string.lang_kn,    incomplete = true),
	KOREAN             ("ko",      "ko",      R.string.lang_ko),
	LITHUANIAN         ("lt",      "lt",      R.string.lang_lt),
	LATVIAN            ("lv",      "lv",      R.string.lang_lv),
	MACEDONIAN         ("mk",      "mk",      R.string.lang_mk),
	MALAYALAM          ("ml",      "ml",      R.string.lang_ml),
	MARATHI            ("mr",      "mr",      R.string.lang_mr,    incomplete = true),
	NORWEGIAN_BOKMAL   ("nb",      "nb",      R.string.lang_nb),
	DUTCH              ("nl",      "nl",      R.string.lang_nl),
	NORWEGIAN_NYNORSK  ("nn",      "nn",      R.string.lang_nn,    incomplete = true),
	OCCITAN            ("oc",      "oc",      R.string.lang_oc,    incomplete = true),
	POLISH             ("pl",      "pl",      R.string.lang_pl),
	PORTUGUESE         ("pt",      "pt",      R.string.lang_pt),
	PORTUGUESE_BRAZIL  ("pt-BR",   "pt_BR",   R.string.lang_pt_br),
	ROMANIAN           ("ro",      "ro",      R.string.lang_ro,    incomplete = true),
	RUSSIAN            ("ru",      "ru",      R.string.lang_ru),
	SANTALI            ("sat",     "sat",     R.string.lang_sat,   incomplete = true),
	SARDINIAN          ("sc",      "sc",      R.string.lang_sc),
	SLOVAK             ("sk",      "sk",      R.string.lang_sk),
	SLOVENIAN          ("sl",      "sl",      R.string.lang_sl),
	SERBIAN            ("sr",      "sr",      R.string.lang_sr),
	SERBIAN_LATIN      ("sr-Latn", "sr+Latn", R.string.lang_sr_latn),
	SWEDISH            ("sv",      "sv",      R.string.lang_sv),
	TURKISH            ("tr",      "tr",      R.string.lang_tr),
	UKRAINIAN          ("uk",      "uk",      R.string.lang_uk),
	VIETNAMESE         ("vi",      "vi",      R.string.lang_vi,    incomplete = true),
	CHINESE_SIMPLIFIED ("zh-CN",   "zh_CN",   R.string.lang_zh_cn, incomplete = true),
	CHINESE_TRADITIONAL("zh-TW",   "zh_TW",   R.string.lang_zh_tw);
	// @formatter:on

	companion object {

		/**
		 * Safely creates a java.util.Locale object directly from any OsmAnd legacy or modern tag.
		 * Eliminates the need for manual parsing of '_' and '+'.
		 */
		@JvmStatic
		fun createLocale(tag: String?): Locale? {
			if (tag.isNullOrEmpty()) return null

			val knownLocale = fromTag(tag)
			if (knownLocale != null) {
				// Use the modern BCP-47 tag to guarantee valid Locale creation without legacy parsing
				return Locale.forLanguageTag(knownLocale.modernTag)
			}

			// Ultimate fallback for custom/unknown tags (if any)
			val fallbackTag = tag
				.replace('_', '-')
				.replace('+', '-')
			return Locale.forLanguageTag(fallbackTag)
		}

		/**
		 * Safely converts any system BCP-47 tag to the OsmAndSettings legacy format.
		 */
		@JvmStatic
		fun normalizeToOsmandLegacy(systemTag: String?): String {
			if (systemTag.isNullOrEmpty()) return ""

			val knownLocale = fromTag(systemTag)
			if (knownLocale != null) {
				return knownLocale.osmandTag
			}

			// Fallback for languages entirely absent from our UI list
			return systemTag.replace('-', '_')
		}

		/**
		 * Finds the corresponding SupportedLocale by ANY known tag (modern or legacy).
		 */
		@JvmStatic
		fun fromTag(tag: String?): SupportedLocale? {
			if (tag.isNullOrEmpty()) return SYSTEM_DEFAULT

			// Single line protection for the old Java anomaly where OS might return "in" instead of "id"
			val normalizedTag = if (tag.equals("in", ignoreCase = true)) "id" else tag

			return entries.find {
				it.modernTag.equals(normalizedTag, ignoreCase = true) ||
						it.osmandTag.equals(normalizedTag, ignoreCase = true)
			}
		}

		/**
		 * Generates the sorted map of languages for the UI Preference screen.
		 * Maps osmandTag (DB key) to the fully resolved human-readable string.
		 */
		@JvmStatic
		fun getPreferredDisplayLanguages(context: Context): Map<String, String> {
			val incompleteSuffix = " (${context.getString(R.string.incomplete_locale)})"
			val languages = mutableMapOf<String, String>()

			entries.forEach { locale ->
				var displayName = context.getString(locale.nameResId)

				if (locale == SYSTEM_DEFAULT) {
					val deviceLanguageInLatin =
						" (${context.getString(R.string.system_locale_no_translate)})"
					displayName += deviceLanguageInLatin
				} else if (locale.incomplete) {
					displayName += incompleteSuffix
				}

				languages[locale.osmandTag] = displayName
			}

			val comparator = ConfigureMapUtils.getLanguagesComparator(languages)
			val sortedLanguages = TreeMap<String, String>(comparator)
			sortedLanguages.putAll(languages)
			return sortedLanguages
		}
	}
}