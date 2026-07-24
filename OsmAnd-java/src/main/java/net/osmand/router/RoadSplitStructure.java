package net.osmand.router;

import java.util.ArrayList;
import java.util.List;

public class RoadSplitStructure {
	boolean keepLeft = false;
	boolean keepRight = false;
	boolean speak = false;
	private static final float TURN_SLIGHT_DEGREE = 5;

	List<AttachedRoadInfo> leftLanesInfo = new ArrayList<>();
	int leftLanes = 0;
	int leftMaxPrio = 0;
	int roadsOnLeft = 0;

	List<AttachedRoadInfo> rightLanesInfo = new ArrayList<>();
	int rightLanes = 0;
	int rightMaxPrio = 0;
	int roadsOnRight = 0;

	public boolean allAreStraight() {
		for (AttachedRoadInfo angle : leftLanesInfo) {
			if (Math.abs(angle.attachedAngle) > TURN_SLIGHT_DEGREE) {
				return false;
			}
		}
		for (AttachedRoadInfo angle : rightLanesInfo) {
			if (Math.abs(angle.attachedAngle) > TURN_SLIGHT_DEGREE) {
				return false;
			}
		}
		return true;
	}

	public static class AttachedRoadInfo {
		int[] parsedLanes;
		double attachedAngle;
		int lanes;
		int speakPriority;
		public boolean attachedOnTheRight;
		public int turnType;
	}
	
	public String roadsInfo(List<AttachedRoadInfo> attached) {
		String s = "";
		if (attached != null) {
			int i = 0;
			for (AttachedRoadInfo a : attached) {
				s += String.format("\n  Road %d angle: %.2f, lanes: %d, lanesInfo: %s", 
						++i, a.attachedAngle, a.lanes, a.parsedLanes == null ? "" :TurnType.lanesToString(a.parsedLanes));
			}
		}
		return s;
	}
	
	@Override
	public String toString() {
		return String.format(" Left lanes %d roads %d prioRoads %d: %s\n Right lanes %d roads %d prioRoads: %d %s", 
				leftLanes, roadsOnLeft, leftMaxPrio, roadsInfo(leftLanesInfo),
				rightLanes, roadsOnRight, rightMaxPrio, roadsInfo(rightLanesInfo));
	}
}
