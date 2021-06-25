package net.osmand.plus.wikipedia;

import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;

import java.io.IOException;
import java.util.List;

public class WikipediaPoiMenu {

	private MapActivity mapActivity;
	private OsmandApplication app;
	private OsmandSettings settings;
	private WikipediaPlugin wikiPlugin;
	private boolean nightMode;

	public WikipediaPoiMenu(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.wikiPlugin = OsmandPlugin.getPlugin(WikipediaPlugin.class);
		this.nightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	private ContextMenuAdapter createLayersItems() {
		final int toggleActionStringId = R.string.shared_string_wikipedia;
		final int languageActionStringId = R.string.shared_string_language;
		final int spaceHeight = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_big_item_height);
		final boolean enabled = app.getPoiFilters().isTopWikiFilterSelected();
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
			                                  final int itemId, final int position, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							wikiPlugin.toggleWikipediaPoi(!enabled, null);
						}
					});
				} else if (itemId == languageActionStringId) {
					SelectWikiLanguagesBottomSheet.showInstance(mapActivity, true);
				}
				return false;
			}
		};

		int toggleIconId = R.drawable.ic_plugin_wikipedia;
		int toggleIconColorId;
		if (enabled) {
			toggleIconColorId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		} else {
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}
		String summary = mapActivity.getString(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setDescription(summary)
				.setIcon(toggleIconId)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(enabled).createItem());

		if (enabled) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.list_item_divider)
					.createItem());

			summary = wikiPlugin.getLanguagesSummary();
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(languageActionStringId, mapActivity)
					.setIcon(R.drawable.ic_action_map_language)
					.setDescription(summary)
					.hideCompoundButton(true)
					.setListener(l)
					.createItem());
		}

		final DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (settings.isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		if (downloadThread.shouldDownloadIndexes()) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_download_map, mapActivity)
					.setDescription(app.getString(R.string.wiki_menu_download_descr))
					.setCategory(true)
					.setLayout(R.layout.list_group_title_with_descr).createItem());
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.list_item_icon_and_download)
					.setTitleId(R.string.downloading_list_indexes, mapActivity)
					.hideDivider(true)
					.setLoading(true)
					.setListener(l).createItem());
		} else {
			try {
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = downloadThread.getCurrentDownloadingItemProgress();
				List<IndexItem> wikiIndexes = DownloadResources.findIndexItemsAt(
						app, mapActivity.getMapLocation(), DownloadActivityType.WIKIPEDIA_FILE,
						false, -1, true);
				if (wikiIndexes.size() > 0) {
					adapter.addItem(new ContextMenuItem.ItemBuilder()
							.setTitleId(R.string.shared_string_download_map, mapActivity)
							.setDescription(app.getString(R.string.wiki_menu_download_descr))
							.setCategory(true)
							.setLayout(R.layout.list_group_title_with_descr).createItem());
					for (int i = 0; i < wikiIndexes.size(); i++) {
						final IndexItem indexItem = wikiIndexes.get(i);
						boolean isLastItem = i == wikiIndexes.size() - 1;
						ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder()
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(DownloadActivityType.WIKIPEDIA_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(DownloadActivityType.WIKIPEDIA_FILE.getIconResource())
								.hideDivider(isLastItem)
								.setListener(new ContextMenuAdapter.ItemClickListener() {
									@Override
									public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
										ContextMenuItem item = adapter.getItem(position);
										if (downloadThread.isDownloading(indexItem)) {
											downloadThread.cancelDownload(indexItem);
											if (item != null) {
												item.setProgress(ContextMenuItem.INVALID_ID);
												item.setLoading(false);
												item.setSecondaryIcon(R.drawable.ic_action_import);
												adapter.notifyDataSetChanged();
											}
										} else {
											new DownloadValidationManager(app).startDownload(mapActivity, indexItem);
											if (item != null) {
												item.setProgress(ContextMenuItem.INVALID_ID);
												item.setLoading(true);
												item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
												adapter.notifyDataSetChanged();
											}
										}
										return false;
									}
								})
								.setProgressListener(new ContextMenuAdapter.ProgressListener() {
									@Override
									public boolean onProgressChanged(Object progressObject, int progress,
									                                 ArrayAdapter<ContextMenuItem> adapter,
									                                 int itemId, int position) {
										if (progressObject != null && progressObject instanceof IndexItem) {
											IndexItem progressItem = (IndexItem) progressObject;
											if (indexItem.compareTo(progressItem) == 0) {
												ContextMenuItem item = adapter.getItem(position);
												if (item != null) {
													item.setProgress(progress);
													item.setLoading(true);
													item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
													adapter.notifyDataSetChanged();
												}
												return true;
											}
										}
										return false;
									}
								});

						if (indexItem == currentDownloadingItem) {
							itemBuilder.setLoading(true)
									.setProgress(currentDownloadingProgress)
									.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						} else {
							itemBuilder.setSecondaryIcon(R.drawable.ic_action_import);
						}
						adapter.addItem(itemBuilder.createItem());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setLayout(R.layout.card_bottom_divider)
				.setMinHeight(spaceHeight)
				.createItem());
		return adapter;
	}

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		return new WikipediaPoiMenu(mapActivity).createLayersItems();
	}

}
