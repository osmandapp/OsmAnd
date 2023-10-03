package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgetstates.SimpleWidgetState.*;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class BaseSimpleWidgetSettingsFragment extends WidgetSettingsBaseFragment {
	private static final String SELECTED_WIDGET_SIZE_ID_KEY = "selected_widget_id_size";
	private static final String SHOW_ICON_KEY = "show_icon_key";
	private boolean isWidgetVertical = false;

	public CommonPreference<Boolean> shouldShowIconPref;
	public OsmandPreference<WidgetSize> widgetSizePref;
	private WidgetType widgetType;

	private boolean showIcon;
	private WidgetSize selectedWidgetSize;

	public void setWidgetType(WidgetType widgetType) {
		this.widgetType = widgetType;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null && widgetInfo.widget instanceof SimpleWidget) {
			SimpleWidget simpleWidget = (SimpleWidget) widgetInfo.widget;
			isWidgetVertical = simpleWidget.isVerticalWidget();
			shouldShowIconPref = simpleWidget.shouldShowIconPref();
			widgetSizePref = simpleWidget.getWidgetSizePref();
			selectedWidgetSize = bundle.containsKey(SELECTED_WIDGET_SIZE_ID_KEY) ? WidgetSize.values()[bundle.getInt(SELECTED_WIDGET_SIZE_ID_KEY)] : widgetSizePref.get();
			showIcon = bundle.containsKey(SHOW_ICON_KEY) ? bundle.getBoolean(SHOW_ICON_KEY) : shouldShowIconPref.get();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_WIDGET_SIZE_ID_KEY, selectedWidgetSize.ordinal());
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

			View widgetSizeContainer = container.findViewById(R.id.widget_size_container);
			widgetSizeContainer.setOnClickListener(v -> showPreferenceDialog(container));
			widgetSizeContainer.setBackground(getPressedStateDrawable());
			TextView widgetSizeDescription = container.findViewById(R.id.widget_size_description);
			widgetSizeDescription.setText(selectedWidgetSize.descriptionId);

			SwitchCompat switchCompat = container.findViewById(R.id.show_icon_toggle);
			switchCompat.setChecked(showIcon);
			View shoIconContainer = container.findViewById(R.id.show_icon_container);
			shoIconContainer.setOnClickListener(v -> updateShowIcon(!showIcon, switchCompat));
			shoIconContainer.setBackground(getPressedStateDrawable());
		}
	}

	private void showPreferenceDialog(ViewGroup container) {
		int selected = selectedWidgetSize.ordinal();
		String[] items = new String[WidgetSize.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = WidgetSize.values()[i].getTitle(app);
		}

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.shared_string_size)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedWidgetSize = WidgetSize.values()[which];
			((TextView) container.findViewById(R.id.widget_size_description)).setText(selectedWidgetSize.getTitle(app));
		});

	}

	private void updateShowIcon(boolean shouldShowIcon, SwitchCompat switchCompat) {
		switchCompat.setChecked(shouldShowIcon);
		showIcon = shouldShowIcon;
	}

	@Override
	protected void applySettings() {
		if (isWidgetVertical) {
			shouldShowIconPref.set(showIcon);
			widgetSizePref.set(selectedWidgetSize);
			MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
			if (widgetInfo != null && widgetInfo.widget instanceof SimpleWidget) {
				SimpleWidget simpleWidget = (SimpleWidget) widgetInfo.widget;
				simpleWidget.showIcon(shouldShowIconPref.get());
				simpleWidget.recreateView();
			}
			app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateControls();
		}
	}
}
