package net.osmand.plus.weather;

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

import com.google.android.material.slider.Slider;

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

import java.util.HashMap;
import java.util.Map;

public class WeatherContoursFragment extends BaseOsmAndFragment {

	public static final String TAG = WeatherContoursFragment.class.getSimpleName();

	private static int TRANSPARENCY_MIN = 0;
	private static int TRANSPARENCY_MAX = 100;

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private WeatherPlugin weatherPlugin;

	private View view;
	private Map<WeatherLayerType, View> radioButtons = new HashMap<>();
	private LayoutInflater themedInflater;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
		weatherPlugin = OsmandPlugin.getPlugin(WeatherPlugin.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);
		view = themedInflater.inflate(R.layout.fragment_weather_contours, container, false);

		setupMainToggle();
		setupContoursTypesCard();
		setupTransparencySliderCard();

		updateScreenMode(weatherPlugin.isContoursEnabled());
		return view;
	}

	private void setupMainToggle() {
		setupToggleButton(
				view.findViewById(R.id.main_toggle),
				R.drawable.ic_plugin_srtm,
				getString(R.string.shared_string_contours),
				weatherPlugin.isContoursEnabled(),
				false,
				v -> {
					boolean newState = !weatherPlugin.isContoursEnabled();
					weatherPlugin.setContoursEnabled(newState);
					updateScreenMode(newState);
				});
	}

	private void setupContoursTypesCard() {
		ViewGroup container = view.findViewById(R.id.contours_types_list);
		WeatherLayerType[] layers = WeatherLayerType.values();
		container.removeAllViews();
		radioButtons.clear();
		for (int i = 0; i < layers.length; i++) {
			WeatherLayerType layer = layers[i];
			View view = themedInflater.inflate(R.layout.bottom_sheet_item_with_radio_btn, container, false);
			boolean showDivider = i < layers.length - 1;
			setupRadioButton(
					view,
					layer.getIconId(),
					layer.toHumanString(app),
					layer == weatherPlugin.getSelectedContoursType(),
					showDivider,
					v -> {
						weatherPlugin.setContoursType(layer);
					}
			);
			radioButtons.put(layer, view);
			container.addView(view);
		}
	}

	private void setupTransparencySliderCard() {
		Slider slider = view.findViewById(R.id.slider);
		TextView tvCurrentValue = view.findViewById(R.id.slider_current_value);

		slider.setValueTo(TRANSPARENCY_MAX);
		slider.setValueFrom(TRANSPARENCY_MIN);

		((TextView) view.findViewById(R.id.slider_min)).setText(String.valueOf(TRANSPARENCY_MIN));
		((TextView) view.findViewById(R.id.slider_max)).setText(String.valueOf(TRANSPARENCY_MAX));

		int value = weatherPlugin.getContoursTransparency(appMode);
		tvCurrentValue.setText(formatPercent(value));
		slider.setValue(value);

		slider.addOnChangeListener((slider_, newValue, fromUser) -> {
			if (fromUser) {
				weatherPlugin.setContoursTransparency(appMode, (int) newValue);
				tvCurrentValue.setText(formatPercent((int) newValue));
			}
		});

		int activeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, false);
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
		setupSelectableBackground(view);
	}

	private void setupRadioButton(@NonNull View view, int iconId, @NonNull String title, boolean enabled,
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

		view.setOnClickListener(v -> {
			if (listener != null) {
				listener.onClick(v);
			}
			updateRadioButtons();
		});

		View divider = view.findViewById(R.id.bottom_divider);
		if (divider != null) {
			AndroidUiHelper.updateVisibility(divider, showDivider);
		}
		setupSelectableBackground(view);
	}

	private void updateRadioButtons() {
		int activeColor = appMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		for (WeatherLayerType type : WeatherLayerType.values()) {
			View view = radioButtons.get(type);
			if (view != null) {
				boolean isChecked = weatherPlugin.getSelectedContoursType() == type;
				ImageView ivIcon = view.findViewById(R.id.icon);
				int iconColor = isChecked ? activeColor : defColor;
				ivIcon.setColorFilter(isChecked ? activeColor : defColor);
				CompoundButton cb = view.findViewById(R.id.compound_button);
				cb.setChecked(isChecked);
			}
		}
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

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new WeatherContoursFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}