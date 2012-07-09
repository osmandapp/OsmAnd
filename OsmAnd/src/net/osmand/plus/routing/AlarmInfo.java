package net.osmand.plus.routing;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

public class AlarmInfo {
	public static int SPEED_CAMERA = 1;
	public static int SPEED_LIMIT = SPEED_CAMERA + 1;
	public static int BORDER_CONTROL = SPEED_LIMIT + 1;
	public static int BARRIER = BORDER_CONTROL + 1;
	public static int TRAFFIC_CALMING = BARRIER + 1;
	public static int TOLL_BOOTH = TRAFFIC_CALMING + 1;
	public static int STOP = TOLL_BOOTH + 1;
	
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
	
	public boolean isSpeedLimit(){
		return type == SPEED_LIMIT;
	}
	
	public boolean isSpeedCamera(){
		return type == SPEED_CAMERA;
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

}
