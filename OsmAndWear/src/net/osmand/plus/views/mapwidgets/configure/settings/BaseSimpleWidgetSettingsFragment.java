package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.List;
import java.util.Set;

public class BaseSimpleWidgetSettingsFragment extends BaseResizableWidgetSettingFragment {
	private static final String SHOW_ICON_KEY = "show_icon_key";
	private boolean isWidgetVertical = false;

	public CommonPreference<Boolean> shouldShowIconPref;
	private WidgetType widgetType;
	@Nullable
	private MapWidgetInfo widgetInfo;

	private boolean showIcon;

	public void setWidgetType(WidgetType widgetType) {
		this.widgetType = widgetType;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null && widgetInfo.widget instanceof SimpleWidget) {
			SimpleWidget simpleWidget = (SimpleWidget) widgetInfo.widget;
			isWidgetVertical = simpleWidget.isVerticalWidget();
			shouldShowIconPref = simpleWidget.shouldShowIconPref();
			showIcon = bundle.containsKey(SHOW_ICON_KEY) ? bundle.getBoolean(SHOW_ICON_KEY) : shouldShowIconPref.get();
		}
		String type = bundle.getString(WIDGET_TYPE_KEY);
		if (widgetType == null && type != null) {
			widgetType = WidgetType.getById(type);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_ICON_KEY, showIcon);
	}

	@NonNull
	@Override
	public WidgetType getWidget() {
		return widgetType;
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		if (isWidgetVertical) {
			themedInflater.inflate(R.layout.simple_widget_settings, container);

			SwitchCompat switchCompat = container.findViewById(R.id.show_icon_toggle);
			switchCompat.setChecked(showIcon);
			View shoIconContainer = container.findViewById(R.id.show_icon_container);
			shoIconContainer.setOnClickListener(v -> updateShowIcon(!showIcon, switchCompat));
			shoIconContainer.setBackground(getPressedStateDrawable());
		}
		super.setupContent(themedInflater, container);
	}

	private void updateShowIcon(boolean shouldShowIcon, SwitchCompat switchCompat) {
		switchCompat.setChecked(shouldShowIcon);
		showIcon = shouldShowIcon;
	}

	@Override
	protected void applySettings() {
		if (isWidgetVertical) {
			shouldShowIconPref.set(showIcon);
			if (widgetInfo != null) {
				if (widgetInfo.widget instanceof SimpleWidget simpleWidget) {
					simpleWidget.showIcon(shouldShowIconPref.get());
					//simpleWidget.recreateView();
				}
			}
		}
		super.applySettings();
	}
}
