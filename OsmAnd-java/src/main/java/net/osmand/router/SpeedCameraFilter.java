package net.osmand.router;

import net.osmand.binary.RouteDataObject;

import java.util.HashSet;
import java.util.Set;

// Speed camera relations (type=enforcement) warn only from "from" towards "to".
// Route points are visited in route order: a "from" whose "to" was passed before is the opposite direction.
public class SpeedCameraFilter {
	private static final String RELATION_ID_TAG = "osmand:speed_camera_relation_id"; // "16276089:from, 16276090:to"

	private final Set<String> visitedFrom = new HashSet<>();
	private final Set<String> visitedTo = new HashSet<>();

	// returns false if the speed camera alarm at this point belongs only to opposite directions
	public boolean visitPoint(RouteDataObject rdo, int pointInd) {
		String[] names = rdo.getPointNames(pointInd);
		int[] nameTypes = rdo.getPointNameTypes(pointInd);
		if (names == null || nameTypes == null) {
			return true;
		}
		for (int i = 0; i < nameTypes.length && i < names.length; i++) {
			if (isRelationIdType(rdo, nameTypes[i])) {
				return visitRelations(names[i]);
			}
		}
		return true;
	}

	public static boolean isRelationIdType(RouteDataObject rdo, int nameType) {
		return RELATION_ID_TAG.equals(rdo.region.quickGetEncodingRule(nameType).getTag());
	}

	private boolean visitRelations(String relations) {
		boolean hasFrom = false;
		boolean alarm = false;
		for (String relation : relations.split(", ")) {
			int roleInd = relation.lastIndexOf(':');
			String id = relation.substring(0, roleInd);
			if ("from".equals(relation.substring(roleInd + 1))) {
				hasFrom = true;
				alarm |= !visitedTo.contains(id);
				visitedFrom.add(id);
			} else if (!visitedFrom.contains(id)) {
				visitedTo.add(id);
			}
		}
		return !hasFrom || alarm;
	}
}
