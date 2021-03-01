package net.osmand.plus.wikipedia;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.search.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchBannerListItem;
import net.osmand.plus.search.listitems.QuickSearchFreeBannerListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.wikimedia.WikiImageHelper;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.WIKIPEDIA_ID;
import static net.osmand.osm.MapPoiTypes.WIKI_LANG;

public class WikipediaPlugin extends OsmandPlugin {

	public static final String ID = "osmand.wikipedia";

	private MapActivity mapActivity;
	private OsmandSettings settings;

	private PoiUIFilter topWikiPoiFilter;

	public WikipediaPlugin(OsmandApplication app) {
		super(app);
		this.settings = app.getSettings();
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_wikipedia;
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_wikipedia);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.plugin_wikipedia_description);
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResumeOnTop(MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		this.mapActivity = null;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		if (activity instanceof MapActivity) {
			mapActivity = (MapActivity) activity;
		}
		return true;
	}

	@Override
	protected void registerLayerContextMenuActions(OsmandMapTileView mapView,
												   ContextMenuAdapter adapter,
												   final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {

			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				if (itemId == R.string.shared_string_wikipedia) {
					mapActivity.getDashboard().setDashboardVisibility(true,
							DashboardOnMap.DashboardType.WIKIPEDIA,
							AndroidUtils.getCenterViewCoordinates(view));
				}
				return false;
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId,
											  final int pos, boolean isChecked, int[] viewCoordinates) {
				if (itemId == R.string.shared_string_wikipedia) {
					toggleWikipediaPoi(isChecked, new CallbackWithObject<Boolean>() {
						@Override
						public boolean processResult(Boolean selected) {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setSelected(selected);
								item.setColor(app, selected ?
										R.color.osmand_orange : ContextMenuItem.INVALID_ID);
								item.setDescription(selected ? getLanguagesSummary() : null);
								adapter.notifyDataSetChanged();
							}
							return true;
						}
					});
				}
				return false;
			}
		};

		boolean selected = app.getPoiFilters().isTopWikiFilterSelected();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(WIKIPEDIA_ID)
				.setTitleId(R.string.shared_string_wikipedia, mapActivity)
				.setDescription(selected ? getLanguagesSummary() : null)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_plugin_wikipedia)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener).createItem());
	}

	@Override
	protected List<PoiUIFilter> getCustomPoiFilters() {
		List<PoiUIFilter> poiFilters = new ArrayList<>();
		if (topWikiPoiFilter == null) {
			AbstractPoiType poiType = app.getPoiTypes().getOsmwiki();
			topWikiPoiFilter = new PoiUIFilter(poiType, app, "");
		}
		poiFilters.add(topWikiPoiFilter);

		return poiFilters;
	}

	public void updateWikipediaState() {
		if (isShowAllLanguages() || hasLanguagesFilter()) {
			refreshWikiOnMap();
		} else {
			toggleWikipediaPoi(false, null);
		}
	}

	public String getWikiLanguageTranslation(String locale) {
		String translation = app.getLangTranslation(locale);
		if (translation.equalsIgnoreCase(locale)) {
			translation = getTranslationFromPhrases(locale);
		}
		return translation;
	}

	private String getTranslationFromPhrases(String locale) {
		String keyName = WIKI_LANG + "_" + locale;
		try {
			Field f = R.string.class.getField("poi_" + keyName);
			Integer in = (Integer) f.get(null);
			return app.getString(in);
		} catch (Throwable e) {
			return locale;
		}
	}

	public boolean hasCustomSettings() {
		return !isShowAllLanguages() && getLanguagesToShow() != null;
	}

	public boolean hasCustomSettings(ApplicationMode profile) {
		return !isShowAllLanguages(profile) && getLanguagesToShow(profile) != null;
	}

	public boolean hasLanguagesFilter() {
		return settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.get() != null;
	}

	public boolean hasLanguagesFilter(ApplicationMode profile) {
		return settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.getModeValue(profile) != null;
	}

	public boolean isShowAllLanguages() {
		return settings.GLOBAL_WIKIPEDIA_POI_ENABLED.get();
	}

	public boolean isShowAllLanguages(ApplicationMode mode) {
		return settings.GLOBAL_WIKIPEDIA_POI_ENABLED.getModeValue(mode);
	}

	public void setShowAllLanguages(boolean showAllLanguages) {
		settings.GLOBAL_WIKIPEDIA_POI_ENABLED.set(showAllLanguages);
	}

	public void setShowAllLanguages(ApplicationMode mode, boolean showAllLanguages) {
		settings.GLOBAL_WIKIPEDIA_POI_ENABLED.setModeValue(mode, showAllLanguages);
	}

	public List<String> getLanguagesToShow() {
		return settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.getStringsList();
	}

	public List<String> getLanguagesToShow(ApplicationMode mode) {
		return settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.getStringsListForProfile(mode);
	}

	public void setLanguagesToShow(List<String> languagesToShow) {
		settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.setStringsList(languagesToShow);
	}

	public void setLanguagesToShow(ApplicationMode mode, List<String> languagesToShow) {
		settings.WIKIPEDIA_POI_ENABLED_LANGUAGES.setStringsListForProfile(mode, languagesToShow);
	}

	public void toggleWikipediaPoi(boolean enable, CallbackWithObject<Boolean> callback) {
		if (enable) {
			showWikiOnMap();
		} else {
			hideWikiFromMap();
		}
		if (callback != null) {
			callback.processResult(enable);
		} else if (mapActivity != null) {
			mapActivity.getDashboard().refreshContent(true);
		}
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	public void refreshWikiOnMap() {
		if (mapActivity == null) {
			return;
		}
		app.getPoiFilters().loadSelectedPoiFilters();
		mapActivity.getDashboard().refreshContent(true);
		mapActivity.refreshMap();
	}

	private void showWikiOnMap() {
		PoiFiltersHelper ph = app.getPoiFilters();
		PoiUIFilter wiki = ph.getTopWikiPoiFilter();
		ph.loadSelectedPoiFilters();
		ph.addSelectedPoiFilter(wiki);
	}

	private void hideWikiFromMap() {
		PoiFiltersHelper ph = app.getPoiFilters();
		PoiUIFilter wiki = ph.getTopWikiPoiFilter();
		ph.removePoiFilter(wiki);
		ph.removeSelectedPoiFilter(wiki);
	}

	public String getLanguagesSummary() {
		if (hasCustomSettings()) {
			List<String> translations = new ArrayList<>();
			for (String locale : getLanguagesToShow()) {
				translations.add(getWikiLanguageTranslation(locale));
			}
			return android.text.TextUtils.join(", ", translations);
		}
		return app.getString(R.string.shared_string_all_languages);
	}

	@Override
	protected String getMapObjectsLocale(Amenity amenity, String preferredLocale) {
		return getWikiArticleLanguage(amenity.getSupportedContentLocales(), preferredLocale);
	}

	@Override
	protected String getMapObjectPreferredLang(MapObject object, String defaultLanguage) {
		if (object instanceof Amenity) {
			Amenity amenity = (Amenity) object;
			if (amenity.getType().isWiki()) {
				return getWikiArticleLanguage(amenity.getSupportedContentLocales(), defaultLanguage);
			}
		}
		return null;
	}

	public String getWikiArticleLanguage(@NonNull Set<String> availableArticleLangs, String preferredLanguage) {
		if (!hasCustomSettings()) {
			// Wikipedia with default settings
			return preferredLanguage;
		}
		if (Algorithms.isEmpty(preferredLanguage)) {
			preferredLanguage = app.getLanguage();
		}
		List<String> wikiLangs = getLanguagesToShow();
		if (!wikiLangs.contains(preferredLanguage)) {
			// return first matched language from enabled Wikipedia languages
			for (String language : wikiLangs) {
				if (availableArticleLangs.contains(language)) {
					return language;
				}
			}
		}
		return preferredLanguage;
	}

	public void showDownloadWikiMapsScreen() {
		if (mapActivity != null) {
			OsmandMapTileView mv = mapActivity.getMapView();
			DownloadedRegionsLayer dl = mv.getLayerByClass(DownloadedRegionsLayer.class);
			String filter = dl.getFilter(new StringBuilder());
			final Intent intent = new Intent(app, app.getAppCustomization().getDownloadIndexActivity());
			intent.putExtra(DownloadActivity.FILTER_KEY, filter);
			intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.WIKIPEDIA_FILE.getTag());
			intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
			mapActivity.startActivity(intent);
		}
	}

	public boolean hasMapsToDownload() {
		try {
			if (mapActivity == null) {
				return false;
			}
			int mapsToDownloadCount = DownloadResources.findIndexItemsAt(
					app, mapActivity.getMapLocation(), DownloadActivityType.WIKIPEDIA_FILE,
					false, 1, false).size();
			return mapsToDownloadCount > 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	protected boolean searchFinished(final QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		if (isResultEmpty && isSearchByWiki(phrase)) {
			if (!Version.isPaidVersion(app)) {
				searchFragment.addSearchListItem(new QuickSearchFreeBannerListItem(app));
			} else {
				final DownloadIndexesThread downloadThread = app.getDownloadThread();
				if (!downloadThread.getIndexes().isDownloadedFromInternet) {
					searchFragment.reloadIndexFiles();
				} else {
					addEmptyWikiBanner(searchFragment, phrase);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	protected void newDownloadIndexes(Fragment fragment) {
		if (fragment instanceof QuickSearchDialogFragment) {
			final QuickSearchDialogFragment f = (QuickSearchDialogFragment) fragment;
			SearchPhrase phrase = app.getSearchUICore().getCore().getPhrase();
			if (f.isResultEmpty() && isSearchByWiki(phrase)) {
				addEmptyWikiBanner(f, phrase);
			}
		}
	}

	private void addEmptyWikiBanner(final QuickSearchDialogFragment fragment, SearchPhrase phrase) {
		QuickSearchBannerListItem banner = new QuickSearchBannerListItem(app);
		banner.addButton(QuickSearchListAdapter.getIncreaseSearchButtonTitle(app, phrase),
				null, QuickSearchBannerListItem.INVALID_ID, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						fragment.increaseSearchRadius();
					}
				});
		if (hasMapsToDownload()) {
			banner.addButton(app.getString(R.string.search_download_wikipedia_maps),
					null, R.drawable.ic_world_globe_dark, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							showDownloadWikiMapsScreen();
						}
					});
		}
		fragment.addSearchListItem(banner);
	}

	@Override
	protected void prepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilters) {
		for (PoiUIFilter filter : poiUIFilters) {
			if (filter.isTopWikiFilter()) {
				boolean prepareByDefault = true;
				if (hasCustomSettings()) {
					prepareByDefault = false;
					String wikiLang = "wiki:lang:";
					StringBuilder sb = new StringBuilder();
					for (String lang : getLanguagesToShow()) {
						if (sb.length() > 1) {
							sb.append(" ");
						}
						sb.append(wikiLang).append(lang);
					}
					filter.setFilterByName(sb.toString());
				}
				if (prepareByDefault) {
					filter.setFilterByName(null);
				}
				return;
			}
		}
	}

	private boolean isSearchByWiki(SearchPhrase phrase) {
		if (phrase.isLastWord(ObjectType.POI_TYPE)) {
			Object obj = phrase.getLastSelectedWord().getResult().object;
			if (obj instanceof PoiUIFilter) {
				PoiUIFilter pf = (PoiUIFilter) obj;
				return pf.isWikiFilter();
			} else if (obj instanceof AbstractPoiType) {
				AbstractPoiType pt = (AbstractPoiType) obj;
				return pt.getKeyName().startsWith(WIKI_LANG);
			}
		}
		return false;
	}

	@Override
	protected List<ImageCard> getContextMenuImageCards(@NonNull Map<String, String> params, @Nullable Map<String, String> additionalParams, @Nullable GetImageCardsListener listener) {
		List<ImageCard> imageCards = new ArrayList<>();
		if (mapActivity != null) {
			if (additionalParams != null) {
				String wikidataId = additionalParams.get(Amenity.WIKIDATA);
				if (wikidataId != null) {
					additionalParams.remove(Amenity.WIKIDATA);
					WikiImageHelper.addWikidataImageCards(mapActivity, wikidataId, imageCards);
				}
				String wikimediaContent = additionalParams.get(Amenity.WIKIMEDIA_COMMONS);
				if (wikimediaContent != null) {
					additionalParams.remove(Amenity.WIKIMEDIA_COMMONS);
					WikiImageHelper.addWikimediaImageCards(mapActivity, wikimediaContent, imageCards);
				}
				params.putAll(additionalParams);
			}
		}
		return imageCards;
	}
}