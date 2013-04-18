package net.osmand.plus.routing;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

public class AlarmInfo {
	public static int SPEED_CAMERA = 1;
	public static int SPEED_LIMIT = SPEED_CAMERA + 1;
	public static int BORDER_CONTROL = SPEED_LIMIT + 1;
	public static int TRAFFIC_CALMING = BORDER_CONTROL + 1;
	public static int TOLL_BOOTH = TRAFFIC_CALMING + 1;
	public static int STOP = TOLL_BOOTH + 1;
	public static int MAXIMUM = STOP + 1;
	
	private int type;
	private float distance;
	private float time;
	protected final int locationIndex;
	private int intValue;
	
	public AlarmInfo(int type, int locationIndex){
		this.type = type;
		this.locationIndex = locationIndex;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public float getTime() {
		return time;
	}
	
	
	public void setTime(float time) {
		this.time = time;
	}
	
	public void setDistance(float distance) {
		this.distance = distance;
	}
	
	public int getType() {
		return type;
	}
	
	public int getIntValue() {
		return intValue;
	}
	
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	
	
	
	public static AlarmInfo createSpeedLimit(int speed){
		AlarmInfo info = new AlarmInfo(SPEED_LIMIT, 0);
		info.setIntValue(speed);
		return info;
	}
	
	public static AlarmInfo createAlarmInfo(RouteTypeRule ruleType, int locInd) {
		if("highway".equals(ruleType.getTag())) {
			if("speed_camera".equals(ruleType.getValue())) {
				return new AlarmInfo(SPEED_CAMERA, locInd);
			} else if("stop".equals(ruleType.getValue())) {
				return new AlarmInfo(STOP, locInd);	
			}
		} else if("barrier".equals(ruleType.getTag())) {
			if("toll_booth".equals(ruleType.getValue())) {
				return new AlarmInfo(TOLL_BOOTH, locInd);
			} else if("border_control".equals(ruleType.getValue())) {
				return new AlarmInfo(BORDER_CONTROL, locInd);
			}
		} else if("traffic_calming".equals(ruleType.getTag())) {
			return new AlarmInfo(TRAFFIC_CALMING, locInd);
		}
		return null;
	}
	
	public int updateDistanceAndGetPriority(float time, float distance) {
		this.distance = distance;
		this.time = time;
		if (distance > 1500) {
			return 0;
		}
		// 1 level of priorities
		if (time < 8 || distance < 100 || type == SPEED_LIMIT) {
			return type;
		}
		if (type == SPEED_CAMERA && (time < 15 || distance < 150)) {
			return type;
		}
		// 2nd level
		if (time < 10 || distance < 150) {
			return type + MAXIMUM;
		}
		return 0;
	}

}
