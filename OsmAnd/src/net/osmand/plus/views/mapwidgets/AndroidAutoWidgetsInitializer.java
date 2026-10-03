package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.CURRENT_TIME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.widgets.CurrentTimeWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AndroidAutoWidgetsInitializer {

	private final OsmandSettings settings;
	private final ApplicationMode appMode;

	private final WidgetInfoCreator.WidgetFactory factory;
	private final WidgetInfoCreator creator;

	private final List<MapWidgetInfo> mapWidgetsCache = new ArrayList<>();

	private AndroidAutoWidgetsInitializer(@NonNull OsmandApplication app, ApplicationMode appMode) {
		this.appMode = appMode;
		settings = app.getSettings();
		factory = new AndroidAutoWidgetsFactory(app);
		creator = new WidgetInfoCreator(app, appMode, null);
	}

	private List<MapWidgetInfo> createAllControls() {
		createCommonWidgets();
		PluginsHelper.createAndroidAutoWidgets(mapWidgetsCache, appMode);
		createCustomWidgets();
		return mapWidgetsCache;
	}

	private void createCommonWidgets() {
		addWidgetInfo(CURRENT_TIME);
	}

	private void createCustomWidgets() {
		List<String> widgetKeys = settings.getAndroidAutoCustomWidgetsKeys().getStringsListForProfile(appMode);
		if (!Algorithms.isEmpty(widgetKeys)) {
			for (String key : widgetKeys) {
				WidgetType widgetType = WidgetType.getById(key);
				if (widgetType != null) {
					MapWidgetInfo widgetInfo = creator.createAndroidAutoWidgetInfo(factory, key, widgetType);
					if (widgetInfo != null) {
						mapWidgetsCache.add(widgetInfo);
					}
				}
			}
		}
	}


	private void addWidgetInfo(@NonNull WidgetType widgetType) {
		MapWidgetInfo widgetInfo = creator.createAndroidAutoWidgetInfo(factory, widgetType);
		if (widgetInfo != null) {
			mapWidgetsCache.add(widgetInfo);
		}
	}

	public static List<MapWidgetInfo> createAllControls(@NonNull OsmandApplication app,
	                                                    @NonNull ApplicationMode appMode) {
		AndroidAutoWidgetsInitializer initializer = new AndroidAutoWidgetsInitializer(app, appMode);
		return initializer.createAllControls();
	}

	static public class AndroidAutoWidgetsFactory implements WidgetInfoCreator.WidgetFactory {

		private final OsmandApplication app;

		public AndroidAutoWidgetsFactory(OsmandApplication app) {
			this.app = app;
		}

		@Override
		public MapWidget createMapWidget(@Nullable String customId, @NonNull WidgetType widgetType, @Nullable WidgetsPanel panel) {
			// add more types here as android auto support is added to widgets
			switch (widgetType) {
				case CURRENT_TIME:
					return new CurrentTimeWidget(app, customId, panel);
				default:
					return PluginsHelper.createAndroidAutoWidget(widgetType, customId, panel);
			}
		}
	}
}
