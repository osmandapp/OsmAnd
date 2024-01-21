package net.osmand.plus.wikipedia;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WIKIPEDIA;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WIKIPEDIA_ID;
import static net.osmand.osm.MapPoiTypes.OSM_WIKI_CATEGORY;
import static net.osmand.osm.MapPoiTypes.WIKI_LANG;
import static net.osmand.osm.MapPoiTypes.WIKI_PLACE;
import static net.osmand.plus.helpers.FileNameTranslationHelper.WIKI_NAME;
import static net.osmand.wiki.WikiCoreHelper.USE_OSMAND_WIKI_API;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.GetImageCardsTask.GetImageCardsListener;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard.ImageCardsHolder;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchBannerListItem;
import net.osmand.plus.search.listitems.QuickSearchFreeBannerListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.wikimedia.WikiImageCard;
import net.osmand.plus.wikimedia.WikiImageHelper;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiImage;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WikipediaPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(WikipediaPlugin.class);
	public static final String URL_PHOTO = "url-photo";
	public static final String ORG_WIKI_SUFFIX = ".org/wiki/";
	public final CommonPreference<Boolean> GLOBAL_WIKIPEDIA_POI_ENABLED;
	public final ListStringPreference WIKIPEDIA_POI_ENABLED_LANGUAGES;

	private MapActivity mapActivity;

	private PoiUIFilter topWikiPoiFilter;

	public WikipediaPlugin(OsmandApplication app) {
		super(app);

		GLOBAL_WIKIPEDIA_POI_ENABLED = registerBooleanPreference("global_wikipedia_poi_enabled", false).makeProfile();
		WIKIPEDIA_POI_ENABLED_LANGUAGES = (ListStringPreference) registerListStringPreference("wikipedia_poi_enabled_languages", null, ",").makeProfile().cache();
	}

	@Override
	public String getId() {
		return PLUGIN_WIKIPEDIA;
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
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.purchases_feature_desc_wikipedia);
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.img_plugin_wikipedia);
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !Version.isPaidVersion(app);
	}

	@Override
	public boolean isEnableByDefault() {
		return true;
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return OsmAndFeature.WIKIPEDIA;
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityResumeOnTop(@NonNull MapActivity activity) {
		this.mapActivity = activity;
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
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
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		toggleWikipediaPoi(false, null);
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		if (isEnabled()) {
			if (isLocked()) {
				PurchasingUtils.createPromoItem(adapter, mapActivity, OsmAndFeature.WIKIPEDIA,
						WIKIPEDIA_ID,
						R.string.shared_string_wikipedia,
						R.string.explore_wikipedia_offline);
			} else {
				createWikipediaItem(adapter, mapActivity);
			}
		}
	}

	private void createWikipediaItem(ContextMenuAdapter adapter,
	                                 MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				mapActivity.getDashboard().setDashboardVisibility(true,
						DashboardOnMap.DashboardType.WIKIPEDIA,
						AndroidUtils.getCenterViewCoordinates(view));
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				toggleWikipediaPoi(isChecked, selected -> {
					item.setSelected(selected);
					item.setColor(app, selected ?
							R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					item.setDescription(selected ? getLanguagesSummary() : null);
					uiAdapter.onDataSetChanged();
					return true;
				});
				return false;
			}
		};

		boolean selected = app.getPoiFilters().isPoiFilterSelected(PoiFiltersHelper.getTopWikiPoiFilterId());
		adapter.addItem(new ContextMenuItem(WIKIPEDIA_ID)
				.setTitleId(R.string.shared_string_wikipedia, mapActivity)
				.setDescription(selected ? getLanguagesSummary() : null)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_plugin_wikipedia)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener));
	}

	@Override
	public List<IndexItem> getSuggestedMaps() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		OsmandSettings settings = app.getSettings();
		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}
		if (!downloadThread.shouldDownloadIndexes()) {
			LatLon latLon = app.getMapViewTrackingUtilities().getMapLocation();
			return getMapsForType(latLon, DownloadActivityType.WIKIPEDIA_FILE);
		}
		return Collections.emptyList();
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

	@Override
	protected boolean createContextMenuImageCard(@NonNull ImageCardsHolder holder, @NonNull JSONObject imageObject) {
		ImageCard imageCard = null;
		if (mapActivity != null) {
			try {
				if (imageObject.has("type") && imageObject.has("url")) {
					String type = imageObject.getString("type");
					if (URL_PHOTO.equals(type)) {
						String url = imageObject.getString("url");
						int colonIdx = url.lastIndexOf(":");
						if (url.contains(ORG_WIKI_SUFFIX) && colonIdx > 0) {
							String fileName = url.substring(colonIdx + 1);
							WikiImage wikiImage = WikiCoreHelper.getImageData(fileName);
							if (wikiImage != null) {
								imageCard = new WikiImageCard(mapActivity, wikiImage);
							}
						}
					}
				}
			} catch (JSONException e) {
				LOG.error(e);
			}
		}
		if (imageCard != null) {
			holder.add(ImageCard.ImageCardType.OTHER, imageCard);
			return true;
		}
		return false;
	}

	public void updateWikipediaState() {
		if (isShowAllLanguages() || hasLanguagesFilter()) {
			refreshWikiOnMap();
		} else {
			toggleWikipediaPoi(false, null);
		}
	}

	public String getWikiLanguageTranslation(String locale) {
		String translation = AndroidUtils.getLangTranslation(app, locale);
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
		return WIKIPEDIA_POI_ENABLED_LANGUAGES.get() != null;
	}

	public boolean hasLanguagesFilter(ApplicationMode profile) {
		return WIKIPEDIA_POI_ENABLED_LANGUAGES.getModeValue(profile) != null;
	}

	public boolean isShowAllLanguages() {
		return GLOBAL_WIKIPEDIA_POI_ENABLED.get();
	}

	public boolean isShowAllLanguages(ApplicationMode mode) {
		return GLOBAL_WIKIPEDIA_POI_ENABLED.getModeValue(mode);
	}

	public void setShowAllLanguages(boolean showAllLanguages) {
		GLOBAL_WIKIPEDIA_POI_ENABLED.set(showAllLanguages);
	}

	public void setShowAllLanguages(ApplicationMode mode, boolean showAllLanguages) {
		GLOBAL_WIKIPEDIA_POI_ENABLED.setModeValue(mode, showAllLanguages);
	}

	public List<String> getLanguagesToShow() {
		return WIKIPEDIA_POI_ENABLED_LANGUAGES.getStringsList();
	}

	public List<String> getLanguagesToShow(ApplicationMode mode) {
		return WIKIPEDIA_POI_ENABLED_LANGUAGES.getStringsListForProfile(mode);
	}

	public void setLanguagesToShow(List<String> languagesToShow) {
		WIKIPEDIA_POI_ENABLED_LANGUAGES.setStringsList(languagesToShow);
	}

	public void setLanguagesToShow(ApplicationMode mode, List<String> languagesToShow) {
		WIKIPEDIA_POI_ENABLED_LANGUAGES.setStringsListForProfile(mode, languagesToShow);
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
		PoiFiltersHelper helper = app.getPoiFilters();
		PoiUIFilter filter = helper.getTopWikiPoiFilter();
		if (filter != null) {
			helper.loadSelectedPoiFilters();
			helper.addSelectedPoiFilter(filter);
		}
	}

	private void hideWikiFromMap() {
		PoiFiltersHelper helper = app.getPoiFilters();
		PoiUIFilter filter = helper.getTopWikiPoiFilter();
		if (filter != null) {
			helper.removePoiFilter(filter);
			helper.removeSelectedPoiFilter(filter);
		}
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
			Intent intent = new Intent(app, app.getAppCustomization().getDownloadIndexActivity());
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
	protected boolean searchFinished(QuickSearchDialogFragment searchFragment, SearchPhrase phrase, boolean isResultEmpty) {
		if (isResultEmpty && isSearchByWiki(phrase)) {
			if (!Version.isPaidVersion(app)) {
				searchFragment.addSearchListItem(new QuickSearchFreeBannerListItem(app));
			} else {
				DownloadIndexesThread downloadThread = app.getDownloadThread();
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
			QuickSearchDialogFragment f = (QuickSearchDialogFragment) fragment;
			SearchPhrase phrase = app.getSearchUICore().getCore().getPhrase();
			if (f.isResultEmpty() && isSearchByWiki(phrase)) {
				addEmptyWikiBanner(f, phrase);
			}
		}
	}

	private void addEmptyWikiBanner(QuickSearchDialogFragment fragment, SearchPhrase phrase) {
		QuickSearchBannerListItem banner = new QuickSearchBannerListItem(app);
		banner.addButton(QuickSearchListAdapter.getIncreaseSearchButtonTitle(app, phrase),
				null, QuickSearchBannerListItem.INVALID_ID, v -> fragment.increaseSearchRadius());
		if (hasMapsToDownload()) {
			banner.addButton(app.getString(R.string.search_download_wikipedia_maps),
					null, R.drawable.ic_world_globe_dark, v -> showDownloadWikiMapsScreen());
		}
		fragment.addSearchListItem(banner);
	}

	@Override
	protected void prepareExtraTopPoiFilters(Set<PoiUIFilter> poiUIFilters) {
		for (PoiUIFilter filter : poiUIFilters) {
			if (filter.isTopWikiFilter()) {
				if (hasCustomSettings()) {
					String wikiLang = "wiki:lang:";
					StringBuilder sb = new StringBuilder();
					for (String lang : getLanguagesToShow()) {
						if (sb.length() > 1) {
							sb.append(" ");
						}
						sb.append(wikiLang).append(lang);
					}
					filter.setFilterByName(sb.toString());
				} else {
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
				return CollectionUtils.startsWithAny(pt.getKeyName(), WIKI_LANG, WIKI_PLACE, OSM_WIKI_CATEGORY);
			}
		}
		return false;
	}

	@Override
	protected void collectContextMenuImageCards(@NonNull ImageCardsHolder holder,
	                                            @NonNull Map<String, String> params,
	                                            @Nullable Map<String, String> additionalParams,
	                                            @Nullable GetImageCardsListener listener) {
		if (mapActivity != null && additionalParams != null) {
			String wikidataId = additionalParams.get(Amenity.WIKIDATA);
			if (wikidataId != null) {
				additionalParams.remove(Amenity.WIKIDATA);
				WikiImageHelper.addWikidataImageCards(mapActivity, wikidataId, holder);
			}
			String wikimediaContent = additionalParams.get(Amenity.WIKIMEDIA_COMMONS);
			if (wikimediaContent != null && !(USE_OSMAND_WIKI_API && wikidataId != null)) {
				additionalParams.remove(Amenity.WIKIMEDIA_COMMONS);
				WikiImageHelper.addWikimediaImageCards(mapActivity, wikimediaContent, holder);
			}
			params.putAll(additionalParams);
		}
	}

	public static boolean containsWikipediaExtension(@NonNull String fileName) {
		return CollectionUtils.containsAny(fileName,
				WIKI_NAME, IndexConstants.BINARY_WIKI_MAP_INDEX_EXT);
	}
}