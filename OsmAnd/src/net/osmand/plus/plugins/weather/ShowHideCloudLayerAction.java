package net.osmand.plus.plugins.weather;

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

public class ShowHideCloudLayerAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(42,
			"cloud.layer.showhide", ShowHideCloudLayerAction.class)
			.nameActionRes(R.string.quick_action_show_hide_title)
			.nameRes(R.string.cloud_layer)
			.iconRes(R.drawable.ic_action_cloud).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideCloudLayerAction() {
		super(TYPE);
	}

	public ShowHideCloudLayerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		ApplicationMode appMode = mapActivity.getMyApplication().getSettings().getApplicationMode();
		if (weatherPlugin != null) {
			weatherPlugin.toggleLayerEnable(appMode, WeatherInfoType.CLOUDS, !weatherPlugin.isLayerEnabled(appMode, WeatherInfoType.CLOUDS));
		}
		mapActivity.getMapLayers().updateLayers(mapActivity);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(mapActivity.getString(R.string.quick_action_cloud_layer));
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
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		return weatherPlugin.isLayerEnabled(app.getSettings().getApplicationMode(), WeatherInfoType.CLOUDS);
	}
}
