package net.osmand.data;

import static net.osmand.data.Amenity.DEFAULT_ELO;
import static net.osmand.data.Amenity.WIKIDATA;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.PoiCategory;
import net.osmand.search.core.SearchResult.SearchResultResource;
import net.osmand.util.Algorithms;

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
		if (!shouldAdd(object)) {
			return;
		}
		if (object instanceof BaseDetailsObject detailsObject) {
			for (Object obj : detailsObject.getObjects()) {
				addObject(obj);
			}
		} else {
			objects.add(object);

			Long osmId = getOsmId(object);
			Amenity amenity = getAmenity(object);
			String wikidata = amenity != null ? amenity.getWikidata() : null;

			if (osmId != null && osmId != -1) {
				osmIds.add(osmId);
			}
			if (!Algorithms.isEmpty(wikidata)) {
				wikidataIds.add(wikidata);
			}
		}
	}

	private Amenity getAmenity(Object object) {
		if (object instanceof Amenity amenity) {
			return amenity;
		}
		if (object instanceof BaseDetailsObject detailsObject) {
			return detailsObject.getSyntheticAmenity();
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
		if (object instanceof BaseDetailsObject detailsObject) {
			return detailsObject.getSyntheticAmenity().getOsmId();
		}
		return null;
	}

	public boolean overlapsWith(Object object) {
		Long osmId = getOsmId(object);
		Amenity amenity = getAmenity(object);
		String wikidata = amenity != null ? amenity.getWikidata() : null;

		boolean osmIdEqual = osmId != null && osmId != -1 && osmIds.contains(osmId);
		boolean wikidataEqual = !Algorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata);
		return osmIdEqual || wikidataEqual;
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
			}
			if (object instanceof BaseDetailsObject detailsObject) {
				processAmenity(detailsObject.syntheticAmenity, contentLocales);
			}
		}
		if (!Algorithms.isEmpty(contentLocales)) {
			syntheticAmenity.updateContentLocales(contentLocales);
		}
	}

	private void sortObjects() {
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

	public static boolean shouldAdd(Object object) {
		return object instanceof Amenity || object instanceof BaseDetailsObject;
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
}
