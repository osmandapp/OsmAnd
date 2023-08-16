package net.osmand.plus.help;

import static net.osmand.IndexConstants.HELP_INDEX_DIR;
import static net.osmand.plus.backup.BackupHelper.SERVER_URL;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoadArticlesTask extends AsyncTask<Void, Void, Void> {

	private static final Log log = PlatformUtil.getLog(LoadArticlesTask.class);

	private static final String HELP_SITEMAP_FILE_NAME = "sitemap.xml";
	private static final String HELP_LINKS_FILE_NAME = "help-links-android.json";

	private static final String SITEMAP_URL = SERVER_URL + "/sitemap.xml";
	private static final String HELP_LINKS_URL = SERVER_URL + "/help-links-android.json";
	public static final String DOCS_LINKS_URL = SERVER_URL + "/docs/user/";

	private final OsmandApplication app;

	private final HelpArticleNode articleNode = new HelpArticleNode(DOCS_LINKS_URL);
	private final Map<String, String> telegramChats = new LinkedHashMap<>();
	private final Map<String, String> popularArticles = new LinkedHashMap<>();

	@Nullable
	private final LoadArticlesListener listener;

	public LoadArticlesTask(@NonNull OsmandApplication app, @Nullable LoadArticlesListener listener) {
		this.app = app;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.downloadStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		loadPopularArticles();
		loadSitemapArticles();
		return null;
	}

	private void loadPopularArticles() {
		File file = new File(getHelpDir(), HELP_LINKS_FILE_NAME);
		if (file.exists()) {
			parsePopularArticles(file);
		} else {
			String error = AndroidNetworkUtils.downloadFile(HELP_LINKS_URL, file, false, null);
			if (error == null) {
				parsePopularArticles(file);
			} else {
				log.error(error);
			}
		}
	}

	private void parsePopularArticles(@NonNull File file) {
		try {
			StringBuilder builder = Algorithms.readFromInputStream(new FileInputStream(file));
			JSONObject jsonObject = new JSONObject(builder.toString());

			JSONObject json = jsonObject.optJSONObject("telegramChats");
			if (json != null) {
				collectGroupItems(json, telegramChats);
			}
			json = jsonObject.optJSONObject("popularArticles");
			if (json != null) {
				collectGroupItems(json, popularArticles);
			}
		} catch (JSONException | IOException e) {
			file.delete();
			log.error(e);
		}
	}

	private void collectGroupItems(@NonNull JSONObject json, @NonNull Map<String, String> map) throws JSONException {
		for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
			String key = iterator.next();
			String url = json.getString(key);
			map.put(key, url);
		}
	}

	private void loadSitemapArticles() {
		File file = new File(getHelpDir(), HELP_SITEMAP_FILE_NAME);
		if (file.exists()) {
			processSitemapArticles(file);
		} else {
			String error = AndroidNetworkUtils.downloadFile(SITEMAP_URL, file, false, null);
			if (error == null) {
				processSitemapArticles(file);
			} else {
				log.error(error);
			}
		}
	}

	private void processSitemapArticles(@NonNull File file) {
		List<String> links = parseSitemapLinks(file);
		for (String url : links) {
			addArticle(articleNode, url);
		}
	}

	private void addArticle(@NonNull HelpArticleNode currentNode, @NonNull String url) {
		String[] parts = url.replace(DOCS_LINKS_URL, "").split("/");

		for (String part : parts) {
			if (!Algorithms.isEmpty(part)) {
				HelpArticleNode articleNode = currentNode.articles.get(part);
				if (articleNode == null) {
					articleNode = new HelpArticleNode(currentNode.url + part + "/");
					currentNode.articles.put(part, articleNode);
				}
				currentNode = articleNode;
			}
		}
	}

	@NonNull
	private List<String> parseSitemapLinks(@NonNull File file) {
		List<String> links = new ArrayList<>();

		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(stream, "UTF-8");

			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tag = parser.getName();
					if ("url".equals(tag)) {
						Map<String, String> values = GPXUtilities.readTextMap(parser, tag);
						String url = values.get("loc");
						if (url != null && url.startsWith(DOCS_LINKS_URL)) {
							links.add(url);
						}
					}
				}
			}
		} catch (IOException | XmlPullParserException e) {
			file.delete();
			log.error(e);
		} finally {
			Algorithms.closeStream(stream);
		}
		return links;
	}

	@NonNull
	private File getHelpDir() {
		File dir = new File(app.getCacheDir(), HELP_INDEX_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (listener != null) {
			listener.downloadFinished(articleNode, popularArticles, telegramChats);
		}
	}

	public interface LoadArticlesListener {

		void downloadStarted();

		void downloadFinished(@NonNull HelpArticleNode articleNode, @NonNull Map<String, String> popularArticles, @NonNull Map<String, String> telegramChats);
	}
}
