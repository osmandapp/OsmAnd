package net.osmand.access;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AccessibilityPlugin extends OsmandPlugin {

	public static final int DIRECTION_NOTIFICATION = 1;
	public static final int INCLINATION_LEFT = 2;
	public static final int INCLINATION_RIGHT = 3;

	private static final String ID = "osmand.accessibility";

	private SoundPool sounds;
	private Map<Integer, Integer> soundIcons = new HashMap<Integer, Integer>();

	public AccessibilityPlugin(OsmandApplication app) {
		super(app);
		OsmandSettings settings = app.getSettings();
		pluginPreferences.add(settings.ACCESSIBILITY_MODE);
		pluginPreferences.add(settings.SPEECH_RATE);
		pluginPreferences.add(settings.ACCESSIBILITY_SMART_AUTOANNOUNCE);
		pluginPreferences.add(settings.ACCESSIBILITY_AUTOANNOUNCE_PERIOD);
		pluginPreferences.add(settings.DIRECTION_STYLE);
		pluginPreferences.add(settings.DIRECTION_AUDIO_FEEDBACK);
		pluginPreferences.add(settings.DIRECTION_HAPTIC_FEEDBACK);
	}

	@Override
	public boolean init(@NonNull final OsmandApplication app, Activity activity) {
		sounds = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		if (sounds != null) {
			soundIcons.put(DIRECTION_NOTIFICATION, loadSoundIcon("sounds/direction_notification.ogg"));
			soundIcons.put(INCLINATION_LEFT, loadSoundIcon("sounds/inclination_left.ogg"));
			soundIcons.put(INCLINATION_RIGHT, loadSoundIcon("sounds/inclination_right.ogg"));
		}
		return true;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.osmand_accessibility_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_accessibility);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.ACCESSIBILITY_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.accessibility_prefs_descr);
	}

	@Override
	public void disable(OsmandApplication app) {
		if (sounds != null) {
			sounds.release();
			sounds = null;
		}
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_accessibility;
	}

	public void playSoundIcon(int iconId) {
		if ((sounds != null) && soundIcons.containsKey(iconId)) {
			int sound = soundIcons.get(iconId);
			if (sound != 0)
				sounds.play(sound, 1.0f, 1.0f, 0, 0, 1.0f);
		}
	}

	private int loadSoundIcon(String path) {
		try {
			return sounds.load(app.getAssets().openFd(path), 1);
		} catch (IOException e) {
			return 0;
		}
	}
}