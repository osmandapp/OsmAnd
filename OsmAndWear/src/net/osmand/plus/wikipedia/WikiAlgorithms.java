package net.osmand.plus.wikipedia;

import static net.osmand.util.Algorithms.isUrl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WikiAlgorithms {
	private static final Log LOG = PlatformUtil.getLog(WikiAlgorithms.class);


	public static final String WIKIPEDIA = "wikipedia";
	public static final String WIKIPEDIA_DOMAIN = ".wikipedia.org/";
	public static final String WIKI_LINK = WIKIPEDIA_DOMAIN + "wiki/";

	@NonNull
	public static String getWikiUrl(@NonNull String text) {
		return getWikiParams("", text).second;
	}

	public static Pair<String, String> getWikiParams(String key, String value) {
		String title = null;
		String langCode = "en";
		// Full OpenStreetMap Wikipedia tag pattern looks like "operator:wikipedia:lang_code",
		// "operator" and "lang_code" is optional parameters and may be skipped.
		if (key.contains(":")) {
			String[] tagParts = key.split(":");
			if (tagParts.length == 3) {
				// In this case tag contains all 3 parameters: "operator", "wikipedia" and "lang_code".
				langCode = tagParts[2];
			} else if (tagParts.length == 2) {
				// In this case one of the optional parameters was skipped.
				// Parameters never change their order and parameter "wikipedia" is always present.
				if (WIKIPEDIA.equals(tagParts[0])) {
					// So if "wikipedia" is the first parameter, then parameter "operator" was skipped.
					// And the second parameter is "lang_code".
					langCode = tagParts[1];
				}
			}
		}
		// Value of an Wikipedia item can be an URL, but it is not recommended.
		// OSM users should use the following pattern "lang_code:article_title" instead.
		// Where "lang_code" is optional parameter for multilingual wikipedia tags.
		String url;
		if (isUrl(value)) {
			// In this case a value is already represented as an URL.
			url = value;
		} else {
			if (value.contains(":")) {
				// If value contains a sign ":" it means that "lang_code" is also present in value.
				String[] valueParts = value.split(":");
				langCode = valueParts[0];
				title = valueParts[1];
			} else {
				title = value;
			}
			// Full article URL has a pattern: "http://lang_code.wikipedia.org/wiki/article_name"
			String formattedTitle = title.replaceAll(" ", "_");
			url = "http://" + langCode + WIKI_LINK + formattedTitle;
		}
		String text = title != null ? title : value;
		return new Pair<>(text, url);
	}

	@Nullable
	public static String formatWikiDate(@Nullable String date) {
		if (date == null) {
			return null;
		}
		String cleanDate = date.startsWith("+") ? date.substring(1) : date;
		cleanDate = cleanDate.endsWith("Z") ? cleanDate.substring(0, cleanDate.length() - 1) : cleanDate;
		try {
			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
			SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
			Date dateTime = inputFormat.parse(cleanDate);
			if (dateTime != null) {
				String dateTimeString = outputFormat.format(dateTime);
				if (!Algorithms.isEmpty(dateTimeString)) {
					return dateTimeString;
				}
			}
			return date;
		} catch (Exception exception) {
			LOG.error(exception);
		}
		return date;
	}
}
