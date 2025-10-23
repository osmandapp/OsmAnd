package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_WEATHER_LAYERS;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.UiUtilities;

public class ShowHideWeatherLayersAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_WEATHER_LAYERS,
			"weather.layers.showhide", ShowHideWeatherLayersAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.weather_layers)
			.iconRes(R.drawable.ic_action_umbrella).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideWeatherLayersAction() {
		super(TYPE);
	}

	public ShowHideWeatherLayersAction(QuickActionType type) {
		super(type);
	}

	public ShowHideWeatherLayersAction(QuickAction quickAction) {
		super(quickAction);
	}

	public QuickActionType getActionType(){
		return TYPE;
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);

		if (weatherPlugin != null) {
			weatherPlugin.setWeatherEnabled(!weatherPlugin.isWeatherEnabled());
			mapActivity.getMapLayers().updateLayers(mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity, boolean nightMode) {
		View view = UiUtilities.inflate(parent.getContext(), nightMode, R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.quick_action_weather_layers));
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
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (weatherPlugin != null) {
			return weatherPlugin.isWeatherEnabled();
		}
		return false;
	}
}