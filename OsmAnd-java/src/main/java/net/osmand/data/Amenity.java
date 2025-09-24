package net.osmand.data;

import static net.osmand.gpx.GPXUtilities.*;
import static net.osmand.osm.MapPoiTypes.ROUTES_PREFIX;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.shared.gpx.GpxFile.XML_COLON;

import net.osmand.Location;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.ObfConstants;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.shared.wiki.WikiHelper;
import net.osmand.shared.wiki.WikiImage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.util.*;
import java.util.Map.Entry;

import gnu.trove.list.array.TIntArrayList;


public class Amenity extends MapObject {

	public static final String WEBSITE = "website";
	public static final String URL = "url";
	public static final String PHONE = "phone";
	public static final String MOBILE = "mobile";
	public static final String BRAND = "brand";
	public static final String OPERATOR = "operator";
	public static final String DESCRIPTION = "description";
	public static final String SHORT_DESCRIPTION = "short_description";
	public static final String ROUTE = "route";
	public static final String OPENING_HOURS = "opening_hours";
	public static final String POPULATION = "population";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String DISTANCE = "distance";
	public static final String SERVICE_TIMES = "service_times";
	public static final String COLLECTION_TIMES = "collection_times";
	public static final String CONTENT = "content";
	public static final String CUISINE = "cuisine";
	public static final String WIKIPEDIA = "wikipedia";
	public static final String WIKIDATA = "wikidata";
	public static final String WIKIMEDIA_COMMONS = "wikimedia_commons";
	public static final String MAPILLARY = "mapillary";
	public static final String DISH = "dish";
	public static final String REF = "ref";
	public static final String OSM_DELETE_VALUE = "delete";
	public static final String OSM_DELETE_TAG = "osmand_change";
	public static final String PRIVATE_VALUE = "private";
	public static final String ACCESS_PRIVATE_TAG = "access_private";
	public static final String IMAGE_TITLE = "image_title";
	public static final String IS_PART = "is_part";
	public static final String IS_PARENT_OF = "is_parent_of";
	public static final String IS_AGGR_PART = "is_aggr_part";
	public static final String CONTENT_JSON = "content_json";
	public static final String ROUTE_ID = "route_id";
	public static final String ROUTE_ID_OSM_PREFIX_LEGACY = "OSM"; // non-indexed
	public static final String ROUTE_ID_OSM_PREFIX = "O"; // indexed in POI-section
	public static final String ROUTE_SOURCE = "route_source";
	public static final String ROUTE_NAME = "route_name";
	public static final String ROUTE_ACTIVITY_TYPE = "route_activity_type";
	public static final String WIKI_PHOTO = "wiki_photo";
	public static final String WIKI_CATEGORY = "wiki_category";
	public static final String TRAVEL_TOPIC = "travel_topic";
	public static final String TRAVEL_ELO = "travel_elo";
	public static final String OSMAND_POI_KEY = "osmand_poi_key";
	public static final String COLOR = "color";
	public static final String LANG_YES = "lang_yes";
	public static final String GPX_ICON = "gpx_icon";
	public static final String TYPE = "type";
	public static final String SUBTYPE = "subtype";
	public static final String NAME = "name";
	public static final String SEPARATOR = ";";
	public static final String ALT_NAME_WITH_LANG_PREFIX = "alt_name:";
	public static final String COLLAPSABLE_PREFIX = "collapsable_";
	public static final String ROUTE_MEMBERS_IDS = "route_members_ids";
	public static final String ROUTE_BBOX_RADIUS = "route_bbox_radius";
	public static final List<String> HIDING_EXTENSIONS_AMENITY_TAGS = Arrays.asList(PHONE, WEBSITE);
	public static final int DEFAULT_ELO = 900;
	public static final String ADDR_STREET = "addr_street";
	
