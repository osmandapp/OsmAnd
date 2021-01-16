package net.osmand.plus.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.dialogs.HelpArticleDialogFragment;

public class HelpActivity extends OsmandActionBarActivity implements AdapterView.OnItemClickListener {

	//	public static final String DIALOG = "dialog";
	@IdRes
	public static final String OSMAND_POLL_HTML = "https://osmand.net/android-poll.html";
	public static final int NULL_ID = -1;
	private ArrayAdapter<ContextMenuItem> mAdapter;

	//public static final String OSMAND_MAP_LEGEND = "https://osmand.net/help/map-legend_default.png";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_help_screen);

		ContextMenuAdapter contextMenuAdapter = new ContextMenuAdapter(getMyApplication());
		contextMenuAdapter.setDefaultLayoutId(R.layout.two_line_with_images_list_item);

		createBeginWithOsmandItems(contextMenuAdapter);
		createFeaturesItems(contextMenuAdapter);
		createPluginsItems(contextMenuAdapter);
		createHelpUsToImproveItems(contextMenuAdapter);
		createOtherItems(contextMenuAdapter);
		createSocialNetworksItems(contextMenuAdapter);

		boolean lightContent = getMyApplication().getSettings().isLightContent();

		mAdapter = contextMenuAdapter.createListAdapter(this, lightContent);

		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(this);
		int dividerColor = lightContent ? R.color.divider_color_light : R.color.divider_color_dark;
		Drawable dividerDrawable = new ColorDrawable(ContextCompat.getColor(this, dividerColor));
		listView.setDivider(dividerDrawable);
		listView.setDividerHeight(AndroidUtils.dpToPx(this, 1f));
		listView.setBackgroundColor(getResources().getColor(
				lightContent ? R.color.list_background_color_light : R.color.list_background_color_dark));

		setTitle(R.string.shared_string_help);
		setupHomeButton();
	}



	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ContextMenuAdapter.ItemClickListener listener =
				mAdapter.getItem(position).getItemClickListener();
		if (listener != null) {
			listener.onContextMenuClick(mAdapter, position, position, false, null);
		}
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

	private void createSocialNetworksItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.follow_us));
		contextMenuAdapter.addItem(createSocialItem(R.string.twitter, R.string.twitter_address,
				R.drawable.ic_action_social_twitter));
	        contextMenuAdapter.addItem(createSocialItem(R.string.reddit, R.string.reddit_address,
				R.drawable.ic_action_social_reddit));
		contextMenuAdapter.addItem(createSocialItem(R.string.facebook, R.string.facebook_address,
				R.drawable.ic_action_social_facebook));
		contextMenuAdapter.addItem(createSocialItem(R.string.vk, R.string.vk_address,
				R.drawable.ic_action_social_vk));
	}

	private void createHelpUsToImproveItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createCategory(R.string.help_us_to_improve_menu_group));
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.help_to_improve_item).createItem());
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

		String releasedate = "";
		if (!this.getString(R.string.app_edition).isEmpty()) {
			releasedate = ", " + this.getString(R.string.shared_string_release).toLowerCase() + ": " + this.getString(R.string.app_edition);
		}
		String version = Version.getFullVersion(getMyApplication()) + releasedate;
		ShowArticleOnTouchListener listener = new ShowArticleOnTouchListener(
				"feature_articles/about.html", this, version);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitle(getString(R.string.shared_string_about))
				.setDescription(version).setListener(listener).createItem());
	}

	// Helper metods
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

	private ContextMenuItem createSocialItem(@StringRes int title,
											 @StringRes int urlRes,
											 @DrawableRes int icon) {
		final String url = getString(urlRes);
		return new ContextMenuItem.ItemBuilder()
				.setTitle(getString(title))
				.setDescription(url)
				.setIcon(icon)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
													  int itemId,
													  int position,
													  boolean isChecked,
													  int[] viewCoordinates) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						startActivity(intent);
						return false;
					}
				})
				.createItem();
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
