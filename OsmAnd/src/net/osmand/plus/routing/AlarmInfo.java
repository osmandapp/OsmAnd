package net.osmand.plus.routing;

import static net.osmand.data.PointDescription.POINT_TYPE_ALARM;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.shared.routing.details.RouteEventHelper;
import net.osmand.shared.routing.details.RouteEventType;

public class AlarmInfo implements LocationPoint {

	private final RouteEventType type;
	protected final int locationIndex;
	private int lastLocationIndex = -1;
	private int intValue;
	private float floatValue;
	private double latitude;
	private double longitude;
	@Nullable private String sourceTag;
	@Nullable private String sourceValue;

	public AlarmInfo(@NonNull RouteEventType type, int locationIndex) {
		this.type = type;
		this.locationIndex = locationIndex;
	}

	@NonNull
	public RouteEventType getType() {
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

	@Nullable
	public String getSourceTag() {
		return sourceTag;
	}

	@Nullable
	public String getSourceValue() {
		return sourceValue;
	}

	public void setLastLocationIndex(int lastLocationIndex) {
		this.lastLocationIndex = lastLocationIndex;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	@NonNull
	public static AlarmInfo createSpeedLimit(int speed, @NonNull Location location, float speedMetersPerSecond) {
		AlarmInfo info = new AlarmInfo(RouteEventType.SPEED_LIMIT, 0);
		info.setLatLon(location.getLatitude(), location.getLongitude());
		info.setIntValue(speed);
		info.setFloatValue(speedMetersPerSecond);
		return info;
	}

	public void setLatLon(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Nullable
	public static AlarmInfo createAlarmInfo(@NonNull RouteTypeRule ruleType, int locInd, @NonNull Location loc) {
		RouteEventType type = RouteEventHelper.INSTANCE.classifyType(ruleType.getTag(), ruleType.getValue());
		if (type == null) {
			return null;
		}
		AlarmInfo alarmInfo = new AlarmInfo(type, locInd);
		alarmInfo.sourceTag = ruleType.getTag();
		alarmInfo.sourceValue = ruleType.getValue();
		alarmInfo.setLatLon(loc.getLatitude(), loc.getLongitude());
		return alarmInfo;
	}

	public int updateDistanceAndGetPriority(float time, float distance) {
		return RouteEventHelper.INSTANCE.updateDistanceAndGetPriority(type, time, distance);
	}

	@NonNull
	public static String getVisualName(@NonNull Context ctx, @NonNull RouteEventType type) {
		// Android resource IDs stay in the Android wrapper; only the backend event type is shared.
		switch (type) {
			case SPEED_CAMERA:
				return ctx.getString(R.string.traffic_warning_speed_camera);
			case SPEED_LIMIT:
				return ctx.getString(R.string.traffic_warning_speed_limit);
			case BORDER_CONTROL:
				return ctx.getString(R.string.traffic_warning_border_control);
			case RAILWAY:
				return ctx.getString(R.string.traffic_warning_railways);
			case TRAFFIC_CALMING:
				return ctx.getString(R.string.traffic_warning_calming);
			case TOLL_BOOTH:
				return ctx.getString(R.string.traffic_warning_payment);
			case STOP:
				return ctx.getString(R.string.traffic_warning_stop);
			case PEDESTRIAN:
				return ctx.getString(R.string.traffic_warning_pedestrian);
			case HAZARD:
				return ctx.getString(R.string.traffic_warning_hazard);
			case MAXIMUM:
				return ctx.getString(R.string.traffic_warning);
			case TUNNEL:
				return ctx.getString(R.string.tunnel_warning);
			case RED_LIGHT_CAMERA:
				return ctx.getString(R.string.traffic_warning_red_light_camera);
			default:
				throw new IllegalArgumentException("Unsupported route event type: " + type);
		}
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(POINT_TYPE_ALARM, getVisualName(ctx, type));
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
