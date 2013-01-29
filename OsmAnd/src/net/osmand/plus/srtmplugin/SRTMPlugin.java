package net.osmand.plus.srtmplugin;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	private static final Log log = PlatformUtil.getLog(SRTMPlugin.class);
	private OsmandApplication app;
	private boolean paid;
	
	@Override
	public String getId() {
		return ID;
	}

	public SRTMPlugin(OsmandApplication app, boolean paid) {
		this.app = app;
		this.paid = paid;

	}
	
	public boolean isPaid() {
		return paid;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.srtm_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {

	}
	
	@Override
	public void disable(OsmandApplication app) {
	}

}
