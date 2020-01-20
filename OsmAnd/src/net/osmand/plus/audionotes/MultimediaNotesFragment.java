package net.osmand.plus.audionotes;

import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.StatFs;
import android.support.v7.preference.Preference;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AUDIO_BITRATE_DEFAULT;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_AUTO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_CONTINUOUS;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_EDOF;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_HIPERFOCAL;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_INFINITY;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.AV_CAMERA_FOCUS_MACRO;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.NOTES_TAB;
import static net.osmand.plus.audionotes.AudioVideoNotesPlugin.cameraPictureSizeDefault;

public class MultimediaNotesFragment extends BaseSettingsFragment {

	private static final Log log = PlatformUtil.getLog(MultimediaNotesFragment.class);

	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String OPEN_NOTES = "open_notes";

	@Override
	protected void setupPreferences() {
		AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);

		if (plugin != null) {

			Camera cam = openCamera();
			if (cam != null) {
				setupExternalPhotoCamPref(plugin);
				setupCameraPictureSizePref(cam, plugin);
				setupCameraFocusTypePref(cam, plugin);
				setupPhotoPlaySoundPref(plugin);
				cam.release();
			}

			setupAudioFormatPref(plugin);
			setupAudioBitratePref(plugin);

			setupExternalRecorderPref(plugin);
			setupVideoQualityPref(plugin);

			setupRecorderSplitPref(plugin);
			setupClipLengthPref(plugin);
			setupStorageSizePref(plugin);

			setupOpenNotesDescrPref();
			setupOpenNotesPref();

			setupCopyProfileSettingsPref();
			setupResetToDefaultPref();
		}
	}

	private void setupExternalPhotoCamPref(AudioVideoNotesPlugin plugin) {
		SwitchPreferenceEx externalPhotoCam = (SwitchPreferenceEx) findPreference(plugin.AV_EXTERNAL_PHOTO_CAM.getId());
		externalPhotoCam.setDescription(getString(R.string.av_use_external_camera_descr));
		externalPhotoCam.setIcon(getActiveIcon(R.drawable.ic_action_photo_dark));
	}

	private void setupCameraPictureSizePref(Camera cam, AudioVideoNotesPlugin plugin) {
		Camera.Parameters parameters = cam.getParameters();

		// Photo picture size
		// get supported sizes
		List<Camera.Size> psps = parameters.getSupportedPictureSizes();
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
			if (px > 102400) {  // 100 K
				px = px / 1048576;
				prefix = "Mpx";
			} else {
				px = px / 1024;
				prefix = "Kpx";
			}

			itemsPicSizes.add((psps.get(picSizesValues.get(index))).width
					+ "x" + (psps.get(picSizesValues.get(index))).height
					+ " ( " + String.format("%.2f", px) + " " + prefix + " )");
		}
		log.debug("onCreate() set default size: width=" + psps.get(cameraPictureSizeDefault).width + " height="
				+ psps.get(cameraPictureSizeDefault).height + " index in ps=" + cameraPictureSizeDefault);

		String[] entries = itemsPicSizes.toArray(new String[0]);
		Integer[] entryValues = picSizesValues.toArray(new Integer[0]);

		ListPreferenceEx cameraPictureSize = (ListPreferenceEx) findPreference(plugin.AV_CAMERA_PICTURE_SIZE.getId());
		if (entries.length > 0) {
			cameraPictureSize.setEntries(entries);
			cameraPictureSize.setEntryValues(entryValues);
			cameraPictureSize.setDescription(R.string.av_camera_pic_size_descr);
			cameraPictureSize.setIcon(getActiveIcon(R.drawable.ic_action_picture_size));
		} else {
			cameraPictureSize.setVisible(false);
		}
	}

	private void setupCameraFocusTypePref(Camera cam, AudioVideoNotesPlugin plugin) {
		Camera.Parameters parameters = cam.getParameters();

		// focus mode settings
		// show in menu only suppoted modes
		List<String> sfm = parameters.getSupportedFocusModes();
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

		String[] entries = items.toArray(new String[0]);
		Integer[] entryValues = itemsValues.toArray(new Integer[0]);

		ListPreferenceEx cameraFocusType = (ListPreferenceEx) findPreference(plugin.AV_CAMERA_FOCUS_TYPE.getId());
		if (entries.length > 0) {
			cameraFocusType.setEntries(entries);
			cameraFocusType.setEntryValues(entryValues);
			cameraFocusType.setDescription(R.string.av_camera_focus_descr);
			cameraFocusType.setIcon(getActiveIcon(R.drawable.ic_action_camera_focus));
		} else {
			cameraFocusType.setVisible(false);
		}
	}

	private void setupPhotoPlaySoundPref(AudioVideoNotesPlugin plugin) {
		SwitchPreferenceEx photoPlaySound = (SwitchPreferenceEx) findPreference(plugin.AV_PHOTO_PLAY_SOUND.getId());
		photoPlaySound.setDescription(getString(R.string.av_photo_play_sound_descr));
		photoPlaySound.setIcon(getContentIcon(R.drawable.ic_action_music_off));
	}

	private void setupAudioFormatPref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = new Integer[] {MediaRecorder.AudioEncoder.DEFAULT, MediaRecorder.AudioEncoder.AAC};
		String[] entries = new String[] {"Default", "AAC"};

		ListPreferenceEx audioFormat = (ListPreferenceEx) findPreference(plugin.AV_AUDIO_FORMAT.getId());
		audioFormat.setEntries(entries);
		audioFormat.setEntryValues(entryValues);
		audioFormat.setDescription(R.string.av_audio_format_descr);
	}

	private void setupAudioBitratePref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = new Integer[] {AUDIO_BITRATE_DEFAULT, 16 * 1024, 32 * 1024, 48 * 1024, 64 * 1024, 96 * 1024, 128 * 1024};
		String[] entries = new String[] {"Default", "16 kbps", "32 kbps", "48 kbps", "64 kbps", "96 kbps", "128 kbps"};

		ListPreferenceEx audioBitrate = (ListPreferenceEx) findPreference(plugin.AV_AUDIO_BITRATE.getId());
		audioBitrate.setEntries(entries);
		audioBitrate.setEntryValues(entryValues);
		audioBitrate.setDescription(R.string.av_audio_bitrate_descr);
	}

	private void setupExternalRecorderPref(AudioVideoNotesPlugin plugin) {
		SwitchPreferenceEx externalRecorder = (SwitchPreferenceEx) findPreference(plugin.AV_EXTERNAL_RECORDER.getId());
		externalRecorder.setDescription(getString(R.string.av_use_external_recorder_descr));
		externalRecorder.setIcon(getContentIcon(R.drawable.ic_action_video_dark));
	}

	private void setupVideoQualityPref(AudioVideoNotesPlugin plugin) {
		List<String> qNames = new ArrayList<>();
		List<Integer> qValues = new ArrayList<>();
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_LOW)) {
			qNames.add(getString(R.string.av_video_quality_low));
			qValues.add(CamcorderProfile.QUALITY_LOW);
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
			qNames.add("720 x 480 (480p)");
			qValues.add(CamcorderProfile.QUALITY_480P);
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
			qNames.add("1280 x 720 (720p)");
			qValues.add(CamcorderProfile.QUALITY_720P);
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
			qNames.add("1920 x 1080 (1080p)");
			qValues.add(CamcorderProfile.QUALITY_1080P);
		}
		if (Build.VERSION.SDK_INT >= 21 && CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2160P)) {
			qNames.add("3840x2160 (2160p)");
			qValues.add(CamcorderProfile.QUALITY_2160P);
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
			qNames.add(getString(R.string.av_video_quality_high));
			qValues.add(CamcorderProfile.QUALITY_HIGH);
		}

		String[] entries = qNames.toArray(new String[0]);
		Integer[] entryValues = qValues.toArray(new Integer[0]);

		ListPreferenceEx videoQuality = (ListPreferenceEx) findPreference(plugin.AV_VIDEO_QUALITY.getId());
		videoQuality.setEntries(entries);
		videoQuality.setEntryValues(entryValues);
		videoQuality.setDescription(R.string.av_video_quality_descr);
		videoQuality.setIcon(getContentIcon(R.drawable.ic_action_picture_size));
	}

	private void setupRecorderSplitPref(AudioVideoNotesPlugin plugin) {
		SwitchPreferenceEx recorderSplit = (SwitchPreferenceEx) findPreference(plugin.AV_RECORDER_SPLIT.getId());
		recorderSplit.setDescription(getString(R.string.rec_split_desc));
	}

	private void setupClipLengthPref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = new Integer[] {1, 2, 3, 4, 5, 7, 10, 15, 20, 25, 30};
		String[] entries = new String[entryValues.length];
		int i = 0;
		String minStr = getString(R.string.int_min);
		for (int v : entryValues) {
			entries[i++] = v + " " + minStr;
		}

		ListPreferenceEx clipLength = (ListPreferenceEx) findPreference(plugin.AV_RS_CLIP_LENGTH.getId());
		clipLength.setEntries(entries);
		clipLength.setEntryValues(entryValues);
		clipLength.setDescription(R.string.rec_split_clip_length_desc);
	}

	private void setupStorageSizePref(AudioVideoNotesPlugin plugin) {
		ListPreferenceEx storageSize = (ListPreferenceEx) findPreference(plugin.AV_RS_STORAGE_SIZE.getId());

		File dir = app.getAppPath("").getParentFile();
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
			String[] entries = new String[gbList.size()];
			Integer[] entryValues = new Integer[gbList.size()];
			int i = 0;
			for (int v : gbList) {
				entryValues[i] = v;
				entries[i] = AndroidUtils.formatSize(getActivity(), v * (1l << 30));
				i++;
			}

			storageSize.setEntries(entries);
			storageSize.setEntryValues(entryValues);
			storageSize.setDescription(R.string.rec_split_storage_size_desc);
		} else {
			storageSize.setVisible(false);
		}
	}

	private void setupOpenNotesDescrPref() {
		Preference nameAndPasswordPref = findPreference("open_notes_description");
		nameAndPasswordPref.setTitle(getText(R.string.multimedia_notes_view_descr));
	}

	private void setupOpenNotesPref() {
		Preference openNotes = findPreference(OPEN_NOTES);
		openNotes.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PLUGIN_SETTINGS);
		copyProfilePrefs.setIcon(getActiveIcon(R.drawable.ic_action_copy));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		resetToDefault.setIcon(getActiveIcon(R.drawable.ic_action_reset_to_default_dark));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (OPEN_NOTES.equals(preference.getKey())) {
			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getFavoritesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			app.getSettings().FAVORITES_TAB.set(NOTES_TAB);
			startActivity(favorites);
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	private Camera openCamera() {
		try {
			return Camera.open();
		} catch (Exception e) {
			log.error("Error open camera", e);
			return null;
		}
	}
}