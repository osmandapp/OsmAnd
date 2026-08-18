package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.quickaction.QuickActionIds.SHOW_HIDE_CLOUD_LAYER_ACTION_ID;

import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ShowHideCloudLayerAction extends BaseWeatherQuickAction {

	public static final QuickActionType TYPE = new QuickActionType(SHOW_HIDE_CLOUD_LAYER_ACTION_ID,
			"cloud.layer.showhide", ShowHideCloudLayerAction.class)
			.nameActionRes(R.string.quick_action_verb_show_hide)
			.nameRes(R.string.cloud_layer)
			.iconRes(R.drawable.ic_action_clouds).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideCloudLayerAction() {
		super(TYPE);
	}

	public ShowHideCloudLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public QuickActionType getActionType() {
		return TYPE;
	}

	@Override
	public short getWeatherBand() {
		return WEATHER_BAND_CLOUD;
	}

	@Override
	public int getQuickActionDescription() {
		return R.string.quick_action_cloud_layer;
	}
}
