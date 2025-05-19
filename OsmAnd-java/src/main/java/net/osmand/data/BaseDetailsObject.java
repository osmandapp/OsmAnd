package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.PoiCategory;
import net.osmand.util.Algorithms;
import net.osmand.search.core.SearchResult.SearchResultResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;

public class BaseDetailsObject {

    protected final Set<Long> osmIds = new HashSet<>();
    protected final Set<String> wikidataIds = new HashSet<>();
    protected final List<Object> objects = new ArrayList<>();
    protected String obfResourceName;
    protected SearchResultResource searchResultResource;

    protected Amenity syntheticAmenity = new Amenity();
    protected final String lang;

    public BaseDetailsObject(String lang) {
        this.lang = lang;
    }

    public BaseDetailsObject(Object object, String lang) {
        addObject(object);
        combineData();
        if (lang != null) {
            this.lang = lang;
        } else {
            this.lang = "en";
        }
    }

    public Amenity getSyntheticAmenity() {
        return syntheticAmenity;
    }

    public LatLon getLocation() {
        return syntheticAmenity.getLocation();
    }

    public List<Object> getObjects() {
        return objects;
    }

    public void addObject(Object object) {
        if (shouldSkip(object)) {
            return;
        }
        objects.add(object);
        Amenity amenity = null;
        if (object instanceof BaseDetailsObject detailsObject) {
            amenity = detailsObject.syntheticAmenity;
        }
        if (object instanceof Amenity) {
            amenity = (Amenity) object;
        }
        if (amenity != null) {
            String wikidata = amenity.getWikidata();
            if (!Algorithms.isEmpty(wikidata)) {
                wikidataIds.add(wikidata);
            }
            osmIds.add(amenity.getOsmId());
        }
    }

