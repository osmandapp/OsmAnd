package net.osmand.plus.wikipedia;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WikipediaPOIMenu {

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		boolean nightMode = isNightMode(mapActivity.getMyApplication());
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(adapter, mapActivity, nightMode);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
	                                      final MapActivity mapActivity,
	                                      final boolean nightMode) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final ApplicationMode appMode = settings.getApplicationMode();
		boolean enabled = isWikipediaPoiEnabled(app);
		final int toggleActionStringId = R.string.shared_string_wikipedia;
		final int languageActionStringId = R.string.shared_string_language;

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
			                              View view, int itemId, int pos) {
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
			                                  final int itemId, final int position, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							toggleWikipediaPOI(mapActivity, !isWikipediaPoiEnabled(app));
						}
					});
				} else if (itemId == languageActionStringId) {
					SelectWikiLanguagesBottomSheet.showInstance(mapActivity, appMode, true,
							new CallbackWithObject<Boolean>() {
								@Override
								public boolean processResult(Boolean result) {
									if (result) {
										ContextMenuItem item = adapter.getItem(position);
										boolean allWikiLangs = isAllLanguages(app);
										boolean hasActiveLanguages = isActiveLanguages(app);
										if (item != null) {
											item.setDescription(getLanguagesSummary(app));
											if (allWikiLangs || hasActiveLanguages) {
												refreshWikiPOI(mapActivity);
											} else {
												toggleWikipediaPOI(mapActivity, false);
											}
											adapter.notifyDataSetChanged();
										}
									}
									return true;
								}
							});
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
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setDescription(summary)
				.setIcon(toggleIconId)
				.setColor(toggleIconColorId)
				.setListener(l)
				.setSelected(enabled).createItem());

		if (enabled) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.list_item_divider)
					.createItem());

			summary = getLanguagesSummary(app);
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
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
		final boolean downloadIndexes = settings.isInternetConnectionAvailable()
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		if (downloadIndexes) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_download_map, mapActivity)
					.setDescription(app.getString(R.string.wiki_menu_download_descr))
					.setCategory(true)
					.setLayout(R.layout.list_group_title_with_descr).createItem());
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.list_item_icon_and_download)
					.setTitleId(R.string.downloading_list_indexes, mapActivity)
					.setLoading(true)
					.setListener(l).createItem());
		} else {
			try {
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = downloadThread.getCurrentDownloadingItemProgress();
				List<IndexItem> wikiIndexes = DownloadResources.findIndexItemsAt(
						app, mapActivity.getMapLocation(), DownloadActivityType.WIKIPEDIA_FILE);
				if (wikiIndexes.size() > 0) {
					contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
							.setTitleId(R.string.shared_string_download_map, mapActivity)
							.setDescription(app.getString(R.string.wiki_menu_download_descr))
							.setCategory(true)
							.setLayout(R.layout.list_group_title_with_descr).createItem());
					for (final IndexItem indexItem : wikiIndexes) {
						ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder()
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(DownloadActivityType.WIKIPEDIA_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(DownloadActivityType.WIKIPEDIA_FILE.getIconResource())
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
						contextMenuAdapter.addItem(itemBuilder.createItem());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean isAllLanguages(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return settings.ENABLE_ALL_WIKI_LANGUAGES.getModeValue(settings.getApplicationMode());
	}

	public static boolean isWikipediaPoiEnabled(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return settings.SHOW_WIKI_POI.getModeValue(settings.getApplicationMode());
	}

	public static boolean isActiveLanguages(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return settings.ENABLED_WIKI_LANGUAGES.getStringsList() != null;
	}

	public static void toggleWikipediaPOI(final MapActivity mapActivity, boolean enable) {
		final OsmandApplication app = mapActivity.getMyApplication();
		if (enable) {
			if (isAllLanguages(app) || isActiveLanguages(app)) {
				app.getSettings().SHOW_WIKI_POI.set(true);
				showWikiPOI(app);
			} else {
				SelectWikiLanguagesBottomSheet.showInstance(mapActivity, app.getSettings().getApplicationMode(), true,
						new CallbackWithObject<Boolean>() {
							@Override
							public boolean processResult(Boolean result) {
								if (result) {
									boolean allWikiLangs = isAllLanguages(app);
									boolean hasActiveLanguages = isActiveLanguages(app);
									if (allWikiLangs || hasActiveLanguages) {
										toggleWikipediaPOI(mapActivity, true);
									} else {
										toggleWikipediaPOI(mapActivity, false);
									}
								}
								return true;
							}
						});
			}
		} else {
			app.getSettings().SHOW_WIKI_POI.set(false);
			hideWikiPOI(app);
		}
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	public static void refreshWikiPOI(MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		hideWikiPOI(app);
		showWikiPOI(app);
	}

	private static void showWikiPOI(OsmandApplication app) {
		PoiFiltersHelper ph = app.getPoiFilters();
		if (isAllLanguages(app)) {
			ph.addSelectedPoiFilter(ph.getGlobalWikiPoiFilter());
		} else {
			List<PoiUIFilter> filters = ph.getWikiPOIFilters();
			for (PoiUIFilter filter : filters) {
				ph.addSelectedPoiFilter(filter);
			}
		}
	}

	private static void hideWikiPOI(OsmandApplication app) {
		PoiFiltersHelper ph = app.getPoiFilters();
		for (PoiUIFilter filter : ph.getSelectedPoiFilters(PoiFiltersHelper.PoiUiType.WIKIPEDIA)) {
			ph.removePoiFilter(filter);
		}
		ph.clearSelectedPoiFilters(PoiFiltersHelper.PoiUiType.WIKIPEDIA);
	}

	public static String getLanguagesSummary(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();
		if (isAllLanguages(app)) {
			return app.getString(R.string.shared_string_all_languages);
		} else {
			List<String> languages = settings.ENABLED_WIKI_LANGUAGES.getStringsList();
			if (languages != null) {
				List<String> translations = new ArrayList<>();
				for (String locale : languages) {
					translations.add(app.getLangTranslation(locale));
				}
				return AndroidUtils.formatStringListAsOneLine(translations, ", ");
			} else {
				// get languages from base profile
			}
		}
		return null;
	}

	public static boolean isNightMode(@NonNull OsmandApplication app) {
		return app.getDaynightHelper().isNightModeForMapControls();
	}
}
