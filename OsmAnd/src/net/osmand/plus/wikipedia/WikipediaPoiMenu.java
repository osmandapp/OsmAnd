package net.osmand.plus.wikipedia;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

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
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.osm.MapPoiTypes.WIKI_LANG;
import static net.osmand.plus.poi.PoiFiltersHelper.PoiTemplateList;

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
		final int spaceHeight = app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_big_item_height);
		final boolean enabled = app.getPoiFilters().isShowingAnyPoi(PoiTemplateList.WIKI);
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.setDefaultLayoutId(R.layout.dash_item_with_description_72dp);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);

		final CallbackWithObject<Boolean> callback = new CallbackWithObject<Boolean>() {
			@Override
			public boolean processResult(Boolean result) {
				mapActivity.getDashboard().refreshContent(true);
				return true;
			}
		};

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
			                                  final int itemId, final int position, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							toggleWikipediaPoi(mapActivity, !enabled, true, callback);
						}
					});
				} else if (itemId == languageActionStringId) {
					showLanguagesDialog(mapActivity, appMode, true, true, callback);
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
					.hideDivider(true)
					.setLoading(true)
					.setListener(l).createItem());
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setLayout(R.layout.card_bottom_divider)
					.setMinHeight(spaceHeight)
					.createItem());
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
					adapter.addItem(new ContextMenuItem.ItemBuilder()
							.setLayout(R.layout.card_bottom_divider)
							.setMinHeight(spaceHeight)
							.createItem());
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
	                                        final boolean refresh,
	                                        final CallbackWithObject<Boolean> callback) {
		final OsmandApplication app = mapActivity.getMyApplication();
		SelectWikiLanguagesBottomSheet.showInstance(mapActivity, appMode, usedOnMap,
				new CallbackWithObject<Boolean>() {
					@Override
					public boolean processResult(Boolean result) {
						if (result) {
							Bundle wikiPoiSetting = getWikiPoiSettingsForProfile(app, appMode);
							if (wikiPoiSetting != null) {
								boolean globalWikiEnabled =
										wikiPoiSetting.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
								if (refresh) {
									refreshWikiPoi(mapActivity, globalWikiEnabled);
								} else {
									toggleWikipediaPoi(mapActivity, true, usedOnMap, callback);
								}
							} else {
								toggleWikipediaPoi(mapActivity, false, usedOnMap, callback);
							}
						}
						return true;
					}
				});
	}

	public static String getTranslation(OsmandApplication app, String locale) {
		String translation = app.getLangTranslation(locale);
		if (translation.equalsIgnoreCase(locale)) {
			translation = getTranslationFromPhrases(app, locale);
		}
		return translation;
	}

	private static String getTranslationFromPhrases(OsmandApplication app, String locale) {
		String keyName = WIKI_LANG + "_" + locale;
		try {
			Field f = R.string.class.getField("poi_" + keyName);
			Integer in = (Integer) f.get(null);
			return app.getString(in);
		} catch (Throwable e) {
			return locale;
		}
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

	public static void toggleWikipediaPoi(final MapActivity mapActivity, boolean enable,
	                                      boolean usedOnMap, CallbackWithObject<Boolean> callback) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		if (enable) {
			Bundle wikiPoiSettings = getWikiPoiSettings(app);
			if (wikiPoiSettings != null) {
				settings.SHOW_WIKIPEDIA_POI.set(true);
				boolean globalWikiEnabled = wikiPoiSettings.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
				showWikiOnMap(app, globalWikiEnabled);
			} else {
				ApplicationMode appMode = settings.getApplicationMode();
				showLanguagesDialog(mapActivity, appMode, usedOnMap, false, callback);
			}
		} else {
			settings.SHOW_WIKIPEDIA_POI.set(false);
			hideWikiFromMap(app);
		}
		if (callback != null) {
			callback.processResult(settings.SHOW_WIKIPEDIA_POI.get());
		}
		mapActivity.refreshMap();
	}

	public static void refreshWikiPoi(MapActivity mapActivity, boolean globalWikiEnabled) {
		OsmandApplication app = mapActivity.getMyApplication();
		hideWikiFromMap(app);
		showWikiOnMap(app, globalWikiEnabled);
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	private static void showWikiOnMap(OsmandApplication app, boolean globalWikiEnabled) {
		PoiFiltersHelper ph = app.getPoiFilters();
		if (globalWikiEnabled) {
			ph.addSelectedPoiFilter(PoiTemplateList.WIKI, ph.getGlobalWikiPoiFilter());
		} else {
			List<PoiUIFilter> filters = ph.getLocalWikipediaPoiFilters(true);
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
			List<String> enabledLocales = wikiLanguagesSetting.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
			if (globalWikiEnabled) {
				return app.getString(R.string.shared_string_all_languages);
			} else if (enabledLocales != null) {
				List<String> translations = new ArrayList<>();
				for (String locale : enabledLocales) {
					translations.add(getTranslation(app, locale));
				}
				return android.text.TextUtils.join(", ", translations);
			}
		}
		return null;
	}

	public static boolean isWikiPoiEnabled(OsmandApplication app) {
		return app.getSettings().SHOW_WIKIPEDIA_POI.get() && getWikiPoiSettings(app) != null;
	}

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		return new WikipediaPoiMenu(mapActivity).createLayersItems();
	}
}
