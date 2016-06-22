package net.osmand.core.samples.android.sample1.search;

import android.support.annotation.NonNull;

import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.samples.android.sample1.search.items.SearchItem;
import net.osmand.core.samples.android.sample1.search.requests.CoreSearchRequest;
import net.osmand.core.samples.android.sample1.search.requests.IntermediateSearchRequest;
import net.osmand.core.samples.android.sample1.search.requests.SearchRequest;

import java.util.List;

public class SearchAPI {

	private ObfsCollection obfsCollection;
	private AreaI searchableArea;
	private AreaI obfAreaFilter;
	private PointI searchLocation;
	private double searchRadius;

	private SearchRequestExecutor executor;
	private SearchString searchString;
	private SearchScope searchScope;
	private List<SearchItem> searchItems;

	public interface SearchCallback {
		void onSearchFinished(List<SearchItem> searchItems);
	}

	public SearchAPI(@NonNull ObfsCollection obfsCollection) {
		this.obfsCollection = obfsCollection;
		this.executor = new SearchRequestExecutor();
		this.searchString = new SearchString();
		this.searchScope = new SearchScope(this);
	}

	public AreaI getSearchableArea() {
		return searchableArea;
	}

	public void setSearchableArea(AreaI searchableArea) {
		this.searchableArea = searchableArea;
	}

	public AreaI getObfAreaFilter() {
		return obfAreaFilter;
	}

	public void setObfAreaFilter(AreaI obfAreaFilter) {
		this.obfAreaFilter = obfAreaFilter;
	}

	public PointI getSearchLocation() {
		return searchLocation;
	}

	public void setSearchLocation(PointI searchLocation) {
		this.searchLocation = searchLocation;
	}

	public double getSearchRadius() {
		return searchRadius;
	}

	public void setSearchRadius(double searchRadius) {
		this.searchRadius = searchRadius;
	}

	public ObfsCollection getObfsCollection() {
		return obfsCollection;
	}

	public SearchString getSearchString() {
		return searchString;
	}

	public SearchScope getSearchScope() {
		return searchScope;
	}

	public List<SearchItem> getSearchItems() {
		return searchItems;
	}

	public void setSearchItems(List<SearchItem> searchItems) {
		this.searchItems = searchItems;
	}

	public void startSearch(String query, int maxSearchResults,
							SearchCallback intermediateSearchCallback,
							SearchCallback coreSearchCallback) {

		searchString.setQueryText(query);
		searchScope.updateScope();
		IntermediateSearchRequest intermediateSearchRequest = null;
		if (searchItems != null && !searchItems.isEmpty()) {
			intermediateSearchRequest =
					new IntermediateSearchRequest(this, maxSearchResults, intermediateSearchCallback);
		}
		executor.run(new CoreSearchRequest(intermediateSearchRequest, this,
				maxSearchResults, coreSearchCallback), true);
	}

	public void cancelSearch() {
		executor.cancel();
	}


	public class SearchRequestExecutor {

		private SearchRequest ongoingSearchRequest;
		private SearchRequest nextSearchRequest;

		public void run(SearchRequest searchRequest, boolean cancelCurrentRequest) {
			if (ongoingSearchRequest != null) {
				nextSearchRequest = searchRequest;
				if (cancelCurrentRequest) {
					ongoingSearchRequest.cancel();
				}
			} else {
				ongoingSearchRequest = searchRequest;
				nextSearchRequest = null;
				searchRequest.setOnFinishedCallback(new Runnable() {
					@Override
					public void run() {
						operationFinished();
					}
				});
				searchRequest.run();
			}
		}

		public void cancel() {
			if (nextSearchRequest != null) {
				nextSearchRequest = null;
			}
			if (ongoingSearchRequest != null) {
				ongoingSearchRequest.cancel();
			}
		}

		private void operationFinished() {
			ongoingSearchRequest = null;
			if (nextSearchRequest != null) {
				run(nextSearchRequest, false);
			}
		}
	}
}
