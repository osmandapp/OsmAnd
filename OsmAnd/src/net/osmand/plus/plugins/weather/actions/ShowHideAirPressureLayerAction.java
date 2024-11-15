package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_AIR_PRESSURE_LAYER_ACTION_ID;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideAirPressureLayerAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_AIR_PRESSURE_LAYER_ACTION_ID,
			"pressure.layer.showhide", ShowHideAirPressureLayerAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
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
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_PRESSURE;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_air_pressure_layer;
	}
}
