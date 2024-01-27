package net.osmand.plus.routing;

import android.content.Context;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;

import androidx.annotation.NonNull;

public class AlarmInfo implements LocationPoint {
	public enum AlarmInfoType {
		SPEED_CAMERA(1, R.string.traffic_warning_speed_camera),
		SPEED_LIMIT(2, R.string.traffic_warning_speed_limit),
		BORDER_CONTROL(3, R.string.traffic_warning_border_control),
		RAILWAY(4, R.string.traffic_warning_railways),
		TRAFFIC_CALMING(5, R.string.traffic_warning_calming),
		TOLL_BOOTH(6, R.string.traffic_warning_payment),
		STOP(7, R.string.traffic_warning_stop),
		PEDESTRIAN(8, R.string.traffic_warning_pedestrian),
		HAZARD(9, R.string.traffic_warning_hazard),
		MAXIMUM(10, R.string.traffic_warning),
		TUNNEL(11, R.string.tunnel_warning);
		
		private final int priority;
		private final int string;

		AlarmInfoType(int p, int string) {
			this.priority = p;
			this.string = string;
		}
		
		public int getPriority(){
			return priority;
		}
		
		
		public String getVisualName(Context ctx) {
			return ctx.getString(string);
		}
	}
	
	private final AlarmInfoType type;
	protected final int locationIndex;
	private int lastLocationIndex = -1;
	private int intValue;
	private float floatValue;
	private double latitude;
	private double longitude;
	
	public AlarmInfo(@NonNull AlarmInfoType type, int locationIndex) {
		this.type = type;
		this.locationIndex = locationIndex;
	}
	
	@NonNull
	public AlarmInfoType getType() {
		return type;
	}

	public float getFloatValue() {
		return floatValue;
	}

	public void setFloatValue(float floatValue) {
		this.floatValue = floatValue;
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

	public int getLastLocationIndex() {
		return lastLocationIndex;
	}

	public void setLastLocationIndex(int lastLocationIndex) {
		this.lastLocationIndex = lastLocationIndex;
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
		} else if("hazard".equals(ruleType.getTag())) {
			alarmInfo = new AlarmInfo(AlarmInfoType.HAZARD, locInd);
		} else if ("railway".equals(ruleType.getTag()) && "level_crossing".equals(ruleType.getValue())) {
			alarmInfo = new AlarmInfo(AlarmInfoType.RAILWAY, locInd);
		} else if ("crossing".equals(ruleType.getTag()) && "uncontrolled".equals(ruleType.getValue())){
			alarmInfo = new AlarmInfo(AlarmInfoType.PEDESTRIAN, locInd);
		}
		if(alarmInfo != null) {
			alarmInfo.setLatLon(loc.getLatitude(), loc.getLongitude());
		}
		return alarmInfo;
	}
	
	public int updateDistanceAndGetPriority(float time, float distance) {
		if (distance > 1500) {
			return Integer.MAX_VALUE;
		}
		// 1 level of priorities
		if (time < 6 || distance < 75 || type == AlarmInfoType.SPEED_LIMIT) {
			return type.getPriority();
		}
		if (type == AlarmInfoType.SPEED_CAMERA && (time < 15 || distance < 150)) {
			return type.getPriority();
		}
		if (type == AlarmInfoType.TOLL_BOOTH && (time < 30 || distance < 500)) {
			return type.getPriority();
		}
		// 2nd level
		if (time < 7 || distance < 100) {
			return type.getPriority() + AlarmInfoType.MAXIMUM.getPriority();
		}
		return Integer.MAX_VALUE;
	}
	
	@Override
	public PointDescription getPointDescription(Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_ALARM, type.getVisualName(ctx));
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
