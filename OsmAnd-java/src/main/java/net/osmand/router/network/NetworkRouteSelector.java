package net.osmand.router.network;


import net.osmand.osm.OsmRouteType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class NetworkRouteSelector {

	public static final String ROUTE_KEY_VALUE_SEPARATOR = "__";
	public static final String NETWORK_ROUTE_TYPE = "type";

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
		if (tags.containsKey("highway") && (tags.containsKey("route_road") || tags.containsKey("route_road_1"))) {
			return true; // avoid road shields but allow click on bus/tram/train/railway
		}
		return ".".equals(tags.get("shield_stub_name")); // avoid double-click on empty shields (v1 only)
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
		public Set<OsmRouteType> typeFilter = null; // null - all
	}

}
