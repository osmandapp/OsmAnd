package net.osmand.search.example;

import java.util.ArrayList;
import java.util.List;

import net.osmand.ResultMatcher;
import net.osmand.search.example.core.SearchPhrase;
import net.osmand.search.example.core.SearchResult;

public class SearchUIAdapter {

	SearchPhrase p;
	List<SearchResult> currentSearchResults = new ArrayList<>(); 
	
	
	public void updateSearchPhrase(SearchPhrase p) {
		boolean hasSameConstantWords = p.hasSameConstantWords(this.p);
		this.p = p;
		if(hasSameConstantWords) {
			filterCurrentResults(currentSearchResults);
		}
		boolean lastWordLooksLikeURLOrNumber = true;
		if(lastWordLooksLikeURLOrNumber) {
			// add search result location 
		}
		
		boolean poiTypeWasNotSelected = true;
		if(poiTypeWasNotSelected) {
			// add search result poi filters
			sortCurrentSearchResults();
		}
		
		asyncCallToSearchCoreAPI(
				// final result
				new ResultMatcher<List<SearchResult<?>>>() {

					@Override
					public boolean publish(List<SearchResult<?>> object) {
						// TODO
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
			
		}, // visitor
		new ResultMatcher<SearchResult<?>>() {

			@Override
			public boolean publish(SearchResult<?> object) {
				if(filterOneResult(object)) {
					currentSearchResults.add(object);
					sortCurrentSearchResults();
					return true;
				}
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		});
	}


	private void asyncCallToSearchCoreAPI(
			ResultMatcher<List<SearchResult<?>>> publisher,
			ResultMatcher<SearchResult<?>> visitor) {
		// TODO Auto-generated method stub
		
	}


	private void filterCurrentResults(List<SearchResult> currentSearchResults2) {
		// Filter current results based on name filters from 
		this.currentSearchResults = currentSearchResults;
		// use
		// filterOneResult(null);
	}
	
	private boolean filterOneResult(SearchResult object) {
		// TODO Auto-generated method stub
		return true;
	}


	private void sortCurrentSearchResults() {
		// sort SearchResult by 1. searchDistance 2. Name 
	}


	
	
	
}
