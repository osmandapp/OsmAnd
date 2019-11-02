package net.osmand.plus.audionotes;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TabActivity.TabItem;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPointParserUtil.GeoParsedPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.RECORDING_LAYER;


public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final int NOTES_TAB = R.string.notes;
	public static final String ID = "osmand.audionotes";
	public static final String THREEGP_EXTENSION = "3gp";
	public static final String MPEG4_EXTENSION = "mp4";
	public static final String IMG_EXTENSION = "jpg";
	private static final Log log = PlatformUtil.getLog(AudioVideoNotesPlugin.class);
	public static final int CAMERA_FOR_VIDEO_REQUEST_CODE = 101;
	public static final int CAMERA_FOR_PHOTO_REQUEST_CODE = 102;
	public static final int AUDIO_REQUEST_CODE = 103;

	// Constants for determining the order of items in the additional actions context menu
	private static final int TAKE_AUDIO_NOTE_ITEM_ORDER = 4100;
	private static final int TAKE_VIDEO_NOTE_ITEM_ORDER = 4300;
	private static final int TAKE_PHOTO_NOTE_ITEM_ORDER = 4500;

	private static Method mRegisterMediaButtonEventReceiver;
	private static Method mUnregisterMediaButtonEventReceiver;
	private OsmandApplication app;
	private TextInfoWidget recordControl;

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

	// camera picture size:
	public static final int AV_PHOTO_SIZE_DEFAULT = -1;
	public static int cameraPictureSizeDefault = 0;

	// camera focus type
	public static final int AV_CAMERA_FOCUS_AUTO = 0;
	public static final int AV_CAMERA_FOCUS_HIPERFOCAL = 1;
	public static final int AV_CAMERA_FOCUS_EDOF = 2;
	public static final int AV_CAMERA_FOCUS_INFINITY = 3;
	public static final int AV_CAMERA_FOCUS_MACRO = 4;
	public static final int AV_CAMERA_FOCUS_CONTINUOUS = 5;
	// photo shot:
	private static int shotId = 0;
	private SoundPool sp = null;
	public static final int FULL_SCEEN_RESULT_DELAY_MS = 3000;

	public final CommonPreference<Integer> AV_CAMERA_PICTURE_SIZE;
	public final CommonPreference<Integer> AV_CAMERA_FOCUS_TYPE;
	public final CommonPreference<Integer> AV_DEFAULT_ACTION;
	public final OsmandPreference<Boolean> SHOW_RECORDINGS;

	public static final int CLIP_LENGTH_DEFAULT = 5;
	public static final int STORAGE_SIZE_DEFAULT = 5;
	public final CommonPreference<Boolean> AV_RECORDER_SPLIT;
	public final CommonPreference<Integer> AV_RS_CLIP_LENGTH;
	public final CommonPreference<Integer> AV_RS_STORAGE_SIZE;

	private DataTileManager<Recording> recordings = new DataTileManager<AudioVideoNotesPlugin.Recording>(14);
	private Map<String, Recording> recordingByFileName = new LinkedHashMap<>();
	private AudioNotesLayer audioNotesLayer;

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

	private final static char SPLIT_DESC = ' ';

	private double actionLat;
	private double actionLon;
	private int runAction = -1;

	public enum AVActionType {
		REC_AUDIO,
		REC_VIDEO,
		REC_PHOTO
	}

	public static class CurrentRecording {
		private AVActionType type;

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
			File to = getBaseFileName(lat, lon, directory, Algorithms.getFileExtension(file));
			if (file.renameTo(to)) {
				file = to;
				return true;
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

		private String formatDateTime(Context ctx, long dateTime) {
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
			return file.getName().endsWith(MPEG4_EXTENSION);// || file.getName().endsWith(THREEGP_EXTENSION);
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

				Constructor c = exClass.getConstructor(new Class[]{String.class});
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method setAttribute = exClass.getMethod("setAttribute", new Class[]{String.class, String.class});
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
				Method saveAttributes = exClass.getMethod("saveAttributes", new Class[]{});
				saveAttributes.invoke(exInstance);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		}

		@SuppressWarnings("rawtypes")
		private int getExifOrientation() {
			int orientation = 0;
			try {
				Class exClass = Class.forName("android.media.ExifInterface");

				Constructor c = exClass.getConstructor(new Class[]{String.class});
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method getAttributeInt = exClass.getMethod("getAttributeInt", new Class[]{String.class, Integer.TYPE});
				Integer it = (Integer) getAttributeInt.invoke(exInstance, "Orientation", 1);
				orientation = it;
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
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
			String size = AndroidUtils.formatSize(file.length());
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
			StringBuilder additional = new StringBuilder("");
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

	}

	private static void initializeRemoteControlRegistrationMethods() {
		try {
			// API 8
			if (mRegisterMediaButtonEventReceiver == null) {
				mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod("registerMediaButtonEventReceiver",
						new Class[]{ComponentName.class});
			}
			if (mUnregisterMediaButtonEventReceiver == null) {
				mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver",
						new Class[]{ComponentName.class});
			}
			/* success, this device will take advantage of better remote */
			/* control event handling */
		} catch (NoSuchMethodException nsme) {
			/* failure, still using the legacy behavior, but this app */
			/* is future-proof! */
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		this.app = app;
		OsmandSettings settings = app.getSettings();
		ApplicationMode.regWidgetVisibility("audionotes", (ApplicationMode[]) null);
		AV_EXTERNAL_RECORDER = settings.registerBooleanPreference("av_external_recorder", false).makeGlobal();
		AV_EXTERNAL_PHOTO_CAM = settings.registerBooleanPreference("av_external_cam", true).makeGlobal();
		AV_VIDEO_FORMAT = settings.registerIntPreference("av_video_format", VIDEO_OUTPUT_MP4).makeGlobal();
		AV_VIDEO_QUALITY = settings.registerIntPreference("av_video_quality", VIDEO_QUALITY_DEFAULT).makeGlobal();
		AV_AUDIO_FORMAT = settings.registerIntPreference("av_audio_format", AUDIO_FORMAT_DEFAULT).makeGlobal();
		AV_AUDIO_BITRATE = settings.registerIntPreference("av_audio_bitrate", AUDIO_BITRATE_DEFAULT).makeGlobal();
		AV_DEFAULT_ACTION = settings.registerIntPreference("av_default_action", AV_DEFAULT_ACTION_CHOOSE).makeGlobal();
		// camera picture size:
		AV_CAMERA_PICTURE_SIZE = settings.registerIntPreference("av_camera_picture_size", AV_PHOTO_SIZE_DEFAULT).makeGlobal();
		// camera focus type:
		AV_CAMERA_FOCUS_TYPE = settings.registerIntPreference("av_camera_focus_type", AV_CAMERA_FOCUS_AUTO).makeGlobal();
		// camera sound:
		AV_PHOTO_PLAY_SOUND = settings.registerBooleanPreference("av_photo_play_sound", true).makeGlobal();

		SHOW_RECORDINGS = settings.registerBooleanPreference("show_recordings", true).makeGlobal();

		AV_RECORDER_SPLIT = settings.registerBooleanPreference("av_recorder_split", false).makeGlobal();
		AV_RS_CLIP_LENGTH = settings.registerIntPreference("av_rs_clip_length", CLIP_LENGTH_DEFAULT).makeGlobal();
		AV_RS_STORAGE_SIZE = settings.registerIntPreference("av_rs_storage_size", STORAGE_SIZE_DEFAULT).makeGlobal();
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.audionotes_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.audionotes_plugin_name);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/audio-video-notes-plugin.html";
	}

	@Override
	public boolean init(@NonNull final OsmandApplication app, Activity activity) {
		initializeRemoteControlRegistrationMethods();
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		if (am != null) {
			registerMediaListener(am);
		}
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		this.mapActivity = activity;
		if (audioNotesLayer != null) {
			activity.getMapView().removeLayer(audioNotesLayer);
		}
		audioNotesLayer = new AudioNotesLayer(activity, this);
		activity.getMapView().addLayer(audioNotesLayer, 3.5f);
		registerWidget(activity);
	}

	public CurrentRecording getCurrentRecording() {
		return currentRecording;
	}

	private void registerMediaListener(AudioManager am) {

		ComponentName receiver = new ComponentName(app.getPackageName(), MediaRemoteControlReceiver.class.getName());
		try {
			if (mRegisterMediaButtonEventReceiver == null) {
				return;
			}
			mRegisterMediaButtonEventReceiver.invoke(am, receiver);
		} catch (Exception ite) {
			log.error(ite.getMessage(), ite);
		}
	}

	private void unregisterMediaListener(AudioManager am) {
		ComponentName receiver = new ComponentName(app.getPackageName(), MediaRemoteControlReceiver.class.getName());
		try {
			if (mUnregisterMediaButtonEventReceiver == null) {
				return;
			}
			mUnregisterMediaButtonEventReceiver.invoke(am, receiver);
		} catch (Exception ite) {
			log.error(ite.getMessage(), ite);
		}
	}

	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
				if (itemId == R.string.layer_recordings) {
					SHOW_RECORDINGS.set(!SHOW_RECORDINGS.get());
					adapter.getItem(pos).setColorRes(SHOW_RECORDINGS.get() ?
							R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					updateLayers(mapView, mapActivity);
				}
				return true;
			}
		};
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.layer_recordings, app)
				.setId(RECORDING_LAYER)
				.setSelected(SHOW_RECORDINGS.get())
				.setIcon(R.drawable.ic_action_micro_dark)
				.setColor(SHOW_RECORDINGS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setPosition(12)
				.setListener(listener).createItem());
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude,
											  ContextMenuAdapter adapter, Object selectedObj) {
		if (isRecording()) {
			return;
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.recording_context_menu_arecord, app)
				.setIcon(R.drawable.ic_action_micro_dark)
				.setOrder(TAKE_AUDIO_NOTE_ITEM_ORDER)
				.setListener(new ContextMenuAdapter.ItemClickListener() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						recordAudio(latitude, longitude, mapActivity);
						return true;
					}
				})
				.createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.recording_context_menu_vrecord, app)
				.setIcon(R.drawable.ic_action_video_dark)
				.setOrder(TAKE_VIDEO_NOTE_ITEM_ORDER)
				.setListener(new ItemClickListener() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						recordVideo(latitude, longitude, mapActivity, false);
						return true;
					}
				})
				.createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.recording_context_menu_precord, app)
				.setIcon(R.drawable.ic_action_photo_dark)
				.setOrder(TAKE_PHOTO_NOTE_ITEM_ORDER)
				.setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						takePhoto(latitude, longitude, mapActivity, false, false);
						return true;
					}

				})
				.createItem());
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (isActive()) {
			if (SHOW_RECORDINGS.get()) {
				if (audioNotesLayer == null) {
					registerLayers(activity);
				} else if (!mapView.getLayers().contains(audioNotesLayer)) {
					mapView.addLayer(audioNotesLayer, 3.5f);
				}
			} else if (audioNotesLayer != null) {
				mapView.removeLayer(audioNotesLayer);
			}
			if (recordControl == null) {
				registerWidget(activity);
			}
		} else {
			if (audioNotesLayer != null) {
				mapView.removeLayer(audioNotesLayer);
				audioNotesLayer = null;
			}
			MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
			if (recordControl != null && mapInfoLayer != null) {
				mapInfoLayer.removeSideWidget(recordControl);
				recordControl = null;
				mapInfoLayer.recreateControls();
			}
			recordControl = null;
		}
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			recordControl = new TextInfoWidget(activity);
			if (mediaRec != null && mediaRecFile != null) {
				updateRecordControl(activity, mediaRecFile);
			} else {
				recordControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.monitoring_rec_inactive));
				setRecordListener(recordControl, activity);
			}
			mapInfoLayer.registerSideWidget(recordControl, R.drawable.ic_action_micro_dark,
					R.string.map_widget_av_notes, "audionotes", false, 32);
			mapInfoLayer.recreateControls();
		}
	}

	private void setRecordListener(final TextInfoWidget recordPlaceControl, final MapActivity mapActivity) {
		recordPlaceControl.setText(app.getString(R.string.shared_string_control_start), "");
		updateWidgetIcon(recordPlaceControl);
		recordPlaceControl.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				defaultAction(mapActivity);
			}
		});
	}

	private void updateWidgetIcon(final TextInfoWidget recordPlaceControl) {
		recordPlaceControl.setIcons(R.drawable.widget_icon_av_inactive_day,
				R.drawable.widget_icon_av_inactive_night);
		if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_VIDEO) {
			recordPlaceControl.setIcons(R.drawable.widget_av_video_day,
					R.drawable.widget_av_video_night);
		} else if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_TAKEPICTURE) {
			recordPlaceControl.setIcons(R.drawable.widget_av_photo_day,
					R.drawable.widget_av_photo_night);
		} else if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_AUDIO) {
			recordPlaceControl.setIcons(R.drawable.widget_av_audio_day,
					R.drawable.widget_av_audio_night);
		}
	}

	public void defaultAction(final MapActivity mapActivity) {
		final Location loc = app.getLocationProvider().getLastKnownLocation();
		// double lat = mapActivity.getMapView().getLatitude();
		// double lon = mapActivity.getMapView().getLongitude();
		if (loc == null) {
			Toast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
			return;
		}
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		int action = AV_DEFAULT_ACTION.get();
		if (action == AV_DEFAULT_ACTION_CHOOSE) {
			chooseDefaultAction(lat, lon, mapActivity);
		} else {
			takeAction(mapActivity, lon, lat, action);
		}
	}

	private void chooseDefaultAction(final double lat, final double lon, final MapActivity mapActivity) {
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder ab = new AlertDialog.Builder(UiUtilities.getThemedContext(mapActivity, nightMode));
		ab.setItems(
				new String[] {mapActivity.getString(R.string.recording_context_menu_arecord),
						mapActivity.getString(R.string.recording_context_menu_vrecord),
						mapActivity.getString(R.string.recording_context_menu_precord),}, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int action = which == 0 ? AV_DEFAULT_ACTION_AUDIO : (which == 1 ? AV_DEFAULT_ACTION_VIDEO
								: AV_DEFAULT_ACTION_TAKEPICTURE);
						takeAction(mapActivity, lon, lat, action);

					}
				});
		ab.show();
	}

	private void takeAction(final MapActivity mapActivity, double lon, double lat, int action) {
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

	public void captureImage(double lat, double lon, final MapActivity mapActivity) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Uri fileUri = AndroidUtils.getUriForFile(mapActivity, getBaseFileName(lat, lon, app, IMG_EXTENSION));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			intent.setClipData(ClipData.newRawUri("", fileUri));
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		// start the image capture Intent
		mapActivity.startActivityForResult(intent, 105);
	}

	public void captureVideoExternal(double lat, double lon, final MapActivity mapActivity) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

		String ext = MPEG4_EXTENSION;
