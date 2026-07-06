package net.osmand.search.core.spatial;

import net.osmand.binary.ObfConstants;
import net.osmand.data.*;
import net.osmand.search.core.ObjectType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.*;

import static net.osmand.data.Amenity.ROUTE_ID;

public class SpatialSearchDeduplication {

    SpatialSearchContext ctx;
    private static final Set<String> FILTER_DUPLICATE_POI_SUBTYPE = new TreeSet<String>(
            Arrays.asList("building", "internet_access_yes"));
    private static final int DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20;
    
    public SpatialSearchDeduplication(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }
    
    public void uniteSimilarAndRemoveDuplicate(List<SpatialSearchResult> input) {
        uniteSearchResultsByOsmIdOrWikidata(input);
        filterSearchDuplicateResults(input);
    }

    private void uniteSearchResultsByOsmIdOrWikidata(List<SpatialSearchResult> input) {
        List<SpatialSearchResult> output = new ArrayList<>();
        Map<Long, Integer> osmIdMap = new HashMap<>();
        Map<String, Integer> wikidataMap = new HashMap<>();
        Map<String, Integer> routeIdMap = new HashMap<>();
        Map<Integer, List<SpatialSearchResult>> copyDataMap = new HashMap<>();
        for (SpatialSearchResult spatial : input) {
            MapObject object = getMapObject(spatial);
            if (object instanceof Amenity that) {
                Long osmId = that.getOsmId();
                String wikidata = that.getWikidata();
                String routeId = that.getRouteId();

                if (osmId != null && osmId < 0) {
                    osmId = null; // do not merge synthetic osmId such as wiki
                }

                Integer foundOsmIdIndex = osmId == null ? null : osmIdMap.get(osmId);
                Integer foundWikidataIndex = wikidata == null ? null : wikidataMap.get(wikidata);
                Integer foundRouteIndex = routeId == null ? null : routeIdMap.get(routeId);

                int indexToUpdate = -1; // unique

                if (foundOsmIdIndex != null) {                    
                    indexToUpdate = foundOsmIdIndex;
                } else if (foundWikidataIndex != null) {
                    indexToUpdate = foundWikidataIndex;
                } else if (foundRouteIndex != null) {
                    indexToUpdate = foundRouteIndex;
                }

                if (indexToUpdate == -1) {
                    output.add(spatial);
                    indexToUpdate = output.size() - 1;
                } else {
                    copyDataMap.computeIfAbsent(indexToUpdate, k -> new ArrayList<>());
                    copyDataMap.get(indexToUpdate).add(spatial);
                }

                if (osmId != null) {
                    osmIdMap.put(osmId, indexToUpdate);
                }
                if (wikidata != null) {
                    wikidataMap.put(wikidata, indexToUpdate);
                }
                if (routeId != null) {
                    routeIdMap.put(routeId, indexToUpdate);
                }
            } else {
                output.add(spatial);
            }
        }
        if (!copyDataMap.isEmpty()) {
            String lang = ctx.lang;
            for (Map.Entry<Integer, List<SpatialSearchResult>> entry : copyDataMap.entrySet()) {
                List<SpatialSearchResult> duplicatedSpatial = entry.getValue();
                int indexToUpdate = entry.getKey();
                SpatialSearchResult r = output.get(indexToUpdate);
                duplicatedSpatial.add(0, r);
                duplicatedSpatial.sort((s1, s2) -> {
                    MapObject m1 = getMapObject(s1);
                    MapObject m2 = getMapObject(s2);
                    if (m1 instanceof Amenity am1 && am1.isRouteArticle() &&
                            m2 instanceof Amenity am2 && am2.isRouteArticle()) {
                        String l1 = BaseDetailsObject.getLangForTravel(am1);
                        String l2 = BaseDetailsObject.getLangForTravel(am2);
                        if (!l1.equals(l2)) {
                            return l1.equals(lang) ? -1 : 1;
                        }
                    }
                    return 0;
                });
                output.set(indexToUpdate, uniteData(duplicatedSpatial));
            }
        }
        if (input.size() != output.size()) {
            input.clear();
            input.addAll(output);
        }
    }

    private SpatialSearchResult uniteData(List<SpatialSearchResult> list) {
        SpatialSearchResult unique = list.remove(0);
        MapObject uniqMapObject = getMapObject(unique);
        BaseDetailsObject base = new BaseDetailsObject(uniqMapObject, ctx.lang);        
        boolean united = false;
        for (SpatialSearchResult iterated : list) {
            MapObject mapObject = getMapObject(iterated);
            if (uniqMapObject == mapObject) {
                continue;
            }            
            base.addObject(mapObject);
            united = true;
        }
        if (united) {
            unique.unitedObject = base;
        }
        return unique;
    }
    
    private MapObject getMapObject(SpatialSearchResult searchResult) {
        if (searchResult.objs.isEmpty())
            return null;
        SpatialSearchResult.SpatialSearchResultRef first = searchResult.objs.get(0);
        if (first.parent != null && first.parent.object != null) {
            return first.parent.object;
        }
        if (first.atom.object != null) {
            return first.atom.object;
        }
        return null;
    }
    
