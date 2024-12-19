package net.osmand.plus.views.mapwidgets;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.IComplexWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class WidgetInfoCreator {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ApplicationMode appMode;

	public WidgetInfoCreator(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		this.app = app;
		this.appMode = appMode;
		settings = app.getSettings();
	}

	@Nullable
	public MapWidgetInfo createWidgetInfo(@NonNull MapWidgetsFactory factory, @NonNull WidgetType widgetType) {
		MapWidget mapWidget = factory.createMapWidget(widgetType);
		if (mapWidget != null) {
			return createWidgetInfo(mapWidget);
		}
		return null;
	}

	@Nullable
	public MapWidgetInfo createWidgetInfo(@NonNull MapWidgetsFactory factory,
	                                      @NonNull String key, @NonNull WidgetType widgetType) {
		WidgetsPanel panel = widgetType.getPanel(key, appMode, settings);
		MapWidget widget = factory.createMapWidget(key, widgetType, panel);
		if (widget != null) {
			return askCreateWidgetInfo(key, widget, widgetType, panel);
		}
		return null;
	}


	@Nullable
	public MapWidgetInfo createWidgetInfo(@NonNull MapWidget widget) {
		WidgetType widgetType = widget.getWidgetType();
		if (widgetType != null) {
			String widgetId = widgetType.id;
			WidgetsPanel panel = widgetType.getPanel(widgetId, appMode, settings);
			int page = panel.getWidgetPage(appMode, widgetId, settings);
			int order = panel.getWidgetOrder(appMode, widgetId, settings);
			return createWidgetInfo(widgetId, widget, widgetType.dayIconId, widgetType.nightIconId,
					widgetType.titleId, null, page, order, panel);
		}
		return null;
	}

	@NonNull
	public MapWidgetInfo createExternalWidget(@NonNull String widgetId,
	                                          @NonNull MapWidget widget,
	                                          @DrawableRes int settingsIconId,
	                                          @Nullable String message,
	                                          @NonNull WidgetsPanel defaultPanel,
	                                          int order) {
		WidgetsPanel panel = getExternalWidgetPanel(widgetId, defaultPanel);
		int page = panel.getWidgetPage(appMode, widgetId, settings);
		int savedOrder = panel.getWidgetOrder(appMode, widgetId, settings);
		if (savedOrder != WidgetsPanel.DEFAULT_ORDER) {
			order = savedOrder;
		}
		return createWidgetInfo(widgetId, widget, settingsIconId,
				settingsIconId, MapWidgetInfo.INVALID_ID, message, page, order, panel);
	}

	@NonNull
	private WidgetsPanel getExternalWidgetPanel(@NonNull String widgetId,
	                                            @NonNull WidgetsPanel defaultPanel) {
		boolean storedInLeftPanel = WidgetsPanel.LEFT.getWidgetOrder(appMode, widgetId, settings) != WidgetsPanel.DEFAULT_ORDER;
		boolean storedInRightPanel = WidgetsPanel.RIGHT.getWidgetOrder(appMode, widgetId, settings) != WidgetsPanel.DEFAULT_ORDER;
		if (storedInLeftPanel) {
			return WidgetsPanel.LEFT;
		} else if (storedInRightPanel) {
			return WidgetsPanel.RIGHT;
		}
		return defaultPanel;
	}

	@Nullable
	public MapWidgetInfo askCreateWidgetInfo(@NonNull String widgetId, @NonNull MapWidget widget,
	                                         @NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		if (widgetType == WidgetType.AIDL_WIDGET) {
			return app.getAidlApi().askCreateExternalWidgetInfo(this, widget, widgetId, panel);
		} else {
			return createCustomWidgetInfo(widgetId, widget, widgetType, panel);
		}
	}

	@NonNull
	private MapWidgetInfo createCustomWidgetInfo(@NonNull String widgetId, @NonNull MapWidget widget,
	                                             @NonNull WidgetType widgetType, @NonNull WidgetsPanel panel) {
		int page = panel.getWidgetPage(appMode, widgetId, settings);
		int order = panel.getWidgetOrder(appMode, widgetId, settings);
		return createWidgetInfo(widgetId, widget, widgetType.dayIconId, widgetType.nightIconId,
				widgetType.titleId, null, page, order, panel);
	}

	@NonNull
	public MapWidgetInfo createWidgetInfo(@NonNull String key,
	                                      @NonNull MapWidget widget,
	                                      @DrawableRes int daySettingsIconId,
	                                      @DrawableRes int nightSettingIconId,
	                                      @StringRes int messageId,
	                                      @Nullable String message,
	                                      int page,
	                                      int order,
	                                      @NonNull WidgetsPanel widgetPanel) {
		if (widget instanceof IComplexWidget) {
			return new ComplexWidgetInfo(key, widget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		}
		if (widget instanceof SimpleWidget simpleWidget) {
			return new SimpleWidgetInfo(key, simpleWidget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		}
		if (widget instanceof TextInfoWidget textInfoWidget) {
			return new SideWidgetInfo(key, textInfoWidget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		} else {
			return new CenterWidgetInfo(key, widget, daySettingsIconId, nightSettingIconId,
					messageId, message, page, order, widgetPanel);
		}
	}

}
