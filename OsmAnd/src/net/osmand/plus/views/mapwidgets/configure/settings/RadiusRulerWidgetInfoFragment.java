package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.Arrays;
import java.util.List;

public class RadiusRulerWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String KEY_RADIUS_RULER_MODE = "radius_ruler_mode";
	private static final String KEY_SHOW_COMPASS = "show_compass";

	private RadiusRulerMode radiusRulerMode;
	private boolean showCompass;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.RADIUS_RULER;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);

		RadiusRulerMode defaultRadiusRulerMode = settings.RADIUS_RULER_MODE.getModeValue(appMode);
		boolean defaultShowCompass = settings.SHOW_COMPASS_ON_RADIUS_RULER.getModeValue(appMode);

		String radiusRulerModeName = bundle.getString(KEY_RADIUS_RULER_MODE, defaultRadiusRulerMode.name());
		radiusRulerMode = RadiusRulerMode.valueOf(radiusRulerModeName);
		showCompass = bundle.getBoolean(KEY_SHOW_COMPASS, defaultShowCompass);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.radius_ruler_widget_settings_fragment, container);
		setupRadiusRulerModeSetting();
		setupCompassSetting();
	}

	private void setupRadiusRulerModeSetting() {
		View hideModeContainer = view.findViewById(R.id.hide_mode_container);
		View darkModeContainer = view.findViewById(R.id.dark_mode_container);
		View lightModeContainer = view.findViewById(R.id.light_mode_container);

		CompoundButton darkButton = darkModeContainer.findViewById(R.id.compound_button);
		CompoundButton lightButton = lightModeContainer.findViewById(R.id.compound_button);
		CompoundButton hideButton = hideModeContainer.findViewById(R.id.compound_button);
		List<CompoundButton> buttons = Arrays.asList(darkButton, lightButton, hideButton);

		ModeSelectionCallback callback = selectedRadiusRulerMode -> {
			if (selectedRadiusRulerMode == radiusRulerMode) {
				return;
			}
			RadiusRulerMode previousMode = radiusRulerMode;
			radiusRulerMode = selectedRadiusRulerMode;

			for (int i = 0; i < buttons.size(); i++) {
				if (i != radiusRulerMode.ordinal()) {
					buttons.get(i).setChecked(false);
				}
			}

			if (radiusRulerMode == RadiusRulerMode.EMPTY || previousMode == RadiusRulerMode.EMPTY) {
				setupCompassSetting();
			}
		};

		setupRadiusRulerModeItem(RadiusRulerMode.EMPTY, hideModeContainer, callback);
		setupRadiusRulerModeItem(RadiusRulerMode.FIRST, darkModeContainer, callback);
		setupRadiusRulerModeItem(RadiusRulerMode.SECOND, lightModeContainer, callback);
	}

	private void setupRadiusRulerModeItem(@NonNull RadiusRulerMode itemMode,
	                                      @NonNull View container,
	                                      @NonNull ModeSelectionCallback callback) {
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		boolean selected = radiusRulerMode == itemMode;

		updateIcon(icon, itemMode.iconId, selected);
		title.setText(itemMode.titleId);

		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		compoundButton.setChecked(selected);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			updateIcon(icon, itemMode.iconId, isChecked);
			if (isChecked) {
				callback.onModeSelected(itemMode);
			}
		});

		container.setOnClickListener(v -> compoundButton.setChecked(true));
		container.setBackground(getPressedStateDrawable());
	}

	private void setupCompassSetting() {
		View container = view.findViewById(R.id.compass_on_circles_container);
		ImageView icon = container.findViewById(R.id.icon);
		TextView text = container.findViewById(R.id.text);
		CompoundButton compassSwitch = container.findViewById(R.id.compass_on_circles_switch);

		boolean radiusRulerEnabled = radiusRulerMode != RadiusRulerMode.EMPTY;

		updateCompassIcon(icon, showCompass, radiusRulerEnabled && showCompass);
		text.setText(R.string.compass_on_circles);

		UiUtilities.setupCompoundButton(compassSwitch, nightMode, CompoundButtonType.GLOBAL);
		compassSwitch.setChecked(showCompass);
		compassSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			updateCompassIcon(icon, isChecked, isChecked && radiusRulerEnabled);
			showCompass = isChecked;
		});
		compassSwitch.setEnabled(radiusRulerEnabled);

		container.setOnClickListener(v -> compassSwitch.setChecked(!compassSwitch.isChecked()));
		container.setBackground(getPressedStateDrawable());
		container.setEnabled(radiusRulerEnabled);
	}

	private void updateCompassIcon(@NonNull ImageView icon, boolean selected, boolean enabled) {
		int iconId = selected ? R.drawable.ic_action_compass_widget : R.drawable.ic_action_compass_widget_hide;
		updateIcon(icon, iconId, enabled);
	}

	private void updateIcon(@NonNull ImageView icon, @DrawableRes int iconId, boolean enabled) {
		int colorId = enabled
				? ColorUtilities.getActiveIconColorId(nightMode)
				: ColorUtilities.getDefaultIconColorId(nightMode);
		icon.setImageDrawable(getIcon(iconId, colorId));
	}

	@Override
	protected void applySettings() {
		super.applySettings();

		settings.RADIUS_RULER_MODE.setModeValue(appMode, radiusRulerMode);
		settings.SHOW_COMPASS_ON_RADIUS_RULER.setModeValue(appMode, showCompass);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_RADIUS_RULER_MODE, radiusRulerMode.name());
		outState.putBoolean(KEY_SHOW_COMPASS, showCompass);
	}

	private interface ModeSelectionCallback {

		void onModeSelected(@NonNull RadiusRulerMode radiusRulerMode);
	}
}