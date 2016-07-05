package net.osmand.search.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.search.example.core.SearchPhrase;
import net.osmand.search.example.core.SearchResult;

public class SearchUIAdapter {

	private SearchPhrase phrase = new SearchPhrase(null);
	private List<SearchResult> currentSearchResults = new ArrayList<>();
	
	private final BinaryMapIndexReader[] searchIndexes;

	private ThreadPoolExecutor singleThreadedExecutor;
	private LinkedBlockingQueue<Runnable> taskQueue;
	private Runnable onResultsComplete = null;
	private AtomicInteger requestNumber = new AtomicInteger();
	
	public SearchUIAdapter(BinaryMapIndexReader[] searchIndexes) {
		this.searchIndexes = searchIndexes;
		taskQueue = new LinkedBlockingQueue<Runnable>();
	    singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,taskQueue);
	}
	
	public List<SearchResult> getCurrentSearchResults() {
		return currentSearchResults;
	}
	
	public void setOnResultsComplete(Runnable onResultsComplete) {
		this.onResultsComplete = onResultsComplete;
	}
	
	public void setSearchLocation(LatLon l) {
		phrase = new SearchPhrase(l);
	}
	
	public void updateSearchPhrase(SearchPhrase p) {
		// TODO update logic
		boolean hasSameConstantWords = p.hasSameConstantWords(this.phrase);
		this.phrase = p;
		if(hasSameConstantWords) {
			filterCurrentResults(phrase);
		}
		boolean lastWordLooksLikeURLOrNumber = true;
		if(lastWordLooksLikeURLOrNumber) {
			// add search result location 
		}
		
		boolean poiTypeWasNotSelected = true;
		if(poiTypeWasNotSelected) {
			// add search result poi filters
			sortSearchResults(currentSearchResults);
		}
		
	}



	private List<SearchResult> filterCurrentResults(SearchPhrase phrase) {
		List<SearchResult> rr = new ArrayList<>();
		List<SearchResult> l = currentSearchResults;
		for(SearchResult r : l) {
			if(filterOneResult(r, phrase)) {
				rr.add(r);
			}
		}
		return rr;
	}
	
	private boolean filterOneResult(SearchResult object, SearchPhrase phrase) {
		StringMatcher nameStringMatcher = phrase.getNameStringMatcher();
		if(phrase.getLastWord().length() <= 2 && phrase.isNoSelectedType()) {
			return true;
		}
		return nameStringMatcher.matches(object.mainName);
	}

	public List<SearchResult> search(final String text) {
		List<SearchResult> list = new ArrayList<>();
		final int request = requestNumber.incrementAndGet();
		final SearchPhrase phrase = this.phrase.generateNewPhrase(text);
		this.phrase =  phrase;
		list.addAll(filterCurrentResults(phrase));
		System.out.println("> Search phrase " + phrase + " " + list.size());
		singleThreadedExecutor.getQueue().drainTo(new ArrayList<>());
		singleThreadedExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(200);
					List<SearchResult> rs = new ArrayList<>();
					for (BinaryMapIndexReader bmir : searchIndexes) {
						if (bmir.getRegionCenter() != null) {
							SearchResult sr = new SearchResult();
							sr.mainName = bmir.getRegionName();
							sr.location = bmir.getRegionCenter();
							sr.preferredZoom = 6;
							if(filterOneResult(sr, phrase)) {
								rs.add(sr);
							}
						}
					}
					boolean cancelled = request != requestNumber.get();
					if (!cancelled) {
						sortSearchResults(rs);
						System.out.println(">> Search phrase " + phrase + " " + rs.size());
						currentSearchResults = rs;
						if (onResultsComplete != null) {
							onResultsComplete.run();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});
		return list;
	}
	
	private void sortSearchResults(List<SearchResult> searchResults) {
		// sort SearchResult by 1. searchDistance 2. Name
		// TODO
	}

	
	
	
}
