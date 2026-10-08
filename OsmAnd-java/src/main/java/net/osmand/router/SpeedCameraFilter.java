package net.osmand.router;

import net.osmand.binary.RouteDataObject;

import java.util.HashSet;
import java.util.Set;

// Speed camera relations (type=enforcement) warn only from "from" towards "to".
// Route points are visited in route order: a "from" whose "to" was passed before is the opposite direction.
public class SpeedCameraFilter {

	public static final String RELATIONS_INFO_TAG = "osmand:speed_camera_relations_info";

	private final Set<String> visitedFromNodes = new HashSet<>();
	private final Set<String> visitedToNodes = new HashSet<>();

	public static boolean isSpeedCameraRelationsInfoTag(RouteDataObject rdo, int nameType) {
		return RELATIONS_INFO_TAG.equals(rdo.region.quickGetEncodingRule(nameType).getTag());
	}

	// returns "true" if the speed camera alarm at this point belongs only to opposite directions. Should be hidden.
	public boolean checkIsHiddenSpeedCamera(RouteDataObject rdo, int nodeIndex) {
		int[] nodeTags = rdo.getPointNameTypes(nodeIndex);
		String[] nodeValues = rdo.getPointNames(nodeIndex);
		if (nodeTags != null && nodeValues != null) {
			for (int i = 0; i < nodeTags.length; i++) {
				if (isSpeedCameraRelationsInfoTag(rdo, nodeTags[i])) {
					return !shouldAddSpeedCameraAlarm(nodeValues[i]);
				}
			}
		}
		return false;
	}


	// Example: node has "from" role in relation 16276089. and "to" role in relation 16276090.
	// { "osmand:speed_camera_relation_id"  :  "16276089:from, 16276090:to" }
	private boolean shouldAddSpeedCameraAlarm(String nodeRelationsInfoValue) {
		boolean shouldAddAlarm = false;

		//"16276089:from, 16276090:to" -> "16276089:from"
		for (String relationInfoValue : nodeRelationsInfoValue.split(", ")) {
			int separatorIndex = relationInfoValue.lastIndexOf(':');
			String relationId = relationInfoValue.substring(0, separatorIndex);  // "16276089"
			String nodeRoleInRelation = relationInfoValue.substring(separatorIndex + 1);  // "from"
			
			if (nodeRoleInRelation.equals("from")) {
				visitedFromNodes.add(relationId);
				if (!visitedToNodes.contains(relationId)) {
					// moving forward: "from" -> "to". Allow. Add speed camera alarm.
					shouldAddAlarm = true;
				}
			} else if (!visitedFromNodes.contains(relationId)) {
				// moving backward: "to" -> "from". Deny. Don't add speed camera alarm.
				visitedToNodes.add(relationId);
			}
		}
		return shouldAddAlarm;
	}
}
