package net.osmand.plus.audionotes;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AUDIO_BITRATE_DEFAULT;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_AUTO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_CONTINUOUS;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_EDOF;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_HIPERFOCAL;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_INFINITY;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_MACRO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_AUDIO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_CHOOSE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_TAKEPICTURE;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_DEFAULT_ACTION_VIDEO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.cameraPictureSizeDefault;

// camera picture size:
// support camera focus select:
////

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

			entries = new String[]{getString(R.string.av_def_action_choose), getString(R.string.av_def_action_audio),
					getString(R.string.av_def_action_video), getString(R.string.av_def_action_picture)};
			intValues = new Integer[]{AV_DEFAULT_ACTION_CHOOSE, AV_DEFAULT_ACTION_AUDIO, AV_DEFAULT_ACTION_VIDEO,
					AV_DEFAULT_ACTION_TAKEPICTURE};
			ListPreference defAct = createListPreference(p.AV_DEFAULT_ACTION, entries, intValues, R.string.av_widget_action,
					R.string.av_widget_action_descr);
			grp.addPreference(defAct);

			PreferenceCategory photo = new PreferenceCategory(this);
			photo.setTitle(R.string.shared_string_photo);
			grp.addPreference(photo);

			final Camera cam = openCamera();
			if (cam != null) {
				// camera type settings
				photo.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_PHOTO_CAM, R.string.av_use_external_camera,
						R.string.av_use_external_camera_descr));

				Parameters parameters = cam.getParameters();
				createCameraPictureSizesPref(p, photo, parameters);
				createCameraFocusModesPref(p, photo, parameters);

				// play sound on success photo
				photo.addPreference(createCheckBoxPreference(p.AV_PHOTO_PLAY_SOUND, R.string.av_photo_play_sound,
						R.string.av_photo_play_sound_descr));

				cam.release();
			}

			// video settings
			PreferenceCategory video = new PreferenceCategory(this);
			video.setTitle(R.string.shared_string_video);
			grp.addPreference(video);

			video.addPreference(createCheckBoxPreference(p.AV_EXTERNAL_RECORDER, R.string.av_use_external_recorder,
					R.string.av_use_external_recorder_descr));

