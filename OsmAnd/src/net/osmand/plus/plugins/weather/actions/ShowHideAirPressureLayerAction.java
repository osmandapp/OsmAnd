package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideAirPressureLayerAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(43,
			"pressure.layer.showhide", ShowHideAirPressureLayerAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.pressure_layer)
			.iconRes(R.drawable.ic_action_air_pressure).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideAirPressureLayerAction() {
		super(TYPE);
	}

	public ShowHideAirPressureLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		WeatherHelper weatherHelper = app.getWeatherHelper();
		WeatherBand weatherBand = weatherHelper.getWeatherBand(WEATHER_BAND_PRESSURE);
		if (weatherBand != null) {
			boolean visible = !weatherBand.isBandVisible();
			weatherBand.setBandVisible(visible);
			mapActivity.getMapLayers().updateLayers(mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.quick_action_air_pressure_layer));
		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication app) {
		String nameRes = app.getString(getNameRes());
		String actionName = isActionWithSlash(app) ? app.getString(R.string.shared_string_hide) : app.getString(R.string.shared_string_show);
		return app.getString(R.string.ltr_or_rtl_combine_via_dash, actionName, nameRes);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication app) {
		WeatherBand weatherBand = app.getWeatherHelper().getWeatherBand(WEATHER_BAND_PRESSURE);
		return weatherBand != null && weatherBand.isBandVisible();
	}
}
