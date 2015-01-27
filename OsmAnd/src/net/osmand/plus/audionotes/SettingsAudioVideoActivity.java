package net.osmand.plus.audionotes;

import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_3GP;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.VIDEO_OUTPUT_MP4;
// camera picture size:
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.cameraPictureSizeDefault;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_PHOTO_SIZE_DEFAULT;
// support camera focus select:
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_AUTO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_HIPERFOCAL;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_EDOF;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_INFINITY;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_MACRO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_CONTINUOUS;
////
import net.osmand.plus.OsmandApplication;
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
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.av_settings);
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

				Parameters parameters = cam.getParameters();

				// Photo picture size
				// get supported sizes:
				List<Camera.Size> psps = parameters.getSupportedPictureSizes();
				// list of megapixels of each resolution:
				List<Integer> mpix = new ArrayList<Integer>();
				// list of index each resolution in list, returned by getSupportedPictureSizes():
				List<Integer> picSizesValues = new ArrayList<Integer>();
				// fill lists for sort:
				for (int index = 0; index < psps.size(); index++) {
					mpix.add( (psps.get(index)).width*(psps.get(index)).height );
					picSizesValues.add(index);
				}
				// sort list for max resolution in begining of list:
				for (int i=0; i < mpix.size(); i++ )
				{
					for (int j=0; j < mpix.size() - i - 1; j++ )
					{
						if ( mpix.get(j) < mpix.get( j + 1 ) )
						{
							// change elements:
							int tmp=mpix.get( j + 1 );
							mpix.set( j + 1, mpix.get( j ) );
							mpix.set( j, tmp );

							tmp=picSizesValues.get( j + 1 );
							picSizesValues.set( j + 1, picSizesValues.get( j ) );
							picSizesValues.set( j, tmp );
						}
					}
				}
				// set default photo size to max resolution (set index of element with max resolution in List, returned by getSupportedPictureSizes() ):
				cameraPictureSizeDefault = picSizesValues.get(0);
				log.debug("onCreate() set cameraPictureSizeDefault=" + cameraPictureSizeDefault);

				List<String> itemsPicSizes = new ArrayList<String>();
				String prefix;
				for (int index = 0; index < psps.size(); index++) {
					float px=(float)((psps.get( picSizesValues.get(index) )).width*(psps.get( picSizesValues.get(index) )).height);
					if(px>102400) // 100 K
					{
						px=px/1048576;
						prefix="Mpx";
					}
					else
						{
							px=px/1024;
							prefix="Kpx";
						}

					itemsPicSizes.add( (psps.get( picSizesValues.get(index) )).width + 
						"x" + 
						(psps.get( picSizesValues.get(index) )).height + 
						" ( " +
						String.format("%.2f", px ) +
						" " +
						prefix +
						" )");
				}
				log.debug("onCreate() set default size: width=" + psps.get( cameraPictureSizeDefault ).width + " height=" 
					+ psps.get( cameraPictureSizeDefault ).height + " index in ps=" + cameraPictureSizeDefault );

				entries = itemsPicSizes.toArray(new String[itemsPicSizes.size()]);
				intValues = picSizesValues.toArray(new Integer[picSizesValues.size()]);
				if (entries.length > 0) {
					ListPreference camSizes = createListPreference(p.AV_CAMERA_PICTURE_SIZE, entries, intValues, R.string.av_camera_pic_size,
							R.string.av_camera_pic_size_descr);
					grp.addPreference(camSizes);
				}

				// focus mode settings:
				// show in menu only suppoted modes:
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
