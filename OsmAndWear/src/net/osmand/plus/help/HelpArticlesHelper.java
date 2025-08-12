package net.osmand.plus.help;

import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.feedback.FeedbackHelper.EXCEPTION_PATH;
import static net.osmand.plus.help.HelpArticleUtils.createArticleItem;
import static net.osmand.plus.help.HelpArticleUtils.createCategory;
import static net.osmand.plus.help.HelpArticleUtils.createMenuItem;
import static net.osmand.plus.help.HelpArticleUtils.getArticleItemClickListener;
import static net.osmand.plus.help.HelpArticleUtils.getUrlItemClickListener;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.help.LoadArticlesTask.LoadArticlesListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpArticlesHelper implements LoadArticlesListener {

	private static final int MAX_VISIBLE_POPULAR_ARTICLES = 5;
	private static final String TROUBLESHOOTING_CATEGORY = "Troubleshooting";

	private final OsmandApplication app;
	private final HelpActivity activity;

	private final Map<String, HelpArticle> articles = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> telegramChats = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> popularArticles = new LinkedHashMap<>();

	private LoadArticlesTask loadArticlesTask;

	public HelpArticlesHelper(@NonNull HelpActivity activity) {
		this.app = activity.getMyApplication();
		this.activity = activity;
	}

	public void loadArticles() {
		loadArticlesTask = new LoadArticlesTask(app, this);
		loadArticlesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public boolean isLoadingPopularArticles() {
		return loadArticlesTask != null && loadArticlesTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	@Override
	public void downloadStarted() {
		activity.setSupportProgressBarIndeterminateVisibility(true);
	}

	@Override
	public void downloadFinished(@NonNull Map<String, HelpArticle> articles, @NonNull Map<String, String> popularArticles, @NonNull Map<String, String> telegramChats) {
		this.articles.clear();
		this.articles.putAll(articles);

		this.popularArticles.clear();
		this.popularArticles.putAll(popularArticles);

		this.telegramChats.clear();
		this.telegramChats.putAll(telegramChats);

		activity.updateContent();
		activity.setSupportProgressBarIndeterminateVisibility(false);
	}

	@NonNull
	public List<ContextMenuItem> createItems() {
		List<ContextMenuItem> items = new ArrayList<>();

		createPopularArticlesCategory(items);
		createDocsCategories(items);
		createContactUsCategory(items);
		createReportIssuesCategory(items);
		createAboutCategory(items);

		return items;
	}

	private void createPopularArticlesCategory(@NonNull List<ContextMenuItem> items) {
		if (!Algorithms.isEmpty(popularArticles)) {
			items.add(createCategory(app.getString(R.string.most_viewed)));

			int counter = 0;
			for (Map.Entry<String, String> entry : popularArticles.entrySet()) {
				String url = SERVER_URL + entry.getValue();
				String title = HelpArticleUtils.getArticleName(app, url, entry.getKey());

				items.add(createMenuItem(title, null, R.drawable.ic_action_file_info,
						getArticleItemClickListener(activity, title, url)));

				counter++;
				if (counter >= MAX_VISIBLE_POPULAR_ARTICLES) {
					break;
				}
			}
		}
	}

	private void createDocsCategories(@NonNull List<ContextMenuItem> items) {
		if (!Algorithms.isEmpty(articles)) {
			createUserGuideCategory(items, articles);
		}
		HelpArticle article = articles.get(TROUBLESHOOTING_CATEGORY);
		if (article != null && !Algorithms.isEmpty(article.articles)) {
			createTroubleshootingCategory(items, article);
		}
	}

	private void createUserGuideCategory(@NonNull List<ContextMenuItem> items, @NonNull Map<String, HelpArticle> articles) {
		items.add(createCategory(app.getString(R.string.user_guide)));

		for (Map.Entry<String, HelpArticle> entry : articles.entrySet()) {
			if (!TROUBLESHOOTING_CATEGORY.equals(entry.getKey())) {
				HelpArticle article = entry.getValue();
				items.add(createArticleItem(activity, article));
			}
		}
	}

	private void createTroubleshootingCategory(@NonNull List<ContextMenuItem> items, @NonNull HelpArticle helpArticle) {
		items.add(createCategory(app.getString(R.string.troubleshooting)));

		for (HelpArticle article : helpArticle.articles.values()) {
			int iconId = HelpArticleUtils.getArticleIconId(article);
			String title = HelpArticleUtils.getArticleName(app, article);
			items.add(createMenuItem(title, null, iconId, getArticleItemClickListener(activity, title, article.url)));
		}
	}

	private void createContactUsCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.contact_us)));

		String email = app.getString(R.string.support_email);
		items.add(createMenuItem(app.getString(R.string.contact_support), email,
				R.drawable.ic_action_at_mail, (uiAdapter, view, item, isChecked) -> {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setData(Uri.parse("mailto:"));
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
					intent.putExtra(Intent.EXTRA_TEXT, app.getFeedbackHelper().getDeviceInfo());
					AndroidUtils.startActivityIfSafe(activity, intent);
					return false;
				}));

		items.add(createMenuItem(app.getString(R.string.github_discussion), app.getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(activity, app.getString(R.string.discussion_github))));

		items.add(createMenuItem(app.getString(R.string.telegram_chats), null,
				R.drawable.ic_action_social_telegram, (uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					TelegramChatsFragment.showInstance(manager, telegramChats);
					return false;
				}));

		for (SocialNetwork network : SocialNetwork.values()) {
			String url = app.getString(network.urlId);
			items.add(createMenuItem(app.getString(network.titleId), url, network.iconId, getUrlItemClickListener(activity, url)));
		}
	}

	private void createReportIssuesCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.report_an_issues)));

		items.add(createMenuItem(app.getString(R.string.open_issue_on_github), app.getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(activity, app.getString(R.string.issues_github))));

		File exceptionLog = app.getAppPath(EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(createMenuItem(app.getString(R.string.send_crash_log), app.getString(R.string.send_crash_log_descr),
					R.drawable.ic_action_bug_outlined_send, (uiAdapter, view, item, isChecked) -> {
						app.getFeedbackHelper().sendCrashLog();
						return false;
					}));
		}
		items.add(createMenuItem(app.getString(R.string.send_logcat_log), app.getString(R.string.send_logcat_log_descr),
				R.drawable.ic_action_file_report_outlined_send, (uiAdapter, view, item, isChecked) -> {
					activity.readAndSaveLogs();
					return false;
				}));
	}

	private void createAboutCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.about_osmand)));
		boolean nightMode = !app.getSettings().isLightContent();

		items.add(createMenuItem(app.getString(R.string.about_osmand), null, R.drawable.ic_action_osmand_logo,
				getArticleItemClickListener(activity, app.getString(R.string.about_osmand), app.getString(R.string.osmand_about)))
				.setUseNaturalIconColor(true)
				.setColor(ColorUtilities.getOsmandIconColor(app, nightMode)));

		String version = Version.getFullVersionWithReleaseDate(app);
		items.add(createMenuItem(app.getString(R.string.what_is_new), version, R.drawable.ic_action_clipboard_notes,
				getArticleItemClickListener(activity, app.getString(R.string.what_is_new), app.getString(R.string.docs_latest_version)))
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareMenu.copyToClipboardWithToast(adapter.getContext(), version, Toast.LENGTH_SHORT);
					return false;
				}));

		String freeVersions = app.getString(R.string.free_versions);
		items.add(createMenuItem(freeVersions, app.getString(R.string.free_versions_descr),
				R.drawable.ic_action_gsave_dark, getArticleItemClickListener(activity, freeVersions, app.getString(R.string.docs_free_versions))));
	}
}
