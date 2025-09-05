package net.osmand.plus.help;

import static android.os.AsyncTask.Status.FINISHED;
import static android.os.AsyncTask.Status.RUNNING;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.help.LoadArticlesTask.LoadArticlesListener;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelpArticlesHelper implements LoadArticlesListener {

	private final OsmandApplication app;

	private final Set<String> languages = new LinkedHashSet<>();
	private final Map<String, String> telegramChats = new LinkedHashMap<>();
	private final Map<String, String> popularArticles = new LinkedHashMap<>();
	private final Map<String, HelpArticle> articles = new LinkedHashMap<>();

	private List<LoadArticlesListener> listeners = new ArrayList<>();

	private HelpActivity activity;
	private LoadArticlesTask loadArticlesTask;

	public HelpArticlesHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	private void clear() {
		languages.clear();
		articles.clear();
		popularArticles.clear();
		telegramChats.clear();
	}

	public void loadArticles() {
		clear();
		loadArticlesTask = new LoadArticlesTask(app, languages, articles, telegramChats, popularArticles, this);
		OsmAndTaskManager.executeTask(loadArticlesTask);
	}

	public boolean isLoadingArticles() {
		return loadArticlesTask != null && loadArticlesTask.getStatus() == RUNNING;
	}

	public boolean isLoadingArticlesFinished() {
		return loadArticlesTask != null && loadArticlesTask.getStatus() == FINISHED;
	}

	public void addListener(@NonNull LoadArticlesListener listener) {
		listeners = CollectionUtils.addToList(listeners, listener);

		if (isLoadingArticles()) {
			listener.downloadStarted();
		}
		if (isLoadingArticlesFinished()) {
			listener.downloadFinished();
		}
	}

	public void removeListener(@NonNull LoadArticlesListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	@NonNull
	public Set<String> getLanguages() {
		return languages;
	}

	@NonNull
	public Map<String, HelpArticle> getArticles() {
		return articles;
	}

	@NonNull
	public Map<String, String> getTelegramChats() {
		return telegramChats;
	}

	@NonNull
	public Map<String, String> getPopularArticles() {
		return popularArticles;
	}

	@Override
	public void downloadStarted() {
		for (LoadArticlesListener listener : listeners) {
			listener.downloadStarted();
		}
	}

	@Override
	public void downloadFinished() {
		for (LoadArticlesListener listener : listeners) {
			listener.downloadFinished();
		}
	}
}