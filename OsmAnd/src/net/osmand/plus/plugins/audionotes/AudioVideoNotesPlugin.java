package net.osmand.plus.plugins.audionotes;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_AUDIO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_PHOTO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_VIDEO_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_AUDIO_VIDEO_NOTES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.RECORDING_LAYER;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_ON_REQUEST;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_AUDIO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_RECORD_VIDEO;
import static net.osmand.plus.views.mapwidgets.WidgetType.AV_NOTES_TAKE_PHOTO;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
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
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final int NOTES_TAB = R.string.notes;
	public static final String DEFAULT_ACTION_SETTING_ID = "av_default_action";
	public static final String EXTERNAL_RECORDER_SETTING_ID = "av_external_recorder";
	public static final String EXTERNAL_PHOTO_CAM_SETTING_ID = "av_external_cam";
	public static final String THREEGP_EXTENSION = "3gp";
	public static final String MPEG4_EXTENSION = "mp4";
	public static final String IMG_EXTENSION = "jpg";
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
	public final CommonPreference<Boolean> AV_PHOTO_PLAY_SOUND;

	public static final int VIDEO_OUTPUT_MP4 = 0;
	public static final int VIDEO_OUTPUT_3GP = 1;
	public static final int VIDEO_QUALITY_DEFAULT = CamcorderProfile.QUALITY_HIGH; // High (highest res)
	public static final int AUDIO_FORMAT_DEFAULT = MediaRecorder.AudioEncoder.AAC; // AAC
	public static final int AUDIO_BITRATE_DEFAULT = 64 * 1024; // 64 kbps
	public final CommonPreference<Integer> AV_VIDEO_FORMAT;
	public final CommonPreference<Integer> AV_VIDEO_QUALITY;
	public final CommonPreference<Integer> AV_AUDIO_FORMAT;
	public final CommonPreference<Integer> AV_AUDIO_BITRATE;

	public static final int AV_DEFAULT_ACTION_AUDIO = 0;
	public static final int AV_DEFAULT_ACTION_VIDEO = 1;
	public static final int AV_DEFAULT_ACTION_TAKEPICTURE = 2;
	public static final int AV_DEFAULT_ACTION_CHOOSE = -1;

	@IntDef({AV_DEFAULT_ACTION.AUDIO, AV_DEFAULT_ACTION.VIDEO, AV_DEFAULT_ACTION.TAKEPICTURE, AV_DEFAULT_ACTION.CHOOSE})
	@Retention(RetentionPolicy.SOURCE)
	@interface AV_DEFAULT_ACTION {
		int AUDIO = AV_DEFAULT_ACTION_AUDIO;
		int VIDEO = AV_DEFAULT_ACTION_VIDEO;
		int TAKEPICTURE = AV_DEFAULT_ACTION_TAKEPICTURE;
		int CHOOSE = AV_DEFAULT_ACTION_CHOOSE;
	}

	// camera picture size:
	public static final int AV_PHOTO_SIZE_DEFAULT = -1;
	public static int cameraPictureSizeDefault;

	// camera focus type
	public static final int AV_CAMERA_FOCUS_AUTO = 0;
	public static final int AV_CAMERA_FOCUS_HIPERFOCAL = 1;
	public static final int AV_CAMERA_FOCUS_EDOF = 2;
	public static final int AV_CAMERA_FOCUS_INFINITY = 3;
	public static final int AV_CAMERA_FOCUS_MACRO = 4;
	public static final int AV_CAMERA_FOCUS_CONTINUOUS = 5;
	// photo shot:
	private static int shotId;
	private SoundPool soundPool;
	public static final int FULL_SCEEN_RESULT_DELAY_MS = 3000;

	public final CommonPreference<Integer> AV_CAMERA_PICTURE_SIZE;
	public final CommonPreference<Integer> AV_CAMERA_FOCUS_TYPE;
	public final OsmandPreference<Boolean> SHOW_RECORDINGS;

	public static final int CLIP_LENGTH_DEFAULT = 5;
	public static final int STORAGE_SIZE_DEFAULT = 5;
	public final CommonPreference<Boolean> AV_RECORDER_SPLIT;
	public final CommonPreference<Integer> AV_RS_CLIP_LENGTH;
	public final CommonPreference<Integer> AV_RS_STORAGE_SIZE;

	public final CommonPreference<NotesSortByMode> NOTES_SORT_BY_MODE;

	private final DataTileManager<Recording> recordings = new DataTileManager<>(14);
	private Map<String, Recording> recordingByFileName = new LinkedHashMap<>();
	private AudioNotesLayer audioNotesLayer;

	@Nullable
	private MapActivity mapActivity;

	private static File mediaRecFile;
	private static MediaRecorder mediaRec;
	private File lastTakingPhoto;
	private byte[] photoJpegData;
	private Timer photoTimer;
	private Camera cam;
	private List<Camera.Size> mSupportedPreviewSizes;
	private int requestedOrientation;
	private boolean autofocus;

	private AudioVideoNoteRecordingMenu recordingMenu;
	private CurrentRecording currentRecording;
	private boolean recordingDone = true;

	private MediaPlayer player;
	private Recording recordingPlaying;
	private Timer playerTimer;

	private static final char SPLIT_DESC = ' ';

	private double actionLat;
	private double actionLon;
	private int runAction = -1;

	public enum AVActionType {
		REC_AUDIO,
		REC_VIDEO,
		REC_PHOTO
	}

	public static class CurrentRecording {
		private final AVActionType type;

		public CurrentRecording(AVActionType type) {
			this.type = type;
		}

		public AVActionType getType() {
			return type;
		}
	}

	public static class Recording {
		public Recording(File f) {
			this.file = f;
		}

		private File file;

		private double lat;
		private double lon;
		private long duration = -1;
		private boolean available = true;

		public double getLatitude() {
			return lat;
		}

		public double getLongitude() {
			return lon;
		}

		private void updateInternalDescription() {
			if (duration == -1) {
				duration = 0;
				if (!isPhoto()) {
					MediaPlayer mediaPlayer = new MediaPlayer();
					try {
						mediaPlayer.setDataSource(file.getAbsolutePath());
						mediaPlayer.prepare();
						duration = mediaPlayer.getDuration();
						available = true;
					} catch (Exception e) {
						log.error("Error reading recording " + file.getAbsolutePath(), e);
						available = false;
					}
				}
			}
		}

		public File getFile() {
			return file;
		}

		public long getLastModified() {
			return file.lastModified();
		}

		public boolean setName(String name) {
			File directory = file.getParentFile();
			String fileName = getFileName();
			File to = new File(directory, name + SPLIT_DESC + getOtherName(fileName));
			if (file.renameTo(to)) {
				file = to;
				return true;
			}
			return false;
		}

		public boolean setLocation(LatLon latLon) {
			File directory = file.getParentFile();
			lat = latLon.getLatitude();
			lon = latLon.getLongitude();
			if (directory != null) {
				File to = getBaseFileName(lat, lon, directory, Algorithms.getFileExtension(file));
				if (file.renameTo(to)) {
					file = to;
					return true;
				}
			}
			return false;
		}

		public String getFileName() {
			return file.getName();
		}

		public String getDescriptionName(String fileName) {
			int hashInd = fileName.lastIndexOf(SPLIT_DESC);
			//backward compatibility
			if (fileName.indexOf('.') - fileName.indexOf('_') > 12 &&
					hashInd < fileName.indexOf('_')) {
				hashInd = fileName.indexOf('_');
			}
			if (hashInd == -1) {
				return null;
			} else {
				return fileName.substring(0, hashInd);
			}
		}

		public String getOtherName(String fileName) {
			String descriptionName = getDescriptionName(fileName);
			if (descriptionName != null) {
				return fileName.substring(descriptionName.length() + 1); // SPLIT_DESC
			} else {
				return fileName;
			}
		}

		public static String formatDateTime(Context ctx, long dateTime) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
			return dateFormat.format(dateTime) + " " + timeFormat.format(dateTime);
		}

		public String getName(Context ctx, boolean includingType) {
			String fileName = file.getName();
			String desc = getDescriptionName(fileName);
			if (desc != null) {
				return desc;
			} else if (this.isAudio() || this.isVideo() || this.isPhoto()) {
				if (includingType) {
					return getType(ctx) + " " + formatDateTime(ctx, file.lastModified());
				} else {
					return formatDateTime(ctx, file.lastModified());
				}
			}
			return "";
		}

		public String getType(@NonNull Context ctx) {
			if (this.isAudio()) {
				return ctx.getResources().getString(R.string.shared_string_audio);
			} else if (this.isVideo()) {
				return ctx.getResources().getString(R.string.shared_string_video);
			} else if (this.isPhoto()) {
				return ctx.getResources().getString(R.string.shared_string_photo);
			} else {
				return "";
			}
		}

		public String getSearchHistoryType() {
			if (isPhoto()) {
				return PointDescription.POINT_TYPE_PHOTO_NOTE;
			} else if (isVideo()) {
				return PointDescription.POINT_TYPE_VIDEO_NOTE;
			} else {
				return PointDescription.POINT_TYPE_PHOTO_NOTE;
			}
		}

		public boolean isPhoto() {
			return file.getName().endsWith(IMG_EXTENSION);
		}

		public boolean isVideo() {
			return file.getName().endsWith(MPEG4_EXTENSION);
		}

		public boolean isAudio() {
			return file.getName().endsWith(THREEGP_EXTENSION);
		}

		private String convertDegToExifRational(double l) {
			if (l < 0) {
				l = -l;
			}
			String s = ((int) l) + "/1,"; // degrees
			l = (l - ((int) l)) * 60.0;
			s += (int) l + "/1,"; // minutes
			l = (l - ((int) l)) * 60000.0;
			s += (int) l + "/1000"; // seconds
			// log.info("deg rational: " + s);
			return s;
		}

		@SuppressWarnings("rawtypes")
		public void updatePhotoInformation(double lat, double lon, Location loc, double rot) throws IOException {
			try {
				Class exClass = Class.forName("android.media.ExifInterface");

				Constructor c = exClass.getConstructor(String.class);
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method setAttribute = exClass.getMethod("setAttribute", String.class, String.class);
				setAttribute.invoke(exInstance, "GPSLatitude", convertDegToExifRational(lat));
				setAttribute.invoke(exInstance, "GPSLatitudeRef", lat > 0 ? "N" : "S");
				setAttribute.invoke(exInstance, "GPSLongitude", convertDegToExifRational(lon));
				setAttribute.invoke(exInstance, "GPSLongitudeRef", lon > 0 ? "E" : "W");
				if (!Double.isNaN(rot)) {
					setAttribute.invoke(exInstance, "GPSImgDirectionRef", "T");
					while (rot < 0) {
						rot += 360;
					}
					while (rot > 360) {
						rot -= 360;
					}
					int abs = (int) (Math.abs(rot) * 100.0);
					String rotString = abs + "/100";
					setAttribute.invoke(exInstance, "GPSImgDirection", rotString);
				}
				if (loc != null && loc.hasAltitude()) {
					double alt = loc.getAltitude();
					String altString = (int) (Math.abs(alt) * 100.0) + "/100";
					setAttribute.invoke(exInstance, "GPSAltitude", altString);
					setAttribute.invoke(exInstance, "GPSAltitudeRef", alt < 0 ? "1" : "0");
				}
				Method saveAttributes = exClass.getMethod("saveAttributes");
				saveAttributes.invoke(exInstance);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		@SuppressWarnings("rawtypes")
		private int getExifOrientation() {
			int orientation = 0;
			try {
				Class exClass = Class.forName("android.media.ExifInterface");
				Constructor c = exClass.getConstructor(String.class);
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method getAttributeInt = exClass.getMethod("getAttributeInt", String.class, Integer.TYPE);
				Integer it = (Integer) getAttributeInt.invoke(exInstance, "Orientation", 1);
				orientation = it;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return orientation;
		}

		public int getBitmapRotation() {
			int rotation = 0;
			switch (getExifOrientation()) {
				case 3:
					rotation = 180;
					break;
				case 6:
					rotation = 90;
					break;
				case 8:
					rotation = 270;
					break;
			}
			return rotation;
		}

		public String getDescription(Context ctx) {
			String time = AndroidUtils.formatDateTime(ctx, file.lastModified());
			if (isPhoto()) {
				return ctx.getString(R.string.recording_photo_description, "", time).trim();
			}
			updateInternalDescription();
			return ctx.getString(R.string.recording_description, "", getDuration(ctx, true), time)
					.trim();
		}

		public String getSmallDescription(Context ctx) {
			String time = AndroidUtils.formatDateTime(ctx, file.lastModified());
			if (isPhoto()) {
				return time;
			}
			updateInternalDescription();
			return time + " " + getDuration(ctx, true);

		}

		public String getExtendedDescription(Context ctx) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(ctx);
			String date = dateFormat.format(file.lastModified());
			String size = AndroidUtils.formatSize(ctx, file.length());
			if (isPhoto()) {
				return date + " • " + size;
			}
			updateInternalDescription();
			return date + " • " + size + " • " + getDuration(ctx, false);
		}

		public String getTypeWithDuration(Context ctx) {
			StringBuilder res = new StringBuilder(getType(ctx));
			if (isAudio() || isVideo()) {
				updateInternalDescription();
				res.append(", ").append(getDuration(ctx, false));
			}
			return res.toString();
		}

		public String getPlainDuration(boolean accessibilityEnabled) {
			updateInternalDescription();
			if (duration > 0) {
				int d = (int) (duration / 1000);
				return Algorithms.formatDuration(d, accessibilityEnabled);
			} else {
				return "";
			}
		}

		private String getDuration(Context ctx, boolean addRoundBrackets) {
			StringBuilder additional = new StringBuilder();
			if (duration > 0) {
				int d = (int) (duration / 1000);
				additional.append(addRoundBrackets ? "(" : "");
				additional.append(Algorithms.formatDuration(d, ((OsmandApplication) ctx.getApplicationContext()).accessibilityEnabled()));
				additional.append(addRoundBrackets ? ")" : "");
			}
			if (!available) {
				additional.append("[").append(ctx.getString(R.string.recording_unavailable)).append("]");
			}
			return additional.toString();
		}

		public static String getNameForMultimediaFile(@NonNull OsmandApplication app, @NonNull String fileName, long lastModified) {
			if (fileName.endsWith(IMG_EXTENSION)) {
				return app.getString(R.string.shared_string_photo) + " " + formatDateTime(app, lastModified);
			} else if (fileName.endsWith(MPEG4_EXTENSION)) {
				return app.getString(R.string.shared_string_video) + " " + formatDateTime(app, lastModified);
			} else if (fileName.endsWith(THREEGP_EXTENSION)) {
				return app.getString(R.string.shared_string_audio) + " " + formatDateTime(app, lastModified);
			}
			return "";
		}
	}

	public static int getIconIdForRecordingFile(@NonNull File file) {
		String fileName = file.getName();
		if (fileName.endsWith(IMG_EXTENSION)) {
			return R.drawable.ic_action_photo_dark;
		} else if (fileName.endsWith(MPEG4_EXTENSION)) {
			return R.drawable.ic_action_video_dark;
		} else if (fileName.endsWith(THREEGP_EXTENSION)) {
			return R.drawable.ic_action_micro_dark;
		}
		return -1;
	}

	@Override
	public String getId() {
		return PLUGIN_AUDIO_VIDEO_NOTES;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_ON_REQUEST, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_RECORD_AUDIO, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_RECORD_VIDEO, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(AV_NOTES_TAKE_PHOTO, noAppMode);

		AV_EXTERNAL_RECORDER = registerBooleanPreference(EXTERNAL_RECORDER_SETTING_ID, false);
		AV_EXTERNAL_PHOTO_CAM = registerBooleanPreference(EXTERNAL_PHOTO_CAM_SETTING_ID, true);
		AV_VIDEO_FORMAT = registerIntPreference("av_video_format", VIDEO_OUTPUT_MP4);
		AV_VIDEO_QUALITY = registerIntPreference("av_video_quality", VIDEO_QUALITY_DEFAULT);
		AV_AUDIO_FORMAT = registerIntPreference("av_audio_format", AUDIO_FORMAT_DEFAULT);
		AV_AUDIO_BITRATE = registerIntPreference("av_audio_bitrate", AUDIO_BITRATE_DEFAULT);
		// camera picture size:
		AV_CAMERA_PICTURE_SIZE = registerIntPreference("av_camera_picture_size", AV_PHOTO_SIZE_DEFAULT);
		// camera focus type:
		AV_CAMERA_FOCUS_TYPE = registerIntPreference("av_camera_focus_type", AV_CAMERA_FOCUS_AUTO);
		// camera sound:
		AV_PHOTO_PLAY_SOUND = registerBooleanPreference("av_photo_play_sound", true);

		SHOW_RECORDINGS = registerBooleanPreference("show_recordings", true);

		AV_RECORDER_SPLIT = registerBooleanPreference("av_recorder_split", false);
		AV_RS_CLIP_LENGTH = registerIntPreference("av_rs_clip_length", CLIP_LENGTH_DEFAULT);
		AV_RS_STORAGE_SIZE = registerIntPreference("av_rs_storage_size", STORAGE_SIZE_DEFAULT);

		NOTES_SORT_BY_MODE = registerEnumStringPreference("notes_sort_by_mode", NotesSortByMode.BY_DATE, NotesSortByMode.values(), NotesSortByMode.class);
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
		loadCameraSoundIfNeeded(false);
		AV_PHOTO_PLAY_SOUND.addListener(change -> app.runInUIThread(() -> {
			loadCameraSoundIfNeeded(true);
		}));
		return true;
	}

	private void loadCameraSoundIfNeeded(boolean checkSoundPool) {
		if (isShutterSoundEnabled() && (!checkSoundPool || soundPool == null)) {
			loadCameraSound();
		}
	}

	private void loadCameraSound() {
		if (soundPool == null) {
			AudioAttributes attr = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
					.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
					.build();
			soundPool = new SoundPool.Builder().setAudioAttributes(attr).setMaxStreams(5).build();
		}
		if (shotId == 0) {
			try {
				AssetFileDescriptor assetFileDescriptor = app.getAssets().openFd("sounds/camera_click.ogg");
				shotId = soundPool.load(assetFileDescriptor, 1);
				assetFileDescriptor.close();
			} catch (Exception e) {
				log.error("cannot get shotId for sounds/camera_click.ogg");
			}
		}
	}

	public boolean isShutterSoundEnabled() {
		return AV_PHOTO_PLAY_SOUND.get();
	}

	public static boolean canDisableShutterSound() {
		CameraInfo info = new CameraInfo();
		return info.canDisableShutterSound;
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

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
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
											  ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
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
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

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
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		switch (widgetType) {
			case AV_NOTES_ON_REQUEST:
				return new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.CHOOSE, customId, widgetsPanel);
			case AV_NOTES_RECORD_AUDIO:
				return new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.AUDIO, customId, widgetsPanel);
			case AV_NOTES_RECORD_VIDEO:
				return new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.VIDEO, customId, widgetsPanel);
			case AV_NOTES_TAKE_PHOTO:
				return new AudioVideoNotesWidget(mapActivity, widgetType, AV_DEFAULT_ACTION.TAKEPICTURE, customId, widgetsPanel);
		}
		return null;
	}

	public void makeAction(@NonNull MapActivity mapActivity, int actionId) {
		Location loc = app.getLocationProvider().getLastKnownLocation();
		if (loc == null) {
			Toast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
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
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder ab = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		ab.setItems(
				new String[]{mapActivity.getString(R.string.recording_context_menu_arecord),
						mapActivity.getString(R.string.recording_context_menu_vrecord),
						mapActivity.getString(R.string.recording_context_menu_precord),}, (dialog, which) -> {
					int action = which == 0 ? AV_DEFAULT_ACTION_AUDIO : (which == 1 ? AV_DEFAULT_ACTION_VIDEO
							: AV_DEFAULT_ACTION_TAKEPICTURE);
					takeAction(mapActivity, lon, lat, action);

				});
		ab.show();
	}

	private void takeAction(MapActivity mapActivity, double lon, double lat, int action) {
		if (action == AV_DEFAULT_ACTION_VIDEO) {
			recordVideo(lat, lon, mapActivity, false);
		} else if (action == AV_DEFAULT_ACTION_TAKEPICTURE) {
			takePhoto(lat, lon, mapActivity, false, false);
		} else if (action == AV_DEFAULT_ACTION_AUDIO) {
			recordAudio(lat, lon, mapActivity);
		}
	}

	private static File getBaseFileName(double lat, double lon, OsmandApplication app, String ext) {
		File baseDir = app.getAppPath(IndexConstants.AV_INDEX_DIR);
		return getBaseFileName(lat, lon, baseDir, ext);
	}

	private static File getBaseFileName(double lat, double lon, @NonNull File baseDir, @NonNull String ext) {
		String basename = MapUtils.createShortLinkString(lat, lon, 15);
		int k = 1;
		baseDir.mkdirs();
		File fl;
		do {
			fl = new File(baseDir, basename + "." + (k++) + "." + ext);
		} while (fl.exists());
		return fl;
	}

	public void captureImage(double lat, double lon, MapActivity mapActivity) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Uri fileUri = AndroidUtils.getUriForFile(mapActivity, getBaseFileName(lat, lon, app, IMG_EXTENSION));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			intent.setClipData(ClipData.newRawUri("", fileUri));
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		// start the image capture Intent
		AndroidUtils.startActivityForResultIfSafe(mapActivity, intent, 105);
	}

	public void captureVideoExternal(double lat, double lon, MapActivity mapActivity) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		Uri fileUri = AndroidUtils.getUriForFile(mapActivity, getBaseFileName(lat, lon, app, MPEG4_EXTENSION));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			intent.setClipData(ClipData.newRawUri("", fileUri));
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
		// start the video capture Intent
		AndroidUtils.startActivityForResultIfSafe(mapActivity, intent, 205);
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
		stopRecording(activity, false);
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
				if (currentRecording.getType() == AVActionType.REC_VIDEO && AV_RECORDER_SPLIT.get()) {
					runAction = AV_DEFAULT_ACTION_VIDEO;
					LatLon latLon = getNextRecordingLocation();
					actionLat = latLon.getLatitude();
					actionLon = latLon.getLongitude();
				}
				stopRecording(activity, false);
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

	public boolean isRecording() {
		return currentRecording != null;
	}

	private void initRecMenu(AVActionType actionType, double lat, double lon) {
		if (mapActivity != null) {
			currentRecording = new CurrentRecording(actionType);
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
		if (ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
			if (AV_EXTERNAL_RECORDER.get() || forceExternal) {
				captureVideoExternal(lat, lon, mapActivity);
			} else {
				openCamera();
				if (cam != null) {
					initRecMenu(AVActionType.REC_VIDEO, lat, lon);
					recordVideoCamera(lat, lon, mapActivity);
				}
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(mapActivity, new String[]{Manifest.permission.CAMERA,
					Manifest.permission.RECORD_AUDIO}, CAMERA_FOR_VIDEO_REQUEST_CODE);
		}

	}

	public void recordVideoCamera(double lat, double lon, MapActivity mapActivity) {
		CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
		Camera.Size mPreviewSize = getPreviewSize();

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
					startCamera(mPreviewSize, holder);

					cam.unlock();
					mr.setCamera(cam);

				} catch (Exception e) {
					logErr(e);
					closeRecordingMenu();
					closeCamera();
					finishRecording();
					return;
				}

				File f = getBaseFileName(lat, lon, app, MPEG4_EXTENSION);
				initMediaRecorder(mr, p, f);
				try {
					if (AV_RECORDER_SPLIT.get()) {
						cleanupSpace();
					}
					runMediaRecorder(mapActivity, mr, f);
				} catch (Exception e) {
					logErr(e);
				}
			}

			@Override
			public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
			}
		});
		recordingMenu.show();
	}

	private void initMediaRecorder(MediaRecorder mr, CamcorderProfile p, File f) {
		mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		giveMediaRecorderHintRotatedScreen(mapActivity, mr);

		mr.setProfile(p);
		mr.setOutputFile(f.getAbsolutePath());
	}

	private void giveMediaRecorderHintRotatedScreen(MapActivity mapActivity, MediaRecorder mr) {
		try {
			Method m = mr.getClass().getDeclaredMethod("setOrientationHint", Integer.TYPE);
			Display display = ((WindowManager) mapActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			if (display.getRotation() == Surface.ROTATION_0) {
				m.invoke(mr, 90);
			} else if (display.getRotation() == Surface.ROTATION_270) {
				m.invoke(mr, 180);
			} else if (display.getRotation() == Surface.ROTATION_180) {
				m.invoke(mr, 270);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void logErr(Exception e) {
		log.error("Error starting recorder ", e);
		Toast.makeText(app, app.getString(R.string.recording_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
	}

	protected void openCamera() {
		if (cam != null) {
			try {
				cam.release();
				cam = null;
			} catch (Exception e) {
				logErr(e);
			}
		}
		try {
			cam = Camera.open();
			if (mSupportedPreviewSizes == null) {
				mSupportedPreviewSizes = cam.getParameters().getSupportedPreviewSizes();
			}
		} catch (Exception e) {
			logErr(e);
		}
	}

	protected void closeCamera() {
		if (cam != null) {
			try {
				cam.release();
			} catch (Exception e) {
				logErr(e);
			}
			cam = null;
		}
	}

	private void lockScreenOrientation() {
		requestedOrientation = mapActivity.getRequestedOrientation();
		mapActivity.setRequestedOrientation(AndroidUiHelper.getScreenOrientation(mapActivity));
	}

	private void restoreScreenOrientation() {
		mapActivity.setRequestedOrientation(requestedOrientation);
	}

	private Camera.Size getPreviewSize() {
		CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
		Camera.Size mPreviewSize;
		if (mSupportedPreviewSizes != null) {
			int width;
			int height;
			if (recordingMenu.isLandscapeLayout()) {
				width = p.videoFrameWidth;
				height = p.videoFrameHeight;
			} else {
				height = p.videoFrameWidth;
				width = p.videoFrameHeight;
			}
			mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
		} else {
			mPreviewSize = null;
		}
		return mPreviewSize;
	}

	protected void startCamera(Camera.Size mPreviewSize, SurfaceHolder holder) throws IOException {
		Parameters parameters = cam.getParameters();

		// camera focus type
		List<String> sfm = parameters.getSupportedFocusModes();
		if (sfm.contains("continuous-video")) {
			parameters.setFocusMode("continuous-video");
		}

		int cameraOrientation = getCamOrientation(mapActivity, Camera.CameraInfo.CAMERA_FACING_BACK);
		cam.setDisplayOrientation(cameraOrientation);
		parameters.set("rotation", cameraOrientation);
		if (mPreviewSize != null) {
			parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		}
		cam.setParameters(parameters);
		if (holder != null) {
			cam.setPreviewDisplay(holder);
		}
		cam.startPreview();
	}

	protected void stopCamera() {
		try {
			if (cam != null) {
				cam.cancelAutoFocus();
				cam.stopPreview();
				cam.setPreviewDisplay(null);
			}
		} catch (Exception e) {
			logErr(e);
		} finally {
			closeCamera();
		}
	}

	private boolean stopMediaRecording(boolean restart) {
		boolean res = true;
		AVActionType type = null;
		if (isRecording()) {
			type = currentRecording.type;
		}
		if (type == null || type == AVActionType.REC_AUDIO) {
			unmuteStreamMusicAndOutputGuidance();
		}
		if (mediaRec != null) {
			try {
				mediaRec.stop();
			} catch (RuntimeException e) {
				log.error(e.getMessage(), e);
			}
			indexFile(true, mediaRecFile);
			mediaRec.release();
			mediaRec = null;
			mediaRecFile = null;

			if (type == null) {
				res = false;
			} else if (restart) {
				try {
					cam.lock();
					if (AV_RECORDER_SPLIT.get()) {
						cleanupSpace();
					}

					currentRecording = new CurrentRecording(type);
					MediaRecorder mr = new MediaRecorder();
					LatLon latLon = getNextRecordingLocation();
					File f = getBaseFileName(latLon.getLatitude(), latLon.getLongitude(), app, MPEG4_EXTENSION);

					cam.unlock();
					mr.setCamera(cam);
					initMediaRecorder(mr, CamcorderProfile.get(AV_VIDEO_QUALITY.get()), f);
					mr.prepare();
					mr.start();
					mediaRec = mr;
					mediaRecFile = f;

				} catch (Exception e) {
					Toast.makeText(app, e.getMessage(), Toast.LENGTH_LONG).show();
					log.error(e.getMessage(), e);
					res = false;
				}
			}
		}
		return res;
	}

	public void recordAudio(double lat, double lon, @NonNull MapActivity mapActivity) {
		if (ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.RECORD_AUDIO)
				== PackageManager.PERMISSION_GRANTED) {

			initRecMenu(AVActionType.REC_AUDIO, lat, lon);
			MediaRecorder mr = new MediaRecorder();
			File f = getBaseFileName(lat, lon, app, THREEGP_EXTENSION);
			mr.setAudioSource(MediaRecorder.AudioSource.MIC);
			mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mr.setAudioEncoder(AV_AUDIO_FORMAT.get());
			mr.setAudioEncodingBitRate(AV_AUDIO_BITRATE.get());
			mr.setOutputFile(f.getAbsolutePath());

			muteStreamMusicAndOutputGuidance();
			try {
				runMediaRecorder(mapActivity, mr, f);
			} catch (Exception e) {
				unmuteStreamMusicAndOutputGuidance();
				log.error("Error starting audio recorder ", e);
				Toast.makeText(app, app.getString(R.string.recording_error) + " : "
						+ e.getMessage(), Toast.LENGTH_LONG).show();
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(mapActivity,
					new String[]{Manifest.permission.RECORD_AUDIO},
					AUDIO_REQUEST_CODE);
		}
	}

	private void muteStreamMusicAndOutputGuidance() {
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		int voiceGuidanceOutput = app.getSettings().AUDIO_MANAGER_STREAM.get();
		am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
		if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
			am.adjustStreamVolume(voiceGuidanceOutput, AudioManager.ADJUST_MUTE, 0);
	}

	private void unmuteStreamMusicAndOutputGuidance() {
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		int voiceGuidanceOutput = app.getSettings().AUDIO_MANAGER_STREAM.get();
		am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
		if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
			am.adjustStreamVolume(voiceGuidanceOutput, AudioManager.ADJUST_UNMUTE, 0);
	}

	public void takePhoto(double lat, double lon, @NonNull MapActivity mapActivity,
						  boolean forceInternal, boolean forceExternal) {
		if (ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED) {
			if ((!AV_EXTERNAL_PHOTO_CAM.get() || forceInternal) && !forceExternal) {
				takePhotoInternalOrExternal(lat, lon, mapActivity);
			} else {
				takePhotoExternal(lat, lon, mapActivity);
			}
		} else {
			actionLat = lat;
			actionLon = lon;
			ActivityCompat.requestPermissions(mapActivity,
					new String[]{Manifest.permission.CAMERA},
					CAMERA_FOR_PHOTO_REQUEST_CODE);
		}
	}

	private void takePhotoInternalOrExternal(double lat, double lon, MapActivity mapActivity) {
		openCamera();
		if (cam != null) {
			initRecMenu(AVActionType.REC_PHOTO, lat, lon);
			takePhotoWithCamera(lat, lon, mapActivity);
		} else {
			takePhotoExternal(lat, lon, mapActivity);
		}
	}

	private void takePhotoWithCamera(double lat, double lon, MapActivity mapActivity) {
		try {
			lastTakingPhoto = getBaseFileName(lat, lon, app, IMG_EXTENSION);
			Camera.Size mPreviewSize;
			Parameters parameters = cam.getParameters();
			List<Camera.Size> psps = parameters.getSupportedPictureSizes();
			int camPicSizeIndex = AV_CAMERA_PICTURE_SIZE.get();
			// camera picture size
			log.debug("takePhotoWithCamera() index=" + camPicSizeIndex);
			if (camPicSizeIndex == AV_PHOTO_SIZE_DEFAULT) {
				camPicSizeIndex = cameraPictureSizeDefault;
				log.debug("takePhotoWithCamera() Default value of picture size. Set index to cameraPictureSizeDefault. Now index="
						+ camPicSizeIndex);
			}
			Camera.Size selectedCamPicSize = psps.get(camPicSizeIndex);
			if (mSupportedPreviewSizes != null) {
				int width = selectedCamPicSize.width;
				int height = selectedCamPicSize.height;
				mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
			} else {
				mPreviewSize = null;
			}
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
						Parameters parameters = cam.getParameters();
						parameters.setPictureSize(selectedCamPicSize.width, selectedCamPicSize.height);
						log.debug("takePhotoWithCamera() set Picture size: width=" + selectedCamPicSize.width
								+ " height=" + selectedCamPicSize.height);

						// camera focus type
						autofocus = true;
						parameters.removeGpsData();
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
							parameters.setGpsLatitude(lat);
							parameters.setGpsLongitude(lon);
						}
						switch (AV_CAMERA_FOCUS_TYPE.get()) {
							case AV_CAMERA_FOCUS_HIPERFOCAL:
								parameters.setFocusMode(Parameters.FOCUS_MODE_FIXED);
								autofocus = false;
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_FIXED");
								break;
							case AV_CAMERA_FOCUS_EDOF:
								parameters.setFocusMode(Parameters.FOCUS_MODE_EDOF);
								autofocus = false;
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_EDOF");
								break;
							case AV_CAMERA_FOCUS_INFINITY:
								parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
								autofocus = false;
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_INFINITY");
								break;
							case AV_CAMERA_FOCUS_MACRO:
								parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_MACRO");
								break;
							case AV_CAMERA_FOCUS_CONTINUOUS:
								parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_CONTINUOUS_PICTURE");
								break;
							default:
								parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
								log.info("Osmand:AudioNotes set camera FOCUS_MODE_AUTO");
								break;
						}

						if (parameters.getSupportedWhiteBalance() != null
								&& parameters.getSupportedWhiteBalance().contains(Parameters.WHITE_BALANCE_AUTO)) {
							parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
						}
						if (parameters.getSupportedFlashModes() != null
								&& parameters.getSupportedFlashModes().contains(Parameters.FLASH_MODE_AUTO)) {
							//parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
						}

						int cameraOrientation = getCamOrientation(mapActivity, Camera.CameraInfo.CAMERA_FACING_BACK);
						cam.setDisplayOrientation(cameraOrientation);
						parameters.set("rotation", cameraOrientation);
						if (mPreviewSize != null) {
							parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
						}
						cam.setParameters(parameters);
						cam.setPreviewDisplay(holder);
						cam.startPreview();
						internalShoot();

					} catch (Exception e) {
						logErr(e);
						closeRecordingMenu();
						closeCamera();
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
			closeCamera();
		}
	}

	private void internalShoot() {
		app.runInUIThread(() -> {
			if (cam != null) {
				if (canDisableShutterSound()) {
					cam.enableShutterSound(isShutterSoundEnabled());
				}
				if (autofocus) {
					takePictureOnAutofocus();
				} else {
					takePicture();
				}
			}
		}, 200);
	}

	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio;
		if (w > h) {
			targetRatio = (double) w / h;
		} else {
			targetRatio = (double) h / w;
		}

		if (sizes == null) return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - h) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - h) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - h);
				}
			}
		}
		return optimalSize;
	}

	private static int getCamOrientation(MapActivity mapActivity, int cameraId) {
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = mapActivity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public void shoot() {
		if (!recordingDone) {
			recordingDone = true;
			if (cam != null && lastTakingPhoto != null) {
				try {
					takePicture();
				} catch (RuntimeException e) {
					closeRecordingMenu();
					closeCamera();
					finishRecording();
				}
			}
		}
	}

	public void takePictureOnAutofocus() {
		cam.autoFocus((success, camera) -> {
			try {
				takePicture();
			} catch (Exception e) {
				logErr(e);
				closeRecordingMenu();
				closeCamera();
				finishRecording();
			}
		});
	}

	public void takePicture() {
		cam.takePicture(null, null, new JpegPhotoHandler());
	}

	public void takePhotoExternal(double lat, double lon, MapActivity mapActivity) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File f = getBaseFileName(lat, lon, app, IMG_EXTENSION);
		lastTakingPhoto = f;
		Uri uri = AndroidUtils.getUriForFile(mapActivity, f);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			takePictureIntent.setClipData(ClipData.newRawUri("", uri));
		}
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		try {
			AndroidUtils.startActivityForResultIfSafe(mapActivity, takePictureIntent, 205);
		} catch (Exception e) {
			log.error("Error taking a picture ", e);
			Toast.makeText(app, app.getString(R.string.recording_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void cleanupSpace() {
		File[] files = app.getAppPath(IndexConstants.AV_INDEX_DIR).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith("." + MPEG4_EXTENSION);
			}
		});

		if (files != null) {
			double usedSpace = 0;
			for (File f : files) {
				usedSpace += f.length();
			}
			usedSpace /= (1 << 30); // gigabytes

			CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
			double bitrate = (((p.videoBitRate + p.audioBitRate) / 8f) * 60f) / (1 << 30); // gigabytes per minute
			double clipSpace = bitrate * AV_RS_CLIP_LENGTH.get();
			double storageSize = AV_RS_STORAGE_SIZE.get();
			double availableSpace = (double) AndroidUtils.getAvailableSpace(app) / (1 << 30) - clipSpace;

			if (usedSpace + clipSpace > storageSize || clipSpace > availableSpace) {
				Arrays.sort(files, (lhs, rhs) -> Long.compare(lhs.lastModified(), rhs.lastModified()));
				boolean wasAnyDeleted = false;
				ArrayList<File> arr = new ArrayList<>(Arrays.asList(files));
				while (arr.size() > 0
						&& (usedSpace + clipSpace > storageSize || clipSpace > availableSpace)) {
					File f = arr.remove(0);
					double length = ((double) f.length()) / (1 << 30);
					Recording r = recordingByFileName.get(f.getName());
					if (r != null) {
						deleteRecording(r, false);
						wasAnyDeleted = true;
						usedSpace -= length;
						availableSpace += length;
					} else if (f.delete()) {
						usedSpace -= length;
						availableSpace += length;
					}
				}
				if (wasAnyDeleted) {
					app.runInUIThread(() -> mapActivity.refreshMap(), 20);
				}
			}
		}
	}

	private void runMediaRecorder(MapActivity mapActivity, MediaRecorder mr, File f) throws IOException {
		mr.prepare();
		mr.start();
		mediaRec = mr;
		mediaRecFile = f;

		recordingMenu.show();
		mapActivity.refreshMap();
	}

	public void stopRecording(@NonNull MapActivity mapActivity, boolean restart) {
		if (!recordingDone) {
			if (!restart || !stopMediaRecording(true)) {
				recordingDone = true;
				stopMediaRecording(false);
				SHOW_RECORDINGS.set(true);
				mapActivity.refreshMap();
				closeRecordingMenu();
			}
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

	private void updateContextMenu(Recording rec) {
		if (mapActivity != null && rec != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			menu.show(new LatLon(rec.lat, rec.lon), audioNotesLayer.getObjectName(rec), rec);
			if (app.getRoutingHelper().isFollowingMode()) {
				menu.hideWithTimeout(3000);
			}
		}
	}

	private void finishRecording() {
		currentRecording = null;
	}

	@Override
	public void addMyPlacesTab(MyPlacesActivity myPlacesActivity, List<TabItem> mTabs, Intent intent) {
		mTabs.add(myPlacesActivity.getTabIndicator(NOTES_TAB, NotesFragment.class));
		if (intent != null && "AUDIO".equals(intent.getStringExtra("TAB"))) {
			app.getSettings().FAVORITES_TAB.set(NOTES_TAB);
		}
	}

	public boolean indexSingleFile(File f) {
		boolean oldFileExist = recordingByFileName.containsKey(f.getName());
		if (oldFileExist) {
			return false;
		}
		Recording r = new Recording(f);
		String fileName = f.getName();
		String otherName = r.getOtherName(fileName);
		int i = otherName.indexOf('.');
		if (i > 0) {
			otherName = otherName.substring(0, i);
		}
		r.file = f;
		GeoParsedPoint geo = MapUtils.decodeShortLinkString(otherName);
		r.lat = geo.getLatitude();
		r.lon = geo.getLongitude();
		Float heading = app.getLocationProvider().getHeading();
		Location loc = app.getLocationProvider().getLastKnownLocation();
		if (lastTakingPhoto != null && lastTakingPhoto.getName().equals(f.getName())) {
			float rot = heading != null ? heading : 0;
			try {
				r.updatePhotoInformation(r.lat, r.lon, loc, rot == 0 ? Double.NaN : rot);
			} catch (IOException e) {
				log.error("Error updating EXIF information " + e.getMessage(), e);
			}
			lastTakingPhoto = null;
		}
		recordings.registerObject(r.lat, r.lon, r);

		Map<String, Recording> newMap = new LinkedHashMap<>(recordingByFileName);
		newMap.put(f.getName(), r);
		recordingByFileName = newMap;

		if (isRecording()) {
			AVActionType type = currentRecording.type;
			finishRecording();
			if (type != AVActionType.REC_AUDIO && (!AV_RECORDER_SPLIT.get() || type != AVActionType.REC_VIDEO)) {
				Recording recordingForMenu = r;
				app.runInUIThread(() -> updateContextMenu(recordingForMenu), 200);
			}
		}

		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		if (soundPool != null) {
			soundPool.release();
			soundPool = null;
			shotId = 0;
		}
	}

	@Override
	public List<String> indexingFiles(@Nullable IProgress progress) {
		return indexingFiles(true, false);
	}

	public List<String> indexingFiles(boolean reIndexAndKeepOld, boolean registerNew) {
		File avPath = app.getAppPath(IndexConstants.AV_INDEX_DIR);
		if (avPath.canRead()) {
			if (!reIndexAndKeepOld) {
				recordings.clear();
				recordingByFileName = new LinkedHashMap<>();
			}
			File[] files = avPath.listFiles();
			if (files != null) {
				for (File f : files) {
					indexFile(registerNew, f);
				}
			}
		}
		return null;
	}

	private void indexFile(boolean registerInGPX, File f) {
		if (f.getName().endsWith(THREEGP_EXTENSION) || f.getName().endsWith(MPEG4_EXTENSION)
				|| f.getName().endsWith(IMG_EXTENSION)) {
			boolean newFileIndexed = indexSingleFile(f);
			if (newFileIndexed && registerInGPX) {
				Recording rec = recordingByFileName.get(f.getName());
				if (rec != null &&
						(app.getSettings().SAVE_TRACK_TO_GPX.get()
								|| app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get())
						&& PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
					String name = f.getName();
					app.getSavingTrackHelper().insertPointData(rec.lat, rec.lon, null, name, null, 0);
				}
			}

		}
	}

	public DataTileManager<Recording> getRecordings() {
		return recordings;
	}

	private void checkRecordings() {
		Iterator<Recording> it = recordingByFileName.values().iterator();
		while (it.hasNext()) {
			Recording r = it.next();
			if (!r.file.exists()) {
				it.remove();
				recordings.unregisterObject(r.lat, r.lon, r);
			}
		}
	}

	public void deleteRecording(Recording r, boolean updateUI) {
		recordings.unregisterObject(r.lat, r.lon, r);
		Map<String, Recording> newMap = new LinkedHashMap<>(recordingByFileName);
		newMap.remove(r.file.getName());
		recordingByFileName = newMap;
		Algorithms.removeAllFiles(r.file);
		if (mapActivity != null && updateUI) {
			if (mapActivity.getContextMenu().getObject() == r) {
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
			indexingFiles(true, true);
		}
	}

	public Collection<Recording> getAllRecordings() {
		return recordingByFileName.values();
	}

	protected Recording[] getRecordingsSorted() {
		checkRecordings();
		Collection<Recording> allObjects = getAllRecordings();
		Recording[] res = allObjects.toArray(new Recording[0]);
		Arrays.sort(res, (object1, object2) -> {
			long l1 = object1.file.lastModified();
			long l2 = object2.file.lastModified();
			return l1 < l2 ? 1 : -1;
		});
		return res;
	}

	public boolean isPlaying() {
		try {
			return player != null && player.isPlaying();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isPlaying(Recording r) {
		return isPlaying() && recordingPlaying == r;
	}

	public int getPlayingPosition() {
		if (isPlaying()) {
			return player.getCurrentPosition();
		} else if (player != null) {
			return player.getDuration();
		} else {
			return -1;
		}
	}

	public void stopPlaying() {
		if (isPlaying()) {
			try {
				player.stop();
			} finally {
				player.release();
				player = null;
				updateContextMenu();
			}
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

	public void playRecording(@NonNull Context ctx, @NonNull Recording r) {
		if (r.isVideo() || r.isPhoto()) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			String type = r.isVideo() ? "video/*" : "image/*";
			intent.setDataAndType(AndroidUtils.getUriForFile(ctx, r.file), type);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			AndroidUtils.startActivityIfSafe(ctx, intent);
			return;
		}

		if (isPlaying()) {
			stopPlaying();
		}
		recordingPlaying = r;
		player = new MediaPlayer();
		try {
			player.setDataSource(r.file.getAbsolutePath());
			player.setOnPreparedListener(mp -> {
				try {
					player.start();

					if (playerTimer != null) {
						playerTimer.cancel();
					}
					playerTimer = new Timer();
					playerTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							updateContextMenu();
							if (!isPlaying()) {
								cancel();
								playerTimer = null;
							}
						}

					}, 10, 1000);

				} catch (Exception e) {
					logErr(e);
				}
			});
			player.setOnCompletionListener(mp -> recordingPlaying = null);
			player.prepareAsync();
		} catch (Exception e) {
			logErr(e);
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
	public void handleRequestPermissionsResult(int requestCode, String[] permissions,
											   int[] grantResults) {
		runAction = -1;
		if (requestCode == CAMERA_FOR_VIDEO_REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED
					&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
				runAction = AV_DEFAULT_ACTION_VIDEO;
			} else {
				app.showToastMessage(R.string.no_camera_permission);
			}
		} else if (requestCode == CAMERA_FOR_PHOTO_REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				runAction = AV_DEFAULT_ACTION_TAKEPICTURE;
			} else {
				app.showToastMessage(R.string.no_camera_permission);
			}
		} else if (requestCode == AUDIO_REQUEST_CODE) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				runAction = AV_DEFAULT_ACTION_AUDIO;
			} else {
				app.showToastMessage(R.string.no_microphone_permission);
			}
		}
	}

	public class JpegPhotoHandler implements PictureCallback {

		public JpegPhotoHandler() {
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			photoJpegData = data;
			if (isShutterSoundEnabled() && soundPool != null && shotId != 0) {
				soundPool.play(shotId, 0.7f, 0.7f, 0, 0, 1);
			}
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
		if (cam != null) {
			try {
				cam.cancelAutoFocus();
				cam.stopPreview();
				if (recordingMenu != null) {
					recordingMenu.hideFinalPhoto();
				}
				cam.startPreview();
				internalShoot();

			} catch (Exception e) {
				logErr(e);
				closeRecordingMenu();
				closeCamera();
				finishRecording();
			}
		}
	}

	public synchronized void finishPhotoRecording(boolean cancel) {
		cancelPhotoTimer();
		if (photoJpegData != null && photoJpegData.length > 0 && lastTakingPhoto != null) {
			try {
				if (!cancel) {
					FileOutputStream fos = new FileOutputStream(lastTakingPhoto);
					fos.write(photoJpegData);
					fos.close();
					indexFile(true, lastTakingPhoto);
				}
			} catch (Exception error) {
				logErr(error);
			} finally {
				photoJpegData = null;
				closeRecordingMenu();
				if (!cancel) {
					finishRecording();
				}
			}
		} else if (cancel) {
			closeRecordingMenu();
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
