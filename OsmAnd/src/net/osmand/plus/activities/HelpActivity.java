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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.ContextMenuItem.ItemBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.development.BaseLogcatActivity;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;

import java.io.File;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class HelpActivity extends BaseLogcatActivity implements OnItemClickListener, OnItemLongClickListener {

	public static final String OSMAND_POLL_HTML = "https://osmand.net/android-poll.html";
	public static final String GITHUB_DISCUSSIONS_URL = "https://github.com/osmandapp/OsmAnd/discussions";
	// public static final String OSMAND_MAP_LEGEND = "https://osmand.net/help/map-legend_default.png";

	public static final int COLLAPSED_TELEGRAM_CHATS_COUNT = 3;
	public static final int NULL_ID = -1;

	public static final String TELEGRAM_CHATS_EXPANDED = "telegram_chats_expanded";
	// public static final String DIALOG = "dialog";

	private ArrayAdapter<ContextMenuItem> mAdapter;
	private boolean telegramChatsExpanded;
	private ListView listView;

	private enum TelegramDiscussion {
		ENGLISH(R.string.lang_en, "https://t.me/OsmAndMaps"),
		RUSSIAN(R.string.lang_ru, "https://t.me/ruosmand"),
		GERMAN(R.string.lang_de, "https://t.me/deosmand"),
		ITALIAN(R.string.lang_it, "https://t.me/itosmand"),
		FRENCH(R.string.lang_fr, "https://t.me/frosmand"),
		POLISH(R.string.lang_pl, "https://t.me/osmand_pl"),
		PORTUGUESE_BRAZIL(R.string.lang_pt_br, "https://t.me/brosmand"),
		SPANISH(R.string.lang_es, "https://t.me/osmand_es"),
		UKRAINIAN(R.string.lang_uk, "https://t.me/uaosmand");

		private final int langTitleId;
		private final String url;

		TelegramDiscussion(int langTitleId, String url) {
			this.langTitleId = langTitleId;
			this.url = url;
		}

		public String getLangTitle(Context ctx) {
			return ctx.getString(langTitleId);
		}

		public String getUrl() {
			return url;
		}
	}

	private enum SocialNetwork {
		TWITTER(R.string.twitter, R.string.twitter_address, R.drawable.ic_action_social_twitter),
		REDDIT(R.string.reddit, R.string.reddit_address, R.drawable.ic_action_social_reddit),
		FACEBOOK(R.string.facebook, R.string.facebook_address, R.drawable.ic_action_social_facebook);

		SocialNetwork(int titleId, int urlId, int iconId) {
			this.titleId = titleId;
			this.urlId = urlId;
			this.iconId = iconId;
		}

		private int titleId;
		private int urlId;
		private int iconId;

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
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_help_screen);
		readBundle(savedInstanceState);
		listView = findViewById(android.R.id.list);
		createItems();
		setTitle(R.string.shared_string_help);
		setupHomeButton();
	}

	private void createItems() {
		ContextMenuAdapter contextMenuAdapter = new ContextMenuAdapter(getMyApplication());
		contextMenuAdapter.setDefaultLayoutId(R.layout.two_line_with_images_list_item);

		createBeginWithOsmandItems(contextMenuAdapter);
		createFeaturesItems(contextMenuAdapter);
		createPluginsItems(contextMenuAdapter);
		createHelpUsToImproveItems(contextMenuAdapter);
		createOtherItems(contextMenuAdapter);
		createDiscussionItems(contextMenuAdapter);
		createSocialNetworksItems(contextMenuAdapter);

		boolean lightContent = getMyApplication().getSettings().isLightContent();

		mAdapter = contextMenuAdapter.createListAdapter(this, lightContent);

		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		int dividerColor = lightContent ? R.color.divider_color_light : R.color.divider_color_dark;
		Drawable dividerDrawable = new ColorDrawable(ContextCompat.getColor(this, dividerColor));
		listView.setDivider(dividerDrawable);
		listView.setDividerHeight(AndroidUtils.dpToPx(this, 1f));
		listView.setBackgroundColor(getResources().getColor(
				lightContent ? R.color.list_background_color_light : R.color.list_background_color_dark));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuAdapter.ItemClickListener listener = mAdapter.getItem(position).getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(mAdapter, position, position, false, null);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuAdapter.ItemLongClickListener listener = mAdapter.getItem(position).getItemLongClickListener();
		if (listener != null) {
			listener.onContextMenuLongClick(mAdapter, position, position, false, null);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
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
		adapter.addItem(createSocialItem(getString(R.string.github), GITHUB_DISCUSSIONS_URL,
				R.drawable.ic_action_social_github));
		TelegramDiscussion[] discussions = TelegramDiscussion.values();
		int countToShow = telegramChatsExpanded ? discussions.length : COLLAPSED_TELEGRAM_CHATS_COUNT;
		for (int i = 0; i < countToShow; i++) {
			TelegramDiscussion discussion = discussions[i];
			String pattern = getString(R.string.ltr_or_rtl_combine_via_space);
			String title = String.format(pattern, getString(R.string.telegram), discussion.getLangTitle(this));
			adapter.addItem(createSocialItem(title, discussion.getUrl(), R.drawable.ic_action_social_telegram));
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
		final OsmandApplication app = getMyApplication();

		contextMenuAdapter.addItem(createCategory(R.string.help_us_to_improve_menu_group));
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.help_to_improve_item).createItem());

		final File exceptionLog = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (exceptionLog.exists()) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitle(getString(R.string.send_crash_log))
					.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
						app.sendCrashLog(exceptionLog);
						return false;
					}).createItem()
			);
		}
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitle(getString(R.string.send_logcat_log))
				.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					startSaveLogsAsyncTask();
					return false;
				}).createItem()
		);
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
		contextMenuAdapter.addItem(createItem(R.string.subscription_osmandlive_item, NULL_ID,
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
		for (final OsmandPlugin osmandPlugin : OsmandPlugin.getAvailablePlugins()) {
			final String helpFileName = osmandPlugin.getHelpFileName();
			if (helpFileName != null) {
				contextMenuAdapter.addItem(createPluginItem(osmandPlugin.getName(), osmandPlugin.getLogoResourceId(),
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
		contextMenuAdapter.addItem(createItem(R.string.what_is_new, NULL_ID,
				"feature_articles/osmand-3-9-released.html"));

		String releaseDate = "";
		if (!getString(R.string.app_edition).isEmpty()) {
			releaseDate = ", " + getString(R.string.shared_string_release).toLowerCase() + ": "
					+ getString(R.string.app_edition);
		}
		String version = Version.getFullVersion(getMyApplication()) + releaseDate;
		ShowArticleOnTouchListener listener = new ShowArticleOnTouchListener(
				"feature_articles/about.html", this, version);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitle(getString(R.string.shared_string_about))
				.setDescription(version)
				.setListener(listener)
				.setLongClickListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					ShareDialog.copyToClipboardWithToast(adapter.getContext(), version, Toast.LENGTH_SHORT);
					return false;
				})
				.createItem());
	}

	// Helper methods
	private ContextMenuItem createCategory(@StringRes int titleRes) {
		return new ContextMenuItem.ItemBuilder().setTitle(
				getString(titleRes)).setCategory(true)
				.setLayout(R.layout.download_item_list_section).createItem();
	}

	private ContextMenuItem createItem(@StringRes int titleRes,
									   @StringRes int descriptionRes,
									   String path) {
		ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder()
				.setTitle(getString(titleRes))
				.setListener(new ShowArticleOnTouchListener(path, this));
		if (descriptionRes != -1) {
			builder.setDescription(getString(descriptionRes));
		}
		return builder.createItem();
	}

	private ContextMenuItem createPluginItem(String title,
											 @DrawableRes int icon,
											 String path) {
		return new ContextMenuItem.ItemBuilder()
				.setTitle(title)
				.setIcon(icon)
				.setListener(new ShowArticleOnTouchListener(path, this))
				.createItem();
	}

	private ContextMenuItem createSocialItem(String title,
	                                         String url,
	                                         @DrawableRes int icon) {
		return new ContextMenuItem.ItemBuilder()
				.setTitle(title)
				.setDescription(url)
				.setIcon(icon)
				.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					if (AndroidUtils.isIntentSafe(this, intent)) {
						startActivity(intent);
					}
					return false;
				})
				.createItem();
	}

	private ContextMenuItem createViewAllButton() {
		return new ItemBuilder()
				.setLayout(R.layout.title_with_right_icon_list_item)
				.setTitle(getString(R.string.shared_string_view_all))
				.setIcon(R.drawable.ic_action_arrow_down)
				.setListener((adapter, itemId, position, isChecked, viewCoordinates) -> {
					telegramChatsExpanded = true;
					int pos = listView.getFirstVisiblePosition();
					createItems();
					listView.setSelection(pos);
					return false;
				})
				.createItem();
	}

	@NonNull
	@Override
	protected String getFilterLevel() {
		return "";
	}

	@Override
	protected void onLogEntryAdded() {
	}

	private static class ShowArticleOnTouchListener implements ContextMenuAdapter.ItemClickListener {
		private final String filename;
		private final FragmentActivity ctx;
		private final String mTitle;

		private ShowArticleOnTouchListener(String filename, FragmentActivity ctx) {
			this.filename = filename;
			this.ctx = ctx;
			mTitle = null;
		}

		private ShowArticleOnTouchListener(String filename, FragmentActivity ctx, String title) {
			this.filename = filename;
			this.ctx = ctx;
			mTitle = title;
		}

		@Override
		public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
			String title = mTitle == null ? adapter.getItem(position).getTitle() : mTitle;
			HelpArticleDialogFragment.instantiateWithAsset(filename, title)
					.show(ctx.getSupportFragmentManager(), "DIALOG_HELP_ARTICLE");
			return false;
		}
	}
}