package net.osmand.plus.development;

import net.osmand.plus.OptionsMenuHelper;
import net.osmand.plus.OptionsMenuHelper.OnOptionsMenuClick;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

public class OsmandDevelopmentPlugin extends OsmandPlugin {
	private static final String ID = "osmand.development";
	private OsmandApplication app;
	
	public OsmandDevelopmentPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_development_plugin_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.debugging_and_development);
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setTitle(R.string.debugging_and_development);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsDevelopmentActivity.class));
				return true;
			}
		});
		screen.addPreference(grp);
	}

	@Override
	public void registerLayers(MapActivity activity) {
	}
}
