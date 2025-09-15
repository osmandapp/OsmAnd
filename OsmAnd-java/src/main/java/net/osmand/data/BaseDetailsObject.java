package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
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

	protected Amenity syntheticAmenity = new Amenity();

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

	public BaseDetailsObject(List<Amenity> amenities, String lang) {
		this(Algorithms.isEmpty(lang) ? "en" : lang);

		for (Amenity amenity : amenities) {
			addObject(amenity);
		}
		objectCompleteness = ObjectCompleteness.FULL;
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

	public boolean isObjectFull() {
		return objectCompleteness == ObjectCompleteness.FULL || objectCompleteness == ObjectCompleteness.COMBINED;
	}

	public boolean isObjectEmpty() {
		return objectCompleteness == ObjectCompleteness.EMPTY;
	}

	public boolean addObject(Object object) {
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
		combineData();
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
		syntheticAmenity = new Amenity();

		sortObjects();
		Set<String> contentLocales = new TreeSet<>();
		for (Object object : objects) {
			mergeObject(object, contentLocales, objects.size() == 1);
		}
		if (!Algorithms.isEmpty(contentLocales)) {
			syntheticAmenity.updateContentLocales(contentLocales);
		}
		if (this.objectCompleteness.ordinal() < ObjectCompleteness.FULL.ordinal()) {
			this.objectCompleteness = syntheticAmenity.getType() == null ? ObjectCompleteness.EMPTY : ObjectCompleteness.COMBINED;
		}
		if (syntheticAmenity.getType() == null) {
			syntheticAmenity.setType(MapPoiTypes.getDefault().getUserDefinedCategory());
			syntheticAmenity.setSubType("");
			this.objectCompleteness = ObjectCompleteness.EMPTY;
		}
	}

	protected void mergeObject(Object object, Set<String> contentLocales, boolean isSingleObject) {
		if (object instanceof Amenity amenity) {
			processAmenity(amenity, contentLocales, isSingleObject);
		} else if (object instanceof TransportStop transportStop) {
			Amenity amenity = transportStop.getAmenity();
			if (amenity != null) {
				processAmenity(amenity, contentLocales, isSingleObject);
			} else {
				processId(transportStop);
				syntheticAmenity.copyNames(transportStop);
				if (syntheticAmenity.getLocation() == null) {
					syntheticAmenity.setLocation(transportStop.getLocation());
				}
			}
		} else if (object instanceof RenderedObject renderedObject) {
			EntityType type = ObfConstants.getOsmEntityType(renderedObject);
			if (type != null) {
				long osmId = ObfConstants.getOsmObjectId(renderedObject);
				long objectId = ObfConstants.createMapObjectIdFromOsmId(osmId, type);

				if (syntheticAmenity.getId() == null && objectId > 0) {
					syntheticAmenity.setId(objectId);
				}
			}
			if (syntheticAmenity.getType() == null) {
				syntheticAmenity.copyAdditionalInfo(renderedObject.getTags(), false);
			}
			syntheticAmenity.copyNames(renderedObject);
			if (syntheticAmenity.getLocation() == null) {
				syntheticAmenity.setLocation(renderedObject.getLocation());
			}
			processPolygonCoordinates(renderedObject.getX(), renderedObject.getY());
		}
	}

	protected void processId(MapObject object) {
		if (syntheticAmenity.getId() == null && ObfConstants.isOsmUrlAvailable(object)) {
			syntheticAmenity.setId(object.getId());
		}
	}

	protected void processAmenity(Amenity amenity, Set<String> contentLocales, boolean isSingleObject) {
		processId(amenity);

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
		syntheticAmenity.copyNames(amenity);
		boolean shouldCopyAdditionalInfo = getResourceType(amenity) != SearchResultResource.TRAVEL
				|| getLangForTravel(amenity).equals(this.lang); // avoid articles in another language
		if (isSingleObject || shouldCopyAdditionalInfo) {
			syntheticAmenity.copyAdditionalInfo(amenity, false);
		}
		processPolygonCoordinates(amenity.getX(), amenity.getY());

		contentLocales.addAll(amenity.getSupportedContentLocales());
	}

	private void processPolygonCoordinates(TIntArrayList x, TIntArrayList y) {
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
		if (searchResultResource == null) {
			searchResultResource = findObfType(obfResourceName, syntheticAmenity);
		}
		return searchResultResource;
	}

	public String getLang() {
		return lang;
	}

	public void setMapIconName(String mapIconName) {
		this.syntheticAmenity.setMapIconName(mapIconName);
	}

	public void setX(TIntArrayList x) {
		this.syntheticAmenity.getX().addAll(x);
	}

	public void setY(TIntArrayList y) {
		this.syntheticAmenity.getY().addAll(y);
	}

	public void addX(int x) {
		this.syntheticAmenity.getX().add(x);
	}

	public void addY(int y) {
		this.syntheticAmenity.getY().add(y);
	}

	public boolean hasGeometry() {
		return !this.syntheticAmenity.getX().isEmpty() && !this.syntheticAmenity.getY().isEmpty();
	}

	public int getPointsLength() {
		return this.syntheticAmenity.getX().size();
	}

	public void clearGeometry() {
		this.syntheticAmenity.getY().clear();
		this.syntheticAmenity.getX().clear();
	}

	protected boolean isSupportedObjectType(Object object) {
		return object instanceof Amenity || object instanceof TransportStop
				|| object instanceof RenderedObject || object instanceof BaseDetailsObject;
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
		return 5;
	}

	@Override
	public String toString() {
		return getSyntheticAmenity().toString();
	}
}