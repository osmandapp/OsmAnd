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
import net.osmand.core.jni.OsmAndCoreJNI;
import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchApiCallback;
import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchCallbackInternal;
import net.osmand.core.samples.android.sample1.search.SearchScope;
import net.osmand.core.samples.android.sample1.search.SearchString;
import net.osmand.core.samples.android.sample1.search.objects.PoiSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObjectsHelper;
import net.osmand.core.samples.android.sample1.search.objects.SearchPositionObject;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class CoreSearchRequest extends SearchRequest {

	private IntermediateSearchRequest intermediateSearchRequest;
	private boolean intermediateSearchDone;
	private SearchCallbackInternal internalCallback;

	private int amenityResultsCounter;
	private int addressResultsCounter;

	public CoreSearchRequest(@Nullable IntermediateSearchRequest intermediateSearchRequest,
							 @NonNull SearchScope searchScope, int maxSearchResults,
							 @Nullable SearchApiCallback searchCallback,
							 @Nullable SearchCallbackInternal internalCallback) {
		super(searchScope, maxSearchResults, searchCallback);
		this.intermediateSearchRequest = intermediateSearchRequest;
		this.internalCallback = internalCallback;
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
	protected void onSearchRequestPostExecute(List<SearchObject> searchObjects) {
		if (intermediateSearchRequest != null && !intermediateSearchDone) {
			intermediateSearchRequest.cancel();
		}
		if (internalCallback != null) {
			internalCallback.onSearchObjectsFound(searchObjects);
		}
	}

	@Override
	public void cancel() {
		if (intermediateSearchRequest != null) {
			intermediateSearchRequest.cancel();
		}
		super.cancel();
	}

	@Override
	protected List<SearchObject> doSearch() {

		List<SearchObject> res = new ArrayList<>();

		SearchString searchString = searchScope.getSearchString();
		SearchToken lastToken = searchString.getLastToken();
		SearchToken token = searchString.getNextNameFilterToken();
		while (token != null && !cancelled) {
			if (token.getType() == SearchToken.TokenType.NAME_FILTER
					&& !Algorithms.isEmpty(token.getQueryText())) {
				res = doCoreSearch(token);
			}
			if (token != lastToken) {
				token = searchString.getNextNameFilterToken();
			} else {
				break;
			}
		}

		return res;
	}

	private List<SearchObject> doCoreSearch(@NonNull SearchToken token) {
		System.out.println("=== Start search");
		amenityResultsCounter = 0;
		addressResultsCounter = 0;

		String keyword = token.getQueryText();
		final List<SearchObject> searchObjects = new ArrayList<>();

		// Setup Amenities by name search
		AmenitiesByNameSearch amByNameSearch = new AmenitiesByNameSearch(searchScope.getObfsCollection());
		AmenitiesByNameSearch.Criteria amenityByNameCriteria = new AmenitiesByNameSearch.Criteria();
		amenityByNameCriteria.setName(keyword);
		if (searchScope.getObfAreaFilter() != null) {
			amenityByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(searchScope.getObfAreaFilter()));
		}
		searchScope.setupAmenitySearchCriteria(amenityByNameCriteria);

		ISearch.INewResultEntryCallback amenityByNameResultCallback = new ISearch.INewResultEntryCallback() {
			@Override
			public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
				Amenity amenity = new AmenityResultEntry(resultEntry).getAmenity();
				PoiSearchObject amenitySearchItem = new PoiSearchObject(amenity);
				if (searchScope.processPoiSearchObject(amenitySearchItem)) {
					searchObjects.add(amenitySearchItem);
				}
				System.out.println("Poi found === " + amenitySearchItem.toString());
				amenityResultsCounter++;
			}
		};

		// Setup Addresses by name search
		AddressesByNameSearch addrByNameSearch = new AddressesByNameSearch(searchScope.getObfsCollection());
		final AddressesByNameSearch.Criteria addrByNameCriteria = new AddressesByNameSearch.Criteria();
		addrByNameCriteria.setName(keyword);
		if (searchScope.getObfAreaFilter() != null) {
			addrByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(searchScope.getObfAreaFilter()));
		}
		searchScope.setupAddressSearchCriteria(addrByNameCriteria);

		ISearch.INewResultEntryCallback addrByNameResultCallback = new ISearch.INewResultEntryCallback() {
			@Override
			public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
				Address address = new AddressResultEntry(resultEntry).getAddress();
				SearchPositionObject addressSearchObject = SearchObjectsHelper.getAddressObject(address);
				if (addressSearchObject != null) {
					if (searchScope.processAddressSearchObject(addressSearchObject)) {
						searchObjects.add(addressSearchObject);
					}
					System.out.println("Address found === " + addressSearchObject.toString());
					addressResultsCounter++;
				}
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
			SearchToken newToken = searchScope.processSearchResult(token, searchObjects);
			if (newToken != null && internalCallback != null) {
				internalCallback.onNewTokenFound(token, newToken);
			}
		}

		System.out.println("=== Finish search");

		return searchObjects;
	}

	private class AmenityResultEntry extends AmenitiesByNameSearch.ResultEntry {
		protected AmenityResultEntry(ISearch.IResultEntry resultEntry) {
			super(OsmAndCoreJNI.AmenitiesByNameSearch_ResultEntry_SWIGUpcast(ISearch.IResultEntry.getCPtr(resultEntry)), false);
		}
	}

	private class AddressResultEntry extends AddressesByNameSearch.ResultEntry {
		protected AddressResultEntry(ISearch.IResultEntry resultEntry) {
			super(OsmAndCoreJNI.AddressesByNameSearch_ResultEntry_SWIGUpcast(ISearch.IResultEntry.getCPtr(resultEntry)), false);
		}
	}
}