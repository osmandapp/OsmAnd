package net.osmand.plus.wikipedia;

import static net.osmand.data.DataSourceType.ONLINE;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.DataSourceType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WikipediaPoiMenu {

	private static final Log log = PlatformUtil.getLog(WikipediaPoiMenu.class);

	private final WikipediaPlugin plugin = PluginsHelper.requirePlugin(WikipediaPlugin.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapActivity activity;
	private final boolean nightMode;

	public WikipediaPoiMenu(@NonNull MapActivity activity) {
		this.activity = activity;
		this.app = activity.getMyApplication();
		this.settings = app.getSettings();
		this.nightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	@NonNull
	private ContextMenuAdapter createLayersItems() {
		String toggleAction = plugin.getPopularPlacesTitle();
		int languageActionStringId = R.string.shared_string_language;
		int spaceHeight = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_big_item_height);
		boolean enabled = app.getPoiFilters().isPoiFilterSelected(PoiUIFilter.TOP_WIKI_FILTER_ID);
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);

		OnRowItemClick listener = new OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
					@Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
				String title = item.getTitle();
				int itemId = item.getTitleId();
				if (Algorithms.stringsEqual(toggleAction, title)) {
					app.runInUIThread(() -> plugin.toggleWikipediaPoi(!enabled, null));
				} else if (itemId == languageActionStringId) {
					SelectWikiLanguagesBottomSheet.showInstance(activity, true);
				} else if (itemId == R.string.data_source) {
					showDataSourceDialog(uiAdapter, view, item);
				} else if (itemId == R.string.show_image_previews) {
					app.getSettings().WIKI_SHOW_IMAGE_PREVIEWS.set(isChecked);
					item.setSelected(isChecked);
					item.setColor(app, isChecked ? ColorUtilities.getActiveColorId(nightMode) : ContextMenuItem.INVALID_ID);
					if (uiAdapter != null) {
						uiAdapter.onDataSetChanged();
					}
					activity.refreshMap();
					activity.updateLayers();
				}
				return false;
			}
		};

		int toggleIconId = R.drawable.ic_plugin_wikipedia;
		int toggleIconColorId;
		if (enabled) {
			toggleIconColorId = ColorUtilities.getActiveColorId(nightMode);
		} else {
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}
		String summary = activity.getString(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);
		adapter.addItem(new ContextMenuItem(null)
				.setTitle(toggleAction)
				.setDescription(summary)
				.setIcon(toggleIconId)
				.setColor(app, toggleIconColorId)
				.setListener(listener)
				.setSelected(enabled));

		if (enabled) {
			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.list_item_divider));

			summary = plugin.getLanguagesSummary();
			adapter.addItem(new ContextMenuItem(null)
					.setTitleId(languageActionStringId, activity)
					.setIcon(R.drawable.ic_action_map_language)
					.setDescription(summary)
					.hideCompoundButton(true)
					.setListener(listener));

			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.list_item_divider));

			boolean onlineDataSource = app.getSettings().WIKI_DATA_SOURCE_TYPE.get() == ONLINE;
			summary = onlineDataSource
					? activity.getString(R.string.shared_string_online)
					: activity.getString(R.string.shared_string_offline);
			adapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.data_source, activity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_world_globe_dark)
					.setColor(app, onlineDataSource ? ColorUtilities.getActiveColorId(nightMode) : ContextMenuItem.INVALID_ID)
					.setDescription(summary)
					.setListener(listener));
			Boolean showImagePreviews = app.getSettings().WIKI_SHOW_IMAGE_PREVIEWS.get();
			adapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.show_image_previews, activity)
					.setIcon(R.drawable.ic_type_img)
					.setColor(app, showImagePreviews ? ColorUtilities.getActiveColorId(nightMode) : ContextMenuItem.INVALID_ID)
					.setListener(listener)
					.setSelected(showImagePreviews));
		}

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (settings.isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		if (downloadThread.shouldDownloadIndexes()) {
			adapter.addItem(new ContextMenuItem(null)
					.setCategory(true)
					.setTitleId(R.string.shared_string_download_map, activity)
					.setDescription(app.getString(R.string.wiki_menu_download_descr))
					.setLayout(R.layout.list_group_title_with_descr));
			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.list_item_icon_and_download)
					.setTitleId(R.string.downloading_list_indexes, activity)
					.setHideDivider(true)
					.setLoading(true)
					.setListener(listener));
		} else {
			try {
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = (int) downloadThread.getCurrentDownloadProgress();
				List<IndexItem> wikiIndexes = DownloadResources.findIndexItemsAt(
						app, activity.getMapLocation(), DownloadActivityType.WIKIPEDIA_FILE,
						false, -1, true);
				if (!Algorithms.isEmpty(wikiIndexes)) {
					adapter.addItem(new ContextMenuItem(null)
							.setCategory(true)
							.setTitleId(R.string.shared_string_download_map, activity)
							.setDescription(app.getString(R.string.wiki_menu_download_descr))
							.setLayout(R.layout.list_group_title_with_descr));
					for (int i = 0; i < wikiIndexes.size(); i++) {
						IndexItem indexItem = wikiIndexes.get(i);
						boolean isLastItem = i == wikiIndexes.size() - 1;
						ContextMenuItem menuItem = new ContextMenuItem(null)
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(DownloadActivityType.WIKIPEDIA_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(DownloadActivityType.WIKIPEDIA_FILE.getIconResource())
								.setHideDivider(isLastItem)
								.setListener((uiAdapter, view, item, isChecked) -> {
									if (downloadThread.isDownloading(indexItem)) {
										downloadThread.cancelDownload(indexItem);
										item.setProgress(ContextMenuItem.INVALID_ID);
										item.setLoading(false);
										item.setSecondaryIcon(R.drawable.ic_action_import);
									} else {
										new DownloadValidationManager(app).startDownload(activity, indexItem);
										item.setProgress(ContextMenuItem.INVALID_ID);
										item.setLoading(true);
										item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
									}
									uiAdapter.onDataSetChanged();
									return false;
								})
								.setProgressListener((progressObject, progress, adapter1, itemId, position) -> {
									if (progressObject instanceof IndexItem progressItem) {
										if (indexItem.compareTo(progressItem) == 0) {
											ContextMenuItem item = adapter1.getItem(position);
											if (item != null) {
												item.setProgress(progress);
												item.setLoading(true);
												item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
												adapter1.notifyDataSetChanged();
											}
											return true;
										}
									}
									return false;
								});

						if (indexItem == currentDownloadingItem) {
							menuItem.setLoading(true)
									.setProgress(currentDownloadingProgress)
									.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						} else {
							menuItem.setSecondaryIcon(R.drawable.ic_action_import);
						}
						adapter.addItem(menuItem);
					}
				}
			} catch (IOException e) {
				log.error(e);
			}
		}
		adapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.card_bottom_divider)
				.setMinHeight(spaceHeight));
		return adapter;
	}

	private void showDataSourceDialog(@Nullable OnDataChangeUiAdapter adapter,
			@Nullable View view, @NotNull ContextMenuItem item) {
		List<PopUpMenuItem> items = new ArrayList<>();
		for (DataSourceType sourceType : DataSourceType.values()) {
			items.add(createDataSourceMenuItem(item, sourceType, adapter));
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view != null ? view.findViewById(R.id.description) : null;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	@NonNull
	private PopUpMenuItem createDataSourceMenuItem(@NotNull ContextMenuItem item,
			@NotNull DataSourceType sourceType, @Nullable OnDataChangeUiAdapter uiAdapter) {
		return new PopUpMenuItem.Builder(activity)
				.setTitleId(sourceType.nameId)
				.setSelected(app.getSettings().WIKI_DATA_SOURCE_TYPE.get() == sourceType)
				.showCompoundBtn(ColorUtilities.getActiveColor(activity, nightMode))
				.setOnClickListener(v -> {
					app.getSettings().WIKI_DATA_SOURCE_TYPE.set(sourceType);
					item.setDescription(app.getString(sourceType.nameId));
					if (uiAdapter != null) {
						uiAdapter.onDataSetChanged();
					}
					activity.refreshMap();
					activity.updateLayers();
				})
				.create();
	}

	@NonNull
	public static ContextMenuAdapter createListAdapter(@NonNull MapActivity activity) {
		WikipediaPoiMenu menu = new WikipediaPoiMenu(activity);
		return menu.createLayersItems();
	}
}
