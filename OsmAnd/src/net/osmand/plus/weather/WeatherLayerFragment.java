package net.osmand.plus.weather;

import android.content.Context;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class WeatherLayerFragment extends BaseOsmAndFragment {

	public static final String TAG = WeatherLayerFragment.class.getSimpleName();

	private static int TRANSPARENCY_MIN = 0;
	private static int TRANSPARENCY_MAX = 100;

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private WeatherPlugin weatherPlugin;

	private View view;
	private LayoutInflater themedInflater;
	private boolean nightMode;

	private WeatherLayerType configureLayer;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
		weatherPlugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
		configureLayer = weatherPlugin.getCurrentConfigureLayer();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_weather_layer, container, false);

		setupMainToggle();
		setupEmptyScreenContent();
		setupTransparencySliderCard();
		setupMeasurementUnitsBlock();

		updateScreenMode(configureLayer.isEnabled());
		return view;
	}

	private void setupMainToggle() {
		setupToggleButton(
				view.findViewById(R.id.main_toggle),
				configureLayer.getIconId(),
				getString(configureLayer.getTitleId()),
				configureLayer.isEnabled(),
				false,
				v -> {
					boolean newState = !configureLayer.isEnabled();
					configureLayer.setEnabled(newState);
					updateScreenMode(newState);
				});
	}

	private void setupTransparencySliderCard() {
		Slider slider = view.findViewById(R.id.slider);
		TextView tvCurrentValue = view.findViewById(R.id.slider_current_value);

		slider.setValueTo(TRANSPARENCY_MAX);
		slider.setValueFrom(TRANSPARENCY_MIN);

		((TextView) view.findViewById(R.id.slider_min)).setText(String.valueOf(TRANSPARENCY_MIN));
		((TextView) view.findViewById(R.id.slider_max)).setText(String.valueOf(TRANSPARENCY_MAX));

		int value = configureLayer.getTransparency(appMode);
		tvCurrentValue.setText(formatPercent(value));
		slider.setValue(value);

		slider.addOnChangeListener((slider_, newValue, fromUser) -> {
			if (fromUser) {
				configureLayer.setTransparency(appMode, (int) newValue);
				tvCurrentValue.setText(formatPercent((int) newValue));
			}
		});

		int activeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, false);
	}

	private void setupMeasurementUnitsBlock() {
		View container = view.findViewById(R.id.measurement_units_block);
		if (configureLayer != WeatherLayerType.CLOUDS) {
			container.setVisibility(View.VISIBLE);
			View card = container.findViewById(R.id.measurement_units_card);
			View button = card.findViewById(R.id.measurement_units_button);
			setupSelectableBackground(button);
			button.setOnClickListener(v -> {
				showChooseUnitDialog(configureLayer);
			});
			updateMeasurementUnitsCard();
		} else {
			container.setVisibility(View.GONE);
		}
	}

	private void setupEmptyScreenContent() {
		ImageView ivIcon = view.findViewById(R.id.empty_screen_icon);
		TextView tvDesc = view.findViewById(R.id.empty_screen_description);
		ivIcon.setImageResource(configureLayer.getIconId());
		tvDesc.setText(configureLayer.getEmprtyStateDesc(app));
	}

	private void updateScreenMode(boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void updateMeasurementUnitsCard() {
		TextView tvUnitsDesc = view.findViewById(R.id.units_description);
		tvUnitsDesc.setText(configureLayer.getSelectedUnitName(app, appMode));
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
		setupSelectableBackground(view);
	}

	private void setupSelectableBackground(@NonNull View view) {
		int activeColor = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	@NonNull
	private String formatPercent(int percent) {
		return percent + "%";
	}

	private void showChooseUnitDialog(WeatherLayerType layer) {
		Context ctx = UiUtilities.getThemedContext(getActivity(), nightMode);
		int profileColor = appMode.getProfileColor(nightMode);
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(layer.getTitleId());
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		int selectedUnitIndex = 0;
		Enum<?>[] values = layer.getUnits();
		Enum<?> selected = layer.getSelectedUnit(appMode);
		String[] entries = new String[values.length];
		Integer[] entryValues = new Integer[values.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = layer.getUnitName(app, values[i]);
			entryValues[i] = values[i].ordinal();
			if (selected == values[i]) {
				selectedUnitIndex = entryValues[i];
			}
		}

		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(
				entries, nightMode, selectedUnitIndex, app, profileColor, themeRes, v -> {
					layer.setSelectedUnit(appMode, values[(int) v.getTag()]);
					updateMeasurementUnitsCard();
				}
		);

		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new WeatherLayerFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
