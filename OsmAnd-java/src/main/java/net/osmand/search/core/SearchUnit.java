package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;

import java.util.ArrayList;
import java.util.List;

public class SearchUnit {

    private List<BinaryMapIndexReader> offlineIndexes;
    private List<SearchResult> searchResults;

    public boolean sorted;

    public int priority;

    public SearchUnit(int priority) {
        sorted = false;
        this.priority = priority;
        searchResults = new ArrayList<>();
        offlineIndexes = new ArrayList<>();
    }

    public int getSearchPriority() {
        return priority;
    }

    public void addSearchResults(List<SearchResult> searchResults) {
        this.searchResults.addAll(searchResults);
        sorted = false;
    }

    public void addSearchResult(SearchResult searchResult) {
        searchResults.add(searchResult);
        sorted = false;
    }

    public void addOfflineIndex(BinaryMapIndexReader reader) {
        if (!offlineIndexes.contains(reader)) {
            offlineIndexes.add(reader);
        }
    }

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }
}