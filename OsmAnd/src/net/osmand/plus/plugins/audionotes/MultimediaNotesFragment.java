package net.osmand.plus.plugins.audionotes;

import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.plugins.PluginInfoFragment.PLUGIN_INFO;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class MultimediaNotesFragment extends BaseSettingsFragment implements CopyAppModePrefsListener, ResetAppModePrefsListener {

	public static final int CAMERA_FOR_PHOTO_PARAMS_REQUEST_CODE = 104;

	private static final Log log = PlatformUtil.getLog(MultimediaNotesFragment.class);

	private static final String OPEN_NOTES_DESCRIPTION = "open_notes_description";
	private static final String CAMERA_PERMISSION = "camera_permission";
	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String OPEN_NOTES = "open_notes";

	boolean showSwitchProfile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			showSwitchProfile = args.getBoolean(PLUGIN_INFO, false);
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, showSwitchProfile);
		}
	}

	@Override
	public Bundle buildArguments() {
		Bundle args = super.buildArguments();
		args.putBoolean(PLUGIN_INFO, showSwitchProfile);
		return args;
	}

	@Override
	protected void setupPreferences() {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			setupCameraPhotoPrefs(plugin);

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

	private void setupCameraPhotoPrefs(AudioVideoNotesPlugin plugin) {
		Camera cam = openCamera();
		setupCameraPermissionPref(cam);
		setupExternalPhotoCamPref(cam, plugin);
		setupCameraPictureSizePref(cam, plugin);
		setupCameraFocusTypePref(cam, plugin);
		setupPhotoPlaySoundPref(cam, plugin);
		if (cam != null) {
			cam.release();
		}
	}

	private void setupCameraPermissionPref(Camera cam) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference cameraPermission = findPreference(CAMERA_PERMISSION);
		boolean permissionGranted = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
		cameraPermission.setVisible(cam == null && !permissionGranted);
	}

	private void setupExternalPhotoCamPref(Camera cam, AudioVideoNotesPlugin plugin) {
		ApplicationMode appMode = getSelectedAppMode();
		boolean useSystemCameraApp = plugin.AV_EXTERNAL_PHOTO_CAM.getModeValue(appMode);
		Preference uiPreference = findPreference(EXTERNAL_PHOTO_CAM_SETTING_ID);
		uiPreference.setIcon(getActiveIcon(R.drawable.ic_action_photo_dark));
		uiPreference.setSummary(getCameraAppTitle(useSystemCameraApp));
		uiPreference.setEnabled(cam != null);
	}

	private void setupCameraPictureSizePref(Camera cam, AudioVideoNotesPlugin plugin) {
		ListPreferenceEx cameraPictureSize = findPreference(plugin.AV_CAMERA_PICTURE_SIZE.getId());
		cameraPictureSize.setDescription(R.string.av_camera_pic_size_descr);
		cameraPictureSize.setIcon(getPersistentPrefIcon(R.drawable.ic_action_picture_size));

		if (cam == null) {
			cameraPictureSize.setEnabled(false);
			return;
		}
		Camera.Parameters parameters = cam.getParameters();

		// Photo picture size
		// get supported sizes
		List<Camera.Size> psps = parameters.getSupportedPictureSizes();
		if (psps == null) {
			cameraPictureSize.setVisible(false);
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
		// sort list for max resolution in beginning of list
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

		if (entries.length > 0) {
			cameraPictureSize.setEntries(entries);
			cameraPictureSize.setEntryValues(entryValues);
			if((Integer) cameraPictureSize.getValue() == AV_PHOTO_SIZE_DEFAULT){
				cameraPictureSize.setValueIndex(0);
				updatePreference(cameraPictureSize);
			}
		} else {
			cameraPictureSize.setVisible(false);
		}
	}

	private void setupCameraFocusTypePref(Camera cam, AudioVideoNotesPlugin plugin) {
		ListPreferenceEx cameraFocusType = findPreference(plugin.AV_CAMERA_FOCUS_TYPE.getId());
		cameraFocusType.setDescription(R.string.av_camera_focus_descr);
		cameraFocusType.setIcon(getPersistentPrefIcon(R.drawable.ic_action_camera_focus));

		if (cam == null) {
			cameraFocusType.setEnabled(false);
			return;
		}

		Camera.Parameters parameters = cam.getParameters();

		// focus mode settings
		// show in menu only supported modes
		List<String> sfm = parameters.getSupportedFocusModes();
		if (sfm == null) {
			cameraFocusType.setVisible(false);
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

		String[] entries = items.toArray(new String[0]);
		Integer[] entryValues = itemsValues.toArray(new Integer[0]);

		if (entries.length > 0) {
			cameraFocusType.setEntries(entries);
			cameraFocusType.setEntryValues(entryValues);
		} else {
			cameraFocusType.setVisible(false);
		}
	}

	private void setupPhotoPlaySoundPref(Camera cam, AudioVideoNotesPlugin plugin) {
		Drawable disabled = getContentIcon(R.drawable.ic_action_music_off);
		Drawable enabled = getActiveIcon(R.drawable.ic_type_audio);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceEx uiPreference = findPreference(plugin.AV_PHOTO_PLAY_SOUND.getId());
		uiPreference.setDescription(getString(R.string.av_photo_play_sound_descr));
		uiPreference.setIcon(icon);
		uiPreference.setEnabled(cam != null);

		ApplicationMode appMode = getSelectedAppMode();
		boolean useOsmAndCamera = !plugin.AV_EXTERNAL_PHOTO_CAM.getModeValue(appMode);
		boolean shouldShowPreference = useOsmAndCamera && canDisableShutterSound();
		uiPreference.setVisible(shouldShowPreference);
	}

	private void setupAudioFormatPref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = {MediaRecorder.AudioEncoder.DEFAULT, MediaRecorder.AudioEncoder.AAC};
		String[] entries = {getString(R.string.shared_string_default), "AAC"};

		ListPreferenceEx audioFormat = findPreference(plugin.AV_AUDIO_FORMAT.getId());
		audioFormat.setEntries(entries);
		audioFormat.setEntryValues(entryValues);
		audioFormat.setDescription(R.string.av_audio_format_bottom_sheet_descr);
	}

	private void setupAudioBitratePref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = {16 * 1024, 32 * 1024, 48 * 1024, 64 * 1024, 96 * 1024, 128 * 1024};
		String[] entries = {"16 kbps", "32 kbps", "48 kbps", "64 kbps", "96 kbps", "128 kbps"};

		ListPreferenceEx audioBitrate = findPreference(plugin.AV_AUDIO_BITRATE.getId());
		audioBitrate.setEntries(entries);
		audioBitrate.setEntryValues(entryValues);
		audioBitrate.setDescription(R.string.av_audio_bitrate_descr);
	}

	private void setupExternalRecorderPref(AudioVideoNotesPlugin plugin) {
		ApplicationMode appMode = getSelectedAppMode();
		boolean useSystemCamera = plugin.AV_EXTERNAL_RECORDER.getModeValue(appMode);
		Preference uiPreference = findPreference(EXTERNAL_RECORDER_SETTING_ID);
		uiPreference.setIcon(getActiveIcon(R.drawable.ic_action_video_dark));
		uiPreference.setSummary(getCameraAppTitle(useSystemCamera));
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

		ListPreferenceEx videoQuality = findPreference(plugin.AV_VIDEO_QUALITY.getId());
		videoQuality.setEntries(entries);
		videoQuality.setEntryValues(entryValues);
		videoQuality.setDescription(R.string.av_video_quality_descr);
		videoQuality.setIcon(getActiveIcon(R.drawable.ic_action_picture_size));
	}

	private void setupRecorderSplitPref(AudioVideoNotesPlugin plugin) {
		SwitchPreferenceEx recorderSplit = findPreference(plugin.AV_RECORDER_SPLIT.getId());
		recorderSplit.setDescription(getString(R.string.rec_split_desc));
	}

	private void setupClipLengthPref(AudioVideoNotesPlugin plugin) {
		Integer[] entryValues = {1, 2, 3, 4, 5, 7, 10, 15, 20, 25, 30};
		String[] entries = new String[entryValues.length];
		int i = 0;
		String minStr = getString(R.string.int_min);
		for (int v : entryValues) {
			entries[i++] = v + " " + minStr;
		}

		ListPreferenceEx clipLength = findPreference(plugin.AV_RS_CLIP_LENGTH.getId());
		clipLength.setEntries(entries);
		clipLength.setEntryValues(entryValues);
		clipLength.setDescription(R.string.rec_split_clip_length_desc);
	}

	private void setupStorageSizePref(AudioVideoNotesPlugin plugin) {
		ListPreferenceEx storageSize = findPreference(plugin.AV_RS_STORAGE_SIZE.getId());

		long size = AndroidUtils.getTotalSpace(app) / (1 << 30);
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
			storageSize.setIcon(getActiveIcon(R.drawable.ic_sdcard));
		} else {
			storageSize.setVisible(false);
		}
	}

	private void setupOpenNotesDescrPref() {
		String menu = getString(R.string.shared_string_menu);
		String myPlaces = getString(R.string.shared_string_my_places);
		String notes = getString(R.string.notes);
		String multimediaNotesPath = getString(R.string.ltr_or_rtl_triple_combine_via_dash, menu, myPlaces, notes);
		String multimediaNotesPathDescr = getString(R.string.multimedia_notes_view_descr, multimediaNotesPath);

		Preference osmEditsDescription = findPreference(OPEN_NOTES_DESCRIPTION);
		int startIndex = multimediaNotesPathDescr.indexOf(multimediaNotesPath);
		if (startIndex != -1) {
			SpannableString titleSpan = new SpannableString(multimediaNotesPathDescr);
			titleSpan.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, startIndex + multimediaNotesPath.length(), 0);
			osmEditsDescription.setTitle(titleSpan);
		} else {
			osmEditsDescription.setTitle(multimediaNotesPathDescr);
		}
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
		String prefId = preference.getKey();
		if (OPEN_NOTES.equals(prefId)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, NOTES_TAB);

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getMyPlacesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			favorites.putExtra(MapActivity.INTENT_PARAMS, bundle);
			startActivity(favorites);
			return true;
		} else if (COPY_PLUGIN_SETTINGS.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, getSelectedAppMode(), this);
			}
		} else if (CAMERA_PERMISSION.equals(prefId)) {
			requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_FOR_PHOTO_PARAMS_REQUEST_CODE);
		} else if (EXTERNAL_RECORDER_SETTING_ID.equals(prefId)) {
			AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
			if (plugin != null) {
				showSelectCameraAppDialog(plugin.AV_EXTERNAL_RECORDER);
			}
			return true;
		} else if (EXTERNAL_PHOTO_CAM_SETTING_ID.equals(prefId)) {
			AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
			if (plugin != null) {
				showSelectCameraAppDialog(plugin.AV_EXTERNAL_PHOTO_CAM);
			}
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	private void showSelectCameraAppDialog(@NonNull CommonPreference<Boolean> preference) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		boolean nightMode = isNightMode();
		ApplicationMode appMode = getSelectedAppMode();
		int profileColor = appMode.getProfileColor(nightMode);

		int selected = preference.getModeValue(appMode) ? 0 : 1;
		String[] entries = new String[] {
				getCameraAppTitle(true),
				getCameraAppTitle(false)
		};

		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(getString(R.string.camera_app))
				.setControlsColor(profileColor);

		CustomAlert.showSingleSelection(dialogData, entries, selected, v -> {
			boolean useSystemApp = (int) v.getTag() == 0;
			onConfirmPreferenceChange(preference.getId(), useSystemApp, ApplyQueryType.SNACK_BAR);
		});
	}

	@NonNull
	private String getCameraAppTitle(boolean useSystemApp) {
		return getString(useSystemApp ? R.string.system_locale : R.string.app_name_osmand);
	}

	private Camera openCamera() {
		try {
			return Camera.open();
		} catch (Exception e) {
			log.error("Error open camera", e);
			return null;
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String prefId = preference.getKey();
		if (CAMERA_PERMISSION.equals(prefId)) {
			setupPrefRoundedBg(holder);
		} else if (OPEN_NOTES_DESCRIPTION.equals(prefId)) {
			int minHeight = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height);
			holder.itemView.setMinimumHeight(minHeight);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == CAMERA_FOR_PHOTO_PARAMS_REQUEST_CODE && grantResults.length > 0
				&& permissions.length > 0 && Manifest.permission.CAMERA.equals(permissions[0])) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				updateAllSettings();
			} else {
				app.showToastMessage(R.string.no_camera_permission);
			}
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			app.getSettings().copyProfilePreferences(appMode, getSelectedAppMode(), plugin.getPreferences());
			updateAllSettings();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			app.getSettings().resetProfilePreferences(appMode, plugin.getPreferences());
			app.showToastMessage(R.string.plugin_prefs_reset_successful);
			updateAllSettings();
		}
	}
}