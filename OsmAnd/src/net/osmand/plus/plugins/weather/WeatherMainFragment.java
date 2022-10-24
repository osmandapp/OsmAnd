package net.osmand.plus.plugins.weather;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class WeatherMainFragment extends BaseOsmAndFragment {

	public static final String TAG = WeatherMainFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private WeatherPlugin weatherPlugin;

	private View view;
	private LayoutInflater themedInflater;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
		weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_weather_main, container, false);

		setupMainToggle();
		setupWeatherLayers();
		setupWeatherContours();
		setupOfflineForecast();

		updateScreenMode(weatherPlugin.isAnyDataVisible(appMode));
		return view;
	}

	private void setupMainToggle() {
		setupToggleButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_action_umbrella,
				getString(R.string.shared_string_weather),
				weatherPlugin.isAnyDataVisible(appMode),
				false,
				v -> {
					boolean newState = !weatherPlugin.isAnyDataVisible(appMode);
					weatherPlugin.setWeatherEnabled(appMode, newState);
					updateScreenMode(newState);
					mapActivity.refreshMap();
				});
	}

	private void setupWeatherLayers() {
		ViewGroup container = view.findViewById(R.id.weather_layers_list);
		WeatherInfoType[] layers = WeatherInfoType.values();
		for (int i = 0; i < layers.length; i++) {
			WeatherInfoType layer = layers[i];
			View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_additional_right_desc, container, false);
			boolean showDivider = i < layers.length - 1;
			setupOnOffButton(
					view,
					layer.getIconId(),
					layer.toHumanString(app),
					null,
					weatherPlugin.isLayerEnabled(appMode, layer),
					showDivider,
					v -> {
						DashboardOnMap dashboard = mapActivity.getDashboard();
						weatherPlugin.setCurrentConfigureLayer(layer);
						int[] coordinates = AndroidUtils.getCenterViewCoordinates(view);
						dashboard.setDashboardVisibility(true, DashboardType.WEAHTER_LAYER, coordinates);
					}
			);
			container.addView(view);
		}
	}

	private void setupWeatherContours() {
		boolean isContoursEnabled = weatherPlugin.isContoursEnabled(appMode);
		WeatherInfoType selectedType = weatherPlugin.getSelectedContoursType(appMode);
		setupOnOffButton(
				view.findViewById(R.id.weather_contours),
				R.drawable.ic_plugin_srtm,
				getString(R.string.shared_string_contours),
				isContoursEnabled ? selectedType.toHumanString(app) : null,
				isContoursEnabled,
				false,
				v -> {
					DashboardOnMap dashboard = mapActivity.getDashboard();
					int[] coordinates = AndroidUtils.getCenterViewCoordinates(view);
					dashboard.setDashboardVisibility(true, DashboardType.WEATHER_CONTOURS, coordinates);
				}
		);
	}

	private void setupOfflineForecast() {
		// todo implement offline forecast card
		View offlineForecastBlock = view.findViewById(R.id.offline_forecast_block);
		ViewGroup container = view.findViewById(R.id.offline_forecast_downloads_list);
		offlineForecastBlock.setVisibility(View.GONE);
	}

	private void updateScreenMode(boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void setupToggleButton(@NonNull View view, int iconId, @NonNull String title, boolean enabled,
	                               boolean showDivider, @Nullable OnClickListener listener) {
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);
		ivIcon.setColorFilter(enabled ? activeColor : defColor);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		CompoundButton cb = view.findViewById(R.id.compound_button);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, activeColor, cb);

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			ivIcon.setColorFilter(isChecked ? activeColor : defColor);
			if (listener != null) {
				listener.onClick(buttonView);
			}
		});

		view.setOnClickListener(v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	private void setupOnOffButton(@NonNull View view, int iconId, @NonNull String title, @Nullable String description,
	                              boolean enabled, boolean showDivider, @Nullable OnClickListener listener) {
		int activeColor = appMode.getProfileColor(nightMode);
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
		String enabledDescripiton = getString(enabled ? R.string.shared_string_on : R.string.shared_string_off);
		tvSecondaryDesc.setText(enabledDescripiton);

		view.setOnClickListener(listener);

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new WeatherMainFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
