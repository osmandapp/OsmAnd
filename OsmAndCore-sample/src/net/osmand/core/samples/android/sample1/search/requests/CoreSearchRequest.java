package net.osmand.core.samples.android.sample1.search.requests;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.AddressesByNameSearch;
import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.Amenity;
import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.ISearch;
import net.osmand.core.jni.NullableAreaI;
import net.osmand.core.samples.android.sample1.search.SearchAPI;
import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchCallback;
import net.osmand.core.samples.android.sample1.search.items.AddressSearchItem;
import net.osmand.core.samples.android.sample1.search.items.AmenitySearchItem;
import net.osmand.core.samples.android.sample1.search.items.SearchItem;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

import java.util.ArrayList;
import java.util.List;

public class CoreSearchRequest extends SearchRequest {

	private IntermediateSearchRequest intermediateSearchRequest;
	private boolean intermediateSearchDone;

	private int amenityResultsCounter;
	private int addressResultsCounter;

	public CoreSearchRequest(@Nullable IntermediateSearchRequest intermediateSearchRequest,
							 @NonNull SearchAPI searchAPI, int maxSearchResults, @Nullable SearchCallback searchCallback) {
		super(searchAPI, maxSearchResults, searchCallback);
		this.intermediateSearchRequest = intermediateSearchRequest;
	}

	@Override
	public void run() {

		if (intermediateSearchRequest != null) {
			intermediateSearchRequest.setOnFinishedCallback(new Runnable() {
				@Override
				public void run() {
					intermediateSearchDone = true;
				}
			});
			intermediateSearchRequest.run();
		}

		super.run();
	}

	@Override
	protected void onSearchRequestPostExecute(List<SearchItem> searchItems) {
		if (intermediateSearchRequest != null && !intermediateSearchDone) {
			intermediateSearchRequest.cancel();
		}
		searchAPI.setSearchItems(searchItems);
	}

	@Override
	public void cancel() {
		if (intermediateSearchRequest != null) {
			intermediateSearchRequest.cancel();
		}
		super.cancel();
	}

	@Override
	protected List<SearchItem> doSearch() {

		List<SearchItem> res = new ArrayList<>();

		SearchToken token = searchString.getNextNameFilterToken();
		while (token != null && !cancelled) {
			if (token.getType() == SearchToken.TokenType.NAME_FILTER) {
				List<SearchItem> searchItems = doCoreSearch(token);
				res.clear();
				res.addAll(searchItems);
			}
			token = searchString.getNextNameFilterToken();
		}

		return res;
	}

	private List<SearchItem> doCoreSearch(@NonNull SearchToken token) {
		System.out.println("=== Start search");
		amenityResultsCounter = 0;
		addressResultsCounter = 0;

		String keyword = token.getQueryText();
		final List<SearchItem> searchItems = new ArrayList<>();

		// Setup Amenities by name search
		AmenitiesByNameSearch amByNameSearch = new AmenitiesByNameSearch(searchAPI.getObfsCollection());
		AmenitiesByNameSearch.Criteria amenityByNameCriteria = new AmenitiesByNameSearch.Criteria();
		amenityByNameCriteria.setName(keyword);
		if (searchAPI.getObfAreaFilter() != null) {
			amenityByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(searchAPI.getObfAreaFilter()));
		}
		searchScope.setupAmenitySearchCriteria(amenityByNameCriteria);

		ISearch.INewResultEntryCallback amenityByNameResultCallback = new ISearch.INewResultEntryCallback() {
			@Override
			public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
				Amenity amenity = new AmenityResultEntry(resultEntry).getAmenity();
				AmenitySearchItem amenitySearchItem = new AmenitySearchItem(amenity);
				if (searchScope.processAmenitySearchItem(amenitySearchItem)) {
					searchItems.add(amenitySearchItem);
				}
				System.out.println("Poi found === " + amenitySearchItem.toString());
				amenityResultsCounter++;
			}
		};

		// Setup Addresses by name search
		AddressesByNameSearch addrByNameSearch = new AddressesByNameSearch(searchAPI.getObfsCollection());
		AddressesByNameSearch.Criteria addrByNameCriteria = new AddressesByNameSearch.Criteria();
		addrByNameCriteria.setName(keyword);
		if (searchAPI.getObfAreaFilter() != null) {
			addrByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(searchAPI.getObfAreaFilter()));
		}
		searchScope.setupAddressSearchCriteria(addrByNameCriteria);

		ISearch.INewResultEntryCallback addrByNameResultCallback = new ISearch.INewResultEntryCallback() {
			@Override
			public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
				Address address = new AddressResultEntry(resultEntry).getAddress();
				AddressSearchItem addrSearchItem = new AddressSearchItem(address);
				if (searchScope.processAddressSearchItem(addrSearchItem)) {
					searchItems.add(addrSearchItem);
				}
				System.out.println("Address found === " + addrSearchItem.toString());
				addressResultsCounter++;
			}
		};

		amByNameSearch.performSearch(amenityByNameCriteria, amenityByNameResultCallback.getBinding(), new IQueryController() {
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

		if (!cancelled) {
			searchScope.processSearchResult(searchItems);
		}

		System.out.println("=== Finish search");

		return searchItems;
	}

	private class AmenityResultEntry extends AmenitiesByNameSearch.ResultEntry {
		protected AmenityResultEntry(ISearch.IResultEntry resultEntry) {
			super(ISearch.IResultEntry.getCPtr(resultEntry), false);
		}
	}

	private class AddressResultEntry extends AddressesByNameSearch.ResultEntry {
		protected AddressResultEntry(ISearch.IResultEntry resultEntry) {
			super(ISearch.IResultEntry.getCPtr(resultEntry), false);
		}
	}
}