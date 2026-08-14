package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.search.core.SearchResult.SearchResultResource;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.*;

import gnu.trove.list.array.TIntArrayList;

public class BaseDetailsObject {

	private static final int MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS = 30;

	private final Set<Long> osmIds = new HashSet<>();
	private final Set<String> wikidataIds = new HashSet<>();
	private final List<Object> objects = new ArrayList<>();

	private final String lang;

	private String obfResourceName;
	private SearchResultResource searchResultResource;

	protected MapObject syntheticMapObject = null;
	private int[] bbox31;

	private ObjectCompleteness objectCompleteness = ObjectCompleteness.EMPTY;

	private enum ObjectCompleteness {
		EMPTY,
		COMBINED,
		FULL
	}

	public BaseDetailsObject(String lang) {
		this.lang = lang;
	}

	public BaseDetailsObject(Object object, String lang) {
		this(Algorithms.isEmpty(lang) ? "en" : lang);
		addObject(object);
	}

	public BaseDetailsObject(List<? extends MapObject> mapObjects, String lang) {
		this(Algorithms.isEmpty(lang) ? "en" : lang);
		boolean containsAmenity = false;
		for (MapObject mo : mapObjects) {
			addObjectNoCombine(mo);
			if (mo instanceof Amenity) {
				containsAmenity = true;
			}
		}
		combineData();
		if (!objects.isEmpty()) {
			objectCompleteness = containsAmenity ? ObjectCompleteness.FULL : ObjectCompleteness.COMBINED;
		}
	}

	public MapObject getSyntheticMapObject() {
		return syntheticMapObject;
	}
	
	public Amenity getSyntheticAmenity() {
		if (syntheticMapObject instanceof Amenity am) {
			return am;
		}
		return null;
	}

	public LatLon getLocation() {
		return syntheticMapObject.getLocation();
	}

	public List<Object> getObjects() {
		return objects;
	}

	public boolean isObjectFull() {
		return objectCompleteness == ObjectCompleteness.FULL;
	}

	public boolean isObjectCombined() {
		return objectCompleteness == ObjectCompleteness.COMBINED;
	}

	public boolean isObjectEmpty() {
		return objectCompleteness == ObjectCompleteness.EMPTY;
	}

	public boolean addObject(Object object) {
		boolean added = addObjectNoCombine(object);
		if (added) {
			combineData();
		}
		return added;
	}
	
	private boolean addObjectNoCombine(Object object) {
		if (bbox31 == null && object instanceof City c) {
			bbox31 = c.getBbox31();
		} else if (bbox31 == null && object instanceof Street s) {
			QuadRect bb = s.getBboxPoints();
			if (bb != null) {
				bbox31 = new int[] { MapUtils.get31TileNumberX(bb.left), MapUtils.get31TileNumberY(bb.top),
						MapUtils.get31TileNumberX(bb.right), MapUtils.get31TileNumberY(bb.bottom) };
			}
		}
		if (!isSupportedObjectType(object)) {
			return false;
		}
		if (object instanceof BaseDetailsObject detailsObject) {
			for (Object obj : detailsObject.getObjects()) {
				addObject(obj);
			}
		} else {
			objects.add(object);

			Long osmId = getOsmId(object);
			String wikidata = getWikidata(object);

			if (osmId != null && osmId != -1) {
				osmIds.add(osmId);
			}
			if (!Algorithms.isEmpty(wikidata)) {
				wikidataIds.add(wikidata);
			}
		}
		return true;
	}

	protected String getWikidata(Object object) {
		if (object instanceof Amenity amenity) {
			return amenity.getWikidata();
		} else if (object instanceof TransportStop transportStop) {
			Amenity amenity = transportStop.getAmenity();
			return amenity != null ? amenity.getWikidata() : null;
		} else if (object instanceof RenderedObject renderedObject) {
			return renderedObject.getTagValue(WIKIDATA);
		} else if (object instanceof MapObject mapObject) {
			return mapObject.getWikidata();
		}
		return null;
	}

	private Long getOsmId(Object object) {
		if (object instanceof Amenity amenity) {
			return amenity.getOsmId();
		}
		if (object instanceof MapObject mapObject) {
			return ObfConstants.getOsmObjectId(mapObject);
		}
		return null;
	}

