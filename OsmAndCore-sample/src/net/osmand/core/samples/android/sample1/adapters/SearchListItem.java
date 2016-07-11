package net.osmand.core.samples.android.sample1.adapters;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.search.example.core.SearchResult;

public class SearchListItem {

	protected SampleApplication app;
	private SearchResult searchResult;
	private double distance;

	public SearchListItem(SampleApplication app, SearchResult searchResult) {
		this.app = app;
		this.searchResult = searchResult;
	}

	public SearchResult getSearchResult() {
		return searchResult;
	}

	public String getName() {
		return searchResult.localeName;
	}

	public String getTypeName() {
		return searchResult.objectType.name();
	}

	public Drawable getIcon() {
		return null;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
}
