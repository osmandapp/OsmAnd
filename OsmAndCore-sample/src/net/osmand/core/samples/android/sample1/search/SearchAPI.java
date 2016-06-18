package net.osmand.core.samples.android.sample1.search;

import android.os.AsyncTask;

import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.ISearch;
import net.osmand.core.jni.NullableAreaI;
import net.osmand.core.jni.ObfsCollection;

import java.util.ArrayList;
import java.util.List;

public class SearchAPI {

	private ObfsCollection obfsCollection;
	private AreaI searchableArea;
	private AreaI obfAreaFilter;
	private SearchRequestExecutor executor;

	public interface SearchAPICallback {
		void onSearchFinished(List<SearchItem> searchItems, boolean cancelled);
	}

	public SearchAPI(ObfsCollection obfsCollection) {
		this.obfsCollection = obfsCollection;
		executor = new SearchRequestExecutor();
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

	public void startSearch(String keyword, int maxSearchResults, SearchAPICallback apiCallback) {
		executor.run(new SearchRequest(keyword, maxSearchResults, apiCallback), true);
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

	public class SearchRequest {
		private String keyword;
		private int maxSearchResults;
		private Runnable onFinished;
		private SearchAPICallback apiCallback;

		private boolean cancelled;
		private int resCount;

		public SearchRequest(String keyword, int maxSearchResults, SearchAPICallback apiCallback) {
			this.keyword = keyword;
			this.maxSearchResults = maxSearchResults;
			this.apiCallback = apiCallback;
		}

		public void run() {

			new AsyncTask<String, Void, List<SearchItem>>() {
				@Override
				protected List<SearchItem> doInBackground(String... params) {
					return doSearch(params[0]);
				}

				@Override
				protected void onPostExecute(List<SearchItem> searchItems) {

					if (onFinished != null) {
						onFinished.run();
					}

					if (apiCallback != null) {
						apiCallback.onSearchFinished(searchItems, cancelled);
					}
				}
			}.execute(keyword);
		}

		private List<SearchItem> doSearch(String keyword) {
			System.out.println("=== Start search");
			resCount = 0;

			final List<SearchItem> searchItems = new ArrayList<>();

			AmenitiesByNameSearch byNameSearch = new AmenitiesByNameSearch(obfsCollection);
			AmenitiesByNameSearch.Criteria criteria = new AmenitiesByNameSearch.Criteria();
			criteria.setName(keyword);
			if (obfAreaFilter != null) {
				criteria.setObfInfoAreaFilter(new NullableAreaI(obfAreaFilter));
			}

			ISearch.INewResultEntryCallback newResultEntryCallback = new ISearch.INewResultEntryCallback() {
				@Override
				public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
					Amenity amenity = new ResultEntry(resultEntry).getAmenity();
					searchItems.add(new AmenitySearchItem(amenity));
					System.out.println("Poi found === " + amenity.getNativeName());
					resCount++;
				}
			};

			byNameSearch.performSearch(criteria, newResultEntryCallback.getBinding(), new IQueryController() {
				@Override
				public boolean isAborted() {
					return resCount >= maxSearchResults || cancelled;
				}
			});

			System.out.println("=== Finish search");

			return searchItems;
		}

		public void cancel() {
			cancelled = true;
		}

		public void setOnFinishedCallback(Runnable onFinished) {
			this.onFinished = onFinished;
		}
	}

	private static class ResultEntry extends AmenitiesByNameSearch.ResultEntry {
		protected ResultEntry(ISearch.IResultEntry resultEntry) {
			super(ISearch.IResultEntry.getCPtr(resultEntry), false);
		}
	}
}
