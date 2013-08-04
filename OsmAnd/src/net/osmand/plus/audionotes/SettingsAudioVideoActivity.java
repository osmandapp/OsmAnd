package net.osmand.plus.audionotes;

import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_3GP;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_MP4;
// support camera focus select:
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_AUTO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_HIPERFOCAL;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_EDOF;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_INFINITY;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_MACRO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_CONTINUOUS;
////
import org.apache.commons.logging.Log;
import net.osmand.PlatformUtil;
import java.util.List;
import java.util.ArrayList;
import android.hardware.Camera.Parameters;
import android.hardware.Camera;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
public class SettingsAudioVideoActivity extends SettingsBaseActivity {

	private static final Log log = PlatformUtil.getLog(AudioVideoNotesPlugin.class);
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.av_settings);
		PreferenceScreen grp = getPreferenceScreen();
		AudioVideoNotesPlugin p = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		if (p != null) {
			String[] entries;
			Integer[] intValues;

			entries = new String[] { getString(R.string.av_def_action_choose), getString(R.string.av_def_action_audio),
					getString(R.string.av_def_action_video), getString(R.string.av_def_action_picture) };
			intValues = new Integer[] { AV_DEFAULT_ACTION_CHOOSE, AV_DEFAULT_ACTION_AUDIO, AV_DEFAULT_ACTION_VIDEO,
					AV_DEFAULT_ACTION_TAKEPICTURE };
			ListPreference defAct = createListPreference(p.AV_DEFAULT_ACTION, entries, intValues, R.string.av_widget_action,
					R.string.av_widget_action_descr);
			grp.addPreference(defAct);
			final Camera cam = openCamera();
			if (cam != null) {
				// camera type settings:
				grp.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_PHOTO_CAM, R.string.av_use_external_camera,
						R.string.av_use_external_camera_descr));
				// focus mode settings:
				// show in menu only suppoted modes:

				Parameters parameters = cam.getParameters();
				List<String> sfm = parameters.getSupportedFocusModes();
				List<String> items = new ArrayList<String>();
				List<Integer> itemsValues = new ArrayList<Integer>();
				// filtering known types for translate and set index:
				for (int index = 0; index < sfm.size(); index++) {
					if (sfm.get(index).equals("auto")) {
						items.add(getString(R.string.av_camera_focus_auto));
						itemsValues.add(AV_CAMERA_FOCUS_AUTO);
					} else if (sfm.get(index).equals("fixed")) {
						items.add(getString(R.string.av_camera_focus_hiperfocal));
						itemsValues.add(AV_CAMERA_FOCUS_HIPERFOCAL);
					} else if (sfm.get(index).equals("edof")) {
						items.add(getString(R.string.av_camera_focus_edof));
						itemsValues.add(AV_CAMERA_FOCUS_EDOF);
					} else if (sfm.get(index).equals("infinity")) {
						items.add(getString(R.string.av_camera_focus_infinity));
						itemsValues.add(AV_CAMERA_FOCUS_INFINITY);
					} else if (sfm.get(index).equals("macro")) {
						items.add(getString(R.string.av_camera_focus_macro));
						itemsValues.add(AV_CAMERA_FOCUS_MACRO);
					} else if (sfm.get(index).equals("continuous-picture")) {
						items.add(getString(R.string.av_camera_focus_continuous));
						itemsValues.add(AV_CAMERA_FOCUS_CONTINUOUS);
					}
				}
				entries = items.toArray(new String[items.size()]);
				intValues = itemsValues.toArray(new Integer[itemsValues.size()]);
				if (entries.length > 0) {
					ListPreference camFocus = createListPreference(p.AV_CAMERA_FOCUS_TYPE, entries, intValues, R.string.av_camera_focus,
							R.string.av_camera_focus_descr);
					grp.addPreference(camFocus);
				}
				// play sound on success photo:
				grp.addPreference(createCheckBoxPreference(p.AV_PHOTO_PLAY_SOUND, R.string.av_photo_play_sound,
						R.string.av_photo_play_sound_descr));
				cam.release();
			}
			// video settings:
			grp.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_RECORDER, R.string.av_use_external_recorder,
					R.string.av_use_external_recorder_descr));
			
			entries = new String[] { "3GP", "MP4" };
			intValues = new Integer[] { VIDEO_OUTPUT_3GP, VIDEO_OUTPUT_MP4 };
			ListPreference lp = createListPreference(p.AV_VIDEO_FORMAT, entries, intValues, R.string.av_video_format,
					R.string.av_video_format_descr);
			grp.addPreference(lp);
		}
	}

	protected Camera openCamera() {
		try {
			return Camera.open();
		} catch (Exception e ){
			log.error("Error open camera", e);
			return null;
		}
	}
}
