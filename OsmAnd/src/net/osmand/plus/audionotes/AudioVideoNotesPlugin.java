package net.osmand.plus.audionotes;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import org.apache.commons.logging.Log;

public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final String ID = "osmand.audionotes";
	private static final Log log = LogUtil.getLog(AudioVideoNotesPlugin.class);
	private OsmandApplication app;
	
	@Override
	public String getId() {
		return ID;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		this.app = app;

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.audionotes_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.audionotes_plugin_name);
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
