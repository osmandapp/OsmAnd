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

	public static boolean isSpeedCameraRelationIdTag(RouteDataObject rdo, int nameType) {
		return RELATION_ID_TAG.equals(rdo.region.quickGetEncodingRule(nameType).getTag());
	}

	// returns true if the speed camera alarm at this point belongs only to opposite directions
	public boolean checkIsHiddenSpeedCamera(RouteDataObject rdo, int pointIndex) {
		String[] names = rdo.getPointNames(pointIndex);
		int[] nameTypes = rdo.getPointNameTypes(pointIndex);
		if (names != null && nameTypes != null) {
			for (int i = 0; i < nameTypes.length && i < names.length; i++) {
				if (isSpeedCameraRelationIdTag(rdo, nameTypes[i])) {
					return shouldHideNodeAlarm(names[i]);
				}
			}
		}
		return false;
	}

	private boolean shouldHideNodeAlarm(String nodeRelationsTags) {
		boolean nodeHasFromRole = false;
		boolean shouldAddAlarm = false;
		for (String relationTag : nodeRelationsTags.split(", ")) {
			int separatorIndex = relationTag.lastIndexOf(':');
			String relationId = relationTag.substring(0, separatorIndex);
			String role = relationTag.substring(separatorIndex + 1);
			
			if (role.equals("from")) {
				nodeHasFromRole = true;
				visitedFrom.add(relationId);
				if (!visitedTo.contains(relationId)) {
					// moving forward: "from" -> "to". Add speedcam alarm.
					shouldAddAlarm = true;
				}
			} else if (!visitedFrom.contains(relationId)) {
				// moving backward: "to" -> "from". Don't add speedcam alarm.
				visitedTo.add(relationId);
			}
		}
		return nodeHasFromRole && !shouldAddAlarm;
	}
}
