package net.osmand.plus.audionotes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.LogUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapStackControl;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;

import org.apache.commons.logging.Log;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final String ID = "osmand.audionotes";
	private static final String THREEGP_EXTENSION = "3gp";
	private static final String MPEG4_EXTENSION = "mp4";
	private static final String IMG_EXTENSION = "jpg";
	private static final Log log = LogUtil.getLog(AudioVideoNotesPlugin.class);
	private OsmandApplication app;
	private TextInfoControl recordControl;
	
	
	private DataTileManager<Recording> recordings = new DataTileManager<AudioVideoNotesPlugin.Recording>(14);
	private AudioNotesLayer audioNotesLayer;
	private MapActivity activity;
	private MediaRecorder mediaRec;
	
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
		
		public void updatePhotoInformation(){
			// FIXME
//			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
//			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitude);
//			exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
//	        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitude);
//	        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
//	        exif.setAttribute("GPSImgDirectionRef", "Magnetic North");
//	        exif.setAttribute("GPSImgDirection", ((float) rot)+"");
//	        exif.saveAttributes();
		}
		
		private int getExifOrientation() {
			ExifInterface exif;
			int orientation = 0;
			try {
				exif = new ExifInterface(file.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
			} catch (IOException e) {
				e.printStackTrace();
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
						DateFormat.format("dd.MM.yyyy kk:mm", file.lastModified())).trim();
			}
			updateInternalDescription();
			String additional = "";
			if(duration > 0) {
				int d = (int) (duration / 1000);
				
				String min;
				if(d % 60 < 10 ) {
					min = "0" + (d % 60);
				} else {
					min = (d % 60) +"";
				}
				additional +=  (d / 60) + ":" + min ;
			}
			if(!available) {
				additional += "("+ctx.getString(R.string.recording_unavailable)+")";
			}
			return ctx.getString(R.string.recording_description, nm, additional, 
					DateFormat.format("dd.MM.yyyy kk:mm", file.lastModified())).trim();
		}
	}
	
	@Override
	public String getId() {
		return ID;
	}

	public AudioVideoNotesPlugin(OsmandApplication app) {
		this.app = app;

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
	
	
	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.layer_recordings) {
					dialog.dismiss();
					app.getSettings().SHOW_RECORDINGS.set(!app.getSettings().SHOW_RECORDINGS.get());
					updateLayers(mapView, mapActivity);
				}
			}
		};
		adapter.registerSelectedItem(R.string.layer_recordings, app.getSettings().SHOW_RECORDINGS.get()? 1 : 0, 0, listener, 5);
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			Object selectedObj) {
		adapter.registerItem(R.string.recording_context_menu_arecord, 0, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				recordAudio(latitude, longitude, mapActivity);
			}
		}, 6);
		adapter.registerItem(R.string.recording_context_menu_vrecord, 0, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				recordVideo(latitude, longitude, mapActivity);
			}
		}, 7);
		adapter.registerItem(R.string.recording_context_menu_precord, 0, new OnContextMenuClick() {
			
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				takePhoto(latitude, longitude, mapActivity);
			}

		}, 8);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(app.getSettings().SHOW_RECORDINGS.get()) {
			if(!mapView.getLayers().contains(audioNotesLayer)) {
				mapView.addLayer(audioNotesLayer, 3.5f);
			}
		} else if(audioNotesLayer != null){
			mapView.removeLayer(audioNotesLayer);
		}
	}
	
	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null ) {
			recordControl = new TextInfoControl(activity, 0, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			recordControl.setImageDrawable(activity.getResources().getDrawable(R.drawable.monitoring_rec_small));
			setRecordListener(recordControl, activity);
			mapInfoLayer.getMapInfoControls().registerSideWidget(recordControl,
					0/*R.drawable.widget_parking*/, R.string.map_widget_av_notes, "audionotes", false,
					EnumSet.allOf(ApplicationMode.class),
					EnumSet.noneOf(ApplicationMode.class), 22);
			mapInfoLayer.recreateControls();
		}
	}

	private void setRecordListener(final TextInfoControl recordPlaceControl, final MapActivity mapActivity) {
		recordPlaceControl.setText(app.getString(R.string.av_control_start), "");
		recordPlaceControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				defaultAction(mapActivity);
			}
		});
	}
	
	private void defaultAction(final MapActivity mapActivity) {
		final Location loc = mapActivity.getLastKnownLocation();
		// double lat = mapActivity.getMapView().getLatitude();
		// double lon = mapActivity.getMapView().getLongitude();
		if (loc == null) {
			AccessibleToast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
			return;
		}
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		if (app.getSettings().AV_DEFAULT_ACTION.get() == OsmandSettings.AV_DEFAULT_ACTION_VIDEO) {
			recordVideo(lat, lon, mapActivity);
		} else if (app.getSettings().AV_DEFAULT_ACTION.get() == OsmandSettings.AV_DEFAULT_ACTION_TAKEPICTURE) {
			takePhoto(lat, lon, mapActivity);
		} else {
			recordAudio(lat, lon, mapActivity);
		}
	}
	
	
	private File getBaseFileName(double lat, double lon, OsmandApplication app, String ext) {
		String basename = MapUtils.createShortLocString(lat, lon, 15);
		int k = 1;
		File f = app.getSettings().extendOsmandPath(ResourceManager.AV_PATH);
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
		if(app.getSettings().AV_VIDEO_FORMAT.get() == OsmandSettings.VIDEO_OUTPUT_3GP ){
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
		if(app.getSettings().AV_EXTERNAL_RECORDER.get()) {
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
				if(app.getSettings().AV_VIDEO_FORMAT.get() == OsmandSettings.VIDEO_OUTPUT_3GP ){
					ext = THREEGP_EXTENSION;
				}
				final File f = getBaseFileName(lat, lon, app,ext );
				
				mr.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
				mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				if(app.getSettings().AV_VIDEO_FORMAT.get() == OsmandSettings.VIDEO_OUTPUT_3GP ){
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
		setRecordListener(recordControl, mapActivity);
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
	
	public void takePhoto(double lat, double lon, final MapActivity mapActivity) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		final File f = getBaseFileName(lat, lon, app, IMG_EXTENSION);
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
		final MapInfoLayer mil = mapActivity.getMapLayers().getMapInfoLayer();
		final MapStackControl par = mil.getRightStack();
		final boolean contains = par.getAllViews().contains(recordControl);
		if(!contains) {
			par.addStackView(recordControl);
		}
		AccessibleToast.makeText(mapActivity, R.string.recording_is_recorded, Toast.LENGTH_LONG).show();
		recordControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!contains) {
					par.removeView(recordControl);
				}
				stopRecording(mapActivity);
				indexFile(f);
				mapActivity.getMapView().refreshMap();
			}

		});
	}
	
	public void indexFile(File f){
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
		recordings.registerObject(r.lat, r.lon, r);
	}

	@Override
	public void disable(OsmandApplication app) {
	}
	
	@Override
	public List<String> indexingFiles(IProgress progress) {
		File avPath = app.getSettings().extendOsmandPath(ResourceManager.AV_PATH);
		if (avPath.canRead()) {
			recordings.clear();
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

	public void deleteRecording(Recording r) {
		recordings.unregisterObject(r.lat, r.lon, r);
		Algoritms.removeAllFiles(r.file);
		activity.getMapLayers().getContextMenuLayer().setLocation(null, "");
		activity.getMapView().refreshMap();
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceGroup grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setSummary(R.string.av_settings_descr);
		grp.setTitle(R.string.av_settings);
		grp.setKey("av_settings");
		screen.addPreference(grp);
		OsmandSettings settings = app.getSettings();
		
		grp.addPreference(activity.createCheckBoxPreference(settings.AV_EXTERNAL_RECORDER, 
				R.string.av_use_external_recorder, R.string.av_use_external_recorder_descr));
		
		String[] entries = new String[] {"3GP", "MP4"};
		Integer[] intValues = new Integer[] {OsmandSettings.VIDEO_OUTPUT_3GP, OsmandSettings.VIDEO_OUTPUT_MP4};
		ListPreference lp = activity.createListPreference(settings.AV_VIDEO_FORMAT, 
				entries, intValues, R.string.av_video_format, R.string.av_video_format_descr);
		grp.addPreference(lp);
		
		entries = new String[] {app.getString(R.string.av_def_action_audio), app.getString(R.string.av_def_action_video),
				app.getString(R.string.av_def_action_picture)};
		intValues = new Integer[] {OsmandSettings.AV_DEFAULT_ACTION_AUDIO, OsmandSettings.AV_DEFAULT_ACTION_VIDEO,
				OsmandSettings.AV_DEFAULT_ACTION_TAKEPICTURE};
		ListPreference defAct = activity.createListPreference(settings.AV_DEFAULT_ACTION, 
				entries, intValues, R.string.av_widget_action, R.string.av_widget_action_descr);
		grp.addPreference(defAct);
		
	}
	
	@Override
	public void onMapActivityExternalResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 205) {
			indexingFiles(null);
		}
	}
	
	public boolean onMapActivityKeyEvent(KeyEvent key){
		if(KeyEvent.KEYCODE_CAMERA == key.getKeyCode()) {
			defaultAction(activity);
			return true;
		}
		return false;
	}

}
