package net.osmand.core.samples.android.sample1.search;

import android.os.AsyncTask;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.AddressesByNameSearch;
import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.ISearch;
import net.osmand.core.jni.ISearch.INewResultEntryCallback;
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
		private int amenityResultsCounter;
		private int addressResultsCounter;

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
			amenityResultsCounter = 0;
			addressResultsCounter = 0;

			final List<SearchItem> searchItems = new ArrayList<>();

			// Setup Amenities by name search
			AmenitiesByNameSearch amByNameSearch = new AmenitiesByNameSearch(obfsCollection);
			AmenitiesByNameSearch.Criteria amByNameCriteria = new AmenitiesByNameSearch.Criteria();
			amByNameCriteria.setName(keyword);
			if (obfAreaFilter != null) {
				amByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(obfAreaFilter));
			}
			INewResultEntryCallback amByNameResultCallback = new ISearch.INewResultEntryCallback() {
				@Override
				public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
					Amenity amenity = new AmenityResultEntry(resultEntry).getAmenity();
					AmenitySearchItem amenitySearchItem = new AmenitySearchItem(amenity);
					searchItems.add(amenitySearchItem);
					System.out.println("Poi found === " + amenitySearchItem.toString());
					amenityResultsCounter++;
				}
			};

			// Setup Addresses by name search
			AddressesByNameSearch addrByNameSearch = new AddressesByNameSearch(obfsCollection);
			AddressesByNameSearch.Criteria addrByNameCriteria = new AddressesByNameSearch.Criteria();
			addrByNameCriteria.setName(keyword);
			if (obfAreaFilter != null) {
				addrByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(obfAreaFilter));
			}
			INewResultEntryCallback addrByNameResultCallback = new ISearch.INewResultEntryCallback() {
				@Override
				public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
					Address address = new AddressResultEntry(resultEntry).getAddress();
					AddressSearchItem addrSearchItem = new AddressSearchItem(address);
					searchItems.add(addrSearchItem);
					System.out.println("Address found === " + addrSearchItem.toString());
					addressResultsCounter++;
				}
			};

			amByNameSearch.performSearch(amByNameCriteria, amByNameResultCallback.getBinding(), new IQueryController() {
				@Override
				public boolean isAborted() {
					return amenityResultsCounter >= maxSearchResults || cancelled;
				}
			});

			if (!cancelled) {
				addrByNameSearch.performSearch(addrByNameCriteria, addrByNameResultCallback.getBinding(), new IQueryController() {
					@Override
					public boolean isAborted() {
						return addressResultsCounter >= maxSearchResults || cancelled;
					}
				});
			}

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

	private static class AmenityResultEntry extends AmenitiesByNameSearch.ResultEntry {
		protected AmenityResultEntry(ISearch.IResultEntry resultEntry) {
			super(ISearch.IResultEntry.getCPtr(resultEntry), false);
		}
	}

	private static class AddressResultEntry extends AddressesByNameSearch.ResultEntry {
		protected AddressResultEntry(ISearch.IResultEntry resultEntry) {
			super(ISearch.IResultEntry.getCPtr(resultEntry), false);
		}
	}
}
