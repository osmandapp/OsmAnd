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
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
		if (!phrase.isUnknownSearchWordPresent()) {
			return false;
		}
		List<BinaryMapIndexReader> files = getSpatialSearchFiles(phrase);
		if (Algorithms.isEmpty(files)) {
			return false;
		}
		LOG.info("\nStart new spatial search");
		SpatialTextSearchSettings settings = createSpatialSettings(phrase);
		LOG.info("Spatial search setting " + (settings.SEARCH_SUGGESTION ? "SUGGESTION" : "Default"));
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);
		SpatialSearchContext context = new SpatialSearchContext(settings, files, poiSearch,
				phrase.getSettings().getOriginalLocation());
		context.resultMatcher = new net.osmand.ResultMatcher<>() {
			@Override
			public boolean publish(SpatialSearchResult object) {
				return !resultMatcher.isCancelled();
			}

			@Override
			public boolean isCancelled() {
				return resultMatcher.isCancelled();
			}
		};
		context.stats.doTiming = phrase.getSettings().getStat() != null;
		context.stats.printLogs = true;

		SpatialSearchResults results = spatialTextSearch.searchAPI(phrase.getFullSearchPhrase(), context);
		if (results.mainResults == null) {
			return true;
		}
		for (SpatialSearchResult spatialResult : results.mainResults) {
			if (resultMatcher.isCancelled()) {
				return false;
			}
			List<MapObject> allObjects = spatialResult.getObjects();
			if (!allObjects.isEmpty()) {
				LOG.info("found spatial " + allObjects.get(0).getName() + ". visible level " + (spatialResult.visibleLevel()));
			}
			if (spatialResult.visibleLevel() > 0) {
				continue;
			}
			SearchResult searchResult = convertResult(phrase, context, spatialResult);
			if (searchResult != null && phrase.isSearchTypeAllowed(searchResult.objectType)) {
				publishWithParent(resultMatcher, searchResult);
			}
		}
		return true;
	}

	private List<BinaryMapIndexReader> getSpatialSearchFiles(SearchPhrase phrase) {
		QuadRect searchBBox31 = phrase.getSettings().getSearchBBox31();
		if (searchBBox31 == null) {
			return phrase.getSettings().getOfflineIndexes();
		}
		List<BinaryMapIndexReader> files = new ArrayList<>();
		addFiles(files, SearchPhrase.getOfflineIndexes(searchBBox31, SearchPhraseDataType.ADDRESS,
				phrase.getSettings().getOfflineIndexes()));
		addFiles(files, SearchPhrase.getOfflineIndexes(searchBBox31, SearchPhraseDataType.POI,
				phrase.getSettings().getOfflineIndexes()));
		return files;
	}

	private void addFiles(List<BinaryMapIndexReader> files, Iterator<BinaryMapIndexReader> iterator) {
		while (iterator.hasNext()) {
			BinaryMapIndexReader reader = iterator.next();
			if (!files.contains(reader)) {
				files.add(reader);
			}
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
		if (!phrase.isUnknownSearchWordPresent()) {
			return -1;
		}
		return SEARCH_PRIORITY;
	}

	@Override
	public boolean isSearchMoreAvailable(SearchPhrase phrase) {
		return false;
	}

	@Override
	public int getMinimalSearchRadius(SearchPhrase phrase) {
		return 0;
	}

	@Override
	public int getNextSearchRadius(SearchPhrase phrase) {
		return 0;
	}

	private SearchResult convertResult(SearchPhrase phrase, SpatialSearchContext context,
	                                   SpatialSearchResult spatialResult) {
		List<SearchResult> chain = new ArrayList<>();
		boolean hasConcreteObject = hasConcreteObject(spatialResult);
		for (SpatialSearchResultRef ref : spatialResult.objs) {
			if (hasConcreteObject && ref.atom.isPoiCategory()) {
				continue;
			}
			SearchResult result = convertRef(phrase, context, spatialResult, ref);
			if (result != null) {
				chain.add(result);
			}
		}
		if (chain.isEmpty()) {
			return null;
		}
		Collections.reverse(chain);
		SearchResult parent = null;
		for (SearchResult result : chain) {
			result.parentSearchResult = parent;
			parent = result;
		}
		SearchResult result = parent;
		List<MapObject> matchedObjects = spatialResult.getObjects();
		result.matchedObjects = Algorithms.isEmpty(matchedObjects) ? null : matchedObjects;
		if (spatialResult.getLatLon() != null) {
			result.location = spatialResult.getLatLon();
		}
		if (!Algorithms.isEmpty(spatialResult.getExtraNameMatch())) {
			result.alternateName = spatialResult.getExtraNameMatch();
		}
		return result;
	}

	private boolean hasConcreteObject(SpatialSearchResult spatialResult) {
		for (SpatialSearchResultRef ref : spatialResult.objs) {
			if (!ref.atom.isPoiCategory() && (ref.atom.object != null || ref.atom.bldObject != null)) {
				return true;
			}
		}
		return false;
	}

	private SearchResult convertRef(SearchPhrase phrase, SpatialSearchContext context,
	                                SpatialSearchResult spatialResult, SpatialSearchResultRef ref) {
		NameIndexAtom atom = ref.atom;
		if (atom.isPoiCategory()) {
			return convertPoiType(phrase, context, atom);
		}
		MapObject object = atom.bldObject != null ? atom.bldObject : atom.object;
		if (object == null) {
			return null;
		}
		SearchResult result = new SearchResult(phrase);
		result.object = object;
		result.objectType = getObjectType(atom, object, spatialResult);
		result.location = spatialResult.getLatLon() != null && result.objectType == ObjectType.HOUSE
				? spatialResult.getLatLon()
				: atom.getResultLocation();
		result.localeName = getLocaleName(object, phrase);
		result.otherNames = object.getOtherNames(true, result.localeName);
		result.file = getFile(context, atom);
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 1;
		result.preferredZoom = getPreferredZoom(result.objectType);
		fillRelatedObject(result, atom, phrase);
		if (object instanceof Amenity amenity) {
			result.cityName = amenity.getCityFromTagGroups(phrase.getSettings().getLang());
		}
		phrase.countUnknownWordsMatchMainResult(result);
		return result;
	}

	private SearchResult convertPoiType(SearchPhrase phrase, SpatialSearchContext context, NameIndexAtom atom) {
		SpatialPoiSearch.SpatialPoiType spatialPoiType = context.poiSearch.getById((int) atom.id);
		AbstractPoiType poiType = spatialPoiType == null ? null : spatialPoiType.singleType;
		if (poiType == null) {
			return null;
		}
		SearchResult result = new SearchResult(phrase);
		result.object = poiType;
		result.objectType = ObjectType.POI_TYPE;
		result.localeName = poiType.getTranslation();
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 0;
		result.preferredZoom = PREFERRED_POI_ZOOM;
//		result.firstUnknownWordMatches = true;
		phrase.countUnknownWordsMatchMainResult(result);
		return result;
	}

	private ObjectType getObjectType(NameIndexAtom atom, MapObject object, SpatialSearchResult spatialResult) {
		if (atom.isBuilding() || object instanceof Building) {
			return ObjectType.HOUSE;
		}
		if (atom.isPOI() || object instanceof Amenity) {
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
		if (atom.isPostcode()) {
			return ObjectType.POSTCODE;
		}
		if (atom.isBoundary()) {
			return ObjectType.BOUNDARY;
		}
		if (atom.isStreet()) {
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
		} else if (object instanceof Building && atom.object instanceof Street street) {
			result.relatedObject = street;
			result.localeRelatedObjectName = street.getName(phrase.getSettings().getLang(),
					phrase.getSettings().isTransliterate());
		}
	}
}