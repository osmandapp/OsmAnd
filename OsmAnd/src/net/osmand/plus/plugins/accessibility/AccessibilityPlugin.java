package net.osmand.plus.plugins.accessibility;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ACCESSIBILITY;

public class AccessibilityPlugin extends OsmandPlugin {

	public static final int DIRECTION_NOTIFICATION = 1;
	public static final int INCLINATION_LEFT = 2;
	public static final int INCLINATION_RIGHT = 3;

	public final OsmandPreference<AccessibilityMode> ACCESSIBILITY_MODE;
	public final OsmandPreference<Float> SPEECH_RATE;
	public final OsmandPreference<Boolean> ACCESSIBILITY_SMART_AUTOANNOUNCE;
	public final OsmandPreference<Integer> ACCESSIBILITY_AUTOANNOUNCE_PERIOD;
	public final OsmandPreference<RelativeDirectionStyle> DIRECTION_STYLE;
	public final OsmandPreference<Boolean> DIRECTION_AUDIO_FEEDBACK;
	public final OsmandPreference<Boolean> DIRECTION_HAPTIC_FEEDBACK;

	private SoundPool sounds;
	private final Map<Integer, Integer> soundIcons = new HashMap<Integer, Integer>();

	public AccessibilityPlugin(OsmandApplication app) {
		super(app);
		ACCESSIBILITY_MODE = registerEnumIntPreference("accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.values(), AccessibilityMode.class).makeProfile().cache();
		SPEECH_RATE = registerFloatPreference("speech_rate", 1f).makeProfile();
		ACCESSIBILITY_SMART_AUTOANNOUNCE = registerBooleanAccessibilityPreference("accessibility_smart_autoannounce", true).makeProfile();
		ACCESSIBILITY_AUTOANNOUNCE_PERIOD = registerIntPreference("accessibility_autoannounce_period", 10000).makeProfile().cache();
		DIRECTION_STYLE = registerEnumIntPreference("direction_style", RelativeDirectionStyle.SIDEWISE, RelativeDirectionStyle.values(), RelativeDirectionStyle.class).makeProfile().cache();
		DIRECTION_AUDIO_FEEDBACK = registerBooleanAccessibilityPreference("direction_audio_feedback", false).makeProfile();
		DIRECTION_HAPTIC_FEEDBACK = registerBooleanAccessibilityPreference("direction_haptic_feedback", false).makeProfile();
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
		return PLUGIN_ACCESSIBILITY;
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