package net.osmand.search.core.spatial;

import net.osmand.data.*;
import java.util.*;

public class SpatialSearchDeduplication {

    SpatialSearchContext ctx;
    private static final Set<String> FILTER_DUPLICATE_POI_SUBTYPE = new TreeSet<String>(
            Arrays.asList("building", "internet_access_yes"));
    private static final int DEPTH_TO_CHECK_SAME_SEARCH_RESULTS = 20;
    
    public SpatialSearchDeduplication(SpatialSearchContext ctx) {
        this.ctx = ctx;
    }

    public void uniteSearchResultsByOsmIdOrWikidata(List<SpatialSearchResult> input) {
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
    
    private String getObjectType(SpatialSearchResult searchResult) {
        if (searchResult.objs.isEmpty())
            return "";
        SpatialSearchResult.SpatialSearchResultRef first = searchResult.objs.get(0);
        return first.atom.typeStr();
    }
}
