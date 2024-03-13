package net.osmand.search.core;


import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS;
import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
import static net.osmand.osm.MapPoiTypes.OSM_WIKI_CATEGORY;
import static net.osmand.osm.MapPoiTypes.WIKI_PLACE;
import static net.osmand.search.core.ObjectType.POI;
import static net.osmand.util.LocationParser.parseOpenLocationCode;

import net.osmand.CollatorStringMatcher;
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
import net.osmand.osm.PoiType;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPointParserUtil;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.LocationParser;
import net.osmand.util.LocationParser.ParsedOpenLocationCode;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;


public class SearchCoreFactory {

	public static final int PREFERRED_STREET_ZOOM = 17;
	public static final int PREFERRED_INDEX_ITEM_ZOOM = 17;
	public static final int PREFERRED_BUILDING_ZOOM = 16;
	public static final int PREFERRED_COUNTRY_ZOOM = 7;
	public static final int PREFERRED_CITY_ZOOM = 13;
	public static final int PREFERRED_POI_ZOOM = 16;
	public static final int PREFERRED_WPT_ZOOM = 16;
	public static final int PREFERRED_GPX_FILE_ZOOM = 17;
	public static final int PREFERRED_DEFAULT_RECENT_ZOOM = 17;
	public static final int PREFERRED_FAVORITES_GROUP_ZOOM = 17;
	public static final int PREFERRED_FAVORITE_ZOOM = 16;
	public static final int PREFERRED_STREET_INTERSECTION_ZOOM = 16;
	public static final int PREFERRED_REGION_ZOOM = 6;
	public static final int PREFERRED_DEFAULT_ZOOM = 15;
	public static boolean DISPLAY_DEFAULT_POI_TYPES = false;
	public static final int MAX_DEFAULT_SEARCH_RADIUS = 7;
	public static final int SEARCH_MAX_PRIORITY = Integer.MAX_VALUE;

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
	
	public static final int SEARCH_OLC_WITH_CITY_PRIORITY = 8;
	public static final int SEARCH_OLC_WITH_CITY_TOTAL_LIMIT = 500;

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

