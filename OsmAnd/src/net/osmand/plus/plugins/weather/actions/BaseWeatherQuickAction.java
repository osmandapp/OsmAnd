package net.osmand.plus.plugins.weather.actions;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public abstract class BaseWeatherQuickAction extends QuickAction {

	public BaseWeatherQuickAction(QuickActionType type) {
		super(type);
	}

	public BaseWeatherQuickAction(QuickAction quickAction) {
		super(quickAction);
	}

	public abstract QuickActionType getActionType();

	public abstract short getWeatherBand();

	@StringRes
	public abstract int getQuickActionDescription();

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		OsmandApplication app = mapActivity.getApp();
		WeatherHelper weatherHelper = app.getWeatherHelper();
		WeatherBand weatherBand = weatherHelper.getWeatherBand(getWeatherBand());
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);

		if (weatherBand != null && weatherPlugin != null) {
			boolean visible = !weatherBand.isBandVisible();
			if (visible && !PluginsHelper.isEnabled(WeatherPlugin.class)) {
				PluginsHelper.enablePlugin(mapActivity, app, weatherPlugin, true);
			}
			weatherBand.setBandVisible(visible);
			weatherPlugin.setWeatherEnabled(weatherHelper.hasVisibleBands());
			mapActivity.getMapLayers().updateLayers(mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(getQuickActionDescription()));
		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		WeatherBand weatherBand = app.getWeatherHelper().getWeatherBand(getWeatherBand());
		return weatherBand != null && weatherBand.isBandVisible();
	}
}