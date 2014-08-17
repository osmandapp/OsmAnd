package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.data.LocationPoint;

public class AlarmInfo implements LocationPoint {
	public enum AlarmInfoType {
		SPEED_CAMERA(1),
		SPEED_LIMIT(2),
		BORDER_CONTROL(3),
		TRAFFIC_CALMING(4),
		TOLL_BOOTH(5),
		STOP(6),
		MAXIMUM(7);
		
		private int priority;

		private AlarmInfoType(int p) {
			this.priority = p;
		}
		
		public int getPriority(){
			return priority;
		}
		
	}
	
	private AlarmInfoType type;
	private float distance;
	private float time;
	protected final int locationIndex;
	private int intValue;
	private double latitude;
	private double longitude;
	
	public AlarmInfo(AlarmInfoType type, int locationIndex){
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
	
	public AlarmInfoType getType() {
		return type;
	}
	
	@Override
	public double getLatitude() {
		return latitude;
	}
	
	@Override
	public double getLongitude() {
		return longitude;
	}
	
	public int getIntValue() {
		return intValue;
	}
	
	public int getLocationIndex() {
		return locationIndex;
	}
	
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	
	public static AlarmInfo createSpeedLimit(int speed, Location loc){
		AlarmInfo info = new AlarmInfo(AlarmInfoType.SPEED_LIMIT, 0);
		info.setLatLon(loc.getLatitude(), loc.getLongitude());
		info.setIntValue(speed);
		return info;
	}
	
	public void setLatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public static AlarmInfo createAlarmInfo(RouteTypeRule ruleType, int locInd, Location loc) {
		AlarmInfo alarmInfo = null;
		if("highway".equals(ruleType.getTag())) {
			if("speed_camera".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(AlarmInfoType.SPEED_CAMERA, locInd);
			} else if("stop".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(AlarmInfoType.STOP, locInd);	
			}
		} else if("barrier".equals(ruleType.getTag())) {
			if("toll_booth".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(AlarmInfoType.TOLL_BOOTH, locInd);
			} else if("border_control".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(AlarmInfoType.BORDER_CONTROL, locInd);
			}
		} else if("traffic_calming".equals(ruleType.getTag())) {
			alarmInfo = new AlarmInfo(AlarmInfoType.TRAFFIC_CALMING, locInd);
		}
		if(alarmInfo != null) {
			alarmInfo.setLatLon(loc.getLatitude(), loc.getLongitude());
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
		if (time < 8 || distance < 100 || type == AlarmInfoType.SPEED_LIMIT) {
			return type.getPriority();
		}
		if (type == AlarmInfoType.SPEED_CAMERA && (time < 15 || distance < 150)) {
			return type.getPriority();
		}
		// 2nd level
		if (time < 10 || distance < 150) {
			return type.getPriority() + AlarmInfoType.MAXIMUM.getPriority();
		}
		return 0;
	}

	@Override
	public String getName() {
		return type.name();
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public boolean isVisible() {
		return false;
	}


}
