package net.osmand.router.select;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.util.Algorithms;

import java.util.*;

import static net.osmand.binary.BinaryMapIndexReader.*;
import static net.osmand.router.select.RouteSelector.*;

public class OsmcRouteSegment {
	private final List<BinaryMapDataObject> binaryMapDataObjects = new ArrayList<>();

	public OsmcRouteSegment(BinaryMapDataObject bMdo) {
		addObject(bMdo);
	}

	void addObject(BinaryMapDataObject bMdo) {
		binaryMapDataObjects.add(bMdo);
	}

	public List<BinaryMapDataObject> getObjects() {
		return binaryMapDataObjects;
	}

	public List<BinaryMapDataObject> getObjectsByRouteKey(String routeKey) {
		List<BinaryMapDataObject> list = new ArrayList<>();
		for (BinaryMapDataObject bMdo : binaryMapDataObjects) {
			if (hasRouteKey(bMdo, routeKey)) {
				list.add(bMdo);
			}
		}
		return list;
	}

	private boolean hasRouteKey(BinaryMapDataObject bMdo, String routeKey) {
		Map<Integer, List<String>> objectTagMap = new HashMap<>();
		for (RouteType routeType : RouteType.values()) {
			for (int routeIdx = 1; routeIdx <= getRouteQuantity(bMdo); routeIdx++) {
				String prefix = routeType.getTypeWithPrefix() + "_" + routeIdx;
				for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
					TagValuePair tp = bMdo.getMapIndex().decodeType(bMdo.getObjectNames().keys()[i]);
					if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
						String tagWoPrefix = tp.tag;
						String value = tagWoPrefix + ROUTE_KEY_VALUE_SEPARATOR
								+ bMdo.getObjectNames().get(bMdo.getObjectNames().keys()[i]);
						putTag(objectTagMap, routeIdx, value);
					}
				}
				int[] allTypes = Arrays.copyOf(bMdo.getTypes(), bMdo.getTypes().length
						+ bMdo.getAdditionalTypes().length);
				System.arraycopy(bMdo.getAdditionalTypes(), 0, allTypes, bMdo.getTypes().length,
						bMdo.getAdditionalTypes().length);
				for (int allType : allTypes) {
					TagValuePair tp = bMdo.getMapIndex().decodeType(allType);
					if (tp != null && tp.tag != null && (tp.tag).startsWith(prefix)) {
						String tagWoPrefix = tp.tag;
						String value = tagWoPrefix
								+ (Algorithms.isEmpty(tp.value) ? "" : ROUTE_KEY_VALUE_SEPARATOR + tp.value);
						putTag(objectTagMap, routeIdx, value);
					}
				}
			}
			if (!objectTagMap.isEmpty()) {
				for (Map.Entry<Integer, List<String>> entry : objectTagMap.entrySet()) {
					List<String> objectTagList = entry.getValue();
					Collections.sort(objectTagList);
					String objectTagKey = getRouteStringKey(objectTagList, routeType.getTypeWithPrefix());
					if (Algorithms.stringsEqual(routeKey, objectTagKey)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void putTag(Map<Integer, List<String>> objectTagMap, int routeIdx, String value) {
		List<String> tagList = objectTagMap.get(routeIdx);
		if (tagList == null) {
			tagList = new ArrayList<>();
		}
		tagList.add(value);
		objectTagMap.put(routeIdx, tagList);
	}

	private int getRouteQuantity(BinaryMapDataObject object) {
		List<String> tagsList = new ArrayList<>();
		for (int i = 0; i < object.getAdditionalTypes().length; i++) {
			TagValuePair tp = object.getMapIndex().decodeType(object.getAdditionalTypes()[i]);
			tagsList.add(tp.tag);
		}
		return RouteSelector.getRouteQuantity(tagsList);
	}
}