//		if (AV_VIDEO_FORMAT.get() == VIDEO_OUTPUT_3GP) {
//			ext = THREEGP_EXTENSION;
//		}
		Uri fileUri = AndroidUtils.getUriForFile(mapActivity, getBaseFileName(lat, lon, app, ext));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			intent.setClipData(ClipData.newRawUri("", fileUri));
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
		// start the video capture Intent
		mapActivity.startActivityForResult(intent, 205);
	}


	@Override
	public void mapActivityScreenOff(MapActivity activity) {
		stopRecording(activity, false);
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		this.mapActivity = activity;
		((AudioManager) activity.getSystemService(Context.AUDIO_SERVICE)).registerMediaButtonEventReceiver(
				new ComponentName(activity, MediaRemoteControlReceiver.class));
		if (runAction != -1) {
			takeAction(activity, actionLon, actionLat, runAction);
			runAction = -1;
		}
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
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

	public MapActivity getMapActivity() {
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

	public void recordVideo(final double lat, final double lon, final MapActivity mapActivity,
			final boolean forceExternal) {
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
			ActivityCompat.requestPermissions(mapActivity, new String[] { Manifest.permission.CAMERA,
					Manifest.permission.RECORD_AUDIO }, CAMERA_FOR_VIDEO_REQUEST_CODE);
		}

	}

	public void recordVideoCamera(final double lat, final double lon, final MapActivity mapActivity) {
		final CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
		final Camera.Size mPreviewSize = getPreviewSize();

		final SurfaceView view;
		if (mPreviewSize != null) {
			view = recordingMenu.prepareSurfaceView(mPreviewSize.width, mPreviewSize.height);
		} else {
			view = recordingMenu.prepareSurfaceView();
		}
		view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		view.getHolder().addCallback(new Callback() {

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {

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

				final File f = getBaseFileName(lat, lon, app, MPEG4_EXTENSION);
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
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			}
		});
		recordingMenu.show();
	}

	private void initMediaRecorder(MediaRecorder mr, CamcorderProfile p, File f) {
		mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		giveMediaRecorderHintRotatedScreen(mapActivity, mr);
		//mr.setPreviewDisplay(holder.getSurface());

		mr.setProfile(p);
		mr.setOutputFile(f.getAbsolutePath());
	}

	private void giveMediaRecorderHintRotatedScreen(final MapActivity mapActivity, final MediaRecorder mr) {
		if (Build.VERSION.SDK_INT >= 9) {
			try {
				Method m = mr.getClass().getDeclaredMethod("setOrientationHint", Integer.TYPE);
				Display display = ((WindowManager) mapActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				if (display.getOrientation() == Surface.ROTATION_0) {
					m.invoke(mr, 90);
				} else if (display.getOrientation() == Surface.ROTATION_270) {
					m.invoke(mr, 180);
				} else if (display.getOrientation() == Surface.ROTATION_180) {
					m.invoke(mr, 270);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
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
		final CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
		final Camera.Size mPreviewSize;
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
			} catch (IllegalStateException e) {
				e.printStackTrace();
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
					final File f = getBaseFileName(latLon.getLatitude(), latLon.getLongitude(), app, MPEG4_EXTENSION);

					cam.unlock();
					mr.setCamera(cam);
					initMediaRecorder(mr, CamcorderProfile.get(AV_VIDEO_QUALITY.get()), f);
					mr.prepare();
					mr.start();
					mediaRec = mr;
					mediaRecFile = f;

				} catch (Exception e) {
					Toast.makeText(app, e.getMessage(), Toast.LENGTH_LONG).show();
					e.printStackTrace();
					res = false;
				}
			}
		}
		return res;
	}

	public void recordAudio(double lat, double lon, final MapActivity mapActivity) {
		if (ActivityCompat.checkSelfPermission(mapActivity, Manifest.permission.RECORD_AUDIO)
				== PackageManager.PERMISSION_GRANTED) {

			initRecMenu(AVActionType.REC_AUDIO, lat, lon);
			MediaRecorder mr = new MediaRecorder();
			final File f = getBaseFileName(lat, lon, app, THREEGP_EXTENSION);
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
        AudioManager am = (AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        int voiceGuidanceOutput = app.getSettings().AUDIO_STREAM_GUIDANCE.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
                am.adjustStreamVolume(voiceGuidanceOutput, AudioManager.ADJUST_MUTE, 0);
        } else {
            am.setStreamMute(AudioManager.STREAM_MUSIC, true);
            if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
                am.setStreamMute(voiceGuidanceOutput, true);
        }
    }

    private void unmuteStreamMusicAndOutputGuidance() {
        AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        int voiceGuidanceOutput = app.getSettings().AUDIO_STREAM_GUIDANCE.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
                am.adjustStreamVolume(voiceGuidanceOutput, AudioManager.ADJUST_UNMUTE, 0);
        } else {
            am.setStreamMute(AudioManager.STREAM_MUSIC, false);
            if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC)
                am.setStreamMute(voiceGuidanceOutput, false);
        }
    }

	public void takePhoto(final double lat, final double lon, final MapActivity mapActivity,
						  final boolean forceInternal, final boolean forceExternal) {
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

	private void takePhotoWithCamera(final double lat, final double lon,
									 final MapActivity mapActivity) {
		try {
			lastTakingPhoto = getBaseFileName(lat, lon, app, IMG_EXTENSION);
			final Camera.Size mPreviewSize;
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
			final Camera.Size selectedCamPicSize = psps.get(camPicSizeIndex);
			if (mSupportedPreviewSizes != null) {
				int width = selectedCamPicSize.width;
				int height = selectedCamPicSize.height;
				mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
			} else {
				mPreviewSize = null;
			}
			final SurfaceView view;
			if (mPreviewSize != null) {
				view = recordingMenu.prepareSurfaceView(mPreviewSize.width, mPreviewSize.height);
			} else {
				view = recordingMenu.prepareSurfaceView();
			}
			view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			view.getHolder().addCallback(new Callback() {

				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
				}

				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					try {
						// load sound befor shot
						if (AV_PHOTO_PLAY_SOUND.get()) {
							if (sp == null)
								sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
							if (shotId == 0) {
								try {
									AssetFileDescriptor assetFileDescriptor = app.getAssets().openFd("sounds/camera_click.ogg");
									shotId = sp.load(assetFileDescriptor, 1);
									assetFileDescriptor.close();
								} catch (Exception e) {
									log.error("cannot get shotId for sounds/camera_click.ogg");
								}
							}
						}

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
						e.printStackTrace();
					}
				}

				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				}
			});

			recordingMenu.show();

		} catch (RuntimeException e) {
			logErr(e);
			closeCamera();
		}
	}


	private void internalShoot() {
		getMapActivity().getMyApplication().runInUIThread(new Runnable() {
			@Override
			public void run() {
				if (!autofocus) {
					cam.takePicture(null, null, new JpegPhotoHandler());
				} else {
					cam.autoFocus(new Camera.AutoFocusCallback() {
						@Override
						public void onAutoFocus(boolean success, Camera camera) {
							try {
								cam.takePicture(null, null, new JpegPhotoHandler());
							} catch (Exception e) {
								logErr(e);
								closeRecordingMenu();
								closeCamera();
								finishRecording();
								e.printStackTrace();
							}
						}
					});
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
					cam.takePicture(null, null, new JpegPhotoHandler());
				} catch (RuntimeException e) {
					closeRecordingMenu();
					closeCamera();
					finishRecording();
				}
			}
		}
	}

	public void takePhotoExternal(double lat, double lon, final MapActivity mapActivity) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final File f = getBaseFileName(lat, lon, app, IMG_EXTENSION);
		lastTakingPhoto = f;
		Uri uri = AndroidUtils.getUriForFile(mapActivity, f);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
			takePictureIntent.setClipData(ClipData.newRawUri("", uri));
		}
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		try {
			mapActivity.startActivityForResult(takePictureIntent, 205);
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

			final CamcorderProfile p = CamcorderProfile.get(AV_VIDEO_QUALITY.get());
			double bitrate = (((p.videoBitRate + p.audioBitRate) / 8f) * 60f) / (1 << 30); // gigabytes per minute
			double clipSpace = bitrate * AV_RS_CLIP_LENGTH.get();
			double storageSize = AV_RS_STORAGE_SIZE.get();

			double availableSpace = storageSize;
			File dir = app.getAppPath("").getParentFile();
			if (dir.canRead()) {
				StatFs fs = new StatFs(dir.getAbsolutePath());
				availableSpace = (double) (fs.getAvailableBlocks()) * fs.getBlockSize() / (1 << 30) - clipSpace;
			}

			if (usedSpace + clipSpace > storageSize || clipSpace > availableSpace) {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File lhs, File rhs) {
						return lhs.lastModified() < rhs.lastModified() ? -1 : (lhs.lastModified() == rhs.lastModified() ? 0 : 1);
					}
				});
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
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							mapActivity.refreshMap();
						}
					}, 20);
				}
			}
		}
	}

	private void runMediaRecorder(final MapActivity mapActivity, MediaRecorder mr, final File f) throws IOException {
		mr.prepare();
		mr.start();
		mediaRec = mr;
		mediaRecFile = f;

		recordingMenu.show();
		updateRecordControl(mapActivity, f);
	}

	private void updateRecordControl(final MapActivity mapActivity, final File f) {
		recordControl.setText(app.getString(R.string.shared_string_control_stop), "");
		recordControl.setIcons(R.drawable.widget_icon_av_active, R.drawable.widget_icon_av_active);
		recordControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopRecording(mapActivity, false);
			}
		});
	}

	public void stopRecording(final MapActivity mapActivity, boolean restart) {
		if (!recordingDone) {
			if (!restart || !stopMediaRecording(true)) {
				recordingDone = true;
				stopMediaRecording(false);
				if (recordControl != null) {
					setRecordListener(recordControl, mapActivity);
				}
				SHOW_RECORDINGS.set(true);
				mapActivity.getMapView().refreshMap();
				updateWidgetIcon(recordControl);
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
	public void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabItem> mTabs, Intent intent) {
		mTabs.add(favoritesActivity.getTabIndicator(NOTES_TAB, NotesFragment.class));
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
				final Recording recordingForMenu = r;
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						updateContextMenu(recordingForMenu);
					}
				}, 200);
			}
		}

		return true;
	}

	@Override
	public void disable(OsmandApplication app) {
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		if (am != null) {
			unregisterMediaListener(am);
		}
	}

	@Override
	public List<String> indexingFiles(IProgress progress) {
		return indexingFiles(progress, true, false);
	}

	public List<String> indexingFiles(IProgress progress, boolean reIndexAndKeepOld, boolean registerNew) {
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
						&& OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
					String name = f.getName();
					SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
					savingTrackHelper.insertPointData(rec.lat, rec.lon, System.currentTimeMillis(), null, name, null, 0);
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
			mapActivity.getMapView().refreshMap();
		}
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAudioVideoActivity.class;
	}


	@Override
	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 205 || requestCode == 105) {
			indexingFiles(null, true, true);
		}
	}

	public boolean onMapActivityKeyEvent(KeyEvent key) {
		if (KeyEvent.KEYCODE_CAMERA == key.getKeyCode()) {
			defaultAction(mapActivity);
			return true;
		}
		return false;
	}


	public Collection<Recording> getAllRecordings() {
		return recordingByFileName.values();
	}

	protected Recording[] getRecordingsSorted() {
		checkRecordings();
		Collection<Recording> allObjects = getAllRecordings();
		Recording[] res = allObjects.toArray(new Recording[allObjects.size()]);
		Arrays.sort(res, new Comparator<Recording>() {

			@Override
			public int compare(Recording object1, Recording object2) {
				long l1 = object1.file.lastModified();
				long l2 = object2.file.lastModified();
				return l1 < l2 ? 1 : -1;
			}
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
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.getContextMenu().updateMenuUI();
				}
			}
		});
	}

	private void closeRecordingMenu() {
		if (mapActivity != null) {
			mapActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (recordingMenu != null) {
						recordingMenu.hide();
						recordingMenu = null;
					}
					restoreScreenOrientation();
				}
			});
		}
	}

	public void playRecording(final @NonNull Context ctx, final @NonNull Recording r) {
		if (r.isVideo()) {
			Intent vint = new Intent(Intent.ACTION_VIEW);
			vint.setDataAndType(AndroidUtils.getUriForFile(ctx, r.file), "video/*");
			vint.setFlags(0x10000000);
			vint.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			try {
				ctx.startActivity(vint);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		} else if (r.isPhoto()) {
			Intent vint = new Intent(Intent.ACTION_VIEW);
			vint.setDataAndType(AndroidUtils.getUriForFile(ctx, r.file), "image/*");
			vint.setFlags(0x10000000);
			vint.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			ctx.startActivity(vint);
			return;
		}

		if (isPlaying()) {
			stopPlaying();
		}
		recordingPlaying = r;
		player = new MediaPlayer();
		try {
			player.setDataSource(r.file.getAbsolutePath());
			player.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
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
				}
			});
			player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					recordingPlaying = null;
				}
			});
			player.prepareAsync();
		} catch (Exception e) {
			logErr(e);
		}
	}

	@Override
	public boolean mapActivityKeyUp(MapActivity mapActivity, int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			defaultAction(mapActivity);
			return true;
		}
		return false;
	}

	@TargetApi(Build.VERSION_CODES.M)
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
		/*
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !mapActivity.isDestroyed()) {
			takeAction(mapActivity, actionLon, actionLat, runAction);
			runAction = -1;
		}
		*/
	}

	public class JpegPhotoHandler implements PictureCallback {

		public JpegPhotoHandler() {
		}

		@Override
		public void onPictureTaken(final byte[] data, Camera camera) {
			photoJpegData = data;

			if (AV_PHOTO_PLAY_SOUND.get()) {
				if (sp != null && shotId != 0) {
					sp.play(shotId, 0.7f, 0.7f, 0, 0, 1);
				}
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
				e.printStackTrace();
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
	public int getAssetResourceName() {
		return R.drawable.audio_video_notes;
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashAudioVideoNotesFragment.FRAGMENT_DATA;
	}
}
