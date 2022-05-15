package net.osmand.plus.views.mapwidgets.configure.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class RadiusRulerWidgetSettingsFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_SHOW_DISTANCE_CIRCLES = "show_distance_circles";
	private static final String KEY_SHOW_COMPASS = "show_compass";
	private static final String KEY_NIGHT_MODE_RADIUS_RULER = "night_mode_radius_ruler";

	private boolean showDistanceCircles;
	private boolean showCompass;
	private boolean nightModeRadiusRuler;

	@NonNull
	@Override
	public WidgetParams getWidget() {
		return WidgetParams.RADIUS_RULER;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);

		boolean defaultShowDistanceCircles = settings.SHOW_DISTANCE_CIRCLES_ON_RADIUS_RULER.getModeValue(appMode);
		boolean defaultShowCompass = settings.SHOW_COMPASS_ON_RADIUS_RULER.getModeValue(appMode);
		boolean defaultNightModeRadiusRuler = settings.RADIUS_RULER_NIGHT_MODE.getModeValue(appMode);

		showDistanceCircles = bundle.getBoolean(KEY_SHOW_DISTANCE_CIRCLES, defaultShowDistanceCircles);
		showCompass = bundle.getBoolean(KEY_SHOW_COMPASS, defaultShowCompass);
		nightModeRadiusRuler = bundle.getBoolean(KEY_NIGHT_MODE_RADIUS_RULER, defaultNightModeRadiusRuler);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.radius_ruler_widget_settings_fragment, container);

		setupDistanceCirclesSetting();
		setupCompassSetting();
		setupNightModeSetting();
	}

	private void setupDistanceCirclesSetting() {
		int containerId = R.id.distance_circles_container;
		int iconId = R.drawable.ic_action_ruler_circle;
		int titleId = R.string.distance_circles;
		CallbackWithObject<Boolean> callback = result -> showDistanceCircles = result;
		setupSwitchSetting(containerId, iconId, iconId, titleId, callback, showDistanceCircles);
	}

	private void setupCompassSetting() {
		int containerId = R.id.compass_on_circles_container;
		int enabledIconId = R.drawable.ic_action_compass_widget;
		int disabledIconId = R.drawable.ic_action_compass_widget_hide;
		int titleId = R.string.compass_on_circles;
		CallbackWithObject<Boolean> callback = result -> showCompass = result;
		setupSwitchSetting(containerId, enabledIconId, disabledIconId, titleId, callback, showCompass);
	}

	private void setupSwitchSetting(@IdRes int containerId,
	                                @DrawableRes int enabledIconId,
	                                @DrawableRes int disabledIconId,
	                                @StringRes int titleId,
	                                @NonNull CallbackWithObject<Boolean> callback,
	                                boolean checked) {
		Context context = requireContext();

		View container = view.findViewById(containerId);
		ImageView icon = container.findViewById(R.id.icon);
		TextView text = container.findViewById(R.id.text);
		CompoundButton visibilitySwitch = container.findViewById(R.id.visibility_switch);

		updateIcon(icon, enabledIconId, disabledIconId, checked);
		text.setText(titleId);

		UiUtilities.setupCompoundButton(visibilitySwitch, nightMode, CompoundButtonType.GLOBAL);
		visibilitySwitch.setChecked(checked);
		visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			updateIcon(icon, enabledIconId, disabledIconId, isChecked);
			callback.processResult(isChecked);
		});

		container.setOnClickListener(v -> visibilitySwitch.setChecked(!visibilitySwitch.isChecked()));
		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		container.setBackground(UiUtilities.getColoredSelectableDrawable(context, activeColor));
	}

	private void updateIcon(@NonNull ImageView icon,
	                        @DrawableRes int enabledIconId,
	                        @DrawableRes int disabledIconId,
	                        boolean enabled) {
		int iconId = enabled ? enabledIconId : disabledIconId;
		int colorId = enabled
				? ColorUtilities.getActiveIconColorId(nightMode)
				: ColorUtilities.getDefaultIconColorId(nightMode);
		icon.setImageDrawable(getIcon(iconId, colorId));
	}

	private void setupNightModeSetting() {
		LinearLayout nightModeToggleView = view.findViewById(R.id.custom_radio_buttons);
		TextToggleButton nightModeToggle = new TextToggleButton(app, nightModeToggleView, nightMode);
		nightModeToggle.setItems(listRadioItems());
		nightModeToggle.setSelectedItem(nightModeRadiusRuler ? 0 : 1);
	}

	@NonNull
	private List<TextRadioItem> listRadioItems() {
		TextRadioItem nightModeColorItem = new TextRadioItem(getString(R.string.light_theme));
		nightModeColorItem.setOnClickListener((radioItem, view1) -> {
			nightModeRadiusRuler = true;
			return true;
		});

		TextRadioItem dayModeColorItem = new TextRadioItem(getString(R.string.dark_theme));
		dayModeColorItem.setOnClickListener((radioItem, view1) -> {
			nightModeRadiusRuler = false;
			return true;
		});

		return Arrays.asList(nightModeColorItem, dayModeColorItem);
	}

	@Override
	protected void applySettings() {
		settings.SHOW_DISTANCE_CIRCLES_ON_RADIUS_RULER.setModeValue(appMode, showDistanceCircles);
		settings.SHOW_COMPASS_ON_RADIUS_RULER.setModeValue(appMode, showCompass);
		settings.RADIUS_RULER_NIGHT_MODE.setModeValue(appMode, nightModeRadiusRuler);

		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).refreshMap();
		}
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SHOW_DISTANCE_CIRCLES, showDistanceCircles);
		outState.putBoolean(KEY_SHOW_COMPASS, showCompass);
		outState.putBoolean(KEY_NIGHT_MODE_RADIUS_RULER, nightModeRadiusRuler);
	}
}