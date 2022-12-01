package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHidePrecipitationLayerAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(40,
			"precipitation.layer.showhide", ShowHidePrecipitationLayerAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.precipitation_layer)
			.iconRes(R.drawable.ic_action_precipitation).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHidePrecipitationLayerAction() {
		super(TYPE);
	}

	public ShowHidePrecipitationLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_PRECIPITATION;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_precipitation_layer;
	}
}
