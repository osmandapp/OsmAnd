package net.osmand.plus.views.mapwidgets.configure.settings;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.ElevationProfileWidget;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

public class ElevationProfileWidgetSettingsFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_SHOW_SLOPE = "show_slope";
	private ElevationProfileWidget elevationWidget;
	private boolean showSlope;

	private ImageView icon;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.ELEVATION_PROFILE;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);

		if (bundle.containsKey(KEY_SHOW_SLOPE)) {
			showSlope = bundle.getBoolean(KEY_SHOW_SLOPE);
		} else if (widgetInfo != null) {
			elevationWidget = ((ElevationProfileWidget) widgetInfo.widget);
			showSlope = elevationWidget.shouldShowSlope(appMode);
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
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
		container.setBackground(getPressedStateDrawable());
	}

	private void updateIcon(boolean showSlope) {
		Drawable drawable = showSlope
				? getIcon(R.drawable.ic_action_slope, ColorUtilities.getActiveIconColorId(nightMode))
				: getIcon(R.drawable.ic_action_slope_hide, ColorUtilities.getDefaultIconColorId(nightMode));
		icon.setImageDrawable(drawable);
	}

	@Override
	protected void applySettings() {
		if (elevationWidget != null) {
			elevationWidget.setShouldShowSlope(appMode, showSlope);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_SHOW_SLOPE, showSlope);
	}
}