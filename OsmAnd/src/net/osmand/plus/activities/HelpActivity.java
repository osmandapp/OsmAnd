package net.osmand.plus.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;
import net.osmand.plus.helpers.FeedbackHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.BaseLogcatActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.ItemLongClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.jetbrains.annotations.NotNull;

import java.io.File;


public class HelpActivity extends BaseLogcatActivity implements OnItemClickListener, OnItemLongClickListener {

	public static final int COLLAPSED_TELEGRAM_CHATS_COUNT = 3;
	public static final int NULL_ID = -1;

	public static final String TELEGRAM_CHATS_EXPANDED = "telegram_chats_expanded";
	public static final int LOGCAT_READ_MS = 5 * 1000;

	private OsmandApplication app;
	private ContextMenuListAdapter mAdapter;
	private boolean telegramChatsExpanded;
	private ListView listView;

	private enum TelegramDiscussion {
		ENGLISH(R.string.lang_en, R.string.discussion_telegram_english),
		RUSSIAN(R.string.lang_ru, R.string.discussion_telegram_russian),
		GERMAN(R.string.lang_de, R.string.discussion_telegram_german),
		ITALIAN(R.string.lang_it, R.string.discussion_telegram_italian),
		FRENCH(R.string.lang_fr, R.string.discussion_telegram_french),
		POLISH(R.string.lang_pl, R.string.discussion_telegram_polish),
		PORTUGUESE_BRAZIL(R.string.lang_pt_br, R.string.discussion_telegram_portuguese_brazil),
		SPANISH(R.string.lang_es, R.string.discussion_telegram_spanish),
		UKRAINIAN(R.string.lang_uk, R.string.discussion_telegram_ukrainian);

		private final int langTitleId;
		private final int urlId;

		TelegramDiscussion(int langTitleId, int urlId) {
			this.langTitleId = langTitleId;
			this.urlId = urlId;
		}

		public String getLangTitle(@NonNull Context ctx) {
			return ctx.getString(langTitleId);
		}

		public String getUrl(@NonNull Context ctx) {
			return ctx.getString(urlId);
		}
	}

	private enum SocialNetwork {
		TWITTER(R.string.twitter, R.string.community_twitter, R.drawable.ic_action_social_twitter),
		REDDIT(R.string.reddit, R.string.community_reddit, R.drawable.ic_action_social_reddit),
		FACEBOOK(R.string.facebook, R.string.community_facebook, R.drawable.ic_action_social_facebook);

		SocialNetwork(int titleId, int urlId, int iconId) {
			this.titleId = titleId;
			this.urlId = urlId;
			this.iconId = iconId;
		}

		private final int titleId;
		private final int urlId;
		private final int iconId;

		public String getTitle(Context ctx) {
			return ctx.getString(titleId);
		}

		public String getUrl(Context ctx) {
			return ctx.getString(urlId);
		}

		public int getIconId() {
			return iconId;
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(TELEGRAM_CHATS_EXPANDED, telegramChatsExpanded);
	}

