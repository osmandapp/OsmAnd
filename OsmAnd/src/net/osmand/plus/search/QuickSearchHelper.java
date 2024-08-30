package net.osmand.plus.search;

import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.osm.MapPoiTypes.OSM_WIKI_CATEGORY;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.map.WorldRegion;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.poi.NominatimPoiFilter;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuickSearchHelper implements ResourceListener {

	public static final int SEARCH_FAVORITE_API_PRIORITY = 150;
	public static final int SEARCH_FAVORITE_API_CATEGORY_PRIORITY = 150;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 150;
	public static final int SEARCH_FAVORITE_CATEGORY_PRIORITY = 151;
	public static final int SEARCH_WPT_API_PRIORITY = 150;
	public static final int SEARCH_WPT_OBJECT_PRIORITY = 152;
	public static final int SEARCH_TRACK_API_PRIORITY = 150;
	public static final int SEARCH_TRACK_OBJECT_PRIORITY = 153;
	public static final int SEARCH_INDEX_ITEM_API_PRIORITY = 150;
	public static final int SEARCH_INDEX_ITEM_PRIORITY = 150;
	public static final int SEARCH_HISTORY_API_PRIORITY = 150;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 154;
	public static final int SEARCH_ONLINE_API_PRIORITY = 500;
	public static final int SEARCH_ONLINE_AMENITY_PRIORITY = 500;

	private final OsmandApplication app;
	private final SearchUICore core;
	private SearchResultCollection resultCollection;
	private boolean mapsIndexed;

	public QuickSearchHelper(OsmandApplication app) {
		this.app = app;
		OsmandSettings settings = app.getSettings();
		core = new SearchUICore(app.getPoiTypes(), settings.MAP_PREFERRED_LOCALE.get(),
				settings.MAP_TRANSLITERATE_NAMES.get());
		app.getResourceManager().addResourceListener(this);
	}

	public SearchUICore getCore() {
		if (mapsIndexed) {
			mapsIndexed = false;
			setRepositoriesForSearchUICore(app);
		}
		return core;
	}

	public SearchResultCollection getResultCollection() {
		return resultCollection;
	}

	public void setResultCollection(SearchResultCollection resultCollection) {
		this.resultCollection = resultCollection;
	}

	public void initSearchUICore() {
		mapsIndexed = false;
		setRepositoriesForSearchUICore(app);
		core.init();

		// Register index item api
		core.registerAPI(new SearchIndexItemApi(app));

		// Register favorites search api
		core.registerAPI(new SearchFavoriteAPI(app));

		// Register favorites by category search api
		core.registerAPI(new SearchFavoriteCategoryAPI(app));

		// Register WptPt search api
		core.registerAPI(new SearchWptAPI(app));
		core.registerAPI(new SearchGpxAPI(app));
		core.registerAPI(new SearchHistoryAPI(app));

		core.registerAPI(new SearchOnlineApi(app));

		refreshCustomPoiFilters();
	}

	public void refreshCustomPoiFilters() {
		if (!app.getPoiTypes().isInit()) {
			return;
		}
		core.clearCustomSearchPoiFilters();
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		for (CustomSearchPoiFilter udf : poiFilters.getUserDefinedPoiFilters(false)) {
			core.addCustomSearchPoiFilter(udf, 0);
		}
		PoiUIFilter topWikiPoiFilter = poiFilters.getTopWikiPoiFilter();
		if (topWikiPoiFilter != null && topWikiPoiFilter.isActive()) {
			core.addCustomSearchPoiFilter(topWikiPoiFilter, 1);
		}
		PoiUIFilter showAllPOIFilter = poiFilters.getShowAllPOIFilter();
		if (showAllPOIFilter != null && showAllPOIFilter.isActive()) {
			core.addCustomSearchPoiFilter(showAllPOIFilter, 1);
		}
		refreshFilterOrders();
	}

	private void refreshFilterOrders() {
		PoiFiltersHelper filtersHelper = app.getPoiFilters();
		core.setActivePoiFiltersByOrder(filtersHelper.getPoiFilterOrders(true));
	}

	public void setRepositoriesForSearchUICore(OsmandApplication app) {
		BinaryMapIndexReader[] binaryMapIndexReaderArray = app.getResourceManager().getQuickSearchFiles();
		core.getSearchSettings().setOfflineIndexes(Arrays.asList(binaryMapIndexReaderArray));
		core.getSearchSettings().setRegions(app.getRegions());
	}

	public Amenity findAmenity(String name, double lat, double lon, String lang, boolean transliterate) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, true);

		MapPoiTypes types = app.getPoiTypes();
		for (Amenity amenity : amenities) {
			String poiSimpleFormat = OsmAndFormatter.getPoiStringWithoutType(amenity, lang, transliterate);
			if (poiSimpleFormat.equals(name)) {
				return amenity;
			}
		}
		for (Amenity amenity : amenities) {
			String amenityName = amenity.getName(lang, transliterate);
			if (Algorithms.isEmpty(amenityName)) {
				AbstractPoiType st = types.getAnyPoiTypeByKey(amenity.getSubType());
				if (st != null) {
					amenityName = st.getTranslation();
				} else {
					amenityName = amenity.getSubType();
				}
			}
			if (name.contains(amenityName)) {
				return amenity;
			}
		}
		return null;
	}

	public static class SearchWptAPI extends SearchBaseAPI {

		private final OsmandApplication app;

		public SearchWptAPI(OsmandApplication app) {
			super(ObjectType.WPT);
			this.app = app;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			if (phrase.isEmpty()) {
				return false;
			}

			List<SelectedGpxFile> list = app.getSelectedGpxHelper().getSelectedGPXFiles();
			for (SelectedGpxFile selectedGpx : list) {
				for (WptPt point : selectedGpx.getGpxFile().getPointsList()) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = point.getName();
					sr.object = point;
					sr.priority = SEARCH_WPT_OBJECT_PRIORITY;
					sr.objectType = ObjectType.WPT;
					sr.location = new LatLon(point.getLatitude(), point.getLongitude());
					//sr.localeRelatedObjectName = app.getRegions().getCountryName(sr.location);
					sr.relatedObject = selectedGpx.getGpxFile();
					sr.preferredZoom = SearchCoreFactory.PREFERRED_WPT_ZOOM;
					if (phrase.getFullSearchPhrase().length() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else {
						NameStringMatcher matcher = new NameStringMatcher(phrase.getFullSearchPhrase().trim(),
								StringMatcherMode.CHECK_CONTAINS);
						if (matcher.matches(sr.localeName)) {
							resultMatcher.publish(sr);
						}
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_WPT_API_PRIORITY;
		}
	}

	public static class SearchFavoriteCategoryAPI extends SearchBaseAPI {

		private final OsmandApplication app;
		private final FavouritesHelper helper;

		public SearchFavoriteCategoryAPI(OsmandApplication app) {
			super(ObjectType.FAVORITE_GROUP);
			this.app = app;
			this.helper = app.getFavoritesHelper();
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			String baseGroupName = app.getString(R.string.shared_string_favorites);
			List<FavoriteGroup> groups = app.getFavoritesHelper().getFavoriteGroups();
			for (FavoriteGroup group : groups) {
				if (group.isVisible()) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = Algorithms.isEmpty(group.getName()) ? baseGroupName : group.getName();
					sr.object = group;
					sr.priority = SEARCH_FAVORITE_CATEGORY_PRIORITY;
					sr.objectType = ObjectType.FAVORITE_GROUP;
					sr.preferredZoom = SearchCoreFactory.PREFERRED_FAVORITES_GROUP_ZOOM;
					if (phrase.getFirstUnknownNameStringMatcher().matches(sr.localeName)) {
						if (group.getPoints().size() < 5) {
							for (FavouritePoint point : group.getPoints()) {
								SearchResult srp = new SearchResult(phrase);
								srp.localeName = point.getName();
								srp.object = point;
								srp.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
								srp.objectType = ObjectType.FAVORITE;
								srp.location = new LatLon(point.getLatitude(), point.getLongitude());
								srp.preferredZoom = SearchCoreFactory.PREFERRED_FAVORITE_ZOOM;
								resultMatcher.publish(srp);
							}
						} else {
							resultMatcher.publish(sr);
						}
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
				return -1;
			}
			return SEARCH_FAVORITE_API_CATEGORY_PRIORITY;
		}
	}

	public static class SearchFavoriteAPI extends SearchBaseAPI {

		private final OsmandApplication app;

		public SearchFavoriteAPI(OsmandApplication app) {
			super(ObjectType.FAVORITE);
			this.app = app;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			List<FavouritePoint> favList = app.getFavoritesHelper().getFavouritePoints();
			for (FavouritePoint point : favList) {
				if (!point.isVisible()) {
					continue;
				}
				SearchResult sr = new SearchResult(phrase);
				sr.localeName = point.getDisplayName(app);
				sr.object = point;
				sr.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
				sr.objectType = ObjectType.FAVORITE;
				sr.location = new LatLon(point.getLatitude(), point.getLongitude());
				sr.preferredZoom = SearchCoreFactory.PREFERRED_FAVORITE_ZOOM;
				if (phrase.isLastWord(ObjectType.FAVORITE_GROUP)) {
					FavoriteGroup group = (FavoriteGroup) phrase.getLastSelectedWord().getResult().object;
					if (group != null && !point.getCategory().equals(group.getName())) {
						continue;
					}
				}
				if (phrase.getFullSearchPhrase().length() <= 1
						&& (phrase.isNoSelectedType() || phrase.isLastWord(ObjectType.FAVORITE_GROUP))) {
					resultMatcher.publish(sr);
				} else {
					NameStringMatcher matcher = new NameStringMatcher(phrase.getFullSearchPhrase().trim(),
							StringMatcherMode.CHECK_CONTAINS);
					if (matcher.matches(sr.localeName)) {
						resultMatcher.publish(sr);
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.isLastWord(ObjectType.FAVORITE_GROUP)) {
				return SEARCH_FAVORITE_API_PRIORITY;
			}
			if (!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
				return -1;
			}
			return SEARCH_FAVORITE_API_PRIORITY;
		}
	}

	public static class SearchOnlineApi extends SearchBaseAPI {
		private static final int SEARCH_RADIUS_INCREMENT = 3;

		private final OsmandApplication app;
		private final NominatimPoiFilter filter;

		public SearchOnlineApi(OsmandApplication app) {
			super(ObjectType.ONLINE_SEARCH);
			this.app = app;
			this.filter = app.getPoiFilters().getNominatimAddressFilter();
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher matcher) throws IOException {
			double lat = phrase.getSettings().getOriginalLocation().getLatitude();
			double lon = phrase.getSettings().getOriginalLocation().getLongitude();
			String text = phrase.getFullSearchPhrase();
			filter.setFilterByName(text);
			publishAmenities(phrase, matcher, filter.initializeNewSearch(lat, lon,
					-1, null, phrase.getRadiusLevel() + 3));
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.hasCustomSearchType(ObjectType.ONLINE_SEARCH)) {
				return SEARCH_ONLINE_API_PRIORITY;
			}
			return -1;
		}

		private void publishAmenities(SearchPhrase phrase, SearchResultMatcher matcher, List<Amenity> amenities) {
			for (Amenity amenity : amenities) {
				SearchResult sr = getSearchResult(phrase, amenity);
				LatLon latLon = amenity.getLocation();
				String lang = sr.requiredSearchPhrase.getSettings().getLang();
				boolean transliterate = sr.requiredSearchPhrase.getSettings().isTransliterate();
				Amenity a = app.getSearchUICore().findAmenity(amenity.getName(), latLon.getLatitude(),
						latLon.getLongitude(), lang, transliterate);
				if (a != null) {
					sr = getSearchResult(phrase, a);
				}
				matcher.publish(sr);
			}
		}

		@NonNull
		private SearchResult getSearchResult(SearchPhrase phrase, Amenity amenity) {
			SearchResult sr = new SearchResult(phrase);
			sr.localeName = amenity.getName();
			sr.object = amenity;
			sr.priority = SEARCH_ONLINE_AMENITY_PRIORITY;
			sr.objectType = ObjectType.POI;
			sr.location = amenity.getLocation();
			sr.preferredZoom = SearchCoreFactory.PREFERRED_POI_ZOOM;
			return sr;
		}

		@Override
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return (int) filter.getSearchRadius(phrase.getRadiusLevel() + SEARCH_RADIUS_INCREMENT);
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return (int) filter.getSearchRadius(phrase.getRadiusLevel() + SEARCH_RADIUS_INCREMENT + 1);
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return phrase.getRadiusLevel() + SEARCH_RADIUS_INCREMENT < filter.getMaxSearchRadiusIndex();
		}
	}

	public static class SearchHistoryAPI extends SearchBaseAPI {

		private final OsmandApplication app;

		public SearchHistoryAPI(OsmandApplication app) {
			super(ObjectType.RECENT_OBJ);
			this.app = app;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			int priority = 0;
			SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
			for (HistoryEntry entry : historyHelper.getHistoryEntries(false)) {
				SearchResult result = createSearchResult(app, entry, phrase);
				result.priority = SEARCH_HISTORY_OBJECT_PRIORITY + (priority++);

				if (phrase.getFullSearchPhrase().length() <= 1 && phrase.isNoSelectedType()) {
					resultMatcher.publish(result);
				} else if (phrase.getFirstUnknownNameStringMatcher().matches(result.localeName)) {
					resultMatcher.publish(result);
				}
			}
			return true;
		}

		@NonNull
		public static SearchResult createSearchResult(OsmandApplication app, HistoryEntry entry, SearchPhrase phrase) {
			SearchResult result = new SearchResult(phrase);

			PointDescription description = entry.getName();
			String name = description.getName();
			result.localeName = name;

			if (description.isPoiType()) {
				MapPoiTypes poiTypes = app.getPoiTypes();
				AbstractPoiType poiType = poiTypes.getAnyPoiTypeByKey(name);
				if (poiType == null) {
					poiType = poiTypes.getAnyPoiAdditionalTypeByKey(name);
				}
				if (poiType != null) {
					result.localeName = poiType.getTranslation();
					if (OSM_WIKI_CATEGORY.equals(poiType.getKeyName())) {
						result.localeName = result.localeName + " (" + poiTypes.getAllLanguagesTranslationSuffix() + ")";
					}
				}
				result.object = poiType;
				result.relatedObject = entry;
				result.priorityDistance = 0;
				result.objectType = ObjectType.POI_TYPE;
			} else if (description.isCustomPoiFilter()) {
				PoiUIFilter filter = app.getPoiFilters().getFilterById(name, true);
				if (filter != null) {
					result.localeName = filter.getName();
				}
				result.object = filter;
				result.relatedObject = entry;
				result.objectType = ObjectType.POI_TYPE;
			} else if (description.isGpxFile()) {
				GPXInfo gpxInfo = GpxUiHelper.getGpxInfoByFileName(app, name);
				if (gpxInfo != null) {
					result.localeName = gpxInfo.getFileName();
				}
				result.object = entry;
				result.objectType = ObjectType.GPX_TRACK;
				result.relatedObject = gpxInfo;
			} else {
				result.object = entry;
				result.objectType = ObjectType.RECENT_OBJ;
				result.location = new LatLon(entry.getLat(), entry.getLon());
				result.preferredZoom = SearchCoreFactory.PREFERRED_DEFAULT_RECENT_ZOOM;
			}
			return result;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isEmpty()) {
				return -1;
			}
			return SEARCH_HISTORY_API_PRIORITY;
		}
	}

	public static class SearchGpxAPI extends SearchBaseAPI {

		private final OsmandApplication app;

		public SearchGpxAPI(OsmandApplication app) {
			super(ObjectType.GPX_TRACK);
			this.app = app;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			File tracksDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			List<GPXInfo> gpxInfoList = new ArrayList<>();
			GpxUiHelper.readGpxDirectory(tracksDir, gpxInfoList, "", false);
			for (GPXInfo gpxInfo : gpxInfoList) {
				SearchResult searchResult = new SearchResult(phrase);
				searchResult.objectType = ObjectType.GPX_TRACK;
				searchResult.localeName = GpxUiHelper.getGpxFileRelativePath(app, gpxInfo.getFileName());
				searchResult.relatedObject = gpxInfo;
				searchResult.priority = SEARCH_TRACK_OBJECT_PRIORITY;
				searchResult.preferredZoom = SearchCoreFactory.PREFERRED_GPX_FILE_ZOOM;
				if (phrase.getFullSearchPhrase().length() <= 1 && phrase.isNoSelectedType()) {
					resultMatcher.publish(searchResult);
				} else {
					NameStringMatcher matcher = new NameStringMatcher(phrase.getFullSearchPhrase().trim(),
							StringMatcherMode.CHECK_CONTAINS);
					if (matcher.matches(searchResult.localeName)) {
						resultMatcher.publish(searchResult);
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_TRACK_API_PRIORITY;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}
	}

	public static class SearchIndexItemApi extends SearchBaseAPI {

		private final OsmandApplication app;

		public SearchIndexItemApi(OsmandApplication app) {
			super(ObjectType.INDEX_ITEM);
			this.app = app;
		}

		@Override
		public boolean search(SearchPhrase phrase,
		                      SearchResultMatcher resultMatcher) {
			DownloadResources indexes = app.getDownloadThread().getIndexes();
			DownloadIndexesThread thread = app.getDownloadThread();
			if (!indexes.isDownloadedFromInternet && app.getSettings().isInternetConnectionAvailable()) {
				app.runInUIThread(thread::runReloadIndexFilesSilent);
			} else {
				processGroup(indexes, phrase, resultMatcher);
			}
			return true;
		}

		private void processGroup(DownloadResourceGroup group,
		                          SearchPhrase phrase,
		                          SearchResultMatcher resultMatcher) {
			IndexItem indexItem = null;
			String name = null;
			WorldRegion region = group.getRegion();
			if (region != null) {
				String searchText = region.getRegionSearchText();
				if (searchText != null) {
					name = searchText;
				}
			}
			if (name == null) {
				name = group.getName(app);
			}

			if (group.getType().isScreen() && group.getParentGroup() != null
					&& group.getParentGroup().getParentGroup() != null
					&& group.getParentGroup().getParentGroup().getType() != DownloadResourceGroupType.WORLD
					&& isMatch(phrase, name)) {

				for (DownloadResourceGroup g : group.getGroups()) {
					if (g.getType() == DownloadResourceGroupType.REGION_MAPS) {
						List<IndexItem> res = g.getIndividualResources();
						if (res != null) {
							for (IndexItem item : res) {
								if (DownloadActivityType.NORMAL_FILE == item.getType() && !item.isDownloaded()) {
									indexItem = item;
									break;
								}
							}
						}
						break;
					}
				}
			}

			if (indexItem != null) {
				SearchResult searchResult = new SearchResult(phrase);
				searchResult.objectType = ObjectType.INDEX_ITEM;
				searchResult.localeName = name;
				searchResult.relatedObject = indexItem;
				searchResult.priority = SEARCH_INDEX_ITEM_PRIORITY;
				searchResult.preferredZoom = SearchCoreFactory.PREFERRED_INDEX_ITEM_ZOOM;
				resultMatcher.publish(searchResult);
			}

			// process sub groups
			if (group.getGroups() != null) {
				for (DownloadResourceGroup g : group.getGroups()) {
					processGroup(g, phrase, resultMatcher);
				}
			}
		}

		private boolean isMatch(SearchPhrase phrase, String text) {
			if (phrase.getFullSearchPhrase().length() <= 1 && phrase.isNoSelectedType()) {
				return true;
			}
			NameStringMatcher matcher = new NameStringMatcher(
					phrase.getFullSearchPhrase(),
					StringMatcherMode.CHECK_EQUALS_FROM_SPACE
			);
			return matcher.matches(text);
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_INDEX_ITEM_API_PRIORITY;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

	}

	@Override
	public void onMapsIndexed() {
		mapsIndexed = true;
	}

	public static void showPoiFilterOnMap(@NonNull MapActivity mapActivity,
	                                      @NonNull PoiUIFilter filter,
	                                      @Nullable Runnable action) {
		TopToolbarController controller = new PoiFilterBarController();
		View.OnClickListener listener = v -> {
			hidePoiFilterOnMap(mapActivity, controller, action);
			mapActivity.getFragmentsHelper().showQuickSearch(filter);
		};
		controller.setOnBackButtonClickListener(listener);
		controller.setOnTitleClickListener(listener);
		controller.setOnCloseButtonClickListener(v -> hidePoiFilterOnMap(mapActivity, controller, action));
		controller.setTitle(filter.getName());
		PoiFiltersHelper helper = mapActivity.getMyApplication().getPoiFilters();
		helper.replaceSelectedPoiFilters(filter);
		mapActivity.showTopToolbar(controller);
		mapActivity.refreshMap();
	}

	private static void hidePoiFilterOnMap(@NonNull MapActivity mapActivity,
	                                       @NonNull TopToolbarController controller,
	                                       @Nullable Runnable action) {
		mapActivity.hideTopToolbar(controller);
		mapActivity.getMyApplication().getPoiFilters().restoreSelectedPoiFilters();
		mapActivity.refreshMap();
		if (action != null) {
			action.run();
		}
	}

	private static class PoiFilterBarController extends TopToolbarController {

		PoiFilterBarController() {
			super(TopToolbarControllerType.POI_FILTER);
		}
	}
}
