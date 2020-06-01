package net.osmand.plus.wikipedia;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
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
import java.util.Set;

import static net.osmand.osm.MapPoiTypes.WIKI_LANG;

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
		final boolean enabled = app.getPoiFilters().isTopWikiFilterSelected();
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
							toggleWikipediaPoi(mapActivity, !enabled, callback);
						}
					});
				} else if (itemId == languageActionStringId) {
					showLanguagesDialog(mapActivity, appMode, true, callback);
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
	                                        final CallbackWithObject<Boolean> callback) {
		final OsmandApplication app = mapActivity.getMyApplication();
		SelectWikiLanguagesBottomSheet.showInstance(mapActivity, appMode, usedOnMap,
				new CallbackWithObject<Boolean>() {
					@Override
					public boolean processResult(Boolean result) {
						if (result) {
							Bundle wikiPoiSetting = getWikiPoiSettingsForProfile(app, appMode);
							if (wikiPoiSetting != null) {
								refreshWikipediaOnMap(mapActivity);
							} else {
								toggleWikipediaPoi(mapActivity, false, callback);
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
	                                      CallbackWithObject<Boolean> callback) {
		OsmandApplication app = mapActivity.getMyApplication();
		if (enable) {
			showWikipediaOnMap(app);
		} else {
			hideWikipediaFromMap(app);
		}
		if (callback != null) {
			callback.processResult(enable);
		}
		mapActivity.refreshMap();
	}

	public static void refreshWikipediaOnMap(MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		hideWikipediaFromMap(app);
		showWikipediaOnMap(app);
		app.getPoiFilters().setUpdatePoiFiltersOnMap(true);
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	private static void showWikipediaOnMap(OsmandApplication app) {
		PoiFiltersHelper ph = app.getPoiFilters();
		PoiUIFilter wiki = ph.getTopWikiPoiFilter();
		ph.addSelectedPoiFilter(wiki);
	}

	private static void hideWikipediaFromMap(OsmandApplication app) {
		PoiFiltersHelper ph = app.getPoiFilters();
		PoiUIFilter wiki = ph.getTopWikiPoiFilter();
		ph.removePoiFilter(wiki);
		ph.removeSelectedPoiFilter(wiki);
	}

	public static String getLanguagesSummary(OsmandApplication app) {
		Bundle wikiSetting = getWikiPoiSettings(app);
		if (wikiSetting != null) {
			boolean globalWikiEnabled = wikiSetting.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY);
			List<String> enabledLocales = wikiSetting.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
			if (!globalWikiEnabled && enabledLocales != null) {
				List<String> translations = new ArrayList<>();
				for (String locale : enabledLocales) {
					translations.add(getTranslation(app, locale));
				}
				return android.text.TextUtils.join(", ", translations);
			}
		}
		return app.getString(R.string.shared_string_all_languages);
	}

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		return new WikipediaPoiMenu(mapActivity).createLayersItems();
	}

	public static String getWikiArticleLanguage(@NonNull OsmandApplication app,
	                                            @NonNull Set<String> availableArticleLangs,
	                                            String preferredLanguage) {
		Bundle wikiPoiSettings = getWikiPoiSettings(app);
		if (!app.getPoiFilters().isTopWikiFilterSelected() || wikiPoiSettings == null) {
			// Wikipedia POI setting disabled
			return preferredLanguage;
		}
		if (wikiPoiSettings.getBoolean(GLOBAL_WIKI_POI_ENABLED_KEY)) {
			// global Wikipedia POI filter enabled
			return preferredLanguage;
		}
		if (Algorithms.isEmpty(preferredLanguage)) {
			preferredLanguage = app.getLanguage();
		}
		List<String> wikiLangs = wikiPoiSettings.getStringArrayList(ENABLED_WIKI_POI_LANGUAGES_KEY);
		if (wikiLangs != null && !wikiLangs.contains(preferredLanguage)) {
			// return first matched language from enabled Wikipedia languages
			for (String language : wikiLangs) {
				if (availableArticleLangs.contains(language)) {
					return language;
				}
			}
		}
		return preferredLanguage;
	}
}
