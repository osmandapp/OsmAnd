package net.osmand.search.core.spatial;

import static net.osmand.search.core.SearchCoreFactory.PREFERRED_BUILDING_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_CITY_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_DEFAULT_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_POI_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_REGION_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_STREET_INTERSECTION_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_STREET_ZOOM;
import static net.osmand.search.core.SearchCoreFactory.SEARCH_ADDRESS_BY_NAME_PRIORITY;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpatialTextSearchAPI extends SearchBaseAPI {

	private static final Log LOG = PlatformUtil.getLog(SpatialTextSearch.class);

	private static final int SEARCH_PRIORITY = SEARCH_ADDRESS_BY_NAME_PRIORITY;

	private final MapPoiTypes poiTypes;
	private final SpatialTextSearch spatialTextSearch = new SpatialTextSearch();
	private final SpatialPoiSearch poiSearch;

	public SpatialTextSearchAPI(MapPoiTypes poiTypes) {
		super(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.BOUNDARY, ObjectType.POSTCODE,
				ObjectType.STREET, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION, ObjectType.POI,
				ObjectType.POI_TYPE);
		this.poiTypes = poiTypes;
		poiSearch = new SpatialPoiSearch(poiTypes);
	}

	@Override
	public boolean isSearchAvailable(SearchPhrase phrase) {
		return !phrase.hasCustomSearchType(ObjectType.ONLINE_SEARCH);
	}

	@Override
	public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
		SpatialTextSearchSettings settings = createSpatialSettings(phrase);
		List<BinaryMapIndexReader> files = getSpatialSearchFiles(phrase, settings);
		if (Algorithms.isEmpty(files)) {
			return false;
		}
		LOG.info("\nStart new spatial search");
		SpatialSearchContext context = createSpatialContext(phrase, resultMatcher, files, poiSearch, settings);
		LOG.info("Spatial search setting " + (context.settings.SEARCH_SUGGESTION ? "SUGGESTION" : "Default"));
		LOG.info("Spatial search setting.LANG_DEDUPLICATE " + context.settings.LANG_DEDUPLICATE);
		LOG.info("Spatial search setting.SUGGESTED_SEARCH_RADIUS_KM " + context.settings.SUGGESTED_SEARCH_RADIUS_KM);

		LOG.info("Spatial search start call spatialTextSearch.searchAPI");
		SpatialSearchResults results = spatialTextSearch.searchAPI(phrase.getFullSearchPhrase(), context);
		LOG.info("Spatial search after call spatialTextSearch.searchAPI");
		if (results.mainResults == null) {
			return true;
		}

		int index = 0;
		LOG.info("found " + results.mainResults.size() + " mainResults");
		for (SpatialSearchResult spatialResult : results.mainResults) {
			LOG.info("found mainResult " + spatialResult + ". visible level " + (spatialResult.visibleLevel()));
			if(index++ == 10) {
				break;
			}
		}

		index = 0;
		if(results.combinations != null) {
			LOG.info("found " + results.combinations.size() + " combinations");
			for (SpatialSearchResultsList combination : results.combinations) {
				LOG.info("found combination " + combination);
				if(index++ == 10) {
					break;
				}
			}
		}


		for (SpatialSearchResult spatialResult : results.mainResults) {
			if (resultMatcher.isCancelled()) {
				return false;
			}
			SearchResult searchResult = convertResult(phrase, context, spatialResult);
			if (searchResult != null) {
				publishWithParent(resultMatcher, searchResult);
			} else {
				LOG.info("searchResult = null");
			}
		}
		return true;
	}

	private SpatialSearchContext createSpatialContext(SearchPhrase phrase, SearchResultMatcher resultMatcher,
			List<BinaryMapIndexReader> files, SpatialPoiSearch poiSearch, SpatialTextSearchSettings settings) {
		SpatialSearchContext context = new SpatialSearchContext(settings, files, poiSearch,
				phrase.getSettings().getOriginalLocation());
		context.resultMatcher = new net.osmand.ResultMatcher<>() {
			@Override
			public boolean publish(SpatialSearchResult object) {
				resultMatcher.sampleMemory();
				return !resultMatcher.isCancelled();
			}

			@Override
			public boolean isCancelled() {
				return resultMatcher.isCancelled();
			}
		};
		context.stats.doTiming = phrase.getSettings().getStat() != null;
		context.stats.printLogs = true;
		return context;
	}

	public List<BinaryMapIndexReader> getSpatialSearchFiles(SearchPhrase phrase) {
		return getSpatialSearchFiles(phrase, createSpatialSettings(phrase));
	}

	private List<BinaryMapIndexReader> getSpatialSearchFiles(SearchPhrase phrase, SpatialTextSearchSettings settings) {
		List<BinaryMapIndexReader> files = new ArrayList<>();
		addFiles(files, getSpatialSearchOfflineIndexes(phrase, settings));
		addRegionsFile(files, phrase);
		return files;
	}

	private Iterator<BinaryMapIndexReader> getSpatialSearchOfflineIndexes(SearchPhrase phrase,
			SpatialTextSearchSettings settings) {
		LatLon latLon = phrase.getLastTokenLocation();
		QuadRect rect = MapUtils.calculate31Bbox(latLon.getLatitude(), latLon.getLongitude(), settings.SUGGESTED_SEARCH_RADIUS_KM * 1000);
		return phrase.getOfflineIndexes(rect, SearchPhraseDataType.MAP);
	}

	private void addRegionsFile(List<BinaryMapIndexReader> files, SearchPhrase phrase) {
		OsmandRegions regions = phrase.getSettings().getRegions();
		if (regions != null) {
			addFile(files, regions.getReader());
		}
	}

	private void addFiles(List<BinaryMapIndexReader> files, Iterator<BinaryMapIndexReader> iterator) {
		while (iterator.hasNext()) {
			addFile(files, iterator.next());
		}
	}

	private void addFile(List<BinaryMapIndexReader> files, BinaryMapIndexReader reader) {
		if (reader != null && !files.contains(reader)) {
			files.add(reader);
		}
	}

	private void publishWithParent(SearchResultMatcher resultMatcher, SearchResult searchResult) {
		SearchResult parent = searchResult.parentSearchResult;
		SearchResult previousParent = resultMatcher.setParentSearchResult(parent);
		resultMatcher.publish(searchResult);
		resultMatcher.setParentSearchResult(previousParent);
	}

	private SpatialTextSearchSettings createSpatialSettings(SearchPhrase phrase) {
		SpatialTextSearchSettings settings = phrase.isLastUnknownSearchWordComplete()
				? SpatialTextSearchSettings.defaultSettings()
				: SpatialTextSearchSettings.suggestionSettings();
		settings.LANG_DEDUPLICATE = phrase.getSettings().getLang();
		return settings;
	}

	@Override
	public int getSearchPriority(SearchPhrase phrase) {
		if (!phrase.isUnknownSearchWordPresent()
				|| phrase.isLastWord(ObjectType.POI_TYPE, ObjectType.STREET)) {
			return -1;
		}
		return SEARCH_PRIORITY;
	}

	@Override
	public boolean isSearchMoreAvailable(SearchPhrase phrase) {
		return false;
	}

	private SearchResult convertResult(SearchPhrase phrase, SpatialSearchContext context, SpatialSearchResult ssr) {
		SearchResult result = new SearchResult(phrase);

		LatLon spatialLocation = ssr.getLatLon();
		LatLon phraseLocation = phrase.getSettings().getOriginalLocation();
		LatLon location = spatialLocation != null ? spatialLocation : phraseLocation; // nullable

		String lang = phrase.getSettings().getLang();
		boolean transliterate = phrase.getSettings().isTransliterate();

		if (convertSpatialSearchResult(ssr, result, context.poiSearch, location, lang, transliterate) == null) {
			return null;
		}

		phrase.countUnknownWordsMatchMainResult(result);
		return result;
	}

	public SearchResult convertSpatialSearchResult(SpatialSearchResult ssr, SearchResult result,
	                                               SpatialPoiSearch poiTypeSearch, LatLon location,
	                                               String lang, boolean transliterate) {
		List<MapObject> objs = ssr.getObjects();

		result.spatialResult = ssr;
		result.location = location; // nullable
		result.spatialSearchVisibleLevel = ssr.visibleLevel();

		if (ssr.isPoiCategory()) {
			return convertPoiType(ssr, result, poiTypeSearch);
		} else if (Algorithms.isNotEmpty(objs)) {
			return convertMapObjects(ssr, objs, result, lang, transliterate);
		}

		return null;
	}

	private SearchResult convertMapObjects(SpatialSearchResult ssr, List<MapObject> mapObjects,
	                                       SearchResult result, String lang, boolean transliterate) {
		MapObject obj = ssr.getMainObject();
		if (obj == null) {
			return null;
		}

		String extraNameMatch = ssr.getExtraNameMatch();

		if (obj instanceof Building b && b.isInterpolation() && Algorithms.isNotEmpty(extraNameMatch)) {
			result.localeName = extraNameMatch; // interpolated house number
		} else {
			result.localeName = obj.getName(lang, transliterate);
			result.otherNames = obj.getOtherNames(transliterate, result.localeName);
			if (Algorithms.isNotEmpty(extraNameMatch)) {
				result.localeName += " (" + extraNameMatch + ")"; // ref
			}
		}

		if (obj instanceof Amenity amenity) {
			result.objectType = ObjectType.POI;
			result.cityName = amenity.getCityFromTagGroups(lang);
		} else if (obj instanceof Street) {
			result.objectType = countStreetRefs(ssr) > 1 ? ObjectType.STREET_INTERSECTION : ObjectType.STREET;
			City city = SpatialTextSearchAPI.getSpatialCity(mapObjects);
			if (city != null) {
				result.relatedObject = city;
				result.localeRelatedObjectName = city.getName(lang, transliterate);
			}
		} else if (obj instanceof Building) {
			result.objectType = ObjectType.HOUSE;
			Street street = SpatialTextSearchAPI.getSpatialStreet(mapObjects);
			if (street != null) {
				result.relatedObject = street;
				result.localeRelatedObjectName = street.getName(lang, transliterate);
			}
			City city = SpatialTextSearchAPI.getSpatialCity(mapObjects);
			if (city != null) {
				SearchResult parent = new SearchResult(result.requiredSearchPhrase);
				parent.relatedObject = city;
				parent.localeRelatedObjectName = city.getName(lang, transliterate);
				result.parentSearchResult = parent;
			}
		} else if (obj instanceof City city) {
			CityType type = city.getType();
			if (type == CityType.CITY || type == CityType.TOWN) {
				result.objectType = ObjectType.CITY;
			} else if (type == CityType.POSTCODE) {
				result.objectType = ObjectType.POSTCODE;
			} else if (type == CityType.BOUNDARY) {
				result.objectType = ObjectType.BOUNDARY;
			} else {
				result.objectType = ObjectType.VILLAGE;
			}
		} else {
			result.objectType = ObjectType.LOCATION;
		}

		result.object = obj;
		result.priorityDistance = 1;
		result.priority = SEARCH_PRIORITY;
		result.preferredZoom = getPreferredZoom(result.objectType);

		return result;
	}

	private static Street getSpatialStreet(List<MapObject> objs) {
		for (MapObject obj : objs) {
			if (obj instanceof Street street) {
				return street;
			}
		}
		return null;
	}

	private static City getSpatialCity(List<MapObject> objs) {
		for (MapObject obj : objs) {
			if (obj instanceof City city) {
				return city;
			}
			if (obj instanceof Street street && street.getCity() != null) {
				return street.getCity();
			}
		}
		return null;
	}

	private SearchResult convertPoiType(SpatialSearchResult ssr, SearchResult result, SpatialPoiSearch poiTypeSearch) {
		SpatialPoiSearch.SpatialPoiType spatialPoiType = ssr.getPoiCategory(poiTypeSearch);
		if (spatialPoiType == null) {
			return null;
		}
		Object object;
		String localeName;
		AbstractPoiType poiType = spatialPoiType.singleType;
		if (poiType != null) {
			object = poiType;
			localeName = poiType.getTranslation();
		} else if (spatialPoiType.poiSubType != null) {
			object = new TopIndexFilter(spatialPoiType.poiSubType, poiTypes, spatialPoiType.poiAdditional);
			localeName = spatialPoiType.names.get(0);
		} else {
			return null;
		}
		result.object = object;
		result.objectType = ObjectType.POI_TYPE;
		result.localeName = localeName;
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 0;
		result.preferredZoom = PREFERRED_POI_ZOOM;
		return result;
	}

	private int countStreetRefs(SpatialSearchResult spatialResult) {
		int count = 0;
		for (SpatialSearchResultRef ref : spatialResult.objs) {
			if (ref.atom.isStreet() && ref.atom.object instanceof Street) {
				count++;
			}
		}
		return count;
	}

	private int getPreferredZoom(ObjectType type) {
		if (type == ObjectType.HOUSE) {
			return PREFERRED_BUILDING_ZOOM;
		} else if (type == ObjectType.STREET) {
			return PREFERRED_STREET_ZOOM;
		} else if (type == ObjectType.STREET_INTERSECTION) {
			return PREFERRED_STREET_INTERSECTION_ZOOM;
		} else if (type == ObjectType.CITY || type == ObjectType.VILLAGE) {
			return PREFERRED_CITY_ZOOM;
		} else if (type == ObjectType.POSTCODE) {
			return PREFERRED_REGION_ZOOM;
		} else if (type == ObjectType.BOUNDARY) {
			return PREFERRED_REGION_ZOOM;
		} else if (type == ObjectType.POI) {
			return PREFERRED_POI_ZOOM;
		}
		return PREFERRED_DEFAULT_ZOOM;
	}
}
