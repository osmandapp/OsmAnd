package net.osmand.search.core;

import com.google.openlocationcode.OpenLocationCode;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.CommonWords;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPointParserUtil;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;
import net.osmand.util.LocationParser;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TLongHashSet;


public class SearchCoreFactory {

	public static final int MAX_DEFAULT_SEARCH_RADIUS = 7;

	//////////////// CONSTANTS //////////
	public static final int SEARCH_REGION_API_PRIORITY = 300;
	public static final int SEARCH_REGION_OBJECT_PRIORITY = 1000;

	// context less
	public static final int SEARCH_LOCATION_PRIORITY = 0;
	public static final int SEARCH_AMENITY_TYPE_PRIORITY = 100;
	public static final int SEARCH_AMENITY_TYPE_API_PRIORITY = 100;

	// context
	public static final int SEARCH_STREET_BY_CITY_PRIORITY = 200;
	public static final int SEARCH_BUILDING_BY_CITY_PRIORITY = 300;
	public static final int SEARCH_BUILDING_BY_STREET_PRIORITY = 100;
	public static final int SEARCH_AMENITY_BY_TYPE_PRIORITY = 300;

	// context less (slow)
	public static final int SEARCH_ADDRESS_BY_NAME_API_PRIORITY = 500;
	public static final int SEARCH_ADDRESS_BY_NAME_API_PRIORITY_RADIUS2 = 500;
	public static final int SEARCH_ADDRESS_BY_NAME_PRIORITY = 500;
	public static final int SEARCH_ADDRESS_BY_NAME_PRIORITY_RADIUS2 = 500;

	// context less (slower)
	public static final int SEARCH_AMENITY_BY_NAME_PRIORITY = 700;
	public static final int SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_POI_TYPE = 700;
	public static final int SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_3_CHAR = 700;
	protected static final double SEARCH_AMENITY_BY_NAME_CITY_PRIORITY_DISTANCE = 0.001;
	protected static final double SEARCH_AMENITY_BY_NAME_TOWN_PRIORITY_DISTANCE = 0.005;

	public static abstract class SearchBaseAPI implements SearchCoreAPI {

		private ObjectType[] searchTypes;

		protected SearchBaseAPI(ObjectType... searchTypes) {
			if (searchTypes == null) {
				throw new IllegalArgumentException("Search types are not defined for search core API");
			}
			this.searchTypes = searchTypes;
		}

		@Override
		public boolean isSearchAvailable(SearchPhrase p) {
			ObjectType[] typesToSearch = p.getSearchTypes();
			ObjectType exclusiveSearchType = p.getExclusiveSearchType();
			if (exclusiveSearchType != null) {
				return searchTypes != null && searchTypes.length == 1 && searchTypes[0] == exclusiveSearchType;
			} else if (typesToSearch == null) {
				return true;
			} else {
				for (ObjectType type : searchTypes) {
					for (ObjectType ts : typesToSearch) {
						if (type == ts) {
							return true;
						}
					}
				}
				return false;
			}
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return phrase.getRadiusLevel() < MAX_DEFAULT_SEARCH_RADIUS;
		}

		protected void subSearchApiOrPublish(SearchPhrase phrase,
											 SearchResultMatcher resultMatcher, SearchResult res, SearchBaseAPI api)
				throws IOException {
			phrase.countUnknownWordsMatch(res);
			int cnt = resultMatcher.getCount();
			List<String> ws = phrase.getUnknownSearchWords(res.otherWordsMatch);
			if(!res.firstUnknownWordMatches) {
				ws.add(phrase.getUnknownSearchWord());
			}
			// publish result to set parentSearchResult before search
			resultMatcher.publish(res);
			if (!ws.isEmpty() && api != null && api.isSearchAvailable(phrase)) {
				SearchPhrase nphrase = phrase.selectWord(res, ws,
						phrase.isLastUnknownSearchWordComplete());
				SearchResult prev = resultMatcher.setParentSearchResult(res);
				res.parentSearchResult = prev;
				api.search(nphrase, resultMatcher);
				resultMatcher.setParentSearchResult(prev);
			}
//			if (resultMatcher.getCount() == cnt) {
//				resultMatcher.publish(res);
//			}
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}



	public static class SearchRegionByNameAPI extends SearchBaseAPI {

		public SearchRegionByNameAPI() {
			super(ObjectType.REGION);
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			for (BinaryMapIndexReader bmir : phrase.getOfflineIndexes()) {
				if (bmir.getRegionCenter() != null) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = bmir.getRegionName();
					sr.object = bmir;
					sr.file = bmir;
					sr.priority = SEARCH_REGION_OBJECT_PRIORITY;
					sr.objectType = ObjectType.REGION;
					sr.location = bmir.getRegionCenter();
					sr.preferredZoom = 6;
					if (phrase.getUnknownSearchWordLength() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
						resultMatcher.publish(sr);
					}
				}
			}
			return true;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(!p.isNoSelectedType()) {
				return -1;
			}
			return SEARCH_REGION_API_PRIORITY;
		}
	}

