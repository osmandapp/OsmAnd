package net.osmand.plus.audionotes;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

public class AudioVideoNotesPlugin extends OsmandPlugin {

	public static final String ID = "osmand.audionotes";
	private static final String AV_EXTENSION = "3gp";
	private static final Log log = LogUtil.getLog(AudioVideoNotesPlugin.class);
	private OsmandApplication app;
	private TextInfoControl recordControl;
	
	
	private DataTileManager<Recording> recordings = new DataTileManager<AudioVideoNotesPlugin.Recording>(14);
	private AudioNotesLayer audioNotesLayer;
	private MapActivity activity;
	
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
			if(duration == -1) {
				duration = 0;
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
		
		public String getDescription(Context ctx){
			String nm = name == null? "" : name ;
			updateInternalDescription();
			String additional = "";
			if(duration > 0) {
				int d = (int) (duration / 1000);
				String min = (String) ((d % 60) < 10 ? "0"+(d % 60) : (d % 60));
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
				recordAudioVideo(latitude, longitude, mapActivity, false);
			}
		}, 6);
//		adapter.registerItem(R.string.recording_context_menu_vrecord, 0, new OnContextMenuClick() {
//			
//			@Override
//			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
//				recordAudioVideo(latitude, longitude, mapActivity, true);
//			}
//		}, 7);
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
				final Location loc = mapActivity.getLastKnownLocation();
//				double lat = mapActivity.getMapView().getLatitude();
//				double lon = mapActivity.getMapView().getLongitude();
				if(loc == null) {
					AccessibleToast.makeText(app, R.string.audionotes_location_not_defined, Toast.LENGTH_LONG).show();
					return;
				}
				recordAudioVideo(loc.getLatitude(), loc.getLongitude(), mapActivity, false);
			}
		});
	}
	
	private File getBaseFileNameForAV(double lat, double lon, OsmandApplication app) {
		String basename = MapUtils.createShortLocString(lat, lon, 15);
		int k = 1;
		File f = app.getSettings().extendOsmandPath(ResourceManager.AV_PATH);
		f.mkdirs();
		File fl;
		do {
			fl = new File(f, basename + "_" + (k++) + "." + AV_EXTENSION);
		} while(fl.exists());
		return fl;
	}
	
	private void recordAudioVideo(double lat, double lon, final MapActivity mapActivity, boolean recordVideo) {
		final MediaRecorder mr = new MediaRecorder();
		final File f = getBaseFileNameForAV(lat, lon, app);
		mr.setOutputFile(f.getAbsolutePath());
		if(recordVideo) {
			mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		}
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
		mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		if(recordVideo){
			mr.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
		}
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		
		recordControl.setText(app.getString(R.string.av_control_stop), "");
		AccessibleToast.makeText(mapActivity, R.string.recording_is_recorded, Toast.LENGTH_LONG).show();
		try {
			mr.prepare();
			mr.start();
			recordControl.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					indexFile(f);
					mr.stop();
					mr.release();
					mapActivity.getMapView().refreshMap();
					setRecordListener(recordControl, mapActivity);
				}
			});
		} catch (Exception e) {
			log.error("Error starting audio recorder ", e);
			AccessibleToast.makeText(app, app.getString(R.string.error_io_error) + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void indexFile(File f){
		Recording r = new Recording(f);
		String encodeName = f.getName();
		int i = encodeName.indexOf('_');
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
			File[] files = avPath.listFiles();
			if (files != null) {
				for (File f : files) {
					if(f.getName().endsWith(AV_EXTENSION)) {
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

}