	private void readBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			telegramChatsExpanded = savedInstanceState.getBoolean(TELEGRAM_CHATS_EXPANDED);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_help_screen);
		readBundle(savedInstanceState);
		listView = findViewById(android.R.id.list);
		createItems();
		setTitle(R.string.shared_string_help);
		setupHomeButton();
	}

	private void createItems() {
		ContextMenuAdapter menu = new ContextMenuAdapter(app);

		createBeginWithOsmandItems(menu);
		createFeaturesItems(menu);
		createPluginsItems(menu);
		createHelpUsToImproveItems(menu);
		createOtherItems(menu);
		createDiscussionItems(menu);
		createSocialNetworksItems(menu);

		boolean lightContent = app.getSettings().isLightContent();

		ViewCreator viewCreator = new ViewCreator(this, !lightContent);
		viewCreator.setDefaultLayoutId(R.layout.two_line_with_images_list_item);
		mAdapter = menu.toListAdapter(this, viewCreator);

		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		int dividerColor = ColorUtilities.getDividerColor(this, !lightContent);
		Drawable dividerDrawable = new ColorDrawable(dividerColor);
		listView.setDivider(dividerDrawable);
		listView.setDividerHeight(AndroidUtils.dpToPx(this, 1f));
		listView.setBackgroundColor(ColorUtilities.getListBgColor(app, !lightContent));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuItem item = mAdapter.getItem(position);
		ItemClickListener listener = item.getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(mAdapter, view, item, false);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ItemLongClickListener listener = mAdapter.getItem(position).getItemLongClickListener();
		if (listener != null) {
			listener.onContextMenuLongClick(mAdapter, position, position, false, null);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void createBeginWithOsmandItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.begin_with_osmand_menu_group));
		contextMenuAdapter.addItem(createItem(R.string.first_usage_item,
				R.string.first_usage_item_description, "feature_articles/start.html"));
		contextMenuAdapter.addItem(createItem(R.string.shared_string_navigation,
				R.string.navigation_item_description, "feature_articles/navigation.html"));
		contextMenuAdapter.addItem(createItem(R.string.faq_item,
				R.string.faq_item_description, "feature_articles/faq.html"));
		contextMenuAdapter.addItem(createItem(R.string.map_legend,
				R.string.legend_item_description, "feature_articles/map-legend.html"));
	}

	private void createDiscussionItems(ContextMenuAdapter adapter) {
		adapter.addItem(createCategory(R.string.shared_string_discussion));
		adapter.addItem(createSocialItem(getString(R.string.github),
				getString(R.string.discussion_github),
				R.drawable.ic_action_social_github));
		TelegramDiscussion[] discussions = TelegramDiscussion.values();
		int countToShow = telegramChatsExpanded ? discussions.length : COLLAPSED_TELEGRAM_CHATS_COUNT;
		for (int i = 0; i < countToShow; i++) {
			TelegramDiscussion discussion = discussions[i];
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String title = String.format(pattern, getString(R.string.telegram), discussion.getLangTitle(this));
			adapter.addItem(createSocialItem(title, discussion.getUrl(this), R.drawable.ic_action_social_telegram));
		}
		if (!telegramChatsExpanded) {
			adapter.addItem(createViewAllButton());
		}
	}

	private void createSocialNetworksItems(ContextMenuAdapter adapter) {
		adapter.addItem(createCategory(R.string.follow_us));
		for (SocialNetwork n : SocialNetwork.values()) {
			String title = n.getTitle(this);
			String url = n.getUrl(this);
			ContextMenuItem item = createSocialItem(title, url, n.getIconId());
			adapter.addItem(item);
		}
	}

	private void createHelpUsToImproveItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.help_us_to_improve_menu_group));
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.help_to_improve_item));

		File exceptionLog = app.getAppPath(FeedbackHelper.EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitle(getString(R.string.send_crash_log))
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.getFeedbackHelper().sendCrashLog();
						return false;
					}));
		}
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitle(getString(R.string.send_logcat_log))
				.setListener((uiAdapter, view, item, isChecked) -> {
					readAndSaveLogs();
					return false;
				}));
	}

	private void readAndSaveLogs() {
		logs.clear();

		startLogcatAsyncTask();
		setSupportProgressBarIndeterminateVisibility(true);

		app.runInUIThread(() -> {
			stopLogcatAsyncTask();
			startSaveLogsAsyncTask();
			setSupportProgressBarIndeterminateVisibility(false);
		}, LOGCAT_READ_MS);
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLogcatAsyncTask();
	}

	private void createFeaturesItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.features_menu_group));
		contextMenuAdapter.addItem(createItem(R.string.map_viewing_item, NULL_ID,
				"feature_articles/map-viewing.html"));
		contextMenuAdapter.addItem(createItem(R.string.search_on_the_map_item, NULL_ID,
				"feature_articles/find-something-on-map.html"));
		contextMenuAdapter.addItem(createItem(R.string.planning_trip_item, NULL_ID,
				"feature_articles/trip-planning.html"));
		contextMenuAdapter.addItem(createItem(R.string.navigation_profiles_item, NULL_ID,
				"feature_articles/navigation-profiles.html"));
		contextMenuAdapter.addItem(createItem(R.string.osmand_purchases_item, NULL_ID,
				"feature_articles/osmand_purchases.html"));
		contextMenuAdapter.addItem(createItem(R.string.osm_live_subscriptions, NULL_ID,
				"feature_articles/subscription.html"));
		contextMenuAdapter.addItem(createItem(R.string.favorites_item, NULL_ID,
				"feature_articles/favourites.html"));
		contextMenuAdapter.addItem(createItem(R.string.map_markers_item, NULL_ID,
				"feature_articles/map-markers.html"));
		contextMenuAdapter.addItem(createItem(R.string.travel_item, NULL_ID,
				"feature_articles/travel.html"));
		contextMenuAdapter.addItem(createItem(R.string.plan_a_route, NULL_ID,
				"feature_articles/plan-route.html"));
		contextMenuAdapter.addItem(createItem(R.string.radius_ruler_item, NULL_ID,
				"feature_articles/ruler.html"));
		contextMenuAdapter.addItem(createItem(R.string.quick_action_item, NULL_ID,
				"feature_articles/quick-action.html"));
		contextMenuAdapter.addItem(createItem(R.string.mapillary_item, NULL_ID,
				"feature_articles/mapillary.html"));
		contextMenuAdapter.addItem(createItem(R.string.tracker_item, NULL_ID,
				"feature_articles/tracker.html"));
	}

	private void createPluginsItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.plugins_menu_group));
		for (OsmandPlugin plugin : PluginsHelper.getAvailablePlugins()) {
			String helpFileName = plugin.getHelpFileName();
			if (helpFileName != null) {
				contextMenuAdapter.addItem(createPluginItem(plugin.getName(), plugin.getLogoResourceId(),
						helpFileName));
			}
		}
	}

	private void createOtherItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.other_menu_group));
		contextMenuAdapter.addItem(createItem(R.string.instalation_troubleshooting_item, NULL_ID,
				"feature_articles/installation-and-troubleshooting.html"));
		contextMenuAdapter.addItem(createItem(R.string.versions_item, NULL_ID,
				"feature_articles/changes.html"));


		contextMenuAdapter.addItem(createItem(R.string.what_is_new, NULL_ID, getString(R.string.docs_latest_version)));

		String releaseDate = "";
		if (!getString(R.string.app_edition).isEmpty()) {
			releaseDate = ", " + getString(R.string.shared_string_release).toLowerCase() + ": "
					+ getString(R.string.app_edition);
		}
		String version = Version.getFullVersion(app) + releaseDate;
		ShowArticleOnTouchListener listener = new ShowArticleOnTouchListener(
				"feature_articles/about.html", this, version);
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitle(getString(R.string.shared_string_about))
				.setDescription(version)
				.setListener(listener)
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareMenu.copyToClipboardWithToast(adapter.getContext(), version, Toast.LENGTH_SHORT);
					return false;
				}));
	}

	// Helper methods
	private ContextMenuItem createCategory(@StringRes int titleRes) {
		return new ContextMenuItem(null)
				.setCategory(true)
				.setTitle(getString(titleRes))
				.setLayout(R.layout.download_item_list_section);
	}

	private ContextMenuItem createItem(@StringRes int titleRes,
	                                   @StringRes int descriptionRes,
	                                   String path) {
		ContextMenuItem item = new ContextMenuItem(null)
				.setTitle(getString(titleRes))
				.setListener(new ShowArticleOnTouchListener(path, this));
		if (descriptionRes != -1) {
			item.setDescription(getString(descriptionRes));
		}
		return item;
	}

	private ContextMenuItem createPluginItem(String title,
	                                         @DrawableRes int icon,
	                                         String path) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setIcon(icon)
				.setListener(new ShowArticleOnTouchListener(path, this));
	}

	private ContextMenuItem createSocialItem(String title,
	                                         String url,
	                                         @DrawableRes int icon) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setDescription(url)
				.setIcon(icon)
				.setListener((uiAdapter, view, item, isChecked) -> {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					AndroidUtils.startActivityIfSafe(this, intent);
					return false;
				});
	}

	private ContextMenuItem createViewAllButton() {
		return new ContextMenuItem(null)
				.setLayout(R.layout.title_with_right_icon_list_item)
				.setTitle(getString(R.string.shared_string_view_all))
				.setIcon(R.drawable.ic_action_arrow_down)
				.setListener((uiAdapter, view, item, isChecked) -> {
					telegramChatsExpanded = true;
					int pos = listView.getFirstVisiblePosition();
					createItems();
					listView.setSelection(pos);
					return false;
				});
	}

	@NonNull
	@Override
	protected String getFilterLevel() {
		return "";
	}

	@Override
	protected void onLogEntryAdded() {
	}

	private static class ShowArticleOnTouchListener implements ItemClickListener {

		private final String path;
		private final FragmentActivity ctx;
		private final String mTitle;

		private ShowArticleOnTouchListener(String path, FragmentActivity ctx) {
			this.path = path;
			this.ctx = ctx;
			mTitle = null;
		}

		private ShowArticleOnTouchListener(String path, FragmentActivity ctx, String title) {
			this.path = path;
			this.ctx = ctx;
			mTitle = title;
		}

		@Override
		public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
			String LATEST_CHANGES_URL = ctx.getString(R.string.docs_latest_version);
			if (LATEST_CHANGES_URL.equals(path)) {
				OsmandApplication app = (OsmandApplication) ctx.getApplication();
				boolean nightMode = !app.getSettings().isLightContent();
				AndroidUtils.openUrl(ctx, Uri.parse(LATEST_CHANGES_URL), nightMode);
			} else {
				String title = mTitle == null ? item.getTitle() : mTitle;
				HelpArticleDialogFragment.instantiateWithAsset(path, title)
						.show(ctx.getSupportFragmentManager(), "DIALOG_HELP_ARTICLE");
			}
			return false;
		}
	}
}