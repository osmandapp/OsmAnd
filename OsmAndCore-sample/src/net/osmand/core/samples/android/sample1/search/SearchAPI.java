package net.osmand.core.samples.android.sample1.search;

import android.support.annotation.NonNull;

import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.requests.CoreSearchRequest;
import net.osmand.core.samples.android.sample1.search.requests.IntermediateSearchRequest;
import net.osmand.core.samples.android.sample1.search.requests.SearchRequest;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

import java.util.ArrayList;
import java.util.List;

public class SearchAPI {

	private ObfsCollection obfsCollection;
	private AreaI searchableArea;
	private AreaI obfAreaFilter;
	private PointI searchLocation31;
	private double searchRadius;
	private String lang;

	private SearchRequestExecutor executor;
	private SearchString searchString;
	private List<SearchObject> searchObjects;

	private SearchCallbackInternal internalCallback = new SearchCallbackInternal() {
		@Override
		public void onSearchObjectsFound(List<SearchObject> searchObjects) {
			setSearchObjects(searchObjects);
		}

		@Override
		public void onNewTokenFound(SearchToken oldToken, SearchToken newToken) {
			searchString.replaceToken(oldToken, newToken);
		}
	};

	public interface SearchApiCallback {
		void onSearchFinished(List<SearchObject> searchObjects);
	}

	public interface SearchCallbackInternal {
		void onSearchObjectsFound(List<SearchObject> searchObjects);
		void onNewTokenFound(SearchToken oldToken, SearchToken newToken);
	}

	public SearchAPI(@NonNull ObfsCollection obfsCollection, String lang) {
		this.obfsCollection = obfsCollection;
		this.executor = new SearchRequestExecutor();
		this.searchString = new SearchString();
		this.lang = lang;
	}

	public String getLang() {
		return lang;
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

	public PointI getSearchLocation31() {
		return searchLocation31;
	}

	public void setSearchLocation31(PointI position31) {
		this.searchLocation31 = position31;
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

	public SearchString getSearchStringCopy() {
		return searchString.copy();
	}

	public List<SearchObject> getSearchObjects() {
		return searchObjects;
	}

	public void setSearchObjects(List<SearchObject> searchObjects) {
		this.searchObjects = searchObjects;
	}

	public void startSearch(String query, int maxSearchResults,
							SearchApiCallback intermediateSearchCallback,
							SearchApiCallback coreSearchCallback) {

		searchString.setQueryText(query);
		SearchScope searchScope = new SearchScope(this);
		IntermediateSearchRequest intermediateSearchRequest = null;
		if (searchObjects != null && !searchObjects.isEmpty()) {
			intermediateSearchRequest =
					new IntermediateSearchRequest(searchScope, new ArrayList<>(searchObjects),
							maxSearchResults, intermediateSearchCallback);
		}
		executor.run(new CoreSearchRequest(intermediateSearchRequest, searchScope,
				maxSearchResults, coreSearchCallback, internalCallback), true);
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
