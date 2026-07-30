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
import net.osmand.search.core.SearchWord;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpatialTextSearchAPI extends SearchBaseAPI {

	private static final Log LOG = PlatformUtil.getLog(SpatialTextSearch.class);

	private static final int SEARCH_PRIORITY = SEARCH_ADDRESS_BY_NAME_PRIORITY;

	private final MapPoiTypes poiTypes;
	private final SpatialTextSearch spatialTextSearch = new SpatialTextSearch();

	public SpatialTextSearchAPI(MapPoiTypes poiTypes) {
		super(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.BOUNDARY, ObjectType.POSTCODE,
				ObjectType.STREET, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION, ObjectType.POI,
				ObjectType.POI_TYPE);
		this.poiTypes = poiTypes;
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
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);
		SpatialSearchContext context = createSpatialContext(phrase, resultMatcher, files, poiSearch, settings);
		LOG.info("Spatial search setting " + (context.settings.SEARCH_SUGGESTION ? "SUGGESTION" : "Default"));
		LOG.info("Spatial search setting.LANG_DEDUPLICATE " + context.settings.LANG_DEDUPLICATE);
		LOG.info("Spatial search setting.SUGGESTED_SEARCH_RADIUS_KM " + context.settings.SUGGESTED_SEARCH_RADIUS_KM);

		SpatialSearchResults results = spatialTextSearch.searchAPI(phrase.getFullSearchPhrase(), context);
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

	private List<BinaryMapIndexReader> getSpatialPoiSearchFiles(SearchPhrase phrase) {
		return getSpatialPoiSearchFiles(phrase, createSpatialSettings(phrase));
	}

	private List<BinaryMapIndexReader> getSpatialPoiSearchFiles(SearchPhrase phrase, SpatialTextSearchSettings settings) {
		List<BinaryMapIndexReader> files = new ArrayList<>();
		addFiles(files, getSpatialSearchOfflineIndexes(phrase, settings));
		return files;
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
		if (!phrase.isUnknownSearchWordPresent() && !phrase.isLastWord(ObjectType.POI_TYPE)) {
			return -1;
		}
		return SEARCH_PRIORITY;
	}

	@Override
	public boolean isSearchMoreAvailable(SearchPhrase phrase) {
		return false;
	}

	private SearchResult convertResult(SearchPhrase phrase, SpatialSearchContext context,
	                                   SpatialSearchResult spatialResult) {
		MapObject mainObject = spatialResult.getMainObject();

		SearchResult result = mainObject != null
				? convertMainObject(phrase, spatialResult, mainObject)
				: convertPoiType(phrase, spatialResult, context);
		if (result == null) {
			return null;
		}
		result.spatialSearchVisibleLevel = spatialResult.visibleLevel();
		result.spatialResult = spatialResult;
		if (spatialResult.getLatLon() != null) {
			result.location = spatialResult.getLatLon();
		}
		if (!Algorithms.isEmpty(spatialResult.getExtraNameMatch())) {
			result.alternateName = spatialResult.getExtraNameMatch();
		}
		return result;
	}

	private SearchResult convertMainObject(SearchPhrase phrase,
	                                       SpatialSearchResult spatialResult, MapObject mainObject) {
		SearchResult result = new SearchResult(phrase);
		result.object = mainObject;
		result.objectType = getObjectType(null, mainObject, spatialResult);
		result.location = mainObject.getLocation();
		result.localeName = getLocaleName(mainObject, phrase);
		result.otherNames = mainObject.getOtherNames(true, result.localeName);
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 1;
		result.preferredZoom = getPreferredZoom(result.objectType);
		fillRelatedObject(result, null, phrase);
		if (mainObject instanceof Amenity amenity) {
			result.cityName = amenity.getCityFromTagGroups(phrase.getSettings().getLang());
		}
		phrase.countUnknownWordsMatchMainResult(result);
		return result;
	}

	private SpatialSearchResultRef getRef(SpatialSearchResult spatialResult, MapObject object) {
		for (SpatialSearchResultRef ref : spatialResult.objs) {
			if (ref.atom.object == object || ref.atom.bldObject == object) {
				return ref;
			}
		}
		return null;
	}

	private SearchResult convertPoiType(SearchPhrase phrase, SpatialSearchResult spatialResult, SpatialSearchContext context) {
		SpatialPoiSearch.SpatialPoiType spatialPoiType = spatialResult.getPoiCategory(context.poiSearch);

		if (spatialPoiType == null) {
			return null;
		}
		Object object = null;
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
		SearchResult result = new SearchResult(phrase);
		result.object = object;
		result.objectType = ObjectType.POI_TYPE;
		result.localeName = localeName;
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 0;
		result.preferredZoom = PREFERRED_POI_ZOOM;
		phrase.countUnknownWordsMatchMainResult(result);
		return result;
	}

	private String getTopIndexTranslation(String value) {
		String key = TopIndexFilter.getValueKey(value);
		String translate = poiTypes.getPoiTranslation(key);
		return translate.equalsIgnoreCase(key) ? value : translate;
	}

	private ObjectType getObjectType(NameIndexAtom atom, MapObject object, SpatialSearchResult spatialResult) {
		if ((atom != null && atom.isBuilding()) || object instanceof Building) {
			return ObjectType.HOUSE;
		}
		if ((atom != null && atom.isPOI()) || object instanceof Amenity) {
			return ObjectType.POI;
		}
		if (object instanceof Street) {
			return countStreetRefs(spatialResult) > 1 ? ObjectType.STREET_INTERSECTION : ObjectType.STREET;
		}
		if (object instanceof City city) {
			CityType type = city.getType();
			if (type == CityType.CITY || type == CityType.TOWN) {
				return ObjectType.CITY;
			} else if (type == CityType.POSTCODE) {
				return ObjectType.POSTCODE;
			} else if (type == CityType.BOUNDARY) {
				return ObjectType.BOUNDARY;
			}
			return ObjectType.VILLAGE;
		}
		if (atom != null && atom.isPostcode()) {
			return ObjectType.POSTCODE;
		}
		if (atom != null && atom.isBoundary()) {
			return ObjectType.BOUNDARY;
		}
		if (atom != null && atom.isStreet()) {
			return ObjectType.STREET;
		}
		return ObjectType.POI;
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

	private String getLocaleName(MapObject object, SearchPhrase phrase) {
		return object.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
	}

	private BinaryMapIndexReader getFile(SpatialSearchContext context, NameIndexAtom atom) {
		if (atom == null) {
			return null;
		}
		int indexInd = context.getFileInd(atom.id);
		for (SpatialTextSearch.SpatialSearchFileCache fileCache : context.internalFile) {
			if (indexInd < fileCache.indexInd + fileCache.indexReaders.size()) {
				int fileInd = fileCache.fileInd;
				return fileInd >= 0 && fileInd < context.files.size() ? context.files.get(fileInd) : null;
			}
		}
		return null;
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

	private void fillRelatedObject(SearchResult result, NameIndexAtom atom, SearchPhrase phrase) {
		Object object = result.object;
		if (object instanceof Street street) {
			City city = street.getCity();
			result.relatedObject = city;
			result.localeRelatedObjectName = city == null ? null : city.getName(phrase.getSettings().getLang(),
					phrase.getSettings().isTransliterate());
		} else if (object instanceof Building && atom != null && atom.object instanceof Street street) {
			result.relatedObject = street;
			result.localeRelatedObjectName = street.getName(phrase.getSettings().getLang(),
					phrase.getSettings().isTransliterate());
		}
	}
}
