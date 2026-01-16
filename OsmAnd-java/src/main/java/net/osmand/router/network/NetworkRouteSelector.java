package net.osmand.router.network;


import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.OsmRouteType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.util.Algorithms;
import net.osmand.util.TransliterationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class NetworkRouteSelector {

	public static final String ROUTE_KEY_VALUE_SEPARATOR = "__";
	public static final String NETWORK_ROUTE_TYPE = "type";

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(RouteDataObject obj) {
		Map<String, String> tags = new TreeMap<>();
		for (int i = 0; obj.nameIds != null && i < obj.nameIds.length; i++) {
			int nameId = obj.nameIds[i];
			String value = obj.names.get(nameId);
			BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(nameId);
			if (rt != null) {
				tags.put(rt.getTag(), value);
			}
		}
		for (int i = 0; obj.types != null && i < obj.types.length; i++) {
			BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(obj.types[i]);
			if (rt != null) {
				tags.put(rt.getTag(), rt.getValue());
			}
		}
		return getRouteKeys(tags);
	}

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(BinaryMapDataObject bMdo) {
		Map<String, String> tags = new TreeMap<>();
		for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
			int keyInd = bMdo.getObjectNames().keys()[i];
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(keyInd);
			String value = bMdo.getObjectNames().get(keyInd);
			if (tp != null) {
				tags.put(tp.tag, value);
			}
		}
		int[] tps = bMdo.getAdditionalTypes();
		for (int i = 0; i < tps.length; i++) {
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
			if (tp != null) {
				tags.put(tp.tag, tp.value);
			}
		}
		tps = bMdo.getTypes();
		for (int i = 0; i < tps.length; i++) {
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
			if (tp != null) {
				tags.put(tp.tag, tp.value);
			}
		}
		return getRouteKeys(tags);
	}

	private static int getRouteQuantity(Map<String, String> tags, OsmRouteType rType) {
		int q = 0;
		for (String tag : tags.keySet()) {
			if (tag.startsWith(rType.getTagPrefix())) {
				int num = Algorithms.extractIntegerNumber(tag);
				if (num > 0 && tag.equals(rType.getTagPrefix() + num)) {
					q = Math.max(q, num);
				}
			}
		}
		return q;
	}

	public static List<NetworkRouteSelector.RouteKey> getRouteKeys(Map<String, String> tags) {
		List<NetworkRouteSelector.RouteKey> lst = new ArrayList<>();
		for (OsmRouteType routeType : OsmRouteType.getAllValues()) {
			if (routeType.getRenderingPropertyAttr() == null) {
				continue; // unsupported
			}
			int rq = getRouteQuantity(tags, routeType);
			for (int routeIdx = 1; routeIdx <= rq; routeIdx++) {
				String prefix = routeType.getTagPrefix() + routeIdx;
				NetworkRouteSelector.RouteKey routeKey = new NetworkRouteSelector.RouteKey(routeType);
				for (Map.Entry<String, String> e : tags.entrySet()) {
					String tag = e.getKey();
					if (tag.startsWith(prefix) && tag.length() > prefix.length()) {
						String key = tag.substring(prefix.length() + 1);
						routeKey.addTag(key, e.getValue());
					}
				}
				lst.add(routeKey);
			}
		}
		return lst;
	}

	public static boolean containsUnclickableRouteTags(Map<String, String> tags) {
		for (OsmRouteType routeType : OsmRouteType.getAllValues()) {
			if (routeType.getRenderingPropertyAttr() == null) {
				String routeName = "route_" + routeType.getName();
				String routeNameWithSuffixFirst = routeName + "_1";
				if (tags.containsKey(routeName) || tags.containsKey(routeNameWithSuffixFirst)) {
					return true;
				}
			}
		}
		return ".".equals(tags.get("shield_stub_name"));
	}

	public static class RouteKey {

		public final OsmRouteType type;
		public final Set<String> tags = new TreeSet<>();

		public RouteKey(OsmRouteType routeType) {
			this.type = routeType;
		}

		public String getValue(String key) {
			key = ROUTE_KEY_VALUE_SEPARATOR + key + ROUTE_KEY_VALUE_SEPARATOR;
			for (String tag : tags) {
				int i = tag.indexOf(key);
				if (i > 0) {
					return tag.substring(i + key.length());
				}
			}
			return "";
		}

		public String getKeyFromTag(String tag) {
			String prefix = "route_" + type.getName() + ROUTE_KEY_VALUE_SEPARATOR;
			if (tag.startsWith(prefix) && tag.length() > prefix.length()) {
				int endIdx = tag.indexOf(ROUTE_KEY_VALUE_SEPARATOR, prefix.length());
				return tag.substring(prefix.length(), endIdx != -1 ? endIdx : tag.length());
			}
			return "";
		}

		public void addTag(String key, String value) {
			value = Algorithms.isEmpty(value) ? "" : ROUTE_KEY_VALUE_SEPARATOR + value;
			tags.add("route_" + type.getName() + ROUTE_KEY_VALUE_SEPARATOR + key + value);
		}

		public String getRouteName() {
			return getRouteName(null);
		}

		public String getRouteName(String localeId) {
			return getRouteName(localeId, false);
		}

		public String getRouteName(String localeId, boolean transliteration) {
			String name;
			if (localeId != null) {
				name = getValue("name:" + localeId);
				if (!name.isEmpty()) {
					return name;
				}
			}
			name = getValue("name");
			if (!name.isEmpty()) {
				return transliteration ? TransliterationHelper.transliterate(name) : name;
			}
			name = getValue("ref");
			if (!name.isEmpty()) {
				return name;
			}
			name = getFromTo();
			if (!name.isEmpty()) {
				return name;
			}
			name = getRelationID();
			if (!name.isEmpty()) {
				return name;
			}
			return this.type.getName(); // avoid emptiness
		}

		public String getRelationID() {
			return getValue("relation_id");
		}

		public String getNetwork() {
			return getValue("network");
		}

		public String getOperator() {
			return getValue("operator");
		}

		public String getSymbol() {
			return getValue("symbol");
		}

		public String getWebsite() {
			return getValue("website");
		}

		public String getWikipedia() {
			return getValue("wikipedia");
		}

		public String getRef() {
			return getValue("ref");
		}

		public String getFromTo() {
			String from = getValue("from");
			String to = getValue("to");
			if (!Algorithms.isEmpty(from) && !Algorithms.isEmpty(to)) {
				return from + " - " + to;
			}
			return "";
		}

		public static RouteKey fromGpxFile(GpxFile gpxFile) {
			Map<String, String> networkRouteKeyTags = gpxFile.getNetworkRouteKeyTags();
			String type = networkRouteKeyTags.get(NETWORK_ROUTE_TYPE);
			if (!Algorithms.isEmpty(type)) {
				OsmRouteType routeType = OsmRouteType.getByTag(type);
				if (routeType != null) {
					RouteKey routeKey = new RouteKey(routeType);
					for (Map.Entry<String, String> tag : networkRouteKeyTags.entrySet()) {
						routeKey.addTag(tag.getKey(), tag.getValue());
					}
					return routeKey;
				}
			}
			Metadata metadata = gpxFile.getMetadata();
			Map<String, String> combinedExtensionsTags = new LinkedHashMap<>();
			combinedExtensionsTags.putAll(metadata.getExtensionsToRead());
			combinedExtensionsTags.putAll(gpxFile.getExtensionsToRead());
			return fromShieldTags(combinedExtensionsTags);
		}

		public static final Map<String, String> SHIELD_TO_OSMC = Map.ofEntries(
				Map.entry("shield_bg", "osmc_background"),
				Map.entry("shield_fg", "osmc_foreground"),
				Map.entry("shield_fg_2", "osmc_foreground2"),
				Map.entry("shield_textcolor", "osmc_textcolor"),
				Map.entry("shield_text", "osmc_text")
		);

		public static RouteKey fromShieldTags(Map<String, String> shieldTags) {
			if (!shieldTags.isEmpty()) {
				for (Map.Entry<String, String> entry : SHIELD_TO_OSMC.entrySet()) {
					String shield = entry.getKey();
					String osmc = entry.getValue();
					String value = shieldTags.get(shield);
					if (value != null) {
						if (shield.startsWith("shield_fg") && !value.startsWith("osmc_")) {
							// Apply native OsmAnd shield_fg icon (OsmcIconParams.OSMAND_FOREGROUND)
							shieldTags.put(osmc.replaceFirst("osmc_", "osmand_"), value);
						} else {
							shieldTags.put(osmc, value
									.replaceFirst("^osmc_", "")
									.replaceFirst("_bg$", "")
							);
						}
					}
				}
				RouteKey tagsAsRouteKey = new RouteKey(OsmRouteType.UNKNOWN);
				shieldTags.forEach(tagsAsRouteKey::addTag);
				return tagsAsRouteKey;
			}
			return null;
		}

		public Map<String, String> tagsToGpx() {
			Map<String, String> networkRouteKey = new HashMap<>();
			networkRouteKey.put(NETWORK_ROUTE_TYPE, type.getName());
			for (String tag : tags) {
				String key = getKeyFromTag(tag);
				String value = getValue(key);
				if (!Algorithms.isEmpty(value)) {
					networkRouteKey.put(key, value);
				}
			}
			return networkRouteKey;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + tags.hashCode();
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RouteKey other = (RouteKey) obj;
			if (!tags.equals(other.tags))
				return false;
			return type == other.type;
		}

		@Override
		public String toString() {
			return "Route [type=" + type + ", set=" + tags + "]";
		}
	 }

	public static class NetworkRouteSelectorFilter {
		public Set<RouteKey> keyFilter = null; // null - all
		public Set<OsmRouteType> typeFilter = null; // null - all

		public List<RouteKey> convert(BinaryMapDataObject obj) {
			return filterKeys(getRouteKeys(obj));
		}

		public List<RouteKey> convert(RouteDataObject obj) {
			return filterKeys(getRouteKeys(obj));
		}

		private List<RouteKey> filterKeys(List<RouteKey> keys) {
			if (keyFilter == null && typeFilter == null) {
				return keys;
			}
			Iterator<RouteKey> it = keys.iterator();
			while (it.hasNext()) {
				RouteKey key = it.next();
				if (keyFilter != null && !keyFilter.contains(key)) {
					it.remove();
				} else if (typeFilter != null && !typeFilter.contains(key.type)) {
					it.remove();
				}
			}
			return keys;
		}
	}

}
