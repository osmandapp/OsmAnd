package net.osmand.plus.track;

import static net.osmand.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;
import static net.osmand.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_A;
import static net.osmand.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_W;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.SensorAttributesUtils;
import net.osmand.plus.plugins.srtm.SRTMPlugin;

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

	public static double getPointElevation(@NonNull WptPt point, @NonNull Track3DStyle style, boolean useExaggeration) {
		Gpx3DVisualizationType type = style.getVisualizationType();
		double elevation = switch (type) {
			case NONE -> 0;
			case ALTITUDE -> getValidElevation(point.ele);
			case SPEED -> point.speed * SPEED_TO_HEIGHT_SCALE;
			case FIXED_HEIGHT -> style.getElevation();
			case HEART_RATE, BICYCLE_CADENCE, BICYCLE_POWER, TEMPERATURE, SPEED_SENSOR ->
					getSensorElevation(point, type);
		};
		return type != FIXED_HEIGHT && useExaggeration ? elevation * style.getExaggeration() : elevation;
	}

	public static float getSensorElevation(@NonNull WptPt point, @NonNull Gpx3DVisualizationType type) {
		double elevationValue = getValidElevation(point.ele);
		float processedValue = processSensorData(point, type);
		return is3DMapsEnabled() ? processedValue + (float) elevationValue : processedValue;
	}

	public static float processSensorData(@NonNull WptPt point, @NonNull Gpx3DVisualizationType type) {
		switch (type) {
			case HEART_RATE:
				return SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_HEART_RATE, 0);
			case BICYCLE_CADENCE:
				return SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_CADENCE, 0);
			case BICYCLE_POWER:
				return SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_BIKE_POWER, 0);
			case TEMPERATURE: {
				float airTemp = SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_TEMPERATURE_A, Float.NaN);
				if (!Float.isNaN(airTemp)) {
					return airTemp * TEMPERATURE_TO_HEIGHT_OFFSET;
				}
				float waterTemp = SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_TEMPERATURE_W, Float.NaN);
				if (!Float.isNaN(waterTemp)) {
					return waterTemp * TEMPERATURE_TO_HEIGHT_OFFSET;
				}
			}
			case SPEED_SENSOR: {
				return SensorAttributesUtils.getPointAttribute(point, SENSOR_TAG_BIKE_POWER, 0) * SPEED_TO_HEIGHT_SCALE;
			}
		}
		return 0;
	}

	private static double getValidElevation(double elevation) {
		return Double.isNaN(elevation) ? 0 : elevation;
	}

	private static boolean is3DMapsEnabled() {
		SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		return plugin != null && plugin.is3DMapsEnabled();
	}
}