	private static String stripBraces(String localeName) {
		int i = localeName.indexOf('(');
		String retName = localeName;
		if (i > -1) {
			retName = localeName.substring(0, i);
			int j = localeName.indexOf(')', i);
			if (j > -1) {
				retName = retName.trim() + ' ' + localeName.substring(j);
			}
		}
		return retName;
	}



	public static class SearchAddressByNameAPI extends SearchBaseAPI {

		private static final int DEFAULT_ADDRESS_BBOX_RADIUS = 100 * 1000;
		private static final int LIMIT = 10000;

		private Map<BinaryMapIndexReader, List<City>> townCities = new LinkedHashMap<>();
		private QuadTree<City> townCitiesQR = new QuadTree<City>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
				8, 0.55f);
		private List<City> resArray = new ArrayList<>();
		private SearchStreetByCityAPI cityApi;
		private SearchBuildingAndIntersectionsByStreetAPI streetsApi;

		public SearchAddressByNameAPI(SearchBuildingAndIntersectionsByStreetAPI streetsApi,
									  SearchStreetByCityAPI cityApi) {
			super(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE,
					ObjectType.STREET, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION);
			this.streetsApi = streetsApi;
			this.cityApi = cityApi;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (!p.isNoSelectedType() && p.getRadiusLevel() == 1) {
				return -1;
			}
			if(p.isLastWord(ObjectType.POI) || p.isLastWord(ObjectType.POI_TYPE)) {
				return -1;
			}
			if (p.isNoSelectedType()) {
				return SEARCH_ADDRESS_BY_NAME_API_PRIORITY;
			}
			return SEARCH_ADDRESS_BY_NAME_API_PRIORITY_RADIUS2;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			// case when street is not found for given city is covered by SearchStreetByCityAPI
			return getSearchPriority(phrase) != -1 && super.isSearchMoreAvailable(phrase);
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if (!phrase.isUnknownSearchWordPresent() && !phrase.isEmptyQueryAllowed()) {
				return false;
			}
			// phrase.isLastWord(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE) || phrase.isLastWord(ObjectType.REGION)
			if (phrase.isNoSelectedType() || phrase.getRadiusLevel() >= 2) {
				initAndSearchCities(phrase, resultMatcher);
				// not publish results (let it sort)
				// resultMatcher.apiSearchFinished(this, phrase);
				searchByName(phrase, resultMatcher);
			}
			return true;
		}

		private void initAndSearchCities(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			QuadRect bbox = phrase.getRadiusBBoxToSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 20);
			Iterator<BinaryMapIndexReader> offlineIndexes = phrase.getOfflineIndexes(bbox, SearchPhraseDataType.ADDRESS);
			while (offlineIndexes.hasNext()) {
				BinaryMapIndexReader r = offlineIndexes.next();
				if (!townCities.containsKey(r)) {
					BinaryMapIndexReader.buildAddressRequest(null);
					List<City> l = r.getCities(null, BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
					townCities.put(r, l);
					for (City c  : l) {
						LatLon cl = c.getLocation();
						c.setReferenceFile(r);
						int y = MapUtils.get31TileNumberY(cl.getLatitude());
						int x = MapUtils.get31TileNumberX(cl.getLongitude());
						QuadRect qr = new QuadRect(x, y, x, y);
						townCitiesQR.insert(c, qr);
					}
				}
			}
			if (phrase.isNoSelectedType() && bbox != null
					&& (phrase.isUnknownSearchWordPresent() || phrase.isEmptyQueryAllowed())
					&& phrase.isSearchTypeAllowed(ObjectType.CITY)) {
				String wrd = phrase.getUnknownWordToSearch();
				NameStringMatcher nm = phrase.getNameStringMatcher(wrd, phrase.isUnknownSearchWordComplete());
				// NameStringMatcher nm = phrase.getNameStringMatcher();
				resArray.clear();
				resArray = townCitiesQR.queryInBox(bbox, resArray);
				int limit = 0;
				for (City c : resArray) {
					SearchResult res = new SearchResult(phrase);
					res.object = c;
					res.file = (BinaryMapIndexReader) c.getReferenceFile();
					res.localeName = c.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = c.getAllNames(true);
					res.localeRelatedObjectName = res.file.getRegionName();
					res.relatedObject = res.file;
					res.location = c.getLocation();
					res.priority = SEARCH_ADDRESS_BY_NAME_PRIORITY;
					res.priorityDistance = 0.1;
					res.objectType = ObjectType.CITY;
					if (phrase.isEmptyQueryAllowed() && phrase.isEmpty()) {
						resultMatcher.publish(res);
					} else if (nm.matches(res.localeName) || nm.matches(res.otherNames)) {
						res.firstUnknownWordMatches = wrd.equals(phrase.getUnknownSearchWord());
						subSearchApiOrPublish(phrase, resultMatcher, res, cityApi);
					}
					if (limit++ > LIMIT * phrase.getRadiusLevel()) {
						break;
					}
				}
			}
		}


