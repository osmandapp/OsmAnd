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
import net.osmand.core.samples.android.sample1.search.SearchObjectsHelper;
import net.osmand.core.samples.android.sample1.search.objects.SearchPositionObject;
import net.osmand.core.samples.android.sample1.search.tokens.NameFilterToken;
import net.osmand.core.samples.android.sample1.search.tokens.ObjectToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

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
		NameFilterToken token = searchString.getNextNameFilterToken();
		while (token != null && !cancelled) {
			if (!token.hasEmptyQuery()) {
				res = doCoreSearch(token);
				List<SearchObject> externalObjects = searchCallback.fetchExternalObjects(token.getPlainText(), searchString.getCompleteObjects());
				if (externalObjects != null) {
					res.addAll(externalObjects);
				}
			}
			if (token != lastToken) {
				searchScope.updateScope();
				token = searchString.getNextNameFilterToken();
			} else {
				break;
			}
		}

		if (lastToken == null || lastToken.hasEmptyQuery()) {
			// 2.4 Search considered to be complete if there no NF in the end (not finished or not regonized objects)
			ObjectToken lastObjectToken = searchString.getLastObjectToken();
			if (lastObjectToken == null) {
				// Last object = [] - none. We display list of poi categories (recents separate tab)
				List<SearchObject> externalObjects = searchCallback.fetchExternalObjects("", null);
				if (externalObjects != null) {
					res = externalObjects;
				}
			} else {
				SearchObject searchObject = lastObjectToken.getSearchObject();
				switch (searchObject.getType()) {
					case POI_TYPE:
						// Last object - poi category/poi filter/poi type. Display: poi filters (if it is poi category & pois around location (if it is specified in query by any previous object) + Search more radius
						// For example: Leiden ice hockey, we display all ice hockey around Leiden
						List<SearchObject> externalObjects = searchCallback.fetchExternalObjects("", searchString.getCompleteObjects());
						if (externalObjects != null) {
							res = externalObjects;
						}
						break;
					case CITY:
						// Last object - City. Display (list of streets could be quite long)
						res = doCoreSearch(new NameFilterToken(0, ""));
						break;
					case STREET:
						// Last object - Street. Display building and intersetcting street
						res = doCoreSearch(new NameFilterToken(0, ""));
						break;
					case POSTCODE:
						// Last object - Postcode. Display building and streets
						res = doCoreSearch(new NameFilterToken(0, ""));
						break;
					case BUILDING:
						// Last object - Building - object is found
						break;
					case POI:
						// Last object - POI - object is found
						break;
				}
			}
		}

		return res;
	}

	private List<SearchObject> doCoreSearch(@NonNull SearchToken token) {
		amenityResultsCounter = 0;
		addressResultsCounter = 0;

		String keyword = token.getPlainText();
		final List<SearchObject> searchObjects = new ArrayList<>();

		AmenitiesByNameSearch amByNameSearch = null;
		AmenitiesByNameSearch.Criteria amenityByNameCriteria = null;
		ISearch.INewResultEntryCallback amenityByNameResultCallback = null;
		if (!keyword.isEmpty()) {
			// Setup Amenities by name search
			amByNameSearch = new AmenitiesByNameSearch(searchScope.getObfsCollection());
			amenityByNameCriteria = new AmenitiesByNameSearch.Criteria();
			amenityByNameCriteria.setName(keyword);
			if (searchScope.getObfAreaFilter() != null) {
				amenityByNameCriteria.setObfInfoAreaFilter(new NullableAreaI(searchScope.getObfAreaFilter()));
			}
			searchScope.setupAmenitySearchCriteria(amenityByNameCriteria);

			amenityByNameResultCallback = new ISearch.INewResultEntryCallback() {
				@Override
				public void method(ISearch.Criteria criteria, ISearch.IResultEntry resultEntry) {
					Amenity amenity = new AmenityResultEntry(resultEntry).getAmenity();
					PoiSearchObject amenitySearchItem = new PoiSearchObject(amenity);
					if (searchScope.processPoiSearchObject(amenitySearchItem)) {
						searchObjects.add(amenitySearchItem);
					}
					amenityResultsCounter++;
				}
			};
		}

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
					addressResultsCounter++;
				}
			}
		};

		if (amByNameSearch != null) {
			amByNameSearch.performSearch(amenityByNameCriteria, amenityByNameResultCallback.getBinding(), new IQueryController() {
				@Override
				public boolean isAborted() {
					return (maxSearchResults > 0 && amenityResultsCounter >= maxSearchResults) || cancelled;
				}
			});
		}

		if (!cancelled) {
			addrByNameSearch.performSearch(addrByNameCriteria, addrByNameResultCallback.getBinding(), new IQueryController() {
				@Override
				public boolean isAborted() {
					return (maxSearchResults > 0 && addressResultsCounter >= maxSearchResults) || cancelled;
				}
			});
		}

		if (!cancelled) {
			SearchToken newToken = searchScope.processSearchResult(token, searchObjects);
			if (newToken != null && internalCallback != null) {
				internalCallback.onNewTokenFound(token, newToken);
			}
		}

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