	private String subType;
	private PoiCategory type;
	// duplicate for fast access
	private String openingHours;
	private Map<String, String> additionalInfo;
	private AmenityRoutePoint routePoint; // for search on path
	// context menu geometry;
	private TIntArrayList y;
	private TIntArrayList x;
	private String mapIconName;
	private int order;
	private Map<Integer, List<TagValuePair>> tagGroups;
	private String regionName;

	private String wikiIconUrl;
	private String wikiImageStubUrl;
	private int travelElo = 0;
	private Set<String> contentLocales;

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Map<Integer, List<TagValuePair>> getTagGroups() {
		return tagGroups;
	}

	public void addTagGroup(int id, List<TagValuePair> tagValues) {
		if (tagGroups == null) {
			tagGroups = new HashMap<Integer, List<TagValuePair>>();
		}
		tagGroups.put(id, tagValues);
	}

	public void setTagGroups(Map<Integer, List<TagValuePair>> tagGroups) {
		this.tagGroups = tagGroups;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public String getRegionName() {
		return regionName;
	}

    public static class AmenityRoutePoint {
		public double deviateDistance;
		public boolean deviationDirectionRight;
		public Location pointA;
		public Location pointB;
	}

	public String getMapIconName() {
		return mapIconName;
	}

	public void setMapIconName(String mapIconName) {
		this.mapIconName = mapIconName;
	}

	public PoiCategory getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	public void setType(PoiCategory type) {
		this.type = type;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public String getSubTypeStr() {
		PoiCategory pc = getType();
		String[] subtypes = getSubType().split(";");
		String typeStr = "";
		//multi value
		for (String subType : subtypes) {
			PoiType pt = pc.getPoiTypeByKeyName(subType);
			if (pt != null) {
				if (!typeStr.isEmpty()) {
					typeStr += ", " + pt.getTranslation().toLowerCase();
				} else {
					typeStr = pt.getTranslation();
				}
			}
		}
		if (typeStr.isEmpty()) {
			typeStr = getSubType();
			typeStr = Algorithms.capitalizeFirstLetterAndLowercase(typeStr.replace('_', ' '));
		}
		return typeStr;
	}

	public String getOpeningHours() {
		return openingHours;
	}

	public String getAdditionalInfo(String key) {
		if (additionalInfo == null) {
			return null;
		}
		String str = additionalInfo.get(key);
		if (str == null && key.contains(":")) {
			str = additionalInfo.get(key.replaceAll(":", XML_COLON)); // try content_-_uk after content:uk
		}
		if (str != null) {
			str = unzipContent(str);
		}
		return str;
	}

	public boolean hasAdditionalInfo() {
		return !Algorithms.isEmpty(additionalInfo);
	}

	// this method should be used carefully
	private Map<String, String> getInternalAdditionalInfoMap() {
		if (additionalInfo == null) {
			return Collections.emptyMap();
		}
		return additionalInfo;
	}

	public Collection<String> getAdditionalInfoValues(boolean excludeZipped) {
		if (additionalInfo == null) {
			return Collections.emptyList();
		}
		boolean zipped = false;
		for (String v : additionalInfo.values()) {
			if (isContentZipped(v)) {
				zipped = true;
				break;
			}
		}
		if (zipped) {
			List<String> r = new ArrayList<>(additionalInfo.size());
			for (String str : additionalInfo.values()) {
				if (excludeZipped && isContentZipped(str)) {

				} else {
					r.add(unzipContent(str));
				}
			}
			return r;
		} else {
			return additionalInfo.values();
		}
	}

	public Collection<String> getAdditionalInfoKeys() {
		if (additionalInfo == null) {
			return Collections.emptyList();
		}
		return additionalInfo.keySet();
	}

	public void setAdditionalInfo(Map<String, String> additionalInfo) {
		this.additionalInfo = null;
		openingHours = null;
		if (additionalInfo != null) {
			Iterator<Entry<String, String>> it = additionalInfo.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				setAdditionalInfo(e.getKey(), e.getValue());
			}
		}
	}

	public void setRoutePoint(AmenityRoutePoint routePoint) {
		this.routePoint = routePoint;
	}

	public AmenityRoutePoint getRoutePoint() {
		return routePoint;
	}

	public void setAdditionalInfo(String tag, String value) {
		if ("name".equals(tag)) {
			setName(value);
		} else if (isNameLangTag(tag)) {
			setName(tag.substring("name:".length()), value);
		} else {
			if (additionalInfo == null) {
				additionalInfo = new LinkedHashMap<String, String>();
			}
			additionalInfo.put(tag, value);

			if (OPENING_HOURS.equals(tag)) {
				openingHours = unzipContent(value);
			}
		}
	}

	public void copyAdditionalInfo(Amenity amenity, boolean overwrite) {
		copyAdditionalInfo(amenity.getInternalAdditionalInfoMap(), overwrite);
	}

	public void copyAdditionalInfo(Map<String, String> map, boolean overwrite) {
		if (overwrite || additionalInfo == null) {
			setAdditionalInfo(map);
		} else {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				if (!additionalInfo.containsKey(key)) {
					setAdditionalInfo(key, entry.getValue());
				}
			}
		}
	}

