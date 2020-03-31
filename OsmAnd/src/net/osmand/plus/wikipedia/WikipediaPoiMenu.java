package net.osmand.plus.wikipedia;

import android.os.Bundle;
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
import net.osmand.plus.poi.PoiTemplateList;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WikipediaPoiMenu {

	public static final String GLOBAL_WIKI_POI_ENABLED_KEY = "global_wikipedia_poi_enabled_key";
	public static final String ENABLED_WIKI_POI_LANGUAGES_KEY = "enabled_wikipedia_poi_languages_key";

	private MapActivity mapActivity;
	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private boolean nightMode;

	public WikipediaPoiMenu(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.settings = app.getSettings();
		this.appMode = settings.getApplicationMode();
		this.nightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	private ContextMenuAdapter createLayersItems() {
		final int toggleActionStringId = R.string.shared_string_wikipedia;
		final int languageActionStringId = R.string.shared_string_language;
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);

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
							toggleWikipediaPoi(mapActivity, !settings.SHOW_WIKIPEDIA_POI.getModeValue(appMode), true);
						}
					});
				} else if (itemId == languageActionStringId) {
					showLanguagesDialog(mapActivity, appMode, true, true);
				}
				return false;
			}
		};

		int toggleIconId = R.drawable.ic_plugin_wikipedia;
		int toggleIconColorId;
		boolean enabled = settings.SHOW_WIKIPEDIA_POI.getModeValue(appMode);
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
				.setColor(toggleIconColorId)
				.setListener(l)
				.setSelected(enabled).createItem());

		if (enabled) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.list_item_divider)
					.createItem());

			summary = getLanguagesSummary(app);
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
		final boolean downloadIndexes = settings.isInternetConnectionAvailable()
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		if (downloadIndexes) {
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_download_map, mapActivity)
					.setDescription(app.getString(R.string.wiki_menu_download_descr))
					.setCategory(true)
					.setLayout(R.layout.list_group_title_with_descr).createItem());
			adapter.addItem(new ContextMenuItem.ItemBuilder()
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
					adapter.addItem(new ContextMenuItem.ItemBuilder()
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
						adapter.addItem(itemBuilder.createItem());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return adapter;
	}

	private static void showLanguagesDialog(@NonNull final MapActivity mapActivity,
	                                        @NonNull final ApplicationMode appMode,
	                                        final boolean usedOnMap,
	                                        final boolean refresh) {
		final OsmandApplication app = mapActivity.getMyApplication();
		SelectWikiLanguagesBottomSheet.showInstance(mapActivity, appMode, usedOnMap,
				new CallbackWithObject<Boolean>() {
					@Override
					public boolean processResult(Boolean result) {
						if (result) {
							Bundle wikiPoiSetting = getWikiPoiSettingsForProfile(app, appMode);
							if (wikiPoiSetting != null) {
								if (refresh) {
									refreshWikiPoi(mapActivity, wikiPoiSetting);
								} else {
									toggleWikipediaPoi(mapActivity, true, usedOnMap);
								}
							} else {
								toggleWikipediaPoi(mapActivity, false, usedOnMap);
							}
						}
						return true;
					}
				});
	}

	public static Bundle getWikiPoiSettings(OsmandApplication app) {
		Bundle wikiSettings = getWikiPoiSettingsForProfile(app, app.getSettings().getApplicationMode());
		if (wikiSettings == null) {
			wikiSettings = getWikiPoiSettingsForProfile(app, app.getSettings().DEFAULT_APPLICATION_MODE.get());
		}
		return wikiSettings;
	}

	private static Bundle getWikiPoiSettingsForProfile(OsmandApplication app, ApplicationMode appMode) {
		OsmandSettings settings = app.getSettings();
		boolean globalWikiPoiEnabled = settings.GLOBAL_WIKIPEDIA_POI_ENABLED.getModeValue(appMode);
		List<String> enabledWikiPoiLanguages = settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.getStringsListForProfile(appMode);
		if (!globalWikiPoiEnabled && Algorithms.isEmpty(enabledWikiPoiLanguages)) {
			return null;
		}
		Bundle bundle = new Bundle();
		bundle.putBoolean(GLOBAL_WIKI_POI_ENABLED_KEY, globalWikiPoiEnabled);
		if (enabledWikiPoiLanguages != null) {
			bundle.putStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY,
					new ArrayList<>(enabledWikiPoiLanguages));
		}
		return bundle;
	}

	public static void toggleWikipediaPoi(final MapActivity mapActivity, boolean enable, boolean usedOnMap) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (enable) {
			Bundle wikiPoiSettings = getWikiPoiSettings(app);
			if (wikiPoiSettings != null) {
				settings.SHOW_WIKIPEDIA_POI.set(true);
				showWikiOnMap(app, wikiPoiSettings);
			} else {
				ApplicationMode appMode = settings.getApplicationMode();
				showLanguagesDialog(mapActivity, appMode, usedOnMap, false);
			}
		} else {
			settings.SHOW_WIKIPEDIA_POI.set(false);
			hideWikiFromMap(app);
		}
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	public static void refreshWikiPoi(MapActivity mapActivity, @NonNull Bundle wikiPoiSettings) {
		OsmandApplication app = mapActivity.getMyApplication();
		hideWikiFromMap(app);
		showWikiOnMap(app, wikiPoiSettings);
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	private static void showWikiOnMap(OsmandApplication app, Bundle wikiPoiSettings) {
		PoiFiltersHelper ph = app.getPoiFilters();
		boolean globalWikiEnabled = wikiPoiSettings.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
		if (globalWikiEnabled) {
			ph.addSelectedPoiFilter(PoiTemplateList.WIKI, ph.getGlobalWikiPoiFilter());
		} else {
			List<PoiUIFilter> filters = ph.getWikiPOIFilters();
			for (PoiUIFilter filter : filters) {
				ph.addSelectedPoiFilter(PoiTemplateList.WIKI, filter);
			}
		}
	}

	private static void hideWikiFromMap(OsmandApplication app) {
		PoiFiltersHelper ph = app.getPoiFilters();
		for (PoiUIFilter filter : ph.getSelectedPoiFilters(PoiTemplateList.WIKI)) {
			ph.removePoiFilter(filter);
		}
		ph.clearSelectedPoiFilters(PoiTemplateList.WIKI);
	}

	private static String getLanguagesSummary(OsmandApplication app) {
		Bundle wikiLanguagesSetting = getWikiPoiSettings(app);
		if (wikiLanguagesSetting != null) {
			boolean globalWikiEnabled = wikiLanguagesSetting.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
			List<String> enabledLanguages = wikiLanguagesSetting.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
			if (globalWikiEnabled) {
				return app.getString(R.string.shared_string_all_languages);
			} else if (enabledLanguages != null) {
				List<String> translations = new ArrayList<>();
				for (String language : enabledLanguages) {
					translations.add(app.getLangTranslation(language));
				}
				return AndroidUtils.makeStringFromList(translations, ", ");
			}
		}
		return null;
	}

	public static boolean isWikiPoiEnabled(OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		boolean shouldShowWiki = settings.SHOW_WIKIPEDIA_POI.get();
		if (shouldShowWiki && getWikiPoiSettings(app) == null) {
			settings.SHOW_WIKIPEDIA_POI.set(false);
			shouldShowWiki = false;
		}
		return shouldShowWiki;
	}

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		return new WikipediaPoiMenu(mapActivity).createLayersItems();
	}
}
