package net.osmand.plus.plugins.weather.actions;

import static net.osmand.plus.quickaction.QuickActionIds.OPEN_WEATHER_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.dialogs.WeatherForecastFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class OpenWeatherAction extends QuickAction {
	public static final QuickActionType TYPE = new QuickActionType(OPEN_WEATHER_ACTION_ID,
			"weather.forecast.open", OpenWeatherAction.class)
			.nameRes(R.string.weather_screen)
			.nameActionRes(R.string.shared_string_open)
			.iconRes(R.drawable.ic_action_umbrella).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public OpenWeatherAction() {
		super(TYPE);
	}

	public OpenWeatherAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		WeatherPlugin weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		if (weatherPlugin != null) {
			if(!PluginsHelper.isEnabled(WeatherPlugin.class)){
				PluginsHelper.enablePlugin(mapActivity, mapActivity.getMyApplication(), weatherPlugin, true);
			}

			mapActivity.getMyApplication().logEvent("weatherForecastOpen");
			WeatherForecastFragment.showInstance(mapActivity.getSupportFragmentManager());
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.open_weather_action_description);
		parent.addView(view);
	}
}
