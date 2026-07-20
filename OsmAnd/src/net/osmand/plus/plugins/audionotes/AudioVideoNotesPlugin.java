package net.osmand.plus.plugins.audionotes;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AUDIO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_PHOTO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_VIDEO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_AUDIO_VIDEO_NOTES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.RECORDING_LAYER;
import static net.osmand.IndexConstants.AV_INDEX_DIR;
import static net.osmand.shared.media.MediaFileNameFormat.IMG_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.MPEG4_EXTENSION;
import static net.osmand.shared.media.MediaFileNameFormat.THREEGP_EXTENSION;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_ON_REQUEST;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_AUDIO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_TAKE_PHOTO;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.media.AudioRecorder;
import net.osmand.plus.media.CameraRecorder;
import net.osmand.plus.media.MediaCaptureHelper;
import net.osmand.plus.media.MediaMetadataUtils;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.mediastorage.MediaStorageHelper;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;


public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final int NOTES_TAB = R.string.notes;
	public static final String DEFAULT_ACTION_SETTING_ID = "av_default_action";
	public static final String EXTERNAL_RECORDER_SETTING_ID = "av_external_recorder";
	public static final String EXTERNAL_PHOTO_CAM_SETTING_ID = "av_external_cam";

	public static final int CAMERA_FOR_VIDEO_REQUEST_CODE = 101;
	public static final int CAMERA_FOR_PHOTO_REQUEST_CODE = 102;
	public static final int AUDIO_REQUEST_CODE = 103;

	private static final Log log = PlatformUtil.getLog(AudioVideoNotesPlugin.class);

	// Constants for determining the order of items in the additional actions context menu
	private static final int TAKE_AUDIO_NOTE_ITEM_ORDER = 4100;
	private static final int TAKE_VIDEO_NOTE_ITEM_ORDER = 4300;
	private static final int TAKE_PHOTO_NOTE_ITEM_ORDER = 4500;

	public final CommonPreference<Boolean> AV_EXTERNAL_RECORDER;
	public final CommonPreference<Boolean> AV_EXTERNAL_PHOTO_CAM;

	public static final int AV_DEFAULT_ACTION_AUDIO = 0;
	public static final int AV_DEFAULT_ACTION_VIDEO = 1;
	public static final int AV_DEFAULT_ACTION_PHOTO = 2;
	public static final int AV_DEFAULT_ACTION_CHOOSE = -1;

	@IntDef({AV_DEFAULT_ACTION.AUDIO, AV_DEFAULT_ACTION.VIDEO, AV_DEFAULT_ACTION.PHOTO, AV_DEFAULT_ACTION.CHOOSE})
	@Retention(RetentionPolicy.SOURCE)
	@interface AV_DEFAULT_ACTION {
		int AUDIO = AV_DEFAULT_ACTION_AUDIO;
		int VIDEO = AV_DEFAULT_ACTION_VIDEO;
		int PHOTO = AV_DEFAULT_ACTION_PHOTO;
		int CHOOSE = AV_DEFAULT_ACTION_CHOOSE;
	}

	// photo shot:
	public static final int FULL_SCEEN_RESULT_DELAY_MS = 3000;

	public final OsmandPreference<Boolean> SHOW_RECORDINGS;

	public final CommonPreference<NotesSortByMode> NOTES_SORT_BY_MODE;

	private AudioNotesLayer audioNotesLayer;

	@Nullable
	private MapActivity mapActivity;

	private static File mediaRecFile;
	private static MediaRecorder mediaRec;
	private File lastTakingPhoto;
	private byte[] photoJpegData;
	private Timer photoTimer;
	private int requestedOrientation;

	private AudioVideoNoteRecordingMenu recordingMenu;
	private CurrentRecording currentRecording;
	private boolean recordingDone = true;
	@Nullable
	private CurrentRecording pendingAttachedRecording;

	private final AudioRecorder audioRecorder;
	private final CameraRecorder cameraRecorder;
	private final RecordingPlayer recordingPlayer;
	private final MediaCaptureHelper mediaCaptureHelper;
	private final RecordingsFileHelper recordingsFileHelper;
	private final AttachedMediaDataHelper attachedMediaDataHelper;

	private double actionLat = Double.NaN;
	private double actionLon = Double.NaN;
	private int runAction = -1;
	private List<RecordingsListener> recordingsListeners = new ArrayList<>();

	@Override
	public String getId() {
		return PLUGIN_AUDIO_VIDEO_NOTES;
	}

	public AudioVideoNotesPlugin(@NonNull OsmandApplication app) {
		super(app);
		attachedMediaDataHelper = new AttachedMediaDataHelper(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_ON_REQUEST, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_RECORD_AUDIO, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_RECORD_VIDEO, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_TAKE_PHOTO, noAppMode);

		AV_EXTERNAL_RECORDER = registerBooleanPreference(EXTERNAL_RECORDER_SETTING_ID, false);
		AV_EXTERNAL_PHOTO_CAM = registerBooleanPreference(EXTERNAL_PHOTO_CAM_SETTING_ID, true);

		mediaCaptureHelper = new MediaCaptureHelper(app, app.getAppPath(AV_INDEX_DIR));
		audioRecorder = mediaCaptureHelper.getAudioRecorder();
		cameraRecorder = mediaCaptureHelper.getCameraRecorder();

		registerPreference(audioRecorder.AV_AUDIO_FORMAT);
		registerPreference(audioRecorder.AV_AUDIO_BITRATE);
		registerPreference(audioRecorder.AV_AUDIO_SAMPLE_RATE);
		registerPreference(cameraRecorder.AV_VIDEO_FORMAT);
		registerPreference(cameraRecorder.AV_VIDEO_QUALITY);
		registerPreference(cameraRecorder.AV_CAMERA_PICTURE_SIZE);
		registerPreference(cameraRecorder.AV_CAMERA_FOCUS_TYPE);
		registerPreference(cameraRecorder.AV_PHOTO_PLAY_SOUND);

		SHOW_RECORDINGS = registerBooleanPreference("show_recordings", true);

		recordingsFileHelper = new RecordingsFileHelper(app);
		registerPreference(recordingsFileHelper.AV_RECORDER_SPLIT);
		registerPreference(recordingsFileHelper.AV_RS_CLIP_LENGTH);
		registerPreference(recordingsFileHelper.AV_RS_STORAGE_SIZE);

		NOTES_SORT_BY_MODE = registerEnumStringPreference("notes_sort_by_mode", NotesSortByMode.BY_DATE, NotesSortByMode.values(), NotesSortByMode.class);

		recordingPlayer = new RecordingPlayer(app, this::updateContextMenu);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.audionotes_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.audionotes_plugin_name);
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		cameraRecorder.loadCameraSoundIfNeeded(false);
		cameraRecorder.AV_PHOTO_PLAY_SOUND.addListener(change -> app.runInUIThread(() -> {
			cameraRecorder.loadCameraSoundIfNeeded(true);
		}));
		return true;
	}

	@NonNull
	public MediaCaptureHelper getMediaCaptureHelper() {
		return mediaCaptureHelper;
	}

	@NonNull
	public RecordingPlayer getRecordingsPlayer() {
		return recordingPlayer;
	}

	@NonNull
	public RecordingsFileHelper getRecordingsFileHelper() {
		return recordingsFileHelper;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		if (audioNotesLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(audioNotesLayer);
		}
		audioNotesLayer = new AudioNotesLayer(context, this);
		app.getOsmandMap().getMapView().addLayer(audioNotesLayer, 3.5f);
	}

	public CurrentRecording getCurrentRecording() {
		return currentRecording;
	}

	public void addRecordingsListener(@NonNull RecordingsListener listener) {
		recordingsListeners = CollectionUtils.addToList(recordingsListeners, listener);
	}

	public void removeRecordingsListener(@NonNull RecordingsListener listener) {
		recordingsListeners = CollectionUtils.removeFromList(recordingsListeners, listener);
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter,
			@NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		if (!isEnabled()) {
			return;
		}
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			int itemId = item.getTitleId();
			if (itemId == R.string.layer_recordings) {
				SHOW_RECORDINGS.set(!SHOW_RECORDINGS.get());
				item.setColor(app, SHOW_RECORDINGS.get() ?
						R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				uiAdapter.onDataSetChanged();
				updateLayers(mapActivity, mapActivity);
			}
			return true;
		};
		adapter.addItem(new ContextMenuItem(RECORDING_LAYER)
				.setTitleId(R.string.layer_recordings, app)
				.setSelected(SHOW_RECORDINGS.get())
				.setIcon(R.drawable.ic_action_micro_dark)
				.setColor(mapActivity, SHOW_RECORDINGS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setItemDeleteAction(SHOW_RECORDINGS)
				.setListener(listener));
	}

	@Override
	public void registerMapContextMenuActions(@NonNull MapActivity mapActivity, double latitude, double longitude,
			@NonNull ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
		if (!configureMenu && isRecording()) {
			return;
		}
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_AUDIO_NOTE)
				.setTitleId(R.string.recording_context_menu_arecord, app)
				.setIcon(R.drawable.ic_action_micro_dark)
				.setOrder(TAKE_AUDIO_NOTE_ITEM_ORDER)
				.setListener((uiAdapter, view, item, isChecked) -> {
					recordAudio(latitude, longitude, mapActivity);
					return true;
				}));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_VIDEO_NOTE)
				.setTitleId(R.string.recording_context_menu_vrecord, app)
				.setIcon(R.drawable.ic_action_video_dark)
				.setOrder(TAKE_VIDEO_NOTE_ITEM_ORDER)
				.setListener((uiAdapter, view, item, isChecked) -> {
					recordVideo(latitude, longitude, mapActivity, false);
					return true;
				}));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_PHOTO_NOTE)
				.setTitleId(R.string.recording_context_menu_precord, app)
				.setIcon(R.drawable.ic_action_photo_dark)
				.setOrder(TAKE_PHOTO_NOTE_ITEM_ORDER)
				.setListener((uiAdapter, view, item, isChecked) -> {
					takePhoto(latitude, longitude, mapActivity, false, false);
					return true;
				}));
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			if (SHOW_RECORDINGS.get()) {
				if (audioNotesLayer == null) {
					registerLayers(context, mapActivity);
				} else if (!mapView.getLayers().contains(audioNotesLayer)) {
					mapView.addLayer(audioNotesLayer, 3.5f);
				}
				mapView.refreshMap();
			} else if (audioNotesLayer != null) {
				mapView.removeLayer(audioNotesLayer);
				mapView.refreshMap();
			}
		} else if (audioNotesLayer != null) {
			mapView.removeLayer(audioNotesLayer);
			mapView.refreshMap();
			audioNotesLayer = null;
		}
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos,
			@NonNull ApplicationMode appMode, @Nullable ScreenLayoutMode layoutMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);

		MapWidget onRequestWidget = createMapWidgetForParams(mapActivity, AV_NOTES_ON_REQUEST);
		widgetsInfos.add(creator.createWidgetInfo(onRequestWidget));

		MapWidget audioWidget = createMapWidgetForParams(mapActivity, AV_NOTES_RECORD_AUDIO);
		widgetsInfos.add(creator.createWidgetInfo(audioWidget));

		MapWidget videoWidget = createMapWidgetForParams(mapActivity, AV_NOTES_RECORD_VIDEO);
		widgetsInfos.add(creator.createWidgetInfo(videoWidget));

		MapWidget photoWidget = createMapWidgetForParams(mapActivity, AV_NOTES_TAKE_PHOTO);
		widgetsInfos.add(creator.createWidgetInfo(photoWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity,
			@NonNull WidgetType widgetType, @Nullable String customId,
			@Nullable WidgetsPanel widgetsPanel) {
		return switch (widgetType) {
			case AV_NOTES_ON_REQUEST ->
					new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.CHOOSE, customId, widgetsPanel);
			case AV_NOTES_RECORD_AUDIO ->
					new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.AUDIO, customId, widgetsPanel);
			case AV_NOTES_RECORD_VIDEO ->
					new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.VIDEO, customId, widgetsPanel);
			case AV_NOTES_TAKE_PHOTO ->
					new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.PHOTO, customId, widgetsPanel);
			default -> null;
		};
	}

	public void makeAction(@NonNull MapActivity mapActivity, int actionId) {
		Location loc = app.getLocationProvider().getLastKnownLocation();
		if (loc == null) {
			app.showToastMessage(R.string.audionotes_location_not_defined);
			return;
		}
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		if (actionId == AV_DEFAULT_ACTION_CHOOSE) {
			chooseDefaultAction(lat, lon, mapActivity);
		} else {
			takeAction(mapActivity, lon, lat, actionId);
		}
	}

	private void chooseDefaultAction(double lat, double lon, MapActivity mapActivity) {
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		AlertDialog.Builder ab = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		ab.setItems(
				new String[] {mapActivity.getString(R.string.recording_context_menu_arecord),
						mapActivity.getString(R.string.recording_context_menu_vrecord),
						mapActivity.getString(R.string.recording_context_menu_precord),}, (dialog, which) -> {
					int action = which == 0 ? AV_DEFAULT_ACTION_AUDIO : (which == 1 ? AV_DEFAULT_ACTION_VIDEO
							: AV_DEFAULT_ACTION_PHOTO);
					takeAction(mapActivity, lon, lat, action);

				});
		ab.show();
	}

	private void takeAction(MapActivity mapActivity, double lon, double lat, int action) {
		if (action == AV_DEFAULT_ACTION_VIDEO) {
			recordVideo(lat, lon, mapActivity, isAttachedMediaRecording());
		} else if (action == AV_DEFAULT_ACTION_PHOTO) {
			takePhoto(lat, lon, mapActivity, false, isAttachedMediaRecording());
		} else if (action == AV_DEFAULT_ACTION_AUDIO) {
			recordAudio(lat, lon, mapActivity);
		}
	}

	public void captureVideoExternal(double lat, double lon, MapActivity mapActivity) {
		File file = getOutputFile(lat, lon, MPEG4_EXTENSION);
		Intent intent = mediaCaptureHelper.createVideoCaptureIntent(mapActivity, file);
		// start the video capture Intent
		if (!AndroidUtils.startActivityForResultIfSafe(mapActivity, intent, 205)) {
			cancelPendingRecordingListeners();
		}
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		ArrayList<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(TakeAudioNoteAction.TYPE);
		quickActionTypes.add(TakePhotoNoteAction.TYPE);
		quickActionTypes.add(TakeVideoNoteAction.TYPE);
		return quickActionTypes;
	}

	@Override
	public void mapActivityScreenOff(@NonNull MapActivity activity) {
		stopAndSaveRecording(activity);
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		this.mapActivity = activity;
		if (Build.VERSION.SDK_INT < 29) {
			runAction(activity);
		}
	}

	@Override
	public void mapActivityResumeOnTop(@NonNull MapActivity activity) {
		this.mapActivity = activity;
		runAction(activity);
	}

	private void runAction(MapActivity activity) {
		if (runAction != -1) {
			takeAction(activity, actionLon, actionLat, runAction);
			runAction = -1;
		}
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		if (isRecording()) {
			if (currentRecording.getType() == AVActionType.REC_PHOTO) {
				finishPhotoRecording(false);
			} else {
				activity.getContextMenu().close();

				if (currentRecording.getType() == AVActionType.REC_VIDEO
						&& !currentRecording.isAttachedMediaRecording() && recordingsFileHelper.AV_RECORDER_SPLIT.get()) {
					runAction = AV_DEFAULT_ACTION_VIDEO;
					LatLon latLon = getNextRecordingLocation();
					actionLat = latLon.getLatitude();
					actionLon = latLon.getLongitude();
				}
				stopAndSaveRecording(activity);
			}
			finishRecording();
		}
		this.mapActivity = null;
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			throw new IllegalStateException("Plugin " + this + " not attached to MapActivity.");
		}
		return mapActivity;
	}

	@Nullable
	public AVActionType getCurrentRecordingType() {
		return isRecording() ? currentRecording.getType() : null;
	}

	public boolean isRecording() {
		return currentRecording != null;
	}

	private boolean isAttachedMediaRecording() {
		return getAttachedMediaRecording() != null;
	}

	@Nullable
	private CurrentRecording getAttachedMediaRecording() {
		if (currentRecording != null && currentRecording.isAttachedMediaRecording()) {
			return currentRecording;
		}
		if (pendingAttachedRecording != null && pendingAttachedRecording.isAttachedMediaRecording()) {
			return pendingAttachedRecording;
		}
		return null;
	}

	public void recordAttachedAudio(double lat, double lon, @NonNull MapActivity activity,
	                                @NonNull File file, @NonNull CallbackWithObject<File> callback) {
		prepareAttachedMediaRecording(AVActionType.REC_AUDIO, file, callback);
		recordAudio(lat, lon, activity);
	}

	public void recordAttachedVideo(double lat, double lon, @NonNull MapActivity activity,
	                                @NonNull File file, @NonNull CallbackWithObject<File> callback) {
		prepareAttachedMediaRecording(AVActionType.REC_VIDEO, file, callback);
		recordVideo(lat, lon, activity, true);
	}

	public void takeAttachedPhoto(double lat, double lon, @NonNull MapActivity activity,
	                              @NonNull File file, @NonNull CallbackWithObject<File> callback) {
		prepareAttachedMediaRecording(AVActionType.REC_PHOTO, file, callback);
		takePhoto(lat, lon, activity, false, true);
	}

	private void prepareAttachedMediaRecording(@NonNull AVActionType type, @NonNull File file,
	                                           @NonNull CallbackWithObject<File> callback) {
		pendingAttachedRecording = new CurrentRecording(type, file, callback);
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}

	@NonNull
	private File getOutputFile(double lat, double lon, @NonNull String extension) {
		CurrentRecording attachedRecording = getAttachedMediaRecording();
		File file = attachedRecording != null ? attachedRecording.getFile() : null;
		return file != null ? file : MediaCaptureHelper.getBaseFileName(lat, lon, mediaCaptureHelper.getTargetDir(), extension);
	}

	private void initRecMenu(AVActionType actionType, double lat, double lon) {
		if (mapActivity != null) {
			if (pendingAttachedRecording != null && pendingAttachedRecording.getType() == actionType) {
				currentRecording = pendingAttachedRecording;
			} else {
				currentRecording = new CurrentRecording(actionType);
			}
			pendingAttachedRecording = null;
			if (actionType == AVActionType.REC_PHOTO) {
				recordingMenu = new AudioVideoNoteRecordingMenuFullScreen(this, lat, lon);
			} else {
				recordingMenu = new AudioVideoNoteRecordingMenu(this, lat, lon);
			}
			recordingDone = false;
			lockScreenOrientation();
		}
	}

	public void recordVideo(double lat, double lon, @NonNull MapActivity mapActivity,
			boolean forceExternal) {
		if (ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.CAMERA) == PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(mapActivity, RECORD_AUDIO) == PERMISSION_GRANTED) {
			if (AV_EXTERNAL_RECORDER.get() || forceExternal) {
				captureVideoExternal(lat, lon, mapActivity);
			} else {
				cameraRecorder.openCamera();
				if (cameraRecorder.hasCamera()) {
					initRecMenu(AVActionType.REC_VIDEO, lat, lon);
					recordVideoCamera(lat, lon, mapActivity);
				} else {
					cancelPendingRecordingListeners();
				}
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(mapActivity, new String[] {Manifest.permission.CAMERA,
					RECORD_AUDIO}, CAMERA_FOR_VIDEO_REQUEST_CODE);
		}

	}

	public void recordVideoCamera(double lat, double lon, MapActivity mapActivity) {
		CamcorderProfile p = CamcorderProfile.get(cameraRecorder.AV_VIDEO_QUALITY.get());
		Camera.Size mPreviewSize = cameraRecorder.getPreviewSize(recordingMenu.isLandscapeLayout());

		SurfaceView view;
		if (mPreviewSize != null) {
			view = recordingMenu.prepareSurfaceView(mPreviewSize.width, mPreviewSize.height);
		} else {
			view = recordingMenu.prepareSurfaceView();
		}
		view.getHolder().addCallback(new Callback() {

			@Override
			public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
			}

			@Override
			public void surfaceCreated(@NonNull SurfaceHolder holder) {

				MediaRecorder mr = new MediaRecorder();
				try {
					cameraRecorder.startCamera(mPreviewSize, holder, CameraRecorder.getDisplayRotation(mapActivity));

					cameraRecorder.unlockCamera();
					mr.setCamera(cameraRecorder.getCamera());

				} catch (Exception e) {
					logErr(e);
					closeRecordingMenu();
					cameraRecorder.closeCamera();
					cancelPendingRecordingListeners();
					finishRecording();
					return;
				}

				File f = getOutputFile(lat, lon, MPEG4_EXTENSION);
				cameraRecorder.configureVideoRecorder(mr, p, f, CameraRecorder.getDisplayRotation(mapActivity));
				try {
					MediaMetadataUtils.setMediaRecorderLocation(mr, lat, lon);
					if (!isAttachedMediaRecording() && recordingsFileHelper.AV_RECORDER_SPLIT.get()) {
						cleanupRecordingSpace(p);
					}
					runMediaRecorder(mapActivity, mr, f);
				} catch (Exception e) {
					cancelPendingRecordingListeners();
					logErr(e);
				}
			}

			@Override
			public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
			}
		});
		recordingMenu.show();
	}

	@Nullable
	public AudioVideoNoteRecordingMenu getRecordingMenu() {
		return recordingMenu;
	}

	private int getRotation() {
		return CameraRecorder.getDisplayRotation(requireMapActivity());
	}

	private void logErr(Exception e) {
		log.error("Error starting recorder ", e);
		app.showToastMessage(app.getString(R.string.recording_error) + " : " + e.getMessage());
	}

	private void lockScreenOrientation() {
		requestedOrientation = mapActivity.getRequestedOrientation();
		mapActivity.setRequestedOrientation(AndroidUiHelper.getScreenOrientation(mapActivity));
	}

	private void restoreScreenOrientation() {
		mapActivity.setRequestedOrientation(requestedOrientation);
	}

	protected void stopCamera() {
		cameraRecorder.stopCamera();
	}

	private void stopMediaRecording(boolean save, boolean notifyOnSaved) {
		AVActionType type = getCurrentRecordingType();
		if (type == null || type.isAudio()) {
			audioRecorder.unmuteStreamMusicAndOutputGuidance();
		}
		if (mediaRec != null) {
			try {
				mediaRec.stop();
			} catch (RuntimeException e) {
				log.error(e.getMessage(), e);
			}
			if (save) {
				if (isAttachedMediaRecording()) {
					notifyAttachedMediaCaptured(mediaRecFile);
				} else {
					indexRecordingFile(mediaRecFile, notifyOnSaved);
				}
			} else {
				cancelPendingRecordingListeners();
				finishRecording();
			}
			mediaRec.release();
			mediaRec = null;
			mediaRecFile = null;
		}
	}

	public void recordAudio(double lat, double lon, @NonNull MapActivity activity) {
		if (ActivityCompat.checkSelfPermission(activity, RECORD_AUDIO) == PERMISSION_GRANTED) {
			initRecMenu(AVActionType.REC_AUDIO, lat, lon);
			try {
				audioRecorder.muteStreamMusicAndOutputGuidance();

				File file = getOutputFile(lat, lon, THREEGP_EXTENSION);
				MediaRecorder recorder = audioRecorder.createRecorder(file);
				MediaMetadataUtils.setMediaRecorderLocation(recorder, lat, lon);
				runMediaRecorder(activity, recorder, file);
			} catch (Exception e) {
				cancelPendingRecordingListeners();
				audioRecorder.unmuteStreamMusicAndOutputGuidance();
				log.error("Error starting audio recorder ", e);
				app.showToastMessage(app.getString(R.string.recording_error) + " : " + e.getMessage());
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(activity, new String[] {RECORD_AUDIO}, AUDIO_REQUEST_CODE);
		}
	}

	public void takePhoto(double lat, double lon, @NonNull MapActivity activity,
			boolean forceInternal, boolean forceExternal) {
		if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
			if ((!AV_EXTERNAL_PHOTO_CAM.get() || forceInternal) && !forceExternal) {
				takePhotoInternalOrExternal(lat, lon, activity);
			} else {
				takePhotoExternal(lat, lon, activity);
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.CAMERA}, CAMERA_FOR_PHOTO_REQUEST_CODE);
		}
	}

	private void takePhotoInternalOrExternal(double lat, double lon, MapActivity mapActivity) {
		cameraRecorder.openCamera();
		if (cameraRecorder.hasCamera()) {
			initRecMenu(AVActionType.REC_PHOTO, lat, lon);
			takePhotoWithCamera(lat, lon, mapActivity);
		} else {
			takePhotoExternal(lat, lon, mapActivity);
		}
	}

	private void takePhotoWithCamera(double lat, double lon, MapActivity mapActivity) {
		try {
			lastTakingPhoto = getOutputFile(lat, lon, IMG_EXTENSION);
			Parameters parameters = cameraRecorder.getCamera().getParameters();
			Camera.Size selectedCamPicSize = cameraRecorder.resolvePictureSize(parameters);
			Camera.Size mPreviewSize = cameraRecorder.getOptimalPreviewSize(selectedCamPicSize.width, selectedCamPicSize.height);
			SurfaceView view;
			if (mPreviewSize != null) {
				view = recordingMenu.prepareSurfaceView(mPreviewSize.width, mPreviewSize.height);
			} else {
				view = recordingMenu.prepareSurfaceView();
			}
			view.getHolder().addCallback(new Callback() {

				@Override
				public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
				}

				@Override
				public void surfaceCreated(@NonNull SurfaceHolder holder) {
					try {
						Parameters params = cameraRecorder.getCamera().getParameters();
						cameraRecorder.applyPhotoParameters(params, selectedCamPicSize, mPreviewSize, holder, CameraRecorder.getDisplayRotation(mapActivity));
						internalShoot();

					} catch (Exception e) {
						logErr(e);
						closeRecordingMenu();
						cameraRecorder.closeCamera();
						cancelPendingRecordingListeners();
						finishRecording();
					}
				}

				@Override
				public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
				}
			});
			recordingMenu.show();
		} catch (RuntimeException e) {
			logErr(e);
			cameraRecorder.closeCamera();
			cancelPendingRecordingListeners();
		}
	}

	private void internalShoot() {
		app.runInUIThread(() -> {
			if (cameraRecorder.hasCamera()) {
				cameraRecorder.applyShutterSoundSetting();
				if (cameraRecorder.isAutofocus()) {
					takePictureOnAutofocus();
				} else {
					takePicture();
				}
			}
		}, 200);
	}

	public void shoot() {
		if (!recordingDone) {
			recordingDone = true;
			if (cameraRecorder.hasCamera() && lastTakingPhoto != null) {
				try {
					takePicture();
				} catch (RuntimeException e) {
					closeRecordingMenu();
					cameraRecorder.closeCamera();
					cancelPendingRecordingListeners();
					finishRecording();
				}
			}
		}
	}

	public void takePictureOnAutofocus() {
		cameraRecorder.autoFocus((success, camera) -> {
			try {
				takePicture();
			} catch (Exception e) {
				logErr(e);
				closeRecordingMenu();
				cameraRecorder.closeCamera();
				cancelPendingRecordingListeners();
				finishRecording();
			}
		});
	}

	public void takePicture() {
		cameraRecorder.takePicture(new JpegPhotoHandler());
	}

	public void takePhotoExternal(double lat, double lon, MapActivity mapActivity) {
		actionLat = lat;
		actionLon = lon;
		File f = getOutputFile(lat, lon, IMG_EXTENSION);
		lastTakingPhoto = f;
		Intent takePictureIntent = mediaCaptureHelper.createPhotoCaptureIntent(mapActivity, f);
		try {
			if (!AndroidUtils.startActivityForResultIfSafe(mapActivity, takePictureIntent, 205)) {
				cancelPendingRecordingListeners();
			}
		} catch (Exception e) {
			cancelPendingRecordingListeners();
			log.error("Error taking a picture ", e);
			app.showToastMessage(app.getString(R.string.recording_error) + " : " + e.getMessage());
		}
	}

	private void runMediaRecorder(MapActivity mapActivity, MediaRecorder recorder, File file) throws IOException {
		recorder.prepare();
		recorder.start();
		mediaRec = recorder;
		mediaRecFile = file;

		recordingMenu.show();
		mapActivity.refreshMap();
	}

	public boolean restartRecording(@NonNull MapActivity mapActivity) {
		if (isAttachedMediaRecording()) {
			return false;
		}
		AVActionType type = getCurrentRecordingType();

		// Stop media recording and save previously recorded data
		stopMediaRecording(true, false);

		boolean success = false;
		if (type != null) {
			success = tryRestartRecording(type);
		}
		if (!success) {
			// Finish recording if it's not possible to restart
			stopRecordingWithoutSaving(mapActivity);
		}
		return success;
	}

	private boolean tryRestartRecording(@NonNull AVActionType type) {
		try {
			cameraRecorder.lockCamera();
			CamcorderProfile profile = CamcorderProfile.get(cameraRecorder.AV_VIDEO_QUALITY.get());
			if (recordingsFileHelper.AV_RECORDER_SPLIT.get()) {
				cleanupRecordingSpace(profile);
			}

			currentRecording = new CurrentRecording(type);
			MediaRecorder recorder = new MediaRecorder();
			LatLon latLon = getNextRecordingLocation();
			File f = mediaCaptureHelper.createVideoFile(latLon.getLatitude(), latLon.getLongitude());

			cameraRecorder.unlockCamera();
			recorder.setCamera(cameraRecorder.getCamera());
			cameraRecorder.configureVideoRecorder(recorder, profile, f, getRotation());
			MediaMetadataUtils.setMediaRecorderLocation(recorder, latLon.getLatitude(), latLon.getLongitude());
			recorder.prepare();
			recorder.start();
			mediaRec = recorder;
			mediaRecFile = f;

		} catch (Exception e) {
			app.showToastMessage(e.getMessage());
			log.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	private void cleanupRecordingSpace(@NonNull CamcorderProfile profile) {
		if (recordingsFileHelper.cleanupSpace(profile)) {
			app.runInUIThread(() -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.refreshMap();
				}
			}, 20);
		}
	}

	public void stopRecordingWithoutSaving(@NonNull MapActivity mapActivity) {
		stopRecording(mapActivity, false, false);
	}

	public void stopAndSaveRecording(@NonNull MapActivity mapActivity) {
		stopRecording(mapActivity, true, true);
	}

	public void stopRecording(@NonNull MapActivity mapActivity, boolean save, boolean notifyOnSaved) {
		if (!recordingDone) {
			boolean attachedMediaRecording = isAttachedMediaRecording();
			recordingDone = true;
			stopMediaRecording(save, notifyOnSaved);
			if (save && !attachedMediaRecording) {
				SHOW_RECORDINGS.set(true);
				mapActivity.refreshMap();
			}
			closeRecordingMenu();
		}
	}

	private LatLon getNextRecordingLocation() {
		double lat = mapActivity.getMapLocation().getLatitude();
		double lon = mapActivity.getMapLocation().getLongitude();
		Location loc = app.getLocationProvider().getLastKnownLocation();
		if (loc != null) {
			lat = loc.getLatitude();
			lon = loc.getLongitude();
		}
		return new LatLon(lat, lon);
	}

	private void updateContextMenu(@Nullable Recording rec) {
		if (mapActivity != null && rec != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			menu.show(new LatLon(rec.getLatitude(), rec.getLongitude()), audioNotesLayer.getObjectName(rec), rec);
			if (app.getRoutingHelper().isFollowingMode()) {
				menu.hideWithTimeout(3000);
			}
		}
	}

	private void finishRecording() {
		currentRecording = null;
	}

	@NonNull
	public AudioNotesLayer getAudioNotesLayer() {
		return audioNotesLayer;
	}

	@Override
	public void addMyPlacesTab(MyPlacesActivity myPlacesActivity, List<TabItem> mTabs, Intent intent) {
		mTabs.add(myPlacesActivity.getTabIndicator(NOTES_TAB, NotesFragment.class));
		if (intent != null && "AUDIO".equals(intent.getStringExtra("TAB"))) {
			app.getSettings().FAVORITES_TAB.set(NOTES_TAB);
		}
	}

	private void notifyRecordingSaved(@NonNull Recording recording, boolean updateMenu) {
		app.runInUIThread(() -> {
			boolean handled = notifyRecordingsAdded(Collections.singletonList(recording));
			if (updateMenu && !handled) {
				app.runInUIThread(() -> updateContextMenu(recording), 200);
			}
		});
	}

	private boolean notifyRecordingsAdded(@NonNull List<Recording> recordings) {
		if (recordings.isEmpty()) {
			return false;
		}
		attachedMediaDataHelper.convertRecordingsToFavorites(recordings);

		boolean handled = false;
		for (RecordingsListener listener : recordingsListeners) {
			handled |= listener.onRecordingsAdded(recordings);
		}
		return handled;
	}

	private void cancelPendingRecordingListeners() {
		List<RecordingsListener> listeners = recordingsListeners;
		recordingsListeners = new ArrayList<>();
		for (RecordingsListener listener : listeners) {
			listener.onRecordingsCancelled();
		}
		cancelAttachedMediaRecording();
	}

	private void notifyAttachedMediaCaptured(@Nullable File file) {
		CurrentRecording attachedRecording = getAttachedMediaRecording();
		clearAttachedMediaRecording();
		finishRecording();
		CallbackWithObject<File> callback = attachedRecording != null ? attachedRecording.getResultCallback() : null;
		if (callback != null && file != null && file.exists() && file.length() > 0) {
			if (attachedRecording.getType() == AVActionType.REC_PHOTO) {
				updateAttachedPhotoInformation(file);
			}
			callback.processResult(file);
		} else if (callback != null) {
			callback.processResult(null);
		}
	}

	private void updateAttachedPhotoInformation(@NonNull File file) {
		double lat = recordingMenu != null ? recordingMenu.lat : actionLat;
		double lon = recordingMenu != null ? recordingMenu.lon : actionLon;
		if (!MapUtils.isValidLatLon(lat, lon)) {
			return;
		}
		Float heading = app.getLocationProvider().getHeading();
		MediaMetadataUtils.updatePhotoInformation(file, lat, lon, null, heading != null && heading != 0 ? heading : Double.NaN);
	}

	private void cancelAttachedMediaRecording() {
		CurrentRecording attachedRecording = getAttachedMediaRecording();
		clearAttachedMediaRecording();
		CallbackWithObject<File> callback = attachedRecording != null ? attachedRecording.getResultCallback() : null;
		if (callback != null) {
			finishRecording();
			callback.processResult(null);
		}
	}

	private void clearAttachedMediaRecording() {
		pendingAttachedRecording = null;
	}

	private void indexRecordingFile(@NonNull File file, boolean notifyOnSaved) {
		boolean updatePhotoInformation = lastTakingPhoto != null && lastTakingPhoto.getName().equals(file.getName());
		boolean newFileIndexed = recordingsFileHelper.indexFile(true, file, updatePhotoInformation);
		if (updatePhotoInformation) {
			lastTakingPhoto = null;
		}
		Recording recording = newFileIndexed ? recordingsFileHelper.getRecordingByFileName().get(file.getName()) : null;
		if (recording == null) {
			return;
		}
		if (recording.getFile().length() == 0) {
			if (notifyOnSaved) {
				cancelPendingRecordingListeners();
			}
			if (isRecording()) {
				finishRecording();
			}
			return;
		}
		new MediaStorageHelper(app).scanMediaFile(file);
		boolean updateMenu = false;
		if (isRecording()) {
			AVActionType type = currentRecording.getType();
			finishRecording();
			updateMenu = !type.isAudio() && (!recordingsFileHelper.AV_RECORDER_SPLIT.get() || !type.isVideo());
		}
		if (notifyOnSaved) {
			notifyRecordingSaved(recording, updateMenu);
		} else {
			notifyRecordingsAdded(Collections.singletonList(recording));
		}
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		cameraRecorder.releaseSound();
	}

	@Override
	public List<String> indexingFiles(@Nullable IProgress progress) {
		return indexingFiles(true, false);
	}

	@Nullable
	public List<String> indexingFiles(boolean reIndexAndKeepOld, boolean registerNew) {
		Set<String> indexedFiles = registerNew ? getIndexedRecordingFileNames() : null;
		List<String> res = recordingsFileHelper.indexingFiles(reIndexAndKeepOld, registerNew);
		if (indexedFiles != null) {
			notifyRecordingsAdded(getNewRecordings(indexedFiles));
		}
		return res;
	}

	@NonNull
	public DataTileManager<Recording> getRecordings() {
		return recordingsFileHelper.getRecordings();
	}

	public void deleteRecording(@NonNull Recording recording, boolean updateUI) {
		recordingsFileHelper.deleteRecording(recording);

		if (mapActivity != null && updateUI) {
			if (mapActivity.getContextMenu().getObject() == recording) {
				mapActivity.getContextMenu().close();
			}
			mapActivity.refreshMap();
		}
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.MULTIMEDIA_NOTES;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.multimedia_notes_prefs_descr);
	}

	@Override
	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 205 || requestCode == 105) {
			CurrentRecording attachedRecording = getAttachedMediaRecording();
			if (attachedRecording != null) {
				notifyAttachedMediaCaptured(attachedRecording.getFile());
			} else if (lastTakingPhoto != null && lastTakingPhoto.exists()) {
				indexRecordingFile(lastTakingPhoto, true);
			} else {
				Set<String> indexedFiles = getIndexedRecordingFileNames();
				recordingsFileHelper.indexingFiles(true, true);
				Recording recording = recordingsFileHelper.getNewRecording(indexedFiles);
				if (recording == null || recording.getFile().length() == 0) {
					cancelPendingRecordingListeners();
				} else {
					notifyRecordingsAdded(Collections.singletonList(recording));
				}
			}
			lastTakingPhoto = null;
		}
	}

	public Collection<Recording> getAllRecordings() {
		return recordingsFileHelper.getRecordingByFileName().values();
	}

	@NonNull
	private Set<String> getIndexedRecordingFileNames() {
		return new HashSet<>(recordingsFileHelper.getRecordingByFileName().keySet());
	}

	private List<Recording> getNewRecordings(@NonNull Set<String> indexedFiles) {
		List<Recording> newRecordings = new ArrayList<>();
		for (Recording recording : recordingsFileHelper.getRecordingByFileName().values()) {
			if (!indexedFiles.contains(recording.getFileName()) && recording.getFile().length() > 0) {
				newRecordings.add(recording);
			}
		}
		return newRecordings;
	}

	public interface RecordingsListener {
		boolean onRecordingsAdded(@NonNull List<Recording> recordings);

		default void onRecordingsCancelled() {
		}
	}

	private void updateContextMenu() {
		app.runInUIThread(() -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getContextMenu().updateMenuUI();
			}
		});
	}

	private void closeRecordingMenu() {
		if (mapActivity != null) {
			mapActivity.runOnUiThread(() -> {
				if (recordingMenu != null) {
					recordingMenu.hide();
					recordingMenu = null;
				}
				restoreScreenOrientation();
			});
		}
	}

	@Override
	public void addCommonKeyEventAssignments(@NonNull List<KeyAssignment> assignments) {
		assignments.add(new KeyAssignment(TakeMediaNoteCommand.ID, KeyEvent.KEYCODE_CAMERA));
	}

	@Override
	public KeyEventCommand createKeyEventCommand(@NonNull String commandId) {
		if (commandId.equals(TakeMediaNoteCommand.ID)) {
			return new TakeMediaNoteCommand();
		}
		return null;
	}

	@Override
	public void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		runAction = -1;
		if (requestCode == CAMERA_FOR_VIDEO_REQUEST_CODE) {
			if (isPermissionGranted(grantResults, 0) && isPermissionGranted(grantResults, 1)) {
				runAction = AV_DEFAULT_ACTION_VIDEO;
			} else {
				cancelPendingRecordingListeners();
				app.showToastMessage(R.string.no_camera_permission);
			}
		} else if (requestCode == CAMERA_FOR_PHOTO_REQUEST_CODE) {
			if (isPermissionGranted(grantResults, 0)) {
				runAction = AV_DEFAULT_ACTION_PHOTO;
			} else {
				cancelPendingRecordingListeners();
				app.showToastMessage(R.string.no_camera_permission);
			}
		} else if (requestCode == AUDIO_REQUEST_CODE) {
			if (isPermissionGranted(grantResults, 0)) {
				runAction = AV_DEFAULT_ACTION_AUDIO;
			} else {
				cancelPendingRecordingListeners();
				app.showToastMessage(R.string.no_microphone_permission);
			}
		}
	}

	private boolean isPermissionGranted(@NonNull int[] grantResults, int index) {
		return grantResults.length > index && grantResults[index] == PERMISSION_GRANTED;
	}

	public class JpegPhotoHandler implements PictureCallback {

		public JpegPhotoHandler() {
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			photoJpegData = data;
			cameraRecorder.playShutterSound();
			if (recordingMenu != null) {
				recordingMenu.showFinalPhoto(data, FULL_SCEEN_RESULT_DELAY_MS);
			}
			startPhotoTimer();
		}
	}

	private void startPhotoTimer() {
		if (photoTimer != null) {
			cancelPhotoTimer();
		}
		photoTimer = new Timer();
		photoTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				finishPhotoRecording(false);
			}
		}, FULL_SCEEN_RESULT_DELAY_MS);
	}

	private void cancelPhotoTimer() {
		if (photoTimer != null) {
			photoTimer.cancel();
			photoTimer = null;
		}
	}

	public synchronized void shootAgain() {
		cancelPhotoTimer();
		photoJpegData = null;
		if (cameraRecorder.hasCamera()) {
			try {
				cameraRecorder.cancelAutoFocus();
				cameraRecorder.stopPreview();
				if (recordingMenu != null) {
					recordingMenu.hideFinalPhoto();
				}
				cameraRecorder.startPreview();
				internalShoot();

			} catch (Exception e) {
				logErr(e);
				closeRecordingMenu();
				cameraRecorder.closeCamera();
				cancelPendingRecordingListeners();
				finishRecording();
			}
		}
	}

	public synchronized void finishPhotoRecording(boolean cancel) {
		cancelPhotoTimer();
		if (photoJpegData != null && photoJpegData.length > 0 && lastTakingPhoto != null) {
			boolean attachedMediaRecording = isAttachedMediaRecording();
			try {
				if (!cancel) {
					FileOutputStream fos = new FileOutputStream(lastTakingPhoto);
					fos.write(photoJpegData);
					fos.close();
					if (attachedMediaRecording) {
						notifyAttachedMediaCaptured(lastTakingPhoto);
					} else {
						indexRecordingFile(lastTakingPhoto, true);
					}
				}
			} catch (Exception error) {
				cancelPendingRecordingListeners();
				logErr(error);
			} finally {
				photoJpegData = null;
				closeRecordingMenu();
				if (!cancel && !attachedMediaRecording) {
					finishRecording();
				}
			}
		} else {
			cancelPendingRecordingListeners();
			if (cancel) {
				closeRecordingMenu();
			}
		}
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_micro_dark;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.audio_video_notes);
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashAudioVideoNotesFragment.FRAGMENT_DATA;
	}
}
