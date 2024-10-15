package net.osmand.search;

import net.osmand.CallbackWithObject;
import net.osmand.Collator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.CustomSearchPoiFilter;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityByNameAPI;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityByTypeAPI;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.search.core.SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI;
import net.osmand.search.core.SearchCoreFactory.SearchStreetByCityAPI;
import net.osmand.search.core.SearchExportSettings;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchUICore {
	

	private static final int TIMEOUT_BETWEEN_CHARS = 700;
	private static final int TIMEOUT_BEFORE_SEARCH = 50;
	private static final int TIMEOUT_BEFORE_FILTER = 20;
	private static final Log LOG = PlatformUtil.getLog(SearchUICore.class);
	private SearchPhrase phrase;
	private SearchResultCollection currentSearchResult;

	private ThreadPoolExecutor singleThreadedExecutor;
	private LinkedBlockingQueue<Runnable> taskQueue;
	private Runnable onSearchStart = null;
	private Runnable onResultsComplete = null;
	private AtomicInteger requestNumber = new AtomicInteger();
	private int totalLimit = -1; // -1 unlimited - not used

	List<SearchCoreAPI> apis = new ArrayList<>();
	private SearchSettings searchSettings;
	private MapPoiTypes poiTypes;

	private static boolean debugMode = false;
	
	private static final Set<String> FILTER_DUPLICATE_POI_SUBTYPE = new TreeSet<String>(
			Arrays.asList("building", "internet_access_yes"));

	public SearchUICore(MapPoiTypes poiTypes, String locale, boolean transliterate) {
		this.poiTypes = poiTypes;
		taskQueue = new LinkedBlockingQueue<Runnable>();
		searchSettings = new SearchSettings(new ArrayList<BinaryMapIndexReader>());
		searchSettings = searchSettings.setLang(locale, transliterate);
		phrase = SearchPhrase.emptyPhrase(searchSettings);
		currentSearchResult = new SearchResultCollection(phrase);
		singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
	}

	public static void setDebugMode(boolean debugMode) {
		SearchUICore.debugMode = debugMode;
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	public static class SearchResultCollection {
		private final List<SearchResult> searchResults = new ArrayList<>();
		private SearchPhrase phrase;
		private boolean useLimit;
		private static final int DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20;

		public SearchResultCollection(SearchPhrase phrase) {
			this.phrase = phrase;
		}

		public SearchResultCollection combineWithCollection(SearchResultCollection collection, boolean resort, boolean removeDuplicates) {
			SearchResultCollection src = new SearchResultCollection(phrase);
			src.addSearchResults(searchResults, false, false);
			src.addSearchResults(collection.searchResults, resort, removeDuplicates);
			return src;
		}
		
		public boolean getUseLimit() {
			return this.useLimit;
		}
		
		public void setUseLimit(boolean useLimit) {
			this.useLimit = useLimit;
		}

		public SearchResultCollection addSearchResults(List<SearchResult> sr, boolean resortAll, boolean removeDuplicates) {
			if (SearchUICore.isDebugMode()) {
				LOG.info("Add search results resortAll=" + (resortAll ? "true" : "false") + " removeDuplicates=" + (removeDuplicates ? "true" : "false") + " Results=" + sr.size() + " Current results=" + this.searchResults.size());
			}
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
			if (SearchUICore.isDebugMode()) {
				LOG.info("Search results added. Current results=" + this.searchResults.size());
			}
			return this;
		}

		public boolean hasSearchResults() {
			return !Algorithms.isEmpty(searchResults);
		}

		public List<SearchResult> getCurrentSearchResults() {
			return Collections.unmodifiableList(searchResults);
		}

		public SearchPhrase getPhrase() {
			return phrase;
		}

		public void sortSearchResults() {
			if (debugMode) {
				LOG.info("Sorting search results <" + phrase + "> Results=" + searchResults.size());
			}
			Collections.sort(searchResults, new SearchResultComparator(phrase));
			if (debugMode) {
				LOG.info("Search results sorted <" + phrase + ">");
			}
		}

		public void filterSearchDuplicateResults() {
			if (debugMode) {
				LOG.info("Filter duplicate results <" + phrase + "> Results=" + searchResults.size());
			}
			filterSearchDuplicateResults(searchResults);
			if (debugMode) {
				LOG.info("Duplicate results filtered <" + phrase + "> Results=" + searchResults.size());
			}
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
			boolean isSameType = r1.objectType == r2.objectType;
			if (isSameType) {
				ObjectType type = r1.objectType;
				if (type == ObjectType.INDEX_ITEM || type == ObjectType.GPX_TRACK) {
					return Algorithms.objectEquals(r1.localeName, r2.localeName);
				}
			}
			if (r1.location != null && r2.location != null &&
					!ObjectType.isTopVisible(r1.objectType) && !ObjectType.isTopVisible(r2.objectType)) {
				if (isSameType) {
					if (r1.objectType == ObjectType.STREET) {
						Street st1 = (Street) r1.object;
						Street st2 = (Street) r2.object;
						return st1.getLocation().equals(st2.getLocation());
					}
				}
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

						boolean isEqualId = a1.getId().longValue() == a2.getId().longValue();

						if (isEqualId && (FILTER_DUPLICATE_POI_SUBTYPE.contains(subType1)
								|| FILTER_DUPLICATE_POI_SUBTYPE.contains(subType2))) {
							return true;

						} else if (!type1.equals(type2)) {
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
	
	public MapPoiTypes getPoiTypes() {
		return poiTypes;
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

	public <T extends SearchCoreAPI> SearchResultCollection shallowSearch(Class<T> cl, String text,
	                                                                      ResultMatcher<SearchResult> matcher) throws IOException {
		return shallowSearch(cl, text, matcher, true, true);
	}

	public <T extends SearchCoreAPI> SearchResultCollection shallowSearch(Class<T> cl, String text,
	                                                                      final ResultMatcher<SearchResult> matcher,
	                                                                      boolean resortAll, boolean removeDuplicates) throws IOException {
		return shallowSearch(cl, text, matcher, resortAll, removeDuplicates, searchSettings);
	}

	public <T extends SearchCoreAPI> SearchResultCollection shallowSearch(Class<T> cl, String text,
	                                                                      final ResultMatcher<SearchResult> matcher,
	                                                                      boolean resortAll, boolean removeDuplicates,
	                                                                      SearchSettings searchSettings) throws IOException {
		T api = getApiByClass(cl);
		if (api != null) {
			if (debugMode) {
				LOG.info("Start shallow search <" + phrase + "> API=<" + api + ">");
			}
			SearchPhrase sphrase = this.phrase.generateNewPhrase(text, searchSettings);
			preparePhrase(sphrase);
			AtomicInteger ai = new AtomicInteger();
			SearchResultMatcher rm = new SearchResultMatcher(matcher, sphrase, ai.get(), ai, totalLimit);
			api.search(sphrase, rm);

			SearchResultCollection collection = new SearchResultCollection(sphrase);
			if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
				collection.setUseLimit(true);
			}
			collection.addSearchResults(rm.getRequestResults(), resortAll, removeDuplicates);
			if (debugMode) {
				LOG.info("Finish shallow search <" + sphrase + "> Results=" + rm.getRequestResults().size());
			}
			return collection;
		}
		return null;
	}

	public <T extends SearchCoreAPI> void shallowSearchAsync(final Class<T> cl, final String text,
	                                                         final ResultMatcher<SearchResult> matcher,
	                                                         final boolean resortAll, final boolean removeDuplicates,
	                                                         final SearchSettings searchSettings,
	                                                         final CallbackWithObject<SearchResultCollection> callback) {
		singleThreadedExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					SearchResultCollection collection = shallowSearch(cl, text, matcher, resortAll, removeDuplicates, searchSettings);
					if (callback != null) {
						callback.processResult(collection);
					}
				} catch (IOException e) {
					e.printStackTrace();
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}

	public void init() {
		SearchAmenityByNameAPI amenitiesApi = new SearchCoreFactory.SearchAmenityByNameAPI();
		apis.add(amenitiesApi);
		apis.add(new SearchCoreFactory.SearchLocationAndUrlAPI(amenitiesApi));
		SearchAmenityTypesAPI searchAmenityTypesAPI = new SearchAmenityTypesAPI(poiTypes);
		apis.add(searchAmenityTypesAPI);
		apis.add(new SearchAmenityByTypeAPI(poiTypes, searchAmenityTypesAPI));
		SearchBuildingAndIntersectionsByStreetAPI streetsApi = new SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI();
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
	
	public void setActivePoiFiltersByOrder(List<String> filterOrders) {
		for (SearchCoreAPI capi : apis) {
			if (capi instanceof SearchAmenityTypesAPI) {
				((SearchAmenityTypesAPI) capi).setActivePoiFiltersByOrder(filterOrders);
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

	private void filterCurrentResults(SearchPhrase phrase, ResultMatcher<SearchResult> matcher) {
		if (matcher == null) {
			return;
		}
		List<SearchResult> l = currentSearchResult.searchResults;
		for (SearchResult r : l) {
			if (filterOneResult(r, phrase)) {
				matcher.publish(r);
			}
			if (matcher.isCancelled()) {
				return;
			}
		}
	}

	private boolean filterOneResult(SearchResult object, SearchPhrase phrase) {
		NameStringMatcher nameStringMatcher = phrase.getFirstUnknownNameStringMatcher();
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
	
	public SearchPhrase resetPhrase(SearchResult result) {
		this.phrase = this.phrase.generateNewPhrase("", searchSettings);
		this.phrase.addResult(result, this.phrase);
		return this.phrase;
	}
	
	public SearchResultCollection immediateSearch(final String text, final LatLon loc) {
		if (loc != null) {
			searchSettings = searchSettings.setOriginalLocation(loc);
		}
		final SearchPhrase searchPhrase = this.phrase.generateNewPhrase(text, searchSettings);
		final SearchResultMatcher rm = new SearchResultMatcher(null, searchPhrase, requestNumber.get(), requestNumber, totalLimit);
		searchInternal(searchPhrase, rm);
		SearchResultCollection resultCollection = new SearchResultCollection(searchPhrase);
		if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
			resultCollection.setUseLimit(true);
		}
		resultCollection.addSearchResults(rm.getRequestResults(), true, true);
		return resultCollection;
	}

	public void search(final String text, final boolean delayedExecution, final ResultMatcher<SearchResult> matcher) {
		search(text, delayedExecution, matcher, searchSettings);
	}

	public void search(final String text, final boolean delayedExecution, final ResultMatcher<SearchResult> matcher, final SearchSettings searchSettings) {
		final int request = requestNumber.incrementAndGet();
		final SearchPhrase phrase = this.phrase.generateNewPhrase(text, searchSettings);
		phrase.setAcceptPrivate(this.phrase.isAcceptPrivate());
		this.phrase = phrase;
		if (debugMode) {
			LOG.info("Prepare search <" + phrase + ">");
		}
		singleThreadedExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (onSearchStart != null) {
						onSearchStart.run();
					}
					final SearchResultMatcher rm = new SearchResultMatcher(matcher, phrase, request, requestNumber, totalLimit);
					if (debugMode) {
						LOG.info("Starting search <" + phrase.toString() + ">");
					}
					rm.searchStarted(phrase);
					if (debugMode) {
						LOG.info("Search started <" + phrase.toString() + ">");
					}
					if (delayedExecution) {
						long startTime = System.currentTimeMillis();
						if (debugMode) {
							LOG.info("Wait for next char <" + phrase.toString() + ">");
						}

						boolean filtered = false;
						while (System.currentTimeMillis() - startTime <= TIMEOUT_BETWEEN_CHARS) {
							if (rm.isCancelled()) {
								if (debugMode) {
									LOG.info("Search cancelled <" + phrase + ">");
								}
								return;
							}
							Thread.sleep(TIMEOUT_BEFORE_FILTER);

							if (!filtered) {
								final SearchResultCollection quickRes = new SearchResultCollection(phrase);
								if (debugMode) {
									LOG.info("Filtering current data <" + phrase + "> Results=" + currentSearchResult.searchResults.size());
								}
								filterCurrentResults(phrase, new ResultMatcher<SearchResult>() {
									@Override
									public boolean publish(SearchResult object) {
										quickRes.searchResults.add(object);
										return true;
									}

									@Override
									public boolean isCancelled() {
										return rm.isCancelled();
									}
								});
								if (debugMode) {
									LOG.info("Current data filtered <" + phrase + "> Results=" + quickRes.searchResults.size());
								}
								if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
									quickRes.setUseLimit(true);
								}
								if (!rm.isCancelled()) {
									currentSearchResult = quickRes;
									rm.filterFinished(phrase);
								}
								filtered = true;
							}
						}
					} else {
						Thread.sleep(TIMEOUT_BEFORE_SEARCH);
					}
					if (rm.isCancelled()) {
						if (debugMode) {
							LOG.info("Search cancelled <" + phrase + ">");
						}
						return;
					}
					searchInternal(phrase, rm);
					if (!rm.isCancelled()) {
						SearchResultCollection collection = new SearchResultCollection(phrase);
						if (rm.totalLimit != -1 && rm.count > rm.totalLimit) {
							collection.setUseLimit(true);
						}
						if (debugMode) {
							LOG.info("Processing search results <" + phrase + ">");
						}
						collection.addSearchResults(rm.getRequestResults(), true, true);
						if (debugMode) {
							LOG.info("Finishing search <" + phrase + "> Results=" + rm.getRequestResults().size());
						}
						currentSearchResult = collection;
						if (phrase.getSettings().isExportObjects()) {
							rm.createTestJSON(collection);
						}
						rm.searchFinished(phrase);
						if (onResultsComplete != null) {
							onResultsComplete.run();
						}
						if (debugMode) {
							LOG.info("Search finished <" + phrase + "> Results=" + rm.getRequestResults().size());
						}
					} else {
						if (debugMode) {
							LOG.info("Search cancelled <" + phrase + ">");
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}


	public boolean isSearchMoreAvailable(SearchPhrase phrase) {
		for (SearchCoreAPI api : apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) >= 0
					&& api.isSearchMoreAvailable(phrase)) {
				return true;
			}
		}
		return false;
	}

	public int getMinimalSearchRadius(SearchPhrase phrase) {
		int radius = Integer.MAX_VALUE;
		for (SearchCoreAPI api : apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
				int apiMinimalRadius = api.getMinimalSearchRadius(phrase);
				if (apiMinimalRadius > 0 && apiMinimalRadius < radius) {
					radius = apiMinimalRadius;
				}
			}
		}
		return radius;
	}

	public int getNextSearchRadius(SearchPhrase phrase) {
		int radius = Integer.MAX_VALUE;
		for (SearchCoreAPI api : apis) {
			if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
				int apiNextSearchRadius = api.getNextSearchRadius(phrase);
				if (apiNextSearchRadius > 0 && apiNextSearchRadius < radius) {
					radius = apiNextSearchRadius;
				}
			}
		}
		return radius;
	}

	public AbstractPoiType getUnselectedPoiType() {
		for (SearchCoreAPI capi : apis) {
			if (capi instanceof SearchAmenityByTypeAPI) {
				return ((SearchAmenityByTypeAPI) capi).getUnselectedPoiType();
			}
		}
		return null;
	}

	public String getCustomNameFilter() {
		for (SearchCoreAPI capi : apis) {
			if (capi instanceof SearchAmenityByTypeAPI) {
				return ((SearchAmenityByTypeAPI) capi).getNameFilter();
			}
		}
		return null;
	}

	void searchInternal(final SearchPhrase phrase, SearchResultMatcher matcher) {
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
				if (debugMode) {
					LOG.info("Run API search <" + phrase + "> API=<" + api + ">");
				}
				api.search(phrase, matcher);
				if (debugMode) {
					LOG.info("API search finishing <" + phrase + "> API=<" + api + ">");
				}
				matcher.apiSearchFinished(api, phrase);
				if (debugMode) {
					LOG.info("API search done <" + phrase + "> API=<" + api + ">");
				}
			} catch (Throwable e) {
				e.printStackTrace();
				LOG.error(e.getMessage(), e);
			}
		}
	}

	private void preparePhrase(final SearchPhrase phrase) {
		if (debugMode) {
			LOG.info("Preparing search phrase <" + phrase + ">");
		}
		for (SearchWord sw : phrase.getWords()) {
			if (sw.getResult() != null && sw.getResult().file != null) {
				phrase.selectFile(sw.getResult().file);
			}
		}
		phrase.sortFiles();
		if (debugMode) {
			LOG.info("Search phrase prepared <" + phrase + ">");
		}
	}

	public static class SearchResultMatcher implements ResultMatcher<SearchResult> {
		private final List<SearchResult> requestResults = new ArrayList<>();
		private final ResultMatcher<SearchResult> matcher;
		private final int request;
		int totalLimit;
		private SearchResult parentSearchResult;
		private final AtomicInteger requestNumber;
		int count = 0;
		private SearchPhrase phrase;
		private List<MapObject> exportedObjects;
		private List<City> exportedCities;

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
		
		public SearchResult getParentSearchResult() {
			return parentSearchResult;
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

		public void filterFinished(SearchPhrase phrase) {
			if (matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.FILTER_FINISHED;
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
			if (matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_API_REGION_FINISHED;
				sr.object = api;
				sr.parentSearchResult = parentSearchResult;
				sr.file = region;
				matcher.publish(sr);
				if (debugMode) {
					LOG.info("API region search done <" + phrase + "> API=<" + api + "> Region=<" + region.getFile().getName() + ">");
				}
			}
		}

		@Override
		public boolean publish(SearchResult object) {
			if (phrase != null && object.otherNames != null && !phrase.getFirstUnknownNameStringMatcher().matches(object.localeName)) {
				if (Algorithms.isEmpty(object.alternateName)) {
					for (String s : object.otherNames) {
						if (phrase.getFirstUnknownNameStringMatcher().matches(s)) {
							object.alternateName = s;
							break;
						}
					}
				}
				if (Algorithms.isEmpty(object.alternateName) && object.object instanceof Amenity) {
					for (String value : ((Amenity) object.object).getAdditionalInfoValues(true)) {
						if (phrase.getFirstUnknownNameStringMatcher().matches(value)) {
							object.alternateName = value;
							break;
						}
					}
				}
			}
			if (Algorithms.isEmpty(object.localeName) && object.alternateName != null) {
				object.localeName = object.alternateName;
				object.alternateName = null;
			}
			object.parentSearchResult = parentSearchResult;
			if (matcher == null || matcher.publish(object)) {
				count++;
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

		public List<MapObject> getExportedObjects() {
			return exportedObjects;
		}

		public List<City> getExportedCities() {
			return exportedCities;
		}

		public void exportObject(SearchPhrase phrase, MapObject object) {
			double maxDistance = phrase.getSettings().getExportSettings().getMaxDistance();
			if (maxDistance > 0) {
				double distance = MapUtils.getDistance(phrase.getSettings().getOriginalLocation(), object.getLocation());
				if (distance > maxDistance) {
					return;
				}
			}
			if (exportedObjects == null) {
				exportedObjects = new ArrayList<>();
			}
			exportedObjects.add(object);
		}

		public void exportCity(SearchPhrase phrase, City city) {
			double maxDistance = phrase.getSettings().getExportSettings().getMaxDistance();
			if (maxDistance > 0) {
				double distance = MapUtils.getDistance(phrase.getSettings().getOriginalLocation(), city.getLocation());
				if (distance > maxDistance) {
					return;
				}
			}
			if (exportedCities == null) {
				exportedCities = new ArrayList<>();
			}
			exportedCities.add(city);
		}

		public JSONObject createTestJSON(SearchResultCollection searchResult) {
			JSONObject json = new JSONObject();

			Set<Amenity> amenities = new HashSet<>();
			Set<City> cities;
			Set<City> matchedCities = new HashSet<>();
			Set<City> streetCities = new HashSet<>();
			if (exportedCities != null) {
				cities = new HashSet<>(exportedCities);
			} else {
				cities = new HashSet<>();
			}
			Set<Street> streets = new HashSet<>();
			if (exportedObjects != null) {
				for (MapObject obj : exportedObjects) {
					if (obj instanceof Amenity) {
						amenities.add((Amenity) obj);
					} else if (obj instanceof Street) {
						Street street = (Street) obj;
						streets.add(street);
						if (street.getCity() != null) {
							final City city = street.getCity();
							cities.add(city);
							streetCities.add(city);
						}
					} else if (obj instanceof City) {
						City city = (City) obj;
						cities.add(city);
						matchedCities.add(city);
					}
				}
			}
			for (City city : cities) {
				List<Street> cityStreets = city.getStreets();
				for (Street street : streets) {
					if (city.equals(street.getCity()) && !cityStreets.contains(street)) {
						cityStreets.add(street);
					}
				}
			}

			SearchExportSettings exportSettings = phrase.getSettings().getExportSettings();
			json.put("settings", phrase.getSettings().toJSON());
			json.put("phrase", phrase.getFullSearchPhrase());
			if (searchResult.hasSearchResults()) {
				JSONArray resultsArr = new JSONArray();
				for (SearchResult r : searchResult.searchResults) {
					resultsArr.put(r.toString());
				}
				json.put("results", resultsArr);
			}
			if (amenities.size() > 0) {
				JSONArray amenitiesArr = new JSONArray();
				for (Amenity amenity : amenities) {
					amenitiesArr.put(amenity.toJSON());
				}
				json.put("amenities", amenitiesArr);
			}
			if (cities.size() > 0) {
				JSONArray citiesArr = new JSONArray();
				for (City city : cities) {
					final JSONObject cityObj = city.toJSON(exportSettings.isExportBuildings());
					if (exportedCities != null && exportedCities.contains(city)) {
						if (!exportSettings.isExportEmptyCities()) {
							continue;
						}
						cityObj.put("init", 1);
					}
					if (matchedCities.contains(city)) {
						cityObj.put("matchCity", 1);
					}
					if (streetCities.contains(city)) {
						cityObj.put("matchStreet", 1);
					}
					citiesArr.put(cityObj);
				}
				json.put("cities", citiesArr);
			}
			return json;
		}
	}
	
	private enum ResultCompareStep {
		TOP_VISIBLE,
		FOUND_WORD_COUNT, // more is better (top)
		UNKNOWN_PHRASE_MATCH_WEIGHT, // more is better (top)
		COMPARE_AMENITY_TYPE_ADDITIONAL,
		SEARCH_DISTANCE_IF_NOT_BY_NAME,
		COMPARE_FIRST_NUMBER_IN_NAME,
		COMPARE_DISTANCE_TO_PARENT_SEARCH_RESULT, // makes sense only for inner subqueries
		COMPARE_BY_NAME,
		COMPARE_BY_DISTANCE,
		AMENITY_LAST_AND_SORT_BY_SUBTYPE;

		// -1 - means 1st is less (higher) than 2nd
		public int compare(SearchResult o1, SearchResult o2, SearchResultComparator c) {
			switch(this) {
			case TOP_VISIBLE: 
				boolean topVisible1 = ObjectType.isTopVisible(o1.objectType);
				boolean topVisible2 = ObjectType.isTopVisible(o2.objectType);
				if (topVisible1 != topVisible2) {
					// -1 - means 1st is less than 2nd
					return topVisible1 ? -1 : 1;
				}
				break;
			case FOUND_WORD_COUNT: 
				if (o1.getFoundWordCount() != o2.getFoundWordCount()) {
					return -Algorithms.compare(o1.getFoundWordCount(), o2.getFoundWordCount());
				}
				break;
			case UNKNOWN_PHRASE_MATCH_WEIGHT:
				// here we check how much each sub search result matches the phrase
				// also we sort it by type house -> street/poi -> city/postcode/village/other
				SearchPhrase ph = o1.requiredSearchPhrase;
				double o1PhraseWeight = o1.getUnknownPhraseMatchWeight();
				double o2PhraseWeight = o2.getUnknownPhraseMatchWeight();
				if (o1PhraseWeight == o2PhraseWeight && o1PhraseWeight / SearchResult.MAX_PHRASE_WEIGHT_TOTAL > 1) {
					if (!ph.getUnknownWordToSearchBuildingNameMatcher().matches(stripBraces(o1.localeName))) {
						o1PhraseWeight--;
					}
					if (!ph.getUnknownWordToSearchBuildingNameMatcher().matches(stripBraces(o2.localeName))) {
						o2PhraseWeight--;
					}
				}
				if (o1PhraseWeight != o2PhraseWeight) {
					return -Double.compare(o1PhraseWeight, o2PhraseWeight);
				}
				break;
			case SEARCH_DISTANCE_IF_NOT_BY_NAME: 
				if (!c.sortByName) {
					double s1 = o1.getSearchDistance(c.loc);
					double s2 = o2.getSearchDistance(c.loc);
					if (s1 != s2) {
						return Double.compare(s1, s2);
					}
				}
				break;
			case COMPARE_FIRST_NUMBER_IN_NAME: {
				String localeName1 = o1.localeName == null ? "" : o1.localeName;
				String localeName2 = o2.localeName == null ? "" : o2.localeName;
				int st1 = Algorithms.extractFirstIntegerNumber(localeName1);
				int st2 = Algorithms.extractFirstIntegerNumber(localeName2);
				if (st1 != st2) {
					return Algorithms.compare(st1, st2);
				}
				break;
			}
			case COMPARE_AMENITY_TYPE_ADDITIONAL: {
				boolean additional1 = o1.object instanceof AbstractPoiType && ((AbstractPoiType) o1.object).isAdditional();
				boolean additional2 = o2.object instanceof AbstractPoiType && ((AbstractPoiType) o2.object).isAdditional();
				if (additional1 != additional2) {
					// -1 - means 1st is less than 2nd
					return additional1 ? 1 : -1;
				}
				break;
			}
			case COMPARE_DISTANCE_TO_PARENT_SEARCH_RESULT:
				double ps1 = o1.parentSearchResult == null ? 0 : o1.parentSearchResult.getSearchDistance(c.loc);
				double ps2 = o2.parentSearchResult == null ? 0 : o2.parentSearchResult.getSearchDistance(c.loc);
				if (ps1 != ps2) {
					return Double.compare(ps1, ps2);
				}
				break;
			case COMPARE_BY_NAME: {
				String localeName1 = o1.localeName == null ? "" : o1.localeName;
				String localeName2 = o2.localeName == null ? "" : o2.localeName;
				int cmp = c.collator.compare(localeName1, localeName2);
				if (cmp != 0) {
					return cmp;
				}
				break;
			}
			case COMPARE_BY_DISTANCE:
				double s1 = o1.getSearchDistance(c.loc, 1);
				double s2 = o2.getSearchDistance(c.loc, 1);
				if (s1 != s2) {
					return Double.compare(s1, s2);
				}
				break;
			case AMENITY_LAST_AND_SORT_BY_SUBTYPE: {
				boolean am1 = o1.object instanceof Amenity;
				boolean am2 = o2.object instanceof Amenity;
				if (am1 != am2) {
					// amenity second
					return am1 ? 1 : -1;
				} else if (am1 && am2) {
					// here 2 points are amenity
					Amenity a1 = (Amenity) o1.object;
					Amenity a2 = (Amenity) o2.object;

					String type1 = a1.getType().getKeyName();
					String type2 = a2.getType().getKeyName();
					String subType1 = a1.getSubType() == null ? "" : a1.getSubType();
					String subType2 = a2.getSubType() == null ? "" : a2.getSubType();

					int cmp = 0;
					boolean subtypeFilter1 = FILTER_DUPLICATE_POI_SUBTYPE.contains(subType1);
					boolean subtypeFilter2 = FILTER_DUPLICATE_POI_SUBTYPE.contains(subType2);
					if (subtypeFilter1 != subtypeFilter2) {
						// to filter second
						return subtypeFilter1 ? 1 : -1;
					}
					cmp = c.collator.compare(type1, type2);
					if (cmp != 0) {
						return cmp;
					}

					cmp = c.collator.compare(subType1, subType2);
					if (cmp != 0) {
						return cmp;
					}
				}
				break;
			}
			}
			return 0;
		}
	}
	
	private static String stripBraces(String localeName) {
		int i = localeName.indexOf('(');
		String retName = localeName;
		if (i > -1) {
			retName = localeName.substring(0, i);
			int j = localeName.indexOf(')', i);
			if (j > -1) {
				retName = retName.trim() + ' ' + localeName.substring(j);
			}
		}
		return retName;
	}

	public static class SearchResultComparator implements Comparator<SearchResult> {
		private Collator collator;
		private LatLon loc;
		private boolean sortByName;
		

		public SearchResultComparator(SearchPhrase sp) {
			this.collator = sp.getCollator();
			loc = sp.getLastTokenLocation();
			sortByName = sp.isSortByName();
		}
		

		@Override
		public int compare(SearchResult o1, SearchResult o2) {
			List<ResultCompareStep> steps = new ArrayList<>();
			for (ResultCompareStep step : ResultCompareStep.values()) {
				int r = step.compare(o1, o2, this);
				steps.add(step);
				if (r != 0) {
					// debug crashes and identify non-transitive comparison
					// LOG.debug(String.format("%d: %s o1='%s' o2='%s'", r, steps, o1, o2));
					return r;
				}
			}
			// LOG.debug(String.format("EQUAL: o1='%s' o2='%s'", o1, o2));
			return 0;
		}

	}
}