		private void searchByName(final SearchPhrase phrase, final SearchResultMatcher resultMatcher)
				throws IOException {
			if (phrase.getRadiusLevel() > 1 || phrase.getUnknownSearchWordLength() > 3 || phrase.getUnknownSearchWords().size() > 0) {
				final boolean locSpecified = phrase.getLastTokenLocation() != null;
				LatLon loc = phrase.getLastTokenLocation();
				final List<SearchResult> immediateResults = new ArrayList<>();
				final QuadRect streetBbox = phrase.getRadiusBBoxToSearch(DEFAULT_ADDRESS_BBOX_RADIUS);
				final QuadRect postcodeBbox = phrase.getRadiusBBoxToSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 5);
				final QuadRect villagesBbox = phrase.getRadiusBBoxToSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 3);
				final QuadRect cityBbox = phrase.getRadiusBBoxToSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 5); // covered by separate search before
				final int priority = phrase.isNoSelectedType() ?
						SEARCH_ADDRESS_BY_NAME_PRIORITY : SEARCH_ADDRESS_BY_NAME_PRIORITY_RADIUS2;
				final BinaryMapIndexReader[] currentFile = new BinaryMapIndexReader[1];
				
				
				ResultMatcher<MapObject> rm = new ResultMatcher<MapObject>() {
					int limit = 0;
					@Override
					public boolean publish(MapObject object) {
						if (isCancelled()) {
							return false;
						}
						SearchResult sr = new SearchResult(phrase);
						sr.object = object;
						sr.file = currentFile[0];
						sr.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						sr.otherNames = object.getAllNames(true);
						sr.localeRelatedObjectName = sr.file.getRegionName();
						sr.relatedObject = sr.file;
						sr.location = object.getLocation();
						sr.priorityDistance = 1;
						sr.priority = priority;
						int y = MapUtils.get31TileNumberY(object.getLocation().getLatitude());
						int x = MapUtils.get31TileNumberX(object.getLocation().getLongitude());
						List<City> closestCities = null;
						if (object instanceof Street) {
							if ((locSpecified && !streetBbox.contains(x, y, x, y))
									|| !phrase.isSearchTypeAllowed(ObjectType.STREET)) {
								return false;
							}
							if (object.getName().startsWith("<")) {
								return false;
							}
							if (!phrase.getNameStringMatcher().matches(stripBraces(sr.localeName))) {
								sr.priorityDistance = 5;
							}
							sr.objectType = ObjectType.STREET;
							sr.localeRelatedObjectName = ((Street)object).getCity().getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
							sr.relatedObject = ((Street)object).getCity();
						} else if (object instanceof City) {
							CityType type = ((City)object).getType();
							if (type == CityType.CITY || type == CityType.TOWN) {
								if (phrase.isNoSelectedType()) {
									// ignore city/town
									return false;
								}
								if ((locSpecified && !cityBbox.contains(x, y, x, y))
										|| !phrase.isSearchTypeAllowed(ObjectType.CITY)) {
									return false;
								}

								sr.objectType = ObjectType.CITY;
								sr.priorityDistance = 0.1;
							} else if (((City)object).isPostcode()) {
								if ((locSpecified && !postcodeBbox.contains(x, y, x, y))
										|| !phrase.isSearchTypeAllowed(ObjectType.POSTCODE)) {
									return false;
								}
								sr.objectType = ObjectType.POSTCODE;
							}  else {
								if ((locSpecified && !villagesBbox.contains(x, y, x, y))
										|| !phrase.isSearchTypeAllowed(ObjectType.VILLAGE)) {
									return false;
								}
								City c = null;
								if (closestCities == null) {
									closestCities = townCitiesQR.queryInBox(villagesBbox, new ArrayList<City>());
								}
								double minDist = -1;
								double pDist = -1;
								for (City s : closestCities) {
									double ll = MapUtils.getDistance(s.getLocation(), object.getLocation());
									double pd = s.getType() == CityType.CITY ? ll : ll * 10;
									if(minDist == -1 || pd < pDist) {
										c = s;
										minDist = ll;
										pDist = pd ;
									}
								}
								if (c != null) {
									sr.localeRelatedObjectName = c.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
									sr.relatedObject = c;
									sr.distRelatedObjectName = minDist;
								}
								sr.objectType = ObjectType.VILLAGE;
							}
						} else {
							return false;
						}
						limit ++;
						immediateResults.add(sr);
						return false;
					}



					@Override
					public boolean isCancelled() {
						return limit > LIMIT * phrase.getRadiusLevel() ||
								resultMatcher.isCancelled();
					}
				};
				Iterator<BinaryMapIndexReader> offlineIterator = phrase.getRadiusOfflineIndexes(DEFAULT_ADDRESS_BBOX_RADIUS * 5,
						SearchPhraseDataType.ADDRESS);
				
				String wordToSearch = phrase.getUnknownWordToSearch();
				while (offlineIterator.hasNext() && wordToSearch.length() > 0) {
					BinaryMapIndexReader r = offlineIterator.next();
					currentFile[0] = r;
					immediateResults.clear();
					SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(rm, wordToSearch.toLowerCase(),
							phrase.isUnknownSearchWordComplete() ? StringMatcherMode.CHECK_EQUALS_FROM_SPACE
									: StringMatcherMode.CHECK_STARTS_FROM_SPACE);
					if (locSpecified) {
						req.setBBoxRadius(loc.getLatitude(), loc.getLongitude(),
								phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 5));
					}
					r.searchAddressDataByName(req);
					for (SearchResult res : immediateResults) {
						res.firstUnknownWordMatches = wordToSearch.equals(phrase.getUnknownSearchWord());
						if (res.objectType == ObjectType.STREET) {
							City ct = ((Street) res.object).getCity();
							phrase.countUnknownWordsMatch(res, 
									ct.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate()),
									ct.getAllNames(true));
							subSearchApiOrPublish(phrase, resultMatcher, res, streetsApi);
						} else {
							subSearchApiOrPublish(phrase, resultMatcher, res, cityApi);
						}
					}
					resultMatcher.apiSearchRegionFinished(this, r, phrase);
				}
			}
		}
	}



	public static class SearchAmenityByNameAPI extends SearchBaseAPI {
		private static final int LIMIT = 10000;
		private static final int BBOX_RADIUS = 500 * 1000;
		private static final int BBOX_RADIUS_INSIDE = 10000 * 1000; // to support city search for basemap

		public SearchAmenityByNameAPI() {
			super(ObjectType.POI);
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if(!phrase.isUnknownSearchWordPresent()) {
				return false;
			}
			final BinaryMapIndexReader[] currentFile = new BinaryMapIndexReader[1];
			Iterator<BinaryMapIndexReader> offlineIterator = phrase.getRadiusOfflineIndexes(BBOX_RADIUS,
					SearchPhraseDataType.POI);
			final NameStringMatcher nm = phrase.getNameStringMatcher();
			QuadRect bbox = phrase.getRadiusBBoxToSearch(BBOX_RADIUS_INSIDE);
			final Set<String> ids = new HashSet<String>();
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
					(int)bbox.centerX(), (int)bbox.centerY(),
					phrase.getUnknownSearchWord(),
					(int)bbox.left, (int)bbox.right,
					(int)bbox.top, (int)bbox.bottom,
					new ResultMatcher<Amenity>() {
						int limit = 0;
						@Override
						public boolean publish(Amenity object) {
							if (limit ++ > LIMIT) {
								return false;
							}
							String poiID = object.getType().getKeyName() + "_" + object.getId();
							if (ids.contains(poiID)) {
								return false;
							}
							SearchResult sr = new SearchResult(phrase);
							sr.otherNames = object.getAllNames(true);
							sr.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
							if (phrase.isUnknownSearchWordComplete()) {
								if(!nm.matches(sr.localeName) && !nm.matches(sr.otherNames)) {
									return false;
								}
							}
							sr.object = object;
							sr.preferredZoom = 17;
							sr.file = currentFile[0];
							sr.location = object.getLocation();
							if (object.getSubType().equals("city") ||
									object.getSubType().equals("country")) {
								sr.priorityDistance = SEARCH_AMENITY_BY_NAME_CITY_PRIORITY_DISTANCE;
								sr.preferredZoom = object.getSubType().equals("country") ? 7 : 13;
							} else if (object.getSubType().equals("town")) {
								sr.priorityDistance = SEARCH_AMENITY_BY_NAME_TOWN_PRIORITY_DISTANCE;
							} else {
								sr.priorityDistance = 1;
							}
							sr.priority = SEARCH_AMENITY_BY_NAME_PRIORITY;
							phrase.countUnknownWordsMatch(sr);
							sr.objectType = ObjectType.POI;
							resultMatcher.publish(sr);
							ids.add(poiID);
							return false;
						}

						@Override
						public boolean isCancelled() {
							return resultMatcher.isCancelled() && (limit < LIMIT) ;
						}
					});

			while (offlineIterator.hasNext()) {
				BinaryMapIndexReader r = offlineIterator.next();
				currentFile[0] = r;
				r.searchPoiByName(req);

				resultMatcher.apiSearchRegionFinished(this, r, phrase);
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.hasObjectType(ObjectType.POI) ||
					!p.isUnknownSearchWordPresent()) {
				return -1;
			}
			if (p.hasObjectType(ObjectType.POI_TYPE)) {
				return -1;
			}
			if (p.getUnknownSearchWordLength() >= 3 || p.getRadiusLevel() > 1) {
				return SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_3_CHAR;
			}
			return -1;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return super.isSearchMoreAvailable(phrase) && getSearchPriority(phrase) != -1;
		}
	}



	public static class SearchAmenityTypesAPI extends SearchBaseAPI {

		private Map<String, PoiType> translatedNames = new LinkedHashMap<>();
		private List<PoiFilter> topVisibleFilters;
		private List<PoiCategory> categories;
		private List<CustomSearchPoiFilter> customPoiFilters = new ArrayList<>();
		private TIntArrayList customPoiFiltersPriorites = new TIntArrayList();
		private MapPoiTypes types;

		public SearchAmenityTypesAPI(MapPoiTypes types) {
			super(ObjectType.POI_TYPE);
			this.types = types;
		}

		public void clearCustomFilters() {
			this.customPoiFilters.clear();
			this.customPoiFiltersPriorites.clear();
		}

		public void addCustomFilter(CustomSearchPoiFilter poiFilter, int priority) {
			this.customPoiFilters.add(poiFilter);
			this.customPoiFiltersPriorites.add(priority);
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			if (translatedNames.isEmpty()) {
				translatedNames = types.getAllTranslatedNames(false);
				topVisibleFilters = types.getTopVisibleFilters();
				categories = types.getCategories(false);
			}
//			results.clear();
			List<AbstractPoiType> results = new ArrayList<AbstractPoiType>() ;
			NameStringMatcher nm = phrase.getNameStringMatcher();
			for (PoiFilter pf : topVisibleFilters) {
				if (!phrase.isUnknownSearchWordPresent() || nm.matches(pf.getTranslation())) {
					results.add(pf);
				}
			}
			if (phrase.isUnknownSearchWordPresent()) {
				for (PoiCategory c : categories) {
					if (!results.contains(c) && nm.matches(c.getTranslation())) {
						results.add(c);
					}
				}
				Iterator<Entry<String, PoiType>> it = translatedNames.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, PoiType> e = it.next();
					PoiType pt = e.getValue();
					if (pt.getCategory() != types.getOtherMapCategory()) {
						if (!results.contains(pt) && (nm.matches(e.getKey()) || nm.matches(pt.getTranslation()))) {
							results.add(pt);
						}
						List<PoiType> additionals = pt.getPoiAdditionals();
						if (additionals != null) {
							for (PoiType a : additionals) {
								if (!a.isReference() && !results.contains(a) && (nm.matches(a.getKeyName().replace('_', ' ')) || nm.matches(a.getTranslation()))) {
									results.add(a);
								}
							}
						}
					}
				}
			}
			for (AbstractPoiType pt : results) {
				SearchResult res = new SearchResult(phrase);
				res.localeName = pt.getTranslation();
				res.object = pt;
				res.priority = SEARCH_AMENITY_TYPE_PRIORITY;
				res.priorityDistance = 0;
				res.objectType = ObjectType.POI_TYPE;
				resultMatcher.publish(res);
			}
			for (int i = 0; i < customPoiFilters.size(); i++) {
				CustomSearchPoiFilter csf = customPoiFilters.get(i);
				int p = customPoiFiltersPriorites.get(i);
				if (!phrase.isUnknownSearchWordPresent() || nm.matches(csf.getName())) {
					SearchResult res = new SearchResult(phrase);
					res.localeName = csf.getName();
					res.object = csf;
					res.priority = SEARCH_AMENITY_TYPE_PRIORITY + p;
					res.objectType = ObjectType.POI_TYPE;
					resultMatcher.publish(res);
				}
			}

			return true;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.hasObjectType(ObjectType.POI) || p.hasObjectType(ObjectType.POI_TYPE)) {
				return -1;
			}
			if (!p.isNoSelectedType() && !p.isUnknownSearchWordPresent()) {
				return -1;
			}
			return SEARCH_AMENITY_TYPE_API_PRIORITY;
		}
	}



	public static class SearchAmenityByTypeAPI extends SearchBaseAPI {

		private MapPoiTypes types;

		public SearchAmenityByTypeAPI(MapPoiTypes types) {
			super(ObjectType.POI);
			this.types = types;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return getSearchPriority(phrase) != -1 && super.isSearchMoreAvailable(phrase);
		}

		private Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<PoiCategory,
				LinkedHashSet<String>>();
		private Map<String, PoiType> poiAdditionals = new HashMap<String, PoiType>();
		public void updateTypesToAccept(AbstractPoiType pt) {
			pt.putTypes(acceptedTypes);
			if (pt instanceof PoiType && ((PoiType) pt).isAdditional() && ((PoiType) pt).getParentType() != null) {
				fillPoiAdditionals(((PoiType) pt).getParentType());
			} else {
				fillPoiAdditionals(pt);
			}
		}

		private void fillPoiAdditionals(AbstractPoiType pt) {
			for (PoiType add : pt.getPoiAdditionals()) {
				poiAdditionals.put(add.getKeyName().replace('_', ':').replace(' ', ':'), add);
				poiAdditionals.put(add.getTranslation().replace(' ', ':').toLowerCase(), add);
			}
			if (pt instanceof PoiFilter && !(pt instanceof PoiCategory)) {
				for (PoiType ps : ((PoiFilter) pt).getPoiTypes()) {
					fillPoiAdditionals(ps);
				}
			}
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if (phrase.isLastWord(ObjectType.POI_TYPE)) {
				Object obj = phrase.getLastSelectedWord().getResult().object;
				SearchPoiTypeFilter ptf;
				if (obj instanceof AbstractPoiType) {
					ptf = getPoiTypeFilter((AbstractPoiType) obj);
				} else if (obj instanceof SearchPoiTypeFilter) {
					ptf = (SearchPoiTypeFilter) obj;
				} else {
					throw new UnsupportedOperationException();
				}

				QuadRect bbox = phrase.getRadiusBBoxToSearch(10000);
				List<BinaryMapIndexReader> oo = phrase.getOfflineIndexes();
				Set<String> searchedPois = new TreeSet<>();
				for (BinaryMapIndexReader o : oo) {
					ResultMatcher<Amenity> rm = getResultMatcher(phrase, resultMatcher, o, searchedPois);
					if (obj instanceof CustomSearchPoiFilter) {
						rm = ((CustomSearchPoiFilter) obj).wrapResultMatcher(rm);
					}
					SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
							(int)bbox.left, (int)bbox.right,
							(int)bbox.top, (int)bbox.bottom, -1, ptf,
							rm);
					o.searchPoi(req);
					resultMatcher.apiSearchRegionFinished(this, o, phrase);
				}
			}
			return true;
		}

		private ResultMatcher<Amenity> getResultMatcher(final SearchPhrase phrase, final SearchResultMatcher resultMatcher,
														final BinaryMapIndexReader selected, final Set<String> searchedPois) {
			final NameStringMatcher ns = phrase.getNameStringMatcher();
			return new ResultMatcher<Amenity>() {

				@Override
				public boolean publish(Amenity object) {
					SearchResult res = new SearchResult(phrase);
					String poiID = object.getType().getKeyName() + "_" + object.getId();
					if(!searchedPois.add(poiID)) {
						return false;
					}
					if(object.isClosed()) {
						return false;
					}
					res.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = object.getAllNames(true);
					if (Algorithms.isEmpty(res.localeName)) {
						AbstractPoiType st = types.getAnyPoiTypeByKey(object.getSubType());
						if (st != null) {
							res.localeName = st.getTranslation();
						} else {
							res.localeName = object.getSubType();
						}
					}
					if (phrase.isUnknownSearchWordPresent()
							&& !(ns.matches(res.localeName) || ns.matches(res.otherNames))) {
						return false;
					}

					res.object = object;
					res.preferredZoom = 17;
					res.file = selected;
					res.location = object.getLocation();
					res.priority = SEARCH_AMENITY_BY_TYPE_PRIORITY;
					res.priorityDistance = 1;
					res.objectType = ObjectType.POI;
					resultMatcher.publish(res);
					return false;
				}

				@Override
				public boolean isCancelled() {
					return resultMatcher.isCancelled();
				}
			};
		}

		private SearchPoiTypeFilter getPoiTypeFilter(AbstractPoiType pt) {

			acceptedTypes.clear();
			poiAdditionals.clear();
			updateTypesToAccept(pt);
			return new SearchPoiTypeFilter() {

				@Override
				public boolean isEmpty() {
					return false;
				}

				@Override
				public boolean accept(PoiCategory type, String subtype) {
					if (type == null) {
						return true;
					}
					if (!types.isRegisteredType(type)) {
						type = types.getOtherPoiCategory();
					}
					if (!acceptedTypes.containsKey(type)) {
						return false;
					}
					LinkedHashSet<String> set = acceptedTypes.get(type);
					if (set == null) {
						return true;
					}
					return set.contains(subtype);
				}
			};
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.isLastWord(ObjectType.POI_TYPE) &&
					p.getLastTokenLocation() != null) {
				return SEARCH_AMENITY_BY_TYPE_PRIORITY;
			}
			return -1;
		}

	}



	public static class SearchStreetByCityAPI extends SearchBaseAPI {

		private SearchBaseAPI streetsAPI;
		public SearchStreetByCityAPI(SearchBuildingAndIntersectionsByStreetAPI streetsAPI) {
			super(ObjectType.HOUSE, ObjectType.STREET, ObjectType.STREET_INTERSECTION);
			this.streetsAPI = streetsAPI;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			// case when street is not found for given city is covered here
			return phrase.getRadiusLevel() == 1 && getSearchPriority(phrase) != -1;
		}

		private static int LIMIT = 10000;
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			SearchWord sw = phrase.getLastSelectedWord();
			if (isLastWordCityGroup(phrase) && sw.getResult() != null && sw.getResult().file != null) {
				City c = (City) sw.getResult().object;
				if (c.getStreets().isEmpty()) {
					sw.getResult().file.preloadStreets(c, null);
				}
				int limit = 0;
				String wordToSearch = phrase.getUnknownWordToSearch();
				boolean firstUnknownWordMatches = wordToSearch.equals(phrase.getUnknownSearchWord());
				NameStringMatcher nm = phrase.getNameStringMatcher(wordToSearch, phrase.isUnknownSearchWordComplete());
				for (Street object : c.getStreets()) {

					SearchResult res = new SearchResult(phrase);
					
					res.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = object.getAllNames(true);
					if (object.getName().startsWith("<")) {
						// streets related to city
						continue;
					}
					if (phrase.isUnknownSearchWordPresent()
							&& !(nm.matches(res.localeName) || nm.matches(res.otherNames))) {
						continue;
					}
					res.firstUnknownWordMatches = firstUnknownWordMatches ||
							phrase.getNameStringMatcher().matches(res.localeName) || 
							phrase.getNameStringMatcher().matches(res.otherNames);
					res.localeRelatedObjectName = c.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.object = object;
					res.preferredZoom = 17;
					res.file = sw.getResult().file;
					res.location = object.getLocation();
					res.priority = SEARCH_STREET_BY_CITY_PRIORITY;
					//res.priorityDistance = 1;
					res.objectType = ObjectType.STREET;
					subSearchApiOrPublish(phrase, resultMatcher, res, streetsAPI);
					if (limit++ > LIMIT) {
						break;
					}

				}
				return true;
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (isLastWordCityGroup(p)) {
				return SEARCH_STREET_BY_CITY_PRIORITY;
			}
			return -1;
		}

	}

	public static boolean isLastWordCityGroup(SearchPhrase p ) {
		return p.isLastWord(ObjectType.CITY) || p.isLastWord(ObjectType.POSTCODE) ||
				p.isLastWord(ObjectType.VILLAGE);
	}



	public static class SearchBuildingAndIntersectionsByStreetAPI extends SearchBaseAPI {
		Street cacheBuilding;

		public SearchBuildingAndIntersectionsByStreetAPI() {
			super(ObjectType.HOUSE, ObjectType.STREET_INTERSECTION);
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		@Override
		public boolean search(SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			Street s = null;
			int priority = SEARCH_BUILDING_BY_STREET_PRIORITY;
			if (phrase.isLastWord(ObjectType.STREET)) {
				s =  (Street) phrase.getLastSelectedWord().getResult().object;
			}
			if (isLastWordCityGroup(phrase)) {
				priority = SEARCH_BUILDING_BY_CITY_PRIORITY;
				Object o = phrase.getLastSelectedWord().getResult().object;
				if (o instanceof City) {
					List<Street> streets = ((City) o).getStreets();
					if (streets.size() == 1) {
						s = streets.get(0);
					} else {
						for (Street st : streets) {
							if (st.getName().equals(((City) o).getName()) ||
									st.getName().equals("<"+((City) o).getName()+">")) {
								s = st;
								break;
							}
						}
					}
				}
			}

			if (s != null) {
				BinaryMapIndexReader file = phrase.getLastSelectedWord().getResult().file;
				
				if (cacheBuilding != s) {
					cacheBuilding = s;
					SearchRequest<Building> sr = BinaryMapIndexReader
							.buildAddressRequest(new ResultMatcher<Building>() {

								@Override
								public boolean publish(Building object) {
									return true;
								}

								@Override
								public boolean isCancelled() {
									return resultMatcher.isCancelled();
								}
							});

					file.preloadBuildings(s, sr);
					Collections.sort(s.getBuildings(), new Comparator<Building>() {

						@Override
						public int compare(Building o1, Building o2) {
							int i1 = Algorithms.extractFirstIntegerNumber(o1.getName());
							int i2 = Algorithms.extractFirstIntegerNumber(o2.getName());
							if (i1 == i2) {
								return 0;
							}
							return Algorithms.compare(i1, i2);
						}
					});
				}
				String lw = phrase.getUnknownWordToSearchBuilding();
				NameStringMatcher buildingMatch = phrase.getNameStringMatcher(lw, phrase.isLastUnknownSearchWordComplete());
				for (Building b : s.getBuildings()) {
					SearchResult res = new SearchResult(phrase);
					boolean interpolation = b.belongsToInterpolation(lw);
					if ((!buildingMatch.matches(b.getName()) && !interpolation)
							|| !phrase.isSearchTypeAllowed(ObjectType.HOUSE)) {
						continue;
					}

					res.localeName = b.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = b.getAllNames(true);
					res.object = b;
					res.file = file;
					res.priority = priority;
					res.priorityDistance = 0;
					res.relatedObject = s;
					res.localeRelatedObjectName = s.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.objectType = ObjectType.HOUSE;
					if(interpolation) {
						res.location = b.getLocation(b.interpolation(lw));
					} else {
						res.location = b.getLocation();
					}
					res.preferredZoom = 17;
					resultMatcher.publish(res);
				}
				String streetIntersection = phrase.getUnknownWordToSearch();
				NameStringMatcher streetMatch = phrase.getNameStringMatcher(streetIntersection, phrase.isLastUnknownSearchWordComplete());
				if (Algorithms.isEmpty(streetIntersection) || 
						(!Character.isDigit(streetIntersection.charAt(0)) && 
								CommonWords.getCommonSearch(streetIntersection) == -1) ) {
					for (Street street : s.getIntersectedStreets()) {
						SearchResult res = new SearchResult(phrase);
						if ((!streetMatch.matches(street.getName()) && !streetMatch.matches(street.getAllNames(true)))
								|| !phrase.isSearchTypeAllowed(ObjectType.STREET_INTERSECTION)) {
							continue;
						}
						res.otherNames = street.getAllNames(true);
						res.localeName = street.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						res.object = street;
						res.file = file;
						res.relatedObject = s;
						res.priority = priority + 1;
						res.localeRelatedObjectName = s.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						res.priorityDistance = 0;
						res.objectType = ObjectType.STREET_INTERSECTION;
						res.location = street.getLocation();
						res.preferredZoom = 16;
						resultMatcher.publish(res);
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (isLastWordCityGroup(p)) {
				return SEARCH_BUILDING_BY_CITY_PRIORITY;
			}
			if (!p.isLastWord(ObjectType.STREET)) {
				return -1;
			}
			return SEARCH_BUILDING_BY_STREET_PRIORITY;
		}
	}



	public static class SearchLocationAndUrlAPI extends SearchBaseAPI {

		public SearchLocationAndUrlAPI() {
			super(ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION);
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
		}

		//		newFormat = PointDescription.FORMAT_DEGREES;
//		newFormat = PointDescription.FORMAT_MINUTES;
//		newFormat = PointDescription.FORMAT_SECONDS;
		public void testUTM() {
			double northing = 0;
			double easting = 0;
			String zone = "";
			char c = zone.charAt(zone.length() -1);
			int z = Integer.parseInt(zone.substring(0, zone.length() - 1));
			UTMPoint upoint = new UTMPoint(northing, easting, z, c);
			LatLonPoint ll = upoint.toLatLonPoint();
			LatLon loc = new LatLon(ll.getLatitude(), ll.getLongitude());
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			if (!phrase.isUnknownSearchWordPresent()) {
				return false;
			}
			boolean parseUrl = parseUrl(phrase, resultMatcher);
			if (!parseUrl) {
				parseLocation(phrase, resultMatcher);
			}
			return super.search(phrase, resultMatcher);
		}

		private boolean isKindOfNumber(String s) {
			for (int i = 0; i < s.length(); i ++) {
				char c = s.charAt(i);
				if (c >= '0' && c <= '9') {
				} else if (c == ':' || c == '.' || c == '#' || c == ',' || c == '-' || c == '\'' || c == '"') {
				} else {
					return false;
				}
			}
			return true;
		}

		LatLon parsePartialLocation(String s) {
			s = s.trim();
			if (s.length() == 0 || !(s.charAt(0) == '-' || Character.isDigit(s.charAt(0))
					|| s.charAt(0) == 'S' || s.charAt(0) == 's'
					|| s.charAt(0) == 'N' || s.charAt(0) == 'n'
					|| s.contains("://"))) {
				return null;
			}
			List<Double> d = new ArrayList<>();
			List<Object> all = new ArrayList<>();
			List<String> strings = new ArrayList<>();
			LocationParser.splitObjects(s, d, all, strings);
			if (d.size() == 0) {
				return null;
			}
			double lat = LocationParser.parse1Coordinate(all, 0, all.size());
			return new LatLon(lat, 0);
		}

		private void parseLocation(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			String lw = phrase.getUnknownSearchPhrase();
			LatLon l = LocationParser.parseLocation(lw);
			if (l != null) {
				if (phrase.isSearchTypeAllowed(ObjectType.LOCATION)) {
					SearchResult sp = new SearchResult(phrase);
					sp.priority = SEARCH_LOCATION_PRIORITY;
					sp.object = sp.location = l;
					sp.localeName = ((float) sp.location.getLatitude()) + ", " + ((float) sp.location.getLongitude());
					sp.objectType = ObjectType.LOCATION;
					sp.wordsSpan = lw;
					resultMatcher.publish(sp);
				}
			} else if (phrase.isNoSelectedType()) {
				LatLon ll = parsePartialLocation(lw);
				if (ll != null && phrase.isSearchTypeAllowed(ObjectType.PARTIAL_LOCATION)) {
					SearchResult sp = new SearchResult(phrase);
					sp.priority = SEARCH_LOCATION_PRIORITY;

					sp.object = sp.location = ll;
					sp.localeName = ((float) sp.location.getLatitude()) + ", <input> ";
					sp.objectType = ObjectType.PARTIAL_LOCATION;
					resultMatcher.publish(sp);
				}
			}
		}

		private boolean parseUrl(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			String text = phrase.getUnknownSearchPhrase();
			GeoParsedPoint pnt = GeoPointParserUtil.parse(text);
			if (pnt != null && pnt.isGeoPoint() && phrase.isSearchTypeAllowed(ObjectType.LOCATION)) {
				SearchResult sp = new SearchResult(phrase);
				sp.priority = 0;
				sp.object = pnt;
				sp.wordsSpan = text;
				sp.location = new LatLon(pnt.getLatitude(), pnt.getLongitude());
				sp.localeName = ((float)pnt.getLatitude()) +", " + ((float) pnt.getLongitude());
				if (pnt.getZoom() > 0) {
					sp.preferredZoom = pnt.getZoom();
				}
				sp.objectType = ObjectType.LOCATION;
				resultMatcher.publish(sp);
				return true;
			}
			return false;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			return SEARCH_LOCATION_PRIORITY;
		}
	}
	
	
}
