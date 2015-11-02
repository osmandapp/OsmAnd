package net.osmand.access;

import android.app.Activity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class AccessibilityPlugin extends OsmandPlugin {
	private static final String ID = "osmand.accessibility";
	private OsmandApplication app;

	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_accessibility_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_accessibility);
	}

	@Override
	public void registerLayers(MapActivity activity) {
	}


	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAccessibilityActivity.class;
	}


	@Override
	public int getAssetResourceName() {
		return 0;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_accessibility;
	}
}
