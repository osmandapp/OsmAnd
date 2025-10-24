package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_ANIMATION;
import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_WIND_ANIMATION_LAYER;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideWindAnimationAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_WIND_ANIMATION_LAYER,
			"wind.animation.layer.showhide", ShowHideWindAnimationAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.wind_animation_layer)
			.iconRes(R.drawable.ic_action_wind).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideWindAnimationAction() {
		super(TYPE);
	}

	public ShowHideWindAnimationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_WIND_ANIMATION;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_wind_animation_layer;
	}
}
