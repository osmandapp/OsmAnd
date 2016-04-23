package net.osmand.access;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class AccessibilityPlugin extends OsmandPlugin {

	public static final int DIRECTION_NOTIFICATION = 1;
	public static final int INCLINATION_LEFT = 2;
	public static final int INCLINATION_RIGHT = 3;

	private static final String ID = "osmand.accessibility";
	private OsmandApplication app;
	private SoundPool sounds;
	private Map<Integer, Integer> soundIcons = new HashMap<Integer, Integer>();

	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(final OsmandApplication app, Activity activity) {
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
	public void registerLayers(MapActivity activity) {
	}


	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAccessibilityActivity.class;
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
