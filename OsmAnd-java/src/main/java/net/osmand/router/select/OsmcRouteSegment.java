package net.osmand.router.select;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;

import java.util.ArrayList;
import java.util.List;

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
		for (int i = 0; i < bMdo.getObjectNames().keys().length; i++) {
			BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(bMdo.getObjectNames().keys()[i]);
			if (tp != null && tp.tag != null && (tp.tag).startsWith(routeKey)) {
				return true;
			}
		}
		return true;
	}
}
