package net.osmand.search.core;

import net.osmand.search.SearchUICore;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class SearchResultGroup {    
    private double minDist;
    private double maxDist;
    private final double priority;
    private final SearchPhrase phrase;
    private final ObjectType objectType;
    private final SearchResult.SearchResultResource resource;
    private final List<SearchResult> searchResults;
    private final List<Double> distances;
    private boolean mixed;
    
    public SearchResultGroup(SearchResult sr, SearchPhrase phrase) {
        searchResults = new ArrayList<>();
        objectType = sr.objectType;
        priority = sr.priority;
        this.phrase = phrase;
        resource = sr.getResourceType();
        searchResults.add(sr);
        double dist = getDistKm(sr);
        distances = new ArrayList<>();
        distances.add(dist);
        minDist = dist;
        maxDist = dist;
        mixed = false;
    }
    
    public boolean isSameGroup(SearchResult sr) {
        return sr.objectType == objectType && resource == sr.getResourceType() && sr.priority == priority;
    }
    
    public void addSearchResult(SearchResult sr) {
        searchResults.add(sr);
        double dist = getDistKm(sr);
        distances.add(dist);
        maxDist = Math.max(dist, maxDist);
        minDist = Math.min(dist, minDist);
    }

    private void addSearchResultAfterIndex(SearchResult sr, int i) {
        if (i >= searchResults.size()) {
            addSearchResult(sr);
            return;
        }
        searchResults.add(i, sr);
        double dist = getDistKm(sr);
        distances.add(i, dist);
        maxDist = Math.max(dist, maxDist);
        minDist = Math.min(dist, minDist);
    }
    
    public int getSize() {
        return searchResults.size();
    }
    
    public boolean mixAnotherGroup(SearchResultGroup another, int afterIndex) {
        if (isEmpty() || another.objectType == ObjectType.STREET_INTERSECTION) {
            return false;
        }
        List<SearchResult> searchList = another.getAndRemoveRange(minDist, 3);
        if (searchList.isEmpty() && another.isBasemap() && !another.isEmpty()) {
            searchList = another.getAndRemoveRange(Integer.MAX_VALUE, 1);            
        }
        if (!searchList.isEmpty()) {
            for (int i = searchList.size() - 1; i >= 0; i--) {
                addSearchResultAfterIndex(searchList.get(i), afterIndex);
            }
            mixed = true;
            return true;
        }
        return false;
    }
    
    private List<SearchResult> getAndRemoveRange(double maxDist, int limit) {
        List<SearchResult> results = new ArrayList<>();
        int maxInd = -1;
        for (int i = 0; i < limit && i < distances.size(); i++) {
            double dist = distances.get(i);
            if (maxDist >= dist) {
                results.add(searchResults.get(i));
                maxInd = i;
            }
        }
        if (maxInd >= 0) {
            distances.subList(0, maxInd + 1).clear();
            searchResults.subList(0, maxInd + 1).clear();
            if (distances.isEmpty()) {
                minDist = 0;
                this.maxDist = 0;
            } else {
                minDist = distances.get(0);
            }
        }
        return results;
    }
    
    public boolean isBasemap() {
        return resource == SearchResult.SearchResultResource.BASEMAP;
    }
    
    public boolean isEmpty() {
        return searchResults.isEmpty();
    }
    
    public List<SearchResult> getSearchResults() {
        return searchResults;
    }
    
    public void cutGroupResult() {
        switch (objectType) {
            case POI:
                cut(50);
                break;
            case STREET:
            case STREET_INTERSECTION:
                cut(12);
                break;
            case HOUSE:
                cut(20);
                break;
            default:
                break;
        }
    }
    
    private void cut(int cnt) {
        if (searchResults.size() > cnt) {
            searchResults.subList(cnt, searchResults.size()).clear();
        }
    }
    
    @Override
    public String toString() {
        String dist = searchResults.size() == 1 ? minDist + "km " : minDist + "-" + maxDist + "km ";
        String type = mixed ? "MIXED" : (isEmpty() ? "EMPTY" : objectType.toString());
        return type + " (" + searchResults.size() + ") " + dist + " " + resource.toString().toLowerCase().substring(0,3);
    }
    
    private double getDistKm(SearchResult sr) {
        if (sr.location == null) {
            return 0.0;
        }
        double dist = MapUtils.getDistance(sr.location, phrase.getLastTokenLocation());
        dist /= 1000;
        dist = (double) Math.round(dist * 100) / 100;
        return dist;
    }
}
