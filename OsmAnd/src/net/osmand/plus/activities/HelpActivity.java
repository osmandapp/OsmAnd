package net.osmand.plus.activities;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
	public static final String OSMAND_POLL_HTML = "http://osmand.net/android-poll.html";
	private ArrayAdapter<ContextMenuItem> mAdapter;

	//public static final String OSMAND_MAP_LEGEND = "http://osmand.net/help/map-legend_default.png";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_help_screen);

		ContextMenuAdapter contextMenuAdapter = new ContextMenuAdapter();
		contextMenuAdapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);

		contextMenuAdapter.addItem(createCategory(R.string.begin_with_osmand_menu_group));
		createBeginWithOsmandItems(contextMenuAdapter);
		contextMenuAdapter.addItem(createCategory(R.string.features_menu_group));
		createFeaturesItems(contextMenuAdapter);
		contextMenuAdapter.addItem(createCategory(R.string.plugins_menu_group));
		createPluginsItems(contextMenuAdapter);
		contextMenuAdapter.addItem(createCategory(R.string.help_us_to_improve_menu_group));
		createHelpUsToImproveItems(contextMenuAdapter);
		contextMenuAdapter.addItem(createCategory(R.string.other_menu_group));
		createOtherItems(contextMenuAdapter);

		mAdapter = contextMenuAdapter.createListAdapter(this, getMyApplication().getSettings().isLightContent());

		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(this);

		setTitle(R.string.shared_string_help);
		setupHomeButton();
	}

	private void createHelpUsToImproveItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.help_to_improve_item).createItem());
	}

	private ContextMenuItem createCategory(@StringRes int titleRes) {
		return new ContextMenuItem.ItemBuilder().setTitle(
				getString(titleRes)).setCategory(true)
				.setLayout(R.layout.download_item_list_section).createItem();
	}

	private ContextMenuItem createItem(@StringRes int titleRes,
									   @StringRes String path) {
		return new ContextMenuItem.ItemBuilder()
				.setTitle(getString(titleRes))
				.setListener(new ShowArticleOnTouchListener(path, this))
				.createItem();
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

	private ContextMenuItem createItem(@StringRes int titleRes,
									   @StringRes int descriptionRes,
									   String path) {
		return new ContextMenuItem.ItemBuilder()
				.setTitle(getString(titleRes))
				.setDescription(getString(descriptionRes))
				.setListener(new ShowArticleOnTouchListener(path, this))
				.createItem();
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
		contextMenuAdapter.addItem(createItem(R.string.first_usage_item, R.string.first_usage_item_description,
				"feature_articles/start.html"));
		contextMenuAdapter.addItem(createItem(R.string.shared_string_navigation, R.string.navigation_item_description,
				"feature_articles/navigation.html"));
		contextMenuAdapter.addItem(createItem(R.string.faq_item, R.string.faq_item_description,
				"feature_articles/faq.html"));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mAdapter.getItem(position).getItemClickListener()
				.onContextMenuClick(mAdapter, position, position, false);
	}

	private void createFeaturesItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createItem(R.string.map_viewing_item,
				"feature_articles/map-viewing.html"));
		contextMenuAdapter.addItem(createItem(R.string.search_on_the_map_item,
				"feature_articles/find-something-on-map.html"));
		contextMenuAdapter.addItem(createItem(R.string.planning_trip_item,
				"feature_articles/trip-planning.html"));
		contextMenuAdapter.addItem(createItem(R.string.map_legend,
				"feature_articles/map-legend.html"));
	}

	private void createPluginsItems(ContextMenuAdapter contextMenuAdapter) {
		for (final OsmandPlugin osmandPlugin : OsmandPlugin.getAvailablePlugins()) {
			final String helpFileName = osmandPlugin.getHelpFileName();
			if (helpFileName != null) {
				contextMenuAdapter.addItem(createPluginItem(osmandPlugin.getName(), osmandPlugin.getLogoResourceId(),
						helpFileName));
			}
		}
	}

	private void createOtherItems(ContextMenuAdapter contextMenuAdapter) {
		contextMenuAdapter.addItem(createItem(R.string.instalation_troubleshooting_item,
				"feature_articles/installation-and-troubleshooting.html"));
		contextMenuAdapter.addItem(createItem(R.string.techical_articles_item,
				"feature_articles/technical-articles.html"));
		contextMenuAdapter.addItem(createItem(R.string.versions_item,
				"feature_articles/changes.html"));

		String releasedate = "";
		if (!this.getString(R.string.app_edition).equals("")) {
			releasedate = ", " + this.getString(R.string.shared_string_release).toLowerCase() + ": " + this.getString(R.string.app_edition);
		}
		String version = Version.getFullVersion(getMyApplication()) + releasedate;
		ShowArticleOnTouchListener listener = new ShowArticleOnTouchListener(
				"feature_articles/about.html", this, version);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitle(getString(R.string.shared_string_about))
				.setDescription(version).setListener(listener).createItem());
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
		public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked) {
			String title = mTitle == null ? adapter.getItem(position).getTitle() : mTitle;
			HelpArticleDialogFragment.instantiateWithAsset(filename, title)
					.show(ctx.getSupportFragmentManager(), "DIALOG_HELP_ARTICLE");
			return false;
		}
	}
}