		@Override
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return 0;
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return 0;
		}

		protected SearchPhrase subSearchApiOrPublish(SearchPhrase phrase, SearchResultMatcher resultMatcher, SearchResult res, SearchBaseAPI api)
				throws IOException {
			return subSearchApiOrPublish(phrase, resultMatcher, res, api, true);
		}

		protected SearchPhrase subSearchApiOrPublish(SearchPhrase phrase, SearchResultMatcher resultMatcher, SearchResult res, SearchBaseAPI api,
											 boolean publish)
				throws IOException {
			phrase.countUnknownWordsMatchMainResult(res);
			boolean firstUnknownWordMatches = res.firstUnknownWordMatches;
			List<String> leftUnknownSearchWords = new ArrayList<String>(phrase.getUnknownSearchWords());
			if (res.otherWordsMatch != null) {
				leftUnknownSearchWords.removeAll(res.otherWordsMatch);
			}
			SearchResult newParentSearchResult = null;
			if (res.parentSearchResult == null && resultMatcher.getParentSearchResult() == null &&
					res.objectType == ObjectType.STREET && res.object instanceof Street && ((Street) res.object).getCity() != null) {
				City ct = ((Street) res.object).getCity();
				SearchResult cityResult = new SearchResult(phrase);
				cityResult.object = ct;
				cityResult.objectType = ObjectType.CITY;
				cityResult.localeName = ct.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
				cityResult.otherNames = ct.getOtherNames(true);
				cityResult.location = ct.getLocation();
				cityResult.localeRelatedObjectName = res.file.getRegionName();
				cityResult.file = res.file;
				phrase.countUnknownWordsMatchMainResult(cityResult);
				boolean match = false;
				if (firstUnknownWordMatches) {
					cityResult.firstUnknownWordMatches = false; // don't count same name twice
				} else if (cityResult.firstUnknownWordMatches) {
					firstUnknownWordMatches = true;
					match = true;
				}
				if (cityResult.otherWordsMatch != null) {
					Iterator<String> iterator = cityResult.otherWordsMatch.iterator();
					while (iterator.hasNext()) {
						String n = iterator.next();
						boolean wasPresent = leftUnknownSearchWords.remove(n);
						if (!wasPresent) {
							iterator.remove(); // don't count same name twice
						} else {
							match = true;
						}
					}
				}
				// include parent search result even if it is empty
				if (match) {
					newParentSearchResult = cityResult;
				}
			}
			if (!firstUnknownWordMatches) {
				leftUnknownSearchWords.add(0, phrase.getFirstUnknownSearchWord());
			}
			// publish result to set parentSearchResult before search
			if (publish) {
				if (newParentSearchResult != null) {
					SearchResult prev = resultMatcher.setParentSearchResult(newParentSearchResult);
					resultMatcher.publish(res);
					resultMatcher.setParentSearchResult(prev);
				} else {
					resultMatcher.publish(res);
				}
			}
			if (!leftUnknownSearchWords.isEmpty() && api != null && api.isSearchAvailable(phrase)) {
				SearchPhrase nphrase = phrase.selectWord(res, leftUnknownSearchWords,
						phrase.isLastUnknownSearchWordComplete() ||
								!leftUnknownSearchWords.contains(phrase.getLastUnknownSearchWord()));
				SearchResult prev = resultMatcher.setParentSearchResult(publish ? res :
						resultMatcher.getParentSearchResult());
				api.search(nphrase, resultMatcher);
				
				resultMatcher.setParentSearchResult(prev);
				return nphrase;
			}
			return null;
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
					sr.preferredZoom = PREFERRED_REGION_ZOOM;
					if (phrase.getFullSearchPhrase().length() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getFirstUnknownNameStringMatcher().matches(sr.localeName)) {
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
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS);
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return phrase.getNextRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS);
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
				NameStringMatcher nm = phrase.getMainUnknownNameStringMatcher();
				resArray.clear();
				resArray = townCitiesQR.queryInBox(bbox, resArray);
				int limit = 0;
				for (City c : resArray) {
					if (phrase.getSettings().isExportObjects()) {
						resultMatcher.exportCity(phrase, c);
					}
					SearchResult res = new SearchResult(phrase);
					res.object = c;
					res.file = (BinaryMapIndexReader) c.getReferenceFile();
					res.localeName = c.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = c.getOtherNames(true);
					res.localeRelatedObjectName = res.file.getRegionName();
					res.relatedObject = res.file;
					res.location = c.getLocation();
					res.priority = SEARCH_ADDRESS_BY_NAME_PRIORITY;
					res.priorityDistance = 0.1;
					res.objectType = ObjectType.CITY;
					if (phrase.isEmptyQueryAllowed() && phrase.isEmpty()) {
						resultMatcher.publish(res);
					} else if (nm.matches(res.localeName) || nm.matches(res.otherNames)) {
						SearchPhrase nphrase = subSearchApiOrPublish(phrase, resultMatcher, res, cityApi);
						searchPoiInCity(nphrase, res, resultMatcher);
					}
					if (limit++ > LIMIT * phrase.getRadiusLevel()) {
						break;
					}
				}
			}
		}
		
		private void searchPoiInCity(SearchPhrase nphrase, SearchResult res, SearchResultMatcher resultMatcher) throws IOException {
			if (nphrase != null && res.objectType == ObjectType.CITY) {
				SearchAmenityByNameAPI poiApi = new SearchCoreFactory.SearchAmenityByNameAPI();
				SearchPhrase newPhrase = nphrase.generateNewPhrase(nphrase, res.file);
				newPhrase.getSettings().setOriginalLocation(res.location);
				poiApi.search(newPhrase, resultMatcher);
			}
		}

		private void searchByName(final SearchPhrase phrase, final SearchResultMatcher resultMatcher)
				throws IOException {
			if (phrase.getRadiusLevel() > 1 || phrase.getUnknownWordToSearch().length() > 3 ||
					phrase.hasMoreThanOneUnknownSearchWord()|| phrase.isSearchTypeAllowed(ObjectType.POSTCODE, true)) {
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
						sr.otherNames = object.getOtherNames(true);
						sr.localeRelatedObjectName = sr.file.getRegionName();
						sr.relatedObject = sr.file;
						sr.location = object.getLocation();
						sr.priorityDistance = 1;
						sr.priority = priority;
						int y = MapUtils.get31TileNumberY(object.getLocation().getLatitude());
						int x = MapUtils.get31TileNumberX(object.getLocation().getLongitude());
						List<City> closestCities = null;
						if (object instanceof Street) {
							// remove limitation by location
							if (  //(locSpecified && !streetBbox.contains(x, y, x, y)) || 
								!phrase.isSearchTypeAllowed(ObjectType.STREET)) {
								return false;
							}
							if (object.getName().startsWith("<")) {
								return false;
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
								sr.priorityDistance = 0;
							} else {
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

				ResultMatcher<MapObject> rawDataCollector = null;
				if (phrase.getSettings().isExportObjects()) {
					rawDataCollector = new ResultMatcher<MapObject>() {
						@Override
						public boolean publish(MapObject object) {
							resultMatcher.exportObject(phrase, object);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					};
				}

				Iterator<BinaryMapIndexReader> offlineIterator = phrase.getRadiusOfflineIndexes(DEFAULT_ADDRESS_BBOX_RADIUS * 5,
						SearchPhraseDataType.ADDRESS);
				String wordToSearch = phrase.getUnknownWordToSearch();
				while (offlineIterator.hasNext() && wordToSearch.length() > 0) {
					BinaryMapIndexReader r = offlineIterator.next();
					currentFile[0] = r;
					immediateResults.clear();
					SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(rm, rawDataCollector, wordToSearch.toLowerCase(),
							phrase.isMainUnknownSearchWordComplete() ? StringMatcherMode.CHECK_EQUALS_FROM_SPACE
									: StringMatcherMode.CHECK_STARTS_FROM_SPACE);
					if (locSpecified) {
						req.setBBoxRadius(loc.getLatitude(), loc.getLongitude(),
								phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS * 5));
					}
					r.searchAddressDataByName(req);
					for (SearchResult res : immediateResults) {
						if (res.objectType == ObjectType.STREET) {
							subSearchApiOrPublish(phrase, resultMatcher, res, streetsApi);
						} else {
							SearchPhrase nphrase = subSearchApiOrPublish(phrase, resultMatcher, res, cityApi);
							searchPoiInCity(nphrase, res, resultMatcher);
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
		private static final int BBOX_RADIUS_INSIDE = 20000 * 1000; // to support city search for basemap
		private static final int BBOX_RADIUS_POI_IN_CITY = 25 * 1000;
		private static final int FIRST_WORD_MIN_LENGTH = 3;

		public SearchAmenityByNameAPI() {
			super(ObjectType.POI);
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if (!phrase.isUnknownSearchWordPresent()) {
				return false;
			}
			if (!phrase.isNoSelectedType()) {
				// don't search by name when type is selected or poi type is part of name
				return false;
			}
			// Take into account POI [bar] - 'Hospital 512'
			// BEFORE: it was searching exact match of whole phrase.getUnknownSearchPhrase() [ Check feedback ] 

			final BinaryMapIndexReader[] currentFile = new BinaryMapIndexReader[1];
			Iterator<BinaryMapIndexReader> offlineIterator = phrase.getRadiusOfflineIndexes(BBOX_RADIUS,
					SearchPhraseDataType.POI);
			String searchWord = phrase.getUnknownWordToSearch();
			final NameStringMatcher nm = phrase.getMainUnknownNameStringMatcher();
			QuadRect bbox = phrase.getFileRequest() != null ? phrase.getRadiusBBoxToSearch(BBOX_RADIUS_POI_IN_CITY) : phrase.getRadiusBBoxToSearch(BBOX_RADIUS_INSIDE);
			final Set<String> ids = new HashSet<String>();

			ResultMatcher<Amenity> rawDataCollector = null;
			if (phrase.getSettings().isExportObjects()) {
				rawDataCollector = new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						resultMatcher.exportObject(phrase, object);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				};
			}
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest((int) bbox.centerX(),
					(int) bbox.centerY(), searchWord, (int) bbox.left, (int) bbox.right, (int) bbox.top,
					(int) bbox.bottom, new ResultMatcher<Amenity>() {
						int limit = 0;

						@Override
						public boolean publish(Amenity object) {
							if (phrase.getSettings().isExportObjects()) {
								resultMatcher.exportObject(phrase, object);
							}
							if (limit++ > LIMIT) {
								return false;
							}
							String poiID = object.getType().getKeyName() + "_" + object.getId();
							if (ids.contains(poiID)) {
								return false;
							}
							SearchResult sr = new SearchResult(phrase);
							sr.otherNames = object.getOtherNames(true);
							sr.localeName = object.getName(phrase.getSettings().getLang());
							if (!nm.matches(sr.localeName)) {
								sr.localeName = object.getName(phrase.getSettings().getLang(),
										phrase.getSettings().isTransliterate());
							}
							if (!nm.matches(sr.localeName) && !nm.matches(sr.otherNames)
									&& !nm.matches(object.getAdditionalInfoValues(false))) {
								return false;
							}
							sr.object = object;
							sr.preferredZoom = SearchCoreFactory.PREFERRED_POI_ZOOM;
							sr.file = currentFile[0];
							sr.location = object.getLocation();
							if (object.getSubType().equals("city") || object.getSubType().equals("country")) {
								sr.priorityDistance = SEARCH_AMENITY_BY_NAME_CITY_PRIORITY_DISTANCE;
								sr.preferredZoom = object.getSubType().equals("country") ? PREFERRED_COUNTRY_ZOOM : PREFERRED_CITY_ZOOM;
							} else if (object.getSubType().equals("town")) {
								sr.priorityDistance = SEARCH_AMENITY_BY_NAME_TOWN_PRIORITY_DISTANCE;
							} else {
								sr.priorityDistance = 1;
							}
							sr.priority = SEARCH_AMENITY_BY_NAME_PRIORITY;
							phrase.countUnknownWordsMatchMainResult(sr);
							sr.objectType = ObjectType.POI;
							resultMatcher.publish(sr);
							ids.add(poiID);
							return false;
						}

						@Override
						public boolean isCancelled() {
							return resultMatcher.isCancelled() && (limit < LIMIT);
						}
					}, rawDataCollector);
			
			BinaryMapIndexReader fileRequest = phrase.getFileRequest();
			if (fileRequest != null) {
				fileRequest.searchPoiByName(req);
				resultMatcher.apiSearchRegionFinished(this, fileRequest, phrase);
			} else {
				while (offlineIterator.hasNext()) {
					BinaryMapIndexReader r = offlineIterator.next();
					currentFile[0] = r;
					r.searchPoiByName(req);
					
					resultMatcher.apiSearchRegionFinished(this, r, phrase);
				}
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
			if (p.getUnknownWordToSearch().length() >= FIRST_WORD_MIN_LENGTH || p.isFirstUnknownSearchWordComplete()) {
				return SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_3_CHAR;
			}
			return -1;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return super.isSearchMoreAvailable(phrase) && getSearchPriority(phrase) != -1;
		}

		@Override
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return phrase.getRadiusSearch(BBOX_RADIUS);
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return phrase.getNextRadiusSearch(BBOX_RADIUS);
		}
	}

	protected static class PoiTypeResult {
		public AbstractPoiType pt;
		public Set<String> foundWords = new LinkedHashSet<String>();
	}

	public static class SearchAmenityTypesAPI extends SearchBaseAPI {

		public final static String STD_POI_FILTER_PREFIX = "std_";

		private Map<String, PoiType> translatedNames = new LinkedHashMap<>();
		private List<AbstractPoiType> topVisibleFilters;
		private List<PoiCategory> categories;
		private List<CustomSearchPoiFilter> customPoiFilters = new ArrayList<>();
		private Map<String, Integer> activePoiFilters = new HashMap<>();
		private MapPoiTypes types;

		public SearchAmenityTypesAPI(MapPoiTypes types) {
			super(ObjectType.POI_TYPE);
			this.types = types;
		}

		public void clearCustomFilters() {
			this.customPoiFilters.clear();
			this.activePoiFilters.clear();
		}

		public void addCustomFilter(CustomSearchPoiFilter poiFilter, int priority) {
			this.customPoiFilters.add(poiFilter);
			if (priority > 0) {
				this.activePoiFilters.put(poiFilter.getFilterId(), priority);
			}
		}

		public void setActivePoiFiltersByOrder(List<String> filterOrder) {
			for (int i = 0; i < filterOrder.size(); i++) {
				this.activePoiFilters.put(filterOrder.get(i), i);
			}
		}

		public Map<String, PoiTypeResult> getPoiTypeResults(NameStringMatcher nm, NameStringMatcher nmAdditional) {
			Map<String, PoiTypeResult> results = new LinkedHashMap<>();
			for (AbstractPoiType pf : topVisibleFilters) {
				PoiTypeResult res = checkPoiType(nm, pf);
				if(res != null) {
					results.put(res.pt.getKeyName(), res);
				}
			}
			// don't spam results with unsearchable additionals like 'description', 'email', ...
			// if (nmAdditional != null) {
			//	addAditonals(nmAdditional, results, types.getOtherMapCategory());
			// }
			for (PoiCategory c : categories) {
				PoiTypeResult res = checkPoiType(nm, c);
				if(res != null) {
					results.put(res.pt.getKeyName(), res);
				}
				if (nmAdditional != null) {
					addAditonals(nmAdditional, results, c);
				}
			}
			Iterator<Entry<String, PoiType>> it = translatedNames.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, PoiType> e = it.next();
				PoiType pt = e.getValue();
				if (pt.getCategory() != types.getOtherMapCategory() && !pt.isReference()) {
					PoiTypeResult res = checkPoiType(nm, pt);
					if(res != null) {
						results.put(res.pt.getKeyName(), res);
					}
					if (nmAdditional != null) {
						addAditonals(nmAdditional, results, pt);
					}
				}
			}
			return results;
		}

		private void addAditonals(NameStringMatcher nm, Map<String, PoiTypeResult> results, AbstractPoiType pt) {
			List<PoiType> additionals = pt.getPoiAdditionals();
			if (additionals != null) {
				for (PoiType a : additionals) {
					PoiTypeResult existingResult = results.get(a.getKeyName());
					if (existingResult != null) {
						PoiAdditionalCustomFilter f ;
						if (existingResult.pt instanceof PoiAdditionalCustomFilter) {
							f = (PoiAdditionalCustomFilter) existingResult.pt;
						} else {
							f = new PoiAdditionalCustomFilter(types, (PoiType) existingResult.pt);
						}
						f.additionalPoiTypes.add(a);
						existingResult.pt = f;
					} else {
						String enTranslation = a.getEnTranslation().toLowerCase();
						if (!"no".equals(enTranslation) // && !"yes".equals(enTranslation)
						) {
							PoiTypeResult ptr = checkPoiType(nm, a);
							if (ptr != null) {
								results.put(a.getKeyName(), ptr);
							}
						}
					}
				}
			}
		}

		private PoiTypeResult checkPoiType(NameStringMatcher nm, AbstractPoiType pf) {
			PoiTypeResult res = null;
			if (nm.matches(pf.getTranslation())) {
				res = addIfMatch(nm, pf.getTranslation(), pf, res);
			}
			if (nm.matches(pf.getEnTranslation())) {
				res = addIfMatch(nm, pf.getEnTranslation(), pf, res);
			}
			if (nm.matches(pf.getKeyName())) {
				res = addIfMatch(nm, pf.getKeyName().replace('_', ' '), pf, res);
			}

			if (nm.matches(pf.getSynonyms())) {
				String[] synonyms = pf.getSynonyms().split(";");
				for (String synonym : synonyms) {
					res = addIfMatch(nm, synonym, pf, res);
				}
			}
			return res;
		}

		private PoiTypeResult addIfMatch(NameStringMatcher nm, String s, AbstractPoiType pf, PoiTypeResult res) {
			if (nm.matches(s)) {
				if (res == null) {
					res = new PoiTypeResult();
					res.pt = pf;
				}
				res.foundWords.add(s);

			}
			return res;
		}

		private void initPoiTypes() {
			if (translatedNames.isEmpty()) {
				translatedNames = types.getAllTranslatedNames(false);
				topVisibleFilters = types.getTopVisibleFilters();
				topVisibleFilters.remove(types.getOsmwiki());
				categories = types.getCategories(false);

				if (DISPLAY_DEFAULT_POI_TYPES) {
					List<String> order = new ArrayList<>();
					for (AbstractPoiType p : topVisibleFilters) {
						order.add(getStandardFilterId(p));
					}
					CustomSearchPoiFilter nearestPois = new CustomSearchPoiFilter() {

						@Override
						public boolean isEmpty() {
							return false;
						}

						@Override
						public boolean accept(PoiCategory type, String subcategory) {
							return true;
						}

						@Override
						public ResultMatcher<Amenity> wrapResultMatcher(ResultMatcher<Amenity> matcher) {
							return matcher;
						}

						@Override
						public String getName() {
							return "Neareset POIs";
						}

						@Override
						public Object getIconResource() {
							return null;
						}

						@Override
						public String getFilterId() {
							return "nearest_pois";
						}
					};
					setActivePoiFiltersByOrder(order);
					addCustomFilter(nearestPois, 100);
				}
			}
		}

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			boolean showTopFiltersOnly = !phrase.isUnknownSearchWordPresent();
			NameStringMatcher nm = phrase.getFirstUnknownNameStringMatcher();

			initPoiTypes();
			if (showTopFiltersOnly) {
				for (AbstractPoiType pt : topVisibleFilters) {
					SearchResult res = new SearchResult(phrase);
					res.localeName = pt.getTranslation();
					res.object = pt;
					addPoiTypeResult(phrase, resultMatcher, showTopFiltersOnly, getStandardFilterId(pt), res);
				}

			} else {
				boolean includeAdditional = !phrase.hasMoreThanOneUnknownSearchWord();
				NameStringMatcher nmAdditional = includeAdditional ?
						new NameStringMatcher(phrase.getFirstUnknownSearchWord(), StringMatcherMode.CHECK_EQUALS_FROM_SPACE) : null;
				Map<String, PoiTypeResult> poiTypes = getPoiTypeResults(nm, nmAdditional);
				PoiTypeResult wikiCategory = poiTypes.get(OSM_WIKI_CATEGORY);
				PoiTypeResult wikiType = poiTypes.get(WIKI_PLACE);
				if (wikiCategory != null && wikiType != null) {
					poiTypes.remove(WIKI_PLACE);
				}
				for (PoiTypeResult ptr : poiTypes.values()) {
					boolean match = !phrase.isFirstUnknownSearchWordComplete();
					if (!match) {
						for (String foundName : ptr.foundWords) {
							CollatorStringMatcher csm = new CollatorStringMatcher(foundName, StringMatcherMode.CHECK_ONLY_STARTS_WITH);
							match = csm.matches(phrase.getUnknownSearchPhrase());
							if (match) {
								break;
							}
						}
					}
					if (match) {
						SearchResult res = new SearchResult(phrase);
						if (OSM_WIKI_CATEGORY.equals(ptr.pt.getKeyName())) {
							res.localeName = ptr.pt.getTranslation() + " (" + types.getAllLanguagesTranslationSuffix() + ")";
						} else {
							res.localeName = ptr.pt.getTranslation();
						}
						res.object = ptr.pt;
						addPoiTypeResult(phrase, resultMatcher, showTopFiltersOnly, getStandardFilterId(ptr.pt),
								res);
					}
				}
			}
			for (int i = 0; i < customPoiFilters.size(); i++) {
				CustomSearchPoiFilter csf = customPoiFilters.get(i);
				if (showTopFiltersOnly || nm.matches(csf.getName())) {
					SearchResult res = new SearchResult(phrase);
					res.localeName = csf.getName();
					res.object = csf;
					addPoiTypeResult(phrase, resultMatcher, showTopFiltersOnly, csf.getFilterId(), res);
				}
			}
			return true;
		}

		private void addPoiTypeResult(SearchPhrase phrase, SearchResultMatcher resultMatcher, boolean showTopFiltersOnly,
									  String stdFilterId, SearchResult res) {
			res.priorityDistance = 0;
			res.objectType = ObjectType.POI_TYPE;
			res.firstUnknownWordMatches = true;
			if (showTopFiltersOnly) {
				if (activePoiFilters.containsKey(stdFilterId)) {
					res.priority = getPoiTypePriority(stdFilterId);
					resultMatcher.publish(res);
				}
			} else {
				phrase.countUnknownWordsMatchMainResult(res);
				res.priority = SEARCH_AMENITY_TYPE_PRIORITY;
				resultMatcher.publish(res);
			}
		}

		private int getPoiTypePriority(String stdFilterId) {
			Integer i = activePoiFilters.get(stdFilterId);
			if ( i == null) {
				return SEARCH_AMENITY_TYPE_PRIORITY;
			}
			return SEARCH_AMENITY_TYPE_PRIORITY + i.intValue();
		}



		public String getStandardFilterId(AbstractPoiType poi) {
			return STD_POI_FILTER_PREFIX + poi.getKeyName();
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
			SearchWord lastSelectedWord = p.getLastSelectedWord();
			if (lastSelectedWord != null && ObjectType.isAddress(lastSelectedWord.getType())) {
				return -1;
			}
			return SEARCH_AMENITY_TYPE_API_PRIORITY;
		}
	}

	public static class SearchAmenityByTypeAPI extends SearchBaseAPI {
		private static final int BBOX_RADIUS = 10000;
		private static final int BBOX_RADIUS_NEAREST = 1000;
		private SearchAmenityTypesAPI searchAmenityTypesAPI;
		private MapPoiTypes types;
		private AbstractPoiType unselectedPoiType;
		private String nameFilter;

		public SearchAmenityByTypeAPI(MapPoiTypes types, SearchAmenityTypesAPI searchAmenityTypesAPI) {
			super(ObjectType.POI);
			this.types = types;
			this.searchAmenityTypesAPI = searchAmenityTypesAPI;
		}

		public AbstractPoiType getUnselectedPoiType() {
			return unselectedPoiType;
		}

		public String getNameFilter() {
			return nameFilter;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return getSearchPriority(phrase) != -1 && super.isSearchMoreAvailable(phrase);
		}

		@Override
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return phrase.getRadiusSearch(BBOX_RADIUS);
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return phrase.getNextRadiusSearch(BBOX_RADIUS);
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			unselectedPoiType = null;
			SearchPoiTypeFilter poiTypeFilter = null;
			String nameFilter = null;
			int countExtraWords = 0;
			Set<String> poiAdditionals = new LinkedHashSet<>();
			if (phrase.isLastWord(ObjectType.POI_TYPE)) {
				Object obj = phrase.getLastSelectedWord().getResult().object;
				if (obj instanceof AbstractPoiType) {
					poiTypeFilter = getPoiTypeFilter((AbstractPoiType) obj, poiAdditionals);
				} else if (obj instanceof SearchPoiTypeFilter) {
					poiTypeFilter = (SearchPoiTypeFilter) obj;
				} else {
					throw new UnsupportedOperationException();
				}
				nameFilter = phrase.getUnknownSearchPhrase();
			} else if (searchAmenityTypesAPI != null && phrase.isNoSelectedType() && phrase.getFirstUnknownSearchWord().length() > 1) {
				NameStringMatcher nm = phrase.getFirstUnknownNameStringMatcher();
				NameStringMatcher nmAdditional = new NameStringMatcher(phrase.getFirstUnknownSearchWord(), StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
				searchAmenityTypesAPI.initPoiTypes();
				Map<String, PoiTypeResult> poiTypeResults = searchAmenityTypesAPI.getPoiTypeResults(nm, nmAdditional);
				// find first full match only
				for (PoiTypeResult poiTypeResult : poiTypeResults.values()) {
					for (String foundName : poiTypeResult.foundWords) {
						CollatorStringMatcher csm = new CollatorStringMatcher(foundName, StringMatcherMode.CHECK_ONLY_STARTS_WITH);
						// matches only completely
						int mwords = SearchPhrase.countWords(foundName);
						if (csm.matches(phrase.getUnknownSearchPhrase()) && countExtraWords < mwords) {
							countExtraWords = SearchPhrase.countWords(foundName);
							List<String> otherSearchWords = phrase.getUnknownSearchWords();
							nameFilter = null;
							if (countExtraWords - 1 < otherSearchWords.size()) {
								nameFilter = "";
								for (int k = countExtraWords - 1; k < otherSearchWords.size(); k++) {
									if (nameFilter.length() > 0) {
										nameFilter += SearchPhrase.DELIMITER;
									}
									nameFilter += otherSearchWords.get(k);
								}
							}
							poiTypeFilter = getPoiTypeFilter(poiTypeResult.pt, poiAdditionals);
							unselectedPoiType = poiTypeResult.pt;
							int wordsInPoiType = SearchPhrase.countWords(foundName);
							int wordsInUnknownPart = SearchPhrase.countWords(phrase.getUnknownSearchPhrase());
							if (wordsInPoiType == wordsInUnknownPart) {
								// store only perfect match
								phrase.setUnselectedPoiType(unselectedPoiType);
							}
						}
					}
				}
			}
			this.nameFilter = nameFilter;
			if (poiTypeFilter != null) {
				int radius = BBOX_RADIUS;
				if (phrase.getRadiusLevel() == 1 && poiTypeFilter instanceof CustomSearchPoiFilter) {
					String name = ((CustomSearchPoiFilter) poiTypeFilter).getFilterId();
					if ("std_null".equals(name)) {
						radius = BBOX_RADIUS_NEAREST;
					}
				}
				QuadRect bbox = phrase.getRadiusBBoxToSearch(radius);
				List<BinaryMapIndexReader> offlineIndexes = phrase.getOfflineIndexes();
				Set<String> searchedPois = new TreeSet<>();
				for (BinaryMapIndexReader r : offlineIndexes) {
					ResultMatcher<Amenity> rm = getResultMatcher(phrase, poiTypeFilter, resultMatcher, nameFilter, r,
							searchedPois, poiAdditionals, countExtraWords);
					if (poiTypeFilter instanceof CustomSearchPoiFilter) {
						rm = ((CustomSearchPoiFilter) poiTypeFilter).wrapResultMatcher(rm);
					}
					SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest((int) bbox.left,
							(int) bbox.right, (int) bbox.top, (int) bbox.bottom, -1, poiTypeFilter, rm);
					r.searchPoi(req);
					resultMatcher.apiSearchRegionFinished(this, r, phrase);
				}
			}
			return true;
		}


		private ResultMatcher<Amenity> getResultMatcher(final SearchPhrase phrase, final SearchPoiTypeFilter poiTypeFilter,
		                                                final SearchResultMatcher resultMatcher, final String nameFilter,
		                                                final BinaryMapIndexReader selected, final Set<String> searchedPois,
		                                                final Collection<String> poiAdditionals, final int countExtraWords) {


			final NameStringMatcher ns = nameFilter == null ? null : new NameStringMatcher(nameFilter, CHECK_STARTS_FROM_SPACE);
			return new ResultMatcher<Amenity>() {

				@Override
				public boolean publish(Amenity object) {
					if (phrase.getSettings().isExportObjects()) {
						resultMatcher.exportObject(phrase, object);
					}
					SearchResult res = new SearchResult(phrase);
					String poiID = object.getType().getKeyName() + "_" + object.getId();
					if (!searchedPois.add(poiID)) {
						return false;
					}
					if (object.isClosed()) {
						return false;
					}
					if (!phrase.isAcceptPrivate() && object.isPrivateAccess()) {
						return false;
					}
					if (!poiAdditionals.isEmpty()) {
						boolean found = false;
						for (String add : poiAdditionals) {
							if (object.getAdditionalInfoKeys().contains(add)) {
								found = true;
								break;
							}
						}
						if (!found) {
							return false;
						}
					}
					res.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = object.getOtherNames(true);
					if (Algorithms.isEmpty(res.localeName)) {
						AbstractPoiType st = types.getAnyPoiTypeByKey(object.getSubType());
						if (st != null) {
							res.localeName = st.getTranslation();
						} else {
							res.localeName = object.getSubType();
						}
					}
					if (ns != null) {
						if (ns.matches(res.localeName) || ns.matches(res.otherNames)) {
							phrase.countUnknownWordsMatchMainResult(res, countExtraWords);
						} else {
							String ref = object.getTagContent(Amenity.REF, null);
							if (ref == null || !ns.matches(ref)) {
								return false;
							} else {
								phrase.countUnknownWordsMatch(res, ref, null, countExtraWords);
								res.localeName += " " + ref;
							}
						}
					} else {
						phrase.countUnknownWordsMatch(res, "", null, countExtraWords);
					}

					res.object = object;
					res.preferredZoom = PREFERRED_POI_ZOOM;
					res.file = selected;
					res.location = object.getLocation();
					res.priority = SEARCH_AMENITY_BY_TYPE_PRIORITY;
					res.priorityDistance = 1;
					res.objectType = POI;
					resultMatcher.publish(res);
					return false;
				}

				@Override
				public boolean isCancelled() {
					return resultMatcher.isCancelled();
				}
			};
		}

		private SearchPoiTypeFilter getPoiTypeFilter(AbstractPoiType pt, Set<String> poiAdditionals ) {
			final Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = new LinkedHashMap<PoiCategory,
					LinkedHashSet<String>>();
			pt.putTypes(acceptedTypes);
			poiAdditionals.clear();
			if (pt.isAdditional()) {
				poiAdditionals.add(pt.getKeyName());
			}
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
			if ((p.isLastWord(ObjectType.POI_TYPE) && p.getLastTokenLocation() != null)
					|| (p.isNoSelectedType())) {
				return SEARCH_AMENITY_BY_TYPE_PRIORITY;
			}
			return -1;
		}
	}

	public static class SearchStreetByCityAPI extends SearchBaseAPI {
		private static final int DEFAULT_ADDRESS_BBOX_RADIUS = 100 * 1000;

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

		@Override
		public int getMinimalSearchRadius(SearchPhrase phrase) {
			return phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS);
		}

		@Override
		public int getNextSearchRadius(SearchPhrase phrase) {
			return phrase.getNextRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS);
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
				NameStringMatcher nm = phrase.getMainUnknownNameStringMatcher();
				for (Street object : c.getStreets()) {
					SearchResult res = new SearchResult(phrase);

					res.localeName = object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.otherNames = object.getOtherNames(true);
					boolean pub = true;
					if (object.getName().startsWith("<")) {
						// streets related to city
						pub = false;
					} else if (phrase.isUnknownSearchWordPresent()
							&& !(nm.matches(res.localeName) || nm.matches(res.otherNames))) {
						continue;
					}
					res.localeRelatedObjectName = c.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.object = object;
					res.preferredZoom = PREFERRED_STREET_ZOOM;
					res.file = sw.getResult().file;
					res.location = object.getLocation();
					res.priority = SEARCH_STREET_BY_CITY_PRIORITY;
					//res.priorityDistance = 1;
					res.objectType = ObjectType.STREET;
					subSearchApiOrPublish(phrase, resultMatcher, res, streetsAPI, pub);
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
				NameStringMatcher buildingMatch = phrase.getUnknownWordToSearchBuildingNameMatcher();
				NameStringMatcher startMatch = new NameStringMatcher(lw, StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				for (Building b : s.getBuildings()) {
					SearchResult res = new SearchResult(phrase);
					boolean interpolation = b.belongsToInterpolation(lw);
					if ((!buildingMatch.matches(b.getName()) && !interpolation)
							|| !phrase.isSearchTypeAllowed(ObjectType.HOUSE)) {
						continue;
					}
					if(interpolation) {
						res.localeName = lw;
						res.location = b.getLocation(b.interpolation(lw));
					} else {
						res.localeName = b.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						res.location = b.getLocation();
					}
					res.otherNames = b.getOtherNames(true);
					res.object = b;
					res.file = file;
					res.priority = priority;
					res.priorityDistance = 0;
					res.firstUnknownWordMatches = startMatch.matches(res.localeName);
					// phrase.countUnknownWordsMatchMainResult(res); // same as above
					res.relatedObject = s;
					res.localeRelatedObjectName = s.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
					res.objectType = ObjectType.HOUSE;
					res.preferredZoom = PREFERRED_BUILDING_ZOOM;

					resultMatcher.publish(res);
				}
				String streetIntersection = phrase.getUnknownWordToSearch();
				NameStringMatcher streetMatch = phrase.getMainUnknownNameStringMatcher();
				if (Algorithms.isEmpty(streetIntersection) ||
						(!Character.isDigit(streetIntersection.charAt(0)) &&
								CommonWords.getCommonSearch(streetIntersection) == -1) ) {
					for (Street street : s.getIntersectedStreets()) {
						SearchResult res = new SearchResult(phrase);
						if ((!streetMatch.matches(street.getName()) && !streetMatch.matches(street.getOtherNames(true)))
								|| !phrase.isSearchTypeAllowed(ObjectType.STREET_INTERSECTION)) {
							continue;
						}
						res.otherNames = street.getOtherNames(true);
						res.localeName = street.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						res.object = street;
						res.file = file;
						res.relatedObject = s;
						res.priority = priority + 1;
						res.localeRelatedObjectName = s.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
						res.priorityDistance = 0;
						res.objectType = ObjectType.STREET_INTERSECTION;
						res.location = street.getLocation();
						res.preferredZoom = PREFERRED_STREET_INTERSECTION_ZOOM;
						phrase.countUnknownWordsMatchMainResult(res);
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

	protected static class PoiAdditionalCustomFilter extends AbstractPoiType {

		protected List<PoiType> additionalPoiTypes = new ArrayList<PoiType>();

		public PoiAdditionalCustomFilter(MapPoiTypes registry, PoiType pt) {
			super(pt.getKeyName(), registry);
			additionalPoiTypes.add(pt);
		}

		@Override
		public boolean isAdditional() {
			return true;
		}

		public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
			for (PoiType p : additionalPoiTypes) {
				if (p.getParentType() == registry.getOtherMapCategory()) {
					for (PoiCategory c : registry.getCategories(false)) {
						c.putTypes(acceptedTypes);
					}
				} else {
					p.getParentType().putTypes(acceptedTypes);
				}

			}
			return acceptedTypes;
		}

	}

	public static class SearchLocationAndUrlAPI extends SearchBaseAPI {

		private static final int OLC_RECALC_DISTANCE_THRESHOLD = 100000; // 100 km
		private int olcPhraseHash;
		private LatLon olcPhraseLocation;
		private ParsedOpenLocationCode cachedParsedCode;
		private final List<String> citySubTypes = Arrays.asList("city", "town", "village");
		private final DecimalFormat latLonFormatter = new DecimalFormat("#.0####", new DecimalFormatSymbols(Locale.US));
		
		private SearchAmenityByNameAPI amenitiesApi;

		public SearchLocationAndUrlAPI(SearchAmenityByNameAPI amenitiesApi) {
			super(ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION);
			this.amenitiesApi = amenitiesApi;
		}

		@Override
		public boolean isSearchMoreAvailable(SearchPhrase phrase) {
			return false;
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
		
		LatLon parsePartialLocation(String s) {
			s = s.trim();
			if (s.length() == 0 || !(s.charAt(0) == '-' || Character.isDigit(s.charAt(0))
					|| s.charAt(0) == 'S' || s.charAt(0) == 's'
					|| s.charAt(0) == 'N' || s.charAt(0) == 'n'
					|| s.contains("://"))) {
				return null;
			}
			boolean[] partial = new boolean[]{false};
			List<Double> d = new ArrayList<>();
			List<Object> all = new ArrayList<>();
			List<String> strings = new ArrayList<>();
			LocationParser.splitObjects(s, d, all, strings, partial);
			if (partial[0]) {
				double lat = LocationParser.parse1Coordinate(all, 0, all.size());
				return new LatLon(lat, 0);
			}
			return null;
		}

		private void parseLocation(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			String lw = phrase.getUnknownSearchPhrase();
			// Detect OLC
			ParsedOpenLocationCode parsedCode = cachedParsedCode;
			if (parsedCode == null) {
				parsedCode = parseOpenLocationCode(lw);
			}
			if (parsedCode != null) {
				LatLon latLon = parsedCode.getLatLon();
				// do we have local code with locality
				if (!parsedCode.isFull() && !Algorithms.isEmpty(parsedCode.getPlaceName())) {
					LatLon cityLocation = searchOLCLocation(phrase,resultMatcher);
					if (cityLocation != null) {
						latLon = parsedCode.recover(cityLocation);
					}
				}
				if (latLon == null && !parsedCode.isFull()) {
					latLon = parsedCode.recover(phrase.getSettings().getOriginalLocation());
				}
				if (latLon != null) {
					publishLocation(phrase, resultMatcher, lw, latLon);
				}
			} else {
				LatLon l = LocationParser.parseLocation(lw);
				if (l != null && phrase.isSearchTypeAllowed(ObjectType.LOCATION)) {
					publishLocation(phrase, resultMatcher, lw, l);
				} else if (l == null && phrase.isNoSelectedType() && phrase.isSearchTypeAllowed(ObjectType.PARTIAL_LOCATION)) {
					LatLon ll = parsePartialLocation(lw);
					if (ll != null) {
						SearchResult sp = new SearchResult(phrase);
						sp.priority = SEARCH_LOCATION_PRIORITY;

						sp.object = sp.location = ll;
						sp.localeName = formatLatLon(sp.location.getLatitude()) + ", <input> ";
						sp.objectType = ObjectType.PARTIAL_LOCATION;
						resultMatcher.publish(sp);
					}
				}
			}
		}
		
		private LatLon searchOLCLocation(SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			List<String> unknownWords = phrase.getUnknownSearchWords();
			String text = !unknownWords.isEmpty() ? unknownWords.get(0) : phrase.getUnknownWordToSearch();
			
			final List<String> allowedTypes = Arrays.asList("city", "town", "village");
			QuadRect searchBBox31 = new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
			final NameStringMatcher nm = new NameStringMatcher(text, CHECK_STARTS_FROM_SPACE);
			final String lang = phrase.getSettings().getLang();
			final boolean transliterate = phrase.getSettings().isTransliterate();
			
			SearchSettings settings = phrase.getSettings().setSearchBBox31(searchBBox31);
			settings = settings.setSortByName(false);
			settings = settings.setAddressSearch(true);
			settings = settings.setEmptyQueryAllowed(true);
			
			SearchPhrase olcPhrase = phrase.generateNewPhrase(text, settings);
			final List<SearchResult> result = new ArrayList<>();
			
			ResultMatcher<SearchResult> matcher = new ResultMatcher<SearchResult>() {
				int count = 0;
				
				@Override
				public boolean publish(SearchResult object) {
					if (count > SEARCH_OLC_WITH_CITY_TOTAL_LIMIT) {
						return false;
					}
					Amenity amenity = null;
					if (object.objectType == POI) {
						amenity = (Amenity) object.object;
					}
					
					if (amenity == null) {
						return false;
					}
					
					String subType = amenity.getSubType();
					String localeName = amenity.getName(lang, transliterate);
					Collection<String> otherNames = object.otherNames;
					
					if (!allowedTypes.contains(subType) || (!nm.matches(localeName) && !nm.matches(otherNames))) {
						return false;
					}
					result.add(object);
					count++;
					return true;
				}
				
				@Override
				public boolean isCancelled() {
					return count > SEARCH_OLC_WITH_CITY_TOTAL_LIMIT || resultMatcher.isCancelled();
				}
			};
			
			SearchResultMatcher rm = new SearchResultMatcher(matcher, olcPhrase, 0, new AtomicInteger(0), SEARCH_OLC_WITH_CITY_TOTAL_LIMIT);
			amenitiesApi.search(olcPhrase, rm);
			
			final NameStringMatcher nmEquals = new NameStringMatcher(text, CHECK_EQUALS);
			
			Collections.sort(result, new Comparator<SearchResult>() {
				@Override
				public int compare(SearchResult sr1, SearchResult sr2) {
					Amenity poi1 = new Amenity();
					Amenity poi2 = new Amenity();
					if (sr1.objectType == POI) {
						poi1 = (Amenity) sr1.object;
					}
					if (sr2.objectType == POI) {
						poi2 = (Amenity) sr2.object;
					}
					
					if (poi1 != null && poi2 != null) {
						int o1 = getIndex(poi1);
						int o2 = getIndex(poi2);
						return Algorithms.compare(o2, o1);
					}
					return 0;
				}
				
				private int getIndex(Amenity poi) {
					int res = 0;
					int poiTypeIndex = allowedTypes.indexOf(poi.getSubType());
					if (poiTypeIndex != -1) {
						res += poiTypeIndex;
						if (nmEquals.matches(poi.getName()) || nmEquals.matches(poi.getOtherNames())) {
							res += SEARCH_OLC_WITH_CITY_PRIORITY;
						}
					}
					return res;
				}
			});
			
			return !result.isEmpty() ? result.get(0).location : null;
		}
		
		
		private void publishLocation(SearchPhrase phrase, SearchResultMatcher resultMatcher, String lw, LatLon l) {
			SearchResult sp = new SearchResult(phrase);
			sp.priority = SEARCH_LOCATION_PRIORITY;
			sp.object = sp.location = l;
			sp.localeName = formatLatLon(sp.location.getLatitude()) + ", " + formatLatLon(sp.location.getLongitude());
			sp.objectType = ObjectType.LOCATION;
			sp.wordsSpan = lw;
			resultMatcher.publish(sp);
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
				sp.localeName = formatLatLon(pnt.getLatitude()) +", " + formatLatLon(pnt.getLongitude());
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
			if (!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
				return -1;
			}
			int olcPhraseHash = p.getUnknownSearchPhrase().hashCode();
			if (this.olcPhraseHash == olcPhraseHash && this.olcPhraseLocation != null) {
				double distance = MapUtils.getDistance(p.getSettings().getOriginalLocation(), this.olcPhraseLocation);
				if (distance > OLC_RECALC_DISTANCE_THRESHOLD) {
					olcPhraseHash++;
				}
			}
			if (this.olcPhraseHash != olcPhraseHash) {
				this.olcPhraseHash = olcPhraseHash;
				this.olcPhraseLocation = p.getSettings().getOriginalLocation();
				cachedParsedCode = parseOpenLocationCode(p.getUnknownSearchPhrase());
			}
			return SEARCH_LOCATION_PRIORITY;
		}
		
		private boolean isSearchDone(SearchPhrase phrase) {
			return cachedParsedCode != null;
		}

		private String formatLatLon(double latLon) {
			return latLonFormatter.format(latLon);
		}
	}


	public static boolean isLastWordCityGroup(SearchPhrase p ) {
		return p.isLastWord(ObjectType.CITY) || p.isLastWord(ObjectType.POSTCODE) ||
				p.isLastWord(ObjectType.VILLAGE);
	}
}