	public StringBuilder printNamesAndAdditional() {
		StringBuilder s = new StringBuilder();
		Map<String, String> additionals = new LinkedHashMap<>();
		Map<String, String> poi_type = new LinkedHashMap<>();
		Map<String, String> text = new LinkedHashMap<>();
		if (additionalInfo != null) {
			for (Map.Entry<String, String> e : additionalInfo.entrySet()) {
				String key = e.getKey();
				String val = e.getValue();
				AbstractPoiType pt = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key);
				if (pt == null && !Algorithms.isEmpty(val) && val.length() < 50) {
					pt = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key + "_" + val);
				}
				if (pt != null) {
					additionals.put(key, val);
				} else {
					PoiType pt2 = MapPoiTypes.getDefault().getPoiTypeByKey(key);
					if (pt2 != null) {
						String cat = pt2.getCategory().getKeyName();
						if (poi_type.containsKey(cat)) {
							key = poi_type.get(cat) + ";" + key;
						}
						poi_type.put(pt2.getCategory().getKeyName(), key);
					} else {
						text.put(key, val);
					}
				}
			}
		}
		if (poi_type.size() > 0) {
			s.append(" [ ");
			printNames("", poi_type, s);
			s.append(" ] ");
		}
		if (additionals.size() > 0) {
			s.append(" poi_additional:[ ");
			printNames("", additionals, s);
			s.append(" ] ");
		}
		if (text.size() > 0) {
			s.append(" non_default_poi_xml:[ ");
			printNames("", text, s);
			s.append(" ] ");
		}
		printNames(" name:", getNamesMap(true), s);
		return s;
	}

	private void printNames(String prefix, Map<String, String> stringMap, StringBuilder s) {
		for (Entry<String, String> e : stringMap.entrySet()) {
			if (e.getValue().startsWith(" gz ")) {
				s.append(prefix).append(e.getKey()).append("='gzip ...' ");
			} else {
				s.append(prefix).append(e.getKey()).append("='").append(e.getValue()).append("' ");
			}
		}
	}

	@Override
	public String toStringEn() {
		return super.toStringEn() + ": " + type.getKeyName() + ":" + subType;
	}

	@Override
	public String toString() {
		return type.getKeyName() + ": " + subType + " " + getName();
	}

	public String getSite() {
		return getAdditionalInfo(WEBSITE);
	}
	
	public String getStreetName() {
		return getAdditionalInfo(ADDR_STREET);
	}

	public void setSite(String site) {
		setAdditionalInfo(WEBSITE, site);
	}

	public String getPhone() {
		return getAdditionalInfo(PHONE);
	}

	public void setPhone(String phone) {
		setAdditionalInfo(PHONE, phone);
	}

	public String getColor() {
		return getAdditionalInfo(COLOR);
	}

	public String getGpxIcon() {
		String wikiVoyageIcon = getAdditionalInfo(GPX_ICON);
		String travelGpxIcon = getAdditionalInfo(ICON_NAME_EXTENSION);
		return Algorithms.isEmpty(wikiVoyageIcon) ? travelGpxIcon : wikiVoyageIcon;
	}

	public String getContentLanguage(String tag, String lang, String defLang) {
		if (lang != null) {
			String translateName = getAdditionalInfo(tag + ":" + lang);
			if (!Algorithms.isEmpty(translateName)) {
				return lang;
			}
		}
		String plainContent = getAdditionalInfo(tag);
		if (!Algorithms.isEmpty(plainContent)) {
			return defLang;
		}
		String enName = getAdditionalInfo(tag + ":en");
		if (!Algorithms.isEmpty(enName)) {
			return "en";
		}
		int maxLen = 0;
		String lng = defLang;
		for (String nm : getAdditionalInfoKeys()) {
			if (nm.startsWith(tag + ":")) {
				String key = nm.substring(tag.length() + 1);
				String cnt = getAdditionalInfo(tag + ":" + key);
				if (!Algorithms.isEmpty(cnt) && cnt.length() > maxLen) {
					maxLen = cnt.length();
					lng = key;
				}
			}
		}
		return lng;
	}

	public Set<String> getSupportedContentLocales() {
		if (contentLocales != null) {
			return contentLocales;
		} else {
			Set<String> supported = new TreeSet<>();
			supported.addAll(getNames(CONTENT, "en"));
			supported.addAll(getNames(DESCRIPTION, "en"));
			return supported;
		}
	}

	public void updateContentLocales(Set<String> locales) {
		if (contentLocales == null) {
			contentLocales = new TreeSet<>();
		}
		contentLocales.addAll(locales);
	}

	public List<String> getNames(String tag, String defTag) {
		List<String> l = new ArrayList<String>();
		for (String nm : getAdditionalInfoKeys()) {
			if (nm.startsWith(tag + ":")) {
				l.add(nm.substring(tag.length() + 1));
			} else if (nm.equals(tag)) {
				l.add(defTag);
			}
		}
		return l;
	}

	public Map<String, String> getAltNamesMap() {
		Map<String, String> names = new HashMap<>();
		for (String nm : getAdditionalInfoKeys()) {
			String name = additionalInfo.get(nm);
			if (nm.startsWith(ALT_NAME_WITH_LANG_PREFIX)) {
				names.put(nm.substring(ALT_NAME_WITH_LANG_PREFIX.length()), name);
			}
		}
		return names;
	}

	public String getTagSuffix(String tagPrefix) {
		for (String infoTag : getAdditionalInfoKeys()) {
			if (infoTag.startsWith(tagPrefix)) {
				if (infoTag.length() > tagPrefix.length()) {
					return infoTag.substring(tagPrefix.length());
				}
			}
		}
		return null;
	}

	public String getTagContent(String tag) {
		return getTagContent(tag, null);
	}

	public String getTagContent(String tag, String lang) {
		String translateName = getStrictTagContent(tag, lang);
		if (translateName != null) {
			return translateName;
		}
		for (String nm : getAdditionalInfoKeys()) {
			if (nm.startsWith(tag + ":") || nm.startsWith(tag + XML_COLON)) {
				return getAdditionalInfo(nm);
			}
		}
		return null;
	}

	public String getRef() {
		return getAdditionalInfo(REF);
	}

	public String getRouteId() {
		return getAdditionalInfo(ROUTE_ID);
	}

	public String getWikiPhoto() {
		return getAdditionalInfo(WIKI_PHOTO);
	}

	public void setWikiPhoto(String wikiPhoto) {
		setAdditionalInfo(WIKI_PHOTO, wikiPhoto);
	}

	public String getWikiCategory() {
		return getAdditionalInfo(WIKI_CATEGORY);
	}

	public void setWikiCategory(String wikiCategory) {
		setAdditionalInfo(WIKI_CATEGORY, wikiCategory);
	}

	public String getTravelTopic() {
		return getAdditionalInfo(TRAVEL_TOPIC);
	}

	public void setTravelTopic(String travelTopic) {
		setAdditionalInfo(TRAVEL_TOPIC, travelTopic);
	}

	public String getTravelElo() {
		return getAdditionalInfo(TRAVEL_ELO);
	}
	public String getWikidata() {
		return getAdditionalInfo(WIKIDATA);
	}

	public int getTravelEloNumber() {
		if (travelElo > 0) {
			return travelElo;
		}
		String travelEloStr = getTravelElo();
		travelElo = Algorithms.parseIntSilently(travelEloStr, DEFAULT_ELO);
		return travelElo;
	}

	public void setTravelEloNumber(int elo) {
		travelElo = elo;
	}

	public String getWikiIconUrl() {
		if (wikiIconUrl == null) {
			obtainWikiUrls();
		}
		return wikiIconUrl;
	}

	public void setWikiIconUrl(String wikiIconUrl) {
		this.wikiIconUrl = wikiIconUrl;
	}

	public String getWikiImageStubUrl() {
		if (wikiImageStubUrl == null) {
			obtainWikiUrls();
		}
		return wikiImageStubUrl;
	}

	public void setWikiImageStubUrl(String wikiImageStubUrl) {
		this.wikiImageStubUrl = wikiImageStubUrl;
	}

	public String getOsmandPoiKey() {
		return getAdditionalInfo(OSMAND_POI_KEY);
	}

	private void obtainWikiUrls() {
		String wikiPhoto = getWikiPhoto();
		if (!Algorithms.isEmpty(wikiPhoto)) {
			WikiImage wikiIMage = WikiHelper.INSTANCE.getImageData(wikiPhoto);
			setWikiIconUrl(wikiIMage.getImageIconUrl());
			setWikiImageStubUrl(wikiIMage.getImageStubUrl());
		}
	}

	public boolean hasOsmRouteId() {
		String routeId = getRouteId();
		return routeId != null &&
				(routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY)
						|| routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX));
	}

	public String getGpxFileName(String lang) {
		final String gpxFileName = lang != null ? getName(lang) : getEnName(true);
		if (!Algorithms.isEmpty(gpxFileName)) {
			return Algorithms.sanitizeFileName(gpxFileName);
		}
		if (!Algorithms.isEmpty(getRouteId())) {
			return getRouteId();
		}
		if (!Algorithms.isEmpty(getSubType())) {
			return getType().getKeyName() + " " + getSubType();
		}
		return getType().getKeyName();
	}

	public String getStrictTagContent(String tag, String lang) {
		if (lang != null) {
			String translateName = getAdditionalInfo(tag + ":" + lang);
			if (!Algorithms.isEmpty(translateName)) {
				return translateName;
			}
		}
		String plainName = getAdditionalInfo(tag);
		if (!Algorithms.isEmpty(plainName)) {
			return plainName;
		}
		String enName = getAdditionalInfo(tag + ":en");
		if (!Algorithms.isEmpty(enName)) {
			return enName;
		}
		return null;
	}

	public String getDescription(String lang) {
		String info = getTagContent(DESCRIPTION, lang);
		if (!Algorithms.isEmpty(info)) {
			return info;
		}
		return getTagContent(CONTENT, lang);
	}

	public void setDescription(String description) {
		setAdditionalInfo(DESCRIPTION, description);
	}

	public void setOpeningHours(String openingHours) {
		setAdditionalInfo(OPENING_HOURS, openingHours);
	}

	public boolean comparePoi(Amenity thatObj) {
		return this.compareObject(thatObj) &&
				Algorithms.objectEquals(this.type.getKeyName(), thatObj.type.getKeyName()) &&
				Algorithms.objectEquals(this.subType, thatObj.subType) &&
				Algorithms.objectEquals(this.additionalInfo, thatObj.additionalInfo);
	}

	public boolean strictEquals(Object object) {
		if (equals(object)) {
			if (x != null && ((Amenity) object).x != null && x.size() == ((Amenity) object).x.size()) {
				for (int i = 0; i < x.size(); i++) {
					if (x.get(i) != ((Amenity) object).x.get(i) || y.get(i) != ((Amenity) object).y.get(i)) {
						return false;
					}
				}
				return true;
			} else {
				return x == null && ((Amenity) object).x == null;
			}
		}
		return false;
	}

	@Override
	public int compareTo(MapObject o) {
		int cmp = super.compareTo(o);
		if (cmp == 0 && o instanceof Amenity) {
			int kn = ((Amenity) o).getType().getKeyName().compareTo(getType().getKeyName());
			if (kn == 0) {
				kn = ((Amenity) o).getSubType().compareTo(getSubType());
			}
			return kn;
		}
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		boolean res = super.equals(o);
		if (res && o instanceof Amenity) {
			return Algorithms.stringsEqual(((Amenity) o).getType().getKeyName(), getType().getKeyName())
					&& Algorithms.stringsEqual(((Amenity) o).getSubType(), getSubType());
		}
		return res;
	}

	public TIntArrayList getY() {
		if (y == null) {
			y = new TIntArrayList();
		}
		return y;
	}

	public TIntArrayList getX() {
		if (x == null) {
			x = new TIntArrayList();
		}
		return x;
	}

	public boolean isClosed() {
		return OSM_DELETE_VALUE.equals(getAdditionalInfo(OSM_DELETE_TAG));
	}

	public boolean isPrivateAccess() {
		return PRIVATE_VALUE.equals(getTagContent(ACCESS_PRIVATE_TAG));
	}

	public boolean isRouteTrack() {
		if (subType == null) {
			return false;
		} else {
			boolean hasRouteTrackSubtype = subType.startsWith(ROUTES_PREFIX) || subType.equals(ROUTE_TRACK);
			boolean hasGeometry = additionalInfo != null && additionalInfo.containsKey(ROUTE_BBOX_RADIUS);
			return hasRouteTrackSubtype && hasGeometry && !Algorithms.isEmpty(getRouteId());
		}
	}

	public boolean isRoutePoint() {
		return subType != null && (subType.equals(ROUTE_TRACK_POINT) || subType.equals(ROUTE_ARTICLE_POINT));
	}

	public boolean isRouteArticle() {
		return Algorithms.stringsEqual(ROUTE_ARTICLE, subType);
	}

	public boolean isSuperRoute() {
		return additionalInfo != null && additionalInfo.containsKey(ROUTE_MEMBERS_IDS);
	}

	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("subType", subType);
		json.put("type", type.getKeyName());
		json.put("openingHours", openingHours);
		if (additionalInfo != null && additionalInfo.size() > 0) {
			JSONObject additionalInfoObj = new JSONObject();
			for (Entry<String, String> e : additionalInfo.entrySet()) {
				additionalInfoObj.put(e.getKey(), e.getValue());
			}
			json.put("additionalInfo", additionalInfoObj);
		}

		return json;
	}

	public static Amenity parseJSON(JSONObject json) {
		Amenity amenity = new Amenity();
		MapObject.parseJSON(json, amenity);

		if (json.has("subType")) {
			amenity.subType = json.getString("subType");
		}
		if (json.has("type")) {
			String categoryName = json.getString("type");
			amenity.setType(MapPoiTypes.getDefault().getPoiCategoryByName(categoryName));
		} else {
			amenity.setType(MapPoiTypes.getDefault().getOtherPoiCategory());
		}
		if (json.has("openingHours")) {
			amenity.openingHours = json.getString("openingHours");
		}
		if (json.has("additionalInfo")) {
			JSONObject namesObj = json.getJSONObject("additionalInfo");
			Iterator<String> iterator = namesObj.keys();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = namesObj.getString(key);
				amenity.setAdditionalInfo(key, value);
			}
		}
		return amenity;
	}

	public Map<String, String> getAmenityExtensions() {
		return getAmenityExtensions(MapPoiTypes.getDefault(), true);
	}

	public Map<String, String> getAmenityExtensions(MapPoiTypes mapPoiTypes, boolean addPrefixes) {
		Map<String, String> result = new HashMap<>();
		Map<String, List<PoiType>> categories = new HashMap<>();

		if (name != null) {
			result.put(addPrefixes ? AMENITY_PREFIX + NAME : NAME, name);
		}
		if (subType != null) {
			result.put(addPrefixes ? AMENITY_PREFIX + SUBTYPE : SUBTYPE, subType);
		}
		if (type != null && type.getKeyName() != null) {
			result.put(addPrefixes ? AMENITY_PREFIX + TYPE : TYPE, type.getKeyName());
		}
		if (openingHours != null) {
			result.put(addPrefixes ? AMENITY_PREFIX + OPENING_HOURS : OPENING_HOURS, openingHours);
		}
		if (hasAdditionalInfo()) {
			result.putAll(getAdditionalInfoAndCollectCategories(mapPoiTypes, categories, addPrefixes));

			//join collected tags by category into one string
			for (Map.Entry<String, List<PoiType>> entry : categories.entrySet()) {
				String key = COLLAPSABLE_PREFIX + entry.getKey();
				List<PoiType> categoryTypes = entry.getValue();
				if (!categoryTypes.isEmpty()) {
					StringBuilder builder = new StringBuilder();
					for (PoiType poiType : categoryTypes) {
						if (builder.length() > 0) {
							builder.append(SEPARATOR);
						}
						builder.append(poiType.getKeyName());
					}
					result.put(key, builder.toString());
				}
			}
		}
		return result;
	}

	public Map<String, String> getAdditionalInfoAndCollectCategories(MapPoiTypes mapPoiTypes,
	                                                                 Map<String, List<PoiType>> categories,
	                                                                 boolean addPrefixes) {
		Map<String, String> result = new HashMap<>();
		for (String key : getAdditionalInfoKeys()) {
			String value = getAdditionalInfo(key);
			PoiType poiType = getPoiType(mapPoiTypes, key, value);
			if (poiType != null && poiType.isFilterOnly()) {
				continue;
			}
			if (poiType != null && !poiType.isText()) {
				if (categories != null) {
					String category = poiType.getPoiAdditionalCategory();
					if (!Algorithms.isEmpty(category)) {
						List<PoiType> types = categories.get(category);
						if (types == null) {
							types = new ArrayList<>();
							categories.put(category, types);
						}
						types.add(poiType);
						continue;
					}
				}
			}
			//save all other values to separate lines
			if (key.endsWith(OPENING_HOURS)) {
				continue;
			}
			if (!HIDING_EXTENSIONS_AMENITY_TAGS.contains(key) && addPrefixes) {
				key = OSM_PREFIX + key;
			}
			result.put(key, value);
		}
		return result;
	}

	private PoiType getPoiType(MapPoiTypes mapPoiTypes, String key, String value) {
		AbstractPoiType abstractPoiType = mapPoiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (abstractPoiType == null && !isContentZipped(value)) {
			abstractPoiType = mapPoiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + value);
		}
		if (abstractPoiType instanceof PoiType) {
			return (PoiType) abstractPoiType;
		}
		return null;
	}

	public String getTranslation(MapPoiTypes mapPoiTypes, String alternateName) {
		for (String key : getAdditionalInfoKeys()) {
			String value = getAdditionalInfo(key);
			if (value.equals(alternateName)) {
				PoiType poiType = getPoiType(mapPoiTypes, key, value);
				if (poiType != null && !poiType.isText()) {
					return poiType.getTranslation();
				}
			}
		}
		return alternateName;
	}

	public String getCityFromTagGroups(String lang) {
		if (tagGroups == null) {
			return null;
		}
		String result = null;
		for (Map.Entry<Integer, List<TagValuePair>> entry : tagGroups.entrySet()) {
			String translated = "";
			String nonTranslated = "";
			City.CityType type = null;
			for (TagValuePair tagValue : entry.getValue()) {
				if (tagValue.tag.endsWith("name:" + lang)) {
					translated = tagValue.value;
				}
				if (tagValue.tag.endsWith("name")) {
					nonTranslated = tagValue.value;
				}
				if (tagValue.tag.equals("place")) {
					type = City.CityType.valueFromString(tagValue.value.toUpperCase());
				}
			}
			String name = translated.isEmpty() ? nonTranslated : translated;
			if (!name.isEmpty() && isCityTypeAccept(type)) {
				result = result == null ? name : result + ", " + name;
			}
		}
		return result;
	}

	private boolean isCityTypeAccept(City.CityType type) {
		if (type == null) {
			return false;
		}
		return type.storedAsSeparateAdminEntity();
	}

	public List<LatLon> getPolygon() {
		List<LatLon> res = new ArrayList<>();
		if (x == null) {
			return res;
		}
		for (int i = 0; i < getX().size(); i++) {
			int x = getX().get(i);
			int y = getY().get(i);
			LatLon l = new LatLon(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
			res.add(l);
		}
		return res;
	}

	public void setX(TIntArrayList x) {
		this.x = x;
	}

	public void setY(TIntArrayList y) {
		this.y = y;
	}

	public String getRouteActivityType() {
		if (!isRouteTrack() && !isSuperRoute()) {
			return "";
		}
		for (Map.Entry<String, String> entry : additionalInfo.entrySet()) {
			if (entry.getKey().startsWith(ROUTE_ACTIVITY_TYPE + "_")) {
				return MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(entry.getKey()).getTranslation();
			}
		}
		return "";
	}

	public Long getOsmId() {
		Long id = getId();
		if (id == null) {
			return null;
		}
		if (ObfConstants.isShiftedID(id)) {
			return ObfConstants.getOsmId(id);
		} else {
			return id >> AMENITY_ID_RIGHT_SHIFT;
		}
	}

	public static String getPoiStringWithoutType(Amenity amenity, String locale, boolean transliterate) {
		String typeName = amenity.getSubTypeStr();
		String localName = amenity.getName(locale, transliterate);
		if (typeName != null && localName.contains(typeName)) {
			// type is contained in name e.g.
			// localName = "Bakery the Corner"
			// type = "Bakery"
			// no need to repeat this
			return localName;
		}
		if (Algorithms.isEmpty(localName) && amenity.isRouteTrack()) {
			localName = amenity.getAdditionalInfo(Amenity.ROUTE_ID);
		}
		if (Algorithms.isEmpty(localName)) {
			return typeName;
		}
		return typeName + " " + localName; // $NON-NLS-1$
	}

	public Map<String, String> getOsmTags() {
		Map<String, String> result = new LinkedHashMap<>();

		Map<String, String> amenityTags = new LinkedHashMap<>();
		for (String amenityTag : getAdditionalInfoKeys()) {
			amenityTags.put(amenityTag, getAdditionalInfo(amenityTag));
		}

		String amenityName = getName();
		if (!Algorithms.isEmpty(amenityName)) {
			result.put(NAME, amenityName);
		}

		PoiCategory category = getType();
		String subTypesList = getSubType();

		if (subTypesList != null) {
			for (String subType : subTypesList.split(";")) {
				PoiType type = category.getPoiTypeByKeyName(subType);
				if (type != null) {
					result.putAll(type.getOsmTagsValues());
					for (PoiType additional : type.getPoiAdditionals()) {
						if (amenityTags.remove(additional.getKeyName()) != null) {
							result.putAll(additional.getOsmTagsValues());
						}
					}
				}
			}
		}

		result.putAll(amenityTags); // unresolved residues

		return result;
	}
}
