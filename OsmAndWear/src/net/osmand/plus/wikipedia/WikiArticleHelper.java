package net.osmand.plus.wikipedia;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;


public class WikiArticleHelper {

	private static final String TAG = WikiArticleHelper.class.getSimpleName();

	private static final int PARTIAL_CONTENT_PHRASES = 3;
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String PAGE_PREFIX_FILE = "file://";
	public static final String WIKIVOYAGE_DOMAIN = ".wikivoyage.org/wiki/";

	public static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";
	public static final String WIKI_DOMAIN_COM = ".wikipedia.com/wiki/";

	private WikiArticleSearchTask articleSearchTask;
	private final FragmentActivity activity;

	private final boolean nightMode;
	private static final String P_OPENED = "<p>";
	private static final String P_CLOSED = "</p>";

	public WikiArticleHelper(FragmentActivity activity, boolean nightMode) {
		this.activity = activity;
		this.nightMode = nightMode;
	}

	public static String normalizeFileUrl(String url) {
		return url.startsWith(PAGE_PREFIX_FILE) ?
				url.replace(PAGE_PREFIX_FILE, PAGE_PREFIX_HTTPS) : url;
	}

	public void showWikiArticle(@Nullable LatLon location, @NonNull String url) {
		if (location != null) {
			showWikiArticle(Collections.singletonList(location), url);
		}
	}

	public void showWikiArticle(@Nullable List<LatLon> locations, @NonNull String url) {
		if (!Algorithms.isEmpty(locations)) {
			articleSearchTask = new WikiArticleSearchTask(locations, url, activity, nightMode);
			articleSearchTask.execute();
		}
	}

	public void stopSearchAsyncTask() {
		if (articleSearchTask != null && articleSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			articleSearchTask.cancel(false);
		}
	}

	@NonNull
	public static String getLanguageFromUrl(String url) {
		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTP) ? PAGE_PREFIX_HTTP.length() : 0, url.indexOf("."));
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
		}
		return "";
	}

	public static String getArticleNameFromUrl(String url, String lang) {
		String domain = url.contains(WIKIVOYAGE_DOMAIN) ? WIKIVOYAGE_DOMAIN :
				url.contains(WIKI_DOMAIN) ? WIKI_DOMAIN : WIKI_DOMAIN_COM;
		String articleName = "";

		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			articleName = url.replace(PAGE_PREFIX_HTTP + lang + domain, "")
					.replaceAll("_", " ");
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			articleName = url.replace(PAGE_PREFIX_HTTPS + lang + domain, "")
					.replaceAll("_", " ");
		}
		try {
			articleName = URLDecoder.decode(articleName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return articleName;
	}

	public static void warnAboutExternalLoad(String url, Context context, boolean nightMode) {
		if (context == null) {
			return;
		}
		new AlertDialog.Builder(context)
				.setTitle(url)
				.setMessage(R.string.online_webpage_warning)
				.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					AndroidUtils.openUrl(context, Uri.parse(url), nightMode);
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	@Nullable
	public static String getPartialContent(String source) {
		if (Algorithms.isEmpty(source)) {
			return null;
		}
		String content = source.replaceAll("\\n", "");
		int firstParagraphStart = content.indexOf(P_OPENED);
		int firstParagraphEnd = content.indexOf(P_CLOSED);
		firstParagraphEnd = firstParagraphEnd < firstParagraphStart ? content.indexOf(P_CLOSED, firstParagraphStart) : firstParagraphEnd;
		String firstParagraphHtml = null;
		if (firstParagraphStart != -1 && firstParagraphEnd != -1
				&& firstParagraphEnd >= firstParagraphStart) {
			firstParagraphHtml = content.substring(firstParagraphStart, firstParagraphEnd + P_CLOSED.length());
			while ((firstParagraphHtml.substring(P_OPENED.length(), firstParagraphHtml.length() - P_CLOSED.length()).trim().isEmpty()
					&& (firstParagraphEnd + P_CLOSED.length()) < content.length())
					|| Html.fromHtml(firstParagraphHtml.replaceAll("(<a.+?/a>)|(<div.+?/div>)", "")).toString().trim().length() == 0) {
				firstParagraphStart = content.indexOf(P_OPENED, firstParagraphEnd);
				firstParagraphEnd = firstParagraphStart == -1 ? -1 : content.indexOf(P_CLOSED, firstParagraphStart);
				if (firstParagraphStart != -1 && firstParagraphEnd != -1) {
					firstParagraphHtml = content.substring(firstParagraphStart, firstParagraphEnd + P_CLOSED.length());
				} else {
					break;
				}
			}
		}

		if (Algorithms.isEmpty(firstParagraphHtml)) {
			firstParagraphHtml = source;
		}
		if (Algorithms.isEmpty(firstParagraphHtml)) {
			return null;
		}

		String firstParagraphText = Html.fromHtml(firstParagraphHtml.replaceAll("(<(/)(a|img)>)|(<(a|img).+?>)|(<div.+?/div>)", ""))
				.toString().trim();
		String[] phrases = firstParagraphText.split("\\. ");
		StringBuilder res = new StringBuilder();
		int limit = Math.min(phrases.length, PARTIAL_CONTENT_PHRASES);
		for (int i = 0; i < limit; i++) {
			res.append(phrases[i]);
			if (i < limit - 1) {
				res.append(". ");
			}
		}
		return res.toString();
	}

	@Nullable
	public static String getFirstParagraph(String descriptionHtml) {
		if (descriptionHtml != null) {
			String firstParagraph = getPartialContent(descriptionHtml);
			if (!Algorithms.isEmpty(firstParagraph)) {
				return firstParagraph;
			}
		}
		return descriptionHtml;
	}

	public static String buildTravelUrl(String url, String lang) {
		String title = url.replace(" ", "_");
		try {
			title = URLEncoder.encode(title, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		return "https://osmand.net/travel?title=" + title + "&lang=" + lang;
	}

	public static String decodeTitleFromTravelUrl(String url) {
		String title = "";
		try {
			if (!Algorithms.isEmpty(url)) {
				title = url.replace("_", " ");
				title = URLDecoder.decode(title, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println(e.getMessage());
		}
		return title;
	}

	public static void askShowArticle(
			@NonNull FragmentActivity activity, boolean nightMode,
			@NonNull LatLon latLon, @NonNull String text
	) {
		askShowArticle(activity, nightMode, Collections.singletonList(latLon), text);
	}

	public static void askShowArticle(
			@NonNull FragmentActivity activity, boolean nightMode,
			@NonNull List<LatLon> locations, @NonNull String text
	) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (Version.isPaidVersion(app)) {
			WikiArticleHelper wikiArticleHelper = new WikiArticleHelper(activity, nightMode);
			wikiArticleHelper.showWikiArticle(locations, text);
		} else {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			WikipediaArticleWikiLinkFragment.showInstance(fragmentManager, text);
		}
	}
}