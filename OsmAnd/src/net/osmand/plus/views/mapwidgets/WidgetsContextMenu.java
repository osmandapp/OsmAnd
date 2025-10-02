package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment.KEY_APP_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment.KEY_WIDGET_ID;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment;
import net.osmand.plus.views.mapwidgets.dialogs.DeleteWidgetConfirmationController;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData.CustomDropDown;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WidgetsContextMenu {

	static public void showMenu(@NonNull View view, @NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
	                            @Nullable String customId, @Nullable List<PopUpMenuItem> widgetActions,
	                            WidgetsPanel widgetsPanel, boolean nightMode, boolean usedOnMap) {
		boolean verticalWidget = widgetsPanel.isPanelVertical();
		OsmandApplication app = mapActivity.getApp();
		OsmandSettings settings = app.getSettings();
		List<PopUpMenuItem> items = new ArrayList<>();
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		String widgetId = customId != null ? customId : widgetType.id;
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		ApplicationMode appMode = settings.getApplicationMode();

		if (widgetInfo != null) {
			UiUtilities uiUtilities = app.getUIUtilities();
			int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

			if (widgetInfo.widget instanceof ISupportMultiRow) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(verticalWidget ? R.string.add_widget_to_the_left : R.string.add_widget_above)
						.setIcon(uiUtilities.getPaintedIcon(verticalWidget ? R.drawable.ic_action_add_item_left : R.drawable.ic_action_add_item_above, iconColor))
						.setOnClickListener(item -> ConfigureWidgetsFragment.showInstance(mapActivity, widgetInfo.getWidgetPanel(), appMode, widgetId, false))
						.create());

				items.add(new PopUpMenuItem.Builder(app)
						.setIcon(uiUtilities.getPaintedIcon(verticalWidget ? R.drawable.ic_action_add_item_right : R.drawable.ic_action_add_item_below, iconColor))
						.setTitleId(verticalWidget ? R.string.add_widget_to_the_right : R.string.add_widget_below)
						.setOnClickListener(item -> ConfigureWidgetsFragment.showInstance(mapActivity, widgetInfo.getWidgetPanel(), appMode, widgetId, true))
						.create());
			}

			if (!Algorithms.isEmpty(widgetActions)) {
				items.addAll(widgetActions);
			}

			WidgetInfoBaseFragment fragment = widgetType.getSettingsFragment(app, widgetInfo);
			if (fragment != null) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_settings)
						.setOnClickListener(item -> {
							Bundle args = new Bundle();
							args.putString(KEY_WIDGET_ID, widgetInfo.key);
							args.putString(KEY_APP_MODE, appMode.getStringKey());
							FragmentManager manager = mapActivity.getSupportFragmentManager();
							WidgetInfoBaseFragment.showInstance(manager, fragment, null, appMode, widgetInfo.key, widgetsPanel);
						})
						.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_settings_outlined, iconColor))
						.showTopDivider(Algorithms.isEmpty(widgetActions) && !items.isEmpty())
						.create());
			}

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_delete)
					.setOnClickListener(item -> DeleteWidgetConfirmationController.showDialog(
							mapActivity, appMode, widgetInfo, usedOnMap, null)
					)
					.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_delete_outlined, iconColor))
					.showTopDivider(true)
					.create());

			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = view;
			displayData.menuItems = items;
			displayData.nightMode = nightMode;
			displayData.widthMode = PopUpMenuWidthMode.STANDARD;
			displayData.showCompound = false;
			displayData.customDropDown = CustomDropDown.NONE;
			displayData.layoutId = R.layout.popup_menu_item_full_divider;
			PopUpMenu.show(displayData);
		}
	}
}
