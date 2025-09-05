package net.osmand.plus.help;

import static net.osmand.IndexConstants.HELP_INDEX_DIR;
import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.help.HelpArticleUtils.DOCS_PATH_PREFIX;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class LoadArticlesTask extends AsyncTask<Void, Void, Void> {

	private static final Log log = PlatformUtil.getLog(LoadArticlesTask.class);

	private static final String HELP_STRUCTURE_FILE_NAME = "help-structure.json";
	private static final String HELP_LINKS_URL = SERVER_URL + "/" + HELP_STRUCTURE_FILE_NAME;

	private final OsmandApplication app;

	private final Set<String> languages;
	private final Map<String, String> telegramChats;
	private final Map<String, String> popularArticles;
	private final Map<String, HelpArticle> articles;

	@Nullable
	private final LoadArticlesListener listener;

	public LoadArticlesTask(@NonNull OsmandApplication app, @NonNull Set<String> languages,
			@NonNull Map<String, HelpArticle> articles,	@NonNull Map<String, String> telegramChats,
			@NonNull Map<String, String> popularArticles, @Nullable LoadArticlesListener listener) {
		this.app = app;
		this.languages = languages;
		this.telegramChats = telegramChats;
		this.popularArticles = popularArticles;
		this.articles = articles;
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
		loadHelpStructure();
		return null;
	}

	private void loadHelpStructure() {
		File file = new File(getHelpDir(), HELP_STRUCTURE_FILE_NAME);
		if (file.exists()) {
			parseHelpStructure(file);
		} else {
			String error = AndroidNetworkUtils.downloadFile(HELP_LINKS_URL, file, false, null);
			if (error == null) {
				parseHelpStructure(file);
			} else {
				log.error(error);
			}
		}
	}

	private void parseHelpStructure(@NonNull File file) {
		try {
			StringBuilder builder = Algorithms.readFromInputStream(new FileInputStream(file));
			JSONObject jsonObject = new JSONObject(builder.toString());

			JSONArray languages = jsonObject.optJSONArray("languages");
			if (languages != null) {
				parseLanguages(languages);
			}
			JSONArray jsonArray = jsonObject.optJSONArray("articles");
			if (jsonArray != null) {
				buildArticlesHierarchy(parseArticles(jsonArray));
			}
			JSONObject android = jsonObject.optJSONObject("android");
			if (android != null) {
				JSONObject json = android.optJSONObject("popularArticles");
				if (json != null) {
					collectGroupItems(json, popularArticles);
				}
				json = android.optJSONObject("telegramChats");
				if (json != null) {
					collectGroupItems(json, telegramChats);
				}
			}
		} catch (Exception e) {
			file.delete();
			log.error(e);
		}
	}

	@NonNull
	private void parseLanguages(@NonNull JSONArray array) throws JSONException {
		for (int i = 0; i < array.length(); i++) {
			languages.add(array.optString(i));
		}
	}

	private void buildArticlesHierarchy(@NonNull List<HelpArticle> articles) {
		List<HelpArticle> lastArticlesPerLevel = new ArrayList<>();
		for (HelpArticle article : articles) {
			processArticle(article, lastArticlesPerLevel);
		}
	}

	private void processArticle(@NonNull HelpArticle article, @NonNull List<HelpArticle> lastArticlesPerLevel) {
		while (lastArticlesPerLevel.size() < article.level) {
			lastArticlesPerLevel.add(null);
		}
		if (article.level == 2) {
			articles.put(article.label, article);
		} else {
			HelpArticle parent = lastArticlesPerLevel.get(article.level - 2);
			if (parent != null) {
				parent.articles.put(article.label, article);
			}
		}
		lastArticlesPerLevel.set(article.level - 1, article);
	}

	private void collectGroupItems(@NonNull JSONObject json, @NonNull Map<String, String> map) throws JSONException {
		for (Iterator<String> iterator = json.keys(); iterator.hasNext(); ) {
			String key = iterator.next();
			String url = json.getString(key);
			map.put(key, getLocalizedUrl(url));
		}
	}

	@NonNull
	private List<HelpArticle> parseArticles(@NonNull JSONArray array) throws JSONException {
		List<HelpArticle> articles = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);

			String url = object.optString("url");
			boolean hasUrl = !Algorithms.isEmpty(url);
			boolean available = object.optBoolean("android", true);

			if (available && (url.startsWith(DOCS_PATH_PREFIX) || !hasUrl)) {
				int level = object.optInt("level");
				String label = object.optString("label");
				articles.add(new HelpArticle(getLocalizedUrl(url), label, level));
			}
		}
		return articles;
	}

	@Nullable
	private String getLocalizedUrl(@Nullable String url) {
		if (!Algorithms.isEmpty(url) && url.startsWith(DOCS_PATH_PREFIX)) {
			return HelpArticleUtils.getLocalizedUrl(app, SERVER_URL + url);
		}
		return url;
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
			listener.downloadFinished();
		}
	}

	public interface LoadArticlesListener {

		void downloadStarted();

		void downloadFinished();
	}
}
