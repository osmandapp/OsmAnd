package net.osmand.plus.views.mapwidgets.configure.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.plugins.development.widget.ZoomLevelWidget;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetstates.ZoomLevelWidgetState.ZoomLevelType;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import androidx.annotation.NonNull;

public class ZoomLevelSettingsFragment extends BaseSimpleWidgetSettingsFragment {

	private static final String ZOOM_LEVEL_TYPE_KEY = "zoom_level_type";

	private OsmandPreference<ZoomLevelType> zoomLevelTypePref;
	private ZoomLevelType zoomLevelType;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.DEV_ZOOM_LEVEL;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			ZoomLevelWidget widget = (ZoomLevelWidget) widgetInfo.widget;
			zoomLevelTypePref = widget.getZoomLevelTypePref();
			zoomLevelType = ZoomLevelType.values()[bundle.getInt(ZOOM_LEVEL_TYPE_KEY, zoomLevelTypePref.get().ordinal())];
		} else {
			dismiss();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(ZOOM_LEVEL_TYPE_KEY, zoomLevelType.ordinal());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		zoomLevelTypePref.setModeValue(appMode, zoomLevelType);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.fragment_widget_settings_zoom_level, container);

		View zoomLevelTypeContainer = container.findViewById(R.id.zoom_level_type_container);
		zoomLevelTypeContainer.setOnClickListener(v -> showZoomLevelTypeSelectionDialog());
		zoomLevelTypeContainer.setBackground(getPressedStateDrawable());
		updateSelectedZoomLevelTypeText();
		themedInflater.inflate(R.layout.divider, container);

		super.setupContent(themedInflater, container);
	}

	private void showZoomLevelTypeSelectionDialog() {
		Context context = getContext();
		if (context == null) {
			return;
		}

		CharSequence[] items = new CharSequence[ZoomLevelType.values().length];
		for (ZoomLevelType zoomLevelType : ZoomLevelType.values()) {
			items[zoomLevelType.ordinal()] = getString(zoomLevelType.titleId);
		}

		AlertDialogData dialogData = new AlertDialogData(context, nightMode)
				.setTitle(R.string.shared_string_show)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, zoomLevelType.ordinal(), v -> {
			int which = (int) v.getTag();
			zoomLevelType = ZoomLevelType.values()[which];
			updateSelectedZoomLevelTypeText();
		});
	}

	private void updateSelectedZoomLevelTypeText() {
		TextView selectedZoomLevelType = view.findViewById(R.id.selected_zoom_level_type);
		selectedZoomLevelType.setText(zoomLevelType.titleId);
	}
}