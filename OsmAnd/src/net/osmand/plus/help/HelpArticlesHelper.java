package net.osmand.plus.help;

import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.helpers.FeedbackHelper.EXCEPTION_PATH;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.help.LoadArticlesTask.LoadArticlesListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpArticlesHelper implements LoadArticlesListener {

	private static final Log log = PlatformUtil.getLog(HelpArticlesHelper.class);

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
		createUserGuideCategory(items);
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
				String key = entry.getKey();
				String url = SERVER_URL + entry.getValue();

				ContextMenuItem item = new ContextMenuItem(null)
						.setTitle(key)
						.setIcon(R.drawable.ic_action_file_info)
						.setListener(new OnRowItemClick() {
							@Override
							public boolean onContextMenuClick(OnDataChangeUiAdapter uiAdapter, View view, ContextMenuItem item, boolean isChecked) {
								FragmentManager manager = activity.getSupportFragmentManager();
								HelpArticleDialogFragment.showInstance(manager, url, key);
								return true;
							}
						});
				items.add(item);

				counter++;
				if (counter >= MAX_VISIBLE_POPULAR_ARTICLES) {
					break;
				}
			}
		}
	}

	private void createUserGuideCategory(@NonNull List<ContextMenuItem> items) {
		if (articleNode != null) {
			items.add(createCategory(getString(R.string.user_guide)));

			for (Map.Entry<String, HelpArticleNode> entry : articleNode.articles.entrySet()) {
				String key = entry.getKey();
				HelpArticleNode node = entry.getValue();

				items.add(new ContextMenuItem(null)
						.setIcon(R.drawable.ic_action_book_info)
						.setTitle(HelpArticlesHelper.getArticleName(app, key))
						.setListener((uiAdapter, view, item, isChecked) -> {
							FragmentManager manager = activity.getSupportFragmentManager();
							HelpArticlesFragment.showInstance(manager, node);
							return false;
						}));
			}
		}
	}

	private void createContactUsCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.contact_us)));

		String email = getString(R.string.support_email);
		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_at_mail)
				.setTitle(getString(R.string.contact_support))
				.setDescription(email)
				.setListener((uiAdapter, view, item, isChecked) -> {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setData(Uri.parse("mailto:"));
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
					AndroidUtils.startActivityIfSafe(activity, intent);
					return false;
				}));

		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_social_github)
				.setTitle(getString(R.string.github_discussion))
				.setDescription(getString(R.string.open_issue_on_github_descr))
				.setListener((uiAdapter, view, item, isChecked) -> {
					Uri uri = Uri.parse(getString(R.string.discussion_github));
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					AndroidUtils.startActivityIfSafe(activity, intent);
					return false;
				}));

		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_social_telegram)
				.setTitle(getString(R.string.telegram_chats))
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					TelegramChatsFragment.showInstance(manager, telegramChats);
					return false;
				}));

		for (SocialNetwork network : SocialNetwork.values()) {
			String url = getString(network.urlId);
			items.add(new ContextMenuItem(null)
					.setIcon(network.iconId)
					.setTitle(getString(network.titleId))
					.setDescription(url)
					.setListener((uiAdapter, view, item, isChecked) -> {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						AndroidUtils.startActivityIfSafe(activity, intent);
						return false;
					}));
		}
	}

	private void createReportIssuesCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.report_an_issues)));

		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_social_github)
				.setTitle(getString(R.string.open_issue_on_github))
				.setDescription(getString(R.string.open_issue_on_github_descr))
				.setListener((uiAdapter, view, item, isChecked) -> {
					Uri uri = Uri.parse(getString(R.string.issues_github));
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					AndroidUtils.startActivityIfSafe(activity, intent);
					return false;
				}));

		File exceptionLog = app.getAppPath(EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(new ContextMenuItem(null)
					.setIcon(R.drawable.ic_action_bug_outlined_send)
					.setTitle(getString(R.string.send_crash_log))
					.setDescription(getString(R.string.send_crash_log_descr))
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.getFeedbackHelper().sendCrashLog();
						return false;
					}));
		}
		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_file_report_outlined_send)
				.setTitle(getString(R.string.send_logcat_log))
				.setDescription(getString(R.string.send_logcat_log_descr))
				.setListener((uiAdapter, view, item, isChecked) -> {
					activity.readAndSaveLogs();
					return false;
				}));
	}

	private void createAboutCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(getString(R.string.about_osmand)));
		boolean nightMode = !app.getSettings().isLightContent();

		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_osmand_logo)
				.setColor(ColorUtilities.getOsmandIconColor(app, nightMode))
				.setUseNaturalIconColor(true)
				.setTitle(getString(R.string.about_osmand))
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					HelpArticleDialogFragment.showInstance(manager, getString(R.string.osmand_about), getString(R.string.about_osmand));
					return false;
				}));

		String version = Version.getFullVersionWithReleaseDate(app);
		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_clipboard_notes)
				.setTitle(getString(R.string.what_is_new))
				.setDescription(version)
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					HelpArticleDialogFragment.showInstance(manager, getString(R.string.docs_latest_version), getString(R.string.what_is_new));
					return false;
				})
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareMenu.copyToClipboardWithToast(adapter.getContext(), version, Toast.LENGTH_SHORT);
					return false;
				}));

		items.add(new ContextMenuItem(null)
				.setIcon(R.drawable.ic_action_gsave_dark)
				.setTitle(getString(R.string.free_versions))
				.setDescription(getString(R.string.free_versions_descr))
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					HelpArticleDialogFragment.showInstance(manager, getString(R.string.docs_free_versions), getString(R.string.free_versions));
					return false;
				}));
	}

	@NonNull
	private ContextMenuItem createCategory(@NonNull String title) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setCategory(true)
				.setLayout(R.layout.help_category_header);
	}

	@NonNull
	private String getString(@StringRes int resId) {
		return app.getString(resId);
	}

	@NonNull
	public static String getArticleName(@NonNull OsmandApplication app, @NonNull String key) {
		String propertyName = key.replace("-", "_");
		String value = AndroidUtils.getStringByProperty(app, "help_article_" + propertyName + "_name");
		return value != null ? value : Algorithms.capitalizeFirstLetterAndLowercase(key.replace("-", " "));
	}

	@NonNull
	public static String getTelegramChatName(@NonNull OsmandApplication app, @NonNull String key) {
		int startIndex = key.indexOf("(");
		int endIndex = key.indexOf(")");

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			String langKey = key.substring(startIndex, endIndex);
			String value = AndroidUtils.getStringByProperty(app, "lang_" + langKey);
			if (!Algorithms.isEmpty(value)) {
				String telegram = app.getString(R.string.telegram);
				return app.getString(R.string.ltr_or_rtl_combine_via_space, telegram, value);
			}
		}
		return key;
	}
}
