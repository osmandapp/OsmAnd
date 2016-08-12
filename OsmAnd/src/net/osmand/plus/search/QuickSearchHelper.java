package net.osmand.plus.search;

import java.util.Arrays;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;

public class QuickSearchHelper implements ResourceListener {

	public static final int SEARCH_FAVORITE_API_PRIORITY = 2;
	public static final int SEARCH_FAVORITE_API_CATEGORY_PRIORITY = 7;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 10;
	public static final int SEARCH_WPT_API_PRIORITY = 2;
	public static final int SEARCH_WPT_OBJECT_PRIORITY = 10;
	public static final int SEARCH_HISTORY_API_PRIORITY = 3;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 10;
	private OsmandApplication app;
	private SearchUICore core;
	private SearchResultCollection resultCollection;
	private boolean mapsIndexed;

	public QuickSearchHelper(OsmandApplication app) {
		this.app = app;
		core = new SearchUICore(app.getPoiTypes(), app.getSettings().MAP_PREFERRED_LOCALE.get(), false);
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
		core.registerAPI(new SearchCoreFactory.SearchBaseAPI() {

			@Override
			public boolean isSearchMoreAvailable(SearchPhrase phrase) {
				return false;
			}
			
			@Override
			public boolean search(SearchPhrase phrase, SearchUICore.SearchResultMatcher resultMatcher) {
				List<FavouritePoint> favList = app.getFavorites().getFavouritePoints();
				for (FavouritePoint point : favList) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = point.getName();
					sr.object = point;
					sr.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
					sr.objectType = ObjectType.FAVORITE;
					sr.location = new LatLon(point.getLatitude(), point.getLongitude());
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
				if(!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
					return -1;
				}
				return SEARCH_FAVORITE_API_PRIORITY;
			}
		});

		// Register favorites by category search api
		core.registerAPI(new SearchCoreFactory.SearchBaseAPI() {
			
			@Override
			public boolean isSearchMoreAvailable(SearchPhrase phrase) {
				return false;
			}

			@Override
			public boolean search(SearchPhrase phrase, SearchUICore.SearchResultMatcher resultMatcher) {
				List<FavouritePoint> favList = app.getFavorites().getFavouritePoints();
				for (FavouritePoint point : favList) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = point.getName();
					sr.object = point;
					sr.priority = SEARCH_FAVORITE_OBJECT_PRIORITY;
					sr.objectType = ObjectType.FAVORITE;
					sr.location = new LatLon(point.getLatitude(), point.getLongitude());
					sr.preferredZoom = 17;
					if (point.getCategory() != null && phrase.getNameStringMatcher().matches(point.getCategory())) {
						resultMatcher.publish(sr);
					}
				}
				return true;
			}

			@Override
			public int getSearchPriority(SearchPhrase p) {
				if(!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
					return -1;
				}
				return SEARCH_FAVORITE_API_CATEGORY_PRIORITY;
			}
		});

		// Register WptPt search api
		core.registerAPI(new SearchWptAPI(app));
		core.registerAPI(new SearchHistoryAPI(app));
		
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		for(CustomSearchPoiFilter udf : poiFilters.getUserDefinedPoiFilters()) {
			core.addCustomSearchPoiFilter(udf, 0);
		}
		core.addCustomSearchPoiFilter(poiFilters.getLocalWikiPOIFilter(), 1);
		core.addCustomSearchPoiFilter(poiFilters.getShowAllPOIFilter(), 1);
	}

	public void setRepositoriesForSearchUICore(final OsmandApplication app) {
		BinaryMapIndexReader[] binaryMapIndexReaderArray = app.getResourceManager().getQuickSearchFiles();
		core.getSearchSettings().setOfflineIndexes(Arrays.asList(binaryMapIndexReaderArray));
	}

	public static class SearchWptAPI extends SearchCoreFactory.SearchBaseAPI {

		private OsmandApplication app;

		public SearchWptAPI(OsmandApplication app) {
			this.app = app;
		}
		
		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchUICore.SearchResultMatcher resultMatcher) {

			if (phrase.isEmpty()) {
				return false;
			}

			List<GpxSelectionHelper.SelectedGpxFile> list = app.getSelectedGpxHelper().getSelectedGPXFiles();
			for (GpxSelectionHelper.SelectedGpxFile selectedGpx : list) {
				if (selectedGpx != null) {
					for (GPXUtilities.WptPt point : selectedGpx.getGpxFile().points) {
						SearchResult sr = new SearchResult(phrase);
						sr.localeName = point.getPointDescription(app).getName();
						sr.object = point;
						sr.priority = SEARCH_WPT_OBJECT_PRIORITY;
						sr.objectType = ObjectType.WPT;
						sr.location = new LatLon(point.getLatitude(), point.getLongitude());
						sr.localeRelatedObjectName = app.getRegions().getCountryName(sr.location);
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
			if(!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_WPT_API_PRIORITY;
		}
	}

	public static class SearchHistoryAPI extends SearchCoreFactory.SearchBaseAPI {

		private OsmandApplication app;

		public SearchHistoryAPI(OsmandApplication app) {
			this.app = app;
		}
		
		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchUICore.SearchResultMatcher resultMatcher) {
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
