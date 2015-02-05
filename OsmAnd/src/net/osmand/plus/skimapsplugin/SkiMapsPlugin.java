package net.osmand.plus.skimapsplugin;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import android.app.Activity;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String ID = "skimaps.plugin";
	public static final String COMPONENT = "net.osmand.skimapsPlugin";
	private OsmandApplication app;
	
	public SkiMapsPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getDescription() {
		return "This will be a plugin enabling ski maps (TODO)";
	}

	@Override
	public String getName() {
		return "Ski Maps";
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
