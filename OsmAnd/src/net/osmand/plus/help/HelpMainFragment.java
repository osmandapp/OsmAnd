package net.osmand.plus.help;

import static net.osmand.plus.feedback.FeedbackHelper.EXCEPTION_PATH;
import static net.osmand.plus.help.HelpArticleUtils.createArticleItem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.ItemLongClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelpMainFragment extends BaseFullScreenFragment implements OnItemClickListener, OnItemLongClickListener {

	public static final String TAG = HelpMainFragment.class.getSimpleName();

	private static final int MAX_VISIBLE_POPULAR_ARTICLES = 5;
	private static final String TROUBLESHOOTING_CATEGORY = "Troubleshooting";

	private HelpArticlesHelper articlesHelper;

	private ContextMenuListAdapter adapter;
	private ListView listView;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Nullable
	@Override
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		articlesHelper = app.getHelpArticlesHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.help_main_screen, container, false);

		listView = view.findViewById(R.id.list_view);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);

		updateContent();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		HelpActivity activity = (HelpActivity) requireActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_help);
		}
	}

	public void updateContent() {
		HelpActivity activity = (HelpActivity) requireActivity();

		List<ContextMenuItem> items = createItems();
		if (!Algorithms.isEmpty(items)) {
			items.get(items.size() - 1).setHideDivider(true);
			items.add(new ContextMenuItem(null).setLayout(R.layout.card_bottom_divider));
		}
		ContextMenuAdapter menuAdapter = new ContextMenuAdapter(app);
		for (ContextMenuItem item : items) {
			menuAdapter.addItem(item);
		}

		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.help_list_item);
		adapter = menuAdapter.toListAdapter(activity, viewCreator);

		listView.setAdapter(adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.help_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
			return true;
		} else if (id == R.id.action_menu) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showOptionsMenu(activity.findViewById(R.id.action_menu));
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void showOptionsMenu(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		HelpActivity activity = (HelpActivity) requireActivity();

		File exceptionLog = app.getAppPath(FeedbackHelper.EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			items.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.send_crash_log)
					.setIcon(getContentIcon(R.drawable.ic_action_bug_outlined_send))
					.setOnClickListener(v -> app.getFeedbackHelper().sendCrashLog()).create());
		}
		items.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.send_logcat_log)
				.setIcon(getContentIcon(R.drawable.ic_action_file_report_outlined_send))
				.setOnClickListener(v -> activity.readAndSaveLogs()).create());

		items.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.copy_build_version)
				.showTopDivider(true)
				.setIcon(getContentIcon(R.drawable.ic_action_osmand_logo))
				.setOnClickListener(v -> ShareMenu.copyToClipboardWithToast(activity,
						Version.getFullVersionWithReleaseDate(app), false)).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.simple_popup_menu_item;
		PopUpMenu.show(displayData);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = adapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(adapter, view, item, false);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ItemLongClickListener listener = adapter.getItem(position).getItemLongClickListener();
		if (listener != null) {
			listener.onContextMenuLongClick(adapter, position, position, false, null);
			return true;
		}
		return false;
	}

	@NonNull
	private List<ContextMenuItem> createItems() {
		List<ContextMenuItem> items = new ArrayList<>();

		createPopularArticlesCategory(items);
		createDocsCategories(items);
		createContactUsCategory(items);
		createReportIssuesCategory(items);
		createAboutCategory(items);

		return items;
	}

	private void createPopularArticlesCategory(@NonNull List<ContextMenuItem> items) {
		Map<String, String> popularArticles = articlesHelper.getPopularArticles();
		if (!Algorithms.isEmpty(popularArticles)) {
			items.add(createCategory(app.getString(R.string.most_viewed)));

			int counter = 0;
			for (Map.Entry<String, String> entry : popularArticles.entrySet()) {
				String url = entry.getValue();
				String title = HelpArticleUtils.getArticleName(app, url, entry.getKey());

				items.add(createMenuItem(title, null, R.drawable.ic_action_file_info,
						getArticleItemClickListener(title, url)));

				counter++;
				if (counter >= MAX_VISIBLE_POPULAR_ARTICLES) {
					break;
				}
			}
		}
	}

	private void createDocsCategories(@NonNull List<ContextMenuItem> items) {
		Map<String, HelpArticle> articles = articlesHelper.getArticles();
		if (!Algorithms.isEmpty(articles)) {
			createUserGuideCategory(items, articles);
		}
		HelpArticle article = articles.get(TROUBLESHOOTING_CATEGORY);
		if (article != null && !Algorithms.isEmpty(article.articles)) {
			createTroubleshootingCategory(items, article);
		}
	}

	private void createUserGuideCategory(@NonNull List<ContextMenuItem> items,
			@NonNull Map<String, HelpArticle> articles) {
		items.add(createCategory(app.getString(R.string.user_guide)));

		FragmentActivity activity = requireActivity();
		for (Map.Entry<String, HelpArticle> entry : articles.entrySet()) {
			if (!TROUBLESHOOTING_CATEGORY.equals(entry.getKey())) {
				HelpArticle article = entry.getValue();
				items.add(createArticleItem(activity, article));
			}
		}
	}

	private void createTroubleshootingCategory(@NonNull List<ContextMenuItem> items,
			@NonNull HelpArticle helpArticle) {
		items.add(createCategory(app.getString(R.string.troubleshooting)));

		for (HelpArticle article : helpArticle.articles.values()) {
			int iconId = HelpArticleUtils.getArticleIconId(article);
			String title = HelpArticleUtils.getArticleName(app, article);
			items.add(createMenuItem(title, null, iconId, getArticleItemClickListener(title, article.url)));
		}
	}

	private void createContactUsCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.contact_us)));

		String email = app.getString(R.string.support_email);
		items.add(createMenuItem(app.getString(R.string.contact_support), email,
				R.drawable.ic_action_at_mail, (uiAdapter, view, item, isChecked) -> {
					callActivity(activity -> {
						Intent intent = new Intent(Intent.ACTION_SENDTO);
						intent.setData(Uri.parse("mailto:"));
						intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
						intent.putExtra(Intent.EXTRA_TEXT, app.getFeedbackHelper().getDeviceInfo());
						AndroidUtils.startActivityIfSafe(activity, intent);
					});
					return false;
				}));

		items.add(createMenuItem(app.getString(R.string.github_discussion), app.getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(app.getString(R.string.discussion_github))));

		items.add(createMenuItem(app.getString(R.string.telegram_chats), null,
				R.drawable.ic_action_social_telegram, (uiAdapter, view, item, isChecked) -> {
					callActivity(activity -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						Map<String, String> telegramChats = articlesHelper.getTelegramChats();
						TelegramChatsFragment.showInstance(manager, telegramChats);
					});
					return false;
				}));

		for (SocialNetwork network : SocialNetwork.values()) {
			String url = app.getString(network.urlId);
			items.add(createMenuItem(app.getString(network.titleId), url, network.iconId, getUrlItemClickListener(url)));
		}
	}

	private void createReportIssuesCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.report_an_issues)));

		items.add(createMenuItem(app.getString(R.string.open_issue_on_github), app.getString(R.string.open_issue_on_github_descr),
				R.drawable.ic_action_social_github, getUrlItemClickListener(app.getString(R.string.issues_github))));

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
					callActivity(HelpActivity.class, activity -> activity.readAndSaveLogs());
					return false;
				}));
	}

	private void createAboutCategory(@NonNull List<ContextMenuItem> items) {
		items.add(createCategory(app.getString(R.string.about_osmand)));
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);

		items.add(createMenuItem(app.getString(R.string.about_osmand), null, R.drawable.ic_action_osmand_logo,
				getArticleItemClickListener(app.getString(R.string.about_osmand), app.getString(R.string.osmand_about)))
				.setUseNaturalIconColor(true)
				.setColor(ColorUtilities.getOsmandIconColor(app, nightMode)));

		String version = Version.getFullVersionWithReleaseDate(app);
		items.add(createMenuItem(app.getString(R.string.what_is_new), version, R.drawable.ic_action_clipboard_notes,
				getArticleItemClickListener(app.getString(R.string.what_is_new), app.getString(R.string.docs_latest_version)))
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareMenu.copyToClipboardWithToast(adapter.getContext(), version, false);
					return false;
				}));

		String freeVersions = app.getString(R.string.free_versions);
		items.add(createMenuItem(freeVersions, app.getString(R.string.free_versions_descr),
				R.drawable.ic_action_gsave_dark, getArticleItemClickListener(freeVersions, app.getString(R.string.docs_free_versions))));
	}

	@NonNull
	public ItemClickListener getUrlItemClickListener(@NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			callActivity(activity -> {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				AndroidUtils.startActivityIfSafe(activity, intent);
			});
			return false;
		};
	}

	@NonNull
	public ItemClickListener getArticleItemClickListener(@NonNull String title,
			@NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			callActivity(activity -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				HelpArticleDialogFragment.showInstance(manager, url, title);
			});
			return false;
		};
	}

	@NonNull
	public ContextMenuItem createCategory(@NonNull String title) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setCategory(true)
				.setLayout(R.layout.help_category_header);
	}

	@NonNull
	public ContextMenuItem createMenuItem(@NonNull String title, @Nullable String description,
			@DrawableRes int iconId, @Nullable ItemClickListener listener) {
		return new ContextMenuItem(null)
				.setIcon(iconId)
				.setTitle(title)
				.setDescription(description)
				.setListener(listener);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			HelpMainFragment fragment = new HelpMainFragment();
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.commitAllowingStateLoss();
		}
	}
}