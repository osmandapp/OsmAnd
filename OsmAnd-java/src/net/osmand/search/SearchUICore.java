package net.osmand.search;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.search.core.SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI;
import net.osmand.search.core.SearchCoreFactory.SearchStreetByCityAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchUICore {

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int TIMEOUT_BEFORE_SEARCH = 50;
	private static final Log LOG = PlatformUtil.getLog(SearchUICore.class);
	private SearchPhrase phrase;
	private SearchResultCollection  currentSearchResult;

	private ThreadPoolExecutor singleThreadedExecutor;
	private LinkedBlockingQueue<Runnable> taskQueue;
	private Runnable onSearchStart = null;
	private Runnable onResultsComplete = null;
	private AtomicInteger requestNumber = new AtomicInteger();
	private int totalLimit = -1; // -1 unlimited - not used

	List<SearchCoreAPI> apis = new ArrayList<>();
	private SearchSettings searchSettings;
	private MapPoiTypes poiTypes;


	public SearchUICore(MapPoiTypes poiTypes, String locale, boolean transliterate) {
		this.poiTypes = poiTypes;
		taskQueue = new LinkedBlockingQueue<Runnable>();
		searchSettings = new SearchSettings(new ArrayList<BinaryMapIndexReader>());
		searchSettings = searchSettings.setLang(locale, transliterate);
		phrase = new SearchPhrase(searchSettings, OsmAndCollator.primaryCollator());
		currentSearchResult = new SearchResultCollection(phrase);
		singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
	}

	public static class SearchResultCollection {
		private List<SearchResult> searchResults;
		private SearchPhrase phrase;
		private static final int DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20;

		public SearchResultCollection(SearchPhrase phrase) {
			searchResults = new ArrayList<>();
			this.phrase = phrase;
		}

		public SearchResultCollection combineWithCollection(SearchResultCollection collection, boolean resort, boolean removeDuplicates) {
			SearchResultCollection src = new SearchResultCollection(phrase);
			src.addSearchResults(searchResults, false, false);
			src.addSearchResults(collection.searchResults, resort, removeDuplicates);
			return src;
		}

		public SearchResultCollection addSearchResults(List<SearchResult> sr, boolean resortAll, boolean removeDuplicates) {
			if (resortAll) {
				this.searchResults.addAll(sr);
				sortSearchResults();
				if (removeDuplicates) {
					filterSearchDuplicateResults();
				}
			} else {
				if (!removeDuplicates) {
					this.searchResults.addAll(sr);
				} else {
					ArrayList<SearchResult> addedResults = new ArrayList<>(sr);
					SearchResultComparator cmp = new SearchResultComparator(phrase);
					Collections.sort(addedResults, cmp);
					filterSearchDuplicateResults(addedResults);
					int i = 0;
					int j = 0;
					while (j < addedResults.size()) {
						SearchResult addedResult = addedResults.get(j);
						if (i >= searchResults.size()) {
							int k = 0;
							boolean same = false;
							while (searchResults.size() > k && k < DEPTH_TO_CHECK_SAME_SEARCH_RESULTS) {
								if (sameSearchResult(addedResult, searchResults.get(searchResults.size() - k - 1))) {
									same = true;
									break;
								}
								k++;
							}
							if (!same) {
								searchResults.add(addedResult);
							}
							j++;
							continue;
						}
						SearchResult existingResult = searchResults.get(i);
						if (sameSearchResult(addedResult, existingResult)) {
							j++;
							continue;
						}
						int compare = cmp.compare(existingResult, addedResult);
						if (compare == 0) {
							// existingResult == addedResult
							j++;
						} else if (compare > 0) {
							// existingResult > addedResult
							this.searchResults.add(addedResults.get(j));
							j++;
						} else {
							// existingResult < addedResult
							i++;
						}
					}
				}
			}
			return this;
		}

		public List<SearchResult> getCurrentSearchResults() {
			return Collections.unmodifiableList(searchResults);
		}

		public SearchPhrase getPhrase() {
			return phrase;
		}

		public void sortSearchResults() {
			Collections.sort(searchResults, new SearchResultComparator(phrase));
		}

		public void filterSearchDuplicateResults() {
			filterSearchDuplicateResults(searchResults);
		}

		private void filterSearchDuplicateResults(List<SearchResult> lst) {
			ListIterator<SearchResult> it = lst.listIterator();
			LinkedList<SearchResult> lstUnique = new LinkedList<SearchResult>();
			while (it.hasNext()) {
				SearchResult r = it.next();
				boolean same = false;
				for (SearchResult rs : lstUnique) {
					same = sameSearchResult(rs, r);
					if (same) {
						break;
					}
				}
				if (same) {
					it.remove();
				} else {
					lstUnique.add(r);
					if (lstUnique.size() > DEPTH_TO_CHECK_SAME_SEARCH_RESULTS) {
						lstUnique.remove(0);
					}
				}
			}
		}

		public boolean sameSearchResult(SearchResult r1, SearchResult r2) {
			if (r1.location != null && r2.location != null) {
				Amenity a1 = null;
				if (r1.object instanceof Amenity) {
					a1 = (Amenity) r1.object;
				}
				Amenity a2 = null;
				if (r2.object instanceof Amenity) {
					a2 = (Amenity) r2.object;
				}
				if (r1.localeName.equals(r2.localeName)) {
					double similarityRadius = 30;
					if (a1 != null && a2 != null) {
						// here 2 points are amenity
						String type1 = a1.getType().getKeyName();
						String type2 = a2.getType().getKeyName();
						String subType1 = a1.getSubType();
						String subType2 = a2.getSubType();
						if(a1.getId().longValue() == a2.getId().longValue() && (subType1.equals("building") || subType2.equals("building"))) {
							return true;
						}
						if (!type1.equals(type2)) {
							return false;
						}
						if (type1.equals("natural")) {
							similarityRadius = 50000;
						} else if (subType1.equals(subType2)) {
							if (subType1.contains("cn_ref") || subType1.contains("wn_ref")
									|| (subType1.startsWith("route_hiking_") && subType1.endsWith("n_poi"))) {
								similarityRadius = 50000;
							}
						}
					} else if (ObjectType.isAddress(r1.objectType) && ObjectType.isAddress(r2.objectType)) {
						similarityRadius = 100;
					}
					return MapUtils.getDistance(r1.location, r2.location) < similarityRadius;
				}
			} else if (r1.object != null && r2.object != null) {
				return r1.object == r2.object;
			}
			return false;
		}
	}

	public void setPoiTypes(MapPoiTypes poiTypes) {
		this.poiTypes = poiTypes;
	}

	public int getTotalLimit() {
		return totalLimit;
	}

	public void setTotalLimit(int totalLimit) {
		this.totalLimit = totalLimit;
	}

	@SuppressWarnings("unchecked")
	public <T> T getApiByClass(Class<T> cl) {
		for (SearchCoreAPI a : apis) {
			if (cl.isInstance(a)) {
				return (T) a;
			}
		}
		return null;
	}

	public <T extends SearchCoreAPI> SearchResultCollection shallowSearch(Class<T> cl,
																		  String text, final ResultMatcher<SearchResult> matcher) throws IOException {
		T api = getApiByClass(cl);
		if (api != null) {
			SearchPhrase sphrase = this.phrase.generateNewPhrase(text, searchSettings);
			preparePhrase(sphrase);
			AtomicInteger ai = new AtomicInteger();
			SearchResultMatcher rm = new SearchResultMatcher(matcher, sphrase, ai.get(), ai, totalLimit);
			api.search(sphrase, rm);

			SearchResultCollection collection = new SearchResultCollection(
					sphrase);
			collection.addSearchResults(rm.getRequestResults(), true, true);
			LOG.info(">> Shallow Search phrase " + phrase + " " + rm.getRequestResults().size());
			return collection;
		}
		return null;
	}

	public void init() {
		apis.add(new SearchCoreFactory.SearchLocationAndUrlAPI());
		apis.add(new SearchCoreFactory.SearchAmenityTypesAPI(poiTypes));
		apis.add(new SearchCoreFactory.SearchAmenityByTypeAPI(poiTypes));
		apis.add(new SearchCoreFactory.SearchAmenityByNameAPI());
		SearchBuildingAndIntersectionsByStreetAPI streetsApi =
				new SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI();
		apis.add(streetsApi);
		SearchStreetByCityAPI cityApi = new SearchCoreFactory.SearchStreetByCityAPI(streetsApi);
		apis.add(cityApi);
		apis.add(new SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi));
	}

	public void clearCustomSearchPoiFilters() {
		for (SearchCoreAPI capi : apis) {
			if (capi instanceof SearchAmenityTypesAPI) {
				((SearchAmenityTypesAPI) capi).clearCustomFilters();
			}
		}
	}

	public void addCustomSearchPoiFilter(CustomSearchPoiFilter poiFilter, int priority) {
		for (SearchCoreAPI capi : apis) {
			if (capi instanceof SearchAmenityTypesAPI) {
				((SearchAmenityTypesAPI) capi).addCustomFilter(poiFilter, priority);
			}
		}
	}

	public void registerAPI(SearchCoreAPI api) {
		apis.add(api);
	}


	public SearchResultCollection getCurrentSearchResult() {
		return currentSearchResult;
	}

	public SearchPhrase getPhrase() {
		return phrase;
	}

	public void setOnSearchStart(Runnable onSearchStart) {
		this.onSearchStart = onSearchStart;
	}

	public void setOnResultsComplete(Runnable onResultsComplete) {
		this.onResultsComplete = onResultsComplete;
	}

	public SearchSettings getSearchSettings() {
		return searchSettings;
	}

	public void updateSettings(SearchSettings settings) {
		searchSettings = settings;
	}

	private List<SearchResult> filterCurrentResults(List<SearchResult> rr, SearchPhrase phrase) {
		List<SearchResult> l = currentSearchResult.searchResults;
		for (SearchResult r : l) {
			if (filterOneResult(r, phrase)) {
				rr.add(r);
			}
		}
		return rr;
	}

	private boolean filterOneResult(SearchResult object, SearchPhrase phrase) {
		NameStringMatcher nameStringMatcher = phrase.getNameStringMatcher();
		return nameStringMatcher.matches(object.localeName) || nameStringMatcher.matches(object.otherNames);
	}

	public boolean selectSearchResult(SearchResult r) {
		this.phrase = this.phrase.selectWord(r);
		return true;
	}

	public SearchPhrase resetPhrase() {
		this.phrase = this.phrase.generateNewPhrase("", searchSettings);
		return this.phrase;
	}

	public SearchPhrase resetPhrase(String text) {
		this.phrase = this.phrase.generateNewPhrase(text, searchSettings);
		return this.phrase;
	}

	public SearchResultCollection search(final String text, final boolean delayedExecution, final ResultMatcher<SearchResult> matcher) {
		final int request = requestNumber.incrementAndGet();
		final SearchPhrase phrase = this.phrase.generateNewPhrase(text, searchSettings);
		this.phrase = phrase;
		SearchResultCollection quickRes = new SearchResultCollection(phrase);
		filterCurrentResults(quickRes.searchResults, phrase);
		LOG.info("> Search phrase " + phrase + " " + quickRes.searchResults.size());
		singleThreadedExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (onSearchStart != null) {
						onSearchStart.run();
					}
					SearchResultMatcher rm = new SearchResultMatcher(matcher, phrase, request, requestNumber, totalLimit);
					rm.searchStarted(phrase);
					if (TIMEOUT_BETWEEN_CHARS > 0 && delayedExecution) {
						long startTime = System.currentTimeMillis();
						while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
							if (rm.isCancelled()) {
								return;
							}
							Thread.sleep(TIMEOUT_BEFORE_SEARCH);
						}
					} else if (TIMEOUT_BEFORE_SEARCH > 0) {
						Thread.sleep(TIMEOUT_BEFORE_SEARCH);
					}
					if (rm.isCancelled()) {
						return;
					}
					searchInBackground(phrase, rm);
					if (!rm.isCancelled()) {
						SearchResultCollection collection = new SearchResultCollection(
								phrase);
						collection.addSearchResults(rm.getRequestResults(), true, true);
						LOG.info(">> Search phrase " + phrase + " " + rm.getRequestResults().size());
						currentSearchResult = collection;
						rm.searchFinished(phrase);
						if (onResultsComplete != null) {
							onResultsComplete.run();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}


		});
		return quickRes;
	}


	public boolean isSearchMoreAvailable(SearchPhrase phrase) {
		for (SearchCoreAPI api : apis) {
			if (api.getSearchPriority(phrase) >= 0 && api.isSearchMoreAvailable(phrase)) {
				return true;
			}
		}
		return false;
	}

	private void searchInBackground(final SearchPhrase phrase, SearchResultMatcher matcher) {
		preparePhrase(phrase);
		ArrayList<SearchCoreAPI> lst = new ArrayList<>(apis);
		Collections.sort(lst, new Comparator<SearchCoreAPI>() {

			@Override
			public int compare(SearchCoreAPI o1, SearchCoreAPI o2) {
				return Algorithms.compare(o1.getSearchPriority(phrase),
						o2.getSearchPriority(phrase));
			}
		});
		for (SearchCoreAPI api : lst) {
			if (matcher.isCancelled()) {
				break;
			}
			if (!api.isSearchAvailable(phrase) || api.getSearchPriority(phrase) == -1) {
				continue;
			}
			try {
				api.search(phrase, matcher);

				matcher.apiSearchFinished(api, phrase);
			} catch (Throwable e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private void preparePhrase(final SearchPhrase phrase) {
		for (SearchWord sw : phrase.getWords()) {
			if (sw.getResult() != null && sw.getResult().file != null) {
				phrase.selectFile(sw.getResult().file);
			}
		}
		phrase.sortFiles();
	}




	public static class SearchResultMatcher implements  ResultMatcher<SearchResult>{
		private final List<SearchResult> requestResults = new ArrayList<>();
		private final ResultMatcher<SearchResult> matcher;
		private final int request;
		private final int totalLimit;
		private SearchResult parentSearchResult;
		private final AtomicInteger requestNumber;
		int count = 0;
		private SearchPhrase phrase;


		public SearchResultMatcher(ResultMatcher<SearchResult> matcher, SearchPhrase phrase, int request,
								   AtomicInteger requestNumber, int totalLimit) {
			this.matcher = matcher;
			this.phrase = phrase;
			this.request = request;
			this.requestNumber = requestNumber;
			this.totalLimit = totalLimit;
		}

		public SearchResult setParentSearchResult(SearchResult parentSearchResult) {
			SearchResult prev = this.parentSearchResult;
			this.parentSearchResult = parentSearchResult;
			return prev;
		}

		public List<SearchResult> getRequestResults() {
			return requestResults;
		}

		public int getCount() {
			return requestResults.size();
		}

		public void searchStarted(SearchPhrase phrase) {
			if (matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_STARTED;
				matcher.publish(sr);
			}
		}

		public void searchFinished(SearchPhrase phrase) {
			if (matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_FINISHED;
				matcher.publish(sr);
			}
		}

		public void apiSearchFinished(SearchCoreAPI api, SearchPhrase phrase) {
			if (matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_API_FINISHED;
				sr.object = api;
				sr.parentSearchResult = parentSearchResult;
				matcher.publish(sr);
			}
		}

		public void apiSearchRegionFinished(SearchCoreAPI api, BinaryMapIndexReader region, SearchPhrase phrase) {
			if(matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_API_REGION_FINISHED;
				sr.object = api;
				sr.parentSearchResult = parentSearchResult;
				sr.file = region;
				matcher.publish(sr);
			}
		}

		@Override
		public boolean publish(SearchResult object) {
			if (phrase != null && object.otherNames != null && !phrase.getNameStringMatcher().matches(object.localeName)) {
				for (String s : object.otherNames) {
					if (phrase.getNameStringMatcher().matches(s)) {
						object.alternateName = s;
						break;
					}
				}
			}
			if (matcher == null || matcher.publish(object)) {
				count++;
				object.parentSearchResult = parentSearchResult;
				if (totalLimit == -1 || count < totalLimit) {
					requestResults.add(object);
				}
				return true;
			}
			return false;
		}
		@Override
		public boolean isCancelled() {
			boolean cancelled = request != requestNumber.get();
			return cancelled || (matcher != null && matcher.isCancelled());
		}
	}

	public static class SearchResultComparator implements Comparator<SearchResult> {
		private SearchPhrase sp;
		private Collator collator;
		private LatLon loc;
		private boolean sortByName;

		public SearchResultComparator(SearchPhrase sp) {
			this.sp = sp;
			this.collator = sp.getCollator();
			loc = sp.getLastTokenLocation();
			sortByName = sp.isSortByName();
		}

		@Override
		public int compare(SearchResult o1, SearchResult o2) {
			if (o1.getFoundWordCount() != o2.getFoundWordCount()) {
				return -Algorithms.compare(o1.getFoundWordCount(), o2.getFoundWordCount());
			}
			if (!sortByName) {
				double s1 = o1.getSearchDistance(loc);
				double s2 = o2.getSearchDistance(loc);
				int cmp = Double.compare(s1, s2);
				if (cmp != 0) {
					return cmp;
				}
			}
			int st1 = Algorithms.extractFirstIntegerNumber(o1.localeName);
			int st2 = Algorithms.extractFirstIntegerNumber(o2.localeName);
			if (st1 != st2) {
				return Algorithms.compare(st1, st2);
			}
			int cmp = collator.compare(o1.localeName, o2.localeName);
			if (cmp != 0) {
				return cmp;
			}
			if (o1.object instanceof Amenity && o2.object instanceof Amenity) {
				// here 2 points are amenity
				Amenity a1 = (Amenity) o1.object;
				Amenity a2 = (Amenity) o2.object;
				String type1 = a1.getType().getKeyName();
				String type2 = a2.getType().getKeyName();
				String subType1 = a1.getSubType();
				String subType2 = a2.getSubType();
				cmp = collator.compare(type1, type2);
				if (cmp != 0) {
					return cmp;
				}
				cmp = collator.compare(subType1, subType2);
				if (cmp != 0) {
					return cmp;
				}
			}

			double s1 = o1.getSearchDistance(loc, 1);
			double s2 = o2.getSearchDistance(loc, 1);
			return Double.compare(s1, s2);
		}

	}
}
