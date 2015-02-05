package net.osmand.plus.openseamapsplugin;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import android.app.Activity;

public class OpenSeaMapsPlugin extends OsmandPlugin {

	public static final String ID = "openseamaps.plugin";
	public static final String COMPONENT = "net.osmand.openseamapsPlugin";
	private OsmandApplication app;
	

	public OpenSeaMapsPlugin(OsmandApplication app) {
		this.app = app;
	}
	

	@Override
	public String getDescription() {
		return "This will be a plugin enabling openseamaps (TODO)";
	}

	@Override
	public String getName() {
		return "Open Sea Maps";
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}


	@Override
	public String getId() {
		return ID;
	}


	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}
}
