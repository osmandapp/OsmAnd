package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.search.core.SearchResult.SearchResultResource;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.*;

import gnu.trove.list.array.TIntArrayList;

public class BaseDetailsObject {

	protected final Set<Long> osmIds = new HashSet<>();
	protected final Set<String> wikidataIds = new HashSet<>();
	protected final List<Object> objects = new ArrayList<>();

	protected final String lang;

	protected String obfResourceName;
	protected SearchResultResource searchResultResource;

	protected Amenity syntheticAmenity = new Amenity();

	public static final int MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS = 30;


	public enum DataEnvelope {
		EMPTY,
		COMBINED,
		FULL
	}

	public DataEnvelope dataEnvelope = DataEnvelope.EMPTY;

	public BaseDetailsObject(String lang) {
		this.lang = lang;
	}

	public BaseDetailsObject(Object object, String lang) {
		this(lang != null ? lang : "en");
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
		if (!isSupportedObjectType(object)) {
			return;
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
	}

	private String getWikidata(Object object) {
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
		return osmIdEqual || wikidataEqual;
	}

	public boolean overlapPublicTransport(Object object, Collection<String> publicTransportTypes) {
		if (object instanceof RenderedObject renderedObject) {
			Collection<TransportStop> stops = getTransportStops();
			if (stops.isEmpty()) {
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
				if (publicTransportTypes.contains(value) || publicTransportTypes.contains(tag + "_" + value)) {
					isStop = true;
					break;
				}
			}
			if (isStop) {
				assert stops.size() == 1; // TODO remove later !!!
				for (TransportStop stop : stops) {
					if (MapUtils.getDistance(stop.getLocation(), renderedObject.getLatLon()) < MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS) {
						return true;
					}
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
		syntheticAmenity = new Amenity();

		sortObjects();
		Set<String> contentLocales = new TreeSet<>();
		for (Object object : objects) {
			if (object instanceof Amenity amenity) {
				processAmenity(amenity, contentLocales);
			} else if (object instanceof TransportStop transportStop) {
				Amenity amenity = transportStop.getAmenity();
				if (amenity != null) {
					processAmenity(amenity, contentLocales);
				} else {
					syntheticAmenity.copyNames(transportStop);
					if (syntheticAmenity.getLocation() == null) {
						syntheticAmenity.setLocation(transportStop.getLocation());
					}
				}
			} else if (object instanceof RenderedObject renderedObject) {
				if (syntheticAmenity.getType() == null) {
					Amenity amenity = BaseDetailsObject.convertToSyntheticAmenity(renderedObject);
					syntheticAmenity.setType(amenity.getType());
					syntheticAmenity.setSubType(amenity.getSubType());
					syntheticAmenity.copyAdditionalInfo(renderedObject.getTags(), false);
				}
				syntheticAmenity.copyNames(renderedObject);
				if (syntheticAmenity.getLocation() == null) {
					syntheticAmenity.setLocation(renderedObject.getLocation());
				}
				processPolygonCoordinates(renderedObject.getX(), renderedObject.getY());
			}
		}
		if (!Algorithms.isEmpty(contentLocales)) {
			syntheticAmenity.updateContentLocales(contentLocales);
		}
		if (this.dataEnvelope.ordinal() < DataEnvelope.FULL.ordinal()) {
			this.dataEnvelope = syntheticAmenity.getType() == null ? DataEnvelope.EMPTY : DataEnvelope.COMBINED;
		}
		if (syntheticAmenity.getType() == null) {
			syntheticAmenity.setType(MapPoiTypes.getDefault().getUserDefinedCategory());
			syntheticAmenity.setSubType("");
			this.dataEnvelope = DataEnvelope.EMPTY;
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
		syntheticAmenity.copyNames(amenity);
		syntheticAmenity.copyAdditionalInfo(amenity, false);
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

	public static boolean isSupportedObjectType(Object object) {
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
		return 4;
	}

	@Override
	public String toString() {
		return getSyntheticAmenity().toString();
	}

	public static Amenity convertToSyntheticAmenity(RenderedObject renderedObject) {
		MapPoiTypes poiTypes = MapPoiTypes.getDefault();

		Amenity amenity = new Amenity();
		amenity.setType(poiTypes.getOtherPoiCategory());
		amenity.setSubType("");

		PoiTranslator translator = poiTypes.getPoiTranslator();
		PoiType pt = null;
		PoiType otherPt = null;
		String subtype = null;
		Map<String, String> additionalInfo = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : renderedObject.getTags().entrySet()) {
			String tag = e.getKey();
			String value = e.getValue();
			if (tag.equals("name")) {
				amenity.setName(value);
				continue;
			}
			if (e.getKey().startsWith("name:")) {
				amenity.setName(tag.substring("name:".length()), value);
				continue;
			}
			if (tag.equals("amenity")) {
				if (pt != null) {
					otherPt = pt;
				}
				pt = poiTypes.getPoiTypeByKey(value);
			} else {
				PoiType poiType = poiTypes.getPoiTypeByKey(e.getKey() + "_" + e.getValue());
				if (poiType == null) {
					poiType = poiTypes.getPoiTypeByKey(e.getKey());
				}
				if (poiType != null) {
					otherPt = pt != null ? poiType : otherPt;
					subtype = pt == null ? value : subtype;
					pt = pt == null ? poiType : pt;
				}
			}
			if (Algorithms.isEmpty(value) && otherPt == null) {
				otherPt = poiTypes.getPoiTypeByKey(tag);
			}
			if (otherPt == null) {
				PoiType poiType = poiTypes.getPoiTypeByKey(value);
				if (poiType != null && poiType.getOsmTag().equals(tag)) {
					otherPt = poiType;
				}
			}
			if (!Algorithms.isEmpty(value)) {
				String translate = translator.getTranslation(tag + "_" + value);
				String translate2 = translator.getTranslation(value);
				if (translate != null && translate2 != null) {
					additionalInfo.put(translate, translate2);
				} else {
					additionalInfo.put(tag, value);
				}
			}
		}
		if (pt != null) {
			amenity.setType(pt.getCategory());
		} else if (otherPt != null) {
			amenity.setType(otherPt.getCategory());
			amenity.setSubType(otherPt.getKeyName());
		}
		if (subtype != null) {
			amenity.setSubType(subtype);
		}
		amenity.setId(renderedObject.getId());
		amenity.setAdditionalInfo(additionalInfo);
		amenity.setX(renderedObject.getX());
		amenity.setY(renderedObject.getY());

		return amenity;
	}
}
