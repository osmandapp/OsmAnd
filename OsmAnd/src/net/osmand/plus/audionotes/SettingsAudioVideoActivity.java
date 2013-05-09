package net.osmand.plus.audionotes;



import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_3GP;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_MP4;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
public class SettingsAudioVideoActivity extends SettingsBaseActivity {

	
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

			grp.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_RECORDER, R.string.av_use_external_recorder,
					R.string.av_use_external_recorder_descr));
			grp.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_PHOTO_CAM, R.string.av_use_external_camera,
					R.string.av_use_external_camera_descr));

			entries = new String[] { "3GP", "MP4" };
			intValues = new Integer[] { VIDEO_OUTPUT_3GP, VIDEO_OUTPUT_MP4 };
			ListPreference lp = createListPreference(p.AV_VIDEO_FORMAT, entries, intValues, R.string.av_video_format,
					R.string.av_video_format_descr);
			grp.addPreference(lp);
		}
	}



}
