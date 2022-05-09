package net.osmand.plus.views.mapwidgets.configure.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

public class ElevationProfileWidgetSettingsFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_SHOW_SLOPE = "show_slope";

	private boolean showSlope;

	private ImageView icon;

	@NonNull
	@Override
	public WidgetParams getWidget() {
		return WidgetParams.ELEVATION_PROFILE;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		showSlope = bundle.containsKey(KEY_SHOW_SLOPE)
				? bundle.getBoolean(KEY_SHOW_SLOPE)
				: settings.SHOW_SLOPES_ON_ELEVATION_WIDGET.getModeValue(appMode);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		Context context = requireContext();
		View content = themedInflater.inflate(R.layout.elevation_profile_widget_settings, container, false);
		container.addView(content);

		icon = view.findViewById(R.id.slope_icon);
		SwitchCompat slopeSwitch = view.findViewById(R.id.slope_switch);

		updateIcon(showSlope);

		UiUtilities.setupCompoundButton(slopeSwitch, nightMode, CompoundButtonType.GLOBAL);
		slopeSwitch.setChecked(showSlope);
		slopeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			showSlope = isChecked;
			updateIcon(showSlope);
		});

		content.setOnClickListener(v -> slopeSwitch.setChecked(!showSlope));
		int activeColor = ColorUtilities.getActiveColor(context, nightMode);
		AndroidUtils.setBackground(content, UiUtilities.getColoredSelectableDrawable(context, activeColor));
	}

	private void updateIcon(boolean showSlope) {
		Drawable drawable = showSlope
				? getIcon(R.drawable.ic_action_slope, ColorUtilities.getActiveIconColorId(nightMode))
				: getIcon(R.drawable.ic_action_slope_hide, ColorUtilities.getDefaultIconColorId(nightMode));
		icon.setImageDrawable(drawable);
	}

	@Override
	protected void applySettings() {
		settings.SHOW_SLOPES_ON_ELEVATION_WIDGET.setModeValue(appMode, showSlope);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SHOW_SLOPE, showSlope);
	}
}