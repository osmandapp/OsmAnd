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
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.List;
import java.util.Set;

public class BaseResizableWidgetSettingFragment extends WidgetSettingsBaseFragment {
	private static final String SELECTED_WIDGET_SIZE_ID_KEY = "selected_widget_id_size";
	protected static final String WIDGET_TYPE_KEY = "widget_type_key";

	public OsmandPreference<WidgetSize> widgetSizePref;
	private WidgetType widgetType;
	@Nullable
	private MapWidgetInfo widgetInfo;

	private WidgetSize selectedWidgetSize;

	public void setWidgetType(WidgetType widgetType) {
		this.widgetType = widgetType;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null && widgetInfo.widget instanceof ISupportWidgetResizing resizableWidget) {
			widgetSizePref = resizableWidget.getWidgetSizePref();
			selectedWidgetSize = bundle.containsKey(SELECTED_WIDGET_SIZE_ID_KEY) ? WidgetSize.values()[bundle.getInt(SELECTED_WIDGET_SIZE_ID_KEY)] : widgetSizePref.get();
		}
		String type = bundle.getString(WIDGET_TYPE_KEY);
		if (widgetType == null && type != null) {
			widgetType = WidgetType.getById(type);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_WIDGET_SIZE_ID_KEY, selectedWidgetSize.ordinal());
		outState.putString(WIDGET_TYPE_KEY, getWidget().name());
	}

	@NonNull
	@Override
	public WidgetType getWidget() {
		return widgetType;
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.resizable_widget_setting, container);

		View widgetSizeContainer = container.findViewById(R.id.widget_size_container);
		widgetSizeContainer.setOnClickListener(v -> showPreferenceDialog(container));
		widgetSizeContainer.setBackground(getPressedStateDrawable());
		TextView widgetSizeDescription = container.findViewById(R.id.widget_size_description);
		widgetSizeDescription.setText(selectedWidgetSize.titleId);
	}

	private void showPreferenceDialog(ViewGroup container) {
		int selected = selectedWidgetSize.ordinal();
		String[] items = new String[WidgetSize.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = app.getString(WidgetSize.values()[i].getTitleId());
		}

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.shared_string_size)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedWidgetSize = WidgetSize.values()[which];
			((TextView) container.findViewById(R.id.widget_size_description)).setText(selectedWidgetSize.getTitleId());
		});
	}

	@Override
	protected void applySettings() {
		if (widgetInfo != null) {
			updateRowWidgets(widgetInfo);
		}

		widgetSizePref.set(selectedWidgetSize);
		if (widgetInfo != null) {
			if (widgetInfo.widget instanceof ISupportWidgetResizing widgetResizing) {
				widgetResizing.recreateView();
			}
		}
		app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateControls();
	}

	private void updateRowWidgets(@NonNull MapWidgetInfo widgetInfo) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			List<Set<MapWidgetInfo>> widgets = widgetRegistry.getPagedWidgetsForPanel(activity,
					appMode, widgetInfo.getWidgetPanel(), AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE);

			Set<MapWidgetInfo> rowMapWidgetsInfo = widgets.get(widgetInfo.pageIndex);
			rowMapWidgetsInfo.remove(widgetInfo);
			applySizeSettingToWidgetsInRow(rowMapWidgetsInfo);
		}
	}

	private void applySizeSettingToWidgetsInRow(@NonNull Set<MapWidgetInfo> mapWidgetInfo) {
		for (MapWidgetInfo info : mapWidgetInfo) {
			if (info.widget instanceof ISupportWidgetResizing widgetResizing) {
				widgetResizing.getWidgetSizePref().set(selectedWidgetSize);
				widgetResizing.recreateView();
			}
		}
	}
}