package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.PoiCategory;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

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

    protected Amenity syntheticAmenity = new Amenity();

    public BaseDetailsObject() {
    }

    public BaseDetailsObject(Object object) {
        addObject(object);
        combineData();
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

    public void merge(BaseDetailsObject other) {
        osmIds.addAll(other.osmIds);
        wikidataIds.addAll(other.wikidataIds);
        objects.addAll(other.getObjects());
    }

    public void combineData() {
        Set<String> contentLocales = new TreeSet<>();
        syntheticAmenity = new Amenity();
        objects.sort((o1, o2) -> {
            int ord1 = getObfType(o1).ordinal();
            int ord2 = getObfType(o2).ordinal();
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

    public SearchResult.ObfType getObfType() {
        if (obfResourceName != null && obfResourceName.contains("basemap")) {
            return SearchResult.ObfType.BASEMAP;
        }
        if (obfResourceName != null && (obfResourceName.contains("travel") || obfResourceName.contains("wikivoyage"))) {
            return SearchResult.ObfType.TRAVEL;
        }
        if (syntheticAmenity.getType().isWiki()) {
            return SearchResult.ObfType.WIKIPEDIA;
        }
        return SearchResult.ObfType.DETAILED;
    }


    private static SearchResult.ObfType getObfType(Object object) {
        if (object instanceof BaseDetailsObject detailsObject) {
            return detailsObject.getObfType();
        }
        if (object instanceof Amenity amenity) {
            BaseDetailsObject baseDetailsObject = new BaseDetailsObject(amenity);
            baseDetailsObject.setObfResourceName(amenity.getRegionName());
            return baseDetailsObject.getObfType();
        }
        return SearchResult.ObfType.DETAILED;
    }

}
