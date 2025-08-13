package net.osmand.plus.track;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_A;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_W;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.SensorAttributesUtils;
import net.osmand.shared.gpx.PointAttributes;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.CollectionUtils;

public enum Gpx3DVisualizationType {

	NONE("none", R.string.shared_string_none),
	ALTITUDE("altitude", R.string.altitude),
	SPEED("shared_string_speed", R.string.shared_string_speed),
	HEART_RATE("map_widget_ant_heart_rate", R.string.map_widget_ant_heart_rate),
	BICYCLE_CADENCE("map_widget_ant_bicycle_cadence", R.string.map_widget_ant_bicycle_cadence),
	BICYCLE_POWER("map_widget_ant_bicycle_power", R.string.map_widget_ant_bicycle_power),
	TEMPERATURE("shared_string_temperature", R.string.shared_string_temperature),
	SPEED_SENSOR("shared_string_speed", R.string.map_widget_ant_bicycle_speed),
	FIXED_HEIGHT("fixed_height", R.string.fixed_height);

	private final String typeName;
	private final int displayNameResId;

	Gpx3DVisualizationType(@NonNull String typeName, @StringRes int displayNameResId) {
		this.typeName = typeName;
		this.displayNameResId = displayNameResId;
	}

	@NonNull
	public static Gpx3DVisualizationType get3DVisualizationType(@Nullable String typeName) {
		for (Gpx3DVisualizationType type : values()) {
			if (type.typeName.equalsIgnoreCase(typeName)) {
				return type;
			}
		}
		return NONE;
	}

	public String getTypeName() {
		return typeName;
	}

	@StringRes
	public int getDisplayNameResId() {
		return displayNameResId;
	}

	public boolean is3dType() {
		return this != NONE;
	}

	private static final Float SPEED_TO_HEIGHT_SCALE = 10.0f;
	private static final Float TEMPERATURE_TO_HEIGHT_OFFSET = 100.0f;

	public static double getPointElevation(@NonNull WptPt point, @NonNull Track3DStyle style, boolean heightmapsActive) {
		PointAttributes attributes = point.getAttributes();
		Gpx3DVisualizationType type = style.getVisualizationType();

		boolean hasAttributes = attributes != null;
		double pointElevation = getValidElevation(hasAttributes ? attributes.getElevation() : point.getEle());
		double elevation = switch (type) {
			case NONE -> 0;
			case ALTITUDE -> pointElevation;
			case SPEED ->
					(hasAttributes ? attributes.getSpeed() : point.getSpeed()) * SPEED_TO_HEIGHT_SCALE;
			case FIXED_HEIGHT -> style.getElevation();
			case HEART_RATE, BICYCLE_CADENCE, BICYCLE_POWER, TEMPERATURE, SPEED_SENSOR ->
					getSensorElevation(point, type, attributes);
		};
		boolean addGpxHeight = heightmapsActive && !CollectionUtils.equalsToAny(type, ALTITUDE, NONE);
		return addGpxHeight ? elevation + pointElevation : elevation;
	}

	private static float getSensorElevation(@NonNull WptPt point,
	                                        @NonNull Gpx3DVisualizationType type,
	                                        @Nullable PointAttributes attributes) {
		boolean hasAttributes = attributes != null;
		switch (type) {
			case HEART_RATE:
				return hasAttributes ? attributes.getHeartRate() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_HEART_RATE, 0);
			case BICYCLE_CADENCE:
				return hasAttributes ? attributes.getBikeCadence() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_CADENCE, 0);
			case BICYCLE_POWER:
				return hasAttributes ? attributes.getBikePower() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_BIKE_POWER, 0);
			case TEMPERATURE: {
				float airTemp = hasAttributes ? attributes.getAirTemperature() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_TEMPERATURE_A, Float.NaN);
				if (!Float.isNaN(airTemp)) {
					return airTemp + TEMPERATURE_TO_HEIGHT_OFFSET;
				}
				float waterTemp = hasAttributes ? attributes.getWaterTemperature() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_TEMPERATURE_W, Float.NaN);
				if (!Float.isNaN(waterTemp)) {
					return waterTemp + TEMPERATURE_TO_HEIGHT_OFFSET;
				}
			}
			case SPEED_SENSOR: {
				return hasAttributes ? attributes.getSensorSpeed() : SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_BIKE_POWER, 0) * SPEED_TO_HEIGHT_SCALE;
			}
		}
		return 0;
	}

	private static double getValidElevation(double elevation) {
		return Double.isNaN(elevation) ? 0 : elevation;
	}
}