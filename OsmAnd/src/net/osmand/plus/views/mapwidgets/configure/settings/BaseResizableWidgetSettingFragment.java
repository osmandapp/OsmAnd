package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;

import java.util.List;
import java.util.Set;

public class BaseResizableWidgetSettingFragment extends WidgetInfoBaseFragment {
	private static final String SELECTED_WIDGET_SIZE_ID_KEY = "selected_widget_id_size";

	protected OsmandPreference<WidgetSize> widgetSizePref;

	private WidgetSize selectedWidgetSize;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof ISupportWidgetResizing resizableWidget) {
			widgetSizePref = resizableWidget.getWidgetSizePref();
			selectedWidgetSize = bundle.containsKey(SELECTED_WIDGET_SIZE_ID_KEY) ? WidgetSize.values()[bundle.getInt(SELECTED_WIDGET_SIZE_ID_KEY)] : widgetSizePref.get();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_WIDGET_SIZE_ID_KEY, selectedWidgetSize.ordinal());
	}

	@Override
	protected void setupTopContent(@NonNull ViewGroup container) {
		if (widgetInfo != null && widgetInfo.widget instanceof ISupportWidgetResizing resizableWidget && resizableWidget.allowResize()) {
			inflate(R.layout.resizable_widget_setting, container);

			TextView height = container.findViewById(R.id.height);
			height.setText(R.string.shared_string_height);
			setupToggleButtons(view);
		}
	}

	private void setupToggleButtons(@NonNull View view) {
		IconToggleButton.IconRadioItem large = createToggleButton(view, WidgetSize.LARGE);
		IconToggleButton.IconRadioItem medium = createToggleButton(view, WidgetSize.MEDIUM);
		IconToggleButton.IconRadioItem small = createToggleButton(view, WidgetSize.SMALL);

		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		IconToggleButton toggleButton = new IconToggleButton(app, container, nightMode);
		toggleButton.setItems(small, medium, large);
		toggleButton.setSelectedItem(isMediumHeight() ? medium : isSmallHeight() ? small : large);
	}

	@NonNull
	private IconToggleButton.IconRadioItem createToggleButton(@NonNull View view, @NonNull WidgetSize widgetSize) {
		IconToggleButton.IconRadioItem item = new IconToggleButton.IconRadioItem(widgetSize.iconId);
		item.setOnClickListener((radioItem, v) -> {
			selectedWidgetSize = widgetSize;
			setupToggleButtons(view);
			onWidgetSizeChanged();
			return true;
		});
		return item;
	}

	protected void onWidgetSizeChanged() {

	}

	private boolean isMediumHeight() {
		return selectedWidgetSize == WidgetSize.MEDIUM;
	}

	protected boolean isSmallHeight() {
		return selectedWidgetSize == WidgetSize.SMALL;
	}

	@Override
	protected void applySettings() {
		if (widgetInfo != null && widgetInfo.widget instanceof ISupportWidgetResizing widgetResizing) {
			boolean isVerticalPanel = widgetInfo.getWidgetPanel().isPanelVertical();
			if (isVerticalPanel) {
				updateRowWidgets(widgetInfo);
			} else {
				widgetResizing.getWidgetSizePref().set(selectedWidgetSize);
				widgetResizing.recreateView();
			}
		}
		app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateControls();
	}

	private void updateRowWidgets(@NonNull MapWidgetInfo widgetInfo) {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		List<Set<MapWidgetInfo>> widgets = widgetRegistry.getPagedWidgetsForPanel(activity,
				appMode, widgetInfo.getWidgetPanel(), AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE);

		for (Set<MapWidgetInfo> rowMapWidgetsInfo : widgets) {
			for (MapWidgetInfo info : rowMapWidgetsInfo) {
				if (info == widgetInfo) {
					applySizeSettingToWidgetsInRow(rowMapWidgetsInfo);
					return;
				}
			}
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