package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.jni.AddressesByNameSearch;
import net.osmand.core.jni.AmenitiesByNameSearch;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.ObfAddressStreetGroupSubtype;
import net.osmand.core.jni.ObfAddressStreetGroupType;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.search.objects.PoiSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.PostcodeSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject.SearchObjectType;
import net.osmand.core.samples.android.sample1.search.objects.SearchPositionObject;
import net.osmand.core.samples.android.sample1.search.objects.StreetGroupSearchObject;
import net.osmand.core.samples.android.sample1.search.objects.StreetSearchObject;
import net.osmand.core.samples.android.sample1.search.tokens.ObjectSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
	boolean citySelected;

	private int resultLimitPoiByName = 25;
	private int poiByNameCounter = 0;
	private int resultLimitCityByName = 25;
	private int cityByNameCounter = 0;
	private int resultLimitStreetByName = 50;
	private int streetByNameCounter = 0;

	public SearchScope(SearchAPI searchAPI) {
		obfsCollection = searchAPI.getObfsCollection();
		lang = searchAPI.getLang();
		searchString = searchAPI.getSearchStringCopy();
		searchLocation31 = searchAPI.getSearchLocation31();
		searchableArea = searchAPI.getSearchableArea();
		obfAreaFilter = searchAPI.getObfAreaFilter();
		searchRadius = searchAPI.getSearchRadius();
		updateScope();
	}

	public void updateScope() {
		objectTokens = searchString.getCompleteObjectTokens();
		citySelected = objectTokens.containsKey(SearchObjectType.CITY)
				|| objectTokens.containsKey(SearchObjectType.VILLAGE)
				|| objectTokens.containsKey(SearchObjectType.POSTCODE)
				|| objectTokens.containsKey(SearchObjectType.STREET);
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
		if (objectTokens.containsKey(SearchObjectType.STREET)) {
			StreetSearchObject streetSearchObject =
					(StreetSearchObject) objectTokens.get(SearchObjectType.STREET).getSearchObject();
			criteria.setAddressFilter(streetSearchObject.getStreet());
			if (objectTokens.containsKey(SearchObjectType.POSTCODE)) {
				PostcodeSearchObject postcodeSearchObject =
						(PostcodeSearchObject) objectTokens.get(SearchObjectType.POSTCODE).getSearchObject();
				criteria.setPostcode(postcodeSearchObject.getNativeName());
			}
		} else if (objectTokens.containsKey(SearchObjectType.CITY)) {
			criteria.setAddressFilter(((StreetGroupSearchObject) objectTokens.get(SearchObjectType.CITY)
					.getSearchObject()).getStreetGroup());
		} else if (objectTokens.containsKey(SearchObjectType.VILLAGE)) {
			criteria.setAddressFilter(((StreetGroupSearchObject) objectTokens.get(SearchObjectType.VILLAGE)
					.getSearchObject()).getStreetGroup());
		} else if (objectTokens.containsKey(SearchObjectType.POSTCODE)) {
			criteria.setAddressFilter(((StreetGroupSearchObject) objectTokens.get(SearchObjectType.POSTCODE)
					.getSearchObject()).getStreetGroup());
		}
	}

	public boolean processPoiSearchObject(PoiSearchObject poiSearchObject) {
		updateDistance(poiSearchObject);
		return processSearchObject(poiSearchObject);
	}

	public boolean processAddressSearchObject(SearchPositionObject addressSearchObject) {
		updateDistance(addressSearchObject);
		return processSearchObject(addressSearchObject);
	}

	public SearchToken processSearchResult(SearchToken token, List<SearchObject> searchObjects) {

		SearchToken newToken = null;
		if (searchObjects.size() > 0) {
			Collections.sort(searchObjects, new Comparator<SearchObject>() {
				@Override
				public int compare(SearchObject lhs, SearchObject rhs) {
					int res = Double.compare(lhs.getPriority(), rhs.getPriority());
					if (res == 0) {
						return lhs.getName(lang).compareToIgnoreCase(rhs.getName(lang));
					} else {
						return res;
					}
				}
			});

			Iterator<SearchObject> it = searchObjects.iterator();
			while (it.hasNext()) {
				SearchObject searchObject = it.next();
				boolean accept;
				switch (searchObject.getType()) {
					case POI:
						accept = poiByNameCounter < resultLimitPoiByName;
						if (accept) {
							poiByNameCounter++;
						}
						break;
					case CITY:
					case VILLAGE:
					case POSTCODE:
						accept = cityByNameCounter < resultLimitCityByName;
						if (accept) {
							cityByNameCounter++;
						}
						break;

					case STREET:
						accept = streetByNameCounter < resultLimitStreetByName;
						if (accept) {
							streetByNameCounter++;
						}
					default:
						accept = true;
						break;
				}
				if (!accept) {
					it.remove();
				}
			}

			if (token.getType() == SearchToken.TokenType.NAME_FILTER
					&& !token.hasEmptyQuery()) {
				boolean suggeston = token == searchString.getLastToken();
				newToken = new ObjectSearchToken(token, searchObjects.get(0), suggeston);
				searchString.replaceToken(token, newToken);
			}
		}
		return newToken;
	}

	private boolean processSearchObject(SearchObject searchObject) {
		double priority = 0.0;
		switch (searchObject.getType()) {
			case BUILDING:
				priority = 3.0;
				break;
			case POI:
				priority = getPriorityByDistance(10.0, ((PoiSearchObject) searchObject).getDistance());
				break;
			case CITY:
			case VILLAGE:
			case POSTCODE:
				double cityType = getCityType((StreetGroupSearchObject) searchObject);
				priority = (getPriorityByDistance(citySelected
						? 20.0 : 7.0 + cityType, ((StreetGroupSearchObject) searchObject).getDistance()));
				break;

			case STREET:
				StreetSearchObject streetSearchObject = (StreetSearchObject) searchObject;
				Street street = streetSearchObject.getStreet();
				if (!citySelected) {
					priority = getPriorityByDistance(9.0, streetSearchObject.getDistance());
				} else {
					boolean streetFromSelectedCity = false;
					for (SearchToken st : objectTokens.values()) {
						if (st.getSearchObject() instanceof StreetGroupSearchObject) {
							StreetGroup streetGroup =
									((StreetGroupSearchObject) st.getSearchObject()).getStreetGroup();
							if (streetGroup.getId().getId().equals(street.getStreetGroup().getId().getId())) {
								streetFromSelectedCity = true;
								break;
							}
						} else if (st.getSearchObject() instanceof StreetSearchObject) {
							StreetGroup streetGroup =
									((StreetSearchObject) st.getSearchObject()).getStreet().getStreetGroup();
							if (streetGroup.getId().getId().equals(street.getStreetGroup().getId().getId())) {
								streetFromSelectedCity = true;
								break;
							}
						}
					}
					if (streetFromSelectedCity) {
						priority = 3.0;
					} else {
						priority = getPriorityByDistance(9.0, streetSearchObject.getDistance());
					}
				}
				break;
		}
		searchObject.setPriority(priority);

		return true;
	}

	private double getCityType(StreetGroupSearchObject searchObject) {
		if (searchObject.getStreetGroup().getType() == ObfAddressStreetGroupType.CityOrTown) {
			if (searchObject.getStreetGroup().getSubtype() == ObfAddressStreetGroupSubtype.City) {
				return 1.0;
			} else if (searchObject.getStreetGroup().getSubtype() == ObfAddressStreetGroupSubtype.Town) {
				return 1.5;
			}
		}
		return 2.5;
	}

	private double getPriorityByDistance(double priority, double distance) {
		return priority + 1.0 - (1.0 / (1.0 + distance));
	}

	private void updateDistance(SearchPositionObject item) {
		item.setDistance(Utilities.distance31(searchLocation31, item.getPosition31()));
	}
}
