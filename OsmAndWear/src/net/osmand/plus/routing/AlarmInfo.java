package net.osmand.plus.routing;

import static net.osmand.data.PointDescription.POINT_TYPE_ALARM;
import static net.osmand.plus.routing.AlarmInfoType.BORDER_CONTROL;
import static net.osmand.plus.routing.AlarmInfoType.HAZARD;
import static net.osmand.plus.routing.AlarmInfoType.MAXIMUM;
import static net.osmand.plus.routing.AlarmInfoType.PEDESTRIAN;
import static net.osmand.plus.routing.AlarmInfoType.RAILWAY;
import static net.osmand.plus.routing.AlarmInfoType.SPEED_CAMERA;
import static net.osmand.plus.routing.AlarmInfoType.SPEED_LIMIT;
import static net.osmand.plus.routing.AlarmInfoType.STOP;
import static net.osmand.plus.routing.AlarmInfoType.TOLL_BOOTH;
import static net.osmand.plus.routing.AlarmInfoType.TRAFFIC_CALMING;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;

public class AlarmInfo implements LocationPoint {

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

	@NonNull
	public static AlarmInfo createSpeedLimit(int speed, @NonNull Location location) {
		AlarmInfo info = new AlarmInfo(SPEED_LIMIT, 0);
		info.setLatLon(location.getLatitude(), location.getLongitude());
		info.setIntValue(speed);
		return info;
	}

	public void setLatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Nullable
	public static AlarmInfo createAlarmInfo(@NonNull RouteTypeRule ruleType, int locInd, @NonNull Location loc) {
		AlarmInfo alarmInfo = null;
		if ("highway".equals(ruleType.getTag())) {
			if ("speed_camera".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(SPEED_CAMERA, locInd);
			} else if ("stop".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(STOP, locInd);
			}
		} else if ("barrier".equals(ruleType.getTag())) {
			if ("toll_booth".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(TOLL_BOOTH, locInd);
			} else if ("border_control".equals(ruleType.getValue())) {
				alarmInfo = new AlarmInfo(BORDER_CONTROL, locInd);
			}
		} else if ("traffic_calming".equals(ruleType.getTag())) {
			alarmInfo = new AlarmInfo(TRAFFIC_CALMING, locInd);
		} else if ("hazard".equals(ruleType.getTag())) {
			alarmInfo = new AlarmInfo(HAZARD, locInd);
		} else if ("railway".equals(ruleType.getTag()) && "level_crossing".equals(ruleType.getValue())) {
			alarmInfo = new AlarmInfo(RAILWAY, locInd);
		} else if ("crossing".equals(ruleType.getTag()) && "uncontrolled".equals(ruleType.getValue())) {
			alarmInfo = new AlarmInfo(PEDESTRIAN, locInd);
		}
		if (alarmInfo != null) {
			alarmInfo.setLatLon(loc.getLatitude(), loc.getLongitude());
		}
		return alarmInfo;
	}

	public int updateDistanceAndGetPriority(float time, float distance) {
		if (distance > 1500) {
			return Integer.MAX_VALUE;
		}
		// 1 level of priorities
		if (time < 6 || distance < 75 || type == SPEED_LIMIT) {
			return type.getPriority();
		}
		if (type == SPEED_CAMERA && (time < 15 || distance < 150)) {
			return type.getPriority();
		}
		if (type == TOLL_BOOTH && (time < 30 || distance < 500)) {
			return type.getPriority();
		}
		// 2nd level
		if (time < 7 || distance < 100) {
			return type.getPriority() + MAXIMUM.getPriority();
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(POINT_TYPE_ALARM, type.getVisualName(ctx));
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
