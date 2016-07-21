package net.osmand.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class SearchUICore {

	private static final int TIMEOUT_BETWEEN_CHARS = 200;
	private static final Log LOG = PlatformUtil.getLog(SearchUICore.class); 
	private SearchPhrase phrase;
	private SearchResultCollection currentSearchResult = new SearchResultCollection(); 
	
	private ThreadPoolExecutor singleThreadedExecutor;
	private LinkedBlockingQueue<Runnable> taskQueue;
	private Runnable onResultsComplete = null;
	private AtomicInteger requestNumber = new AtomicInteger();
	private int totalLimit = -1; // -1 unlimited - not used
	
	List<SearchCoreAPI> apis = new ArrayList<>();
	private SearchSettings searchSettings;
	private MapPoiTypes poiTypes;
	private Collator collator;
	
	
	public SearchUICore(MapPoiTypes poiTypes, String locale, BinaryMapIndexReader[] searchIndexes) {
		this.poiTypes = poiTypes;
		List<BinaryMapIndexReader> searchIndexesList = Arrays.asList(searchIndexes);
		taskQueue = new LinkedBlockingQueue<Runnable>();
		searchSettings = new SearchSettings(searchIndexesList);
		searchSettings = searchSettings.setLang(locale);
		phrase = new SearchPhrase(searchSettings);
		singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
		init();
		collator = OsmAndCollator.primaryCollator();
	}
	
	public static class SearchResultCollection {
		private List<SearchResult> searchResults;
		private SearchPhrase phrase;
		
		public SearchResultCollection(List<SearchResult> requestResults, SearchPhrase phrase) {
			searchResults = requestResults;
			this.phrase = phrase;
		}
		
		public SearchResultCollection() {
			searchResults = new ArrayList<>();
		}

		public List<SearchResult> getCurrentSearchResults() {
			return searchResults;
		}
		
		public SearchPhrase getPhrase() {
			return phrase;
		}
	}
	
	public int getTotalLimit() {
		return totalLimit;
	}
	
	public void setTotalLimit(int totalLimit) {
		this.totalLimit = totalLimit;
	}
	
	public void init() {
		apis.add(new SearchCoreFactory.SearchAmenityTypesAPI(poiTypes));
		apis.add(new SearchCoreFactory.SearchAmenityByTypeAPI(poiTypes));
		apis.add(new SearchCoreFactory.SearchAmenityByNameAPI());
		apis.add(new SearchCoreFactory.SearchStreetByCityAPI());
		apis.add(new SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI());
		apis.add(new SearchCoreFactory.SearchLocationAndUrlAPI());
		apis.add(new SearchCoreFactory.SearchAddressByNameAPI());
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
		for(SearchResult r : l) {
			if(filterOneResult(r, phrase)) {
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
	
	
	public SearchResultCollection search(final String text, final ResultMatcher<SearchResult> matcher) {
		SearchResultCollection quickRes = new SearchResultCollection();
		final int request = requestNumber.incrementAndGet();
		final SearchPhrase phrase = this.phrase.generateNewPhrase(text, searchSettings);
		this.phrase = phrase;
		quickRes.phrase = phrase;
		filterCurrentResults(quickRes.searchResults, phrase);
		LOG.info("> Search phrase " + phrase + " " + quickRes.searchResults.size());
		singleThreadedExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					SearchResultMatcher rm = new SearchResultMatcher(matcher, request, requestNumber, totalLimit);
					if(rm.isCancelled()) {
						return;
					}
					if(TIMEOUT_BETWEEN_CHARS > 0) { 
						Thread.sleep(TIMEOUT_BETWEEN_CHARS);
					}
					searchInBackground(phrase, rm);
					if (!rm.isCancelled()) {
						sortSearchResults(phrase, rm.getRequestResults());
						filterSearchDuplicateResults(phrase, rm.getRequestResults());
						LOG.info(">> Search phrase " + phrase + " " + rm.getRequestResults().size());
						SearchResultCollection collection = new SearchResultCollection(rm.getRequestResults(),
								phrase);
						currentSearchResult = collection;
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

	private void searchInBackground(final SearchPhrase phrase, SearchResultMatcher matcher) {
		for (SearchWord sw : phrase.getWords()) {
			if(sw.getResult() != null && sw.getResult().file != null) {
				phrase.selectFile(sw.getResult().file);
			}
		}
		phrase.sortFiles();
		ArrayList<SearchCoreAPI> lst = new ArrayList<>(apis);
		Collections.sort(lst, new Comparator<SearchCoreAPI>() {

			@Override
			public int compare(SearchCoreAPI o1, SearchCoreAPI o2) {
				return Algorithms.compare(o1.getSearchPriority(phrase),
						o2.getSearchPriority(phrase));
			}
		});
		for(SearchCoreAPI api : lst) {
			if(matcher.isCancelled()) {
				break;
			}
			if(api.getSearchPriority(phrase) == -1) {
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
	
	public boolean sameSearchResult(SearchResult r1, SearchResult r2) {
		if(r1.location != null && r2.location != null) {
			Amenity a1 = null;
			if(r1.object instanceof Amenity) {
				a1 = (Amenity) r1.object;
			}
			Amenity a2 = null;
			if(r2.object instanceof Amenity) {
				a2 = (Amenity) r2.object;
			}
			if (r1.localeName.equals(r2.localeName)) {
				double similarityRadius = 20;
				if(a1 != null && a2 != null) {
					if(a1.getType().getKeyName().equals("natural") && 
							a2.getType().getKeyName().equals("natural")) {
						similarityRadius = 10000;
					}
				}
				return MapUtils.getDistance(r1.location, r2.location) < similarityRadius;
			}
		}
		return false;
	}
	
	public void filterSearchDuplicateResults(SearchPhrase sp, List<SearchResult> searchResults) {
		Iterator<SearchResult> it = searchResults.iterator();
		SearchResult found = null;
		while(it.hasNext()) {
			SearchResult r = it.next();
			if(found != null && sameSearchResult(found, r)) {
				it.remove();
			} else {
				found = r;
			}
		}
	}
	
	
	public void sortSearchResults(SearchPhrase sp, List<SearchResult> searchResults) {
		// sort SearchResult by 1. searchDistance 2. Name
		final LatLon loc = sp.getLastTokenLocation();
		Collections.sort(searchResults, new Comparator<SearchResult>() {

			@Override
			public int compare(SearchResult o1, SearchResult o2) {
				double s1 = o1.getSearchDistance(loc);
				double s2 = o2.getSearchDistance(loc);
				int cmp = Double.compare(s1, s2);
				if(cmp != 0) {
					return cmp;
				}
				int st1 = Algorithms.extractFirstIntegerNumber(o1.localeName);
				int st2 = Algorithms.extractFirstIntegerNumber(o2.localeName);
				if(st1 != st2) {
					return Algorithms.compare(st1, st2);
				}
				return collator.compare(o1.localeName, o2.localeName);
			}
		});
	}
	
	public static class SearchResultMatcher implements  ResultMatcher<SearchResult>{
		private final List<SearchResult> requestResults = new ArrayList<>();
		private final ResultMatcher<SearchResult> matcher;
		private final int request;
		private final int totalLimit;
		private final AtomicInteger requestNumber;
		int count = 0;
		
		
		public SearchResultMatcher(ResultMatcher<SearchResult> matcher, int request, 
				AtomicInteger requestNumber, int totalLimit) {
			this.matcher = matcher;
			this.request = request;
			this.requestNumber = requestNumber;
			this.totalLimit = totalLimit;
		}
		
		public List<SearchResult> getRequestResults() {
			return requestResults;
		}
		
		public void apiSearchFinished(SearchCoreAPI api, SearchPhrase phrase) {
			if(matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_API_FINISHED;
				sr.object = api;
				matcher.publish(sr);
			}
		}
		
		public void apiSearchRegionFinished(SearchCoreAPI api, BinaryMapIndexReader region, SearchPhrase phrase) {
			if(matcher != null) {
				SearchResult sr = new SearchResult(phrase);
				sr.objectType = ObjectType.SEARCH_API_REGION_FINISHED;
				sr.object = api;
				sr.file = region;
				matcher.publish(sr);
			}
		}
		
		@Override
		public boolean publish(SearchResult object) {
			if(matcher == null || matcher.publish(object)) {
				count++;
				if(totalLimit == -1 || count < totalLimit) {
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
}
