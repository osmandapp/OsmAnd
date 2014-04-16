package net.osmand.plus.sherpafy;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.activities.MapActivity;

public class SherpafyPlugin extends OsmandPlugin {

	public static final String ID = "osmand.shepafy";
	protected OsmandApplication app;
	
	@Override
	public String getId() {
		return  ID ;
	}

	public SherpafyPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public String getDescription() {
		return "Sherpafy plugin (TODO externalize)";
	}

	@Override
	public String getName() {
		return "Sherpafy plugin ";
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}
	
	@Override
	public void disable(OsmandApplication app) {
	}

	@Override
	public void registerLayers(MapActivity activity) {
	}

}
