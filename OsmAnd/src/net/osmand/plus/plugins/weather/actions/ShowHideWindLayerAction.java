package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideWindLayerAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(41,
			"wind.layer.showhide", ShowHideWindLayerAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.wind_layer)
			.iconRes(R.drawable.ic_action_wind).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideWindLayerAction() {
		super(TYPE);
	}

	public ShowHideWindLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_WIND_SPEED;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_wind_layer;
	}
}
