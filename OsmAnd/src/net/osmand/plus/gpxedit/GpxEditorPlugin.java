package net.osmand.plus.gpxedit;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

/**
 * @author Koen Rabaey
 */
public class GpxEditorPlugin extends OsmandPlugin {

	public static final String ID = "osmand.gpx.editor";

	private OsmandApplication _app;
	private GpxEditorLayer _gpxEditorLayer;

	private final GpxEditorStates _states = new GpxEditorStates();

	public GpxEditorPlugin(OsmandApplication app) {
		_app = app;
		ApplicationMode.regWidget(ID, ApplicationMode.DEFAULT);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return _app.getString(R.string.gpx_edit_plugin_description);
	}

	@Override
	public String getName() {
		return _app.getString(R.string.gpx_edit_plugin_name);
	}

	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(final MapActivity activity) {
		if (_gpxEditorLayer != null) {
			activity.getMapView().removeLayer(_gpxEditorLayer);
		}
		final MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();

		final GpxEditorTopWidget topWidget = new GpxEditorTopWidget(activity);

		//first register to create regInfo
		mapInfoLayer.getMapInfoControls().registerTopWidget(topWidget, android.R.color.transparent, R.string.gpx_edit_redo, ID, MapWidgetRegistry.MAIN_CONTROL, 20, false);

		//find the registered widget
		MapWidgetRegistry.MapWidgetRegInfo registered = null;
		for (MapWidgetRegistry.MapWidgetRegInfo widgetRegInfo : mapInfoLayer.getMapInfoControls().getTopWidgets()) {
			if (widgetRegInfo.key.equals(ID)) {
				registered = widgetRegInfo;
			}
		}

		//remove the registered widget
		if (registered != null) {
			mapInfoLayer.getMapInfoControls().getTopWidgets().remove(registered);
		}

		//register side widget
		final GpxEditorWidget widget = new GpxEditorWidget(activity, _states, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText(), registered);
		mapInfoLayer.getMapInfoControls().registerSideWidget(widget, R.drawable.ic_action_polygom_dark, R.string.gpx_edit_new_gpx, ID, false, 21);

		//register layer
		_gpxEditorLayer = new GpxEditorLayer(activity, widget, _states);
		activity.getMapView().addLayer(_gpxEditorLayer, 8.7f);

		//
		topWidget.init(_states, widget, _gpxEditorLayer);

		mapInfoLayer.recreateControls();
	}
}
