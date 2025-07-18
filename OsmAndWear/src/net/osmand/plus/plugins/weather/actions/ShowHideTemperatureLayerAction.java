package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_TEMPERATURE_LAYER_ACTION_ID;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideTemperatureLayerAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_TEMPERATURE_LAYER_ACTION_ID,
			"temperature.layer.showhide", ShowHideTemperatureLayerAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
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
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_TEMPERATURE;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_temperature_layer;
	}
}
