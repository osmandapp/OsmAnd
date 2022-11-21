package net.osmand.plus.plugins.weather;

import static net.osmand.plus.plugins.weather.WeatherInfoType.TEMPERATURE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;

public class ShowHideTemperatureLayerAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(39,
			"temperature.layer.showhide", ShowHideTemperatureLayerAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.temperature_layer)
			.iconRes(R.drawable.ic_action_thermometer).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideTemperatureLayerAction() {
		super(TYPE);
	}

	public ShowHideTemperatureLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (plugin != null) {
			ApplicationMode appMode = mapActivity.getMyApplication().getSettings().getApplicationMode();
			plugin.toggleLayerEnable(appMode, TEMPERATURE, !plugin.isLayerEnabled(appMode, TEMPERATURE));
			mapActivity.getMapLayers().updateLayers(mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.quick_action_temperature_layer));
		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(@NonNull OsmandApplication app) {
		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		return plugin != null && plugin.isLayerEnabled(app.getSettings().getApplicationMode(), TEMPERATURE);
	}
}
