package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.AddressesByNameSearch;
import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringListHash;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.search.items.AddressSearchItem;
import net.osmand.core.samples.android.sample1.search.items.AmenitySearchItem;
import net.osmand.core.samples.android.sample1.search.items.SearchItem;
import net.osmand.core.samples.android.sample1.search.tokens.CitySearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.PoiTypeSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.PostcodeSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken.TokenType;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;

public class SearchScope {

	private SearchAPI searchAPI;
	private Map<TokenType, SearchToken> resolvedTokens;
	private double searchLat;
	private double searchLon;
	private double searchRadius;

	public SearchScope(SearchAPI searchAPI) {
		this.searchAPI = searchAPI;
	}

	public void updateScope() {
		resolvedTokens = searchAPI.getSearchString().getResolvedTokens();
		LatLon latLon = Utilities.convert31ToLatLon(searchAPI.getSearchLocation());
		searchLat = latLon.getLatitude();
		searchLon = latLon.getLongitude();
		searchRadius = searchAPI.getSearchRadius();
	}

	public void setupAmenitySearchCriteria(AmenitiesByNameSearch.Criteria criteria) {
		String categoryName = null;
		String typeName = null;
		if (resolvedTokens.containsKey(TokenType.POI_TYPE)) {
			PoiTypeSearchToken token = (PoiTypeSearchToken)resolvedTokens.get(TokenType.POI_TYPE);
			categoryName = token.getPoiType().getCategory().getKeyName();
			typeName = token.getName();
		} else if (resolvedTokens.containsKey(TokenType.POI_FILTER)) {
			SearchToken token = resolvedTokens.get(TokenType.POI_FILTER);
			categoryName = token.getName();
		} else if (resolvedTokens.containsKey(TokenType.POI_CATEGORY)) {
			SearchToken token = resolvedTokens.get(TokenType.POI_CATEGORY);
			categoryName = token.getName();
		}
		if (!Algorithms.isEmpty(categoryName) && !Algorithms.isEmpty(typeName)) {
			QStringStringListHash list = new QStringStringListHash();
			QStringList stringList = new QStringList();
			//todo list.set(categoryName, stringList);
			criteria.setCategoriesFilter(list);
		} else if (!Algorithms.isEmpty(categoryName)) {
			QStringStringListHash list = new QStringStringListHash();
			//todo list.set(categoryName, new QStringList());
			criteria.setCategoriesFilter(list);
		}
	}

	public void setupAddressSearchCriteria(AddressesByNameSearch.Criteria criteria) {
		//not implemented
	}

	public boolean processAmenitySearchItem(AmenitySearchItem item) {
		boolean res = true;
		updateDistance(item);
		if (searchRadius > 0) {
			res = item.getDistance() < searchRadius;
		}
		return res;
	}

	public boolean processAddressSearchItem(AddressSearchItem item) {
		boolean res = true;
		if (resolvedTokens.containsKey(TokenType.CITY) && item.getParentCityObfId() != null) {
			CitySearchToken token = (CitySearchToken)resolvedTokens.get(TokenType.CITY);
			res = token.getObfId().equals(item.getParentCityObfId());
		} else if (resolvedTokens.containsKey(TokenType.POSTCODE) && item.getParentPostcodeObfId() != null) {
			PostcodeSearchToken token = (PostcodeSearchToken)resolvedTokens.get(TokenType.CITY);
			res = token.getObfId().equals(item.getParentPostcodeObfId());
		}
		if (res) {
			updateDistance(item);
		}
		return res;
	}

	public void processSearchResult(List<SearchItem> searchItems) {
		/*
		Collections.sort(searchItems, new Comparator<SearchItem>() {
			@Override
			public int compare(SearchItem lhs, SearchItem rhs) {
				int res = Double.compare(lhs.getDistance(), rhs.getDistance());
				if (res == 0) {
					return lhs.getName().compareToIgnoreCase(rhs.getName());
				} else {
					return res;
				}
			}
		});
		*/

		//todo
	}

	private void updateDistance(SearchItem item) {
		item.setDistance(Utilities.distance(
				searchLon, searchLat, item.getLongitude(), item.getLatitude()));
	}
}
