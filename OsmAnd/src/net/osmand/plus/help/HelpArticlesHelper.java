package net.osmand.plus.help;

import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.helpers.FeedbackHelper.EXCEPTION_PATH;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.help.LoadArticlesTask.LoadArticlesListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpArticlesHelper implements LoadArticlesListener {

	private static final int MAX_VISIBLE_POPULAR_ARTICLES = 5;

	private final OsmandApplication app;
	private final HelpActivity activity;
	private final LinkedHashMap<String, String> telegramChats = new LinkedHashMap<>();
	private final LinkedHashMap<String, String> popularArticles = new LinkedHashMap<>();
	private HelpArticleNode articleNode;

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
	public void downloadFinished(@NonNull HelpArticleNode articleNode, @NonNull Map<String, String> popularArticles, @NonNull Map<String, String> telegramChats) {
		this.articleNode = articleNode;

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
		createArticleNodeCategories(items);
		createContactUsCategory(items);
		createReportIssuesCategory(items);
		createAboutCategory(items);

		return items;
	}

	private void createPopularArticlesCategory(@NonNull List<ContextMenuItem> items) {
		if (!Algorithms.isEmpty(popularArticles)) {
			items.add(createCategory(getString(R.string.most_viewed)));

			int counter = 0;
			for (Map.Entry<String, String> entry : popularArticles.entrySet()) {
				String url = SERVER_URL + entry.getValue();
				String title = HelpArticleUtils.getArticleName(app, url);

				items.add(createMenuItem(title, null, R.drawable.ic_action_file_info,
						getArticleItemClickListener(title, url)));

				counter++;
				if (counter >= MAX_VISIBLE_POPULAR_ARTICLES) {
					break;
				}
			}
		}
	}

	private void createArticleNodeCategories(@NonNull List<ContextMenuItem> items) {
		if (articleNode != null) {
			Map<String, HelpArticleNode> articles = new LinkedHashMap<>(articleNode.articles);
			HelpArticleNode troubleshootingNode = articles.remove("troubleshooting");

			if (!Algorithms.isEmpty(articles)) {
				createUserGuideCategory(items, articles);
			}
			if (troubleshootingNode != null && !Algorithms.isEmpty(troubleshootingNode.articles)) {
				createTroubleshootingCategory(items, troubleshootingNode);
			}
		}
	}

	private void createUserGuideCategory(@NonNull List<ContextMenuItem> items, @NonNull Map<String, HelpArticleNode> articles) {
		items.add(createCategory(getString(R.string.user_guide)));

		createUserGuideArticleItem("start-with", items, articles);
		createUserGuideArticleItem("map", items, articles);
		createUserGuideArticleItem("map-legend", items, articles);
		createUserGuideArticleItem("widgets", items, articles);
		createUserGuideArticleItem("navigation", items, articles);
		createUserGuideArticleItem("search", items, articles);
		createUserGuideArticleItem("personal", items, articles);
		createUserGuideArticleItem("plan-route", items, articles);
		createUserGuideArticleItem("plugins", items, articles);
		createUserGuideArticleItem("purchases", items, articles);

		for (Map.Entry<String, HelpArticleNode> entry : articles.entrySet()) {
			HelpArticleNode node = entry.getValue();

			String title = HelpArticleUtils.getArticleName(app, node.url);
			items.add(createMenuItem(title, null, R.drawable.ic_action_book_info,
					(uiAdapter, view, item, isChecked) -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						HelpArticlesFragment.showInstance(manager, node);
						return false;
					}));
		}
	}

	private void createUserGuideArticleItem(@NonNull String key, @NonNull List<ContextMenuItem> items,
	                                        @NonNull Map<String, HelpArticleNode> articles) {
		HelpArticleNode node = articles.remove(key);
		if (node != null) {
			String title = HelpArticleUtils.getArticleName(app, node.url);
			items.add(createMenuItem(title, null, R.drawable.ic_action_book_info,
					(uiAdapter, view, item, isChecked) -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						HelpArticlesFragment.showInstance(manager, node);
						return false;
					}));
		}
	}

	private void createTroubleshootingCategory(@NonNull List<ContextMenuItem> items, @NonNull HelpArticleNode articleNode) {
		items.add(createCategory(getString(R.string.troubleshooting)));

		Map<String, HelpArticleNode> articles = new LinkedHashMap<>(articleNode.articles);

		createTroubleshootingArticleItem("setup", items, articles, R.drawable.ic_action_device_download);
		createTroubleshootingArticleItem("maps-data", items, articles, R.drawable.ic_action_layers);
		createTroubleshootingArticleItem("navigation", items, articles, R.drawable.ic_action_gdirections_dark);
		createTroubleshootingArticleItem("track-recording-issues", items, articles, R.drawable.ic_action_track_recordable);
		createTroubleshootingArticleItem("general", items, articles, R.drawable.ic_action_book_info);
		createTroubleshootingArticleItem("crash-logs", items, articles, R.drawable.ic_action_book_info);

		for (HelpArticleNode node : articles.values()) {
			String title = HelpArticleUtils.getArticleName(app, node.url);
			items.add(createMenuItem(title, null, R.drawable.ic_action_book_info,
					getArticleItemClickListener(title, node.url)));
		}
	}

	private void createTroubleshootingArticleItem(@NonNull String key, @NonNull List<ContextMenuItem> items,
	                                              @NonNull Map<String, HelpArticleNode> articles, @DrawableRes int iconId) {
		HelpArticleNode node = articles.remove(key);
		if (node != null) {
			String title = HelpArticleUtils.getArticleName(app, node.url);
			items.add(createMenuItem(title, null, iconId,
					getArticleItemClickListener(title, node.url)));
		}
	}

	private void createContactUsCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.contact_us)));

		String email = getString(R.string.support_email);
		items.add(createMenuItem(getString(R.string.contact_support), email,
				R.drawable.ic_action_at_mail, (uiAdapter, view, item, isChecked) -> {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setData(Uri.parse("mailto:"));
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
					intent.putExtra(Intent.EXTRA_TEXT, app.getFeedbackHelper().getDeviceInfo());
					AndroidUtils.startActivityIfSafe(activity, intent);
					return false;
				}));

		items.add(createMenuItem(getString(R.string.github_discussion), getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(getString(R.string.discussion_github))));

		items.add(createMenuItem(getString(R.string.telegram_chats), null,
				R.drawable.ic_action_social_telegram, (uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					TelegramChatsFragment.showInstance(manager, telegramChats);
					return false;
				}));

		for (SocialNetwork network : SocialNetwork.values()) {
			String url = getString(network.urlId);
			items.add(createMenuItem(getString(network.titleId), url, network.iconId, getUrlItemClickListener(url)));
		}
	}

	private void createReportIssuesCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.report_an_issues)));

		items.add(createMenuItem(getString(R.string.open_issue_on_github), getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(getString(R.string.issues_github))));

		File exceptionLog = app.getAppPath(EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(createMenuItem(getString(R.string.send_crash_log), getString(R.string.send_crash_log_descr),
					R.drawable.ic_action_bug_outlined_send, (uiAdapter, view, item, isChecked) -> {
						app.getFeedbackHelper().sendCrashLog();
						return false;
					}));
		}
		items.add(createMenuItem(getString(R.string.send_logcat_log), getString(R.string.send_logcat_log_descr),
				R.drawable.ic_action_file_report_outlined_send, (uiAdapter, view, item, isChecked) -> {
					activity.readAndSaveLogs();
					return false;
				}));
	}

	private void createAboutCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.about_osmand)));
		boolean nightMode = !app.getSettings().isLightContent();

		items.add(createMenuItem(getString(R.string.about_osmand), null, R.drawable.ic_action_osmand_logo,
				getArticleItemClickListener(getString(R.string.about_osmand), getString(R.string.osmand_about)))
				.setUseNaturalIconColor(true)
				.setColor(ColorUtilities.getOsmandIconColor(app, nightMode)));

		String version = Version.getFullVersionWithReleaseDate(app);
		items.add(createMenuItem(getString(R.string.what_is_new), version, R.drawable.ic_action_clipboard_notes,
				getArticleItemClickListener(getString(R.string.what_is_new), getString(R.string.docs_latest_version)))
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareMenu.copyToClipboardWithToast(adapter.getContext(), version, Toast.LENGTH_SHORT);
					return false;
				}));

		String freeVersions = getString(R.string.free_versions);
		items.add(createMenuItem(freeVersions, getString(R.string.free_versions_descr),
				R.drawable.ic_action_gsave_dark, getArticleItemClickListener(freeVersions, getString(R.string.docs_free_versions))));
	}

	@NonNull
	private ContextMenuItem createCategory(@NonNull String title) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setCategory(true)
				.setLayout(R.layout.help_category_header);
	}

	@NonNull
	private ContextMenuItem createMenuItem(@NonNull String title, @Nullable String description, @DrawableRes int iconId, @Nullable ItemClickListener listener) {
		return new ContextMenuItem(null)
				.setIcon(iconId)
				.setTitle(title)
				.setDescription(description)
				.setListener(listener);
	}

	@NonNull
	private ItemClickListener getUrlItemClickListener(@NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			AndroidUtils.startActivityIfSafe(activity, intent);
			return false;
		};
	}

	@NonNull
	private ItemClickListener getArticleItemClickListener(@NonNull String title, @NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			HelpArticleDialogFragment.showInstance(manager, url, title);
			return false;
		};
	}

	@NonNull
	private String getString(@StringRes int resId) {
		return app.getString(resId);
	}
}
