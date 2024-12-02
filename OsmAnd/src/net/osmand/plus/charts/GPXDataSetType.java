package net.osmand.plus.charts;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_SPEED;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.gpx.PointAttributes;

public enum GPXDataSetType {

	ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),
	SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed_outlined, PointAttributes.POINT_SPEED, R.color.gpx_chart_orange_label, R.color.gpx_chart_orange),
	SLOPE(R.string.shared_string_slope, R.drawable.ic_action_slope, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_green_label, R.color.gpx_chart_green),
	ALTITUDE_EXTRM(R.string.altitude, R.drawable.ic_action_altitude_average, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),

	SENSOR_SPEED(R.string.shared_string_speed, R.drawable.ic_action_sensor_speed_outlined, SENSOR_TAG_SPEED, R.color.gpx_chart_yellow_label, R.color.gpx_chart_yellow),
	SENSOR_HEART_RATE(R.string.map_widget_ant_heart_rate, R.drawable.ic_action_sensor_heart_rate_outlined, SENSOR_TAG_HEART_RATE, R.color.gpx_chart_pink_label, R.color.gpx_chart_pink),
	SENSOR_BIKE_POWER(R.string.map_widget_ant_bicycle_power, R.drawable.ic_action_sensor_bicycle_power_outlined, SENSOR_TAG_BIKE_POWER, R.color.gpx_chart_teal_label, R.color.gpx_chart_teal),
	SENSOR_BIKE_CADENCE(R.string.map_widget_ant_bicycle_cadence, R.drawable.ic_action_sensor_cadence_outlined, SENSOR_TAG_CADENCE, R.color.gpx_chart_indigo_label, R.color.gpx_chart_indigo),
	SENSOR_TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, SENSOR_TAG_TEMPERATURE, R.color.gpx_chart_green_label, R.color.gpx_chart_green),

	ZOOM_ANIMATED(R.string.zoom_animated, R.drawable.ic_action_map_zoom, PointAttributes.DEV_ANIMATED_ZOOM, R.color.gpx_chart_teal_label, R.color.gpx_chart_teal),
	ZOOM_NON_ANIMATED(R.string.zoom_non_animated, R.drawable.ic_action_map_zoom, PointAttributes.DEV_RAW_ZOOM, R.color.gpx_chart_indigo_label, R.color.gpx_chart_indigo);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	private final String dataKey;
	@ColorRes
	private final int textColorId;
	@ColorRes
	private final int fillColorId;

	GPXDataSetType(@StringRes int titleId, @DrawableRes int iconId, @NonNull String dataKey, @ColorRes int textColorId, @ColorRes int fillColorId) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.dataKey = dataKey;
		this.textColorId = textColorId;
		this.fillColorId = fillColorId;
	}

	public String getName(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getDataKey() {
		return dataKey;
	}

	@ColorRes
	public int getTextColorId(boolean additional) {
		if (this == SPEED) {
			return additional ? R.color.gpx_chart_red_label : textColorId;
		}
		return textColorId;
	}

	@ColorRes
	public int getFillColorId(boolean additional) {
		if (this == SPEED) {
			return additional ? R.color.gpx_chart_red : fillColorId;
		}
		return fillColorId;
	}

	@NonNull
	public String getMainUnitY(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return switch (this) {
			case ALTITUDE -> {
				boolean shouldUseFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
				yield app.getString(shouldUseFeet ? R.string.foot : R.string.m);
			}
			case SLOPE -> "%";
			case SPEED, SENSOR_SPEED -> settings.SPEED_SYSTEM.get().toShortString();
			case SENSOR_HEART_RATE -> app.getString(R.string.beats_per_minute_short);
			case SENSOR_BIKE_POWER -> app.getString(R.string.power_watts_unit);
			case SENSOR_BIKE_CADENCE -> app.getString(R.string.revolutions_per_minute_unit);
			case SENSOR_TEMPERATURE -> app.getString(R.string.degree_celsius);
			default -> "";
		};
	}
}
