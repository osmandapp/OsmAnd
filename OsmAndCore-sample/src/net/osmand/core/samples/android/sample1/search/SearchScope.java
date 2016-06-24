package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.AddressesByNameSearch;
import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.ObfAddressStreetGroupSubtype;
import net.osmand.core.jni.ObfAddressStreetGroupType;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.search.objects.PoiSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject.SearchObjectType;
import net.osmand.core.samples.android.sample1.search.objects.SearchPositionObject;
import net.osmand.core.samples.android.sample1.search.objects.StreetGroupSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.StreetSearchObject;
import net.osmand.core.samples.android.sample1.search.tokens.ObjectSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SearchScope {

	private ObfsCollection obfsCollection;
	private SearchString searchString;
	private String lang;
	private Map<SearchObjectType, SearchToken> objectTokens;
	private PointI searchLocation31;
	private AreaI searchableArea;
	private AreaI obfAreaFilter;
	private double searchRadius;

	public SearchScope(SearchAPI searchAPI) {
		obfsCollection = searchAPI.getObfsCollection();
		lang = searchAPI.getLang();
		searchString = searchAPI.getSearchStringCopy();
		objectTokens = searchString.getObjectTokens();
		searchLocation31 = searchAPI.getSearchLocation31();
		searchableArea = searchAPI.getSearchableArea();
		obfAreaFilter = searchAPI.getObfAreaFilter();
		searchRadius = searchAPI.getSearchRadius();
	}

	public ObfsCollection getObfsCollection() {
		return obfsCollection;
	}

	public String getLang() {
		return lang;
	}

	public SearchString getSearchString() {
		return searchString;
	}

	public Map<SearchObjectType, SearchToken> getObjectTokens() {
		return objectTokens;
	}

	public PointI getSearchLocation31() {
		return searchLocation31;
	}

	public AreaI getSearchableArea() {
		return searchableArea;
	}

	public AreaI getObfAreaFilter() {
		return obfAreaFilter;
	}

	public double getSearchRadius() {
		return searchRadius;
	}

	public void setupAmenitySearchCriteria(AmenitiesByNameSearch.Criteria criteria) {
		//todo criteria.setCategoriesFilter() if needed;
	}

	public void setupAddressSearchCriteria(AddressesByNameSearch.Criteria criteria) {
		//not implemented
	}

	public boolean processPoiSearchObject(PoiSearchObject poiSearchObject) {
		updateDistance(poiSearchObject);
		return true;
	}

	public boolean processAddressSearchObject(SearchPositionObject addressSearchObject) {
		updateDistance(addressSearchObject);
		return true;
	}

	public SearchToken processSearchResult(SearchToken token, List<SearchObject> searchObjects) {

		SearchToken newToken = null;

		boolean cityVillagePostcodeSelected = objectTokens.containsKey(SearchObjectType.CITY)
				|| objectTokens.containsKey(SearchObjectType.VILLAGE)
				|| objectTokens.containsKey(SearchObjectType.POSTCODE);

		for (SearchObject searchObject : searchObjects) {
			float priority = 0f;
			boolean sortByName = false;
			switch (searchObject.getType()) {
				case POI:
					priority = getPriorityByDistance(10, ((PoiSearchObject) searchObject).getDistance());
					break;
				case CITY:
				case VILLAGE:
				case POSTCODE:
					float cityType = getCityType((StreetGroupSearchObject) searchObject);
					priority = (getPriorityByDistance(cityVillagePostcodeSelected
							? 20f : 7f + cityType, ((StreetGroupSearchObject) searchObject).getDistance()));
					break;

				case STREET:
					StreetSearchObject streetSearchObject = (StreetSearchObject) searchObject;
					Street street = streetSearchObject.getStreet();
					if (!cityVillagePostcodeSelected) {
						priority = getPriorityByDistance(9f, streetSearchObject.getDistance());
					} else {
						boolean streetFromSelectedCity = false;
						for (SearchToken st : objectTokens.values()) {
							if (st.getSearchObject() instanceof StreetGroupSearchObject) {
								StreetGroupSearchObject streetGroupSearchObject = (StreetGroupSearchObject) st.getSearchObject();
								if (streetGroupSearchObject.getStreetGroup().getId().getId()
										.equals(street.getStreetGroup().getId().getId())) {
									streetFromSelectedCity = true;
									break;
								}
							}
						}
						if (streetFromSelectedCity) {
							priority = 3f;
							sortByName = true;
						} else {
							priority = getPriorityByDistance(9f, streetSearchObject.getDistance());
						}
					}
					break;
			}
			searchObject.setPriority(priority);
			searchObject.setSortByName(sortByName);
		}

		if (searchObjects.size() > 0) {
			Collections.sort(searchObjects, new Comparator<SearchObject>() {
				@Override
				public int compare(SearchObject lhs, SearchObject rhs) {
					int res = Double.compare(lhs.getPriority(), rhs.getPriority());
					if (res == 0 && lhs.isSortByName() && rhs.isSortByName()) {
						return lhs.getName(lang).compareToIgnoreCase(rhs.getName(lang));
					} else {
						return res;
					}
				}
			});

			if (token.getType() == SearchToken.TokenType.NAME_FILTER
					&& !Algorithms.isEmpty(token.getQueryText())) {
				newToken = new ObjectSearchToken(token, searchObjects.get(0));
			}
		}
		return newToken;
	}

	private float getCityType(StreetGroupSearchObject searchObject) {
		if (searchObject.getStreetGroup().getType() == ObfAddressStreetGroupType.CityOrTown) {
			if (searchObject.getStreetGroup().getSubtype() == ObfAddressStreetGroupSubtype.City) {
				return 1f;
			} else if (searchObject.getStreetGroup().getSubtype() == ObfAddressStreetGroupSubtype.Town) {
				return 1.5f;
			}
		}
		return 2.5f;
	}

	private float getPriorityByDistance(float priority, double distance) {
		return priority + (float)(1 / (1 + distance));
	}

	private void updateDistance(SearchPositionObject item) {
		item.setDistance(Utilities.distance31(searchLocation31, item.getPosition31()));
	}
}
