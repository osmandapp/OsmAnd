package net.osmand.search;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE;
import static net.osmand.CollatorStringMatcher.StringMatcherMode.MULTISEARCH;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

import net.osmand.CallbackWithObject;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportStop;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.core.AmenityIndexRepository;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class AmenitySearcher {

    protected final Map<String, AmenityIndexRepository> amenityRepositories = new ConcurrentHashMap<>();
    private final TravelFileVisibility travelFileVisibility;
    private ThreadPoolExecutor singleThreadedExecutor;
    private LinkedBlockingQueue<Runnable> taskQueue;

    private static final int AMENITY_SEARCH_RADIUS = 50;
    private static final int AMENITY_SEARCH_RADIUS_FOR_RELATION = 500;
    private String lang;
    private boolean transliterate;
    private final MapPoiTypes mapPoiTypes; // nullable

    public AmenitySearcher(String lang, boolean transliterate, MapPoiTypes mapPoiTypes,
                             TravelFileVisibility travelFileVisibility) {
        this.travelFileVisibility = travelFileVisibility;
        this.lang = lang;
        this.transliterate = transliterate;
        this.mapPoiTypes = mapPoiTypes;
        taskQueue = new LinkedBlockingQueue<Runnable>();
        singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
    }

    public void updateLangAndTransliterate(String lang, boolean transliterate) {
        this.lang = lang;
        this.transliterate = transliterate;
    }

    public interface TravelFileVisibility {
        public boolean getTravelFileVisibility(String fileName);
    }

    public List<AmenityIndexRepository> getAmenityRepositories(boolean includeTravel) {
        List<String> fileNames = new ArrayList<>(amenityRepositories.keySet());
        List<AmenityIndexRepository> travelMaps = new ArrayList<>();
        List<AmenityIndexRepository> baseMaps = new ArrayList<>();
        List<AmenityIndexRepository> result = new ArrayList<>();

        fileNames.sort(Algorithms.getStringVersionComparator());

        for (String fileName : fileNames) {
            AmenityIndexRepository r = amenityRepositories.get(fileName);
            if (r != null && fileName.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
                if (includeTravel && !travelFileVisibility.getTravelFileVisibility(fileName)) {
                    travelMaps.add(r);
                }
            } else if (r != null && r.isWorldMap()) {
                baseMaps.add(r);
            } else if (r != null) {
                result.add(r);
            }
        }

        result.addAll(baseMaps);
        result.addAll(travelMaps);

        return result;
    }

    public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter, QuadRect rect,
                                         boolean includeTravel) {
        return searchAmenities(filter, null, rect.top, rect.left, rect.bottom, rect.right, -1, includeTravel, null);
    }

    public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter,
                                         BinaryMapIndexReader.SearchPoiAdditionalFilter additionalFilter, double topLatitude,
                                         double leftLongitude, double bottomLatitude,
                                         double rightLongitude, int zoom, boolean includeTravel,
                                         ResultMatcher<Amenity> matcher) {

        Set<Long> openAmenities = new HashSet<>();
        Set<Long> closedAmenities = new HashSet<>();
        List<Amenity> actualAmenities = new ArrayList<>();

        boolean isEmpty = filter.isEmpty();
        if (isEmpty && additionalFilter != null) {
            filter = null;
        }
        if (!isEmpty || additionalFilter != null) {
            int top31 = MapUtils.get31TileNumberY(topLatitude);
            int left31 = MapUtils.get31TileNumberX(leftLongitude);
            int bottom31 = MapUtils.get31TileNumberY(bottomLatitude);
            int right31 = MapUtils.get31TileNumberX(rightLongitude);

            List<AmenityIndexRepository> repos = getAmenityRepositories(includeTravel);

            for (AmenityIndexRepository repo : repos) {
                if (matcher != null && matcher.isCancelled()) {
                    break;
                }
                if (repo.checkContainsInt(top31, left31, bottom31, right31)) {
                    List<Amenity> foundAmenities = repo.searchAmenities(top31, left31, bottom31, right31,
                            zoom, filter, additionalFilter, matcher);
                    if (foundAmenities != null) {
                        for (Amenity amenity : foundAmenities) {
                            Long id = amenity.getId();
                            if (amenity.isClosed()) {
                                closedAmenities.add(id);
                            } else if (!closedAmenities.contains(id)) {
                                if (openAmenities.add(id)) {
                                    actualAmenities.add(amenity);
                                }
                            }
                        }
                    }
                }
            }
        }

        return actualAmenities;
    }

    public Amenity searchDetailsAmenity(LatLon latLon, Long id, Collection<String> names, String wikidata) {
        BaseDetailsObject detail = searchDetailsObject(latLon, id, names, wikidata);
        return detail != null ? detail.getSyntheticAmenity() : null;
    }

    public BaseDetailsObject searchDetailsObject(Object object) {
        LatLon latLon = null;
        Long id = null;
        Collection<String> names = null;
        String wikidata = null;
        if (object instanceof Amenity amenity) {
            latLon = amenity.getLocation();
            id = amenity.getId();
            wikidata = amenity.getWikidata();
            names = amenity.getOtherNames();
            names.add(amenity.getName());
        }
        if (object instanceof RenderedObject renderedObject) {
            latLon = renderedObject.getLatLon();
            names = renderedObject.getOriginalNames();
            id = ObfConstants.getOsmObjectId(renderedObject) << AMENITY_ID_RIGHT_SHIFT;
            wikidata = renderedObject.getTagValue(WIKIDATA);
        }
        if (object instanceof TransportStop stop) {
            latLon = stop.getLocation();
            id = stop.getId();
            names = stop.getOtherNames();
            names.add(stop.getName());
        }
        if (object instanceof BaseDetailsObject detailsObject) {
            if (detailsObject.isObjectFull()) {
                completeGeometry(detailsObject, detailsObject.getObjects().get(0));
                return detailsObject;
            }
            if (!detailsObject.getObjects().isEmpty()) {
                Object obj = detailsObject.getObjects().get(0);
                return searchDetailsObject(obj);
            }
        }
        BaseDetailsObject detailsObject = null;
        if (latLon != null) {
            detailsObject = searchDetailsObject(latLon, id, names, wikidata);
        }
        completeGeometry(detailsObject, object);
        return detailsObject;
    }

	public BaseDetailsObject searchDetailsObject(LatLon latLon, Long obId, Collection<String> names, String wikidata) {
		if (latLon == null) {
            return null;
        }
        long id = obId == null ? -1 : obId;
        boolean relation = ObfConstants.isIdFromRelation(id >> AMENITY_ID_RIGHT_SHIFT);
        int searchRadius = relation ? AMENITY_SEARCH_RADIUS_FOR_RELATION : AMENITY_SEARCH_RADIUS;
        QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), searchRadius);

        List<Amenity> amenities = searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, false);

        long osmId = ObfConstants.isShiftedID(id) ? ObfConstants.getOsmId(id) : id >> AMENITY_ID_RIGHT_SHIFT;
        List<Amenity> filtered = new ArrayList<>();
        if (osmId > 0 || wikidata != null) {
            filtered = filterByOsmIdOrWikidata(amenities, osmId, latLon, wikidata);
        }
        if (Algorithms.isEmpty(filtered) && !Algorithms.isEmpty(names)) {
            Amenity amenity = findByName(amenities, names, latLon);
            if (amenity != null) {
                filtered = filterByOsmIdOrWikidata(amenities, amenity.getOsmId(), amenity.getLocation(), amenity.getWikidata());
            }
        }
        if (!Algorithms.isEmpty(filtered)) {
	        return new BaseDetailsObject(filtered, lang);
        }
        return null;
    }

    private List<Amenity> filterByOsmIdOrWikidata(Collection<Amenity> amenities, long id, LatLon point, String wikidata) {
        List<Amenity> result = new ArrayList<>();
        double minDist = AMENITY_SEARCH_RADIUS_FOR_RELATION * 4;
        for (Amenity amenity : amenities) {
            Long initAmenityId = amenity.getId();
            if (initAmenityId != null) {
                String wiki = amenity.getWikidata();
                boolean wikiEqual = wiki != null && wiki.equals(wikidata);
                boolean idEqual = amenity.getOsmId() != null && amenity.getOsmId() == id;
                if ((idEqual || wikiEqual) && !amenity.isClosed()) {
                    double dist = MapUtils.getDistance(amenity.getLocation(), point);
                    if (dist < minDist) {
                        result.add(0, amenity); // to the top
                        minDist = dist;
                    } else {
                        result.add(amenity);
                    }
                }
            }
        }
        return result;
    }

    private Amenity findByName(Collection<Amenity> amenities, Collection<String> names, LatLon searchLatLon) {
        if (!Algorithms.isEmpty(names) && !Algorithms.isEmpty(amenities)) {
            return amenities.stream()
                    .sorted(Comparator.comparingDouble(a -> MapUtils.getDistance(a.getLocation(), searchLatLon)))
                    .filter(amenity -> !amenity.isClosed())
                    .filter(amenity -> namesMatcher(amenity, names, false))
                    .findAny()
                    .orElseGet(() ->
                            amenities.stream()
                                    .filter(amenity -> !amenity.isClosed())
                                    .filter(amenity -> amenity.isRoutePoint())
                                    .filter(amenity -> amenity.getName().isEmpty())
                                    .filter(amenity -> {
                                        String travelRouteId = amenity.getAdditionalInfo("route_id");
                                        return travelRouteId != null && names.contains(travelRouteId);
                                    })
                                    .findAny()
                                    .orElseGet(() ->
                                            amenities.stream()
                                                    .filter(amenity -> namesMatcher(amenity, names, true))
                                                    .findAny().orElse(null)));
        }
        return null;
    }

    private boolean namesMatcher(Amenity amenity, Collection<String> matchList, boolean matchAllLanguagesAndAltNames) {
        String poiSimpleFormat = Amenity.getPoiStringWithoutType(amenity, lang, transliterate);
        if (matchList.contains(poiSimpleFormat)) {
            return true;
        }

        String amenityName = amenity.getName(lang, transliterate);
        if (!Algorithms.isEmpty(amenityName)) {
            for (String match : matchList) {
                if (match.endsWith(amenityName)) {
                    return true;
                }
            }
        }

        if (mapPoiTypes != null) {
            AbstractPoiType st = mapPoiTypes.getAnyPoiTypeByKey(amenity.getSubType());
            String poiTypeName = st != null ? st.getTranslation() : amenity.getSubType();
            if (matchList.contains(poiTypeName)) {
                return true;
            }
        }

        if (matchAllLanguagesAndAltNames) {
            Set<String> allAmenityNames = new HashSet<>();
            allAmenityNames.addAll(amenity.getAltNamesMap().values());
            allAmenityNames.addAll(amenity.getNamesMap(true).values());

            String typeName = amenity.getSubTypeStr();
            if (!Algorithms.isEmpty(typeName)) {
                Set<String> withPoiTypes = new HashSet<>();
                for (String name : allAmenityNames) {
                    withPoiTypes.add(typeName + " " + name);
                }
                allAmenityNames.addAll(withPoiTypes);
            }

            for (String match : matchList) {
                if (allAmenityNames.contains(match)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addAmenityRepository(String fileName, AmenityIndexRepository repository) {
        amenityRepositories.put(fileName, repository);
    }

    public Collection<AmenityIndexRepository> getAmenityRepositories() {
        return amenityRepositories.values();
    }

    public AmenityIndexRepository getAmenityRepository(String fileName) {
        return amenityRepositories.get(fileName);
    }

    public void removeAmenityRepository(String fileName) {
        amenityRepositories.remove(fileName);
    }

    public void clearAmenityRepositories() {
        amenityRepositories.clear();
    }

    public List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius,
                                                  BinaryMapIndexReader.SearchPoiTypeFilter filter,
                                                  ResultMatcher<Amenity> matcher) {
        List<Amenity> amenities = new ArrayList<>();

        if (locations != null && !locations.isEmpty()) {
            List<AmenityIndexRepository> repos = new ArrayList<>();
            double topLatitude = locations.get(0).getLatitude();
            double bottomLatitude = locations.get(0).getLatitude();
            double leftLongitude = locations.get(0).getLongitude();
            double rightLongitude = locations.get(0).getLongitude();
            for (Location l : locations) {
                topLatitude = Math.max(topLatitude, l.getLatitude());
                bottomLatitude = Math.min(bottomLatitude, l.getLatitude());
                leftLongitude = Math.min(leftLongitude, l.getLongitude());
                rightLongitude = Math.max(rightLongitude, l.getLongitude());
            }
            if (!filter.isEmpty()) {
                for (AmenityIndexRepository index : getAmenityRepositories()) {
                    if (index.checkContainsInt(
                            MapUtils.get31TileNumberY(topLatitude),
                            MapUtils.get31TileNumberX(leftLongitude),
                            MapUtils.get31TileNumberY(bottomLatitude),
                            MapUtils.get31TileNumberX(rightLongitude))) {
                        repos.add(index);
                    }
                }
                if (!repos.isEmpty()) {
                    for (AmenityIndexRepository r : repos) {
                        List<Amenity> res = r.searchAmenitiesOnThePath(locations, radius, filter, matcher);
                        if (res != null) {
                            amenities.addAll(res);
                        }
                    }
                }
            }
        }

        return amenities;
    }

    private List<Amenity> searchRouteByName(String multipleSearch, CollatorStringMatcher.StringMatcherMode mode, ResultMatcher<Amenity> matcher) {
        List<Amenity> result = new ArrayList<>();
        BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
                0, 0, multipleSearch, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, matcher
        );
        req.setMatcherMode(mode);
        for (AmenityIndexRepository index : getAmenityRepositories(false)) {
            List<Amenity> amenities = index.searchPoiByName(req);
            if (!Algorithms.isEmpty(amenities)) {
                result.addAll(amenities);
            }
        }
        return result;
    }

    public List<Amenity> searchRoutePartOf(String routeId) {
        ResultMatcher<Amenity> matcher = new ResultMatcher<Amenity>() {
            @Override
            public boolean publish(Amenity amenity) {
                String members = amenity.getAdditionalInfo(Amenity.ROUTE_MEMBERS_IDS);
                if (members != null) {
                    HashSet<String> ids = new HashSet<>();
                    Collections.addAll(ids, members.split(" "));
                    return ids.contains(routeId);
                }
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        return searchRouteByName(routeId, CHECK_EQUALS_FROM_SPACE, matcher);
    }

    public Map<String, List<Amenity>> searchRouteMembers(String multipleSearch) {
        HashSet<String> ids = new HashSet<>();
        Collections.addAll(ids, multipleSearch.split(" "));
        ResultMatcher<Amenity> matcher = new ResultMatcher<Amenity>() {
            @Override
            public boolean publish(Amenity amenity) {
                String routeId = amenity.getAdditionalInfo(Amenity.ROUTE_ID);
                return routeId != null && ids.contains(routeId);
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        Map<String, List<Amenity>> map = new HashMap<>();
        List<Amenity> result = searchRouteByName(multipleSearch, MULTISEARCH, matcher);
        for (Amenity am : result) {
            String routeId = am.getAdditionalInfo(Amenity.ROUTE_ID);
            List<Amenity> amenities = map.computeIfAbsent(routeId, l -> new ArrayList<>());
            amenities.add(am);
        }
        for (String id : ids) {
            if (!map.containsKey(id)) {
                map.put(id, null);
            }
        }
        return map;
    }

    public List<Amenity> searchAmenitiesByName(String searchQuery,
                                               double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
                                               double lat, double lon, ResultMatcher<Amenity> matcher) {
        List<Amenity> amenities = new ArrayList<>();
        List<AmenityIndexRepository> list = new ArrayList<>();
        int left = MapUtils.get31TileNumberX(leftLongitude);
        int top = MapUtils.get31TileNumberY(topLatitude);
        int right = MapUtils.get31TileNumberX(rightLongitude);
        int bottom = MapUtils.get31TileNumberY(bottomLatitude);
        for (AmenityIndexRepository index : getAmenityRepositories(false)) {
            if (matcher != null && matcher.isCancelled()) {
                break;
            }
            if (index.checkContainsInt(top, left, bottom, right)) {
                if (index.checkContains(lat, lon)) {
                    list.add(0, index);
                } else {
                    list.add(index);
                }

            }
        }

        for (AmenityIndexRepository index : list) {
            if (matcher != null && matcher.isCancelled()) {
                break;
            }
            List<Amenity> result = index.searchAmenitiesByName(MapUtils.get31TileNumberX(lon), MapUtils.get31TileNumberY(lat),
                    left, top, right, bottom,
                    searchQuery, matcher);
            amenities.addAll(result);
        }

        return amenities;
    }

    private List<BinaryMapDataObject> searchBinaryMapDataForAmenity(Amenity amenity, int limit) {
        long osmId = ObfConstants.getOsmObjectId(amenity);
        boolean checkId = osmId > 0;
        String wikidata = amenity.getWikidata();
        boolean checkWikidata = !Algorithms.isEmpty(wikidata);

        ResultMatcher<BinaryMapDataObject> matcher = new ResultMatcher<>() {
            @Override
            public boolean publish(BinaryMapDataObject object) {
                if (checkId && osmId == ObfConstants.getOsmObjectId(object)) {
                    return true;
                }
                if (checkWikidata) {
                    TIntObjectHashMap<String> names = object.getObjectNames();
                    return names != null && !names.isEmpty() && names.containsValue(wikidata);
                }
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        return searchBinaryMapDataObjects(amenity.getLocation(), matcher, limit);
    }

    private List<BinaryMapDataObject> searchBinaryMapDataObjects(LatLon latLon,
                                                                 ResultMatcher<BinaryMapDataObject> matcher, int limit) {
        List<BinaryMapDataObject> list = new ArrayList<>();

        int y = MapUtils.get31TileNumberY(latLon.getLatitude());
        int x = MapUtils.get31TileNumberX(latLon.getLongitude());

        BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> request = BinaryMapIndexReader.buildSearchRequest(x,
                x + 1, y, y + 1, 15, null, new ResultMatcher<>() {
                    @Override
                    public boolean publish(BinaryMapDataObject object) {
                        if (matcher == null || matcher.publish(object)) {
                            list.add(object);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean isCancelled() {
                        return matcher != null && matcher.isCancelled()
                                || limit != -1 && list.size() == limit;
                    }
                });

        for (AmenityIndexRepository repository : getAmenityRepositories(false)) {
            if (matcher != null && matcher.isCancelled()) {
                break;
            }
            if (repository.isPoiSectionIntersects(request)) {
                repository.searchMapIndex(request);
            }
        }
        return list;
    }

    public void searchDetailsAmenityAsync(LatLon latLon, long id, Collection<String> names, String wikidata, CallbackWithObject<Amenity> callbackWithAmenity) {
        singleThreadedExecutor.submit(() -> {
            Amenity amenity = searchDetailsAmenity(latLon, id, names, wikidata);
            callbackWithAmenity.processResult(amenity);
        });
    }

    public void searchBaseDetailsObjectAsync(RenderedObject renderedObject, CallbackWithObject<BaseDetailsObject> callback) {
        LatLon latLon = renderedObject.getLatLon();
        if (latLon == null) {
            callback.processResult(null);
            return;
        }
        final LatLon finalLatLon = latLon;
        singleThreadedExecutor.submit(() -> {
            String wikidata = renderedObject.getTagValue(Amenity.WIKIDATA);
            long osmId = ObfConstants.getOsmObjectId(renderedObject);
            BaseDetailsObject detailsObject = searchDetailsObject(finalLatLon,
                    osmId << AMENITY_ID_RIGHT_SHIFT, renderedObject.getOriginalNames(), wikidata);
            if (detailsObject != null) {
                Amenity amenity = detailsObject.getSyntheticAmenity();
                amenity.setX(renderedObject.getX());
                amenity.setY(renderedObject.getY());
            }
            callback.processResult(detailsObject);
        });
    }

    public void searchDetailsObjectAsync(Object object, CallbackWithObject<Object> callback) {
        singleThreadedExecutor.submit(() -> {
            Object fetched = searchDetailsObject(object);
            callback.processResult(fetched == null ? object : fetched);
        });
    }

    private void completeGeometry(BaseDetailsObject detailsObject, Object object) {
        if (detailsObject == null) {
            return;
        }
        TIntArrayList xx = null;
        TIntArrayList yy = null;
        if (object instanceof Amenity amenity) {
            xx = amenity.getX();
            yy = amenity.getY();
        }
        if (object instanceof RenderedObject renderedObject) {
            xx = renderedObject.getX();
            yy = renderedObject.getY();
        }
        if (object instanceof BaseDetailsObject base) {
            xx = base.getSyntheticAmenity().getX();
            yy = base.getSyntheticAmenity().getY();
        }
        if (xx != null && yy != null && !xx.isEmpty()) {
            detailsObject.setX(xx);
            detailsObject.setY(yy);
        } else {
            List<BinaryMapDataObject> dataObjects = searchBinaryMapDataForAmenity(detailsObject.getSyntheticAmenity(), 1);
            for (BinaryMapDataObject dataObject : dataObjects) {
                if (copyCoordinates(detailsObject, dataObject)) {
                    break;
                }
            }
        }
    }

    private boolean copyCoordinates(BaseDetailsObject detailsObject, BinaryMapDataObject mapObject) {
        int pointsLength = mapObject.getPointsLength();
        for (int i = 0; i < pointsLength; i++) {
            detailsObject.addX(mapObject.getPoint31XTile(i));
            detailsObject.addY(mapObject.getPoint31YTile(i));
        }
        return pointsLength > 0;
    }
}