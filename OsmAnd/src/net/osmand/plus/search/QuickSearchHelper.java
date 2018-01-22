package net.osmand.plus.search;

import android.support.annotation.NonNull;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.poi.NominatimPoiFilter;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class QuickSearchHelper implements ResourceListener {

	public static final int SEARCH_FAVORITE_API_PRIORITY = 50;
	public static final int SEARCH_FAVORITE_API_CATEGORY_PRIORITY = 50;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 50;
	public static final int SEARCH_FAVORITE_CATEGORY_PRIORITY = 51;
	public static final int SEARCH_WPT_API_PRIORITY = 50;
	public static final int SEARCH_WPT_OBJECT_PRIORITY = 52;
	public static final int SEARCH_HISTORY_API_PRIORITY = 50;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 53;
	public static final int SEARCH_ONLINE_API_PRIORITY = 500;
	public static final int SEARCH_ONLINE_AMENITY_PRIORITY = 500;
	private OsmandApplication app;
	private SearchUICore core;
	private SearchResultCollection resultCollection;
	private boolean mapsIndexed;

	public QuickSearchHelper(OsmandApplication app) {
		this.app = app;
		core = new SearchUICore(app.getPoiTypes(), app.getSettings().MAP_PREFERRED_LOCALE.get(),
				app.getSettings().MAP_TRANSLITERATE_NAMES.get());
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
		// Register favorites search api
		core.registerAPI(new SearchFavoriteAPI(app));

		// Register favorites by category search api
		core.registerAPI(new SearchFavoriteCategoryAPI(app));

		// Register WptPt search api
		core.registerAPI(new SearchWptAPI(app));
		core.registerAPI(new SearchHistoryAPI(app));

		core.registerAPI(new SearchOnlineApi(app));

		refreshCustomPoiFilters();
	}

	public void refreshCustomPoiFilters() {
		core.clearCustomSearchPoiFilters();
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		for (CustomSearchPoiFilter udf : poiFilters.getUserDefinedPoiFilters()) {
			core.addCustomSearchPoiFilter(udf, 0);
		}
		PoiUIFilter localWikiPoiFilter = poiFilters.getLocalWikiPOIFilter();
		if (localWikiPoiFilter != null) {
			core.addCustomSearchPoiFilter(localWikiPoiFilter, 1);
		}
		core.addCustomSearchPoiFilter(poiFilters.getShowAllPOIFilter(), 1);
	}

	public void setRepositoriesForSearchUICore(final OsmandApplication app) {
		BinaryMapIndexReader[] binaryMapIndexReaderArray = app.getResourceManager().getQuickSearchFiles();
		core.getSearchSettings().setOfflineIndexes(Arrays.asList(binaryMapIndexReaderArray));
	}

	public Amenity findAmenity(String name, double lat, double lon, String lang, boolean transliterate) {
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 15);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

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

		private OsmandApplication app;

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

			List<GpxSelectionHelper.SelectedGpxFile> list = app.getSelectedGpxHelper().getSelectedGPXFiles();
			for (GpxSelectionHelper.SelectedGpxFile selectedGpx : list) {
				if (selectedGpx != null) {
					for (GPXUtilities.WptPt point : selectedGpx.getGpxFile().getPoints()) {
						SearchResult sr = new SearchResult(phrase);
						sr.localeName = point.getPointDescription(app).getName();
						sr.object = point;
						sr.priority = SEARCH_WPT_OBJECT_PRIORITY;
						sr.objectType = ObjectType.WPT;
						sr.location = new LatLon(point.getLatitude(), point.getLongitude());
						//sr.localeRelatedObjectName = app.getRegions().getCountryName(sr.location);
						sr.relatedObject = selectedGpx.getGpxFile();
						sr.preferredZoom = 17;
						if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
							resultMatcher.publish(sr);
						} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
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

		private OsmandApplication app;
		private FavouritesDbHelper helper;

		public SearchFavoriteCategoryAPI(OsmandApplication app) {
			super(ObjectType.FAVORITE_GROUP);
			this.app = app;
			this.helper = app.getFavorites();
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			String baseGroupName = app.getString(R.string.shared_string_favorites);
			List<FavoriteGroup> groups = app.getFavorites().getFavoriteGroups();
			for (FavoriteGroup group : groups) {
				if (group.visible) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = Algorithms.isEmpty(group.name) ? baseGroupName : group.name;
					sr.object = group;
					sr.priority = SEARCH_FAVORITE_CATEGORY_PRIORITY;
					sr.objectType = ObjectType.FAVORITE_GROUP;
					sr.preferredZoom = 17;
					if (phrase.getNameStringMatcher().matches(sr.localeName)) {
						if (group.points.size() < 5) {
							for (FavouritePoint point : group.points) {
								SearchResult srp = new SearchResult(phrase);
								srp.localeName = point.getName();
								srp.object = point;
								srp.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
								srp.objectType = ObjectType.FAVORITE;
								srp.location = new LatLon(point.getLatitude(), point.getLongitude());
								srp.preferredZoom = 17;
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

		private OsmandApplication app;

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
			List<FavouritePoint> favList = app.getFavorites().getFavouritePoints();
			for (FavouritePoint point : favList) {
				if (!point.isVisible()) {
					continue;
				}
				SearchResult sr = new SearchResult(phrase);
				sr.localeName = point.getName();
				sr.object = point;
				sr.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
				sr.objectType = ObjectType.FAVORITE;
				sr.location = new LatLon(point.getLatitude(), point.getLongitude());
				sr.preferredZoom = 17;
				if (phrase.isLastWord(ObjectType.FAVORITE_GROUP)) {
					FavoriteGroup group = (FavoriteGroup) phrase.getLastSelectedWord().getResult().object;
					if (group != null && !point.getCategory().equals(group.name)) {
						continue;
					}
				}
				if (phrase.getUnknownSearchWordLength() <= 1
						&& (phrase.isNoSelectedType() || phrase.isLastWord(ObjectType.FAVORITE_GROUP))) {
					resultMatcher.publish(sr);
				} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
					resultMatcher.publish(sr);
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

		private OsmandApplication app;
		private NominatimPoiFilter filter;

		public SearchOnlineApi(OsmandApplication app) {
			super(ObjectType.ONLINE_SEARCH);
			this.app = app;
			this.filter = app.getPoiFilters().getNominatimPOIFilter();
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher matcher) throws IOException {
			double lat = phrase.getSettings().getOriginalLocation().getLatitude();
			double lon = phrase.getSettings().getOriginalLocation().getLongitude();
			String text = phrase.getUnknownSearchPhrase();
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
			sr.preferredZoom = 17;
			return sr;
		}
	}

	public static class SearchHistoryAPI extends SearchBaseAPI {

		private OsmandApplication app;

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
			SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
			List<SearchHistoryHelper.HistoryEntry> points = helper.getHistoryEntries();
			int p = 0;
			for (SearchHistoryHelper.HistoryEntry point : points) {
				SearchResult sr = new SearchResult(phrase);
				sr.localeName = point.getName().getName();
				sr.object = point;
				sr.priority = SEARCH_HISTORY_OBJECT_PRIORITY + (p++);
				sr.objectType = ObjectType.RECENT_OBJ;
				sr.location = new LatLon(point.getLat(), point.getLon());
				sr.preferredZoom = 17;
				if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
					resultMatcher.publish(sr);
				} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
					resultMatcher.publish(sr);
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isEmpty()) {
				return -1;
			}
			return SEARCH_HISTORY_API_PRIORITY;
		}
	}

	@Override
	public void onMapsIndexed() {
		mapsIndexed = true;
	}
}