//			entries = new String[] { "3GP", "MP4" };
//			intValues = new Integer[] { VIDEO_OUTPUT_3GP, VIDEO_OUTPUT_MP4 };
//			ListPreference lp = createListPreference(p.AV_VIDEO_FORMAT, entries, intValues, R.string.av_video_format,
//					R.string.av_video_format_descr);
//			video.addPreference(lp);

			List<String> qNames = new ArrayList<>();
			List<Integer> qValues = new ArrayList<>();
			if (Build.VERSION.SDK_INT < 11 || CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_LOW)) {
				qNames.add(getString(R.string.av_video_quality_low));
				qValues.add(CamcorderProfile.QUALITY_LOW);
			}
			if (Build.VERSION.SDK_INT >= 11 && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
				qNames.add("720 x 480 (480p)");
				qValues.add(CamcorderProfile.QUALITY_480P);
			}
			if (Build.VERSION.SDK_INT >= 11 && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
				qNames.add("1280 x 720 (720p)");
				qValues.add(CamcorderProfile.QUALITY_720P);
			}
			if (Build.VERSION.SDK_INT >= 11 && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
				qNames.add("1920 x 1080 (1080p)");
				qValues.add(CamcorderProfile.QUALITY_1080P);
			}
			if (Build.VERSION.SDK_INT >= 21 && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2160P)) {
				qNames.add("3840x2160 (2160p)");
				qValues.add(CamcorderProfile.QUALITY_2160P);
			}
			if (Build.VERSION.SDK_INT < 11 || CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
				qNames.add(getString(R.string.av_video_quality_high));
				qValues.add(CamcorderProfile.QUALITY_HIGH);
			}

			ListPreference lp = createListPreference(p.AV_VIDEO_QUALITY,
					qNames.toArray(new String[qNames.size()]),
					qValues.toArray(new Integer[qValues.size()]),
					R.string.av_video_quality,
					R.string.av_video_quality_descr);
			video.addPreference(lp);

			// Recorder Split settings
			PreferenceCategory recSplit = new PreferenceCategory(this);
			recSplit.setTitle(R.string.rec_split);
			grp.addPreference(recSplit);

			recSplit.addPreference(createCheckBoxPreference(p.AV_RECORDER_SPLIT, R.string.rec_split_title,
					R.string.rec_split_desc));

			intValues = new Integer[]{1, 2, 3, 4, 5, 7, 10, 15, 20, 25, 30};
			entries = new String[intValues.length];
			int i = 0;
			String minStr = getString(R.string.int_min);
			for (int v : intValues) {
				entries[i++] = String.valueOf(v) + " " + minStr;
			}
			lp = createListPreference(p.AV_RS_CLIP_LENGTH, entries, intValues,
					R.string.rec_split_clip_length,
					R.string.rec_split_clip_length_desc);
			recSplit.addPreference(lp);

			File dir = getMyApplication().getAppPath("").getParentFile();
			long size = 0;
			if (dir.canRead()) {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				size = ((long) fs.getBlockSize() * (long) fs.getBlockCount()) / (1 << 30);
			}
			if (size > 0) {
				int value = 1;
				ArrayList<Integer> gbList = new ArrayList<>();
				while (value < size) {
					gbList.add(value);
					if (value < 5) {
						value++;
					} else {
						value += 5;
					}
				}
				if (value != size) {
					gbList.add((int) size);
				}
				entries = new String[gbList.size()];
				intValues = new Integer[gbList.size()];
				i = 0;
				for (int v : gbList) {
					intValues[i] = v;
					entries[i] = AndroidUtils.formatSize(this, v * (1l << 30));
					i++;
				}

				lp = createListPreference(p.AV_RS_STORAGE_SIZE, entries, intValues,
						R.string.rec_split_storage_size,
						R.string.rec_split_storage_size_desc);
				recSplit.addPreference(lp);
			}

			// audio settings
			PreferenceCategory audio = new PreferenceCategory(this);
			audio.setTitle(R.string.shared_string_audio);
			grp.addPreference(audio);

			entries = new String[]{"Default", "AAC"};
			intValues = new Integer[]{MediaRecorder.AudioEncoder.DEFAULT, MediaRecorder.AudioEncoder.AAC};
			lp = createListPreference(p.AV_AUDIO_FORMAT, entries, intValues,
					R.string.av_audio_format,
					R.string.av_audio_format_descr);
			audio.addPreference(lp);

			entries = new String[]{"Default", "16 kbps", "32 kbps", "48 kbps", "64 kbps", "96 kbps", "128 kbps"};
			intValues = new Integer[]{AUDIO_BITRATE_DEFAULT, 16 * 1024, 32 * 1024, 48 * 1024, 64 * 1024, 96 * 1024, 128 * 1024};
			lp = createListPreference(p.AV_AUDIO_BITRATE, entries, intValues,
					R.string.av_audio_bitrate,
					R.string.av_audio_bitrate_descr);
			audio.addPreference(lp);
		}
	}

	private void createCameraPictureSizesPref(AudioVideoNotesPlugin p, PreferenceCategory photo, Parameters parameters) {
		String[] entries;
		Integer[] intValues;
		// Photo picture size
		// get supported sizes
		List<Camera.Size> psps = parameters.getSupportedPictureSizes();
		if (psps == null) {
			return;
		}
		// list of megapixels of each resolution
		List<Integer> mpix = new ArrayList<Integer>();
		// list of index each resolution in list, returned by getSupportedPictureSizes()
		List<Integer> picSizesValues = new ArrayList<Integer>();
		// fill lists for sort
		for (int index = 0; index < psps.size(); index++) {
			mpix.add((psps.get(index)).width * (psps.get(index)).height);
			picSizesValues.add(index);
		}
		// sort list for max resolution in begining of list
		for (int i = 0; i < mpix.size(); i++) {
			for (int j = 0; j < mpix.size() - i - 1; j++) {
				if (mpix.get(j) < mpix.get(j + 1)) {
					// change elements
					int tmp = mpix.get(j + 1);
					mpix.set(j + 1, mpix.get(j));
					mpix.set(j, tmp);

					tmp = picSizesValues.get(j + 1);
					picSizesValues.set(j + 1, picSizesValues.get(j));
					picSizesValues.set(j, tmp);
				}
			}
		}
		// set default photo size to max resolution (set index of element with max resolution in List, returned by getSupportedPictureSizes() )
		cameraPictureSizeDefault = picSizesValues.get(0);
		log.debug("onCreate() set cameraPictureSizeDefault=" + cameraPictureSizeDefault);

		List<String> itemsPicSizes = new ArrayList<String>();
		String prefix;
		for (int index = 0; index < psps.size(); index++) {
			float px = (float) ((psps.get(picSizesValues.get(index))).width * (psps.get(picSizesValues.get(index))).height);
			if (px > 102400) // 100 K
			{
				px = px / 1048576;
				prefix = "Mpx";
			} else {
				px = px / 1024;
				prefix = "Kpx";
			}

			itemsPicSizes.add((psps.get(picSizesValues.get(index))).width +
					"x" +
					(psps.get(picSizesValues.get(index))).height +
					" ( " +
					String.format("%.2f", px) +
					" " +
					prefix +
					" )");
		}
		log.debug("onCreate() set default size: width=" + psps.get(cameraPictureSizeDefault).width + " height="
				+ psps.get(cameraPictureSizeDefault).height + " index in ps=" + cameraPictureSizeDefault);

		entries = itemsPicSizes.toArray(new String[itemsPicSizes.size()]);
		intValues = picSizesValues.toArray(new Integer[picSizesValues.size()]);
		if (entries.length > 0) {
			ListPreference camSizes = createListPreference(p.AV_CAMERA_PICTURE_SIZE, entries, intValues, R.string.av_camera_pic_size,
					R.string.av_camera_pic_size_descr);
			photo.addPreference(camSizes);
		}
	}

	private void createCameraFocusModesPref(AudioVideoNotesPlugin p, PreferenceCategory photo, Parameters parameters) {
		String[] entries;
		Integer[] intValues;
		// focus mode settings
		// show in menu only suppoted modes
		List<String> sfm = parameters.getSupportedFocusModes();
		if (sfm == null) {
			return;
		}
		List<String> items = new ArrayList<String>();
		List<Integer> itemsValues = new ArrayList<Integer>();
		// filtering known types for translate and set index
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
			photo.addPreference(camFocus);
		}
	}

	protected Camera openCamera() {
		try {
			return Camera.open();
		} catch (Exception e) {
			log.error("Error open camera", e);
			return null;
		}
	}
}
