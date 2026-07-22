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
import net.osmand.search.core.spatial.SpatialSearchResult.SpatialSearchResultRef;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

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
			return searchSelectedPoiType(phrase, resultMatcher);
		}
		List<BinaryMapIndexReader> files = getSpatialSearchFiles(phrase);
		if (Algorithms.isEmpty(files)) {
			return false;
		}
		LOG.info("\nStart new spatial search");
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);
		SpatialSearchContext context = createSpatialContext(phrase, resultMatcher, files, poiSearch);
		LOG.info("Spatial search setting " + (context.settings.SEARCH_SUGGESTION ? "SUGGESTION" : "Default"));

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

	private boolean searchSelectedPoiType(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
		if (!phrase.isLastWord(ObjectType.POI_TYPE)) {
			return false;
		}
		SearchWord selectedWord = phrase.getLastSelectedWord();
		Object object = selectedWord == null || selectedWord.getResult() == null ? null : selectedWord.getResult().object;
		if (!(object instanceof AbstractPoiType poiType)) {
			return false;
		}
		List<BinaryMapIndexReader> files = getSpatialPoiSearchFiles(phrase);
		if (Algorithms.isEmpty(files)) {
			return false;
		}
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);
		SpatialPoiSearch.SpatialPoiType spatialPoiType = poiSearch.getByKey(poiType.getKeyName());
		if (spatialPoiType == null) {
			return false;
		}
		SpatialSearchContext context = createSpatialContext(phrase, resultMatcher, files, poiSearch);
		LatLon location = phrase.getSettings().getOriginalLocation();
		if (location == null) {
			return false;
		}
		List<Amenity> amenities = poiSearch.loadPOIObjects(context, spatialPoiType, location,
				getPoiTypeSearchRadius(phrase, location), phrase.getSettings().getTotalLimit());
		for (Amenity amenity : amenities) {
			if (resultMatcher.isCancelled()) {
				return false;
			}
			if (amenity.isClosed() || (!phrase.isAcceptPrivate() && amenity.isPrivateAccess())) {
				continue;
			}
			SearchResult result = createSelectedPoiTypeResult(phrase, amenity);
			resultMatcher.publish(result);
		}
		return true;
	}

	private SpatialSearchContext createSpatialContext(SearchPhrase phrase, SearchResultMatcher resultMatcher,
			List<BinaryMapIndexReader> files, SpatialPoiSearch poiSearch) {
		SpatialSearchContext context = new SpatialSearchContext(createSpatialSettings(phrase), files, poiSearch,
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

	private int getPoiTypeSearchRadius(SearchPhrase phrase, LatLon location) {
		QuadRect searchBBox31 = phrase.getSettings().getSearchBBox31();
		if (searchBBox31 == null) {
			return phrase.getRadiusSearch(10_000);
		}
		double leftLon = MapUtils.get31LongitudeX((int) searchBBox31.left);
		double rightLon = MapUtils.get31LongitudeX((int) searchBBox31.right);
		double topLat = MapUtils.get31LatitudeY((int) searchBBox31.top);
		double bottomLat = MapUtils.get31LatitudeY((int) searchBBox31.bottom);
		double radius = Math.max(
				MapUtils.getDistance(location.getLatitude(), location.getLongitude(), topLat, leftLon),
				MapUtils.getDistance(location.getLatitude(), location.getLongitude(), bottomLat, rightLon));
		return Math.max(1, (int) Math.ceil(radius));
	}

	private SearchResult createSelectedPoiTypeResult(SearchPhrase phrase, Amenity amenity) {
		SearchResult result = new SearchResult(phrase);
		result.object = amenity;
		result.objectType = ObjectType.POI;
		result.location = amenity.getLocation();
		result.preferredZoom = PREFERRED_POI_ZOOM;
		result.cityName = amenity.getCityFromTagGroups(phrase.getSettings().getLang());
		result.localeName = amenity.getName(phrase.getSettings().getLang(), phrase.getSettings().isTransliterate());
		result.otherNames = amenity.getOtherNames(true, result.localeName);
		if (Algorithms.isEmpty(result.localeName)) {
			AbstractPoiType poiType = poiTypes.getAnyPoiTypeByKey(amenity.getSubType());
			result.localeName = poiType == null ? amenity.getSubType() : poiType.getTranslation();
		}
		result.setFirstUnknownWordMatches(true);
		result.priority = SEARCH_PRIORITY;
		result.priorityDistance = 1;
		return result;
	}

	private List<BinaryMapIndexReader> getSpatialPoiSearchFiles(SearchPhrase phrase) {
		QuadRect searchBBox31 = phrase.getSettings().getSearchBBox31();
		List<BinaryMapIndexReader> files = new ArrayList<>();
		if (searchBBox31 == null) {
			addFiles(files, phrase.getOfflineIndexes().iterator());
		} else {
			addFiles(files, phrase.getOfflineIndexes(searchBBox31, SearchPhraseDataType.POI));
		}
		return files;
	}

	private List<BinaryMapIndexReader> getSpatialSearchFiles(SearchPhrase phrase) {
		QuadRect searchBBox31 = phrase.getSettings().getSearchBBox31();
		List<BinaryMapIndexReader> files = new ArrayList<>();
		if (searchBBox31 == null) {
			addFiles(files, phrase.getOfflineIndexes().iterator());
		} else {
			addFiles(files, phrase.getOfflineIndexes(searchBBox31, SearchPhraseDataType.ADDRESS));
			addFiles(files, phrase.getOfflineIndexes(searchBBox31, SearchPhraseDataType.POI));
		}
		addRegionsFile(files, phrase);
		return files;
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