    public boolean overlapsWith(Object object) {
        Amenity amenity = null;
        if (object instanceof BaseDetailsObject detailsObject) {
            amenity = detailsObject.syntheticAmenity;
        }
        if (object instanceof Amenity) {
            amenity = (Amenity) object;
        }
        if (amenity == null) {
            return false;
        }
        String wikidata = amenity.getWikidata();
        boolean wikidataEqual = !Algorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata);
        return osmIds.contains(amenity.getOsmId()) || wikidataEqual;
    }

    public void merge(Object object) {
        if (object instanceof BaseDetailsObject baseDetailsObject)
            merge(baseDetailsObject);
        if (object instanceof TransportStop transportStop)
            merge(transportStop);
        if (object instanceof RenderedObject renderedObject)
            merge(renderedObject);
    }

    private void merge(BaseDetailsObject other) {
        osmIds.addAll(other.osmIds);
        wikidataIds.addAll(other.wikidataIds);
        objects.addAll(other.getObjects());
    }

    private void merge(TransportStop other) {
        osmIds.add(ObfConstants.getOsmObjectId(other));
        Amenity amenity = other.getAmenity();
        if (amenity != null) {
            wikidataIds.add(amenity.getWikidata());
        }
        objects.add(other);
    }

    private void merge(RenderedObject renderedObject) {
        osmIds.add(ObfConstants.getOsmObjectId(renderedObject));
        String wikidata = renderedObject.getTagValue(WIKIDATA);
        if (!Algorithms.isEmpty(wikidata)) {
            wikidataIds.add(wikidata);
        }
        objects.add(renderedObject);
    }

    public void combineData() {
        Set<String> contentLocales = new TreeSet<>();
        syntheticAmenity = new Amenity();
        objects.sort((o1, o2) -> {
            String l1 = getLangForTravel(o1);
            String l2 = getLangForTravel(o2);
            if (l2.equals(l1)) {
                return 0;
            }
            if (l2.equals(lang)) {
                return 1;
            }
            if (l1.equals(lang)) {
                return -1;
            }
            return 0;
        });
        objects.sort((o1, o2) -> {
            int ord1 = getResourceType(o1).ordinal();
            int ord2 = getResourceType(o2).ordinal();
            if (ord1 != ord2) {
                return ord2 > ord1 ? -1 : 1;
            }
            return 0;
        });
        for (Object object : objects) {
            if (object instanceof Amenity amenity) {
                processAmenity(amenity, contentLocales);
            }
            if (object instanceof BaseDetailsObject detailsObject) {
                processAmenity(detailsObject.syntheticAmenity, contentLocales);
            }
        }
        if (!Algorithms.isEmpty(contentLocales)) {
            syntheticAmenity.updateContentLocales(contentLocales);
        }
    }

    protected void processAmenity(Amenity amenity, Set<String> contentLocales) {
        if (syntheticAmenity.getId() == null && ObfConstants.isOsmUrlAvailable(amenity)) {
            syntheticAmenity.setId(amenity.getId());
        }
        LatLon location = amenity.getLocation();
        if (syntheticAmenity.getLocation() == null && location != null) {
            syntheticAmenity.setLocation(location);
        }
        PoiCategory type = amenity.getType();
        if (syntheticAmenity.getType() == null && type != null) {
            syntheticAmenity.setType(type);
        }
        String subType = amenity.getSubType();
        if (syntheticAmenity.getSubType() == null && subType != null) {
            syntheticAmenity.setSubType(subType);
        }
        String mapIconName = amenity.getMapIconName();
        if (syntheticAmenity.getMapIconName() == null && mapIconName != null) {
            syntheticAmenity.setMapIconName(mapIconName);
        }
        String regionName = amenity.getRegionName();
        if (syntheticAmenity.getRegionName() == null && regionName != null) {
            syntheticAmenity.setRegionName(regionName);
        }
        Map<Integer, List<BinaryMapIndexReader.TagValuePair>> groups = amenity.getTagGroups();
        if (syntheticAmenity.getTagGroups() == null && groups != null) {
            syntheticAmenity.setTagGroups(new HashMap<>(groups));
        }
        int travelElo = amenity.getTravelEloNumber();
        if (syntheticAmenity.getTravelEloNumber() == DEFAULT_ELO && travelElo != DEFAULT_ELO) {
            syntheticAmenity.setTravelEloNumber(travelElo);
        }
        TIntArrayList x = amenity.getX();
        if (syntheticAmenity.getX().isEmpty() && !x.isEmpty()) {
            syntheticAmenity.getX().addAll(x);
        }
        TIntArrayList y = amenity.getY();
        if (syntheticAmenity.getY().isEmpty() && !y.isEmpty()) {
            syntheticAmenity.getY().addAll(y);
        }
        syntheticAmenity.copyNames(amenity);
        syntheticAmenity.copyAdditionalInfo(amenity, false);

        contentLocales.addAll(amenity.getSupportedContentLocales());
    }

    public static boolean shouldSkip(Object object) {
        return !(object instanceof Amenity) && !(object instanceof BaseDetailsObject);
    }

    public void setObfResourceName(String obfName) {
        obfResourceName = obfName;
    }

    public SearchResultResource getResourceType() {
        if (searchResultResource == null) {
            searchResultResource = findObfType(obfResourceName, syntheticAmenity);
        }
        return searchResultResource;
    }

    private static SearchResultResource findObfType(String obfResourceName, Amenity amenity) {
        if (obfResourceName != null && obfResourceName.contains("basemap")) {
            return SearchResultResource.BASEMAP;
        }
        if (obfResourceName != null && (obfResourceName.contains("travel") || obfResourceName.contains("wikivoyage"))) {
            return SearchResultResource.TRAVEL;
        }
        if (amenity.getType().isWiki()) {
            return SearchResultResource.WIKIPEDIA;
        }
        return SearchResultResource.DETAILED;
    }


    private static SearchResultResource getResourceType(Object object) {
        if (object instanceof BaseDetailsObject detailsObject) {
            return detailsObject.getResourceType();
        }
        if (object instanceof Amenity amenity) {
            return findObfType(amenity.getRegionName(), amenity);
         }
        return SearchResultResource.DETAILED;
    }

    private static String getLangForTravel(Object object) {
        Amenity amenity = null;
        if (object instanceof Amenity) {
            amenity = (Amenity) object;
        }
        if (object instanceof BaseDetailsObject) {
            amenity = ((BaseDetailsObject) object).syntheticAmenity;
        }
        if (amenity != null && getResourceType(object) == SearchResultResource.TRAVEL) {
            String lang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
            if (lang != null) {
                return lang;
            }
        }
        return "en";
    }

    public String getLang() {
        return lang;
    }

}
