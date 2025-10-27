package net.osmand.gpx.clickable;


import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClickableWayTags {
	public static final Set<String> CLICKABLE_TAGS =
			Set.of("piste:type", "piste:difficulty", "mtb:scale", "dirtbike:scale",
					"snowmobile=yes", "snowmobile=designated", "snowmobile=permissive"
			);
	public static final Map<String, String> FORBIDDEN_TAGS =
			Map.of("area", "yes",
					"access", "no",
					"piste:type", "connection",
					"osmc_stub_name", ".",
					"aerialway", "*"
			);
	public static final Set<String> REQUIRED_TAGS_ANY =
			Set.of("name", "ref", "piste:name", "mtb:name", "shield_stub_name");
	public static final Map<String, String> GPX_COLORS = Map.ofEntries(
			Map.entry("0", "brown"),
			Map.entry("1", "green"),
			Map.entry("2", "blue"),
			Map.entry("3", "red"),
			Map.entry("4", "black"),
			Map.entry("5", "black"),
			Map.entry("6", "black"),
			Map.entry("novice", "green"),
			Map.entry("easy", "blue"),
			Map.entry("intermediate", "red"),
			Map.entry("advanced", "black"),
			Map.entry("expert", "black"),
			Map.entry("freeride", "yellow")
			// others use default track color (red)
	);

	public static String getGpxColorByTags(Map<String, String> tags) {
		for (String t : CLICKABLE_TAGS) {
			String val = tags.get(t);
			if (val != null) {
				for (Map.Entry<String, String> matchColor : GPX_COLORS.entrySet()) {
					if (val.contains(matchColor.getKey())) {
						return matchColor.getValue();
					}
				}
			}
		}
		return null;
	}

	public static Map<String, String> getGpxShieldTags(String color) {
		return color != null ? Map.of("shield_fg", "osmc_" + color + "_bar") : new HashMap<>();
	}

	public static boolean isClickableWayTags(String name, Map<String, String> tags) {
		for (Map.Entry<String, String> forbidden : FORBIDDEN_TAGS.entrySet()) {
			if (forbidden.getValue().equals(tags.get(forbidden.getKey()))
					|| "*".equals(forbidden.getValue()) && tags.containsKey(forbidden.getKey())
			) {
				return false;
			}
		}
		for (String required : REQUIRED_TAGS_ANY) {
			// some objects have name passed from object props but not in the tags
			// nameless objects may be included using a fake name, such as "shield_stub_name"
			boolean isRequiredNameFound = "name".equals(required) && !Algorithms.isEmpty(name);
			if (tags.containsKey(required) || isRequiredNameFound) {
				for (String key : tags.keySet()) {
					if (CLICKABLE_TAGS.contains(key)) {
						return true;
					}
					String value = tags.get(key); // snowmobile=yes, etc
					if (value != null && CLICKABLE_TAGS.contains(key + "=" + value)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
