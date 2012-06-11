package net.osmand.access;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class AccessibilityPlugin extends OsmandPlugin {
	private static final String ID = "osmand.accessibility";
	private OsmandSettings settings;
	private OsmandApplication app;
	private ListPreference accessibilityModePreference;
	private ListPreference directionStylePreference;
	
	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
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
	public void settingsActivityUpdate(SettingsActivity activity) {
		if(accessibilityModePreference != null) {
			accessibilityModePreference.setSummary(app.getString(R.string.accessibility_mode_descr) + "  [" + settings.ACCESSIBILITY_MODE.get().toHumanString(app) + "]");
		}
		if(directionStylePreference != null) {
			directionStylePreference.setSummary(app.getString(R.string.settings_direction_style_descr) + "  [" + settings.DIRECTION_STYLE.get().toHumanString(app) + "]");
		}
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, final PreferenceScreen screen) {
		PreferenceScreen grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setTitle(R.string.accessibility_preferences);
		grp.setSummary(R.string.accessibility_preferences_descr);
		grp.setKey("accessibility_preferences");
		((PreferenceCategory)screen.findPreference("global_settings")).addPreference(grp);

		String[] entries = new String[AccessibilityMode.values().length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = AccessibilityMode.values()[i].toHumanString(activity);
		}
		accessibilityModePreference = activity.createListPreference(settings.ACCESSIBILITY_MODE, entries, AccessibilityMode.values(),
				R.string.accessibility_mode, R.string.accessibility_mode_descr);
		accessibilityModePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PreferenceCategory accessibilityOptions = ((PreferenceCategory)(screen.findPreference("accessibility_options")));
				if (accessibilityOptions != null)
					accessibilityOptions.setEnabled(app.accessibilityEnabled());
				accessibilityModePreference.setSummary(app.getString(R.string.accessibility_mode_descr) + "  [" + settings.ACCESSIBILITY_MODE.get().toHumanString(app) + "]");
				return true;
			}
		});
		
		grp.addPreference(accessibilityModePreference);
		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setKey("accessibility_options");
		grp.addPreference(cat);

		entries = new String[RelativeDirectionStyle.values().length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = RelativeDirectionStyle.values()[i].toHumanString(activity);
		}
		directionStylePreference = activity.createListPreference(settings.DIRECTION_STYLE, entries, RelativeDirectionStyle.values(),
				R.string.settings_direction_style, R.string.settings_direction_style_descr);
		directionStylePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				directionStylePreference.setSummary(app.getString(R.string.settings_direction_style_descr) + "  [" + settings.DIRECTION_STYLE.get().toHumanString(app) + "]");
				return true;
			}
		});
		cat.addPreference(directionStylePreference);

		cat.addPreference(activity.createCheckBoxPreference(settings.ZOOM_BY_TRACKBALL, R.string.zoom_by_trackball,
				R.string.zoom_by_trackball_descr));
		cat.addPreference(activity.createCheckBoxPreference(settings.SCROLL_MAP_BY_GESTURES, R.string.scroll_map_by_gestures,
				R.string.scroll_map_by_gestures_descr));
		cat.addPreference(activity.createCheckBoxPreference(settings.ZOOM_BY_TRACKBALL, R.string.use_short_object_names,
				R.string.use_short_object_names_descr));
		cat.addPreference(activity.createCheckBoxPreference(settings.ACCESSIBILITY_EXTENSIONS, R.string.accessibility_extensions,
				R.string.accessibility_extensions));
	}


}
