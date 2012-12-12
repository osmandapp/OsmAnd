package net.osmand.plus.distancecalculator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

/**
 * @author Maciej Papiez
 *
 */
public class DistanceCalculatorPlugin extends OsmandPlugin {
	private static final String ID = "osmand.distance";
	private OsmandApplication app;
	private DistanceCalculatorLayer distanceCalculatorLayer;

	public DistanceCalculatorPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_distance_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_distance_plugin_name);
	}

	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		// remove old if existing
		if(distanceCalculatorLayer != null) {
			activity.getMapView().removeLayer(distanceCalculatorLayer);
		}
		distanceCalculatorLayer = new DistanceCalculatorLayer(activity);
		activity.getMapView().addLayer(distanceCalculatorLayer, 8.5f);
	}

}