	public boolean overlapsWith(Object object) {
		Long osmId = getOsmId(object);
		String wikidata = getWikidata(object);

		boolean osmIdEqual = osmId != null && osmId != -1 && osmIds.contains(osmId);
		boolean wikidataEqual = !Algorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata);

		if (osmIdEqual || wikidataEqual) {
			return true;
		}
		if (object instanceof RenderedObject renderedObject) {
			List<TransportStop> stops = getTransportStops();
			return overlapPublicTransport(Collections.singletonList(renderedObject), stops);
		}
		if (object instanceof TransportStop transportStop) {
			List<RenderedObject> renderedObjects = getRenderedObjects();
			return overlapPublicTransport(renderedObjects, Collections.singletonList(transportStop));
		}
		return false;
	}

	private boolean overlapPublicTransport(List<RenderedObject> renderedObjects,
			List<TransportStop> stops) {
		for (RenderedObject renderedObject : renderedObjects) {
			if (overlapPublicTransport(renderedObject, stops)) {
				return true;
			}
		}
		return false;
	}

	private boolean overlapPublicTransport(RenderedObject renderedObject,
			List<TransportStop> stops) {
		List<String> transportTypes = MapPoiTypes.getDefault().getPublicTransportTypes();
		if (Algorithms.isEmpty(stops) || Algorithms.isEmpty(transportTypes)) {
			return false;
		}
		Map<String, String> tags = renderedObject.getTags();
		String name = renderedObject.getName();
		if (!Algorithms.isEmpty(name)) {
			boolean namesEqual = false;
			for (TransportStop stop : stops) {
				if (stop.getName().contains(name) || name.contains(stop.getName())) {
					namesEqual = true;
					break;
				}
			}
			if (!namesEqual) {
				return false;
			}
		}
		boolean isStop = false;
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			String tag = entry.getKey();
			String value = entry.getValue();
			if (transportTypes.contains(value) || transportTypes.contains(tag + "_" + value)) {
				isStop = true;
				break;
			}
		}
		if (isStop) {
			for (TransportStop stop : stops) {
				if (MapUtils.getDistance(stop.getLocation(), renderedObject.getLatLon()) < MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS) {
					return true;
				}
			}
		}
		return false;
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
			String wikidata = amenity.getWikidata();
			if (wikidata != null)
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

	private void combineData() {
		if (isAddressType()) {
			syntheticMapObject = getAddressObject();
		} else {
			Amenity synthetic = new Amenity();
			synthetic.setBbox31(bbox31);
			syntheticMapObject = synthetic;
		}
		sortObjects();
		for (Object object : objects) {
			mergeObject(object, objects.size() == 1);
		}
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			if (this.objectCompleteness.ordinal() < ObjectCompleteness.FULL.ordinal()) {
				this.objectCompleteness = syntheticAmenity.getType() == null ? ObjectCompleteness.EMPTY : ObjectCompleteness.COMBINED;
			}
			if (syntheticAmenity.getType() == null) {
				syntheticAmenity.setType(MapPoiTypes.getDefault().getUserDefinedCategory());
				syntheticAmenity.setSubType("");
				this.objectCompleteness = ObjectCompleteness.EMPTY;
			}
		}
	}

	protected void mergeObject(Object object, boolean isSingleObject) {
		if (object instanceof Amenity amenity) {
			processAmenity(amenity, isSingleObject);
		} else if (object instanceof TransportStop transportStop) {
			Amenity amenity = transportStop.getAmenity();
			if (amenity != null) {
				processAmenity(amenity, isSingleObject);
			} else {
				processId(transportStop);
				syntheticMapObject.copyNames(transportStop);
				if (syntheticMapObject.getLocation() == null) {
					syntheticMapObject.setLocation(transportStop.getLocation());
				}
			}
		} else if (object instanceof RenderedObject renderedObject) {
			EntityType type = ObfConstants.getOsmEntityType(renderedObject);
			if (type != null) {
				long osmId = ObfConstants.getOsmObjectId(renderedObject);
				long objectId = ObfConstants.createMapObjectIdFromCleanOsmId(osmId, type);

				if (syntheticMapObject.getId() == null && objectId > 0) {
					syntheticMapObject.setId(objectId);
				}
			}
			if (syntheticMapObject instanceof Amenity syntheticAmenity && syntheticAmenity.getType() == null) {
				Amenity amenity = convertRenderedObjectToAmenity(renderedObject, MapPoiTypes.getDefault());
				syntheticAmenity.setType(amenity.getType());
				syntheticAmenity.setSubType(amenity.getSubType());
				syntheticAmenity.copyAdditionalInfo(renderedObject.getTags(), false);
			}
			syntheticMapObject.copyNames(renderedObject);
			if (syntheticMapObject.getLocation() == null) {
				syntheticMapObject.setLocation(renderedObject.getLocation());
			}
			if (syntheticMapObject.getLocation() == null) {
				syntheticMapObject.setLocation(renderedObject.getLabelLatLon());
			}
			processPolygonCoordinates(renderedObject.getX(), renderedObject.getY());
		}
	}

	protected void processId(MapObject object) {
		processId(syntheticMapObject, object);
	}

	protected static void processId(MapObject syntheticMapObject, MapObject object) {
		if (syntheticMapObject.getId() == null && ObfConstants.isOsmUrlAvailable(object)) {
			syntheticMapObject.setId(object.getId());
		}
	}

	private static void updateAmenitySubTypes(Amenity amenity, String subTypesToAdd) {
		if (amenity.getSubType() == null) {
			amenity.setSubType(subTypesToAdd);
		} else {
			for (String subType : subTypesToAdd.split(";")) {
				boolean isSubTypeUnique = true;
				for (String s : amenity.getSubType().split(";")) {
					if (s.equals(subType)) {
						isSubTypeUnique = false;
						break;
					}
				}
				if (isSubTypeUnique) {
					amenity.setSubType(amenity.getSubType() + ";" + subType);
				}
			}
		}
	}

	protected void processAmenity(Amenity amenity, boolean isSingleObject) {
		mergeAmenityData(syntheticMapObject, amenity, lang, isSingleObject);
	}

	public static void mergeAmenityData(MapObject syntheticMapObject, Amenity amenity, String lang, boolean isSingleObject) {
		processId(syntheticMapObject, amenity);

		LatLon location = amenity.getLocation();
		if (syntheticMapObject.getLocation() == null && location != null) {
			syntheticMapObject.setLocation(location);
		}
		syntheticMapObject.copyNames(amenity);
		PoiCategory type = amenity.getType();
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			if (syntheticAmenity.getType() == null && type != null) {
				syntheticAmenity.setType(type);
			}
			String subType = amenity.getSubType();
			if (subType != null && !Algorithms.stringsEqual(subType, syntheticAmenity.getSubType())) {
				updateAmenitySubTypes(syntheticAmenity, subType);
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
			boolean shouldCopyAdditionalInfo = getResourceType(amenity) != SearchResultResource.TRAVEL
					|| getLangForTravel(amenity).equals(lang); // avoid articles in another language
			if (isSingleObject || shouldCopyAdditionalInfo) {
				syntheticAmenity.copyAdditionalInfo(amenity, false);
			}
			processPolygonCoordinates(syntheticAmenity, amenity.getX(), amenity.getY());
	
			Set<String> contentLocales = amenity.getSupportedContentLocales();
			if (!Algorithms.isEmpty(contentLocales)) {
				syntheticAmenity.updateContentLocales(contentLocales);
			}
		}
	}

	private void processPolygonCoordinates(TIntArrayList x, TIntArrayList y) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			processPolygonCoordinates(syntheticAmenity, x, y);
		}
	}

	private static void processPolygonCoordinates(Amenity syntheticAmenity, TIntArrayList x, TIntArrayList y) {
		if (syntheticAmenity.getX().isEmpty() && !x.isEmpty()) {
			syntheticAmenity.getX().addAll(x);
		}
		if (syntheticAmenity.getY().isEmpty() && !y.isEmpty()) {
			syntheticAmenity.getY().addAll(y);
		}
	}

	public void processPolygonCoordinates(Object object) {
		if (object instanceof Amenity amenity) {
			processPolygonCoordinates(amenity.getX(), amenity.getY());
		}
		if (object instanceof RenderedObject renderedObject) {
			processPolygonCoordinates(renderedObject.getX(), renderedObject.getY());
		}
	}

	private void sortObjects() {
		sortObjectsByLang();
		sortObjectsByResourceType();
		sortObjectsByClass();
	}

	private void sortObjectsByLang() {
		objects.sort((o1, o2) -> {
			String l1 = getLangForTravel(o1);
			String l2 = getLangForTravel(o2);

			boolean preferred1 = Algorithms.stringsEqual(l1, lang);
			boolean preferred2 = Algorithms.stringsEqual(l2, lang);
			if (preferred1 == preferred2) {
				return 0;
			}
			return preferred1 ? -1 : 1;
		});
	}

	private void sortObjectsByResourceType() {
		objects.sort((o1, o2) -> {
			int ord1 = getResourceType(o1).ordinal();
			int ord2 = getResourceType(o2).ordinal();
			if (ord1 != ord2) {
				return ord2 > ord1 ? -1 : 1;
			}
			return 0;
		});
	}

	private void sortObjectsByClass() {
		objects.sort((o1, o2) -> {
			int ord1 = getClassOrder(o1);
			int ord2 = getClassOrder(o2);
			if (ord1 != ord2) {
				return ord2 > ord1 ? -1 : 1;
			}
			return 0;
		});
	}

	public void setObfResourceName(String obfName) {
		obfResourceName = obfName;
	}

	public SearchResultResource getResourceType() {
		if (searchResultResource == null && syntheticMapObject instanceof Amenity syntheticAmenity) {
			searchResultResource = findObfType(obfResourceName, syntheticAmenity);
		}
		return searchResultResource;
	}

	public String getLang() {
		return lang;
	}

	public void setMapIconName(String mapIconName) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.setMapIconName(mapIconName);
		}
	}

	public void setX(TIntArrayList x) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.getX().addAll(x);
		}
	}

	public void setY(TIntArrayList y) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.getY().addAll(y);
		}
	}

	public void addX(int x) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.getX().add(x);
		}
	}

	public void addY(int y) {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.getY().add(y);
		}
	}

	public boolean hasGeometry() {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			return !syntheticAmenity.getX().isEmpty() && !syntheticAmenity.getY().isEmpty();
		}
		return false;
	}

	public int getPointsLength() {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			return syntheticAmenity.getX().size();
		}
		return 0;
	}

	public void clearGeometry() {
		if (syntheticMapObject instanceof Amenity syntheticAmenity) {
			syntheticAmenity.getY().clear();
			syntheticAmenity.getX().clear();
		}
	}

	protected boolean isSupportedObjectType(Object object) {
		return object instanceof MapObject || object instanceof BaseDetailsObject;
	}

	public List<Amenity> getAmenities() {
		List<Amenity> amenities = new ArrayList<>();
		for (Object object : objects) {
			if (object instanceof Amenity amenity) {
				amenities.add(amenity);
			}
		}
		return amenities;
	}

	public List<TransportStop> getTransportStops() {
		List<TransportStop> stops = new ArrayList<>();
		for (Object object : objects) {
			if (object instanceof TransportStop transportStop) {
				stops.add(transportStop);
			}
		}
		return stops;
	}

	public List<RenderedObject> getRenderedObjects() {
		List<RenderedObject> renderedObjects = new ArrayList<>();
		for (Object object : objects) {
			if (object instanceof RenderedObject renderedObject) {
				renderedObjects.add(renderedObject);
			}
		}
		return renderedObjects;
	}

	private static SearchResultResource findObfType(String obfResourceName, MapObject mapObject) {
		if (obfResourceName != null && obfResourceName.contains("basemap")) {
			return SearchResultResource.BASEMAP;
		}
		if (obfResourceName != null && (obfResourceName.contains("travel") || obfResourceName.contains("wikivoyage"))) {
			return SearchResultResource.TRAVEL;
		}
		if (mapObject instanceof Amenity amenity && amenity.getType().isWiki()) {
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

	public static String getLangForTravel(Object object) {
		Amenity amenity = null;
		if (object instanceof Amenity) {
			amenity = (Amenity) object;
		}
		if (object instanceof BaseDetailsObject) {
			if (((BaseDetailsObject) object).syntheticMapObject instanceof Amenity am) {
				amenity = am;
			}
		}
		if (amenity != null && getResourceType(object) == SearchResultResource.TRAVEL) {
			String lang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
			if (lang != null) {
				return lang;
			}
		}
		return "en";
	}

	private static int getClassOrder(Object object) {
		if (object instanceof BaseDetailsObject) {
			return 1;
		}
		if (object instanceof Amenity) {
			return 2;
		}
		if (object instanceof TransportStop) {
			return 3;
		}
		if (object instanceof RenderedObject) {
			return 4;
		}
		if (object instanceof MapObject) {
			return 2;
		}
		return 5;
	}

	@Override
	public String toString() {
		if (isAddressType()) {
			return objects.get(0).toString();
		}
		return getSyntheticMapObject().toString();
	}

	public static Amenity convertRenderedObjectToAmenity(RenderedObject renderedObject, MapPoiTypes mapPoiTypes) {
		Amenity am = new Amenity();
		am.setType(mapPoiTypes.getOtherPoiCategory());
		am.setSubType("");
		MapPoiTypes.PoiTranslator poiTranslator = mapPoiTypes.getPoiTranslator();
		PoiType pt = null;
		PoiType otherPt = null;
		String subtype = null;
		Map<String, String> additionalInfo = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : renderedObject.getTags().entrySet()) {
			String tag = e.getKey();
			String value = e.getValue();
			if (tag.equals("name")) {
				am.setName(value);
				continue;
			}
			if (e.getKey().startsWith("name:")) {
				am.setName(tag.substring("name:".length()), value);
				continue;
			}
			if (tag.equals("amenity")) {
				if (pt != null) {
					otherPt = pt;
				}
				pt = mapPoiTypes.getPoiTypeByKey(value);
			} else {
				PoiType poiType = mapPoiTypes.getPoiTypeByKey(e.getKey() + "_" + e.getValue());
				if (poiType == null) {
					poiType = mapPoiTypes.getPoiTypeByKey(e.getKey());
				}
				if (poiType != null) {
					otherPt = pt != null ? poiType : otherPt;
					subtype = pt == null ? value : subtype;
					pt = pt == null ? poiType : pt;
				}
			}
			if (Algorithms.isEmpty(value) && otherPt == null) {
				otherPt = mapPoiTypes.getPoiTypeByKey(tag);
			}
			if (otherPt == null) {
				PoiType poiType = mapPoiTypes.getPoiTypeByKey(value);
				if (poiType != null && poiType.getOsmTag().equals(tag)) {
					otherPt = poiType;
				}
			}
			if (!Algorithms.isEmpty(value)) {
				String translate = poiTranslator.getTranslation(tag + "_" + value);
				String translate2 = poiTranslator.getTranslation(value);
				if (translate != null && translate2 != null) {
					additionalInfo.put(translate, translate2);
				} else {
					additionalInfo.put(tag, value);
				}
			}
		}
		if (pt != null) {
			am.setType(pt.getCategory());
		} else if (otherPt != null) {
			am.setType(otherPt.getCategory());
			am.setSubType(otherPt.getKeyName());
		}
		if (subtype != null) {
			am.setSubType(subtype);
		}
		Entity.EntityType type = ObfConstants.getOsmEntityType(renderedObject);
		if (type != null) {
			long osmId = ObfConstants.getOsmObjectId(renderedObject);
			long objectId = ObfConstants.createMapObjectIdFromCleanOsmId(osmId, type);
			am.setId(objectId);
		}
		am.setAdditionalInfo(additionalInfo);
		am.setX(renderedObject.getX());
		am.setY(renderedObject.getY());
		return am;
	}
	
	private boolean isAddressType() {
		if (objects.size() > 0) {
			Object o = objects.get(0);
			if (o instanceof Street || o instanceof City || o instanceof Building) {
				return true;
			}
		}
		return false;
	}
	
	private MapObject getAddressObject() {
		Object o = objects.get(0);
		if (o instanceof Street street) {
			return street;
		}
		if (o instanceof City city) {
			return city;
		}
		if (o instanceof Building building) {
			return building;
		}
		return null;
	}

}
