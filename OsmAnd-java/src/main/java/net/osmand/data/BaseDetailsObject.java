package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.PoiCategory;
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

    protected final Amenity syntheticAmenity = new Amenity();

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
        if (object instanceof MapObject mapObject) {
            long osmId = ObfConstants.getOsmObjectId(mapObject);
            osmIds.add(osmId);
        }
        if (object instanceof Amenity amenity) {
            String wikidata = amenity.getWikidata();
            if (!Algorithms.isEmpty(wikidata)) {
                wikidataIds.add(wikidata);
            }
        }
    }

    public boolean overlapsWith(Object object) {
        Long osmId = (object instanceof MapObject) ? ObfConstants.getOsmObjectId((MapObject) object) : null;
        String wikidata = (object instanceof Amenity) ? ((Amenity) object).getWikidata() : null;

        return (osmId != null && osmIds.contains(osmId))
                || (!Algorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata));
    }

    public void merge(BaseDetailsObject other) {
        osmIds.addAll(other.osmIds);
        wikidataIds.addAll(other.wikidataIds);
        objects.addAll(other.getObjects());
    }

    public void combineData() {
        Set<String> contentLocales = new TreeSet<>();
        for (Object object : objects) {
            if (object instanceof Amenity amenity) {
                processAmenity(amenity, contentLocales);
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
        return !(object instanceof Amenity);
    }

}