    private MapObject getBuilding(SpatialSearchResult searchResult) {
        if (searchResult.objs.isEmpty())
            return null;
        SpatialSearchResult.SpatialSearchResultRef first = searchResult.objs.get(0);
        if (first.atom.bldObject != null) {
            return first.atom.bldObject;
        }
        return null;
    }
    
    private ObjectType getObjectType(SpatialSearchResult searchResult) {
        if (searchResult.objs.isEmpty())
            return null;
        SpatialSearchResult.SpatialSearchResultRef first = searchResult.objs.get(0);
        if (first.atom.isPOI()) {
            return ObjectType.POI;
        }
        if (first.atom.isBuilding()) {
            return ObjectType.HOUSE;
        }
        if (first.atom.isStreet()) {
            return ObjectType.STREET;
        }
        if (first.atom.isCity()) {
            return ObjectType.CITY;
        }
        if (first.atom.isPostcode()) {
            return ObjectType.POSTCODE;
        }
        if (first.atom.isBoundary()) {
            return ObjectType.BOUNDARY;
        }
        return null;
    }

    private void filterSearchDuplicateResults(List<SpatialSearchResult> lst) {
        for (int i = 0; i < lst.size();) {
            SpatialSearchResult current = lst.get(i);
            boolean duplicate = false;
            for (int j = i - 1; j >= Math.max(i - DEPTH_TO_CHECK_SAME_SEARCH_RESULTS, 0); j--) {
                SpatialSearchResult prevAdded = lst.get(j);
                if (sameSearchResult(prevAdded, current)) {
                    duplicate = true;
                    if (ObjectType.getTypeWeight(getObjectType(current)) > ObjectType.getTypeWeight(getObjectType(prevAdded))) {
                        lst.set(j, current);
                    }
                }
            }
            if (duplicate) {
                lst.remove(i);
            } else {
                i++;
            }
        }
    }

    private boolean sameSearchResult(SpatialSearchResult r1, SpatialSearchResult r2) {
        ObjectType objectType1 = getObjectType(r1);
        ObjectType objectType2 = getObjectType(r2);
        MapObject m1 = getMapObject(r1);
        MapObject m2 = getMapObject(r2);
        if (m1 == null || m2 == null) {
            return false;
        }
        boolean isSameType = objectType1 == objectType2;
        boolean interpolated = false;
        if (isSameType) {
            MapObject b2 = getBuilding(r2);
            if (objectType2 == ObjectType.HOUSE && b2 instanceof Building building) {
                boolean streetEquals = m1.getName().equals(m2.getName());
                interpolated = streetEquals && building.getInterpolationType() != null;
            }
        }
        if (m1.getLocation() != null && m2.getLocation() != null) {
            if (isSameType) {
                if (objectType1 == ObjectType.STREET) {
                    Street st1 = (Street) m1;
                    Street st2 = (Street) m2;
                    return st1.getLocation().equals(st2.getLocation());
                }
            }
            Amenity a1 = null;
            if (m1 instanceof Amenity) {
                a1 = (Amenity) m1;
            }
            Amenity a2 = null;
            if (m2 instanceof Amenity) {
                a2 = (Amenity) m2;
            }
            if (m1.getName().equals(m2.getName())) {
                double similarityRadius = 30;
                if (a1 != null && a2 != null && a1.getId() != null && a2.getId() != null) {
                    // here 2 points are amenity
                    String type1 = a1.getType().getKeyName();
                    String type2 = a2.getType().getKeyName();
                    String subType1 = a1.getSubType();
                    String subType2 = a2.getSubType();

                    boolean isEqualId = ObfConstants.getOsmObjectId(a1) == ObfConstants.getOsmObjectId(a2);

                    if (isEqualId && (FILTER_DUPLICATE_POI_SUBTYPE.contains(subType1)
                            || FILTER_DUPLICATE_POI_SUBTYPE.contains(subType2))) {
                        return true;

                    } else if (!type1.equals(type2)) {
                        return false;
                    }

                    if (type1.equals("natural")) {
                        similarityRadius = 50000;
                    } else if (subType1.equals(subType2)) {
                        if (subType1.contains("cn_ref") || subType1.contains("wn_ref")
                                || (subType1.startsWith("route_hiking_") && subType1.endsWith("n_poi"))) {
                            similarityRadius = 50000;
                        }
                        if (a1.getAdditionalInfo(ROUTE_ID) != null && Algorithms.stringsEqual(a1.getAdditionalInfo(ROUTE_ID), a2.getAdditionalInfo(ROUTE_ID))) {
                            similarityRadius = 1_000_000;
                        }
                    }
                } else if (ObjectType.isAddress(objectType1) && ObjectType.isAddress(objectType2)) {
                    if (interpolated) {
                        similarityRadius = 1000;
                    } else {
                        similarityRadius = 100;
                    }
                }
                return MapUtils.getDistance(m1.getLocation(), m2.getLocation()) < similarityRadius;
            }
        } else {
            return m1.equals(m2);
        }
        return false;
    }
}
