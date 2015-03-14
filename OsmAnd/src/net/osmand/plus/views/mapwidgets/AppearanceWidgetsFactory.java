package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class AppearanceWidgetsFactory {

	public static AppearanceWidgetsFactory INSTANCE = new AppearanceWidgetsFactory();
	public static boolean POSITION_ON_THE_MAP = false;

	public void registerAppearanceWidgets(final MapActivity map, final MapInfoLayer mapInfoLayer,
			final MapWidgetRegistry mapInfoControls) {
		final OsmandMapTileView view = map.getMapView();

		final MapWidgetRegistry.MapWidgetRegInfo showRuler = mapInfoControls.registerAppearanceWidget(0, 0,
		// R.drawable.widget_ruler, R.drawable.widget_ruler,
				R.string.map_widget_show_ruler, "showRuler", view.getSettings().SHOW_RULER);
		showRuler.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_RULER.set(!view.getSettings().SHOW_RULER.get());
				view.refreshMap();
			}
		});
		final MapWidgetRegistry.MapWidgetRegInfo showDestinationArrow = mapInfoControls.registerAppearanceWidget(0, 0,
				// R.drawable.widget_ruler, R.drawable.widget_ruler,
				R.string.map_widget_show_destination_arrow, "show_destination_arrow",
				view.getSettings().SHOW_DESTINATION_ARROW);
		showDestinationArrow.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_DESTINATION_ARROW.set(!view.getSettings().SHOW_DESTINATION_ARROW.get());
				mapInfoLayer.recreateControls();
			}
		});

		final MapWidgetRegistry.MapWidgetRegInfo transparent = mapInfoControls.registerAppearanceWidget(0, 0,
		// R.drawable.widget_ruler, R.drawable.widget_ruler,
				R.string.map_widget_transparent, "transparent", view.getSettings().TRANSPARENT_MAP_THEME);
		transparent.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().TRANSPARENT_MAP_THEME.set(!view.getSettings().TRANSPARENT_MAP_THEME.get());
				mapInfoLayer.recreateControls();
			}
		});
		final MapWidgetRegistry.MapWidgetRegInfo centerPosition = mapInfoControls.registerAppearanceWidget(0, 0,
		// R.drawable.widget_position_marker, R.drawable.widget_position_marker
				R.string.always_center_position_on_map, "centerPosition", view.getSettings().CENTER_POSITION_ON_MAP);
		centerPosition.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().CENTER_POSITION_ON_MAP.set(!view.getSettings().CENTER_POSITION_ON_MAP.get());
				map.updateApplicationModeSettings();
				view.refreshMap(true);
				mapInfoLayer.recreateControls();
			}
		});
	}

}
