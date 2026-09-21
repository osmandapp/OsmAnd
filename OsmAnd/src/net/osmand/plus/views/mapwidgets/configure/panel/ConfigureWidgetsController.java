package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment.KEY_APP_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment.KEY_WIDGET_ID;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.mapwidgets.AndroidAutoWidgetsInitializer.AndroidAutoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfigureWidgetsController implements IDialogController {

	public static final String PROCESS_ID = "configure_widgets_controller";

	private MapWidgetInfo addedWidget;
	private final Map<WidgetsPanel, List<Object>> reorderLists = new EnumMap<>(WidgetsPanel.class);

	@Nullable
	public MapWidgetInfo getAddedWidget() {
		return addedWidget;
	}

	public void resetAddedWidget() {
		addedWidget = null;
	}

	public void openAddNewWidgetScreen(@NonNull MapActivity mapActivity, @NonNull WidgetsPanel selectedPanel,
	                                   @NonNull String widgetId, @NonNull ApplicationMode selectedAppMode,
	                                   @NonNull ConfigureWidgetsFragment fragment) {
		openAddNewWidgetScreen(mapActivity, selectedPanel, widgetId, selectedAppMode, false, fragment);
	}

	public void openAddNewAAWidgetScreen(@NonNull MapActivity mapActivity, @NonNull WidgetsPanel selectedPanel,
	                                   @NonNull String widgetId, @NonNull ApplicationMode selectedAppMode,
	                                   @NonNull ConfigureWidgetsFragment fragment) {
		openAddNewWidgetScreen(mapActivity, selectedPanel, widgetId, selectedAppMode, true, fragment);
	}

	public void openAddNewWidgetScreen(@NonNull MapActivity mapActivity, @NonNull WidgetsPanel selectedPanel,
	                                   @NonNull String widgetId, @NonNull ApplicationMode selectedAppMode,
									   boolean isAndroidAutoMode,
	                                   @NonNull ConfigureWidgetsFragment fragment) {
		WidgetType widgetType = WidgetType.getById(widgetId);
		OsmandApplication app = mapActivity.getApp();
		if (widgetType == null) {
			return;
		}
		MapWidget widget;
		MapWidgetInfo widgetInfo = null;
		String id = WidgetType.getDuplicateWidgetId(widgetId);
		ScreenLayoutMode layoutMode = null;
		WidgetInfoCreator.WidgetFactory widgetsFactory;

		if (isAndroidAutoMode) {
			widgetsFactory = new AndroidAutoWidgetsFactory(app);
			widget = widgetsFactory.createMapWidget(id, widgetType, selectedPanel);
		} else {
			layoutMode = fragment.getScreenLayoutMode();
			widgetsFactory = new MapWidgetsFactory(mapActivity);
			widget = widgetsFactory.createMapWidget(id, widgetType, selectedPanel);
		}
		if (widget != null) {
			WidgetInfoCreator creator = new WidgetInfoCreator(app, selectedAppMode, layoutMode);
			widgetInfo = creator.askCreateWidgetInfo(id, widget, widgetType, selectedPanel);
		}

		if (widgetInfo != null) {
			WidgetInfoBaseFragment settingsBaseFragment = widgetType.getSettingsFragment(app, widgetInfo);
			if (settingsBaseFragment != null) {
				addedWidget = widgetInfo;
				Bundle args = new Bundle();
				args.putString(KEY_WIDGET_ID, widgetInfo.key);
				args.putString(KEY_APP_MODE, selectedAppMode.getStringKey());

				WidgetInfoBaseFragment.showAddWidgetFragment(mapActivity.getSupportFragmentManager(),
						settingsBaseFragment, fragment, selectedAppMode, id, selectedPanel, layoutMode, isAndroidAutoMode);
			} else {
				fragment.onWidgetAdded(widgetInfo);
				mapActivity.getSupportFragmentManager().popBackStack(SearchWidgetsFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	public List<Object> getReorderList(@NonNull WidgetsPanel panel) {
		List<Object> reorderList = reorderLists.get(panel);
		return reorderList != null ? new ArrayList<>(reorderList) : new ArrayList<>();
	}

	public void setReorderList(@NonNull WidgetsPanel panel, @NonNull List<Object> reorderList) {
		List<Object> panelReorderList = reorderLists.computeIfAbsent(panel, p -> new ArrayList<>());
		panelReorderList.clear();
		panelReorderList.addAll(reorderList);
	}

}