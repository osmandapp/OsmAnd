package net.osmand.plus.plugins.weather.dialogs;

import static net.osmand.plus.plugins.weather.WeatherBand.*;

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
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.WeatherPlugin;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class WeatherLayerFragment extends BaseOsmAndFragment {

	public static final String TAG = WeatherLayerFragment.class.getSimpleName();

	private static final String WEATHER_BAND_KEY = "weather_band_key";
	private static final int TRANSPARENCY_MIN = 0;
	private static final int TRANSPARENCY_MAX = 100;
	private static final int MAX_FORECAST_DAYS = 7;
	private static final long MS_IN_DAY = 24 * 60 * 60 * 1000;

	private WeatherBand weatherBand;
	private boolean isSliderDragging = false;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WeatherPlugin plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		short bandIndex = plugin != null ? plugin.getCurrentConfigureBand() : WEATHER_BAND_NOTHING;

		if (bandIndex == WEATHER_BAND_NOTHING && savedInstanceState != null) {
			bandIndex = savedInstanceState.getShort(WEATHER_BAND_KEY);
		}
		weatherBand = app.getWeatherHelper().getWeatherBand(bandIndex);
		if (weatherBand == null) {
			requireActivity().onBackPressed();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_weather_layer, container, false);

		if (weatherBand != null) {
			setupHeader(view);
			setupEmptyScreenContent(view);
			setupTransparencySliderCard(view);
			setupMeasurementUnitsBlock(view);

			updateScreenMode(view, weatherBand.isBandVisible());
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putShort(WEATHER_BAND_KEY, weatherBand.getBandIndex());
	}

	private void setupHeader(@NonNull View view) {
		TransportLinesFragment.setupButton(
				view.findViewById(R.id.main_toggle),
				weatherBand.getIconId(),
				weatherBand.getMeasurementName(),
				weatherBand.isBandVisible(),
				false,
				v -> {
					boolean visible = !weatherBand.isBandVisible();
					weatherBand.setBandVisible(visible);
					updateScreenMode(view, visible);
					refreshMap((MapActivity) getMyActivity());
				});
	}

	private void refreshMap(@NonNull MapActivity mapActivity) {
		app.runInUIThread(mapActivity::refreshMap);
	}

	private void setupTransparencySliderCard(@NonNull View view) {
		CommonPreference<Float> alphaPref = weatherBand.getAlphaPreference();
		if (alphaPref != null) {
			Slider slider = view.findViewById(R.id.slider);
			TextView tvCurrentValue = view.findViewById(R.id.slider_current_value);

			slider.setStepSize(0.01f);
			slider.setValueTo(1);
			slider.setValueFrom(0);

			((TextView) view.findViewById(R.id.slider_min)).setText(String.valueOf(TRANSPARENCY_MIN));
			((TextView) view.findViewById(R.id.slider_max)).setText(String.valueOf(TRANSPARENCY_MAX));

			float value = alphaPref.get();
			tvCurrentValue.setText(formatAlpha(value));
			slider.setValue(value);

			slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					isSliderDragging = true;
				}

				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
					isSliderDragging = false;
				}
			});

			slider.addOnChangeListener((slider_, newValue, fromUser) -> {
				if (fromUser && !isSliderDragging) {
					weatherBand.getAlphaPreference().set(newValue);
					tvCurrentValue.setText(formatAlpha(newValue));
					WeatherTileResourcesManager weatherTileResourcesManager = app.getWeatherHelper().getWeatherResourcesManager();
					if (weatherTileResourcesManager != null) {
						weatherTileResourcesManager.clearDbCache(System.currentTimeMillis() + MAX_FORECAST_DAYS * MS_IN_DAY);
					}
				}
			});
			int activeColor = settings.getApplicationMode().getProfileColor(nightMode);
			UiUtilities.setupSlider(slider, nightMode, activeColor, false);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.transparency_card), alphaPref != null);
	}

	private String formatAlpha(float value) {
		return (int) (value * 100) + "%";
	}

	private void setupMeasurementUnitsBlock(@NonNull View view) {
		View container = view.findViewById(R.id.measurement_units_block);
		if (weatherBand.getBandIndex() != WEATHER_BAND_CLOUD) {
			container.setVisibility(View.VISIBLE);
			View card = container.findViewById(R.id.measurement_units_card);
			View button = card.findViewById(R.id.measurement_units_button);
			setupSelectableBackground(button);
			button.setOnClickListener(v -> showChooseUnitDialog(view));
			updateMeasurementUnitsCard(view);
		} else {
			container.setVisibility(View.GONE);
		}
	}

	private void updateMeasurementUnitsCard(@NonNull View view) {
		TextView tvUnitsDesc = view.findViewById(R.id.units_description);
		tvUnitsDesc.setText(weatherBand.getBandUnit().toHumanString(app));
	}

	private void setupSelectableBackground(@NonNull View view) {
		int activeColor = settings.getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	private void setupEmptyScreenContent(@NonNull View view) {
		ImageView ivIcon = view.findViewById(R.id.empty_screen_icon);
		TextView tvDesc = view.findViewById(R.id.empty_screen_description);
		ivIcon.setImageResource(weatherBand.getIconId());
		tvDesc.setText(getEmptyStateDesc());
	}

	private void showChooseUnitDialog(@NonNull View view) {
		CommonPreference<? extends WeatherUnit> preference = weatherBand.getBandUnitPref();
		if (preference != null) {
			OnClickListener listener = v -> {
				int selected = (int) v.getTag();
				settings.setPreference(weatherBand.getBandUnitPref().getId(), selected);
				updateMeasurementUnitsCard(view);
				app.getWeatherHelper().updateBandsSettings();
				refreshMap((MapActivity) getMyActivity());
			};
			int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
			int selectedIndex = weatherBand.getAvailableBandUnits().indexOf(preference.get());
			WeatherDialogs.showChooseUnitDialog(view.getContext(), weatherBand, selectedIndex, profileColor, nightMode, listener);
		}
	}

	@Nullable
	public String getEmptyStateDesc() {
		switch (weatherBand.getBandIndex()) {
			case WEATHER_BAND_CLOUD:
				return app.getString(R.string.empty_screen_weather_clouds_layer);
			case WEATHER_BAND_TEMPERATURE:
				return app.getString(R.string.empty_screen_weather_temperature_layer);
			case WEATHER_BAND_PRESSURE:
				return app.getString(R.string.empty_screen_weather_pressure_layer);
			case WEATHER_BAND_WIND_ANIMATION:
			case WEATHER_BAND_WIND_SPEED:
				return app.getString(R.string.empty_screen_weather_wind_layer);
			case WEATHER_BAND_PRECIPITATION:
				return app.getString(R.string.empty_screen_weather_precipitation_layer);
			default:
				return null;
		}
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new WeatherLayerFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}