package net.osmand.search;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE;
import static net.osmand.CollatorStringMatcher.StringMatcherMode.MULTISEARCH;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

import net.osmand.CallbackWithObject;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.NativeLibrary;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.search.core.AmenityIndexRepository;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import gnu.trove.map.hash.TIntObjectHashMap;

public class FullAmenitySearch {

    protected final Map<String, AmenityIndexRepository> amenityRepositories = new ConcurrentHashMap<>();
    private boolean progress;
    private final TravelFileVisibility travelFileVisibility;
    private ThreadPoolExecutor singleThreadedExecutor;
    private LinkedBlockingQueue<Runnable> taskQueue;

    private static final int AMENITY_SEARCH_RADIUS = 50;
    private static final int AMENITY_SEARCH_RADIUS_FOR_RELATION = 500;
    private final String lang;

    public FullAmenitySearch(boolean progress, TravelFileVisibility travelFileVisibility, String lang) {
        this.progress = progress;
        this.travelFileVisibility = travelFileVisibility;
        this.lang = lang;
        taskQueue = new LinkedBlockingQueue<Runnable>();
        singleThreadedExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, taskQueue);
    }

    public interface TravelFileVisibility {
        public boolean getTravelFileVisibility(String fileName);
    }
    public List<AmenityIndexRepository> getAmenityRepositories(boolean includeTravel) {
        List<String> fileNames = new ArrayList<>(amenityRepositories.keySet());
        List<AmenityIndexRepository> baseMaps = new ArrayList<>();
        List<AmenityIndexRepository> result = new ArrayList<>();

        fileNames.sort(Algorithms.getStringVersionComparator());

        for (String fileName : fileNames) {
            if (fileName.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
                if (!includeTravel || !travelFileVisibility.getTravelFileVisibility(fileName)) {
                    continue;
                }
            }
            AmenityIndexRepository r = amenityRepositories.get(fileName);
            if (r != null && r.isWorldMap()) {
                baseMaps.add(r);
            } else if (r != null) {
                result.add(r);
            }
        }

        result.addAll(baseMaps);
        return result;
    }

    public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter, QuadRect rect,
                                         boolean includeTravel) {
        return searchAmenities(filter, null, rect.top, rect.left, rect.bottom, rect.right, -1, includeTravel, null);
    }

    public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter, double top,
                                         double left, double bottom,
                                         double right, int zoom, boolean includeTravel,
                                         ResultMatcher<Amenity> matcher) {
        return searchAmenities(filter, null, top, left, bottom, right, zoom, includeTravel, matcher);
    }

    public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter,
                                         BinaryMapIndexReader.SearchPoiAdditionalFilter additionalFilter, double topLatitude,
                                         double leftLongitude, double bottomLatitude,
                                         double rightLongitude, int zoom, boolean includeTravel,
                                         ResultMatcher<Amenity> matcher) {

        Set<Long> openAmenities = new HashSet<>();
        Set<Long> closedAmenities = new HashSet<>();
        List<Amenity> actualAmenities = new ArrayList<>();

        progress = true;
        try {
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
                        progress = false;
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
        } finally {
            progress = false;
        }
        return actualAmenities;
    }

    public Amenity findAmenity(LatLon latLon, Long id, Collection<String> names, String wikidata) {
        BaseDetailsObject detail = findPlaceDetails(latLon, id, names, wikidata);
        if (detail != null) {
            Amenity amenity = detail.getSyntheticAmenity();
            amenity.setContainsFullInfo(true);
            return amenity;
        }
        return null;
    }

    public BaseDetailsObject findPlaceDetails(LatLon latLon, Long obId, Collection<String> names, String wikidata) {
        long id = obId == null ? -1 : obId;
        int searchRadius = ObfConstants.isIdFromRelation(id >> AMENITY_ID_RIGHT_SHIFT)
                ? AMENITY_SEARCH_RADIUS_FOR_RELATION
                : AMENITY_SEARCH_RADIUS;
        QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), searchRadius);
        List<Amenity> amenities = searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, true);
        long osmId = ObfConstants.getOsmId(id >> AMENITY_ID_RIGHT_SHIFT);
        List<Amenity> filtered = new ArrayList<>();
        if (osmId > 0 || wikidata != null) {
            filtered = findAmenitiesByOsmIdOrWikidata(amenities, osmId, latLon, wikidata);
        }
        if (Algorithms.isEmpty(filtered) && !Algorithms.isEmpty(names)) {
            Amenity amenity = findAmenityByName(amenities, names);
            if (amenity != null) {
                filtered = findAmenitiesByOsmIdOrWikidata(amenities, amenity.getOsmId(), amenity.getLocation(), amenity.getWikidata());
            }
        }
        if (!Algorithms.isEmpty(filtered)) {
            BaseDetailsObject detailObj = new BaseDetailsObject(filtered.get(0), lang);
            for (int i = 1; i < filtered.size(); i++) {
                detailObj.addObject(filtered.get(i));
                detailObj.combineData();
            }
            return detailObj;
        }
        return null;
    }

    private List<Amenity> findAmenitiesByOsmIdOrWikidata(Collection<Amenity> amenities, long id, LatLon point, String wikidata) {
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

    public static Amenity findAmenityByName(Collection<Amenity> amenities,
                                            Collection<String> names) {
        if (!Algorithms.isEmpty(names) && !Algorithms.isEmpty(amenities)) {
            return amenities.stream()
                    .filter(amenity -> !amenity.isClosed())
                    .filter(amenity -> names.contains(amenity.getName()))
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
                                                    .filter(amenity -> names.contains(amenity.toStringEn()))
                                                    .findAny().orElse(null)));
        }
        return null;
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
        progress = true;
        List<Amenity> amenities = new ArrayList<>();
        try {
            if (locations != null && locations.size() > 0) {
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
        } finally {
            progress = false;
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

    public List<BinaryMapDataObject> searchBinaryMapDataForAmenity(Amenity amenity, int limit) {
        String name = amenity.getName();
        long osmId = ObfConstants.getOsmObjectId(amenity);
        boolean checkId = osmId != -1;
        boolean checkName = !Algorithms.isEmpty(name);

        ResultMatcher<BinaryMapDataObject> matcher = new ResultMatcher<>() {
            @Override
            public boolean publish(BinaryMapDataObject object) {
                if (checkId && osmId == ObfConstants.getOsmObjectId(object)) {
                    return true;
                }
                if (checkName) {
                    TIntObjectHashMap<String> names = object.getObjectNames();
                    return names != null && !names.isEmpty() && names.containsValue(name);
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

    public void searchAmenityAsync(LatLon latLon, long id, Collection<String> names, String wikidata, CallbackWithObject<Amenity> callbackWithAmenity) {
        singleThreadedExecutor.submit(() -> {
            Amenity amenity = findAmenity(latLon, id, names, wikidata);
            callbackWithAmenity.processResult(amenity);
        });
    }

    public void searchAmenityAsync(NativeLibrary.RenderedObject renderedObject, CallbackWithObject<Amenity> callbackWithAmenity) {
        LatLon latLon = renderedObject.getLabelLatLon();
        if (latLon == null && renderedObject.getLabelX() != 0) {
            latLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getLabelY()), MapUtils.get31LongitudeX(renderedObject.getLabelX()));
        }
        if (latLon == null && !renderedObject.getX().isEmpty()) {
            latLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)), MapUtils.get31LongitudeX(renderedObject.getX().get(0)));
        }
        if (latLon == null) {
            callbackWithAmenity.processResult(null);
            return;
        }
        final LatLon finalLatLon = latLon;
        singleThreadedExecutor.submit(() -> {
            String wikidata = renderedObject.getTagValue(Amenity.WIKIDATA);
            Amenity amenity = findAmenity(finalLatLon, renderedObject.getId(), null, wikidata);
            callbackWithAmenity.processResult(amenity);
        });
    }

}
