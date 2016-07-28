package net.osmand.plus.search;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.resources.RegionAddressRepository;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QuickSearchHelper {

	public static final int SEARCH_FAVORITE_API_PRIORITY = 2;
	public static final int SEARCH_FAVORITE_OBJECT_PRIORITY = 10;
	public static final int SEARCH_WPT_API_PRIORITY = 2;
	public static final int SEARCH_WPT_OBJECT_PRIORITY = 10;
	public static final int SEARCH_HISTORY_API_PRIORITY = 3;
	public static final int SEARCH_HISTORY_OBJECT_PRIORITY = 10;
	private OsmandApplication app;
	private SearchUICore core;
	
	public QuickSearchHelper(OsmandApplication app) {
		this.app = app;
		core = new SearchUICore(app.getPoiTypes(), app.getSettings().MAP_PREFERRED_LOCALE.get(), new BinaryMapIndexReader[]{});
	}
	
	public SearchUICore getCore() {
		return core;
	}

	public void initSearchUICore() {
		setRepositoriesForSearchUICore(app);
		// Register favorites search api
		core.registerAPI(new SearchCoreFactory.SearchBaseAPI() {

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
					} else if (point.getCategory() != null && phrase.getNameStringMatcher().matches(point.getCategory())) {
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

		// Register WptPt search api
		core.registerAPI(new SearchWptAPI(app));
	}

	public void setRepositoriesForSearchUICore(final OsmandApplication app) {
		Collection<RegionAddressRepository> regionAddressRepositories = app.getResourceManager().getAddressRepositories();
		BinaryMapIndexReader[] binaryMapIndexReaderArray = new BinaryMapIndexReader[regionAddressRepositories.size()];
		int i = 0;
		for (RegionAddressRepository rep : regionAddressRepositories) {
			binaryMapIndexReaderArray[i++] = rep.getFile();
		}
		core.getSearchSettings().setOfflineIndexes(Arrays.asList(binaryMapIndexReaderArray));
	}

	public static class SearchWptAPI extends SearchCoreFactory.SearchBaseAPI {

		private OsmandApplication app;

		public SearchWptAPI(OsmandApplication app) {
			this.app = app;
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
		public boolean search(SearchPhrase phrase, SearchUICore.SearchResultMatcher resultMatcher) {
			SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
			List<SearchHistoryHelper.HistoryEntry> points = helper.getHistoryEntries();
			for (SearchHistoryHelper.HistoryEntry point : points) {
				SearchResult sr = new SearchResult(phrase);
				sr.localeName = point.getName().getName();
				sr.object = point;
				sr.priority = SEARCH_HISTORY_OBJECT_PRIORITY;
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
			if (!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_HISTORY_API_PRIORITY;
		}
	}
}
