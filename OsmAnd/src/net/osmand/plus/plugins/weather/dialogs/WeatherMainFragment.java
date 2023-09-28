package net.osmand.plus.plugins.weather.dialogs;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherContour;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.WeatherSettings;
import net.osmand.plus.plugins.weather.listener.RemoveLocalForecastListener;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Iterator;

public class WeatherMainFragment extends BaseOsmAndFragment implements DownloadEvents, RemoveLocalForecastListener {

	public static final String TAG = WeatherMainFragment.class.getSimpleName();

	private WeatherHelper weatherHelper;
	private OfflineForecastHelper forecastHelper;
	private WeatherPlugin weatherPlugin;
	private WeatherSettings weatherSettings;

	private OfflineWeatherForecastCard offlineForecastCard;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		weatherHelper = app.getWeatherHelper();
		forecastHelper = app.getOfflineForecastHelper();
		weatherSettings = weatherHelper.getWeatherSettings();
		weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_weather_main, container, false);

		setupHeader(view);
		setupWeatherLayers(view, themedInflater);
		setupWeatherContours(view);
		setupOfflineForecastCard(view);

		updateScreenMode(view, weatherSettings.weatherEnabled.get());

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		forecastHelper.registerRemoveLocalForecastListener(this);
		offlineForecastCard.onUpdatedIndexesList();
	}

	@Override
	public void onPause() {
		super.onPause();
		forecastHelper.unregisterRemoveLocalForecastListener(this);
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_umbrella,
				getString(R.string.shared_string_weather),
				weatherSettings.weatherEnabled.get(),
				false,
				v -> {
					boolean enabled = !weatherSettings.weatherEnabled.get();
					weatherSettings.weatherEnabled.set(enabled);
					updateScreenMode(view, enabled);
					refreshMap((MapActivity) getMyActivity());
				});
	}

	private void refreshMap(@NonNull MapActivity mapActivity) {
		app.runInUIThread(mapActivity::refreshMap);
	}

	private void setupWeatherLayers(@NonNull View view, @NonNull LayoutInflater inflater) {
		ViewGroup container = view.findViewById(R.id.weather_layers_list);
		Iterator<WeatherBand> iterator = weatherHelper.getWeatherBands().iterator();
		while (iterator.hasNext()) {
			WeatherBand weatherBand = iterator.next();
			View itemView = inflater.inflate(R.layout.bottom_sheet_item_with_additional_right_desc, container, false);
			setupOnOffButton(itemView, weatherBand.getIconId(), weatherBand.getMeasurementName(),
					null, weatherBand.isBandVisible(), iterator.hasNext(), v -> {
						MapActivity mapActivity = (MapActivity) getMyActivity();
						if (mapActivity != null) {
							DashboardOnMap dashboard = mapActivity.getDashboard();
							weatherPlugin.setCurrentConfigureBand(weatherBand.getBandIndex());
							int[] coordinates = AndroidUtils.getCenterViewCoordinates(itemView);
							dashboard.setDashboardVisibility(true, DashboardType.WEATHER_LAYER, coordinates);
						}
					}
			);
			container.addView(itemView);
		}
	}

	private void setupWeatherContours(@NonNull View view) {
		boolean isContoursEnabled = weatherPlugin.isContoursEnabled();
		WeatherContour selectedType = weatherPlugin.getSelectedContoursType();
		setupOnOffButton(
				view.findViewById(R.id.weather_contours),
				R.drawable.ic_plugin_srtm,
				getString(R.string.shared_string_contours),
				isContoursEnabled ? selectedType.toHumanString(app) : null,
				isContoursEnabled,
				false,
				v -> {
					MapActivity mapActivity = (MapActivity) getMyActivity();
					if (mapActivity != null) {
						DashboardOnMap dashboard = mapActivity.getDashboard();
						int[] coordinates = AndroidUtils.getCenterViewCoordinates(view);
						dashboard.setDashboardVisibility(true, DashboardType.WEATHER_CONTOURS, coordinates);
					}
				}
		);
	}

	private void setupOnOffButton(@NonNull View view, int iconId, @NonNull String title, @Nullable String description,
	                              boolean enabled, boolean showDivider, @Nullable OnClickListener listener) {
		int activeColor = settings.getApplicationMode().getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);
		ivIcon.setColorFilter(enabled ? activeColor : defColor);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView tvDesc = view.findViewById(R.id.description);
		if (description != null) {
			tvDesc.setVisibility(View.VISIBLE);
			tvDesc.setText(description);
		} else {
			tvDesc.setVisibility(View.GONE);
		}

		TextView tvSecondaryDesc = view.findViewById(R.id.secondary_description);
		String enabledDescription = getString(enabled ? R.string.shared_string_on : R.string.shared_string_off);
		tvSecondaryDesc.setText(enabledDescription);

		view.setOnClickListener(listener);

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	private void setupOfflineForecastCard(@NonNull View view) {
		FragmentActivity activity = requireMyActivity();
		offlineForecastCard = new OfflineWeatherForecastCard((MapActivity) activity);
		ViewGroup cardContainer = view.findViewById(R.id.offline_forecast_section);
		cardContainer.removeAllViews();
		cardContainer.addView(offlineForecastCard.build(activity));
	}

	@Override
	public void onUpdatedIndexesList() {
		offlineForecastCard.onUpdatedIndexesList();
	}

	@Override
	public void downloadInProgress() {
		offlineForecastCard.downloadInProgress();
	}

	@Override
	public void downloadHasFinished() {
		offlineForecastCard.downloadHasFinished();
	}

	@Override
	public void onRemoveLocalForecastEvent() {
		offlineForecastCard.onUpdatedIndexesList();
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new WeatherMainFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}