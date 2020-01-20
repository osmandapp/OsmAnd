package net.osmand.access;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.access.RelativeDirectionStyle;
import net.osmand.plus.settings.BaseSettingsFragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AccessibilityPlugin extends OsmandPlugin {

	public static final int DIRECTION_NOTIFICATION = 1;
	public static final int INCLINATION_LEFT = 2;
	public static final int INCLINATION_RIGHT = 3;

	private static final String ID = "osmand.accessibility";
	private OsmandApplication app;
	private SoundPool sounds;
	private Map<Integer, Integer> soundIcons = new HashMap<Integer, Integer>();

	public final OsmandSettings.OsmandPreference<AccessibilityMode> ACCESSIBILITY_MODE;
	public final OsmandSettings.OsmandPreference<Float> SPEECH_RATE;
	public final OsmandSettings.OsmandPreference<Boolean> ACCESSIBILITY_SMART_AUTOANNOUNCE;
	public final OsmandSettings.OsmandPreference<Integer> ACCESSIBILITY_AUTOANNOUNCE_PERIOD;
	public final OsmandSettings.OsmandPreference<Boolean> DISABLE_OFFROUTE_RECALC;
	public final OsmandSettings.OsmandPreference<Boolean> DISABLE_WRONG_DIRECTION_RECALC;
	public final OsmandSettings.OsmandPreference<RelativeDirectionStyle> DIRECTION_STYLE;
	public final OsmandSettings.OsmandPreference<Boolean> DIRECTION_AUDIO_FEEDBACK;
	public final OsmandSettings.OsmandPreference<Boolean> DIRECTION_HAPTIC_FEEDBACK;

	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
		ACCESSIBILITY_MODE = registerEnumIntPreference(app, "accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.values(), AccessibilityMode.class).makeProfile().cache();
		SPEECH_RATE = registerFloatPreference(app, "speech_rate", 1f).makeProfile();
		ACCESSIBILITY_SMART_AUTOANNOUNCE = registerBooleanAccessibilityPreference(app, "accessibility_smart_autoannounce", true).makeProfile();
		ACCESSIBILITY_AUTOANNOUNCE_PERIOD = registerIntPreference(app, "accessibility_autoannounce_period", 10000).makeProfile().cache();
		DISABLE_OFFROUTE_RECALC = registerBooleanAccessibilityPreference(app, "disable_offroute_recalc", false).makeProfile();
		DISABLE_WRONG_DIRECTION_RECALC = registerBooleanAccessibilityPreference(app, "disable_wrong_direction_recalc", false).makeProfile();
		DIRECTION_STYLE = registerEnumIntPreference(app, "direction_style", RelativeDirectionStyle.SIDEWISE, RelativeDirectionStyle.values(), RelativeDirectionStyle.class).makeProfile().cache();
		DIRECTION_AUDIO_FEEDBACK = registerBooleanAccessibilityPreference(app, "direction_audio_feedback", false).makeProfile();
		DIRECTION_HAPTIC_FEEDBACK = registerBooleanAccessibilityPreference(app, "direction_haptic_feedback", false).makeProfile();
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
	public String getDescription() {
		return app.getString(R.string.osmand_accessibility_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_accessibility);
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAccessibilityActivity.class;
	}

	@Override
	public Class<? extends BaseSettingsFragment> getSettingsFragment() {
		return AccessibilitySettingsFragment.class;
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
	public int getAssetResourceName() {
		return 0;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_accessibility;
	}

	private OsmandSettings.CommonPreference<Boolean> registerBooleanAccessibilityPreference(OsmandApplication app, String prefId, boolean defValue) {
		OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().registerBooleanAccessibilityPreference(prefId, defValue);
		pluginPreferences.add(preference);
		return preference;
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