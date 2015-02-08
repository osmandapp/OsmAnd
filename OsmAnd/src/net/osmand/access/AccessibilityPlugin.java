package net.osmand.access;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.app.Activity;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

public class AccessibilityPlugin extends OsmandPlugin {
	private static final String ID = "osmand.accessibility";
	private OsmandApplication app;
	
	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app, Activity activity) {
		return true;
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
		return app.getString(R.string.accessibility_preferences);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAccessibilityActivity.class;
	}


}
