package net.osmand.data;

import static net.osmand.gpx.GPXUtilities.AMENITY_PREFIX;
import static net.osmand.gpx.GPXUtilities.OSM_PREFIX;

import net.osmand.Location;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;


public class Amenity extends MapObject {

	public static final String WEBSITE = "website";
	public static final String PHONE = "phone";
	public static final String MOBILE = "mobile";
	public static final String DESCRIPTION = "description";
	public static final String ROUTE = "route";
	public static final String OPENING_HOURS = "opening_hours";
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
	public static final String ROUTE_SOURCE = "route_source";
	public static final String ROUTE_NAME = "route_name";
	public static final String COLOR = "color";
	public static final String LANG_YES = "lang_yes";
	public static final String GPX_ICON = "gpx_icon";
	public static final String TYPE = "type";
	public static final String SUBTYPE = "subtype";
	public static final String NAME = "name";
	public static final String SEPARATOR = ";";
	public static final String ALT_NAME_WITH_LANG_PREFIX = "alt_name:";
	public static final String COLLAPSABLE_PREFIX = "collapsable_";
	public static final List<String> HIDING_EXTENSIONS_AMENITY_TAGS = Arrays.asList(PHONE, WEBSITE);

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
		str = unzipContent(str);
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
		} else if (tag.startsWith("name:")) {
			setName(tag.substring("name:".length()), value);
		} else {
			if (this.additionalInfo == null) {
				this.additionalInfo = new LinkedHashMap<String, String>();
			}
			this.additionalInfo.put(tag, value);
			if (OPENING_HOURS.equals(tag)) {
				this.openingHours = unzipContent(value);
			}
		}
	}

	public StringBuilder printNamesAndAdditional() {
		StringBuilder s = new StringBuilder();
		Map<String, String> additionals = new HashMap<>();
		Map<String, String> poi_type = new HashMap<>();
		Map<String, String> text = new HashMap<>();
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
							val = poi_type.get(cat) + ";" + val;
						}
						poi_type.put(pt2.getCategory().getKeyName(), val);
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
				s.append(prefix).append(e.getKey()).append("='gzip ...'");
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
		return getAdditionalInfo(GPX_ICON);
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
		Set<String> supported = new TreeSet<>();
		supported.addAll(getNames("content", "en"));
		supported.addAll(getNames("description", "en"));
		return supported;
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
			if (nm.startsWith(tag + ":")) {
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
		Amenity a = new Amenity();
		MapObject.parseJSON(json, a);

		if (json.has("subType")) {
			a.subType = json.getString("subType");
		}
		if (json.has("type")) {
			String categoryName = json.getString("type");
			a.setType(MapPoiTypes.getDefault().getPoiCategoryByName(categoryName));
		} else {
			a.setType(MapPoiTypes.getDefault().getOtherPoiCategory());
		}
		if (json.has("openingHours")) {
			a.openingHours = json.getString("openingHours");
		}
		if (json.has("additionalInfo")) {
			JSONObject namesObj = json.getJSONObject("additionalInfo");
			a.additionalInfo = new HashMap<>();
			Iterator<String> iterator = namesObj.keys();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = namesObj.getString(key);
				a.additionalInfo.put(key, value);
			}
		}
		return a;
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
		if (type != null) {
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
}