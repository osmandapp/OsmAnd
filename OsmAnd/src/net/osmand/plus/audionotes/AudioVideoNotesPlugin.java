package net.osmand.plus.audionotes;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.LocalIndexesActivity;
import net.osmand.plus.activities.LocalIndexesActivity.LoadLocalIndexTask;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.StackWidgetView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final String ID = "osmand.audionotes";
	private static final String THREEGP_EXTENSION = "3gp";
	private static final String MPEG4_EXTENSION = "mp4";
	private static final String IMG_EXTENSION = "jpg";
	private static final Log log = PlatformUtil.getLog(AudioVideoNotesPlugin.class);
	private static Method mRegisterMediaButtonEventReceiver;
	private static Method mUnregisterMediaButtonEventReceiver;
	private OsmandApplication app;
	private TextInfoWidget recordControl;
	
	public final CommonPreference<Boolean> AV_EXTERNAL_RECORDER ;
	public final CommonPreference<Boolean> AV_EXTERNAL_PHOTO_CAM ;
	
	public static final int VIDEO_OUTPUT_MP4 = 0;
	public static final int VIDEO_OUTPUT_3GP = 1;
	public final CommonPreference<Integer> AV_VIDEO_FORMAT;
	
	public static final int AV_DEFAULT_ACTION_AUDIO = 0;
	public static final int AV_DEFAULT_ACTION_VIDEO = 1;
	public static final int AV_DEFAULT_ACTION_TAKEPICTURE = 2;
	public static final int AV_DEFAULT_ACTION_CHOOSE = -1;
	public final CommonPreference<Integer> AV_DEFAULT_ACTION;
	
	public final OsmandPreference<Boolean> SHOW_RECORDINGS ;

	
	
	private DataTileManager<Recording> recordings = new DataTileManager<AudioVideoNotesPlugin.Recording>(14);
	private Map<String, Recording> recordingByFileName = new LinkedHashMap<String, Recording>();
	private AudioNotesLayer audioNotesLayer;
	private MapActivity activity;
	private MediaRecorder mediaRec;
	private File lastTakingPhoto;
	
	public static class Recording {
		public Recording(File f) {
			this.file = f;
		}
		public File file;
		public String name;
		
		private double lat;
		private double lon;
		private long duration = -1;
		private boolean available = true;
		
		public double getLatitude(){
			return lat;
		}
		
		public double getLongitude(){
			return lon;
		}
		
		private void updateInternalDescription(){
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

		public boolean isPhoto() {
			return file.getName().endsWith(IMG_EXTENSION);
		}
		
		public boolean isVideo() {
			return file.getName().endsWith(MPEG4_EXTENSION) || file.getName().endsWith(THREEGP_EXTENSION);
		}
		
		public boolean isAudio() {
			return file.getName().endsWith(MPEG4_EXTENSION) || file.getName().endsWith(THREEGP_EXTENSION);
		}

		private String convertDegToExifRational(double l) {
			if(l < 0){
				l = -l;
			}
			String s = ((int) l) + "/1,"; //degrees
			l = (l - ((int) l)) * 60.0;
			s+= (int) l + "/1,"; //minutes
			l = (l - ((int) l)) * 60000.0;
			s+= (int) l + "/1000"; //seconds
			//log.info("deg rational: " + s);
			return s;
		}
		
		@SuppressWarnings("rawtypes")
		public void updatePhotoInformation(double lat, double lon, double rot) throws IOException {
			try {
				Class exClass = Class.forName("android.media.ExifInterface");
				
				Constructor c = exClass.getConstructor(new Class[] { String.class });
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method setAttribute = exClass.getMethod("setAttribute",
				           new Class[] { String.class, String.class } );
				setAttribute.invoke(exInstance,"GPSLatitude", convertDegToExifRational(lat));
				setAttribute.invoke(exInstance,"GPSLatitudeRef", lat > 0 ?"N" :"S");
				setAttribute.invoke(exInstance,"GPSLongitude", convertDegToExifRational(lon));
				setAttribute.invoke(exInstance,"GPSLongitudeRef", lon > 0?"E" : "W");
				if(!Double.isNaN(rot)){
					setAttribute.invoke(exInstance,"GPSImgDirectionRef", "T");
					String rotString = (int) (rot * 100.0) + "/100";
					setAttribute.invoke(exInstance,"GPSImgDirection", rotString);
				}
				Method saveAttributes = exClass.getMethod("saveAttributes",
				           new Class[] {} );
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

				Constructor c = exClass.getConstructor(new Class[] { String.class });
				Object exInstance = c.newInstance(file.getAbsolutePath());
				Method getAttributeInt = exClass.getMethod("getAttributeInt",
				           new Class[] { String.class, Integer.TYPE} );
				Integer it = (Integer) getAttributeInt.invoke(exInstance, "Orientation", new Integer(1));
				orientation = it.intValue();
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
		
		public String getDescription(Context ctx){
			String nm = name == null? "" : name ;
			if(isPhoto()){
				return ctx.getString(R.string.recording_photo_description, nm, 
						Algorithms.formatDateTime(file.lastModified())).trim();
			}
			updateInternalDescription();
			return ctx.getString(R.string.recording_description, nm, getDuration(ctx), 
					Algorithms.formatDateTime(file.lastModified())).trim();
		}
		
		public String getSmallDescription(Context ctx){
			String nm = name == null? "" : name ;
			if(isPhoto()){
				return ctx.getString(R.string.recording_photo_description, nm,
						Algorithms.formatDateTime(file.lastModified())).trim();
			}
			return ctx.getString(R.string.recording_description, nm, "", 
					Algorithms.formatDateTime(file.lastModified())).trim();
		}

		private String getDuration(Context ctx) {
			String additional = "";
			if(duration > 0) {
				int d = (int) (duration / 1000);
				additional += Algorithms.formatDuration(d);
			}
			if(!available) {
				additional += "("+ctx.getString(R.string.recording_unavailable)+")";
			}
			return additional;
		}

		
	}
	
	private static void initializeRemoteControlRegistrationMethods() {
		   try {
		      if (mRegisterMediaButtonEventReceiver == null) {
		         mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "registerMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      if (mUnregisterMediaButtonEventReceiver == null) {
		         mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "unregisterMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      /* success, this device will take advantage of better remote */
		      /* control event handling                                    */
		   } catch (NoSuchMethodException nsme) {
		      /* failure, still using the legacy behavior, but this app    */
		      /* is future-proof!                                          */
		   }
		}

	
	@Override
	public String getId() {
		return ID;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		this.app = app;
		OsmandSettings settings = app.getSettings();
		AV_EXTERNAL_RECORDER = settings.registerBooleanPreference("av_external_recorder", false).makeGlobal();
		AV_EXTERNAL_PHOTO_CAM = settings.registerBooleanPreference("av_external_cam", true).makeGlobal();
		AV_VIDEO_FORMAT = settings.registerIntPreference("av_video_format", VIDEO_OUTPUT_MP4).makeGlobal();
		AV_DEFAULT_ACTION = settings.registerIntPreference("av_default_action", AV_DEFAULT_ACTION_CHOOSE).makeGlobal();
		SHOW_RECORDINGS = settings.registerBooleanPreference("show_recordings", true).makeGlobal();
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
	public boolean init(final OsmandApplication app) {
		initializeRemoteControlRegistrationMethods();
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		if(am != null){
			registerMediaListener(am);
		}
		return true;
	}
	
	
	

	@Override
	public void registerLayers(MapActivity activity) {
		this.activity = activity;
		if(audioNotesLayer != null) {
			activity.getMapView().removeLayer(audioNotesLayer);
		}
		audioNotesLayer = new AudioNotesLayer(activity, this);
		activity.getMapView().addLayer(audioNotesLayer, 3.5f);
		registerWidget(activity);
	}


	private void registerMediaListener(AudioManager am) {
		
		ComponentName receiver = new ComponentName(app.getPackageName(), 
				MediaRemoteControlReceiver.class.getName());
		try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(am,
                    receiver);
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
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.layer_recordings) {
					dialog.dismiss();
					SHOW_RECORDINGS.set(!SHOW_RECORDINGS.get());
					updateLayers(mapView, mapActivity);
				}
			}
		};
		adapter.registerSelectedItem(R.string.layer_recordings, SHOW_RECORDINGS.get()? 1 : 0, R.drawable.list_activities_rec_layer2, listener, 5);
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			Object selectedObj) {
		adapter.registerItem(R.string.recording_context_menu_arecord, R.drawable.list_activities_audio_note, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				recordAudio(latitude, longitude, mapActivity);
			}
		}, 6);
		adapter.registerItem(R.string.recording_context_menu_vrecord, R.drawable.list_activities_video_note, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				recordVideo(latitude, longitude, mapActivity);
			}
		}, 7);
		adapter.registerItem(R.string.recording_context_menu_precord, R.drawable.list_activities_photo, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				takePhoto(latitude, longitude, mapActivity);
			}

		}, 8);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(SHOW_RECORDINGS.get()) {
			if(audioNotesLayer == null) {
				registerLayers(activity);
			} else if(!mapView.getLayers().contains(audioNotesLayer)) {
				mapView.addLayer(audioNotesLayer, 3.5f);
			}
		} else if(audioNotesLayer != null){
			mapView.removeLayer(audioNotesLayer);
		}
	}
	
	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null ) {
			recordControl = new TextInfoWidget(activity, 0, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			recordControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.monitoring_rec_inactive));
			setRecordListener(recordControl, activity);
			mapInfoLayer.getMapInfoControls().registerSideWidget(recordControl,
					R.drawable.widget_icon_av_inactive, R.string.map_widget_av_notes, "audionotes", false,
					EnumSet.allOf(ApplicationMode.class),
					EnumSet.noneOf(ApplicationMode.class), 22);
			mapInfoLayer.recreateControls();
		}
	}

	private void setRecordListener(final TextInfoWidget recordPlaceControl, final MapActivity mapActivity) {
		recordPlaceControl.setText(app.getString(R.string.av_control_start), "");
		updateWidgetIcon(recordPlaceControl);
		recordPlaceControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				defaultAction(mapActivity);
			}
		});
	}


	private void updateWidgetIcon(final TextInfoWidget recordPlaceControl) {
		recordPlaceControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.widget_icon_av_inactive));
		if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_VIDEO) {
			recordPlaceControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.widget_icon_video));
		} else if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_TAKEPICTURE) {
			recordPlaceControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.widget_icon_photo));
		} else if (AV_DEFAULT_ACTION.get() == AV_DEFAULT_ACTION_AUDIO) {
			recordPlaceControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.widget_icon_audio));
		}
	}
	
	private void defaultAction(final MapActivity mapActivity) {
		final Location loc = app.getLocationProvider().getLastKnownLocation();
		// double lat = mapActivity.getMapView().getLatitude();
		// double lon = mapActivity.getMapView().getLongitude();
		if (loc == null) {
			AccessibleToast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
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
		AccessibleAlertBuilder ab = new AccessibleAlertBuilder(mapActivity);
		ab.setItems(new String[] {
				mapActivity.getString(R.string.recording_context_menu_arecord),
				mapActivity.getString(R.string.recording_context_menu_vrecord),
				mapActivity.getString(R.string.recording_context_menu_precord),
		}, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int action = which == 0 ? AV_DEFAULT_ACTION_AUDIO : 
					(which == 1 ? AV_DEFAULT_ACTION_VIDEO:AV_DEFAULT_ACTION_TAKEPICTURE);
				takeAction(mapActivity, lon, lat, action);
				
			}
		});
		ab.show();
	}


	private void takeAction(final MapActivity mapActivity, double lon, double lat, int action) {
		if (action == AV_DEFAULT_ACTION_VIDEO) {
			recordVideo(lat, lon, mapActivity);
		} else if (action == AV_DEFAULT_ACTION_TAKEPICTURE) {
			takePhoto(lat, lon, mapActivity);
		} else if (action == AV_DEFAULT_ACTION_AUDIO) {
			recordAudio(lat, lon, mapActivity);
		}
	}
	
	
	private File getBaseFileName(double lat, double lon, OsmandApplication app, String ext) {
		String basename = MapUtils.createShortLocString(lat, lon, 15);
		int k = 1;
		File f = app.getAppPath(IndexConstants.AV_INDEX_DIR);
		f.mkdirs();
		File fl;
		do {
			fl = new File(f, basename + "-" + (k++) + "." + ext);
		} while(fl.exists());
		return fl;
	}
	
	public void captureImage(double lat, double lon, final MapActivity mapActivity) {
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    Uri fileUri = Uri.fromFile(getBaseFileName(lat, lon, app, IMG_EXTENSION));
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
	    // start the image capture Intent
	    mapActivity.startActivityForResult(intent, 105);
	}
	
	public void captureVideoExternal(double lat, double lon, final MapActivity mapActivity) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

		String ext = MPEG4_EXTENSION;
		if(AV_VIDEO_FORMAT.get() == VIDEO_OUTPUT_3GP ){
			ext = THREEGP_EXTENSION;
		}
		Uri fileUri = Uri.fromFile(getBaseFileName(lat, lon, app, ext));
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name

	    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
	    // start the video capture Intent
	    mapActivity.startActivityForResult(intent, 205);
	}
	
	@Override
	public void mapActivityPause(MapActivity activity) {
		stopRecording(activity);
	}
	
	public void recordVideo(final double lat, final double lon, final MapActivity mapActivity) {
		if(AV_EXTERNAL_RECORDER.get()) {
			captureVideoExternal(lat, lon, mapActivity);
		} else {
			recordVideoCamera(lat, lon, mapActivity);
		}
	}
	
	public void recordVideoCamera(final double lat, final double lon, final MapActivity mapActivity) {
		final Dialog dlg = new Dialog(mapActivity);
		SurfaceView view = new SurfaceView(dlg.getContext());
		view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		view.getHolder().addCallback(new Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				MediaRecorder mr = new MediaRecorder();
				String ext = MPEG4_EXTENSION;
				if(AV_VIDEO_FORMAT.get() == VIDEO_OUTPUT_3GP ){
					ext = THREEGP_EXTENSION;
				}
				final File f = getBaseFileName(lat, lon, app,ext );
				
				mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
				mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				if(AV_VIDEO_FORMAT.get() == VIDEO_OUTPUT_3GP ){
					mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				} else {
					mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				}
				
				giveMediaRecorderHintRotatedScreen(mapActivity, mr);
				mr.setPreviewDisplay(holder.getSurface());
				mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
				mr.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
				mr.setOutputFile(f.getAbsolutePath());
				try {
					runMediaRecorder(mapActivity, mr, f);
				} catch (Exception e) {
					logErr(e);
					return;
				}
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			}
		});
		dlg.setContentView(view);
		dlg.show();
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
		AccessibleToast.makeText(app, app.getString(R.string.recording_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		return;
	}

	protected Camera openCamera() {
		try {
			return Camera.open();
		} catch (Exception e ){
			logErr(e);
			return null;
		}
	}
	
	private void stopRecording(final MapActivity mapActivity) {
		if (mediaRec != null) {
			mediaRec.stop();
			mediaRec.release();
			mediaRec = null;
		}
		if (recordControl != null) {
			setRecordListener(recordControl, mapActivity);
		}
	}
	
	
	public void recordAudio(double lat, double lon, final MapActivity mapActivity) {
		MediaRecorder mr = new MediaRecorder();
		final File f = getBaseFileName(lat, lon, app, THREEGP_EXTENSION);
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mr.setOutputFile(f.getAbsolutePath());
		try {
			runMediaRecorder(mapActivity, mr, f);
		} catch (Exception e) {
			log.error("Error starting audio recorder ", e);
			AccessibleToast.makeText(app, app.getString(R.string.recording_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void takePhoto(final double lat, final double lon, final MapActivity mapActivity) {
		if (AV_EXTERNAL_PHOTO_CAM.get()) {
			takeIntentPhoto(lat, lon, mapActivity);
		} else {
			final Camera cam = openCamera();
			if (cam != null) {
				takePhotoWithCamera(lat, lon, mapActivity, cam);
			} else {
				takeIntentPhoto(lat, lon, mapActivity);
			}
		}
	}


	private void takePhotoWithCamera(final double lat, final double lon, final MapActivity mapActivity, final Camera cam) {
		try {
			final Dialog dlg = new Dialog(mapActivity);
			final File f = getBaseFileName(lat, lon, app, IMG_EXTENSION);
			lastTakingPhoto = f;
			SurfaceView view = new SurfaceView(dlg.getContext());
			view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			view.getHolder().addCallback(new Callback() {
				
				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
				}
				
				public void setCameraDisplayOrientation(android.hardware.Camera camera, Parameters parameters) {
					// android.hardware.Camera.CameraInfo info =
					// new android.hardware.Camera.CameraInfo();
					// android.hardware.Camera.getCameraInfo(cameraId, info);
					int rotation = mapActivity.getWindowManager().getDefaultDisplay().getRotation();
					int degrees = 0;
					switch (rotation) {
					case /*Surface.ROTATION_0*/ 0:
						degrees = 0;
						break;
					case /*Surface.ROTATION_90*/ 1:
						degrees = 90;
						break;
					case /*Surface.ROTATION_180*/ 2:
						degrees = 180;
						break;
					case /*Surface.ROTATION_270*/ 3:
						degrees = 270;
						break;
					}

					// if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					// result = (info.orientation + degrees) % 360;
					// result = (360 - result) % 360; // compensate the mirror
					// } else { // back-facing
					// result = (info.orientation - degrees + 360) % 360;
					// }
					// API 8
					// camera.setDisplayOrientation((90 + 360 - degrees) % 360);
					parameters.set("rotation", (90 + 360 - degrees) % 360);
				}
				
				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					try {
						Parameters parameters = cam.getParameters();
						boolean autofocus = true;
//						boolean autofocus = !Boolean.parseBoolean(parameters.get("auto-exposure-lock-supported"));
						parameters.setGpsLatitude(lat);
						parameters.setGpsLongitude(lon);
						if(autofocus) {
							parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
						} else {
//							parameters.setFocusMode(Parameters.FOCUS_MODE_FIXED);
//							parameters.set("auto-exposure-lock", "true");
						}
						parameters.setWhiteBalance(Parameters.WHITE_BALANCE_AUTO);
						parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);

						setCameraDisplayOrientation(cam, parameters);
						cam.setParameters(parameters);
						cam.setPreviewDisplay(holder);
						cam.startPreview();
						if (!autofocus) {
							printCamParams(parameters, !autofocus);
							cam.takePicture(null, null, new AudioVideoPhotoHandler(dlg, f));
						} else {
							cam.autoFocus(new AutoFocusCallback() {
								@Override
								public void onAutoFocus(boolean success, Camera camera) {
									cam.takePicture(null, null, new AudioVideoPhotoHandler(dlg, f));
								}
							});
						}
					} catch (Exception e) {
						logErr(e);
						cam.release();
						e.printStackTrace();
					}
				}

				private void printCamParams(Parameters parameters, boolean autoExposure) {
					log.info("Cam params auto exposure=" + autoExposure + 
							" focus_distances="+parameters.get("focus-distances"));
				}

				@Override
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				}
			});
			dlg.setContentView(view);
			dlg.show();
		} catch (RuntimeException e) {
			logErr(e);
			cam.release();
		}
	}


	private void takeIntentPhoto(double lat, double lon, final MapActivity mapActivity) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final File f = getBaseFileName(lat, lon, app, IMG_EXTENSION);
		lastTakingPhoto = f;
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
		try {
			mapActivity.startActivityForResult(takePictureIntent, 205);
		} catch (Exception e) {
			log.error("Error taking a picture ", e);
			AccessibleToast.makeText(app, app.getString(R.string.recording_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void runMediaRecorder(final MapActivity mapActivity, MediaRecorder mr, final File f) throws IOException {
		mr.prepare();
		mr.start();
		mediaRec = mr;
		recordControl.setText(app.getString(R.string.av_control_stop), "");
		recordControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.widget_icon_av_active));
		final MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
		final StackWidgetView par = mil.getRightStack();
		final boolean contains = par.getAllViews().contains(recordControl);
		if(!contains) {
			par.addStackView(recordControl);
			mapActivity.getMapView().refreshMap(true);
		}
		AccessibleToast.makeText(mapActivity, R.string.recording_is_recorded, Toast.LENGTH_LONG).show();
		recordControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!contains) {
					par.removeView(recordControl);
				}
				stopRecording(mapActivity);
				SHOW_RECORDINGS.set(true);
				indexFile(f);
				mapActivity.getMapView().refreshMap();
				updateWidgetIcon(recordControl);
			}
		});
	}
	
	public void indexFile(File f){
		boolean oldFileExist = recordingByFileName.containsKey(f.getName());
		if(oldFileExist){
			return;
		}
		Recording r = new Recording(f);
		String encodeName = f.getName();
		int i = encodeName.indexOf('-');
		if(i > 0) {
			encodeName = encodeName.substring(0, i);
		}
		i = encodeName.indexOf('.');
		if(i > 0) {
			encodeName = encodeName.substring(0, i);
		}
		r.file = f;
		LatLon l = MapUtils.decodeShortLocString(encodeName);
		r.lat = l.getLatitude();
		r.lon = l.getLongitude();
		Float heading = app.getLocationProvider().getHeading();
		if(lastTakingPhoto != null && lastTakingPhoto.getName().equals(f.getName()) && heading != null) {
			float rot = heading.floatValue();
			try {
				r.updatePhotoInformation(r.lat, r.lon, rot == 0 ? Double.NaN : rot);
			} catch (IOException e) {
				log.error("Error updating EXIF information " + e.getMessage(), e);
			}
			lastTakingPhoto = null;
		}
		recordings.registerObject(r.lat, r.lon, r);
		recordingByFileName.put(f.getName(), r);
	}

	@Override
	public void disable(OsmandApplication app) {
		AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		if(am != null){
			unregisterMediaListener(am);
		}
	}
	
	@Override
	public List<String> indexingFiles(IProgress progress) {
		return indexingFiles(progress, true);
	}
		
	public List<String> indexingFiles(IProgress progress, boolean reIndexAndKeepOld) {
		File avPath = app.getAppPath(IndexConstants.AV_INDEX_DIR);
		if (avPath.canRead()) {
			if(!reIndexAndKeepOld) {
				recordings.clear();
				recordingByFileName.clear();
			}
			File[] files = avPath.listFiles();
			if (files != null) {
				for (File f : files) {
					if(f.getName().endsWith(THREEGP_EXTENSION)
							|| f.getName().endsWith(MPEG4_EXTENSION)
							|| f.getName().endsWith(IMG_EXTENSION)) {
						indexFile(f);
					}
				}
			}
		}
		return null;
	}
	

	public DataTileManager<Recording> getRecordings() {
		return recordings;
	}
	
	private void checkRecordings(){
		Iterator<Recording> it = recordingByFileName.values().iterator();
		while(it.hasNext()){
			Recording r = it.next();
			if(!r.file.exists()) {
				it.remove();
				recordings.unregisterObject(r.lat, r.lon, r);
			}
		}
	}
	

	public void deleteRecording(Recording r) {
		recordings.unregisterObject(r.lat, r.lon, r);
		recordingByFileName.remove(r.file.getName());
		Algorithms.removeAllFiles(r.file);
		activity.getMapLayers().getContextMenuLayer().setLocation(null, "");
		activity.getMapView().refreshMap();
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		String[] entries;
		Integer[] intValues ;
		PreferenceGroup grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setSummary(R.string.av_settings_descr);
		grp.setTitle(R.string.av_settings);
		grp.setKey("av_settings");
		screen.addPreference(grp);

		entries = new String[] { app.getString(R.string.av_def_action_choose),
				app.getString(R.string.av_def_action_audio), app.getString(R.string.av_def_action_video),
				app.getString(R.string.av_def_action_picture)};
		intValues = new Integer[] {AV_DEFAULT_ACTION_CHOOSE, AV_DEFAULT_ACTION_AUDIO, AV_DEFAULT_ACTION_VIDEO,
				AV_DEFAULT_ACTION_TAKEPICTURE};
		ListPreference defAct = activity.createListPreference(AV_DEFAULT_ACTION, 
				entries, intValues, R.string.av_widget_action, R.string.av_widget_action_descr);
		grp.addPreference(defAct);
		
	
		
		grp.addPreference(activity.createCheckBoxPreference(AV_EXTERNAL_RECORDER, 
				R.string.av_use_external_recorder, R.string.av_use_external_recorder_descr));
		grp.addPreference(activity.createCheckBoxPreference(AV_EXTERNAL_PHOTO_CAM, 
				R.string.av_use_external_camera, R.string.av_use_external_camera_descr));
		
		entries = new String[] {"3GP", "MP4"};
		intValues = new Integer[] {VIDEO_OUTPUT_3GP, VIDEO_OUTPUT_MP4};
		ListPreference lp = activity.createListPreference(AV_VIDEO_FORMAT, 
				entries, intValues, R.string.av_video_format, R.string.av_video_format_descr);
		grp.addPreference(lp);
	}
	
	@Override
	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 205) {
			indexingFiles(null, true);
		}
	}
	
	public boolean onMapActivityKeyEvent(KeyEvent key){
		if(KeyEvent.KEYCODE_CAMERA == key.getKeyCode()) {
			defaultAction(activity);
			return true;
		}
		return false;
	}
	
	public class RecordingLocalIndexInfo extends LocalIndexInfo {

		private Recording rec;

		public RecordingLocalIndexInfo(Recording r) {
			super(LocalIndexType.AV_DATA, r.file, false);
			this.rec = r;
		}
		
		@Override
		public String getName() {
			return rec.getSmallDescription(app);
		}
		
	}
	
	@Override
	public void loadLocalIndexes(List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {
		List<LocalIndexInfo> progress = new ArrayList<LocalIndexInfo>();
		for (Recording r : getRecordingsSorted()) {
			LocalIndexInfo info = new RecordingLocalIndexInfo(r);
			result.add(info);
			progress.add(info);
			if (progress.size() > 7) {
				loadTask.loadFile(progress.toArray(new LocalIndexInfo[progress.size()]));
				progress.clear();
			}

		}
		if (!progress.isEmpty()) {
			loadTask.loadFile(progress.toArray(new LocalIndexInfo[progress.size()]));
		}
	}
	
	@Override
	public void updateLocalIndexDescription(LocalIndexInfo info) {
		if (info instanceof RecordingLocalIndexInfo) {
			info.setDescription(((RecordingLocalIndexInfo) info).rec.getDescription(app));
		}
	}
	
	@Override
	public void contextMenuLocalIndexes(final LocalIndexesActivity la, LocalIndexInfo info, ContextMenuAdapter adapter) {
		if (info.getType() == LocalIndexType.AV_DATA) {
			final RecordingLocalIndexInfo ri = (RecordingLocalIndexInfo) info;
			OnContextMenuClick listener = new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					playRecording(la, ri.rec);
				}
			};
			if(ri.rec.isPhoto()) {
				adapter.registerItem(R.string.recording_context_menu_show, R.drawable.list_activities_play_note, listener, 0);
			} else {
				adapter.registerItem(R.string.recording_context_menu_play, R.drawable.list_activities_play_note, listener, 0);
			}
			adapter.registerItem(R.string.show_location, 0, new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					SHOW_RECORDINGS.set(true);
					app.getSettings().setMapLocationToShow(ri.rec.lat, ri.rec.lon, app.getSettings().getLastKnownMapZoom());
					MapActivity.launchMapActivityMoveToTop(la);

				}
			}, 0);
		}
	}
	
	public Collection<Recording> getAllRecordings(){
		return recordingByFileName.values();
	}
	
	private Recording[] getRecordingsSorted() {
		checkRecordings();
		Collection<Recording> allObjects = getAllRecordings();
		Recording[] res = allObjects.toArray(new Recording[allObjects.size()]);
		Arrays.sort(res, new Comparator<Recording>() {

			@Override
			public int compare(Recording object1, Recording object2) {
				long l1 = object1.file.lastModified();
				long l2 = object1.file.lastModified();
				return l1 < l2 ? 1 : -1;
			}
		});
		return res;
	}
	
	
	public void playRecording(final Context ctx, final Recording r) {
		final MediaPlayer player = r.isPhoto() ? null : new MediaPlayer();
		final AccessibleAlertBuilder dlg = new AccessibleAlertBuilder(ctx);
		dlg.setPositiveButton(R.string.recording_open_external_player, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface v, int w) {
				if(player == null) {
					Intent vint = new Intent(Intent.ACTION_VIEW);
					vint.setDataAndType(Uri.fromFile(r.file), "image/*");
					vint.setFlags(0x10000000);
					ctx.startActivity(vint);
				} else {
					if (player.isPlaying()) {
						player.stop();
					}
					Intent vint = new Intent(Intent.ACTION_VIEW);
					vint.setDataAndType(Uri.fromFile(r.file), "video/*");
					vint.setFlags(0x10000000);
					try {
						ctx.startActivity(vint);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		dlg.setNegativeButton(R.string.default_buttons_cancel, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(player != null && player.isPlaying()) {
					player.stop();
				}				
				
			}
			
		});
		try {
			if (r.isPhoto()) {
				ImageView img = new ImageView(ctx);
				Options opts = new Options();
				opts.inSampleSize = 4;
				int rot = r.getBitmapRotation();
				Bitmap bmp = BitmapFactory.decodeFile(r.file.getAbsolutePath(), opts);
				if (rot != 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(rot);
					Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
					bmp.recycle();
					bmp = resizedBitmap;
				}
				img.setImageBitmap(bmp);
				dlg.setView(img);
				dlg.show();
			} else {
				dlg.setMessage(ctx.getString(R.string.recording_playing, r.getDescription(ctx)));
				player.setDataSource(r.file.getAbsolutePath());
				player.setOnPreparedListener(new OnPreparedListener() {

					@Override
					public void onPrepared(MediaPlayer mp) {
						dlg.show();
						player.start();
					}
				});
				player.prepareAsync();
			}
		} catch (Exception e) {
			AccessibleToast.makeText(ctx, R.string.recording_can_not_be_played, Toast.LENGTH_SHORT).show();
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
	
	public class AudioVideoPhotoHandler implements PictureCallback {
		private File pictureFile;
		private Dialog dlg;

		public AudioVideoPhotoHandler(Dialog dlg, File fileName) {
			this.dlg = dlg;
			this.pictureFile = fileName;
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				indexingFiles(null, true);
				dlg.dismiss();
			} catch (Exception error) {
				logErr(error);
			} finally {
				camera.release();
			}
		}

	} 

}
