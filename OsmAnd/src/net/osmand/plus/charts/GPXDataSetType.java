package net.osmand.plus.charts;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.SensorAttributesUtils;
import net.osmand.plus.settings.backend.OsmandSettings;

public enum GPXDataSetType {

	ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average, GPXUtilities.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),
	SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed, GPXUtilities.POINT_SPEED, R.color.gpx_chart_orange_label, R.color.gpx_chart_orange),
	SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent, GPXUtilities.POINT_ELEVATION, R.color.gpx_chart_green_label, R.color.gpx_chart_green),
	ALTITUDE_EXTRM(R.string.altitude, R.drawable.ic_action_altitude_average, GPXUtilities.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),

	SENSOR_SPEED(R.string.shared_string_speed, R.drawable.ic_action_sensor_speed_outlined, SensorAttributesUtils.SENSOR_TAG_SPEED, R.color.gpx_chart_yellow_label, R.color.gpx_chart_yellow),
	SENSOR_HEART_RATE(R.string.map_widget_ant_heart_rate, R.drawable.ic_action_sensor_heart_rate_outlined, SensorAttributesUtils.SENSOR_TAG_HEART_RATE, R.color.gpx_chart_pink_label, R.color.gpx_chart_pink),
	SENSOR_BIKE_POWER(R.string.map_widget_ant_bicycle_power, R.drawable.ic_action_sensor_bicycle_power_outlined, SensorAttributesUtils.SENSOR_TAG_BIKE_POWER, R.color.gpx_chart_teal_label, R.color.gpx_chart_teal),
	SENSOR_BIKE_CADENCE(R.string.map_widget_ant_bicycle_cadence, R.drawable.ic_action_sensor_cadence_outlined, SensorAttributesUtils.SENSOR_TAG_CADENCE, R.color.gpx_chart_indigo_label, R.color.gpx_chart_indigo),
	SENSOR_TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, SensorAttributesUtils.SENSOR_TAG_TEMPERATURE, R.color.gpx_chart_green_label, R.color.gpx_chart_green);


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
		switch (this) {
			case ALTITUDE: {
				boolean shouldUseFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
				return app.getString(shouldUseFeet ? R.string.foot : R.string.m);
			}
			case SLOPE: {
				return "%";
			}
			case SPEED:
			case SENSOR_SPEED: {
				return settings.SPEED_SYSTEM.get().toShortString(app);
			}
			case SENSOR_HEART_RATE: {
				return app.getString(R.string.beats_per_minute_short);
			}
			case SENSOR_BIKE_POWER: {
				return app.getString(R.string.power_watts_unit);
			}
			case SENSOR_BIKE_CADENCE: {
				return app.getString(R.string.revolutions_per_minute_unit);
			}
			case SENSOR_TEMPERATURE:
				return "";
		}
		return "";
	}
}
